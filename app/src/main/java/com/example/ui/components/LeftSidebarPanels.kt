package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.compositor.SceneCompositor
import com.example.data.AiScriptState
import com.example.data.AudioStudioTab
import com.example.data.CharacterBitmapCodec
import com.example.data.DialogueLine
import com.example.data.MotionGraphicElement
import com.example.data.MotionGraphicsStudioTab
import com.example.data.SceneAudioClip
import com.example.data.SceneTransitionConfig
import com.example.data.SidebarSection
import com.example.data.StudioAsset
import com.example.data.StudioAudioAsset
import com.example.data.StudioCharacter
import com.example.data.StudioPresets
import com.example.data.StudioScene
import com.example.data.SubtitleCue
import com.example.data.SubtitleStyleConfig
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioAmberDim
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioBorderSubtle
import com.example.ui.theme.StudioCharcoal
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioElevated
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioPanel
import com.example.ui.theme.StudioTeal
import com.example.ui.theme.StudioTealDim
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.StudioVoid
import com.example.viewmodel.StudioUiState

private fun iconForSidebar(section: SidebarSection): ImageVector = when (section) {
    SidebarSection.SCRIPT -> Icons.Default.Description
    SidebarSection.SCENES -> Icons.Default.Movie
    SidebarSection.CHARACTERS -> Icons.Default.People
    SidebarSection.LOCATIONS -> Icons.Default.Landscape
    SidebarSection.ASSETS -> Icons.Default.FolderCopy
    SidebarSection.AUDIO -> Icons.Default.Audiotrack
    SidebarSection.MOTION_GRAPHICS -> Icons.Default.Animation
}

