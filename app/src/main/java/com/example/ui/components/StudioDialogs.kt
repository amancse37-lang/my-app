package com.example.ui.components

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StudioAsset
import com.example.data.StudioCharacter
import com.example.data.StudioPresets
import com.example.data.StudioProject
import com.example.data.StudioScene
import com.example.data.TimelineTrackType
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
import com.example.ui.theme.StudioVoid

@Composable
fun ProjectSettingsDialog(
    project: StudioProject,
    userEmail: String?,
    onDismiss: () -> Unit,
    onApply: (String, String, String, Long, String) -> Unit,
    onSignOut: () -> Unit
) {
    var name by remember(project.id) { mutableStateOf(project.name) }
    var language by remember(project.id) { mutableStateOf(project.scriptLanguage) }
    var aspect by remember(project.id) { mutableStateOf(project.aspectRatio) }
    var fps by remember(project.id) { mutableLongStateOf(project.fps) }
    var resolution by remember(project.id) { mutableStateOf(project.resolution) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioPanel,
        title = {
            Text("Project Settings", style = MaterialTheme.typography.headlineSmall, color = StudioTextPrimary)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(120) },
                    label = { Text("Project Title") },
                    singleLine = true,
                    colors = dialogFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Script Language", style = MaterialTheme.typography.labelMedium, color = StudioTextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StudioPresets.languages.forEach { lang ->
                        FilterChip(
                            selected = language == lang,
                            onClick = { language = lang },
                            label = { Text(lang, style = MaterialTheme.typography.labelSmall) },
                            colors = dialogChipColors()
                        )
                    }
                }

                Text("Aspect Ratio", style = MaterialTheme.typography.labelMedium, color = StudioTextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StudioPresets.aspectRatios.forEach { ratio ->
                        FilterChip(
                            selected = aspect == ratio,
                            onClick = { aspect = ratio },
                            label = { Text(ratio, style = MaterialTheme.typography.labelSmall) },
                            colors = dialogChipColors()
                        )
                    }
                }

                Text("Frame Rate (FPS)", style = MaterialTheme.typography.labelMedium, color = StudioTextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StudioPresets.frameRates.forEach { rate ->
                        FilterChip(
                            selected = fps == rate,
                            onClick = { fps = rate },
                            label = { Text("${rate} fps", style = MaterialTheme.typography.labelSmall) },
                            colors = dialogChipColors()
                        )
                    }
                }

                Text("Target Resolution", style = MaterialTheme.typography.labelMedium, color = StudioTextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StudioPresets.resolutions.forEach { res ->
                        FilterChip(
                            selected = resolution == res,
                            onClick = { resolution = res },
                            label = { Text(res, style = MaterialTheme.typography.labelSmall) },
                            colors = dialogChipColors()
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StudioCharcoal,
                    border = BorderStroke(1.dp, StudioBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Cloud Account", style = MaterialTheme.typography.labelSmall, color = StudioTextMuted)
                            Text(
                                text = userEmail ?: "Google Account Connected",
                                style = MaterialTheme.typography.bodySmall,
                                color = StudioTextPrimary
                            )
                        }
                        TextButton(onClick = onSignOut) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = StudioCoral, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Sign Out", color = StudioCoral, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApply(name, language, aspect, fps, resolution)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = StudioAmber, contentColor = StudioVoid)
            ) {
                Text("Apply Settings", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = StudioTextSecondary)
            }
        }
    )
}

