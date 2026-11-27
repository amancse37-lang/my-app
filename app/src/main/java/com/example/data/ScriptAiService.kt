package com.example.data

import android.util.Log
import com.example.audio.AudioTimelineEngine
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

private const val AI_TAG = "ScriptAiService"
private const val STUDIO_TEXT_MODEL = "gemini-3.8-flash"

data class IdentifiedCharacterPlan(
    val existingCharCode: String,
    val name: String,
    val role: String,
    val visualAge: String,
    val faceDescription: String,
    val hairStyle: String,
    val hairColor: String,
    val clothing: String,
    val clothingColors: String,
    val bodyProportions: String,
    val accessories: String,
    val personality: String,
    val defaultEmotion: String,
    val visualStyle: String,
    val consistencyNotes: String,
    val recurring: Boolean
)

data class SceneCastMemberPlan(
    val characterName: String,
    val existingCharCode: String,
    val expression: String,
    val action: String,
    val poseView: String,
    val stagePosition: String
)

data class GeneratedDialoguePlan(
    val speakerName: String,
    val existingCharCode: String = "",
    val text: String,
    val emotion: String = "Neutral",
    val startSec: Double = 0.0,
    val durationSec: Double = 3.0
)

data class GeneratedSoundSuggestionPlan(
    val category: String = "SFX",
    val title: String,
    val description: String = "",
    val suggestedStartSec: Double = 0.0,
    val suggestedVolume: Double = 0.65,
    val loopEnabled: Boolean = false
)

data class GeneratedScenePlan(
    val title: String,
    val description: String,
    val visualSummary: String,
    val narrationText: String,
    val subtitleText: String,
    val durationSec: Double,
    val backgroundPreset: String,
    val cameraMovement: String,
    val animationStyle: String,
    val effectPreset: String,
    val motionGraphicTitle: String,
    val motionGraphicStyle: String,
    val musicCue: String,
    val sfxCue: String,
    val objectProp: String,
    val suggestedCharacterCode: String,
    val cast: List<SceneCastMemberPlan>,
    val scenePlan: ScenePlanMetadata = ScenePlanMetadata(),
    val dialogueLines: List<GeneratedDialoguePlan> = emptyList(),
    val soundSuggestions: List<GeneratedSoundSuggestionPlan> = emptyList()
)

data class ScriptBreakdownResult(
    val identifiedCharacters: List<IdentifiedCharacterPlan>,
    val scenes: List<GeneratedScenePlan>,
    val storyAnalysis: StoryAnalysisSummary = StoryAnalysisSummary()
)

class ScriptAiService {

