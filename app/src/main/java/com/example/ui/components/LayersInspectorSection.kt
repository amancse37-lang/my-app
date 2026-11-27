package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flip
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.compositor.SceneCompositor
import com.example.compositor.SceneLayerType
import com.example.compositor.SelectedStageTarget
import com.example.data.SceneVisualElement
import com.example.data.StudioAsset
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
import com.example.ui.theme.StudioVoid

private val elementPlaceableLayers = listOf(
    SceneLayerType.PROPS,
    SceneLayerType.MIDGROUND,
    SceneLayerType.FOREGROUND,
    SceneLayerType.BACKGROUND
)

@Composable
fun LayersInspectorSection(
    scene: StudioScene,
    assets: List<StudioAsset>,
    selectedStageTarget: SelectedStageTarget?,
    onSelectStageTarget: (SelectedStageTarget?) -> Unit,
    onToggleLayerVisibility: (SceneLayerType) -> Unit,
    onMoveLayerOrder: (SceneLayerType, Int) -> Unit,
    onUpdateLayerOpacity: (SceneLayerType, Double) -> Unit,
    onUpdateLayerParallax: (SceneLayerType, Double) -> Unit = { _, _ -> },
    onAddVisualElementFromAsset: (StudioAsset, SceneLayerType) -> Unit,
    onImportVisualElement: (Uri, SceneLayerType) -> Unit,
    onRemoveVisualElement: (String) -> Unit,
    onUpdateVisualElement: (String, Boolean, (SceneVisualElement) -> SceneVisualElement) -> Unit,
    onNudgeElementZIndex: (String, Int) -> Unit,
    onOpenPropGenerator: (SceneLayerType) -> Unit = {},
    onOpenSceneSuggestions: () -> Unit = {}
) {
    val normalizedLayers = remember(scene.layerStates) {
        SceneCompositor.normalizeLayerStates(scene.layerStates)
    }
    // Display front-to-back (highest order at top, matching 2D NLE/compositor convention)
    val displayLayers = remember(normalizedLayers) {
        normalizedLayers.sortedByDescending { it.order }
    }
    var targetImportLayer by remember { mutableStateOf(SceneLayerType.PROPS) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onImportVisualElement(uri, targetImportLayer)
        }
    }

    val renderableAssets = remember(assets) {
        assets.filter { asset ->
            SceneCompositor.hasRenderableVisualAsset(
                elementUri = asset.localUri,
                elementPresetKey = asset.presetKey,
                linkedAsset = asset
            )
        }
    }

    // 1. 6-LAYER COMPOSITING STACK
    Text(
        text = "SCENE LAYER STACK (FRONT TO BACK)",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
        color = StudioTextSecondary
    )
    Text(
        text = "Toggle visibility, reorder depth stack, or adjust opacity & active 2D parallax depth.",
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
        color = StudioTextMuted
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        displayLayers.forEach { layerState ->
            val layerType = SceneLayerType.fromKeyOrDefault(layerState.layerKey, SceneLayerType.BACKGROUND)
            val canMoveForward = layerState.order < normalizedLayers.lastIndex
            val canMoveBack = layerState.order > 0
            val charCount = scene.effectiveCast().count {
                SceneLayerType.fromKeyOrDefault(it.layer, SceneLayerType.CHARACTERS) == layerType
            }
            val elCount = scene.sceneElements.count {
                SceneLayerType.fromKeyOrDefault(it.layer, SceneLayerType.PROPS) == layerType
            }
            val summaryText = buildString {
                if (layerType == SceneLayerType.BACKGROUND) append("base")
                if (layerType == SceneLayerType.OVERLAY) append("fx/mg/sub")
                if (charCount > 0) {
                    if (isNotEmpty()) append(" · ")
                    append("${charCount}ch")
                }
                if (elCount > 0) {
                    if (isNotEmpty()) append(" · ")
                    append("${elCount}el")
                }
                if (isEmpty()) append("empty")
                append(" · depth ${String.format("%.2f", layerState.parallaxFactor)}x")
            }

            Surface(
                shape = RoundedCornerShape(7.dp),
                color = if (layerState.visible) StudioCharcoal else StudioVoid.copy(alpha = 0.6f),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (layerState.visible) StudioBorder else StudioBorder.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("layer_row_${layerType.name.lowercase()}")
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (layerState.visible) StudioAmberDim else StudioElevated,
                            border = BorderStroke(
                                1.dp,
                                if (layerState.visible) StudioAmber.copy(alpha = 0.6f) else StudioBorder
                            )
                        ) {
                            Text(
                                text = layerType.shortCode,
                                style = StudioTimecodeStyle.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                color = if (layerState.visible) StudioAmber else StudioTextMuted,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = layerType.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (layerState.visible) StudioTextPrimary else StudioTextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = summaryText,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                color = StudioTextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = { onMoveLayerOrder(layerType, +1) },
                            enabled = canMoveForward,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("layer_up_${layerType.name.lowercase()}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Move ${layerType.label} forward",
                                tint = if (canMoveForward) StudioTextSecondary else StudioTextMuted.copy(alpha = 0.35f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        IconButton(
                            onClick = { onMoveLayerOrder(layerType, -1) },
                            enabled = canMoveBack,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("layer_down_${layerType.name.lowercase()}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "Move ${layerType.label} backward",
                                tint = if (canMoveBack) StudioTextSecondary else StudioTextMuted.copy(alpha = 0.35f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        IconButton(
                            onClick = { onToggleLayerVisibility(layerType) },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("layer_vis_${layerType.name.lowercase()}")
                        ) {
                            Icon(
                                imageVector = if (layerState.visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle ${layerType.label} visibility",
                                tint = if (layerState.visible) StudioTeal else StudioTextMuted,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    if (layerState.visible) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Opacity ${(layerState.opacity * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                color = StudioTextMuted,
                                modifier = Modifier.width(68.dp)
                            )
                            Slider(
                                value = layerState.opacity.toFloat().coerceIn(0f, 1f),
                                onValueChange = { v -> onUpdateLayerOpacity(layerType, v.toDouble()) },
                                valueRange = 0f..1f,
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
                        if (layerType != SceneLayerType.OVERLAY) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Depth ${String.format("%.2f", layerState.parallaxFactor)}x",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                    color = StudioTextMuted,
                                    modifier = Modifier.width(68.dp)
                                )
                                Slider(
                                    value = layerState.parallaxFactor.toFloat().coerceIn(0f, 2f),
                                    onValueChange = { v -> onUpdateLayerParallax(layerType, v.toDouble()) },
                                    valueRange = 0f..2f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = StudioAmber,
                                        activeTrackColor = StudioAmber,
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
        }
    }

    // 2. ADD VISUAL SCENE ELEMENT (REAL PNG/IMAGE ASSETS ONLY)
    Spacer(Modifier.height(4.dp))
    Text(
        text = "ADD VISUAL PROP / DEPTH ELEMENT",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
        color = StudioTextSecondary
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        elementPlaceableLayers.forEach { layer ->
            FilterChip(
                selected = targetImportLayer == layer,
                onClick = { targetImportLayer = layer },
                label = { Text(layer.label, style = MaterialTheme.typography.labelSmall) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = StudioTealDim,
                    selectedLabelColor = StudioTeal
                )
            )
        }
    }

    Button(
        onClick = { onOpenPropGenerator(targetImportLayer) },
        colors = ButtonDefaults.buttonColors(containerColor = StudioAmber, contentColor = StudioVoid),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("open_ai_prop_generator_button")
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "Generate AI Prop for ${targetImportLayer.shortCode}",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
        )
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedButton(
            onClick = {
                imagePicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            border = BorderStroke(1.dp, StudioTeal),
            modifier = Modifier
                .weight(1f)
                .testTag("import_visual_element_button")
        ) {
            Icon(
                imageVector = Icons.Default.AddPhotoAlternate,
                contentDescription = null,
                tint = StudioTeal,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "Import PNG",
                style = MaterialTheme.typography.labelSmall,
                color = StudioTeal
            )
        }
        OutlinedButton(
            onClick = onOpenSceneSuggestions,
            border = BorderStroke(1.dp, StudioAmber.copy(alpha = 0.7f)),
            modifier = Modifier
                .weight(1f)
                .testTag("layers_scene_suggestions_button")
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = StudioAmber,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "AI Suggest",
                style = MaterialTheme.typography.labelSmall,
                color = StudioAmber
            )
        }
    }

    if (renderableAssets.isNotEmpty()) {
        Text(
            text = "PLACE FROM VISUAL ASSETS",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = StudioTextMuted
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            renderableAssets.forEach { asset ->
                val chipTitle = when {
                    asset.hasTransparency -> "${asset.name} (α)"
                    asset.imageBase64.isNotBlank() -> "${asset.name} (AI)"
                    else -> asset.name
                }
                FilterChip(
                    selected = false,
                    onClick = { onAddVisualElementFromAsset(asset, targetImportLayer) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = StudioAmber,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    label = { Text(chipTitle, style = MaterialTheme.typography.labelSmall, maxLines = 1) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = StudioAmberDim,
                        selectedLabelColor = StudioAmber
                    ),
                    modifier = Modifier.testTag("place_asset_${asset.id}")
                )
            }
        }
    }

    // 3. STAGED VISUAL ELEMENTS LIST
    Spacer(Modifier.height(4.dp))
    Text(
        text = "STAGED VISUAL ELEMENTS (${scene.sceneElements.size})",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
        color = StudioTextSecondary
    )

    val selectedElementId = (selectedStageTarget as? SelectedStageTarget.ElementTarget)?.elementId
    val activeElement = scene.sceneElements.firstOrNull { it.elementId == selectedElementId }
        ?: scene.sceneElements.firstOrNull()

    if (scene.sceneElements.isEmpty()) {
        Surface(
            shape = RoundedCornerShape(7.dp),
            color = StudioCharcoal,
            border = BorderStroke(1.dp, StudioBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "No visual props or depth elements placed yet. Import a PNG/JPEG image or pick an existing visual asset above to composite it onto the 16:9 stage.",
                style = MaterialTheme.typography.bodySmall,
                color = StudioTextMuted,
                modifier = Modifier.padding(10.dp)
            )
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            scene.sceneElements.forEach { el ->
                val isSelected = el.elementId == activeElement?.elementId
                val elLayer = SceneLayerType.fromKeyOrDefault(el.layer, SceneLayerType.PROPS)
                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = if (isSelected) StudioElevated else StudioCharcoal,
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) StudioTeal else StudioBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSelectStageTarget(SelectedStageTarget.ElementTarget(el.elementId))
                        }
                        .testTag("scene_element_item_${el.elementId}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = StudioTealDim,
                            border = BorderStroke(1.dp, StudioTeal.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = elLayer.shortCode,
                                style = StudioTimecodeStyle.copy(fontSize = 8.5.sp),
                                color = StudioTeal,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(Modifier.width(7.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = el.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (el.visible) StudioTextPrimary else StudioTextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${elLayer.label} · Z:${el.zIndex} · ${String.format("%.2f", el.scale)}x",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = StudioTextMuted
                            )
                        }
                        IconButton(
                            onClick = {
                                onUpdateVisualElement(el.elementId, true) { it.copy(visible = !it.visible) }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (el.visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle element visibility",
                                tint = if (el.visible) StudioTeal else StudioTextMuted,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        IconButton(
                            onClick = { onRemoveVisualElement(el.elementId) },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("remove_element_${el.elementId}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove visual element",
                                tint = StudioCoral,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // 4. SELECTED VISUAL ELEMENT TRANSFORM INSPECTOR
    if (activeElement != null) {
        val elId = activeElement.elementId
        Spacer(Modifier.height(4.dp))
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = StudioTealDim.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, StudioTeal.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(10.dp)
            ) {
                Text(
                    text = "ELEMENT TRANSFORM · ${activeElement.name}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = StudioTeal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Layer assignment chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    elementPlaceableLayers.forEach { layer ->
                        val assigned = activeElement.layer.equals(layer.name, ignoreCase = true)
                        FilterChip(
                            selected = assigned,
                            onClick = {
                                onUpdateVisualElement(elId, true) { it.copy(layer = layer.name) }
                            },
                            label = { Text(layer.shortCode, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StudioAmberDim,
                                selectedLabelColor = StudioAmber
                            )
                        )
                    }
                }

                // Quick toggles: Visible, Flip H, Z-Order
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = activeElement.flipX,
                        onClick = {
                            onUpdateVisualElement(elId, true) { it.copy(flipX = !it.flipX) }
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Flip, contentDescription = null, modifier = Modifier.size(13.dp))
                        },
                        label = { Text("Flip H", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StudioAmberDim,
                            selectedLabelColor = StudioAmber
                        )
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "Z:${activeElement.zIndex}",
                        style = StudioTimecodeStyle.copy(fontSize = 10.sp),
                        color = StudioTextSecondary
                    )
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = StudioPanel,
                        border = BorderStroke(1.dp, StudioBorder),
                        modifier = Modifier.clickable { onNudgeElementZIndex(elId, -1) }
                    ) {
                        Text(
                            text = "Back",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = StudioTextSecondary,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = StudioPanel,
                        border = BorderStroke(1.dp, StudioBorder),
                        modifier = Modifier.clickable { onNudgeElementZIndex(elId, +1) }
                    ) {
                        Text(
                            text = "Front",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = StudioAmber,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        )
                    }
                }

                ElementSliderRow(
                    label = "Position X",
                    valueText = "${activeElement.posX.toInt()} px",
                    value = activeElement.posX.toFloat(),
                    range = -480f..480f,
                    onValueChange = { v ->
                        onUpdateVisualElement(elId, false) { it.copy(posX = v.toDouble()) }
                    }
                )
                ElementSliderRow(
                    label = "Position Y",
                    valueText = "${activeElement.posY.toInt()} px",
                    value = activeElement.posY.toFloat(),
                    range = -270f..270f,
                    onValueChange = { v ->
                        onUpdateVisualElement(elId, false) { it.copy(posY = v.toDouble()) }
                    }
                )
                ElementSliderRow(
                    label = "Scale",
                    valueText = "${String.format("%.2f", activeElement.scale)}x",
                    value = activeElement.scale.toFloat(),
                    range = 0.25f..3.0f,
                    onValueChange = { v ->
                        onUpdateVisualElement(elId, false) { it.copy(scale = v.toDouble()) }
                    }
                )
                ElementSliderRow(
                    label = "Rotation",
                    valueText = "${activeElement.rotationDeg.toInt()}°",
                    value = activeElement.rotationDeg.toFloat(),
                    range = -180f..180f,
                    onValueChange = { v ->
                        onUpdateVisualElement(elId, false) { it.copy(rotationDeg = v.toDouble()) }
                    }
                )
                ElementSliderRow(
                    label = "Opacity",
                    valueText = "${(activeElement.opacity * 100).toInt()}%",
                    value = activeElement.opacity.toFloat(),
                    range = 0f..1f,
                    onValueChange = { v ->
                        onUpdateVisualElement(elId, false) { it.copy(opacity = v.toDouble()) }
                    }
                )
            }
        }
    }
}

@Composable
private fun ElementSliderRow(
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
                thumbColor = StudioTeal,
                activeTrackColor = StudioTeal,
                inactiveTrackColor = StudioElevated
            ),
            modifier = Modifier.height(28.dp)
        )
    }
}
