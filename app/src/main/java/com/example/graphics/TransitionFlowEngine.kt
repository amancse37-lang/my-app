package com.example.graphics

import com.example.animation.EasingType
import com.example.data.SceneTransitionConfig
import com.example.data.StudioScene
import kotlin.math.min

/**
 * Active dual-scene transition snapshot at a global playhead timestamp.
 *
 * Uses a non-mutating boundary window `[boundary - halfDur, boundary + halfDur]`
 * so individual scene durations and keyframe timestamps are NEVER corrupted.
 */
data class ActiveTransitionFrame(
    val outgoingScene: StudioScene,
    val incomingScene: StudioScene,
    val outgoingSceneIndex: Int,
    val incomingSceneIndex: Int,
    val outgoingLocalTimeSec: Double,
    val incomingLocalTimeSec: Double,
    val transition: SceneTransitionConfig,
    val progress: Float
)

/**
 * Deterministic compositor visual transforms for outgoing Scene A and incoming Scene B.
 */
data class TransitionCompositorTransforms(
    val outgoingAlpha: Float,
    val incomingAlpha: Float,
    val outgoingOffsetXFraction: Float = 0f,
    val outgoingOffsetYFraction: Float = 0f,
    val incomingOffsetXFraction: Float = 0f,
    val incomingOffsetYFraction: Float = 0f,
    val outgoingScale: Float = 1f,
    val incomingScale: Float = 1f,
    val incomingWipeClipFraction: Float = 1f,
    val wipeDirection: String = "LEFT",
    val outgoingBlurRadiusDp: Float = 0f,
    val incomingBlurRadiusDp: Float = 0f,
    val dipBlackOverlayAlpha: Float = 0f
)

/**
 * Story Flow sequence descriptor for a scene and its boundary transition into the next scene.
 */
data class StoryFlowSegment(
    val scene: StudioScene,
    val index: Int,
    val startSec: Double,
    val endSec: Double,
    val hasNextScene: Boolean,
    val outgoingTransition: SceneTransitionConfig,
    val previewBoundaryTimeSec: Double
)

/**
 * Pure, deterministic Phase 6 Scene Transition & Story Flow Engine.
 *
 * Supports real dual-scene visual compositing for:
 * CUT, FADE, CROSSFADE, DISSOLVE, WIPE, SLIDE, ZOOM, and BLUR.
 */
object TransitionFlowEngine {

    const val MIN_DURATION_SEC = 0.2
    const val MAX_DURATION_SEC = 3.0

    fun normalizeTransition(config: SceneTransitionConfig): SceneTransitionConfig {
        val validTypes = setOf("CUT", "FADE", "CROSSFADE", "DISSOLVE", "WIPE", "SLIDE", "ZOOM", "BLUR")
        val validDirs = setOf("LEFT", "RIGHT", "UP", "DOWN", "IN", "OUT")
        val type = config.type.trim().uppercase().takeIf { it in validTypes } ?: "CUT"
        val dir = config.direction.trim().uppercase().takeIf { it in validDirs } ?: "LEFT"
        val safeDur = if (config.durationSec.isNaN() || config.durationSec.isInfinite()) {
            0.8
        } else {
            config.durationSec.coerceIn(MIN_DURATION_SEC, MAX_DURATION_SEC)
        }
        val safeFeather = if (config.feather.isNaN() || config.feather.isInfinite()) {
            0.25
        } else {
            config.feather.coerceIn(0.0, 1.0)
        }
        return config.copy(
            type = type,
            durationSec = safeDur,
            direction = dir,
            feather = safeFeather
        )
    }

    /**
     * Computes story flow timing segments across all scenes without modifying scene durations.
     */
    fun computeStoryFlowSegments(scenes: List<StudioScene>): List<StoryFlowSegment> {
        var cursor = 0.0
        return scenes.mapIndexed { idx, scene ->
            val dur = scene.durationSec.coerceAtLeast(1.0)
            val start = cursor
            val end = cursor + dur
            cursor = end
            val normalizedTransition = normalizeTransition(scene.outgoingTransition)
            val hasNext = idx < scenes.lastIndex
            val previewJump = if (hasNext && normalizedTransition.type != "CUT") {
                val half = (normalizedTransition.durationSec / 2.0).coerceAtMost(dur * 0.45)
                (end - half - 0.15).coerceAtLeast(start)
            } else {
                (end - 1.0).coerceAtLeast(start)
            }
            StoryFlowSegment(
                scene = scene,
                index = idx,
                startSec = start,
                endSec = end,
                hasNextScene = hasNext,
                outgoingTransition = normalizedTransition,
                previewBoundaryTimeSec = previewJump
            )
        }
    }