@Composable
fun LeftSidebarPane(
    state: StudioUiState,
    onSelectSection: (SidebarSection) -> Unit,
    onScriptTextChange: (String) -> Unit,
    onScriptLanguageChange: (String) -> Unit,
    onLoadSampleScript: (Boolean) -> Unit,
    onGenerateScenesFromScript: () -> Unit,
    onSelectScene: (String) -> Unit,
    onAddScene: () -> Unit,
    onDeleteScene: (String) -> Unit,
    onMoveScene: (String, Int) -> Unit,
    onSelectLibraryCharacter: (String) -> Unit,
    onOpenAddCharacter: (StudioCharacter?) -> Unit,
    onRenameCharacter: (StudioCharacter) -> Unit,
    onRequestDeleteCharacter: (StudioCharacter) -> Unit,
    onOpenCharacterGenerator: (StudioCharacter) -> Unit,
    onAddCharacterToScene: (String) -> Unit,
    onRemoveCharacterFromScene: (String) -> Unit,
    onSetReferenceAsPrimary: (String, String) -> Unit,
    onDeleteCharacterReference: (String, String) -> Unit,
    onOpenAddAsset: (String) -> Unit,
    onImportImageAsset: (Uri) -> Unit,
    onApplyAssetToScene: (StudioAsset) -> Unit,
    onDeleteAsset: (String) -> Unit,
    onAssetCategoryChange: (String) -> Unit,
    onOpenBackgroundGenerator: () -> Unit = {},
    onOpenPropGenerator: () -> Unit = {},
    onOpenSceneSuggestions: () -> Unit = {},
    onToggleSceneApproved: (String) -> Unit = {},
    onRegenerateSingleScene: (String) -> Unit = {},
    onRegenerateSceneNarration: () -> Unit = {},
    onRegenerateSceneVisualDirection: () -> Unit = {},
    onRegenerateSceneDialogue: () -> Unit = {},
    onApplyScenePlanSuggestions: () -> Unit = {},
    onSyncSceneDurationToNarration: (String) -> Unit = {},
    onSwapNarrationBetweenScenes: (Int, Int) -> Unit = { _, _ -> },
    onSelectAudioTab: (AudioStudioTab) -> Unit = {},
    onRequestImportAudio: (category: String, dialogueLineId: String?) -> Unit = { _, _ -> },
    onPreviewAudioAsset: (String) -> Unit = {},
    onDeleteAudioAsset: (String) -> Unit = {},
    onAddAudioClipFromAsset: (StudioAudioAsset, String, String) -> Unit = { _, _, _ -> },
    onSelectAudioClip: (String?) -> Unit = {},
    onUpdateAudioClip: (String, Boolean, (SceneAudioClip) -> SceneAudioClip) -> Unit = { _, _, _ -> },
    onDeleteAudioClip: (String) -> Unit = {},
    onPreviewTtsVoice: (String, String, String, Float, Float) -> Unit = { _, _, _, _, _ -> },
    onGenerateNarrationTts: (String, String, Float, Float) -> Unit = { _, _, _, _ -> },
    onAddDialogueLine: (String, String, String) -> Unit = { _, _, _ -> },
    onUpdateDialogueLine: (String, (DialogueLine) -> DialogueLine) -> Unit = { _, _ -> },
    onDeleteDialogueLine: (String) -> Unit = {},
    onReorderDialogueLine: (String, Int) -> Unit = { _, _ -> },
    onGenerateDialogueTts: (String, String?) -> Unit = { _, _ -> },
    onAssignAssetToDialogue: (String, String) -> Unit = { _, _ -> },
    onUpdateCharacterVoice: (String, String, String, Double, Double) -> Unit = { _, _, _, _, _ -> },
    onRegenerateLipSync: () -> Unit = {},
    onSyncSpeakingToActing: () -> Unit = {},
    onGenerateSoundSuggestions: () -> Unit = {},
    onApplySoundSuggestion: (String) -> Unit = {},
    onDismissSoundSuggestion: (String) -> Unit = {},
    onSelectMotionGfxTab: (MotionGraphicsStudioTab) -> Unit = {},
    onSelectGraphic: (String?) -> Unit = {},
    onAddGraphicTemplate: (String, String?) -> Unit = { _, _ -> },
    onUpdateGraphic: (String, (MotionGraphicElement) -> MotionGraphicElement) -> Unit = { _, _ -> },
    onDuplicateGraphic: (String) -> Unit = {},
    onDeleteGraphic: (String) -> Unit = {},
    onAddGraphicKeyframeAtPlayhead: (String) -> Unit = {},
    onSaveGraphicTemplateToAssets: (String) -> Unit = {},
    onApplyGraphicTemplateAsset: (StudioAsset) -> Unit = {},
    onRequestAiGraphicSuggestions: () -> Unit = {},
    onApplyAiGraphicSuggestion: (String) -> Unit = {},
    onDismissAiGraphicSuggestion: (String) -> Unit = {},
    onGenerateSubtitles: () -> Unit = {},
    onAddSubtitleCue: () -> Unit = {},
    onUpdateSubtitleCue: (String, (SubtitleCue) -> SubtitleCue) -> Unit = { _, _ -> },
    onSplitSubtitleCue: (String) -> Unit = {},
    onDeleteSubtitleCue: (String) -> Unit = {},
    onFitSubtitles: () -> Unit = {},
    onUpdateSubtitleStyle: ((SubtitleStyleConfig) -> SubtitleStyleConfig) -> Unit = {},
    onApplySubtitleStylePreset: (String) -> Unit = {},
    onBuildExportSrt: () -> String = { "" },
    onNotifyMessage: (String) -> Unit = {},
    onRequestImportMapImage: (String) -> Unit = {},
    onUpdateSceneDuration: (String, Double) -> Unit = { _, _ -> },
    onUpdateSceneTransition: (String, SceneTransitionConfig) -> Unit = { _, _ -> },
    onPreviewTransition: (String) -> Unit = {},
    onUpdateSelectedScene: ((StudioScene) -> StudioScene) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(StudioPanel)
            .border(1.dp, StudioBorder)
    ) {
        // Left Vertical Icon Navigation Rail (all 7 Studio sections)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .width(62.dp)
                .fillMaxHeight()
                .background(StudioCharcoal)
                .border(width = 1.dp, color = StudioBorderSubtle)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 6.dp)
        ) {
            SidebarSection.entries.forEach { section ->
                val active = state.activeSidebarSection == section
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) StudioAmberDim else Color.Transparent)
                        .clickable { onSelectSection(section) }
                        .padding(vertical = 8.dp, horizontal = 2.dp)
                        .testTag("sidebar_${section.name.lowercase()}")
                ) {
                    Icon(
                        imageVector = iconForSidebar(section),
                        contentDescription = section.label,
                        tint = if (active) StudioAmber else StudioTextSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = section.label,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                        color = if (active) StudioAmber else StudioTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Active Sidebar Content Panel
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(StudioPanel)
        ) {
            when (state.activeSidebarSection) {
                SidebarSection.SCRIPT -> ScriptSidebarContent(
                    scriptText = state.activeProject?.scriptText.orEmpty(),
                    scriptLanguage = state.activeProject?.scriptLanguage ?: "Bilingual",
                    scenes = state.scenes,
                    aiState = state.aiScriptState,
                    onScriptTextChange = onScriptTextChange,
                    onLanguageChange = onScriptLanguageChange,
                    onLoadSample = onLoadSampleScript,
                    onGenerateClick = onGenerateScenesFromScript,
                    onSyncSceneDuration = onSyncSceneDurationToNarration,
                    onSwapNarration = onSwapNarrationBetweenScenes
                )
                SidebarSection.SCENES -> ScenesSidebarContent(
                    scenes = state.scenes,
                    assets = state.assets,
                    selectedSceneId = state.selectedScene?.id,
                    regeneratingTask = state.regeneratingSceneTask,
                    onSelectScene = onSelectScene,
                    onAddScene = onAddScene,
                    onDeleteScene = onDeleteScene,
                    onMoveScene = onMoveScene,
                    onOpenSceneSuggestions = onOpenSceneSuggestions,
                    onToggleSceneApproved = onToggleSceneApproved,
                    onRegenerateSingleScene = onRegenerateSingleScene,
                    onRegenerateNarration = onRegenerateSceneNarration,
                    onRegenerateVisualDirection = onRegenerateSceneVisualDirection,
                    onRegenerateDialogue = onRegenerateSceneDialogue,
                    onApplyVisualPlan = onApplyScenePlanSuggestions
                )
                SidebarSection.CHARACTERS -> CharacterLibraryPanel(
                    characters = state.characters,
                    selectedCharacter = state.selectedLibraryCharacter,
                    selectedCharacterReferences = state.selectedCharacterReferences,
                    assignedSceneCharacterIds = state.selectedScene
                        ?.effectiveCast()
                        ?.map { it.characterId }
                        ?.toSet()
                        .orEmpty(),
                    onSelectCharacter = onSelectLibraryCharacter,
                    onAddCharacter = { onOpenAddCharacter(null) },
                    onEditCharacter = { onOpenAddCharacter(it) },
                    onRenameCharacter = onRenameCharacter,
                    onRequestDeleteCharacter = onRequestDeleteCharacter,
                    onOpenGenerator = onOpenCharacterGenerator,
                    onAddCharacterToScene = onAddCharacterToScene,
                    onRemoveCharacterFromScene = onRemoveCharacterFromScene,
                    onSetReferenceAsPrimary = onSetReferenceAsPrimary,
                    onDeleteReference = onDeleteCharacterReference
                )
                SidebarSection.LOCATIONS -> LocationsSidebarContent(
                    activeBackground = state.selectedScene?.backgroundPreset.orEmpty(),
                    locationAssets = state.assets.filter { it.category == "Location" || it.category == "Background" },
                    onApplyBackground = { preset ->
                        onUpdateSelectedScene { it.copy(backgroundPreset = preset) }
                    },
                    onAddLocation = { onOpenAddAsset("Location") },
                    onOpenBackgroundGenerator = onOpenBackgroundGenerator
                )
                SidebarSection.ASSETS -> AssetsSidebarContent(
                    assets = state.assets,
                    selectedCategory = state.assetCategoryFilter,
                    onCategoryChange = onAssetCategoryChange,
                    onAddAsset = { onOpenAddAsset(if (state.assetCategoryFilter == "All") "Prop" else state.assetCategoryFilter) },
                    onImportImage = onImportImageAsset,
                    onApplyAsset = onApplyAssetToScene,
                    onDeleteAsset = onDeleteAsset,
                    onOpenBackgroundGenerator = onOpenBackgroundGenerator,
                    onOpenPropGenerator = onOpenPropGenerator
                )
                SidebarSection.AUDIO -> AudioStudioSidebarPanel(
                    project = state.activeProject,
                    scene = state.selectedScene,
                    characters = state.characters,
                    audioAssets = state.audioAssets,
                    activeTab = state.activeAudioTab,
                    availableTtsVoices = state.availableTtsVoices,
                    selectedAudioClipId = state.selectedAudioClipId,
                    previewingAudioAssetId = state.previewingAudioAssetId,
                    audioImportState = state.audioImportState,
                    ttsGenState = state.ttsGenState,
                    soundSuggestionsState = state.soundSuggestionsState,
                    onSelectTab = onSelectAudioTab,
                    onUpdateScene = onUpdateSelectedScene,
                    onRequestImportAudio = onRequestImportAudio,
                    onPreviewAudioAsset = onPreviewAudioAsset,
                    onDeleteAudioAsset = onDeleteAudioAsset,
                    onAddClipFromAsset = onAddAudioClipFromAsset,
                    onSelectAudioClip = onSelectAudioClip,
                    onUpdateAudioClip = onUpdateAudioClip,
                    onDeleteAudioClip = onDeleteAudioClip,
                    onPreviewTtsVoice = onPreviewTtsVoice,
                    onGenerateNarrationTts = onGenerateNarrationTts,
                    onAddDialogueLine = onAddDialogueLine,
                    onUpdateDialogueLine = onUpdateDialogueLine,
                    onDeleteDialogueLine = onDeleteDialogueLine,
                    onReorderDialogueLine = onReorderDialogueLine,
                    onGenerateDialogueTts = onGenerateDialogueTts,
                    onAssignAssetToDialogue = onAssignAssetToDialogue,
                    onUpdateCharacterVoice = onUpdateCharacterVoice,
                    onRegenerateLipSync = onRegenerateLipSync,
                    onSyncSpeakingToActing = onSyncSpeakingToActing,
                    onGenerateSoundSuggestions = onGenerateSoundSuggestions,
                    onApplySoundSuggestion = onApplySoundSuggestion,
                    onDismissSoundSuggestion = onDismissSoundSuggestion,
                    onSyncSceneDurationToNarration = onSyncSceneDurationToNarration
                )
                SidebarSection.MOTION_GRAPHICS -> MotionGraphicsStudioPanel(
                    scenes = state.scenes,
                    scene = state.selectedScene,
                    assets = state.assets,
                    activeTab = state.activeMotionGfxTab,
                    selectedGraphicId = state.selectedGraphicId,
                    graphicSuggestionsState = state.graphicSuggestionsState,
                    lastSubtitleUsedRealAudio = state.lastSubtitleUsedRealAudio,
                    onSelectTab = onSelectMotionGfxTab,
                    onSelectGraphic = onSelectGraphic,
                    onAddGraphicTemplate = onAddGraphicTemplate,
                    onUpdateGraphic = onUpdateGraphic,
                    onDuplicateGraphic = onDuplicateGraphic,
                    onDeleteGraphic = onDeleteGraphic,
                    onAddGraphicKeyframeAtPlayhead = onAddGraphicKeyframeAtPlayhead,
                    onSaveGraphicTemplateToAssets = onSaveGraphicTemplateToAssets,
                    onApplyGraphicTemplateAsset = onApplyGraphicTemplateAsset,
                    onRequestAiGraphicSuggestions = onRequestAiGraphicSuggestions,
                    onApplyAiGraphicSuggestion = onApplyAiGraphicSuggestion,
                    onDismissAiGraphicSuggestion = onDismissAiGraphicSuggestion,
                    onGenerateSubtitles = onGenerateSubtitles,
                    onAddSubtitleCue = onAddSubtitleCue,
                    onUpdateSubtitleCue = onUpdateSubtitleCue,
                    onSplitSubtitleCue = onSplitSubtitleCue,
                    onDeleteSubtitleCue = onDeleteSubtitleCue,
                    onFitSubtitles = onFitSubtitles,
                    onUpdateSubtitleStyle = onUpdateSubtitleStyle,
                    onApplySubtitleStylePreset = onApplySubtitleStylePreset,
                    onBuildExportSrt = onBuildExportSrt,
                    onNotifyMessage = onNotifyMessage,
                    onRequestImportMapImage = onRequestImportMapImage,
                    onSelectScene = onSelectScene,
                    onMoveScene = onMoveScene,
                    onUpdateSceneDuration = onUpdateSceneDuration,
                    onUpdateSceneTransition = onUpdateSceneTransition,
                    onPreviewTransition = onPreviewTransition
                )
            }
        }
    }
}

