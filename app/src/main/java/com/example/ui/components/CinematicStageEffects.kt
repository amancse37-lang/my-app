package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import com.example.cinematics.AtmosphereMode
import com.example.cinematics.CinematicEngine
import com.example.cinematics.LetterboxMode
import com.example.cinematics.ResolvedVfxState
import com.example.data.SceneCinematics
import com.example.ui.theme.StudioAmber
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Aerial perspective depth haze veil applied per depth plane behind the focus plane.
 */
@Composable
fun StageDepthHazeVeil(
    alpha: Float,
    tintColor: Color,
    modifier: Modifier = Modifier
) {
    val safeAlpha = alpha.coerceIn(0f, 0.50f)
    if (safeAlpha <= 0.005f) return
    val mistColor = lerp(tintColor, Color(0xFFD8E4EE), 0.55f)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        mistColor.copy(alpha = safeAlpha * 0.72f),
                        mistColor.copy(alpha = safeAlpha)
                    )
                )
            )
    )
}

/**
 * Directional key-light glow and soft opposing shadow falloff rendered directly in Compose Canvas.
 * Lightweight single-pass gradient rendering suitable for low-end Android GPUs.
 */
@Composable
fun StageDirectionalLightingOverlay(
    cinematics: SceneCinematics,
    modifier: Modifier = Modifier
) {
    val glow = cinematics.lightGlowIntensity.toFloat().coerceIn(0f, 1f)
    val shadow = cinematics.shadowIntensity.toFloat().coerceIn(0f, 1f)
    if (glow <= 0.01f && shadow <= 0.01f) return

    val temp = cinematics.temperature.toFloat().coerceIn(-1f, 1f)
    val lightColor = when {
        temp > 0.08f -> lerp(Color(0xFFFFF4DC), Color(0xFFFFB763), temp)
        temp < -0.08f -> lerp(Color(0xFFEAF4FF), Color(0xFF7CB4FF), -temp)
        else -> Color(0xFFFFF8EA)
    }
    val shadowColor = when {
        temp < -0.1f -> Color(0xFF040A18)
        temp > 0.2f -> Color(0xFF120A04)
        else -> Color(0xFF06070A)
    }

    val lightXNorm = ((cinematics.lightSourceX.toFloat().coerceIn(-1f, 1f) + 1f) * 0.5f)
    val lightYNorm = ((cinematics.lightSourceY.toFloat().coerceIn(-1f, 1f) + 1f) * 0.5f)

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val lightCenter = Offset(x = w * lightXNorm, y = h * lightYNorm)
        val diag = max(w, h) * 1.15f

        // Directional opposing shadow gradient (darkens side opposite the key light)
        if (shadow > 0.01f) {
            val shadowCenter = Offset(x = w * (1f - lightXNorm), y = h * (1f - lightYNorm))
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        shadowColor.copy(alpha = (shadow * 0.42f).coerceIn(0f, 0.55f)),
                        shadowColor.copy(alpha = (shadow * 0.68f).coerceIn(0f, 0.78f))
                    ),
                    start = lightCenter,
                    end = shadowCenter
                )
            )
        }

        // Directional key light radial cone + warm/cool atmospheric bloom
        if (glow > 0.01f) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        lightColor.copy(alpha = (glow * 0.48f).coerceIn(0f, 0.55f)),
                        lightColor.copy(alpha = (glow * 0.18f).coerceIn(0f, 0.25f)),
                        Color.Transparent
                    ),
                    center = lightCenter,
                    radius = diag * 0.72f
                )
            )
        }
    }
}

/**
 * Deterministic, stateless procedural atmosphere & weather Canvas renderer.
 * Driven strictly by [timeSec] so scrubbing, pausing, and replaying produce identical frames.
 */
