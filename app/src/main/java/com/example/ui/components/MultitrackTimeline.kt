package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Path
import com.example.animation.AnimationTargetType
import com.example.audio.AudioTimelineEngine
import com.example.data.SceneAudioClip
import com.example.data.StudioAudioAsset
import com.example.data.StudioCharacter
import com.example.data.StudioScene
import com.example.data.TimelineTrackType
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioTeal
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioBorderSubtle
import com.example.ui.theme.StudioCharcoal
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioElevated
import com.example.ui.theme.StudioPanel
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.StudioTimecodeStyle
import com.example.ui.theme.StudioViolet
import com.example.ui.theme.StudioVoid
import com.example.ui.theme.TrackAmbientColor
import com.example.ui.theme.TrackBackgroundColor
import com.example.ui.theme.TrackCameraColor
import com.example.ui.theme.TrackCharacterColor
import com.example.ui.theme.TrackDialogueColor
import com.example.ui.theme.TrackMotionGfxColor
import com.example.ui.theme.TrackMusicColor
import com.example.ui.theme.TrackNarrationColor
import com.example.ui.theme.TrackObjectColor
import com.example.graphics.MotionGraphicsEngine
import com.example.graphics.TransitionFlowEngine
import com.example.ui.theme.TrackInfographicsColor
import com.example.ui.theme.TrackLowerThirdsColor
import com.example.ui.theme.TrackMapsColor
import com.example.ui.theme.TrackSfxColor
import com.example.ui.theme.TrackSubtitleColor
import com.example.ui.theme.TrackTitlesTextColor
import com.example.ui.theme.TrackTransitionsColor
import kotlin.math.ceil
import kotlin.math.roundToInt

private fun colorForTrack(track: TimelineTrackType): Color = when (track) {
    TimelineTrackType.BACKGROUND -> TrackBackgroundColor
    TimelineTrackType.CHARACTER -> TrackCharacterColor
    TimelineTrackType.OBJECT -> TrackObjectColor
    TimelineTrackType.CAMERA -> TrackCameraColor
    TimelineTrackType.MOTION_GRAPHICS -> TrackMotionGfxColor
    TimelineTrackType.TITLES_TEXT -> TrackTitlesTextColor
    TimelineTrackType.LOWER_THIRDS -> TrackLowerThirdsColor
    TimelineTrackType.INFOGRAPHICS -> TrackInfographicsColor
    TimelineTrackType.MAPS -> TrackMapsColor
    TimelineTrackType.TRANSITIONS -> TrackTransitionsColor
    TimelineTrackType.NARRATION -> TrackNarrationColor
    TimelineTrackType.DIALOGUE -> TrackDialogueColor
    TimelineTrackType.MUSIC -> TrackMusicColor
    TimelineTrackType.SOUND_EFFECTS -> TrackSfxColor
    TimelineTrackType.AMBIENT -> TrackAmbientColor
    TimelineTrackType.SUBTITLE -> TrackSubtitleColor
}

