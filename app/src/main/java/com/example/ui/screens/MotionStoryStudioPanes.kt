package com.example.ui.screens

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.compositor.SceneLayerType
import com.example.compositor.SelectedStageTarget
import com.example.data.CompactWorkspaceTab
import com.example.data.StudioAsset
import com.example.data.StudioCharacter
import com.example.data.StudioScene
import com.example.ui.components.LeftSidebarPane
import com.example.ui.components.MultitrackTimeline
import com.example.ui.components.PreviewCanvasPane
import com.example.ui.components.RightInspectorPanel
import com.example.viewmodel.StudioUiState
import com.example.viewmodel.StudioViewModel

@Composable
internal fun StudioLeftSidebar(
    state: StudioUiState,
    viewModel: StudioViewModel,
    onOpenAddCharacter: (StudioCharacter?) -> Unit,
    onRenameCharacter: (StudioCharacter) -> Unit,
    onRequestDeleteCharacter: (StudioCharacter) -> Unit,
    onOpenCharacterGenerator: (StudioCharacter) -> Unit,
    onOpenAddAsset: (String) -> Unit,
    onOpenBackgroundGenerator: () -> Unit,
    onOpenPropGenerator: () -> Unit,
    onOpenSceneSuggestions: () -> Unit,
    onRequestImportAudio: (String, String?) -> Unit,
    onRequestImportMapImage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LeftSidebarPane(
        state = state,
        onSelectSection = viewModel::setSidebarSection,
        onScriptTextChange = viewModel::updateScriptText,
        onScriptLanguageChange = viewModel::updateScriptLanguage,
        onLoadSampleScript = viewModel::loadSampleScript,
        onGenerateScenesFromScript = viewModel::generateScenesFromScript,
        onSelectScene = viewModel::selectScene,
        onAddScene = viewModel::addScene,
        onDeleteScene = viewModel::deleteScene,
        onMoveScene = viewModel::moveScene,
        onSelectLibraryCharacter = viewModel::selectLibraryCharacter,
        onOpenAddCharacter = onOpenAddCharacter,
        onRenameCharacter = onRenameCharacter,
        onRequestDeleteCharacter = onRequestDeleteCharacter,
        onOpenCharacterGenerator = onOpenCharacterGenerator,
        onAddCharacterToScene = viewModel::addCharacterToSelectedScene,
        onRemoveCharacterFromScene = viewModel::removeCharacterFromSelectedScene,
        onSetReferenceAsPrimary = viewModel::setReferenceAsPrimary,
        onDeleteCharacterReference = viewModel::deleteCharacterReference,
        onOpenAddAsset = onOpenAddAsset,
        onImportImageAsset = { uri ->
            viewModel.saveAsset(
                StudioAsset(
                    name = "Imported Frame",
                    category = "Image",
                    tags = "imported,photo",
                    notes = "Imported via Android Photo Picker",
                    presetKey = "custom_photo",
                    localUri = uri.toString(),
                    durationLabel = "Image"
                )
            )
        },
        onApplyAssetToScene = viewModel::applyAssetToSelectedScene,
        onDeleteAsset = viewModel::deleteAsset,
        onAssetCategoryChange = viewModel::setAssetCategoryFilter,
        onOpenBackgroundGenerator = onOpenBackgroundGenerator,
        onOpenPropGenerator = onOpenPropGenerator,
        onOpenSceneSuggestions = onOpenSceneSuggestions,
        onToggleSceneApproved = viewModel::toggleSceneApproved,
        onRegenerateSingleScene = viewModel::regenerateSingleScene,
        onRegenerateSceneNarration = viewModel::regenerateSelectedSceneNarration,
        onRegenerateSceneVisualDirection = viewModel::regenerateSelectedSceneVisualDirection,
        onRegenerateSceneDialogue = viewModel::regenerateSelectedSceneDialogue,
        onApplyScenePlanSuggestions = { viewModel.applyScenePlanSuggestionsToSelectedScene() },
        onSyncSceneDurationToNarration = viewModel::autoSyncSceneDurationToNarration,
        onSwapNarrationBetweenScenes = viewModel::swapNarrationBetweenScenes,
        onSelectAudioTab = viewModel::setAudioStudioTab,
        onRequestImportAudio = onRequestImportAudio,
        onPreviewAudioAsset = viewModel::previewAudioAsset,
        onDeleteAudioAsset = viewModel::deleteAudioAsset,
        onAddAudioClipFromAsset = { asset, trackType, scopeStr ->
            viewModel.addAudioClipFromAsset(asset, trackType, scope = scopeStr)
        },
        onSelectAudioClip = viewModel::selectAudioClip,
        onUpdateAudioClip = viewModel::updateAudioClip,
        onDeleteAudioClip = viewModel::deleteAudioClip,
        onPreviewTtsVoice = viewModel::previewTtsVoice,
        onGenerateNarrationTts = viewModel::generateTtsForSelectedSceneNarration,
        onAddDialogueLine = viewModel::addDialogueLine,
        onUpdateDialogueLine = { lineId, transform ->
            viewModel.updateDialogueLine(lineId, recordUndo = true, transform = transform)
        },
        onDeleteDialogueLine = viewModel::deleteDialogueLine,
        onReorderDialogueLine = viewModel::reorderDialogueLine,
        onGenerateDialogueTts = viewModel::generateTtsForDialogueLine,
        onAssignAssetToDialogue = viewModel::assignAudioAssetToDialogueLine,
        onUpdateCharacterVoice = viewModel::updateCharacterVoiceSettings,
        onRegenerateLipSync = viewModel::regenerateLipSyncForSelectedScene,
        onSyncSpeakingToActing = viewModel::syncDialogueSpeakingToActingCues,
        onGenerateSoundSuggestions = viewModel::generateAiSoundSuggestionsForSelectedScene,
        onApplySoundSuggestion = viewModel::applyAiSoundSuggestion,
        onDismissSoundSuggestion = viewModel::dismissAiSoundSuggestion,
        onSelectMotionGfxTab = viewModel::setMotionGfxTab,
        onSelectGraphic = viewModel::selectGraphic,
        onAddGraphicTemplate = viewModel::addGraphicTemplate,
        onUpdateGraphic = { gid, transform -> viewModel.updateMotionGraphic(gid, true, transform) },
        onDuplicateGraphic = viewModel::duplicateMotionGraphic,
        onDeleteGraphic = viewModel::deleteMotionGraphic,
        onAddGraphicKeyframeAtPlayhead = viewModel::addGraphicKeyframeAtPlayhead,
        onSaveGraphicTemplateToAssets = viewModel::saveGraphicTemplateToAssets,
        onApplyGraphicTemplateAsset = viewModel::applyGraphicTemplateAsset,
        onRequestAiGraphicSuggestions = viewModel::requestAiGraphicSuggestions,
        onApplyAiGraphicSuggestion = viewModel::applyAiGraphicSuggestion,
        onDismissAiGraphicSuggestion = viewModel::dismissAiGraphicSuggestion,
        onGenerateSubtitles = viewModel::generateSubtitlesForActiveScene,
        onAddSubtitleCue = viewModel::addSubtitleCue,
        onUpdateSubtitleCue = viewModel::updateSubtitleCue,
        onSplitSubtitleCue = viewModel::splitSubtitleCue,
        onDeleteSubtitleCue = viewModel::deleteSubtitleCue,
        onFitSubtitles = viewModel::fitActiveSceneSubtitles,
        onUpdateSubtitleStyle = viewModel::updateSubtitleStyle,
        onApplySubtitleStylePreset = viewModel::applySubtitleStylePreset,
        onBuildExportSrt = viewModel::buildExportSrt,
        onNotifyMessage = viewModel::postSnackbar,
        onRequestImportMapImage = onRequestImportMapImage,
        onUpdateSceneDuration = viewModel::updateSceneDurationById,
        onUpdateSceneTransition = viewModel::updateSceneTransition,
        onPreviewTransition = viewModel::previewTransitionForScene,
        onUpdateSelectedScene = { transform -> viewModel.updateSelectedScene(true, transform) },
        modifier = modifier
    )
}

