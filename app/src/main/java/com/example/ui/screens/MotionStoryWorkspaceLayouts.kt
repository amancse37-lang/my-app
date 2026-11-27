package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewTimeline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.compositor.SceneLayerType
import com.example.compositor.SelectedStageTarget
import com.example.data.CompactWorkspaceTab
import com.example.data.StudioScene
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioAmberDim
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCharcoal
import com.example.ui.theme.StudioPanel
import com.example.ui.theme.StudioTextSecondary
import com.example.viewmodel.StudioUiState
import com.example.viewmodel.StudioViewModel

@Composable
internal fun StudioExpandedWorkspace(
    state: StudioUiState,
    activePlaybackScene: StudioScene?,
    sceneProgress: Double,
    activeSceneTimeSec: Double,
    viewModel: StudioViewModel,
    dialogState: StudioDialogState,
    onSelectStageTarget: (SelectedStageTarget?) -> Unit,
    onImportVisualElement: (Uri, SceneLayerType) -> Unit,
    onRequestImportAudio: (String, String?) -> Unit,
    onRequestImportMapImage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .weight(0.62f)
                .fillMaxWidth()
        ) {
            StudioLeftSidebar(
                state = state,
                viewModel = viewModel,
                onOpenAddCharacter = { charToEdit -> dialogState.editingCharacter = true to charToEdit },
                onRenameCharacter = { charToRename -> dialogState.renamingCharacter = charToRename },
                onRequestDeleteCharacter = { charToDelete -> dialogState.deletingCharacter = charToDelete },
                onOpenCharacterGenerator = { charToGen ->
                    dialogState.openCharacterGenerator(viewModel, charToGen)
                },
                onOpenAddAsset = { cat -> dialogState.addingAssetCategory = cat },
                onOpenBackgroundGenerator = { dialogState.openDefaultBackgroundGenerator(viewModel) },
                onOpenPropGenerator = {
                    dialogState.openDefaultPropGenerator(viewModel, SceneLayerType.PROPS)
                },
                onOpenSceneSuggestions = dialogState::openSceneSuggestions,
                onRequestImportAudio = onRequestImportAudio,
                onRequestImportMapImage = onRequestImportMapImage,
                modifier = Modifier
                    .width(305.dp)
                    .fillMaxHeight()
            )

            StudioPreviewStage(
                state = state,
                activePlaybackScene = activePlaybackScene,
                sceneProgress = sceneProgress,
                activeSceneTimeSec = activeSceneTimeSec,
                viewModel = viewModel,
                onSelectStageTarget = onSelectStageTarget,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )

            StudioInspectorPane(
                state = state,
                viewModel = viewModel,
                onImportVisualElement = onImportVisualElement,
                onOpenCharacterGenerator = { charToGen ->
                    dialogState.openCharacterGenerator(viewModel, charToGen)
                },
                onOpenBackgroundGenerator = { dialogState.openDefaultBackgroundGenerator(viewModel) },
                onOpenPropGenerator = { layer ->
                    dialogState.openDefaultPropGenerator(viewModel, layer)
                },
                onOpenSceneSuggestions = dialogState::openSceneSuggestions,
                modifier = Modifier
                    .width(280.dp)
                    .fillMaxHeight()
            )
        }

        StudioBottomTimeline(
            state = state,
            viewModel = viewModel,
            switchToPanelsOnCompact = false,
            modifier = Modifier
                .weight(0.38f)
                .fillMaxWidth()
                .navigationBarsPadding()
        )
    }
}

@Composable
internal fun StudioCompactWorkspace(
    state: StudioUiState,
    activePlaybackScene: StudioScene?,
    sceneProgress: Double,
    activeSceneTimeSec: Double,
    viewModel: StudioViewModel,
    dialogState: StudioDialogState,
    onSelectStageTarget: (SelectedStageTarget?) -> Unit,
    onImportVisualElement: (Uri, SceneLayerType) -> Unit,
    onRequestImportAudio: (String, String?) -> Unit,
    onRequestImportMapImage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        StudioPreviewStage(
            state = state,
            activePlaybackScene = activePlaybackScene,
            sceneProgress = sceneProgress,
            activeSceneTimeSec = activeSceneTimeSec,
            viewModel = viewModel,
            onSelectStageTarget = onSelectStageTarget,
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.40f)
        )

        CompactWorkspaceTabBar(
            activeTab = state.compactWorkspaceTab,
            onSelectTab = viewModel::setCompactWorkspaceTab
        )

        Box(
            modifier = Modifier
                .weight(0.60f)
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            when (state.compactWorkspaceTab) {
                CompactWorkspaceTab.TIMELINE -> {
                    StudioBottomTimeline(
                        state = state,
                        viewModel = viewModel,
                        switchToPanelsOnCompact = true,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                CompactWorkspaceTab.PANELS -> {
                    StudioLeftSidebar(
                        state = state,
                        viewModel = viewModel,
                        onOpenAddCharacter = { charToEdit -> dialogState.editingCharacter = true to charToEdit },
                        onRenameCharacter = { charToRename -> dialogState.renamingCharacter = charToRename },
                        onRequestDeleteCharacter = { charToDelete -> dialogState.deletingCharacter = charToDelete },
                        onOpenCharacterGenerator = { charToGen ->
                            dialogState.openCharacterGenerator(viewModel, charToGen)
                        },
                        onOpenAddAsset = { cat -> dialogState.addingAssetCategory = cat },
                        onOpenBackgroundGenerator = { dialogState.openDefaultBackgroundGenerator(viewModel) },
                        onOpenPropGenerator = {
                            dialogState.openDefaultPropGenerator(viewModel, SceneLayerType.PROPS)
                        },
                        onOpenSceneSuggestions = dialogState::openSceneSuggestions,
                        onRequestImportAudio = onRequestImportAudio,
                        onRequestImportMapImage = onRequestImportMapImage,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                CompactWorkspaceTab.INSPECTOR -> {
                    StudioInspectorPane(
                        state = state,
                        viewModel = viewModel,
                        onImportVisualElement = onImportVisualElement,
                        onOpenCharacterGenerator = { charToGen ->
                            dialogState.openCharacterGenerator(viewModel, charToGen)
                        },
                        onOpenBackgroundGenerator = { dialogState.openDefaultBackgroundGenerator(viewModel) },
                        onOpenPropGenerator = { layer ->
                            dialogState.openDefaultPropGenerator(viewModel, layer)
                        },
                        onOpenSceneSuggestions = dialogState::openSceneSuggestions,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
internal fun CompactWorkspaceTabBar(
    activeTab: CompactWorkspaceTab,
    onSelectTab: (CompactWorkspaceTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = StudioCharcoal,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, StudioBorder)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            CompactWorkspaceTab.entries.forEach { tab ->
                val selected = activeTab == tab
                val icon = when (tab) {
                    CompactWorkspaceTab.TIMELINE -> Icons.Default.ViewTimeline
                    CompactWorkspaceTab.PANELS -> Icons.Default.DashboardCustomize
                    CompactWorkspaceTab.INSPECTOR -> Icons.Default.Tune
                }
                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = if (selected) StudioAmberDim else StudioPanel,
                    border = BorderStroke(1.dp, if (selected) StudioAmber else StudioBorder),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .clickable { onSelectTab(tab) }
                        .testTag("compact_tab_${tab.name.lowercase()}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = tab.label,
                            tint = if (selected) StudioAmber else StudioTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) StudioAmber else StudioTextSecondary
                        )
                    }
                }
            }
        }
    }
}
