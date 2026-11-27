package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioTimelineEngine
import com.example.data.AudioImportState
import com.example.data.AudioStudioTab
import com.example.data.DialogueLine
import com.example.data.SceneAudioClip
import com.example.data.SoundSuggestionsState
import com.example.data.StudioAudioAsset
import com.example.data.StudioCharacter
import com.example.data.StudioPresets
import com.example.data.StudioProject
import com.example.data.StudioScene
import com.example.data.TtsGenerationState
import com.example.data.TtsVoiceOption
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioAmberDim
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCharcoal
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioElevated
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioTeal
import com.example.ui.theme.StudioTealDim
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.StudioViolet
import com.example.ui.theme.StudioVoid

/**
 * Renders genuine decoded PCM waveform peaks ONLY when [peaks] is non-empty.
 * Never fabricates decorative or synthetic waveforms.
 */
@Composable
fun RealWaveformStrip(
    peaks: List<Double>,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    if (peaks.isEmpty()) {
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = modifier
                .background(StudioVoid.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp)
        ) {
            Text(
                text = "Audio linked (no PCM peaks decoded)",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                color = StudioTextMuted
            )
        }
        return
    }
    Canvas(
        modifier = modifier
            .background(StudioVoid.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
            .padding(horizontal = 3.dp, vertical = 2.dp)
    ) {
        val count = peaks.size
        val stepX = size.width / count.coerceAtLeast(1)
        val centerY = size.height / 2f
        peaks.forEachIndexed { idx, peak ->
            val amp = peak.toFloat().coerceIn(0.06f, 1f)
            val halfH = (amp * size.height * 0.44f).coerceAtLeast(1.5f)
            val x = idx * stepX + stepX / 2f
            drawLine(
                color = barColor,
                start = Offset(x, centerY - halfH),
                end = Offset(x, centerY + halfH),
                strokeWidth = (stepX * 0.62f).coerceIn(1.2f, 4f)
            )
        }
    }
}

/**
 * Complete Phase 5 Audio, Narration, TTS, Dialogue, Lip-Sync, Music & SFX Studio panel.
 */
