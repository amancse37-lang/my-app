package com.example.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.util.LruCache
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.math.min

private const val CHAR_IMG_TAG = "CharacterImageService"

object CharacterBitmapCodec {
    private val bitmapCache = object : LruCache<String, Bitmap>(24) {}

    fun encodeBitmapToBase64Jpeg(
        bitmap: Bitmap,
        maxDimension: Int = 512,
        quality: Int = 74
    ): String {
        val scale = min(
            1f,
            min(
                maxDimension.toFloat() / bitmap.width.coerceAtLeast(1),
                maxDimension.toFloat() / bitmap.height.coerceAtLeast(1)
            )
        )
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt().coerceAtLeast(64),
                (bitmap.height * scale).toInt().coerceAtLeast(64),
                true
            )
        } else {
            bitmap
        }

        val stream = ByteArrayOutputStream()
        var currentQuality = quality.coerceIn(30, 90)
        var bytes: ByteArray
        do {
            stream.reset()
            scaled.compress(Bitmap.CompressFormat.JPEG, currentQuality, stream)
            bytes = stream.toByteArray()
            currentQuality -= 15
        } while (bytes.size > 190_000 && currentQuality >= 25)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /**
     * Inspects actual bitmap alpha values rather than assuming PNG transparency.
     * Returns true only when the bitmap has an alpha channel AND contains pixels
     * with meaningful translucency/transparency (alpha < 250).
     */
    fun hasActualTransparency(bitmap: Bitmap): Boolean {
        if (!bitmap.hasAlpha()) return false
        val w = bitmap.width
        val h = bitmap.height
        if (w <= 0 || h <= 0) return false
        val stepX = (w / 24).coerceAtLeast(1)
        val stepY = (h / 24).coerceAtLeast(1)
        var y = 0
        while (y < h) {
            var x = 0
            while (x < w) {
                val alpha = (bitmap.getPixel(x, y) ushr 24) and 0xFF
                if (alpha < 250) return true
                x += stepX
            }
            y += stepY
        }
        return false
    }

    /**
     * Encodes bitmaps that genuinely have transparent pixels as lossless PNG when
     * within the Firestore payload ceiling, falling back to compressed JPEG for
     * opaque images or oversized bitmaps.
     */
    fun encodeBitmapPreservingAlpha(
        bitmap: Bitmap,
        maxDimension: Int = 512,
        jpegQuality: Int = 74
    ): String {
        if (hasActualTransparency(bitmap)) {
            val scale = min(
                1f,
                min(
                    maxDimension.toFloat() / bitmap.width.coerceAtLeast(1),
                    maxDimension.toFloat() / bitmap.height.coerceAtLeast(1)
                )
            )
            val scaled = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scale).toInt().coerceAtLeast(64),
                    (bitmap.height * scale).toInt().coerceAtLeast(64),
                    true
                )
            } else {
                bitmap
            }
            val pngStream = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.PNG, 100, pngStream)
            val pngBytes = pngStream.toByteArray()
            if (pngBytes.size <= 190_000) {
                return Base64.encodeToString(pngBytes, Base64.NO_WRAP)
            }
        }
        return encodeBitmapToBase64Jpeg(bitmap, maxDimension, jpegQuality)
    }

    fun decodeBase64ToBitmap(base64: String): Bitmap? {
        if (base64.isBlank()) return null
        val cacheKey = "${base64.length}_${base64.take(24)}_${base64.takeLast(16)}"
        bitmapCache.get(cacheKey)?.let { return it }
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bmp != null) {
                bitmapCache.put(cacheKey, bmp)
            }
            bmp
        } catch (e: Exception) {
            Log.w(CHAR_IMG_TAG, "Failed to decode character bitmap", e)
            null
        }
    }
}

data class GeneratedCharacterDesign(
    val bitmap: Bitmap,
    val base64Jpeg: String,
    val promptSummary: String
)

class CharacterImageService {

