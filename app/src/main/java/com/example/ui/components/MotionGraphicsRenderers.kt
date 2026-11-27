package com.example.ui.components

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.audio.LipSyncEngine
import com.example.data.MotionGraphicElement
import com.example.data.StudioScene
import com.example.data.TimelineTrackType
import com.example.graphics.EvaluatedGraphicFrame
import com.example.graphics.MotionGraphicsEngine
import com.example.graphics.SubtitleEngine
import com.example.graphics.TransitionCompositorTransforms
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.theme.SpaceGroteskFamily
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCharcoal
import com.example.ui.theme.StudioTeal
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.StudioVoid
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

fun resolveStudioFontFamily(key: String): FontFamily = when (key.uppercase()) {
    "SPACE_GROTESK" -> SpaceGroteskFamily
    "PLUS_JAKARTA" -> PlusJakartaSansFamily
    "JETBRAINS_MONO" -> JetBrainsMonoFamily
    "SERIF" -> FontFamily.Serif
    "MONOSPACE" -> FontFamily.Monospace
    else -> FontFamily.SansSerif
}

private fun resolveTextAlign(alignment: String): TextAlign = when (alignment.uppercase()) {
    "LEFT" -> TextAlign.Start
    "RIGHT" -> TextAlign.End
    else -> TextAlign.Center
}

private fun resolveColumnAlign(alignment: String): Alignment.Horizontal = when (alignment.uppercase()) {
    "LEFT" -> Alignment.Start
    "RIGHT" -> Alignment.End
    else -> Alignment.CenterHorizontally
}

private fun revealClipShape(fraction: Float): GenericShape = GenericShape { size, _ ->
    val w = size.width * fraction.coerceIn(0f, 1f)
    moveTo(0f, 0f)
    lineTo(w, 0f)
    lineTo(w, size.height)
    lineTo(0f, size.height)
    close()
}

private fun wipeClipShape(fraction: Float, direction: String): GenericShape = GenericShape { size, _ ->
    val f = fraction.coerceIn(0f, 1f)
    when (direction.uppercase()) {
        "RIGHT" -> {
            val left = size.width * (1f - f)
            moveTo(left, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height)
            lineTo(left, size.height)
            close()
        }
        "UP" -> {
            val top = size.height * (1f - f)
            moveTo(0f, top)
            lineTo(size.width, top)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        "DOWN" -> {
            val bottom = size.height * f
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, bottom)
            lineTo(0f, bottom)
            close()
        }
        else -> { // LEFT (reveals left-to-right)
            val right = size.width * f
            moveTo(0f, 0f)
            lineTo(right, 0f)
            lineTo(right, size.height)
            lineTo(0f, size.height)
            close()
        }
    }
}

/**
 * Renders any evaluated Phase 6 [MotionGraphicElement] directly inside the 16:9 compositor stage:
 * - Animated typography & all 13 documentary title/card templates
 * - 4 structured Lower Third layouts
 * - All 10 animated data infographics
 * - Animated documentary maps with imported image or stylized cartographic canvas, routes & markers
 */
@Composable
fun StageMotionGraphicVisual(
    evaluated: EvaluatedGraphicFrame,
    stageScaleFactor: Float,
    modifier: Modifier = Modifier
) {
    val g = evaluated.graphic
    val clipMod = if (evaluated.clipRevealFraction < 0.995f) {
        modifier.clip(revealClipShape(evaluated.clipRevealFraction))
    } else {
        modifier
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = clipMod
            .graphicsLayer {
                scaleX = evaluated.scaleX.toFloat().coerceIn(-3.5f, 3.5f)
                scaleY = evaluated.scaleY.toFloat().coerceIn(0.15f, 3.5f)
                rotationZ = evaluated.rotationDeg.toFloat()
                alpha = evaluated.opacity
            }
    ) {
        when (g.category.uppercase()) {
            "INFOGRAPHIC" -> StageInfographicCard(
                graphic = g,
                internalProgress = evaluated.internalProgress,
                stageScaleFactor = stageScaleFactor
            )
            "MAP" -> StageDocumentaryMapCard(
                graphic = g,
                localTimeSec = evaluated.localTimeSec,
                stageScaleFactor = stageScaleFactor
            )
            "LOWER_THIRD" -> StageLowerThirdCard(
                evaluated = evaluated,
                stageScaleFactor = stageScaleFactor
            )
            else -> StageTypographyTemplateCard(
                evaluated = evaluated,
                stageScaleFactor = stageScaleFactor
            )
        }
    }
}

