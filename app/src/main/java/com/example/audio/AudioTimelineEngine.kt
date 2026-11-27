package com.example.audio

import com.example.data.SceneAudioClip
import com.example.data.StudioAudioAsset
import com.example.data.StudioScene
import com.example.data.TimelineTrackType
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Resolved playback instruction for a single active audio clip at a specific global timeline timestamp.
 */
data class ActiveAudioClipWindow(
    val clipId: String,
    val audioAssetId: String,
    val localFilePath: String,
    val trackType: TimelineTrackType,
    val title: String,
    val sourceSeekMs: Int,
    val effectiveGain: Float,
    val loopEnabled: Boolean
)

/**
 * Diagnostic report for narration-to-scene synchronization.
 */
data class SceneNarrationSyncStatus(
    val sceneId: String,
    val sceneTitle: String,
    val orderIndex: Int,
    val hasNarrationText: Boolean,
    val hasNarrationAudioClip: Boolean,
    val estimatedSpeechSec: Double,
    val sceneDurationSec: Double,
    val durationMismatchSec: Double,
    val warning: String?
)

data class NarrationSyncReport(
    val sceneStatuses: List<SceneNarrationSyncStatus>,
    val totalEstimatedSpeechSec: Double,
    val totalSceneDurationSec: Double,
    val missingNarrationCount: Int,
    val durationMismatchCount: Int
)

sealed interface AudioValidationResult {
    data object Valid : AudioValidationResult
    data class Invalid(val reason: String) : AudioValidationResult
}

/**
 * Pure deterministic multi-track audio math, narration duration estimation, trimming,
 * volume/fade envelope calculation, and import validation engine.
 */
object AudioTimelineEngine {

    const val MAX_AUDIO_FILE_BYTES: Long = 15L * 1024L * 1024L // 15 MB strict security cap
    const val MIN_CLIP_DURATION_SEC: Double = 0.25

    val ALLOWED_AUDIO_MIME_TYPES: Set<String> = setOf(
        "audio/mpeg",
        "audio/mp3",
        "audio/wav",
        "audio/x-wav",
        "audio/mp4",
        "audio/m4a",
        "audio/aac",
        "audio/ogg",
        "audio/webm",
        "audio/flac"
    )

    fun validateAudioImport(mimeType: String, fileSizeBytes: Long): AudioValidationResult {
        val normalized = mimeType.trim().lowercase()
        if (normalized !in ALLOWED_AUDIO_MIME_TYPES) {
            return AudioValidationResult.Invalid(
                "Unsupported audio format ($mimeType). Use MP3, WAV, M4A, AAC, OGG, FLAC, or WEBM."
            )
        }
        if (fileSizeBytes <= 0L) {
            return AudioValidationResult.Invalid("Audio file appears empty (0 bytes).")
        }
        if (fileSizeBytes > MAX_AUDIO_FILE_BYTES) {
            val mb = fileSizeBytes / (1024.0 * 1024.0)
            return AudioValidationResult.Invalid(
                "Audio file (${"%.1f".format(mb)} MB) exceeds the 15 MB studio limit."
            )
        }
        return AudioValidationResult.Valid
    }

    fun trackTypeForClip(trackTypeKey: String): TimelineTrackType {
        return when (trackTypeKey.trim().uppercase()) {
            "NARRATION" -> TimelineTrackType.NARRATION
            "DIALOGUE" -> TimelineTrackType.DIALOGUE
            "MUSIC" -> TimelineTrackType.MUSIC
            "SFX" -> TimelineTrackType.SOUND_EFFECTS
            "AMBIENT" -> TimelineTrackType.AMBIENT
            else -> TimelineTrackType.NARRATION
        }
    }

    fun trackTypeKeyForCategory(category: String): String {
        return when (category.trim()) {
            "Narration" -> "NARRATION"
            "Dialogue" -> "DIALOGUE"
            "Music" -> "MUSIC"
            "SFX" -> "SFX"
            "Ambient" -> "AMBIENT"
            else -> "NARRATION"
        }
    }

