package com.example.graphics

import com.example.animation.AnimationEvaluator
import com.example.animation.AnimationTargetType
import com.example.animation.EasingType
import com.example.compositor.StageTransformMath
import com.example.data.AiGraphicSuggestion
import com.example.data.AnimationKeyframe
import com.example.data.InfographicDataPoint
import com.example.data.MapMarkerItem
import com.example.data.MapRouteItem
import com.example.data.MotionGraphicElement
import com.example.data.ScriptAiService
import com.example.data.StudioScene
import com.example.data.TimelineTrackType
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Deterministic frame evaluation for a single [MotionGraphicElement] at a scene timestamp `t`.
 */
data class EvaluatedGraphicFrame(
    val graphic: MotionGraphicElement,
    val visible: Boolean,
    val localTimeSec: Double,
    val normalizedLifeProgress: Double,
    val posX: Double,
    val posY: Double,
    val scaleX: Double,
    val scaleY: Double,
    val rotationDeg: Double,
    val opacity: Float,
    val clipRevealFraction: Float,
    val visiblePrimaryChars: Int,
    val letterSpacingBoostSp: Float,
    val internalProgress: Float,
    val hasKeyframes: Boolean
)

/**
 * Validated, normalized infographic dataset safe for rendering and math.
 */
data class ValidatedInfographicDataset(
    val points: List<InfographicDataPoint>,
    val maxPositiveValue: Double,
    val totalAbsoluteValue: Double,
    val sanitizedWarnings: List<String>
)

/**
 * Evaluated camera zoom & pan state for a documentary map graphic at local time `t`.
 */
data class EvaluatedMapCamera(
    val zoom: Float,
    val panXNorm: Float,
    val panYNorm: Float
)

/**
 * Pure, deterministic Phase 6 engine covering:
 * - Animated typography & entrance/exit presets
 * - 13 documentary title/lower-third/card templates
 * - Editable infographic normalization & animated chart/counter progress
 * - Non-destructive documentary map markers, routes, and animated zoom/pan
 * - Pure-Kotlin JSON template serialization for Asset Library reuse
 */
object MotionGraphicsEngine {

    const val MAX_GRAPHICS_PER_SCENE = 30

    fun trackForGraphic(graphic: MotionGraphicElement): TimelineTrackType {
        return when (graphic.category.uppercase()) {
            "LOWER_THIRD" -> TimelineTrackType.LOWER_THIRDS
            "INFOGRAPHIC" -> TimelineTrackType.INFOGRAPHICS
            "MAP" -> TimelineTrackType.MAPS
            "TEXT", "TITLE" -> TimelineTrackType.TITLES_TEXT
            else -> TimelineTrackType.MOTION_GRAPHICS
        }
    }

    fun isGraphicTrackMuted(
        graphic: MotionGraphicElement,
        mutedTracks: Set<TimelineTrackType>
    ): Boolean {
        if (TimelineTrackType.MOTION_GRAPHICS in mutedTracks) return true
        return trackForGraphic(graphic) in mutedTracks
    }