    /**
     * Resolves whether [globalPlayheadSec] currently falls inside an active transition
     * window between consecutive scenes `i` and `i + 1`.
     *
     * Returns `null` for `CUT` transitions or when playhead is outside all boundary windows.
     */
    fun resolveTransitionAtPlayhead(
        scenes: List<StudioScene>,
        globalPlayheadSec: Double
    ): ActiveTransitionFrame? {
        if (scenes.size < 2 || globalPlayheadSec.isNaN() || globalPlayheadSec.isInfinite()) return null
        val segments = computeStoryFlowSegments(scenes)

        for (i in 0 until segments.lastIndex) {
            val outSeg = segments[i]
            val inSeg = segments[i + 1]
            val transition = outSeg.outgoingTransition
            if (transition.type == "CUT") continue

            val outDur = outSeg.scene.durationSec.coerceAtLeast(1.0)
            val inDur = inSeg.scene.durationSec.coerceAtLeast(1.0)
            // Cap effective transition duration so it never exceeds 80% of either adjacent scene
            val maxAllowed = min(outDur, inDur) * 0.8
            val effectiveDur = transition.durationSec.coerceIn(MIN_DURATION_SEC, maxAllowed.coerceAtLeast(MIN_DURATION_SEC))
            val half = effectiveDur / 2.0

            val boundary = outSeg.endSec
            val windowStart = boundary - half
            val windowEnd = boundary + half

            if (globalPlayheadSec >= windowStart && globalPlayheadSec <= windowEnd) {
                val rawProgress = ((globalPlayheadSec - windowStart) / effectiveDur).coerceIn(0.0, 1.0)
                val easing = EasingType.fromKeyOrDefault(transition.easing, EasingType.EASE_IN_OUT)
                val eased = easing.apply(rawProgress).toFloat().coerceIn(0f, 1f)

                val outLocal = (globalPlayheadSec - outSeg.startSec).coerceIn(0.0, outDur)
                val inLocal = (globalPlayheadSec - windowStart).coerceIn(0.0, inDur)
                return ActiveTransitionFrame(
                    outgoingScene = outSeg.scene,
                    incomingScene = inSeg.scene,
                    outgoingSceneIndex = i,
                    incomingSceneIndex = i + 1,
                    outgoingLocalTimeSec = outLocal,
                    incomingLocalTimeSec = inLocal,
                    transition = transition.copy(durationSec = effectiveDur),
                    progress = eased
                )
            }
        }
        return null
    }

