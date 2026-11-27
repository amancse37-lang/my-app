package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GraphicSuggestionsState
import com.example.data.MotionGraphicElement
import com.example.data.MotionGraphicsStudioTab
import com.example.data.SceneTransitionConfig
import com.example.data.StudioAsset
import com.example.data.StudioPresets
import com.example.data.StudioScene
import com.example.data.SubtitleCue
import com.example.data.SubtitleStyleConfig
import com.example.graphics.SubtitleEngine
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioAmberDim
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCharcoal
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioElevated
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioPanel
import com.example.ui.theme.StudioTeal
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.StudioViolet
import com.example.ui.theme.StudioVoid
import kotlin.math.roundToInt

@Composable
fun MotionGraphicsStudioPanel(
    scenes: List<StudioScene>,
    scene: StudioScene?,
    assets: List<StudioAsset>,
    activeTab: MotionGraphicsStudioTab,
    selectedGraphicId: String?,
    graphicSuggestionsState: GraphicSuggestionsState,
    lastSubtitleUsedRealAudio: Boolean,
    onSelectTab: (MotionGraphicsStudioTab) -> Unit,
    onSelectGraphic: (String?) -> Unit,
    onAddGraphicTemplate: (String, String?) -> Unit,
    onUpdateGraphic: (String, (MotionGraphicElement) -> MotionGraphicElement) -> Unit,
    onDuplicateGraphic: (String) -> Unit,
    onDeleteGraphic: (String) -> Unit,
    onAddGraphicKeyframeAtPlayhead: (String) -> Unit,
    onSaveGraphicTemplateToAssets: (String) -> Unit,
    onApplyGraphicTemplateAsset: (StudioAsset) -> Unit,
    onRequestAiGraphicSuggestions: () -> Unit,
    onApplyAiGraphicSuggestion: (String) -> Unit,
    onDismissAiGraphicSuggestion: (String) -> Unit,
    onGenerateSubtitles: () -> Unit,
    onAddSubtitleCue: () -> Unit,
    onUpdateSubtitleCue: (String, (SubtitleCue) -> SubtitleCue) -> Unit,
    onSplitSubtitleCue: (String) -> Unit,
    onDeleteSubtitleCue: (String) -> Unit,
    onFitSubtitles: () -> Unit,
    onUpdateSubtitleStyle: ((SubtitleStyleConfig) -> SubtitleStyleConfig) -> Unit,
    onApplySubtitleStylePreset: (String) -> Unit,
    onBuildExportSrt: () -> String,
    onNotifyMessage: (String) -> Unit,
    onRequestImportMapImage: (String) -> Unit,
    onSelectScene: (String) -> Unit,
    onMoveScene: (String, Int) -> Unit,
    onUpdateSceneDuration: (String, Double) -> Unit,
    onUpdateSceneTransition: (String, SceneTransitionConfig) -> Unit,
    onPreviewTransition: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioPanel)
    ) {
        // Phase 6 Sub-Tab Strip
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(StudioCharcoal)
                .border(1.dp, StudioBorder)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            MotionGraphicsStudioTab.entries.forEach { tab ->
                val selected = activeTab == tab
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (selected) StudioAmberDim else StudioPanel,
                    border = BorderStroke(1.dp, if (selected) StudioAmber else StudioBorder),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onSelectTab(tab) }
                        .testTag("mgfx_tab_${tab.name.lowercase()}")
                ) {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (selected) StudioAmber else StudioTextSecondary,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }
            }
        }

        if (scene == null && activeTab != MotionGraphicsStudioTab.FLOW_TRANSITIONS) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Select a scene to create motion graphics, subtitles, infographics, or maps.",
                    style = MaterialTheme.typography.bodySmall,
                    color = StudioTextSecondary
                )
            }
            return
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(10.dp)
        ) {
            when (activeTab) {
                MotionGraphicsStudioTab.TEMPLATES -> TemplatesAndAiTabContent(
                    scene = scene!!,
                    savedGraphicTemplates = assets.filter {
                        it.category.equals("MotionGraphic", ignoreCase = true) && it.graphicPayloadJson.isNotBlank()
                    },
                    graphicSuggestionsState = graphicSuggestionsState,
                    onAddTemplate = { tpl ->
                        onAddGraphicTemplate(tpl, null)
                        onSelectTab(MotionGraphicsStudioTab.TEXT_GFX)
                    },
                    onRequestAiSuggestions = onRequestAiGraphicSuggestions,
                    onApplyAiSuggestion = onApplyAiGraphicSuggestion,
                    onDismissAiSuggestion = onDismissAiGraphicSuggestion,
                    onApplySavedTemplate = { asset ->
                        onApplyGraphicTemplateAsset(asset)
                        onSelectTab(MotionGraphicsStudioTab.TEXT_GFX)
                    }
                )

                MotionGraphicsStudioTab.TEXT_GFX -> TextAndGraphicsTabContent(
                    scene = scene!!,
                    selectedGraphicId = selectedGraphicId,
                    onSelectGraphic = onSelectGraphic,
                    onAddQuickTitle = { onAddGraphicTemplate("LOWER_THIRD", null) },
                    onUpdateGraphic = onUpdateGraphic,
                    onDuplicateGraphic = onDuplicateGraphic,
                    onDeleteGraphic = onDeleteGraphic,
                    onAddKeyframeAtPlayhead = onAddGraphicKeyframeAtPlayhead,
                    onSaveToLibrary = onSaveGraphicTemplateToAssets
                )

                MotionGraphicsStudioTab.SUBTITLES -> SubtitlesTabContent(
                    scene = scene!!,
                    usedRealAudio = lastSubtitleUsedRealAudio,
                    onGenerateSubtitles = onGenerateSubtitles,
                    onAddCue = onAddSubtitleCue,
                    onUpdateCue = onUpdateSubtitleCue,
                    onSplitCue = onSplitSubtitleCue,
                    onDeleteCue = onDeleteSubtitleCue,
                    onFitSubtitles = onFitSubtitles,
                    onUpdateStyle = onUpdateSubtitleStyle,
                    onApplyStylePreset = onApplySubtitleStylePreset,
                    onBuildExportSrt = onBuildExportSrt,
                    onNotifyMessage = onNotifyMessage
                )

                MotionGraphicsStudioTab.INFOGRAPHICS -> InfographicsTabContent(
                    scene = scene!!,
                    selectedGraphicId = selectedGraphicId,
                    onSelectGraphic = onSelectGraphic,
                    onCreateInfographic = { infoType -> onAddGraphicTemplate("INFOGRAPHIC", infoType) },
                    onUpdateGraphic = onUpdateGraphic,
                    onDuplicateGraphic = onDuplicateGraphic,
                    onDeleteGraphic = onDeleteGraphic,
                    onSaveToLibrary = onSaveGraphicTemplateToAssets
                )

                MotionGraphicsStudioTab.MAPS -> DocumentaryMapsTabContent(
                    scene = scene!!,
                    selectedGraphicId = selectedGraphicId,
                    onSelectGraphic = onSelectGraphic,
                    onCreateMap = { onAddGraphicTemplate("MAP_GRAPHIC", null) },
                    onUpdateGraphic = onUpdateGraphic,
                    onDeleteGraphic = onDeleteGraphic,
                    onRequestImportMapImage = onRequestImportMapImage,
                    onSaveToLibrary = onSaveGraphicTemplateToAssets
                )

                MotionGraphicsStudioTab.FLOW_TRANSITIONS -> StoryFlowTransitionsTabContent(
                    scenes = scenes,
                    selectedSceneId = scene?.id,
                    onSelectScene = onSelectScene,
                    onMoveScene = onMoveScene,
                    onUpdateSceneDuration = onUpdateSceneDuration,
                    onUpdateTransition = onUpdateSceneTransition,
                    onPreviewTransition = onPreviewTransition
                )
            }
        }
    }
}

