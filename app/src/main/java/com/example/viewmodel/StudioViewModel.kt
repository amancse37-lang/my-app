package com.example.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.animation.AnimatableProperty
import com.example.animation.AnimationEvaluator
import com.example.animation.AnimationTargetType
import com.example.animation.EasingType
import com.example.audio.AudioFileStorageManager
import com.example.audio.AudioPlaybackEngine
import com.example.audio.AudioTimelineEngine
import com.example.audio.LipSyncEngine
import com.example.audio.StudioTtsEngine
import com.example.cinematics.CinematicCameraPreset
import com.example.cinematics.CinematicEngine
import com.example.cinematics.LightingPreset
import com.example.compositor.SceneCompositor
import com.example.compositor.SceneLayerType
import com.example.compositor.SelectedStageTarget
import com.example.compositor.StageTransformMath
import com.example.data.AiScriptState
import com.example.data.AiSoundSuggestion
import com.example.data.AudioImportState
import com.example.data.AudioStudioTab
import com.example.data.BackgroundGenerationRequest
import com.example.data.CharacterImageGenState
import com.example.data.CharacterImageService
import com.example.data.CharacterReferenceSheet
import com.example.data.CompactWorkspaceTab
import com.example.data.DialogueLine
import com.example.data.GeneratedScenePlan
import com.example.data.GraphicSuggestionsState
import com.example.data.InspectorSection
import com.example.data.MotionGraphicElement
import com.example.data.MotionGraphicsStudioTab
import com.example.data.PropGenerationRequest
import com.example.data.SaveStatus
import com.example.data.SceneAudioClip
import com.example.data.SceneCharacterPlacement
import com.example.data.SceneCinematics
import com.example.data.ScenePlanMetadata
import com.example.data.SceneSuggestionsState
import com.example.data.SceneTransitionConfig
import com.example.data.SceneVisualAiService
import com.example.data.SceneVisualElement
import com.example.data.ScriptAiService
import com.example.data.ScriptBreakdownResult
import com.example.data.SidebarSection
import com.example.data.SoundSuggestionsState
import com.example.data.StoryAnalysisSummary
import com.example.data.StudioAsset
import com.example.data.StudioAudioAsset
import com.example.data.StudioCharacter
import com.example.data.StudioHistorySnapshot
import com.example.data.StudioPresets
import com.example.data.StudioProject
import com.example.data.StudioRepository
import com.example.data.StudioScene
import com.example.data.SubtitleCue
import com.example.data.SubtitleStyleConfig
import com.example.data.TimelineTrackType
import com.example.data.TtsGenerationState
import com.example.data.TtsVoiceOption
import com.example.data.VisualAssetGenState
import com.example.graphics.MotionGraphicsEngine
import com.example.graphics.SubtitleEngine
import com.example.graphics.TransitionFlowEngine
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private const val VM_TAG = "StudioViewModel"

data class StudioUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val snackbarMessage: String? = null,
    val projects: List<StudioProject> = emptyList(),
    val activeProject: StudioProject? = null,
    val scenes: List<StudioScene> = emptyList(),
    val selectedSceneId: String? = null,
    val selectedCastCharacterId: String? = null,
    val selectedStageTarget: SelectedStageTarget? = null,
    val characters: List<StudioCharacter> = emptyList(),
    val selectedLibraryCharacterId: String? = null,
    val selectedCharacterReferences: List<CharacterReferenceSheet> = emptyList(),
    val characterReferencesByCharId: Map<String, List<CharacterReferenceSheet>> = emptyMap(),
    val assets: List<StudioAsset> = emptyList(),
    val audioAssets: List<StudioAudioAsset> = emptyList(),
    val activeSidebarSection: SidebarSection = SidebarSection.SCRIPT,
    val activeAudioTab: AudioStudioTab = AudioStudioTab.NARRATION,
    val activeMotionGfxTab: MotionGraphicsStudioTab = MotionGraphicsStudioTab.TEMPLATES,
    val selectedGraphicId: String? = null,
    val graphicSuggestionsState: GraphicSuggestionsState = GraphicSuggestionsState.Idle,
    val lastSubtitleUsedRealAudio: Boolean = false,
    val activeInspectorSection: InspectorSection = InspectorSection.SCENE,
    val compactWorkspaceTab: CompactWorkspaceTab = CompactWorkspaceTab.TIMELINE,
    val assetCategoryFilter: String = "All",
    val isPlaying: Boolean = false,
    val playheadSec: Double = 0.0,
    val canvasZoom: Float = 1.0f,
    val timelineZoomDpPerSec: Float = 32f,
    val mutedTracks: Set<TimelineTrackType> = emptySet(),
    val isTheaterPreview: Boolean = false,
    val showFramingGuides: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val saveStatus: SaveStatus = SaveStatus.Saved,
    val aiScriptState: AiScriptState = AiScriptState.Idle,
    val pendingStoryDraft: ScriptBreakdownResult? = null,
    val regeneratingSceneTask: String? = null,
    val selectedAudioClipId: String? = null,
    val previewingAudioAssetId: String? = null,
    val availableTtsVoices: List<TtsVoiceOption> = emptyList(),
    val audioImportState: AudioImportState = AudioImportState.Idle,
    val ttsGenState: TtsGenerationState = TtsGenerationState.Idle,
    val soundSuggestionsState: SoundSuggestionsState = SoundSuggestionsState.Idle,
    val characterImageGenState: CharacterImageGenState = CharacterImageGenState.Idle,
    val backgroundGenState: VisualAssetGenState = VisualAssetGenState.Idle,
    val propGenState: VisualAssetGenState = VisualAssetGenState.Idle,
    val sceneSuggestionsState: SceneSuggestionsState = SceneSuggestionsState.Idle
) {
    val selectedScene: StudioScene?
        get() = scenes.firstOrNull { it.id == selectedSceneId } ?: scenes.firstOrNull()

    val selectedLibraryCharacter: StudioCharacter?
        get() = characters.firstOrNull { it.id == selectedLibraryCharacterId } ?: characters.firstOrNull()

    val selectedSceneIndex: Int
        get() = scenes.indexOfFirst { it.id == selectedScene?.id }.coerceAtLeast(0)

    val totalDurationSec: Double
        get() = scenes.sumOf { it.durationSec }.coerceAtLeast(1.0)

    fun sceneAtTime(timeSec: Double): Pair<StudioScene?, Double> {
        var cursor = 0.0
        for (scene in scenes) {
            val end = cursor + scene.durationSec
            if (timeSec <= end || scene == scenes.lastOrNull()) {
                val progress = ((timeSec - cursor) / scene.durationSec.coerceAtLeast(0.1))
                    .coerceIn(0.0, 1.0)
                return scene to progress
            }
            cursor = end
        }
        return null to 0.0
    }

    fun startTimeOfScene(sceneId: String): Double {
        var cursor = 0.0
        for (scene in scenes) {
            if (scene.id == sceneId) return cursor
            cursor += scene.durationSec
        }
        return 0.0
    }

    val selectedSceneLocalTimeSec: Double
        get() {
            val scene = selectedScene ?: return 0.0
            val start = startTimeOfScene(scene.id)
            return (playheadSec - start).coerceIn(0.0, scene.durationSec.coerceAtLeast(0.1))
        }
}