@Composable
fun ProjectSwitcherDialog(
    projects: List<StudioProject>,
    activeProjectId: String?,
    onDismiss: () -> Unit,
    onSelectProject: (StudioProject) -> Unit,
    onCreateProject: (String, String) -> Unit,
    onDeleteProject: (String) -> Unit
) {
    var newTitle by remember { mutableStateOf("") }
    var newLanguage by remember { mutableStateOf("Bilingual") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioPanel,
        title = {
            Text("Studio Projects", style = MaterialTheme.typography.headlineSmall, color = StudioTextPrimary)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                projects.forEach { proj ->
                    val active = proj.id == activeProjectId
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (active) StudioAmberDim else StudioCharcoal,
                        border = BorderStroke(1.dp, if (active) StudioAmber else StudioBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectProject(proj)
                                onDismiss()
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(proj.name, style = MaterialTheme.typography.labelLarge, color = StudioTextPrimary)
                                Text(
                                    text = "${proj.aspectRatio} · ${proj.fps}fps · ${proj.resolution} · ${proj.scriptLanguage}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (active) StudioAmber else StudioTextMuted
                                )
                            }
                            if (projects.size > 1) {
                                IconButton(
                                    onClick = { onDeleteProject(proj.id) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete project", tint = StudioCoral, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text("Create New Documentary Project", style = MaterialTheme.typography.labelMedium, color = StudioTeal)
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it.take(120) },
                    placeholder = { Text("e.g. River of Stories", color = StudioTextMuted) },
                    singleLine = true,
                    colors = dialogFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StudioPresets.languages.forEach { lang ->
                        FilterChip(
                            selected = newLanguage == lang,
                            onClick = { newLanguage = lang },
                            label = { Text(lang, style = MaterialTheme.typography.labelSmall) },
                            colors = dialogChipColors()
                        )
                    }
                }
                Button(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            onCreateProject(newTitle.trim(), newLanguage)
                            onDismiss()
                        }
                    },
                    enabled = newTitle.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = StudioTeal, contentColor = StudioVoid),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Create Project", fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = StudioTextSecondary)
            }
        }
    )
}


@Composable
fun AssetEditorDialog(
    defaultCategory: String,
    onDismiss: () -> Unit,
    onSave: (StudioAsset) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(if (defaultCategory == "All") "Prop" else defaultCategory) }
    var tags by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var durationLabel by remember { mutableStateOf("1920x1080") }

    val categories = StudioPresets.assetCategories.filterNot { it == "All" }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioPanel,
        title = {
            Text("Add Studio Asset", style = MaterialTheme.typography.headlineSmall, color = StudioTextPrimary)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(120) },
                    label = { Text("Asset Name") },
                    singleLine = true,
                    colors = dialogFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Category", style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = {
                                category = cat
                                durationLabel = when (cat) {
                                    "Audio", "SFX" -> "00:15"
                                    "MotionGraphic" -> "00:05"
                                    else -> "1920x1080"
                                }
                            },
                            label = { Text(cat, style = MaterialTheme.typography.labelSmall) },
                            colors = dialogChipColors()
                        )
                    }
                }
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it.take(200) },
                    label = { Text("Tags (comma separated)") },
                    singleLine = true,
                    colors = dialogFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it.take(1000) },
                    label = { Text("Art / Production Notes") },
                    minLines = 2,
                    colors = dialogFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val presetKey = when (category) {
                        "Location", "Background" -> "himalayas"
                        "MotionGraphic" -> "Lower Third"
                        else -> name.lowercase().replace(" ", "_").take(32)
                    }
                    onSave(
                        StudioAsset(
                            name = name.trim(),
                            category = category,
                            tags = tags.trim(),
                            notes = notes.trim(),
                            presetKey = presetKey,
                            localUri = "",
                            durationLabel = durationLabel
                        )
                    )
                    onDismiss()
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = StudioAmber, contentColor = StudioVoid)
            ) {
                Text("Add Asset", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = StudioTextSecondary)
            }
        }
    )
}

