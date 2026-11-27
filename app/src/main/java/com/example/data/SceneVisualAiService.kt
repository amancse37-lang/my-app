package com.example.data

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AspectRatio
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ImageSize
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

private const val VISUAL_AI_TAG = "SceneVisualAiService"

data class BackgroundGenerationRequest(
    val assetName: String = "",
    val sceneTitle: String = "",
    val sceneDescription: String = "",
    val sceneNarration: String = "",
    val location: String = "",
    val eraContext: String = "",
    val timeOfDay: String = "Golden Hour",
    val weather: String = "Clear",
    val visualStyle: String = "Painterly 2D Documentary",
    val moodAtmosphere: String = "",
    val customPrompt: String = "",
    val variationSeed: Int = 1
)

data class PropGenerationRequest(
    val propName: String = "",
    val propCategory: String = "Historical Object",
    val propDescription: String = "",
    val sceneContext: String = "",
    val eraContext: String = "",
    val visualStyle: String = "Painterly 2D Documentary",
    val targetLayer: String = "PROPS",
    val requestTransparentPng: Boolean = true,
    val variationSeed: Int = 1
)

data class GeneratedVisualAsset(
    val bitmap: Bitmap,
    val imageBase64: String,
    val hasActualTransparency: Boolean,
    val formatLabel: String,
    val promptUsed: String,
    val width: Int,
    val height: Int
)

data class SuggestedBackgroundPlan(
    val title: String,
    val location: String,
    val eraContext: String,
    val timeOfDay: String,
    val weather: String,
    val moodAtmosphere: String,
    val prompt: String,
    val rationale: String
)

data class SuggestedPropPlan(
    val name: String,
    val category: String,
    val suggestedLayer: String,
    val prompt: String,
    val rationale: String
)

data class SceneVisualSuggestions(
    val sceneId: String,
    val background: SuggestedBackgroundPlan,
    val props: List<SuggestedPropPlan>,
    val environmentalDetails: List<String>
)

class SceneVisualAiService {