    /**
     * Computes exact visual transforms for dual-scene compositing at eased [progress] in [0f..1f].
     */
    fun evaluateTransitionTransforms(
        transition: SceneTransitionConfig,
        progress: Float
    ): TransitionCompositorTransforms {
        val p = if (progress.isNaN() || progress.isInfinite()) 0f else progress.coerceIn(0f, 1f)
        val norm = normalizeTransition(transition)

        return when (norm.type) {
            "CUT" -> TransitionCompositorTransforms(
                outgoingAlpha = if (p < 0.5f) 1f else 0f,
                incomingAlpha = if (p < 0.5f) 0f else 1f
            )
            "FADE" -> {
                // Smooth fade-through-black dip
                if (p < 0.5f) {
                    val outFade = 1f - (p * 2f)
                    TransitionCompositorTransforms(
                        outgoingAlpha = outFade.coerceIn(0f, 1f),
                        incomingAlpha = 0f,
                        dipBlackOverlayAlpha = (p * 2f).coerceIn(0f, 1f)
                    )
                } else {
                    val inFade = (p - 0.5f) * 2f
                    TransitionCompositorTransforms(
                        outgoingAlpha = 0f,
                        incomingAlpha = inFade.coerceIn(0f, 1f),
                        dipBlackOverlayAlpha = (1f - inFade).coerceIn(0f, 1f)
                    )
                }
            }
            "CROSSFADE" -> TransitionCompositorTransforms(
                outgoingAlpha = (1f - p).coerceIn(0f, 1f),
                incomingAlpha = p.coerceIn(0f, 1f)
            )
            "DISSOLVE" -> {
                // Cinema smoothstep optical dissolve with subtle contrast preservation
                val smooth = p * p * (3f - 2f * p)
                TransitionCompositorTransforms(
                    outgoingAlpha = (1f - smooth * 0.85f).coerceIn(0f, 1f),
                    incomingAlpha = smooth.coerceIn(0f, 1f)
                )
            }
            "WIPE" -> TransitionCompositorTransforms(
                outgoingAlpha = 1f,
                incomingAlpha = 1f,
                incomingWipeClipFraction = p,
                wipeDirection = norm.direction
            )
            "SLIDE" -> {
                val (outDx, outDy, inDx, inDy) = when (norm.direction) {
                    "RIGHT" -> Quad(p, 0f, -(1f - p), 0f)
                    "UP" -> Quad(0f, -p, 0f, (1f - p))
                    "DOWN" -> Quad(0f, p, 0f, -(1f - p))
                    else -> Quad(-p, 0f, (1f - p), 0f) // LEFT default
                }
                TransitionCompositorTransforms(
                    outgoingAlpha = 1f,
                    incomingAlpha = 1f,
                    outgoingOffsetXFraction = outDx,
                    outgoingOffsetYFraction = outDy,
                    incomingOffsetXFraction = inDx,
                    incomingOffsetYFraction = inDy
                )
            }
            "ZOOM" -> {
                val zoomIn = norm.direction != "OUT"
                val outScale = if (zoomIn) 1f + p * 0.45f else 1f - p * 0.30f
                val inScale = if (zoomIn) 0.72f + p * 0.28f else 1.38f - p * 0.38f
                TransitionCompositorTransforms(
                    outgoingAlpha = (1f - p).coerceIn(0f, 1f),
                    incomingAlpha = p.coerceIn(0f, 1f),
                    outgoingScale = outScale.coerceIn(0.5f, 2.0f),
                    incomingScale = inScale.coerceIn(0.5f, 2.0f)
                )
            }
            "BLUR" -> {
                val peakBlur = 18f * (0.45f + norm.feather.toFloat() * 0.55f)
                val outBlur = (p * peakBlur).coerceAtLeast(0f)
                val inBlur = ((1f - p) * peakBlur).coerceAtLeast(0f)
                TransitionCompositorTransforms(
                    outgoingAlpha = (1f - p).coerceIn(0f, 1f),
                    incomingAlpha = p.coerceIn(0f, 1f),
                    outgoingBlurRadiusDp = outBlur,
                    incomingBlurRadiusDp = inBlur
                )
            }
            else -> TransitionCompositorTransforms(
                outgoingAlpha = (1f - p).coerceIn(0f, 1f),
                incomingAlpha = p.coerceIn(0f, 1f)
            )
        }
    }

    /**
     * Maps Phase 5 AI `transitionSuggestion` strings to structured Phase 6 [SceneTransitionConfig].
     */
    fun fromPlanSuggestion(suggestion: String): SceneTransitionConfig {
        val low = suggestion.trim().lowercase()
        return when {
            "dissolve" in low -> SceneTransitionConfig(type = "DISSOLVE", durationSec = 1.0)
            "cross" in low -> SceneTransitionConfig(type = "CROSSFADE", durationSec = 0.9)
            "fade" in low || "dip" in low -> SceneTransitionConfig(type = "FADE", durationSec = 1.0)
            "wipe" in low -> SceneTransitionConfig(type = "WIPE", durationSec = 0.8, direction = "LEFT")
            "slide" in low -> SceneTransitionConfig(type = "SLIDE", durationSec = 0.8, direction = "LEFT")
            "zoom" in low -> SceneTransitionConfig(type = "ZOOM", durationSec = 0.8, direction = "IN")
            "blur" in low -> SceneTransitionConfig(type = "BLUR", durationSec = 0.9)
            else -> SceneTransitionConfig(type = "CUT", durationSec = 0.6)
        }
    }

    private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
}