@Composable
private fun ScriptSidebarContent(
    scriptText: String,
    scriptLanguage: String,
    scenes: List<StudioScene>,
    aiState: AiScriptState,
    onScriptTextChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    onLoadSample: (Boolean) -> Unit,
    onGenerateClick: () -> Unit,
    onSyncSceneDuration: (String) -> Unit,
    onSwapNarration: (Int, Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "DOCUMENTARY SCRIPT",
            style = MaterialTheme.typography.titleSmall,
            color = StudioAmber
        )
        Text(
            text = "Paste Hindi or English documentary storytelling scripts. Gemini breaks them into animated scenes.",
            style = MaterialTheme.typography.bodySmall,
            color = StudioTextSecondary
        )

        // Language Chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            StudioPresets.languages.forEach { lang ->
                FilterChip(
                    selected = scriptLanguage == lang,
                    onClick = { onLanguageChange(lang) },
                    label = { Text(lang, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = StudioAmberDim,
                        selectedLabelColor = StudioAmber
                    )
                )
            }
        }

        // Quick Sample Script Loaders
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = { onLoadSample(false) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                border = BorderStroke(1.dp, StudioBorder),
                modifier = Modifier.weight(1f)
            ) {
                Text("English Sample", style = MaterialTheme.typography.labelSmall, color = StudioTextPrimary)
            }
            OutlinedButton(
                onClick = { onLoadSample(true) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                border = BorderStroke(1.dp, StudioBorder),
                modifier = Modifier.weight(1f)
            ) {
                Text("हिन्दी Sample", style = MaterialTheme.typography.labelSmall, color = StudioTeal)
            }
        }

        // Script Text Input
        OutlinedTextField(
            value = scriptText,
            onValueChange = onScriptTextChange,
            placeholder = {
                Text(
                    "SCENE 1 - EXT. HIMALAYAN PASS...\nया अपना हिन्दी डॉक्यूमेंट्री स्क्रिप्ट यहाँ पेस्ट करें...",
                    style = MaterialTheme.typography.bodySmall,
                    color = StudioTextMuted
                )
            },
            textStyle = MaterialTheme.typography.bodySmall.copy(color = StudioTextPrimary),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = StudioCharcoal,
                unfocusedContainerColor = StudioCharcoal,
                focusedBorderColor = StudioAmber,
                unfocusedBorderColor = StudioBorder
            ),
            minLines = 7,
            maxLines = 12,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("script_input_field")
        )

        if (aiState is AiScriptState.Error) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = aiState.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(8.dp)
                )
            }
        } else if (aiState is AiScriptState.Success) {
            Surface(
                color = StudioTealDim,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Generated ${aiState.sceneCount} scenes & identified ${aiState.characterCount} cast characters. Tap Save to sync.",
                    style = MaterialTheme.typography.bodySmall,
                    color = StudioTeal,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        Button(
            onClick = onGenerateClick,
            enabled = aiState !is AiScriptState.Generating,
            colors = ButtonDefaults.buttonColors(
                containerColor = StudioAmber,
                contentColor = StudioVoid
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("generate_scenes_button")
        ) {
            if (aiState is AiScriptState.Generating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = StudioVoid
                )
                Spacer(Modifier.width(8.dp))
                Text("Analyzing Script...", style = MaterialTheme.typography.labelLarge)
            } else {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Analyze Script & Plan Scenes",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (scenes.isNotEmpty()) {
            NarrationSyncDiagnosticCard(
                scenes = scenes,
                language = scriptLanguage,
                onSyncSceneDuration = onSyncSceneDuration,
                onSwapNarration = onSwapNarration
            )
        }
    }
}

@Composable
private fun ScenesSidebarContent(
    scenes: List<StudioScene>,
    assets: List<StudioAsset>,
    selectedSceneId: String?,
    regeneratingTask: String?,
    onSelectScene: (String) -> Unit,
    onAddScene: () -> Unit,
    onDeleteScene: (String) -> Unit,
    onMoveScene: (String, Int) -> Unit,
    onOpenSceneSuggestions: () -> Unit,
    onToggleSceneApproved: (String) -> Unit,
    onRegenerateSingleScene: (String) -> Unit,
    onRegenerateNarration: () -> Unit,
    onRegenerateVisualDirection: () -> Unit,
    onRegenerateDialogue: () -> Unit,
    onApplyVisualPlan: () -> Unit
) {
    val assetById = remember(assets) { assets.associateBy { it.id } }
    val selectedScene = remember(scenes, selectedSceneId) { scenes.firstOrNull { it.id == selectedSceneId } }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = "SCENES (${scenes.size})",
                style = MaterialTheme.typography.titleSmall,
                color = StudioAmber
            )
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = StudioTealDim,
                    border = BorderStroke(1.dp, StudioTeal.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onOpenSceneSuggestions)
                        .testTag("scenes_ai_visuals_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI Scene Visuals", tint = StudioTeal, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("AI Visuals", style = MaterialTheme.typography.labelSmall, color = StudioTeal)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = StudioAmberDim,
                    border = BorderStroke(1.dp, StudioAmber.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onAddScene)
                        .testTag("add_scene_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Scene", tint = StudioAmber, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("Add", style = MaterialTheme.typography.labelSmall, color = StudioAmber)
                    }
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (selectedScene != null) {
                item(key = "scene_intelligence_controls") {
                    SceneIntelligenceControlCard(
                        scene = selectedScene,
                        regeneratingTask = regeneratingTask,
                        onToggleApproved = { onToggleSceneApproved(selectedScene.id) },
                        onRegenerateWholeScene = { onRegenerateSingleScene(selectedScene.id) },
                        onRegenerateNarration = onRegenerateNarration,
                        onRegenerateVisualDirection = onRegenerateVisualDirection,
                        onRegenerateDialogue = onRegenerateDialogue,
                        onApplyVisualPlan = onApplyVisualPlan
                    )
                }
            }
            itemsIndexed(scenes, key = { _, scene -> scene.id }) { index, scene ->
                val isSelected = scene.id == selectedSceneId
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) StudioElevated else StudioCharcoal,
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) StudioAmber else StudioBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectScene(scene.id) }
                        .testTag("scene_card_${index + 1}")
                ) {
                    Column(modifier = Modifier.padding(9.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Scene Thumbnail (supports AI asset:<id>, content URI, and bundled presets)
                            Box(
                                modifier = Modifier
                                    .size(width = 56.dp, height = 34.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(StudioVoid)
                                    .border(0.5.dp, StudioBorder, RoundedCornerShape(5.dp))
                            ) {
                                val bgAssetId = SceneCompositor.resolveBackgroundAssetId(scene.backgroundPreset)
                                val bgAssetBase64 = bgAssetId?.let { assetById[it]?.imageBase64 }.orEmpty()
                                val decodedAiBg = remember(bgAssetBase64) {
                                    if (bgAssetBase64.isNotBlank()) {
                                        CharacterBitmapCodec.decodeBase64ToBitmap(bgAssetBase64)
                                    } else {
                                        null
                                    }
                                }
                                val bgRes = backgroundDrawableForPreset(scene.backgroundPreset)
                                val safeUri = if (scene.backgroundPreset.startsWith("uri:")) {
                                    safeMediaUriOrNull(scene.backgroundPreset.removePrefix("uri:"))
                                } else {
                                    null
                                }
                                if (decodedAiBg != null) {
                                    Image(
                                        bitmap = decodedAiBg.asImageBitmap(),
                                        contentDescription = scene.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else if (safeUri != null) {
                                    AsyncImage(
                                        model = safeUri,
                                        contentDescription = scene.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else if (bgRes != null) {
                                    Image(
                                        painter = painterResource(bgRes),
                                        contentDescription = scene.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .background(StudioVoid.copy(alpha = 0.8f))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "#${index + 1}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = StudioAmber
                                    )
                                }
                            }

                            Spacer(Modifier.width(8.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Text(
                                        text = scene.title,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = StudioTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (scene.scenePlan.approved) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = StudioEmerald.copy(alpha = 0.2f),
                                            border = BorderStroke(0.5.dp, StudioEmerald)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Lock,
                                                    contentDescription = "Approved",
                                                    tint = StudioEmerald,
                                                    modifier = Modifier.size(9.dp)
                                                )
                                                Spacer(Modifier.width(2.dp))
                                                Text(
                                                    text = "OK",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                                    color = StudioEmerald
                                                )
                                            }
                                        }
                                    }
                                }
                                Text(
                                    text = "${String.format("%.1f", scene.durationSec)}s · ${scene.cameraMovement}" +
                                        if (scene.audioClips.isNotEmpty()) " · ${scene.audioClips.size} audio" else "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StudioTeal,
                                    maxLines = 1
                                )
                            }
                        }

                        if (scene.description.isNotBlank()) {
                            Spacer(Modifier.height(5.dp))
                            Text(
                                text = scene.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = StudioTextSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (scene.visualSummary.isNotBlank()) {
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = "Visual: ${scene.visualSummary}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                color = StudioTextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(
                                onClick = { onMoveScene(scene.id, -1) },
                                enabled = index > 0,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.ArrowUpward,
                                    contentDescription = "Move scene up",
                                    tint = if (index > 0) StudioTextSecondary else StudioTextMuted,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            IconButton(
                                onClick = { onMoveScene(scene.id, 1) },
                                enabled = index < scenes.lastIndex,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.ArrowDownward,
                                    contentDescription = "Move scene down",
                                    tint = if (index < scenes.lastIndex) StudioTextSecondary else StudioTextMuted,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            IconButton(
                                onClick = { onDeleteScene(scene.id) },
                                enabled = scenes.size > 1,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Delete scene",
                                    tint = if (scenes.size > 1) StudioCoral else StudioTextMuted,
                                    modifier = Modifier.size(15.dp)
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
private fun LocationsSidebarContent(
    activeBackground: String,
    locationAssets: List<StudioAsset>,
    onApplyBackground: (String) -> Unit,
    onAddLocation: () -> Unit,
    onOpenBackgroundGenerator: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = "LOCATIONS & BG",
                style = MaterialTheme.typography.titleSmall,
                color = StudioAmber
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = StudioAmberDim,
                    border = BorderStroke(1.dp, StudioAmber.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onOpenBackgroundGenerator)
                        .testTag("locations_ai_gen_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI Generate Background", tint = StudioAmber, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("AI Gen", style = MaterialTheme.typography.labelSmall, color = StudioAmber)
                    }
                }
                TextButton(onClick = onAddLocation, contentPadding = PaddingValues(horizontal = 5.dp)) {
                    Text("+ Add", style = MaterialTheme.typography.labelSmall, color = StudioTeal)
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (locationAssets.isNotEmpty()) {
                item {
                    Text(
                        text = "AI & CUSTOM SAVED BACKGROUNDS",
                        style = MaterialTheme.typography.labelSmall,
                        color = StudioTextMuted,
                        modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
                    )
                }
                items(locationAssets, key = { it.id }) { loc ->
                    val target = when {
                        loc.imageBase64.isNotBlank() && loc.id.isNotBlank() -> "asset:${loc.id}"
                        loc.localUri.isNotBlank() -> "uri:${loc.localUri}"
                        else -> loc.presetKey.ifBlank { "himalayas" }
                    }
                    val selected = activeBackground == target
                    val decodedThumb = remember(loc.imageBase64) {
                        if (loc.imageBase64.isNotBlank()) {
                            CharacterBitmapCodec.decodeBase64ToBitmap(loc.imageBase64)
                        } else {
                            null
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (selected) StudioElevated else StudioCharcoal,
                        border = BorderStroke(1.dp, if (selected) StudioTeal else StudioBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onApplyBackground(target) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 64.dp, height = 38.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(StudioVoid)
                            ) {
                                if (decodedThumb != null) {
                                    Image(
                                        bitmap = decodedThumb.asImageBitmap(),
                                        contentDescription = loc.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    val safeUri = safeMediaUriOrNull(loc.localUri)
                                    val res = backgroundDrawableForPreset(loc.presetKey)
                                    if (safeUri != null) {
                                        AsyncImage(
                                            model = safeUri,
                                            contentDescription = loc.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else if (res != null) {
                                        Image(
                                            painter = painterResource(res),
                                            contentDescription = loc.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = loc.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = StudioTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (selected) {
                                        "Active backdrop"
                                    } else if (loc.imageBase64.isNotBlank()) {
                                        "AI 16:9 · Tap to apply"
                                    } else {
                                        loc.notes.ifBlank { "Tap to apply to scene" }
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) StudioTeal else StudioTextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "BUILT-IN DOCUMENTARY SETS",
                    style = MaterialTheme.typography.labelSmall,
                    color = StudioTextMuted,
                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                )
            }
            items(StudioPresets.backgrounds) { (key, title) ->
                val selected = activeBackground == key
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (selected) StudioElevated else StudioCharcoal,
                    border = BorderStroke(1.dp, if (selected) StudioTeal else StudioBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onApplyBackground(key) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 64.dp, height = 38.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(StudioVoid)
                        ) {
                            val res = backgroundDrawableForPreset(key)
                            if (res != null) {
                                Image(
                                    painter = painterResource(res),
                                    contentDescription = title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(title, style = MaterialTheme.typography.labelLarge, color = StudioTextPrimary)
                            Text(
                                text = if (selected) "Active backdrop" else "Tap to apply to scene",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) StudioTeal else StudioTextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AssetsSidebarContent(
    assets: List<StudioAsset>,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    onAddAsset: () -> Unit,
    onImportImage: (Uri) -> Unit,
    onApplyAsset: (StudioAsset) -> Unit,
    onDeleteAsset: (String) -> Unit,
    onOpenBackgroundGenerator: () -> Unit,
    onOpenPropGenerator: () -> Unit
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onImportImage(uri)
        }
    }

    val filtered = if (selectedCategory == "All") assets else assets.filter { it.category == selectedCategory }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "ASSET LIBRARY",
                style = MaterialTheme.typography.titleSmall,
                color = StudioAmber
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("import_image_asset_button")
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Import Image", tint = StudioTeal, modifier = Modifier.size(17.dp))
                }
                IconButton(
                    onClick = onAddAsset,
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("add_asset_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Asset", tint = StudioAmber, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Quick AI Asset Generators Bar
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 2.dp)
        ) {
            OutlinedButton(
                onClick = onOpenBackgroundGenerator,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                border = BorderStroke(1.dp, StudioAmber.copy(alpha = 0.7f)),
                modifier = Modifier
                    .weight(1f)
                    .testTag("assets_ai_bg_button")
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = StudioAmber, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text("AI BG", style = MaterialTheme.typography.labelSmall, color = StudioAmber)
            }
            OutlinedButton(
                onClick = onOpenPropGenerator,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                border = BorderStroke(1.dp, StudioTeal.copy(alpha = 0.7f)),
                modifier = Modifier
                    .weight(1f)
                    .testTag("assets_ai_prop_button")
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = StudioTeal, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text("AI Prop", style = MaterialTheme.typography.labelSmall, color = StudioTeal)
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp)
        ) {
            StudioPresets.assetCategories.forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { onCategoryChange(cat) },
                    label = { Text(cat, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = StudioAmberDim,
                        selectedLabelColor = StudioAmber
                    )
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filtered, key = { it.id }) { asset ->
                val decodedThumb = remember(asset.imageBase64) {
                    if (asset.imageBase64.isNotBlank()) {
                        CharacterBitmapCodec.decodeBase64ToBitmap(asset.imageBase64)
                    } else {
                        null
                    }
                }
                val safeUri = safeMediaUriOrNull(asset.localUri)
                val presetRes = backgroundDrawableForPreset(asset.presetKey)
                val hasThumb = decodedThumb != null || safeUri != null || presetRes != null

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StudioCharcoal,
                    border = BorderStroke(1.dp, StudioBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onApplyAsset(asset) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(9.dp)
                    ) {
                        if (hasThumb) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(StudioVoid)
                                    .border(0.5.dp, StudioBorder, RoundedCornerShape(6.dp))
                            ) {
                                if (decodedThumb != null) {
                                    Image(
                                        bitmap = decodedThumb.asImageBitmap(),
                                        contentDescription = asset.name,
                                        contentScale = if (asset.category == "Prop") ContentScale.Fit else ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else if (safeUri != null) {
                                    AsyncImage(
                                        model = safeUri,
                                        contentDescription = asset.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else if (presetRes != null) {
                                    Image(
                                        painter = painterResource(presetRes),
                                        contentDescription = asset.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            Spacer(Modifier.width(9.dp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Surface(
                                    color = StudioTealDim,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = asset.category.uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                        color = StudioTeal,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                                if (asset.hasTransparency) {
                                    Surface(
                                        color = StudioAmberDim,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "PNG α",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                            color = StudioAmber,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                } else if (asset.imageBase64.isNotBlank()) {
                                    Surface(
                                        color = StudioAmberDim,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "AI",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                            color = StudioAmber,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = asset.durationLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = StudioTextMuted
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = asset.name,
                                style = MaterialTheme.typography.labelLarge,
                                color = StudioTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (asset.notes.isNotBlank()) {
                                Text(
                                    text = asset.notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StudioTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        IconButton(
                            onClick = { onDeleteAsset(asset.id) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete asset", tint = StudioCoral, modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MotionGraphicsSidebarContent(
    scene: StudioScene?,
    onUpdateScene: ((StudioScene) -> StudioScene) -> Unit
) {
    if (scene == null) return
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
    ) {
        Text("MOTION GRAPHICS", style = MaterialTheme.typography.titleSmall, color = StudioAmber)
        Text(
            text = "Overlay editorial documentary titles, date stamps, lower thirds, and archival map callouts live on canvas.",
            style = MaterialTheme.typography.bodySmall,
            color = StudioTextSecondary
        )

        Text("Overlay Text", style = MaterialTheme.typography.labelMedium, color = StudioTextSecondary)
        OutlinedTextField(
            value = scene.motionGraphicTitle,
            onValueChange = { txt -> onUpdateScene { it.copy(motionGraphicTitle = txt.take(120)) } },
            placeholder = { Text("e.g. CHAPTER I · 1842", color = StudioTextMuted) },
            textStyle = MaterialTheme.typography.bodySmall.copy(color = StudioTextPrimary),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = StudioCharcoal,
                unfocusedContainerColor = StudioCharcoal,
                focusedBorderColor = StudioAmber,
                unfocusedBorderColor = StudioBorder
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Graphic Template", style = MaterialTheme.typography.labelMedium, color = StudioTextSecondary)
        StudioPresets.motionGraphicStyles.forEach { styleName ->
            val selected = scene.motionGraphicStyle == styleName
            Surface(
                shape = RoundedCornerShape(7.dp),
                color = if (selected) StudioAmberDim else StudioCharcoal,
                border = BorderStroke(1.dp, if (selected) StudioAmber else StudioBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onUpdateScene { it.copy(motionGraphicStyle = styleName) } }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = styleName,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) StudioAmber else StudioTextPrimary
                    )
                    if (selected) {
                        Text("ACTIVE", style = MaterialTheme.typography.labelSmall, color = StudioTeal)
                    }
                }
            }
        }
    }
}