    private val backgroundImageModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
                imageConfig = ImageConfig.builder()
                    .setAspectRatio(AspectRatio.LANDSCAPE_16x9)
                    .setImageSize(ImageSize.SIZE_1K)
                    .build()
            }
        )
    }

    private val propImageModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
                imageConfig = ImageConfig.builder()
                    .setAspectRatio(AspectRatio.SQUARE_1x1)
                    .setImageSize(ImageSize.SIZE_1K)
                    .build()
            }
        )
    }

    private val suggestionSchema = Schema.obj(
        properties = mapOf(
            "background" to Schema.obj(
                properties = mapOf(
                    "title" to Schema.string(description = "Short descriptive name for the generated background asset (max 60 chars)"),
                    "location" to Schema.string(description = "Specific setting or environment location"),
                    "eraContext" to Schema.string(description = "Historical period or contemporary documentary context"),
                    "timeOfDay" to Schema.enumeration(
                        values = StudioPresets.timeOfDayOptions,
                        description = "Lighting time of day matching the scene"
                    ),
                    "weather" to Schema.enumeration(
                        values = StudioPresets.weatherOptions,
                        description = "Weather or atmospheric condition"
                    ),
                    "moodAtmosphere" to Schema.string(description = "Cinematic color palette, lighting, and emotional tone"),
                    "prompt" to Schema.string(description = "Rich 2D documentary background art prompt without characters or text"),
                    "rationale" to Schema.string(description = "Short explanation of why this background fits the scene")
                )
            ),
            "props" to Schema.array(
                description = "2 to 4 scene-relevant individual visual props or architectural/environmental elements for 2D compositing.",
                items = Schema.obj(
                    properties = mapOf(
                        "name" to Schema.string(description = "Concise visual prop name (max 50 chars)"),
                        "category" to Schema.enumeration(
                            values = StudioPresets.propCategories,
                            description = "Prop category"
                        ),
                        "suggestedLayer" to Schema.enumeration(
                            values = listOf("PROPS", "MIDGROUND", "FOREGROUND"),
                            description = "Recommended compositor depth layer"
                        ),
                        "prompt" to Schema.string(description = "Detailed visual prompt for generating this single isolated 2D prop"),
                        "rationale" to Schema.string(description = "Why this prop enriches the documentary storytelling")
                    )
                )
            ),
            "environmentalDetails" to Schema.array(
                description = "2 to 4 concise staging or atmospheric detail suggestions (lighting cues, framing, depth cues).",
                items = Schema.string()
            )
        )
    )

    private val suggestionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = suggestionSchema
            }
        )
    }

    fun defaultBackgroundRequestForScene(
        scene: StudioScene,
        projectVisualStyle: String = "Painterly 2D Documentary"
    ): BackgroundGenerationRequest {
        val inferredLocation = scene.title
            .substringAfter("·", scene.title)
            .trim()
            .ifBlank { scene.backgroundPreset.replace("_", " ") }
        val baseDraft = BackgroundGenerationRequest(
            assetName = "${scene.title.take(40).ifBlank { "Scene Backdrop" }} BG",
            sceneTitle = scene.title,
            sceneDescription = scene.description,
            sceneNarration = scene.narrationText,
            location = inferredLocation,
            eraContext = inferEraHint(scene),
            timeOfDay = inferTimeOfDay(scene),
            weather = inferWeather(scene),
            visualStyle = projectVisualStyle.ifBlank { "Painterly 2D Documentary" },
            moodAtmosphere = inferMood(scene),
            customPrompt = ""
        )
        return baseDraft.copy(
            customPrompt = buildEditableBackgroundSummary(baseDraft)
        )
    }

    fun defaultPropRequestForScene(
        scene: StudioScene,
        projectVisualStyle: String = "Painterly 2D Documentary"
    ): PropGenerationRequest {
        val rawPropName = scene.objectProp.trim().ifBlank { "Documentary Artifact" }
        val desc = buildString {
            append(rawPropName)
            if (scene.visualSummary.isNotBlank()) {
                append(", matching the visual world of ")
                append(scene.visualSummary.take(160))
            }
        }
        return PropGenerationRequest(
            propName = rawPropName.take(60),
            propCategory = "Historical Object",
            propDescription = desc.take(500),
            sceneContext = scene.description.take(400),
            eraContext = inferEraHint(scene),
            visualStyle = projectVisualStyle.ifBlank { "Painterly 2D Documentary" },
            targetLayer = "PROPS",
            requestTransparentPng = true
        )
    }

    fun buildEditableBackgroundSummary(request: BackgroundGenerationRequest): String {
        return buildString {
            if (request.location.isNotBlank()) append("2D illustrated documentary environment of ${request.location.trim()}. ")
            if (request.sceneDescription.isNotBlank()) append("${request.sceneDescription.trim().take(240)} ")
            if (request.eraContext.isNotBlank()) append("Context/Era: ${request.eraContext.trim()}. ")
            if (request.timeOfDay.isNotBlank()) append("Time of day: ${request.timeOfDay}. ")
            if (request.weather.isNotBlank() && request.weather != "Indoor / N/A") append("Weather: ${request.weather}. ")
            if (request.moodAtmosphere.isNotBlank()) append("Mood: ${request.moodAtmosphere.trim()}.")
        }.trim()
    }

    fun buildBackgroundPrompt(request: BackgroundGenerationRequest): String {
        val style = request.visualStyle.ifBlank { "Painterly 2D Documentary" }
        return buildString {
            appendLine("Create a cinematic 16:9 widescreen 2D animated documentary background painting.")
            appendLine()
            appendLine("ART DIRECTION & VISUAL STYLE:")
            appendLine("- Visual Style: $style, hand-crafted 2D animation background art with rich depth separation.")
            if (request.location.isNotBlank()) appendLine("- Location / Setting: ${request.location.trim()}")
            if (request.eraContext.isNotBlank()) appendLine("- Historical / Contemporary Context: ${request.eraContext.trim()}")
            if (request.timeOfDay.isNotBlank()) appendLine("- Time of Day & Lighting: ${request.timeOfDay.trim()}")
            if (request.weather.isNotBlank()) appendLine("- Weather / Atmosphere: ${request.weather.trim()}")
            if (request.moodAtmosphere.isNotBlank()) appendLine("- Mood & Color Atmosphere: ${request.moodAtmosphere.trim()}")
            appendLine()
            appendLine("SCENE CONTEXT:")
            if (request.sceneTitle.isNotBlank()) appendLine("- Scene Title: ${request.sceneTitle.trim()}")
            if (request.sceneDescription.isNotBlank()) appendLine("- Scene Description: ${request.sceneDescription.trim().take(600)}")
            if (request.sceneNarration.isNotBlank()) appendLine("- Scene Narration Context: ${request.sceneNarration.trim().take(500)}")
            if (request.customPrompt.isNotBlank()) {
                appendLine()
                appendLine("USER VISUAL DIRECTION:")
                appendLine(request.customPrompt.trim().take(900))
            }
            if (request.variationSeed > 1) {
                appendLine("- Creative Variation #${request.variationSeed}: explore a fresh framing angle, sky/lighting arrangement, and architectural composition while remaining faithful to the scene.")
            }
            appendLine()
            appendLine("STRICT COMPOSITING CONSTRAINTS:")
            appendLine("- Full-bleed 16:9 background environment ONLY.")
            appendLine("- Leave clean, balanced midground/foreground staging space for 2D characters and props.")
            appendLine("- NO text, NO captions, NO watermarks, NO logos, NO UI elements, NO borders or picture frames, NO random characters or people.")
        }.trim()
    }

    fun buildPropPrompt(request: PropGenerationRequest): String {
        val style = request.visualStyle.ifBlank { "Painterly 2D Documentary" }
        return buildString {
            appendLine("Create a single isolated 2D animated documentary visual prop asset for scene compositing.")
            appendLine()
            appendLine("PROP SPECIFICATION:")
            appendLine("- Object Name: ${request.propName.trim().ifBlank { "Documentary Prop" }}")
            appendLine("- Category: ${request.propCategory.ifBlank { "Documentary Artifact" }}")
            if (request.propDescription.isNotBlank()) {
                appendLine("- Detailed Visual Description: ${request.propDescription.trim().take(700)}")
            }
            if (request.eraContext.isNotBlank()) {
                appendLine("- Historical / Contemporary Context: ${request.eraContext.trim()}")
            }
            if (request.sceneContext.isNotBlank()) {
                appendLine("- Matching Scene Atmosphere: ${request.sceneContext.trim().take(300)}")
            }
            appendLine("- Visual Art Style: $style, clean 2D animation production prop illustration with crisp edges and readable silhouette.")
            if (request.variationSeed > 1) {
                appendLine("- Creative Variation #${request.variationSeed}: vary surface detailing, material patina, and slight angle while depicting the exact same object.")
            }
            appendLine()
            appendLine("ISOLATION & COMPOSITING CONSTRAINTS:")
            if (request.requestTransparentPng) {
                appendLine("- Render the single object centered with generous margin on a completely transparent PNG background (alpha = 0 outside the prop) with NO drop shadow box or background scenery.")
            } else {
                appendLine("- Render the single object centered with generous margin on a clean, uniform neutral dark studio backdrop with NO background scenery.")
            }
            appendLine("- Single prop only. NO text, NO labels, NO watermarks, NO borders, NO UI chrome, NO hands or people.")
        }.trim()
    }

    suspend fun generateSceneBackground(
        request: BackgroundGenerationRequest
    ): Result<GeneratedVisualAsset> {
        if (request.location.isBlank() && request.customPrompt.isBlank() && request.sceneDescription.isBlank()) {
            return Result.failure(
                IllegalArgumentException("Enter a background prompt or scene description before generating.")
            )
        }
        val fullPrompt = buildBackgroundPrompt(request)
        return try {
            val bitmap = withTimeout(60_000L) {
                val response = backgroundImageModel.generateContent(fullPrompt)
                response.candidates.firstOrNull()
                    ?.content?.parts?.filterIsInstance<ImagePart>()
                    ?.firstOrNull()
                    ?.image
            }
            if (bitmap == null) {
                return Result.failure(
                    IllegalStateException("Image model did not return a background image. Verify Firebase Blaze billing and try again.")
                )
            }
            val encoded = withContext(Dispatchers.Default) {
                CharacterBitmapCodec.encodeBitmapToBase64Jpeg(
                    bitmap = bitmap,
                    maxDimension = 768,
                    quality = 78
                )
            }
            Result.success(
                GeneratedVisualAsset(
                    bitmap = bitmap,
                    imageBase64 = encoded,
                    hasActualTransparency = false,
                    formatLabel = "16:9 JPEG · ${bitmap.width}×${bitmap.height}",
                    promptUsed = fullPrompt.take(1500),
                    width = bitmap.width,
                    height = bitmap.height
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(VISUAL_AI_TAG, "Background generation timed out", e)
            Result.failure(Exception("Background generation timed out after 60 seconds. Please retry."))
        } catch (e: QuotaExceededException) {
            Log.e(VISUAL_AI_TAG, "Background generation quota exceeded", e)
            Result.failure(Exception("Image generation quota exceeded. Verify your Firebase Blaze plan or retry later."))
        } catch (e: IOException) {
            Log.e(VISUAL_AI_TAG, "Network error during background generation", e)
            Result.failure(Exception("Network error while generating background. Check your connection and retry."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(VISUAL_AI_TAG, "Background generation failed", e)
            Result.failure(
                Exception(
                    e.localizedMessage
                        ?: "Background generation failed. Ensure Firebase AI Logic image generation (Blaze plan) is enabled."
                )
            )
        }
    }

    suspend fun generateVisualProp(
        request: PropGenerationRequest
    ): Result<GeneratedVisualAsset> {
        if (request.propName.isBlank() && request.propDescription.isBlank()) {
            return Result.failure(
                IllegalArgumentException("Enter a prop name or visual description before generating.")
            )
        }
        val fullPrompt = buildPropPrompt(request)
        return try {
            val bitmap = withTimeout(60_000L) {
                val response = propImageModel.generateContent(fullPrompt)
                response.candidates.firstOrNull()
                    ?.content?.parts?.filterIsInstance<ImagePart>()
                    ?.firstOrNull()
                    ?.image
            }
            if (bitmap == null) {
                return Result.failure(
                    IllegalStateException("Image model did not return a prop illustration. Verify Firebase Blaze billing and try again.")
                )
            }
            val (actualAlpha, encoded) = withContext(Dispatchers.Default) {
                val hasAlpha = CharacterBitmapCodec.hasActualTransparency(bitmap)
                val base64 = if (hasAlpha) {
                    CharacterBitmapCodec.encodeBitmapPreservingAlpha(
                        bitmap = bitmap,
                        maxDimension = 512,
                        jpegQuality = 78
                    )
                } else {
                    CharacterBitmapCodec.encodeBitmapToBase64Jpeg(
                        bitmap = bitmap,
                        maxDimension = 512,
                        quality = 78
                    )
                }
                // Re-verify decoded alpha in case oversized PNG fell back to JPEG
                val persistedAlpha = hasAlpha && (
                    CharacterBitmapCodec.decodeBase64ToBitmap(base64)
                        ?.let { CharacterBitmapCodec.hasActualTransparency(it) } == true
                    )
                persistedAlpha to base64
            }
            val fmt = if (actualAlpha) {
                "Transparent PNG · ${bitmap.width}×${bitmap.height}"
            } else {
                "Opaque JPEG · ${bitmap.width}×${bitmap.height}"
            }
            Result.success(
                GeneratedVisualAsset(
                    bitmap = bitmap,
                    imageBase64 = encoded,
                    hasActualTransparency = actualAlpha,
                    formatLabel = fmt,
                    promptUsed = fullPrompt.take(1500),
                    width = bitmap.width,
                    height = bitmap.height
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(VISUAL_AI_TAG, "Prop generation timed out", e)
            Result.failure(Exception("Prop generation timed out after 60 seconds. Please retry."))
        } catch (e: QuotaExceededException) {
            Log.e(VISUAL_AI_TAG, "Prop generation quota exceeded", e)
            Result.failure(Exception("Image generation quota exceeded. Verify your Firebase Blaze plan or retry later."))
        } catch (e: IOException) {
            Log.e(VISUAL_AI_TAG, "Network error during prop generation", e)
            Result.failure(Exception("Network error while generating prop. Check your connection and retry."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(VISUAL_AI_TAG, "Prop generation failed", e)
            Result.failure(
                Exception(
                    e.localizedMessage
                        ?: "Prop generation failed. Ensure Firebase AI Logic image generation (Blaze plan) is enabled."
                )
            )
        }
    }

    suspend fun suggestSceneVisuals(
        scene: StudioScene,
        projectName: String,
        projectVisualStyle: String
    ): Result<SceneVisualSuggestions> {
        val prompt = """
            You are a 2D documentary art director for MotionStory Studio.
            Analyze the documentary scene below and suggest:
            1. One cohesive 16:9 2D animated documentary background (no characters, no text).
            2. 2 to 4 specific visual props or environmental/architectural objects that belong in this scene (e.g. historical objects, books, furniture, tools, vehicles, lanterns, maps, documentary artifacts).
            3. 2 to 4 concise environmental staging details (lighting, depth separation, atmosphere).

            PROJECT: ${projectName.ifBlank { "Documentary Project" }}
            TARGET 2D VISUAL STYLE: ${projectVisualStyle.ifBlank { "Painterly 2D Documentary" }}
            SCENE TITLE: ${scene.title}
            SCENE DESCRIPTION: ${scene.description}
            VISUAL SUMMARY: ${scene.visualSummary}
            NARRATION: ${scene.narrationText}
            SUBTITLE: ${scene.subtitleText}
            EXISTING PROP NOTES: ${scene.objectProp}
            CURRENT EFFECT: ${scene.effectPreset}
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                suggestionModel.generateContent(prompt)
            }
            val jsonText = response.text
                ?: return Result.failure(IllegalStateException("Gemini returned empty visual suggestions."))
            val parsed = parseSuggestionsJson(scene.id, jsonText)
            Result.success(parsed)
        } catch (e: TimeoutCancellationException) {
            Log.e(VISUAL_AI_TAG, "Scene suggestions timed out", e)
            Result.failure(Exception("Visual suggestions timed out after 60 seconds. Please retry."))
        } catch (e: QuotaExceededException) {
            Log.e(VISUAL_AI_TAG, "Scene suggestions quota exceeded", e)
            Result.failure(Exception("Gemini API quota limit reached. Please wait a moment and retry."))
        } catch (e: JSONException) {
            Log.e(VISUAL_AI_TAG, "Malformed scene suggestions JSON", e)
            Result.failure(Exception("AI returned an unexpected suggestion format. Please retry."))
        } catch (e: IOException) {
            Log.e(VISUAL_AI_TAG, "Network error during scene suggestions", e)
            Result.failure(Exception("Network error while fetching scene visual suggestions."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(VISUAL_AI_TAG, "Scene suggestions failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Failed to generate scene visual suggestions."))
        }
    }

    internal fun parseSuggestionsJson(sceneId: String, jsonText: String): SceneVisualSuggestions {
        val root = JSONObject(jsonText)
        val bgObj = root.optJSONObject("background") ?: JSONObject()
        val rawBackground = SuggestedBackgroundPlan(
            title = bgObj.optString("title", ""),
            location = bgObj.optString("location", ""),
            eraContext = bgObj.optString("eraContext", ""),
            timeOfDay = bgObj.optString("timeOfDay", "Golden Hour"),
            weather = bgObj.optString("weather", "Clear"),
            moodAtmosphere = bgObj.optString("moodAtmosphere", ""),
            prompt = bgObj.optString("prompt", ""),
            rationale = bgObj.optString("rationale", "")
        )

        val propsArray = root.optJSONArray("props")
        val rawProps = buildList {
            if (propsArray != null) {
                for (i in 0 until propsArray.length()) {
                    val p = propsArray.optJSONObject(i) ?: continue
                    add(
                        SuggestedPropPlan(
                            name = p.optString("name", ""),
                            category = p.optString("category", "Documentary Artifact"),
                            suggestedLayer = p.optString("suggestedLayer", "PROPS"),
                            prompt = p.optString("prompt", ""),
                            rationale = p.optString("rationale", "")
                        )
                    )
                }
            }
        }

        val detailsArray = root.optJSONArray("environmentalDetails")
        val rawDetails = buildList {
            if (detailsArray != null) {
                for (i in 0 until detailsArray.length()) {
                    add(detailsArray.optString(i, ""))
                }
            }
        }

        return normalizeSuggestions(
            sceneId = sceneId,
            rawBackground = rawBackground,
            rawProps = rawProps,
            rawDetails = rawDetails
        )
    }

    internal fun normalizeSuggestions(
        sceneId: String,
        rawBackground: SuggestedBackgroundPlan,
        rawProps: List<SuggestedPropPlan>,
        rawDetails: List<String>
    ): SceneVisualSuggestions {
        val bgPlan = SuggestedBackgroundPlan(
            title = rawBackground.title.trim().ifBlank { "Documentary Scene Backdrop" }.take(80),
            location = rawBackground.location.trim().take(160),
            eraContext = rawBackground.eraContext.trim().take(160),
            timeOfDay = rawBackground.timeOfDay.trim()
                .takeIf { it in StudioPresets.timeOfDayOptions } ?: "Golden Hour",
            weather = rawBackground.weather.trim()
                .takeIf { it in StudioPresets.weatherOptions } ?: "Clear",
            moodAtmosphere = rawBackground.moodAtmosphere.trim().take(200),
            prompt = rawBackground.prompt.trim().take(900),
            rationale = rawBackground.rationale.trim().take(300)
        )

        val validLayers = setOf("PROPS", "MIDGROUND", "FOREGROUND")
        val propPlans = rawProps.mapNotNull { p ->
            val name = p.name.trim()
            if (name.isBlank()) {
                null
            } else {
                SuggestedPropPlan(
                    name = name.take(80),
                    category = p.category.trim()
                        .takeIf { it in StudioPresets.propCategories } ?: "Documentary Artifact",
                    suggestedLayer = p.suggestedLayer.trim().uppercase()
                        .takeIf { it in validLayers } ?: "PROPS",
                    prompt = p.prompt.trim().ifBlank { name }.take(600),
                    rationale = p.rationale.trim().take(240)
                )
            }
        }.take(6)

        val details = rawDetails
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { it.take(240) }
            .take(6)

        return SceneVisualSuggestions(
            sceneId = sceneId,
            background = bgPlan,
            props = propPlans,
            environmentalDetails = details
        )
    }

    private fun inferTimeOfDay(scene: StudioScene): String {
        val text = "${scene.title} ${scene.description} ${scene.visualSummary}".lowercase()
        return when {
            "dawn" in text || "sunrise" in text || "सूर्योदय" in text -> "Dawn"
            "twilight" in text || "dusk" in text || "evening" in text || "संध्या" in text -> "Twilight"
            "night" in text || "lamp" in text || "रात" in text -> "Night"
            "morning" in text -> "Morning"
            "sunset" in text || "golden" in text -> "Golden Hour"
            else -> "Golden Hour"
        }
    }

    private fun inferWeather(scene: StudioScene): String {
        val text = "${scene.title} ${scene.description} ${scene.visualSummary}".lowercase()
        return when {
            "mist" in text || "fog" in text -> "Misty"
            "snow" in text || "बर्फ" in text -> "Snowy"
            "rain" in text || "monsoon" in text -> "Rainy"
            "study" in text || "interior" in text || "int." in text || "कक्ष" in text -> "Indoor / N/A"
            "dust" in text -> "Dusty Haze"
            else -> "Clear"
        }
    }

    private fun inferEraHint(scene: StudioScene): String {
        val text = "${scene.title} ${scene.description} ${scene.narrationText} ${scene.motionGraphicTitle}"
        val yearMatch = Regex("\\b(1[5-9]\\d{2}|20\\d{2})\\b").find(text)
        return when {
            yearMatch != null -> "Historical era circa ${yearMatch.value}"
            text.contains("ancient", ignoreCase = true) || text.contains("heritage", ignoreCase = true) ->
                "Historical heritage documentary setting"
            text.contains("archive", ignoreCase = true) || text.contains("vintage", ignoreCase = true) ->
                "Archival documentary setting"
            else -> "Documentary storytelling setting"
        }
    }

    private fun inferMood(scene: StudioScene): String {
        return when (scene.effectPreset) {
            "Warm Sepia" -> "Warm archival nostalgia with golden-brown shadows"
            "Cinematic Vignette" -> "Intimate dramatic chiaroscuro documentary lighting"
            "Archival Dust" -> "Textured historical atmosphere with soft warm highlights"
            else -> "Cinematic documentary atmosphere with balanced depth and warm highlights"
        }
    }
}
