package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.InfographicDataPoint
import com.example.data.MapMarkerItem
import com.example.data.MapRouteItem
import com.example.data.MotionGraphicElement
import com.example.data.SceneTransitionConfig
import com.example.data.StudioPresets
import com.example.data.StudioScene
import com.example.graphics.TransitionFlowEngine
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioAmberDim
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCharcoal
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioTeal
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.StudioViolet
import com.example.ui.theme.StudioVoid
import kotlin.math.roundToInt

@Composable
internal fun InfographicsTabContent(
    scene: StudioScene,
    selectedGraphicId: String?,
    onSelectGraphic: (String?) -> Unit,
    onCreateInfographic: (String) -> Unit,
    onUpdateGraphic: (String, (MotionGraphicElement) -> MotionGraphicElement) -> Unit,
    onDuplicateGraphic: (String) -> Unit,
    onDeleteGraphic: (String) -> Unit,
    onSaveToLibrary: (String) -> Unit
) {
    Text(
        text = "ANIMATED INFOGRAPHICS (10 TYPES)",
        style = MaterialTheme.typography.titleSmall,
        color = StudioEmerald
    )
    Text(
        text = "Create editable, time-animated data charts, counters, comparisons, timelines, and flow diagrams.",
        style = MaterialTheme.typography.bodySmall,
        color = StudioTextSecondary
    )

    StudioPresets.infographicTypeOptions.chunked(2).forEach { pair ->
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            pair.forEach { (key, label) ->
                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = StudioCharcoal,
                    border = BorderStroke(1.dp, StudioEmerald.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(7.dp))
                        .clickable { onCreateInfographic(key) }
                        .testTag("mgfx_info_${key.lowercase()}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = StudioEmerald, modifier = Modifier.size(13.dp))
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

    val infographics = scene.motionGraphics.filter { it.category.equals("INFOGRAPHIC", ignoreCase = true) }
    if (infographics.isEmpty()) {
        Text(
            text = "Tap any chart type above to add an animated infographic to this scene.",
            style = MaterialTheme.typography.bodySmall,
            color = StudioTextMuted
        )
        return
    }

    // Active Infographic Selector
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        infographics.forEach { info ->
            FilterChip(
                selected = info.graphicId == selectedGraphicId || (selectedGraphicId == null && info == infographics.first()),
                onClick = { onSelectGraphic(info.graphicId) },
                label = { Text(info.title.take(18), style = MaterialTheme.typography.labelSmall) },
                colors = mgfxChipColors()
            )
        }
    }

    val active = infographics.firstOrNull { it.graphicId == selectedGraphicId } ?: infographics.first()

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
                    text = "DATA: ${active.infographicType}",
                    style = MaterialTheme.typography.labelMedium,
                    color = StudioEmerald
                )
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = { onSaveToLibrary(active.graphicId) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.BookmarkAdd, contentDescription = "Save template", tint = StudioTeal, modifier = Modifier.size(15.dp))
                    }
                    IconButton(onClick = { onDuplicateGraphic(active.graphicId) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate infographic", tint = StudioTextSecondary, modifier = Modifier.size(14.dp))
                    }
                    IconButton(onClick = { onDeleteGraphic(active.graphicId) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete infographic", tint = StudioCoral, modifier = Modifier.size(15.dp))
                    }
                }
            }

            OutlinedTextField(
                value = active.primaryText,
                onValueChange = { v -> onUpdateGraphic(active.graphicId) { it.copy(primaryText = v.take(160), title = v.take(120)) } },
                label = { Text("Chart Title") },
                singleLine = true,
                colors = mgfxFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = active.secondaryText,
                    onValueChange = { v -> onUpdateGraphic(active.graphicId) { it.copy(secondaryText = v.take(180)) } },
                    label = { Text("Subtitle") },
                    singleLine = true,
                    colors = mgfxFieldColors(),
                    modifier = Modifier.weight(1.4f)
                )
                OutlinedTextField(
                    value = active.dataUnit,
                    onValueChange = { v -> onUpdateGraphic(active.graphicId) { it.copy(dataUnit = v.take(24)) } },
                    label = { Text("Unit") },
                    singleLine = true,
                    colors = mgfxFieldColors(),
                    modifier = Modifier.weight(0.7f)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                MgfxFieldLabel("Data Rows (${active.dataPoints.size})")
                OutlinedButton(
                    onClick = {
                        val nextIdx = active.dataPoints.size + 1
                        val swatch = StudioPresets.graphicColorSwatches[nextIdx % StudioPresets.graphicColorSwatches.size]
                        val newRow = InfographicDataPoint(
                            id = "dp_${System.currentTimeMillis() % 100000}",
                            label = "Item $nextIdx",
                            value = 50.0,
                            secondaryValue = if (active.infographicType == "COMPARISON") 75.0 else 0.0,
                            colorHex = swatch
                        )
                        onUpdateGraphic(active.graphicId) { it.copy(dataPoints = (it.dataPoints + newRow).take(16)) }
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    border = BorderStroke(1.dp, StudioEmerald)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = StudioEmerald, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("Add Row", style = MaterialTheme.typography.labelSmall, color = StudioEmerald)
                }
            }

            active.dataPoints.forEach { dp ->
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = StudioVoid,
                    border = BorderStroke(1.dp, if (dp.highlighted) StudioAmber else StudioBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(7.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = dp.label,
                                onValueChange = { txt ->
                                    onUpdateGraphic(active.graphicId) { g ->
                                        g.copy(dataPoints = g.dataPoints.map { if (it.id == dp.id) it.copy(label = txt.take(48)) else it })
                                    }
                                },
                                label = { Text("Label") },
                                singleLine = true,
                                colors = mgfxFieldColors(),
                                modifier = Modifier.weight(1.2f)
                            )
                            OutlinedTextField(
                                value = if (dp.value == dp.value.toLong().toDouble()) dp.value.toLong().toString() else dp.value.toString(),
                                onValueChange = { txt ->
                                    val parsed = txt.trim().toDoubleOrNull()
                                    if (parsed != null && !parsed.isNaN() && !parsed.isInfinite()) {
                                        onUpdateGraphic(active.graphicId) { g ->
                                            g.copy(dataPoints = g.dataPoints.map { if (it.id == dp.id) it.copy(value = parsed) else it })
                                        }
                                    }
                                },
                                label = { Text("Value") },
                                singleLine = true,
                                colors = mgfxFieldColors(),
                                modifier = Modifier.weight(0.8f)
                            )
                            IconButton(
                                onClick = {
                                    onUpdateGraphic(active.graphicId) { g ->
                                        g.copy(dataPoints = g.dataPoints.filterNot { it.id == dp.id })
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete data row", tint = StudioCoral, modifier = Modifier.size(14.dp))
                            }
                        }

                        if (active.infographicType == "COMPARISON") {
                            OutlinedTextField(
                                value = if (dp.secondaryValue == dp.secondaryValue.toLong().toDouble()) dp.secondaryValue.toLong().toString() else dp.secondaryValue.toString(),
                                onValueChange = { txt ->
                                    val parsed = txt.trim().toDoubleOrNull()
                                    if (parsed != null && !parsed.isNaN() && !parsed.isInfinite()) {
                                        onUpdateGraphic(active.graphicId) { g ->
                                            g.copy(dataPoints = g.dataPoints.map { if (it.id == dp.id) it.copy(secondaryValue = parsed) else it })
                                        }
                                    }
                                },
                                label = { Text("Comparison Value B") },
                                singleLine = true,
                                colors = mgfxFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = dp.calloutText,
                                onValueChange = { txt ->
                                    onUpdateGraphic(active.graphicId) { g ->
                                        g.copy(dataPoints = g.dataPoints.map { if (it.id == dp.id) it.copy(calloutText = txt.take(64)) else it })
                                    }
                                },
                                label = { Text("Callout Note") },
                                singleLine = true,
                                colors = mgfxFieldColors(),
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = dp.highlighted,
                                onClick = {
                                    onUpdateGraphic(active.graphicId) { g ->
                                        g.copy(dataPoints = g.dataPoints.map { if (it.id == dp.id) it.copy(highlighted = !it.highlighted) else it })
                                    }
                                },
                                label = { Text("Focus", style = MaterialTheme.typography.labelSmall) },
                                colors = mgfxChipColors()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun DocumentaryMapsTabContent(
    scene: StudioScene,
    selectedGraphicId: String?,
    onSelectGraphic: (String?) -> Unit,
    onCreateMap: () -> Unit,
    onUpdateGraphic: (String, (MotionGraphicElement) -> MotionGraphicElement) -> Unit,
    onDeleteGraphic: (String) -> Unit,
    onRequestImportMapImage: (String) -> Unit,
    onSaveToLibrary: (String) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "DOCUMENTARY MAPS",
            style = MaterialTheme.typography.titleSmall,
            color = StudioTeal
        )
        Button(
            onClick = onCreateMap,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            colors = ButtonDefaults.buttonColors(containerColor = StudioTeal, contentColor = StudioVoid),
            modifier = Modifier.testTag("mgfx_add_map_btn")
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(3.dp))
            Text("Add Route Map", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }

    Text(
        text = "Uses built-in stylized cartographic canvases or your own imported map image (Photo Picker). Live external map tile streaming is not required.",
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
        color = StudioTextMuted
    )

    val maps = scene.motionGraphics.filter { it.category.equals("MAP", ignoreCase = true) }
    if (maps.isEmpty()) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = StudioCharcoal,
            border = BorderStroke(1.dp, StudioBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Tap + Add Route Map to insert an animated documentary map with city pins, progressive route arrows, distance captions, and zoom/pan.",
                style = MaterialTheme.typography.bodySmall,
                color = StudioTextSecondary,
                modifier = Modifier.padding(10.dp)
            )
        }
        return
    }

    val active = maps.firstOrNull { it.graphicId == selectedGraphicId } ?: maps.first()

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
                Text(active.title, style = MaterialTheme.typography.labelMedium, color = StudioTeal)
                Row {
                    IconButton(onClick = { onSaveToLibrary(active.graphicId) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.BookmarkAdd, contentDescription = "Save map template", tint = StudioTeal, modifier = Modifier.size(15.dp))
                    }
                    IconButton(onClick = { onDeleteGraphic(active.graphicId) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete map", tint = StudioCoral, modifier = Modifier.size(15.dp))
                    }
                }
            }

            OutlinedTextField(
                value = active.primaryText,
                onValueChange = { v -> onUpdateGraphic(active.graphicId) { it.copy(primaryText = v.take(120)) } },
                label = { Text("Map Region Title") },
                singleLine = true,
                colors = mgfxFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            // Map Style Presets
            MgfxFieldLabel("Cartographic Base Style")
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                StudioPresets.mapPresetStyles.forEach { (key, label) ->
                    FilterChip(
                        selected = active.mapPresetStyle == key,
                        onClick = { onUpdateGraphic(active.graphicId) { it.copy(mapPresetStyle = key) } },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        colors = mgfxChipColors()
                    )
                }
            }

            // Zero-permission Photo Picker map image import
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = { onRequestImportMapImage(active.graphicId) },
                    border = BorderStroke(1.dp, StudioAmber),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mgfx_import_map_image_btn")
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = StudioAmber, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = if (active.mapBackgroundUri.isBlank()) "Import Map Image" else "Replace Map Image",
                        style = MaterialTheme.typography.labelSmall,
                        color = StudioAmber
                    )
                }
                if (active.mapBackgroundUri.isNotBlank()) {
                    OutlinedButton(
                        onClick = { onUpdateGraphic(active.graphicId) { it.copy(mapBackgroundUri = "") } },
                        border = BorderStroke(1.dp, StudioBorder)
                    ) {
                        Text("Use Atlas", style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary)
                    }
                }
            }

            // Animated Zoom & Pan Sliders
            MgfxSliderRow(
                label = "Zoom Start",
                valueText = "${String.format("%.2f", active.mapZoomStart)}x",
                value = active.mapZoomStart.toFloat(),
                range = 1.0f..2.5f,
                onValueChange = { v -> onUpdateGraphic(active.graphicId) { it.copy(mapZoomStart = v.toDouble()) } }
            )
            MgfxSliderRow(
                label = "Zoom End",
                valueText = "${String.format("%.2f", active.mapZoomEnd)}x",
                value = active.mapZoomEnd.toFloat(),
                range = 1.0f..2.5f,
                onValueChange = { v -> onUpdateGraphic(active.graphicId) { it.copy(mapZoomEnd = v.toDouble()) } }
            )
            MgfxSliderRow(
                label = "Pan X End",
                valueText = "${(active.mapPanEndX * 100).roundToInt()}%",
                value = active.mapPanEndX.toFloat(),
                range = -0.35f..0.35f,
                onValueChange = { v -> onUpdateGraphic(active.graphicId) { it.copy(mapPanEndX = v.toDouble()) } }
            )

            // Markers CRUD
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                MgfxFieldLabel("City / Location Pins (${active.mapMarkers.size})")
                OutlinedButton(
                    onClick = {
                        val idx = active.mapMarkers.size + 1
                        val newMarker = MapMarkerItem(
                            markerId = "mk_${System.currentTimeMillis() % 100000}",
                            label = "Location $idx",
                            regionTag = "Waypoint",
                            xNorm = (0.2 + (idx * 0.16) % 0.65).coerceIn(0.1, 0.9),
                            yNorm = (0.3 + (idx * 0.14) % 0.45).coerceIn(0.15, 0.85),
                            colorHex = StudioPresets.graphicColorSwatches[idx % StudioPresets.graphicColorSwatches.size],
                            appearAtSec = (idx * 0.4).coerceAtMost(active.durationSec * 0.7)
                        )
                        onUpdateGraphic(active.graphicId) { it.copy(mapMarkers = (it.mapMarkers + newMarker).take(12)) }
                    },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    border = BorderStroke(1.dp, StudioTeal)
                ) { Text("+ Pin", style = MaterialTheme.typography.labelSmall, color = StudioTeal) }
            }

            active.mapMarkers.forEach { mk ->
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = StudioVoid,
                    border = BorderStroke(1.dp, StudioBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            OutlinedTextField(
                                value = mk.label,
                                onValueChange = { txt ->
                                    onUpdateGraphic(active.graphicId) { g ->
                                        g.copy(mapMarkers = g.mapMarkers.map { if (it.markerId == mk.markerId) it.copy(label = txt.take(40)) else it })
                                    }
                                },
                                label = { Text("Pin Label") },
                                singleLine = true,
                                colors = mgfxFieldColors(),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    onUpdateGraphic(active.graphicId) { g ->
                                        g.copy(mapMarkers = g.mapMarkers.filterNot { it.markerId == mk.markerId })
                                    }
                                },
                                modifier = Modifier.size(26.dp)
                            ) { Icon(Icons.Default.DeleteOutline, contentDescription = "Delete pin", tint = StudioCoral, modifier = Modifier.size(14.dp)) }
                        }
                        MgfxSliderRow(
                            label = "X Position",
                            valueText = "${(mk.xNorm * 100).roundToInt()}%",
                            value = mk.xNorm.toFloat(),
                            range = 0.05f..0.95f,
                            onValueChange = { v ->
                                onUpdateGraphic(active.graphicId) { g ->
                                    g.copy(mapMarkers = g.mapMarkers.map { if (it.markerId == mk.markerId) it.copy(xNorm = v.toDouble()) else it })
                                }
                            }
                        )
                        MgfxSliderRow(
                            label = "Y Position",
                            valueText = "${(mk.yNorm * 100).roundToInt()}%",
                            value = mk.yNorm.toFloat(),
                            range = 0.08f..0.92f,
                            onValueChange = { v ->
                                onUpdateGraphic(active.graphicId) { g ->
                                    g.copy(mapMarkers = g.mapMarkers.map { if (it.markerId == mk.markerId) it.copy(yNorm = v.toDouble()) else it })
                                }
                            }
                        )
                    }
                }
            }

            // Routes CRUD
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                MgfxFieldLabel("Animated Routes (${active.mapRoutes.size})")
                OutlinedButton(
                    onClick = {
                        val idx = active.mapRoutes.size + 1
                        val newRoute = MapRouteItem(
                            routeId = "rt_${System.currentTimeMillis() % 100000}",
                            label = "Route $idx",
                            distanceCaption = "250 km",
                            fromX = 0.25,
                            fromY = 0.70,
                            toX = 0.72,
                            toY = 0.28,
                            colorHex = "#F5A623",
                            showArrow = true,
                            startSec = 0.3,
                            durationSec = 1.5
                        )
                        onUpdateGraphic(active.graphicId) { it.copy(mapRoutes = (it.mapRoutes + newRoute).take(8)) }
                    },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    border = BorderStroke(1.dp, StudioAmber)
                ) { Text("+ Route", style = MaterialTheme.typography.labelSmall, color = StudioAmber) }
            }

            active.mapRoutes.forEach { rt ->
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = StudioVoid,
                    border = BorderStroke(1.dp, StudioBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(6.dp)
                    ) {
                        OutlinedTextField(
                            value = rt.label,
                            onValueChange = { txt ->
                                onUpdateGraphic(active.graphicId) { g ->
                                    g.copy(mapRoutes = g.mapRoutes.map { if (it.routeId == rt.routeId) it.copy(label = txt.take(40)) else it })
                                }
                            },
                            label = { Text("Route") },
                            singleLine = true,
                            colors = mgfxFieldColors(),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = rt.distanceCaption,
                            onValueChange = { txt ->
                                onUpdateGraphic(active.graphicId) { g ->
                                    g.copy(mapRoutes = g.mapRoutes.map { if (it.routeId == rt.routeId) it.copy(distanceCaption = txt.take(24)) else it })
                                }
                            },
                            label = { Text("Distance") },
                            singleLine = true,
                            colors = mgfxFieldColors(),
                            modifier = Modifier.weight(0.75f)
                        )
                        IconButton(
                            onClick = {
                                onUpdateGraphic(active.graphicId) { g ->
                                    g.copy(mapRoutes = g.mapRoutes.filterNot { it.routeId == rt.routeId })
                                }
                            },
                            modifier = Modifier.size(26.dp)
                        ) { Icon(Icons.Default.DeleteOutline, contentDescription = "Delete route", tint = StudioCoral, modifier = Modifier.size(14.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
internal fun StoryFlowTransitionsTabContent(
    scenes: List<StudioScene>,
    selectedSceneId: String?,
    onSelectScene: (String) -> Unit,
    onMoveScene: (String, Int) -> Unit,
    onUpdateSceneDuration: (String, Double) -> Unit,
    onUpdateTransition: (String, SceneTransitionConfig) -> Unit,
    onPreviewTransition: (String) -> Unit
) {
    val segments = remember(scenes) { TransitionFlowEngine.computeStoryFlowSegments(scenes) }

    Text(
        text = "STORY FLOW & SCENE TRANSITIONS",
        style = MaterialTheme.typography.titleSmall,
        color = StudioViolet
    )
    Text(
        text = "Reorder scenes, tune pacing duration, and configure real dual-scene visual transitions (Cut, Fade, Crossfade, Dissolve, Wipe, Slide, Zoom, Blur) without altering keyframes.",
        style = MaterialTheme.typography.bodySmall,
        color = StudioTextSecondary
    )

    if (segments.isEmpty()) {
        Text("No scenes in this project yet.", style = MaterialTheme.typography.bodySmall, color = StudioTextMuted)
        return
    }

    segments.forEach { seg ->
        val isSel = seg.scene.id == selectedSceneId
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isSel) StudioAmberDim else StudioCharcoal,
            border = BorderStroke(1.dp, if (isSel) StudioAmber else StudioBorder),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelectScene(seg.scene.id) }
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SC ${seg.index + 1} · ${seg.scene.title}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSel) StudioAmber else StudioTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${String.format("%.1f", seg.startSec)}s → ${String.format("%.1f", seg.endSec)}s (${String.format("%.1f", seg.scene.durationSec)}s)",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = StudioTextSecondary
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = { onUpdateSceneDuration(seg.scene.id, (seg.scene.durationSec - 1.0).coerceAtLeast(1.5)) },
                            contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp),
                            border = BorderStroke(1.dp, StudioBorder),
                            modifier = Modifier.height(24.dp)
                        ) { Text("-1s", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = StudioTextSecondary) }
                        Spacer(Modifier.width(3.dp))
                        OutlinedButton(
                            onClick = { onUpdateSceneDuration(seg.scene.id, (seg.scene.durationSec + 1.0).coerceAtMost(60.0)) },
                            contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp),
                            border = BorderStroke(1.dp, StudioBorder),
                            modifier = Modifier.height(24.dp)
                        ) { Text("+1s", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = StudioTextSecondary) }
                        IconButton(
                            onClick = { onMoveScene(seg.scene.id, -1) },
                            enabled = seg.index > 0,
                            modifier = Modifier.size(26.dp)
                        ) { Icon(Icons.Default.ArrowUpward, contentDescription = "Move scene earlier", tint = StudioTextSecondary, modifier = Modifier.size(14.dp)) }
                        IconButton(
                            onClick = { onMoveScene(seg.scene.id, 1) },
                            enabled = seg.hasNextScene,
                            modifier = Modifier.size(26.dp)
                        ) { Icon(Icons.Default.ArrowDownward, contentDescription = "Move scene later", tint = StudioTextSecondary, modifier = Modifier.size(14.dp)) }
                    }
                }
            }
        }

        // Transition Editor between Scene i and Scene i+1
        if (seg.hasNextScene) {
            val tr = seg.outgoingTransition
            Surface(
                shape = RoundedCornerShape(7.dp),
                color = StudioVoid,
                border = BorderStroke(1.dp, if (tr.type == "CUT") StudioBorder else StudioViolet.copy(alpha = 0.7f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "TRANSITION SC ${seg.index + 1} → SC ${seg.index + 2}: ${tr.type}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (tr.type == "CUT") StudioTextSecondary else StudioViolet,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = { onPreviewTransition(seg.scene.id) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StudioViolet, contentColor = StudioVoid),
                            modifier = Modifier
                                .height(26.dp)
                                .testTag("mgfx_preview_transition_${seg.scene.id}")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("Preview", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp), fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        StudioPresets.sceneTransitionTypes.forEach { (typeKey, label) ->
                            FilterChip(
                                selected = tr.type == typeKey,
                                onClick = {
                                    onUpdateTransition(
                                        seg.scene.id,
                                        tr.copy(
                                            type = typeKey,
                                            durationSec = if (typeKey == "CUT") 0.6 else tr.durationSec.coerceIn(0.4, 3.0)
                                        )
                                    )
                                },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                colors = mgfxChipColors()
                            )
                        }
                    }

                    if (tr.type != "CUT") {
                        MgfxSliderRow(
                            label = "Transition Duration",
                            valueText = "${String.format("%.1f", tr.durationSec)}s",
                            value = tr.durationSec.toFloat(),
                            range = 0.2f..3.0f,
                            onValueChange = { v -> onUpdateTransition(seg.scene.id, tr.copy(durationSec = v.toDouble())) }
                        )
                        if (tr.type in setOf("WIPE", "SLIDE", "ZOOM")) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.horizontalScroll(rememberScrollState())
                            ) {
                                val dirs = if (tr.type == "ZOOM") listOf("IN", "OUT") else listOf("LEFT", "RIGHT", "UP", "DOWN")
                                dirs.forEach { dir ->
                                    FilterChip(
                                        selected = tr.direction == dir,
                                        onClick = { onUpdateTransition(seg.scene.id, tr.copy(direction = dir)) },
                                        label = { Text(dir, style = MaterialTheme.typography.labelSmall) },
                                        colors = mgfxChipColors()
                                    )
                                }
                            }
                        }
                        if (tr.type == "BLUR") {
                            MgfxSliderRow(
                                label = "Blur Feather Intensity",
                                valueText = "${(tr.feather * 100).roundToInt()}%",
                                value = tr.feather.toFloat(),
                                range = 0f..1f,
                                onValueChange = { v -> onUpdateTransition(seg.scene.id, tr.copy(feather = v.toDouble())) }
                            )
                        }
                    }
                }
            }
        }
    }
}
