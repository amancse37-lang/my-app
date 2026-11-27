package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class StoryAnalysisSummary(
    val detectedLanguage: String = "Bilingual",
    val storyLogline: String = "",
    val eraOrPeriod: String = "",
    val emotionalArc: String = "",
    val locations: List<String> = emptyList(),
    val characterRelationships: List<String> = emptyList(),
    val narrativeBeats: List<String> = emptyList(),
    val estimatedTotalDurationSec: Double = 0.0,
    val approved: Boolean = false
)

@Keep
@IgnoreExtraProperties
data class ScenePlanMetadata(
    val location: String = "",
    val environment: String = "",
    val timeOfDay: String = "",
    val weather: String = "",
    val emotionalTone: String = "",
    val visualDirection: String = "",
    val cameraSuggestion: String = "",
    val lightingSuggestion: String = "",
    val atmosphereSuggestion: String = "",
    val vfxSuggestion: String = "",
    val transitionSuggestion: String = "Cut",
    val audioCuesSummary: String = "",
    val suggestedProps: List<String> = emptyList(),
    val approved: Boolean = false
)

@Keep
@IgnoreExtraProperties
data class DialogueLine(
    val lineId: String = "",
    val characterId: String = "",
    val speakerName: String = "",
    val text: String = "",
    val language: String = "English",
    val startSec: Double = 0.0,
    val durationSec: Double = 3.0,
    val audioAssetId: String = "",
    val clipId: String = "",
    val voiceId: String = "",
    val emotion: String = "Neutral",
    val orderIndex: Int = 0
)

@Keep
@IgnoreExtraProperties
data class SceneAudioClip(
    val clipId: String = "",
    val audioAssetId: String = "",
    val title: String = "",
    val trackType: String = "NARRATION",
    val scope: String = "SCENE",
    val startSec: Double = 0.0,
    val sourceDurationSec: Double = 5.0,
    val trimStartSec: Double = 0.0,
    val trimEndSec: Double = 0.0,
    val volume: Double = 1.0,
    val fadeInSec: Double = 0.0,
    val fadeOutSec: Double = 0.0,
    val loopEnabled: Boolean = false,
    val muted: Boolean = false,
    val dialogueLineId: String = "",
    val characterId: String = ""
)

@Keep
@IgnoreExtraProperties
data class VisemeCue(
    val cueId: String = "",
    val characterId: String = "",
    val dialogueLineId: String = "",
    val timeSec: Double = 0.0,
    val durationSec: Double = 0.12,
    val viseme: String = "SILENT",
    val energy: Double = 0.0
)

@Keep
@IgnoreExtraProperties
data class AiSoundSuggestion(
    val suggestionId: String = "",
    val category: String = "SFX",
    val title: String = "",
    val description: String = "",
    val suggestedStartSec: Double = 0.0,
    val suggestedVolume: Double = 0.7,
    val loopEnabled: Boolean = false,
    val applied: Boolean = false
)

@Keep
@IgnoreExtraProperties
data class InfographicDataPoint(
    val id: String = "",
    val label: String = "",
    val value: Double = 0.0,
    val secondaryValue: Double = 0.0,
    val colorHex: String = "#F5A623",
    val highlighted: Boolean = false,
    val calloutText: String = ""
)

@Keep
@IgnoreExtraProperties
data class MapMarkerItem(
    val markerId: String = "",
    val label: String = "",
    val regionTag: String = "",
    val xNorm: Double = 0.5,
    val yNorm: Double = 0.5,
    val markerStyle: String = "PIN",
    val colorHex: String = "#F5A623",
    val appearAtSec: Double = 0.3,
    val highlighted: Boolean = false
)

@Keep
@IgnoreExtraProperties
data class MapRouteItem(
    val routeId: String = "",
    val label: String = "",
    val distanceCaption: String = "",
    val fromX: Double = 0.25,
    val fromY: Double = 0.65,
    val toX: Double = 0.75,
    val toY: Double = 0.35,
    val colorHex: String = "#26D0CE",
    val showArrow: Boolean = true,
    val startSec: Double = 0.5,
    val durationSec: Double = 2.0
)