private fun clipLabelForTrack(
    track: TimelineTrackType,
    scene: StudioScene,
    characters: List<StudioCharacter>
): String? = when (track) {
    TimelineTrackType.BACKGROUND -> scene.backgroundPreset.removePrefix("uri:").takeLast(24)
    TimelineTrackType.CHARACTER -> {
        val cast = scene.effectiveCast()
        if (cast.isEmpty()) {
            null
        } else {
            val charMap = characters.associateBy { it.id }
            if (cast.size == 1) {
                val p = cast.first()
                val c = charMap[p.characterId]
                c?.let { "${it.charCode} ${it.name} (${p.action})" }
            } else {
                val codes = cast.mapNotNull { charMap[it.characterId]?.charCode }
                if (codes.isNotEmpty()) "${codes.joinToString(", ")} (${cast.size} cast)" else "${cast.size} cast"
            }
        }
    }
    TimelineTrackType.OBJECT -> {
        val visibleEls = scene.sceneElements.filter { it.visible }
        when {
            visibleEls.size == 1 -> visibleEls.first().name.ifBlank { "Visual Element" }
            visibleEls.size > 1 -> "${visibleEls.first().name.take(14)} +${visibleEls.size - 1} els"
            scene.objectProp.isNotBlank() -> scene.objectProp
            else -> null
        }
    }
    TimelineTrackType.CAMERA -> {
        val camKfCount = scene.animationKeyframes.count {
            it.targetType.equals(AnimationTargetType.CAMERA.name, ignoreCase = true)
        }
        if (camKfCount > 0) {
            "Keyframed Camera ($camKfCount KF)"
        } else {
            "${scene.cameraMovement} (${String.format("%.2f", scene.cameraZoom)}x)"
        }
    }
    TimelineTrackType.MOTION_GRAPHICS -> scene.motionGraphicTitle.takeIf { it.isNotBlank() && scene.motionGraphicStyle != "None" }
    TimelineTrackType.TITLES_TEXT -> null
    TimelineTrackType.LOWER_THIRDS -> null
    TimelineTrackType.INFOGRAPHICS -> null
    TimelineTrackType.MAPS -> null
    TimelineTrackType.TRANSITIONS -> null
    TimelineTrackType.NARRATION -> scene.narrationText.takeIf { it.isNotBlank() }
    TimelineTrackType.DIALOGUE -> scene.dialogueLines.takeIf { it.isNotEmpty() }?.let { lines ->
        if (lines.size == 1) "${lines.first().speakerName}: ${lines.first().text}" else "${lines.size} lines · ${lines.first().speakerName}"
    }
    TimelineTrackType.MUSIC -> scene.musicCue.takeIf { it.isNotBlank() && it != "Silent / Ambience Only" }
    TimelineTrackType.SOUND_EFFECTS -> scene.sfxCue.takeIf { it.isNotBlank() }
    TimelineTrackType.AMBIENT -> scene.audioClips.firstOrNull { it.trackType == "AMBIENT" }?.title
    TimelineTrackType.SUBTITLE -> scene.subtitleText.takeIf { it.isNotBlank() && scene.subtitleCues.isEmpty() }
}

private fun isAudioTrack(track: TimelineTrackType): Boolean = track in setOf(
    TimelineTrackType.NARRATION,
    TimelineTrackType.DIALOGUE,
    TimelineTrackType.MUSIC,
    TimelineTrackType.SOUND_EFFECTS,
    TimelineTrackType.AMBIENT
)

private fun isGraphicTrack(track: TimelineTrackType): Boolean = track in setOf(
    TimelineTrackType.TITLES_TEXT,
    TimelineTrackType.LOWER_THIRDS,
    TimelineTrackType.INFOGRAPHICS,
    TimelineTrackType.MAPS
)

private fun keyframeTimesForTrack(
    track: TimelineTrackType,
    scene: StudioScene
): Pair<List<Double>, List<Double>> {
    val kfTimes = when (track) {
        TimelineTrackType.CHARACTER -> scene.animationKeyframes
            .filter { it.targetType.equals(AnimationTargetType.CHARACTER.name, ignoreCase = true) }
            .map { it.timeSec }
            .distinct()
        TimelineTrackType.OBJECT -> scene.animationKeyframes
            .filter { it.targetType.equals(AnimationTargetType.ELEMENT.name, ignoreCase = true) }
            .map { it.timeSec }
            .distinct()
        TimelineTrackType.CAMERA -> scene.animationKeyframes
            .filter { it.targetType.equals(AnimationTargetType.CAMERA.name, ignoreCase = true) }
            .map { it.timeSec }
            .distinct()
        TimelineTrackType.MOTION_GRAPHICS,
        TimelineTrackType.TITLES_TEXT,
        TimelineTrackType.LOWER_THIRDS,
        TimelineTrackType.INFOGRAPHICS,
        TimelineTrackType.MAPS -> scene.animationKeyframes
            .filter { it.targetType.equals(AnimationTargetType.GRAPHIC.name, ignoreCase = true) }
            .map { it.timeSec }
            .distinct()
        else -> emptyList()
    }
    val cueTimes = if (track == TimelineTrackType.CHARACTER) {
        scene.characterActingCues.map { it.timeSec }.distinct()
    } else {
        emptyList()
    }
    return kfTimes to cueTimes
}