    /**
     * Evaluates a [MotionGraphicElement] deterministically at [sceneTimeSec],
     * layering Phase 3 [AnimationKeyframe] overrides together with Phase 6
     * entrance/exit presets and internal infographic/map progress.
     */
    fun evaluateGraphicAtTime(
        graphic: MotionGraphicElement,
        sceneKeyframes: List<AnimationKeyframe>,
        sceneTimeSec: Double
    ): EvaluatedGraphicFrame {
        val safeTime = if (sceneTimeSec.isNaN() || sceneTimeSec.isInfinite()) 0.0 else sceneTimeSec.coerceAtLeast(0.0)
        val safeStart = graphic.startSec.coerceAtLeast(0.0)
        val safeDuration = graphic.durationSec.coerceAtLeast(0.3)
        val endSec = safeStart + safeDuration

        if (!graphic.visible || safeTime < safeStart - 1e-6 || safeTime > endSec + 1e-6) {
            return EvaluatedGraphicFrame(
                graphic = graphic,
                visible = false,
                localTimeSec = (safeTime - safeStart).coerceIn(0.0, safeDuration),
                normalizedLifeProgress = 0.0,
                posX = graphic.posX,
                posY = graphic.posY,
                scaleX = graphic.scale,
                scaleY = graphic.scale,
                rotationDeg = graphic.rotationDeg,
                opacity = 0f,
                clipRevealFraction = 0f,
                visiblePrimaryChars = 0,
                letterSpacingBoostSp = 0f,
                internalProgress = 0f,
                hasKeyframes = false
            )
        }

        val localTime = (safeTime - safeStart).coerceIn(0.0, safeDuration)
        val lifeNorm = (localTime / safeDuration).coerceIn(0.0, 1.0)

        // 1. Evaluate Phase 3 Keyframes targeting this graphic (if any)
        val evalTransform = AnimationEvaluator.evaluateTransform(
            keyframes = sceneKeyframes,
            targetType = AnimationTargetType.GRAPHIC,
            targetId = graphic.graphicId,
            timeSec = safeTime,
            basePosX = graphic.posX,
            basePosY = graphic.posY,
            baseScale = graphic.scale,
            baseRotationDeg = graphic.rotationDeg,
            baseOpacity = graphic.opacity,
            baseFlipX = false
        )

        val easing = EasingType.fromKeyOrDefault(graphic.animationEasing, EasingType.EASE_OUT)
        val entDur = graphic.entranceDurationSec.coerceIn(0.0, safeDuration * 0.65)
        val exitDur = graphic.exitDurationSec.coerceIn(0.0, safeDuration * 0.5)

        val entRaw = if (entDur <= 1e-4) 1.0 else (localTime / entDur).coerceIn(0.0, 1.0)
        val entT = easing.apply(entRaw)

        val remaining = (safeDuration - localTime).coerceAtLeast(0.0)
        val exitRaw = if (exitDur <= 1e-4 || remaining >= exitDur) 0.0 else (1.0 - (remaining / exitDur)).coerceIn(0.0, 1.0)
        val exitT = easing.apply(exitRaw)

        var posX = evalTransform.posX
        var posY = evalTransform.posY
        var scaleMult = 1.0
        var alphaMult = 1.0
        var clipReveal = 1f
        var visibleChars = graphic.primaryText.length
        var letterSpacingBoost = 0f

        // 2. Entrance Animation Preset
        when (graphic.entranceAnimation.uppercase()) {
            "FADE_IN" -> {
                alphaMult *= entT
            }
            "SLIDE_IN" -> {
                posX += (1.0 - entT) * -120.0
                alphaMult *= (0.25 + 0.75 * entT).coerceIn(0.0, 1.0)
            }
            "RISE" -> {
                posY += (1.0 - entT) * 56.0
                alphaMult *= entT
            }
            "DROP" -> {
                posY += (1.0 - entT) * -56.0
                alphaMult *= entT
            }
            "ZOOM_IN" -> {
                scaleMult *= (0.72 + 0.28 * entT)
                alphaMult *= entT
            }
            "POP" -> {
                // Smooth overshoot bounce that settles cleanly to 1.0
                val popScale = if (entRaw < 0.72) {
                    val u = entRaw / 0.72
                    0.5 + 0.62 * EasingType.EASE_OUT.apply(u)
                } else {
                    val u = (entRaw - 0.72) / 0.28
                    1.12 - 0.12 * EasingType.EASE_IN_OUT.apply(u)
                }
                scaleMult *= popScale
                alphaMult *= (entRaw * 3.0).coerceIn(0.0, 1.0)
            }
            "REVEAL" -> {
                clipReveal = entT.toFloat().coerceIn(0.01f, 1f)
            }
            "TYPEWRITER" -> {
                val totalChars = graphic.primaryText.length
                val typeWindow = (safeDuration * 0.55).coerceAtLeast(entDur.coerceAtLeast(0.6))
                val typeProgress = (localTime / typeWindow).coerceIn(0.0, 1.0)
                visibleChars = if (totalChars == 0) 0 else (totalChars * typeProgress).roundToInt().coerceIn(1, totalChars)
            }
            "TRACKING_REVEAL" -> {
                letterSpacingBoost = ((1.0 - entT) * 7.5).toFloat()
                alphaMult *= entT
            }
            "NONE" -> Unit
        }

        // 3. Exit Animation Preset
        if (exitT > 1e-4) {
            when (graphic.exitAnimation.uppercase()) {
                "FADE_OUT" -> {
                    alphaMult *= (1.0 - exitT)
                }
                "SLIDE_OUT" -> {
                    posX += exitT * 120.0
                    alphaMult *= (1.0 - exitT)
                }
                "ZOOM_OUT" -> {
                    scaleMult *= (1.0 - 0.28 * exitT)
                    alphaMult *= (1.0 - exitT)
                }
                "NONE" -> Unit
            }
        }

        val finalScaleX = (evalTransform.scale * scaleMult * if (evalTransform.flipX) -1.0 else 1.0)
        val finalScaleY = (evalTransform.scale * scaleMult)
        val finalOpacity = (evalTransform.opacity * alphaMult).toFloat().coerceIn(0f, 1f)

        // Internal deterministic progress for infographics (chart growth, counters, timeline steps)
        val animGrowWindow = (safeDuration * 0.65).coerceIn(0.6, 3.2)
        val internalProgress = easing.apply((localTime / animGrowWindow).coerceIn(0.0, 1.0)).toFloat()

        return EvaluatedGraphicFrame(
            graphic = graphic,
            visible = finalOpacity > 0.005f,
            localTimeSec = localTime,
            normalizedLifeProgress = lifeNorm,
            posX = posX.coerceIn(StageTransformMath.MIN_POS_X, StageTransformMath.MAX_POS_X),
            posY = posY.coerceIn(StageTransformMath.MIN_POS_Y, StageTransformMath.MAX_POS_Y),
            scaleX = finalScaleX,
            scaleY = finalScaleY,
            rotationDeg = evalTransform.rotationDeg,
            opacity = finalOpacity,
            clipRevealFraction = clipReveal,
            visiblePrimaryChars = visibleChars,
            letterSpacingBoostSp = letterSpacingBoost,
            internalProgress = internalProgress,
            hasKeyframes = evalTransform.hasTransformKeyframes
        )
    }