@Composable
internal fun StudioPreviewStage(
    state: StudioUiState,
    activePlaybackScene: StudioScene?,
    sceneProgress: Double,
    activeSceneTimeSec: Double,
    viewModel: StudioViewModel,
    onSelectStageTarget: (SelectedStageTarget?) -> Unit,
    modifier: Modifier = Modifier
) {
    PreviewCanvasPane(
        scene = activePlaybackScene,
        scenes = state.scenes,
        sceneNumber = state.selectedSceneIndex + 1,
        totalScenes = state.scenes.size.coerceAtLeast(1),
        sceneProgress = sceneProgress,
        sceneTimeSec = activeSceneTimeSec,
        characters = state.characters,
        characterReferencesByCharId = state.characterReferencesByCharId,
        assets = state.assets,
        selectedStageTarget = state.selectedStageTarget,
        isPlaying = state.isPlaying,
        playheadSec = state.playheadSec,
        totalDurationSec = state.totalDurationSec,
        fps = state.activeProject?.fps ?: 24L,
        canvasZoom = state.canvasZoom,
        mutedTracks = state.mutedTracks,
        showFramingGuides = state.showFramingGuides,
        onSelectStageTarget = onSelectStageTarget,
        onBeginTransformGesture = viewModel::beginTransformGestureSnapshot,
        onDragStageTarget = viewModel::dragStageTarget,
        onScaleStageTarget = viewModel::scaleStageTarget,
        onRotateStageTarget = viewModel::rotateStageTarget,
        onPreviousScene = viewModel::previousScene,
        onTogglePlayPause = viewModel::togglePlayPause,
        onStop = viewModel::stopPlayback,
        onNextScene = viewModel::nextScene,
        onZoomOut = { viewModel.adjustCanvasZoom(-0.15f) },
        onZoomReset = viewModel::resetCanvasZoom,
        onZoomIn = { viewModel.adjustCanvasZoom(0.15f) },
        modifier = modifier
    )
}

