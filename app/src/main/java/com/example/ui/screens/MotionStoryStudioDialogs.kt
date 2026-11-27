package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.credentials.CredentialManager
import com.example.compositor.SceneLayerType
import com.example.data.BackgroundGenerationRequest
import com.example.data.PropGenerationRequest
import com.example.data.StudioCharacter
import com.example.ui.auth.signOutStudio
import com.example.ui.components.AssetEditorDialog
import com.example.ui.components.BackgroundGeneratorDialog
import com.example.ui.components.CharacterDesignGeneratorDialog
import com.example.ui.components.CharacterProfileEditorDialog
import com.example.ui.components.CharacterRenameDialog
import com.example.ui.components.ConfirmDeleteCharacterDialog
import com.example.ui.components.ExportVideoDialog
import com.example.ui.components.ProjectSettingsDialog
import com.example.ui.components.ProjectSwitcherDialog
import com.example.ui.components.PropGeneratorDialog
import com.example.ui.components.SceneSuggestionsDialog
import com.example.ui.components.StoryPlanReviewDialog
import com.example.viewmodel.StudioUiState
import com.example.viewmodel.StudioViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope

@Stable
internal class StudioDialogState {
    var showSettingsDialog by mutableStateOf(false)
    var showProjectSwitcher by mutableStateOf(false)
    var showExportDialog by mutableStateOf(false)
    var editingCharacter by mutableStateOf<Pair<Boolean, StudioCharacter?>>(false to null)
    var generatingCharacterId by mutableStateOf<String?>(null)
    var renamingCharacter by mutableStateOf<StudioCharacter?>(null)
    var deletingCharacter by mutableStateOf<StudioCharacter?>(null)
    var addingAssetCategory by mutableStateOf<String?>(null)
    var backgroundGenInitialRequest by mutableStateOf<BackgroundGenerationRequest?>(null)
    var propGenInitialRequest by mutableStateOf<PropGenerationRequest?>(null)
    var showSceneSuggestionsDialog by mutableStateOf(false)

    fun openCharacterGenerator(viewModel: StudioViewModel, character: StudioCharacter) {
        viewModel.selectLibraryCharacter(character.id)
        viewModel.clearCharacterImageGenState()
        generatingCharacterId = character.id
    }

    fun openDefaultBackgroundGenerator(viewModel: StudioViewModel) {
        viewModel.clearBackgroundGenState()
        backgroundGenInitialRequest = viewModel.defaultBackgroundRequestForSelectedScene()
    }

    fun openDefaultPropGenerator(viewModel: StudioViewModel, preferredLayer: SceneLayerType) {
        viewModel.clearPropGenState()
        val effectiveLayer = when (preferredLayer) {
            SceneLayerType.PROPS, SceneLayerType.MIDGROUND, SceneLayerType.FOREGROUND -> preferredLayer.name
            else -> SceneLayerType.PROPS.name
        }
        propGenInitialRequest = viewModel.defaultPropRequestForSelectedScene().copy(targetLayer = effectiveLayer)
    }

    fun openSceneSuggestions() {
        showSceneSuggestionsDialog = true
    }
}

@Composable
internal fun rememberStudioDialogState(): StudioDialogState = remember { StudioDialogState() }

