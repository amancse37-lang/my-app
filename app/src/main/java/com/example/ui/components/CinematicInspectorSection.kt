package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.animation.AnimationEvaluator
import com.example.animation.AnimationTargetType
import com.example.cinematics.AtmosphereMode
import com.example.cinematics.CinematicCameraPreset
import com.example.cinematics.LetterboxMode
import com.example.cinematics.LightingPreset
import com.example.compositor.SceneCompositor
import com.example.compositor.SceneLayerType
import com.example.data.InspectorSection
import com.example.data.SceneCinematics
import com.example.data.StudioPresets
import com.example.data.StudioScene
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioAmberDim
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCharcoal
import com.example.ui.theme.StudioElevated
import com.example.ui.theme.StudioPanel
import com.example.ui.theme.StudioTeal
import com.example.ui.theme.StudioTealDim
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.StudioTimecodeStyle
import com.example.ui.theme.StudioVoid

@Composable
fun CameraDepthInspectorSection(
    scene: StudioScene,
    showFramingGuides: Boolean,
    onToggleFramingGuides: () -> Unit,
    onApplyCinematicCameraPreset: (CinematicCameraPreset) -> Unit,
    onResetCameraToFit: () -> Unit,
    onUpdateSceneCinematics: (Boolean, (SceneCinematics) -> SceneCinematics) -> Unit,
    onUpdateScene: (Boolean, (StudioScene) -> StudioScene) -> Unit,
    onUpdateLayerParallax: (SceneLayerType, Double) -> Unit,
    onOpenAnimateTab: () -> Unit
) {
    val cin = scene.cinematics
    val cameraKfCount = remember(scene.animationKeyframes) {
        scene.animationKeyframes.count { it.targetType.equals(AnimationTargetType.CAMERA.name, ignoreCase = true) }
    }
    val normalizedLayers = remember(scene.layerStates) {
        SceneCompositor.normalizeLayerStates(scene.layerStates)
            .filter { !it.layerKey.equals(SceneLayerType.OVERLAY.name, ignoreCase = true) }
            .sortedBy { it.order }
    }

    // 1. QUICK CAMERA ACTIONS: FIT TO SCENE, FRAMING GUIDES, KEYFRAME LINK
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedButton(
            onClick = onResetCameraToFit,
            border = BorderStroke(1.dp, StudioAmber.copy(alpha = 0.7f)),
            modifier = Modifier
                .weight(1f)
                .testTag("camera_reset_fit_button")
        ) {
            Icon(
                imageVector = Icons.Default.CenterFocusStrong,
                contentDescription = null,
                tint = StudioAmber,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text("Fit Scene", style = MaterialTheme.typography.labelSmall, color = StudioAmber)
        }
        FilterChip(
            selected = showFramingGuides,
            onClick = onToggleFramingGuides,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.GridOn,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
            },
            label = { Text("3×3", style = MaterialTheme.typography.labelSmall) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = StudioTealDim,
                selectedLabelColor = StudioTeal
            ),
            modifier = Modifier.testTag("camera_guides_toggle")
        )
    }

    // 2. CINEMATIC CAMERA PRESETS (11 REAL PRESETS)
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        InspectorFieldLabel("Cinematic Camera Presets")
        if (cameraKfCount > 0) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = StudioTealDim,
                border = BorderStroke(1.dp, StudioTeal.copy(alpha = 0.6f)),
                modifier = Modifier
                    .clickable { onOpenAnimateTab() }
                    .testTag("camera_open_keyframes_chip")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = StudioTeal,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = "${cameraKfCount}KF · Edit",
                        style = StudioTimecodeStyle.copy(fontSize = 8.5.sp),
                        color = StudioTeal
                    )
                }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        CinematicCameraPreset.entries.forEach { preset ->
            val selected = cin.cameraPreset.equals(preset.label, ignoreCase = true)
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (selected) StudioAmberDim else StudioCharcoal,
                border = BorderStroke(1.dp, if (selected) StudioAmber else StudioBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onApplyCinematicCameraPreset(preset) }
                    .testTag("camera_preset_${preset.name.lowercase()}")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = preset.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) StudioAmber else StudioTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = preset.summary,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                            color = StudioTextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (preset.isAnimatedMotion) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = StudioTealDim,
                            border = BorderStroke(1.dp, StudioTeal.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "MOTION",
                                style = StudioTimecodeStyle.copy(fontSize = 7.5.sp, fontWeight = FontWeight.Bold),
                                color = StudioTeal,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // 3. DIRECT CAMERA RIG CONTROLS (POSITION X/Y, ZOOM, ROTATION, SHAKE)
    Spacer(Modifier.height(2.dp))
    InspectorFieldLabel("Camera Rig & Framing")
    if (cameraKfCount > 0) {
        Text(
            text = "Animated camera keyframes are active on this scene. Edit curves in the Animate tab or pick a static framing preset above.",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
            color = StudioTeal
        )
    }

    InspectorSliderRow(
        label = "Camera Zoom",
        valueText = "${String.format("%.2f", scene.cameraZoom)}x",
        value = scene.cameraZoom.toFloat(),
        range = 0.50f..2.50f,
        onValueChange = { v -> onUpdateScene(false) { it.copy(cameraZoom = v.toDouble()) } }
    )
    InspectorSliderRow(
        label = "Camera Pan X",
        valueText = "${cin.cameraPosX.toInt()} px",
        value = cin.cameraPosX.toFloat(),
        range = -240f..240f,
        onValueChange = { v -> onUpdateSceneCinematics(false) { it.copy(cameraPosX = v.toDouble()) } }
    )
    InspectorSliderRow(
        label = "Camera Pan Y",
        valueText = "${cin.cameraPosY.toInt()} px",
        value = cin.cameraPosY.toFloat(),
        range = -160f..160f,
        onValueChange = { v -> onUpdateSceneCinematics(false) { it.copy(cameraPosY = v.toDouble()) } }
    )
    InspectorSliderRow(
        label = "Camera Rotation",
        valueText = "${String.format("%.1f", cin.cameraRotationDeg)}°",
        value = cin.cameraRotationDeg.toFloat(),
        range = -45f..45f,
        onValueChange = { v -> onUpdateSceneCinematics(false) { it.copy(cameraRotationDeg = v.toDouble()) } }
    )
    InspectorSliderRow(
        label = "Handheld Shake",
        valueText = "${(cin.cameraShakeIntensity * 100).toInt()}%",
        value = cin.cameraShakeIntensity.toFloat(),
        range = 0f..1f,
        onValueChange = { v -> onUpdateSceneCinematics(false) { it.copy(cameraShakeIntensity = v.toDouble()) } }
    )

    // 4. LAYER DEPTH & 2D PARALLAX ENGINE
    Spacer(Modifier.height(4.dp))
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DEPTH & PARALLAX ENGINE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = StudioTeal
                    )
                    Text(
                        text = "Multiplane camera parallax, DoF blur (Android 12+) & aerial haze",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                        color = StudioTextMuted
                    )
                }
                Switch(
                    checked = cin.parallaxEnabled,
                    onCheckedChange = { enabled ->
                        onUpdateSceneCinematics(true) { it.copy(parallaxEnabled = enabled) }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = StudioVoid,
                        checkedTrackColor = StudioTeal
                    ),
                    modifier = Modifier.testTag("parallax_enabled_switch")
                )
            }

            if (cin.parallaxEnabled) {
                InspectorSliderRow(
                    label = "Parallax Strength",
                    valueText = "${String.format("%.2f", cin.parallaxStrength)}x",
                    value = cin.parallaxStrength.toFloat(),
                    range = 0f..2f,
                    onValueChange = { v -> onUpdateSceneCinematics(false) { it.copy(parallaxStrength = v.toDouble()) } }
                )
            }

            InspectorSliderRow(
                label = "Focus Plane Depth",
                valueText = "${String.format("%.2f", cin.depthFocusPlane)}x",
                value = cin.depthFocusPlane.toFloat(),
                range = 0.20f..1.30f,
                onValueChange = { v -> onUpdateSceneCinematics(false) { it.copy(depthFocusPlane = v.toDouble()) } }
            )
            InspectorSliderRow(
                label = "Depth Blur (DoF)",
                valueText = "${(cin.depthBlurIntensity * 100).toInt()}%",
                value = cin.depthBlurIntensity.toFloat(),
                range = 0f..1f,
                onValueChange = { v -> onUpdateSceneCinematics(false) { it.copy(depthBlurIntensity = v.toDouble()) } }
            )
            InspectorSliderRow(
                label = "Aerial Depth Haze",
                valueText = "${(cin.depthHazeIntensity * 100).toInt()}%",
                value = cin.depthHazeIntensity.toFloat(),
                range = 0f..1f,
                onValueChange = { v -> onUpdateSceneCinematics(false) { it.copy(depthHazeIntensity = v.toDouble()) } }
            )

            // Per-layer depth quick tuning
            Text(
                text = "PER-LAYER DEPTH FACTORS",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.SemiBold),
                color = StudioTextSecondary
            )
            normalizedLayers.forEach { layerState ->
                val layerType = SceneLayerType.fromKeyOrDefault(layerState.layerKey, SceneLayerType.BACKGROUND)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${layerType.shortCode} ${String.format("%.2f", layerState.parallaxFactor)}x",
                        style = StudioTimecodeStyle.copy(fontSize = 9.sp),
                        color = StudioTextSecondary,
                        modifier = Modifier.width(62.dp)
                    )
                    Slider(
                        value = layerState.parallaxFactor.toFloat().coerceIn(0f, 2f),
                        onValueChange = { v -> onUpdateLayerParallax(layerType, v.toDouble()) },
                        valueRange = 0f..2f,
                        colors = SliderDefaults.colors(
                            thumbColor = StudioTeal,
                            activeTrackColor = StudioTeal,
                            inactiveTrackColor = StudioElevated
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LightingAtmosphereVfxSection(
    scene: StudioScene,
    onApplyLightingPreset: (LightingPreset) -> Unit,
    onUpdateSceneCinematics: (Boolean, (SceneCinematics) -> SceneCinematics) -> Unit,
    onUpdateScene: (Boolean, (StudioScene) -> StudioScene) -> Unit
) {
    val cin = scene.cinematics
    val currentAtmosphere = remember(cin.atmosphereMode) {
        AtmosphereMode.fromLabelOrDefault(cin.atmosphereMode)
    }
    val currentLetterbox = remember(cin.letterboxAspect) {
        LetterboxMode.fromLabelOrDefault(cin.letterboxAspect)
    }

    // 1. CINEMATIC LIGHTING & COLOR GRADING PRESETS (8 PRESETS)
    InspectorFieldLabel("Lighting & Color Grade Presets")
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        LightingPreset.entries.forEach { preset ->
            val selected = cin.lightingPreset.equals(preset.label, ignoreCase = true)
            FilterChip(
                selected = selected,
                onClick = { onApplyLightingPreset(preset) },
                label = { Text(preset.label, style = MaterialTheme.typography.labelSmall) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = StudioAmberDim,
                    selectedLabelColor = StudioAmber
                ),
                modifier = Modifier.testTag("lighting_preset_${preset.name.lowercase()}")
            )
        }
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = StudioCharcoal,
        border = BorderStroke(1.dp, StudioBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(10.dp)
        ) {
            Text(
                text = "COLOR GRADING & DIRECTIONAL LIGHT",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = StudioAmber
            )
            InspectorSliderRow(
                label = "Exposure",
                valueText = String.format("%+.2f", cin.exposure),
                value = cin.exposure.toFloat(),
                range = -0.50f..0.50f,
                onValueChange = { v -> onUpdateSceneCinematics(false) { it.copy(exposure = v.toDouble()) } }
            )
            InspectorSliderRow(
                label = "Contrast",
                valueText = "${(cin.contrast * 100).toInt()}%",
                value = cin.contrast.toFloat(),
                range = 0.60f..1.60f,
                onValueChange = { v -> onUpdateSceneCinematics(false) { it.copy(contrast = v.toDouble()) } }
            )
            InspectorSliderRow(
                label = "Saturation",
                valueText = "${(cin.saturation * 100).toInt()}%",
                value = cin.saturation.toFloat(),
                range = 0f..1.80f,
                onValueChange = { v -> onUpdateSceneCinematics(false) { it.copy(saturation = v.toDouble()) } }
            )
            InspectorSliderRow(
                label = "Temperature (Cool ↔ Warm)",
                valueText = String.format("%+.2f", cin.temperature),
                value = cin.temperature.toFloat(),
                range = -1.0f..1.0f,
                onValueChange = { v -> onUpdateSceneCinematics(false) { it.copy(temperature = v.toDouble()) } }
            )
            InspectorSliderRow(
                label = "Key Light X",
                valueText = String.format("%+.2f", cin.lightSourceX),
                value = cin.lightSourceX.toFloat(),
                range = -1.0f..1.0f,
                onValueChange = { v -> onUpdateSceneCinematics(false) { it.copy(lightSourceX = v.toDouble()) } }
            )
            InspectorSliderRow(
                label = "Key Light Y",
                valueText = String.format("%+.2f", cin.lightSourceY),
                value = cin.lightSourceY.toFloat(),
                range = -1.0f..1.0f,
                onValueChange = { v -> onUpdateSceneCinematics(false) { it.copy(lightSourceY = v.toDouble()) } }
            )
            InspectorSliderRow(
                label = "Key Light Glow",
                valueText = "${(cin.lightGlowIntensity * 100).toInt()}%",
                value = cin.lightGlowIntensity.toFloat(),
                range = 0f..1f,
                onValueChange = { v -> onUpdateSceneCinematics(false) { it.copy(lightGlowIntensity = v.toDouble()) } }
            )
            InspectorSliderRow(
                label = "Shadow Depth",
                valueText = "${(cin.shadowIntensity * 100).toInt()}%",
                value = cin.shadowIntensity.toFloat(),
                range = 0f..1f,
                onValueChange = { v -> onUpdateSceneCinematics(false) { it.copy(shadowIntensity = v.toDouble()) } }
            )
        }
    }

    // 2. ATMOSPHERE & WEATHER EFFECTS (8 MODES)
    Spacer(Modifier.height(2.dp))
    InspectorFieldLabel("Atmosphere & Weather")
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        AtmosphereMode.entries.forEach { mode ->
            val selected = currentAtmosphere == mode
            FilterChip(
                selected = selected,
                onClick = {
                    onUpdateSceneCinematics(true) { current ->
                        current.copy(
                            atmosphereMode = mode.label,
                            atmosphereIntensity = if (mode != AtmosphereMode.NONE && current.atmosphereIntensity < 0.05) {
                                0.50
                            } else {
                                current.atmosphereIntensity
                            }
                        )
                    }
                },
                label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = StudioTealDim,
                    selectedLabelColor = StudioTeal
                ),
                modifier = Modifier.testTag("atmosphere_mode_${mode.name.lowercase()}")
            )
        }
    }

    if (currentAtmosphere != AtmosphereMode.NONE) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = StudioTealDim.copy(alpha = 0.25f),
            border = BorderStroke(1.dp, StudioTeal.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(10.dp)
            ) {
                InspectorSliderRow(
                    label = "Atmosphere Intensity",
                    valueText = "${(cin.atmosphereIntensity * 100).toInt()}%",
                    value = cin.atmosphereIntensity.toFloat(),
                    range = 0f..1f,
                    onValueChange = { v -> onUpdateSceneCinematics(false) { it.copy(atmosphereIntensity = v.toDouble()) } }
                )
                InspectorSliderRow(
                    label = "Particle Speed",
                    valueText = "${String.format("%.1f", cin.atmosphereSpeed)}x",
                    value = cin.atmosphereSpeed.toFloat(),
                    range = 0.2f..2.5f,
                    onValueChange = { v -> onUpdateSceneCinematics(false) { it.copy(atmosphereSpeed = v.toDouble()) } }
                )
                InspectorSliderRow(
                    label = "Wind Direction",
                    valueText = String.format("%+.0f%%", cin.windStrength * 100),
                    value = cin.windStrength.toFloat(),
                    range = -1.0f..1.0f,
                    onValueChange = { v -> onUpdateSceneCinematics(false) { it.copy(windStrength = v.toDouble()) } }
                )
            }
        }
    }

    // 3. POST-PROCESSING VFX & CINEMA LETTERBOX
    Spacer(Modifier.height(2.dp))
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = StudioCharcoal,
        border = BorderStroke(1.dp, StudioBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(10.dp)
        ) {
            Text(
                text = "POST-PROCESSING & CINEMA FRAMING",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = StudioAmber
            )
            InspectorSliderRow(
                label = "Vignette",
                valueText = "${(cin.vignetteIntensity * 100).toInt()}%",
                value = cin.vignetteIntensity.toFloat(),
                range = 0f..1f,
                onValueChange = { v -> onUpdateSceneCinematics(false) { it.copy(vignetteIntensity = v.toDouble()) } }
            )
            InspectorSliderRow(
                label = "Film Grain",
                valueText = "${(cin.filmGrainIntensity * 100).toInt()}%",
                value = cin.filmGrainIntensity.toFloat(),
                range = 0f..1f,
                onValueChange = { v -> onUpdateSceneCinematics(false) { it.copy(filmGrainIntensity = v.toDouble()) } }
            )
            InspectorSliderRow(
                label = "Highlight Bloom",
                valueText = "${(cin.bloomIntensity * 100).toInt()}%",
                value = cin.bloomIntensity.toFloat(),
                range = 0f..1f,
                onValueChange = { v -> onUpdateSceneCinematics(false) { it.copy(bloomIntensity = v.toDouble()) } }
            )

            Text(
                text = "CINEMA LETTERBOX BARS",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.SemiBold),
                color = StudioTextSecondary
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                LetterboxMode.entries.forEach { mode ->
                    FilterChip(
                        selected = currentLetterbox == mode,
                        onClick = {
                            onUpdateSceneCinematics(true) { it.copy(letterboxAspect = mode.label) }
                        },
                        label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StudioAmberDim,
                            selectedLabelColor = StudioAmber
                        ),
                        modifier = Modifier.testTag("letterbox_${mode.name.lowercase()}")
                    )
                }
            }
        }
    }

    // 4. LEGACY COMPATIBILITY EFFECT PRESETS & MOTION GRAPHIC TITLE
    Spacer(Modifier.height(2.dp))
    InspectorFieldLabel("Quick Legacy Effect Overlay")
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        StudioPresets.effectPresets.forEach { fx ->
            FilterChip(
                selected = scene.effectPreset == fx,
                onClick = { onUpdateScene(true) { it.copy(effectPreset = fx) } },
                label = { Text(fx, style = MaterialTheme.typography.labelSmall) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = StudioAmberDim,
                    selectedLabelColor = StudioAmber
                )
            )
        }
    }
    if (scene.effectPreset != "None") {
        InspectorSliderRow(
            label = "Legacy Effect Intensity",
            valueText = "${(scene.effectIntensity * 100).toInt()}%",
            value = scene.effectIntensity.toFloat(),
            range = 0f..1f,
            onValueChange = { v -> onUpdateScene(false) { it.copy(effectIntensity = v.toDouble()) } }
        )
    }

    InspectorFieldLabel("Motion Graphic Title")
    OutlinedTextField(
        value = scene.motionGraphicTitle,
        onValueChange = { v -> onUpdateScene(false) { it.copy(motionGraphicTitle = v.take(120)) } },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodySmall.copy(color = StudioTextPrimary),
        colors = inspectorTextFieldColors(),
        modifier = Modifier.fillMaxWidth()
    )
}
