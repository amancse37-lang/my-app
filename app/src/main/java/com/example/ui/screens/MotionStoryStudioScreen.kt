package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.compositor.SceneLayerType
import com.example.compositor.SelectedStageTarget
import com.example.data.InspectorSection
import com.example.data.MotionGraphicsStudioTab
import com.example.data.SidebarSection
import com.example.ui.components.TheaterPreviewOverlay
import com.example.ui.components.TopStudioBar
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.StudioVoid
import com.example.viewmodel.StudioViewModel

@Composable
fun MotionStoryStudioScreen(
    viewModel: StudioViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val dialogState = rememberStudioDialogState()

    // Zero-permission Android SAF Audio File Picker (MP3 / WAV / M4A / AAC / OGG)
    var pendingAudioImportTarget by remember { mutableStateOf<Pair<String, String?>?>(null) }
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        val target = pendingAudioImportTarget
        pendingAudioImportTarget = null
        if (uri != null && target != null) {
            viewModel.importAudioAssetFromUri(
                uri = uri,
                category = target.first,
                language = state.activeProject?.scriptLanguage ?: "English",
                dialogueLineId = target.second
            )
        }
    }
    val onRequestImportAudio: (String, String?) -> Unit = { category, dialogueLineId ->
        pendingAudioImportTarget = category to dialogueLineId
        audioPickerLauncher.launch(arrayOf("audio/*"))
    }

    // Zero-permission Android Photo Picker for Documentary Map base image import
    var pendingMapGraphicId by remember { mutableStateOf<String?>(null) }
    val mapImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val gid = pendingMapGraphicId
        pendingMapGraphicId = null
        if (uri != null && gid != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Temporary Photo Picker grant still works in-session.
            }
            viewModel.attachMapImageToGraphic(gid, uri.toString())
        }
    }
    val onRequestImportMapImage: (String) -> Unit = { graphicId ->
        pendingMapGraphicId = graphicId
        mapImagePickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    LaunchedEffect(state.snackbarMessage) {
        val msg = state.snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    LaunchedEffect(state.errorMessage) {
        val err = state.errorMessage
        if (err != null) {
            snackbarHostState.showSnackbar(err)
            viewModel.clearError()
        }
    }

    val (activePlaybackScene, sceneProgress, activeSceneTimeSec) = remember(
        state.scenes,
        state.playheadSec,
        state.selectedSceneId,
        state.isPlaying
    ) {
        if (state.isPlaying) {
            val (sceneAtPlayhead, prog) = state.sceneAtTime(state.playheadSec)
            val start = sceneAtPlayhead?.let { state.startTimeOfScene(it.id) } ?: 0.0
            val dur = sceneAtPlayhead?.durationSec?.coerceAtLeast(0.1) ?: 0.1
            val localT = (state.playheadSec - start).coerceIn(0.0, dur)
            Triple(sceneAtPlayhead, prog, localT)
        } else {
            val selected = state.selectedScene
            if (selected != null) {
                val start = state.startTimeOfScene(selected.id)
                val dur = selected.durationSec.coerceAtLeast(0.1)
                val localT = (state.playheadSec - start).coerceIn(0.0, dur)
                val progress = (localT / dur).coerceIn(0.0, 1.0)
                Triple(selected, progress, localT)
            } else {
                Triple(null, 0.0, 0.0)
            }
        }
    }

    val onSelectStageTargetSynced: (SelectedStageTarget?) -> Unit = { target ->
        viewModel.selectStageTarget(target)
        if (state.activeInspectorSection != InspectorSection.ANIMATE) {
            when (target) {
                is SelectedStageTarget.CharacterTarget -> viewModel.setInspectorSection(InspectorSection.CHARACTER)
                is SelectedStageTarget.ElementTarget -> viewModel.setInspectorSection(InspectorSection.LAYERS)
                is SelectedStageTarget.GraphicTarget -> {
                    viewModel.setSidebarSection(SidebarSection.MOTION_GRAPHICS)
                    val gfx = state.selectedScene?.motionGraphics?.firstOrNull { it.graphicId == target.graphicId }
                    val tab = when (gfx?.category?.uppercase()) {
                        "INFOGRAPHIC" -> MotionGraphicsStudioTab.INFOGRAPHICS
                        "MAP" -> MotionGraphicsStudioTab.MAPS
                        else -> MotionGraphicsStudioTab.TEXT_GFX
                    }
                    viewModel.setMotionGfxTab(tab)
                }
                null -> Unit
            }
        }
    }

    val onImportVisualElementWithPermission: (Uri, SceneLayerType) -> Unit = { uri, targetLayer ->
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // Photo Picker temporary grants do not always support persistable permissions; still usable in session.
        }
        val cleanName = uri.lastPathSegment
            ?.substringAfterLast('/')
            ?.substringAfterLast(':')
            ?.take(48)
            ?.takeIf { it.isNotBlank() && !it.all(Char::isDigit) }
            ?: "${targetLayer.label} Image"
        viewModel.importAndPlaceVisualElement(
            name = cleanName,
            uriString = uri.toString(),
            targetLayer = targetLayer
        )
    }

    if (state.isTheaterPreview) {
        TheaterPreviewOverlay(
            scene = activePlaybackScene,
            scenes = state.scenes,
            sceneNumber = state.selectedSceneIndex + 1,
            totalScenes = state.scenes.size.coerceAtLeast(1),
            sceneProgress = sceneProgress,
            sceneTimeSec = activeSceneTimeSec,
            characters = state.characters,
            characterReferencesByCharId = state.characterReferencesByCharId,
            assets = state.assets,
            isPlaying = state.isPlaying,
            playheadSec = state.playheadSec,
            totalDurationSec = state.totalDurationSec,
            fps = state.activeProject?.fps ?: 24L,
            mutedTracks = state.mutedTracks,
            onTogglePlayPause = viewModel::togglePlayPause,
            onStop = viewModel::stopPlayback,
            onClose = { viewModel.toggleTheaterPreview(false) }
        )
        return
    }

    Scaffold(
        containerColor = StudioVoid,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(StudioVoid)
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            val isExpandedLayout = maxWidth >= 720.dp

            if (state.isLoading) {
                StudioLoadingState()
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    TopStudioBar(
                        projectName = state.activeProject?.name ?: "MotionStory Project",
                        saveStatus = state.saveStatus,
                        canUndo = state.canUndo,
                        canRedo = state.canRedo,
                        isExpanded = isExpandedLayout,
                        onOpenProjects = { dialogState.showProjectSwitcher = true },
                        onSaveClick = viewModel::saveProjectToCloud,
                        onUndoClick = viewModel::undo,
                        onRedoClick = viewModel::redo,
                        onSettingsClick = { dialogState.showSettingsDialog = true },
                        onPreviewClick = { viewModel.toggleTheaterPreview(true) },
                        onExportClick = { dialogState.showExportDialog = true }
                    )

                    if (isExpandedLayout) {
                        StudioExpandedWorkspace(
                            state = state,
                            activePlaybackScene = activePlaybackScene,
                            sceneProgress = sceneProgress,
                            activeSceneTimeSec = activeSceneTimeSec,
                            viewModel = viewModel,
                            dialogState = dialogState,
                            onSelectStageTarget = onSelectStageTargetSynced,
                            onImportVisualElement = onImportVisualElementWithPermission,
                            onRequestImportAudio = onRequestImportAudio,
                            onRequestImportMapImage = onRequestImportMapImage,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                    } else {
                        StudioCompactWorkspace(
                            state = state,
                            activePlaybackScene = activePlaybackScene,
                            sceneProgress = sceneProgress,
                            activeSceneTimeSec = activeSceneTimeSec,
                            viewModel = viewModel,
                            dialogState = dialogState,
                            onSelectStageTarget = onSelectStageTargetSynced,
                            onImportVisualElement = onImportVisualElementWithPermission,
                            onRequestImportAudio = onRequestImportAudio,
                            onRequestImportMapImage = onRequestImportMapImage,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    StudioCoreDialogs(
        state = state,
        viewModel = viewModel,
        dialogState = dialogState,
        credentialManager = credentialManager,
        scope = scope
    )

    StudioAiGeneratorDialogs(
        state = state,
        viewModel = viewModel,
        dialogState = dialogState
    )
}

@Composable
private fun StudioLoadingState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        CircularProgressIndicator(color = StudioAmber)
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Loading MotionStory Studio workspace...",
            style = MaterialTheme.typography.bodyMedium,
            color = StudioTextSecondary
        )
    }
}