@Composable
fun StageAtmosphereLayer(
    mode: AtmosphereMode,
    timeSec: Double,
    intensity: Double,
    speed: Double,
    windStrength: Double,
    modifier: Modifier = Modifier
) {
    if (mode == AtmosphereMode.NONE || intensity <= 0.01) return

    val particles = remember(mode, timeSec, intensity, speed, windStrength) {
        CinematicEngine.computeAtmosphereParticles(
            mode = mode,
            timeSec = timeSec,
            intensity = intensity,
            speed = speed,
            windStrength = windStrength
        )
    }
    if (particles.isEmpty()) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val minDim = size.minDimension.coerceAtLeast(1f)

        when (mode) {
            AtmosphereMode.RAIN -> {
                particles.forEach { p ->
                    val startX = (p.normX * w).toFloat()
                    val startY = (p.normY * h).toFloat()
                    val rad = (p.angleDeg * PI / 180.0)
                    val lenPx = (p.lengthNorm * h).toFloat()
                    val endX = startX + (sin(rad) * lenPx).toFloat()
                    val endY = startY + (cos(rad) * lenPx).toFloat()
                    drawLine(
                        color = Color(0xFFD6EAFF).copy(alpha = p.alpha.toFloat().coerceIn(0f, 0.85f)),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = (p.sizeNorm * w).toFloat().coerceIn(1f, 3.2f),
                        cap = StrokeCap.Round
                    )
                }
            }

            AtmosphereMode.SNOW -> {
                particles.forEach { p ->
                    val cx = (p.normX * w).toFloat()
                    val cy = (p.normY * h).toFloat()
                    val radius = (p.sizeNorm * minDim).toFloat().coerceIn(1.2f, 7f)
                    drawCircle(
                        color = Color.White.copy(alpha = p.alpha.toFloat().coerceIn(0f, 0.92f)),
                        radius = radius,
                        center = Offset(cx, cy)
                    )
                }
            }

            AtmosphereMode.MIST -> {
                particles.forEach { p ->
                    val cx = (p.normX * w).toFloat()
                    val cy = (p.normY * h).toFloat()
                    val radius = (p.sizeNorm * w).toFloat().coerceAtLeast(24f)
                    val alpha = p.alpha.toFloat().coerceIn(0f, 0.42f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFE2ECF4).copy(alpha = alpha),
                                Color(0xFFD0DCE6).copy(alpha = alpha * 0.45f),
                                Color.Transparent
                            ),
                            center = Offset(cx, cy),
                            radius = radius
                        ),
                        radius = radius,
                        center = Offset(cx, cy)
                    )
                }
            }

            AtmosphereMode.DUST_MOTES -> {
                particles.forEach { p ->
                    val cx = (p.normX * w).toFloat()
                    val cy = (p.normY * h).toFloat()
                    val radius = (p.sizeNorm * minDim).toFloat().coerceIn(1.2f, 5.5f)
                    val alpha = p.alpha.toFloat().coerceIn(0f, 0.85f)
                    drawCircle(
                        color = Color(0xFFFFE6B3).copy(alpha = alpha),
                        radius = radius,
                        center = Offset(cx, cy)
                    )
                }
            }

            AtmosphereMode.SUNBEAMS -> {
                particles.forEach { p ->
                    val topX = (p.normX * w).toFloat()
                    val beamWidth = (p.sizeNorm * w).toFloat().coerceAtLeast(18f)
                    val rad = (p.angleDeg * PI / 180.0)
                    val shiftX = (sin(rad) * h * 1.15).toFloat()
                    val alpha = p.alpha.toFloat().coerceIn(0f, 0.34f)
                    val path = Path().apply {
                        moveTo(topX - beamWidth * 0.45f, -4f)
                        lineTo(topX + beamWidth * 0.45f, -4f)
                        lineTo(topX + shiftX + beamWidth * 1.15f, h + 4f)
                        lineTo(topX + shiftX - beamWidth * 0.75f, h + 4f)
                        close()
                    }
                    drawPath(
                        path = path,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFFE3A3).copy(alpha = alpha),
                                Color(0xFFFFD178).copy(alpha = alpha * 0.40f),
                                Color.Transparent
                            )
                        )
                    )
                }
            }

            AtmosphereMode.EMBERS -> {
                particles.forEach { p ->
                    val cx = (p.normX * w).toFloat()
                    val cy = (p.normY * h).toFloat()
                    val radius = (p.sizeNorm * minDim).toFloat().coerceIn(1.3f, 5.5f)
                    val alpha = p.alpha.toFloat().coerceIn(0f, 0.95f)
                    // Outer warm halo
                    drawCircle(
                        color = Color(0xFFFF6A1A).copy(alpha = alpha * 0.40f),
                        radius = radius * 2.2f,
                        center = Offset(cx, cy)
                    )
                    // Bright hot core
                    drawCircle(
                        color = Color(0xFFFFD27A).copy(alpha = alpha),
                        radius = radius,
                        center = Offset(cx, cy)
                    )
                }
            }

            AtmosphereMode.ARCHIVAL_DUST -> {
                particles.forEach { p ->
                    val cx = (p.normX * w).toFloat()
                    val cy = (p.normY * h).toFloat()
                    val alpha = p.alpha.toFloat().coerceIn(0f, 0.55f)
                    if (p.lengthNorm > 0.05) {
                        val scratchLen = (p.lengthNorm * h).toFloat()
                        drawLine(
                            color = StudioAmber.copy(alpha = alpha * 0.65f),
                            start = Offset(cx, cy),
                            end = Offset(cx + 1.2f, cy + scratchLen),
                            strokeWidth = 1f
                        )
                    } else {
                        drawCircle(
                            color = StudioAmber.copy(alpha = alpha),
                            radius = (p.sizeNorm * minDim).toFloat().coerceIn(1f, 3f),
                            center = Offset(cx, cy)
                        )
                    }
                }
            }

            AtmosphereMode.NONE -> Unit
        }
    }
}