@Composable
private fun TemplatesAndAiTabContent(
    scene: StudioScene,
    savedGraphicTemplates: List<StudioAsset>,
    graphicSuggestionsState: GraphicSuggestionsState,
    onAddTemplate: (String) -> Unit,
    onRequestAiSuggestions: () -> Unit,
    onApplyAiSuggestion: (String) -> Unit,
    onDismissAiSuggestion: (String) -> Unit,
    onApplySavedTemplate: (StudioAsset) -> Unit
) {
    Text(
        text = "AI GRAPHIC SUGGESTIONS",
        style = MaterialTheme.typography.titleSmall,
        color = StudioViolet
    )
    Text(
        text = "Gemini analyzes \"${scene.title}\" narration and dialogue to suggest titles, lower thirds, infographics, and maps. Review & approve before adding.",
        style = MaterialTheme.typography.bodySmall,
        color = StudioTextSecondary
    )

    val isLoading = graphicSuggestionsState is GraphicSuggestionsState.Loading
    Button(
        onClick = onRequestAiSuggestions,
        enabled = !isLoading,
        colors = ButtonDefaults.buttonColors(containerColor = StudioViolet, contentColor = StudioVoid),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mgfx_ai_suggest_button")
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = StudioVoid)
            Spacer(Modifier.width(8.dp))
            Text("Analyzing Scene Script...", fontWeight = FontWeight.Bold)
        } else {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Suggest Documentary Graphics (AI)", fontWeight = FontWeight.Bold)
        }
    }

    if (graphicSuggestionsState is GraphicSuggestionsState.Error) {
        Text(
            text = graphicSuggestionsState.message,
            style = MaterialTheme.typography.labelSmall,
            color = StudioCoral
        )
    }

    val pendingSuggestions = scene.graphicSuggestions
    if (pendingSuggestions.isNotEmpty()) {
        pendingSuggestions.forEach { sug ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = StudioCharcoal,
                border = BorderStroke(1.dp, if (sug.applied) StudioEmerald else StudioViolet.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${sug.category} · ${sug.title}",
                            style = MaterialTheme.typography.labelMedium,
                            color = StudioAmber,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${String.format("%.1f", sug.suggestedStartSec)}s · ${String.format("%.1f", sug.suggestedDurationSec)}s",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = StudioTextMuted
                        )
                    }
                    Text(
                        text = sug.primaryText,
                        style = MaterialTheme.typography.bodySmall,
                        color = StudioTextPrimary
                    )
                    if (sug.secondaryText.isNotBlank()) {
                        Text(
                            text = sug.secondaryText,
                            style = MaterialTheme.typography.labelSmall,
                            color = StudioTextSecondary
                        )
                    }
                    if (sug.rationale.isNotBlank()) {
                        Text(
                            text = sug.rationale,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                            color = StudioTextMuted
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { onApplyAiSuggestion(sug.suggestionId) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (sug.applied) StudioEmerald else StudioTeal,
                                contentColor = StudioVoid
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (sug.applied) "Applied (Add Again)" else "Approve & Add",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        OutlinedButton(
                            onClick = { onDismissAiSuggestion(sug.suggestionId) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            border = BorderStroke(1.dp, StudioBorder)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss suggestion", tint = StudioTextSecondary, modifier = Modifier.size(13.dp))
                        }
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(4.dp))
    Text(
        text = "DOCUMENTARY GRAPHIC TEMPLATES (13)",
        style = MaterialTheme.typography.titleSmall,
        color = StudioAmber
    )
    Text(
        text = "Tap any template to insert an editable, animated overlay at the current scene playhead.",
        style = MaterialTheme.typography.bodySmall,
        color = StudioTextSecondary
    )

    StudioPresets.documentaryGraphicTemplates.chunked(2).forEach { pair ->
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            pair.forEach { (key, label) ->
                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = StudioCharcoal,
                    border = BorderStroke(1.dp, StudioBorder),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(7.dp))
                        .clickable { onAddTemplate(key) }
                        .testTag("mgfx_tpl_${key.lowercase()}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = StudioAmber, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = StudioTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            if (pair.size == 1) Spacer(Modifier.weight(1f))
        }
    }

    if (savedGraphicTemplates.isNotEmpty()) {
        Spacer(Modifier.height(4.dp))
        Text(
            text = "SAVED LIBRARY TEMPLATES (${savedGraphicTemplates.size})",
            style = MaterialTheme.typography.titleSmall,
            color = StudioTeal
        )
        savedGraphicTemplates.forEach { asset ->
            Surface(
                shape = RoundedCornerShape(7.dp),
                color = StudioCharcoal,
                border = BorderStroke(1.dp, StudioTeal.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = asset.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = StudioTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = asset.presetKey,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = StudioTextMuted
                        )
                    }
                    OutlinedButton(
                        onClick = { onApplySavedTemplate(asset) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        border = BorderStroke(1.dp, StudioTeal)
                    ) {
                        Text("Insert", style = MaterialTheme.typography.labelSmall, color = StudioTeal)
                    }
                }
            }
        }
    }
}

@Composable
private fun TextAndGraphicsTabContent(
    scene: StudioScene,
    selectedGraphicId: String?,
    onSelectGraphic: (String?) -> Unit,
    onAddQuickTitle: () -> Unit,
    onUpdateGraphic: (String, (MotionGraphicElement) -> MotionGraphicElement) -> Unit,
    onDuplicateGraphic: (String) -> Unit,
    onDeleteGraphic: (String) -> Unit,
    onAddKeyframeAtPlayhead: (String) -> Unit,
    onSaveToLibrary: (String) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "SCENE GRAPHICS (${scene.motionGraphics.size})",
            style = MaterialTheme.typography.titleSmall,
            color = StudioAmber
        )
        OutlinedButton(
            onClick = onAddQuickTitle,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            border = BorderStroke(1.dp, StudioAmber),
            modifier = Modifier.testTag("mgfx_add_quick_lower_third")
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = StudioAmber, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(3.dp))
            Text("Lower Third", style = MaterialTheme.typography.labelSmall, color = StudioAmber)
        }
    }

    if (scene.motionGraphics.isEmpty()) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = StudioCharcoal,
            border = BorderStroke(1.dp, StudioBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "No animated graphics in this scene yet. Add a template from Templates, Infographics, or Maps, or tap + Lower Third.",
                style = MaterialTheme.typography.bodySmall,
                color = StudioTextSecondary,
                modifier = Modifier.padding(10.dp)
            )
        }
        return
    }

    // Graphics Layer List
    scene.motionGraphics.forEach { g ->
        val isSel = g.graphicId == selectedGraphicId
        Surface(
            shape = RoundedCornerShape(7.dp),
            color = if (isSel) StudioAmberDim else StudioCharcoal,
            border = BorderStroke(1.dp, if (isSel) StudioAmber else StudioBorder),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelectGraphic(g.graphicId) }
                .testTag("mgfx_item_${g.graphicId}")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = g.title.ifBlank { g.templateType },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSel) StudioAmber else StudioTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${g.category} · ${String.format("%.1f", g.startSec)}s–${String.format("%.1f", g.startSec + g.durationSec)}s · ${g.entranceAnimation}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = StudioTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = { onUpdateGraphic(g.graphicId) { it.copy(visible = !it.visible) } },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (g.visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle graphic visibility",
                        tint = if (g.visible) StudioTeal else StudioTextMuted,
                        modifier = Modifier.size(15.dp)
                    )
                }
                IconButton(
                    onClick = { onDuplicateGraphic(g.graphicId) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Duplicate graphic",
                        tint = StudioTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
                IconButton(
                    onClick = { onDeleteGraphic(g.graphicId) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete graphic",
                        tint = StudioCoral,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }

    val active = scene.motionGraphics.firstOrNull { it.graphicId == selectedGraphicId }
        ?: scene.motionGraphics.firstOrNull()
        ?: return

    // Active Graphic Typography & Animation Editor
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
                Text(
                    text = "EDIT: ${active.templateType}",
                    style = MaterialTheme.typography.labelMedium,
                    color = StudioAmber
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(
                        onClick = { onAddKeyframeAtPlayhead(active.graphicId) },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        border = BorderStroke(1.dp, StudioViolet)
                    ) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = StudioViolet, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("KF", style = MaterialTheme.typography.labelSmall, color = StudioViolet)
                    }
                    OutlinedButton(
                        onClick = { onSaveToLibrary(active.graphicId) },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        border = BorderStroke(1.dp, StudioTeal)
                    ) {
                        Icon(Icons.Default.BookmarkAdd, contentDescription = null, tint = StudioTeal, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("Save", style = MaterialTheme.typography.labelSmall, color = StudioTeal)
                    }
                }
            }

            OutlinedTextField(
                value = active.primaryText,
                onValueChange = { v -> onUpdateGraphic(active.graphicId) { it.copy(primaryText = v.take(600)) } },
                label = { Text("Primary Text") },
                colors = mgfxFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mgfx_primary_text_input")
            )
            OutlinedTextField(
                value = active.secondaryText,
                onValueChange = { v -> onUpdateGraphic(active.graphicId) { it.copy(secondaryText = v.take(400)) } },
                label = { Text("Subtitle / Role / Secondary") },
                singleLine = true,
                colors = mgfxFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            if (active.category == "LOWER_THIRD" || active.templateType in setOf("NAME_DESIGNATION", "LOCATION_CAPTION", "DATE_TIME_CAPTION", "CREDITS")) {
                OutlinedTextField(
                    value = active.tertiaryText,
                    onValueChange = { v -> onUpdateGraphic(active.graphicId) { it.copy(tertiaryText = v.take(240)) } },
                    label = { Text("Location / Organization") },
                    singleLine = true,
                    colors = mgfxFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = active.quaternaryText,
                    onValueChange = { v -> onUpdateGraphic(active.graphicId) { it.copy(quaternaryText = v.take(160)) } },
                    label = { Text("Date / Tag / Era") },
                    singleLine = true,
                    colors = mgfxFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Lower Third Layout Preset Selector
            if (active.category == "LOWER_THIRD") {
                MgfxFieldLabel("Lower Third Layout")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    StudioPresets.lowerThirdLayoutPresets.forEach { (key, label) ->
                        FilterChip(
                            selected = active.layoutPreset == key,
                            onClick = { onUpdateGraphic(active.graphicId) { it.copy(layoutPreset = key) } },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            colors = mgfxChipColors()
                        )
                    }
                }
            }

            // Font Family
            MgfxFieldLabel("Font Family")
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                StudioPresets.fontFamilyOptions.forEach { (key, label) ->
                    FilterChip(
                        selected = active.fontFamilyKey == key,
                        onClick = { onUpdateGraphic(active.graphicId) { it.copy(fontFamilyKey = key) } },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        colors = mgfxChipColors()
                    )
                }
            }

            // Font Size & Background Opacity
            MgfxSliderRow(
                label = "Font Size",
                valueText = "${active.fontSizeSp.roundToInt()} sp",
                value = active.fontSizeSp.toFloat(),
                range = 9f..44f,
                onValueChange = { v -> onUpdateGraphic(active.graphicId) { it.copy(fontSizeSp = v.toDouble()) } }
            )
            MgfxSliderRow(
                label = "Card Backdrop Opacity",
                valueText = "${(active.backgroundOpacity * 100).roundToInt()}%",
                value = active.backgroundOpacity.toFloat(),
                range = 0f..1f,
                onValueChange = { v -> onUpdateGraphic(active.graphicId) { it.copy(backgroundOpacity = v.toDouble()) } }
            )
            MgfxSliderRow(
                label = "Letter Spacing",
                valueText = "${String.format("%.1f", active.letterSpacingSp)} sp",
                value = active.letterSpacingSp.toFloat(),
                range = 0f..6f,
                onValueChange = { v -> onUpdateGraphic(active.graphicId) { it.copy(letterSpacingSp = v.toDouble()) } }
            )

            // Alignment
            MgfxFieldLabel("Alignment")
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf("LEFT", "CENTER", "RIGHT").forEach { align ->
                    FilterChip(
                        selected = active.alignment == align,
                        onClick = { onUpdateGraphic(active.graphicId) { it.copy(alignment = align) } },
                        label = { Text(align, style = MaterialTheme.typography.labelSmall) },
                        colors = mgfxChipColors()
                    )
                }
            }

            // Text & Accent Color Swatches
            MgfxFieldLabel("Text Color")
            MgfxColorSwatchRow(
                selectedHex = active.textColorHex,
                onSelect = { hex -> onUpdateGraphic(active.graphicId) { it.copy(textColorHex = hex) } }
            )
            MgfxFieldLabel("Accent Color")
            MgfxColorSwatchRow(
                selectedHex = active.accentColorHex,
                onSelect = { hex -> onUpdateGraphic(active.graphicId) { it.copy(accentColorHex = hex) } }
            )

            // Timing Controls
            val maxStart = (scene.durationSec - 0.5).coerceAtLeast(0.5).toFloat()
            MgfxSliderRow(
                label = "Start Time",
                valueText = "${String.format("%.1f", active.startSec)}s",
                value = active.startSec.toFloat().coerceIn(0f, maxStart),
                range = 0f..maxStart,
                onValueChange = { v -> onUpdateGraphic(active.graphicId) { it.copy(startSec = v.toDouble()) } }
            )
            MgfxSliderRow(
                label = "Duration",
                valueText = "${String.format("%.1f", active.durationSec)}s",
                value = active.durationSec.toFloat().coerceIn(0.4f, scene.durationSec.coerceAtLeast(1.0).toFloat()),
                range = 0.4f..scene.durationSec.coerceAtLeast(1.5).toFloat(),
                onValueChange = { v -> onUpdateGraphic(active.graphicId) { it.copy(durationSec = v.toDouble()) } }
            )

            // Entrance & Exit Animation Presets
            MgfxFieldLabel("Entrance Animation")
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                StudioPresets.textEntranceAnimations.forEach { (key, label) ->
                    FilterChip(
                        selected = active.entranceAnimation == key,
                        onClick = { onUpdateGraphic(active.graphicId) { it.copy(entranceAnimation = key) } },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        colors = mgfxChipColors()
                    )
                }
            }

            MgfxFieldLabel("Exit Animation")
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                StudioPresets.textExitAnimations.forEach { (key, label) ->
                    FilterChip(
                        selected = active.exitAnimation == key,
                        onClick = { onUpdateGraphic(active.graphicId) { it.copy(exitAnimation = key) } },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        colors = mgfxChipColors()
                    )
                }
            }

            // Shadow & Camera Attachment Switches
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Text Drop Shadow", style = MaterialTheme.typography.labelSmall, color = StudioTextPrimary)
                Switch(
                    checked = active.shadowEnabled,
                    onCheckedChange = { checked -> onUpdateGraphic(active.graphicId) { it.copy(shadowEnabled = checked) } },
                    colors = SwitchDefaults.colors(checkedThumbColor = StudioVoid, checkedTrackColor = StudioAmber)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Attach to Camera (World Space)", style = MaterialTheme.typography.labelSmall, color = StudioTextPrimary)
                    Text(
                        text = if (active.attachToCamera) "Moves with scene pan/zoom/rotation" else "Pinned level on screen overlay",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = StudioTextMuted
                    )
                }
                Switch(
                    checked = active.attachToCamera,
                    onCheckedChange = { checked -> onUpdateGraphic(active.graphicId) { it.copy(attachToCamera = checked) } },
                    colors = SwitchDefaults.colors(checkedThumbColor = StudioVoid, checkedTrackColor = StudioTeal)
                )
            }
        }
    }
}