@Composable
internal fun StudioInspectorPane(
    state: StudioUiState,
    viewModel: StudioViewModel,
    onImportVisualElement: (Uri, SceneLayerType) -> Unit,
    onOpenCharacterGenerator: (StudioCharacter) -> Unit,
    onOpenBackgroundGenerator: () -> Unit,
    onOpenPropGenerator: (SceneLayerType) -> Unit,
    onOpenSceneSuggestions: () -> Unit,
    modifier: Modifier = Modifier
) {
    RightInspectorPanel(
        scene = state.selectedScene,
        sceneTimeSec = state.selectedSceneLocalTimeSec,
        isPlaying = state.isPlaying,
        characters = state.characters,
        characterReferencesByCharId = state.characterReferencesByCharId,
        assets = state.assets,
        selectedCastCharacterId = state.selectedCastCharacterId,
        selectedStageTarget = state.selectedStageTarget,
        activeSection = state.activeInspectorSection,
        onSelectSection = viewModel::setInspectorSection,
        onSelectStageTarget = viewModel::selectStageTarget,
        onSelectCastCharacter = viewModel::selectCastCharacter,
        onAddCharacterToScene = viewModel::addCharacterToSelectedScene,
        onRemoveCharacterFromScene = viewModel::removeCharacterFromSelectedScene,
        onUpdateCastMember = { charId, recordUndo, transform ->
            viewModel.updateCastMemberInSelectedScene(charId, recordUndo, transform)
        },
        onNudgeCharacterZIndex = viewModel::nudgeCharacterZIndex,
        onToggleLayerVisibility = viewModel::toggleSceneLayerVisibility,
        onMoveLayerOrder = viewModel::moveSceneLayerOrder,
        onUpdateLayerOpacity = viewModel::updateSceneLayerOpacity,
        onUpdateLayerParallax = viewModel::updateSceneLayerParallax,
        onAddVisualElementFromAsset = viewModel::addVisualElementFromAsset,
        onImportVisualElement = onImportVisualElement,
        onRemoveVisualElement = viewModel::removeVisualElementFromSelectedScene,
        onUpdateVisualElement = { elementId, recordUndo, transform ->
            viewModel.updateVisualElementInSelectedScene(elementId, recordUndo, transform)
        },
        onNudgeElementZIndex = viewModel::nudgeElementZIndex,
        onOpenCharacterGenerator = onOpenCharacterGenerator,
        onOpenBackgroundGenerator = onOpenBackgroundGenerator,
        onOpenPropGenerator = onOpenPropGenerator,
        onOpenSceneSuggestions = onOpenSceneSuggestions,
        onSeekWithinScene = viewModel::seekWithinSelectedScene,
        onTogglePlayPause = viewModel::togglePlayPause,
        onUpsertKeyframe = viewModel::upsertKeyframeAtCurrentTime,
        onUpdateKeyframeEasing = viewModel::updateKeyframeEasing,
        onDeleteKeyframe = viewModel::deleteKeyframe,
        onClearTargetKeyframes = viewModel::clearTargetKeyframes,
        onJumpPrevKeyframe = viewModel::jumpToPreviousKeyframe,
        onJumpNextKeyframe = viewModel::jumpToNextKeyframe,
        onApplyCharacterTransformPreset = viewModel::applyCharacterTransformPreset,
        onApplyCameraPreset = viewModel::applyCameraKeyframePreset,
        onUpsertActingCue = viewModel::upsertActingCueAtCurrentTime,
        onDeleteActingCue = viewModel::deleteActingCue,
        showFramingGuides = state.showFramingGuides,
        onToggleFramingGuides = viewModel::toggleFramingGuides,
        onApplyCinematicCameraPreset = viewModel::applyCinematicCameraPreset,
        onResetCameraToFit = viewModel::resetCameraToFit,
        onApplyLightingPreset = viewModel::applyLightingPreset,
        onUpdateSceneCinematics = { recordUndo, transform ->
            viewModel.updateSceneCinematics(recordUndo, transform)
        },
        onUpdateScene = { recordUndo, transform ->
            viewModel.updateSelectedScene(recordUndo, transform)
        },
        modifier = modifier
    )
}