/**
 * Post-processing documentary VFX stack: bloom veil, radial vignette, deterministic film grain,
 * legacy sepia/archival compatibility, and optional cinema letterbox bars.
 */
@Composable
fun StagePostProcessingVfxOverlay(
    vfx: ResolvedVfxState,
    timeSec: Double,
    modifier: Modifier = Modifier
) {
    val hasSepia = vfx.sepiaOverlayAlpha > 0.005
    val hasBloom = vfx.bloomIntensity > 0.01
    val hasVignette = vfx.vignetteIntensity > 0.01
    val hasGrain = vfx.filmGrainIntensity > 0.01 || vfx.legacyArchivalDustAlpha > 0.01
    val barFrac = vfx.letterboxMode.barHeightFraction().toFloat()
    val hasLetterbox = barFrac > 0.002f

    if (!hasSepia && !hasBloom && !hasVignette && !hasGrain && !hasLetterbox) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // 1. Legacy Warm Sepia compatibility tint
        if (hasSepia) {
            drawRect(
                color = Color(0xFF8C5A2B).copy(alpha = vfx.sepiaOverlayAlpha.toFloat().coerceIn(0f, 0.45f))
            )
        }

        // 2. Soft Highlight Bloom glow
        if (hasBloom) {
            val bloomA = (vfx.bloomIntensity.toFloat() * 0.28f).coerceIn(0f, 0.34f)
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFF6E5).copy(alpha = bloomA),
                        Color(0xFFFFE7C2).copy(alpha = bloomA * 0.35f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.5f, h * 0.44f),
                    radius = max(w, h) * 0.70f
                )
            )
        }

        // 3. Smooth Radial Cinema Vignette
        if (hasVignette) {
            val vigA = (vfx.vignetteIntensity.toFloat() * 0.85f).coerceIn(0f, 0.90f)
            drawRect(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to Color.Transparent,
                        0.52f to Color.Transparent,
                        0.82f to Color.Black.copy(alpha = vigA * 0.42f),
                        1.0f to Color.Black.copy(alpha = vigA)
                    ),
                    center = Offset(w * 0.5f, h * 0.5f),
                    radius = max(w, h) * 0.66f
                )
            )
        }

        // 4. Deterministic time-seeded Film Grain / Legacy Archival Dust (strictly capped)
        if (hasGrain) {
            val effectiveGrain = maxOf(vfx.filmGrainIntensity, vfx.legacyArchivalDustAlpha).toFloat().coerceIn(0f, 1f)
            val isWarmDust = vfx.legacyArchivalDustAlpha > vfx.filmGrainIntensity
            val speckCount = (effectiveGrain * CinematicEngine.MAX_FILM_GRAIN_SPECKS)
                .toInt()
                .coerceIn(6, CinematicEngine.MAX_FILM_GRAIN_SPECKS)
            val frameTick = (timeSec.coerceAtLeast(0.0) * 18.0).toInt()
            for (i in 0 until speckCount) {
                val px = ((i * 73 + frameTick * 17) % 997) / 997f * w
                val py = ((i * 131 + frameTick * 29) % 991) / 991f * h
                val speckColor = if (isWarmDust) {
                    StudioAmber.copy(alpha = (effectiveGrain * 0.36f).coerceIn(0f, 0.45f))
                } else {
                    if (i % 3 == 0) {
                        Color.Black.copy(alpha = (effectiveGrain * 0.20f).coerceIn(0f, 0.28f))
                    } else {
                        Color.White.copy(alpha = (effectiveGrain * 0.24f).coerceIn(0f, 0.30f))
                    }
                }
                drawCircle(
                    color = speckColor,
                    radius = if (i % 5 == 0) 1.9f else 1.1f,
                    center = Offset(px, py)
                )
            }
        }

        // 5. Cinema Letterbox Bars (1.85:1 or 2.39:1 Anamorphic)
        if (hasLetterbox) {
            val barHeightPx = h * barFrac
            drawRect(
                color = Color.Black,
                topLeft = Offset(0f, 0f),
                size = Size(w, barHeightPx)
            )
            drawRect(
                color = Color.Black,
                topLeft = Offset(0f, h - barHeightPx),
                size = Size(w, barHeightPx)
            )
        }
    }
}