@Composable
fun ExportVideoDialog(
    project: StudioProject?,
    sceneCount: Int,
    totalDurationSec: Double,
    shotlistJson: String,
    srtContent: String = "",
    onDismiss: () -> Unit,
    onCopiedShotlist: () -> Unit,
    onCopiedSrt: () -> Unit = {}
) {
    val clipboard = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioPanel,
        title = {
            Text("Export Studio Blueprint", style = MaterialTheme.typography.headlineSmall, color = StudioTextPrimary)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StudioAmberDim,
                    border = BorderStroke(1.dp, StudioAmber.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = StudioAmber, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Studio Scope: Real-time 2D layered compositing, AI backgrounds/props/characters, keyframe animation, camera/lighting/VFX, audio & lip-sync, animated motion graphics, infographics, documentary maps, dual-scene transitions, and SubRip (.SRT) export are active. Offline MP4 video encoding is planned for the next studio stage.",
                            style = MaterialTheme.typography.bodySmall,
                            color = StudioTextPrimary
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
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Text("PROJECT SUMMARY", style = MaterialTheme.typography.labelSmall, color = StudioTeal)
                        Text("Title: ${project?.name ?: "Untitled"}", style = MaterialTheme.typography.bodySmall, color = StudioTextPrimary)
                        Text("Format: ${project?.resolution ?: "1080p"} · ${project?.aspectRatio ?: "16:9"} · ${project?.fps ?: 24} fps", style = MaterialTheme.typography.bodySmall, color = StudioTextSecondary)
                        Text("Timeline: $sceneCount scenes · ${String.format("%.1f", totalDurationSec)}s · ${TimelineTrackType.entries.size} tracks", style = MaterialTheme.typography.bodySmall, color = StudioTextSecondary)
                    }
                }

                OutlinedButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(shotlistJson))
                        onCopiedShotlist()
                    },
                    border = BorderStroke(1.dp, StudioTeal),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = StudioTeal, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Copy Project JSON & Shotlist", color = StudioTeal)
                }

                if (srtContent.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(srtContent))
                            onCopiedSrt()
                        },
                        border = BorderStroke(1.dp, StudioAmber),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = StudioAmber, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copy Subtitles (.SRT)", color = StudioAmber)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = StudioAmber, contentColor = StudioVoid)
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun TheaterPreviewOverlay(
    scene: StudioScene?,
    scenes: List<StudioScene> = emptyList(),
    sceneNumber: Int,
    totalScenes: Int,
    sceneProgress: Double,
    sceneTimeSec: Double = 0.0,
    characters: List<StudioCharacter>,
    characterReferencesByCharId: Map<String, List<com.example.data.CharacterReferenceSheet>> = emptyMap(),
    assets: List<StudioAsset>,
    isPlaying: Boolean,
    playheadSec: Double,
    totalDurationSec: Double,
    fps: Long,
    mutedTracks: Set<TimelineTrackType>,
    onTogglePlayPause: () -> Unit,
    onStop: () -> Unit,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioVoid)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        ) {
            Text(
                text = "THEATER PREVIEW · ${formatTimecode(playheadSec, fps)} / ${formatTimecode(totalDurationSec, fps)}",
                style = StudioTimecodeStyle,
                color = StudioAmber
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Exit Theater Preview", tint = StudioTextPrimary)
            }
        }

        val activeTransition = remember(scenes, playheadSec, mutedTracks) {
            if (scenes.size >= 2 && TimelineTrackType.TRANSITIONS !in mutedTracks) {
                com.example.graphics.TransitionFlowEngine.resolveTransitionAtPlayhead(scenes, playheadSec)
            } else null
        }

        if (activeTransition != null) {
            val transforms = remember(activeTransition.transition, activeTransition.progress) {
                com.example.graphics.TransitionFlowEngine.evaluateTransitionTransforms(
                    activeTransition.transition,
                    activeTransition.progress
                )
            }
            val outDur = activeTransition.outgoingScene.durationSec.coerceAtLeast(0.1)
            val inDur = activeTransition.incomingScene.durationSec.coerceAtLeast(0.1)
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
                        canvasZoom = 1.0f,
                        mutedTracks = mutedTracks,
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
                        canvasZoom = 1.0f,
                        mutedTracks = mutedTracks,
                        modifier = Modifier.fillMaxSize()
                    )
                },
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            )
        } else if (scene != null) {
            CinematicStage16x9(
                scene = scene,
                sceneNumber = sceneNumber,
                totalScenes = totalScenes,
                sceneProgress = sceneProgress,
                sceneTimeSec = sceneTimeSec,
                characters = characters,
                characterReferencesByCharId = characterReferencesByCharId,
                assets = assets,
                selectedStageTarget = null,
                interactive = false,
                isPlaying = isPlaying,
                canvasZoom = 1.0f,
                mutedTracks = mutedTracks,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        ) {
            Button(
                onClick = onTogglePlayPause,
                colors = ButtonDefaults.buttonColors(containerColor = StudioAmber, contentColor = StudioVoid)
            ) {
                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(if (isPlaying) "Pause" else "Play Preview", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onStop,
                border = BorderStroke(1.dp, StudioBorder)
            ) {
                Icon(Icons.Default.Stop, contentDescription = null, tint = StudioTextPrimary)
                Spacer(Modifier.width(4.dp))
                Text("Reset", color = StudioTextPrimary)
            }
        }
    }
}

@Composable
private fun dialogFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = StudioCharcoal,
    unfocusedContainerColor = StudioCharcoal,
    focusedBorderColor = StudioAmber,
    unfocusedBorderColor = StudioBorder,
    focusedTextColor = StudioTextPrimary,
    unfocusedTextColor = StudioTextPrimary
)

@Composable
private fun dialogChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = StudioAmberDim,
    selectedLabelColor = StudioAmber
)