@Composable
internal fun StudioBottomTimeline(
    state: StudioUiState,
    viewModel: StudioViewModel,
    switchToPanelsOnCompact: Boolean,
    modifier: Modifier = Modifier
) {
    MultitrackTimeline(
        scenes = state.scenes,
        characters = state.characters,
        selectedSceneId = state.selectedScene?.id,
        playheadSec = state.playheadSec,
        totalDurationSec = state.totalDurationSec,
        zoomDpPerSec = state.timelineZoomDpPerSec,
        mutedTracks = state.mutedTracks,
        onSelectScene = viewModel::selectScene,
        onSeek = viewModel::seekToTime,
        onToggleMute = viewModel::toggleTrackMute,
        onZoomOut = { viewModel.adjustTimelineZoom(-6f) },
        onZoomIn = { viewModel.adjustTimelineZoom(6f) },
        projectAudioClips = state.activeProject?.projectAudioClips.orEmpty(),
        audioAssets = state.audioAssets,
        selectedAudioClipId = state.selectedAudioClipId,
        onSelectAudioClip = viewModel::selectAudioClip,
        selectedGraphicId = state.selectedGraphicId,
        onSelectGraphic = { sceneId, gfxId ->
            viewModel.selectGraphicInScene(sceneId, gfxId)
            if (switchToPanelsOnCompact) {
                viewModel.setCompactWorkspaceTab(CompactWorkspaceTab.PANELS)
            }
        },
        onSelectTransitionScene = { sceneId ->
            viewModel.selectTransitionForScene(sceneId)
            if (switchToPanelsOnCompact) {
                viewModel.setCompactWorkspaceTab(CompactWorkspaceTab.PANELS)
            }
        },
        modifier = modifier
    )
}