    /**
     * Validates and sanitizes infographic data points so NaN/Infinity/empty states never crash canvas rendering.
     */
    fun validateInfographicData(points: List<InfographicDataPoint>): ValidatedInfographicDataset {
        val warnings = mutableListOf<String>()
        val sanitized = points.mapIndexed { idx, dp ->
            val v = if (dp.value.isNaN() || dp.value.isInfinite()) {
                warnings.add("Row ${idx + 1} primary value reset to 0.")
                0.0
            } else dp.value
            val sv = if (dp.secondaryValue.isNaN() || dp.secondaryValue.isInfinite()) {
                warnings.add("Row ${idx + 1} secondary value reset to 0.")
                0.0
            } else dp.secondaryValue
            dp.copy(
                id = dp.id.ifBlank { "dp_$idx" },
                label = dp.label.trim().ifBlank { "Item ${idx + 1}" },
                value = v,
                secondaryValue = sv
            )
        }
        val maxPos = sanitized.maxOfOrNull { max(abs(it.value), abs(it.secondaryValue)) }?.coerceAtLeast(1.0) ?: 1.0
        val totalAbs = sanitized.sumOf { abs(it.value) }.coerceAtLeast(1.0)
        return ValidatedInfographicDataset(
            points = sanitized,
            maxPositiveValue = maxPos,
            totalAbsoluteValue = totalAbs,
            sanitizedWarnings = warnings
        )
    }

    /**
     * Evaluates a numeric counter or animated metric deterministically at [internalProgress] in [0..1].
     */
    fun evaluateAnimatedNumber(targetValue: Double, internalProgress: Float): Double {
        if (targetValue.isNaN() || targetValue.isInfinite()) return 0.0
        return targetValue * internalProgress.coerceIn(0f, 1f).toDouble()
    }

    /**
     * Computes deterministic pie chart sweep angles (in degrees) animated by [internalProgress].
     */
    fun evaluatePieSweeps(dataset: ValidatedInfographicDataset, internalProgress: Float): List<Float> {
        if (dataset.points.isEmpty()) return emptyList()
        val total = dataset.totalAbsoluteValue.coerceAtLeast(1e-6)
        val sweepBudget = 360f * internalProgress.coerceIn(0f, 1f)
        var remaining = sweepBudget
        return dataset.points.map { dp ->
            val fullSlice = ((abs(dp.value) / total) * 360.0).toFloat()
            val drawn = fullSlice.coerceAtMost(remaining.coerceAtLeast(0f))
            remaining = (remaining - drawn).coerceAtLeast(0f)
            drawn
        }
    }

    /**
     * Evaluates documentary map animated camera zoom & pan deterministically at local graphic time.
     */
    fun evaluateMapCameraAtTime(graphic: MotionGraphicElement, localTimeSec: Double): EvaluatedMapCamera {
        val dur = graphic.durationSec.coerceAtLeast(0.3)
        val rawT = (localTimeSec / dur).coerceIn(0.0, 1.0)
        val t = EasingType.fromKeyOrDefault(graphic.animationEasing, EasingType.EASE_IN_OUT).apply(rawT)
        val zoom = (graphic.mapZoomStart + (graphic.mapZoomEnd - graphic.mapZoomStart) * t).coerceIn(0.5, 3.0)
        val panX = (graphic.mapPanStartX + (graphic.mapPanEndX - graphic.mapPanStartX) * t).coerceIn(-1.0, 1.0)
        val panY = (graphic.mapPanStartY + (graphic.mapPanEndY - graphic.mapPanStartY) * t).coerceIn(-1.0, 1.0)
        return EvaluatedMapCamera(
            zoom = zoom.toFloat(),
            panXNorm = panX.toFloat(),
            panYNorm = panY.toFloat()
        )
    }

    /**
     * Evaluates progressive route reveal progress in [0f..1f] at local graphic time.
     */
    fun evaluateRouteRevealFraction(route: MapRouteItem, localTimeSec: Double): Float {
        val start = route.startSec.coerceAtLeast(0.0)
        val dur = route.durationSec.coerceAtLeast(0.1)
        if (localTimeSec < start) return 0f
        val raw = ((localTimeSec - start) / dur).coerceIn(0.0, 1.0)
        return EasingType.EASE_OUT.apply(raw).toFloat()
    }

    /**
     * Evaluates animated map marker appearance scale/alpha in [0f..1f] at local graphic time.
     */
    fun evaluateMarkerAppearProgress(marker: MapMarkerItem, localTimeSec: Double): Float {
        val start = marker.appearAtSec.coerceAtLeast(0.0)
        if (localTimeSec < start) return 0f
        val raw = ((localTimeSec - start) / 0.35).coerceIn(0.0, 1.0)
        return EasingType.EASE_OUT.apply(raw).toFloat()
    }

