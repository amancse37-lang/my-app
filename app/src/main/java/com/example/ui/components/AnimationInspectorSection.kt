package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.animation.AnimatableProperty
import com.example.animation.AnimationEvaluator
import com.example.animation.AnimationTargetType
import com.example.animation.EasingType
import com.example.compositor.SelectedStageTarget
import com.example.data.CharacterReferenceSheet
import com.example.data.StudioCharacter
import com.example.data.StudioPresets
import com.example.data.StudioScene
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioAmberDim
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCharcoal
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioElevated
import com.example.ui.theme.StudioTeal
import com.example.ui.theme.StudioTealDim
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.StudioTimecodeStyle
import com.example.ui.theme.StudioVoid

private data class AnimationTargetChoice(
    val targetType: AnimationTargetType,
    val targetId: String,
    val label: String
)

@Composable
fun AnimationInspectorSection(
    scene: StudioScene,
    sceneTimeSec: Double,
    isPlaying: Boolean,
    characters: List<StudioCharacter>,
    characterReferencesByCharId: Map<String, List<CharacterReferenceSheet>>,
    selectedStageTarget: SelectedStageTarget?,
    onSelectStageTarget: (SelectedStageTarget?) -> Unit,
    onSeekWithinScene: (Double) -> Unit,
    onTogglePlayPause: () -> Unit,
    onUpsertKeyframe: (AnimationTargetType, String, AnimatableProperty, Double, EasingType) -> Unit,
    onUpdateKeyframeEasing: (String, EasingType) -> Unit,
    onDeleteKeyframe: (String) -> Unit,
    onClearTargetKeyframes: (AnimationTargetType, String) -> Unit,
    onJumpPrevKeyframe: (AnimationTargetType, String) -> Unit,
    onJumpNextKeyframe: (AnimationTargetType, String) -> Unit,
    onApplyCharacterTransformPreset: (String, String) -> Unit,
    onApplyCameraPreset: (String) -> Unit,
    onUpsertActingCue: (String, String, String, String, String) -> Unit,
    onDeleteActingCue: (String) -> Unit
) {
    val safeDuration = scene.durationSec.coerceAtLeast(0.5)
    val safeTime = sceneTimeSec.coerceIn(0.0, safeDuration)
    val charById = remember(characters) { characters.associateBy { it.id } }
    val cast = remember(scene) { scene.effectiveCast() }

    val targetChoices = remember(scene, characters) {
        buildList {
            cast.forEach { placement ->
                val c = charById[placement.characterId]
                add(
                    AnimationTargetChoice(
                        targetType = AnimationTargetType.CHARACTER,
                        targetId = placement.characterId,
                        label = c?.let { "${it.charCode} ${it.name}" } ?: placement.characterId
                    )
                )
            }
            scene.sceneElements.forEach { el ->
                add(
                    AnimationTargetChoice(
                        targetType = AnimationTargetType.ELEMENT,
                        targetId = el.elementId,
                        label = el.name.ifBlank { "Element" }
                    )
                )
            }
            scene.motionGraphics.forEach { gfx ->
                add(
                    AnimationTargetChoice(
                        targetType = AnimationTargetType.GRAPHIC,
                        targetId = gfx.graphicId,
                        label = "GFX: ${gfx.title.ifBlank { gfx.templateType }.take(16)}"
                    )
                )
            }
            add(
                AnimationTargetChoice(
                    targetType = AnimationTargetType.CAMERA,
                    targetId = AnimationEvaluator.CAMERA_TARGET_ID,
                    label = "Camera Rig"
                )
            )
        }
    }

    var selectedTargetKey by remember(scene.id) {
        mutableStateOf(
            when (selectedStageTarget) {
                is SelectedStageTarget.CharacterTarget -> "CHARACTER:${selectedStageTarget.characterId}"
                is SelectedStageTarget.ElementTarget -> "ELEMENT:${selectedStageTarget.elementId}"
                is SelectedStageTarget.GraphicTarget -> "GRAPHIC:${selectedStageTarget.graphicId}"
                null -> targetChoices.firstOrNull()?.let { "${it.targetType.name}:${it.targetId}" }
                    ?: "CAMERA:${AnimationEvaluator.CAMERA_TARGET_ID}"
            }
        )
    }

    LaunchedEffect(selectedStageTarget) {
        when (selectedStageTarget) {
            is SelectedStageTarget.CharacterTarget -> {
                selectedTargetKey = "CHARACTER:${selectedStageTarget.characterId}"
            }
            is SelectedStageTarget.ElementTarget -> {
                selectedTargetKey = "ELEMENT:${selectedStageTarget.elementId}"
            }
            is SelectedStageTarget.GraphicTarget -> {
                selectedTargetKey = "GRAPHIC:${selectedStageTarget.graphicId}"
            }
            null -> Unit
        }
    }

    val activeChoice = targetChoices.firstOrNull { "${it.targetType.name}:${it.targetId}" == selectedTargetKey }
        ?: targetChoices.first()

    var activeEasing by remember { mutableStateOf(EasingType.EASE_IN_OUT) }

    val evaluatedFrame = remember(scene, characters, characterReferencesByCharId, safeTime) {
        AnimationEvaluator.evaluateSceneAtTime(
            scene = scene,
            characters = characters,
            referenceSheetsByCharId = characterReferencesByCharId,
            timeSec = safeTime,
            cameraMuted = false
        )
    }

    // 1. Target Selector Row
    InspectorFieldLabel("Animation Target")
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        targetChoices.forEach { choice ->
            val isSelected = choice == activeChoice
            val chipTint = when (choice.targetType) {
                AnimationTargetType.CHARACTER -> StudioAmber
                AnimationTargetType.ELEMENT -> StudioTeal
                AnimationTargetType.GRAPHIC -> com.example.ui.theme.StudioViolet
                AnimationTargetType.CAMERA -> StudioCoral
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isSelected) chipTint.copy(alpha = 0.22f) else StudioCharcoal,
                border = BorderStroke(1.dp, if (isSelected) chipTint else StudioBorder),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable {
                        selectedTargetKey = "${choice.targetType.name}:${choice.targetId}"
                        when (choice.targetType) {
                            AnimationTargetType.CHARACTER -> onSelectStageTarget(
                                SelectedStageTarget.CharacterTarget(choice.targetId)
                            )
                            AnimationTargetType.ELEMENT -> onSelectStageTarget(
                                SelectedStageTarget.ElementTarget(choice.targetId)
                            )
                            AnimationTargetType.GRAPHIC -> onSelectStageTarget(
                                SelectedStageTarget.GraphicTarget(choice.targetId)
                            )
                            AnimationTargetType.CAMERA -> onSelectStageTarget(null)
                        }
                    }
                    .testTag("anim_target_${choice.targetType.name.lowercase()}_${choice.targetId}")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    if (choice.targetType == AnimationTargetType.CAMERA) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = if (isSelected) chipTint else StudioTextSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text = choice.label,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = if (isSelected) chipTint else StudioTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    // 2. Scene-Local Scrubber & Keyframe Jump Controls
    val distinctTimes = remember(scene.animationKeyframes, scene.characterActingCues, activeChoice) {
        AnimationEvaluator.distinctTimesForTarget(
            keyframes = scene.animationKeyframes,
            actingCues = scene.characterActingCues,
            targetType = activeChoice.targetType,
            targetId = activeChoice.targetId
        )
    }
    val hasPrevKf = AnimationEvaluator.findPreviousTimestamp(distinctTimes, safeTime) != null
    val hasNextKf = AnimationEvaluator.findNextTimestamp(distinctTimes, safeTime) != null

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = StudioCharcoal,
        border = BorderStroke(1.dp, StudioBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "SCENE TIME: ${String.format("%.2f", safeTime)}s / ${String.format("%.1f", safeDuration)}s",
                    style = StudioTimecodeStyle.copy(fontSize = 10.sp),
                    color = StudioAmber,
                    modifier = Modifier.testTag("anim_scene_time_label")
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = { onJumpPrevKeyframe(activeChoice.targetType, activeChoice.targetId) },
                        enabled = hasPrevKf,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("anim_prev_keyframe")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous keyframe",
                            tint = if (hasPrevKf) StudioAmber else StudioTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onTogglePlayPause,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("anim_play_pause")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause preview" else "Play preview",
                            tint = StudioTeal,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    IconButton(
                        onClick = { onJumpNextKeyframe(activeChoice.targetType, activeChoice.targetId) },
                        enabled = hasNextKf,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("anim_next_keyframe")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next keyframe",
                            tint = if (hasNextKf) StudioAmber else StudioTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Slider(
                value = safeTime.toFloat(),
                onValueChange = { onSeekWithinScene(it.toDouble()) },
                valueRange = 0f..safeDuration.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = StudioAmber,
                    activeTrackColor = StudioAmber,
                    inactiveTrackColor = StudioElevated
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .testTag("anim_scene_scrubber")
            )

            // Easing Curve Picker for new/updated keyframes
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Easing:",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                    color = StudioTextMuted
                )
                EasingType.selectableCurves.forEach { curve ->
                    val selected = activeEasing == curve
                    FilterChip(
                        selected = selected,
                        onClick = { activeEasing = curve },
                        label = {
                            Text(
                                text = curve.label,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StudioAmberDim,
                            selectedLabelColor = StudioAmber
                        ),
                        modifier = Modifier.height(26.dp)
                    )
                }
            }
        }
    }

    // 3. Honest Capability Notice & Quick Keyframe Presets
    if (activeChoice.targetType == AnimationTargetType.CHARACTER) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = StudioElevated,
            border = BorderStroke(1.dp, StudioBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = StudioTeal,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Transform keyframes (position, scale, rotation, opacity, flip) + time-based acting cues. Skeletal limb animation and lip-sync are not implemented.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = StudioTextSecondary
                )
            }
        }

        InspectorFieldLabel("Transform Motion Presets (Creates Editable Keyframes)")
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            listOf(
                "walk_across_ltr" to "Traverse L→R",
                "enter_from_left" to "Enter Left",
                "exit_to_right" to "Exit Right",
                "fade_in" to "Fade In"
            ).forEach { (presetKey, label) ->
                OutlinedButton(
                    onClick = { onApplyCharacterTransformPreset(activeChoice.targetId, presetKey) },
                    border = BorderStroke(1.dp, StudioAmber.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("anim_preset_$presetKey")
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                        color = StudioAmber
                    )
                }
            }
        }
    } else if (activeChoice.targetType == AnimationTargetType.CAMERA) {
        InspectorFieldLabel("Camera Keyframe Presets")
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            listOf(
                "push_in" to "Zoom Push In",
                "pan_sweep" to "Pan Sweep",
                "tilt_reveal" to "Tilt Reveal"
            ).forEach { (presetKey, label) ->
                OutlinedButton(
                    onClick = { onApplyCameraPreset(presetKey) },
                    border = BorderStroke(1.dp, StudioCoral.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("anim_cam_preset_$presetKey")
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                        color = StudioCoral
                    )
                }
            }
        }
    }

    // 4. Animatable Properties with Live Evaluated Readout + Direct Keyframing
    val props = AnimatableProperty.forTargetType(activeChoice.targetType)
    val targetKeyframes = remember(scene.animationKeyframes, activeChoice) {
        scene.animationKeyframes.filter { kf ->
            kf.targetType.equals(activeChoice.targetType.name, ignoreCase = true) &&
                (kf.targetId == activeChoice.targetId ||
                    (activeChoice.targetType == AnimationTargetType.CAMERA && kf.targetId.isBlank()))
        }.sortedWith(compareBy({ it.timeSec }, { it.property }))
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        InspectorFieldLabel("Properties (${targetKeyframes.size} KF)")
        if (targetKeyframes.isNotEmpty()) {
            Text(
                text = "Clear All",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = StudioCoral,
                modifier = Modifier
                    .clickable { onClearTargetKeyframes(activeChoice.targetType, activeChoice.targetId) }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .testTag("anim_clear_target_kfs")
            )
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        props.forEach { prop ->
            val currentVal = currentEvaluatedPropertyValue(
                activeChoice = activeChoice,
                property = prop,
                evaluatedFrame = evaluatedFrame,
                scene = scene
            )
            val propKfs = targetKeyframes.filter { it.property.equals(prop.name, ignoreCase = true) }
            val kfAtNow = AnimationEvaluator.findKeyframeAtTime(
                keyframes = propKfs,
                targetType = activeChoice.targetType,
                targetId = activeChoice.targetId,
                property = prop,
                timeSec = safeTime
            )

            Surface(
                shape = RoundedCornerShape(7.dp),
                color = StudioCharcoal,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (kfAtNow != null) StudioAmber else StudioBorder
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = prop.label,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                color = StudioTextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            if (propKfs.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = StudioAmberDim
                                ) {
                                    Text(
                                        text = "${propKfs.size} KF",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                        color = StudioAmber,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = prop.formatValue(currentVal),
                            style = StudioTimecodeStyle.copy(fontSize = 10.sp),
                            color = if (kfAtNow != null) StudioAmber else StudioTeal
                        )

                        Spacer(Modifier.width(6.dp))

                        // Keyframe toggle / add button at playhead
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (kfAtNow != null) StudioAmber else StudioElevated,
                            border = BorderStroke(1.dp, if (kfAtNow != null) StudioAmber else StudioBorder),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    onUpsertKeyframe(
                                        activeChoice.targetType,
                                        activeChoice.targetId,
                                        prop,
                                        currentVal,
                                        activeEasing
                                    )
                                }
                                .testTag("anim_set_kf_${prop.name.lowercase()}")
                        ) {
                            Text(
                                text = if (kfAtNow != null) "◆ KF" else "◇ +KF",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (kfAtNow != null) StudioVoid else StudioTextPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }

                        if (kfAtNow != null) {
                            IconButton(
                                onClick = { onDeleteKeyframe(kfAtNow.keyframeId) },
                                modifier = Modifier
                                    .size(24.dp)
                                    .testTag("anim_del_kf_${prop.name.lowercase()}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Delete ${prop.label} keyframe at current time",
                                    tint = StudioCoral,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }

                    if (prop.discrete) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val isFlipped = currentVal >= 0.5
                            OutlinedButton(
                                onClick = {
                                    onUpsertKeyframe(
                                        activeChoice.targetType,
                                        activeChoice.targetId,
                                        prop,
                                        if (isFlipped) 0.0 else 1.0,
                                        EasingType.STEP
                                    )
                                },
                                border = BorderStroke(1.dp, if (isFlipped) StudioTeal else StudioBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(30.dp)
                            ) {
                                Text(
                                    text = if (isFlipped) "Flipped H (Tap to Keyframe Normal)" else "Normal (Tap to Keyframe Flip H)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = if (isFlipped) StudioTeal else StudioTextSecondary
                                )
                            }
                        }
                    } else {
                        Slider(
                            value = currentVal.toFloat().coerceIn(prop.minValue.toFloat(), prop.maxValue.toFloat()),
                            onValueChange = { newValue ->
                                onUpsertKeyframe(
                                    activeChoice.targetType,
                                    activeChoice.targetId,
                                    prop,
                                    newValue.toDouble(),
                                    activeEasing
                                )
                            },
                            valueRange = prop.minValue.toFloat()..prop.maxValue.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = if (kfAtNow != null) StudioAmber else StudioTeal,
                                activeTrackColor = if (kfAtNow != null) StudioAmber else StudioTeal,
                                inactiveTrackColor = StudioElevated
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp)
                                .testTag("anim_slider_${prop.name.lowercase()}")
                        )
                    }
                }
            }
        }
    }

    // 5. Authored Keyframes List for Selected Target
    if (targetKeyframes.isNotEmpty()) {
        InspectorFieldLabel("Authored Keyframes (${targetKeyframes.size})")
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            targetKeyframes.forEach { kf ->
                val prop = AnimatableProperty.fromKeyOrNull(kf.property)
                val easing = EasingType.fromKeyOrDefault(kf.easing, EasingType.EASE_IN_OUT)
                val nextEasing = when (easing) {
                    EasingType.LINEAR -> EasingType.EASE_IN
                    EasingType.EASE_IN -> EasingType.EASE_OUT
                    EasingType.EASE_OUT -> EasingType.EASE_IN_OUT
                    EasingType.EASE_IN_OUT -> EasingType.LINEAR
                    EasingType.STEP -> EasingType.STEP
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = StudioCharcoal,
                    border = BorderStroke(1.dp, StudioBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSeekWithinScene(kf.timeSec) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${String.format("%.2f", kf.timeSec)}s · ${prop?.shortLabel ?: kf.property} = ${prop?.formatValue(kf.value) ?: kf.value}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = StudioTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (prop?.discrete != true) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = StudioElevated,
                                border = BorderStroke(1.dp, StudioBorder),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { onUpdateKeyframeEasing(kf.keyframeId, nextEasing) }
                            ) {
                                Text(
                                    text = easing.label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                    color = StudioAmber,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = { onDeleteKeyframe(kf.keyframeId) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete keyframe",
                                tint = StudioCoral,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // 6. Character Acting Cues Foundation (for Character Targets)
    if (activeChoice.targetType == AnimationTargetType.CHARACTER) {
        val charId = activeChoice.targetId
        val activePlacement = cast.firstOrNull { it.characterId == charId }
        val activeActing = evaluatedFrame.characterActing[charId]
        val availableSheets = characterReferencesByCharId[charId].orEmpty()
        val charCues = remember(scene.characterActingCues, charId) {
            scene.characterActingCues
                .filter { it.characterId == charId }
                .sortedBy { it.timeSec }
        }

        var draftExpression by remember(charId, activeActing?.expression) {
            mutableStateOf(activeActing?.expression ?: activePlacement?.expression ?: "Neutral")
        }
        var draftAction by remember(charId, activeActing?.action) {
            mutableStateOf(activeActing?.action ?: activePlacement?.action ?: "Standing")
        }
        var draftPoseView by remember(charId, activeActing?.poseView) {
            mutableStateOf(activeActing?.poseView ?: activePlacement?.poseView ?: "Three-Quarter View")
        }
        var draftRefSheetId by remember(charId, activeActing?.matchedReferenceSheetId) {
            mutableStateOf("")
        }

        Spacer(Modifier.height(4.dp))
        InspectorFieldLabel("Character Acting Cues (Time-Based)")

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = StudioCharcoal,
            border = BorderStroke(1.dp, StudioTeal.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = "Active @ ${String.format("%.2f", safeTime)}s: ${activeActing?.expression ?: "-"} · ${activeActing?.action ?: "-"} · ${activeActing?.poseView ?: "-"}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = StudioTeal,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (activeActing?.matchedReferenceSheet == true) {
                        "Reference Switching: Matched generated reference sheet (${availableSheets.size} available)"
                    } else if (availableSheets.isNotEmpty()) {
                        "Reference Switching: Using primary portrait (${availableSheets.size} sheets available — match expression/view or select below)"
                    } else {
                        "Reference Switching: Using primary portrait (generate expression/view sheets in Characters panel to enable automatic switching)"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp),
                    color = StudioTextSecondary
                )

                Text("Expression", style = MaterialTheme.typography.labelSmall, color = StudioTextMuted)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    StudioPresets.characterEmotions.forEach { emotion ->
                        FilterChip(
                            selected = draftExpression.equals(emotion, ignoreCase = true),
                            onClick = { draftExpression = emotion },
                            label = { Text(emotion, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StudioTealDim,
                                selectedLabelColor = StudioTeal
                            ),
                            modifier = Modifier.height(26.dp)
                        )
                    }
                }

                Text("Action / Acting Beat", style = MaterialTheme.typography.labelSmall, color = StudioTextMuted)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    StudioPresets.characterActions.forEach { action ->
                        FilterChip(
                            selected = draftAction.equals(action, ignoreCase = true),
                            onClick = { draftAction = action },
                            label = { Text(action, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StudioAmberDim,
                                selectedLabelColor = StudioAmber
                            ),
                            modifier = Modifier.height(26.dp)
                        )
                    }
                }

                Text("Pose / Framing View", style = MaterialTheme.typography.labelSmall, color = StudioTextMuted)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    StudioPresets.characterViews.forEach { view ->
                        FilterChip(
                            selected = draftPoseView.equals(view, ignoreCase = true),
                            onClick = { draftPoseView = view },
                            label = { Text(view, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StudioTealDim,
                                selectedLabelColor = StudioTeal
                            ),
                            modifier = Modifier.height(26.dp)
                        )
                    }
                }

                if (availableSheets.isNotEmpty()) {
                    Text("Reference Image Sheet", style = MaterialTheme.typography.labelSmall, color = StudioTextMuted)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        FilterChip(
                            selected = draftRefSheetId.isBlank(),
                            onClick = { draftRefSheetId = "" },
                            label = { Text("Auto-Match", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StudioTealDim,
                                selectedLabelColor = StudioTeal
                            ),
                            modifier = Modifier.height(26.dp)
                        )
                        availableSheets.forEach { sheet ->
                            FilterChip(
                                selected = draftRefSheetId == sheet.id,
                                onClick = { draftRefSheetId = sheet.id },
                                label = {
                                    Text(
                                        "${sheet.expression} / ${sheet.viewType}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StudioAmberDim,
                                    selectedLabelColor = StudioAmber
                                ),
                                modifier = Modifier.height(26.dp)
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        onUpsertActingCue(
                            charId,
                            draftExpression,
                            draftAction,
                            draftPoseView,
                            draftRefSheetId
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudioTeal,
                        contentColor = StudioVoid
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .testTag("anim_add_acting_cue_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "Set Acting Cue @ ${String.format("%.2f", safeTime)}s",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (charCues.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                charCues.forEach { cue ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = StudioCharcoal,
                        border = BorderStroke(1.dp, StudioTeal.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSeekWithinScene(cue.timeSec) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${String.format("%.2f", cue.timeSec)}s · ${cue.expression} · ${cue.action}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = StudioTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = cue.poseView,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = StudioTextMuted
                                )
                            }
                            IconButton(
                                onClick = { onDeleteActingCue(cue.cueId) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete acting cue",
                                    tint = StudioCoral,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun currentEvaluatedPropertyValue(
    activeChoice: AnimationTargetChoice,
    property: AnimatableProperty,
    evaluatedFrame: com.example.animation.EvaluatedSceneFrame,
    scene: StudioScene
): Double {
    return when (activeChoice.targetType) {
        AnimationTargetType.CHARACTER -> {
            val t = evaluatedFrame.characterTransforms[activeChoice.targetId]
            val base = scene.effectiveCast().firstOrNull { it.characterId == activeChoice.targetId }
            when (property) {
                AnimatableProperty.POS_X -> t?.posX ?: base?.posX ?: 0.0
                AnimatableProperty.POS_Y -> t?.posY ?: base?.posY ?: 0.0
                AnimatableProperty.SCALE -> t?.scale ?: base?.scale ?: 1.0
                AnimatableProperty.ROTATION -> t?.rotationDeg ?: base?.rotationDeg ?: 0.0
                AnimatableProperty.OPACITY -> t?.opacity ?: base?.opacity ?: 1.0
                AnimatableProperty.FLIP_X -> if (t?.flipX ?: base?.flipX ?: false) 1.0 else 0.0
                else -> 0.0
            }
        }
        AnimationTargetType.ELEMENT -> {
            val t = evaluatedFrame.elementTransforms[activeChoice.targetId]
            val base = scene.sceneElements.firstOrNull { it.elementId == activeChoice.targetId }
            when (property) {
                AnimatableProperty.POS_X -> t?.posX ?: base?.posX ?: 0.0
                AnimatableProperty.POS_Y -> t?.posY ?: base?.posY ?: 0.0
                AnimatableProperty.SCALE -> t?.scale ?: base?.scale ?: 1.0
                AnimatableProperty.ROTATION -> t?.rotationDeg ?: base?.rotationDeg ?: 0.0
                AnimatableProperty.OPACITY -> t?.opacity ?: base?.opacity ?: 1.0
                AnimatableProperty.FLIP_X -> if (t?.flipX ?: base?.flipX ?: false) 1.0 else 0.0
                else -> 0.0
            }
        }
        AnimationTargetType.GRAPHIC -> {
            val base = scene.motionGraphics.firstOrNull { it.graphicId == activeChoice.targetId }
            val eval = if (base != null) {
                AnimationEvaluator.evaluateTransform(
                    keyframes = scene.animationKeyframes,
                    targetType = AnimationTargetType.GRAPHIC,
                    targetId = base.graphicId,
                    timeSec = evaluatedFrame.timeSec,
                    basePosX = base.posX,
                    basePosY = base.posY,
                    baseScale = base.scale,
                    baseRotationDeg = base.rotationDeg,
                    baseOpacity = base.opacity,
                    baseFlipX = false
                )
            } else null
            when (property) {
                AnimatableProperty.POS_X -> eval?.posX ?: base?.posX ?: 0.0
                AnimatableProperty.POS_Y -> eval?.posY ?: base?.posY ?: 0.0
                AnimatableProperty.SCALE -> eval?.scale ?: base?.scale ?: 1.0
                AnimatableProperty.ROTATION -> eval?.rotationDeg ?: base?.rotationDeg ?: 0.0
                AnimatableProperty.OPACITY -> eval?.opacity ?: base?.opacity ?: 1.0
                AnimatableProperty.FLIP_X -> if (eval?.flipX == true) 1.0 else 0.0
                else -> 0.0
            }
        }
        AnimationTargetType.CAMERA -> {
            val cam = evaluatedFrame.camera
            when (property) {
                AnimatableProperty.CAMERA_ZOOM -> cam.zoom
                AnimatableProperty.CAMERA_PAN_X -> cam.panX
                AnimatableProperty.CAMERA_PAN_Y -> cam.panY
                AnimatableProperty.CAMERA_ROTATION -> cam.rotationDeg
                else -> 1.0
            }
        }
    }
}
