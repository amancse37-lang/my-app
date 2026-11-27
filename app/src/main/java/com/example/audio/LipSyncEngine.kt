package com.example.audio

import com.example.data.CharacterActingCue
import com.example.data.DialogueLine
import com.example.data.StudioAudioAsset
import com.example.data.StudioScene
import com.example.data.VisemeCue
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Standard mouth-shape viseme categories for 2D lip-sync and future mouth-sprite rig integration.
 */
enum class MouthViseme(val code: String, val label: String) {
    SILENT("SIL", "Closed / Rest"),
    A_OPEN("AA", "Wide Open (A/आ)"),
    E_WIDE("EE", "Wide Mid (E/ए)"),
    I_SMILE("IH", "Narrow Smile (I/इ)"),
    O_ROUND("OH", "Round Open (O/ओ)"),
    U_PUCKER("OO", "Puckered (U/उ)"),
    M_CLOSED("MBP", "Bilabial Closed (M/B/P/म/ब/प)"),
    F_V_DENTAL("FV", "Dental (F/V/फ/व)"),
    L_T_N("LTN", "Alveolar (L/T/N/ल/त/न)");

    companion object {
        fun fromKey(key: String): MouthViseme {
            return entries.firstOrNull { it.name.equals(key, ignoreCase = true) || it.code.equals(key, ignoreCase = true) }
                ?: SILENT
        }
    }
}

/**
 * Deterministic speaking state for a character at any scene timestamp.
 * Ready for both non-destructive UI speaking indicators and future mouth-sprite/rig swapping.
 */
data class CharacterSpeakingState(
    val characterId: String,
    val speaking: Boolean,
    val activeDialogueLineId: String? = null,
    val activeDialogueText: String = "",
    val activeViseme: MouthViseme = MouthViseme.SILENT,
    val energy: Float = 0f
)

/**
 * Genuine deterministic lip-sync & character speaking-state engine.
 *
 * Generates time-aligned [VisemeCue] sequences from dialogue text (supporting both English Latin
 * and Hindi Devanagari characters) modulated by real decoded audio amplitude peaks when an audio
 * asset is linked. Never fakes mouth deformation on static PNG images.
 */
object LipSyncEngine {

    const val MAX_VISEME_CUES_PER_SCENE = 240
    private const val SILENCE_ENERGY_THRESHOLD = 0.08

    /**
     * Maps a single English or Hindi Devanagari character to its corresponding [MouthViseme].
     */
    fun mapCharToViseme(ch: Char): MouthViseme {
        val lower = ch.lowercaseChar()
        return when (lower) {
            // Whitespace / punctuation -> rest
            ' ', '\n', '\t', '.', ',', '!', '?', ';', ':', '-', '—', '"', '\'', '।' -> MouthViseme.SILENT

            // English vowels
            'a' -> MouthViseme.A_OPEN
            'e' -> MouthViseme.E_WIDE
            'i', 'y' -> MouthViseme.I_SMILE
            'o' -> MouthViseme.O_ROUND
            'u', 'w' -> MouthViseme.U_PUCKER

            // English consonants grouped by mouth posture
            'm', 'b', 'p' -> MouthViseme.M_CLOSED
            'f', 'v' -> MouthViseme.F_V_DENTAL
            'l', 't', 'd', 'n', 'r', 's', 'z', 'c', 'k', 'g', 'h', 'j', 'q', 'x' -> MouthViseme.L_T_N

            // Hindi Devanagari independent vowels & matras
            'अ', 'आ', 'ा', 'ऑ', 'ॉ' -> MouthViseme.A_OPEN
            'ए', 'ऐ', 'े', 'ै' -> MouthViseme.E_WIDE
            'इ', 'ई', 'ि', 'ी' -> MouthViseme.I_SMILE
            'ओ', 'औ', 'ो', 'ौ' -> MouthViseme.O_ROUND
            'उ', 'ऊ', 'ु', 'ू' -> MouthViseme.U_PUCKER

            // Hindi bilabials (प, फ, ब, भ, म)
            'प', 'ब', 'भ', 'म' -> MouthViseme.M_CLOSED
            // Hindi labiodental / fricative (फ, व)
            'फ', 'व' -> MouthViseme.F_V_DENTAL

            // Other Devanagari consonants (क-न, य-ह)
            in '\u0915'..'\u0939' -> MouthViseme.L_T_N

            else -> MouthViseme.SILENT
        }
    }