    private val imageModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
            }
        )
    }

    fun buildCharacterDesignPrompt(
        character: StudioCharacter,
        viewType: String,
        expression: String,
        poseAction: String,
        visualStyle: String,
        hasAnchorReference: Boolean
    ): String {
        val styleLock = visualStyle.ifBlank { character.visualStyle.ifBlank { "Painterly 2D Documentary" } }
        val clothingColors = character.clothingColors.ifBlank { character.wardrobePalette }
        val anchorDirective = if (hasAnchorReference) {
            "CRITICAL IDENTITY LOCK: Use the attached character reference image as the exact visual identity source. Keep the IDENTICAL face structure, age, skin tone, hairstyle, hair color, clothing design, clothing colors, body proportions, and accessories. ONLY change the camera angle/view, facial expression, and body pose as requested below."
        } else {
            "Create a canonical 2D animated documentary character design reference sheet image that establishes a clear, memorable, repeatable visual identity for animation production."
        }

        return buildString {
            appendLine(anchorDirective)
            appendLine()
            appendLine("CHARACTER IDENTITY BLUEPRINT (${character.charCode}):")
            appendLine("- Name & Role: ${character.name} (${character.role.ifBlank { "Documentary Character" }})")
            if (character.visualAge.isNotBlank()) appendLine("- Visual Age: ${character.visualAge}")
            if (character.faceDescription.isNotBlank()) appendLine("- Face Description: ${character.faceDescription}")
            if (character.hairStyle.isNotBlank() || character.hairColor.isNotBlank()) {
                appendLine("- Hair: ${character.hairStyle} (${character.hairColor})")
            }
            if (character.clothing.isNotBlank()) appendLine("- Clothing: ${character.clothing}")
            if (clothingColors.isNotBlank()) appendLine("- Clothing Color Palette: $clothingColors")
            if (character.bodyProportions.isNotBlank()) appendLine("- Body Proportions: ${character.bodyProportions}")
            if (character.accessories.isNotBlank()) appendLine("- Accessories: ${character.accessories}")
            if (character.personality.isNotBlank()) appendLine("- Personality Demeanor: ${character.personality}")
            if (character.appearanceNotes.isNotBlank()) {
                appendLine("- Strict Consistency Notes: ${character.appearanceNotes}")
            }
            appendLine()
            appendLine("REQUESTED SHOT SPECIFICATION:")
            appendLine("- View & Framing: $viewType")
            appendLine("- Facial Expression: $expression")
            appendLine("- Body Pose / Action: $poseAction")
            appendLine("- Visual Art Style: $styleLock, clean 2D documentary animation production art")
            appendLine("- Studio Backdrop: Clean, neutral deep slate charcoal studio backdrop (#181B23) with soft warm documentary rim light, centered subject, NO text, NO labels, NO watermarks, single character only.")
        }
    }

    suspend fun generateCharacterReference(
        character: StudioCharacter,
        viewType: String,
        expression: String,
        poseAction: String,
        visualStyle: String,
        useAnchorReference: Boolean = true
    ): Result<GeneratedCharacterDesign> {
        if (character.name.isBlank()) {
            return Result.failure(IllegalArgumentException("Character name must not be blank before generating a reference image."))
        }
        val anchorBitmap = if (useAnchorReference && character.referenceImageBase64.isNotBlank()) {
            withContext(Dispatchers.Default) {
                CharacterBitmapCodec.decodeBase64ToBitmap(character.referenceImageBase64)
            }
        } else {
            null
        }

        val prompt = buildCharacterDesignPrompt(
            character = character,
            viewType = viewType,
            expression = expression,
            poseAction = poseAction,
            visualStyle = visualStyle,
            hasAnchorReference = anchorBitmap != null
        )

        val summary = "${character.charCode} ${character.name} · $viewType · $expression · $poseAction"

        return try {
            val bitmap = withTimeout(60_000L) {
                val response = if (anchorBitmap != null) {
                    imageModel.generateContent(
                        content {
                            image(anchorBitmap)
                            text(prompt)
                        }
                    )
                } else {
                    imageModel.generateContent(prompt)
                }
                response.candidates.firstOrNull()
                    ?.content?.parts?.filterIsInstance<ImagePart>()
                    ?.firstOrNull()
                    ?.image
            }

            if (bitmap == null) {
                return Result.failure(
                    IllegalStateException("Image model did not return a character illustration. Verify Firebase Blaze billing and try again.")
                )
            }

            val encoded = withContext(Dispatchers.Default) {
                CharacterBitmapCodec.encodeBitmapPreservingAlpha(bitmap)
            }

            Result.success(
                GeneratedCharacterDesign(
                    bitmap = bitmap,
                    base64Jpeg = encoded,
                    promptSummary = summary.take(1000)
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(CHAR_IMG_TAG, "Character image generation timed out", e)
            Result.failure(Exception("Character image generation timed out after 60 seconds. Please retry."))
        } catch (e: QuotaExceededException) {
            Log.e(CHAR_IMG_TAG, "Image generation quota exceeded", e)
            Result.failure(Exception("Image generation quota exceeded. Check your Firebase Blaze plan or try again later."))
        } catch (e: IOException) {
            Log.e(CHAR_IMG_TAG, "Network error during character image generation", e)
            Result.failure(Exception("Network error while generating character illustration. Please check your internet connection and retry."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(CHAR_IMG_TAG, "Character image generation failed", e)
            Result.failure(
                Exception(
                    e.localizedMessage
                        ?: "Character image generation failed. Ensure Firebase AI Logic image generation (Blaze plan) is enabled."
                )
            )
        }
    }
}
