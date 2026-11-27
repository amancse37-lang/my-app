package com.example.graphics

import com.example.audio.AudioTimelineEngine
import com.example.data.StudioAudioAsset
import com.example.data.StudioScene
import com.example.data.SubtitleCue
import com.example.data.SubtitleStyleConfig
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Diagnostic summary of subtitle timing health within a scene.
 */
data class SubtitleDiagnostics(
    val overlapCount: Int,
    val outOfBoundsCount: Int,
    val usedRealAudioTiming: Boolean,
    val summary: String
)

/**
 * Pure, deterministic Phase 6 Subtitle & Caption Engine:
 * - Sentence & phrase segmentation (English + Hindi Devanagari `।` punctuation)
 * - Audio-aware cue timing from real Phase 5 audio clips (honestly distinguishing audio vs estimated duration)
 * - Overlap diagnostics & non-destructive fitting
 * - Real SubRip (`.srt`) export across scenes or single scene
 */
object SubtitleEngine {

    const val MAX_CUES_PER_SCENE = 80
    const val DEFAULT_MAX_CHARS_PER_CUE = 54

    /**
     * Applies one of the 4 curated documentary subtitle style presets.
     */
    fun applyStylePreset(presetKey: String, current: SubtitleStyleConfig): SubtitleStyleConfig {
        return when (presetKey.uppercase()) {
            "CINEMA_BOX" -> current.copy(
                stylePreset = "CINEMA_BOX",
                fontFamilyKey = "SPACE_GROTESK",
                fontSizeSp = 13.5,
                textColorHex = "#FFFFFF",
                backgroundColorHex = "#0A0C10",
                backgroundOpacity = 0.90,
                outlineEnabled = false
            )
            "HIGH_CONTRAST_YELLOW" -> current.copy(
                stylePreset = "HIGH_CONTRAST_YELLOW",
                fontFamilyKey = "PLUS_JAKARTA",
                fontSizeSp = 14.0,
                textColorHex = "#FFE066",
                backgroundColorHex = "#000000",
                backgroundOpacity = 0.72,
                outlineEnabled = true,
                outlineColorHex = "#000000"
            )
            "MINIMAL_OUTLINE" -> current.copy(
                stylePreset = "MINIMAL_OUTLINE",
                fontFamilyKey = "PLUS_JAKARTA",
                fontSizeSp = 14.0,
                textColorHex = "#F2F4F8",
                backgroundColorHex = "#0A0C10",
                backgroundOpacity = 0.0,
                outlineEnabled = true,
                outlineColorHex = "#000000"
            )
            else -> current.copy(
                stylePreset = "DOCUMENTARY_CLEAN",
                fontFamilyKey = "PLUS_JAKARTA",
                fontSizeSp = 13.0,
                textColorHex = "#F2F4F8",
                backgroundColorHex = "#0A0C10",
                backgroundOpacity = 0.78,
                outlineEnabled = true,
                outlineColorHex = "#000000"
            )
        }
    }