    /**
     * Generates deterministic [VisemeCue] frames for a single [DialogueLine].
     * When [waveformPeaks] from a real decoded audio file are supplied, each frame's energy
     * is sampled directly from the real waveform envelope and silence-gated below 0.08.
     */
    fun generateVisemeCuesForDialogue(
        line: DialogueLine,
        waveformPeaks: List<Double> = emptyList(),
        maxFramesForLine: Int = 48
    ): List<VisemeCue> {
        val text = line.text.trim()
        if (text.isBlank() || line.characterId.isBlank()) return emptyList()
        val duration = line.durationSec.takeIf { it.isFinite() && it >= 0.25 } ?: 2.0
        val start = line.startSec.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0

        // Collapse consecutive identical visemes while keeping word-boundary pauses
        val rawTokens = mutableListOf<MouthViseme>()
        var prev: MouthViseme? = null
        for (ch in text) {
            val mapped = mapCharToViseme(ch)
            if (mapped != prev || mapped == MouthViseme.SILENT) {
                if (!(mapped == MouthViseme.SILENT && prev == MouthViseme.SILENT)) {
                    rawTokens.add(mapped)
                    prev = mapped
                }
            }
        }
        if (rawTokens.isEmpty()) return emptyList()

        // Downsample deterministically if dialogue is very long so Firestore cues stay compact
        val cap = maxFramesForLine.coerceIn(4, 60)
        val sampledTokens: List<MouthViseme> = if (rawTokens.size <= cap) {
            rawTokens
        } else {
            val step = rawTokens.size.toDouble() / cap.toDouble()
            List(cap) { idx -> rawTokens[(idx * step).toInt().coerceIn(0, rawTokens.lastIndex)] }
        }

        val frameDur = (duration / sampledTokens.size.toDouble()).coerceAtLeast(0.05)
        val hasRealWaveform = waveformPeaks.isNotEmpty()

        return sampledTokens.mapIndexed { idx, baseViseme ->
            val progress = idx.toDouble() / max(1, sampledTokens.size).toDouble()
            val energy = if (hasRealWaveform) {
                val peakIdx = (progress * waveformPeaks.size).toInt().coerceIn(0, waveformPeaks.lastIndex)
                waveformPeaks[peakIdx].coerceIn(0.0, 1.0)
            } else {
                when (baseViseme) {
                    MouthViseme.SILENT -> 0.0
                    MouthViseme.A_OPEN, MouthViseme.O_ROUND -> 0.85
                    MouthViseme.E_WIDE, MouthViseme.U_PUCKER -> 0.72
                    MouthViseme.I_SMILE -> 0.62
                    MouthViseme.M_CLOSED -> 0.35
                    MouthViseme.F_V_DENTAL, MouthViseme.L_T_N -> 0.55
                }
            }

            // Real audio silence gating: if decoded audio amplitude is below threshold, close mouth
            val effectiveViseme = if (hasRealWaveform && energy < SILENCE_ENERGY_THRESHOLD) {
                MouthViseme.SILENT
            } else {
                baseViseme
            }

            VisemeCue(
                cueId = "vis_${line.lineId}_$idx".take(64),
                characterId = line.characterId,
                dialogueLineId = line.lineId,
                timeSec = start + idx * frameDur,
                durationSec = frameDur,
                viseme = effectiveViseme.name,
                energy = energy
            )
        }
    }

    /**
     * Rebuilds the complete bounded list of [VisemeCue]s for all dialogue lines in [scene],
     * using real decoded waveform peaks whenever a dialogue line is linked to a [StudioAudioAsset].
     */
    fun generateSceneLipSyncCues(
        scene: StudioScene,
        audioAssets: List<StudioAudioAsset>
    ): List<VisemeCue> {
        if (scene.dialogueLines.isEmpty()) return emptyList()
        val assetsById = audioAssets.associateBy { it.id }
        val sortedLines = scene.dialogueLines.sortedWith(compareBy<DialogueLine> { it.startSec }.thenBy { it.orderIndex })
        val perLineBudget = max(6, MAX_VISEME_CUES_PER_SCENE / max(1, sortedLines.size))
        val all = mutableListOf<VisemeCue>()
        sortedLines.forEach { line ->
            val peaks = assetsById[line.audioAssetId]?.waveformPeaks.orEmpty()
            all.addAll(generateVisemeCuesForDialogue(line, peaks, maxFramesForLine = perLineBudget))
        }
        return all.take(MAX_VISEME_CUES_PER_SCENE)
    }