    /**
     * Returns the trimmed playable window duration of a single pass of [clip].
     */
    fun trimmedSourceDuration(clip: SceneAudioClip): Double {
        val src = clip.sourceDurationSec.takeIf { it.isFinite() && it > 0.0 } ?: 5.0
        val start = clip.trimStartSec.coerceIn(0.0, (src - MIN_CLIP_DURATION_SEC).coerceAtLeast(0.0))
        val rawEnd = if (clip.trimEndSec <= 0.0) src else clip.trimEndSec
        val end = rawEnd.coerceIn(start + MIN_CLIP_DURATION_SEC, max(src, start + MIN_CLIP_DURATION_SEC))
        return (end - start).coerceAtLeast(MIN_CLIP_DURATION_SEC)
    }

    /**
     * Returns how long [clip] occupies on its container timeline (respecting loop when enabled).
     */
    fun effectiveTimelineDuration(clip: SceneAudioClip, containerDurationSec: Double): Double {
        val base = trimmedSourceDuration(clip)
        if (!clip.loopEnabled) return base
        val remaining = (containerDurationSec - clip.startSec).coerceAtLeast(MIN_CLIP_DURATION_SEC)
        return max(base, remaining)
    }

    /**
     * Computes deterministic real-time linear gain [0f..1f] at [localContainerTimeSec],
     * factoring in mute, volume, fade-in ramp, and fade-out ramp.
     */
    fun computeClipGainAtTime(
        clip: SceneAudioClip,
        localContainerTimeSec: Double,
        containerDurationSec: Double
    ): Float {
        if (clip.muted || !localContainerTimeSec.isFinite()) return 0f
        val clipDuration = effectiveTimelineDuration(clip, containerDurationSec)
        val elapsed = localContainerTimeSec - clip.startSec
        if (elapsed < 0.0 || elapsed > clipDuration) return 0f

        val baseVolume = clip.volume.takeIf { it.isFinite() }?.coerceIn(0.0, 1.5) ?: 1.0
        if (baseVolume <= 0.0) return 0f

        val fadeIn = clip.fadeInSec.takeIf { it.isFinite() }?.coerceIn(0.0, clipDuration) ?: 0.0
        val fadeOut = clip.fadeOutSec.takeIf { it.isFinite() }?.coerceIn(0.0, clipDuration) ?: 0.0

        val fadeInFactor = if (fadeIn > 0.001 && elapsed < fadeIn) {
            (elapsed / fadeIn).coerceIn(0.0, 1.0)
        } else {
            1.0
        }

        val timeFromEnd = (clipDuration - elapsed).coerceAtLeast(0.0)
        val fadeOutFactor = if (fadeOut > 0.001 && timeFromEnd < fadeOut) {
            (timeFromEnd / fadeOut).coerceIn(0.0, 1.0)
        } else {
            1.0
        }

        // MediaPlayer.setVolume accepts [0.0f..1.0f]; clamp boost safely to 1.0f
        return (baseVolume * min(fadeInFactor, fadeOutFactor)).coerceIn(0.0, 1.0).toFloat()
    }

    /**
     * Computes the exact millisecond seek position inside the underlying audio source file
     * at [localContainerTimeSec], accounting for trimStartSec and loop modulo.
     */
    fun computeSourceSeekMs(clip: SceneAudioClip, localContainerTimeSec: Double): Int {
        val elapsed = (localContainerTimeSec - clip.startSec).coerceAtLeast(0.0)
        val trimStart = clip.trimStartSec.coerceAtLeast(0.0)
        val trimmedDur = trimmedSourceDuration(clip)
        val offsetInWindow = if (clip.loopEnabled && trimmedDur > 0.05) {
            elapsed % trimmedDur
        } else {
            elapsed.coerceAtMost(trimmedDur)
        }
        return ((trimStart + offsetInWindow) * 1000.0).toInt().coerceAtLeast(0)
    }

