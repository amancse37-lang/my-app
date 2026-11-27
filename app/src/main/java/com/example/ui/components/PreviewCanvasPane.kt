package com.example.ui.components

import android.net.Uri
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.audio.LipSyncEngine
import com.example.cinematics.AtmosphereMode
import com.example.cinematics.CinematicEngine
import com.example.compositor.CompositeRenderNode
import com.example.compositor.SceneCompositor
import com.example.compositor.SceneLayerType
import com.example.compositor.SelectedStageTarget
import com.example.compositor.StageTransformMath
import com.example.data.CharacterBitmapCodec
import com.example.data.CharacterReferenceSheet
import com.example.data.StudioAsset
import com.example.data.StudioCharacter
import com.example.data.StudioScene
import com.example.data.TimelineTrackType
import com.example.graphics.MotionGraphicsEngine
import com.example.graphics.TransitionFlowEngine
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCharcoal
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioElevated
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioPanel
import com.example.ui.theme.StudioTeal
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.StudioTimecodeStyle
import com.example.ui.theme.StudioViolet
import com.example.ui.theme.StudioVoid
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

fun formatTimecode(seconds: Double, fps: Long = 24L): String {
    val safeSec = seconds.coerceAtLeast(0.0)
    val mins = (safeSec / 60.0).toInt()
    val secs = (safeSec % 60.0).toInt()
    val frames = ((safeSec - safeSec.toInt()) * fps).toInt().coerceIn(0, (fps - 1).toInt())
    return String.format("%02d:%02d:%02d", mins, secs, frames)
}

fun backgroundDrawableForPreset(preset: String): Int? = when (preset) {
    "himalayas" -> R.drawable.img_scene_himalayas_1795597589359
    "old_delhi" -> R.drawable.img_scene_old_delhi_1795597592145
    "archive_study" -> R.drawable.img_scene_archive_study_1795597589726
    else -> null
}

fun parseHexColor(hex: String, fallback: Color = StudioCharcoal): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        fallback
    }
}

fun safeMediaUriOrNull(rawUri: String): Uri? {
    val trimmed = rawUri.trim()
    if (trimmed.isBlank()) return null
    val parsed = try {
        Uri.parse(trimmed)
    } catch (_: Exception) {
        return null
    }
    return if (parsed.scheme.equals("content", ignoreCase = true) ||
        parsed.scheme.equals("https", ignoreCase = true)
    ) {
        parsed
    } else {
        null
    }
}