    /**
     * Deterministically evaluates every character's speaking state and active viseme at [sceneLocalTimeSec].
     */
    fun evaluateSpeakingStatesAtTime(
        scene: StudioScene,
        sceneLocalTimeSec: Double
    ): Map<String, CharacterSpeakingState> {
        val safeTime = if (sceneLocalTimeSec.isFinite()) sceneLocalTimeSec.coerceAtLeast(0.0) else 0.0
        val castIds = (scene.effectiveCast().map { it.characterId } + scene.dialogueLines.map { it.characterId })
            .filter { it.isNotBlank() }
            .distinct()

        val result = mutableMapOf<String, CharacterSpeakingState>()
        castIds.forEach { charId ->
            val activeLine = scene.dialogueLines.firstOrNull { line ->
                line.characterId == charId &&
                    safeTime >= line.startSec &&
                    safeTime <= (line.startSec + line.durationSec)
            }
            if (activeLine == null) {
                result[charId] = CharacterSpeakingState(
                    characterId = charId,
                    speaking = false,
                    activeViseme = MouthViseme.SILENT,
                    energy = 0f
                )
            } else {
                val activeCue = scene.lipSyncCues.firstOrNull { cue ->
                    cue.characterId == charId &&
                        cue.dialogueLineId == activeLine.lineId &&
                        safeTime >= cue.timeSec &&
                        safeTime <= (cue.timeSec + cue.durationSec + 0.02)
                }
                val viseme = activeCue?.let { MouthViseme.fromKey(it.viseme) }
                    ?: fallbackVisemeAtTime(activeLine, safeTime)
                val energy = activeCue?.energy?.toFloat()?.coerceIn(0f, 1f)
                    ?: if (viseme == MouthViseme.SILENT) 0.1f else 0.65f
                result[charId] = CharacterSpeakingState(
                    characterId = charId,
                    speaking = true,
                    activeDialogueLineId = activeLine.lineId,
                    activeDialogueText = activeLine.text,
                    activeViseme = viseme,
                    energy = energy
                )
            }
        }
        return result
    }

    private fun fallbackVisemeAtTime(line: DialogueLine, timeSec: Double): MouthViseme {
        val clean = line.text.trim()
        if (clean.isBlank() || line.durationSec <= 0.05) return MouthViseme.SILENT
        val progress = ((timeSec - line.startSec) / line.durationSec).coerceIn(0.0, 0.999)
        val idx = (progress * clean.length).toInt().coerceIn(0, clean.lastIndex)
        return mapCharToViseme(clean[idx])
    }

    /**
     * Merges speaking-state acting cues into the existing Phase 3 [CharacterActingCue] list:
     * inserts `action = "Talking"` (with dialogue emotion) at each dialogue line's start and
     * returns the character to `"Listening"` / their default action at the end of the line,
     * preserving unrelated user-authored acting cues.
     */
    fun buildSpeakingActingCues(scene: StudioScene): List<CharacterActingCue> {
        val preserved = scene.characterActingCues.filterNot { it.cueId.startsWith("dlg_act_") }
        val generated = mutableListOf<CharacterActingCue>()
        val castById = scene.effectiveCast().associateBy { it.characterId }

        scene.dialogueLines
            .sortedBy { it.startSec }
            .forEach { line ->
                if (line.characterId.isBlank()) return@forEach
                val basePlacement = castById[line.characterId]
                val pose = basePlacement?.poseView?.ifBlank { "Three-Quarter View" } ?: "Three-Quarter View"
                val baseExpression = basePlacement?.expression?.ifBlank { "Calm" } ?: "Calm"
                val speakExpression = line.emotion.ifBlank { baseExpression }

                generated.add(
                    CharacterActingCue(
                        cueId = "dlg_act_start_${line.lineId}".take(64),
                        characterId = line.characterId,
                        timeSec = line.startSec.coerceIn(0.0, scene.durationSec),
                        expression = speakExpression,
                        action = "Talking",
                        poseView = pose,
                        referenceSheetId = ""
                    )
                )
                val endTime = min(scene.durationSec, line.startSec + line.durationSec)
                generated.add(
                    CharacterActingCue(
                        cueId = "dlg_act_end_${line.lineId}".take(64),
                        characterId = line.characterId,
                        timeSec = endTime,
                        expression = baseExpression,
                        action = basePlacement?.action?.takeIf { it != "Talking" } ?: "Listening",
                        poseView = pose,
                        referenceSheetId = ""
                    )
                )
            }

        return (preserved + generated)
            .sortedBy { it.timeSec }
            .take(60)
    }

    /**
     * Convenience helper returning the currently spoken dialogue caption at [sceneLocalTimeSec],
     * useful when rendering live dialogue subtitles on the preview canvas.
     */
    fun activeDialogueSubtitleAtTime(scene: StudioScene, sceneLocalTimeSec: Double): String? {
        val line = scene.dialogueLines.firstOrNull {
            sceneLocalTimeSec >= it.startSec && sceneLocalTimeSec <= (it.startSec + it.durationSec)
        } ?: return null
        val prefix = line.speakerName.trim()
        return if (prefix.isNotBlank()) "$prefix: ${line.text.trim()}" else line.text.trim()
    }
}