@Composable
internal fun StudioCoreDialogs(
    state: StudioUiState,
    viewModel: StudioViewModel,
    dialogState: StudioDialogState,
    credentialManager: CredentialManager,
    scope: CoroutineScope
) {
    if (dialogState.showSettingsDialog && state.activeProject != null) {
        ProjectSettingsDialog(
            project = state.activeProject,
            userEmail = Firebase.auth.currentUser?.email,
            onDismiss = { dialogState.showSettingsDialog = false },
            onApply = viewModel::updateProjectSettings,
            onSignOut = {
                dialogState.showSettingsDialog = false
                signOutStudio(credentialManager, scope)
            }
        )
    }

    if (dialogState.showProjectSwitcher) {
        ProjectSwitcherDialog(
            projects = state.projects,
            activeProjectId = state.activeProject?.id,
            onDismiss = { dialogState.showProjectSwitcher = false },
            onSelectProject = viewModel::selectProject,
            onCreateProject = viewModel::createNewProject,
            onDeleteProject = viewModel::deleteProject
        )
    }

    if (dialogState.editingCharacter.first) {
        CharacterProfileEditorDialog(
            initial = dialogState.editingCharacter.second,
            suggestedCharCode = viewModel.nextAvailableCharacterCode(state.characters),
            onDismiss = { dialogState.editingCharacter = false to null },
            onSave = viewModel::saveCharacter
        )
    }

    val genChar = dialogState.generatingCharacterId?.let { gid ->
        state.characters.firstOrNull { it.id == gid }
    }
    if (genChar != null) {
        CharacterDesignGeneratorDialog(
            character = genChar,
            references = if (state.selectedLibraryCharacter?.id == genChar.id) {
                state.selectedCharacterReferences
            } else {
                emptyList()
            },
            genState = state.characterImageGenState,
            onDismiss = {
                dialogState.generatingCharacterId = null
                viewModel.clearCharacterImageGenState()
            },
            onGenerate = { viewType, expression, poseAction, visualStyle, useAnchor, setAsPrimary ->
                viewModel.generateCharacterReferenceDesign(
                    character = genChar,
                    viewType = viewType,
                    expression = expression,
                    poseAction = poseAction,
                    visualStyle = visualStyle,
                    useAnchorReference = useAnchor,
                    setAsPrimary = setAsPrimary
                )
            },
            onSetPrimary = { base64 ->
                viewModel.setReferenceAsPrimary(genChar.id, base64)
            },
            onDeleteReference = { refId ->
                viewModel.deleteCharacterReference(genChar.id, refId)
            }
        )
    }

    val charToRename = dialogState.renamingCharacter
    if (charToRename != null) {
        CharacterRenameDialog(
            character = charToRename,
            onDismiss = { dialogState.renamingCharacter = null },
            onConfirm = { newName -> viewModel.renameCharacter(charToRename, newName) }
        )
    }

    val charToDelete = dialogState.deletingCharacter
    if (charToDelete != null) {
        ConfirmDeleteCharacterDialog(
            character = charToDelete,
            onDismiss = { dialogState.deletingCharacter = null },
            onConfirmDelete = {
                if (dialogState.generatingCharacterId == charToDelete.id) {
                    dialogState.generatingCharacterId = null
                }
                viewModel.deleteCharacter(charToDelete.id)
            }
        )
    }

    val assetCat = dialogState.addingAssetCategory
    if (assetCat != null) {
        AssetEditorDialog(
            defaultCategory = assetCat,
            onDismiss = { dialogState.addingAssetCategory = null },
            onSave = viewModel::saveAsset
        )
    }

    if (dialogState.showExportDialog) {
        ExportVideoDialog(
            project = state.activeProject,
            sceneCount = state.scenes.size,
            totalDurationSec = state.totalDurationSec,
            shotlistJson = viewModel.buildExportShotlistJson(),
            srtContent = viewModel.buildExportSrt(),
            onDismiss = { dialogState.showExportDialog = false },
            onCopiedShotlist = {
                dialogState.showExportDialog = false
                viewModel.saveProjectToCloud()
            },
            onCopiedSrt = {
                viewModel.postSnackbar("Copied project subtitles (.SRT) to clipboard")
            }
        )
    }
}