/**
 * Optional viewport Rule-of-Thirds, center crosshair, and title-safe framing guide.
 * Strictly a studio composition aid (not baked into scene data).
 */
@Composable
fun StageFramingGuidesOverlay(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val guideColor = Color.White.copy(alpha = 0.28f)
        val safeColor = StudioAmber.copy(alpha = 0.26f)

        // Rule of thirds grid
        val x1 = w / 3f
        val x2 = w * 2f / 3f
        val y1 = h / 3f
        val y2 = h * 2f / 3f

        drawLine(guideColor, Offset(x1, 0f), Offset(x1, h), strokeWidth = 1f)
        drawLine(guideColor, Offset(x2, 0f), Offset(x2, h), strokeWidth = 1f)
        drawLine(guideColor, Offset(0f, y1), Offset(w, y1), strokeWidth = 1f)
        drawLine(guideColor, Offset(0f, y2), Offset(w, y2), strokeWidth = 1f)

        // Center crosshair
        val cx = w * 0.5f
        val cy = h * 0.5f
        val cross = 8f
        drawLine(safeColor, Offset(cx - cross, cy), Offset(cx + cross, cy), strokeWidth = 1.2f)
        drawLine(safeColor, Offset(cx, cy - cross), Offset(cx, cy + cross), strokeWidth = 1.2f)

        // 90% Title-Safe inner box
        val padX = w * 0.05f
        val padY = h * 0.05f
        drawRect(
            color = safeColor,
            topLeft = Offset(padX, padY),
            size = Size(w - padX * 2f, h - padY * 2f),
            style = Stroke(width = 1f)
        )
    }
}