@Composable
private fun StageTypographyTemplateCard(
    evaluated: EvaluatedGraphicFrame,
    stageScaleFactor: Float
) {
    val g = evaluated.graphic
    val fontFamily = remember(g.fontFamilyKey) { resolveStudioFontFamily(g.fontFamilyKey) }
    val textColor = parseHexColor(g.textColorHex, StudioTextPrimary)
    val accentColor = parseHexColor(g.accentColorHex, StudioAmber)
    val bgColor = parseHexColor(g.backgroundColorHex, StudioCharcoal)
    val outlineColor = parseHexColor(g.outlineColorHex, StudioVoid)
    val scaledFontSp = (g.fontSizeSp.toFloat() * stageScaleFactor).coerceIn(7f, 34f)
    val primaryDisplay = if (g.entranceAnimation.equals("TYPEWRITER", ignoreCase = true)) {
        g.primaryText.take(evaluated.visiblePrimaryChars.coerceAtLeast(0))
    } else {
        g.primaryText
    }

    val shadow = if (g.shadowEnabled) {
        Shadow(color = Color.Black.copy(alpha = 0.78f), offset = Offset(1.5f, 1.5f), blurRadius = 4f)
    } else null

    val primaryStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = if (g.templateType == "QUOTE_CARD") FontWeight.Medium else FontWeight.Bold,
        fontStyle = if (g.templateType == "QUOTE_CARD") FontStyle.Italic else FontStyle.Normal,
        fontSize = scaledFontSp.sp,
        lineHeight = (scaledFontSp * g.lineSpacingMult.toFloat()).sp,
        letterSpacing = (g.letterSpacingSp.toFloat() + evaluated.letterSpacingBoostSp).sp,
        color = textColor,
        shadow = shadow
    )

    val bgAlpha = g.backgroundOpacity.toFloat().coerceIn(0f, 1f)
    val outlineBorder = if (g.outlineWidthDp > 0.1) {
        BorderStroke(g.outlineWidthDp.dp, outlineColor)
    } else if (bgAlpha > 0.05f) {
        BorderStroke(1.dp, accentColor.copy(alpha = 0.55f))
    } else null

    Surface(
        color = if (bgAlpha > 0.02f) bgColor.copy(alpha = bgAlpha) else Color.Transparent,
        border = outlineBorder,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = resolveColumnAlign(g.alignment),
            verticalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = (10f * stageScaleFactor).coerceAtLeast(6f).dp, vertical = (7f * stageScaleFactor).coerceAtLeast(4f).dp)
        ) {
            if (g.templateType in setOf("CHAPTER_TITLE", "SECTION_TITLE", "MAIN_TITLE", "DOCUMENTARY_TITLE_CARD")) {
                Box(
                    modifier = Modifier
                        .width((46f * stageScaleFactor).coerceAtLeast(24f).dp)
                        .height(2.5.dp)
                        .background(accentColor, RoundedCornerShape(2.dp))
                )
            }
            if (g.templateType == "QUOTE_CARD") {
                Text(
                    text = "“",
                    style = TextStyle(fontFamily = FontFamily.Serif, fontSize = (scaledFontSp * 1.4f).sp, color = accentColor),
                    lineHeight = (scaledFontSp * 1.1f).sp
                )
            }
            if (g.tertiaryText.isNotBlank() && g.templateType in setOf("DOCUMENTARY_TITLE_CARD", "INFORMATION_CARD")) {
                Text(
                    text = g.tertiaryText.uppercase(),
                    style = TextStyle(
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = (scaledFontSp * 0.45f).coerceAtLeast(6.5f).sp,
                        letterSpacing = 1.2.sp,
                        color = accentColor,
                        fontWeight = FontWeight.SemiBold
                    ),
                    textAlign = resolveTextAlign(g.alignment),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = primaryDisplay,
                style = primaryStyle,
                textAlign = resolveTextAlign(g.alignment),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            if (g.secondaryText.isNotBlank()) {
                Text(
                    text = g.secondaryText,
                    style = TextStyle(
                        fontFamily = PlusJakartaSansFamily,
                        fontSize = (scaledFontSp * 0.58f).coerceAtLeast(7f).sp,
                        color = if (g.templateType == "DATE_TIME_CAPTION") accentColor else StudioTextSecondary,
                        shadow = shadow
                    ),
                    textAlign = resolveTextAlign(g.alignment),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (g.tertiaryText.isNotBlank() && g.templateType == "CREDITS") {
                Text(
                    text = g.tertiaryText,
                    style = TextStyle(
                        fontFamily = PlusJakartaSansFamily,
                        fontSize = (scaledFontSp * 0.50f).coerceAtLeast(6.5f).sp,
                        color = accentColor
                    ),
                    textAlign = resolveTextAlign(g.alignment),
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun StageLowerThirdCard(
    evaluated: EvaluatedGraphicFrame,
    stageScaleFactor: Float
) {
    val g = evaluated.graphic
    val fontFamily = remember(g.fontFamilyKey) { resolveStudioFontFamily(g.fontFamilyKey) }
    val textColor = parseHexColor(g.textColorHex, StudioTextPrimary)
    val accentColor = parseHexColor(g.accentColorHex, StudioAmber)
    val bgColor = parseHexColor(g.backgroundColorHex, StudioCharcoal)
    val scaledFontSp = (g.fontSizeSp.toFloat() * stageScaleFactor).coerceIn(7f, 22f)
    val primaryDisplay = if (g.entranceAnimation.equals("TYPEWRITER", ignoreCase = true)) {
        g.primaryText.take(evaluated.visiblePrimaryChars.coerceAtLeast(0))
    } else {
        g.primaryText
    }

    val shape = when (g.layoutPreset.uppercase()) {
        "EDITORIAL_PILL" -> RoundedCornerShape(18.dp)
        "MINIMAL_SPLIT" -> RoundedCornerShape(2.dp)
        else -> RoundedCornerShape(5.dp)
    }

    Surface(
        color = bgColor.copy(alpha = g.backgroundOpacity.toFloat().coerceIn(0.15f, 1f)),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.65f)),
        shape = shape,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = (8f * stageScaleFactor).coerceAtLeast(5f).dp, vertical = (5f * stageScaleFactor).coerceAtLeast(3.5f).dp)
        ) {
            if (g.layoutPreset.uppercase() != "EDITORIAL_PILL") {
                Box(
                    modifier = Modifier
                        .width(if (g.layoutPreset.uppercase() == "ARCHIVAL_ACCENT") 5.dp else 3.dp)
                        .height((28f * stageScaleFactor).coerceAtLeast(18f).dp)
                        .background(accentColor, RoundedCornerShape(2.dp))
                )
                Spacer(Modifier.width(7.dp))
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(1.5.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = primaryDisplay,
                    style = TextStyle(
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = scaledFontSp.sp,
                        letterSpacing = (g.letterSpacingSp.toFloat() + evaluated.letterSpacingBoostSp).sp,
                        color = textColor
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (g.secondaryText.isNotBlank()) {
                    Text(
                        text = g.secondaryText,
                        style = TextStyle(
                            fontFamily = PlusJakartaSansFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = (scaledFontSp * 0.72f).coerceAtLeast(6.5f).sp,
                            color = accentColor
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                val metaLine = listOf(g.tertiaryText, g.quaternaryText).filter { it.isNotBlank() }.joinToString(" · ")
                if (metaLine.isNotBlank()) {
                    Text(
                        text = metaLine,
                        style = TextStyle(
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = (scaledFontSp * 0.58f).coerceAtLeast(6f).sp,
                            color = StudioTextSecondary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun StageInfographicCard(
    graphic: MotionGraphicElement,
    internalProgress: Float,
    stageScaleFactor: Float
) {
    val dataset = remember(graphic.dataPoints) {
        MotionGraphicsEngine.validateInfographicData(graphic.dataPoints)
    }
    val bgColor = parseHexColor(graphic.backgroundColorHex, StudioCharcoal)
    val accentColor = parseHexColor(graphic.accentColorHex, StudioAmber)
    val textColor = parseHexColor(graphic.textColorHex, StudioTextPrimary)
    val titleSp = (graphic.fontSizeSp.toFloat() * stageScaleFactor).coerceIn(8f, 18f)

    Surface(
        color = bgColor.copy(alpha = graphic.backgroundOpacity.toFloat().coerceIn(0.25f, 1f)),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding((8f * stageScaleFactor).coerceAtLeast(5f).dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = graphic.primaryText.ifBlank { graphic.title },
                        style = TextStyle(
                            fontFamily = resolveStudioFontFamily(graphic.fontFamilyKey),
                            fontWeight = FontWeight.Bold,
                            fontSize = titleSp.sp,
                            color = textColor
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (graphic.secondaryText.isNotBlank()) {
                        Text(
                            text = graphic.secondaryText,
                            style = TextStyle(
                                fontFamily = PlusJakartaSansFamily,
                                fontSize = (titleSp * 0.66f).coerceAtLeast(6.5f).sp,
                                color = StudioTextSecondary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (graphic.dataUnit.isNotBlank()) {
                    Surface(
                        color = accentColor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = graphic.dataUnit.uppercase(),
                            style = TextStyle(
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = (titleSp * 0.58f).coerceAtLeast(6f).sp,
                                color = accentColor,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val points = dataset.points
                val p = internalProgress.coerceIn(0f, 1f)
                when (graphic.infographicType.uppercase()) {
                    "BAR_CHART" -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Canvas(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                if (points.isEmpty()) return@Canvas
                                val slotW = size.width / points.size.toFloat()
                                val barW = (slotW * 0.58f).coerceAtLeast(6f)
                                val baseY = size.height - 4f
                                drawLine(StudioBorder, Offset(0f, baseY), Offset(size.width, baseY), strokeWidth = 1.5f)
                                points.forEachIndexed { idx, dp ->
                                    val ratio = (dp.value.coerceAtLeast(0.0) / dataset.maxPositiveValue).toFloat().coerceIn(0f, 1f)
                                    val barH = (ratio * (baseY - 8f) * p).coerceAtLeast(2f)
                                    val left = idx * slotW + (slotW - barW) / 2f
                                    val barColor = parseHexColor(dp.colorHex, accentColor)
                                    drawRoundRect(
                                        color = barColor,
                                        topLeft = Offset(left, baseY - barH),
                                        size = Size(barW, barH),
                                        cornerRadius = CornerRadius(4f, 4f)
                                    )
                                }
                            }
                            Row(
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                points.forEach { dp ->
                                    val animVal = MotionGraphicsEngine.evaluateAnimatedNumber(dp.value, p).roundToInt()
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "$animVal",
                                            style = TextStyle(fontFamily = JetBrainsMonoFamily, fontSize = 7.5.sp, color = parseHexColor(dp.colorHex, accentColor), fontWeight = FontWeight.Bold),
                                            maxLines = 1
                                        )
                                        Text(
                                            text = dp.label,
                                            style = TextStyle(fontFamily = PlusJakartaSansFamily, fontSize = 7.sp, color = StudioTextSecondary),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    "LINE_CHART" -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Canvas(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                if (points.size < 2) return@Canvas
                                val padX = 12f
                                val padY = 10f
                                val w = (size.width - padX * 2f).coerceAtLeast(10f)
                                val h = (size.height - padY * 2f).coerceAtLeast(10f)
                                // Horizontal gridlines
                                for (gIdx in 0..2) {
                                    val gy = padY + (h / 2f) * gIdx
                                    drawLine(StudioBorder.copy(alpha = 0.5f), Offset(padX, gy), Offset(size.width - padX, gy), 1f)
                                }
                                val coords = points.mapIndexed { idx, dp ->
                                    val x = padX + (idx.toFloat() / points.lastIndex.toFloat()) * w
                                    val normY = (dp.value.coerceAtLeast(0.0) / dataset.maxPositiveValue).toFloat().coerceIn(0f, 1f)
                                    val y = padY + (1f - normY) * h
                                    Offset(x, y)
                                }
                                val totalSegments = coords.lastIndex.toFloat()
                                val scaledSeg = p * totalSegments
                                val linePath = Path()
                                linePath.moveTo(coords.first().x, coords.first().y)
                                for (i in 0 until coords.lastIndex) {
                                    if (scaledSeg <= i) break
                                    val segT = (scaledSeg - i).coerceIn(0f, 1f)
                                    val a = coords[i]
                                    val b = coords[i + 1]
                                    val tip = Offset(a.x + (b.x - a.x) * segT, a.y + (b.y - a.y) * segT)
                                    linePath.lineTo(tip.x, tip.y)
                                }
                                drawPath(
                                    path = linePath,
                                    color = accentColor,
                                    style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )
                                coords.forEachIndexed { idx, pt ->
                                    if (scaledSeg >= idx - 0.05f) {
                                        val c = parseHexColor(points[idx].colorHex, accentColor)
                                        drawCircle(color = StudioVoid, radius = 5f, center = pt)
                                        drawCircle(color = c, radius = 3.5f, center = pt)
                                    }
                                }
                            }
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                            ) {
                                points.forEach { dp ->
                                    Text(
                                        text = dp.label,
                                        style = TextStyle(fontFamily = JetBrainsMonoFamily, fontSize = 7.sp, color = StudioTextSecondary),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    "PIE_CHART" -> {
                        val sweeps = MotionGraphicsEngine.evaluatePieSweeps(dataset, p)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Canvas(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                val d = min(size.width, size.height) * 0.86f
                                val tl = Offset((size.width - d) / 2f, (size.height - d) / 2f)
                                var startAngle = -90f
                                sweeps.forEachIndexed { idx, sweep ->
                                    if (sweep > 0.2f) {
                                        val sliceColor = parseHexColor(points[idx].colorHex, accentColor)
                                        drawArc(
                                            color = sliceColor,
                                            startAngle = startAngle,
                                            sweepAngle = sweep,
                                            useCenter = true,
                                            topLeft = tl,
                                            size = Size(d, d)
                                        )
                                        startAngle += sweep
                                    }
                                }
                                // Donut center hole for modern documentary aesthetic
                                drawCircle(color = bgColor, radius = d * 0.24f, center = center)
                            }
                            Column(
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                points.take(5).forEach { dp ->
                                    val pct = ((dp.value / dataset.totalAbsoluteValue) * 100.0).roundToInt()
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(Modifier.size(7.dp).clip(CircleShape).background(parseHexColor(dp.colorHex, accentColor)))
                                        Text(
                                            text = "${dp.label} ($pct%)",
                                            style = TextStyle(fontFamily = PlusJakartaSansFamily, fontSize = 7.5.sp, color = textColor),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    "PERCENTAGE" -> {
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            points.take(3).forEach { dp ->
                                val pct = dp.value.coerceIn(0.0, 100.0)
                                val animPct = (pct * p).roundToInt()
                                val ringColor = parseHexColor(dp.colorHex, accentColor)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(50.dp)) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            val strokeW = 5.dp.toPx()
                                            drawArc(
                                                color = StudioBorder,
                                                startAngle = 0f,
                                                sweepAngle = 360f,
                                                useCenter = false,
                                                style = Stroke(strokeW)
                                            )
                                            drawArc(
                                                color = ringColor,
                                                startAngle = -90f,
                                                sweepAngle = (pct * 3.6 * p).toFloat(),
                                                useCenter = false,
                                                style = Stroke(strokeW, cap = StrokeCap.Round)
                                            )
                                        }
                                        Text(
                                            text = "$animPct%",
                                            style = TextStyle(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = ringColor)
                                        )
                                    }
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        text = dp.label,
                                        style = TextStyle(fontFamily = PlusJakartaSansFamily, fontSize = 7.5.sp, color = textColor),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    "STATISTIC", "COUNTER" -> {
                        val lead = points.firstOrNull()
                        val animatedVal = MotionGraphicsEngine.evaluateAnimatedNumber(lead?.value ?: 0.0, p)
                        val displayNum = if (animatedVal >= 1000.0) {
                            String.format("%,d", animatedVal.roundToInt())
                        } else if (animatedVal % 1.0 == 0.0) {
                            "${animatedVal.roundToInt()}"
                        } else {
                            String.format("%.1f", animatedVal)
                        }
                        val statColor = parseHexColor(lead?.colorHex ?: graphic.accentColorHex, accentColor)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = "$displayNum ${graphic.dataUnit}".trim(),
                                style = TextStyle(
                                    fontFamily = if (graphic.infographicType == "COUNTER") JetBrainsMonoFamily else SpaceGroteskFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = (28f * stageScaleFactor).coerceIn(16f, 34f).sp,
                                    color = statColor
                                ),
                                maxLines = 1
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.55f * p.coerceAtLeast(0.05f))
                                    .height(2.5.dp)
                                    .background(statColor, RoundedCornerShape(2.dp))
                            )
                            if (lead != null && (lead.calloutText.isNotBlank() || lead.label.isNotBlank())) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = lead.calloutText.ifBlank { lead.label },
                                    style = TextStyle(fontFamily = PlusJakartaSansFamily, fontSize = 8.5.sp, color = textColor),
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    "COMPARISON" -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            points.take(4).forEach { dp ->
                                val c1 = parseHexColor(dp.colorHex, StudioAmber)
                                val frac1 = (dp.value.coerceAtLeast(0.0) / dataset.maxPositiveValue).toFloat().coerceIn(0.04f, 1f) * p
                                val frac2 = (dp.secondaryValue.coerceAtLeast(0.0) / dataset.maxPositiveValue).toFloat().coerceIn(0.04f, 1f) * p
                                Column(verticalArrangement = Arrangement.spacedBy(1.5.dp)) {
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text(dp.label, style = TextStyle(fontFamily = PlusJakartaSansFamily, fontSize = 7.5.sp, color = textColor), maxLines = 1)
                                        Text(
                                            "${MotionGraphicsEngine.evaluateAnimatedNumber(dp.value, p).roundToInt()} vs ${MotionGraphicsEngine.evaluateAnimatedNumber(dp.secondaryValue, p).roundToInt()}",
                                            style = TextStyle(fontFamily = JetBrainsMonoFamily, fontSize = 7.sp, color = StudioTextSecondary)
                                        )
                                    }
                                    Box(Modifier.fillMaxWidth(frac1.coerceAtLeast(0.02f)).height(5.dp).background(c1, RoundedCornerShape(2.dp)))
                                    Box(Modifier.fillMaxWidth(frac2.coerceAtLeast(0.02f)).height(5.dp).background(StudioTeal, RoundedCornerShape(2.dp)))
                                }
                            }
                        }
                    }

                    "TIMELINE" -> {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Canvas(modifier = Modifier.fillMaxWidth().height(26.dp)) {
                                if (points.isEmpty()) return@Canvas
                                val y = size.height / 2f
                                drawLine(StudioBorder, Offset(10f, y), Offset(size.width - 10f, y), 2.5f)
                                val progX = 10f + (size.width - 20f) * p
                                drawLine(accentColor, Offset(10f, y), Offset(progX, y), 3f)
                                points.forEachIndexed { idx, dp ->
                                    val tPos = if (points.size == 1) 0.5f else idx.toFloat() / points.lastIndex.toFloat()
                                    val x = 10f + (size.width - 20f) * tPos
                                    if (p >= tPos * 0.9f) {
                                        drawCircle(parseHexColor(dp.colorHex, accentColor), radius = if (dp.highlighted) 6.5f else 4.5f, center = Offset(x, y))
                                    }
                                }
                            }
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                points.forEachIndexed { idx, dp ->
                                    val tPos = if (points.size == 1) 0f else idx.toFloat() / points.lastIndex.toFloat()
                                    val alpha = if (p >= tPos * 0.85f) 1f else 0.2f
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f).graphicsLayer { this.alpha = alpha }
                                    ) {
                                        Text(dp.label, style = TextStyle(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold, fontSize = 8.sp, color = parseHexColor(dp.colorHex, accentColor)), maxLines = 1)
                                        Text(dp.calloutText, style = TextStyle(fontFamily = PlusJakartaSansFamily, fontSize = 6.5.sp, color = StudioTextSecondary), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }

                    else -> { // PROCESS & FLOW
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            points.take(5).forEachIndexed { idx, dp ->
                                val stepThreshold = idx.toFloat() / points.size.coerceAtLeast(1).toFloat()
                                val stepAlpha = ((p - stepThreshold) / 0.28f).coerceIn(0f, 1f)
                                val stepColor = parseHexColor(dp.colorHex, accentColor)
                                Surface(
                                    color = stepColor.copy(alpha = 0.16f),
                                    border = BorderStroke(1.dp, stepColor.copy(alpha = 0.8f)),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(0.75f)
                                        .graphicsLayer {
                                            alpha = stepAlpha
                                            scaleX = 0.85f + 0.15f * stepAlpha
                                            scaleY = 0.85f + 0.15f * stepAlpha
                                        }
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(4.dp)
                                    ) {
                                        Text(dp.label, style = TextStyle(fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Bold, fontSize = 8.sp, color = stepColor), textAlign = TextAlign.Center, maxLines = 1)
                                        if (dp.calloutText.isNotBlank()) {
                                            Text(dp.calloutText, style = TextStyle(fontFamily = PlusJakartaSansFamily, fontSize = 6.5.sp, color = textColor), textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                                if (idx < points.take(5).lastIndex) {
                                    Text("→", color = accentColor.copy(alpha = stepAlpha), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StageDocumentaryMapCard(
    graphic: MotionGraphicElement,
    localTimeSec: Double,
    stageScaleFactor: Float
) {
    val mapCam = remember(graphic, localTimeSec) {
        MotionGraphicsEngine.evaluateMapCameraAtTime(graphic, localTimeSec)
    }
    val accentColor = parseHexColor(graphic.accentColorHex, StudioAmber)
    val safeMapUri = remember(graphic.mapBackgroundUri) { safeMediaUriOrNull(graphic.mapBackgroundUri) }

    val (bgA, bgB, gridColor, contourColor) = when (graphic.mapPresetStyle.uppercase()) {
        "ARCHIVAL_PARCHMENT" -> QuadMapPalette(Color(0xFF2E261C), Color(0xFF1D1711), Color(0xFF5A4A36), Color(0xFFC59B5F))
        "TOPO_SLATE" -> QuadMapPalette(Color(0xFF18242B), Color(0xFF0E161B), Color(0xFF2D434F), Color(0xFF4CA8B8))
        else -> QuadMapPalette(Color(0xFF111926), Color(0xFF0A0F18), Color(0xFF24344B), Color(0xFF3E6B89))
    }

    Surface(
        color = bgB,
        border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize().clipToBounds()) {
            // Animated Zoom + Pan Map Surface
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = mapCam.zoom
                        scaleY = mapCam.zoom
                        translationX = mapCam.panXNorm * size.width * 0.35f
                        translationY = mapCam.panYNorm * size.height * 0.35f
                    }
            ) {
                if (safeMapUri != null) {
                    AsyncImage(
                        model = safeMapUri,
                        contentDescription = graphic.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Built-in stylized documentary cartographic backdrop (no external API key required)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(Brush.radialGradient(listOf(bgA, bgB)))
                        // Graticule grid
                        val cols = 6
                        val rows = 4
                        for (c in 1 until cols) {
                            val x = size.width * (c.toFloat() / cols)
                            drawLine(gridColor.copy(alpha = 0.45f), Offset(x, 0f), Offset(x, size.height), 1f)
                        }
                        for (r in 1 until rows) {
                            val y = size.height * (r.toFloat() / rows)
                            drawLine(gridColor.copy(alpha = 0.45f), Offset(0f, y), Offset(size.width, y), 1f)
                        }
                        // Stylized cartographic ridge & coastline contours
                        val ridge1 = Path().apply {
                            moveTo(0f, size.height * 0.78f)
                            cubicTo(size.width * 0.25f, size.height * 0.52f, size.width * 0.55f, size.height * 0.62f, size.width, size.height * 0.28f)
                        }
                        val ridge2 = Path().apply {
                            moveTo(0f, size.height * 0.44f)
                            cubicTo(size.width * 0.32f, size.height * 0.24f, size.width * 0.68f, size.height * 0.40f, size.width, size.height * 0.14f)
                        }
                        drawPath(ridge1, contourColor.copy(alpha = 0.42f), style = Stroke(2f))
                        drawPath(ridge2, contourColor.copy(alpha = 0.28f), style = Stroke(1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))))
                    }
                }

                // Progressive Animated Routes & Directional Arrowheads
                Canvas(modifier = Modifier.fillMaxSize()) {
                    graphic.mapRoutes.forEach { route ->
                        val reveal = MotionGraphicsEngine.evaluateRouteRevealFraction(route, localTimeSec)
                        if (reveal <= 0.01f) return@forEach
                        val start = Offset(route.fromX.toFloat() * size.width, route.fromY.toFloat() * size.height)
                        val end = Offset(route.toX.toFloat() * size.width, route.toY.toFloat() * size.height)
                        val tip = Offset(
                            start.x + (end.x - start.x) * reveal,
                            start.y + (end.y - start.y) * reveal
                        )
                        val routeColor = parseHexColor(route.colorHex, StudioTeal)
                        drawLine(
                            color = routeColor.copy(alpha = 0.32f),
                            start = start,
                            end = tip,
                            strokeWidth = 7f,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = routeColor,
                            start = start,
                            end = tip,
                            strokeWidth = 3f,
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f))
                        )
                        if (route.showArrow && reveal > 0.15f) {
                            val angle = atan2((tip.y - start.y).toDouble(), (tip.x - start.x).toDouble())
                            val headLen = 10f
                            val p1 = Offset(
                                (tip.x - headLen * cos(angle - PI / 6)).toFloat(),
                                (tip.y - headLen * sin(angle - PI / 6)).toFloat()
                            )
                            val p2 = Offset(
                                (tip.x - headLen * cos(angle + PI / 6)).toFloat(),
                                (tip.y - headLen * sin(angle + PI / 6)).toFloat()
                            )
                            val arrowPath = Path().apply {
                                moveTo(tip.x, tip.y)
                                lineTo(p1.x, p1.y)
                                lineTo(p2.x, p2.y)
                                close()
                            }
                            drawPath(arrowPath, color = routeColor)
                        }
                    }
                }

                // Route distance captions + Staggered Animated Pins/Markers
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val w = maxWidth
                    val h = maxHeight

                    graphic.mapRoutes.forEach { route ->
                        val reveal = MotionGraphicsEngine.evaluateRouteRevealFraction(route, localTimeSec)
                        if (reveal > 0.45f && route.distanceCaption.isNotBlank()) {
                            val midX = ((route.fromX + route.toX) / 2.0).toFloat()
                            val midY = ((route.fromY + route.toY) / 2.0).toFloat()
                            Surface(
                                color = StudioVoid.copy(alpha = 0.82f),
                                border = BorderStroke(0.75.dp, parseHexColor(route.colorHex, StudioTeal)),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier
                                    .offset(x = (w * midX - 24.dp).coerceAtLeast(2.dp), y = (h * midY - 10.dp).coerceAtLeast(2.dp))
                            ) {
                                Text(
                                    text = route.distanceCaption,
                                    style = TextStyle(fontFamily = JetBrainsMonoFamily, fontSize = 6.5.sp, color = parseHexColor(route.colorHex, StudioTeal)),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    graphic.mapMarkers.forEach { marker ->
                        val appear = MotionGraphicsEngine.evaluateMarkerAppearProgress(marker, localTimeSec)
                        if (appear <= 0.01f) return@forEach
                        val pinColor = parseHexColor(marker.colorHex, accentColor)
                        val px = (w * marker.xNorm.toFloat() - 8.dp).coerceAtLeast(2.dp)
                        val py = (h * marker.yNorm.toFloat() - 8.dp).coerceAtLeast(2.dp)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .offset(x = px, y = py)
                                .graphicsLayer {
                                    alpha = appear
                                    scaleX = 0.6f + 0.4f * appear
                                    scaleY = 0.6f + 0.4f * appear
                                }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(if (marker.highlighted) 14.dp else 10.dp)
                                    .clip(CircleShape)
                                    .background(pinColor.copy(alpha = 0.28f))
                                    .border(1.5.dp, pinColor, CircleShape)
                            ) {
                                Box(Modifier.size(4.5.dp).clip(CircleShape).background(pinColor))
                            }
                            if (marker.label.isNotBlank()) {
                                Spacer(Modifier.width(3.dp))
                                Surface(
                                    color = StudioVoid.copy(alpha = 0.82f),
                                    shape = RoundedCornerShape(3.dp)
                                ) {
                                    Text(
                                        text = marker.label,
                                        style = TextStyle(fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Bold, fontSize = (7.5f * stageScaleFactor).coerceIn(6.5f, 10f).sp, color = StudioTextPrimary),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Map Header Badge (stays crisp over zoom/pan)
            Surface(
                color = StudioVoid.copy(alpha = 0.80f),
                border = BorderStroke(0.75.dp, accentColor.copy(alpha = 0.65f)),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)) {
                    Text(
                        text = graphic.primaryText.ifBlank { graphic.title },
                        style = TextStyle(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold, fontSize = 8.sp, color = accentColor),
                        maxLines = 1
                    )
                    if (graphic.secondaryText.isNotBlank()) {
                        Text(
                            text = graphic.secondaryText,
                            style = TextStyle(fontFamily = PlusJakartaSansFamily, fontSize = 6.5.sp, color = StudioTextSecondary),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

private data class QuadMapPalette(
    val bgA: Color,
    val bgB: Color,
    val grid: Color,
    val contour: Color
)

/**
 * Renders Phase 6 structured timed subtitles with customizable style, position, alignment,
 * font, color, background box, and outline, falling back smoothly to Phase 5 dialogue / legacy subtitleText.
 */
@Composable
fun StageStyledSubtitleOverlay(
    scene: StudioScene,
    sceneTimeSec: Double,
    mutedTracks: Set<TimelineTrackType>,
    stageScaleFactor: Float,
    modifier: Modifier = Modifier
) {
    if (TimelineTrackType.SUBTITLE in mutedTracks) return

    val activePhase6Cues = remember(scene.subtitleCues, sceneTimeSec) {
        SubtitleEngine.activeSubtitlesAtTime(scene.subtitleCues, sceneTimeSec)
    }

    val displayText = if (activePhase6Cues.isNotEmpty()) {
        activePhase6Cues.joinToString("\n") { cue ->
            if (cue.speakerName.isNotBlank()) "${cue.speakerName}: ${cue.text}" else cue.text
        }
    } else if (scene.subtitleCues.isEmpty()) {
        val liveDialogue = LipSyncEngine.activeDialogueSubtitleAtTime(scene, sceneTimeSec)
        liveDialogue?.takeIf { it.isNotBlank() } ?: scene.subtitleText
    } else {
        ""
    }

    if (displayText.isBlank()) return

    val styleCfg = scene.subtitleStyle
    val fontFamily = remember(styleCfg.fontFamilyKey) { resolveStudioFontFamily(styleCfg.fontFamilyKey) }
    val textColor = parseHexColor(styleCfg.textColorHex, StudioTextPrimary)
    val bgColor = parseHexColor(styleCfg.backgroundColorHex, StudioVoid)
    val outlineColor = parseHexColor(styleCfg.outlineColorHex, Color.Black)
    val scaledSize = (styleCfg.fontSizeSp.toFloat() * stageScaleFactor).coerceIn(8.5f, 20f)

    val alignment = when (styleCfg.verticalPosition.uppercase()) {
        "TOP" -> Alignment.TopCenter
        "CENTER" -> Alignment.Center
        else -> Alignment.BottomCenter
    }

    Box(
        contentAlignment = alignment,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = bgColor.copy(alpha = styleCfg.backgroundOpacity.toFloat().coerceIn(0f, 1f)),
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = displayText,
                style = TextStyle(
                    fontFamily = fontFamily,
                    fontSize = scaledSize.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor,
                    shadow = if (styleCfg.outlineEnabled) {
                        Shadow(color = outlineColor, offset = Offset(1.5f, 1.5f), blurRadius = 3f)
                    } else null
                ),
                textAlign = resolveTextAlign(styleCfg.alignment),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Applies real dual-scene visual transition compositing (CUT, FADE, CROSSFADE, DISSOLVE, WIPE, SLIDE, ZOOM, BLUR)
 * between an outgoing scene composable and an incoming scene composable.
 */
@Composable
fun DualSceneTransitionBox(
    transforms: TransitionCompositorTransforms,
    outgoingContent: @Composable () -> Unit,
    incomingContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier.clipToBounds()
    ) {
        val wPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val hPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        // Outgoing Scene A
        if (transforms.outgoingAlpha > 0.005f) {
            val outBlurMod = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && transforms.outgoingBlurRadiusDp > 0.4f) {
                Modifier.blur(transforms.outgoingBlurRadiusDp.dp)
            } else Modifier
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(outBlurMod)
                    .graphicsLayer {
                        alpha = transforms.outgoingAlpha
                        translationX = transforms.outgoingOffsetXFraction * wPx
                        translationY = transforms.outgoingOffsetYFraction * hPx
                        scaleX = transforms.outgoingScale
                        scaleY = transforms.outgoingScale
                    }
            ) {
                outgoingContent()
            }
        }

        // Incoming Scene B
        if (transforms.incomingAlpha > 0.005f) {
            val wipeMod = if (transforms.incomingWipeClipFraction < 0.999f) {
                Modifier.clip(wipeClipShape(transforms.incomingWipeClipFraction, transforms.wipeDirection))
            } else Modifier
            val inBlurMod = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && transforms.incomingBlurRadiusDp > 0.4f) {
                Modifier.blur(transforms.incomingBlurRadiusDp.dp)
            } else Modifier
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(wipeMod)
                    .then(inBlurMod)
                    .graphicsLayer {
                        alpha = transforms.incomingAlpha
                        translationX = transforms.incomingOffsetXFraction * wPx
                        translationY = transforms.incomingOffsetYFraction * hPx
                        scaleX = transforms.incomingScale
                        scaleY = transforms.incomingScale
                    }
            ) {
                incomingContent()
            }
        }

        // Dip-to-black veil for FADE transition
        if (transforms.dipBlackOverlayAlpha > 0.005f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = transforms.dipBlackOverlayAlpha))
            )
        }
    }
}