@Composable
fun MultitrackTimeline(
    scenes: List<StudioScene>,
    characters: List<StudioCharacter>,
    selectedSceneId: String?,
    playheadSec: Double,
    totalDurationSec: Double,
    zoomDpPerSec: Float,
    mutedTracks: Set<TimelineTrackType>,
    onSelectScene: (String) -> Unit,
    onSeek: (Double) -> Unit,
    onToggleMute: (TimelineTrackType) -> Unit,
    onZoomOut: () -> Unit,
    onZoomIn: () -> Unit,
    projectAudioClips: List<SceneAudioClip> = emptyList(),
    audioAssets: List<StudioAudioAsset> = emptyList(),
    selectedAudioClipId: String? = null,
    onSelectAudioClip: (String?) -> Unit = {},
    selectedGraphicId: String? = null,
    onSelectGraphic: (String, String) -> Unit = { _, _ -> },
    onSelectTransitionScene: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val assetById = remember(audioAssets) { audioAssets.associateBy { it.id } }
    val horizontalScroll = rememberScrollState()
    val verticalScroll = rememberScrollState()
    val headerWidth = 118.dp
    val rowHeight = 28.dp
    val rulerHeight = 24.dp
    val density = LocalDensity.current

    val timelineSeconds = ceil(totalDurationSec + 4.0).toInt().coerceAtLeast(16)
    val contentWidthDp = (timelineSeconds * zoomDpPerSec).dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioCharcoal)
            .border(1.dp, StudioBorder)
    ) {
        // Timeline Toolbar Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .background(StudioPanel)
                .padding(horizontal = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "MULTITRACK TIMELINE",
                    style = MaterialTheme.typography.labelSmall,
                    color = StudioAmber,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${scenes.size} scenes · ${TimelineTrackType.entries.size} tracks · ${String.format("%.1f", totalDurationSec)}s",
                    style = MaterialTheme.typography.labelSmall,
                    color = StudioTextSecondary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Zoom",
                    style = MaterialTheme.typography.labelSmall,
                    color = StudioTextMuted
                )
                IconButton(
                    onClick = onZoomOut,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("timeline_zoom_out")
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Zoom out timeline",
                        tint = StudioTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
                IconButton(
                    onClick = onZoomIn,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("timeline_zoom_in")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom in timeline",
                        tint = StudioTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Ruler Row (Fixed corner + Scrollable time ruler)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(rulerHeight)
                .background(StudioVoid)
                .border(1.dp, StudioBorderSubtle)
        ) {
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .width(headerWidth)
                    .fillMaxHeight()
                    .background(StudioPanel)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = "TRACKS",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = StudioTextMuted
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .horizontalScroll(horizontalScroll)
            ) {
                val pxPerSec = with(density) { zoomDpPerSec.dp.toPx() }
                Box(
                    modifier = Modifier
                        .width(contentWidthDp)
                        .fillMaxHeight()
                        .pointerInput(zoomDpPerSec) {
                            detectTapGestures { offset ->
                                onSeek((offset.x / pxPerSec).toDouble())
                            }
                        }
                        .testTag("timeline_ruler")
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        for (sec in 0..timelineSeconds) {
                            val x = sec * pxPerSec
                            val isMajor = sec % 5 == 0
                            drawLine(
                                color = if (isMajor) StudioTextSecondary else StudioBorder,
                                start = Offset(x, if (isMajor) 4f else 12f),
                                end = Offset(x, size.height),
                                strokeWidth = if (isMajor) 1.5f else 1f
                            )
                        }
                    }
                    // Time labels every 5 seconds
                    for (sec in 0..timelineSeconds step 5) {
                        Text(
                            text = String.format("%02d:%02d", sec / 60, sec % 60),
                            style = StudioTimecodeStyle.copy(fontSize = 9.sp),
                            color = StudioTextSecondary,
                            modifier = Modifier
                                .offset(x = (sec * zoomDpPerSec + 3f).dp, y = 2.dp)
                        )
                    }
                    // Playhead cap on ruler
                    val playheadX = (playheadSec.toFloat() * zoomDpPerSec).dp
                    Box(
                        modifier = Modifier
                            .offset(x = playheadX - 4.dp, y = 0.dp)
                            .size(width = 9.dp, height = rulerHeight)
                            .background(StudioCoral, RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
                    )
                }
            }
        }

        // Scrollable 9-Track Editing Lanes
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(verticalScroll)
        ) {
            // Track Headers Column
            Column(
                modifier = Modifier
                    .width(headerWidth)
                    .background(StudioPanel)
            ) {
                TimelineTrackType.entries.forEach { track ->
                    val isMuted = track in mutedTracks
                    val trackAccent = colorForTrack(track)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(rowHeight)
                            .border(0.5.dp, StudioBorderSubtle)
                            .padding(horizontal = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(14.dp)
                                    .background(if (isMuted) StudioTextMuted else trackAccent, RoundedCornerShape(2.dp))
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = track.label,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                color = if (isMuted) StudioTextMuted else StudioTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = if (isMuted) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle ${track.label} visibility",
                            tint = if (isMuted) StudioTextMuted else StudioTextSecondary,
                            modifier = Modifier
                                .size(15.dp)
                                .clickable { onToggleMute(track) }
                                .testTag("track_toggle_${track.shortCode.lowercase()}")
                        )
                    }
                }
            }

            // Horizontally Scrollable Clip Lanes + Full-Height Playhead Line
            Box(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(horizontalScroll)
            ) {
                val pxPerSec = with(density) { zoomDpPerSec.dp.toPx() }
                Box(
                    modifier = Modifier
                        .width(contentWidthDp)
                        .height(rowHeight * TimelineTrackType.entries.size)
                        .pointerInput(zoomDpPerSec) {
                            detectTapGestures { offset ->
                                onSeek((offset.x / pxPerSec).toDouble())
                            }
                        }
                ) {
                    // Subtle grid lines
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        for (sec in 0..timelineSeconds step 2) {
                            val x = sec * pxPerSec
                            drawLine(
                                color = StudioBorderSubtle.copy(alpha = 0.45f),
                                start = Offset(x, 0f),
                                end = Offset(x, size.height),
                                strokeWidth = 1f
                            )
                        }
                    }

                    // 11 Track Rows (Visual + Audio + Dialogue + Ambient + Subtitles)
                    Column(modifier = Modifier.fillMaxSize()) {
                        TimelineTrackType.entries.forEach { track ->
                            val isMuted = track in mutedTracks
                            val trackColor = colorForTrack(track)
                            val matchingProjectClips = projectAudioClips.filter {
                                AudioTimelineEngine.trackTypeForClip(it.trackType) == track
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(rowHeight)
                                    .border(0.5.dp, StudioBorderSubtle.copy(alpha = 0.6f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    scenes.forEach { scene ->
                                        val clipWidth = (scene.durationSec.toFloat() * zoomDpPerSec).dp
                                        val isSelected = scene.id == selectedSceneId
                                        val label = clipLabelForTrack(track, scene, characters)
                                        val sceneTrackAudioClips = if (isAudioTrack(track)) {
                                            scene.audioClips.filter {
                                                AudioTimelineEngine.trackTypeForClip(it.trackType) == track
                                            }
                                        } else {
                                            emptyList()
                                        }

                                        val sceneTrackGraphics = if (isGraphicTrack(track)) {
                                            scene.motionGraphics.filter { MotionGraphicsEngine.trackForGraphic(it) == track }
                                        } else {
                                            emptyList()
                                        }

                                        Box(
                                            contentAlignment = Alignment.CenterStart,
                                            modifier = Modifier
                                                .width(clipWidth)
                                                .fillMaxHeight()
                                                .padding(horizontal = 1.dp, vertical = 2.dp)
                                        ) {
                                            if (sceneTrackGraphics.isNotEmpty()) {
                                                // Positioned Phase 6 Motion Graphic / Lower Third / Infographic / Map clips
                                                sceneTrackGraphics.forEach { gfx ->
                                                    val clampedStart = gfx.startSec.coerceIn(0.0, (scene.durationSec - 0.3).coerceAtLeast(0.0))
                                                    val clampedDur = gfx.durationSec.coerceIn(0.3, (scene.durationSec - clampedStart).coerceAtLeast(0.3))
                                                    val gfxOffsetDp = (clampedStart.toFloat() * zoomDpPerSec).dp
                                                    val gfxW = (clampedDur.toFloat() * zoomDpPerSec).dp.coerceAtLeast(22.dp)
                                                    val gfxSelected = gfx.graphicId == selectedGraphicId
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = trackColor.copy(
                                                            alpha = when {
                                                                isMuted || !gfx.visible -> 0.16f
                                                                gfxSelected -> 0.90f
                                                                else -> 0.64f
                                                            }
                                                        ),
                                                        border = BorderStroke(
                                                            width = if (gfxSelected) 1.5.dp else 0.75.dp,
                                                            color = if (gfxSelected) StudioAmber else trackColor
                                                        ),
                                                        modifier = Modifier
                                                            .offset(x = gfxOffsetDp)
                                                            .width(gfxW)
                                                            .fillMaxHeight()
                                                            .clickable { onSelectGraphic(scene.id, gfx.graphicId) }
                                                            .testTag("timeline_gfx_${gfx.graphicId}")
                                                    ) {
                                                        Box(
                                                            contentAlignment = Alignment.CenterStart,
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .padding(horizontal = 4.dp)
                                                        ) {
                                                            Text(
                                                                text = gfx.title.ifBlank { gfx.templateType },
                                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                                                color = if (isMuted || !gfx.visible) StudioTextMuted else Color.White,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                }
                                            } else if (track == TimelineTrackType.SUBTITLE && scene.subtitleCues.isNotEmpty()) {
                                                // Positioned Phase 6 Subtitle Cue bars
                                                scene.subtitleCues.sortedBy { it.startSec }.forEach { cue ->
                                                    val cStart = cue.startSec.coerceIn(0.0, (scene.durationSec - 0.2).coerceAtLeast(0.0))
                                                    val cDur = (cue.endSec - cStart).coerceIn(0.25, (scene.durationSec - cStart).coerceAtLeast(0.25))
                                                    val cOffsetDp = (cStart.toFloat() * zoomDpPerSec).dp
                                                    val cW = (cDur.toFloat() * zoomDpPerSec).dp.coerceAtLeast(18.dp)
                                                    Surface(
                                                        shape = RoundedCornerShape(3.dp),
                                                        color = trackColor.copy(alpha = if (isMuted) 0.16f else 0.68f),
                                                        border = BorderStroke(0.75.dp, if (isSelected) StudioAmber else trackColor),
                                                        modifier = Modifier
                                                            .offset(x = cOffsetDp)
                                                            .width(cW)
                                                            .fillMaxHeight()
                                                            .clickable { onSelectScene(scene.id) }
                                                    ) {
                                                        Box(
                                                            contentAlignment = Alignment.CenterStart,
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .padding(horizontal = 3.dp)
                                                        ) {
                                                            Text(
                                                                text = cue.text,
                                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                                                color = if (isMuted) StudioTextMuted else StudioVoid,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                }
                                            } else if (sceneTrackAudioClips.isNotEmpty()) {
                                                // Real positioned audio clips with genuine decoded PCM waveforms & fade indicators
                                                sceneTrackAudioClips.forEach { audioClip ->
                                                    val effDur = AudioTimelineEngine.effectiveTimelineDuration(
                                                        clip = audioClip,
                                                        containerDurationSec = scene.durationSec
                                                    ).coerceAtLeast(0.4)
                                                    val maxRemaining = (scene.durationSec - audioClip.startSec).coerceAtLeast(0.4)
                                                    val clampedDur = effDur.coerceAtMost(maxRemaining)
                                                    val clipOffsetDp = (audioClip.startSec.toFloat() * zoomDpPerSec).dp
                                                    val clipW = (clampedDur.toFloat() * zoomDpPerSec).dp.coerceAtLeast(18.dp)
                                                    val clipSelected = audioClip.clipId == selectedAudioClipId
                                                    val peaks = assetById[audioClip.audioAssetId]?.waveformPeaks.orEmpty()

                                                    TimelineAudioClipBar(
                                                        clip = audioClip,
                                                        peaks = peaks,
                                                        trackColor = trackColor,
                                                        isTrackMuted = isMuted,
                                                        isClipSelected = clipSelected,
                                                        effectiveDurationSec = clampedDur,
                                                        onClick = {
                                                            onSelectScene(scene.id)
                                                            onSelectAudioClip(audioClip.clipId)
                                                        },
                                                        modifier = Modifier
                                                            .offset(x = clipOffsetDp)
                                                            .width(clipW)
                                                            .fillMaxHeight()
                                                    )
                                                }
                                            } else if (label != null) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = trackColor.copy(
                                                        alpha = when {
                                                            isMuted -> 0.18f
                                                            isSelected -> 0.85f
                                                            else -> 0.52f
                                                        }
                                                    ),
                                                    border = BorderStroke(
                                                        width = if (isSelected) 1.dp else 0.5.dp,
                                                        color = if (isSelected) StudioAmber else trackColor
                                                    ),
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clickable { onSelectScene(scene.id) }
                                                ) {
                                                    Box(modifier = Modifier.fillMaxSize()) {
                                                        Box(
                                                            contentAlignment = Alignment.CenterStart,
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .padding(horizontal = 5.dp)
                                                        ) {
                                                            Text(
                                                                text = label,
                                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                                color = if (isMuted) StudioTextMuted else Color.White,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }

                                                        // Real Keyframe (Amber), Acting Cue (Teal) & Dialogue/Lip-Sync (Violet) Markers
                                                        val (kfTimes, cueTimes) = keyframeTimesForTrack(track, scene)
                                                        val speakingWindows = if (track == TimelineTrackType.DIALOGUE || track == TimelineTrackType.CHARACTER) {
                                                            scene.dialogueLines
                                                        } else {
                                                            emptyList()
                                                        }
                                                        if (kfTimes.isNotEmpty() || cueTimes.isNotEmpty() || speakingWindows.isNotEmpty()) {
                                                            val safeDur = scene.durationSec.coerceAtLeast(0.1)
                                                            Canvas(modifier = Modifier.fillMaxSize()) {
                                                                val r = 3.5.dp.toPx()
                                                                val cy = size.height - r - 1.5.dp.toPx()
                                                                kfTimes.forEach { t ->
                                                                    val frac = (t / safeDur).coerceIn(0.0, 1.0).toFloat()
                                                                    val cx = (frac * size.width).coerceIn(r, (size.width - r).coerceAtLeast(r))
                                                                    val diamond = Path().apply {
                                                                        moveTo(cx, cy - r)
                                                                        lineTo(cx + r, cy)
                                                                        lineTo(cx, cy + r)
                                                                        lineTo(cx - r, cy)
                                                                        close()
                                                                    }
                                                                    drawPath(diamond, color = StudioAmber)
                                                                }
                                                                val cueY = r + 1.5.dp.toPx()
                                                                cueTimes.forEach { t ->
                                                                    val frac = (t / safeDur).coerceIn(0.0, 1.0).toFloat()
                                                                    val cx = (frac * size.width).coerceIn(r, (size.width - r).coerceAtLeast(r))
                                                                    val diamond = Path().apply {
                                                                        moveTo(cx, cueY - r)
                                                                        lineTo(cx + r, cueY)
                                                                        lineTo(cx, cueY + r)
                                                                        lineTo(cx - r, cueY)
                                                                        close()
                                                                    }
                                                                    drawPath(diamond, color = StudioTeal)
                                                                }
                                                                // Dialogue speaking window strip along top edge
                                                                speakingWindows.forEach { line ->
                                                                    val x0 = ((line.startSec / safeDur).coerceIn(0.0, 1.0) * size.width).toFloat()
                                                                    val x1 = (((line.startSec + line.durationSec) / safeDur).coerceIn(0.0, 1.0) * size.width).toFloat()
                                                                    if (x1 > x0) {
                                                                        drawLine(
                                                                            color = StudioViolet,
                                                                            start = Offset(x0, 2f),
                                                                            end = Offset(x1, 2f),
                                                                            strokeWidth = 3f
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Clickable Scene Transition Boundary Markers on TRANSITIONS Track
                                if (track == TimelineTrackType.TRANSITIONS) {
                                    val segments = TransitionFlowEngine.computeStoryFlowSegments(scenes)
                                    segments.filter { it.hasNextScene }.forEach { seg ->
                                        val tr = seg.outgoingTransition
                                        val spanSec = if (tr.type == "CUT") 0.5 else tr.durationSec
                                        val centerDp = (seg.endSec.toFloat() * zoomDpPerSec).dp
                                        val badgeW = (spanSec.toFloat() * zoomDpPerSec).dp.coerceAtLeast(36.dp)
                                        val shortTag = when (tr.type) {
                                            "CROSSFADE" -> "CF"
                                            "DISSOLVE" -> "DS"
                                            "FADE" -> "FD"
                                            "WIPE" -> "WP"
                                            "SLIDE" -> "SL"
                                            "ZOOM" -> "ZM"
                                            "BLUR" -> "BL"
                                            else -> "CUT"
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (tr.type == "CUT") {
                                                StudioElevated.copy(alpha = 0.7f)
                                            } else {
                                                trackColor.copy(alpha = if (isMuted) 0.18f else 0.85f)
                                            },
                                            border = BorderStroke(
                                                1.dp,
                                                if (seg.scene.id == selectedSceneId) StudioAmber else trackColor
                                            ),
                                            modifier = Modifier
                                                .align(Alignment.CenterStart)
                                                .offset(x = (centerDp - badgeW / 2f).coerceAtLeast(0.dp))
                                                .width(badgeW)
                                                .height(rowHeight - 6.dp)
                                                .clickable { onSelectTransitionScene(seg.scene.id) }
                                                .testTag("timeline_transition_${seg.scene.id}")
                                        ) {
                                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                Text(
                                                    text = if (tr.type == "CUT") "CUT" else "$shortTag ${String.format("%.1f", tr.durationSec)}s",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp, fontWeight = FontWeight.Bold),
                                                    color = if (isMuted) StudioTextMuted else Color.White,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }

                                // Project-spanning audio clips (e.g., global documentary score / ambient bed)
                                matchingProjectClips.forEach { projClip ->
                                    val effDur = AudioTimelineEngine.effectiveTimelineDuration(
                                        clip = projClip,
                                        containerDurationSec = totalDurationSec
                                    ).coerceAtLeast(0.5)
                                    val clipOffsetDp = (projClip.startSec.toFloat() * zoomDpPerSec).dp
                                    val clipW = (effDur.toFloat() * zoomDpPerSec).dp.coerceAtLeast(24.dp)
                                    val peaks = assetById[projClip.audioAssetId]?.waveformPeaks.orEmpty()
                                    TimelineAudioClipBar(
                                        clip = projClip,
                                        peaks = peaks,
                                        trackColor = trackColor,
                                        isTrackMuted = isMuted,
                                        isClipSelected = projClip.clipId == selectedAudioClipId,
                                        effectiveDurationSec = effDur,
                                        onClick = { onSelectAudioClip(projClip.clipId) },
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .padding(vertical = 2.dp)
                                            .offset(x = clipOffsetDp)
                                            .width(clipW)
                                            .fillMaxHeight()
                                    )
                                }
                            }
                        }
                    }

                    // Full-height Red/Coral Playhead Line
                    val playheadOffsetPx = (playheadSec.toFloat() * pxPerSec).roundToInt()
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(x = playheadOffsetPx, y = 0) }
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(StudioCoral)
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineAudioClipBar(
    clip: SceneAudioClip,
    peaks: List<Double>,
    trackColor: Color,
    isTrackMuted: Boolean,
    isClipSelected: Boolean,
    effectiveDurationSec: Double,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val effectivelyMuted = isTrackMuted || clip.muted
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = trackColor.copy(
            alpha = when {
                effectivelyMuted -> 0.16f
                isClipSelected -> 0.88f
                else -> 0.62f
            }
        ),
        border = BorderStroke(
            width = if (isClipSelected) 1.5.dp else 0.75.dp,
            color = if (isClipSelected) StudioAmber else trackColor
        ),
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("timeline_audio_clip_${clip.clipId}")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Render genuine decoded waveform peaks ONLY when available (never fabricated)
            if (peaks.isNotEmpty() || clip.fadeInSec > 0.0 || clip.fadeOutSec > 0.0) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (peaks.isNotEmpty()) {
                        val barCount = peaks.size
                        val slotW = size.width / barCount.toFloat()
                        val midY = size.height / 2f
                        val maxHalfH = (size.height * 0.42f).coerceAtLeast(2f)
                        peaks.forEachIndexed { idx, p ->
                            val amp = p.coerceIn(0.05, 1.0).toFloat()
                            val x = idx * slotW + slotW * 0.5f
                            val h = amp * maxHalfH
                            drawLine(
                                color = Color.White.copy(alpha = if (effectivelyMuted) 0.18f else 0.42f),
                                start = Offset(x, midY - h),
                                end = Offset(x, midY + h),
                                strokeWidth = (slotW * 0.55f).coerceIn(1f, 3f)
                            )
                        }
                    }
                    val safeDur = effectiveDurationSec.coerceAtLeast(0.1)
                    if (clip.fadeInSec > 0.01) {
                        val fx = ((clip.fadeInSec / safeDur).coerceIn(0.0, 0.45) * size.width).toFloat()
                        val fadeInPath = Path().apply {
                            moveTo(0f, 0f)
                            lineTo(fx, 0f)
                            lineTo(0f, size.height)
                            close()
                        }
                        drawPath(fadeInPath, color = StudioVoid.copy(alpha = 0.45f))
                    }
                    if (clip.fadeOutSec > 0.01) {
                        val fx = ((clip.fadeOutSec / safeDur).coerceIn(0.0, 0.45) * size.width).toFloat()
                        val fadeOutPath = Path().apply {
                            moveTo(size.width, 0f)
                            lineTo(size.width - fx, 0f)
                            lineTo(size.width, size.height)
                            close()
                        }
                        drawPath(fadeOutPath, color = StudioVoid.copy(alpha = 0.45f))
                    }
                }
            }

            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 5.dp)
            ) {
                Text(
                    text = buildString {
                        if (clip.scope == "PROJECT") append("[PRJ] ")
                        append(clip.title.ifBlank { clip.trackType })
                        if (clip.loopEnabled) append(" ↻")
                    },
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                    color = if (effectivelyMuted) StudioTextMuted else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