    /**
     * Segments English, Hindi (Devanagari `।`), or Bilingual prose into readable subtitle chunks
     * without breaking words mid-token.
     */
    fun segmentTextIntoChunks(
        rawText: String,
        maxCharsPerCue: Int = DEFAULT_MAX_CHARS_PER_CUE
    ): List<String> {
        val cleaned = rawText.trim().replace(Regex("\\s+"), " ")
        if (cleaned.isBlank()) return emptyList()
        val limit = maxCharsPerCue.coerceIn(20, 120)

        // First split on sentence boundaries including Devanagari danda `।`
        val sentences = cleaned
            .split(Regex("(?<=[.!?।])\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val chunks = mutableListOf<String>()
        sentences.forEach { sentence ->
            if (sentence.length <= limit) {
                chunks.add(sentence)
            } else {
                // Split further at clause punctuation (comma, semicolon, em-dash)
                val clauses = sentence
                    .split(Regex("(?<=[,;—–:])\\s+"))
                    .map { it.trim() }
                    .filter { it.isNotBlank() }

                clauses.forEach { clause ->
                    if (clause.length <= limit) {
                        chunks.add(clause)
                    } else {
                        val words = clause.split(" ").filter { it.isNotBlank() }
                        val current = StringBuilder()
                        words.forEach { word ->
                            if (current.isNotEmpty() && current.length + 1 + word.length > limit) {
                                chunks.add(current.toString().trim())
                                current.clear()
                            }
                            if (current.isNotEmpty()) current.append(' ')
                            current.append(word)
                        }
                        if (current.isNotEmpty()) {
                            chunks.add(current.toString().trim())
                        }
                    }
                }
            }
        }
        return chunks.filter { it.isNotBlank() }
    }

    /**
     * Generates timed [SubtitleCue]s for a [StudioScene] using Phase 5 dialogue lines,
     * attached real audio clip durations when present, and scene narration as fallback.
     *
     * Never falsely treats estimated text length as real audio duration: reports whether
     * real attached audio clip timing drove the cue spans.
     */
    fun generateSubtitlesForScene(
        scene: StudioScene,
        audioAssets: List<StudioAudioAsset> = emptyList(),
        language: String = "Bilingual",
        maxCharsPerCue: Int = DEFAULT_MAX_CHARS_PER_CUE
    ): Pair<List<SubtitleCue>, Boolean> {
        val sceneDur = scene.durationSec.coerceAtLeast(1.0)
        val audioById = audioAssets.associateBy { it.id }
        val result = mutableListOf<SubtitleCue>()
        var usedRealAudio = false
        var idx = 0

        // 1. Prefer structured Phase 5 Dialogue Lines when present
        if (scene.dialogueLines.isNotEmpty()) {
            scene.dialogueLines.sortedBy { it.startSec }.forEach { line ->
                val chunks = segmentTextIntoChunks(line.text, maxCharsPerCue)
                if (chunks.isEmpty()) return@forEach

                val linkedClip = scene.audioClips.firstOrNull {
                    (line.clipId.isNotBlank() && it.clipId == line.clipId) ||
                        it.dialogueLineId == line.lineId
                }
                val clipStart = linkedClip?.startSec ?: line.startSec
                val clipDur = if (linkedClip != null) {
                    usedRealAudio = true
                    AudioTimelineEngine.effectiveTimelineDuration(linkedClip, sceneDur)
                } else {
                    val assetDur = line.audioAssetId.takeIf { it.isNotBlank() }?.let { audioById[it]?.durationSec }
                    if (assetDur != null && assetDur > 0.2) {
                        usedRealAudio = true
                        assetDur
                    } else {
                        line.durationSec.coerceAtLeast(
                            AudioTimelineEngine.estimateSpeechDurationSec(line.text, line.language.ifBlank { language })
                        )
                    }
                }

                val safeStart = clipStart.coerceIn(0.0, (sceneDur - 0.3).coerceAtLeast(0.0))
                val totalSpan = clipDur.coerceIn(0.4, (sceneDur - safeStart).coerceAtLeast(0.4))
                val totalChars = chunks.sumOf { it.length }.coerceAtLeast(1)
                var cursor = safeStart
                chunks.forEach { chunk ->
                    val share = (chunk.length.toDouble() / totalChars.toDouble()) * totalSpan
                    val cueEnd = (cursor + share.coerceAtLeast(0.4)).coerceAtMost(sceneDur)
                    if (result.size < MAX_CUES_PER_SCENE) {
                        result.add(
                            SubtitleCue(
                                cueId = "sub_${scene.id.take(12)}_${idx++}",
                                sceneId = scene.id,
                                dialogueLineId = line.lineId,
                                speakerName = line.speakerName,
                                text = chunk,
                                startSec = round2(cursor),
                                endSec = round2(cueEnd.coerceAtLeast(cursor + 0.2)),
                                positionYNorm = 0.88
                            )
                        )
                    }
                    cursor = cueEnd
                }
            }
        }

        // 2. Include scene narration when no dialogue covered it (or alongside narration clip)
        val narrationSource = scene.narrationText.trim().ifBlank { scene.subtitleText.trim() }
        if (result.isEmpty() && narrationSource.isNotBlank()) {
            val chunks = segmentTextIntoChunks(narrationSource, maxCharsPerCue)
            val narrationClip = scene.audioClips.firstOrNull { it.trackType.equals("NARRATION", ignoreCase = true) }
            val startOffset = narrationClip?.startSec?.coerceIn(0.0, (sceneDur - 0.4).coerceAtLeast(0.0)) ?: 0.2
            val totalSpan = if (narrationClip != null) {
                usedRealAudio = true
                AudioTimelineEngine.effectiveTimelineDuration(narrationClip, sceneDur)
                    .coerceIn(0.5, (sceneDur - startOffset).coerceAtLeast(0.5))
            } else {
                AudioTimelineEngine.estimateSpeechDurationSec(narrationSource, language)
                    .coerceIn(1.0, (sceneDur - startOffset).coerceAtLeast(1.0))
            }

            val totalChars = chunks.sumOf { it.length }.coerceAtLeast(1)
            var cursor = startOffset
            chunks.forEach { chunk ->
                val share = (chunk.length.toDouble() / totalChars.toDouble()) * totalSpan
                val cueEnd = (cursor + share.coerceAtLeast(0.45)).coerceAtMost(sceneDur)
                if (result.size < MAX_CUES_PER_SCENE) {
                    result.add(
                        SubtitleCue(
                            cueId = "sub_${scene.id.take(12)}_${idx++}",
                            sceneId = scene.id,
                            speakerName = "",
                            text = chunk,
                            startSec = round2(cursor),
                            endSec = round2(cueEnd.coerceAtLeast(cursor + 0.2)),
                            positionYNorm = 0.88
                        )
                    )
                }
                cursor = cueEnd
            }
        }

        return fitSubtitlesToDuration(result, sceneDur) to usedRealAudio
    }

    /**
     * Evaluates active subtitles at [sceneTimeSec] deterministically.
     */
    fun activeSubtitlesAtTime(cues: List<SubtitleCue>, sceneTimeSec: Double): List<SubtitleCue> {
        val t = if (sceneTimeSec.isNaN() || sceneTimeSec.isInfinite()) 0.0 else sceneTimeSec
        return cues
            .filter { it.text.isNotBlank() && t >= it.startSec - 1e-4 && t <= it.endSec + 1e-4 }
            .sortedBy { it.startSec }
    }

    /**
     * Diagnoses overlapping or out-of-bounds subtitle cues inside a scene.
     */
    fun analyzeSubtitleDiagnostics(
        cues: List<SubtitleCue>,
        sceneDurationSec: Double,
        usedRealAudio: Boolean = false
    ): SubtitleDiagnostics {
        val sorted = cues.sortedBy { it.startSec }
        var overlaps = 0
        var outOfBounds = 0
        sorted.forEachIndexed { idx, cue ->
            if (cue.startSec < -1e-4 || cue.endSec > sceneDurationSec + 0.05 || cue.endSec <= cue.startSec) {
                outOfBounds++
            }
            if (idx > 0 && cue.startSec < sorted[idx - 1].endSec - 0.02) {
                overlaps++
            }
        }
        val summary = when {
            sorted.isEmpty() -> "No subtitle cues yet."
            overlaps == 0 && outOfBounds == 0 -> "${sorted.size} cue(s) cleanly timed."
            else -> "$overlaps overlap(s), $outOfBounds timing bound issue(s)."
        }
        return SubtitleDiagnostics(overlaps, outOfBounds, usedRealAudio, summary)
    }

    /**
     * Resolves overlaps and clamps all cues cleanly within [0, sceneDurationSec].
     */
    fun fitSubtitlesToDuration(cues: List<SubtitleCue>, sceneDurationSec: Double): List<SubtitleCue> {
        if (cues.isEmpty()) return emptyList()
        val maxDur = sceneDurationSec.coerceAtLeast(0.5)
        val sorted = cues.sortedWith(compareBy<SubtitleCue> { it.startSec }.thenBy { it.cueId })
        val fixed = mutableListOf<SubtitleCue>()
        var minStart = 0.0
        sorted.forEach { cue ->
            val start = cue.startSec.coerceAtLeast(minStart).coerceAtMost((maxDur - 0.25).coerceAtLeast(0.0))
            val desiredDur = (cue.endSec - cue.startSec).coerceAtLeast(0.4)
            val end = (start + desiredDur).coerceIn(start + 0.2, maxDur)
            fixed.add(cue.copy(startSec = round2(start), endSec = round2(end)))
            minStart = end
        }
        return fixed
    }

    /**
     * Splits a single subtitle cue at its midpoint (word boundary + timing midpoint).
     */
    fun splitCue(cues: List<SubtitleCue>, cueId: String): List<SubtitleCue> {
        val target = cues.firstOrNull { it.cueId == cueId } ?: return cues
        val words = target.text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.size < 2) return cues
        val midWord = words.size / 2
        val part1 = words.take(midWord).joinToString(" ")
        val part2 = words.drop(midWord).joinToString(" ")
        val midTime = round2((target.startSec + target.endSec) / 2.0)

        val first = target.copy(text = part1, endSec = midTime.coerceAtLeast(target.startSec + 0.2))
        val second = target.copy(
            cueId = "${target.cueId}_b_${abs(part2.hashCode()) % 1000}",
            text = part2,
            startSec = first.endSec,
            endSec = target.endSec.coerceAtLeast(first.endSec + 0.2)
        )
        return cues.flatMap { if (it.cueId == cueId) listOf(first, second) else listOf(it) }
    }

    /**
     * Formats seconds into exact SubRip `.srt` timestamp `HH:MM:SS,mmm`.
     */
    fun formatSrtTimestamp(seconds: Double): String {
        val safeSec = if (seconds.isNaN() || seconds.isInfinite() || seconds < 0.0) 0.0 else seconds
        val totalMs = (safeSec * 1000.0).roundToInt().coerceAtLeast(0)
        val hours = totalMs / 3_600_000
        val minutes = (totalMs % 3_600_000) / 60_000
        val secs = (totalMs % 60_000) / 1000
        val ms = totalMs % 1000
        return String.format(Locale.US, "%02d:%02d:%02d,%03d", hours, minutes, secs, ms)
    }

    /**
     * Exports a complete, standards-compliant `.srt` subtitle file across all scenes in order.
     * Falls back to `scene.subtitleText` or `scene.narrationText` if a scene has no structured `subtitleCues`.
     */
    fun generateSrtForProject(scenes: List<StudioScene>): String {
        val sb = StringBuilder()
        var sequenceNumber = 1
        var sceneStartOffset = 0.0

        scenes.forEach { scene ->
            val sceneDur = scene.durationSec.coerceAtLeast(1.0)
            val cuesForScene = if (scene.subtitleCues.isNotEmpty()) {
                scene.subtitleCues.sortedBy { it.startSec }
            } else {
                val fallbackText = scene.subtitleText.trim().ifBlank { scene.narrationText.trim() }
                if (fallbackText.isNotBlank()) {
                    listOf(
                        SubtitleCue(
                            cueId = "fallback_${scene.id}",
                            sceneId = scene.id,
                            text = fallbackText,
                            startSec = 0.0,
                            endSec = sceneDur
                        )
                    )
                } else emptyList()
            }

            cuesForScene.forEach { cue ->
                val text = cue.text.trim()
                if (text.isBlank()) return@forEach
                val absStart = sceneStartOffset + cue.startSec.coerceIn(0.0, sceneDur)
                val absEnd = (sceneStartOffset + cue.endSec.coerceIn(0.0, sceneDur))
                    .coerceAtLeast(absStart + 0.25)
                val displayLine = if (cue.speakerName.isNotBlank()) "${cue.speakerName}: $text" else text
                sb.append(sequenceNumber++).append('\n')
                sb.append(formatSrtTimestamp(absStart))
                    .append(" --> ")
                    .append(formatSrtTimestamp(absEnd))
                    .append('\n')
                sb.append(displayLine).append("\n\n")
            }
            sceneStartOffset += sceneDur
        }
        return sb.toString().trim()
    }

    private fun round2(v: Double): Double = (v * 100.0).roundToInt() / 100.0
}
