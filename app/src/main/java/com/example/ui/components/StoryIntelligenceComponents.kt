package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.audio.AudioTimelineEngine
import com.example.data.GeneratedScenePlan
import com.example.data.ScriptBreakdownResult
import com.example.data.StoryAnalysisSummary
import com.example.data.StudioScene
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioAmberDim
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCharcoal
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioElevated
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioPanel
import com.example.ui.theme.StudioTeal
import com.example.ui.theme.StudioTealDim
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.StudioViolet
import com.example.ui.theme.StudioVoid

/**
 * Full-screen Review, Edit & Approve dialog shown after Gemini Story Intelligence analyzes a script,
 * ensuring the user inspects, edits, reorders, and approves scenes before modifying the project.
 */
@Composable
fun StoryPlanReviewDialog(
    draft: ScriptBreakdownResult,
    approvedExistingSceneCount: Int,
    onUpdateAnalysis: ((StoryAnalysisSummary) -> StoryAnalysisSummary) -> Unit,
    onUpdateScene: (Int, (GeneratedScenePlan) -> GeneratedScenePlan) -> Unit,
    onMoveScene: (Int, Int) -> Unit,
    onRemoveScene: (Int) -> Unit,
    onApproveAndApply: (preserveApprovedScenes: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var preserveApproved by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = StudioPanel,
            border = BorderStroke(1.dp, StudioBorder),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .testTag("story_plan_review_dialog")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StudioElevated)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = StudioAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "REVIEW & APPROVE AI STORY PLAN",
                                style = MaterialTheme.typography.titleSmall,
                                color = StudioTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${draft.scenes.size} planned scenes · ${draft.identifiedCharacters.size} characters · Est. ${"%.1f".format(draft.scenes.sumOf { it.durationSec })}s",
                                style = MaterialTheme.typography.labelSmall,
                                color = StudioTeal
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss review", tint = StudioTextSecondary)
                    }
                }

                HorizontalDivider(color = StudioBorder)

                LazyColumn(
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Story Analysis Summary Card
                    item(key = "story_analysis_card") {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = StudioCharcoal,
                            border = BorderStroke(1.dp, StudioTeal.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("STORY INTELLIGENCE SUMMARY", style = MaterialTheme.typography.labelMedium, color = StudioTeal)
                                    Surface(color = StudioTealDim, shape = RoundedCornerShape(4.dp)) {
                                        Text(
                                            text = draft.storyAnalysis.detectedLanguage,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = StudioTeal,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = draft.storyAnalysis.storyLogline,
                                    onValueChange = { txt -> onUpdateAnalysis { it.copy(storyLogline = txt.take(600)) } },
                                    label = { Text("Story Logline (Editable)", style = MaterialTheme.typography.labelSmall) },
                                    textStyle = MaterialTheme.typography.bodySmall.copy(color = StudioTextPrimary),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = StudioVoid,
                                        unfocusedContainerColor = StudioVoid,
                                        focusedBorderColor = StudioTeal,
                                        unfocusedBorderColor = StudioBorder
                                    ),
                                    maxLines = 3,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (draft.storyAnalysis.eraOrPeriod.isNotBlank() || draft.storyAnalysis.emotionalArc.isNotBlank()) {
                                    Text(
                                        text = buildString {
                                            if (draft.storyAnalysis.eraOrPeriod.isNotBlank()) append("Period: ${draft.storyAnalysis.eraOrPeriod}")
                                            if (draft.storyAnalysis.emotionalArc.isNotBlank()) {
                                                if (isNotEmpty()) append("  ·  ")
                                                append("Arc: ${draft.storyAnalysis.emotionalArc}")
                                            }
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = StudioTextSecondary
                                    )
                                }

                                if (draft.storyAnalysis.locations.isNotEmpty()) {
                                    Text(
                                        text = "Locations: ${draft.storyAnalysis.locations.joinToString(", ")}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = StudioTextSecondary
                                    )
                                }
                                if (draft.storyAnalysis.characterRelationships.isNotEmpty()) {
                                    Text(
                                        text = "Relationships: ${draft.storyAnalysis.characterRelationships.joinToString(" · ")}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = StudioTextSecondary
                                    )
                                }
                                if (draft.identifiedCharacters.isNotEmpty()) {
                                    Text(
                                        text = "Characters: " + draft.identifiedCharacters.joinToString(", ") { c ->
                                            val tag = if (c.existingCharCode.isNotBlank()) "Reuse ${c.existingCharCode}" else "New"
                                            "${c.name} ($tag)"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StudioAmber
                                    )
                                }
                            }
                        }
                    }

                    // Editable Planned Scene Cards
                    itemsIndexed(draft.scenes, key = { idx, sc -> "draft_${idx}_${sc.title}" }) { idx, plan ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = StudioCharcoal,
                            border = BorderStroke(1.dp, StudioBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(color = StudioAmberDim, shape = RoundedCornerShape(4.dp)) {
                                            Text(
                                                text = "SCENE ${idx + 1}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = StudioAmber,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = "${"%.1f".format(plan.durationSec)}s · ${plan.scenePlan.transitionSuggestion}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = StudioTeal
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        IconButton(
                                            onClick = { onMoveScene(idx, -1) },
                                            enabled = idx > 0,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.ArrowUpward, contentDescription = "Move up", tint = StudioTextSecondary, modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(
                                            onClick = { onMoveScene(idx, 1) },
                                            enabled = idx < draft.scenes.lastIndex,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.ArrowDownward, contentDescription = "Move down", tint = StudioTextSecondary, modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(
                                            onClick = { onRemoveScene(idx) },
                                            enabled = draft.scenes.size > 1,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Remove scene", tint = StudioCoral, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = plan.title,
                                    onValueChange = { txt -> onUpdateScene(idx) { it.copy(title = txt.take(120)) } },
                                    label = { Text("Scene Title", style = MaterialTheme.typography.labelSmall) },
                                    textStyle = MaterialTheme.typography.bodySmall.copy(color = StudioTextPrimary),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = StudioVoid,
                                        unfocusedContainerColor = StudioVoid,
                                        focusedBorderColor = StudioAmber,
                                        unfocusedBorderColor = StudioBorder
                                    ),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = plan.narrationText,
                                    onValueChange = { txt ->
                                        val est = AudioTimelineEngine.estimateSpeechDurationSec(txt, draft.storyAnalysis.detectedLanguage)
                                        onUpdateScene(idx) {
                                            it.copy(
                                                narrationText = txt.take(3000),
                                                durationSec = est.coerceIn(3.0, 60.0)
                                            )
                                        }
                                    },
                                    label = { Text("Narration Segment (auto-updates duration)", style = MaterialTheme.typography.labelSmall) },
                                    textStyle = MaterialTheme.typography.bodySmall.copy(color = StudioTextPrimary),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = StudioVoid,
                                        unfocusedContainerColor = StudioVoid,
                                        focusedBorderColor = StudioTeal,
                                        unfocusedBorderColor = StudioBorder
                                    ),
                                    minLines = 2,
                                    maxLines = 4,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Metadata chips
                                Text(
                                    text = buildString {
                                        if (plan.scenePlan.location.isNotBlank()) append("Loc: ${plan.scenePlan.location}")
                                        if (plan.scenePlan.cameraSuggestion.isNotBlank()) {
                                            if (isNotEmpty()) append(" · ")
                                            append("Cam: ${plan.scenePlan.cameraSuggestion}")
                                        }
                                        if (plan.scenePlan.lightingSuggestion.isNotBlank()) {
                                            if (isNotEmpty()) append(" · ")
                                            append("Light: ${plan.scenePlan.lightingSuggestion}")
                                        }
                                        if (plan.scenePlan.atmosphereSuggestion.isNotBlank()) {
                                            if (isNotEmpty()) append(" · ")
                                            append("Atmo: ${plan.scenePlan.atmosphereSuggestion}")
                                        }
                                    }.ifBlank { "Visual: ${plan.visualSummary}" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StudioTextSecondary
                                )

                                if (plan.dialogueLines.isNotEmpty()) {
                                    Text(
                                        text = "Dialogue (${plan.dialogueLines.size}): " +
                                            plan.dialogueLines.joinToString(" | ") { "${it.speakerName}: \"${it.text.take(40)}\"" },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StudioViolet,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (plan.soundSuggestions.isNotEmpty()) {
                                    Text(
                                        text = "Audio Cues: " + plan.soundSuggestions.joinToString(", ") { "${it.category}: ${it.title}" },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StudioEmerald,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = StudioBorder)

                // Footer Approval Bar
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StudioElevated)
                        .padding(14.dp)
                ) {
                    if (approvedExistingSceneCount > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { preserveApproved = !preserveApproved }
                        ) {
                            Checkbox(
                                checked = preserveApproved,
                                onCheckedChange = { preserveApproved = it },
                                colors = CheckboxDefaults.colors(checkedColor = StudioEmerald)
                            )
                            Text(
                                text = "Preserve $approvedExistingSceneCount existing Approved scene(s) without overwriting them",
                                style = MaterialTheme.typography.bodySmall,
                                color = StudioTextPrimary
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            border = BorderStroke(1.dp, StudioBorder),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("story_plan_cancel_button")
                        ) {
                            Text("Cancel", color = StudioTextSecondary)
                        }
                        Button(
                            onClick = { onApproveAndApply(preserveApproved) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudioAmber,
                                contentColor = StudioVoid
                            ),
                            modifier = Modifier
                                .weight(1.6f)
                                .testTag("story_plan_approve_button")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Approve & Apply Plan", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact narration-to-scene sync health diagnostics shown inside the Script Sidebar panel.
 */
@Composable
fun NarrationSyncDiagnosticCard(
    scenes: List<StudioScene>,
    language: String,
    onSyncSceneDuration: (String) -> Unit,
    onSwapNarration: (Int, Int) -> Unit
) {
    if (scenes.isEmpty()) return
    val report = remember(scenes, language) {
        AudioTimelineEngine.analyzeNarrationSync(scenes, language)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = StudioCharcoal,
        border = BorderStroke(
            1.dp,
            if (report.missingNarrationCount > 0 || report.durationMismatchCount > 0) {
                StudioAmber.copy(alpha = 0.6f)
            } else {
                StudioEmerald.copy(alpha = 0.45f)
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("NARRATION SYNC CHECK", style = MaterialTheme.typography.labelMedium, color = StudioTeal)
                Text(
                    text = "Speech ~${"%.1f".format(report.totalEstimatedSpeechSec)}s / ${"%.1f".format(report.totalSceneDurationSec)}s",
                    style = MaterialTheme.typography.labelSmall,
                    color = StudioTextSecondary
                )
            }

            if (report.missingNarrationCount == 0 && report.durationMismatchCount == 0) {
                Text(
                    text = "All ${scenes.size} scenes have aligned narration timing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = StudioEmerald
                )
            } else {
                report.sceneStatuses.filter { it.warning != null }.take(4).forEach { st ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(StudioVoid, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = StudioAmber, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(5.dp))
                            Column {
                                Text(
                                    text = "S${st.orderIndex + 1} · ${st.sceneTitle}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StudioTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = st.warning.orEmpty(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                    color = StudioAmber
                                )
                            }
                        }
                        if (st.hasNarrationText) {
                            TextButton(
                                onClick = { onSyncSceneDuration(st.sceneId) },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                            ) {
                                Text("Fit", style = MaterialTheme.typography.labelSmall, color = StudioTeal)
                            }
                        }
                    }
                }
            }

            if (scenes.size >= 2) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Swap narration between adjacent scenes in Scenes tab",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                        color = StudioTextMuted
                    )
                    TextButton(
                        onClick = { onSwapNarration(0, 1) },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Text("Swap S1↔S2", style = MaterialTheme.typography.labelSmall, color = StudioAmber)
                    }
                }
            }
        }
    }
}

/**
 * Targeted Regeneration & AI Visual Direction Card for the currently selected scene.
 */
@Composable
fun SceneIntelligenceControlCard(
    scene: StudioScene,
    regeneratingTask: String?,
    onToggleApproved: () -> Unit,
    onRegenerateWholeScene: () -> Unit,
    onRegenerateNarration: () -> Unit,
    onRegenerateVisualDirection: () -> Unit,
    onRegenerateDialogue: () -> Unit,
    onApplyVisualPlan: () -> Unit
) {
    val busy = regeneratingTask != null
    val plan = scene.scenePlan

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = StudioCharcoal,
        border = BorderStroke(1.dp, if (plan.approved) StudioEmerald.copy(alpha = 0.65f) else StudioBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("scene_intelligence_card")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier.padding(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("AI SCENE INTELLIGENCE", style = MaterialTheme.typography.labelMedium, color = StudioAmber)
                Surface(
                    shape = RoundedCornerShape(5.dp),
                    color = if (plan.approved) StudioEmerald.copy(alpha = 0.2f) else StudioElevated,
                    border = BorderStroke(1.dp, if (plan.approved) StudioEmerald else StudioBorder),
                    modifier = Modifier
                        .clickable(onClick = onToggleApproved)
                        .testTag("scene_approve_lock_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = if (plan.approved) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Toggle approval lock",
                            tint = if (plan.approved) StudioEmerald else StudioTextSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (plan.approved) "APPROVED" else "DRAFT",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = if (plan.approved) StudioEmerald else StudioTextSecondary
                        )
                    }
                }
            }

            if (busy) {
                Text(
                    text = regeneratingTask.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = StudioTeal
                )
            }

            if (plan.visualDirection.isNotBlank() || plan.cameraSuggestion.isNotBlank() || plan.lightingSuggestion.isNotBlank()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StudioVoid, RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    if (plan.location.isNotBlank() || plan.timeOfDay.isNotBlank()) {
                        Text(
                            text = listOfNotNull(
                                plan.location.takeIf { it.isNotBlank() },
                                plan.timeOfDay.takeIf { it.isNotBlank() },
                                plan.weather.takeIf { it.isNotBlank() }
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.labelSmall,
                            color = StudioTeal
                        )
                    }
                    if (plan.visualDirection.isNotBlank()) {
                        Text(
                            text = plan.visualDirection,
                            style = MaterialTheme.typography.bodySmall,
                            color = StudioTextSecondary,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    val cineSummary = listOfNotNull(
                        plan.cameraSuggestion.takeIf { it.isNotBlank() }?.let { "Cam: $it" },
                        plan.lightingSuggestion.takeIf { it.isNotBlank() }?.let { "Light: $it" },
                        plan.atmosphereSuggestion.takeIf { it.isNotBlank() }?.let { "Atmo: $it" }
                    ).joinToString(" · ")
                    if (cineSummary.isNotBlank()) {
                        Text(
                            text = cineSummary,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                            color = StudioAmber
                        )
                        OutlinedButton(
                            onClick = onApplyVisualPlan,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            border = BorderStroke(1.dp, StudioTeal.copy(alpha = 0.65f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(30.dp)
                                .testTag("apply_ai_visual_plan_button")
                        ) {
                            Text("Apply Suggested Camera / Lighting / Atmosphere", style = MaterialTheme.typography.labelSmall, color = StudioTeal)
                        }
                    }
                }
            }

            // Targeted Regeneration Action Grid
            Text("Targeted Regeneration (preserves other scenes)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp), color = StudioTextMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onRegenerateWholeScene,
                    enabled = !busy,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    border = BorderStroke(1.dp, StudioAmber.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(30.dp)
                        .testTag("regen_scene_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = StudioAmber, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("Scene", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp), color = StudioAmber)
                }
                OutlinedButton(
                    onClick = onRegenerateNarration,
                    enabled = !busy,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    border = BorderStroke(1.dp, StudioTeal.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(30.dp)
                        .testTag("regen_narration_button")
                ) {
                    Text("Narration", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp), color = StudioTeal)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onRegenerateVisualDirection,
                    enabled = !busy,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    border = BorderStroke(1.dp, StudioBorder),
                    modifier = Modifier
                        .weight(1f)
                        .height(30.dp)
                        .testTag("regen_visual_dir_button")
                ) {
                    Text("Visual Dir", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp), color = StudioTextPrimary)
                }
                OutlinedButton(
                    onClick = onRegenerateDialogue,
                    enabled = !busy,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    border = BorderStroke(1.dp, StudioViolet.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(30.dp)
                        .testTag("regen_dialogue_button")
                ) {
                    Text("Dialogue", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp), color = StudioViolet)
                }
            }
        }
    }
}
