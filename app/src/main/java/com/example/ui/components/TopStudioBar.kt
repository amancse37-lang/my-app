package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.SaveStatus
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCharcoal
import com.example.ui.theme.StudioElevated
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioPanel
import com.example.ui.theme.StudioTeal
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.StudioVoid

@Composable
fun TopStudioBar(
    projectName: String,
    saveStatus: SaveStatus,
    canUndo: Boolean,
    canRedo: Boolean,
    isExpanded: Boolean,
    onOpenProjects: () -> Unit,
    onSaveClick: () -> Unit,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onPreviewClick: () -> Unit,
    onExportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = StudioPanel,
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .border(width = 1.dp, color = StudioBorder)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .padding(horizontal = 10.dp)
        ) {
            // Left: Logo + Brand + Project Switcher Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_motionstory_logo_1795597590266),
                    contentDescription = "MotionStory Studio Logo",
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .border(1.dp, StudioAmber.copy(alpha = 0.5f), RoundedCornerShape(7.dp))
                )
                if (isExpanded) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "MotionStory Studio",
                        style = MaterialTheme.typography.titleMedium,
                        color = StudioTextPrimary,
                        maxLines = 1
                    )
                    Spacer(Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(20.dp)
                            .background(StudioBorder)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = StudioElevated,
                    border = BorderStroke(1.dp, StudioBorder),
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .clickable(onClick = onOpenProjects)
                        .testTag("project_selector_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = projectName,
                            style = MaterialTheme.typography.labelLarge,
                            color = StudioTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.width(if (isExpanded) 170.dp else 104.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = "Switch project",
                            tint = StudioTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.width(6.dp))

            // Right: Action Controls (scrollable on very narrow screens so nothing ever clips)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                // Undo
                IconButton(
                    onClick = onUndoClick,
                    enabled = canUndo,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("undo_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (canUndo) StudioTextPrimary else StudioTextMuted,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Redo
                IconButton(
                    onClick = onRedoClick,
                    enabled = canRedo,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("redo_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        tint = if (canRedo) StudioTextPrimary else StudioTextMuted,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Save Project
                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = if (saveStatus == SaveStatus.Unsaved) StudioAmber.copy(alpha = 0.16f) else StudioElevated,
                    border = BorderStroke(
                        1.dp,
                        if (saveStatus == SaveStatus.Unsaved) StudioAmber.copy(alpha = 0.65f) else StudioBorder
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .clickable(enabled = saveStatus != SaveStatus.Saving, onClick = onSaveClick)
                        .testTag("save_project_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp)
                    ) {
                        when (saveStatus) {
                            SaveStatus.Saving -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = StudioAmber
                                )
                            }
                            SaveStatus.Saved -> {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = "Saved",
                                    tint = StudioEmerald,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = "Save Project",
                                    tint = StudioAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        if (isExpanded) {
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = when (saveStatus) {
                                    SaveStatus.Saving -> "Saving"
                                    SaveStatus.Saved -> "Saved"
                                    SaveStatus.Unsaved -> "Save"
                                    is SaveStatus.Error -> "Retry Save"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = if (saveStatus == SaveStatus.Unsaved) StudioAmber else StudioTextPrimary
                            )
                        } else if (saveStatus == SaveStatus.Unsaved) {
                            Spacer(Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(StudioAmber)
                            )
                        }
                    }
                }

                // Preview (Full Theater Mode)
                IconButton(
                    onClick = onPreviewClick,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("preview_fullscreen_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Theater Preview",
                        tint = StudioTeal,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Settings
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Project Settings",
                        tint = StudioTextSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Spacer(Modifier.width(4.dp))

                // Export Video CTA
                Button(
                    onClick = onExportClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudioAmber,
                        contentColor = StudioVoid
                    ),
                    shape = RoundedCornerShape(7.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = if (isExpanded) 12.dp else 9.dp,
                        vertical = 0.dp
                    ),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("export_video_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MovieCreation,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    if (isExpanded) {
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "Export Video",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