@Composable
internal fun StudioAiGeneratorDialogs(
    state: StudioUiState,
    viewModel: StudioViewModel,
    dialogState: StudioDialogState
) {
    val bgReq = dialogState.backgroundGenInitialRequest
    if (bgReq != null) {
        BackgroundGeneratorDialog(
            sceneTitle = state.selectedScene?.title.orEmpty(),
            initialRequest = bgReq,
            genState = state.backgroundGenState,
            onBuildSummaryPrompt = viewModel::buildEditableBackgroundSummary,
            onGenerate = viewModel::generateSceneBackground,
            onReject = viewModel::rejectGeneratedBackground,
            onSaveToLibrary = { name, style, tags, applyToScene ->
                viewModel.saveGeneratedBackgroundToLibrary(
                    name = name,
                    visualStyle = style,
                    tags = tags,
                    applyToCurrentScene = applyToScene,
                    onApplied = {
                        dialogState.backgroundGenInitialRequest = null
                        viewModel.clearBackgroundGenState()
                    }
                )
            },
            onDismiss = {
                dialogState.backgroundGenInitialRequest = null
                viewModel.clearBackgroundGenState()
            }
        )
    }

    val propReq = dialogState.propGenInitialRequest
    if (propReq != null) {
        PropGeneratorDialog(
            sceneTitle = state.selectedScene?.title.orEmpty(),
            initialRequest = propReq,
            genState = state.propGenState,
            onGenerate = viewModel::generateVisualProp,
            onReject = viewModel::rejectGeneratedProp,
            onSaveToLibrary = { name, category, style, targetLayer, placeInScene ->
                viewModel.saveGeneratedPropToLibrary(
                    name = name,
                    propCategory = category,
                    visualStyle = style,
                    targetLayer = targetLayer,
                    placeInCurrentScene = placeInScene,
                    onPlaced = {
                        dialogState.propGenInitialRequest = null
                        viewModel.clearPropGenState()
                    }
                )
            },
            onDismiss = {
                dialogState.propGenInitialRequest = null
                viewModel.clearPropGenState()
            }
        )
    }

    val selectedSceneForSuggestions = state.selectedScene
    if (dialogState.showSceneSuggestionsDialog && selectedSceneForSuggestions != null) {
        val defaultStyle = state.characters.firstOrNull()?.visualStyle?.ifBlank { "Painterly 2D Documentary" }
            ?: "Painterly 2D Documentary"
        SceneSuggestionsDialog(
            scene = selectedSceneForSuggestions,
            defaultVisualStyle = defaultStyle,
            suggestionsState = state.sceneSuggestionsState,
            onRequestSuggestions = { style ->
                viewModel.suggestVisualAssetsForSelectedScene(style)
            },
            onSelectBackgroundSuggestion = { suggestedBg, style ->
                dialogState.showSceneSuggestionsDialog = false
                viewModel.clearBackgroundGenState()
                dialogState.backgroundGenInitialRequest = BackgroundGenerationRequest(
                    assetName = suggestedBg.title,
                    sceneTitle = selectedSceneForSuggestions.title,
                    sceneDescription = selectedSceneForSuggestions.description,
                    sceneNarration = selectedSceneForSuggestions.narrationText,
                    location = suggestedBg.location,
                    eraContext = suggestedBg.eraContext,
                    timeOfDay = suggestedBg.timeOfDay,
                    weather = suggestedBg.weather,
                    visualStyle = style,
                    moodAtmosphere = suggestedBg.moodAtmosphere,
                    customPrompt = suggestedBg.prompt
                )
            },
            onSelectPropSuggestion = { suggestedProp, style ->
                dialogState.showSceneSuggestionsDialog = false
                viewModel.clearPropGenState()
                dialogState.propGenInitialRequest = PropGenerationRequest(
                    propName = suggestedProp.name,
                    propCategory = suggestedProp.category,
                    propDescription = suggestedProp.prompt,
                    sceneContext = selectedSceneForSuggestions.description.take(400),
                    eraContext = "",
                    visualStyle = style,
                    targetLayer = suggestedProp.suggestedLayer,
                    requestTransparentPng = true
                )
            },
            onDismiss = { dialogState.showSceneSuggestionsDialog = false }
        )
    }

    val storyDraft = state.pendingStoryDraft
    if (storyDraft != null) {
        StoryPlanReviewDialog(
            draft = storyDraft,
            approvedExistingSceneCount = state.scenes.count { it.scenePlan.approved },
            onUpdateAnalysis = viewModel::updatePendingDraftAnalysis,
            onUpdateScene = viewModel::updatePendingDraftScene,
            onMoveScene = viewModel::movePendingDraftScene,
            onRemoveScene = viewModel::removePendingDraftScene,
            onApproveAndApply = viewModel::approveAndApplyStoryDraft,
            onDismiss = viewModel::dismissPendingStoryDraft
        )
    }
}