    /**
     * Factory creating ready-to-edit graphics for all 13 documentary templates,
     * all 10 infographic types, and documentary map graphics.
     */
    fun createTemplateGraphic(
        templateType: String,
        scene: StudioScene,
        startSec: Double = 0.25,
        infographicTypeOverride: String? = null,
        idSuffix: String = "${System.currentTimeMillis() % 100000}"
    ): MotionGraphicElement {
        val sceneDur = scene.durationSec.coerceAtLeast(2.0)
        val safeStart = startSec.coerceIn(0.0, (sceneDur - 0.6).coerceAtLeast(0.0))
        val defaultDur = (sceneDur - safeStart).coerceIn(1.5, 5.0)
        val loc = scene.scenePlan.location.ifBlank { scene.title }

        return when (templateType.uppercase()) {
            "MAIN_TITLE" -> MotionGraphicElement(
                graphicId = "gfx_main_$idSuffix",
                title = "Main Title",
                category = "TITLE",
                templateType = "MAIN_TITLE",
                startSec = safeStart,
                durationSec = defaultDur,
                posX = 0.0,
                posY = -10.0,
                widthFraction = 0.78,
                primaryText = scene.title.uppercase(),
                secondaryText = "A DOCUMENTARY CHRONICLE",
                fontFamilyKey = "SPACE_GROTESK",
                fontSizeSp = 26.0,
                textColorHex = "#F2F4F8",
                accentColorHex = "#F5A623",
                backgroundColorHex = "#0A0C10",
                backgroundOpacity = 0.72,
                letterSpacingSp = 2.4,
                entranceAnimation = "TRACKING_REVEAL",
                exitAnimation = "FADE_OUT"
            )
            "SCENE_TITLE" -> MotionGraphicElement(
                graphicId = "gfx_scene_$idSuffix",
                title = "Scene Title",
                category = "TITLE",
                templateType = "SCENE_TITLE",
                startSec = safeStart,
                durationSec = defaultDur,
                posX = -150.0,
                posY = -175.0,
                widthFraction = 0.56,
                primaryText = scene.title,
                secondaryText = "SCENE ${scene.orderIndex + 1}",
                fontFamilyKey = "SPACE_GROTESK",
                fontSizeSp = 17.0,
                alignment = "LEFT",
                entranceAnimation = "SLIDE_IN",
                exitAnimation = "FADE_OUT"
            )
            "DOCUMENTARY_TITLE_CARD" -> MotionGraphicElement(
                graphicId = "gfx_doccard_$idSuffix",
                title = "Documentary Title Card",
                category = "TITLE",
                templateType = "DOCUMENTARY_TITLE_CARD",
                startSec = safeStart,
                durationSec = defaultDur,
                posX = 0.0,
                posY = 0.0,
                widthFraction = 0.82,
                primaryText = scene.title,
                secondaryText = scene.visualSummary.ifBlank { "Archival Visual Reconstruction" },
                tertiaryText = scene.scenePlan.location.ifBlank { "Documentary Feature" },
                fontFamilyKey = "SERIF",
                fontSizeSp = 24.0,
                backgroundColorHex = "#12141A",
                backgroundOpacity = 0.90,
                entranceAnimation = "FADE_IN",
                exitAnimation = "FADE_OUT"
            )
            "SECTION_TITLE" -> MotionGraphicElement(
                graphicId = "gfx_sec_$idSuffix",
                title = "Section Title",
                category = "TITLE",
                templateType = "SECTION_TITLE",
                startSec = safeStart,
                durationSec = defaultDur,
                posX = 0.0,
                posY = -20.0,
                widthFraction = 0.66,
                primaryText = "PART II · ${scene.title.uppercase()}",
                secondaryText = scene.scenePlan.emotionalTone.ifBlank { "FIELD INVESTIGATION" },
                fontFamilyKey = "SPACE_GROTESK",
                fontSizeSp = 21.0,
                accentColorHex = "#26D0CE",
                entranceAnimation = "REVEAL",
                exitAnimation = "FADE_OUT"
            )
            "CHAPTER_TITLE" -> MotionGraphicElement(
                graphicId = "gfx_chap_$idSuffix",
                title = "Chapter Title",
                category = "TITLE",
                templateType = "CHAPTER_TITLE",
                startSec = safeStart,
                durationSec = defaultDur,
                posX = 0.0,
                posY = -125.0,
                widthFraction = 0.72,
                primaryText = scene.motionGraphicTitle.ifBlank { "CHAPTER ${scene.orderIndex + 1} · ${scene.title.uppercase()}" },
                secondaryText = loc,
                fontFamilyKey = "SPACE_GROTESK",
                fontSizeSp = 20.0,
                accentColorHex = "#F5A623",
                entranceAnimation = "RISE",
                exitAnimation = "FADE_OUT"
            )
            "SUBTITLE_CARD" -> MotionGraphicElement(
                graphicId = "gfx_subcard_$idSuffix",
                title = "Subtitle Card",
                category = "TEXT",
                templateType = "SUBTITLE_CARD",
                startSec = safeStart,
                durationSec = defaultDur,
                posX = 0.0,
                posY = 140.0,
                widthFraction = 0.76,
                primaryText = scene.subtitleText.ifBlank { scene.narrationText.take(90).ifBlank { "Key narrative caption" } },
                fontFamilyKey = "PLUS_JAKARTA",
                fontSizeSp = 15.0,
                entranceAnimation = "FADE_IN",
                exitAnimation = "FADE_OUT"
            )
            "LOWER_THIRD", "NAME_DESIGNATION" -> MotionGraphicElement(
                graphicId = "gfx_l3_$idSuffix",
                title = if (templateType.equals("NAME_DESIGNATION", true)) "Name & Designation" else "Lower Third",
                category = "LOWER_THIRD",
                templateType = templateType.uppercase(),
                startSec = safeStart,
                durationSec = defaultDur,
                posX = -155.0,
                posY = 155.0,
                widthFraction = 0.54,
                primaryText = "Dr. Aarav Mehta",
                secondaryText = "Lead Historian & Archival Researcher",
                tertiaryText = loc,
                quaternaryText = scene.scenePlan.timeOfDay.ifBlank { "Interview Archive" },
                fontFamilyKey = "SPACE_GROTESK",
                fontSizeSp = 16.0,
                alignment = "LEFT",
                accentColorHex = "#F5A623",
                backgroundColorHex = "#12141A",
                backgroundOpacity = 0.88,
                layoutPreset = "DOCUMENTARY_BAR",
                entranceAnimation = "SLIDE_IN",
                exitAnimation = "SLIDE_OUT"
            )
            "LOCATION_CAPTION" -> MotionGraphicElement(
                graphicId = "gfx_loc_$idSuffix",
                title = "Location Caption",
                category = "LOWER_THIRD",
                templateType = "LOCATION_CAPTION",
                startSec = safeStart,
                durationSec = defaultDur,
                posX = -210.0,
                posY = -180.0,
                widthFraction = 0.44,
                primaryText = loc.uppercase(),
                secondaryText = scene.scenePlan.environment.ifBlank { "34.1526° N, 77.5771° E" },
                fontFamilyKey = "JETBRAINS_MONO",
                fontSizeSp = 13.0,
                alignment = "LEFT",
                accentColorHex = "#26D0CE",
                layoutPreset = "EDITORIAL_PILL",
                entranceAnimation = "TYPEWRITER",
                exitAnimation = "FADE_OUT"
            )
            "DATE_TIME_CAPTION" -> MotionGraphicElement(
                graphicId = "gfx_date_$idSuffix",
                title = "Date & Time Caption",
                category = "TEXT",
                templateType = "DATE_TIME_CAPTION",
                startSec = safeStart,
                durationSec = defaultDur,
                posX = 220.0,
                posY = -185.0,
                widthFraction = 0.42,
                primaryText = "OCTOBER 1842",
                secondaryText = scene.scenePlan.timeOfDay.ifBlank { "DAWN · ARCHIVE ENTRY" },
                fontFamilyKey = "JETBRAINS_MONO",
                fontSizeSp = 14.0,
                alignment = "RIGHT",
                accentColorHex = "#F5A623",
                entranceAnimation = "TYPEWRITER",
                exitAnimation = "FADE_OUT"
            )
            "QUOTE_CARD" -> MotionGraphicElement(
                graphicId = "gfx_quote_$idSuffix",
                title = "Quote Card",
                category = "TITLE",
                templateType = "QUOTE_CARD",
                startSec = safeStart,
                durationSec = defaultDur,
                posX = 0.0,
                posY = 0.0,
                widthFraction = 0.74,
                primaryText = "\"Every mountain pass carried ink, silk, and human memory.\"",
                secondaryText = "— ARCHIVAL FIELD JOURNAL, 1842",
                fontFamilyKey = "SERIF",
                fontSizeSp = 17.0,
                accentColorHex = "#F5A623",
                backgroundColorHex = "#12141A",
                backgroundOpacity = 0.88,
                entranceAnimation = "RISE",
                exitAnimation = "FADE_OUT"
            )
            "INFORMATION_CARD" -> MotionGraphicElement(
                graphicId = "gfx_info_$idSuffix",
                title = "Information Card",
                category = "TITLE",
                templateType = "INFORMATION_CARD",
                startSec = safeStart,
                durationSec = defaultDur,
                posX = 165.0,
                posY = -20.0,
                widthFraction = 0.48,
                primaryText = "HISTORICAL CONTEXT",
                secondaryText = scene.description.take(120).ifBlank { "Key verified documentary fact card." },
                tertiaryText = "SOURCE: ARCHIVAL SURVEY",
                fontFamilyKey = "PLUS_JAKARTA",
                fontSizeSp = 14.0,
                alignment = "LEFT",
                accentColorHex = "#26D0CE",
                backgroundColorHex = "#12141A",
                backgroundOpacity = 0.88,
                entranceAnimation = "POP",
                exitAnimation = "FADE_OUT"
            )
            "CREDITS" -> MotionGraphicElement(
                graphicId = "gfx_credits_$idSuffix",
                title = "Credits Roll",
                category = "TITLE",
                templateType = "CREDITS",
                startSec = safeStart,
                durationSec = defaultDur,
                posX = 0.0,
                posY = 0.0,
                widthFraction = 0.68,
                primaryText = "DIRECTED & PRODUCED WITH MOTIONSTORY STUDIO",
                secondaryText = "Research & Narration · Documentary Production Unit",
                tertiaryText = "Archival Visuals · Sound Design · Editorial Graphics",
                fontFamilyKey = "SPACE_GROTESK",
                fontSizeSp = 15.0,
                backgroundColorHex = "#0A0C10",
                backgroundOpacity = 0.85,
                entranceAnimation = "RISE",
                exitAnimation = "FADE_OUT"
            )
            "INFOGRAPHIC" -> createInfographicTemplate(
                infographicType = infographicTypeOverride ?: "BAR_CHART",
                scene = scene,
                startSec = safeStart,
                idSuffix = idSuffix
            )
            "MAP_GRAPHIC", "MAP" -> createMapGraphicTemplate(
                scene = scene,
                startSec = safeStart,
                idSuffix = idSuffix
            )
            else -> MotionGraphicElement(
                graphicId = "gfx_txt_$idSuffix",
                title = "Text Overlay",
                category = "TEXT",
                templateType = "MAIN_TITLE",
                startSec = safeStart,
                durationSec = defaultDur,
                primaryText = scene.title,
                entranceAnimation = "FADE_IN",
                exitAnimation = "FADE_OUT"
            )
        }
    }

