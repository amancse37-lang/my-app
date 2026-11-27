package com.example.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.data.TtsVoiceOption
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume

private const val TTS_TAG = "StudioTtsEngine"

/**
 * Real Android on-device Text-to-Speech synthesis engine supporting Hindi (`hi-IN`) and
 * English (`en-US` / `en-IN`). Enumerates actual installed system voices, supports audible voice
 * previews, and synthesizes narration or character dialogue directly into persistent WAV files
 * via [TextToSpeech.synthesizeToFile] so the audio plays in the synchronized multitrack mixer.
 */
class StudioTtsEngine(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var initDeferred: CompletableDeferred<Boolean>? = null
    private val pendingSyntheses = mutableMapOf<String, CompletableDeferred<Result<File>>>()
    private val pendingFiles = mutableMapOf<String, File>()

    private suspend fun ensureInitialized(): Boolean = withContext(Dispatchers.Main) {
        val existing = initDeferred
        if (existing != null && tts != null) {
            return@withContext existing.await()
        }
        val deferred = CompletableDeferred<Boolean>()
        initDeferred = deferred
        try {
            val engine = TextToSpeech(context.applicationContext) { status ->
                val ok = status == TextToSpeech.SUCCESS
                if (!ok) {
                    Log.w(TTS_TAG, "Android TextToSpeech initialization failed with status=$status")
                }
                deferred.complete(ok)
            }
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    val id = utteranceId ?: return
                    val outFile = pendingFiles.remove(id)
                    val waiter = pendingSyntheses.remove(id)
                    if (waiter != null && outFile != null) {
                        if (outFile.exists() && outFile.length() > 44L) {
                            waiter.complete(Result.success(outFile))
                        } else {
                            waiter.complete(Result.failure(IllegalStateException("TTS generated an empty audio file.")))
                        }
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    val id = utteranceId ?: return
                    pendingFiles.remove(id)
                    pendingSyntheses.remove(id)?.complete(
                        Result.failure(IllegalStateException("Android TTS synthesis failed on this device."))
                    )
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    val id = utteranceId ?: return
                    pendingFiles.remove(id)
                    pendingSyntheses.remove(id)?.complete(
                        Result.failure(IllegalStateException("Android TTS synthesis failed (error code $errorCode)."))
                    )
                }
            })
            tts = engine
        } catch (e: Exception) {
            Log.w(TTS_TAG, "Failed to construct TextToSpeech", e)
            deferred.complete(false)
        }
        try {
            withTimeout(6_000L) { deferred.await() }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Lists genuine installed voices on the user's Android device filtered to Hindi and English.
     * Never invents fake voice names.
     */
    suspend fun listAvailableVoices(): List<TtsVoiceOption> {
        if (!ensureInitialized()) return emptyList()
        val engine = tts ?: return emptyList()
        return withContext(Dispatchers.Main) {
            runCatching {
                val installed = engine.voices?.toList().orEmpty()
                val relevant = installed.filter { v ->
                    val lang = v.locale?.language?.lowercase().orEmpty()
                    lang == "en" || lang == "hi"
                }
                val list = relevant.sortedWith(
                    compareBy<android.speech.tts.Voice> { it.locale?.language != "en" }
                        .thenBy { it.isNetworkConnectionRequired }
                        .thenBy { it.name }
                ).take(24).map { v ->
                    val locale = v.locale ?: Locale.US
                    val langTag = locale.toLanguageTag()
                    val humanLocale = locale.displayName.ifBlank { langTag }
                    val shortTag = v.name.substringAfterLast('-').uppercase().take(8)
                    val networkSuffix = if (v.isNetworkConnectionRequired) " (Net)" else " (Local)"
                    TtsVoiceOption(
                        voiceId = v.name,
                        displayName = "${locale.displayLanguage} · $shortTag$networkSuffix",
                        languageTag = langTag,
                        localeLabel = humanLocale,
                        requiresNetwork = v.isNetworkConnectionRequired
                    )
                }
                if (list.isNotEmpty()) {
                    list
                } else {
                    // Fallback to system default English / Hindi locale entries if device omits voice enumeration
                    listOf(
                        TtsVoiceOption("default_en_US", "System Default English (US)", "en-US", "English (US)"),
                        TtsVoiceOption("default_hi_IN", "System Default Hindi (IN)", "hi-IN", "Hindi (India)")
                    )
                }
            }.getOrDefault(emptyList())
        }
    }

    /**
     * Plays a short live speech preview directly through the device TTS engine.
     */
    suspend fun previewVoice(
        sampleText: String,
        language: String,
        voiceId: String,
        pitch: Float = 1.0f,
        rate: Float = 1.0f
    ): Result<Unit> {
        if (!ensureInitialized()) {
            return Result.failure(IllegalStateException("Device Text-to-Speech engine is unavailable."))
        }
        val engine = tts ?: return Result.failure(IllegalStateException("TTS engine not ready."))
        return withContext(Dispatchers.Main) {
            try {
                configureEngine(engine, language, voiceId, pitch, rate)
                val utteranceId = "prev_${UUID.randomUUID().toString().take(8)}"
                val text = sampleText.trim().take(240).ifBlank {
                    if (language.equals("Hindi", ignoreCase = true)) {
                        "नमस्ते, मोशन स्टोरी स्टूडियो में आपका स्वागत है।"
                    } else {
                        "Welcome to MotionStory Studio documentary voice preview."
                    }
                }
                val code = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                if (code == TextToSpeech.SUCCESS) {
                    Result.success(Unit)
                } else {
                    Result.failure(IllegalStateException("Voice preview failed to start."))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    fun stopPreview() {
        runCatching { tts?.stop() }
    }

    /**
     * Synthesizes [text] into a genuine persistent WAV file at [outputFile] using Android's
     * [TextToSpeech.synthesizeToFile].
     */
    suspend fun synthesizeToWavFile(
        text: String,
        language: String,
        voiceId: String,
        pitch: Float = 1.0f,
        rate: Float = 1.0f,
        outputFile: File
    ): Result<File> {
        val cleanText = text.trim().take(2800)
        if (cleanText.isBlank()) {
            return Result.failure(IllegalArgumentException("Enter narration or dialogue text before generating speech."))
        }
        if (!ensureInitialized()) {
            return Result.failure(
                IllegalStateException(
                    "Android Text-to-Speech is unavailable on this device. Install Google Speech Services or import an external WAV/MP3 voiceover."
                )
            )
        }
        val engine = tts ?: return Result.failure(IllegalStateException("TTS engine not initialized."))
        val utteranceId = "syn_${UUID.randomUUID().toString().take(12)}"
        val completion = CompletableDeferred<Result<File>>()
        pendingFiles[utteranceId] = outputFile
        pendingSyntheses[utteranceId] = completion

        val started = withContext(Dispatchers.Main) {
            try {
                configureEngine(engine, language, voiceId, pitch, rate)
                val params = Bundle()
                engine.synthesizeToFile(cleanText, params, outputFile, utteranceId) == TextToSpeech.SUCCESS
            } catch (e: Exception) {
                Log.w(TTS_TAG, "synthesizeToFile threw", e)
                false
            }
        }

        if (!started) {
            pendingFiles.remove(utteranceId)
            pendingSyntheses.remove(utteranceId)
            return Result.failure(
                IllegalStateException("Could not synthesize speech for $language. Check installed TTS language packs or import audio.")
            )
        }

        return try {
            withTimeout(25_000L) {
                suspendCancellableCoroutine { cont ->
                    cont.invokeOnCancellation {
                        pendingFiles.remove(utteranceId)
                        pendingSyntheses.remove(utteranceId)
                        runCatching { engine.stop() }
                    }
                    completion.invokeOnCompletion {
                        if (cont.isActive) {
                            val res = runCatching { completion.getCompleted() }
                                .getOrElse { Result.failure(it) }
                            cont.resume(res)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            pendingFiles.remove(utteranceId)
            pendingSyntheses.remove(utteranceId)
            Result.failure(IllegalStateException("Speech synthesis timed out after 25s. Try shorter text or import external audio."))
        }
    }

    private fun configureEngine(
        engine: TextToSpeech,
        language: String,
        voiceId: String,
        pitch: Float,
        rate: Float
    ) {
        val targetLocale = when {
            language.equals("Hindi", ignoreCase = true) -> Locale("hi", "IN")
            else -> Locale.US
        }
        val matchedVoice = if (voiceId.isNotBlank() && !voiceId.startsWith("default_")) {
            runCatching { engine.voices?.firstOrNull { it.name == voiceId } }.getOrNull()
        } else {
            null
        }
        if (matchedVoice != null) {
            engine.voice = matchedVoice
        } else {
            val res = engine.setLanguage(targetLocale)
            if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                engine.setLanguage(Locale.US)
            }
        }
        engine.setPitch(pitch.coerceIn(0.5f, 2.0f))
        engine.setSpeechRate(rate.coerceIn(0.5f, 2.0f))
    }

    fun release() {
        pendingFiles.clear()
        pendingSyntheses.values.forEach {
            it.complete(Result.failure(IllegalStateException("TTS engine released.")))
        }
        pendingSyntheses.clear()
        runCatching {
            tts?.stop()
            tts?.shutdown()
        }
        tts = null
        initDeferred = null
    }
}