@Keep
@IgnoreExtraProperties
data class MotionGraphicElement(
    val graphicId: String = "",
    val title: String = "Title Graphic",
    val category: String = "TEXT",
    val templateType: String = "MAIN_TITLE",
    val startSec: Double = 0.0,
    val durationSec: Double = 4.0,
    val layer: String = "OVERLAY",
    val attachToCamera: Boolean = false,
    val posX: Double = 0.0,
    val posY: Double = 0.0,
    val scale: Double = 1.0,
    val rotationDeg: Double = 0.0,
    val widthFraction: Double = 0.68,
    val heightFraction: Double = 0.42,
    val zIndex: Int = 0,
    val visible: Boolean = true,
    val opacity: Double = 1.0,
    val primaryText: String = "Documentary Title",
    val secondaryText: String = "",
    val tertiaryText: String = "",
    val quaternaryText: String = "",
    val fontFamilyKey: String = "SPACE_GROTESK",
    val fontSizeSp: Double = 22.0,
    val textColorHex: String = "#F2F4F8",
    val accentColorHex: String = "#F5A623",
    val backgroundColorHex: String = "#12141A",
    val backgroundOpacity: Double = 0.82,
    val outlineColorHex: String = "#0A0C10",
    val outlineWidthDp: Double = 0.0,
    val shadowEnabled: Boolean = true,
    val alignment: String = "CENTER",
    val letterSpacingSp: Double = 0.8,
    val lineSpacingMult: Double = 1.18,
    val layoutPreset: String = "DOCUMENTARY_BAR",
    val entranceAnimation: String = "FADE_IN",
    val exitAnimation: String = "FADE_OUT",
    val entranceDurationSec: Double = 0.65,
    val exitDurationSec: Double = 0.45,
    val animationEasing: String = "EASE_OUT",
    val infographicType: String = "BAR_CHART",
    val dataUnit: String = "",
    val dataPoints: List<InfographicDataPoint> = emptyList(),
    val mapBackgroundUri: String = "",
    val mapPresetStyle: String = "DARK_ATLAS",
    val mapZoomStart: Double = 1.0,
    val mapZoomEnd: Double = 1.25,
    val mapPanStartX: Double = 0.0,
    val mapPanStartY: Double = 0.0,
    val mapPanEndX: Double = 0.0,
    val mapPanEndY: Double = 0.0,
    val mapMarkers: List<MapMarkerItem> = emptyList(),
    val mapRoutes: List<MapRouteItem> = emptyList()
)

@Keep
@IgnoreExtraProperties
data class SubtitleCue(
    val cueId: String = "",
    val sceneId: String = "",
    val dialogueLineId: String = "",
    val speakerName: String = "",
    val text: String = "",
    val startSec: Double = 0.0,
    val endSec: Double = 3.0,
    val positionYNorm: Double = 0.88
)

@Keep
@IgnoreExtraProperties
data class SubtitleStyleConfig(
    val stylePreset: String = "DOCUMENTARY_CLEAN",
    val fontFamilyKey: String = "PLUS_JAKARTA",
    val fontSizeSp: Double = 13.0,
    val textColorHex: String = "#F2F4F8",
    val backgroundColorHex: String = "#0A0C10",
    val backgroundOpacity: Double = 0.78,
    val outlineEnabled: Boolean = true,
    val outlineColorHex: String = "#000000",
    val alignment: String = "CENTER",
    val verticalPosition: String = "BOTTOM"
)

@Keep
@IgnoreExtraProperties
data class SceneTransitionConfig(
    val type: String = "CUT",
    val durationSec: Double = 0.8,
    val direction: String = "LEFT",
    val easing: String = "EASE_IN_OUT",
    val feather: Double = 0.25
)

@Keep
@IgnoreExtraProperties
data class AiGraphicSuggestion(
    val suggestionId: String = "",
    val category: String = "TITLE",
    val templateType: String = "CHAPTER_TITLE",
    val title: String = "",
    val rationale: String = "",
    val primaryText: String = "",
    val secondaryText: String = "",
    val suggestedStartSec: Double = 0.3,
    val suggestedDurationSec: Double = 4.0,
    val infographicType: String = "",
    val suggestedDataSummary: String = "",
    val applied: Boolean = false
)