    fun createInfographicTemplate(
        infographicType: String,
        scene: StudioScene,
        startSec: Double = 0.4,
        idSuffix: String = "${System.currentTimeMillis() % 100000}"
    ): MotionGraphicElement {
        val sceneDur = scene.durationSec.coerceAtLeast(2.0)
        val safeStart = startSec.coerceIn(0.0, (sceneDur - 0.6).coerceAtLeast(0.0))
        val dur = (sceneDur - safeStart).coerceIn(2.0, 5.5)
        val type = infographicType.uppercase()

        val (title, subtitle, unit, points) = when (type) {
            "LINE_CHART" -> Quad(
                "Trade Volume Growth",
                "Recorded annual caravans (1830–1860)",
                "caravans",
                listOf(
                    InfographicDataPoint("dp_1", "1830", 85.0, 0.0, "#26D0CE"),
                    InfographicDataPoint("dp_2", "1840", 142.0, 0.0, "#26D0CE"),
                    InfographicDataPoint("dp_3", "1850", 215.0, 0.0, "#F5A623", true, "Peak route"),
                    InfographicDataPoint("dp_4", "1860", 190.0, 0.0, "#34D399")
                )
            )
            "PIE_CHART" -> Quad(
                "Goods Carried Over Pass",
                "Share of archival manifest records",
                "%",
                listOf(
                    InfographicDataPoint("dp_1", "Silk & Wool", 44.0, 0.0, "#F5A623", true),
                    InfographicDataPoint("dp_2", "Spices & Tea", 28.0, 0.0, "#26D0CE"),
                    InfographicDataPoint("dp_3", "Manuscripts", 16.0, 0.0, "#A78BFA"),
                    InfographicDataPoint("dp_4", "Metals", 12.0, 0.0, "#34D399")
                )
            )
            "PERCENTAGE" -> Quad(
                "Archive Preservation Index",
                "Digitized parchment maps verified",
                "%",
                listOf(
                    InfographicDataPoint("dp_1", "Preserved Maps", 78.0, 100.0, "#26D0CE", true, "78% Verified"),
                    InfographicDataPoint("dp_2", "Translated Entries", 64.0, 100.0, "#F5A623")
                )
            )
            "STATISTIC" -> Quad(
                "HIGHEST CARAVAN PASS",
                "Elevation documented in 1842 survey",
                "m",
                listOf(
                    InfographicDataPoint("dp_1", "Elevation", 5359.0, 0.0, "#F5A623", true, "Above sea level")
                )
            )
            "COUNTER" -> Quad(
                "Archival Folios Indexed",
                "Cumulative historical documents cataloged",
                "folios",
                listOf(
                    InfographicDataPoint("dp_1", "Indexed Folios", 1842.0, 0.0, "#34D399", true, "+310 this decade")
                )
            )
            "TIMELINE" -> Quad(
                "Historical Route Timeline",
                "Key milestones across the frontier",
                "",
                listOf(
                    InfographicDataPoint("dp_1", "1815", 1.0, 0.0, "#26D0CE", false, "First Survey"),
                    InfographicDataPoint("dp_2", "1842", 2.0, 0.0, "#F5A623", true, "Treaty & Atlas"),
                    InfographicDataPoint("dp_3", "1868", 3.0, 0.0, "#A78BFA", false, "Telegraph Line"),
                    InfographicDataPoint("dp_4", "1904", 4.0, 0.0, "#34D399", false, "Modern Road")
                )
            )
            "COMPARISON" -> Quad(
                "Summer vs Winter Crossing",
                "Average travel days by season",
                "days",
                listOf(
                    InfographicDataPoint("dp_1", "North Ridge", 14.0, 29.0, "#F5A623", true, "Summer vs Winter"),
                    InfographicDataPoint("dp_2", "Valley Trail", 9.0, 19.0, "#26D0CE"),
                    InfographicDataPoint("dp_3", "River Ford", 11.0, 24.0, "#A78BFA")
                )
            )
            "PROCESS" -> Quad(
                "Map Restoration Workflow",
                "4-stage archival preservation steps",
                "",
                listOf(
                    InfographicDataPoint("dp_1", "1. Scan", 25.0, 0.0, "#26D0CE", false, "Optical capture"),
                    InfographicDataPoint("dp_2", "2. Clean", 50.0, 0.0, "#F5A623", false, "Ink recovery"),
                    InfographicDataPoint("dp_3", "3. Verify", 75.0, 0.0, "#A78BFA", true, "Cross-check"),
                    InfographicDataPoint("dp_4", "4. Archive", 100.0, 0.0, "#34D399", false, "Public record")
                )
            )
            "FLOW" -> Quad(
                "Information Exchange Flow",
                "How frontier reports reached the capital",
                "",
                listOf(
                    InfographicDataPoint("dp_1", "Outpost", 1.0, 0.0, "#F5A623", false, "Field notes"),
                    InfographicDataPoint("dp_2", "Caravan", 2.0, 0.0, "#26D0CE", true, "Dispatches"),
                    InfographicDataPoint("dp_3", "Bazaar Guild", 3.0, 0.0, "#A78BFA", false, "Calligraphy"),
                    InfographicDataPoint("dp_4", "Archive", 4.0, 0.0, "#34D399", false, "Chronicle")
                )
            )
            else -> Quad(
                "Regional Trade Comparison",
                "Annual recorded tonnage by route",
                "tons",
                listOf(
                    InfographicDataPoint("dp_1", "Ladakh", 145.0, 0.0, "#F5A623", true),
                    InfographicDataPoint("dp_2", "Kashmir", 210.0, 0.0, "#26D0CE"),
                    InfographicDataPoint("dp_3", "Delhi", 280.0, 0.0, "#34D399"),
                    InfographicDataPoint("dp_4", "Agra", 165.0, 0.0, "#A78BFA")
                )
            )
        }

        return MotionGraphicElement(
            graphicId = "gfx_info_${type.lowercase()}_$idSuffix",
            title = title,
            category = "INFOGRAPHIC",
            templateType = "INFOGRAPHIC",
            startSec = safeStart,
            durationSec = dur,
            posX = 0.0,
            posY = 0.0,
            widthFraction = 0.68,
            heightFraction = 0.56,
            primaryText = title,
            secondaryText = subtitle,
            fontFamilyKey = "SPACE_GROTESK",
            fontSizeSp = 15.0,
            accentColorHex = "#F5A623",
            backgroundColorHex = "#12141A",
            backgroundOpacity = 0.90,
            entranceAnimation = "ZOOM_IN",
            exitAnimation = "FADE_OUT",
            infographicType = type,
            dataUnit = unit,
            dataPoints = points
        )
    }