@Composable
private fun SubtitlesTabContent(
    scene: StudioScene,
    usedRealAudio: Boolean,
    onGenerateSubtitles: () -> Unit,
    onAddCue: () -> Unit,
    onUpdateCue: (String, (SubtitleCue) -> SubtitleCue) -> Unit,
    onSplitCue: (String) -> Unit,
    onDeleteCue: (String) -> Unit,
    onFitSubtitles: () -> Unit,
    onUpdateStyle: ((SubtitleStyleConfig) -> SubtitleStyleConfig) -> Unit,
    onApplyStylePreset: (String) -> Unit,
    onBuildExportSrt: () -> String,
    onNotifyMessage: (String) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val diagnostics = remember(scene.subtitleCues, scene.durationSec, usedRealAudio) {
        SubtitleEngine.analyzeSubtitleDiagnostics(scene.subtitleCues, scene.durationSec, usedRealAudio)
    }

    Text(
        text = "SUBTITLE & CAPTION ENGINE",
        style = MaterialTheme.typography.titleSmall,
        color = StudioAmber
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Button(
            onClick = onGenerateSubtitles,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = StudioAmber, contentColor = StudioVoid),
            modifier = Modifier
                .weight(1f)
                .testTag("mgfx_generate_subtitles_btn")
        ) {
            Text("Auto-Generate Cues", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
        OutlinedButton(
            onClick = onAddCue,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            border = BorderStroke(1.dp, StudioTeal)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = StudioTeal, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(3.dp))
            Text("Cue", style = MaterialTheme.typography.labelSmall, color = StudioTeal)
        }
    }

    // Honest timing source & overlap diagnostics banner
    Surface(
        shape = RoundedCornerShape(7.dp),
        color = StudioCharcoal,
        border = BorderStroke(
            1.dp,
            if (diagnostics.overlapCount > 0 || diagnostics.outOfBoundsCount > 0) StudioCoral else StudioBorder
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = diagnostics.summary,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (diagnostics.overlapCount > 0 || diagnostics.outOfBoundsCount > 0) StudioCoral else StudioEmerald
                )
                if (scene.subtitleCues.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onFitSubtitles,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        border = BorderStroke(1.dp, StudioAmber)
                    ) {
                        Text("Fix Overlaps", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = StudioAmber)
                    }
                }
            }
            Text(
                text = if (usedRealAudio) {
                    "Timing source: Attached Phase 5 audio clips (exact duration)."
                } else {
                    "Timing source: Estimated speech duration (attach TTS or imported audio clips for exact sync)."
                },
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = StudioTextMuted
            )
        }
    }

    // Export .SRT action
    OutlinedButton(
        onClick = {
            val srt = onBuildExportSrt()
            if (srt.isBlank()) {
                onNotifyMessage("No subtitle or narration text to export yet.")
            } else {
                clipboard.setText(AnnotatedString(srt))
                onNotifyMessage("Copied full project SubRip (.srt) captions to clipboard")
            }
        },
        border = BorderStroke(1.dp, StudioTeal),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mgfx_copy_srt_btn")
    ) {
        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = StudioTeal, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text("Copy Project Subtitles (.SRT)", style = MaterialTheme.typography.labelSmall, color = StudioTeal)
    }

    // Style Presets & Controls
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = StudioCharcoal,
        border = BorderStroke(1.dp, StudioBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier.padding(8.dp)
        ) {
            MgfxFieldLabel("Subtitle Style Preset")
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                StudioPresets.subtitleStylePresets.forEach { (key, label) ->
                    FilterChip(
                        selected = scene.subtitleStyle.stylePreset == key,
                        onClick = { onApplyStylePreset(key) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        colors = mgfxChipColors()
                    )
                }
            }

            MgfxFieldLabel("Vertical Position")
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf("BOTTOM", "CENTER", "TOP").forEach { pos ->
                    FilterChip(
                        selected = scene.subtitleStyle.verticalPosition == pos,
                        onClick = { onUpdateStyle { it.copy(verticalPosition = pos) } },
                        label = { Text(pos, style = MaterialTheme.typography.labelSmall) },
                        colors = mgfxChipColors()
                    )
                }
            }

            MgfxSliderRow(
                label = "Caption Size",
                valueText = "${scene.subtitleStyle.fontSizeSp.roundToInt()} sp",
                value = scene.subtitleStyle.fontSizeSp.toFloat(),
                range = 9f..22f,
                onValueChange = { v -> onUpdateStyle { it.copy(fontSizeSp = v.toDouble()) } }
            )
            MgfxSliderRow(
                label = "Box Opacity",
                valueText = "${(scene.subtitleStyle.backgroundOpacity * 100).roundToInt()}%",
                value = scene.subtitleStyle.backgroundOpacity.toFloat(),
                range = 0f..1f,
                onValueChange = { v -> onUpdateStyle { it.copy(backgroundOpacity = v.toDouble()) } }
            )
        }
    }

    // Subtitle Cue List
    scene.subtitleCues.sortedBy { it.startSec }.forEachIndexed { idx, cue ->
        Surface(
            shape = RoundedCornerShape(7.dp),
            color = StudioCharcoal,
            border = BorderStroke(1.dp, StudioBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.padding(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "#${idx + 1} · ${String.format("%.2f", cue.startSec)}s → ${String.format("%.2f", cue.endSec)}s",
                        style = MaterialTheme.typography.labelSmall,
                        color = StudioTeal
                    )
                    Row {
                        IconButton(
                            onClick = { onSplitCue(cue.cueId) },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(Icons.Default.Splitscreen, contentDescription = "Split subtitle cue", tint = StudioAmber, modifier = Modifier.size(14.dp))
                        }
                        IconButton(
                            onClick = { onDeleteCue(cue.cueId) },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete subtitle cue", tint = StudioCoral, modifier = Modifier.size(14.dp))
                        }
                    }
                }
                OutlinedTextField(
                    value = cue.text,
                    onValueChange = { v -> onUpdateCue(cue.cueId) { it.copy(text = v.take(300)) } },
                    label = { Text("Caption Text") },
                    colors = mgfxFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = cue.speakerName,
                        onValueChange = { v -> onUpdateCue(cue.cueId) { it.copy(speakerName = v.take(48)) } },
                        label = { Text("Speaker (opt)") },
                        singleLine = true,
                        colors = mgfxFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = {
                            onUpdateCue(cue.cueId) {
                                val nextStart = (it.startSec - 0.25).coerceAtLeast(0.0)
                                it.copy(startSec = nextStart, endSec = it.endSec.coerceAtLeast(nextStart + 0.3))
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        border = BorderStroke(1.dp, StudioBorder)
                    ) { Text("-0.25s", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = StudioTextSecondary) }
                    OutlinedButton(
                        onClick = {
                            onUpdateCue(cue.cueId) {
                                val nextEnd = (it.endSec + 0.25).coerceAtMost(scene.durationSec)
                                it.copy(endSec = nextEnd.coerceAtLeast(it.startSec + 0.3))
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        border = BorderStroke(1.dp, StudioBorder)
                    ) { Text("+0.25s", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = StudioTextSecondary) }
                }
            }
        }
    }
}

@Composable
internal fun MgfxFieldLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
        color = StudioTextSecondary
    )
}

@Composable
internal fun MgfxSliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary)
            Text(valueText, style = MaterialTheme.typography.labelSmall, color = StudioAmber)
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
            modifier = Modifier.height(26.dp)
        )
    }
}

@Composable
internal fun MgfxColorSwatchRow(
    selectedHex: String,
    onSelect: (String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        StudioPresets.graphicColorSwatches.forEach { hex ->
            val selected = selectedHex.equals(hex, ignoreCase = true)
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(parseHexColor(hex, StudioAmber))
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) Color.White else StudioBorder,
                        shape = CircleShape
                    )
                    .clickable { onSelect(hex) }
            )
        }
    }
}

@Composable
internal fun mgfxFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = StudioVoid,
    unfocusedContainerColor = StudioVoid,
    focusedBorderColor = StudioAmber,
    unfocusedBorderColor = StudioBorder,
    focusedTextColor = StudioTextPrimary,
    unfocusedTextColor = StudioTextPrimary,
    focusedLabelColor = StudioAmber,
    unfocusedLabelColor = StudioTextSecondary
)

@Composable
internal fun mgfxChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = StudioAmberDim,
    selectedLabelColor = StudioAmber,
    containerColor = StudioPanel,
    labelColor = StudioTextSecondary
)