@Keep
@IgnoreExtraProperties
data class StudioAudioAsset(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val category: String = "Narration",
    val sourceType: String = "IMPORTED",
    val storageState: String = "LOCAL_PERSISTENT",
    val localFilePath: String = "",
    val mimeType: String = "audio/wav",
    val fileSizeBytes: Long = 0L,
    val durationSec: Double = 0.0,
    val language: String = "English",
    val voiceId: String = "",
    val characterId: String = "",
    val waveformPeaks: List<Double> = emptyList(),
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StudioProject(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "Untitled Documentary",
    val scriptText: String = "",
    val scriptLanguage: String = "Bilingual",
    val aspectRatio: String = "16:9",
    val fps: Long = 24L,
    val resolution: String = "1080p",
    val storyAnalysis: StoryAnalysisSummary = StoryAnalysisSummary(),
    val projectAudioClips: List<SceneAudioClip> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LayerState(
    val layerKey: String = "BACKGROUND",
    val visible: Boolean = true,
    val order: Int = 0,
    val opacity: Double = 1.0,
    val parallaxFactor: Double = 1.0
)

@Keep
@IgnoreExtraProperties
data class SceneVisualElement(
    val elementId: String = "",
    val name: String = "",
    val elementType: String = "PROP",
    val assetId: String = "",
    val assetPresetKey: String = "",
    val assetUri: String = "",
    val layer: String = "PROPS",
    val posX: Double = 0.0,
    val posY: Double = 0.0,
    val scale: Double = 1.0,
    val rotationDeg: Double = 0.0,
    val zIndex: Int = 0,
    val visible: Boolean = true,
    val opacity: Double = 1.0,
    val flipX: Boolean = false
)

@Keep
@IgnoreExtraProperties
data class SceneCharacterPlacement(
    val characterId: String = "",
    val expression: String = "Neutral",
    val action: String = "Standing",
    val poseView: String = "Three-Quarter View",
    val posX: Double = 0.0,
    val posY: Double = 16.0,
    val scale: Double = 1.0,
    val rotationDeg: Double = 0.0,
    val layer: String = "CHARACTERS",
    val zIndex: Int = 0,
    val visible: Boolean = true,
    val opacity: Double = 1.0,
    val flipX: Boolean = false
)

@Keep
@IgnoreExtraProperties
data class AnimationKeyframe(
    val keyframeId: String = "",
    val targetType: String = "CHARACTER",
    val targetId: String = "",
    val property: String = "POS_X",
    val timeSec: Double = 0.0,
    val value: Double = 0.0,
    val easing: String = "EASE_IN_OUT"
)

@Keep
@IgnoreExtraProperties
data class CharacterActingCue(
    val cueId: String = "",
    val characterId: String = "",
    val timeSec: Double = 0.0,
    val expression: String = "Neutral",
    val action: String = "Standing",
    val poseView: String = "Three-Quarter View",
    val referenceSheetId: String = ""
)

@Keep
@IgnoreExtraProperties
data class SceneCinematics(
    val cameraPreset: String = "Wide Shot",
    val cameraPosX: Double = 0.0,
    val cameraPosY: Double = 0.0,
    val cameraRotationDeg: Double = 0.0,
    val parallaxEnabled: Boolean = true,
    val parallaxStrength: Double = 1.0,
    val depthFocusPlane: Double = 0.85,
    val depthBlurIntensity: Double = 0.0,
    val depthHazeIntensity: Double = 0.0,
    val lightingPreset: String = "Natural Documentary",
    val exposure: Double = 0.0,
    val contrast: Double = 1.0,
    val saturation: Double = 1.0,
    val temperature: Double = 0.0,
    val lightSourceX: Double = 0.35,
    val lightSourceY: Double = -0.65,
    val lightGlowIntensity: Double = 0.0,
    val shadowIntensity: Double = 0.0,
    val atmosphereMode: String = "None",
    val atmosphereIntensity: Double = 0.0,
    val atmosphereSpeed: Double = 1.0,
    val windStrength: Double = 0.15,
    val vignetteIntensity: Double = 0.0,
    val filmGrainIntensity: Double = 0.0,
    val bloomIntensity: Double = 0.0,
    val cameraShakeIntensity: Double = 0.0,
    val letterboxAspect: String = "None"
)

@Keep
@IgnoreExtraProperties
data class StudioScene(
    @DocumentId val id: String = "",
    val userId: String = "",
    val orderIndex: Long = 0L,
    val title: String = "New Scene",
    val description: String = "",
    val visualSummary: String = "",
    val narrationText: String = "",
    val subtitleText: String = "",
    val durationSec: Double = 6.0,
    val backgroundPreset: String = "himalayas",
    val backgroundTintHex: String = "#1B2838",
    val characterId: String = "",
    val characterCast: List<SceneCharacterPlacement> = emptyList(),
    val sceneElements: List<SceneVisualElement> = emptyList(),
    val layerStates: List<LayerState> = emptyList(),
    val animationKeyframes: List<AnimationKeyframe> = emptyList(),
    val characterActingCues: List<CharacterActingCue> = emptyList(),
    val cinematics: SceneCinematics = SceneCinematics(),
    val scenePlan: ScenePlanMetadata = ScenePlanMetadata(),
    val dialogueLines: List<DialogueLine> = emptyList(),
    val audioClips: List<SceneAudioClip> = emptyList(),
    val lipSyncCues: List<VisemeCue> = emptyList(),
    val soundSuggestions: List<AiSoundSuggestion> = emptyList(),
    val motionGraphics: List<MotionGraphicElement> = emptyList(),
    val subtitleCues: List<SubtitleCue> = emptyList(),
    val subtitleStyle: SubtitleStyleConfig = SubtitleStyleConfig(),
    val outgoingTransition: SceneTransitionConfig = SceneTransitionConfig(),
    val graphicSuggestions: List<AiGraphicSuggestion> = emptyList(),
    val posX: Double = 0.0,
    val posY: Double = 0.0,
    val scale: Double = 1.0,
    val rotationDeg: Double = 0.0,
    val animationStyle: String = "Subtle Drift",
    val cameraMovement: String = "Slow Push In",
    val cameraZoom: Double = 1.12,
    val effectPreset: String = "Film Grain",
    val effectIntensity: Double = 0.35,
    val motionGraphicTitle: String = "",
    val motionGraphicStyle: String = "Lower Third",
    val musicCue: String = "Ambient Documentary Bed",
    val sfxCue: String = "Room Tone",
    val objectProp: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    /**
     * Returns the effective multi-character cast for this scene, preserving full
     * backwards compatibility with single-character scenes created prior to Stage 2.
     */
    fun effectiveCast(): List<SceneCharacterPlacement> {
        if (characterCast.isNotEmpty()) return characterCast
        if (characterId.isNotBlank()) {
            return listOf(
                SceneCharacterPlacement(
                    characterId = characterId,
                    expression = "Calm",
                    action = "Standing",
                    poseView = "Three-Quarter View",
                    posX = posX,
                    posY = posY,
                    scale = scale,
                    rotationDeg = rotationDeg,
                    layer = "CHARACTERS",
                    zIndex = 0,
                    visible = true,
                    opacity = 1.0,
                    flipX = false
                )
            )
        }
        return emptyList()
    }
}

@Keep
@IgnoreExtraProperties
data class StudioCharacter(
    @DocumentId val id: String = "",
    val userId: String = "",
    val charCode: String = "CHR-01",
    val name: String = "",
    val role: String = "",
    val visualAge: String = "",
    val faceDescription: String = "",
    val hairStyle: String = "",
    val hairColor: String = "",
    val clothing: String = "",
    val clothingColors: String = "",
    val bodyProportions: String = "",
    val accessories: String = "",
    val personality: String = "",
    val appearanceNotes: String = "",
    val wardrobePalette: String = "",
    val expressionDefault: String = "Calm",
    val visualStyle: String = "Painterly 2D Documentary",
    val referenceImageBase64: String = "",
    val avatarPreset: String = "narrator",
    val accentColorHex: String = "#F5A623",
    val voiceLanguage: String = "English",
    val voiceId: String = "",
    val voicePitch: Double = 1.0,
    val voiceRate: Double = 1.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CharacterReferenceSheet(
    @DocumentId val id: String = "",
    val userId: String = "",
    val viewType: String = "Front View",
    val expression: String = "Neutral",
    val poseAction: String = "Standing",
    val visualStyle: String = "Painterly 2D Documentary",
    val promptSummary: String = "",
    val imageBase64: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StudioAsset(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val category: String = "Background",
    val tags: String = "",
    val notes: String = "",
    val presetKey: String = "himalayas",
    val localUri: String = "",
    val durationLabel: String = "1920x1080",
    val imageBase64: String = "",
    val hasTransparency: Boolean = false,
    val visualStyle: String = "",
    val promptUsed: String = "",
    val graphicPayloadJson: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class SidebarSection(val label: String) {
    SCRIPT("Script"),
    SCENES("Scenes"),
    CHARACTERS("Characters"),
    LOCATIONS("Locations"),
    ASSETS("Assets"),
    AUDIO("Audio"),
    MOTION_GRAPHICS("Motion GFX")
}

enum class TimelineTrackType(val label: String, val shortCode: String) {
    BACKGROUND("Background", "BG"),
    CHARACTER("Character", "CH"),
    OBJECT("Object", "OB"),
    CAMERA("Camera", "CM"),
    MOTION_GRAPHICS("Motion Graphics", "MG"),
    TITLES_TEXT("Titles & Text", "TX"),
    LOWER_THIRDS("Lower Thirds", "L3"),
    INFOGRAPHICS("Infographics", "IG"),
    MAPS("Maps", "MP"),
    TRANSITIONS("Transitions", "TR"),
    NARRATION("Narration", "VO"),
    DIALOGUE("Dialogue", "DL"),
    MUSIC("Music", "MU"),
    SOUND_EFFECTS("Sound Effects", "FX"),
    AMBIENT("Ambient", "AM"),
    SUBTITLE("Subtitle", "CC")
}

enum class MotionGraphicsStudioTab(val label: String) {
    TEMPLATES("Templates"),
    TEXT_GFX("Text & GFX"),
    SUBTITLES("Subtitles"),
    INFOGRAPHICS("Infographics"),
    MAPS("Maps"),
    FLOW_TRANSITIONS("Transitions")
}

enum class AudioStudioTab(val label: String) {
    NARRATION("Narration"),
    DIALOGUE("Dialogue & Sync"),
    MUSIC_SFX("Music & SFX"),
    LIBRARY("Library")
}

data class TtsVoiceOption(
    val voiceId: String,
    val displayName: String,
    val languageTag: String,
    val localeLabel: String,
    val requiresNetwork: Boolean = false
)

enum class CompactWorkspaceTab(val label: String) {
    TIMELINE("Timeline"),
    PANELS("Studio Panels"),
    INSPECTOR("Inspector")
}

enum class InspectorSection(val label: String) {
    SCENE("Scene"),
    LAYERS("Layers"),
    ANIMATE("Animate"),
    CHARACTER("Characters"),
    CAMERA("Camera"),
    BACKGROUND("Backdrop"),
    EFFECTS("FX & GFX")
}

sealed interface AiScriptState {
    data object Idle : AiScriptState
    data object Generating : AiScriptState
    data class Success(val sceneCount: Int, val characterCount: Int = 0) : AiScriptState
    data class Error(val message: String) : AiScriptState
}

sealed interface CharacterImageGenState {
    data object Idle : CharacterImageGenState
    data class Generating(val characterName: String, val viewType: String) : CharacterImageGenState
    data class Success(val characterId: String, val viewType: String) : CharacterImageGenState
    data class Error(val message: String) : CharacterImageGenState
}

sealed interface VisualAssetGenState {
    data object Idle : VisualAssetGenState
    data class Generating(val label: String) : VisualAssetGenState
    data class PreviewReady(
        val asset: GeneratedVisualAsset,
        val savedAssetId: String? = null
    ) : VisualAssetGenState
    data class Error(val message: String) : VisualAssetGenState
}

sealed interface SceneSuggestionsState {
    data object Idle : SceneSuggestionsState
    data object Loading : SceneSuggestionsState
    data class Ready(val suggestions: SceneVisualSuggestions) : SceneSuggestionsState
    data class Error(val message: String) : SceneSuggestionsState
}

sealed interface AudioImportState {
    data object Idle : AudioImportState
    data class Importing(val fileName: String) : AudioImportState
    data class Success(val assetName: String, val durationSec: Double) : AudioImportState
    data class Error(val message: String) : AudioImportState
}

sealed interface TtsGenerationState {
    data object Idle : TtsGenerationState
    data class Synthesizing(val label: String) : TtsGenerationState
    data class Success(val assetName: String, val durationSec: Double) : TtsGenerationState
    data class Error(val message: String) : TtsGenerationState
}

sealed interface SoundSuggestionsState {
    data object Idle : SoundSuggestionsState
    data object Loading : SoundSuggestionsState
    data class Ready(val count: Int) : SoundSuggestionsState
    data class Error(val message: String) : SoundSuggestionsState
}

sealed interface GraphicSuggestionsState {
    data object Idle : GraphicSuggestionsState
    data object Loading : GraphicSuggestionsState
    data class Ready(val count: Int) : GraphicSuggestionsState
    data class Error(val message: String) : GraphicSuggestionsState
}

sealed interface SaveStatus {
    data object Saved : SaveStatus
    data object Unsaved : SaveStatus
    data object Saving : SaveStatus
    data class Error(val message: String) : SaveStatus
}

data class StudioHistorySnapshot(
    val project: StudioProject,
    val scenes: List<StudioScene>,
    val selectedSceneId: String?
)

object StudioPresets {
    val languages = listOf("English", "Hindi", "Bilingual")
    val aspectRatios = listOf("16:9", "21:9", "9:16")
    val frameRates = listOf(24L, 30L, 60L)
    val resolutions = listOf("720p", "1080p", "4K")

    // 11 Supported Documentary Character Emotions
    val characterEmotions = listOf(
        "Happy",
        "Sad",
        "Angry",
        "Worried",
        "Fearful",
        "Surprised",
        "Calm",
        "Thinking",
        "Crying",
        "Determined",
        "Neutral"
    )

    // 16 Supported Documentary Character Actions
    val characterActions = listOf(
        "Walking",
        "Running",
        "Sitting",
        "Standing",
        "Turning",
        "Looking around",
        "Pointing",
        "Hand gestures",
        "Talking",
        "Listening",
        "Crying",
        "Laughing",
        "Thinking",
        "Reacting",
        "Interacting with other characters",
        "Holding objects"
    )

    // 5 Supported Character View & Framing Modes
    val characterViews = listOf(
        "Front View",
        "Side View",
        "Three-Quarter View",
        "Full Body",
        "Portrait"
    )

    // Documentary 2D Visual Styles
    val characterVisualStyles = listOf(
        "Painterly 2D Documentary",
        "Editorial Line & Wash",
        "Graphic Novel Documentary",
        "Warm Archival Watercolor",
        "Stylized Vector Cinema"
    )

    val characterAccentPalette = listOf(
        "#F5A623",
        "#26D0CE",
        "#A78BFA",
        "#34D399",
        "#FF5C6C",
        "#60A5FA"
    )

    val backgrounds = listOf(
        "himalayas" to "Himalayan Dawn Valley",
        "old_delhi" to "Old Delhi Heritage Lane",
        "archive_study" to "Historian's Archive Study",
        "night_studio" to "Dark Documentary Studio",
        "sepia_map" to "Vintage Cartography Canvas"
    )

    val backgroundTints = listOf(
        "#1B2838" to "Deep Slate",
        "#2A1E17" to "Warm Sepia",
        "#11262A" to "Petrol Teal",
        "#1E1B2E" to "Midnight Indigo",
        "#0E1117" to "Neutral Dark"
    )

    val animationStyles = listOf(
        "Subtle Drift",
        "Expressive Gesture",
        "Interview Talk",
        "Walk Cycle",
        "Static Pose"
    )

    val cameraMovements = listOf(
        "Slow Push In",
        "Parallax Pan Left",
        "Parallax Pan Right",
        "Ken Burns Pull Out",
        "Static Lock"
    )

    val effectPresets = listOf(
        "Film Grain",
        "Warm Sepia",
        "Cinematic Vignette",
        "Archival Dust",
        "None"
    )

    val motionGraphicStyles = listOf(
        "Lower Third",
        "Chapter Card",
        "Date Stamp",
        "Map Callout",
        "None"
    )

    val assetCategories = listOf(
        "All",
        "Background",
        "Location",
        "Character",
        "Prop",
        "Image",
        "Audio",
        "SFX",
        "MotionGraphic"
    )

    val timeOfDayOptions = listOf(
        "Dawn",
        "Morning",
        "Midday",
        "Golden Hour",
        "Twilight",
        "Night",
        "Candlelit Interior"
    )

    val weatherOptions = listOf(
        "Clear",
        "Misty",
        "Overcast",
        "Rainy",
        "Snowy",
        "Dusty Haze",
        "Indoor / N/A"
    )

    val propCategories = listOf(
        "Historical Object",
        "Books & Documents",
        "Furniture",
        "Tools & Instruments",
        "Vehicle",
        "Architecture",
        "Everyday Object",
        "Documentary Artifact"
    )

    val audioCategories = listOf(
        "Narration",
        "Dialogue",
        "Music",
        "SFX",
        "Ambient"
    )

    val audioTrackTypes = listOf(
        "NARRATION",
        "DIALOGUE",
        "MUSIC",
        "SFX",
        "AMBIENT"
    )

    val transitionSuggestions = listOf(
        "Cut",
        "Cross Dissolve",
        "Fade to Black",
        "Dip to White",
        "L-Cut Audio Lead",
        "J-Cut Audio Pre-Lap"
    )

    // Phase 6 Motion Graphics, Typography, Infographics, Maps & Transitions Presets
    val fontFamilyOptions = listOf(
        "SPACE_GROTESK" to "Space Grotesk",
        "PLUS_JAKARTA" to "Plus Jakarta Sans",
        "JETBRAINS_MONO" to "JetBrains Mono",
        "SERIF" to "Editorial Serif",
        "SANS" to "Clean Sans",
        "MONOSPACE" to "Typewriter Mono"
    )

    val textEntranceAnimations = listOf(
        "NONE" to "None",
        "FADE_IN" to "Fade In",
        "SLIDE_IN" to "Slide In",
        "TYPEWRITER" to "Typewriter",
        "ZOOM_IN" to "Zoom In",
        "REVEAL" to "Clip Reveal",
        "POP" to "Pop",
        "RISE" to "Rise Up",
        "DROP" to "Drop In",
        "TRACKING_REVEAL" to "Tracking Reveal"
    )

    val textExitAnimations = listOf(
        "NONE" to "None",
        "FADE_OUT" to "Fade Out",
        "SLIDE_OUT" to "Slide Out",
        "ZOOM_OUT" to "Zoom Out"
    )

    val documentaryGraphicTemplates = listOf(
        "MAIN_TITLE" to "Main Title",
        "SCENE_TITLE" to "Scene Title",
        "DOCUMENTARY_TITLE_CARD" to "Documentary Title Card",
        "SECTION_TITLE" to "Section Title",
        "CHAPTER_TITLE" to "Chapter Title",
        "SUBTITLE_CARD" to "Subtitle Card",
        "LOWER_THIRD" to "Lower Third",
        "NAME_DESIGNATION" to "Name & Designation",
        "LOCATION_CAPTION" to "Location Caption",
        "DATE_TIME_CAPTION" to "Date & Time Caption",
        "QUOTE_CARD" to "Quote Card",
        "INFORMATION_CARD" to "Information Card",
        "CREDITS" to "Credits Roll"
    )

    val lowerThirdLayoutPresets = listOf(
        "DOCUMENTARY_BAR" to "Documentary Bar",
        "MINIMAL_SPLIT" to "Minimal Split",
        "EDITORIAL_PILL" to "Editorial Pill",
        "ARCHIVAL_ACCENT" to "Archival Accent"
    )

    val subtitleStylePresets = listOf(
        "DOCUMENTARY_CLEAN" to "Documentary Clean",
        "CINEMA_BOX" to "Cinema Box",
        "HIGH_CONTRAST_YELLOW" to "High-Contrast Yellow",
        "MINIMAL_OUTLINE" to "Minimal Outline"
    )

    val infographicTypeOptions = listOf(
        "BAR_CHART" to "Bar Chart",
        "LINE_CHART" to "Line Chart",
        "PIE_CHART" to "Pie Chart",
        "PERCENTAGE" to "Percentage Ring",
        "STATISTIC" to "Statistic Highlight",
        "COUNTER" to "Numeric Counter",
        "TIMELINE" to "Timeline Graphic",
        "COMPARISON" to "Comparison Graphic",
        "PROCESS" to "Process Diagram",
        "FLOW" to "Flow Diagram"
    )

    val mapPresetStyles = listOf(
        "DARK_ATLAS" to "Dark Documentary Atlas",
        "ARCHIVAL_PARCHMENT" to "Archival Parchment Map",
        "TOPO_SLATE" to "Topographic Slate"
    )

    val sceneTransitionTypes = listOf(
        "CUT" to "Cut",
        "FADE" to "Fade",
        "CROSSFADE" to "Crossfade",
        "DISSOLVE" to "Dissolve",
        "WIPE" to "Wipe",
        "SLIDE" to "Slide",
        "ZOOM" to "Zoom",
        "BLUR" to "Blur"
    )

    val transitionDirections = listOf("LEFT", "RIGHT", "UP", "DOWN", "IN", "OUT")

    val graphicColorSwatches = listOf(
        "#F2F4F8",
        "#F5A623",
        "#26D0CE",
        "#A78BFA",
        "#34D399",
        "#FF5C6C",
        "#60A5FA",
        "#FFE066",
        "#12141A",
        "#1B2838"
    )

    val musicCues = listOf(
        "Himalayan Strings Drone",
        "Sitar & Cello Documentary Pulse",
        "Archival Piano Reflection",
        "Investigative Minimal Synth",
        "Silent / Ambience Only"
    )

    val sfxCues = listOf(
        "High Altitude Wind",
        "Bazaar Twilight Footsteps",
        "Paper Map Rustle & Clock Tick",
        "Projector Reel Click",
        "Room Tone"
    )

    val sampleEnglishScript = """
        SCENE 1 - EXT. HIMALAYAN PASS - DAWN
        Mist curls over ancient stone trade trails as golden sunlight pierces snow-capped ridges.
        DR. AARAV MEHTA (mid-40s historian with round glasses and an ochre scarf) stands pointing toward the northern ridge while MEERA SEN (archival cartographer in an indigo shawl) listens intently, holding a leather map journal.
        DR. AARAV MEHTA: "Long before highways crossed these valleys, every mountain pass carried ink, silk, and human memory."
        MOTION GRAPHIC: Chapter I · The High Passes (1842)

        SCENE 2 - EXT. OLD DELHI HERITAGE BAZAAR - TWILIGHT
        Warm brass lanterns flicker along carved sandstone arches. MEERA SEN walks down the cobblestone lane looking around thoughtfully, while ELDER CALLIGRAPHER HAKIM (70s, white beard, ivory kurta) gestures warmly from his workshop doorway.
        DR. AARAV MEHTA (V.O.): "In the narrow lanes of Shahjahanabad, calligraphers and mapmakers traded tales brought down from the northern frontier."
        SFX: Soft lantern creak, distant evening bazaar murmur.

        SCENE 3 - INT. HISTORIAN'S ARCHIVE STUDY - NIGHT
        A green banker's lamp illuminates fragile hand-drawn parchment maps. DR. AARAV MEHTA sits beside MEERA SEN as they examine the antique map together with calm determination.
        DR. AARAV MEHTA: "When we trace these faded borders today, we are not just studying geography — we are listening to the voices that survived."
        MOTION GRAPHIC: Lower Third · Dr. Aarav Mehta & Meera Sen
    """.trimIndent()

    val sampleHindiScript = """
        दृश्य 1 - हिमालय की घाटी - सूर्योदय
        बर्फीली चोटियों के ऊपर सुनहरी सुबह की पहली किरणें। डॉ. आरव मेहता (45 वर्षीय इतिहासकार, गोल चश्मा और गेरुआ मफलर) पहाड़ी दर्रे की ओर इशारा करते हुए बोलते हैं और मीरा सेन (मानचित्रकार, नीली शॉल) ध्यान से सुनती हैं।
        डॉ. आरव मेहता: "सदियों पहले, जब न सड़कें थीं और न नक्शे, तब इन्हीं पहाड़ी दर्रों से कहानियाँ और संस्कृतियाँ सफर करती थीं।"
        मोशन ग्राफिक: अध्याय १ · अनकहे रास्ते

        दृश्य 2 - पुरानी दिल्ली की ऐतिहासिक गली - संध्या
        नक्काशीदार मेहराबों के नीचे पीतल के लालटेन जलते हैं। मीरा सेन उत्सुकता से चारों ओर देखते हुए गली में चलती हैं और वृद्ध सुलेखक हकीम (सफेद दाढ़ी, हाथ में पुरानी पांडुलिपि) से बात करती हैं।
        डॉ. आरव मेहता: "पुरानी दिल्ली की इन गलियों में आज भी उन कारीगरों और इतिहासकारों की गूँज सुनाई देती है।"

        दृश्य 3 - इतिहासकार का अध्ययन कक्ष - रात
        मेज़ पर फैले पुराने मानचित्र और हरे लैंप की रोशनी में डॉ. आरव मेहता और मीरा सेन शांत और दृढ़ भाव से मानचित्र का अध्ययन करते हैं।
        डॉ. आरव मेहता: "हर पुराने दस्तावेज़ के पीछे एक जीवित कहानी छुपी होती है, जिसे आज हम फिर से पर्दे पर जीवंत कर रहे हैं।"
    """.trimIndent()
}