    private val sceneItemSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(description = "Short cinematic scene title (max 60 chars)"),
            "description" to Schema.string(description = "Director's scene action & storytelling description faithful to script"),
            "visualSummary" to Schema.string(description = "2D art direction & framing visual summary"),
            "narrationText" to Schema.string(description = "Voiceover narration segment in the original script language (Hindi or English)"),
            "subtitleText" to Schema.string(description = "Concise on-screen subtitle line"),
            "durationSec" to Schema.double(description = "Estimated scene duration in seconds (4.0 to 30.0)"),
            "backgroundPreset" to Schema.enumeration(
                values = listOf("himalayas", "old_delhi", "archive_study", "night_studio", "sepia_map"),
                description = "Best matching documentary environment preset"
            ),
            "cameraMovement" to Schema.enumeration(
                values = StudioPresets.cameraMovements,
                description = "2D camera movement choreography"
            ),
            "animationStyle" to Schema.enumeration(
                values = StudioPresets.animationStyles,
                description = "2D character animation style"
            ),
            "effectPreset" to Schema.enumeration(
                values = StudioPresets.effectPresets,
                description = "Cinematic color/atmosphere effect"
            ),
            "motionGraphicTitle" to Schema.string(description = "Short lower third, date stamp, or chapter title text (or empty string)"),
            "motionGraphicStyle" to Schema.enumeration(
                values = StudioPresets.motionGraphicStyles,
                description = "Motion graphic overlay template"
            ),
            "musicCue" to Schema.string(description = "Background documentary music cue title"),
            "sfxCue" to Schema.string(description = "Ambient or punctual sound effect cue"),
            "objectProp" to Schema.string(description = "Key foreground prop or visual symbol"),
            "suggestedCharacterCode" to Schema.string(description = "Primary character charCode if applicable"),
            "location" to Schema.string(description = "Specific scene location extracted from script"),
            "environment" to Schema.string(description = "Environment & setting details"),
            "timeOfDay" to Schema.string(description = "Time of day e.g. Dawn, Golden Hour, Twilight, Night"),
            "weather" to Schema.string(description = "Weather or indoor atmosphere"),
            "emotionalTone" to Schema.string(description = "Emotional tone of the scene"),
            "visualDirection" to Schema.string(description = "Actionable 2D staging, entrance/exit, and visual framing notes"),
            "cameraSuggestion" to Schema.string(description = "Suggested camera framing preset e.g. Wide Shot, Medium Shot, Close-Up, Golden Establishing"),
            "lightingSuggestion" to Schema.string(description = "Suggested lighting mood e.g. Natural Documentary, Golden Hour, Warm Indoor, Night Dramatic, Vintage Archival"),
            "atmosphereSuggestion" to Schema.string(description = "Suggested weather/atmosphere e.g. None, Fog / Mist, Rain, Snow, Floating Dust"),
            "vfxSuggestion" to Schema.string(description = "Suggested subtle VFX e.g. Subtle Film Grain, Warm Glow, Vignette"),
            "transitionSuggestion" to Schema.enumeration(
                values = StudioPresets.transitionSuggestions,
                description = "Transition into next scene"
            ),
            "suggestedProps" to Schema.array(
                description = "Up to 5 key props or visual objects mentioned in this scene",
                items = Schema.string()
            ),
            "cast" to Schema.array(
                description = "All characters present in this scene with context-aware emotion, action, view, and stage position.",
                items = Schema.obj(
                    properties = mapOf(
                        "characterName" to Schema.string(description = "Exact character name matching identifiedCharacters"),
                        "existingCharCode" to Schema.string(description = "Matching charCode if already in library, else empty string"),
                        "expression" to Schema.enumeration(
                            values = StudioPresets.characterEmotions,
                            description = "Scene-specific emotional expression"
                        ),
                        "action" to Schema.enumeration(
                            values = StudioPresets.characterActions,
                            description = "Scene-specific physical action based on script context"
                        ),
                        "poseView" to Schema.enumeration(
                            values = StudioPresets.characterViews,
                            description = "Camera framing/view for this character in the scene"
                        ),
                        "stagePosition" to Schema.enumeration(
                            values = listOf("Left", "Center", "Right"),
                            description = "Horizontal placement on the 16:9 stage"
                        )
                    ),
                    optionalProperties = listOf("existingCharCode")
                )
            ),
            "dialogueLines" to Schema.array(
                description = "Spoken dialogue lines in this scene mapped to character speakers (empty if pure voiceover).",
                items = Schema.obj(
                    properties = mapOf(
                        "speakerName" to Schema.string(description = "Speaker name matching identifiedCharacters"),
                        "existingCharCode" to Schema.string(description = "Matching charCode if known"),
                        "text" to Schema.string(description = "Spoken dialogue line in original script language"),
                        "emotion" to Schema.enumeration(
                            values = StudioPresets.characterEmotions,
                            description = "Delivery emotion"
                        ),
                        "startSec" to Schema.double(description = "Suggested start offset inside the scene in seconds"),
                        "durationSec" to Schema.double(description = "Estimated speaking duration in seconds")
                    ),
                    optionalProperties = listOf("existingCharCode", "startSec", "durationSec")
                )
            ),
            "soundSuggestions" to Schema.array(
                description = "2 to 4 editable music, SFX, or ambient sound suggestions matching this scene.",
                items = Schema.obj(
                    properties = mapOf(
                        "category" to Schema.enumeration(values = listOf("MUSIC", "SFX", "AMBIENT")),
                        "title" to Schema.string(description = "Short descriptive cue name"),
                        "description" to Schema.string(description = "How and when this sound supports the scene"),
                        "suggestedStartSec" to Schema.double(description = "Start time inside scene"),
                        "suggestedVolume" to Schema.double(description = "Suggested volume 0.1 to 1.0"),
                        "loopEnabled" to Schema.boolean(description = "Whether the cue should loop")
                    ),
                    optionalProperties = listOf("suggestedStartSec", "suggestedVolume", "loopEnabled")
                )
            )
        ),
        optionalProperties = listOf(
            "motionGraphicTitle", "objectProp", "suggestedCharacterCode",
            "location", "environment", "timeOfDay", "weather", "emotionalTone",
            "visualDirection", "cameraSuggestion", "lightingSuggestion", "atmosphereSuggestion",
            "vfxSuggestion", "transitionSuggestion", "suggestedProps", "dialogueLines", "soundSuggestions"
        )
    )

    private val sceneBreakdownSchema = Schema.obj(
        properties = mapOf(
            "storyAnalysis" to Schema.obj(
                description = "High-level documentary story intelligence summary extracted directly from the script without inventing unrelated plot.",
                properties = mapOf(
                    "detectedLanguage" to Schema.enumeration(values = StudioPresets.languages),
                    "storyLogline" to Schema.string(description = "1-2 sentence faithful summary of the documentary script"),
                    "eraOrPeriod" to Schema.string(description = "Historical or contemporary time period mentioned or implied"),
                    "emotionalArc" to Schema.string(description = "Emotional progression across the script"),
                    "locations" to Schema.array(items = Schema.string(), description = "Key locations in the script"),
                    "characterRelationships" to Schema.array(items = Schema.string(), description = "Key relationships between characters"),
                    "narrativeBeats" to Schema.array(items = Schema.string(), description = "Ordered story beats")
                ),
                optionalProperties = listOf("eraOrPeriod", "characterRelationships", "narrativeBeats")
            ),
            "identifiedCharacters" to Schema.array(
                description = "All recurring and non-recurring characters identified in the script. Deduplicate so each unique person appears only once.",
                items = Schema.obj(
                    properties = mapOf(
                        "existingCharCode" to Schema.string(description = "Matching charCode from AVAILABLE CHARACTER LIBRARY if this is an existing character, otherwise empty string"),
                        "name" to Schema.string(description = "Character full name or documentary designation"),
                        "role" to Schema.string(description = "Role in the documentary story"),
                        "visualAge" to Schema.string(description = "Visual age range e.g. Mid 40s, Late 60s, Young Adult"),
                        "faceDescription" to Schema.string(description = "Consistent facial features, skin tone, eyes, beard or eyebrows"),
                        "hairStyle" to Schema.string(description = "Consistent hairstyle"),
                        "hairColor" to Schema.string(description = "Consistent hair color"),
                        "clothing" to Schema.string(description = "Consistent documentary outfit description"),
                        "clothingColors" to Schema.string(description = "Consistent clothing color palette"),
                        "bodyProportions" to Schema.string(description = "Build, height impression, posture"),
                        "accessories" to Schema.string(description = "Glasses, scarf, satchel, notebook, jewelry, etc."),
                        "personality" to Schema.string(description = "Personality traits supported by the script"),
                        "defaultEmotion" to Schema.enumeration(
                            values = StudioPresets.characterEmotions,
                            description = "Character's default emotional state"
                        ),
                        "visualStyle" to Schema.enumeration(
                            values = StudioPresets.characterVisualStyles,
                            description = "Consistent 2D documentary illustration style"
                        ),
                        "consistencyNotes" to Schema.string(description = "Strict visual traits to keep identical across all scenes"),
                        "recurring" to Schema.boolean(description = "True if character appears in more than one scene or narrates throughout")
                    ),
                    optionalProperties = listOf("existingCharCode", "accessories", "personality")
                )
            ),
            "scenes" to Schema.array(
                description = "Ordered list of 2D animated documentary scenes parsed from the script.",
                items = sceneItemSchema
            )
        ),
        optionalProperties = listOf("storyAnalysis")
    )

    private val soundSuggestionsSchema = Schema.obj(
        properties = mapOf(
            "suggestions" to Schema.array(
                description = "3 to 5 practical, royalty-free-friendly sound design cue suggestions (music, SFX, ambient).",
                items = Schema.obj(
                    properties = mapOf(
                        "category" to Schema.enumeration(values = listOf("MUSIC", "SFX", "AMBIENT")),
                        "title" to Schema.string(),
                        "description" to Schema.string(),
                        "suggestedStartSec" to Schema.double(),
                        "suggestedVolume" to Schema.double(),
                        "loopEnabled" to Schema.boolean()
                    ),
                    optionalProperties = listOf("suggestedStartSec", "suggestedVolume", "loopEnabled")
                )
            )
        )
    )

    private val graphicSuggestionsSchema = Schema.obj(
        properties = mapOf(
            "suggestions" to Schema.array(
                description = "1 to 4 genuinely context-appropriate documentary motion graphic, lower third, infographic, or map suggestions for this scene (avoid unnecessary clutter).",
                items = Schema.obj(
                    properties = mapOf(
                        "category" to Schema.enumeration(values = listOf("TITLE", "LOWER_THIRD", "INFOGRAPHIC", "MAP", "TEXT")),
                        "templateType" to Schema.enumeration(
                            values = StudioPresets.documentaryGraphicTemplates.map { it.first } + listOf("INFOGRAPHIC", "MAP_GRAPHIC")
                        ),
                        "title" to Schema.string(description = "Short descriptive name for the suggested graphic"),
                        "rationale" to Schema.string(description = "Why this graphic genuinely enhances viewer comprehension for this scene"),
                        "primaryText" to Schema.string(description = "Suggested main headline, speaker name, or chart title"),
                        "secondaryText" to Schema.string(description = "Suggested subtitle, designation, location, or date caption"),
                        "suggestedStartSec" to Schema.double(description = "Suggested entrance time inside the scene"),
                        "suggestedDurationSec" to Schema.double(description = "Suggested on-screen duration in seconds"),
                        "infographicType" to Schema.string(description = "If INFOGRAPHIC: BAR_CHART, LINE_CHART, PIE_CHART, PERCENTAGE, STATISTIC, COUNTER, TIMELINE, COMPARISON, PROCESS, or FLOW; otherwise empty"),
                        "suggestedDataSummary" to Schema.string(description = "Optional label:value pairs e.g. '1840:120, 1860:245' or key map waypoints")
                    ),
                    optionalProperties = listOf("secondaryText", "suggestedStartSec", "suggestedDurationSec", "infographicType", "suggestedDataSummary")
                )
            )
        )
    )

    private val narrationRegenSchema = Schema.obj(
        properties = mapOf(
            "narrationText" to Schema.string(description = "Refined scene narration in the original script language"),
            "subtitleText" to Schema.string(description = "Concise on-screen subtitle"),
            "durationSec" to Schema.double(description = "Estimated speaking duration in seconds")
        )
    )

    private val visualDirectionSchema = Schema.obj(
        properties = mapOf(
            "location" to Schema.string(),
            "environment" to Schema.string(),
            "timeOfDay" to Schema.string(),
            "weather" to Schema.string(),
            "emotionalTone" to Schema.string(),
            "visualDirection" to Schema.string(),
            "cameraSuggestion" to Schema.string(),
            "lightingSuggestion" to Schema.string(),
            "atmosphereSuggestion" to Schema.string(),
            "vfxSuggestion" to Schema.string(),
            "transitionSuggestion" to Schema.enumeration(values = StudioPresets.transitionSuggestions),
            "suggestedProps" to Schema.array(items = Schema.string())
        ),
        optionalProperties = listOf("weather", "vfxSuggestion", "suggestedProps")
    )

    private val dialogueRegenSchema = Schema.obj(
        properties = mapOf(
            "dialogueLines" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "speakerName" to Schema.string(),
                        "existingCharCode" to Schema.string(),
                        "text" to Schema.string(),
                        "emotion" to Schema.enumeration(values = StudioPresets.characterEmotions),
                        "startSec" to Schema.double(),
                        "durationSec" to Schema.double()
                    ),
                    optionalProperties = listOf("existingCharCode", "startSec", "durationSec")
                )
            )
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = STUDIO_TEXT_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = sceneBreakdownSchema
            }
        )
    }

    private fun buildCharacterRoster(characters: List<StudioCharacter>): String {
        return if (characters.isEmpty()) {
            "No characters defined yet."
        } else {
            characters.joinToString("\n") { c ->
                "- ${c.charCode}: ${c.name} (${c.role}) | Age: ${c.visualAge} | Look: ${c.appearanceNotes}"
            }
        }
    }

    suspend fun generateScenesFromScript(
        scriptText: String,
        language: String,
        characters: List<StudioCharacter>
    ): Result<ScriptBreakdownResult> {
        val trimmedScript = scriptText.trim()
        if (trimmedScript.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter a Hindi or English documentary script first."))
        }
        if (trimmedScript.length < 10) {
            return Result.failure(IllegalArgumentException("Script is too short. Please enter at least one documentary scene or narration sentence."))
        }
        val boundedScript = trimmedScript.take(25_000)

        val prompt = """
            You are a senior 2D documentary film director, story analyst, character supervisor, and sound designer for MotionStory Studio.
            Analyze the documentary script ($language) carefully WITHOUT inventing unrelated plot or characters.
            Produce:
            1. `storyAnalysis`: Detected language, concise faithful logline, era/time period, emotional arc, locations, character relationships, and ordered narrative beats.
            2. `identifiedCharacters`: Every recurring and non-recurring character appearing or speaking in the script.
               - Reuse `existingCharCode` from AVAILABLE CHARACTER LIBRARY whenever a character matches an existing profile so no duplicates are created.
               - Preserve visual consistency details across scenes.
            3. `scenes`: 3 to 6 structured 2D animated documentary scenes faithfully segmenting the script's narration and dialogue.
               - Preserve the script's original language (Hindi Devanagari, English, or Bilingual) for `narrationText`, `subtitleText`, and `dialogueLines`.
               - Populate `location`, `environment`, `timeOfDay`, `weather`, `emotionalTone`, `visualDirection`, `cameraSuggestion`, `lightingSuggestion`, `atmosphereSuggestion`, `vfxSuggestion`, `transitionSuggestion`, `suggestedProps`, `cast`, `dialogueLines`, and `soundSuggestions`.
               - Keep `durationSec` aligned to speaking duration of narration + dialogue.

            AVAILABLE CHARACTER LIBRARY:
            ${buildCharacterRoster(characters)}

            DOCUMENTARY SCRIPT:
            $boundedScript
        """.trimIndent()

        return runAiCall("Scene breakdown") {
            val response = withTimeout(60_000L) { model.generateContent(prompt) }
            val jsonText = response.text
                ?: return@runAiCall Result.failure(IllegalStateException("Gemini returned an empty scene breakdown."))
            parseBreakdownJson(jsonText)
        }
    }

    suspend fun regenerateSingleScene(
        scriptText: String,
        language: String,
        existingScene: StudioScene,
        sceneIndex: Int,
        totalScenes: Int,
        characters: List<StudioCharacter>
    ): Result<GeneratedScenePlan> {
        val singleSceneModel = Firebase.ai.generativeModel(
            modelName = STUDIO_TEXT_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = sceneItemSchema
            }
        )
        val prompt = """
            Regenerate ONLY Scene ${sceneIndex + 1} of $totalScenes for a 2D documentary in $language.
            Keep story continuity with the script and reuse existing character codes where applicable.
            Current scene title: ${existingScene.title}
            Current description: ${existingScene.description}
            Current narration: ${existingScene.narrationText}

            AVAILABLE CHARACTER LIBRARY:
            ${buildCharacterRoster(characters)}

            FULL SCRIPT CONTEXT:
            ${scriptText.take(12_000)}
        """.trimIndent()

        return runAiCall("Single scene regeneration") {
            val response = withTimeout(50_000L) { singleSceneModel.generateContent(prompt) }
            val jsonText = response.text
                ?: return@runAiCall Result.failure(IllegalStateException("Gemini returned an empty scene plan."))
            val rootObj = parseJsonObject(jsonText)
            Result.success(parseSceneMap(rootObj, sceneIndex))
        }
    }

    suspend fun regenerateSceneNarration(
        scriptText: String,
        language: String,
        scene: StudioScene
    ): Result<Triple<String, String, Double>> {
        val narrationModel = Firebase.ai.generativeModel(
            modelName = STUDIO_TEXT_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = narrationRegenSchema
            }
        )
        val prompt = """
            Refine the documentary narration segment for this scene in $language without inventing unrelated facts.
            Preserve Hindi Devanagari if the script or scene is in Hindi.
            Scene Title: ${scene.title}
            Scene Description: ${scene.description}
            Current Narration: ${scene.narrationText}
            Script Context: ${scriptText.take(6_000)}
        """.trimIndent()

        return runAiCall("Narration regeneration") {
            val response = withTimeout(45_000L) { narrationModel.generateContent(prompt) }
            val obj = parseJsonObject(response.text ?: throw IllegalStateException("Empty narration response."))
            val narration = obj.jString("narrationText", scene.narrationText).trim().take(3000)
            val subtitle = obj.jString("subtitleText", narration.take(120)).trim().take(1000)
            val rawDur = obj.jDouble("durationSec", AudioTimelineEngine.estimateSpeechDurationSec(narration, language))
            val safeDur = if (rawDur.isNaN() || rawDur.isInfinite()) 7.0 else rawDur.coerceIn(3.0, 60.0)
            Result.success(Triple(narration, subtitle, safeDur))
        }
    }

    suspend fun regenerateSceneVisualDirection(
        scene: StudioScene,
        characters: List<StudioCharacter>
    ): Result<ScenePlanMetadata> {
        val dirModel = Firebase.ai.generativeModel(
            modelName = STUDIO_TEXT_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = visualDirectionSchema
            }
        )
        val prompt = """
            Provide actionable 2D documentary visual direction for this scene: location, environment,
            time of day, weather, emotional tone, character staging direction, camera framing preset,
            lighting mood, atmosphere weather, subtle VFX, transition, and up to 5 key props.
            Scene Title: ${scene.title}
            Description: ${scene.description}
            Narration: ${scene.narrationText}
            Characters: ${buildCharacterRoster(characters)}
        """.trimIndent()

        return runAiCall("Visual direction") {
            val response = withTimeout(45_000L) { dirModel.generateContent(prompt) }
            val obj = parseJsonObject(response.text ?: throw IllegalStateException("Empty visual direction response."))
            val props = obj.jStringList("suggestedProps").map { it.take(80) }
            Result.success(
                scene.scenePlan.copy(
                    location = obj.jString("location", scene.scenePlan.location).take(120),
                    environment = obj.jString("environment", scene.scenePlan.environment).take(240),
                    timeOfDay = obj.jString("timeOfDay", scene.scenePlan.timeOfDay).take(64),
                    weather = obj.jString("weather", scene.scenePlan.weather).take(64),
                    emotionalTone = obj.jString("emotionalTone", scene.scenePlan.emotionalTone).take(100),
                    visualDirection = obj.jString("visualDirection", scene.scenePlan.visualDirection).take(600),
                    cameraSuggestion = obj.jString("cameraSuggestion", scene.scenePlan.cameraSuggestion).take(120),
                    lightingSuggestion = obj.jString("lightingSuggestion", scene.scenePlan.lightingSuggestion).take(120),
                    atmosphereSuggestion = obj.jString("atmosphereSuggestion", scene.scenePlan.atmosphereSuggestion).take(120),
                    vfxSuggestion = obj.jString("vfxSuggestion", scene.scenePlan.vfxSuggestion).take(120),
                    transitionSuggestion = obj.jString("transitionSuggestion", "Cut")
                        .takeIf { it in StudioPresets.transitionSuggestions } ?: "Cut",
                    suggestedProps = props.take(8)
                )
            )
        }
    }

    suspend fun regenerateSceneDialogue(
        scriptText: String,
        language: String,
        scene: StudioScene,
        characters: List<StudioCharacter>
    ): Result<List<GeneratedDialoguePlan>> {
        val dlgModel = Firebase.ai.generativeModel(
            modelName = STUDIO_TEXT_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = dialogueRegenSchema
            }
        )
        val prompt = """
            Extract or refine spoken dialogue lines for this scene in $language mapped to existing characters.
            Stay faithful to the documentary script and fit within ${"%.1f".format(scene.durationSec)} seconds.
            Scene Title: ${scene.title}
            Description: ${scene.description}
            Narration: ${scene.narrationText}
            Characters: ${buildCharacterRoster(characters)}
            Script Context: ${scriptText.take(6_000)}
        """.trimIndent()

        return runAiCall("Dialogue regeneration") {
            val response = withTimeout(45_000L) { dlgModel.generateContent(prompt) }
            val root = parseJsonObject(response.text ?: throw IllegalStateException("Empty dialogue response."))
            Result.success(parseDialogueList(root.jList("dialogueLines")))
        }
    }

    suspend fun generateSceneSoundSuggestions(scene: StudioScene): Result<List<AiSoundSuggestion>> {
        val soundModel = Firebase.ai.generativeModel(
            modelName = STUDIO_TEXT_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = soundSuggestionsSchema
            }
        )
        val prompt = """
            Suggest 3 to 5 documentary sound design cues (MUSIC, SFX, AMBIENT) for this 2D scene.
            Do NOT reference copyrighted commercial songs — describe atmospheric textures, instruments,
            environmental sounds (e.g., rain, wind, footsteps, birds, room tone, market murmur, clock tick).
            Scene Title: ${scene.title}
            Location / Environment: ${scene.scenePlan.location} ${scene.scenePlan.environment}
            Description: ${scene.description}
            Narration: ${scene.narrationText}
            Scene Duration: ${"%.1f".format(scene.durationSec)} seconds
        """.trimIndent()

        return runAiCall("AI sound suggestions") {
            val response = withTimeout(45_000L) { soundModel.generateContent(prompt) }
            parseSoundSuggestionsJson(response.text ?: throw IllegalStateException("Empty sound suggestions response."))
        }
    }

    suspend fun suggestSceneGraphics(
        scene: StudioScene,
        scriptContext: String,
        characters: List<StudioCharacter>
    ): Result<List<AiGraphicSuggestion>> {
        val gfxModel = Firebase.ai.generativeModel(
            modelName = STUDIO_TEXT_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = graphicSuggestionsSchema
            }
        )
        val prompt = """
            You are a documentary motion graphics director for MotionStory Studio.
            Analyze this scene and suggest ONLY 1 to 4 genuinely helpful, non-cluttered documentary graphics
            (titles, lower thirds, date/location captions, charts, timelines, statistics, maps, quote cards, or comparisons)
            that directly clarify facts, speakers, dates, locations, or numbers in the scene.
            Do NOT spam unnecessary graphics. Preserve the original script language (Hindi or English) in primaryText/secondaryText.
            Scene Title: ${scene.title}
            Description: ${scene.description}
            Narration: ${scene.narrationText}
            Location: ${scene.scenePlan.location} | Time: ${scene.scenePlan.timeOfDay}
            Duration: ${"%.1f".format(scene.durationSec)}s
            Characters in project: ${buildCharacterRoster(characters)}
            Script Excerpt: ${scriptContext.take(3_000)}
        """.trimIndent()

        return runAiCall("AI graphic suggestions") {
            val response = withTimeout(45_000L) { gfxModel.generateContent(prompt) }
            parseGraphicSuggestionsJson(
                jsonText = response.text ?: throw IllegalStateException("Empty graphic suggestions response."),
                sceneDurationSec = scene.durationSec
            )
        }
    }

    private suspend fun <T> runAiCall(label: String, block: suspend () -> Result<T>): Result<T> {
        return try {
            block()
        } catch (e: TimeoutCancellationException) {
            Log.e(AI_TAG, "$label timed out", e)
            Result.failure(Exception("$label timed out. Please check your connection and try again."))
        } catch (e: QuotaExceededException) {
            Log.e(AI_TAG, "$label quota exceeded", e)
            Result.failure(Exception("Gemini API quota limit reached. Please wait a moment and try again."))
        } catch (e: JSONException) {
            Log.e(AI_TAG, "$label malformed JSON", e)
            Result.failure(Exception("AI returned an unexpected format during $label. Please retry."))
        } catch (e: IOException) {
            Log.e(AI_TAG, "$label network error", e)
            Result.failure(Exception("Network error while reaching Gemini AI. Check your internet connection and retry."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(AI_TAG, "$label failed", e)
            Result.failure(Exception(e.localizedMessage ?: "$label failed."))
        }
    }

    companion object {

        @Suppress("UNCHECKED_CAST")
        private fun Map<String, Any?>.jObj(key: String): Map<String, Any?>? = this[key] as? Map<String, Any?>
        private fun Map<String, Any?>.jList(key: String): List<Any?>? = this[key] as? List<*>
        private fun Map<String, Any?>.jString(key: String, default: String = ""): String =
            (this[key] as? String) ?: default
        private fun Map<String, Any?>.jDouble(key: String, default: Double): Double =
            (this[key] as? Number)?.toDouble() ?: default
        private fun Map<String, Any?>.jBool(key: String, default: Boolean): Boolean =
            (this[key] as? Boolean) ?: default
        private fun Map<String, Any?>.jStringList(key: String): List<String> =
            jList(key)?.mapNotNull { (it as? String)?.trim()?.takeIf { s -> s.isNotBlank() } }.orEmpty()

        @Suppress("UNCHECKED_CAST")
        internal fun parseJsonObject(raw: String): Map<String, Any?> {
            val parsed = PureJsonReader(raw).parseRoot()
            return parsed as? Map<String, Any?>
                ?: throw IllegalArgumentException("Expected JSON object at root.")
        }

        fun parseBreakdownJson(jsonText: String): Result<ScriptBreakdownResult> {
            return try {
                val root = parseJsonObject(jsonText)

                val analysisObj = root.jObj("storyAnalysis")
                val storyAnalysis = if (analysisObj != null) {
                    StoryAnalysisSummary(
                        detectedLanguage = analysisObj.jString("detectedLanguage", "Bilingual")
                            .takeIf { it in StudioPresets.languages } ?: "Bilingual",
                        storyLogline = analysisObj.jString("storyLogline", "").take(600),
                        eraOrPeriod = analysisObj.jString("eraOrPeriod", "").take(120),
                        emotionalArc = analysisObj.jString("emotionalArc", "").take(300),
                        locations = analysisObj.jStringList("locations").map { it.take(160) }.take(12),
                        characterRelationships = analysisObj.jStringList("characterRelationships").map { it.take(160) }.take(12),
                        narrativeBeats = analysisObj.jStringList("narrativeBeats").map { it.take(160) }.take(12),
                        approved = false
                    )
                } else {
                    StoryAnalysisSummary()
                }

                val charsArray = root.jList("identifiedCharacters").orEmpty()
                val identifiedChars = buildList {
                    charsArray.forEach { item ->
                        @Suppress("UNCHECKED_CAST")
                        val c = item as? Map<String, Any?> ?: return@forEach
                        val name = c.jString("name", "").trim()
                        if (name.isBlank()) return@forEach
                        add(
                            IdentifiedCharacterPlan(
                                existingCharCode = c.jString("existingCharCode", "").trim().take(32),
                                name = name.take(100),
                                role = c.jString("role", "Documentary Subject").take(120),
                                visualAge = c.jString("visualAge", "Adult").take(64),
                                faceDescription = c.jString("faceDescription", "").take(600),
                                hairStyle = c.jString("hairStyle", "").take(160),
                                hairColor = c.jString("hairColor", "").take(80),
                                clothing = c.jString("clothing", "").take(400),
                                clothingColors = c.jString("clothingColors", "").take(160),
                                bodyProportions = c.jString("bodyProportions", "Average build").take(240),
                                accessories = c.jString("accessories", "").take(300),
                                personality = c.jString("personality", "").take(400),
                                defaultEmotion = c.jString("defaultEmotion", "Calm")
                                    .takeIf { it in StudioPresets.characterEmotions } ?: "Calm",
                                visualStyle = c.jString("visualStyle", "Painterly 2D Documentary")
                                    .takeIf { it in StudioPresets.characterVisualStyles } ?: "Painterly 2D Documentary",
                                consistencyNotes = c.jString("consistencyNotes", "").take(1500),
                                recurring = c.jBool("recurring", true)
                            )
                        )
                    }
                }

                val scenesArray = root.jList("scenes")
                    ?: return Result.failure(IllegalStateException("AI response did not include any scenes."))

                val parsedScenes = buildList {
                    scenesArray.forEachIndexed { idx, item ->
                        @Suppress("UNCHECKED_CAST")
                        val obj = item as? Map<String, Any?> ?: return@forEachIndexed
                        add(parseSceneMap(obj, idx))
                    }
                }

                if (parsedScenes.isEmpty()) {
                    Result.failure(IllegalStateException("No scenes were generated from the provided script."))
                } else {
                    val totalDur = parsedScenes.sumOf { it.durationSec }
                    Result.success(
                        ScriptBreakdownResult(
                            identifiedCharacters = identifiedChars,
                            scenes = parsedScenes,
                            storyAnalysis = storyAnalysis.copy(estimatedTotalDurationSec = totalDur)
                        )
                    )
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        private fun parseSceneMap(obj: Map<String, Any?>, index: Int): GeneratedScenePlan {
            val validBgKeys = StudioPresets.backgrounds.map { it.first }.toSet()
            val validStagePositions = setOf("Left", "Center", "Right")

            val castList = buildList {
                obj.jList("cast").orEmpty().forEach { item ->
                    @Suppress("UNCHECKED_CAST")
                    val member = item as? Map<String, Any?> ?: return@forEach
                    val memberName = member.jString("characterName", "").trim()
                    if (memberName.isBlank()) return@forEach
                    add(
                        SceneCastMemberPlan(
                            characterName = memberName.take(100),
                            existingCharCode = member.jString("existingCharCode", "").trim().take(32),
                            expression = member.jString("expression", "Calm")
                                .takeIf { it in StudioPresets.characterEmotions } ?: "Calm",
                            action = member.jString("action", "Standing")
                                .takeIf { it in StudioPresets.characterActions } ?: "Standing",
                            poseView = member.jString("poseView", "Three-Quarter View")
                                .takeIf { it in StudioPresets.characterViews } ?: "Three-Quarter View",
                            stagePosition = member.jString("stagePosition", "Center")
                                .takeIf { it in validStagePositions } ?: "Center"
                        )
                    )
                }
            }

            val dialogueList = parseDialogueList(obj.jList("dialogueLines"))
            val soundList = parseSoundPlanList(obj.jList("soundSuggestions"))
            val suggestedProps = obj.jStringList("suggestedProps").map { it.take(80) }

            val rawDuration = obj.jDouble("durationSec", 7.0)
            val safeDuration = if (rawDuration.isNaN() || rawDuration.isInfinite()) 7.0 else rawDuration.coerceIn(3.0, 60.0)
            val musicCue = obj.jString("musicCue", "Ambient Documentary Bed").take(120)
            val sfxCue = obj.jString("sfxCue", "Room Tone").take(120)

            val planMeta = ScenePlanMetadata(
                location = obj.jString("location", "").take(120),
                environment = obj.jString("environment", "").take(240),
                timeOfDay = obj.jString("timeOfDay", "").take(64),
                weather = obj.jString("weather", "").take(64),
                emotionalTone = obj.jString("emotionalTone", "").take(100),
                visualDirection = obj.jString("visualDirection", obj.jString("visualSummary", "")).take(600),
                cameraSuggestion = obj.jString("cameraSuggestion", obj.jString("cameraMovement", "")).take(120),
                lightingSuggestion = obj.jString("lightingSuggestion", "").take(120),
                atmosphereSuggestion = obj.jString("atmosphereSuggestion", "").take(120),
                vfxSuggestion = obj.jString("vfxSuggestion", obj.jString("effectPreset", "")).take(120),
                transitionSuggestion = obj.jString("transitionSuggestion", "Cut")
                    .takeIf { it in StudioPresets.transitionSuggestions } ?: "Cut",
                audioCuesSummary = "$musicCue · $sfxCue".take(300),
                suggestedProps = suggestedProps.take(8),
                approved = false
            )

            return GeneratedScenePlan(
                title = obj.jString("title", "Scene ${index + 1}").trim().ifBlank { "Scene ${index + 1}" }.take(120),
                description = obj.jString("description", "").take(2000),
                visualSummary = obj.jString("visualSummary", "").take(1000),
                narrationText = obj.jString("narrationText", "").take(3000),
                subtitleText = obj.jString("subtitleText", "").take(1000),
                durationSec = safeDuration,
                backgroundPreset = obj.jString("backgroundPreset", "himalayas")
                    .takeIf { it in validBgKeys } ?: "himalayas",
                cameraMovement = obj.jString("cameraMovement", "Slow Push In")
                    .takeIf { it in StudioPresets.cameraMovements } ?: "Slow Push In",
                animationStyle = obj.jString("animationStyle", "Subtle Drift")
                    .takeIf { it in StudioPresets.animationStyles } ?: "Subtle Drift",
                effectPreset = obj.jString("effectPreset", "Film Grain")
                    .takeIf { it in StudioPresets.effectPresets } ?: "Film Grain",
                motionGraphicTitle = obj.jString("motionGraphicTitle", "").take(120),
                motionGraphicStyle = obj.jString("motionGraphicStyle", "Lower Third")
                    .takeIf { it in StudioPresets.motionGraphicStyles } ?: "Lower Third",
                musicCue = musicCue,
                sfxCue = sfxCue,
                objectProp = obj.jString("objectProp", suggestedProps.firstOrNull().orEmpty()).take(120),
                suggestedCharacterCode = obj.jString("suggestedCharacterCode", "").take(32),
                cast = castList,
                scenePlan = planMeta,
                dialogueLines = dialogueList,
                soundSuggestions = soundList
            )
        }

        private fun parseDialogueList(list: List<Any?>?): List<GeneratedDialoguePlan> {
            if (list == null) return emptyList()
            var cursor = 0.4
            return buildList {
                list.forEach { item ->
                    @Suppress("UNCHECKED_CAST")
                    val d = item as? Map<String, Any?> ?: return@forEach
                    val text = d.jString("text", "").trim()
                    if (text.isBlank()) return@forEach
                    val estDur = AudioTimelineEngine.estimateSpeechDurationSec(text)
                    val rawDur = d.jDouble("durationSec", estDur)
                    val dur = if (rawDur.isNaN() || rawDur.isInfinite() || rawDur < 0.4) estDur else rawDur.coerceIn(0.5, 60.0)
                    val rawStart = d.jDouble("startSec", cursor)
                    val start = if (rawStart.isNaN() || rawStart.isInfinite() || rawStart < 0.0) cursor else rawStart.coerceIn(0.0, 240.0)
                    cursor = start + dur + 0.35
                    add(
                        GeneratedDialoguePlan(
                            speakerName = d.jString("speakerName", "Speaker").trim().take(100),
                            existingCharCode = d.jString("existingCharCode", "").trim().take(32),
                            text = text.take(1200),
                            emotion = d.jString("emotion", "Neutral")
                                .takeIf { it in StudioPresets.characterEmotions } ?: "Neutral",
                            startSec = start,
                            durationSec = dur
                        )
                    )
                }
            }
        }

        private fun parseSoundPlanList(list: List<Any?>?): List<GeneratedSoundSuggestionPlan> {
            if (list == null) return emptyList()
            return buildList {
                list.forEach { item ->
                    @Suppress("UNCHECKED_CAST")
                    val s = item as? Map<String, Any?> ?: return@forEach
                    val title = s.jString("title", "").trim()
                    if (title.isBlank()) return@forEach
                    val cat = s.jString("category", "SFX").uppercase()
                        .takeIf { it in setOf("MUSIC", "SFX", "AMBIENT") } ?: "SFX"
                    val rawStart = s.jDouble("suggestedStartSec", 0.0)
                    val rawVol = s.jDouble("suggestedVolume", if (cat == "MUSIC") 0.35 else 0.7)
                    add(
                        GeneratedSoundSuggestionPlan(
                            category = cat,
                            title = title.take(100),
                            description = s.jString("description", "").take(300),
                            suggestedStartSec = if (rawStart.isNaN() || rawStart.isInfinite()) 0.0 else rawStart.coerceIn(0.0, 300.0),
                            suggestedVolume = if (rawVol.isNaN() || rawVol.isInfinite()) 0.65 else rawVol.coerceIn(0.05, 1.2),
                            loopEnabled = s.jBool("loopEnabled", cat != "SFX")
                        )
                    )
                }
            }
        }

        fun parseSoundSuggestionsJson(jsonText: String): Result<List<AiSoundSuggestion>> {
            return try {
                val root = parseJsonObject(jsonText)
                val plans = parseSoundPlanList(root.jList("suggestions"))
                val mapped = plans.mapIndexed { idx, p ->
                    AiSoundSuggestion(
                        suggestionId = "snd_${System.currentTimeMillis() % 100000}_$idx",
                        category = p.category,
                        title = p.title,
                        description = p.description,
                        suggestedStartSec = p.suggestedStartSec,
                        suggestedVolume = p.suggestedVolume,
                        loopEnabled = p.loopEnabled,
                        applied = false
                    )
                }
                if (mapped.isEmpty()) {
                    Result.failure(IllegalStateException("AI returned no sound cue suggestions."))
                } else {
                    Result.success(mapped)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        fun parseGraphicSuggestionsJson(
            jsonText: String,
            sceneDurationSec: Double = 8.0
        ): Result<List<AiGraphicSuggestion>> {
            val validCategories = setOf("TITLE", "LOWER_THIRD", "INFOGRAPHIC", "MAP", "TEXT")
            val validTemplates = StudioPresets.documentaryGraphicTemplates.map { it.first }.toSet() +
                setOf("INFOGRAPHIC", "MAP_GRAPHIC")
            val validInfoTypes = StudioPresets.infographicTypeOptions.map { it.first }.toSet()
            return try {
                val root = parseJsonObject(jsonText)
                val list = root.jList("suggestions").orEmpty()
                val mapped = buildList {
                    list.forEachIndexed { idx, item ->
                        @Suppress("UNCHECKED_CAST")
                        val g = item as? Map<String, Any?> ?: return@forEachIndexed
                        val title = g.jString("title", "").trim()
                        val primary = g.jString("primaryText", "").trim()
                        if (title.isBlank() && primary.isBlank()) return@forEachIndexed
                        val cat = g.jString("category", "TITLE").trim().uppercase()
                            .takeIf { it in validCategories } ?: "TITLE"
                        val tpl = g.jString("templateType", "CHAPTER_TITLE").trim().uppercase()
                            .takeIf { it in validTemplates }
                            ?: when (cat) {
                                "LOWER_THIRD" -> "LOWER_THIRD"
                                "INFOGRAPHIC" -> "INFOGRAPHIC"
                                "MAP" -> "MAP_GRAPHIC"
                                else -> "CHAPTER_TITLE"
                            }
                        val infoType = g.jString("infographicType", "").trim().uppercase()
                            .takeIf { it in validInfoTypes }.orEmpty()
                        val rawStart = g.jDouble("suggestedStartSec", 0.4)
                        val safeStart = if (rawStart.isNaN() || rawStart.isInfinite()) 0.4
                            else rawStart.coerceIn(0.0, (sceneDurationSec - 0.5).coerceAtLeast(0.0))
                        val rawDur = g.jDouble("suggestedDurationSec", 4.0)
                        val safeDur = if (rawDur.isNaN() || rawDur.isInfinite()) 4.0
                            else rawDur.coerceIn(1.0, sceneDurationSec.coerceAtLeast(2.0))
                        add(
                            AiGraphicSuggestion(
                                suggestionId = "gs_${System.currentTimeMillis() % 100000}_$idx",
                                category = cat,
                                templateType = tpl,
                                title = title.ifBlank { primary }.take(120),
                                rationale = g.jString("rationale", "").take(300),
                                primaryText = primary.ifBlank { title }.take(240),
                                secondaryText = g.jString("secondaryText", "").take(200),
                                suggestedStartSec = safeStart,
                                suggestedDurationSec = safeDur,
                                infographicType = infoType,
                                suggestedDataSummary = g.jString("suggestedDataSummary", "").take(300),
                                applied = false
                            )
                        )
                    }
                }
                if (mapped.isEmpty()) {
                    Result.failure(IllegalStateException("AI returned no context-relevant graphic suggestions."))
                } else {
                    Result.success(mapped)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}

/**
 * Lightweight, zero-dependency RFC-8259 JSON parser supporting full Unicode (including Hindi Devanagari)
 * so AI response parsing is 100% deterministic across Android runtime and pure JVM unit tests.
 */
private class PureJsonReader(private val input: String) {
    private var pos = 0

    fun parseRoot(): Any? {
        skipWhitespace()
        val value = parseValue()
        skipWhitespace()
        return value
    }

    private fun parseValue(): Any? {
        skipWhitespace()
        if (pos >= input.length) throw IllegalArgumentException("Unexpected end of JSON")
        return when (input[pos]) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> parseString()
            't' -> { expectLiteral("true"); true }
            'f' -> { expectLiteral("false"); false }
            'n' -> { expectLiteral("null"); null }
            '-', in '0'..'9' -> parseNumber()
            else -> throw IllegalArgumentException("Unexpected char '${input[pos]}' at index $pos")
        }
    }

    private fun parseObject(): Map<String, Any?> {
        pos++ // '{'
        val map = LinkedHashMap<String, Any?>()
        skipWhitespace()
        if (pos < input.length && input[pos] == '}') {
            pos++
            return map
        }
        while (pos < input.length) {
            skipWhitespace()
            val key = parseString()
            skipWhitespace()
            if (pos >= input.length || input[pos] != ':') {
                throw IllegalArgumentException("Expected ':' after key '$key' at index $pos")
            }
            pos++
            val value = parseValue()
            map[key] = value
            skipWhitespace()
            if (pos < input.length && input[pos] == ',') {
                pos++
            } else if (pos < input.length && input[pos] == '}') {
                pos++
                return map
            } else {
                throw IllegalArgumentException("Expected ',' or '}' at index $pos")
            }
        }
        throw IllegalArgumentException("Unterminated JSON object")
    }

    private fun parseArray(): List<Any?> {
        pos++ // '['
        val list = mutableListOf<Any?>()
        skipWhitespace()
        if (pos < input.length && input[pos] == ']') {
            pos++
            return list
        }
        while (pos < input.length) {
            list.add(parseValue())
            skipWhitespace()
            if (pos < input.length && input[pos] == ',') {
                pos++
            } else if (pos < input.length && input[pos] == ']') {
                pos++
                return list
            } else {
                throw IllegalArgumentException("Expected ',' or ']' at index $pos")
            }
        }
        throw IllegalArgumentException("Unterminated JSON array")
    }

    private fun parseString(): String {
        if (pos >= input.length || input[pos] != '"') {
            throw IllegalArgumentException("Expected '\"' at index $pos")
        }
        pos++
        val sb = StringBuilder()
        while (pos < input.length) {
            val ch = input[pos++]
            when (ch) {
                '"' -> return sb.toString()
                '\\' -> {
                    if (pos >= input.length) throw IllegalArgumentException("Unterminated escape")
                    when (val esc = input[pos++]) {
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        '/' -> sb.append('/')
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            val hex = input.substring(pos, (pos + 4).coerceAtMost(input.length))
                            sb.append(hex.toInt(16).toChar())
                            pos += 4
                        }
                        else -> sb.append(esc)
                    }
                }
                else -> sb.append(ch)
            }
        }
        throw IllegalArgumentException("Unterminated JSON string")
    }

    private fun parseNumber(): Number {
        val start = pos
        if (input[pos] == '-') pos++
        while (pos < input.length && input[pos].isDigit()) pos++
        var isFloat = false
        if (pos < input.length && input[pos] == '.') {
            isFloat = true
            pos++
            while (pos < input.length && input[pos].isDigit()) pos++
        }
        if (pos < input.length && (input[pos] == 'e' || input[pos] == 'E')) {
            isFloat = true
            pos++
            if (pos < input.length && (input[pos] == '+' || input[pos] == '-')) pos++
            while (pos < input.length && input[pos].isDigit()) pos++
        }
        val token = input.substring(start, pos)
        return if (isFloat) token.toDouble() else token.toLongOrNull() ?: token.toDouble()
    }

    private fun expectLiteral(lit: String) {
        if (!input.startsWith(lit, pos)) {
            throw IllegalArgumentException("Expected '$lit' at index $pos")
        }
        pos += lit.length
    }

    private fun skipWhitespace() {
        while (pos < input.length && input[pos].isWhitespace()) pos++
    }
}