@Composable
fun AudioStudioSidebarPanel(
    project: StudioProject?,
    scene: StudioScene?,
    characters: List<StudioCharacter>,
    audioAssets: List<StudioAudioAsset>,
    activeTab: AudioStudioTab,
    availableTtsVoices: List<TtsVoiceOption>,
    selectedAudioClipId: String?,
    previewingAudioAssetId: String?,
    audioImportState: AudioImportState,
    ttsGenState: TtsGenerationState,
    soundSuggestionsState: SoundSuggestionsState,
    onSelectTab: (AudioStudioTab) -> Unit,
    onUpdateScene: ((StudioScene) -> StudioScene) -> Unit,
    onRequestImportAudio: (category: String, dialogueLineId: String?) -> Unit,
    onPreviewAudioAsset: (String) -> Unit,
    onDeleteAudioAsset: (String) -> Unit,
    onAddClipFromAsset: (StudioAudioAsset, String, String) -> Unit,
    onSelectAudioClip: (String?) -> Unit,
    onUpdateAudioClip: (String, Boolean, (SceneAudioClip) -> SceneAudioClip) -> Unit,
    onDeleteAudioClip: (String) -> Unit,
    onPreviewTtsVoice: (sampleText: String, language: String, voiceId: String, pitch: Float, rate: Float) -> Unit,
    onGenerateNarrationTts: (language: String, voiceId: String, pitch: Float, rate: Float) -> Unit,
    onAddDialogueLine: (characterId: String, text: String, emotion: String) -> Unit,
    onUpdateDialogueLine: (String, (DialogueLine) -> DialogueLine) -> Unit,
    onDeleteDialogueLine: (String) -> Unit,
    onReorderDialogueLine: (String, Int) -> Unit,
    onGenerateDialogueTts: (String, String?) -> Unit,
    onAssignAssetToDialogue: (String, String) -> Unit,
    onUpdateCharacterVoice: (String, String, String, Double, Double) -> Unit,
    onRegenerateLipSync: () -> Unit,
    onSyncSpeakingToActing: () -> Unit,
    onGenerateSoundSuggestions: () -> Unit,
    onApplySoundSuggestion: (String) -> Unit,
    onDismissSoundSuggestion: (String) -> Unit,
    onSyncSceneDurationToNarration: (String) -> Unit
) {
    if (scene == null) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Select a scene to edit narration, dialogue, music & sound effects.", style = MaterialTheme.typography.bodySmall, color = StudioTextMuted)
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab switcher
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            AudioStudioTab.entries.forEach { tab ->
                val selected = tab == activeTab
                FilterChip(
                    selected = selected,
                    onClick = { onSelectTab(tab) },
                    label = { Text(tab.label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = StudioAmberDim,
                        selectedLabelColor = StudioAmber
                    ),
                    modifier = Modifier.testTag("audio_tab_${tab.name.lowercase()}")
                )
            }
        }

        // Active status banners
        when (audioImportState) {
            is AudioImportState.Importing -> StatusBannerStrip(audioImportState.fileName, StudioTeal)
            is AudioImportState.Error -> StatusBannerStrip(audioImportState.message, StudioCoral)
            else -> Unit
        }
        when (ttsGenState) {
            is TtsGenerationState.Synthesizing -> StatusBannerStrip(ttsGenState.label, StudioAmber)
            is TtsGenerationState.Error -> StatusBannerStrip(ttsGenState.message, StudioCoral)
            else -> Unit
        }

        HorizontalDivider(color = StudioBorder)

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(10.dp)
        ) {
            when (activeTab) {
                AudioStudioTab.NARRATION -> NarrationVoiceoverTab(
                    scene = scene,
                    language = project?.scriptLanguage ?: "Bilingual",
                    audioAssets = audioAssets,
                    availableTtsVoices = availableTtsVoices,
                    selectedAudioClipId = selectedAudioClipId,
                    previewingAudioAssetId = previewingAudioAssetId,
                    isSynthesizing = ttsGenState is TtsGenerationState.Synthesizing,
                    onUpdateScene = onUpdateScene,
                    onImportNarration = { onRequestImportAudio("Narration", null) },
                    onPreviewAudioAsset = onPreviewAudioAsset,
                    onPreviewTtsVoice = onPreviewTtsVoice,
                    onGenerateNarrationTts = onGenerateNarrationTts,
                    onSelectClip = onSelectAudioClip,
                    onUpdateClip = onUpdateAudioClip,
                    onDeleteClip = onDeleteAudioClip,
                    onSyncDuration = { onSyncSceneDurationToNarration(scene.id) }
                )

                AudioStudioTab.DIALOGUE -> DialogueLipSyncTab(
                    scene = scene,
                    characters = characters,
                    audioAssets = audioAssets,
                    availableTtsVoices = availableTtsVoices,
                    previewingAudioAssetId = previewingAudioAssetId,
                    isSynthesizing = ttsGenState is TtsGenerationState.Synthesizing,
                    onAddDialogueLine = onAddDialogueLine,
                    onUpdateDialogueLine = onUpdateDialogueLine,
                    onDeleteDialogueLine = onDeleteDialogueLine,
                    onReorderDialogueLine = onReorderDialogueLine,
                    onImportDialogueAudio = { lineId -> onRequestImportAudio("Dialogue", lineId) },
                    onGenerateDialogueTts = onGenerateDialogueTts,
                    onAssignAssetToDialogue = onAssignAssetToDialogue,
                    onPreviewAudioAsset = onPreviewAudioAsset,
                    onUpdateCharacterVoice = onUpdateCharacterVoice,
                    onRegenerateLipSync = onRegenerateLipSync,
                    onSyncSpeakingToActing = onSyncSpeakingToActing
                )

                AudioStudioTab.MUSIC_SFX -> MusicAndSfxTab(
                    project = project,
                    scene = scene,
                    audioAssets = audioAssets,
                    selectedAudioClipId = selectedAudioClipId,
                    previewingAudioAssetId = previewingAudioAssetId,
                    soundSuggestionsState = soundSuggestionsState,
                    onUpdateScene = onUpdateScene,
                    onImportAudio = { cat -> onRequestImportAudio(cat, null) },
                    onAddClipFromAsset = onAddClipFromAsset,
                    onSelectClip = onSelectAudioClip,
                    onUpdateClip = onUpdateAudioClip,
                    onDeleteClip = onDeleteAudioClip,
                    onPreviewAudioAsset = onPreviewAudioAsset,
                    onGenerateSoundSuggestions = onGenerateSoundSuggestions,
                    onApplySoundSuggestion = onApplySoundSuggestion,
                    onDismissSoundSuggestion = onDismissSoundSuggestion
                )

                AudioStudioTab.LIBRARY -> AudioLibraryTab(
                    audioAssets = audioAssets,
                    previewingAudioAssetId = previewingAudioAssetId,
                    onImportAudio = { cat -> onRequestImportAudio(cat, null) },
                    onPreviewAudioAsset = onPreviewAudioAsset,
                    onAddClipFromAsset = onAddClipFromAsset,
                    onDeleteAudioAsset = onDeleteAudioAsset
                )
            }
        }
    }
}