    fun createMapGraphicTemplate(
        scene: StudioScene,
        startSec: Double = 0.3,
        idSuffix: String = "${System.currentTimeMillis() % 100000}"
    ): MotionGraphicElement {
        val sceneDur = scene.durationSec.coerceAtLeast(2.0)
        val safeStart = startSec.coerceIn(0.0, (sceneDur - 0.6).coerceAtLeast(0.0))
        val dur = (sceneDur - safeStart).coerceIn(2.5, 6.0)
        return MotionGraphicElement(
            graphicId = "gfx_map_$idSuffix",
            title = "Documentary Route Map",
            category = "MAP",
            templateType = "MAP_GRAPHIC",
            startSec = safeStart,
            durationSec = dur,
            posX = 0.0,
            posY = 0.0,
            widthFraction = 0.74,
            heightFraction = 0.62,
            primaryText = scene.scenePlan.location.ifBlank { "NORTHERN TRADE CORRIDOR" }.uppercase(),
            secondaryText = "Annotated Documentary Route Map · 1842",
            fontFamilyKey = "JETBRAINS_MONO",
            fontSizeSp = 13.0,
            accentColorHex = "#F5A623",
            backgroundColorHex = "#111A22",
            backgroundOpacity = 0.94,
            entranceAnimation = "FADE_IN",
            exitAnimation = "FADE_OUT",
            mapPresetStyle = "DARK_ATLAS",
            mapZoomStart = 1.0,
            mapZoomEnd = 1.24,
            mapPanStartX = -0.04,
            mapPanStartY = 0.03,
            mapPanEndX = 0.05,
            mapPanEndY = -0.04,
            mapMarkers = listOf(
                MapMarkerItem("mk_1", "Shahjahanabad", "Imperial Hub", 0.24, 0.72, "PIN", "#F5A623", 0.25, true),
                MapMarkerItem("mk_2", "Srinagar", "Valley Waypoint", 0.48, 0.44, "CITY", "#26D0CE", 0.95, false),
                MapMarkerItem("mk_3", "Leh High Pass", "5,359m Frontier", 0.76, 0.24, "REGION", "#34D399", 1.75, true)
            ),
            mapRoutes = listOf(
                MapRouteItem("rt_1", "Old Frontier Trail", "430 km", 0.24, 0.72, 0.48, 0.44, "#F5A623", true, 0.45, 1.2),
                MapRouteItem("rt_2", "High Mountain Pass", "385 km", 0.48, 0.44, 0.76, 0.24, "#26D0CE", true, 1.35, 1.4)
            )
        )
    }