class StudioViewModel(
    private val repository: StudioRepository,
    private val aiService: ScriptAiService,
    private val characterImageService: CharacterImageService,
    private val visualAiService: SceneVisualAiService = SceneVisualAiService(),
    private val currentUserId: String,
    private val audioStorageManager: AudioFileStorageManager? = null,
    private val ttsEngine: StudioTtsEngine? = null,
    private val audioPlaybackEngine: AudioPlaybackEngine? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudioUiState())
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    private val undoStack = ArrayDeque<StudioHistorySnapshot>()
    private val redoStack = ArrayDeque<StudioHistorySnapshot>()
    private var sceneObserverJob: Job? = null
    private var charRefObserverJob: Job? = null
    private val allCharRefObserverJobs = mutableMapOf<String, Job>()
    private var playbackJob: Job? = null
    private var observedProjectId: String? = null
    private var observedCharRefId: String? = null

    init {
        initializeWorkspace()
        loadAvailableTtsVoices()
    }

    private fun initializeWorkspace() {
        viewModelScope.launch {
            val seedResult = repository.seedStarterWorkspaceIfEmpty(currentUserId)
            seedResult.onFailure { err ->
                _uiState.update { it.copy(errorMessage = err.localizedMessage ?: "Failed to initialize workspace.") }
            }
            startObservers()
        }
    }

    private fun startObservers() {
        viewModelScope.launch {
            repository.observeProjects(currentUserId)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val rootError = firestoreError ?: error
                    Log.w(VM_TAG, "Error observing projects", rootError)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = rootError.message ?: "Failed to observe projects."
                        )
                    }
                }
                .collect { projectList ->
                    val currentActive = _uiState.value.activeProject
                    val nextActive = when {
                        currentActive != null && projectList.any { it.id == currentActive.id } -> {
                            if (_uiState.value.saveStatus == SaveStatus.Unsaved) {
                                currentActive
                            } else {
                                projectList.first { it.id == currentActive.id }
                            }
                        }
                        projectList.isNotEmpty() -> projectList.first()
                        else -> null
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            projects = projectList,
                            activeProject = nextActive
                        )
                    }
                    if (nextActive != null && observedProjectId != nextActive.id) {
                        observeProjectScenes(nextActive.id)
                    }
                }
        }

        viewModelScope.launch {
            repository.observeCharacters(currentUserId)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    Log.w(VM_TAG, "Error observing characters", firestoreError ?: error)
                }
                .collect { chars ->
                    val currentSelectedCharId = _uiState.value.selectedLibraryCharacterId
                    val nextCharId = when {
                        currentSelectedCharId != null && chars.any { it.id == currentSelectedCharId } -> currentSelectedCharId
                        else -> chars.firstOrNull()?.id
                    }
                    _uiState.update {
                        it.copy(
                            characters = chars,
                            selectedLibraryCharacterId = nextCharId,
                            selectedCharacterReferences = nextCharId?.let { id -> it.characterReferencesByCharId[id] }.orEmpty()
                        )
                    }
                    syncAllCharacterReferenceObservers(chars.map { it.id }.toSet())
                    if (nextCharId != null && observedCharRefId != nextCharId) {
                        observeCharacterReferences(nextCharId)
                    } else if (nextCharId == null) {
                        charRefObserverJob?.cancel()
                        observedCharRefId = null
                        _uiState.update { it.copy(selectedCharacterReferences = emptyList()) }
                    }
                }
        }

        viewModelScope.launch {
            repository.observeAssets(currentUserId)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    Log.w(VM_TAG, "Error observing assets", firestoreError ?: error)
                }
                .collect { assetList ->
                    _uiState.update { it.copy(assets = assetList) }
                }
        }

        viewModelScope.launch {
            repository.observeAudioAssets(currentUserId)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    Log.w(VM_TAG, "Error observing audio assets", firestoreError ?: error)
                }
                .collect { rawAudio ->
                    val verified = rawAudio.map { asset ->
                        val existsLocally = audioStorageManager?.fileExists(asset.localFilePath) ?: true
                        if (!existsLocally && asset.storageState != "MISSING_LOCAL") {
                            asset.copy(storageState = "MISSING_LOCAL")
                        } else {
                            asset
                        }
                    }
                    _uiState.update { it.copy(audioAssets = verified) }
                }
        }
    }

    private fun observeProjectScenes(projectId: String) {
        sceneObserverJob?.cancel()
        observedProjectId = projectId
        undoStack.clear()
        redoStack.clear()
        sceneObserverJob = viewModelScope.launch {
            repository.observeScenes(currentUserId, projectId)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    Log.w(VM_TAG, "Error observing scenes", firestoreError ?: error)
                    _uiState.update {
                        it.copy(errorMessage = (firestoreError ?: error).message ?: "Failed to load scenes.")
                    }
                }
                .collect { remoteScenes ->
                    if (_uiState.value.saveStatus == SaveStatus.Unsaved) return@collect
                    val sorted = remoteScenes.sortedBy { it.orderIndex }
                    _uiState.update { state ->
                        val nextSelectedId = when {
                            state.selectedSceneId != null && sorted.any { it.id == state.selectedSceneId } -> state.selectedSceneId
                            else -> sorted.firstOrNull()?.id
                        }
                        val nextScene = sorted.firstOrNull { it.id == nextSelectedId }
                        val nextCastId = nextScene?.effectiveCast()?.firstOrNull()?.characterId
                        state.copy(
                            scenes = sorted,
                            selectedSceneId = nextSelectedId,
                            selectedCastCharacterId = state.selectedCastCharacterId
                                ?.takeIf { cid -> nextScene?.effectiveCast()?.any { it.characterId == cid } == true }
                                ?: nextCastId,
                            canUndo = undoStack.isNotEmpty(),
                            canRedo = redoStack.isNotEmpty()
                        )
                    }
                }
        }
    }

    private fun syncAllCharacterReferenceObservers(activeCharIds: Set<String>) {
        // Cancel observers for deleted characters
        val staleIds = allCharRefObserverJobs.keys - activeCharIds
        staleIds.forEach { staleId ->
            allCharRefObserverJobs.remove(staleId)?.cancel()
        }
        if (staleIds.isNotEmpty()) {
            _uiState.update { state ->
                state.copy(characterReferencesByCharId = state.characterReferencesByCharId - staleIds)
            }
        }

        // Start observers for newly added characters (cap at 24 concurrent listeners)
        activeCharIds.take(24).forEach { charId ->
            if (charId.isNotBlank() && charId !in allCharRefObserverJobs) {
                allCharRefObserverJobs[charId] = viewModelScope.launch {
                    repository.observeCharacterReferences(currentUserId, charId)
                        .catch { error ->
                            val firestoreError = generateSequence(error) { it.cause }
                                .filterIsInstance<FirebaseFirestoreException>()
                                .firstOrNull()
                            if (firestoreError == null && error is CancellationException) throw error
                            Log.w(VM_TAG, "Error observing reference cache for $charId", firestoreError ?: error)
                        }
                        .collect { sheets ->
                            _uiState.update { state ->
                                val nextMap = state.characterReferencesByCharId + (charId to sheets)
                                state.copy(
                                    characterReferencesByCharId = nextMap,
                                    selectedCharacterReferences = if (state.selectedLibraryCharacterId == charId) {
                                        sheets
                                    } else {
                                        state.selectedCharacterReferences
                                    }
                                )
                            }
                        }
                }
            }
        }
    }

    private fun observeCharacterReferences(characterId: String) {
        charRefObserverJob?.cancel()
        observedCharRefId = characterId
        val cached = _uiState.value.characterReferencesByCharId[characterId]
        if (cached != null) {
            _uiState.update { it.copy(selectedCharacterReferences = cached) }
        }
        charRefObserverJob = viewModelScope.launch {
            repository.observeCharacterReferences(currentUserId, characterId)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    Log.w(VM_TAG, "Error observing character references", firestoreError ?: error)
                }
                .collect { sheets ->
                    _uiState.update {
                        it.copy(
                            selectedCharacterReferences = sheets,
                            characterReferencesByCharId = it.characterReferencesByCharId + (characterId to sheets)
                        )
                    }
                }
        }
    }

    // --- HISTORY (UNDO / REDO) ---

    private fun pushHistorySnapshot() {
        val state = _uiState.value
        val proj = state.activeProject ?: return
        undoStack.addLast(
            StudioHistorySnapshot(
                project = proj,
                scenes = state.scenes,
                selectedSceneId = state.selectedSceneId
            )
        )
        while (undoStack.size > 30) {
            undoStack.removeFirst()
        }
        redoStack.clear()
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val state = _uiState.value
        val currentProj = state.activeProject ?: return
        redoStack.addLast(
            StudioHistorySnapshot(
                project = currentProj,
                scenes = state.scenes,
                selectedSceneId = state.selectedSceneId
            )
        )
        val previous = undoStack.removeLast()
        _uiState.update {
            it.copy(
                activeProject = previous.project,
                scenes = previous.scenes,
                selectedSceneId = previous.selectedSceneId,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                saveStatus = SaveStatus.Unsaved
            )
        }
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val state = _uiState.value
        val currentProj = state.activeProject ?: return
        undoStack.addLast(
            StudioHistorySnapshot(
                project = currentProj,
                scenes = state.scenes,
                selectedSceneId = state.selectedSceneId
            )
        )
        val next = redoStack.removeLast()
        _uiState.update {
            it.copy(
                activeProject = next.project,
                scenes = next.scenes,
                selectedSceneId = next.selectedSceneId,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                saveStatus = SaveStatus.Unsaved
            )
        }
    }

    // --- PROJECT ACTIONS ---

    fun selectProject(project: StudioProject) {
        stopPlayback()
        _uiState.update {
            it.copy(
                activeProject = project,
                saveStatus = SaveStatus.Saved,
                playheadSec = 0.0,
                aiScriptState = AiScriptState.Idle
            )
        }
        observeProjectScenes(project.id)
    }

    fun createNewProject(name: String, language: String) {
        val cleanName = name.trim().take(120)
        if (cleanName.isBlank()) {
            _uiState.update { it.copy(snackbarMessage = "Please enter a project title.") }
            return
        }
        viewModelScope.launch {
            repository.createProject(cleanName, language)
                .onSuccess { newId ->
                    _uiState.update { it.copy(snackbarMessage = "Created project \"$cleanName\"") }
                    observedProjectId = null
                    observeProjectScenes(newId)
                }
                .onFailure { err ->
                    _uiState.update { it.copy(errorMessage = err.localizedMessage ?: "Could not create project.") }
                }
        }
    }

    fun deleteProject(projectId: String) {
        if (_uiState.value.projects.size <= 1) {
            _uiState.update { it.copy(snackbarMessage = "Keep at least one active project in your studio.") }
            return
        }
        viewModelScope.launch {
            repository.deleteProject(projectId)
                .onSuccess {
                    observedProjectId = null
                    _uiState.update { it.copy(snackbarMessage = "Project deleted.") }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(errorMessage = err.localizedMessage ?: "Failed to delete project.") }
                }
        }
    }

    fun saveProjectToCloud() {
        if (_uiState.value.saveStatus == SaveStatus.Saving) return
        val project = _uiState.value.activeProject ?: return
        val scenes = _uiState.value.scenes
        viewModelScope.launch {
            _uiState.update { it.copy(saveStatus = SaveStatus.Saving) }
            repository.saveProjectWithScenes(project, scenes)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            saveStatus = SaveStatus.Saved,
                            snackbarMessage = "Saved \"${project.name}\" to cloud"
                        )
                    }
                }
                .onFailure { err ->
                    val msg = err.localizedMessage ?: "Save failed."
                    _uiState.update {
                        it.copy(
                            saveStatus = SaveStatus.Error(msg),
                            errorMessage = msg
                        )
                    }
                }
        }
    }

    fun updateProjectSettings(
        name: String,
        scriptLanguage: String,
        aspectRatio: String,
        fps: Long,
        resolution: String
    ) {
        val current = _uiState.value.activeProject ?: return
        pushHistorySnapshot()
        _uiState.update {
            it.copy(
                activeProject = current.copy(
                    name = name.trim().take(120).ifBlank { current.name },
                    scriptLanguage = scriptLanguage,
                    aspectRatio = aspectRatio,
                    fps = fps,
                    resolution = resolution
                ),
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                saveStatus = SaveStatus.Unsaved
            )
        }
    }

    fun updateScriptText(newText: String) {
        val current = _uiState.value.activeProject ?: return
        _uiState.update {
            it.copy(
                activeProject = current.copy(scriptText = newText.take(25000)),
                saveStatus = SaveStatus.Unsaved
            )
        }
    }

    fun updateScriptLanguage(language: String) {
        val current = _uiState.value.activeProject ?: return
        _uiState.update {
            it.copy(
                activeProject = current.copy(scriptLanguage = language),
                saveStatus = SaveStatus.Unsaved
            )
        }
    }

    fun loadSampleScript(isHindi: Boolean) {
        val current = _uiState.value.activeProject ?: return
        pushHistorySnapshot()
        _uiState.update {
            it.copy(
                activeProject = current.copy(
                    scriptText = if (isHindi) StudioPresets.sampleHindiScript else StudioPresets.sampleEnglishScript,
                    scriptLanguage = if (isHindi) "Hindi" else "English"
                ),
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                saveStatus = SaveStatus.Unsaved
            )
        }
    }

    // --- AI SCRIPT-TO-SCENE + AUTOMATIC CHARACTER IDENTIFICATION & DEDUPLICATION ---

    fun nextAvailableCharacterCode(existingChars: List<StudioCharacter>): String {
        val usedNumbers = existingChars.mapNotNull { c ->
            Regex("CHR-(\\d+)").find(c.charCode)?.groupValues?.getOrNull(1)?.toIntOrNull()
        }.toSet()
        var candidate = 1
        while (candidate in usedNumbers) {
            candidate++
        }
        return "CHR-%02d".format(candidate)
    }

    private fun normalizeCharKey(raw: String): String {
        return raw.lowercase()
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()
    }

    fun generateScenesFromScript() {
        if (_uiState.value.aiScriptState is AiScriptState.Generating) return
        val project = _uiState.value.activeProject ?: return
        val script = project.scriptText.trim()
        if (script.isBlank()) {
            _uiState.update {
                it.copy(aiScriptState = AiScriptState.Error("Paste a Hindi or English documentary script first."))
            }
            return
        }
        if (script.length < 10) {
            _uiState.update {
                it.copy(aiScriptState = AiScriptState.Error("Script is too short. Enter at least one documentary scene or narration line."))
            }
            return
        }

        stopPlayback()
        viewModelScope.launch {
            _uiState.update { it.copy(aiScriptState = AiScriptState.Generating) }
            val existingChars = _uiState.value.characters
            val result = aiService.generateScenesFromScript(
                scriptText = script,
                language = project.scriptLanguage,
                characters = existingChars
            )
            result.onSuccess { breakdown ->
                // Stage the AI breakdown for Review, Edit & Approval BEFORE overwriting any scenes
                _uiState.update {
                    it.copy(
                        pendingStoryDraft = breakdown,
                        aiScriptState = AiScriptState.Success(
                            sceneCount = breakdown.scenes.size,
                            characterCount = breakdown.identifiedCharacters.size
                        ),
                        snackbarMessage = "AI Story Plan ready for review (${breakdown.scenes.size} scenes, ${breakdown.identifiedCharacters.size} characters)"
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        aiScriptState = AiScriptState.Error(
                            err.localizedMessage ?: "Failed to analyze script and generate scenes."
                        )
                    )
                }
            }
        }
    }

    fun updatePendingDraftAnalysis(transform: (StoryAnalysisSummary) -> StoryAnalysisSummary) {
        _uiState.update { state ->
            val draft = state.pendingStoryDraft ?: return@update state
            state.copy(pendingStoryDraft = draft.copy(storyAnalysis = transform(draft.storyAnalysis)))
        }
    }

    fun updatePendingDraftScene(index: Int, transform: (GeneratedScenePlan) -> GeneratedScenePlan) {
        _uiState.update { state ->
            val draft = state.pendingStoryDraft ?: return@update state
            if (index !in draft.scenes.indices) return@update state
            val updatedScenes = draft.scenes.toMutableList()
            updatedScenes[index] = transform(updatedScenes[index])
            state.copy(
                pendingStoryDraft = draft.copy(
                    scenes = updatedScenes,
                    storyAnalysis = draft.storyAnalysis.copy(
                        estimatedTotalDurationSec = updatedScenes.sumOf { it.durationSec }
                    )
                )
            )
        }
    }

    fun movePendingDraftScene(index: Int, delta: Int) {
        _uiState.update { state ->
            val draft = state.pendingStoryDraft ?: return@update state
            val target = index + delta
            if (index !in draft.scenes.indices || target !in draft.scenes.indices) return@update state
            val list = draft.scenes.toMutableList()
            val item = list.removeAt(index)
            list.add(target, item)
            state.copy(pendingStoryDraft = draft.copy(scenes = list))
        }
    }

    fun removePendingDraftScene(index: Int) {
        _uiState.update { state ->
            val draft = state.pendingStoryDraft ?: return@update state
            if (draft.scenes.size <= 1 || index !in draft.scenes.indices) return@update state
            val list = draft.scenes.toMutableList()
            list.removeAt(index)
            state.copy(pendingStoryDraft = draft.copy(scenes = list))
        }
    }

    fun dismissPendingStoryDraft() {
        _uiState.update { it.copy(pendingStoryDraft = null) }
    }

    fun approveAndApplyStoryDraft(preserveApprovedScenes: Boolean = true) {
        val breakdown = _uiState.value.pendingStoryDraft ?: return
        val existingChars = _uiState.value.characters
        val existingScenes = _uiState.value.scenes
        val audioAssets = _uiState.value.audioAssets
        val language = _uiState.value.activeProject?.scriptLanguage ?: "Bilingual"

        stopPlayback()
        viewModelScope.launch {
            val workingLibrary = existingChars.toMutableList()
            val nameToDocId = mutableMapOf<String, String>()
            val codeToDocId = mutableMapOf<String, String>()

            workingLibrary.forEach { c ->
                nameToDocId[normalizeCharKey(c.name)] = c.id
                codeToDocId[c.charCode.uppercase()] = c.id
            }

            var newlyCreatedCount = 0
            var failedCharSaves = 0
            breakdown.identifiedCharacters.forEach { identified ->
                val normName = normalizeCharKey(identified.name)
                val matchedExistingId =
                    codeToDocId[identified.existingCharCode.uppercase()]
                        ?: nameToDocId[normName]
                        ?: workingLibrary.firstOrNull { existing ->
                            val exNorm = normalizeCharKey(existing.name)
                            exNorm.isNotBlank() && (exNorm.contains(normName) || normName.contains(exNorm))
                        }?.id

                if (matchedExistingId != null) {
                    nameToDocId[normName] = matchedExistingId
                } else {
                    val nextCode = nextAvailableCharacterCode(workingLibrary)
                    val accent = StudioPresets.characterAccentPalette[
                        workingLibrary.size % StudioPresets.characterAccentPalette.size
                    ]
                    val newCharacter = StudioCharacter(
                        userId = currentUserId,
                        charCode = nextCode,
                        name = identified.name,
                        role = identified.role,
                        visualAge = identified.visualAge,
                        faceDescription = identified.faceDescription,
                        hairStyle = identified.hairStyle,
                        hairColor = identified.hairColor,
                        clothing = identified.clothing,
                        clothingColors = identified.clothingColors,
                        bodyProportions = identified.bodyProportions,
                        accessories = identified.accessories,
                        personality = identified.personality,
                        appearanceNotes = identified.consistencyNotes.ifBlank {
                            "${identified.faceDescription} ${identified.clothing}".trim()
                        },
                        wardrobePalette = identified.clothingColors,
                        expressionDefault = identified.defaultEmotion,
                        visualStyle = identified.visualStyle,
                        referenceImageBase64 = "",
                        avatarPreset = "narrator",
                        accentColorHex = accent,
                        voiceLanguage = if (language == "Hindi") "Hindi" else "English"
                    )
                    val savedIdResult = repository.saveCharacter(newCharacter)
                    savedIdResult.onSuccess { newDocId ->
                        val persisted = newCharacter.copy(id = newDocId)
                        workingLibrary.add(persisted)
                        nameToDocId[normName] = newDocId
                        codeToDocId[nextCode.uppercase()] = newDocId
                        newlyCreatedCount++
                    }.onFailure { err ->
                        failedCharSaves++
                        Log.w(VM_TAG, "Failed to save identified character ${identified.name}", err)
                    }
                }
            }

            pushHistorySnapshot()

            val preservedApproved = if (preserveApprovedScenes) {
                existingScenes.filter { it.scenePlan.approved }
            } else {
                emptyList()
            }

            val plannedScenes = breakdown.scenes.mapIndexed { idx, plan ->
                buildStudioSceneFromPlan(
                    plan = plan,
                    orderIndex = (preservedApproved.size + idx).toLong(),
                    language = language,
                    workingLibrary = workingLibrary,
                    nameToDocId = nameToDocId,
                    codeToDocId = codeToDocId,
                    audioAssets = audioAssets
                )
            }

            val finalScenes = (preservedApproved + plannedScenes).mapIndexed { idx, sc ->
                sc.copy(orderIndex = idx.toLong())
            }
            val firstScene = finalScenes.firstOrNull()

            _uiState.update { state ->
                val updatedProject = state.activeProject?.copy(
                    storyAnalysis = breakdown.storyAnalysis.copy(
                        estimatedTotalDurationSec = finalScenes.sumOf { it.durationSec },
                        approved = true
                    )
                )
                state.copy(
                    activeProject = updatedProject,
                    scenes = finalScenes,
                    selectedSceneId = firstScene?.id,
                    selectedCastCharacterId = firstScene?.effectiveCast()?.firstOrNull()?.characterId,
                    pendingStoryDraft = null,
                    playheadSec = 0.0,
                    activeSidebarSection = SidebarSection.SCENES,
                    canUndo = undoStack.isNotEmpty(),
                    canRedo = redoStack.isNotEmpty(),
                    saveStatus = SaveStatus.Unsaved,
                    snackbarMessage = buildString {
                        append("Applied ${plannedScenes.size} planned scenes")
                        if (preservedApproved.isNotEmpty()) append(" (preserved ${preservedApproved.size} approved)")
                        if (newlyCreatedCount > 0) append(" · $newlyCreatedCount new character(s)")
                        if (failedCharSaves > 0) append(" · $failedCharSaves char save failed")
                    }
                )
            }
        }
    }

    private fun buildStudioSceneFromPlan(
        plan: GeneratedScenePlan,
        orderIndex: Long,
        language: String,
        workingLibrary: List<StudioCharacter>,
        nameToDocId: Map<String, String>,
        codeToDocId: Map<String, String>,
        audioAssets: List<StudioAudioAsset>,
        existingId: String? = null
    ): StudioScene {
        val resolvedCast = plan.cast.mapIndexedNotNull { castIdx, member ->
            val normMember = normalizeCharKey(member.characterName)
            val charId = codeToDocId[member.existingCharCode.uppercase()]
                ?: nameToDocId[normMember]
                ?: workingLibrary.firstOrNull {
                    val ex = normalizeCharKey(it.name)
                    ex.isNotBlank() && (ex.contains(normMember) || normMember.contains(ex))
                }?.id
            if (charId.isNullOrBlank()) {
                null
            } else {
                val stageX = when (member.stagePosition) {
                    "Left" -> -115.0
                    "Right" -> 115.0
                    else -> when {
                        plan.cast.size == 1 -> 0.0
                        castIdx == 0 -> -105.0
                        castIdx == 1 -> 105.0
                        else -> 0.0
                    }
                }
                SceneCharacterPlacement(
                    characterId = charId,
                    expression = member.expression,
                    action = member.action,
                    poseView = member.poseView,
                    posX = stageX,
                    posY = 16.0,
                    scale = if (plan.cast.size > 1) 0.94 else 1.02,
                    rotationDeg = 0.0
                )
            }
        }.distinctBy { it.characterId }.take(12)

        val fallbackPrimaryId = resolvedCast.firstOrNull()?.characterId
            ?: codeToDocId[plan.suggestedCharacterCode.uppercase()]
            ?: workingLibrary.firstOrNull()?.id.orEmpty()

        val finalCast = if (resolvedCast.isNotEmpty()) {
            resolvedCast
        } else if (fallbackPrimaryId.isNotBlank()) {
            listOf(
                SceneCharacterPlacement(
                    characterId = fallbackPrimaryId,
                    expression = "Calm",
                    action = "Standing",
                    poseView = "Three-Quarter View",
                    posX = if (orderIndex % 2L == 0L) -80.0 else 80.0,
                    posY = 16.0,
                    scale = 1.0,
                    rotationDeg = 0.0
                )
            )
        } else {
            emptyList()
        }

        val resolvedDialogues = plan.dialogueLines.mapIndexed { dIdx, d ->
            val normSpk = normalizeCharKey(d.speakerName)
            val charId = codeToDocId[d.existingCharCode.uppercase()]
                ?: nameToDocId[normSpk]
                ?: workingLibrary.firstOrNull {
                    val ex = normalizeCharKey(it.name)
                    ex.isNotBlank() && (ex.contains(normSpk) || normSpk.contains(ex))
                }?.id
                ?: finalCast.firstOrNull()?.characterId.orEmpty()
            val matchedChar = workingLibrary.firstOrNull { it.id == charId }
            DialogueLine(
                lineId = "dlg_${UUID.randomUUID().toString().replace("-", "").take(10)}",
                characterId = charId,
                speakerName = matchedChar?.name ?: d.speakerName,
                text = d.text,
                language = language,
                startSec = d.startSec.coerceIn(0.0, plan.durationSec),
                durationSec = d.durationSec.coerceIn(0.5, plan.durationSec),
                voiceId = matchedChar?.voiceId.orEmpty(),
                emotion = d.emotion,
                orderIndex = dIdx
            )
        }

        val soundSuggestions = plan.soundSuggestions.mapIndexed { sIdx, s ->
            AiSoundSuggestion(
                suggestionId = "snd_${UUID.randomUUID().toString().replace("-", "").take(8)}_$sIdx",
                category = s.category,
                title = s.title,
                description = s.description,
                suggestedStartSec = s.suggestedStartSec.coerceIn(0.0, plan.durationSec),
                suggestedVolume = s.suggestedVolume,
                loopEnabled = s.loopEnabled,
                applied = false
            )
        }

        val primary = finalCast.firstOrNull()
        val draftScene = StudioScene(
            id = existingId ?: "scene_ai_${UUID.randomUUID().toString().replace("-", "").take(12)}",
            userId = currentUserId,
            orderIndex = orderIndex,
            title = plan.title,
            description = plan.description,
            visualSummary = plan.visualSummary,
            narrationText = plan.narrationText,
            subtitleText = plan.subtitleText,
            durationSec = plan.durationSec,
            backgroundPreset = plan.backgroundPreset,
            backgroundTintHex = "#1B2838",
            characterId = primary?.characterId.orEmpty(),
            characterCast = finalCast,
            scenePlan = plan.scenePlan,
            dialogueLines = resolvedDialogues,
            soundSuggestions = soundSuggestions,
            posX = primary?.posX ?: 0.0,
            posY = primary?.posY ?: 16.0,
            scale = primary?.scale ?: 1.0,
            rotationDeg = primary?.rotationDeg ?: 0.0,
            animationStyle = plan.animationStyle,
            cameraMovement = plan.cameraMovement,
            cameraZoom = 1.15,
            effectPreset = plan.effectPreset,
            effectIntensity = 0.35,
            motionGraphicTitle = plan.motionGraphicTitle,
            motionGraphicStyle = plan.motionGraphicStyle,
            musicCue = plan.musicCue,
            sfxCue = plan.sfxCue,
            objectProp = plan.objectProp
        )
        return draftScene.copy(
            lipSyncCues = LipSyncEngine.generateSceneLipSyncCues(draftScene, audioAssets)
        )
    }

    // --- PHASE 5: TARGETED REGENERATION, APPROVAL & NARRATION SYNC ---

    fun toggleSceneApproved(sceneId: String) {
        pushHistorySnapshot()
        _uiState.update { state ->
            val updated = state.scenes.map { sc ->
                if (sc.id == sceneId) {
                    sc.copy(scenePlan = sc.scenePlan.copy(approved = !sc.scenePlan.approved))
                } else sc
            }
            val nowApproved = updated.firstOrNull { it.id == sceneId }?.scenePlan?.approved == true
            state.copy(
                scenes = updated,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                saveStatus = SaveStatus.Unsaved,
                snackbarMessage = if (nowApproved) "Scene marked Approved (protected from script re-runs)" else "Scene unlocked for regeneration"
            )
        }
    }

    fun regenerateSingleScene(sceneId: String) {
        val state = _uiState.value
        if (state.regeneratingSceneTask != null) return
        val sceneIdx = state.scenes.indexOfFirst { it.id == sceneId }
        if (sceneIdx < 0) return
        val targetScene = state.scenes[sceneIdx]
        val project = state.activeProject ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(regeneratingSceneTask = "Regenerating Scene ${sceneIdx + 1}...") }
            val result = aiService.regenerateSingleScene(
                scriptText = project.scriptText,
                language = project.scriptLanguage,
                existingScene = targetScene,
                sceneIndex = sceneIdx,
                totalScenes = state.scenes.size,
                characters = state.characters
            )
            result.onSuccess { plan ->
                val chars = _uiState.value.characters
                val nameMap = chars.associate { normalizeCharKey(it.name) to it.id }
                val codeMap = chars.associate { it.charCode.uppercase() to it.id }
                val rebuilt = buildStudioSceneFromPlan(
                    plan = plan,
                    orderIndex = targetScene.orderIndex,
                    language = project.scriptLanguage,
                    workingLibrary = chars,
                    nameToDocId = nameMap,
                    codeToDocId = codeMap,
                    audioAssets = _uiState.value.audioAssets,
                    existingId = targetScene.id
                ).copy(
                    // Preserve user's existing staged visual elements, audio clips & keyframes on targeted regen
                    sceneElements = targetScene.sceneElements,
                    layerStates = targetScene.layerStates,
                    audioClips = targetScene.audioClips,
                    animationKeyframes = targetScene.animationKeyframes
                )
                pushHistorySnapshot()
                _uiState.update { s ->
                    s.copy(
                        scenes = s.scenes.map { if (it.id == targetScene.id) rebuilt else it },
                        regeneratingSceneTask = null,
                        canUndo = undoStack.isNotEmpty(),
                        canRedo = redoStack.isNotEmpty(),
                        saveStatus = SaveStatus.Unsaved,
                        snackbarMessage = "Regenerated Scene ${sceneIdx + 1} (other scenes untouched)"
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        regeneratingSceneTask = null,
                        errorMessage = err.localizedMessage ?: "Failed to regenerate scene."
                    )
                }
            }
        }
    }

    fun regenerateSelectedSceneNarration() {
        val state = _uiState.value
        val scene = state.selectedScene ?: return
        val project = state.activeProject ?: return
        if (state.regeneratingSceneTask != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(regeneratingSceneTask = "Refining narration...") }
            val result = aiService.regenerateSceneNarration(project.scriptText, project.scriptLanguage, scene)
            result.onSuccess { (narration, subtitle, estDuration) ->
                updateSelectedScene(recordUndo = true) {
                    it.copy(
                        narrationText = narration,
                        subtitleText = subtitle.ifBlank { it.subtitleText },
                        durationSec = estDuration.coerceAtLeast(it.durationSec.coerceAtMost(estDuration))
                    )
                }
                _uiState.update {
                    it.copy(
                        regeneratingSceneTask = null,
                        snackbarMessage = "Updated scene narration (~${"%.1f".format(estDuration)}s)"
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        regeneratingSceneTask = null,
                        errorMessage = err.localizedMessage ?: "Narration regeneration failed."
                    )
                }
            }
        }
    }

    fun regenerateSelectedSceneVisualDirection() {
        val state = _uiState.value
        val scene = state.selectedScene ?: return
        if (state.regeneratingSceneTask != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(regeneratingSceneTask = "Planning visual direction...") }
            val result = aiService.regenerateSceneVisualDirection(scene, state.characters)
            result.onSuccess { newPlan ->
                updateSelectedScene(recordUndo = true) {
                    it.copy(
                        scenePlan = newPlan,
                        visualSummary = newPlan.visualDirection.ifBlank { it.visualSummary }
                    )
                }
                _uiState.update {
                    it.copy(
                        regeneratingSceneTask = null,
                        snackbarMessage = "Updated visual direction & cinematic suggestions"
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        regeneratingSceneTask = null,
                        errorMessage = err.localizedMessage ?: "Visual direction update failed."
                    )
                }
            }
        }
    }

    fun regenerateSelectedSceneDialogue() {
        val state = _uiState.value
        val scene = state.selectedScene ?: return
        val project = state.activeProject ?: return
        if (state.regeneratingSceneTask != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(regeneratingSceneTask = "Extracting scene dialogue...") }
            val result = aiService.regenerateSceneDialogue(
                scriptText = project.scriptText,
                language = project.scriptLanguage,
                scene = scene,
                characters = state.characters
            )
            result.onSuccess { plans ->
                val chars = _uiState.value.characters
                val nameMap = chars.associate { normalizeCharKey(it.name) to it.id }
                val codeMap = chars.associate { it.charCode.uppercase() to it.id }
                val mapped = plans.mapIndexed { idx, p ->
                    val cid = codeMap[p.existingCharCode.uppercase()]
                        ?: nameMap[normalizeCharKey(p.speakerName)]
                        ?: scene.effectiveCast().firstOrNull()?.characterId
                        ?: chars.firstOrNull()?.id.orEmpty()
                    val matched = chars.firstOrNull { it.id == cid }
                    DialogueLine(
                        lineId = "dlg_${UUID.randomUUID().toString().replace("-", "").take(10)}",
                        characterId = cid,
                        speakerName = matched?.name ?: p.speakerName,
                        text = p.text,
                        language = project.scriptLanguage,
                        startSec = p.startSec.coerceIn(0.0, scene.durationSec),
                        durationSec = p.durationSec.coerceIn(0.5, scene.durationSec),
                        voiceId = matched?.voiceId.orEmpty(),
                        emotion = p.emotion,
                        orderIndex = idx
                    )
                }
                updateSelectedScene(recordUndo = true) { current ->
                    val withDlg = current.copy(dialogueLines = mapped)
                    withDlg.copy(lipSyncCues = LipSyncEngine.generateSceneLipSyncCues(withDlg, _uiState.value.audioAssets))
                }
                _uiState.update {
                    it.copy(
                        regeneratingSceneTask = null,
                        snackbarMessage = "Updated ${mapped.size} dialogue line(s) & synced lip-sync visemes"
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        regeneratingSceneTask = null,
                        errorMessage = err.localizedMessage ?: "Dialogue regeneration failed."
                    )
                }
            }
        }
    }

    fun applyScenePlanSuggestionsToSelectedScene(
        applyCamera: Boolean = true,
        applyLighting: Boolean = true,
        applyAtmosphere: Boolean = true
    ) {
        val scene = _uiState.value.selectedScene ?: return
        val plan = scene.scenePlan
        updateSelectedScene(recordUndo = true) { current ->
            var updated = current
            if (applyCamera && plan.cameraSuggestion.isNotBlank()) {
                val matchedCam = CinematicCameraPreset.entries.firstOrNull {
                    plan.cameraSuggestion.contains(it.label, ignoreCase = true)
                }
                if (matchedCam != null) {
                    updated = CinematicEngine.applyCameraPreset(updated, matchedCam)
                }
            }
            if (applyLighting && plan.lightingSuggestion.isNotBlank()) {
                val matchedLight = LightingPreset.entries.firstOrNull {
                    plan.lightingSuggestion.contains(it.label, ignoreCase = true)
                }
                if (matchedLight != null) {
                    updated = CinematicEngine.applyLightingPreset(updated, matchedLight)
                }
            }
            if (applyAtmosphere && plan.atmosphereSuggestion.isNotBlank()) {
                val matchedAtmo = com.example.cinematics.AtmosphereMode.entries.firstOrNull {
                    plan.atmosphereSuggestion.contains(it.label, ignoreCase = true)
                }
                if (matchedAtmo != null) {
                    updated = updated.copy(
                        cinematics = updated.cinematics.copy(
                            atmosphereMode = matchedAtmo.label,
                            atmosphereIntensity = if (matchedAtmo == com.example.cinematics.AtmosphereMode.NONE) 0.0 else 0.45
                        )
                    )
                }
            }
            updated
        }
        _uiState.update { it.copy(snackbarMessage = "Applied AI camera/lighting/atmosphere suggestions to scene") }
    }

    fun autoSyncSceneDurationToNarration(sceneId: String) {
        val state = _uiState.value
        val scene = state.scenes.firstOrNull { it.id == sceneId } ?: return
        val lang = state.activeProject?.scriptLanguage ?: "Bilingual"
        val narClipDur = scene.audioClips
            .filter { it.trackType == "NARRATION" }
            .maxOfOrNull { it.startSec + AudioTimelineEngine.trimmedSourceDuration(it) }
        val textEst = AudioTimelineEngine.estimateSpeechDurationSec(scene.narrationText, lang)
        val targetDur = (narClipDur ?: textEst).coerceIn(3.0, 120.0)
        pushHistorySnapshot()
        _uiState.update { s ->
            s.copy(
                scenes = s.scenes.map { if (it.id == sceneId) it.copy(durationSec = targetDur) else it },
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                saveStatus = SaveStatus.Unsaved,
                snackbarMessage = "Synced scene duration to ${"%.1f".format(targetDur)}s"
            )
        }
    }

    fun swapNarrationBetweenScenes(fromIndex: Int, toIndex: Int) {
        val scenes = _uiState.value.scenes
        if (fromIndex !in scenes.indices || toIndex !in scenes.indices || fromIndex == toIndex) return
        pushHistorySnapshot()
        val updated = scenes.toMutableList()
        val a = updated[fromIndex]
        val b = updated[toIndex]
        updated[fromIndex] = a.copy(narrationText = b.narrationText, subtitleText = b.subtitleText)
        updated[toIndex] = b.copy(narrationText = a.narrationText, subtitleText = a.subtitleText)
        _uiState.update {
            it.copy(
                scenes = updated,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                saveStatus = SaveStatus.Unsaved,
                snackbarMessage = "Swapped narration between Scene ${fromIndex + 1} and Scene ${toIndex + 1}"
            )
        }
    }

    // --- PHASE 5: AUDIO ASSET IMPORT, TTS, DIALOGUE, LIP-SYNC, MUSIC & SFX ---

    fun setAudioStudioTab(tab: AudioStudioTab) {
        _uiState.update { it.copy(activeAudioTab = tab) }
    }

    fun loadAvailableTtsVoices() {
        val engine = ttsEngine ?: return
        viewModelScope.launch {
            val voices = engine.listAvailableVoices()
            _uiState.update { it.copy(availableTtsVoices = voices) }
        }
    }

    fun importAudioAssetFromUri(
        uri: Uri,
        category: String,
        language: String = "English",
        autoAttachToScene: Boolean = true,
        dialogueLineId: String? = null
    ) {
        val manager = audioStorageManager ?: run {
            _uiState.update { it.copy(audioImportState = AudioImportState.Error("Audio storage service unavailable.")) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(audioImportState = AudioImportState.Importing("Analyzing audio...")) }
            val result = manager.importFromUri(uri, fallbackName = "$category Audio")
            result.onSuccess { info ->
                val newAsset = StudioAudioAsset(
                    userId = currentUserId,
                    name = info.displayName,
                    category = category,
                    sourceType = "IMPORTED",
                    storageState = "LOCAL_PERSISTENT",
                    localFilePath = info.localFilePath,
                    mimeType = info.mimeType,
                    fileSizeBytes = info.fileSizeBytes,
                    durationSec = info.durationSec,
                    language = language,
                    waveformPeaks = info.waveformPeaks,
                    notes = "Imported audio (${"%.1f".format(info.durationSec)}s)"
                )
                repository.saveAudioAsset(newAsset).onSuccess { savedId ->
                    val persisted = newAsset.copy(id = savedId)
                    _uiState.update { s ->
                        val already = s.audioAssets.any { it.id == savedId }
                        s.copy(
                            audioAssets = if (already) s.audioAssets else listOf(persisted) + s.audioAssets,
                            audioImportState = AudioImportState.Success(persisted.name, persisted.durationSec),
                            snackbarMessage = "Imported \"${persisted.name}\" (${"%.1f".format(persisted.durationSec)}s)"
                        )
                    }
                    if (!dialogueLineId.isNullOrBlank()) {
                        assignAudioAssetToDialogueLine(dialogueLineId, savedId)
                    } else if (autoAttachToScene) {
                        val trackKey = AudioTimelineEngine.trackTypeKeyForCategory(category)
                        addAudioClipFromAsset(
                            audioAsset = persisted,
                            trackType = trackKey,
                            scope = if (category == "Music") "PROJECT" else "SCENE",
                            loopEnabled = category == "Music" || category == "Ambient"
                        )
                    }
                }.onFailure { err ->
                    manager.deleteLocalFile(info.localFilePath)
                    _uiState.update {
                        it.copy(audioImportState = AudioImportState.Error(err.localizedMessage ?: "Failed to save audio metadata."))
                    }
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(audioImportState = AudioImportState.Error(err.localizedMessage ?: "Audio import failed."))
                }
            }
        }
    }

    fun deleteAudioAsset(audioAssetId: String) {
        val asset = _uiState.value.audioAssets.firstOrNull { it.id == audioAssetId } ?: return
        if (_uiState.value.previewingAudioAssetId == audioAssetId) {
            stopAudioPreview()
        }
        viewModelScope.launch {
            repository.deleteAudioAsset(audioAssetId).onSuccess {
                audioStorageManager?.deleteLocalFile(asset.localFilePath)
                _uiState.update { state ->
                    state.copy(
                        scenes = state.scenes.map { sc ->
                            sc.copy(
                                audioClips = sc.audioClips.filterNot { it.audioAssetId == audioAssetId },
                                dialogueLines = sc.dialogueLines.map { line ->
                                    if (line.audioAssetId == audioAssetId) line.copy(audioAssetId = "", clipId = "") else line
                                }
                            )
                        },
                        activeProject = state.activeProject?.copy(
                            projectAudioClips = state.activeProject.projectAudioClips.filterNot { it.audioAssetId == audioAssetId }
                        ),
                        snackbarMessage = "Removed audio asset \"${asset.name}\""
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(errorMessage = err.localizedMessage ?: "Failed to delete audio asset.") }
            }
        }
    }

    fun previewAudioAsset(audioAssetId: String) {
        val player = audioPlaybackEngine ?: return
        if (_uiState.value.previewingAudioAssetId == audioAssetId) {
            stopAudioPreview()
            return
        }
        val asset = _uiState.value.audioAssets.firstOrNull { it.id == audioAssetId } ?: return
        pausePlayback()
        _uiState.update { it.copy(previewingAudioAssetId = audioAssetId) }
        player.previewFile(
            localFilePath = asset.localFilePath,
            volume = 1.0f,
            onComplete = {
                _uiState.update { if (it.previewingAudioAssetId == audioAssetId) it.copy(previewingAudioAssetId = null) else it }
            },
            onError = { msg ->
                _uiState.update { it.copy(previewingAudioAssetId = null, errorMessage = msg) }
            }
        )
    }

    fun stopAudioPreview() {
        audioPlaybackEngine?.stopPreview()
        ttsEngine?.stopPreview()
        _uiState.update { it.copy(previewingAudioAssetId = null) }
    }

    fun previewTtsVoice(sampleText: String, language: String, voiceId: String, pitch: Float, rate: Float) {
        val engine = ttsEngine ?: return
        pausePlayback()
        viewModelScope.launch {
            engine.previewVoice(sampleText, language, voiceId, pitch, rate).onFailure { err ->
                _uiState.update { it.copy(errorMessage = err.localizedMessage ?: "Voice preview failed.") }
            }
        }
    }

    fun generateTtsForSelectedSceneNarration(
        language: String,
        voiceId: String,
        pitch: Float,
        rate: Float
    ) {
        val scene = _uiState.value.selectedScene ?: return
        val engine = ttsEngine ?: run {
            _uiState.update { it.copy(ttsGenState = TtsGenerationState.Error("TTS engine unavailable.")) }
            return
        }
        val manager = audioStorageManager ?: return
        val text = scene.narrationText.trim()
        if (text.isBlank()) {
            _uiState.update { it.copy(ttsGenState = TtsGenerationState.Error("Enter scene narration text first.")) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(ttsGenState = TtsGenerationState.Synthesizing("Synthesizing $language narration...")) }
            val outFile = manager.createPersistentWavFile("nar_${scene.orderIndex + 1}")
            val synthRes = engine.synthesizeToWavFile(text, language, voiceId, pitch, rate, outFile)
            synthRes.onSuccess { wavFile ->
                val info = manager.analyzeLocalAudioFile(
                    file = wavFile,
                    displayName = "Scene ${scene.orderIndex + 1} Narration ($language TTS)",
                    mimeType = "audio/wav"
                )
                val audioAsset = StudioAudioAsset(
                    userId = currentUserId,
                    name = info.displayName,
                    category = "Narration",
                    sourceType = "ANDROID_TTS",
                    storageState = "LOCAL_PERSISTENT",
                    localFilePath = info.localFilePath,
                    mimeType = "audio/wav",
                    fileSizeBytes = info.fileSizeBytes,
                    durationSec = info.durationSec,
                    language = language,
                    voiceId = voiceId,
                    waveformPeaks = info.waveformPeaks,
                    notes = "Generated with Android TTS (pitch=${"%.2f".format(pitch)}, rate=${"%.2f".format(rate)})"
                )
                repository.saveAudioAsset(audioAsset).onSuccess { savedId ->
                    val persisted = audioAsset.copy(id = savedId)
                    _uiState.update { s ->
                        val already = s.audioAssets.any { it.id == savedId }
                        s.copy(
                            audioAssets = if (already) s.audioAssets else listOf(persisted) + s.audioAssets,
                            ttsGenState = TtsGenerationState.Success(persisted.name, persisted.durationSec),
                            snackbarMessage = "Synthesized narration WAV (${"%.1f".format(persisted.durationSec)}s) & attached to scene"
                        )
                    }
                    addAudioClipFromAsset(
                        audioAsset = persisted,
                        trackType = "NARRATION",
                        scope = "SCENE",
                        startSec = 0.0,
                        replaceExistingNarration = true
                    )
                }.onFailure { err ->
                    manager.deleteLocalFile(info.localFilePath)
                    _uiState.update { it.copy(ttsGenState = TtsGenerationState.Error(err.localizedMessage ?: "Failed to persist TTS asset.")) }
                }
            }.onFailure { err ->
                manager.deleteLocalFile(outFile.absolutePath)
                _uiState.update { it.copy(ttsGenState = TtsGenerationState.Error(err.localizedMessage ?: "TTS synthesis failed.")) }
            }
        }
    }

    fun generateTtsForDialogueLine(lineId: String, languageOverride: String? = null) {
        val scene = _uiState.value.selectedScene ?: return
        val line = scene.dialogueLines.firstOrNull { it.lineId == lineId } ?: return
        val engine = ttsEngine ?: return
        val manager = audioStorageManager ?: return
        val character = _uiState.value.characters.firstOrNull { it.id == line.characterId }
        val lang = languageOverride ?: character?.voiceLanguage?.ifBlank { line.language } ?: line.language
        val voiceId = line.voiceId.ifBlank { character?.voiceId.orEmpty() }
        val pitch = (character?.voicePitch ?: 1.0).toFloat()
        val rate = (character?.voiceRate ?: 1.0).toFloat()

        viewModelScope.launch {
            _uiState.update { it.copy(ttsGenState = TtsGenerationState.Synthesizing("Synthesizing ${line.speakerName}...")) }
            val outFile = manager.createPersistentWavFile("dlg_${line.speakerName.take(10)}")
            val synthRes = engine.synthesizeToWavFile(line.text, lang, voiceId, pitch, rate, outFile)
            synthRes.onSuccess { wavFile ->
                val info = manager.analyzeLocalAudioFile(
                    file = wavFile,
                    displayName = "${line.speakerName.ifBlank { "Dialogue" }} ($lang TTS)",
                    mimeType = "audio/wav"
                )
                val audioAsset = StudioAudioAsset(
                    userId = currentUserId,
                    name = info.displayName,
                    category = "Dialogue",
                    sourceType = "ANDROID_TTS",
                    storageState = "LOCAL_PERSISTENT",
                    localFilePath = info.localFilePath,
                    mimeType = "audio/wav",
                    fileSizeBytes = info.fileSizeBytes,
                    durationSec = info.durationSec,
                    language = lang,
                    voiceId = voiceId,
                    characterId = line.characterId,
                    waveformPeaks = info.waveformPeaks,
                    notes = line.text.take(200)
                )
                repository.saveAudioAsset(audioAsset).onSuccess { savedId ->
                    val persisted = audioAsset.copy(id = savedId)
                    _uiState.update { s ->
                        val already = s.audioAssets.any { it.id == savedId }
                        s.copy(
                            audioAssets = if (already) s.audioAssets else listOf(persisted) + s.audioAssets,
                            ttsGenState = TtsGenerationState.Success(persisted.name, persisted.durationSec),
                            snackbarMessage = "Generated dialogue speech (${"%.1f".format(persisted.durationSec)}s) & updated lip-sync"
                        )
                    }
                    assignAudioAssetToDialogueLine(lineId, savedId)
                }.onFailure { err ->
                    manager.deleteLocalFile(info.localFilePath)
                    _uiState.update { it.copy(ttsGenState = TtsGenerationState.Error(err.localizedMessage ?: "Failed to save dialogue WAV.")) }
                }
            }.onFailure { err ->
                manager.deleteLocalFile(outFile.absolutePath)
                _uiState.update { it.copy(ttsGenState = TtsGenerationState.Error(err.localizedMessage ?: "Dialogue TTS failed.")) }
            }
        }
    }

    fun addDialogueLine(characterId: String, text: String, emotion: String = "Neutral") {
        val scene = _uiState.value.selectedScene ?: return
        val cleanText = text.trim()
        if (cleanText.isBlank()) return
        val character = _uiState.value.characters.firstOrNull { it.id == characterId }
        val lang = character?.voiceLanguage?.ifBlank { _uiState.value.activeProject?.scriptLanguage ?: "English" }
            ?: (_uiState.value.activeProject?.scriptLanguage ?: "English")
        val nextStart = scene.dialogueLines.maxOfOrNull { it.startSec + it.durationSec + 0.25 } ?: 0.5
        val estDur = AudioTimelineEngine.estimateSpeechDurationSec(cleanText, lang)
        val newLine = DialogueLine(
            lineId = "dlg_${UUID.randomUUID().toString().replace("-", "").take(10)}",
            characterId = characterId,
            speakerName = character?.name ?: "Speaker",
            text = cleanText.take(1200),
            language = lang,
            startSec = nextStart.coerceIn(0.0, (scene.durationSec - 0.5).coerceAtLeast(0.0)),
            durationSec = estDur.coerceIn(0.5, scene.durationSec),
            voiceId = character?.voiceId.orEmpty(),
            emotion = emotion,
            orderIndex = scene.dialogueLines.size
        )
        updateSelectedScene(recordUndo = true) { current ->
            val updated = current.copy(dialogueLines = (current.dialogueLines + newLine).take(40))
            updated.copy(lipSyncCues = LipSyncEngine.generateSceneLipSyncCues(updated, _uiState.value.audioAssets))
        }
    }

    fun updateDialogueLine(lineId: String, recordUndo: Boolean = true, transform: (DialogueLine) -> DialogueLine) {
        updateSelectedScene(recordUndo = recordUndo) { current ->
            val updatedLines = current.dialogueLines.map { line ->
                if (line.lineId == lineId) transform(line) else line
            }
            val updatedLine = updatedLines.firstOrNull { it.lineId == lineId }
            val syncedClips = if (updatedLine != null && updatedLine.clipId.isNotBlank()) {
                current.audioClips.map { clip ->
                    if (clip.clipId == updatedLine.clipId) {
                        clip.copy(startSec = updatedLine.startSec, characterId = updatedLine.characterId)
                    } else clip
                }
            } else current.audioClips

            val nextScene = current.copy(dialogueLines = updatedLines, audioClips = syncedClips)
            nextScene.copy(lipSyncCues = LipSyncEngine.generateSceneLipSyncCues(nextScene, _uiState.value.audioAssets))
        }
    }

    fun deleteDialogueLine(lineId: String) {
        updateSelectedScene(recordUndo = true) { current ->
            val removed = current.dialogueLines.firstOrNull { it.lineId == lineId }
            val nextLines = current.dialogueLines.filterNot { it.lineId == lineId }
                .mapIndexed { idx, l -> l.copy(orderIndex = idx) }
            val nextClips = if (removed?.clipId?.isNotBlank() == true) {
                current.audioClips.filterNot { it.clipId == removed.clipId }
            } else current.audioClips
            val nextScene = current.copy(dialogueLines = nextLines, audioClips = nextClips)
            nextScene.copy(lipSyncCues = LipSyncEngine.generateSceneLipSyncCues(nextScene, _uiState.value.audioAssets))
        }
    }

    fun reorderDialogueLine(lineId: String, delta: Int) {
        updateSelectedScene(recordUndo = true) { current ->
            val idx = current.dialogueLines.indexOfFirst { it.lineId == lineId }
            val target = idx + delta
            if (idx < 0 || target !in current.dialogueLines.indices) return@updateSelectedScene current
            val list = current.dialogueLines.toMutableList()
            val item = list.removeAt(idx)
            list.add(target, item)
            var cursor = 0.3
            val retimed = list.mapIndexed { i, l ->
                val placed = l.copy(orderIndex = i, startSec = cursor.coerceIn(0.0, current.durationSec))
                cursor += placed.durationSec + 0.25
                placed
            }
            val nextScene = current.copy(dialogueLines = retimed)
            nextScene.copy(lipSyncCues = LipSyncEngine.generateSceneLipSyncCues(nextScene, _uiState.value.audioAssets))
        }
    }

    fun assignAudioAssetToDialogueLine(lineId: String, audioAssetId: String) {
        val asset = _uiState.value.audioAssets.firstOrNull { it.id == audioAssetId } ?: return
        updateSelectedScene(recordUndo = true) { current ->
            val line = current.dialogueLines.firstOrNull { it.lineId == lineId } ?: return@updateSelectedScene current
            val clipId = line.clipId.ifBlank { "clip_dlg_${UUID.randomUUID().toString().replace("-", "").take(8)}" }
            val newClip = SceneAudioClip(
                clipId = clipId,
                audioAssetId = asset.id,
                title = "${line.speakerName}: ${line.text.take(24)}",
                trackType = "DIALOGUE",
                scope = "SCENE",
                startSec = line.startSec,
                sourceDurationSec = asset.durationSec.coerceAtLeast(0.5),
                trimStartSec = 0.0,
                trimEndSec = asset.durationSec.coerceAtLeast(0.5),
                volume = 1.0,
                fadeInSec = 0.05,
                fadeOutSec = 0.08,
                loopEnabled = false,
                muted = false,
                dialogueLineId = lineId,
                characterId = line.characterId
            )
            val updatedLines = current.dialogueLines.map { l ->
                if (l.lineId == lineId) {
                    l.copy(
                        audioAssetId = asset.id,
                        clipId = clipId,
                        durationSec = asset.durationSec.coerceIn(0.5, 120.0)
                    )
                } else l
            }
            val updatedClips = (current.audioClips.filterNot { it.clipId == clipId || it.dialogueLineId == lineId } + newClip).take(40)
            val nextScene = current.copy(dialogueLines = updatedLines, audioClips = updatedClips)
            nextScene.copy(lipSyncCues = LipSyncEngine.generateSceneLipSyncCues(nextScene, _uiState.value.audioAssets))
        }
    }

    fun updateCharacterVoiceSettings(
        characterId: String,
        voiceLanguage: String,
        voiceId: String,
        voicePitch: Double,
        voiceRate: Double
    ) {
        val character = _uiState.value.characters.firstOrNull { it.id == characterId } ?: return
        val updated = character.copy(
            voiceLanguage = voiceLanguage,
            voiceId = voiceId,
            voicePitch = voicePitch.coerceIn(0.5, 2.0),
            voiceRate = voiceRate.coerceIn(0.5, 2.0)
        )
        saveCharacter(updated)
    }

    fun regenerateLipSyncForSelectedScene() {
        updateSelectedScene(recordUndo = true) { current ->
            current.copy(lipSyncCues = LipSyncEngine.generateSceneLipSyncCues(current, _uiState.value.audioAssets))
        }
        _uiState.update { it.copy(snackbarMessage = "Recomputed deterministic lip-sync viseme cues from dialogue & audio") }
    }

    fun syncDialogueSpeakingToActingCues() {
        val scene = _uiState.value.selectedScene ?: return
        if (scene.dialogueLines.isEmpty()) {
            _uiState.update { it.copy(snackbarMessage = "Add dialogue lines first to sync character speaking states.") }
            return
        }
        updateSelectedScene(recordUndo = true) { current ->
            val syncedCues = LipSyncEngine.buildSpeakingActingCues(current)
            val visemes = LipSyncEngine.generateSceneLipSyncCues(current, _uiState.value.audioAssets)
            current.copy(characterActingCues = syncedCues, lipSyncCues = visemes)
        }
        _uiState.update { it.copy(snackbarMessage = "Synced character speaking/idle states to Phase 3 Acting Cues") }
    }

    fun addAudioClipFromAsset(
        audioAsset: StudioAudioAsset,
        trackType: String,
        scope: String = "SCENE",
        startSec: Double = 0.0,
        volume: Double = if (trackType == "MUSIC") 0.35 else if (trackType == "AMBIENT") 0.45 else 1.0,
        loopEnabled: Boolean = trackType == "MUSIC" || trackType == "AMBIENT",
        replaceExistingNarration: Boolean = false
    ) {
        val clipId = "clip_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        val dur = audioAsset.durationSec.coerceAtLeast(0.5)
        val newClip = SceneAudioClip(
            clipId = clipId,
            audioAssetId = audioAsset.id,
            title = audioAsset.name,
            trackType = trackType,
            scope = scope,
            startSec = startSec.coerceAtLeast(0.0),
            sourceDurationSec = dur,
            trimStartSec = 0.0,
            trimEndSec = dur,
            volume = volume.coerceIn(0.0, 1.5),
            fadeInSec = if (trackType == "MUSIC" || trackType == "AMBIENT") 0.8 else 0.05,
            fadeOutSec = if (trackType == "MUSIC" || trackType == "AMBIENT") 1.2 else 0.08,
            loopEnabled = loopEnabled,
            muted = false
        )

        if (scope == "PROJECT") {
            val proj = _uiState.value.activeProject ?: return
            pushHistorySnapshot()
            _uiState.update { state ->
                state.copy(
                    activeProject = proj.copy(
                        projectAudioClips = (proj.projectAudioClips + newClip).take(20)
                    ),
                    selectedAudioClipId = clipId,
                    canUndo = undoStack.isNotEmpty(),
                    canRedo = redoStack.isNotEmpty(),
                    saveStatus = SaveStatus.Unsaved,
                    snackbarMessage = "Added \"${audioAsset.name}\" to Project $trackType track"
                )
            }
        } else {
            updateSelectedScene(recordUndo = true) { current ->
                val base = if (replaceExistingNarration && trackType == "NARRATION") {
                    current.audioClips.filterNot { it.trackType == "NARRATION" }
                } else {
                    current.audioClips
                }
                current.copy(audioClips = (base + newClip).take(40))
            }
            _uiState.update {
                it.copy(
                    selectedAudioClipId = clipId,
                    snackbarMessage = "Added \"${audioAsset.name}\" to Scene $trackType track"
                )
            }
        }
    }

    fun selectAudioClip(clipId: String?) {
        _uiState.update { it.copy(selectedAudioClipId = clipId) }
    }

    fun updateAudioClip(clipId: String, recordUndo: Boolean = true, transform: (SceneAudioClip) -> SceneAudioClip) {
        val state = _uiState.value
        val inProject = state.activeProject?.projectAudioClips?.any { it.clipId == clipId } == true
        if (inProject) {
            val proj = state.activeProject ?: return
            if (recordUndo) pushHistorySnapshot()
            _uiState.update { s ->
                s.copy(
                    activeProject = proj.copy(
                        projectAudioClips = proj.projectAudioClips.map { c ->
                            if (c.clipId == clipId) transform(c) else c
                        }
                    ),
                    canUndo = undoStack.isNotEmpty(),
                    canRedo = redoStack.isNotEmpty(),
                    saveStatus = SaveStatus.Unsaved
                )
            }
        } else {
            updateSelectedScene(recordUndo = recordUndo) { current ->
                current.copy(
                    audioClips = current.audioClips.map { c ->
                        if (c.clipId == clipId) transform(c) else c
                    }
                )
            }
        }
        syncAudioEngineToCurrentPlayhead()
    }

    fun deleteAudioClip(clipId: String) {
        val state = _uiState.value
        val inProject = state.activeProject?.projectAudioClips?.any { it.clipId == clipId } == true
        if (inProject) {
            val proj = state.activeProject ?: return
            pushHistorySnapshot()
            _uiState.update { s ->
                s.copy(
                    activeProject = proj.copy(
                        projectAudioClips = proj.projectAudioClips.filterNot { it.clipId == clipId }
                    ),
                    selectedAudioClipId = if (s.selectedAudioClipId == clipId) null else s.selectedAudioClipId,
                    canUndo = undoStack.isNotEmpty(),
                    canRedo = redoStack.isNotEmpty(),
                    saveStatus = SaveStatus.Unsaved,
                    snackbarMessage = "Removed project audio clip"
                )
            }
        } else {
            updateSelectedScene(recordUndo = true) { current ->
                current.copy(
                    audioClips = current.audioClips.filterNot { it.clipId == clipId },
                    dialogueLines = current.dialogueLines.map { l ->
                        if (l.clipId == clipId) l.copy(clipId = "") else l
                    }
                )
            }
            _uiState.update {
                it.copy(
                    selectedAudioClipId = if (it.selectedAudioClipId == clipId) null else it.selectedAudioClipId,
                    snackbarMessage = "Removed audio clip from scene"
                )
            }
        }
        syncAudioEngineToCurrentPlayhead()
    }

    fun generateAiSoundSuggestionsForSelectedScene() {
        val scene = _uiState.value.selectedScene ?: return
        if (_uiState.value.soundSuggestionsState is SoundSuggestionsState.Loading) return
        viewModelScope.launch {
            _uiState.update { it.copy(soundSuggestionsState = SoundSuggestionsState.Loading) }
            val result = aiService.generateSceneSoundSuggestions(scene)
            result.onSuccess { suggestions ->
                updateSelectedScene(recordUndo = true) { it.copy(soundSuggestions = suggestions) }
                _uiState.update {
                    it.copy(
                        soundSuggestionsState = SoundSuggestionsState.Ready(suggestions.size),
                        snackbarMessage = "Generated ${suggestions.size} editable AI sound design suggestions"
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        soundSuggestionsState = SoundSuggestionsState.Error(
                            err.localizedMessage ?: "Failed to generate sound suggestions."
                        )
                    )
                }
            }
        }
    }

    fun applyAiSoundSuggestion(suggestionId: String) {
        updateSelectedScene(recordUndo = true) { current ->
            val target = current.soundSuggestions.firstOrNull { it.suggestionId == suggestionId }
                ?: return@updateSelectedScene current
            val updatedSuggestions = current.soundSuggestions.map { s ->
                if (s.suggestionId == suggestionId) s.copy(applied = true) else s
            }
            when (target.category) {
                "MUSIC" -> current.copy(musicCue = target.title, soundSuggestions = updatedSuggestions)
                else -> current.copy(sfxCue = target.title, soundSuggestions = updatedSuggestions)
            }
        }
        _uiState.update { it.copy(snackbarMessage = "Applied AI sound cue to scene (attach imported audio to mix)") }
    }

    fun dismissAiSoundSuggestion(suggestionId: String) {
        updateSelectedScene(recordUndo = true) { current ->
            current.copy(soundSuggestions = current.soundSuggestions.filterNot { it.suggestionId == suggestionId })
        }
    }

    // --- SCENE MANAGEMENT & MULTI-CHARACTER INSPECTOR MUTATIONS ---

    fun selectScene(sceneId: String) {
        val startTime = _uiState.value.startTimeOfScene(sceneId)
        val scene = _uiState.value.scenes.firstOrNull { it.id == sceneId }
        val firstCharId = scene?.effectiveCast()?.firstOrNull()?.characterId
        _uiState.update {
            it.copy(
                selectedSceneId = sceneId,
                selectedCastCharacterId = firstCharId,
                selectedStageTarget = null,
                playheadSec = startTime
            )
        }
    }

    fun selectCastCharacter(characterId: String) {
        _uiState.update {
            it.copy(
                selectedCastCharacterId = characterId,
                selectedStageTarget = SelectedStageTarget.CharacterTarget(characterId)
            )
        }
    }

    fun selectStageTarget(target: SelectedStageTarget?) {
        _uiState.update { state ->
            when (target) {
                is SelectedStageTarget.CharacterTarget -> state.copy(
                    selectedStageTarget = target,
                    selectedCastCharacterId = target.characterId
                )
                is SelectedStageTarget.ElementTarget -> state.copy(
                    selectedStageTarget = target
                )
                is SelectedStageTarget.GraphicTarget -> state.copy(
                    selectedStageTarget = target,
                    selectedGraphicId = target.graphicId
                )
                null -> state.copy(selectedStageTarget = null)
            }
        }
    }

    fun beginTransformGestureSnapshot() {
        pushHistorySnapshot()
        _uiState.update {
            it.copy(
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
    }

    fun dragStageTarget(target: SelectedStageTarget, deltaStageX: Double, deltaStageY: Double) {
        val state = _uiState.value
        val scene = state.selectedScene ?: return
        val localTime = state.selectedSceneLocalTimeSec
        when (target) {
            is SelectedStageTarget.CharacterTarget -> {
                val hasPosKeyframes = AnimationEvaluator.hasKeyframesForProperty(
                    scene.animationKeyframes, AnimationTargetType.CHARACTER, target.characterId, AnimatableProperty.POS_X
                ) || AnimationEvaluator.hasKeyframesForProperty(
                    scene.animationKeyframes, AnimationTargetType.CHARACTER, target.characterId, AnimatableProperty.POS_Y
                )
                if (hasPosKeyframes) {
                    val base = scene.effectiveCast().firstOrNull { it.characterId == target.characterId } ?: return
                    val curX = AnimationEvaluator.evaluateProperty(
                        scene.animationKeyframes, AnimationTargetType.CHARACTER, target.characterId,
                        AnimatableProperty.POS_X, localTime, base.posX
                    )
                    val curY = AnimationEvaluator.evaluateProperty(
                        scene.animationKeyframes, AnimationTargetType.CHARACTER, target.characterId,
                        AnimatableProperty.POS_Y, localTime, base.posY
                    )
                    val (nx, ny) = StageTransformMath.applyDragDelta(curX, curY, deltaStageX, deltaStageY)
                    updateSelectedScene(recordUndo = false) { s ->
                        var kfs = AnimationEvaluator.upsertKeyframe(
                            s.animationKeyframes, AnimationTargetType.CHARACTER, target.characterId,
                            AnimatableProperty.POS_X, localTime, nx, EasingType.EASE_IN_OUT, s.durationSec
                        )
                        kfs = AnimationEvaluator.upsertKeyframe(
                            kfs, AnimationTargetType.CHARACTER, target.characterId,
                            AnimatableProperty.POS_Y, localTime, ny, EasingType.EASE_IN_OUT, s.durationSec
                        )
                        s.copy(animationKeyframes = kfs)
                    }
                } else {
                    updateCastMemberInSelectedScene(target.characterId, recordUndo = false) { p ->
                        val (nx, ny) = StageTransformMath.applyDragDelta(p.posX, p.posY, deltaStageX, deltaStageY)
                        p.copy(posX = nx, posY = ny)
                    }
                }
            }
            is SelectedStageTarget.ElementTarget -> {
                val hasPosKeyframes = AnimationEvaluator.hasKeyframesForProperty(
                    scene.animationKeyframes, AnimationTargetType.ELEMENT, target.elementId, AnimatableProperty.POS_X
                ) || AnimationEvaluator.hasKeyframesForProperty(
                    scene.animationKeyframes, AnimationTargetType.ELEMENT, target.elementId, AnimatableProperty.POS_Y
                )
                if (hasPosKeyframes) {
                    val base = scene.sceneElements.firstOrNull { it.elementId == target.elementId } ?: return
                    val curX = AnimationEvaluator.evaluateProperty(
                        scene.animationKeyframes, AnimationTargetType.ELEMENT, target.elementId,
                        AnimatableProperty.POS_X, localTime, base.posX
                    )
                    val curY = AnimationEvaluator.evaluateProperty(
                        scene.animationKeyframes, AnimationTargetType.ELEMENT, target.elementId,
                        AnimatableProperty.POS_Y, localTime, base.posY
                    )
                    val (nx, ny) = StageTransformMath.applyDragDelta(curX, curY, deltaStageX, deltaStageY)
                    updateSelectedScene(recordUndo = false) { s ->
                        var kfs = AnimationEvaluator.upsertKeyframe(
                            s.animationKeyframes, AnimationTargetType.ELEMENT, target.elementId,
                            AnimatableProperty.POS_X, localTime, nx, EasingType.EASE_IN_OUT, s.durationSec
                        )
                        kfs = AnimationEvaluator.upsertKeyframe(
                            kfs, AnimationTargetType.ELEMENT, target.elementId,
                            AnimatableProperty.POS_Y, localTime, ny, EasingType.EASE_IN_OUT, s.durationSec
                        )
                        s.copy(animationKeyframes = kfs)
                    }
                } else {
                    updateVisualElementInSelectedScene(target.elementId, recordUndo = false) { el ->
                        val (nx, ny) = StageTransformMath.applyDragDelta(el.posX, el.posY, deltaStageX, deltaStageY)
                        el.copy(posX = nx, posY = ny)
                    }
                }
            }
            is SelectedStageTarget.GraphicTarget -> {
                val hasPosKeyframes = AnimationEvaluator.hasKeyframesForProperty(
                    scene.animationKeyframes, AnimationTargetType.GRAPHIC, target.graphicId, AnimatableProperty.POS_X
                ) || AnimationEvaluator.hasKeyframesForProperty(
                    scene.animationKeyframes, AnimationTargetType.GRAPHIC, target.graphicId, AnimatableProperty.POS_Y
                )
                if (hasPosKeyframes) {
                    val base = scene.motionGraphics.firstOrNull { it.graphicId == target.graphicId } ?: return
                    val curX = AnimationEvaluator.evaluateProperty(
                        scene.animationKeyframes, AnimationTargetType.GRAPHIC, target.graphicId,
                        AnimatableProperty.POS_X, localTime, base.posX
                    )
                    val curY = AnimationEvaluator.evaluateProperty(
                        scene.animationKeyframes, AnimationTargetType.GRAPHIC, target.graphicId,
                        AnimatableProperty.POS_Y, localTime, base.posY
                    )
                    val (nx, ny) = StageTransformMath.applyDragDelta(curX, curY, deltaStageX, deltaStageY)
                    updateSelectedScene(recordUndo = false) { s ->
                        var kfs = AnimationEvaluator.upsertKeyframe(
                            s.animationKeyframes, AnimationTargetType.GRAPHIC, target.graphicId,
                            AnimatableProperty.POS_X, localTime, nx, EasingType.EASE_IN_OUT, s.durationSec
                        )
                        kfs = AnimationEvaluator.upsertKeyframe(
                            kfs, AnimationTargetType.GRAPHIC, target.graphicId,
                            AnimatableProperty.POS_Y, localTime, ny, EasingType.EASE_IN_OUT, s.durationSec
                        )
                        s.copy(animationKeyframes = kfs)
                    }
                } else {
                    updateMotionGraphic(target.graphicId, recordUndo = false) { g ->
                        val (nx, ny) = StageTransformMath.applyDragDelta(g.posX, g.posY, deltaStageX, deltaStageY)
                        g.copy(posX = nx, posY = ny)
                    }
                }
            }
        }
    }

    fun scaleStageTarget(target: SelectedStageTarget, scaleFactor: Double) {
        val state = _uiState.value
        val scene = state.selectedScene ?: return
        val localTime = state.selectedSceneLocalTimeSec
        when (target) {
            is SelectedStageTarget.CharacterTarget -> {
                val hasScaleKf = AnimationEvaluator.hasKeyframesForProperty(
                    scene.animationKeyframes, AnimationTargetType.CHARACTER, target.characterId, AnimatableProperty.SCALE
                )
                if (hasScaleKf) {
                    val base = scene.effectiveCast().firstOrNull { it.characterId == target.characterId } ?: return
                    val curScale = AnimationEvaluator.evaluateProperty(
                        scene.animationKeyframes, AnimationTargetType.CHARACTER, target.characterId,
                        AnimatableProperty.SCALE, localTime, base.scale
                    )
                    val nextScale = StageTransformMath.applyResizeFactor(curScale, scaleFactor)
                    updateSelectedScene(recordUndo = false) { s ->
                        s.copy(
                            animationKeyframes = AnimationEvaluator.upsertKeyframe(
                                s.animationKeyframes, AnimationTargetType.CHARACTER, target.characterId,
                                AnimatableProperty.SCALE, localTime, nextScale, EasingType.EASE_IN_OUT, s.durationSec
                            )
                        )
                    }
                } else {
                    updateCastMemberInSelectedScene(target.characterId, recordUndo = false) { p ->
                        p.copy(scale = StageTransformMath.applyResizeFactor(p.scale, scaleFactor))
                    }
                }
            }
            is SelectedStageTarget.ElementTarget -> {
                val hasScaleKf = AnimationEvaluator.hasKeyframesForProperty(
                    scene.animationKeyframes, AnimationTargetType.ELEMENT, target.elementId, AnimatableProperty.SCALE
                )
                if (hasScaleKf) {
                    val base = scene.sceneElements.firstOrNull { it.elementId == target.elementId } ?: return
                    val curScale = AnimationEvaluator.evaluateProperty(
                        scene.animationKeyframes, AnimationTargetType.ELEMENT, target.elementId,
                        AnimatableProperty.SCALE, localTime, base.scale
                    )
                    val nextScale = StageTransformMath.applyResizeFactor(curScale, scaleFactor)
                    updateSelectedScene(recordUndo = false) { s ->
                        s.copy(
                            animationKeyframes = AnimationEvaluator.upsertKeyframe(
                                s.animationKeyframes, AnimationTargetType.ELEMENT, target.elementId,
                                AnimatableProperty.SCALE, localTime, nextScale, EasingType.EASE_IN_OUT, s.durationSec
                            )
                        )
                    }
                } else {
                    updateVisualElementInSelectedScene(target.elementId, recordUndo = false) { el ->
                        el.copy(scale = StageTransformMath.applyResizeFactor(el.scale, scaleFactor))
                    }
                }
            }
            is SelectedStageTarget.GraphicTarget -> {
                val hasScaleKf = AnimationEvaluator.hasKeyframesForProperty(
                    scene.animationKeyframes, AnimationTargetType.GRAPHIC, target.graphicId, AnimatableProperty.SCALE
                )
                if (hasScaleKf) {
                    val base = scene.motionGraphics.firstOrNull { it.graphicId == target.graphicId } ?: return
                    val curScale = AnimationEvaluator.evaluateProperty(
                        scene.animationKeyframes, AnimationTargetType.GRAPHIC, target.graphicId,
                        AnimatableProperty.SCALE, localTime, base.scale
                    )
                    val nextScale = StageTransformMath.applyResizeFactor(curScale, scaleFactor)
                    updateSelectedScene(recordUndo = false) { s ->
                        s.copy(
                            animationKeyframes = AnimationEvaluator.upsertKeyframe(
                                s.animationKeyframes, AnimationTargetType.GRAPHIC, target.graphicId,
                                AnimatableProperty.SCALE, localTime, nextScale, EasingType.EASE_IN_OUT, s.durationSec
                            )
                        )
                    }
                } else {
                    updateMotionGraphic(target.graphicId, recordUndo = false) { g ->
                        g.copy(scale = StageTransformMath.applyResizeFactor(g.scale, scaleFactor))
                    }
                }
            }
        }
    }

    fun rotateStageTarget(target: SelectedStageTarget, deltaDeg: Double) {
        val state = _uiState.value
        val scene = state.selectedScene ?: return
        val localTime = state.selectedSceneLocalTimeSec
        when (target) {
            is SelectedStageTarget.CharacterTarget -> {
                val hasRotKf = AnimationEvaluator.hasKeyframesForProperty(
                    scene.animationKeyframes, AnimationTargetType.CHARACTER, target.characterId, AnimatableProperty.ROTATION
                )
                if (hasRotKf) {
                    val base = scene.effectiveCast().firstOrNull { it.characterId == target.characterId } ?: return
                    val curRot = AnimationEvaluator.evaluateProperty(
                        scene.animationKeyframes, AnimationTargetType.CHARACTER, target.characterId,
                        AnimatableProperty.ROTATION, localTime, base.rotationDeg
                    )
                    val nextRot = StageTransformMath.normalizeRotation(curRot + deltaDeg)
                    updateSelectedScene(recordUndo = false) { s ->
                        s.copy(
                            animationKeyframes = AnimationEvaluator.upsertKeyframe(
                                s.animationKeyframes, AnimationTargetType.CHARACTER, target.characterId,
                                AnimatableProperty.ROTATION, localTime, nextRot, EasingType.EASE_IN_OUT, s.durationSec
                            )
                        )
                    }
                } else {
                    updateCastMemberInSelectedScene(target.characterId, recordUndo = false) { p ->
                        p.copy(rotationDeg = StageTransformMath.normalizeRotation(p.rotationDeg + deltaDeg))
                    }
                }
            }
            is SelectedStageTarget.ElementTarget -> {
                val hasRotKf = AnimationEvaluator.hasKeyframesForProperty(
                    scene.animationKeyframes, AnimationTargetType.ELEMENT, target.elementId, AnimatableProperty.ROTATION
                )
                if (hasRotKf) {
                    val base = scene.sceneElements.firstOrNull { it.elementId == target.elementId } ?: return
                    val curRot = AnimationEvaluator.evaluateProperty(
                        scene.animationKeyframes, AnimationTargetType.ELEMENT, target.elementId,
                        AnimatableProperty.ROTATION, localTime, base.rotationDeg
                    )
                    val nextRot = StageTransformMath.normalizeRotation(curRot + deltaDeg)
                    updateSelectedScene(recordUndo = false) { s ->
                        s.copy(
                            animationKeyframes = AnimationEvaluator.upsertKeyframe(
                                s.animationKeyframes, AnimationTargetType.ELEMENT, target.elementId,
                                AnimatableProperty.ROTATION, localTime, nextRot, EasingType.EASE_IN_OUT, s.durationSec
                            )
                        )
                    }
                } else {
                    updateVisualElementInSelectedScene(target.elementId, recordUndo = false) { el ->
                        el.copy(rotationDeg = StageTransformMath.normalizeRotation(el.rotationDeg + deltaDeg))
                    }
                }
            }
            is SelectedStageTarget.GraphicTarget -> {
                val hasRotKf = AnimationEvaluator.hasKeyframesForProperty(
                    scene.animationKeyframes, AnimationTargetType.GRAPHIC, target.graphicId, AnimatableProperty.ROTATION
                )
                if (hasRotKf) {
                    val base = scene.motionGraphics.firstOrNull { it.graphicId == target.graphicId } ?: return
                    val curRot = AnimationEvaluator.evaluateProperty(
                        scene.animationKeyframes, AnimationTargetType.GRAPHIC, target.graphicId,
                        AnimatableProperty.ROTATION, localTime, base.rotationDeg
                    )
                    val nextRot = StageTransformMath.normalizeRotation(curRot + deltaDeg)
                    updateSelectedScene(recordUndo = false) { s ->
                        s.copy(
                            animationKeyframes = AnimationEvaluator.upsertKeyframe(
                                s.animationKeyframes, AnimationTargetType.GRAPHIC, target.graphicId,
                                AnimatableProperty.ROTATION, localTime, nextRot, EasingType.EASE_IN_OUT, s.durationSec
                            )
                        )
                    }
                } else {
                    updateMotionGraphic(target.graphicId, recordUndo = false) { g ->
                        g.copy(rotationDeg = StageTransformMath.normalizeRotation(g.rotationDeg + deltaDeg))
                    }
                }
            }
        }
    }

    // --- PHASE 3: KEYFRAME ANIMATION & CHARACTER ACTING CUE MUTATIONS ---

    fun seekWithinSelectedScene(localTimeSec: Double) {
        val state = _uiState.value
        val scene = state.selectedScene ?: return
        val sceneStart = state.startTimeOfScene(scene.id)
        val clampedLocal = localTimeSec.coerceIn(0.0, scene.durationSec.coerceAtLeast(0.1))
        _uiState.update {
            it.copy(playheadSec = (sceneStart + clampedLocal).coerceIn(0.0, it.totalDurationSec))
        }
    }

    fun upsertKeyframeAtCurrentTime(
        targetType: AnimationTargetType,
        targetId: String,
        property: AnimatableProperty,
        value: Double,
        easing: EasingType = EasingType.EASE_IN_OUT
    ) {
        val state = _uiState.value
        val scene = state.selectedScene ?: return
        val localTime = state.selectedSceneLocalTimeSec
        val existingAtNow = AnimationEvaluator.findKeyframeAtTime(
            keyframes = scene.animationKeyframes,
            targetType = targetType,
            targetId = targetId,
            property = property,
            timeSec = localTime
        )
        // Record undo snapshot when creating a brand-new keyframe (avoid flooding undo stack on continuous slider drags)
        updateSelectedScene(recordUndo = existingAtNow == null) { s ->
            s.copy(
                animationKeyframes = AnimationEvaluator.upsertKeyframe(
                    existing = s.animationKeyframes,
                    targetType = targetType,
                    targetId = targetId,
                    property = property,
                    timeSec = localTime,
                    value = value,
                    easing = easing,
                    sceneDurationSec = s.durationSec
                )
            )
        }
    }

    fun updateKeyframeEasing(keyframeId: String, easing: EasingType) {
        updateSelectedScene(recordUndo = true) { s ->
            s.copy(
                animationKeyframes = s.animationKeyframes.map { kf ->
                    if (kf.keyframeId == keyframeId) kf.copy(easing = easing.name) else kf
                }
            )
        }
    }

    fun deleteKeyframe(keyframeId: String) {
        updateSelectedScene(recordUndo = true) { s ->
            s.copy(animationKeyframes = AnimationEvaluator.deleteKeyframe(s.animationKeyframes, keyframeId))
        }
    }

    fun clearTargetKeyframes(targetType: AnimationTargetType, targetId: String) {
        updateSelectedScene(recordUndo = true) { s ->
            s.copy(
                animationKeyframes = AnimationEvaluator.clearTargetKeyframes(
                    s.animationKeyframes, targetType, targetId
                )
            )
        }
        _uiState.update { it.copy(snackbarMessage = "Cleared keyframes for ${targetType.label.lowercase()}") }
    }

    fun jumpToPreviousKeyframe(targetType: AnimationTargetType, targetId: String) {
        val state = _uiState.value
        val scene = state.selectedScene ?: return
        val times = AnimationEvaluator.distinctTimesForTarget(
            scene.animationKeyframes, scene.characterActingCues, targetType, targetId
        )
        val prev = AnimationEvaluator.findPreviousTimestamp(times, state.selectedSceneLocalTimeSec) ?: return
        seekWithinSelectedScene(prev)
    }

    fun jumpToNextKeyframe(targetType: AnimationTargetType, targetId: String) {
        val state = _uiState.value
        val scene = state.selectedScene ?: return
        val times = AnimationEvaluator.distinctTimesForTarget(
            scene.animationKeyframes, scene.characterActingCues, targetType, targetId
        )
        val next = AnimationEvaluator.findNextTimestamp(times, state.selectedSceneLocalTimeSec) ?: return
        seekWithinSelectedScene(next)
    }

    fun applyCharacterTransformPreset(characterId: String, presetKey: String) {
        val scene = _uiState.value.selectedScene ?: return
        val placement = scene.effectiveCast().firstOrNull { it.characterId == characterId } ?: return
        updateSelectedScene(recordUndo = true) { s ->
            s.copy(
                animationKeyframes = AnimationEvaluator.applyCharacterTransformPreset(
                    existing = s.animationKeyframes,
                    placement = placement,
                    sceneDurationSec = s.durationSec,
                    presetKey = presetKey
                )
            )
        }
        _uiState.update { it.copy(snackbarMessage = "Applied transform motion keyframes") }
    }

    fun applyCameraKeyframePreset(presetKey: String) {
        updateSelectedScene(recordUndo = true) { s ->
            s.copy(
                animationKeyframes = AnimationEvaluator.applyCameraKeyframePreset(
                    existing = s.animationKeyframes,
                    sceneDurationSec = s.durationSec,
                    presetKey = presetKey
                )
            )
        }
        _uiState.update { it.copy(snackbarMessage = "Applied camera motion keyframes") }
    }

    fun upsertActingCueAtCurrentTime(
        characterId: String,
        expression: String,
        action: String,
        poseView: String,
        referenceSheetId: String = ""
    ) {
        val state = _uiState.value
        val localTime = state.selectedSceneLocalTimeSec
        updateSelectedScene(recordUndo = true) { s ->
            s.copy(
                characterActingCues = AnimationEvaluator.upsertActingCue(
                    existing = s.characterActingCues,
                    characterId = characterId,
                    timeSec = localTime,
                    expression = expression,
                    action = action,
                    poseView = poseView,
                    referenceSheetId = referenceSheetId,
                    sceneDurationSec = s.durationSec
                )
            )
        }
        _uiState.update {
            it.copy(snackbarMessage = "Set acting cue ($expression · $action) at ${String.format("%.2f", localTime)}s")
        }
    }

    fun deleteActingCue(cueId: String) {
        updateSelectedScene(recordUndo = true) { s ->
            s.copy(characterActingCues = AnimationEvaluator.deleteActingCue(s.characterActingCues, cueId))
        }
    }

    // --- 6-LAYER SCENE COMPOSITOR & VISUAL ELEMENT MUTATIONS ---

    fun toggleSceneLayerVisibility(layer: SceneLayerType) {
        updateSelectedScene(recordUndo = true) { scene ->
            scene.copy(layerStates = SceneCompositor.toggleLayerVisibility(scene.layerStates, layer))
        }
    }

    fun moveSceneLayerOrder(layer: SceneLayerType, delta: Int) {
        updateSelectedScene(recordUndo = true) { scene ->
            scene.copy(layerStates = SceneCompositor.moveLayerOrder(scene.layerStates, layer, delta))
        }
    }

    fun updateSceneLayerOpacity(layer: SceneLayerType, opacity: Double) {
        updateSelectedScene(recordUndo = false) { scene ->
            scene.copy(layerStates = SceneCompositor.updateLayerOpacity(scene.layerStates, layer, opacity))
        }
    }

    fun updateSceneLayerParallax(layer: SceneLayerType, parallaxFactor: Double) {
        updateSelectedScene(recordUndo = false) { scene ->
            scene.copy(layerStates = SceneCompositor.updateLayerParallax(scene.layerStates, layer, parallaxFactor))
        }
    }

    // --- PHASE 4: CINEMATIC CAMERA, DEPTH, LIGHTING, ATMOSPHERE & VFX ---

    fun toggleFramingGuides() {
        _uiState.update { it.copy(showFramingGuides = !it.showFramingGuides) }
    }

    fun applyCinematicCameraPreset(preset: CinematicCameraPreset) {
        updateSelectedScene(recordUndo = true) { s ->
            CinematicEngine.applyCameraPreset(s, preset)
        }
        val modeNote = if (preset.isAnimatedMotion) "with editable keyframes" else "framing"
        _uiState.update { it.copy(snackbarMessage = "Applied ${preset.label} ($modeNote)") }
    }

    fun resetCameraToFit() {
        updateSelectedScene(recordUndo = true) { s ->
            CinematicEngine.resetCameraToFit(s)
        }
        _uiState.update { it.copy(snackbarMessage = "Reset camera to fit 16:9 scene") }
    }

    fun applyLightingPreset(preset: LightingPreset) {
        updateSelectedScene(recordUndo = true) { s ->
            CinematicEngine.applyLightingPreset(s, preset)
        }
        _uiState.update { it.copy(snackbarMessage = "Applied ${preset.label} lighting") }
    }

    fun updateSceneCinematics(
        recordUndo: Boolean = false,
        transform: (SceneCinematics) -> SceneCinematics
    ) {
        updateSelectedScene(recordUndo = recordUndo) { s ->
            s.copy(cinematics = transform(s.cinematics))
        }
    }

    fun addVisualElementFromAsset(
        asset: StudioAsset,
        targetLayer: SceneLayerType = SceneLayerType.PROPS
    ) {
        val scene = _uiState.value.selectedScene ?: return
        if (scene.sceneElements.size >= 24) {
            _uiState.update { it.copy(snackbarMessage = "Maximum 24 visual elements per scene reached.") }
            return
        }
        val hasVisual = SceneCompositor.hasRenderableVisualAsset(
            elementUri = asset.localUri,
            elementPresetKey = asset.presetKey,
            linkedAsset = asset
        )
        if (!hasVisual) {
            _uiState.update {
                it.copy(snackbarMessage = "\"${asset.name}\" has no image file. Import a PNG/JPEG asset to place it on stage.")
            }
            return
        }
        val nextZ = (scene.sceneElements.maxOfOrNull { it.zIndex } ?: 0) + 1
        val newElement = SceneVisualElement(
            elementId = "el_${UUID.randomUUID().toString().replace("-", "").take(12)}",
            name = asset.name.trim().take(120).ifBlank { "Visual Element" },
            elementType = when (targetLayer) {
                SceneLayerType.BACKGROUND -> "BACKGROUND"
                SceneLayerType.MIDGROUND -> "MIDGROUND"
                SceneLayerType.FOREGROUND -> "FOREGROUND"
                else -> "PROP"
            },
            assetId = asset.id,
            assetPresetKey = asset.presetKey.takeIf { it in SceneCompositor.bundledDrawablePresetKeys }.orEmpty(),
            assetUri = asset.localUri.takeIf { SceneCompositor.isSafeVisualUri(it) }.orEmpty(),
            layer = targetLayer.name,
            posX = ((scene.sceneElements.size % 5) - 2) * 55.0,
            posY = 20.0,
            scale = if (targetLayer == SceneLayerType.PROPS) 0.85 else 1.0,
            rotationDeg = 0.0,
            zIndex = StageTransformMath.clampZIndex(nextZ),
            visible = true,
            opacity = 1.0,
            flipX = false
        )
        updateSelectedScene(recordUndo = true) { current ->
            current.copy(
                sceneElements = current.sceneElements + newElement,
                objectProp = if (targetLayer == SceneLayerType.PROPS && current.objectProp.isBlank()) {
                    newElement.name
                } else {
                    current.objectProp
                }
            )
        }
        _uiState.update {
            it.copy(
                selectedStageTarget = SelectedStageTarget.ElementTarget(newElement.elementId),
                snackbarMessage = "Placed \"${newElement.name}\" on ${targetLayer.label} layer"
            )
        }
    }

    fun importAndPlaceVisualElement(
        name: String,
        uriString: String,
        targetLayer: SceneLayerType = SceneLayerType.PROPS
    ) {
        if (!SceneCompositor.isSafeVisualUri(uriString)) {
            _uiState.update { it.copy(snackbarMessage = "Unsupported image URI.") }
            return
        }
        val cleanName = name.trim().take(120).ifBlank { "Stage Asset" }
        val category = when (targetLayer) {
            SceneLayerType.BACKGROUND -> "Background"
            SceneLayerType.MIDGROUND, SceneLayerType.FOREGROUND -> "Image"
            else -> "Prop"
        }
        viewModelScope.launch {
            val asset = StudioAsset(
                name = cleanName,
                category = category,
                tags = "compositor,${targetLayer.name.lowercase()}",
                notes = "Imported visual asset for ${targetLayer.label} layer",
                presetKey = "custom_photo",
                localUri = uriString,
                durationLabel = "Image"
            )
            repository.saveAsset(asset)
                .onSuccess { savedAssetId ->
                    addVisualElementFromAsset(asset.copy(id = savedAssetId), targetLayer)
                }
                .onFailure {
                    // Still allow immediate local staging on canvas even if asset library save failed
                    addVisualElementFromAsset(asset, targetLayer)
                }
        }
    }

    fun removeVisualElementFromSelectedScene(elementId: String) {
        updateSelectedScene(recordUndo = true) { scene ->
            scene.copy(sceneElements = scene.sceneElements.filterNot { it.elementId == elementId })
        }
        _uiState.update { state ->
            val clearedTarget = if (
                state.selectedStageTarget is SelectedStageTarget.ElementTarget &&
                state.selectedStageTarget.elementId == elementId
            ) {
                null
            } else {
                state.selectedStageTarget
            }
            state.copy(
                selectedStageTarget = clearedTarget,
                snackbarMessage = "Removed visual element from scene"
            )
        }
    }

    fun updateVisualElementInSelectedScene(
        elementId: String,
        recordUndo: Boolean = false,
        transform: (SceneVisualElement) -> SceneVisualElement
    ) {
        updateSelectedScene(recordUndo = recordUndo) { scene ->
            scene.copy(
                sceneElements = scene.sceneElements.map { el ->
                    if (el.elementId == elementId) transform(el) else el
                }
            )
        }
    }

    fun nudgeElementZIndex(elementId: String, delta: Int) {
        updateVisualElementInSelectedScene(elementId, recordUndo = true) { el ->
            el.copy(zIndex = StageTransformMath.clampZIndex(el.zIndex + delta))
        }
    }

    fun nudgeCharacterZIndex(characterId: String, delta: Int) {
        updateCastMemberInSelectedScene(characterId, recordUndo = true) { placement ->
            placement.copy(zIndex = StageTransformMath.clampZIndex(placement.zIndex + delta))
        }
    }

    fun addScene() {
        pushHistorySnapshot()
        val currentScenes = _uiState.value.scenes
        val nextNum = currentScenes.size + 1
        val defaultChar = _uiState.value.characters.firstOrNull()
        val initialCast = if (defaultChar != null) {
            listOf(
                SceneCharacterPlacement(
                    characterId = defaultChar.id,
                    expression = defaultChar.expressionDefault.ifBlank { "Calm" },
                    action = "Standing",
                    poseView = "Three-Quarter View",
                    posX = 0.0,
                    posY = 16.0,
                    scale = 1.0,
                    rotationDeg = 0.0
                )
            )
        } else {
            emptyList()
        }
        val newScene = StudioScene(
            id = "scene_${UUID.randomUUID().toString().replace("-", "").take(12)}",
            userId = currentUserId,
            orderIndex = currentScenes.size.toLong(),
            title = "Scene $nextNum · Documentary Beat",
            description = "Describe the narrative beat, environment, and visual pacing.",
            visualSummary = "2D layered animated shot with gentle camera push.",
            narrationText = "",
            subtitleText = "",
            durationSec = 7.0,
            backgroundPreset = StudioPresets.backgrounds[nextNum % StudioPresets.backgrounds.size].first,
            characterId = defaultChar?.id.orEmpty(),
            characterCast = initialCast,
            motionGraphicTitle = "SCENE 0$nextNum",
            motionGraphicStyle = "Lower Third"
        )
        _uiState.update {
            it.copy(
                scenes = currentScenes + newScene,
                selectedSceneId = newScene.id,
                selectedCastCharacterId = defaultChar?.id,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                saveStatus = SaveStatus.Unsaved
            )
        }
    }

    fun deleteScene(sceneId: String) {
        val currentScenes = _uiState.value.scenes
        if (currentScenes.size <= 1) {
            _uiState.update { it.copy(snackbarMessage = "A project needs at least one scene.") }
            return
        }
        pushHistorySnapshot()
        val updated = currentScenes
            .filterNot { it.id == sceneId }
            .mapIndexed { idx, s -> s.copy(orderIndex = idx.toLong()) }
        val nextSelected = if (_uiState.value.selectedSceneId == sceneId) updated.firstOrNull() else _uiState.value.selectedScene
        _uiState.update {
            it.copy(
                scenes = updated,
                selectedSceneId = nextSelected?.id,
                selectedCastCharacterId = nextSelected?.effectiveCast()?.firstOrNull()?.characterId,
                playheadSec = it.playheadSec.coerceAtMost(updated.sumOf { sc -> sc.durationSec }),
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                saveStatus = SaveStatus.Unsaved
            )
        }
    }

    fun moveScene(sceneId: String, delta: Int) {
        val currentScenes = _uiState.value.scenes
        val idx = currentScenes.indexOfFirst { it.id == sceneId }
        val target = idx + delta
        if (idx == -1 || target !in currentScenes.indices) return
        pushHistorySnapshot()
        val mutable = currentScenes.toMutableList()
        val item = mutable.removeAt(idx)
        mutable.add(target, item)
        val reindexed = mutable.mapIndexed { i, s -> s.copy(orderIndex = i.toLong()) }
        _uiState.update {
            it.copy(
                scenes = reindexed,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                saveStatus = SaveStatus.Unsaved
            )
        }
    }

    fun updateSelectedScene(recordUndo: Boolean = true, transform: (StudioScene) -> StudioScene) {
        val targetId = _uiState.value.selectedScene?.id ?: return
        if (recordUndo) pushHistorySnapshot()
        _uiState.update { state ->
            val updated = state.scenes.map { scene ->
                if (scene.id == targetId) transform(scene) else scene
            }
            state.copy(
                scenes = updated,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                saveStatus = SaveStatus.Unsaved
            )
        }
    }

    fun addCharacterToSelectedScene(characterId: String) {
        val scene = _uiState.value.selectedScene ?: return
        val character = _uiState.value.characters.firstOrNull { it.id == characterId } ?: return
        val currentCast = scene.effectiveCast()
        if (currentCast.any { it.characterId == characterId }) {
            _uiState.update {
                it.copy(
                    selectedCastCharacterId = characterId,
                    snackbarMessage = "${character.name} is already assigned to this scene"
                )
            }
            return
        }
        if (currentCast.size >= 12) {
            _uiState.update { it.copy(snackbarMessage = "Maximum 12 characters per scene reached.") }
            return
        }
        val staggeredX = when (currentCast.size) {
            0 -> 0.0
            1 -> 115.0
            2 -> -115.0
            else -> ((currentCast.size % 5) - 2) * 75.0
        }
        val placement = SceneCharacterPlacement(
            characterId = character.id,
            expression = character.expressionDefault.ifBlank { "Calm" },
            action = "Standing",
            poseView = "Three-Quarter View",
            posX = staggeredX,
            posY = 16.0,
            scale = if (currentCast.isEmpty()) 1.0 else 0.94,
            rotationDeg = 0.0
        )
        val newCast = currentCast + placement
        updateSelectedScene(recordUndo = true) {
            it.copy(
                characterId = newCast.first().characterId,
                characterCast = newCast,
                posX = newCast.first().posX,
                posY = newCast.first().posY,
                scale = newCast.first().scale,
                rotationDeg = newCast.first().rotationDeg
            )
        }
        _uiState.update {
            it.copy(
                selectedCastCharacterId = character.id,
                snackbarMessage = "Added ${character.charCode} ${character.name} to scene"
            )
        }
    }

    fun removeCharacterFromSelectedScene(characterId: String) {
        val scene = _uiState.value.selectedScene ?: return
        val newCast = scene.effectiveCast().filterNot { it.characterId == characterId }
        val primary = newCast.firstOrNull()
        updateSelectedScene(recordUndo = true) {
            it.copy(
                characterId = primary?.characterId.orEmpty(),
                characterCast = newCast,
                posX = primary?.posX ?: 0.0,
                posY = primary?.posY ?: 0.0,
                scale = primary?.scale ?: 1.0,
                rotationDeg = primary?.rotationDeg ?: 0.0
            )
        }
        _uiState.update {
            it.copy(
                selectedCastCharacterId = primary?.characterId,
                snackbarMessage = "Removed character from scene"
            )
        }
    }

    fun updateCastMemberInSelectedScene(
        characterId: String,
        recordUndo: Boolean = false,
        transform: (SceneCharacterPlacement) -> SceneCharacterPlacement
    ) {
        updateSelectedScene(recordUndo = recordUndo) { scene ->
            val updatedCast = scene.effectiveCast().map { placement ->
                if (placement.characterId == characterId) transform(placement) else placement
            }
            val primary = updatedCast.firstOrNull()
            scene.copy(
                characterId = primary?.characterId.orEmpty(),
                characterCast = updatedCast,
                posX = primary?.posX ?: scene.posX,
                posY = primary?.posY ?: scene.posY,
                scale = primary?.scale ?: scene.scale,
                rotationDeg = primary?.rotationDeg ?: scene.rotationDeg
            )
        }
    }

    // --- CHARACTER LIBRARY & CONSISTENCY ENGINE ---

    fun selectLibraryCharacter(characterId: String) {
        _uiState.update {
            it.copy(
                selectedLibraryCharacterId = characterId,
                characterImageGenState = CharacterImageGenState.Idle
            )
        }
        if (observedCharRefId != characterId) {
            observeCharacterReferences(characterId)
        }
    }

    fun saveCharacter(character: StudioCharacter) {
        val cleanName = character.name.trim().take(100)
        if (cleanName.isBlank()) {
            _uiState.update { it.copy(snackbarMessage = "Character name cannot be empty.") }
            return
        }
        viewModelScope.launch {
            val withSafeCode = character.copy(
                name = cleanName,
                charCode = character.charCode.trim().take(32)
                    .ifBlank { nextAvailableCharacterCode(_uiState.value.characters) }
            )
            repository.saveCharacter(withSafeCode)
                .onSuccess { docId ->
                    selectLibraryCharacter(docId)
                    _uiState.update {
                        it.copy(snackbarMessage = "Saved ${withSafeCode.charCode} · ${withSafeCode.name}")
                    }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(errorMessage = err.localizedMessage ?: "Failed to save character.") }
                }
        }
    }

    fun renameCharacter(character: StudioCharacter, newName: String) {
        val clean = newName.trim().take(100)
        if (clean.isBlank()) {
            _uiState.update { it.copy(snackbarMessage = "Character name cannot be empty.") }
            return
        }
        saveCharacter(character.copy(name = clean))
    }

    fun deleteCharacter(characterId: String) {
        viewModelScope.launch {
            repository.deleteCharacter(characterId)
                .onSuccess {
                    // Clean up references to deleted character in currently loaded scenes
                    _uiState.update { state ->
                        var modifiedScenes = false
                        val cleanedScenes = state.scenes.map { sc ->
                            val beforeCast = sc.effectiveCast()
                            val newCast = beforeCast.filterNot { it.characterId == characterId }
                            if (newCast.size != beforeCast.size || sc.characterId == characterId) {
                                modifiedScenes = true
                            }
                            val primary = newCast.firstOrNull()
                            sc.copy(
                                characterId = primary?.characterId.orEmpty(),
                                characterCast = newCast
                            )
                        }
                        state.copy(
                            scenes = cleanedScenes,
                            saveStatus = if (modifiedScenes) SaveStatus.Unsaved else state.saveStatus,
                            snackbarMessage = if (modifiedScenes) {
                                "Character deleted & removed from scenes. Tap Save to sync scenes."
                            } else {
                                "Character permanently deleted from library"
                            }
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(errorMessage = err.localizedMessage ?: "Failed to delete character.") }
                }
        }
    }

    fun generateCharacterReferenceDesign(
        character: StudioCharacter,
        viewType: String,
        expression: String,
        poseAction: String,
        visualStyle: String,
        useAnchorReference: Boolean,
        setAsPrimary: Boolean
    ) {
        if (_uiState.value.characterImageGenState is CharacterImageGenState.Generating) return
        if (character.id.isBlank()) {
            _uiState.update {
                it.copy(characterImageGenState = CharacterImageGenState.Error("Save the character profile before generating a reference image."))
            }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    characterImageGenState = CharacterImageGenState.Generating(
                        characterName = character.name,
                        viewType = viewType
                    )
                )
            }
            val result = characterImageService.generateCharacterReference(
                character = character,
                viewType = viewType,
                expression = expression,
                poseAction = poseAction,
                visualStyle = visualStyle,
                useAnchorReference = useAnchorReference
            )
            result.onSuccess { generated ->
                val refSheet = CharacterReferenceSheet(
                    userId = currentUserId,
                    viewType = viewType,
                    expression = expression,
                    poseAction = poseAction,
                    visualStyle = visualStyle,
                    promptSummary = generated.promptSummary,
                    imageBase64 = generated.base64Jpeg
                )
                val shouldUpdatePrimary = setAsPrimary || character.referenceImageBase64.isBlank()
                repository.saveCharacterReference(
                    characterId = character.id,
                    reference = refSheet,
                    alsoSetAsPrimary = shouldUpdatePrimary
                ).onSuccess {
                    _uiState.update {
                        it.copy(
                            characterImageGenState = CharacterImageGenState.Success(
                                characterId = character.id,
                                viewType = viewType
                            ),
                            snackbarMessage = "Generated & saved $viewType reference for ${character.name}"
                        )
                    }
                }.onFailure { saveErr ->
                    _uiState.update {
                        it.copy(
                            characterImageGenState = CharacterImageGenState.Error(
                                saveErr.localizedMessage ?: "Generated image could not be saved to Firestore."
                            )
                        )
                    }
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        characterImageGenState = CharacterImageGenState.Error(
                            err.localizedMessage ?: "Character image generation failed."
                        )
                    )
                }
            }
        }
    }

    fun setReferenceAsPrimary(characterId: String, imageBase64: String) {
        viewModelScope.launch {
            repository.setPrimaryCharacterReference(characterId, imageBase64)
                .onSuccess {
                    _uiState.update { it.copy(snackbarMessage = "Updated primary character reference image") }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(errorMessage = err.localizedMessage ?: "Failed to update primary image.") }
                }
        }
    }

    fun deleteCharacterReference(characterId: String, referenceId: String) {
        viewModelScope.launch {
            repository.deleteCharacterReference(characterId, referenceId)
                .onSuccess {
                    _uiState.update { it.copy(snackbarMessage = "Removed reference sheet") }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(errorMessage = err.localizedMessage ?: "Failed to delete reference.") }
                }
        }
    }

    fun clearCharacterImageGenState() {
        _uiState.update { it.copy(characterImageGenState = CharacterImageGenState.Idle) }
    }

    // --- ASSET & LOCATION LIBRARY CRUD ---

    fun saveAsset(asset: StudioAsset) {
        viewModelScope.launch {
            repository.saveAsset(asset)
                .onSuccess {
                    _uiState.update { it.copy(snackbarMessage = "Asset \"${asset.name}\" added to library") }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(errorMessage = err.localizedMessage ?: "Failed to save asset.") }
                }
        }
    }

    fun deleteAsset(assetId: String) {
        viewModelScope.launch {
            repository.deleteAsset(assetId)
                .onSuccess {
                    _uiState.update { it.copy(snackbarMessage = "Asset removed") }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(errorMessage = err.localizedMessage ?: "Failed to delete asset.") }
                }
        }
    }

    // --- PHASE 2: AI SCENE BACKGROUND, VISUAL PROP & SCENE SUGGESTION ENGINE ---

    fun defaultBackgroundRequestForSelectedScene(): BackgroundGenerationRequest {
        val scene = _uiState.value.selectedScene ?: StudioScene()
        val projectStyle = _uiState.value.characters.firstOrNull()?.visualStyle
            ?.ifBlank { "Painterly 2D Documentary" }
            ?: "Painterly 2D Documentary"
        return visualAiService.defaultBackgroundRequestForScene(scene, projectStyle)
    }

    fun defaultPropRequestForSelectedScene(): PropGenerationRequest {
        val scene = _uiState.value.selectedScene ?: StudioScene()
        val projectStyle = _uiState.value.characters.firstOrNull()?.visualStyle
            ?.ifBlank { "Painterly 2D Documentary" }
            ?: "Painterly 2D Documentary"
        return visualAiService.defaultPropRequestForScene(scene, projectStyle)
    }

    fun buildEditableBackgroundSummary(request: BackgroundGenerationRequest): String {
        return visualAiService.buildEditableBackgroundSummary(request)
    }

    fun generateSceneBackground(request: BackgroundGenerationRequest) {
        if (_uiState.value.backgroundGenState is VisualAssetGenState.Generating) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    backgroundGenState = VisualAssetGenState.Generating(
                        "Generating 16:9 2D scene background..."
                    )
                )
            }
            val result = visualAiService.generateSceneBackground(request)
            result.onSuccess { generated ->
                _uiState.update {
                    it.copy(
                        backgroundGenState = VisualAssetGenState.PreviewReady(
                            asset = generated,
                            savedAssetId = null
                        )
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        backgroundGenState = VisualAssetGenState.Error(
                            err.localizedMessage ?: "Failed to generate scene background."
                        )
                    )
                }
            }
        }
    }

    fun rejectGeneratedBackground() {
        _uiState.update {
            it.copy(
                backgroundGenState = VisualAssetGenState.Idle,
                snackbarMessage = "Discarded generated background preview"
            )
        }
    }

    fun clearBackgroundGenState() {
        _uiState.update { it.copy(backgroundGenState = VisualAssetGenState.Idle) }
    }

    fun saveGeneratedBackgroundToLibrary(
        name: String,
        visualStyle: String,
        tags: String,
        applyToCurrentScene: Boolean,
        onApplied: () -> Unit = {}
    ) {
        val preview = _uiState.value.backgroundGenState as? VisualAssetGenState.PreviewReady ?: return
        val cleanName = name.trim().take(120).ifBlank { "AI Scene Background" }
        val existingSavedId = preview.savedAssetId

        viewModelScope.launch {
            val resolvedAssetId: String = if (!existingSavedId.isNullOrBlank()) {
                existingSavedId
            } else {
                val newAsset = StudioAsset(
                    name = cleanName,
                    category = "Background",
                    tags = tags.trim().take(200).ifBlank { "ai,background,16:9" },
                    notes = "AI 2D documentary background (${preview.asset.formatLabel})",
                    presetKey = "ai_background",
                    localUri = "",
                    durationLabel = "AI 16:9",
                    imageBase64 = preview.asset.imageBase64,
                    hasTransparency = false,
                    visualStyle = visualStyle.take(100),
                    promptUsed = preview.asset.promptUsed.take(1500)
                )
                val saveRes = repository.saveAsset(newAsset)
                val id = saveRes.getOrElse { err ->
                    val msg = err.localizedMessage ?: "Could not save generated background to asset library."
                    _uiState.update {
                        it.copy(
                            backgroundGenState = VisualAssetGenState.Error(msg),
                            errorMessage = msg
                        )
                    }
                    return@launch
                }
                val persistedAsset = newAsset.copy(id = id, userId = currentUserId)
                _uiState.update { state ->
                    val alreadyPresent = state.assets.any { it.id == id }
                    state.copy(
                        assets = if (alreadyPresent) state.assets else listOf(persistedAsset) + state.assets,
                        backgroundGenState = preview.copy(savedAssetId = id)
                    )
                }
                id
            }

            if (applyToCurrentScene) {
                updateSelectedScene(recordUndo = true) { scene ->
                    scene.copy(backgroundPreset = "asset:$resolvedAssetId")
                }
                _uiState.update {
                    it.copy(
                        snackbarMessage = "Applied AI background \"$cleanName\" to scene & saved to library"
                    )
                }
                onApplied()
            } else {
                _uiState.update {
                    it.copy(snackbarMessage = "Saved AI background \"$cleanName\" to Asset Library")
                }
            }
        }
    }

    fun generateVisualProp(request: PropGenerationRequest) {
        if (_uiState.value.propGenState is VisualAssetGenState.Generating) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    propGenState = VisualAssetGenState.Generating(
                        "Generating 2D visual prop \"${request.propName.ifBlank { "asset" }}\"..."
                    )
                )
            }
            val result = visualAiService.generateVisualProp(request)
            result.onSuccess { generated ->
                _uiState.update {
                    it.copy(
                        propGenState = VisualAssetGenState.PreviewReady(
                            asset = generated,
                            savedAssetId = null
                        )
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        propGenState = VisualAssetGenState.Error(
                            err.localizedMessage ?: "Failed to generate visual prop."
                        )
                    )
                }
            }
        }
    }

    fun rejectGeneratedProp() {
        _uiState.update {
            it.copy(
                propGenState = VisualAssetGenState.Idle,
                snackbarMessage = "Discarded generated prop preview"
            )
        }
    }

    fun clearPropGenState() {
        _uiState.update { it.copy(propGenState = VisualAssetGenState.Idle) }
    }

    fun saveGeneratedPropToLibrary(
        name: String,
        propCategory: String,
        visualStyle: String,
        targetLayer: SceneLayerType,
        placeInCurrentScene: Boolean,
        onPlaced: () -> Unit = {}
    ) {
        val preview = _uiState.value.propGenState as? VisualAssetGenState.PreviewReady ?: return
        val cleanName = name.trim().take(120).ifBlank { "AI Visual Prop" }
        val existingSavedId = preview.savedAssetId

        viewModelScope.launch {
            val persistedAsset: StudioAsset = if (!existingSavedId.isNullOrBlank()) {
                _uiState.value.assets.firstOrNull { it.id == existingSavedId }
                    ?: StudioAsset(
                        id = existingSavedId,
                        userId = currentUserId,
                        name = cleanName,
                        category = "Prop",
                        tags = "ai,prop,${propCategory.lowercase()}",
                        notes = "AI 2D visual prop (${preview.asset.formatLabel})",
                        presetKey = "ai_prop",
                        localUri = "",
                        durationLabel = if (preview.asset.hasActualTransparency) "PNG α" else "AI Prop",
                        imageBase64 = preview.asset.imageBase64,
                        hasTransparency = preview.asset.hasActualTransparency,
                        visualStyle = visualStyle.take(100),
                        promptUsed = preview.asset.promptUsed.take(1500)
                    )
            } else {
                val newAsset = StudioAsset(
                    name = cleanName,
                    category = "Prop",
                    tags = "ai,prop,${propCategory.lowercase()}".take(200),
                    notes = "AI 2D visual prop (${preview.asset.formatLabel})",
                    presetKey = "ai_prop",
                    localUri = "",
                    durationLabel = if (preview.asset.hasActualTransparency) "PNG α" else "AI Prop",
                    imageBase64 = preview.asset.imageBase64,
                    hasTransparency = preview.asset.hasActualTransparency,
                    visualStyle = visualStyle.take(100),
                    promptUsed = preview.asset.promptUsed.take(1500)
                )
                val saveRes = repository.saveAsset(newAsset)
                val id = saveRes.getOrElse { err ->
                    val msg = err.localizedMessage ?: "Could not save generated prop to asset library."
                    _uiState.update {
                        it.copy(
                            propGenState = VisualAssetGenState.Error(msg),
                            errorMessage = msg
                        )
                    }
                    return@launch
                }
                val saved = newAsset.copy(id = id, userId = currentUserId)
                _uiState.update { state ->
                    val alreadyPresent = state.assets.any { it.id == id }
                    state.copy(
                        assets = if (alreadyPresent) state.assets else listOf(saved) + state.assets,
                        propGenState = preview.copy(savedAssetId = id)
                    )
                }
                saved
            }

            if (placeInCurrentScene) {
                addVisualElementFromAsset(persistedAsset, targetLayer)
                onPlaced()
            } else {
                _uiState.update {
                    it.copy(snackbarMessage = "Saved AI prop \"$cleanName\" to Asset Library")
                }
            }
        }
    }

    fun suggestVisualAssetsForSelectedScene(visualStyle: String = "") {
        if (_uiState.value.sceneSuggestionsState is SceneSuggestionsState.Loading) return
        val scene = _uiState.value.selectedScene ?: run {
            _uiState.update {
                it.copy(sceneSuggestionsState = SceneSuggestionsState.Error("Select a scene first to analyze its visual assets."))
            }
            return
        }
        val projectName = _uiState.value.activeProject?.name.orEmpty()
        val effectiveStyle = visualStyle.ifBlank {
            _uiState.value.characters.firstOrNull()?.visualStyle?.ifBlank { "Painterly 2D Documentary" }
                ?: "Painterly 2D Documentary"
        }
        viewModelScope.launch {
            _uiState.update { it.copy(sceneSuggestionsState = SceneSuggestionsState.Loading) }
            val result = visualAiService.suggestSceneVisuals(
                scene = scene,
                projectName = projectName,
                projectVisualStyle = effectiveStyle
            )
            result.onSuccess { suggestions ->
                _uiState.update {
                    it.copy(sceneSuggestionsState = SceneSuggestionsState.Ready(suggestions))
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        sceneSuggestionsState = SceneSuggestionsState.Error(
                            err.localizedMessage ?: "Failed to generate visual suggestions for this scene."
                        )
                    )
                }
            }
        }
    }

    fun clearSceneSuggestions() {
        _uiState.update { it.copy(sceneSuggestionsState = SceneSuggestionsState.Idle) }
    }

    fun applyAssetToSelectedScene(asset: StudioAsset) {
        when (asset.category) {
            "Background", "Location" -> {
                val preset = when {
                    asset.imageBase64.isNotBlank() && asset.id.isNotBlank() -> "asset:${asset.id}"
                    asset.localUri.isNotBlank() -> "uri:${asset.localUri}"
                    else -> asset.presetKey.ifBlank { "himalayas" }
                }
                updateSelectedScene(recordUndo = true) { it.copy(backgroundPreset = preset) }
                _uiState.update { it.copy(snackbarMessage = "Applied \"${asset.name}\" as scene background") }
            }
            "Image" -> {
                if (SceneCompositor.hasRenderableVisualAsset(asset.localUri, asset.presetKey, asset)) {
                    addVisualElementFromAsset(asset, SceneLayerType.PROPS)
                } else {
                    _uiState.update { it.copy(snackbarMessage = "\"${asset.name}\" has no image asset to render.") }
                }
            }
            "Prop" -> {
                if (SceneCompositor.hasRenderableVisualAsset(asset.localUri, asset.presetKey, asset)) {
                    addVisualElementFromAsset(asset, SceneLayerType.PROPS)
                } else {
                    updateSelectedScene { it.copy(objectProp = asset.name) }
                    _uiState.update {
                        it.copy(snackbarMessage = "Saved prop note \"${asset.name}\". Generate or import a visual prop in Layers to stage it.")
                    }
                }
            }
            "Audio" -> {
                updateSelectedScene { it.copy(musicCue = asset.name) }
                _uiState.update { it.copy(snackbarMessage = "Assigned music cue \"${asset.name}\"") }
            }
            "SFX" -> {
                updateSelectedScene { it.copy(sfxCue = asset.name) }
                _uiState.update { it.copy(snackbarMessage = "Assigned SFX cue \"${asset.name}\"") }
            }
            "MotionGraphic" -> {
                if (asset.graphicPayloadJson.isNotBlank()) {
                    applyGraphicTemplateAsset(asset)
                } else {
                    updateSelectedScene {
                        it.copy(
                            motionGraphicStyle = asset.presetKey.ifBlank { "Chapter Card" },
                            motionGraphicTitle = it.motionGraphicTitle.ifBlank { asset.name }
                        )
                    }
                    _uiState.update { it.copy(snackbarMessage = "Applied motion graphic \"${asset.name}\"") }
                }
            }
        }
    }

    // --- PHASE 6: MOTION GRAPHICS, SUBTITLES, INFOGRAPHICS, MAPS & STORY FLOW ENGINE ---

    private fun updateSceneById(sceneId: String, recordUndo: Boolean = true, transform: (StudioScene) -> StudioScene) {
        if (recordUndo) pushHistorySnapshot()
        _uiState.update { state ->
            val updated = state.scenes.map { s -> if (s.id == sceneId) transform(s) else s }
            state.copy(
                scenes = updated,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                saveStatus = SaveStatus.Unsaved
            )
        }
    }

    fun setMotionGfxTab(tab: MotionGraphicsStudioTab) {
        _uiState.update { it.copy(activeMotionGfxTab = tab) }
    }

    fun selectGraphic(graphicId: String?) {
        _uiState.update {
            it.copy(
                selectedGraphicId = graphicId,
                selectedStageTarget = graphicId?.let { id -> SelectedStageTarget.GraphicTarget(id) }
            )
        }
    }

    fun selectGraphicInScene(sceneId: String, graphicId: String) {
        val state = _uiState.value
        val scene = state.scenes.firstOrNull { it.id == sceneId } ?: return
        val gfx = scene.motionGraphics.firstOrNull { it.graphicId == graphicId }
        val start = state.startTimeOfScene(sceneId)
        val jumpTime = if (gfx != null) {
            start + (gfx.startSec + (gfx.durationSec * 0.25)).coerceIn(0.0, scene.durationSec)
        } else start
        val targetTab = when (gfx?.category?.uppercase()) {
            "INFOGRAPHIC" -> MotionGraphicsStudioTab.INFOGRAPHICS
            "MAP" -> MotionGraphicsStudioTab.MAPS
            else -> MotionGraphicsStudioTab.TEXT_GFX
        }
        _uiState.update {
            it.copy(
                selectedSceneId = sceneId,
                selectedGraphicId = graphicId,
                selectedStageTarget = SelectedStageTarget.GraphicTarget(graphicId),
                activeSidebarSection = SidebarSection.MOTION_GRAPHICS,
                activeMotionGfxTab = targetTab,
                playheadSec = jumpTime.coerceIn(0.0, it.totalDurationSec)
            )
        }
    }

    fun selectTransitionForScene(sceneId: String) {
        selectScene(sceneId)
        _uiState.update {
            it.copy(
                activeSidebarSection = SidebarSection.MOTION_GRAPHICS,
                activeMotionGfxTab = MotionGraphicsStudioTab.FLOW_TRANSITIONS
            )
        }
    }

    fun addGraphicTemplate(templateType: String, infographicTypeOverride: String? = null) {
        val state = _uiState.value
        val scene = state.selectedScene ?: return
        if (scene.motionGraphics.size >= MotionGraphicsEngine.MAX_GRAPHICS_PER_SCENE) {
            _uiState.update { it.copy(snackbarMessage = "Maximum ${MotionGraphicsEngine.MAX_GRAPHICS_PER_SCENE} graphics per scene reached.") }
            return
        }
        val localTime = state.selectedSceneLocalTimeSec
        val created = MotionGraphicsEngine.createTemplateGraphic(
            templateType = templateType,
            scene = scene,
            startSec = localTime,
            infographicTypeOverride = infographicTypeOverride,
            idSuffix = UUID.randomUUID().toString().take(8)
        )
        updateSelectedScene(recordUndo = true) { s ->
            s.copy(motionGraphics = s.motionGraphics + created)
        }
        _uiState.update {
            it.copy(
                selectedGraphicId = created.graphicId,
                selectedStageTarget = SelectedStageTarget.GraphicTarget(created.graphicId),
                snackbarMessage = "Added \"${created.title}\" at ${String.format("%.1f", created.startSec)}s"
            )
        }
    }

    fun updateMotionGraphic(
        graphicId: String,
        recordUndo: Boolean = true,
        transform: (MotionGraphicElement) -> MotionGraphicElement
    ) {
        updateSelectedScene(recordUndo = recordUndo) { s ->
            s.copy(
                motionGraphics = s.motionGraphics.map { g ->
                    if (g.graphicId == graphicId) transform(g) else g
                }
            )
        }
    }

    fun duplicateMotionGraphic(graphicId: String) {
        val scene = _uiState.value.selectedScene ?: return
        val source = scene.motionGraphics.firstOrNull { it.graphicId == graphicId } ?: return
        if (scene.motionGraphics.size >= MotionGraphicsEngine.MAX_GRAPHICS_PER_SCENE) {
            _uiState.update { it.copy(snackbarMessage = "Maximum ${MotionGraphicsEngine.MAX_GRAPHICS_PER_SCENE} graphics per scene reached.") }
            return
        }
        val copy = MotionGraphicsEngine.duplicateGraphic(source, UUID.randomUUID().toString().take(8))
        updateSelectedScene(recordUndo = true) { s -> s.copy(motionGraphics = s.motionGraphics + copy) }
        _uiState.update {
            it.copy(
                selectedGraphicId = copy.graphicId,
                selectedStageTarget = SelectedStageTarget.GraphicTarget(copy.graphicId),
                snackbarMessage = "Duplicated \"${source.title}\""
            )
        }
    }

    fun deleteMotionGraphic(graphicId: String) {
        updateSelectedScene(recordUndo = true) { s ->
            s.copy(
                motionGraphics = s.motionGraphics.filterNot { it.graphicId == graphicId },
                animationKeyframes = s.animationKeyframes.filterNot {
                    it.targetType.equals(AnimationTargetType.GRAPHIC.name, ignoreCase = true) && it.targetId == graphicId
                }
            )
        }
        _uiState.update { state ->
            val nextId = state.selectedScene?.motionGraphics?.firstOrNull()?.graphicId
            state.copy(
                selectedGraphicId = if (state.selectedGraphicId == graphicId) nextId else state.selectedGraphicId,
                selectedStageTarget = if ((state.selectedStageTarget as? SelectedStageTarget.GraphicTarget)?.graphicId == graphicId) {
                    null
                } else state.selectedStageTarget,
                snackbarMessage = "Removed graphic from scene"
            )
        }
    }

    fun addGraphicKeyframeAtPlayhead(graphicId: String) {
        val state = _uiState.value
        val scene = state.selectedScene ?: return
        val gfx = scene.motionGraphics.firstOrNull { it.graphicId == graphicId } ?: return
        val localTime = state.selectedSceneLocalTimeSec
        val eval = AnimationEvaluator.evaluateTransform(
            keyframes = scene.animationKeyframes,
            targetType = AnimationTargetType.GRAPHIC,
            targetId = graphicId,
            timeSec = localTime,
            basePosX = gfx.posX,
            basePosY = gfx.posY,
            baseScale = gfx.scale,
            baseRotationDeg = gfx.rotationDeg,
            baseOpacity = gfx.opacity,
            baseFlipX = false
        )
        updateSelectedScene(recordUndo = true) { s ->
            var kfs = AnimationEvaluator.upsertKeyframe(
                s.animationKeyframes, AnimationTargetType.GRAPHIC, graphicId,
                AnimatableProperty.POS_X, localTime, eval.posX, EasingType.EASE_IN_OUT, s.durationSec
            )
            kfs = AnimationEvaluator.upsertKeyframe(
                kfs, AnimationTargetType.GRAPHIC, graphicId,
                AnimatableProperty.POS_Y, localTime, eval.posY, EasingType.EASE_IN_OUT, s.durationSec
            )
            kfs = AnimationEvaluator.upsertKeyframe(
                kfs, AnimationTargetType.GRAPHIC, graphicId,
                AnimatableProperty.OPACITY, localTime, eval.opacity, EasingType.EASE_IN_OUT, s.durationSec
            )
            s.copy(animationKeyframes = kfs)
        }
        _uiState.update {
            it.copy(snackbarMessage = "Keyframed \"${gfx.title}\" Position & Opacity at ${String.format("%.1f", localTime)}s")
        }
    }

    fun saveGraphicTemplateToAssets(graphicId: String) {
        val scene = _uiState.value.selectedScene ?: return
        val gfx = scene.motionGraphics.firstOrNull { it.graphicId == graphicId } ?: return
        val payload = MotionGraphicsEngine.serializeGraphicTemplate(gfx)
        if (payload.length > 31_500) {
            _uiState.update { it.copy(errorMessage = "Graphic template payload exceeds library size limit.") }
            return
        }
        val asset = StudioAsset(
            name = gfx.title.ifBlank { gfx.templateType }.take(120),
            category = "MotionGraphic",
            tags = "template,${gfx.category.lowercase()},${gfx.templateType.lowercase()}",
            notes = "Reusable Phase 6 Motion Graphic Template (${gfx.templateType})",
            presetKey = gfx.templateType,
            durationLabel = "${String.format("%.1f", gfx.durationSec)}s",
            graphicPayloadJson = payload
        )
        saveAsset(asset)
    }

    fun applyGraphicTemplateAsset(asset: StudioAsset) {
        val state = _uiState.value
        val scene = state.selectedScene ?: return
        if (scene.motionGraphics.size >= MotionGraphicsEngine.MAX_GRAPHICS_PER_SCENE) {
            _uiState.update { it.copy(snackbarMessage = "Maximum ${MotionGraphicsEngine.MAX_GRAPHICS_PER_SCENE} graphics per scene reached.") }
            return
        }
        val instantiated = MotionGraphicsEngine.deserializeGraphicTemplate(
            json = asset.graphicPayloadJson,
            newGraphicId = "gfx_tpl_${UUID.randomUUID().toString().take(8)}",
            startSec = state.selectedSceneLocalTimeSec
        )
        if (instantiated == null) {
            _uiState.update { it.copy(errorMessage = "Could not parse saved graphic template.") }
            return
        }
        updateSelectedScene(recordUndo = true) { s -> s.copy(motionGraphics = s.motionGraphics + instantiated) }
        _uiState.update {
            it.copy(
                selectedGraphicId = instantiated.graphicId,
                selectedStageTarget = SelectedStageTarget.GraphicTarget(instantiated.graphicId),
                snackbarMessage = "Inserted template \"${asset.name}\" into scene"
            )
        }
    }

    fun requestAiGraphicSuggestions() {
        val state = _uiState.value
        val scene = state.selectedScene ?: return
        if (state.graphicSuggestionsState is GraphicSuggestionsState.Loading) return
        viewModelScope.launch {
            _uiState.update { it.copy(graphicSuggestionsState = GraphicSuggestionsState.Loading) }
            val result = aiService.suggestSceneGraphics(
                scene = scene,
                scriptContext = state.activeProject?.scriptText.orEmpty(),
                characters = state.characters
            )
            result.onSuccess { suggestions ->
                updateSelectedScene(recordUndo = true) { it.copy(graphicSuggestions = suggestions) }
                _uiState.update {
                    it.copy(
                        graphicSuggestionsState = GraphicSuggestionsState.Ready(suggestions.size),
                        snackbarMessage = "AI suggested ${suggestions.size} documentary graphic(s). Review & approve below."
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        graphicSuggestionsState = GraphicSuggestionsState.Error(
                            err.localizedMessage ?: "Failed to generate graphic suggestions."
                        )
                    )
                }
            }
        }
    }

    fun applyAiGraphicSuggestion(suggestionId: String) {
        val scene = _uiState.value.selectedScene ?: return
        val suggestion = scene.graphicSuggestions.firstOrNull { it.suggestionId == suggestionId } ?: return
        if (scene.motionGraphics.size >= MotionGraphicsEngine.MAX_GRAPHICS_PER_SCENE) {
            _uiState.update { it.copy(snackbarMessage = "Maximum ${MotionGraphicsEngine.MAX_GRAPHICS_PER_SCENE} graphics per scene reached.") }
            return
        }
        val created = MotionGraphicsEngine.createGraphicFromSuggestion(
            suggestion = suggestion,
            scene = scene,
            idSuffix = UUID.randomUUID().toString().take(8)
        )
        updateSelectedScene(recordUndo = true) { s ->
            s.copy(
                motionGraphics = s.motionGraphics + created,
                graphicSuggestions = s.graphicSuggestions.map {
                    if (it.suggestionId == suggestionId) it.copy(applied = true) else it
                }
            )
        }
        _uiState.update {
            it.copy(
                selectedGraphicId = created.graphicId,
                selectedStageTarget = SelectedStageTarget.GraphicTarget(created.graphicId),
                snackbarMessage = "Approved & added \"${created.title}\""
            )
        }
    }

    fun dismissAiGraphicSuggestion(suggestionId: String) {
        updateSelectedScene(recordUndo = true) { s ->
            s.copy(
                graphicSuggestions = s.graphicSuggestions.filterNot { it.suggestionId == suggestionId }
            )
        }
    }

    fun generateSubtitlesForActiveScene() {
        val state = _uiState.value
        val scene = state.selectedScene ?: return
        val (cues, usedRealAudio) = SubtitleEngine.generateSubtitlesForScene(
            scene = scene,
            audioAssets = state.audioAssets,
            language = state.activeProject?.scriptLanguage ?: "Bilingual"
        )
        if (cues.isEmpty()) {
            _uiState.update { it.copy(snackbarMessage = "Add narration or dialogue text first to auto-generate subtitles.") }
            return
        }
        updateSelectedScene(recordUndo = true) { it.copy(subtitleCues = cues) }
        _uiState.update {
            it.copy(
                lastSubtitleUsedRealAudio = usedRealAudio,
                snackbarMessage = if (usedRealAudio) {
                    "Generated ${cues.size} subtitle cue(s) synced to attached audio clips"
                } else {
                    "Generated ${cues.size} subtitle cue(s) using estimated speech pacing"
                }
            )
        }
    }

    fun addSubtitleCue() {
        val state = _uiState.value
        val scene = state.selectedScene ?: return
        if (scene.subtitleCues.size >= SubtitleEngine.MAX_CUES_PER_SCENE) {
            _uiState.update { it.copy(snackbarMessage = "Maximum ${SubtitleEngine.MAX_CUES_PER_SCENE} subtitle cues per scene reached.") }
            return
        }
        val start = state.selectedSceneLocalTimeSec.coerceIn(0.0, (scene.durationSec - 0.5).coerceAtLeast(0.0))
        val end = (start + 2.5).coerceAtMost(scene.durationSec.coerceAtLeast(1.0))
        val newCue = SubtitleCue(
            cueId = "sub_${UUID.randomUUID().toString().take(8)}",
            sceneId = scene.id,
            text = "New documentary caption",
            startSec = start,
            endSec = end
        )
        updateSelectedScene(recordUndo = true) { s -> s.copy(subtitleCues = s.subtitleCues + newCue) }
    }

    fun updateSubtitleCue(cueId: String, transform: (SubtitleCue) -> SubtitleCue) {
        updateSelectedScene(recordUndo = true) { s ->
            s.copy(subtitleCues = s.subtitleCues.map { if (it.cueId == cueId) transform(it) else it })
        }
    }

    fun splitSubtitleCue(cueId: String) {
        updateSelectedScene(recordUndo = true) { s ->
            s.copy(subtitleCues = SubtitleEngine.splitCue(s.subtitleCues, cueId))
        }
    }

    fun deleteSubtitleCue(cueId: String) {
        updateSelectedScene(recordUndo = true) { s ->
            s.copy(subtitleCues = s.subtitleCues.filterNot { it.cueId == cueId })
        }
    }

    fun fitActiveSceneSubtitles() {
        updateSelectedScene(recordUndo = true) { s ->
            s.copy(subtitleCues = SubtitleEngine.fitSubtitlesToDuration(s.subtitleCues, s.durationSec))
        }
        _uiState.update { it.copy(snackbarMessage = "Resolved subtitle overlaps and clamped to scene duration") }
    }

    fun updateSubtitleStyle(transform: (SubtitleStyleConfig) -> SubtitleStyleConfig) {
        updateSelectedScene(recordUndo = true) { s -> s.copy(subtitleStyle = transform(s.subtitleStyle)) }
    }

    fun applySubtitleStylePreset(presetKey: String) {
        updateSelectedScene(recordUndo = true) { s ->
            s.copy(subtitleStyle = SubtitleEngine.applyStylePreset(presetKey, s.subtitleStyle))
        }
    }

    fun buildExportSrt(): String = SubtitleEngine.generateSrtForProject(_uiState.value.scenes)

    fun postSnackbar(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    fun attachMapImageToGraphic(graphicId: String, uriString: String) {
        updateMotionGraphic(graphicId, recordUndo = true) { g ->
            g.copy(mapBackgroundUri = uriString.take(500))
        }
        _uiState.update { it.copy(snackbarMessage = "Attached custom map image to documentary map") }
    }

    fun updateSceneDurationById(sceneId: String, newDurationSec: Double) {
        updateSceneById(sceneId, recordUndo = true) { s ->
            s.copy(durationSec = newDurationSec.coerceIn(1.5, 60.0))
        }
    }

    fun updateSceneTransition(sceneId: String, transition: SceneTransitionConfig) {
        val normalized = TransitionFlowEngine.normalizeTransition(transition)
        updateSceneById(sceneId, recordUndo = true) { s ->
            s.copy(outgoingTransition = normalized)
        }
    }

    fun previewTransitionForScene(sceneId: String) {
        val state = _uiState.value
        val seg = TransitionFlowEngine.computeStoryFlowSegments(state.scenes)
            .firstOrNull { it.scene.id == sceneId } ?: return
        seekToTime(seg.previewBoundaryTimeSec)
        startPlayback()
    }

    // --- PLAYBACK & TIMELINE ENGINE ---

    fun togglePlayPause() {
        if (_uiState.value.isPlaying) {
            pausePlayback()
        } else {
            startPlayback()
        }
    }

    private fun syncAudioEngineToCurrentPlayhead() {
        val engine = audioPlaybackEngine ?: return
        val state = _uiState.value
        if (!state.isPlaying) {
            engine.pauseAll()
            return
        }
        val activeClips = AudioTimelineEngine.resolveActiveClipsAtPlayhead(
            scenes = state.scenes,
            projectAudioClips = state.activeProject?.projectAudioClips.orEmpty(),
            audioAssets = state.audioAssets,
            globalPlayheadSec = state.playheadSec,
            mutedTracks = state.mutedTracks
        )
        engine.syncToPlayhead(activeClips, isPlaying = true)
    }

    private fun startPlayback() {
        playbackJob?.cancel()
        audioPlaybackEngine?.stopPreview()
        val total = _uiState.value.totalDurationSec
        if (_uiState.value.playheadSec >= total - 0.05) {
            _uiState.update { it.copy(playheadSec = 0.0) }
        }
        _uiState.update { it.copy(isPlaying = true, previewingAudioAssetId = null) }
        syncAudioEngineToCurrentPlayhead()
        playbackJob = viewModelScope.launch {
            var lastTimeMs = System.currentTimeMillis()
            while (isActive) {
                delay(33L)
                val nowMs = System.currentTimeMillis()
                val deltaSec = (nowMs - lastTimeMs) / 1000.0
                lastTimeMs = nowMs

                val state = _uiState.value
                val nextTime = state.playheadSec + deltaSec
                if (nextTime >= state.totalDurationSec) {
                    audioPlaybackEngine?.stopAll()
                    _uiState.update { it.copy(isPlaying = false, playheadSec = state.totalDurationSec) }
                    break
                } else {
                    val (activeScene, _) = state.sceneAtTime(nextTime)
                    _uiState.update {
                        it.copy(
                            playheadSec = nextTime,
                            selectedSceneId = activeScene?.id ?: it.selectedSceneId
                        )
                    }
                    syncAudioEngineToCurrentPlayhead()
                }
            }
        }
    }

    fun pausePlayback() {
        playbackJob?.cancel()
        audioPlaybackEngine?.pauseAll()
        _uiState.update { it.copy(isPlaying = false) }
    }

    fun stopPlayback() {
        playbackJob?.cancel()
        audioPlaybackEngine?.stopAll()
        val firstId = _uiState.value.scenes.firstOrNull()?.id
        _uiState.update {
            it.copy(
                isPlaying = false,
                playheadSec = 0.0,
                selectedSceneId = firstId ?: it.selectedSceneId
            )
        }
    }

    fun seekToTime(timeSec: Double) {
        val clamped = timeSec.coerceIn(0.0, _uiState.value.totalDurationSec)
        val (activeScene, _) = _uiState.value.sceneAtTime(clamped)
        _uiState.update {
            it.copy(
                playheadSec = clamped,
                selectedSceneId = activeScene?.id ?: it.selectedSceneId
            )
        }
        if (_uiState.value.isPlaying) {
            syncAudioEngineToCurrentPlayhead()
        }
    }

    fun previousScene() {
        val state = _uiState.value
        if (state.scenes.isEmpty()) return
        val prevIdx = (state.selectedSceneIndex - 1).coerceAtLeast(0)
        selectScene(state.scenes[prevIdx].id)
    }

    fun nextScene() {
        val state = _uiState.value
        if (state.scenes.isEmpty()) return
        val nextIdx = (state.selectedSceneIndex + 1).coerceAtMost(state.scenes.lastIndex)
        selectScene(state.scenes[nextIdx].id)
    }

    // --- UI NAVIGATION, ZOOM & TRACK CONTROLS ---

    fun setSidebarSection(section: SidebarSection) {
        _uiState.update { it.copy(activeSidebarSection = section) }
    }

    fun setInspectorSection(section: InspectorSection) {
        _uiState.update { it.copy(activeInspectorSection = section) }
    }

    fun setCompactWorkspaceTab(tab: CompactWorkspaceTab) {
        _uiState.update { it.copy(compactWorkspaceTab = tab) }
    }

    fun setAssetCategoryFilter(category: String) {
        _uiState.update { it.copy(assetCategoryFilter = category) }
    }

    fun adjustCanvasZoom(delta: Float) {
        _uiState.update {
            it.copy(canvasZoom = (it.canvasZoom + delta).coerceIn(0.5f, 2.0f))
        }
    }

    fun resetCanvasZoom() {
        _uiState.update { it.copy(canvasZoom = 1.0f) }
    }

    fun adjustTimelineZoom(delta: Float) {
        _uiState.update {
            it.copy(timelineZoomDpPerSec = (it.timelineZoomDpPerSec + delta).coerceIn(18f, 72f))
        }
    }

    fun toggleTrackMute(track: TimelineTrackType) {
        _uiState.update { state ->
            val updated = if (track in state.mutedTracks) {
                state.mutedTracks - track
            } else {
                state.mutedTracks + track
            }
            state.copy(mutedTracks = updated)
        }
        syncAudioEngineToCurrentPlayhead()
    }

    fun toggleTheaterPreview(enabled: Boolean) {
        _uiState.update { it.copy(isTheaterPreview = enabled) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun buildExportShotlistJson(): String {
        val state = _uiState.value
        val project = state.activeProject
        val charById = state.characters.associateBy { it.id }
        val root = JSONObject().apply {
            put("studio", "MotionStory Studio")
            put("stage", "Character Consistency Engine & Storyboard Blueprint")
            put("projectName", project?.name ?: "Untitled")
            put("aspectRatio", project?.aspectRatio ?: "16:9")
            put("fps", project?.fps ?: 24)
            put("resolution", project?.resolution ?: "1080p")
            put("totalDurationSec", state.totalDurationSec)
            put("characters", JSONArray().apply {
                state.characters.forEach { c ->
                    put(JSONObject().apply {
                        put("characterId", c.charCode)
                        put("name", c.name)
                        put("role", c.role)
                        put("visualAge", c.visualAge)
                        put("visualStyle", c.visualStyle)
                        put("defaultEmotion", c.expressionDefault)
                        put("hasReferenceImage", c.referenceImageBase64.isNotBlank())
                        put("consistencyNotes", c.appearanceNotes)
                    })
                }
            })
            put("scenes", JSONArray().apply {
                state.scenes.forEachIndexed { idx, s ->
                    put(JSONObject().apply {
                        put("sceneNumber", idx + 1)
                        put("title", s.title)
                        put("durationSec", s.durationSec)
                        put("background", s.backgroundPreset)
                        put("cameraMovement", s.cameraMovement)
                        put("cast", JSONArray().apply {
                            s.effectiveCast().forEach { placement ->
                                val c = charById[placement.characterId]
                                put(JSONObject().apply {
                                    put("characterId", c?.charCode ?: placement.characterId)
                                    put("name", c?.name ?: "Unknown")
                                    put("expression", placement.expression)
                                    put("action", placement.action)
                                    put("poseView", placement.poseView)
                                })
                            }
                        })
                        put("sceneElementsCount", s.sceneElements.size)
                        put("animationKeyframesCount", s.animationKeyframes.size)
                        put("characterActingCuesCount", s.characterActingCues.size)
                        put("cameraPreset", s.cinematics.cameraPreset)
                        put("lightingPreset", s.cinematics.lightingPreset)
                        put("atmosphereMode", s.cinematics.atmosphereMode)
                        put("letterboxAspect", s.cinematics.letterboxAspect)
                        put("dialogueLinesCount", s.dialogueLines.size)
                        put("audioClipsCount", s.audioClips.size)
                        put("lipSyncCuesCount", s.lipSyncCues.size)
                        put("motionGraphicsCount", s.motionGraphics.size)
                        put("subtitleCuesCount", s.subtitleCues.size)
                        put("outgoingTransition", s.outgoingTransition.type)
                        put("outgoingTransitionDurationSec", s.outgoingTransition.durationSec)
                        put("narration", s.narrationText)
                        put("motionGraphic", s.motionGraphicTitle)
                        put("musicCue", s.musicCue)
                        put("sfxCue", s.sfxCue)
                    })
                }
            })
        }
        return root.toString(2)
    }

    override fun onCleared() {
        super.onCleared()
        playbackJob?.cancel()
        sceneObserverJob?.cancel()
        charRefObserverJob?.cancel()
        allCharRefObserverJobs.values.forEach { it.cancel() }
        allCharRefObserverJobs.clear()
        audioPlaybackEngine?.release()
        ttsEngine?.release()
    }
}