@Composable
private fun StatusBannerStrip(message: String, accent: Color) {
    Surface(
        color = accent.copy(alpha = 0.14f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 3.dp),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun NarrationVoiceoverTab(
    scene: StudioScene,
    language: String,
    audioAssets: List<StudioAudioAsset>,
    availableTtsVoices: List<TtsVoiceOption>,
    selectedAudioClipId: String?,
    previewingAudioAssetId: String?,
    isSynthesizing: Boolean,
    onUpdateScene: ((StudioScene) -> StudioScene) -> Unit,
    onImportNarration: () -> Unit,
    onPreviewAudioAsset: (String) -> Unit,
    onPreviewTtsVoice: (String, String, String, Float, Float) -> Unit,
    onGenerateNarrationTts: (String, String, Float, Float) -> Unit,
    onSelectClip: (String?) -> Unit,
    onUpdateClip: (String, Boolean, (SceneAudioClip) -> SceneAudioClip) -> Unit,
    onDeleteClip: (String) -> Unit,
    onSyncDuration: () -> Unit
) {
    var ttsLanguage by remember(language) { mutableStateOf(if (language == "Hindi") "Hindi" else "English") }
    var selectedVoiceId by remember { mutableStateOf("") }
    var pitch by remember { mutableFloatStateOf(1.0f) }
    var rate by remember { mutableFloatStateOf(1.0f) }

    val filteredVoices = remember(availableTtsVoices, ttsLanguage) {
        val code = if (ttsLanguage == "Hindi") "hi" else "en"
        availableTtsVoices.filter { it.languageTag.lowercase().startsWith(code) }.ifEmpty { availableTtsVoices }
    }
    val estSec = remember(scene.narrationText, ttsLanguage, rate) {
        AudioTimelineEngine.estimateSpeechDurationSec(scene.narrationText, ttsLanguage, rate.toDouble())
    }
    val narrationClips = remember(scene.audioClips) {
        scene.audioClips.filter { it.trackType == "NARRATION" }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("SCENE NARRATION", style = MaterialTheme.typography.titleSmall, color = StudioAmber)
        TextButton(
            onClick = onSyncDuration,
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
        ) {
            Text("Fit ${"%.1f".format(estSec)}s", style = MaterialTheme.typography.labelSmall, color = StudioTeal)
        }
    }

    OutlinedTextField(
        value = scene.narrationText,
        onValueChange = { txt -> onUpdateScene { it.copy(narrationText = txt.take(3000)) } },
        label = { Text("Narration Text (Est. ${"%.1f".format(estSec)}s)", style = MaterialTheme.typography.labelSmall) },
        textStyle = MaterialTheme.typography.bodySmall.copy(color = StudioTextPrimary),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = StudioCharcoal,
            unfocusedContainerColor = StudioCharcoal,
            focusedBorderColor = StudioTeal,
            unfocusedBorderColor = StudioBorder
        ),
        minLines = 3,
        maxLines = 5,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("narration_text_input")
    )

    OutlinedTextField(
        value = scene.subtitleText,
        onValueChange = { txt -> onUpdateScene { it.copy(subtitleText = txt.take(1000)) } },
        label = { Text("On-Screen Subtitle Overlay", style = MaterialTheme.typography.labelSmall) },
        textStyle = MaterialTheme.typography.bodySmall.copy(color = StudioTextPrimary),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = StudioCharcoal,
            unfocusedContainerColor = StudioCharcoal,
            focusedBorderColor = StudioAmber,
            unfocusedBorderColor = StudioBorder
        ),
        maxLines = 2,
        modifier = Modifier.fillMaxWidth()
    )

    // External Voiceover Import (MP3/WAV/M4A — e.g. ElevenLabs, Studio Mic)
    OutlinedButton(
        onClick = onImportNarration,
        border = BorderStroke(1.dp, StudioTeal.copy(alpha = 0.7f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("import_narration_audio_button")
    ) {
        Icon(Icons.Default.UploadFile, contentDescription = null, tint = StudioTeal, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("Import Voiceover (MP3 / WAV / M4A · External Tools)", style = MaterialTheme.typography.labelSmall, color = StudioTeal)
    }

    // Real On-Device Android TTS Synthesizer (Hindi & English)
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = StudioCharcoal,
        border = BorderStroke(1.dp, StudioBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = StudioAmber, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text("DEVICE TEXT-TO-SPEECH (REAL WAV)", style = MaterialTheme.typography.labelMedium, color = StudioAmber)
            }
            Text(
                text = "Uses installed Android Speech voices (offline/free). For studio neural narration, import external WAV/MP3 above.",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                color = StudioTextMuted
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("English", "Hindi").forEach { lang ->
                    FilterChip(
                        selected = ttsLanguage == lang,
                        onClick = {
                            ttsLanguage = lang
                            selectedVoiceId = ""
                        },
                        label = { Text(lang, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StudioAmberDim,
                            selectedLabelColor = StudioAmber
                        )
                    )
                }
            }

            if (filteredVoices.isNotEmpty()) {
                Text("Installed Device Voices (${filteredVoices.size})", style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    filteredVoices.take(8).forEach { v ->
                        val active = selectedVoiceId == v.voiceId || (selectedVoiceId.isBlank() && v == filteredVoices.first())
                        FilterChip(
                            selected = active,
                            onClick = { selectedVoiceId = v.voiceId },
                            label = { Text(v.displayName, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StudioTealDim,
                                selectedLabelColor = StudioTeal
                            )
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Pitch ${"%.2f".format(pitch)}x", style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary)
                    Slider(
                        value = pitch,
                        onValueChange = { pitch = it },
                        valueRange = 0.65f..1.45f,
                        colors = SliderDefaults.colors(thumbColor = StudioAmber, activeTrackColor = StudioAmber)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Speed ${"%.2f".format(rate)}x", style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary)
                    Slider(
                        value = rate,
                        onValueChange = { rate = it },
                        valueRange = 0.7f..1.35f,
                        colors = SliderDefaults.colors(thumbColor = StudioTeal, activeTrackColor = StudioTeal)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = {
                        onPreviewTtsVoice(
                            scene.narrationText.take(120),
                            ttsLanguage,
                            selectedVoiceId.ifBlank { filteredVoices.firstOrNull()?.voiceId.orEmpty() },
                            pitch,
                            rate
                        )
                    },
                    border = BorderStroke(1.dp, StudioBorder),
                    modifier = Modifier.weight(1f).testTag("tts_preview_voice_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = StudioTextPrimary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Preview", style = MaterialTheme.typography.labelSmall, color = StudioTextPrimary)
                }
                Button(
                    onClick = {
                        onGenerateNarrationTts(
                            ttsLanguage,
                            selectedVoiceId.ifBlank { filteredVoices.firstOrNull()?.voiceId.orEmpty() },
                            pitch,
                            rate
                        )
                    },
                    enabled = !isSynthesizing && scene.narrationText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = StudioAmber, contentColor = StudioVoid),
                    modifier = Modifier.weight(1.4f).testTag("tts_generate_narration_button")
                ) {
                    Text(
                        text = if (isSynthesizing) "Synthesizing..." else "Generate WAV",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Active Scene Narration Audio Clips
    if (narrationClips.isNotEmpty()) {
        Text("SCENE NARRATION CLIPS (${narrationClips.size})", style = MaterialTheme.typography.labelMedium, color = StudioTeal)
        narrationClips.forEach { clip ->
            val asset = audioAssets.firstOrNull { it.id == clip.audioAssetId }
            AudioClipInspectorCard(
                clip = clip,
                asset = asset,
                containerDurationSec = scene.durationSec,
                isSelected = selectedAudioClipId == clip.clipId,
                isPreviewing = previewingAudioAssetId == clip.audioAssetId,
                onSelect = { onSelectClip(clip.clipId) },
                onPreview = { asset?.let { onPreviewAudioAsset(it.id) } },
                onUpdateClip = { recordUndo, fn -> onUpdateClip(clip.clipId, recordUndo, fn) },
                onDelete = { onDeleteClip(clip.clipId) }
            )
        }
    }
}

@Composable
private fun DialogueLipSyncTab(
    scene: StudioScene,
    characters: List<StudioCharacter>,
    audioAssets: List<StudioAudioAsset>,
    availableTtsVoices: List<TtsVoiceOption>,
    previewingAudioAssetId: String?,
    isSynthesizing: Boolean,
    onAddDialogueLine: (String, String, String) -> Unit,
    onUpdateDialogueLine: (String, (DialogueLine) -> DialogueLine) -> Unit,
    onDeleteDialogueLine: (String) -> Unit,
    onReorderDialogueLine: (String, Int) -> Unit,
    onImportDialogueAudio: (String) -> Unit,
    onGenerateDialogueTts: (String, String?) -> Unit,
    onAssignAssetToDialogue: (String, String) -> Unit,
    onPreviewAudioAsset: (String) -> Unit,
    onUpdateCharacterVoice: (String, String, String, Double, Double) -> Unit,
    onRegenerateLipSync: () -> Unit,
    onSyncSpeakingToActing: () -> Unit
) {
    var selectedSpeakerId by remember(characters) {
        mutableStateOf(scene.effectiveCast().firstOrNull()?.characterId ?: characters.firstOrNull()?.id.orEmpty())
    }
    var newLineText by remember { mutableStateOf("") }
    var newLineEmotion by remember { mutableStateOf("Calm") }

    Text("CHARACTER DIALOGUE & LIP-SYNC", style = MaterialTheme.typography.titleSmall, color = StudioAmber)

    // Lip-sync status & acting sync actions
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = StudioCharcoal,
        border = BorderStroke(1.dp, StudioViolet.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("LIP-SYNC FOUNDATION", style = MaterialTheme.typography.labelMedium, color = StudioViolet)
                Text("${scene.lipSyncCues.size} viseme cues", style = MaterialTheme.typography.labelSmall, color = StudioTeal)
            }
            Text(
                text = "Deterministic English + Hindi viseme timeline & real audio envelope gating. Shows live Speaking State + viseme telemetry on stage without fake mouth distortion on static PNGs.",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                color = StudioTextSecondary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onRegenerateLipSync,
                    border = BorderStroke(1.dp, StudioViolet.copy(alpha = 0.65f)),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.weight(1f).height(32.dp).testTag("recompute_lipsync_button")
                ) {
                    Text("Rebuild Visemes", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp), color = StudioViolet)
                }
                Button(
                    onClick = onSyncSpeakingToActing,
                    colors = ButtonDefaults.buttonColors(containerColor = StudioTeal, contentColor = StudioVoid),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.weight(1.2f).height(32.dp).testTag("sync_speaking_acting_button")
                ) {
                    Text("Sync to Acting Cues", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp), fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Add New Dialogue Line Card
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = StudioCharcoal,
        border = BorderStroke(1.dp, StudioBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.padding(10.dp)) {
            Text("ADD DIALOGUE LINE", style = MaterialTheme.typography.labelMedium, color = StudioTeal)
            if (characters.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    characters.forEach { c ->
                        FilterChip(
                            selected = selectedSpeakerId == c.id,
                            onClick = { selectedSpeakerId = c.id },
                            label = { Text("${c.charCode} ${c.name.take(14)}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StudioAmberDim,
                                selectedLabelColor = StudioAmber
                            )
                        )
                    }
                }
            }

            // Character voice profile quick settings for selected speaker
            val activeChar = characters.firstOrNull { it.id == selectedSpeakerId }
            if (activeChar != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StudioVoid, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "Voice: ${activeChar.voiceLanguage} · P ${"%.1f".format(activeChar.voicePitch)} · R ${"%.1f".format(activeChar.voiceRate)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                        color = StudioTextSecondary
                    )
                    TextButton(
                        onClick = {
                            val nextLang = if (activeChar.voiceLanguage == "Hindi") "English" else "Hindi"
                            val matchedVoice = availableTtsVoices.firstOrNull {
                                it.languageTag.lowercase().startsWith(if (nextLang == "Hindi") "hi" else "en")
                            }?.voiceId.orEmpty()
                            onUpdateCharacterVoice(activeChar.id, nextLang, matchedVoice, activeChar.voicePitch, activeChar.voiceRate)
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Text("Switch EN/HI", style = MaterialTheme.typography.labelSmall, color = StudioAmber)
                    }
                }
            }

            OutlinedTextField(
                value = newLineText,
                onValueChange = { newLineText = it.take(1200) },
                placeholder = { Text("Enter spoken dialogue line (English or Hindi)...", color = StudioTextMuted) },
                textStyle = MaterialTheme.typography.bodySmall.copy(color = StudioTextPrimary),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = StudioVoid,
                    unfocusedContainerColor = StudioVoid,
                    focusedBorderColor = StudioAmber,
                    unfocusedBorderColor = StudioBorder
                ),
                minLines = 2,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth().testTag("new_dialogue_input")
            )

            Button(
                onClick = {
                    if (newLineText.isNotBlank() && selectedSpeakerId.isNotBlank()) {
                        onAddDialogueLine(selectedSpeakerId, newLineText, newLineEmotion)
                        newLineText = ""
                    }
                },
                enabled = newLineText.isNotBlank() && selectedSpeakerId.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = StudioAmber, contentColor = StudioVoid),
                modifier = Modifier.fillMaxWidth().testTag("add_dialogue_line_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp))
                Text("Add Spoken Line", fontWeight = FontWeight.Bold)
            }
        }
    }

    // Existing Scene Dialogue Lines
    if (scene.dialogueLines.isEmpty()) {
        Text(
            text = "No dialogue lines in this scene yet. Add a line above or extract dialogue via Scenes -> AI Intelligence.",
            style = MaterialTheme.typography.bodySmall,
            color = StudioTextMuted
        )
    } else {
        scene.dialogueLines.sortedBy { it.orderIndex }.forEachIndexed { idx, line ->
            val charProfile = characters.firstOrNull { it.id == line.characterId }
            val linkedAsset = audioAssets.firstOrNull { it.id == line.audioAssetId }
            val isPlaying = previewingAudioAssetId != null && previewingAudioAssetId == line.audioAssetId
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = StudioCharcoal,
                border = BorderStroke(1.dp, if (linkedAsset != null) StudioTeal.copy(alpha = 0.55f) else StudioBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(9.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${charProfile?.charCode ?: "SPK"} · ${charProfile?.name ?: line.speakerName}",
                                style = MaterialTheme.typography.labelMedium,
                                color = StudioAmber,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Start ${"%.1f".format(line.startSec)}s · Dur ${"%.1f".format(line.durationSec)}s · ${line.emotion}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                color = StudioTeal
                            )
                        }
                        Row {
                            IconButton(
                                onClick = { onReorderDialogueLine(line.lineId, -1) },
                                enabled = idx > 0,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = "Move up", tint = StudioTextSecondary, modifier = Modifier.size(14.dp))
                            }
                            IconButton(
                                onClick = { onReorderDialogueLine(line.lineId, 1) },
                                enabled = idx < scene.dialogueLines.lastIndex,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = "Move down", tint = StudioTextSecondary, modifier = Modifier.size(14.dp))
                            }
                            IconButton(
                                onClick = { onDeleteDialogueLine(line.lineId) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete line", tint = StudioCoral, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    Text(
                        text = "\"${line.text}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = StudioTextPrimary
                    )

                    // Timing nudge slider
                    Text("Start Offset: ${"%.2f".format(line.startSec)}s", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp), color = StudioTextSecondary)
                    Slider(
                        value = line.startSec.toFloat().coerceIn(0f, scene.durationSec.toFloat().coerceAtLeast(1f)),
                        onValueChange = { v -> onUpdateDialogueLine(line.lineId) { it.copy(startSec = v.toDouble()) } },
                        valueRange = 0f..scene.durationSec.toFloat().coerceAtLeast(1f),
                        colors = SliderDefaults.colors(thumbColor = StudioTeal, activeTrackColor = StudioTeal)
                    )

                    if (linkedAsset != null) {
                        RealWaveformStrip(
                            peaks = linkedAsset.waveformPeaks,
                            barColor = StudioTeal,
                            modifier = Modifier.fillMaxWidth().height(22.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { onImportDialogueAudio(line.lineId) },
                            border = BorderStroke(1.dp, StudioBorder),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f).height(30.dp)
                        ) {
                            Text("Import Audio", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp), color = StudioTextPrimary)
                        }
                        OutlinedButton(
                            onClick = { onGenerateDialogueTts(line.lineId, null) },
                            enabled = !isSynthesizing,
                            border = BorderStroke(1.dp, StudioAmber.copy(alpha = 0.65f)),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f).height(30.dp)
                        ) {
                            Text("TTS WAV", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp), color = StudioAmber)
                        }
                        if (linkedAsset != null) {
                            IconButton(
                                onClick = { onPreviewAudioAsset(linkedAsset.id) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = "Preview dialogue audio",
                                    tint = StudioEmerald,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MusicAndSfxTab(
    project: StudioProject?,
    scene: StudioScene,
    audioAssets: List<StudioAudioAsset>,
    selectedAudioClipId: String?,
    previewingAudioAssetId: String?,
    soundSuggestionsState: SoundSuggestionsState,
    onUpdateScene: ((StudioScene) -> StudioScene) -> Unit,
    onImportAudio: (String) -> Unit,
    onAddClipFromAsset: (StudioAudioAsset, String, String) -> Unit,
    onSelectClip: (String?) -> Unit,
    onUpdateClip: (String, Boolean, (SceneAudioClip) -> SceneAudioClip) -> Unit,
    onDeleteClip: (String) -> Unit,
    onPreviewAudioAsset: (String) -> Unit,
    onGenerateSoundSuggestions: () -> Unit,
    onApplySoundSuggestion: (String) -> Unit,
    onDismissSoundSuggestion: (String) -> Unit
) {
    Text("MUSIC, SFX & AMBIENT MIXER", style = MaterialTheme.typography.titleSmall, color = StudioAmber)

    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { onImportAudio("Music") },
            border = BorderStroke(1.dp, StudioEmerald.copy(alpha = 0.65f)),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
            modifier = Modifier.weight(1f).height(34.dp).testTag("import_music_button")
        ) {
            Text("+ Music", style = MaterialTheme.typography.labelSmall, color = StudioEmerald)
        }
        OutlinedButton(
            onClick = { onImportAudio("SFX") },
            border = BorderStroke(1.dp, StudioAmber.copy(alpha = 0.65f)),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
            modifier = Modifier.weight(1f).height(34.dp).testTag("import_sfx_button")
        ) {
            Text("+ SFX", style = MaterialTheme.typography.labelSmall, color = StudioAmber)
        }
        OutlinedButton(
            onClick = { onImportAudio("Ambient") },
            border = BorderStroke(1.dp, StudioTeal.copy(alpha = 0.65f)),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
            modifier = Modifier.weight(1f).height(34.dp).testTag("import_ambient_button")
        ) {
            Text("+ Ambient", style = MaterialTheme.typography.labelSmall, color = StudioTeal)
        }
    }

    // AI Sound Suggestions Card
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = StudioCharcoal,
        border = BorderStroke(1.dp, StudioAmber.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = StudioAmber, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("AI SOUND DESIGN CUES", style = MaterialTheme.typography.labelMedium, color = StudioAmber)
                }
                OutlinedButton(
                    onClick = onGenerateSoundSuggestions,
                    enabled = soundSuggestionsState !is SoundSuggestionsState.Loading,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    border = BorderStroke(1.dp, StudioAmber.copy(alpha = 0.65f)),
                    modifier = Modifier.height(28.dp).testTag("ai_sound_suggest_button")
                ) {
                    Text(
                        text = if (soundSuggestionsState is SoundSuggestionsState.Loading) "Analyzing..." else "AI Suggest",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                        color = StudioAmber
                    )
                }
            }

            if (soundSuggestionsState is SoundSuggestionsState.Error) {
                Text(soundSuggestionsState.message, style = MaterialTheme.typography.labelSmall, color = StudioCoral)
            }

            if (scene.soundSuggestions.isEmpty()) {
                Text(
                    text = "Let Gemini recommend music moods, ambient beds, and SFX cues for this scene (no copyrighted downloads).",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                    color = StudioTextMuted
                )
            } else {
                scene.soundSuggestions.forEach { sug ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = StudioVoid,
                        border = BorderStroke(1.dp, if (sug.applied) StudioEmerald else StudioBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.padding(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "${sug.category} · ${sug.title}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (sug.applied) StudioEmerald else StudioTextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(
                                        onClick = { onApplySoundSuggestion(sug.suggestionId) },
                                        contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp)
                                    ) {
                                        Text(if (sug.applied) "Applied" else "Use Cue", style = MaterialTheme.typography.labelSmall, color = StudioTeal)
                                    }
                                    IconButton(
                                        onClick = { onDismissSoundSuggestion(sug.suggestionId) },
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Dismiss", tint = StudioTextMuted, modifier = Modifier.size(13.dp))
                                    }
                                }
                            }
                            if (sug.description.isNotBlank()) {
                                Text(sug.description, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp), color = StudioTextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }

    // Project-Level Background Music Clips
    val projectClips = project?.projectAudioClips.orEmpty()
    if (projectClips.isNotEmpty()) {
        Text("PROJECT-LEVEL MUSIC / BEDS (${projectClips.size})", style = MaterialTheme.typography.labelMedium, color = StudioEmerald)
        projectClips.forEach { clip ->
            val asset = audioAssets.firstOrNull { it.id == clip.audioAssetId }
            AudioClipInspectorCard(
                clip = clip,
                asset = asset,
                containerDurationSec = 120.0,
                isSelected = selectedAudioClipId == clip.clipId,
                isPreviewing = previewingAudioAssetId == clip.audioAssetId,
                onSelect = { onSelectClip(clip.clipId) },
                onPreview = { asset?.let { onPreviewAudioAsset(it.id) } },
                onUpdateClip = { recordUndo, fn -> onUpdateClip(clip.clipId, recordUndo, fn) },
                onDelete = { onDeleteClip(clip.clipId) }
            )
        }
    }

    // Scene-Level Music, SFX & Ambient Clips
    val sceneFxClips = scene.audioClips.filter { it.trackType in setOf("MUSIC", "SFX", "AMBIENT") }
    if (sceneFxClips.isNotEmpty()) {
        Text("SCENE AUDIO CLIPS (${sceneFxClips.size})", style = MaterialTheme.typography.labelMedium, color = StudioTeal)
        sceneFxClips.forEach { clip ->
            val asset = audioAssets.firstOrNull { it.id == clip.audioAssetId }
            AudioClipInspectorCard(
                clip = clip,
                asset = asset,
                containerDurationSec = scene.durationSec,
                isSelected = selectedAudioClipId == clip.clipId,
                isPreviewing = previewingAudioAssetId == clip.audioAssetId,
                onSelect = { onSelectClip(clip.clipId) },
                onPreview = { asset?.let { onPreviewAudioAsset(it.id) } },
                onUpdateClip = { recordUndo, fn -> onUpdateClip(clip.clipId, recordUndo, fn) },
                onDelete = { onDeleteClip(clip.clipId) }
            )
        }
    }

    // Cue Labels (Preserving Phase 1–4 metadata notes)
    HorizontalDivider(color = StudioBorder)
    Text("Scene Cue Notes", style = MaterialTheme.typography.labelSmall, color = StudioTextMuted)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = scene.musicCue,
            onValueChange = { txt -> onUpdateScene { it.copy(musicCue = txt.take(120)) } },
            label = { Text("Music Cue", style = MaterialTheme.typography.labelSmall) },
            textStyle = MaterialTheme.typography.bodySmall.copy(color = StudioTextPrimary),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = StudioCharcoal,
                unfocusedContainerColor = StudioCharcoal,
                focusedBorderColor = StudioEmerald,
                unfocusedBorderColor = StudioBorder
            ),
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = scene.sfxCue,
            onValueChange = { txt -> onUpdateScene { it.copy(sfxCue = txt.take(120)) } },
            label = { Text("SFX Cue", style = MaterialTheme.typography.labelSmall) },
            textStyle = MaterialTheme.typography.bodySmall.copy(color = StudioTextPrimary),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = StudioCharcoal,
                unfocusedContainerColor = StudioCharcoal,
                focusedBorderColor = StudioAmber,
                unfocusedBorderColor = StudioBorder
            ),
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AudioLibraryTab(
    audioAssets: List<StudioAudioAsset>,
    previewingAudioAssetId: String?,
    onImportAudio: (String) -> Unit,
    onPreviewAudioAsset: (String) -> Unit,
    onAddClipFromAsset: (StudioAudioAsset, String, String) -> Unit,
    onDeleteAudioAsset: (String) -> Unit
) {
    var filterCat by remember { mutableStateOf("All") }

    Text("PERSISTENT AUDIO LIBRARY", style = MaterialTheme.typography.titleSmall, color = StudioAmber)
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = StudioElevated,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Audio files are copied to app-private persistent storage (max 15 MB). Metadata & waveforms sync via Firestore; cloud audio file sync requires enabling Firebase Storage.",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
            color = StudioTextSecondary,
            modifier = Modifier.padding(8.dp)
        )
    }

    OutlinedButton(
        onClick = { onImportAudio(if (filterCat == "All") "Narration" else filterCat) },
        border = BorderStroke(1.dp, StudioTeal.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth().testTag("audio_library_import_button")
    ) {
        Icon(Icons.Default.AudioFile, contentDescription = null, tint = StudioTeal, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(6.dp))
        Text("Import Audio File into Library", style = MaterialTheme.typography.labelSmall, color = StudioTeal)
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        (listOf("All") + StudioPresets.audioCategories).forEach { cat ->
            FilterChip(
                selected = filterCat == cat,
                onClick = { filterCat = cat },
                label = { Text(cat, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = StudioAmberDim,
                    selectedLabelColor = StudioAmber
                )
            )
        }
    }

    val filtered = remember(audioAssets, filterCat) {
        if (filterCat == "All") audioAssets else audioAssets.filter { it.category == filterCat }
    }

    if (filtered.isEmpty()) {
        Text(
            text = "No audio assets yet. Import a recording, ElevenLabs voiceover, music track, or generate Android TTS audio.",
            style = MaterialTheme.typography.bodySmall,
            color = StudioTextMuted
        )
    } else {
        filtered.forEach { asset ->
            val playing = previewingAudioAssetId == asset.id
            val missing = asset.storageState == "MISSING_LOCAL"
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = StudioCharcoal,
                border = BorderStroke(1.dp, if (missing) StudioCoral.copy(alpha = 0.6f) else StudioBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(9.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(color = StudioTealDim, shape = RoundedCornerShape(4.dp)) {
                                    Text(
                                        text = asset.category.uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                        color = StudioTeal,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                                Surface(color = StudioAmberDim, shape = RoundedCornerShape(4.dp)) {
                                    Text(
                                        text = if (asset.sourceType == "ANDROID_TTS") "TTS" else "IMPORT",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                        color = StudioAmber,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                                Text(
                                    text = "${"%.1f".format(asset.durationSec)}s · ${(asset.fileSizeBytes / 1024L).coerceAtLeast(1L)} KB",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = StudioTextMuted
                                )
                            }
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = asset.name,
                                style = MaterialTheme.typography.labelLarge,
                                color = StudioTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (missing) {
                                Text(
                                    text = "Local audio missing on this device — re-import to play.",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = StudioCoral
                                )
                            }
                        }

                        Row {
                            IconButton(
                                onClick = { onPreviewAudioAsset(asset.id) },
                                enabled = !missing,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = if (playing) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = "Preview audio",
                                    tint = if (playing) StudioCoral else StudioEmerald,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            IconButton(
                                onClick = { onDeleteAudioAsset(asset.id) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete audio asset", tint = StudioCoral, modifier = Modifier.size(15.dp))
                            }
                        }
                    }

                    RealWaveformStrip(
                        peaks = asset.waveformPeaks,
                        barColor = StudioAmber,
                        modifier = Modifier.fillMaxWidth().height(24.dp)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        val trackKey = AudioTimelineEngine.trackTypeKeyForCategory(asset.category)
                        OutlinedButton(
                            onClick = { onAddClipFromAsset(asset, trackKey, "SCENE") },
                            border = BorderStroke(1.dp, StudioTeal.copy(alpha = 0.6f)),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f).height(28.dp)
                        ) {
                            Text("+ Add to Scene", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp), color = StudioTeal)
                        }
                        if (asset.category == "Music" || asset.category == "Ambient") {
                            OutlinedButton(
                                onClick = { onAddClipFromAsset(asset, trackKey, "PROJECT") },
                                border = BorderStroke(1.dp, StudioEmerald.copy(alpha = 0.6f)),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.weight(1f).height(28.dp)
                            ) {
                                Text("+ Project Bed", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp), color = StudioEmerald)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Reusable Clip Mixer & Trim Card for editing start time, trim start/end, volume, fade-in/out, and looping.
 */
@Composable
fun AudioClipInspectorCard(
    clip: SceneAudioClip,
    asset: StudioAudioAsset?,
    containerDurationSec: Double,
    isSelected: Boolean,
    isPreviewing: Boolean,
    onSelect: () -> Unit,
    onPreview: () -> Unit,
    onUpdateClip: (recordUndo: Boolean, (SceneAudioClip) -> SceneAudioClip) -> Unit,
    onDelete: () -> Unit
) {
    val srcDur = clip.sourceDurationSec.coerceAtLeast(0.5)
    val effectiveTrimEnd = if (clip.trimEndSec <= 0.0) srcDur else clip.trimEndSec.coerceAtMost(srcDur)
    val playedDur = AudioTimelineEngine.trimmedSourceDuration(clip)

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) StudioElevated else StudioCharcoal,
        border = BorderStroke(1.dp, if (isSelected) StudioAmber else StudioBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .testTag("audio_clip_card_${clip.clipId}")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(9.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.GraphicEq, contentDescription = null, tint = StudioTeal, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(5.dp))
                    Column {
                        Text(
                            text = clip.title.ifBlank { asset?.name ?: "Audio Clip" },
                            style = MaterialTheme.typography.labelMedium,
                            color = StudioTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${clip.trackType} · ${clip.scope} · ${"%.1f".format(clip.startSec)}s → ${"%.1f".format(clip.startSec + playedDur)}s",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = StudioTextSecondary
                        )
                    }
                }
                Row {
                    IconButton(onClick = onPreview, enabled = asset != null, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = if (isPreviewing) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = "Preview clip",
                            tint = StudioEmerald,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete clip", tint = StudioCoral, modifier = Modifier.size(15.dp))
                    }
                }
            }

            if (asset != null) {
                RealWaveformStrip(
                    peaks = asset.waveformPeaks,
                    barColor = StudioTeal,
                    modifier = Modifier.fillMaxWidth().height(20.dp)
                )
            }

            // Start position
            Text("Start Time: ${"%.2f".format(clip.startSec)}s", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp), color = StudioTextSecondary)
            Slider(
                value = clip.startSec.toFloat().coerceIn(0f, containerDurationSec.toFloat().coerceAtLeast(1f)),
                onValueChange = { v -> onUpdateClip(false) { AudioTimelineEngine.moveClip(it, v.toDouble(), containerDurationSec) } },
                valueRange = 0f..containerDurationSec.toFloat().coerceAtLeast(1f),
                colors = SliderDefaults.colors(thumbColor = StudioAmber, activeTrackColor = StudioAmber)
            )

            // Trim Start / End
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Trim In: ${"%.1f".format(clip.trimStartSec)}s", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = StudioTextSecondary)
                    Slider(
                        value = clip.trimStartSec.toFloat().coerceIn(0f, (srcDur - 0.25).toFloat().coerceAtLeast(0.1f)),
                        onValueChange = { v -> onUpdateClip(false) { AudioTimelineEngine.trimClip(it, v.toDouble(), effectiveTrimEnd) } },
                        valueRange = 0f..(srcDur - 0.25).toFloat().coerceAtLeast(0.1f),
                        colors = SliderDefaults.colors(thumbColor = StudioTeal, activeTrackColor = StudioTeal)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Trim Out: ${"%.1f".format(effectiveTrimEnd)}s", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = StudioTextSecondary)
                    Slider(
                        value = effectiveTrimEnd.toFloat().coerceIn(0.25f, srcDur.toFloat()),
                        onValueChange = { v -> onUpdateClip(false) { AudioTimelineEngine.trimClip(it, it.trimStartSec, v.toDouble()) } },
                        valueRange = 0.25f..srcDur.toFloat().coerceAtLeast(0.3f),
                        colors = SliderDefaults.colors(thumbColor = StudioTeal, activeTrackColor = StudioTeal)
                    )
                }
            }

            // Volume & Fades
            Text("Volume: ${(clip.volume * 100).toInt()}%", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp), color = StudioTextSecondary)
            Slider(
                value = clip.volume.toFloat().coerceIn(0f, 1.5f),
                onValueChange = { v -> onUpdateClip(false) { it.copy(volume = v.toDouble()) } },
                valueRange = 0f..1.5f,
                colors = SliderDefaults.colors(thumbColor = StudioEmerald, activeTrackColor = StudioEmerald)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Fade In: ${"%.1f".format(clip.fadeInSec)}s", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = StudioTextSecondary)
                    Slider(
                        value = clip.fadeInSec.toFloat().coerceIn(0f, 5f),
                        onValueChange = { v -> onUpdateClip(false) { it.copy(fadeInSec = v.toDouble()) } },
                        valueRange = 0f..5f,
                        colors = SliderDefaults.colors(thumbColor = StudioViolet, activeTrackColor = StudioViolet)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Fade Out: ${"%.1f".format(clip.fadeOutSec)}s", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = StudioTextSecondary)
                    Slider(
                        value = clip.fadeOutSec.toFloat().coerceIn(0f, 5f),
                        onValueChange = { v -> onUpdateClip(false) { it.copy(fadeOutSec = v.toDouble()) } },
                        valueRange = 0f..5f,
                        colors = SliderDefaults.colors(thumbColor = StudioViolet, activeTrackColor = StudioViolet)
                    )
                }
            }

            // Loop & Mute Switches
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Loop", style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary)
                    Spacer(Modifier.width(6.dp))
                    Switch(
                        checked = clip.loopEnabled,
                        onCheckedChange = { enabled -> onUpdateClip(true) { it.copy(loopEnabled = enabled) } },
                        colors = SwitchDefaults.colors(checkedTrackColor = StudioTeal)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Mute", style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary)
                    Spacer(Modifier.width(6.dp))
                    Switch(
                        checked = clip.muted,
                        onCheckedChange = { muted -> onUpdateClip(true) { it.copy(muted = muted) } },
                        colors = SwitchDefaults.colors(checkedTrackColor = StudioCoral)
                    )
                }
            }
        }
    }
}