    /**
     * Resolves every currently audible audio clip across the active scene AND project-level tracks
     * at [globalPlayheadSec], respecting track mutes and asset file presence.
     */
    fun resolveActiveClipsAtPlayhead(
        scenes: List<StudioScene>,
        projectAudioClips: List<SceneAudioClip>,
        audioAssets: List<StudioAudioAsset>,
        globalPlayheadSec: Double,
        mutedTracks: Set<TimelineTrackType>,
        maxConcurrentTracks: Int = 4
    ): List<ActiveAudioClipWindow> {
        if (scenes.isEmpty() || !globalPlayheadSec.isFinite()) return emptyList()
        val assetById = audioAssets.associateBy { it.id }
        val totalProjectDuration = scenes.sumOf { it.durationSec }.coerceAtLeast(1.0)
        val results = mutableListOf<ActiveAudioClipWindow>()

        // 1. Find active scene and its local time
        var cursor = 0.0
        var activeScene: StudioScene? = null
        var sceneLocalTime = 0.0
        for (scene in scenes) {
            val end = cursor + scene.durationSec
            if (globalPlayheadSec <= end || scene == scenes.last()) {
                activeScene = scene
                sceneLocalTime = (globalPlayheadSec - cursor).coerceIn(0.0, scene.durationSec)
                break
            }
            cursor = end
        }

        // 2. Scene-scoped clips (Narration, Dialogue, SFX, Ambient, Scene Music)
        activeScene?.audioClips?.forEach { clip ->
            val track = trackTypeForClip(clip.trackType)
            if (track in mutedTracks) return@forEach
            val asset = assetById[clip.audioAssetId] ?: return@forEach
            if (asset.localFilePath.isBlank()) return@forEach

            val gain = computeClipGainAtTime(clip, sceneLocalTime, activeScene.durationSec)
            if (gain > 0.001f) {
                results.add(
                    ActiveAudioClipWindow(
                        clipId = clip.clipId,
                        audioAssetId = clip.audioAssetId,
                        localFilePath = asset.localFilePath,
                        trackType = track,
                        title = clip.title.ifBlank { asset.name },
                        sourceSeekMs = computeSourceSeekMs(clip, sceneLocalTime),
                        effectiveGain = gain,
                        loopEnabled = clip.loopEnabled
                    )
                )
            }
        }

        // 3. Project-scoped clips (e.g., continuous background score / ambient bed)
        projectAudioClips.forEach { clip ->
            val track = trackTypeForClip(clip.trackType)
            if (track in mutedTracks) return@forEach
            val asset = assetById[clip.audioAssetId] ?: return@forEach
            if (asset.localFilePath.isBlank()) return@forEach

            val gain = computeClipGainAtTime(clip, globalPlayheadSec, totalProjectDuration)
            if (gain > 0.001f) {
                results.add(
                    ActiveAudioClipWindow(
                        clipId = clip.clipId,
                        audioAssetId = clip.audioAssetId,
                        localFilePath = asset.localFilePath,
                        trackType = track,
                        title = clip.title.ifBlank { asset.name },
                        sourceSeekMs = computeSourceSeekMs(clip, globalPlayheadSec),
                        effectiveGain = gain,
                        loopEnabled = clip.loopEnabled
                    )
                )
            }
        }

        // Prioritize speech (Narration & Dialogue) first, then SFX, Ambient, Music up to hardware pool cap
        val priority = mapOf(
            TimelineTrackType.DIALOGUE to 0,
            TimelineTrackType.NARRATION to 1,
            TimelineTrackType.SOUND_EFFECTS to 2,
            TimelineTrackType.AMBIENT to 3,
            TimelineTrackType.MUSIC to 4
        )
        return results
            .sortedBy { priority[it.trackType] ?: 5 }
            .take(maxConcurrentTracks.coerceAtLeast(1))
    }

    /**
     * Deterministically moves a clip to [newStartSec] clamped within valid range.
     */
    fun moveClip(clip: SceneAudioClip, newStartSec: Double, maxContainerSec: Double): SceneAudioClip {
        val clamped = if (newStartSec.isFinite()) {
            newStartSec.coerceIn(0.0, maxContainerSec.coerceAtLeast(1.0))
        } else {
            0.0
        }
        return clip.copy(startSec = clamped)
    }

