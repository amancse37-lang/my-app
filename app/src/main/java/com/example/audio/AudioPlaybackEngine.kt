package com.example.audio

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import java.io.File
import kotlin.math.abs

private const val PLAYBACK_TAG = "AudioPlaybackEngine"

/**
 * Real multi-track synchronized Android [MediaPlayer] mixing engine.
 *
 * Supports:
 * - Up to 4 simultaneous active audio tracks (e.g., Narration + Dialogue + Background Music + SFX/Ambient)
 * - Real-time per-clip volume and fade-in/fade-out gain envelopes (`setVolume`)
 * - Playhead seek synchronization with drift correction (> 250 ms)
 * - Seamless looping for music & ambient beds
 * - Standalone one-shot audition preview in the Audio Library / Studio
 * - Clean error isolation when a synced asset's local audio file is absent on another device
 */
class AudioPlaybackEngine {

    private data class ActiveTrackSlot(
        val clipId: String,
        val filePath: String,
        val player: MediaPlayer,
        var prepared: Boolean = false
    )

    private val activeSlots = mutableMapOf<String, ActiveTrackSlot>()
    private var previewPlayer: MediaPlayer? = null
    private var lastSyncedWasPlaying: Boolean = false

    private val audioAttrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
        .build()

    /**
     * Auditions a single audio file from the Audio Library or Inspector.
     */
    fun previewFile(
        localFilePath: String,
        volume: Float = 1.0f,
        onComplete: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        stopPreview()
        val file = File(localFilePath)
        if (localFilePath.isBlank() || !file.exists() || file.length() <= 0L) {
            onError("Local audio file is not available on this device. Re-import the audio file.")
            return
        }
        try {
            val mp = MediaPlayer().apply {
                setAudioAttributes(audioAttrs)
                setDataSource(file.absolutePath)
                val safeVol = volume.coerceIn(0f, 1f)
                setVolume(safeVol, safeVol)
                setOnCompletionListener {
                    stopPreview()
                    onComplete()
                }
                setOnErrorListener { _, what, extra ->
                    Log.w(PLAYBACK_TAG, "Preview MediaPlayer error what=$what extra=$extra")
                    stopPreview()
                    onError("Could not play audio file (codec error $what).")
                    true
                }
                prepare()
                start()
            }
            previewPlayer = mp
        } catch (e: Exception) {
            Log.w(PLAYBACK_TAG, "Audio preview failed", e)
            stopPreview()
            onError(e.localizedMessage ?: "Audio preview failed.")
        }
    }

    fun stopPreview() {
        previewPlayer?.let { mp ->
            runCatching { if (mp.isPlaying) mp.stop() }
            runCatching { mp.release() }
        }
        previewPlayer = null
    }

    /**
     * Synchronizes the multi-track MediaPlayer pool with the currently active timeline clips
     * at the current playhead timestamp. Called on playback ticks, seeks, pause, and mute toggles.
     */
    fun syncToPlayhead(activeClips: List<ActiveAudioClipWindow>, isPlaying: Boolean) {
        if (!isPlaying) {
            if (lastSyncedWasPlaying) {
                pauseAll()
            }
            lastSyncedWasPlaying = false
            return
        }
        lastSyncedWasPlaying = true
        stopPreview()

        val desiredIds = activeClips.map { it.clipId }.toSet()

        // Release slots no longer active at this playhead position
        val toRemove = activeSlots.keys.filter { it !in desiredIds }
        toRemove.forEach { clipId ->
            releaseSlot(clipId)
        }

        // Start, seek, and mix active clips
        activeClips.forEach { window ->
            val file = File(window.localFilePath)
            if (!file.exists() || file.length() <= 0L) return@forEach

            val existing = activeSlots[window.clipId]
            if (existing != null && existing.filePath == window.localFilePath) {
                updateSlot(existing, window)
            } else {
                if (existing != null) releaseSlot(window.clipId)
                createAndStartSlot(window)
            }
        }
    }

    private fun createAndStartSlot(window: ActiveAudioClipWindow) {
        try {
            val mp = MediaPlayer().apply {
                setAudioAttributes(audioAttrs)
                setDataSource(window.localFilePath)
                isLooping = window.loopEnabled
                setVolume(window.effectiveGain, window.effectiveGain)
                prepare()
                if (duration > 0) {
                    val safeSeek = window.sourceSeekMs.coerceIn(0, (duration - 50).coerceAtLeast(0))
                    seekTo(safeSeek)
                }
                start()
            }
            activeSlots[window.clipId] = ActiveTrackSlot(
                clipId = window.clipId,
                filePath = window.localFilePath,
                player = mp,
                prepared = true
            )
        } catch (e: Exception) {
            Log.w(PLAYBACK_TAG, "Failed to start track ${window.clipId}: ${e.message}")
        }
    }

    private fun updateSlot(slot: ActiveTrackSlot, window: ActiveAudioClipWindow) {
        val mp = slot.player
        try {
            mp.isLooping = window.loopEnabled
            mp.setVolume(window.effectiveGain, window.effectiveGain)
            val currentPos = mp.currentPosition
            val dur = mp.duration
            if (dur > 0) {
                val targetSeek = window.sourceSeekMs.coerceIn(0, (dur - 50).coerceAtLeast(0))
                if (abs(currentPos - targetSeek) > 280) {
                    mp.seekTo(targetSeek)
                }
            }
            if (!mp.isPlaying) {
                mp.start()
            }
        } catch (e: Exception) {
            Log.w(PLAYBACK_TAG, "Failed to sync track ${window.clipId}: ${e.message}")
            releaseSlot(window.clipId)
        }
    }

    fun pauseAll() {
        lastSyncedWasPlaying = false
        activeSlots.values.forEach { slot ->
            runCatching {
                if (slot.player.isPlaying) {
                    slot.player.pause()
                }
            }
        }
    }

    fun stopAll() {
        lastSyncedWasPlaying = false
        val ids = activeSlots.keys.toList()
        ids.forEach { releaseSlot(it) }
        stopPreview()
    }

    private fun releaseSlot(clipId: String) {
        activeSlots.remove(clipId)?.let { slot ->
            runCatching { if (slot.player.isPlaying) slot.player.stop() }
            runCatching { slot.player.release() }
        }
    }

    fun release() {
        stopAll()
    }
}