@Composable
fun PreviewCanvasPane(
    scene: StudioScene?,
    scenes: List<StudioScene> = emptyList(),
    sceneNumber: Int,
    totalScenes: Int,
    sceneProgress: Double,
    sceneTimeSec: Double = 0.0,
    characters: List<StudioCharacter>,
    characterReferencesByCharId: Map<String, List<CharacterReferenceSheet>> = emptyMap(),
    assets: List<StudioAsset>,
    selectedStageTarget: SelectedStageTarget?,
    isPlaying: Boolean,
    playheadSec: Double,
    totalDurationSec: Double,
    fps: Long,
    canvasZoom: Float,
    mutedTracks: Set<TimelineTrackType>,
    showFramingGuides: Boolean = false,
    onSelectStageTarget: (SelectedStageTarget?) -> Unit,
    onBeginTransformGesture: () -> Unit,
    onDragStageTarget: (SelectedStageTarget, Double, Double) -> Unit,
    onScaleStageTarget: (SelectedStageTarget, Double) -> Unit,
    onRotateStageTarget: (SelectedStageTarget, Double) -> Unit,
    onPreviousScene: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onStop: () -> Unit,
    onNextScene: () -> Unit,
    onZoomOut: () -> Unit,
    onZoomReset: () -> Unit,
    onZoomIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(StudioVoid)
            .fillMaxWidth()
    ) {
        // Dark workspace surround containing the 16:9 cinematic compositor canvas
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(StudioVoid)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clipToBounds()
        ) {
            val activeTransition = remember(scenes, playheadSec, mutedTracks) {
                if (scenes.size >= 2 && TimelineTrackType.TRANSITIONS !in mutedTracks) {
                    TransitionFlowEngine.resolveTransitionAtPlayhead(scenes, playheadSec)
                } else {
                    null
                }
            }
            if (activeTransition != null) {
                val transforms = remember(activeTransition.transition, activeTransition.progress) {
                    TransitionFlowEngine.evaluateTransitionTransforms(
                        transition = activeTransition.transition,
                        progress = activeTransition.progress
                    )
                }
                val outDur = activeTransition.outgoingScene.durationSec.coerceAtLeast(0.1)
                val inDur = activeTransition.incomingScene.durationSec.coerceAtLeast(0.1)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                ) {
                    DualSceneTransitionBox(
                        transforms = transforms,
                        outgoingContent = {
                            CinematicStage16x9(
                                scene = activeTransition.outgoingScene,
                                sceneNumber = activeTransition.outgoingSceneIndex + 1,
                                totalScenes = totalScenes,
                                sceneProgress = (activeTransition.outgoingLocalTimeSec / outDur).coerceIn(0.0, 1.0),
                                sceneTimeSec = activeTransition.outgoingLocalTimeSec,
                                characters = characters,
                                characterReferencesByCharId = characterReferencesByCharId,
                                assets = assets,
                                selectedStageTarget = null,
                                interactive = false,
                                isPlaying = isPlaying,
                                canvasZoom = canvasZoom,
                                mutedTracks = mutedTracks,
                                showFramingGuides = false,
                                modifier = Modifier.fillMaxSize()
                            )
                        },
                        incomingContent = {
                            CinematicStage16x9(
                                scene = activeTransition.incomingScene,
                                sceneNumber = activeTransition.incomingSceneIndex + 1,
                                totalScenes = totalScenes,
                                sceneProgress = (activeTransition.incomingLocalTimeSec / inDur).coerceIn(0.0, 1.0),
                                sceneTimeSec = activeTransition.incomingLocalTimeSec,
                                characters = characters,
                                characterReferencesByCharId = characterReferencesByCharId,
                                assets = assets,
                                selectedStageTarget = null,
                                interactive = false,
                                isPlaying = isPlaying,
                                canvasZoom = canvasZoom,
                                mutedTracks = mutedTracks,
                                showFramingGuides = false,
                                modifier = Modifier.fillMaxSize()
                            )
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    Surface(
                        color = StudioVoid.copy(alpha = 0.80f),
                        border = BorderStroke(1.dp, StudioViolet),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = "${activeTransition.transition.type} · ${(activeTransition.progress * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                            color = StudioViolet,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            } else if (scene == null) {
                Text(
                    text = "Select or generate a scene to preview",
                    style = MaterialTheme.typography.bodyMedium,
                    color = StudioTextSecondary
                )
            } else {
                CinematicStage16x9(
                    scene = scene,
                    sceneNumber = sceneNumber,
                    totalScenes = totalScenes,
                    sceneProgress = sceneProgress,
                    sceneTimeSec = sceneTimeSec,
                    characters = characters,
                    characterReferencesByCharId = characterReferencesByCharId,
                    assets = assets,
                    selectedStageTarget = if (isPlaying) null else selectedStageTarget,
                    interactive = !isPlaying,
                    isPlaying = isPlaying,
                    canvasZoom = canvasZoom,
                    mutedTracks = mutedTracks,
                    showFramingGuides = showFramingGuides,
                    onSelectStageTarget = onSelectStageTarget,
                    onBeginTransformGesture = onBeginTransformGesture,
                    onDragStageTarget = onDragStageTarget,
                    onScaleStageTarget = onScaleStageTarget,
                    onRotateStageTarget = onRotateStageTarget,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                )
            }
        }

        // Studio Transport & Zoom Bar
        Surface(
            color = StudioPanel,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StudioBorder)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 10.dp)
            ) {
                // Timecode Display
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(StudioVoid, RoundedCornerShape(6.dp))
                        .border(1.dp, StudioBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isPlaying) StudioCoral else StudioTeal)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${formatTimecode(playheadSec, fps)} / ${formatTimecode(totalDurationSec, fps)}",
                        style = StudioTimecodeStyle,
                        color = StudioTextPrimary,
                        modifier = Modifier.testTag("timecode_display")
                    )
                }

                // Transport Controls: Prev, Play/Pause, Stop, Next
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onPreviousScene,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("prev_scene_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous Scene",
                            tint = StudioTextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = StudioAmber,
                        modifier = Modifier.size(36.dp)
                    ) {
                        IconButton(
                            onClick = onTogglePlayPause,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("play_pause_button")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = StudioVoid,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onStop,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("stop_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop",
                            tint = StudioTextSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    IconButton(
                        onClick = onNextScene,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("next_scene_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Scene",
                            tint = StudioTextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Zoom Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(StudioElevated, RoundedCornerShape(6.dp))
                        .border(1.dp, StudioBorder, RoundedCornerShape(6.dp))
                ) {
                    IconButton(
                        onClick = onZoomOut,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("canvas_zoom_out")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Zoom out canvas",
                            tint = StudioTextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Surface(
                        color = Color.Transparent,
                        onClick = onZoomReset
                    ) {
                        Text(
                            text = "${(canvasZoom * 100).roundToInt()}%",
                            style = StudioTimecodeStyle.copy(fontSize = 11.sp),
                            color = StudioTextPrimary,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                    IconButton(
                        onClick = onZoomIn,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("canvas_zoom_in")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Zoom in canvas",
                            tint = StudioTextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

private data class SelectionOverlaySpec(
    val target: SelectedStageTarget,
    val label: String,
    val accentColor: Color,
    val offsetXPx: Float,
    val offsetYPx: Float,
    val boxWidthDp: Dp,
    val boxHeightDp: Dp,
    val baseSizePx: Float,
    val rotationDeg: Float
)

@Composable
fun CinematicStage16x9(
    scene: StudioScene,
    sceneNumber: Int,
    totalScenes: Int,
    sceneProgress: Double,
    sceneTimeSec: Double = sceneProgress * scene.durationSec.coerceAtLeast(0.1),
    characters: List<StudioCharacter>,
    characterReferencesByCharId: Map<String, List<CharacterReferenceSheet>> = emptyMap(),
    assets: List<StudioAsset> = emptyList(),
    selectedStageTarget: SelectedStageTarget? = null,
    interactive: Boolean = true,
    isPlaying: Boolean,
    canvasZoom: Float,
    mutedTracks: Set<TimelineTrackType>,
    showFramingGuides: Boolean = false,
    onSelectStageTarget: (SelectedStageTarget?) -> Unit = {},
    onBeginTransformGesture: () -> Unit = {},
    onDragStageTarget: (SelectedStageTarget, Double, Double) -> Unit = { _, _, _ -> },
    onScaleStageTarget: (SelectedStageTarget, Double) -> Unit = { _, _ -> },
    onRotateStageTarget: (SelectedStageTarget, Double) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val progressF = sceneProgress.toFloat().coerceIn(0f, 1f)
    val safeSceneTime = sceneTimeSec.coerceIn(0.0, scene.durationSec.coerceAtLeast(0.1))
    val compositionPlan = remember(scene, characters, characterReferencesByCharId, assets, mutedTracks, safeSceneTime) {
        SceneCompositor.buildCompositionPlan(
            scene = scene,
            characters = characters,
            assets = assets,
            mutedTracks = mutedTracks,
            timeSec = safeSceneTime,
            characterReferencesByCharId = characterReferencesByCharId
        )
    }

    // Time-based evaluated Camera (Phase 4 framing + Phase 3 keyframes + deterministic handheld shake)
    val evaluatedCam = compositionPlan.evaluatedCamera
    val camScale = evaluatedCam.zoom.toFloat().coerceIn(0.5f, 2.5f)
    val camRotDeg = evaluatedCam.rotationDeg.toFloat().coerceIn(-45f, 45f)
    val camRotRad = camRotDeg.toDouble() * PI / 180.0
    val cosCamRot = cos(camRotRad).toFloat()
    val sinCamRot = sin(camRotRad).toFloat()

    // Phase 4 Color Grading ColorMatrix filter (GPU-accelerated across all Android API levels)
    val colorGradeFilter = remember(scene.cinematics) {
        if (CinematicEngine.isNeutralColorGrade(scene.cinematics)) {
            null
        } else {
            ColorFilter.colorMatrix(ColorMatrix(CinematicEngine.computeColorGradeMatrix(scene.cinematics)))
        }
    }
    val atmosphereMode = remember(scene.cinematics.atmosphereMode) {
        AtmosphereMode.fromLabelOrDefault(scene.cinematics.atmosphereMode)
    }
    val bgTintColor = remember(scene.backgroundTintHex) {
        parseHexColor(scene.backgroundTintHex, StudioCharcoal)
    }

    // Legacy subtle breathing motion only when playing AND character has no explicit position/rotation keyframes
    val motionPhase = (progressF * 4f * PI).toFloat()
    val charBobY = if (!isPlaying) 0f else when (scene.animationStyle) {
        "Subtle Drift" -> sin(motionPhase) * 4f
        "Expressive Gesture" -> sin(motionPhase * 1.5f) * 7f
        "Walk Cycle" -> abs(sin(motionPhase * 2f)) * -10f
        "Interview Talk" -> sin(motionPhase * 2.2f) * 2.5f
        else -> 0f
    }
    val charTilt = if (!isPlaying) 0f else when (scene.animationStyle) {
        "Expressive Gesture" -> sin(motionPhase) * 3.5f
        "Interview Talk" -> sin(motionPhase * 1.2f) * 1.5f
        else -> 0f
    }

    val currentZoom by rememberUpdatedState(canvasZoom.coerceAtLeast(0.25f))
    val currentOnSelect by rememberUpdatedState(onSelectStageTarget)
    val currentOnBeginGesture by rememberUpdatedState(onBeginTransformGesture)
    val currentOnDrag by rememberUpdatedState(onDragStageTarget)
    val currentOnScale by rememberUpdatedState(onScaleStageTarget)
    val currentOnRotate by rememberUpdatedState(onRotateStageTarget)

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(StudioCharcoal)
            .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
            .graphicsLayer {
                scaleX = canvasZoom
                scaleY = canvasZoom
            }
            .clipToBounds()
            .then(
                if (interactive) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { currentOnSelect(null) }
                        )
                    }
                } else {
                    Modifier
                }
            )
            .testTag("compositor_stage_16x9")
    ) {
        val density = LocalDensity.current
        val stageW = maxWidth
        val stageH = maxHeight
        val stageWPx = with(density) { stageW.toPx() }.coerceAtLeast(1f)
        val stageHPx = with(density) { stageH.toPx() }.coerceAtLeast(1f)
        val currentStageWPx by rememberUpdatedState(stageWPx)
        val currentStageHPx by rememberUpdatedState(stageHPx)

        val baseCharSizeDp = stageH * 0.50f
        val baseCharSizePx = with(density) { baseCharSizeDp.toPx() }.coerceAtLeast(24f)
        val baseElementSizeDp = stageH * 0.36f
        val baseElementSizePx = with(density) { baseElementSizeDp.toPx() }.coerceAtLeast(24f)
        val stageScaleFactor = (stageW.value / 360f).coerceIn(0.6f, 1.75f)

        val evaluatedGraphics = remember(scene.motionGraphics, scene.animationKeyframes, safeSceneTime, mutedTracks) {
            scene.motionGraphics
                .filter { !MotionGraphicsEngine.isGraphicTrackMuted(it, mutedTracks) }
                .map { g -> MotionGraphicsEngine.evaluateGraphicAtTime(g, scene.animationKeyframes, safeSceneTime) }
                .filter { it.visible }
                .sortedBy { it.graphic.zIndex }
        }
        val worldGraphics = evaluatedGraphics.filter { it.graphic.attachToCamera }
        val screenGraphics = evaluatedGraphics.filter { !it.graphic.attachToCamera }

        var activeSelectionSpec: SelectionOverlaySpec? = null
        var characterCounter = 0

        val sceneDepthLayers = compositionPlan.orderedLayers.filter { it.layerType != SceneLayerType.OVERLAY }
        val overlayLayer = compositionPlan.orderedLayers.firstOrNull { it.layerType == SceneLayerType.OVERLAY }

        // PASS 1: Camera-Rotated Multiplane Scene Stack (BG, MG, Characters, Props, FG + Lighting + Atmosphere)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = camRotDeg
                }
        ) {
            sceneDepthLayers.forEach { layerPlan ->
                if (!layerPlan.visible || layerPlan.opacity <= 0.001f) return@forEach

                val depth = layerPlan.depthTransform
                val layerZoom = (depth?.zoomScale ?: evaluatedCam.zoom).toFloat().coerceIn(0.45f, 3.0f)
                val (layerCamPanPxX, layerCamPanPxY) = StageTransformMath.stageToCanvasPx(
                    posX = depth?.panX ?: evaluatedCam.panX,
                    posY = depth?.panY ?: evaluatedCam.panY,
                    canvasWidthPx = stageWPx,
                    canvasHeightPx = stageHPx
                )
                val layerBlurDp = (depth?.blurRadiusDp ?: 0.0).toFloat()
                val layerHazeAlpha = (depth?.depthHazeAlpha ?: 0.0).toFloat()

                val depthModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && layerBlurDp > 0.25f) {
                    Modifier.fillMaxSize().blur(layerBlurDp.dp)
                } else {
                    Modifier.fillMaxSize()
                }

                Box(modifier = depthModifier) {
                    layerPlan.nodes.forEach { node ->
                        when (node) {
                            is CompositeRenderNode.BackgroundBaseNode -> {
                                StageBackgroundLayer(
                                    sceneTitle = scene.title,
                                    backgroundPreset = node.backgroundPreset,
                                    backgroundTintHex = node.backgroundTintHex,
                                    resolvedImageBase64 = node.resolvedImageBase64,
                                    opacity = node.opacity,
                                    camScale = layerZoom,
                                    camPanX = layerCamPanPxX,
                                    camPanY = layerCamPanPxY,
                                    colorFilter = colorGradeFilter
                                )
                            }

                            is CompositeRenderNode.CharacterNode -> {
                                val castIdx = characterCounter++
                                val placement = node.placement
                                val target = SelectedStageTarget.CharacterTarget(node.character.id)
                                val isSelected = interactive && selectedStageTarget == target
                                val charAccent = parseHexColor(node.character.accentColorHex, StudioAmber)

                                val (stagePxX, stagePxY) = StageTransformMath.stageToCanvasPx(
                                    posX = placement.posX,
                                    posY = placement.posY,
                                    canvasWidthPx = stageWPx,
                                    canvasHeightPx = stageHPx
                                )
                                val phaseOffset = castIdx * 0.85f
                                val individualBobY = if (isPlaying && !node.hasVerticalOrRotationKeyframes) {
                                    charBobY * (if (castIdx % 2 == 0) 1f else 0.75f) +
                                        sin(motionPhase + phaseOffset) * 1.5f
                                } else {
                                    0f
                                }
                                val individualTilt = if (isPlaying && !node.hasVerticalOrRotationKeyframes) {
                                    charTilt
                                } else {
                                    0f
                                }
                                val offsetXPx = stagePxX * layerZoom + layerCamPanPxX
                                val offsetYPx = stagePxY * layerZoom + layerCamPanPxY + individualBobY
                                val absScale = (placement.scale.toFloat().coerceIn(
                                    StageTransformMath.MIN_SCALE.toFloat(),
                                    StageTransformMath.MAX_SCALE.toFloat()
                                )) * layerZoom
                                val boxSizeDp = baseCharSizeDp * absScale
                                val effectiveRotation = placement.rotationDeg.toFloat() + individualTilt

                                if (isSelected) {
                                    val rotSelX = offsetXPx * cosCamRot - offsetYPx * sinCamRot
                                    val rotSelY = offsetXPx * sinCamRot + offsetYPx * cosCamRot
                                    activeSelectionSpec = SelectionOverlaySpec(
                                        target = target,
                                        label = "${node.character.charCode} · ${node.character.name} (${node.activeExpression})",
                                        accentColor = charAccent,
                                        offsetXPx = rotSelX,
                                        offsetYPx = rotSelY,
                                        boxWidthDp = boxSizeDp,
                                        boxHeightDp = boxSizeDp,
                                        baseSizePx = baseCharSizePx * absScale,
                                        rotationDeg = effectiveRotation + camRotDeg
                                    )
                                }

                                // Direct borderless image compositing (no portrait card chrome, preserves PNG alpha)
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .offset {
                                            IntOffset(
                                                x = offsetXPx.roundToInt(),
                                                y = offsetYPx.roundToInt()
                                            )
                                        }
                                        .size(boxSizeDp)
                                        .then(
                                            if (interactive) {
                                                Modifier
                                                    .pointerInput(node.nodeId, interactive) {
                                                        detectTapGestures(
                                                            onTap = { currentOnSelect(target) }
                                                        )
                                                    }
                                                    .pointerInput("drag_${node.nodeId}", interactive) {
                                                        detectDragGestures(
                                                            onDragStart = {
                                                                currentOnSelect(target)
                                                                currentOnBeginGesture()
                                                            },
                                                            onDrag = { change, dragAmount ->
                                                                change.consume()
                                                                val (dx, dy) = StageTransformMath.canvasPxDeltaToStageDelta(
                                                                    deltaPxX = dragAmount.x / (currentZoom * layerZoom),
                                                                    deltaPxY = dragAmount.y / (currentZoom * layerZoom),
                                                                    canvasWidthPx = currentStageWPx,
                                                                    canvasHeightPx = currentStageHPx
                                                                )
                                                                currentOnDrag(target, dx, dy)
                                                            }
                                                        )
                                                    }
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .testTag("stage_character_${node.character.charCode.lowercase()}")
                                ) {
                                    val speakingState = remember(scene.dialogueLines, scene.lipSyncCues, safeSceneTime, node.character.id) {
                                        LipSyncEngine.evaluateSpeakingStatesAtTime(scene, safeSceneTime)[node.character.id]
                                    }
                                    CharacterReferenceImage(
                                        base64Image = node.resolvedImageBase64,
                                        contentDescription = "${node.character.charCode} ${node.character.name} ${node.activeExpression} ${node.activeAction}",
                                        contentScale = ContentScale.Fit,
                                        colorFilter = colorGradeFilter,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .then(
                                                if (speakingState?.speaking == true) {
                                                    Modifier.border(
                                                        width = 1.5.dp,
                                                        color = StudioViolet.copy(alpha = 0.72f),
                                                        shape = RoundedCornerShape(6.dp)
                                                    )
                                                } else {
                                                    Modifier
                                                }
                                            )
                                            .graphicsLayer {
                                                scaleX = if (placement.flipX) -1f else 1f
                                                rotationZ = effectiveRotation
                                                alpha = node.opacity
                                            }
                                    )
                                    // Honest non-destructive speaking state + viseme cue indicator (no fake PNG mouth warping)
                                    if (speakingState?.speaking == true) {
                                        Surface(
                                            color = StudioVoid.copy(alpha = 0.84f),
                                            border = BorderStroke(1.dp, StudioViolet),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(bottom = 2.dp)
                                        ) {
                                            Text(
                                                text = "SPEAKING · ${speakingState.activeViseme.code}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp),
                                                color = StudioEmerald,
                                                maxLines = 1,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            is CompositeRenderNode.VisualElementNode -> {
                                val el = node.element
                                val target = SelectedStageTarget.ElementTarget(el.elementId)
                                val isSelected = interactive && selectedStageTarget == target
                                val (stagePxX, stagePxY) = StageTransformMath.stageToCanvasPx(
                                    posX = el.posX,
                                    posY = el.posY,
                                    canvasWidthPx = stageWPx,
                                    canvasHeightPx = stageHPx
                                )
                                val isFullBackdropPlane = el.layer.equals(SceneLayerType.BACKGROUND.name, ignoreCase = true)
                                val absScale = (el.scale.toFloat().coerceIn(
                                    StageTransformMath.MIN_SCALE.toFloat(),
                                    StageTransformMath.MAX_SCALE.toFloat()
                                )) * layerZoom
                                val boxWidthDp = if (isFullBackdropPlane) stageW * absScale else baseElementSizeDp * absScale
                                val boxHeightDp = if (isFullBackdropPlane) stageH * absScale else baseElementSizeDp * absScale
                                val effectiveRotation = el.rotationDeg.toFloat()
                                val finalElX = stagePxX * layerZoom + layerCamPanPxX
                                val finalElY = stagePxY * layerZoom + layerCamPanPxY

                                if (isSelected) {
                                    val rotSelX = finalElX * cosCamRot - finalElY * sinCamRot
                                    val rotSelY = finalElX * sinCamRot + finalElY * cosCamRot
                                    activeSelectionSpec = SelectionOverlaySpec(
                                        target = target,
                                        label = "${node.layer.shortCode} · ${el.name}",
                                        accentColor = StudioTeal,
                                        offsetXPx = rotSelX,
                                        offsetYPx = rotSelY,
                                        boxWidthDp = boxWidthDp,
                                        boxHeightDp = boxHeightDp,
                                        baseSizePx = (if (isFullBackdropPlane) stageHPx else baseElementSizePx) * absScale,
                                        rotationDeg = effectiveRotation + camRotDeg
                                    )
                                }

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .offset {
                                            IntOffset(
                                                x = finalElX.roundToInt(),
                                                y = finalElY.roundToInt()
                                            )
                                        }
                                        .size(width = boxWidthDp, height = boxHeightDp)
                                        .then(
                                            if (interactive) {
                                                Modifier
                                                    .pointerInput(node.nodeId, interactive) {
                                                        detectTapGestures(
                                                            onTap = { currentOnSelect(target) }
                                                        )
                                                    }
                                                    .pointerInput("drag_${node.nodeId}", interactive) {
                                                        detectDragGestures(
                                                            onDragStart = {
                                                                currentOnSelect(target)
                                                                currentOnBeginGesture()
                                                            },
                                                            onDrag = { change, dragAmount ->
                                                                change.consume()
                                                                val (dx, dy) = StageTransformMath.canvasPxDeltaToStageDelta(
                                                                    deltaPxX = dragAmount.x / (currentZoom * layerZoom),
                                                                    deltaPxY = dragAmount.y / (currentZoom * layerZoom),
                                                                    canvasWidthPx = currentStageWPx,
                                                                    canvasHeightPx = currentStageHPx
                                                                )
                                                                currentOnDrag(target, dx, dy)
                                                            }
                                                        )
                                                    }
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .testTag("stage_element_${el.elementId}")
                                ) {
                                    val visualModifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            scaleX = if (el.flipX) -1f else 1f
                                            rotationZ = effectiveRotation
                                            alpha = node.opacity
                                        }
                                    val scaleMode = if (isFullBackdropPlane) ContentScale.Crop else ContentScale.Fit
                                    val decodedBitmap = remember(node.resolvedImageBase64) {
                                        if (node.resolvedImageBase64.isNotBlank()) {
                                            CharacterBitmapCodec.decodeBase64ToBitmap(node.resolvedImageBase64)
                                        } else {
                                            null
                                        }
                                    }
                                    if (decodedBitmap != null) {
                                        Image(
                                            bitmap = decodedBitmap.asImageBitmap(),
                                            contentDescription = el.name,
                                            contentScale = scaleMode,
                                            colorFilter = colorGradeFilter,
                                            modifier = visualModifier
                                        )
                                    } else {
                                        val safeUri = safeMediaUriOrNull(node.resolvedUri)
                                        if (safeUri != null) {
                                            AsyncImage(
                                                model = safeUri,
                                                contentDescription = el.name,
                                                contentScale = scaleMode,
                                                colorFilter = colorGradeFilter,
                                                modifier = visualModifier
                                            )
                                        } else {
                                            val drawableRes = backgroundDrawableForPreset(node.resolvedPresetKey)
                                            if (drawableRes != null) {
                                                Image(
                                                    painter = painterResource(id = drawableRes),
                                                    contentDescription = el.name,
                                                    contentScale = scaleMode,
                                                    colorFilter = colorGradeFilter,
                                                    modifier = visualModifier
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            is CompositeRenderNode.OverlayStackNode -> Unit
                        }
                    }

                    // Aerial depth haze separation for layers further back than the camera focus plane
                    if (layerHazeAlpha > 0.005f) {
                        StageDepthHazeVeil(
                            alpha = layerHazeAlpha,
                            tintColor = bgTintColor
                        )
                    }
                }
            }

            // World-Space Phase 6 Motion Graphics (attached to scene camera pan/zoom/rotation)
            if (worldGraphics.isNotEmpty()) {
                val (camPanPxX, camPanPxY) = StageTransformMath.stageToCanvasPx(
                    posX = evaluatedCam.panX,
                    posY = evaluatedCam.panY,
                    canvasWidthPx = stageWPx,
                    canvasHeightPx = stageHPx
                )
                worldGraphics.forEach { eval ->
                    val g = eval.graphic
                    val target = SelectedStageTarget.GraphicTarget(g.graphicId)
                    val isSelected = interactive && selectedStageTarget == target
                    val (gxPx, gyPx) = StageTransformMath.stageToCanvasPx(
                        posX = eval.posX,
                        posY = eval.posY,
                        canvasWidthPx = stageWPx,
                        canvasHeightPx = stageHPx
                    )
                    val offX = gxPx * camScale + camPanPxX
                    val offY = gyPx * camScale + camPanPxY
                    val boxW = stageW * g.widthFraction.toFloat().coerceIn(0.22f, 0.92f) * camScale
                    val boxH = stageH * g.heightFraction.toFloat().coerceIn(0.14f, 0.88f) * camScale
                    if (isSelected) {
                        val rotSelX = offX * cosCamRot - offY * sinCamRot
                        val rotSelY = offX * sinCamRot + offY * cosCamRot
                        activeSelectionSpec = SelectionOverlaySpec(
                            target = target,
                            label = "GFX · ${g.title.take(18)}",
                            accentColor = StudioViolet,
                            offsetXPx = rotSelX,
                            offsetYPx = rotSelY,
                            boxWidthDp = boxW * eval.scaleY.toFloat().coerceIn(0.3f, 2.5f),
                            boxHeightDp = boxH * eval.scaleY.toFloat().coerceIn(0.3f, 2.5f),
                            baseSizePx = with(density) { boxW.toPx() }.coerceAtLeast(32f),
                            rotationDeg = eval.rotationDeg.toFloat() + camRotDeg
                        )
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset { IntOffset(offX.roundToInt(), offY.roundToInt()) }
                            .size(width = boxW, height = boxH)
                            .then(
                                if (interactive) {
                                    Modifier
                                        .pointerInput(g.graphicId, interactive) {
                                            detectTapGestures(onTap = { currentOnSelect(target) })
                                        }
                                        .pointerInput("drag_gfx_${g.graphicId}", interactive) {
                                            detectDragGestures(
                                                onDragStart = {
                                                    currentOnSelect(target)
                                                    currentOnBeginGesture()
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    val (dx, dy) = StageTransformMath.canvasPxDeltaToStageDelta(
                                                        deltaPxX = dragAmount.x / (currentZoom * camScale),
                                                        deltaPxY = dragAmount.y / (currentZoom * camScale),
                                                        canvasWidthPx = currentStageWPx,
                                                        canvasHeightPx = currentStageHPx
                                                    )
                                                    currentOnDrag(target, dx, dy)
                                                }
                                            )
                                        }
                                } else Modifier
                            )
                            .testTag("stage_graphic_${g.graphicId}")
                    ) {
                        StageMotionGraphicVisual(
                            evaluated = eval,
                            stageScaleFactor = stageScaleFactor * camScale,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Directional Key Light & Soft Shadow Falloff
            StageDirectionalLightingOverlay(cinematics = scene.cinematics)

            // Deterministic Environmental Atmosphere & Weather Layer
            StageAtmosphereLayer(
                mode = atmosphereMode,
                timeSec = safeSceneTime,
                intensity = scene.cinematics.atmosphereIntensity,
                speed = scene.cinematics.atmosphereSpeed,
                windStrength = scene.cinematics.windStrength
            )
        }

        // PASS 2: Level Overlay Stack (Post-Processing VFX, Letterbox Bars, Phase 6 Screen Motion Graphics, Subtitles)
        if (overlayLayer != null && overlayLayer.visible && overlayLayer.opacity > 0.001f) {
            StageOverlayStackLayer(
                scene = scene,
                sceneTimeSec = safeSceneTime,
                mutedTracks = mutedTracks,
                opacity = overlayLayer.opacity,
                stageScaleFactor = stageScaleFactor
            )

            // Screen-Overlay Phase 6 Motion Graphics (pinned level above camera & VFX, directly selectable & draggable)
            screenGraphics.forEach { eval ->
                val g = eval.graphic
                val target = SelectedStageTarget.GraphicTarget(g.graphicId)
                val isSelected = interactive && selectedStageTarget == target
                val (gxPx, gyPx) = StageTransformMath.stageToCanvasPx(
                    posX = eval.posX,
                    posY = eval.posY,
                    canvasWidthPx = stageWPx,
                    canvasHeightPx = stageHPx
                )
                val boxW = stageW * g.widthFraction.toFloat().coerceIn(0.22f, 0.92f)
                val boxH = stageH * g.heightFraction.toFloat().coerceIn(0.14f, 0.88f)
                if (isSelected) {
                    activeSelectionSpec = SelectionOverlaySpec(
                        target = target,
                        label = "GFX · ${g.title.take(18)}",
                        accentColor = StudioViolet,
                        offsetXPx = gxPx,
                        offsetYPx = gyPx,
                        boxWidthDp = boxW * eval.scaleY.toFloat().coerceIn(0.3f, 2.5f),
                        boxHeightDp = boxH * eval.scaleY.toFloat().coerceIn(0.3f, 2.5f),
                        baseSizePx = with(density) { boxW.toPx() }.coerceAtLeast(32f),
                        rotationDeg = eval.rotationDeg.toFloat()
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset { IntOffset(gxPx.roundToInt(), gyPx.roundToInt()) }
                        .size(width = boxW, height = boxH)
                        .graphicsLayer { alpha = overlayLayer.opacity }
                        .then(
                            if (interactive) {
                                Modifier
                                    .pointerInput(g.graphicId, interactive) {
                                        detectTapGestures(onTap = { currentOnSelect(target) })
                                    }
                                    .pointerInput("drag_screen_gfx_${g.graphicId}", interactive) {
                                        detectDragGestures(
                                            onDragStart = {
                                                currentOnSelect(target)
                                                currentOnBeginGesture()
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                val (dx, dy) = StageTransformMath.canvasPxDeltaToStageDelta(
                                                    deltaPxX = dragAmount.x / currentZoom,
                                                    deltaPxY = dragAmount.y / currentZoom,
                                                    canvasWidthPx = currentStageWPx,
                                                    canvasHeightPx = currentStageHPx
                                                )
                                                currentOnDrag(target, dx, dy)
                                            }
                                        )
                                    }
                            } else Modifier
                        )
                        .testTag("stage_graphic_${g.graphicId}")
                ) {
                    StageMotionGraphicVisual(
                        evaluated = eval,
                        stageScaleFactor = stageScaleFactor,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // PASS 3: Optional Studio Rule-of-Thirds & Title-Safe Framing Guides
        if (interactive && showFramingGuides) {
            StageFramingGuidesOverlay()
        }

        // PASS 4: Interactive Selection Bounding Box & Direct Transform Handles (always accessible on top)
        val selection = activeSelectionSpec
        if (interactive && selection != null) {
            StageSelectionHandlesOverlay(
                spec = selection,
                currentZoom = currentZoom,
                currentStageWPx = currentStageWPx,
                currentStageHPx = currentStageHPx,
                onBeginGesture = { currentOnBeginGesture() },
                onDrag = { dx, dy -> currentOnDrag(selection.target, dx, dy) },
                onScale = { factor -> currentOnScale(selection.target, factor) },
                onRotate = { deltaDeg -> currentOnRotate(selection.target, deltaDeg) },
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Subtle HUD Scene Counter & Active Layer / Keyframe / Camera Summary (bottom right)
        Surface(
            color = StudioVoid.copy(alpha = 0.65f),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(6.dp)
        ) {
            val kfCount = scene.animationKeyframes.size
            val cueCount = scene.characterActingCues.size
            val hudText = buildString {
                append("SC ${sceneNumber}/$totalScenes · ${String.format("%.1f", safeSceneTime)}s")
                append(" · ${String.format("%.2f", camScale)}x")
                if (abs(camRotDeg) > 0.4f) append(" ${camRotDeg.roundToInt()}°")
                append(" · ${compositionPlan.visibleCharacterCount}CH · ${compositionPlan.visibleVisualElementCount}EL")
                if (kfCount > 0) append(" · ${kfCount}KF")
                if (cueCount > 0) append(" · ${cueCount}ACT")
                if (atmosphereMode != AtmosphereMode.NONE) append(" · ${atmosphereMode.label}")
            }
            Text(
                text = hudText,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                color = StudioTextSecondary,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun StageSelectionHandlesOverlay(
    spec: SelectionOverlaySpec,
    currentZoom: Float,
    currentStageWPx: Float,
    currentStageHPx: Float,
    onBeginGesture: () -> Unit,
    onDrag: (Double, Double) -> Unit,
    onScale: (Double) -> Unit,
    onRotate: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val zoomState by rememberUpdatedState(currentZoom)
    val stageWState by rememberUpdatedState(currentStageWPx)
    val stageHState by rememberUpdatedState(currentStageHPx)
    val baseSizeState by rememberUpdatedState(spec.baseSizePx.coerceAtLeast(32f))
    val onBeginState by rememberUpdatedState(onBeginGesture)
    val onDragState by rememberUpdatedState(onDrag)
    val onScaleState by rememberUpdatedState(onScale)
    val onRotateState by rememberUpdatedState(onRotate)

    // Extra padding gives room for top rotate handle and bottom-right scale handle without clipping
    val handleAllowance = 22.dp
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .offset {
                IntOffset(
                    x = spec.offsetXPx.roundToInt(),
                    y = spec.offsetYPx.roundToInt()
                )
            }
            .size(
                width = spec.boxWidthDp + handleAllowance * 2,
                height = spec.boxHeightDp + handleAllowance * 2
            )
    ) {
        // Selection bounding frame + body drag (works even when element sits behind a foreground layer)
        Box(
            modifier = Modifier
                .size(width = spec.boxWidthDp, height = spec.boxHeightDp)
                .graphicsLayer { rotationZ = spec.rotationDeg }
                .border(1.5.dp, spec.accentColor, RoundedCornerShape(4.dp))
                .pointerInput(spec.target, "selected_body_drag") {
                    detectDragGestures(
                        onDragStart = { onBeginState() },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val (dx, dy) = StageTransformMath.canvasPxDeltaToStageDelta(
                                deltaPxX = dragAmount.x / zoomState,
                                deltaPxY = dragAmount.y / zoomState,
                                canvasWidthPx = stageWState,
                                canvasHeightPx = stageHState
                            )
                            onDragState(dx, dy)
                        }
                    )
                }
        )

        // Floating identifier pill (top-left of frame)
        Surface(
            color = StudioVoid.copy(alpha = 0.85f),
            border = BorderStroke(1.dp, spec.accentColor.copy(alpha = 0.85f)),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = handleAllowance, bottom = 2.dp)
        ) {
            Text(
                text = spec.label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.SemiBold),
                color = spec.accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
            )
        }

        // Top-Center Rotate Handle
        Surface(
            shape = CircleShape,
            color = StudioVoid.copy(alpha = 0.92f),
            border = BorderStroke(1.5.dp, spec.accentColor),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(26.dp)
                .pointerInput(spec.target, "rotate_handle") {
                    detectDragGestures(
                        onDragStart = { onBeginState() },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val deltaDeg = (dragAmount.x / zoomState) * 0.7
                            onRotateState(deltaDeg)
                        }
                    )
                }
                .testTag("stage_rotate_handle")
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Default.RotateRight,
                    contentDescription = "Drag horizontally to rotate selected element",
                    tint = spec.accentColor,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Bottom-Right Resize Handle
        Surface(
            shape = CircleShape,
            color = spec.accentColor,
            border = BorderStroke(1.5.dp, StudioVoid),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 6.dp, bottom = 6.dp)
                .size(26.dp)
                .pointerInput(spec.target, "resize_handle") {
                    detectDragGestures(
                        onDragStart = { onBeginState() },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val radialDelta = ((dragAmount.x + dragAmount.y) * 0.5f) / zoomState
                            val factor = (1.0 + (radialDelta / baseSizeState)).coerceIn(0.85, 1.15)
                            onScaleState(factor)
                        }
                    )
                }
                .testTag("stage_resize_handle")
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Default.OpenInFull,
                    contentDescription = "Drag to resize selected element",
                    tint = StudioVoid,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

@Composable
private fun StageBackgroundLayer(
    sceneTitle: String,
    backgroundPreset: String,
    backgroundTintHex: String,
    resolvedImageBase64: String,
    opacity: Float,
    camScale: Float,
    camPanX: Float,
    camPanY: Float = 0f,
    colorFilter: ColorFilter? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = camScale
                scaleY = camScale
                translationX = camPanX
                translationY = camPanY
                alpha = opacity
            }
    ) {
        val bgTint = parseHexColor(backgroundTintHex, StudioCharcoal)
        val decodedAiBg = remember(resolvedImageBase64) {
            if (resolvedImageBase64.isNotBlank()) {
                CharacterBitmapCodec.decodeBase64ToBitmap(resolvedImageBase64)
            } else {
                null
            }
        }
        val customUri = if (backgroundPreset.startsWith("uri:")) {
            safeMediaUriOrNull(backgroundPreset.removePrefix("uri:"))
        } else {
            null
        }
        if (decodedAiBg != null) {
            Image(
                bitmap = decodedAiBg.asImageBitmap(),
                contentDescription = sceneTitle,
                contentScale = ContentScale.Crop,
                colorFilter = colorFilter,
                modifier = Modifier.fillMaxSize()
            )
        } else if (customUri != null) {
            AsyncImage(
                model = customUri,
                contentDescription = sceneTitle,
                contentScale = ContentScale.Crop,
                colorFilter = colorFilter,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val drawableRes = backgroundDrawableForPreset(backgroundPreset)
            if (drawableRes != null) {
                Image(
                    painter = painterResource(id = drawableRes),
                    contentDescription = sceneTitle,
                    contentScale = ContentScale.Crop,
                    colorFilter = colorFilter,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(bgTint, StudioVoid)
                            )
                        )
                )
            }
        }
        // Atmospheric color grading veil
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgTint.copy(alpha = 0.24f))
        )
    }
}

@Composable
private fun StageOverlayStackLayer(
    scene: StudioScene,
    sceneTimeSec: Double,
    mutedTracks: Set<TimelineTrackType>,
    opacity: Float,
    stageScaleFactor: Float = 1f
) {
    val resolvedVfx = remember(scene.cinematics, scene.effectPreset, scene.effectIntensity) {
        CinematicEngine.resolveEffectiveVfx(scene)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = opacity }
    ) {
        // PHASE 4 POST-PROCESSING VFX + LEGACY COMPATIBILITY + CINEMA LETTERBOX
        StagePostProcessingVfxOverlay(
            vfx = resolvedVfx,
            timeSec = sceneTimeSec
        )

        // LEGACY MOTION GRAPHICS OVERLAY (preserved when no structured Phase 6 motionGraphics exist)
        if (
            scene.motionGraphics.isEmpty() &&
            scene.motionGraphicStyle != "None" &&
            scene.motionGraphicTitle.isNotBlank() &&
            TimelineTrackType.MOTION_GRAPHICS !in mutedTracks
        ) {
            when (scene.motionGraphicStyle) {
                "Chapter Card" -> {
                    Surface(
                        color = StudioVoid.copy(alpha = 0.76f),
                        border = BorderStroke(1.dp, StudioAmber.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(14.dp)
                                    .background(StudioAmber)
                            )
                            Spacer(Modifier.width(7.dp))
                            Text(
                                text = scene.motionGraphicTitle,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = StudioTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                "Date Stamp" -> {
                    Surface(
                        color = StudioVoid.copy(alpha = 0.80f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = scene.motionGraphicTitle.uppercase(),
                            style = StudioTimecodeStyle.copy(fontSize = 10.sp),
                            color = StudioTeal,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                else -> {
                    Surface(
                        color = StudioVoid.copy(alpha = 0.82f),
                        border = BorderStroke(1.dp, StudioTeal.copy(alpha = 0.55f)),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 10.dp, bottom = 34.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(StudioTeal)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = scene.motionGraphicTitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = StudioTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // PHASE 6 STYLED SUBTITLE & CAPTION OVERLAY (with Phase 5 dialogue & legacy fallback)
        StageStyledSubtitleOverlay(
            scene = scene,
            sceneTimeSec = sceneTimeSec,
            mutedTracks = mutedTracks,
            stageScaleFactor = stageScaleFactor
        )
    }
}