    fun duplicateGraphic(
        source: MotionGraphicElement,
        idSuffix: String = "${System.currentTimeMillis() % 100000}"
    ): MotionGraphicElement {
        return source.copy(
            graphicId = "gfx_dup_$idSuffix",
            title = "${source.title} Copy".take(120),
            posX = (source.posX + 24.0).coerceIn(StageTransformMath.MIN_POS_X, StageTransformMath.MAX_POS_X),
            posY = (source.posY + 20.0).coerceIn(StageTransformMath.MIN_POS_Y, StageTransformMath.MAX_POS_Y),
            zIndex = (source.zIndex + 1).coerceAtMost(100)
        )
    }

    /**
     * Converts a reviewed & approved [AiGraphicSuggestion] into an editable [MotionGraphicElement].
     */
    fun createGraphicFromSuggestion(
        suggestion: AiGraphicSuggestion,
        scene: StudioScene,
        idSuffix: String = "${System.currentTimeMillis() % 100000}"
    ): MotionGraphicElement {
        val base = createTemplateGraphic(
            templateType = suggestion.templateType,
            scene = scene,
            startSec = suggestion.suggestedStartSec,
            infographicTypeOverride = suggestion.infographicType.takeIf { it.isNotBlank() },
            idSuffix = idSuffix
        )
        val parsedPoints = parseSuggestedDataPairs(suggestion.suggestedDataSummary)
        return base.copy(
            title = suggestion.title.ifBlank { base.title }.take(120),
            primaryText = suggestion.primaryText.ifBlank { base.primaryText }.take(600),
            secondaryText = suggestion.secondaryText.ifBlank { base.secondaryText }.take(400),
            startSec = suggestion.suggestedStartSec.coerceIn(0.0, (scene.durationSec - 0.5).coerceAtLeast(0.0)),
            durationSec = suggestion.suggestedDurationSec.coerceIn(1.0, scene.durationSec.coerceAtLeast(2.0)),
            dataPoints = if (base.category == "INFOGRAPHIC" && parsedPoints.isNotEmpty()) parsedPoints else base.dataPoints
        )
    }

