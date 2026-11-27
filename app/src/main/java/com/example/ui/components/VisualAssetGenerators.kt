package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.compositor.SceneLayerType
import com.example.data.BackgroundGenerationRequest
import com.example.data.PropGenerationRequest
import com.example.data.SceneSuggestionsState
import com.example.data.StudioPresets
import com.example.data.StudioScene
import com.example.data.SuggestedBackgroundPlan
import com.example.data.SuggestedPropPlan
import com.example.data.VisualAssetGenState
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
import com.example.ui.theme.StudioVoid

private val propTargetLayers = listOf(
    SceneLayerType.PROPS,
    SceneLayerType.MIDGROUND,
    SceneLayerType.FOREGROUND
)

@Composable
fun BackgroundGeneratorDialog(
    sceneTitle: String,
    initialRequest: BackgroundGenerationRequest,
    genState: VisualAssetGenState,
    onBuildSummaryPrompt: (BackgroundGenerationRequest) -> String,
    onGenerate: (BackgroundGenerationRequest) -> Unit,
    onReject: () -> Unit,
    onSaveToLibrary: (name: String, visualStyle: String, tags: String, applyToScene: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var assetName by remember(initialRequest) { mutableStateOf(initialRequest.assetName) }
    var location by remember(initialRequest) { mutableStateOf(initialRequest.location) }
    var eraContext by remember(initialRequest) { mutableStateOf(initialRequest.eraContext) }
    var timeOfDay by remember(initialRequest) { mutableStateOf(initialRequest.timeOfDay) }
    var weather by remember(initialRequest) { mutableStateOf(initialRequest.weather) }
    var visualStyle by remember(initialRequest) { mutableStateOf(initialRequest.visualStyle) }
    var moodAtmosphere by remember(initialRequest) { mutableStateOf(initialRequest.moodAtmosphere) }
    var customPrompt by remember(initialRequest) { mutableStateOf(initialRequest.customPrompt) }
    var variationSeed by remember(initialRequest) { mutableIntStateOf(initialRequest.variationSeed.coerceAtLeast(1)) }

    fun currentRequest(seed: Int = variationSeed): BackgroundGenerationRequest {
        return initialRequest.copy(
            assetName = assetName,
            location = location,
            eraContext = eraContext,
            timeOfDay = timeOfDay,
            weather = weather,
            visualStyle = visualStyle,
            moodAtmosphere = moodAtmosphere,
            customPrompt = customPrompt,
            variationSeed = seed
        )
    }

    val isGenerating = genState is VisualAssetGenState.Generating

    Dialog(
        onDismissRequest = { if (!isGenerating) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = StudioPanel,
            border = BorderStroke(1.dp, StudioBorder),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 720.dp)
                .testTag("background_generator_dialog")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StudioCharcoal)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Landscape,
                            contentDescription = null,
                            tint = StudioAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "AI SCENE BACKGROUND GENERATOR",
                                style = MaterialTheme.typography.titleSmall,
                                color = StudioAmber
                            )
                            Text(
                                text = "Scene: ${sceneTitle.ifBlank { "Current Scene" }} · 16:9 Widescreen",
                                style = MaterialTheme.typography.labelSmall,
                                color = StudioTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        enabled = !isGenerating,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = StudioTextSecondary)
                    }
                }

                // Body
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp)
                ) {
                    // Generated Preview Panel
                    when (genState) {
                        is VisualAssetGenState.Generating -> {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = StudioCharcoal,
                                border = BorderStroke(1.dp, StudioAmber.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    CircularProgressIndicator(color = StudioAmber, strokeWidth = 2.5.dp)
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        text = genState.label,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = StudioTextPrimary
                                    )
                                    Text(
                                        text = "Rendering borderless 2D documentary artwork via Firebase AI Logic",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StudioTextMuted
                                    )
                                }
                            }
                        }

                        is VisualAssetGenState.PreviewReady -> {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = StudioVoid,
                                border = BorderStroke(1.5.dp, StudioTeal),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(16f / 9f)
                                            .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                                    ) {
                                        Image(
                                            bitmap = genState.asset.bitmap.asImageBitmap(),
                                            contentDescription = "Generated scene background preview",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Surface(
                                            color = StudioVoid.copy(alpha = 0.82f),
                                            shape = RoundedCornerShape(5.dp),
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(6.dp)
                                        ) {
                                            Text(
                                                text = genState.asset.formatLabel,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                color = StudioTeal,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    // Accept / Save / Reject Action Bar
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(StudioCharcoal)
                                            .padding(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = onReject,
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            border = BorderStroke(1.dp, StudioCoral.copy(alpha = 0.6f)),
                                            modifier = Modifier.testTag("reject_bg_button")
                                        ) {
                                            Text("Reject", style = MaterialTheme.typography.labelSmall, color = StudioCoral)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                val tags = listOf("ai", "background", timeOfDay.lowercase(), weather.lowercase())
                                                    .filter { it.isNotBlank() && it != "indoor / n/a" }
                                                    .joinToString(",")
                                                onSaveToLibrary(assetName, visualStyle, tags, false)
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            border = BorderStroke(1.dp, StudioTeal),
                                            modifier = Modifier.testTag("save_bg_library_button")
                                        ) {
                                            Icon(Icons.Default.BookmarkAdd, contentDescription = null, tint = StudioTeal, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = if (genState.savedAssetId != null) "Saved" else "Save",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = StudioTeal
                                            )
                                        }
                                        Button(
                                            onClick = {
                                                val tags = listOf("ai", "background", timeOfDay.lowercase(), weather.lowercase())
                                                    .filter { it.isNotBlank() && it != "indoor / n/a" }
                                                    .joinToString(",")
                                                onSaveToLibrary(assetName, visualStyle, tags, true)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = StudioAmber, contentColor = StudioVoid),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("apply_bg_scene_button")
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Apply to Scene", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        is VisualAssetGenState.Error -> {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = genState.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        VisualAssetGenState.Idle -> Unit
                    }

                    // Asset Name & Location
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = assetName,
                            onValueChange = { assetName = it.take(120) },
                            label = { Text("Asset Name", style = MaterialTheme.typography.labelSmall) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = StudioTextPrimary),
                            colors = generatorTextFieldColors(),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it.take(160) },
                            label = { Text("Location / Setting", style = MaterialTheme.typography.labelSmall) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = StudioTextPrimary),
                            colors = generatorTextFieldColors(),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Era / Context & Mood
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = eraContext,
                            onValueChange = { eraContext = it.take(160) },
                            label = { Text("Historical / Era Context", style = MaterialTheme.typography.labelSmall) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = StudioTextPrimary),
                            colors = generatorTextFieldColors(),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = moodAtmosphere,
                            onValueChange = { moodAtmosphere = it.take(200) },
                            label = { Text("Mood & Atmosphere", style = MaterialTheme.typography.labelSmall) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = StudioTextPrimary),
                            colors = generatorTextFieldColors(),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Time of Day Chips
                    GeneratorChipGroup(
                        label = "TIME OF DAY",
                        options = StudioPresets.timeOfDayOptions,
                        selected = timeOfDay,
                        onSelect = { timeOfDay = it }
                    )

                    // Weather Chips
                    GeneratorChipGroup(
                        label = "WEATHER & ATMOSPHERE",
                        options = StudioPresets.weatherOptions,
                        selected = weather,
                        onSelect = { weather = it },
                        useTeal = true
                    )

                    // Visual Art Style Chips
                    GeneratorChipGroup(
                        label = "2D VISUAL ART STYLE",
                        options = StudioPresets.characterVisualStyles,
                        selected = visualStyle,
                        onSelect = { visualStyle = it }
                    )

                    // Editable Visual Prompt
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "VISUAL PROMPT (EDITABLE)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = StudioTextSecondary
                        )
                        TextButton(
                            onClick = { customPrompt = onBuildSummaryPrompt(currentRequest()) },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = StudioTeal, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Sync from Fields", style = MaterialTheme.typography.labelSmall, color = StudioTeal)
                        }
                    }

                    OutlinedTextField(
                        value = customPrompt,
                        onValueChange = { customPrompt = it.take(900) },
                        placeholder = {
                            Text(
                                "Describe architecture, landscape depth, lighting, and atmosphere...",
                                style = MaterialTheme.typography.bodySmall,
                                color = StudioTextMuted
                            )
                        },
                        minLines = 3,
                        maxLines = 5,
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = StudioTextPrimary),
                        colors = generatorTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bg_prompt_input")
                    )

                    Text(
                        text = "Automatically enforces 16:9 widescreen framing with no text, watermarks, borders, UI chrome, or random characters.",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                        color = StudioTextMuted
                    )
                }

                // Footer Generate / Regenerate Controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StudioCharcoal)
                        .border(1.dp, StudioBorder)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    if (genState is VisualAssetGenState.PreviewReady) {
                        OutlinedButton(
                            onClick = {
                                variationSeed += 1
                                onGenerate(currentRequest(variationSeed))
                            },
                            enabled = !isGenerating,
                            border = BorderStroke(1.dp, StudioAmber),
                            modifier = Modifier.testTag("regenerate_bg_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = StudioAmber, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Variation #${variationSeed + 1}", style = MaterialTheme.typography.labelMedium, color = StudioAmber)
                        }
                    }
                    Button(
                        onClick = { onGenerate(currentRequest(variationSeed)) },
                        enabled = !isGenerating,
                        colors = ButtonDefaults.buttonColors(containerColor = StudioAmber, contentColor = StudioVoid),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("generate_bg_submit_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isGenerating) "Generating..." else if (genState is VisualAssetGenState.PreviewReady) "Regenerate Background" else "Generate Background",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PropGeneratorDialog(
    sceneTitle: String,
    initialRequest: PropGenerationRequest,
    genState: VisualAssetGenState,
    onGenerate: (PropGenerationRequest) -> Unit,
    onReject: () -> Unit,
    onSaveToLibrary: (name: String, category: String, visualStyle: String, targetLayer: SceneLayerType, placeInScene: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var propName by remember(initialRequest) { mutableStateOf(initialRequest.propName) }
    var propCategory by remember(initialRequest) { mutableStateOf(initialRequest.propCategory) }
    var propDescription by remember(initialRequest) { mutableStateOf(initialRequest.propDescription) }
    var eraContext by remember(initialRequest) { mutableStateOf(initialRequest.eraContext) }
    var visualStyle by remember(initialRequest) { mutableStateOf(initialRequest.visualStyle) }
    var targetLayer by remember(initialRequest) {
        mutableStateOf(
            SceneLayerType.fromKeyOrDefault(initialRequest.targetLayer, SceneLayerType.PROPS)
                .takeIf { it in propTargetLayers } ?: SceneLayerType.PROPS
        )
    }
    var requestTransparentPng by remember(initialRequest) { mutableStateOf(initialRequest.requestTransparentPng) }
    var variationSeed by remember(initialRequest) { mutableIntStateOf(initialRequest.variationSeed.coerceAtLeast(1)) }

    fun currentRequest(seed: Int = variationSeed): PropGenerationRequest {
        return initialRequest.copy(
            propName = propName,
            propCategory = propCategory,
            propDescription = propDescription,
            eraContext = eraContext,
            visualStyle = visualStyle,
            targetLayer = targetLayer.name,
            requestTransparentPng = requestTransparentPng,
            variationSeed = seed
        )
    }

    val isGenerating = genState is VisualAssetGenState.Generating

    Dialog(
        onDismissRequest = { if (!isGenerating) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = StudioPanel,
            border = BorderStroke(1.dp, StudioBorder),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 720.dp)
                .testTag("prop_generator_dialog")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StudioCharcoal)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = StudioTeal,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "AI VISUAL PROP GENERATOR",
                                style = MaterialTheme.typography.titleSmall,
                                color = StudioTeal
                            )
                            Text(
                                text = "Scene: ${sceneTitle.ifBlank { "Current Scene" }} · Placeable 2D Layer Asset",
                                style = MaterialTheme.typography.labelSmall,
                                color = StudioTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        enabled = !isGenerating,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = StudioTextSecondary)
                    }
                }

                // Body
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp)
                ) {
                    // Generated Preview Panel with real transparency checkerboard & honest alpha indicator
                    when (genState) {
                        is VisualAssetGenState.Generating -> {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = StudioCharcoal,
                                border = BorderStroke(1.dp, StudioTeal.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(210.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    CircularProgressIndicator(color = StudioTeal, strokeWidth = 2.5.dp)
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        text = genState.label,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = StudioTextPrimary
                                    )
                                    Text(
                                        text = "Inspecting generated pixel alpha automatically upon completion",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StudioTextMuted
                                    )
                                }
                            }
                        }

                        is VisualAssetGenState.PreviewReady -> {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = StudioVoid,
                                border = BorderStroke(1.5.dp, StudioTeal),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(210.dp)
                                            .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                                    ) {
                                        // Checkerboard canvas reveals true alpha transparency immediately
                                        TransparencyCheckerboard(modifier = Modifier.fillMaxSize())
                                        Image(
                                            bitmap = genState.asset.bitmap.asImageBitmap(),
                                            contentDescription = "Generated visual prop preview",
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(12.dp)
                                        )
                                    }

                                    // Honest Pixel Alpha Status Banner
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (genState.asset.hasActualTransparency) {
                                                    StudioTealDim
                                                } else {
                                                    StudioElevated
                                                }
                                            )
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = if (genState.asset.hasActualTransparency) {
                                                "Verified Transparent PNG (alpha channel detected)"
                                            } else {
                                                "Opaque Image (no transparent alpha channel detected)"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (genState.asset.hasActualTransparency) StudioTeal else StudioAmber,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = genState.asset.formatLabel,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                            color = StudioTextSecondary
                                        )
                                    }

                                    // Accept / Save / Place Bar
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(StudioCharcoal)
                                            .padding(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = onReject,
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            border = BorderStroke(1.dp, StudioCoral.copy(alpha = 0.6f)),
                                            modifier = Modifier.testTag("reject_prop_button")
                                        ) {
                                            Text("Reject", style = MaterialTheme.typography.labelSmall, color = StudioCoral)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                onSaveToLibrary(propName, propCategory, visualStyle, targetLayer, false)
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            border = BorderStroke(1.dp, StudioAmber),
                                            modifier = Modifier.testTag("save_prop_library_button")
                                        ) {
                                            Icon(Icons.Default.BookmarkAdd, contentDescription = null, tint = StudioAmber, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = if (genState.savedAssetId != null) "Saved" else "Save",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = StudioAmber
                                            )
                                        }
                                        Button(
                                            onClick = {
                                                onSaveToLibrary(propName, propCategory, visualStyle, targetLayer, true)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = StudioTeal, contentColor = StudioVoid),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("place_prop_scene_button")
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Place on ${targetLayer.shortCode}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        is VisualAssetGenState.Error -> {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = genState.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        VisualAssetGenState.Idle -> Unit
                    }

                    // Prop Name & Era
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = propName,
                            onValueChange = { propName = it.take(80) },
                            label = { Text("Prop Name", style = MaterialTheme.typography.labelSmall) },
                            placeholder = { Text("e.g. Brass Compass", color = StudioTextMuted) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = StudioTextPrimary),
                            colors = generatorTextFieldColors(),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("prop_name_input")
                        )
                        OutlinedTextField(
                            value = eraContext,
                            onValueChange = { eraContext = it.take(120) },
                            label = { Text("Era / Context", style = MaterialTheme.typography.labelSmall) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = StudioTextPrimary),
                            colors = generatorTextFieldColors(),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Target Compositor Layer Chips
                    Text(
                        text = "TARGET COMPOSITOR LAYER",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = StudioTextSecondary
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        propTargetLayers.forEach { layer ->
                            FilterChip(
                                selected = targetLayer == layer,
                                onClick = { targetLayer = layer },
                                label = { Text("${layer.shortCode} · ${layer.label}", style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StudioTealDim,
                                    selectedLabelColor = StudioTeal
                                )
                            )
                        }
                    }

                    // Prop Category Chips
                    GeneratorChipGroup(
                        label = "PROP CATEGORY",
                        options = StudioPresets.propCategories,
                        selected = propCategory,
                        onSelect = { propCategory = it }
                    )

                    // Visual Style Chips
                    GeneratorChipGroup(
                        label = "2D VISUAL STYLE",
                        options = StudioPresets.characterVisualStyles,
                        selected = visualStyle,
                        onSelect = { visualStyle = it },
                        useTeal = true
                    )

                    // Transparent PNG Toggle
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = StudioCharcoal,
                        border = BorderStroke(1.dp, StudioBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Request Transparent PNG Background",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = StudioTextPrimary
                                )
                                Text(
                                    text = "Actual alpha pixels are verified after generation and reported honestly.",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = StudioTextMuted
                                )
                            }
                            Switch(
                                checked = requestTransparentPng,
                                onCheckedChange = { requestTransparentPng = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = StudioVoid,
                                    checkedTrackColor = StudioTeal
                                )
                            )
                        }
                    }

                    // Editable Prop Visual Prompt
                    Text(
                        text = "PROP VISUAL DESCRIPTION / PROMPT",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = StudioTextSecondary
                    )
                    OutlinedTextField(
                        value = propDescription,
                        onValueChange = { propDescription = it.take(700) },
                        placeholder = {
                            Text(
                                "Describe materials, silhouette, patina, and documentary details...",
                                style = MaterialTheme.typography.bodySmall,
                                color = StudioTextMuted
                            )
                        },
                        minLines = 3,
                        maxLines = 5,
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = StudioTextPrimary),
                        colors = generatorTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("prop_prompt_input")
                    )
                }

                // Footer
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StudioCharcoal)
                        .border(1.dp, StudioBorder)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    if (genState is VisualAssetGenState.PreviewReady) {
                        OutlinedButton(
                            onClick = {
                                variationSeed += 1
                                onGenerate(currentRequest(variationSeed))
                            },
                            enabled = !isGenerating,
                            border = BorderStroke(1.dp, StudioTeal),
                            modifier = Modifier.testTag("regenerate_prop_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = StudioTeal, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Variation #${variationSeed + 1}", style = MaterialTheme.typography.labelMedium, color = StudioTeal)
                        }
                    }
                    Button(
                        onClick = { onGenerate(currentRequest(variationSeed)) },
                        enabled = !isGenerating,
                        colors = ButtonDefaults.buttonColors(containerColor = StudioTeal, contentColor = StudioVoid),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("generate_prop_submit_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isGenerating) "Generating..." else if (genState is VisualAssetGenState.PreviewReady) "Regenerate Prop" else "Generate Visual Prop",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SceneSuggestionsDialog(
    scene: StudioScene,
    defaultVisualStyle: String,
    suggestionsState: SceneSuggestionsState,
    onRequestSuggestions: (String) -> Unit,
    onSelectBackgroundSuggestion: (SuggestedBackgroundPlan, String) -> Unit,
    onSelectPropSuggestion: (SuggestedPropPlan, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedStyle by remember(defaultVisualStyle) {
        mutableStateOf(defaultVisualStyle.ifBlank { "Painterly 2D Documentary" })
    }

    // Automatically trigger analysis when opening in Idle state for the current scene
    LaunchedEffect(scene.id) {
        val alreadyLoadedForScene = (suggestionsState as? SceneSuggestionsState.Ready)?.suggestions?.sceneId == scene.id
        if (!alreadyLoadedForScene && suggestionsState !is SceneSuggestionsState.Loading) {
            onRequestSuggestions(selectedStyle)
        }
    }

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
                .heightIn(max = 700.dp)
                .testTag("scene_suggestions_dialog")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StudioCharcoal)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = StudioAmber, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "SCENE VISUAL ASSET SUGGESTIONS",
                                style = MaterialTheme.typography.titleSmall,
                                color = StudioAmber
                            )
                            Text(
                                text = scene.title.ifBlank { "Selected Scene" },
                                style = MaterialTheme.typography.labelSmall,
                                color = StudioTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = StudioTextSecondary)
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = StudioCharcoal,
                        border = BorderStroke(1.dp, StudioBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Review AI-recommended background and visual props for this scene. Existing scene assets are never replaced automatically — tap any suggestion to edit and generate it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = StudioTextSecondary,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    GeneratorChipGroup(
                        label = "TARGET 2D VISUAL STYLE",
                        options = StudioPresets.characterVisualStyles,
                        selected = selectedStyle,
                        onSelect = { selectedStyle = it }
                    )

                    when (suggestionsState) {
                        SceneSuggestionsState.Idle, SceneSuggestionsState.Loading -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp)
                            ) {
                                CircularProgressIndicator(color = StudioAmber, strokeWidth = 2.5.dp)
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = "Analyzing scene script, location, and storytelling beats...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StudioTextSecondary
                                )
                            }
                        }

                        is SceneSuggestionsState.Error -> {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = suggestionsState.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    OutlinedButton(
                                        onClick = { onRequestSuggestions(selectedStyle) },
                                        border = BorderStroke(1.dp, StudioAmber)
                                    ) {
                                        Text("Retry Analysis", style = MaterialTheme.typography.labelSmall, color = StudioAmber)
                                    }
                                }
                            }
                        }

                        is SceneSuggestionsState.Ready -> {
                            val plan = suggestionsState.suggestions

                            // 1. Suggested Background
                            Text(
                                text = "SUGGESTED 16:9 BACKGROUND",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = StudioAmber
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = StudioCharcoal,
                                border = BorderStroke(1.dp, StudioAmber.copy(alpha = 0.55f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(10.dp)
                                ) {
                                    Text(
                                        text = plan.background.title,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = StudioTextPrimary
                                    )
                                    Text(
                                        text = listOf(plan.background.location, plan.background.timeOfDay, plan.background.weather)
                                            .filter { it.isNotBlank() }
                                            .joinToString(" · "),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StudioTeal
                                    )
                                    if (plan.background.prompt.isNotBlank()) {
                                        Text(
                                            text = plan.background.prompt,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = StudioTextSecondary,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (plan.background.rationale.isNotBlank()) {
                                        Text(
                                            text = plan.background.rationale,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = StudioTextMuted
                                        )
                                    }
                                    Button(
                                        onClick = { onSelectBackgroundSuggestion(plan.background, selectedStyle) },
                                        colors = ButtonDefaults.buttonColors(containerColor = StudioAmber, contentColor = StudioVoid),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("use_suggested_bg_button")
                                    ) {
                                        Icon(Icons.Default.Landscape, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Edit & Generate Background", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // 2. Suggested Individual Props
                            if (plan.props.isNotEmpty()) {
                                Text(
                                    text = "SUGGESTED VISUAL PROPS (${plan.props.size})",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = StudioTeal
                                )
                                plan.props.forEachIndexed { idx, prop ->
                                    Surface(
                                        shape = RoundedCornerShape(9.dp),
                                        color = StudioCharcoal,
                                        border = BorderStroke(1.dp, StudioBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(5.dp),
                                            modifier = Modifier.padding(10.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = prop.name,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = StudioTextPrimary,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = StudioTealDim
                                                ) {
                                                    Text(
                                                        text = "${prop.category} · ${prop.suggestedLayer}",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                                        color = StudioTeal,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = prop.prompt,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = StudioTextSecondary,
                                                maxLines = 3,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (prop.rationale.isNotBlank()) {
                                                Text(
                                                    text = prop.rationale,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = StudioTextMuted
                                                )
                                            }
                                            OutlinedButton(
                                                onClick = { onSelectPropSuggestion(prop, selectedStyle) },
                                                border = BorderStroke(1.dp, StudioTeal),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("use_suggested_prop_$idx")
                                            ) {
                                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = StudioTeal, modifier = Modifier.size(15.dp))
                                                Spacer(Modifier.width(5.dp))
                                                Text("Edit & Generate Prop", style = MaterialTheme.typography.labelSmall, color = StudioTeal)
                                            }
                                        }
                                    }
                                }
                            }

                            // 3. Environmental Details
                            if (plan.environmentalDetails.isNotEmpty()) {
                                Text(
                                    text = "ENVIRONMENTAL STAGING NOTES",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = StudioTextSecondary
                                )
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
                                        plan.environmentalDetails.forEach { detail ->
                                            Text(
                                                text = "• $detail",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = StudioTextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Footer
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StudioCharcoal)
                        .border(1.dp, StudioBorder)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    TextButton(
                        onClick = { onRequestSuggestions(selectedStyle) },
                        enabled = suggestionsState !is SceneSuggestionsState.Loading
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = StudioAmber, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Refresh Suggestions", style = MaterialTheme.typography.labelMedium, color = StudioAmber)
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Close", style = MaterialTheme.typography.labelMedium, color = StudioTextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun GeneratorChipGroup(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    useTeal: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = StudioTextSecondary
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = selected == option,
                    onClick = { onSelect(option) },
                    label = { Text(option, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (useTeal) StudioTealDim else StudioAmberDim,
                        selectedLabelColor = if (useTeal) StudioTeal else StudioAmber
                    )
                )
            }
        }
    }
}

@Composable
private fun TransparencyCheckerboard(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val tileSize = 14.dp.toPx()
        val light = Color(0xFF2E323D)
        val dark = Color(0xFF1D2028)
        val cols = (size.width / tileSize).toInt() + 1
        val rows = (size.height / tileSize).toInt() + 1
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                drawRect(
                    color = if ((r + c) % 2 == 0) light else dark,
                    topLeft = Offset(c * tileSize, r * tileSize),
                    size = Size(tileSize, tileSize)
                )
            }
        }
    }
}

@Composable
private fun generatorTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = StudioCharcoal,
    unfocusedContainerColor = StudioCharcoal,
    focusedBorderColor = StudioAmber,
    unfocusedBorderColor = StudioBorder
)
