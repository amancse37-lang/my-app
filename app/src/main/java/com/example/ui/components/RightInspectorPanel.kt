package com.example.ui.components

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.animation.AnimatableProperty
import com.example.animation.AnimationTargetType
import com.example.animation.EasingType
import com.example.cinematics.CinematicCameraPreset
import com.example.cinematics.LightingPreset
import com.example.compositor.SceneLayerType
import com.example.compositor.SelectedStageTarget
import com.example.data.CharacterBitmapCodec
import com.example.data.CharacterReferenceSheet
import com.example.data.InspectorSection
import com.example.data.SceneCharacterPlacement
import com.example.data.SceneCinematics
import com.example.data.SceneVisualElement
import com.example.data.StudioAsset
import com.example.data.StudioCharacter
import com.example.data.StudioPresets
import com.example.data.StudioScene
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioAmberDim
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCharcoal
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioElevated
import com.example.ui.theme.StudioPanel
import com.example.ui.theme.StudioTeal
import com.example.ui.theme.StudioTealDim
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.StudioTimecodeStyle

private val characterStageLayers = listOf(
    SceneLayerType.MIDGROUND,
    SceneLayerType.CHARACTERS,
    SceneLayerType.PROPS,
    SceneLayerType.FOREGROUND
)

@Composable
fun RightInspectorPanel(
    scene: StudioScene?,
    sceneTimeSec: Double = 0.0,
    isPlaying: Boolean = false,
    characters: List<StudioCharacter>,
    characterReferencesByCharId: Map<String, List<CharacterReferenceSheet>> = emptyMap(),
    assets: List<StudioAsset>,
    selectedCastCharacterId: String?,
    selectedStageTarget: SelectedStageTarget?,
    activeSection: InspectorSection,
    onSelectSection: (InspectorSection) -> Unit,
    onSelectStageTarget: (SelectedStageTarget?) -> Unit,
    onSelectCastCharacter: (String) -> Unit,
    onAddCharacterToScene: (String) -> Unit,
    onRemoveCharacterFromScene: (String) -> Unit,
    onUpdateCastMember: (String, Boolean, (SceneCharacterPlacement) -> SceneCharacterPlacement) -> Unit,
    onNudgeCharacterZIndex: (String, Int) -> Unit,
    onToggleLayerVisibility: (SceneLayerType) -> Unit,
    onMoveLayerOrder: (SceneLayerType, Int) -> Unit,
    onUpdateLayerOpacity: (SceneLayerType, Double) -> Unit,
    onUpdateLayerParallax: (SceneLayerType, Double) -> Unit = { _, _ -> },
    onAddVisualElementFromAsset: (StudioAsset, SceneLayerType) -> Unit,
    onImportVisualElement: (Uri, SceneLayerType) -> Unit,
    onRemoveVisualElement: (String) -> Unit,
    onUpdateVisualElement: (String, Boolean, (SceneVisualElement) -> SceneVisualElement) -> Unit,
    onNudgeElementZIndex: (String, Int) -> Unit,
    onOpenCharacterGenerator: (StudioCharacter) -> Unit,
    onOpenBackgroundGenerator: () -> Unit = {},
    onOpenPropGenerator: (SceneLayerType) -> Unit = {},
    onOpenSceneSuggestions: () -> Unit = {},
    onSeekWithinScene: (Double) -> Unit = {},
    onTogglePlayPause: () -> Unit = {},
    onUpsertKeyframe: (AnimationTargetType, String, AnimatableProperty, Double, EasingType) -> Unit = { _, _, _, _, _ -> },
    onUpdateKeyframeEasing: (String, EasingType) -> Unit = { _, _ -> },
    onDeleteKeyframe: (String) -> Unit = {},
    onClearTargetKeyframes: (AnimationTargetType, String) -> Unit = { _, _ -> },
    onJumpPrevKeyframe: (AnimationTargetType, String) -> Unit = { _, _ -> },
    onJumpNextKeyframe: (AnimationTargetType, String) -> Unit = { _, _ -> },
    onApplyCharacterTransformPreset: (String, String) -> Unit = { _, _ -> },
    onApplyCameraPreset: (String) -> Unit = {},
    onUpsertActingCue: (String, String, String, String, String) -> Unit = { _, _, _, _, _ -> },
    onDeleteActingCue: (String) -> Unit = {},
    showFramingGuides: Boolean = false,
    onToggleFramingGuides: () -> Unit = {},
    onApplyCinematicCameraPreset: (CinematicCameraPreset) -> Unit = {},
    onResetCameraToFit: () -> Unit = {},
    onApplyLightingPreset: (LightingPreset) -> Unit = {},
    onUpdateSceneCinematics: (Boolean, (SceneCinematics) -> SceneCinematics) -> Unit = { _, _ -> },
    onUpdateScene: (Boolean, (StudioScene) -> StudioScene) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(StudioPanel)
            .border(1.dp, StudioBorder)
    ) {
        // Inspector Header + Section Tabs
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(StudioCharcoal)
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Text(
                text = "INSPECTOR",
                style = MaterialTheme.typography.titleSmall,
                color = StudioAmber
            )
            Spacer(Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                InspectorSection.entries.forEach { section ->
                    val selected = activeSection == section
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (selected) StudioAmberDim else StudioElevated,
                        border = BorderStroke(1.dp, if (selected) StudioAmber else StudioBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onSelectSection(section) }
                            .testTag("inspector_tab_${section.name.lowercase()}")
                    ) {
                        Text(
                            text = section.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = if (selected) StudioAmber else StudioTextSecondary,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        if (scene == null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Select a scene to inspect properties",
                    style = MaterialTheme.typography.bodySmall,
                    color = StudioTextMuted
                )
            }
            return
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            when (activeSection) {
                InspectorSection.SCENE -> {
                    InspectorFieldLabel("Scene Title")
                    OutlinedTextField(
                        value = scene.title,
                        onValueChange = { v -> onUpdateScene(false) { it.copy(title = v.take(120)) } },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = StudioTextPrimary),
                        colors = inspectorTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    InspectorFieldLabel("Scene Description")
                    OutlinedTextField(
                        value = scene.description,
                        onValueChange = { v -> onUpdateScene(false) { it.copy(description = v.take(2000)) } },
                        minLines = 2,
                        maxLines = 4,
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = StudioTextPrimary),
                        colors = inspectorTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    InspectorSliderRow(
                        label = "Scene Duration",
                        valueText = "${String.format("%.1f", scene.durationSec)}s",
                        value = scene.durationSec.toFloat(),
                        range = 2f..30f,
                        onValueChange = { v ->
                            onUpdateScene(false) { it.copy(durationSec = (v * 2).toInt() / 2.0) }
                        }
                    )

                    InspectorFieldLabel("Foreground Prop Notes (Timeline)")
                    OutlinedTextField(
                        value = scene.objectProp,
                        onValueChange = { v -> onUpdateScene(false) { it.copy(objectProp = v.take(120)) } },
                        placeholder = { Text("e.g. Brass Compass", color = StudioTextMuted) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = StudioTextPrimary),
                        colors = inspectorTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                InspectorSection.LAYERS -> {
                    LayersInspectorSection(
                        scene = scene,
                        assets = assets,
                        selectedStageTarget = selectedStageTarget,
                        onSelectStageTarget = onSelectStageTarget,
                        onToggleLayerVisibility = onToggleLayerVisibility,
                        onMoveLayerOrder = onMoveLayerOrder,
                        onUpdateLayerOpacity = onUpdateLayerOpacity,
                        onUpdateLayerParallax = onUpdateLayerParallax,
                        onAddVisualElementFromAsset = onAddVisualElementFromAsset,
                        onImportVisualElement = onImportVisualElement,
                        onRemoveVisualElement = onRemoveVisualElement,
                        onUpdateVisualElement = onUpdateVisualElement,
                        onNudgeElementZIndex = onNudgeElementZIndex,
                        onOpenPropGenerator = onOpenPropGenerator,
                        onOpenSceneSuggestions = onOpenSceneSuggestions
                    )
                }

                InspectorSection.ANIMATE -> {
                    AnimationInspectorSection(
                        scene = scene,
                        sceneTimeSec = sceneTimeSec,
                        isPlaying = isPlaying,
                        characters = characters,
                        characterReferencesByCharId = characterReferencesByCharId,
                        selectedStageTarget = selectedStageTarget,
                        onSelectStageTarget = onSelectStageTarget,
                        onSeekWithinScene = onSeekWithinScene,
                        onTogglePlayPause = onTogglePlayPause,
                        onUpsertKeyframe = onUpsertKeyframe,
                        onUpdateKeyframeEasing = onUpdateKeyframeEasing,
                        onDeleteKeyframe = onDeleteKeyframe,
                        onClearTargetKeyframes = onClearTargetKeyframes,
                        onJumpPrevKeyframe = onJumpPrevKeyframe,
                        onJumpNextKeyframe = onJumpNextKeyframe,
                        onApplyCharacterTransformPreset = onApplyCharacterTransformPreset,
                        onApplyCameraPreset = onApplyCameraPreset,
                        onUpsertActingCue = onUpsertActingCue,
                        onDeleteActingCue = onDeleteActingCue
                    )
                }

                InspectorSection.CHARACTER -> {
                    val cast = scene.effectiveCast()
                    val charMap = characters.associateBy { it.id }
                    val activePlacement = cast.firstOrNull { it.characterId == selectedCastCharacterId }
                        ?: cast.firstOrNull()
                    val activeChar = activePlacement?.let { charMap[it.characterId] }

                    InspectorFieldLabel("Assigned Scene Cast (${cast.size})")
                    if (cast.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(7.dp),
                            color = StudioCharcoal,
                            border = BorderStroke(1.dp, StudioBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No characters assigned to this scene yet. Add a character from your library below.",
                                style = MaterialTheme.typography.bodySmall,
                                color = StudioTextMuted,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            cast.forEach { placement ->
                                val c = charMap[placement.characterId]
                                val isSelected = placement.characterId == activePlacement?.characterId
                                val accent = parseHexColor(c?.accentColorHex.orEmpty(), StudioAmber)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) StudioElevated else StudioCharcoal,
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) accent else StudioBorder
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectCastCharacter(placement.characterId) }
                                        .testTag("scene_cast_item_${c?.charCode?.lowercase() ?: placement.characterId}")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        if (c != null) {
                                            CharacterAvatarThumbnail(
                                                character = c,
                                                size = 32.dp,
                                                borderColor = accent
                                            )
                                            Spacer(Modifier.width(8.dp))
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = c?.let { "${it.charCode} · ${it.name}" } ?: placement.characterId,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = StudioTextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${placement.expression} · ${placement.action}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                                color = if (isSelected) StudioTeal else StudioTextMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        IconButton(
                                            onClick = { onRemoveCharacterFromScene(placement.characterId) },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .testTag("remove_cast_${c?.charCode?.lowercase() ?: placement.characterId}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove character from scene",
                                                tint = StudioCoral,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Add Character to Scene Picker
                    val assignedIds = cast.map { it.characterId }.toSet()
                    val unassigned = characters.filterNot { it.id in assignedIds }
                    if (unassigned.isNotEmpty()) {
                        InspectorFieldLabel("Add Character to Scene")
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            unassigned.forEach { c ->
                                FilterChip(
                                    selected = false,
                                    onClick = { onAddCharacterToScene(c.id) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = StudioAmber,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    },
                                    label = { Text("${c.charCode} ${c.name}", style = MaterialTheme.typography.labelSmall) },
                                    colors = inspectorChipColors(),
                                    modifier = Modifier.testTag("add_cast_${c.charCode.lowercase()}")
                                )
                            }
                        }
                    }

                    if (activePlacement != null) {
                        val activeCharId = activePlacement.characterId
                        if (activeChar != null) {
                            val hasTransparentAlpha = remember(activeChar.referenceImageBase64) {
                                if (activeChar.referenceImageBase64.isNotBlank()) {
                                    CharacterBitmapCodec.decodeBase64ToBitmap(activeChar.referenceImageBase64)
                                        ?.let { CharacterBitmapCodec.hasActualTransparency(it) } == true
                                } else {
                                    false
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(7.dp),
                                color = StudioAmberDim.copy(alpha = 0.45f),
                                border = BorderStroke(1.dp, StudioAmber.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "EDITING ${activeChar.charCode} · ${activeChar.name}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = StudioAmber,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (hasTransparentAlpha) {
                                                "${activeChar.visualStyle} · Transparent PNG alpha"
                                            } else {
                                                "${activeChar.visualStyle} · Opaque image (no alpha cutout)"
                                            },
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                            color = if (hasTransparentAlpha) StudioTeal else StudioTextSecondary
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(5.dp),
                                        color = StudioPanel,
                                        border = BorderStroke(1.dp, StudioAmber.copy(alpha = 0.6f)),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(5.dp))
                                            .clickable { onOpenCharacterGenerator(activeChar) }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = "Generate 2D Reference",
                                                tint = StudioAmber,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(Modifier.width(3.dp))
                                            Text("Ref", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = StudioAmber)
                                        }
                                    }
                                }
                            }
                        }

                        // Character Staging Layer, Visibility, Flip & Z-Order
                        InspectorFieldLabel("Stage Layer & Ordering")
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            characterStageLayers.forEach { layer ->
                                FilterChip(
                                    selected = activePlacement.layer.equals(layer.name, ignoreCase = true),
                                    onClick = {
                                        onUpdateCastMember(activeCharId, true) {
                                            it.copy(layer = layer.name)
                                        }
                                    },
                                    label = { Text(layer.label, style = MaterialTheme.typography.labelSmall) },
                                    colors = inspectorTealChipColors()
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilterChip(
                                selected = activePlacement.visible,
                                onClick = {
                                    onUpdateCastMember(activeCharId, true) { it.copy(visible = !it.visible) }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (activePlacement.visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp)
                                    )
                                },
                                label = { Text(if (activePlacement.visible) "Visible" else "Hidden", style = MaterialTheme.typography.labelSmall) },
                                colors = inspectorTealChipColors()
                            )
                            FilterChip(
                                selected = activePlacement.flipX,
                                onClick = {
                                    onUpdateCastMember(activeCharId, true) { it.copy(flipX = !it.flipX) }
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Flip, contentDescription = null, modifier = Modifier.size(13.dp))
                                },
                                label = { Text("Flip H", style = MaterialTheme.typography.labelSmall) },
                                colors = inspectorChipColors()
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = "Z:${activePlacement.zIndex}",
                                style = StudioTimecodeStyle.copy(fontSize = 10.sp),
                                color = StudioTextSecondary
                            )
                            Surface(
                                shape = RoundedCornerShape(5.dp),
                                color = StudioCharcoal,
                                border = BorderStroke(1.dp, StudioBorder),
                                modifier = Modifier.clickable { onNudgeCharacterZIndex(activeCharId, -1) }
                            ) {
                                Text(
                                    text = "-",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = StudioTextSecondary,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(5.dp),
                                color = StudioCharcoal,
                                border = BorderStroke(1.dp, StudioBorder),
                                modifier = Modifier.clickable { onNudgeCharacterZIndex(activeCharId, +1) }
                            ) {
                                Text(
                                    text = "+",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = StudioAmber,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Scene-Specific Expression (11 Emotions)
                        InspectorFieldLabel("Scene Expression")
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            StudioPresets.characterEmotions.forEach { emotion ->
                                FilterChip(
                                    selected = activePlacement.expression == emotion,
                                    onClick = {
                                        onUpdateCastMember(activeCharId, true) {
                                            it.copy(expression = emotion)
                                        }
                                    },
                                    label = { Text(emotion, style = MaterialTheme.typography.labelSmall) },
                                    colors = inspectorChipColors()
                                )
                            }
                        }

                        // Scene-Specific Action (16 Actions)
                        InspectorFieldLabel("Scene Action")
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            StudioPresets.characterActions.forEach { action ->
                                FilterChip(
                                    selected = activePlacement.action == action,
                                    onClick = {
                                        onUpdateCastMember(activeCharId, true) {
                                            it.copy(action = action)
                                        }
                                    },
                                    label = { Text(action, style = MaterialTheme.typography.labelSmall) },
                                    colors = inspectorTealChipColors()
                                )
                            }
                        }

                        // Pose / Camera View Framing (5 Views)
                        InspectorFieldLabel("Character Pose / View")
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            StudioPresets.characterViews.forEach { view ->
                                FilterChip(
                                    selected = activePlacement.poseView == view,
                                    onClick = {
                                        onUpdateCastMember(activeCharId, true) {
                                            it.copy(poseView = view)
                                        }
                                    },
                                    label = { Text(view, style = MaterialTheme.typography.labelSmall) },
                                    colors = inspectorChipColors()
                                )
                            }
                        }

                        // Per-Character Transform Controls (2-way synced with 16:9 compositor stage)
                        InspectorSliderRow(
                            label = "Position X",
                            valueText = "${activePlacement.posX.toInt()} px",
                            value = activePlacement.posX.toFloat(),
                            range = -480f..480f,
                            onValueChange = { v ->
                                onUpdateCastMember(activeCharId, false) { it.copy(posX = v.toDouble()) }
                            }
                        )

                        InspectorSliderRow(
                            label = "Position Y",
                            valueText = "${activePlacement.posY.toInt()} px",
                            value = activePlacement.posY.toFloat(),
                            range = -270f..270f,
                            onValueChange = { v ->
                                onUpdateCastMember(activeCharId, false) { it.copy(posY = v.toDouble()) }
                            }
                        )

                        InspectorSliderRow(
                            label = "Scale",
                            valueText = "${String.format("%.2f", activePlacement.scale)}x",
                            value = activePlacement.scale.toFloat(),
                            range = 0.25f..3.0f,
                            onValueChange = { v ->
                                onUpdateCastMember(activeCharId, false) { it.copy(scale = v.toDouble()) }
                            }
                        )

                        InspectorSliderRow(
                            label = "Rotation",
                            valueText = "${activePlacement.rotationDeg.toInt()}°",
                            value = activePlacement.rotationDeg.toFloat(),
                            range = -180f..180f,
                            onValueChange = { v ->
                                onUpdateCastMember(activeCharId, false) { it.copy(rotationDeg = v.toDouble()) }
                            }
                        )

                        InspectorSliderRow(
                            label = "Opacity",
                            valueText = "${(activePlacement.opacity * 100).toInt()}%",
                            value = activePlacement.opacity.toFloat(),
                            range = 0f..1f,
                            onValueChange = { v ->
                                onUpdateCastMember(activeCharId, false) { it.copy(opacity = v.toDouble()) }
                            }
                        )
                    }

                    InspectorFieldLabel("Scene Motion Style")
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        StudioPresets.animationStyles.forEach { anim ->
                            val selected = scene.animationStyle == anim
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (selected) StudioTealDim else StudioCharcoal,
                                border = BorderStroke(1.dp, if (selected) StudioTeal else StudioBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onUpdateScene(true) { it.copy(animationStyle = anim) } }
                            ) {
                                Text(
                                    text = anim,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (selected) StudioTeal else StudioTextPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }

                InspectorSection.CAMERA -> {
                    CameraDepthInspectorSection(
                        scene = scene,
                        showFramingGuides = showFramingGuides,
                        onToggleFramingGuides = onToggleFramingGuides,
                        onApplyCinematicCameraPreset = onApplyCinematicCameraPreset,
                        onResetCameraToFit = onResetCameraToFit,
                        onUpdateSceneCinematics = onUpdateSceneCinematics,
                        onUpdateScene = onUpdateScene,
                        onUpdateLayerParallax = onUpdateLayerParallax,
                        onOpenAnimateTab = { onSelectSection(InspectorSection.ANIMATE) }
                    )
                }

                InspectorSection.BACKGROUND -> {
                    Button(
                        onClick = onOpenBackgroundGenerator,
                        colors = ButtonDefaults.buttonColors(containerColor = StudioAmber, contentColor = com.example.ui.theme.StudioVoid),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("inspector_generate_bg_button")
                    ) {
                        Icon(Icons.Default.Landscape, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Generate AI Scene Background",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    OutlinedButton(
                        onClick = onOpenSceneSuggestions,
                        border = BorderStroke(1.dp, StudioTeal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("inspector_scene_suggestions_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = StudioTeal, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Generate Visual Assets for This Scene",
                            style = MaterialTheme.typography.labelSmall,
                            color = StudioTeal
                        )
                    }

                    val savedBgAssets = remember(assets) {
                        assets.filter {
                            (it.category == "Background" || it.category == "Location") &&
                                (it.imageBase64.isNotBlank() || it.localUri.isNotBlank())
                        }
                    }
                    if (savedBgAssets.isNotEmpty()) {
                        InspectorFieldLabel("Saved Project Backgrounds (${savedBgAssets.size})")
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            savedBgAssets.forEach { bgAsset ->
                                val targetKey = when {
                                    bgAsset.imageBase64.isNotBlank() -> "asset:${bgAsset.id}"
                                    bgAsset.localUri.isNotBlank() -> "uri:${bgAsset.localUri}"
                                    else -> bgAsset.presetKey
                                }
                                val selected = scene.backgroundPreset == targetKey
                                val decodedThumb = remember(bgAsset.imageBase64) {
                                    if (bgAsset.imageBase64.isNotBlank()) {
                                        CharacterBitmapCodec.decodeBase64ToBitmap(bgAsset.imageBase64)
                                    } else {
                                        null
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(7.dp),
                                    color = if (selected) StudioTealDim else StudioCharcoal,
                                    border = BorderStroke(1.dp, if (selected) StudioTeal else StudioBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onUpdateScene(true) { it.copy(backgroundPreset = targetKey) } }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(width = 48.dp, height = 28.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(com.example.ui.theme.StudioVoid)
                                        ) {
                                            if (decodedThumb != null) {
                                                Image(
                                                    bitmap = decodedThumb.asImageBitmap(),
                                                    contentDescription = bgAsset.name,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                val safeUri = safeMediaUriOrNull(bgAsset.localUri)
                                                if (safeUri != null) {
                                                    AsyncImage(
                                                        model = safeUri,
                                                        contentDescription = bgAsset.name,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = bgAsset.name,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (selected) StudioTeal else StudioTextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = bgAsset.visualStyle.ifBlank { bgAsset.durationLabel },
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                                color = StudioTextMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    InspectorFieldLabel("Built-In Environment Presets")
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        StudioPresets.backgrounds.forEach { (key, title) ->
                            val selected = scene.backgroundPreset == key
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (selected) StudioTealDim else StudioCharcoal,
                                border = BorderStroke(1.dp, if (selected) StudioTeal else StudioBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onUpdateScene(true) { it.copy(backgroundPreset = key) } }
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (selected) StudioTeal else StudioTextPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    InspectorFieldLabel("Atmospheric Grading Tint")
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        StudioPresets.backgroundTints.forEach { (hex, label) ->
                            val selected = scene.backgroundTintHex.equals(hex, ignoreCase = true)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { onUpdateScene(true) { it.copy(backgroundTintHex = hex) } }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(parseHexColor(hex))
                                        .border(
                                            width = if (selected) 2.dp else 1.dp,
                                            color = if (selected) StudioAmber else StudioBorder,
                                            shape = CircleShape
                                        )
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = label.split(" ").first(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                    color = if (selected) StudioAmber else StudioTextMuted
                                )
                            }
                        }
                    }
                }

                InspectorSection.EFFECTS -> {
                    LightingAtmosphereVfxSection(
                        scene = scene,
                        onApplyLightingPreset = onApplyLightingPreset,
                        onUpdateSceneCinematics = onUpdateSceneCinematics,
                        onUpdateScene = onUpdateScene
                    )
                }
            }
        }
    }
}

@Composable
internal fun InspectorFieldLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
        color = StudioTextSecondary
    )
}

@Composable
internal fun InspectorSliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = StudioTextSecondary
            )
            Text(
                text = valueText,
                style = StudioTimecodeStyle.copy(fontSize = 11.sp),
                color = StudioTeal
            )
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = StudioAmber,
                activeTrackColor = StudioAmber,
                inactiveTrackColor = StudioElevated
            ),
            modifier = Modifier.height(30.dp)
        )
    }
}

@Composable
internal fun inspectorTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = StudioCharcoal,
    unfocusedContainerColor = StudioCharcoal,
    focusedBorderColor = StudioAmber,
    unfocusedBorderColor = StudioBorder
)

@Composable
private fun inspectorChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = StudioAmberDim,
    selectedLabelColor = StudioAmber
)

@Composable
private fun inspectorTealChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = StudioTealDim,
    selectedLabelColor = StudioTeal
)