    /**
     * Parses simple comma-separated `Label:Number` pairs (e.g. "1840:120, 1860:245") into data rows.
     */
    fun parseSuggestedDataPairs(raw: String): List<InfographicDataPoint> {
        if (raw.isBlank()) return emptyList()
        val colors = listOf("#F5A623", "#26D0CE", "#A78BFA", "#34D399", "#FF5C6C", "#60A5FA")
        return raw.split(",", ";", "\n")
            .mapNotNull { token ->
                val parts = token.split(":")
                if (parts.size < 2) return@mapNotNull null
                val label = parts[0].trim().take(48)
                val valueStr = parts[1].trim().replace(Regex("[^0-9.\\-]"), "")
                val num = valueStr.toDoubleOrNull() ?: return@mapNotNull null
                if (label.isBlank() || num.isNaN() || num.isInfinite()) return@mapNotNull null
                label to num
            }
            .take(10)
            .mapIndexed { idx, (label, value) ->
                InfographicDataPoint(
                    id = "dp_ai_$idx",
                    label = label,
                    value = value,
                    colorHex = colors[idx % colors.size],
                    highlighted = idx == 0
                )
            }
    }

    // --- PURE-KOTLIN JSON TEMPLATE SERIALIZATION FOR ASSET LIBRARY ---

    private fun esc(s: String): String = buildString {
        append('"')
        s.forEach { ch ->
            when (ch) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
        append('"')
    }

    fun serializeGraphicTemplate(g: MotionGraphicElement): String {
        val pointsJson = g.dataPoints.joinToString(",") { dp ->
            """{"id":${esc(dp.id)},"label":${esc(dp.label)},"value":${dp.value},"secondaryValue":${dp.secondaryValue},"colorHex":${esc(dp.colorHex)},"highlighted":${dp.highlighted},"calloutText":${esc(dp.calloutText)}}"""
        }
        val markersJson = g.mapMarkers.joinToString(",") { m ->
            """{"markerId":${esc(m.markerId)},"label":${esc(m.label)},"regionTag":${esc(m.regionTag)},"xNorm":${m.xNorm},"yNorm":${m.yNorm},"markerStyle":${esc(m.markerStyle)},"colorHex":${esc(m.colorHex)},"appearAtSec":${m.appearAtSec},"highlighted":${m.highlighted}}"""
        }
        val routesJson = g.mapRoutes.joinToString(",") { r ->
            """{"routeId":${esc(r.routeId)},"label":${esc(r.label)},"distanceCaption":${esc(r.distanceCaption)},"fromX":${r.fromX},"fromY":${r.fromY},"toX":${r.toX},"toY":${r.toY},"colorHex":${esc(r.colorHex)},"showArrow":${r.showArrow},"startSec":${r.startSec},"durationSec":${r.durationSec}}"""
        }
        return """{"title":${esc(g.title)},"category":${esc(g.category)},"templateType":${esc(g.templateType)},"durationSec":${g.durationSec},"layer":${esc(g.layer)},"attachToCamera":${g.attachToCamera},"posX":${g.posX},"posY":${g.posY},"scale":${g.scale},"rotationDeg":${g.rotationDeg},"widthFraction":${g.widthFraction},"heightFraction":${g.heightFraction},"opacity":${g.opacity},"primaryText":${esc(g.primaryText)},"secondaryText":${esc(g.secondaryText)},"tertiaryText":${esc(g.tertiaryText)},"quaternaryText":${esc(g.quaternaryText)},"fontFamilyKey":${esc(g.fontFamilyKey)},"fontSizeSp":${g.fontSizeSp},"textColorHex":${esc(g.textColorHex)},"accentColorHex":${esc(g.accentColorHex)},"backgroundColorHex":${esc(g.backgroundColorHex)},"backgroundOpacity":${g.backgroundOpacity},"outlineColorHex":${esc(g.outlineColorHex)},"outlineWidthDp":${g.outlineWidthDp},"shadowEnabled":${g.shadowEnabled},"alignment":${esc(g.alignment)},"letterSpacingSp":${g.letterSpacingSp},"lineSpacingMult":${g.lineSpacingMult},"layoutPreset":${esc(g.layoutPreset)},"entranceAnimation":${esc(g.entranceAnimation)},"exitAnimation":${esc(g.exitAnimation)},"entranceDurationSec":${g.entranceDurationSec},"exitDurationSec":${g.exitDurationSec},"animationEasing":${esc(g.animationEasing)},"infographicType":${esc(g.infographicType)},"dataUnit":${esc(g.dataUnit)},"dataPoints":[$pointsJson],"mapBackgroundUri":${esc(g.mapBackgroundUri)},"mapPresetStyle":${esc(g.mapPresetStyle)},"mapZoomStart":${g.mapZoomStart},"mapZoomEnd":${g.mapZoomEnd},"mapPanStartX":${g.mapPanStartX},"mapPanStartY":${g.mapPanStartY},"mapPanEndX":${g.mapPanEndX},"mapPanEndY":${g.mapPanEndY},"mapMarkers":[$markersJson],"mapRoutes":[$routesJson]}"""
    }

    @Suppress("UNCHECKED_CAST")
    fun deserializeGraphicTemplate(
        json: String,
        newGraphicId: String = "gfx_tpl_${System.currentTimeMillis() % 100000}",
        startSec: Double = 0.2
    ): MotionGraphicElement? {
        if (json.isBlank()) return null
        return try {
            val map = ScriptAiService.parseJsonObject(json)
            val nonNullMap = map.filterValues { it != null } as Map<String, Any>
            val parsed = com.example.data.StudioRepository.mapsToMotionGraphics(listOf(nonNullMap)).firstOrNull()
                ?: return null
            parsed.copy(graphicId = newGraphicId, startSec = startSec.coerceAtLeast(0.0), visible = true)
        } catch (_: Exception) {
            null
        }
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