    /**
     * Deterministically trims a clip while guaranteeing trimEndSec >= trimStartSec + MIN_CLIP_DURATION_SEC.
     */
    fun trimClip(clip: SceneAudioClip, newTrimStartSec: Double, newTrimEndSec: Double): SceneAudioClip {
        val src = clip.sourceDurationSec.coerceAtLeast(MIN_CLIP_DURATION_SEC)
        val safeStart = newTrimStartSec.takeIf { it.isFinite() }
            ?.coerceIn(0.0, (src - MIN_CLIP_DURATION_SEC).coerceAtLeast(0.0)) ?: 0.0
        val safeEnd = newTrimEndSec.takeIf { it.isFinite() }
            ?.coerceIn(safeStart + MIN_CLIP_DURATION_SEC, src) ?: src
        return clip.copy(trimStartSec = safeStart, trimEndSec = safeEnd)
    }

    /**
     * Estimates documentary narration / dialogue speaking duration in seconds for English,
     * Hindi (Devanagari), or Bilingual scripts.
     */
    fun estimateSpeechDurationSec(
        text: String,
        language: String = "Bilingual",
        speechRate: Double = 1.0
    ): Double {
        val clean = text.trim()
        if (clean.isBlank()) return 0.0
        val safeRate = speechRate.takeIf { it.isFinite() }?.coerceIn(0.5, 2.0) ?: 1.0
        val words = clean.split(Regex("\\s+")).filter { it.isNotBlank() }
        val wordCount = words.size
        val devanagariChars = clean.count { it in '\u0900'..'\u097F' }
        val isHindiDominant = language.equals("Hindi", ignoreCase = true) || devanagariChars > clean.length / 4

        // Documentary pacing: English ~140 wpm (2.33 words/sec), Hindi ~125 wpm (2.08 words/sec)
        val baseWps = if (isHindiDominant) 2.08 else 2.33
        val punctuationPauses = clean.count { it == '.' || it == '।' || it == '?' || it == '!' } * 0.35 +
            clean.count { it == ',' || it == ';' || it == '—' } * 0.18
        val rawSec = (wordCount / baseWps + punctuationPauses) / safeRate
        return (ceil(rawSec * 10.0) / 10.0).coerceIn(1.0, 300.0)
    }

    /**
     * Analyzes narration coverage across all scenes, detecting missing narration text,
     * missing audio tracks, and timing mismatches between speech length and scene duration.
     */
    fun analyzeNarrationSync(scenes: List<StudioScene>, language: String): NarrationSyncReport {
        val statuses = scenes.mapIndexed { idx, scene ->
            val hasText = scene.narrationText.isNotBlank()
            val hasNarrationClip = scene.audioClips.any { it.trackType == "NARRATION" && it.audioAssetId.isNotBlank() }
            val estSpeech = if (hasText) estimateSpeechDurationSec(scene.narrationText, language) else 0.0
            val diff = estSpeech - scene.durationSec
            val warning = when {
                !hasText -> "No narration assigned"
                diff > 1.5 -> "Narration (~${"%.1f".format(estSpeech)}s) exceeds scene (${"%.1f".format(scene.durationSec)}s)"
                diff < -3.5 && scene.dialogueLines.isEmpty() -> "Scene is ${"%.1f".format(abs(diff))}s longer than narration"
                else -> null
            }
            SceneNarrationSyncStatus(
                sceneId = scene.id,
                sceneTitle = scene.title,
                orderIndex = idx,
                hasNarrationText = hasText,
                hasNarrationAudioClip = hasNarrationClip,
                estimatedSpeechSec = estSpeech,
                sceneDurationSec = scene.durationSec,
                durationMismatchSec = diff,
                warning = warning
            )
        }
        return NarrationSyncReport(
            sceneStatuses = statuses,
            totalEstimatedSpeechSec = statuses.sumOf { it.estimatedSpeechSec },
            totalSceneDurationSec = statuses.sumOf { it.sceneDurationSec },
            missingNarrationCount = statuses.count { !it.hasNarrationText },
            durationMismatchCount = statuses.count { it.hasNarrationText && abs(it.durationMismatchSec) > 1.5 }
        )
    }
}
