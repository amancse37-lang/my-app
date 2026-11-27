package com.example.cinematics

import com.example.animation.AnimatableProperty
import com.example.animation.AnimationEvaluator
import com.example.animation.AnimationTargetType
import com.example.animation.EasingType
import com.example.data.AnimationKeyframe
import com.example.data.SceneCinematics
import com.example.data.StudioScene
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Supported cinematic camera framing & motion presets.
 * Static presets configure editable base camera framing; motion presets author real editable camera keyframes.
 */
enum class CinematicCameraPreset(
    val label: String,
    val isAnimatedMotion: Boolean,
    val summary: String
) {
    CLOSE_UP("Close-Up", false, "Tight 1.55x portrait framing"),
    MEDIUM_SHOT("Medium Shot", false, "Balanced 1.25x documentary framing"),
    WIDE_SHOT("Wide Shot", false, "Full 1.00x stage view"),
    EXTREME_WIDE("Extreme Wide Shot", false, "Pulled-back 0.85x environmental view"),
    ESTABLISHING("Establishing Shot", true, "Slow wide horizontal sweep"),
    OVERHEAD("Overhead Shot", false, "High-angle tilted 2D framing"),
    DRAMATIC_ZOOM("Dramatic Zoom", true, "Ease-in push from 1.05x to 1.65x"),
    SLOW_PUSH_IN("Slow Push-In", true, "Smooth documentary zoom 1.0x → 1.28x"),
    PULL_OUT("Pull-Out", true, "Reveal pull-back 1.38x → 1.0x"),
    PAN_LEFT("Pan Left", true, "Smooth horizontal pan right → left"),
    PAN_RIGHT("Pan Right", true, "Smooth horizontal pan left → right");

    companion object {
        fun fromLabelOrNull(raw: String): CinematicCameraPreset? {
            return entries.firstOrNull {
                it.label.equals(raw.trim(), ignoreCase = true) ||
                    it.name.equals(raw.trim(), ignoreCase = true)
            }
        }
    }
}

/**
 * Supported documentary lighting & color grading presets.
 */
enum class LightingPreset(
    val label: String,
    val exposure: Double,
    val contrast: Double,
    val saturation: Double,
    val temperature: Double,
    val lightSourceX: Double,
    val lightSourceY: Double,
    val lightGlowIntensity: Double,
    val shadowIntensity: Double,
    val bloomIntensity: Double
) {
    NATURAL("Natural Documentary", 0.0, 1.0, 1.0, 0.0, 0.35, -0.65, 0.0, 0.0, 0.0),
    GOLDEN_HOUR("Golden Hour", 0.06, 1.08, 1.18, 0.58, 0.65, -0.55, 0.48, 0.22, 0.32),
    BLUE_HOUR("Blue Hour / Twilight", -0.08, 1.12, 0.92, -0.62, -0.45, -0.70, 0.28, 0.38, 0.16),
    NOIR("Noir High-Contrast", -0.06, 1.42, 0.18, -0.15, -0.70, -0.60, 0.20, 0.65, 0.08),
    CANDLELIGHT("Warm Candlelight", -0.04, 1.15, 1.12, 0.72, 0.15, 0.10, 0.55, 0.48, 0.36),
    ARCHIVAL_SEPIA("Archival Sepia", -0.02, 1.06, 0.45, 0.68, 0.0, -0.60, 0.18, 0.35, 0.12),
    OVERCAST("Overcast Soft", 0.02, 0.90, 0.84, -0.22, 0.0, -0.85, 0.12, 0.15, 0.08),
    DRAMATIC_RIM("Dramatic Rim Light", -0.05, 1.24, 1.05, 0.20, -0.85, -0.35, 0.60, 0.50, 0.30);

    companion object {
        fun fromLabelOrDefault(raw: String): LightingPreset {
            return entries.firstOrNull {
                it.label.equals(raw.trim(), ignoreCase = true) ||
                    it.name.equals(raw.trim(), ignoreCase = true)
            } ?: NATURAL
        }
    }
}

/**
 * Deterministic environmental atmosphere & weather rendering modes.
 */
enum class AtmosphereMode(val label: String) {
    NONE("None"),
    MIST("Mist / Fog"),
    RAIN("Rain"),
    SNOW("Snow"),
    DUST_MOTES("Floating Dust"),
    SUNBEAMS("Warm Sunbeams"),
    EMBERS("Embers / Sparks"),
    ARCHIVAL_DUST("Archival Film Dust");

    companion object {
        fun fromLabelOrDefault(raw: String): AtmosphereMode {
            return entries.firstOrNull {
                it.label.equals(raw.trim(), ignoreCase = true) ||
                    it.name.equals(raw.trim(), ignoreCase = true)
            } ?: NONE
        }
    }
}

/**
 * Cinema letterbox framing modes.
 */
enum class LetterboxMode(val label: String, val targetAspect: Double?) {
    NONE("None", null),
    CINEMA_185("1.85:1 Cinema", 1.85),
    ANAMORPHIC_239("2.39:1 Anamorphic", 2.39);

    /**
     * Returns the vertical fraction (0.0..0.25) of each top/bottom black bar on a 16:9 canvas.
     */
    fun barHeightFraction(stageAspect: Double = 16.0 / 9.0): Double {
        val target = targetAspect ?: return 0.0
        if (target <= stageAspect) return 0.0
        val activeHeightFrac = stageAspect / target
        return ((1.0 - activeHeightFrac) / 2.0).coerceIn(0.0, 0.25)
    }

    companion object {
        fun fromLabelOrDefault(raw: String): LetterboxMode {
            return entries.firstOrNull {
                it.label.equals(raw.trim(), ignoreCase = true) ||
                    it.name.equals(raw.trim(), ignoreCase = true)
            } ?: NONE
        }
    }
}

/**
 * Evaluated per-layer depth transform derived from camera state + layer depth factor.
 */
data class LayerDepthTransform(
    val effectiveDepthFactor: Double,
    val panX: Double,
    val panY: Double,
    val zoomScale: Double,
    val blurRadiusDp: Double,
    val depthHazeAlpha: Double
)

/**
 * Deterministic handheld camera micro-shake offsets at a timestamp.
 */
data class CameraShakeOffset(
    val offsetX: Double,
    val offsetY: Double,
    val rotationDeg: Double
)

/**
 * Single deterministic atmospheric particle state in normalized [0..1] stage coordinates.
 */
data class AtmosphereParticle(
    val normX: Double,
    val normY: Double,
    val sizeNorm: Double,
    val lengthNorm: Double,
    val alpha: Double,
    val angleDeg: Double
)

/**
 * Resolved post-processing VFX values combining Phase 4 fields with backwards-compatible legacy presets.
 */
data class ResolvedVfxState(
    val vignetteIntensity: Double,
    val filmGrainIntensity: Double,
    val bloomIntensity: Double,
    val sepiaOverlayAlpha: Double,
    val legacyArchivalDustAlpha: Double,
    val letterboxMode: LetterboxMode,
    val cameraShakeIntensity: Double
)

/**
 * Pure, deterministic Phase 4 Cinematic Camera, Depth/Parallax, Lighting, Atmosphere & VFX engine.
 */
object CinematicEngine {

    const val MAX_ATMOSPHERE_PARTICLES = 56
    const val MAX_FILM_GRAIN_SPECKS = 48

    /**
     * Applies a [CinematicCameraPreset] to [scene], producing updated base camera framing
     * and real editable camera keyframes for animated motion presets.
     */
    fun applyCameraPreset(scene: StudioScene, preset: CinematicCameraPreset): StudioScene {
        val dur = scene.durationSec.coerceAtLeast(1.0)
        val clearedKfs = AnimationEvaluator.clearTargetKeyframes(
            scene.animationKeyframes,
            AnimationTargetType.CAMERA,
            AnimationEvaluator.CAMERA_TARGET_ID
        )
        val cid = AnimationEvaluator.CAMERA_TARGET_ID

        return when (preset) {
            CinematicCameraPreset.CLOSE_UP -> scene.copy(
                cameraMovement = "Static Lock",
                cameraZoom = 1.55,
                animationKeyframes = clearedKfs,
                cinematics = scene.cinematics.copy(
                    cameraPreset = preset.label,
                    cameraPosX = 0.0,
                    cameraPosY = -36.0,
                    cameraRotationDeg = 0.0
                )
            )
            CinematicCameraPreset.MEDIUM_SHOT -> scene.copy(
                cameraMovement = "Static Lock",
                cameraZoom = 1.25,
                animationKeyframes = clearedKfs,
                cinematics = scene.cinematics.copy(
                    cameraPreset = preset.label,
                    cameraPosX = 0.0,
                    cameraPosY = -14.0,
                    cameraRotationDeg = 0.0
                )
            )
            CinematicCameraPreset.WIDE_SHOT -> scene.copy(
                cameraMovement = "Static Lock",
                cameraZoom = 1.0,
                animationKeyframes = clearedKfs,
                cinematics = scene.cinematics.copy(
                    cameraPreset = preset.label,
                    cameraPosX = 0.0,
                    cameraPosY = 0.0,
                    cameraRotationDeg = 0.0
                )
            )
            CinematicCameraPreset.EXTREME_WIDE -> scene.copy(
                cameraMovement = "Static Lock",
                cameraZoom = 0.85,
                animationKeyframes = clearedKfs,
                cinematics = scene.cinematics.copy(
                    cameraPreset = preset.label,
                    cameraPosX = 0.0,
                    cameraPosY = 10.0,
                    cameraRotationDeg = 0.0
                )
            )
            CinematicCameraPreset.OVERHEAD -> scene.copy(
                cameraMovement = "Static Lock",
                cameraZoom = 1.30,
                animationKeyframes = clearedKfs,
                cinematics = scene.cinematics.copy(
                    cameraPreset = preset.label,
                    cameraPosX = 0.0,
                    cameraPosY = -52.0,
                    cameraRotationDeg = -6.0
                )
            )
            CinematicCameraPreset.ESTABLISHING -> {
                var kfs = clearedKfs
                kfs = AnimationEvaluator.upsertKeyframe(kfs, AnimationTargetType.CAMERA, cid, AnimatableProperty.CAMERA_ZOOM, 0.0, 1.14, EasingType.EASE_IN_OUT, dur)
                kfs = AnimationEvaluator.upsertKeyframe(kfs, AnimationTargetType.CAMERA, cid, AnimatableProperty.CAMERA_ZOOM, dur, 1.06, EasingType.EASE_IN_OUT, dur)
                kfs = AnimationEvaluator.upsertKeyframe(kfs, AnimationTargetType.CAMERA, cid, AnimatableProperty.CAMERA_PAN_X, 0.0, -55.0, EasingType.EASE_IN_OUT, dur)
                kfs = AnimationEvaluator.upsertKeyframe(kfs, AnimationTargetType.CAMERA, cid, AnimatableProperty.CAMERA_PAN_X, dur, 55.0, EasingType.EASE_IN_OUT, dur)
                kfs = AnimationEvaluator.upsertKeyframe(kfs, AnimationTargetType.CAMERA, cid, AnimatableProperty.CAMERA_PAN_Y, 0.0, -14.0, EasingType.EASE_IN_OUT, dur)
                kfs = AnimationEvaluator.upsertKeyframe(kfs, AnimationTargetType.CAMERA, cid, AnimatableProperty.CAMERA_PAN_Y, dur, 0.0, EasingType.EASE_IN_OUT, dur)
                scene.copy(
                    cameraMovement = "Parallax Pan Right",
                    cameraZoom = 1.14,
                    animationKeyframes = kfs,
                    cinematics = scene.cinematics.copy(
                        cameraPreset = preset.label,
                        cameraPosX = 0.0,
                        cameraPosY = 0.0,
                        cameraRotationDeg = 0.0
                    )
                )
            }
            CinematicCameraPreset.DRAMATIC_ZOOM -> {
                var kfs = clearedKfs
                kfs = AnimationEvaluator.upsertKeyframe(kfs, AnimationTargetType.CAMERA, cid, AnimatableProperty.CAMERA_ZOOM, 0.0, 1.05, EasingType.EASE_IN, dur)
                kfs = AnimationEvaluator.upsertKeyframe(kfs, AnimationTargetType.CAMERA, cid, AnimatableProperty.CAMERA_ZOOM, dur, 1.65, EasingType.EASE_IN, dur)
                kfs = AnimationEvaluator.upsertKeyframe(kfs, AnimationTargetType.CAMERA, cid, AnimatableProperty.CAMERA_PAN_Y, 0.0, 0.0, EasingType.EASE_IN, dur)
                kfs = AnimationEvaluator.upsertKeyframe(kfs, AnimationTargetType.CAMERA, cid, AnimatableProperty.CAMERA_PAN_Y, dur, -26.0, EasingType.EASE_IN, dur)
                scene.copy(
                    cameraMovement = "Slow Push In",
                    cameraZoom = 1.65,
                    animationKeyframes = kfs,
                    cinematics = scene.cinematics.copy(
                        cameraPreset = preset.label,
                        cameraPosX = 0.0,
                        cameraPosY = 0.0,
                        cameraRotationDeg = 0.0
                    )
                )
            }
            CinematicCameraPreset.SLOW_PUSH_IN -> {
                var kfs = clearedKfs
                kfs = AnimationEvaluator.upsertKeyframe(kfs, AnimationTargetType.CAMERA, cid, AnimatableProperty.CAMERA_ZOOM, 0.0, 1.0, EasingType.EASE_IN_OUT, dur)
                kfs = AnimationEvaluator.upsertKeyframe(kfs, AnimationTargetType.CAMERA, cid, AnimatableProperty.CAMERA_ZOOM, dur, 1.28, EasingType.EASE_IN_OUT, dur)
                scene.copy(
                    cameraMovement = "Slow Push In",
                    cameraZoom = 1.28,
                    animationKeyframes = kfs,
                    cinematics = scene.cinematics.copy(
                        cameraPreset = preset.label,
                        cameraPosX = 0.0,
                        cameraPosY = 0.0,
                        cameraRotationDeg = 0.0
                    )
                )
            }
            CinematicCameraPreset.PULL_OUT -> {
                var kfs = clearedKfs
                kfs = AnimationEvaluator.upsertKeyframe(kfs, AnimationTargetType.CAMERA, cid, AnimatableProperty.CAMERA_ZOOM, 0.0, 1.38, EasingType.EASE_OUT, dur)
                kfs = AnimationEvaluator.upsertKeyframe(kfs, AnimationTargetType.CAMERA, cid, AnimatableProperty.CAMERA_ZOOM, dur, 1.0, EasingType.EASE_OUT, dur)
                scene.copy(
                    cameraMovement = "Ken Burns Pull Out",
                    cameraZoom = 1.38,
                    animationKeyframes = kfs,
                    cinematics = scene.cinematics.copy(
                        cameraPreset = preset.label,
                        cameraPosX = 0.0,
                        cameraPosY = 0.0,
                        cameraRotationDeg = 0.0
                    )
                )
            }
            CinematicCameraPreset.PAN_LEFT -> {
                var kfs = clearedKfs
                kfs = AnimationEvaluator.upsertKeyframe(kfs, AnimationTargetType.CAMERA, cid, AnimatableProperty.CAMERA_ZOOM, 0.0, 1.18, EasingType.LINEAR, dur)
                kfs = AnimationEvaluator.upsertKeyframe(kfs, AnimationTargetType.CAMERA, cid, AnimatableProperty.CAMERA_PAN_X, 0.0, 80.0, EasingType.EASE_IN_OUT, dur)
                kfs = AnimationEvaluator.upsertKeyframe(kfs, AnimationTargetType.CAMERA, cid, AnimatableProperty.CAMERA_PAN_X, dur, -80.0, EasingType.EASE_IN_OUT, dur)
                scene.copy(
                    cameraMovement = "Parallax Pan Left",
                    cameraZoom = 1.18,
                    animationKeyframes = kfs,
                    cinematics = scene.cinematics.copy(
                        cameraPreset = preset.label,
                        cameraPosX = 0.0,
                        cameraPosY = 0.0,
                        cameraRotationDeg = 0.0
                    )
                )
            }
            CinematicCameraPreset.PAN_RIGHT -> {
                var kfs = clearedKfs
                kfs = AnimationEvaluator.upsertKeyframe(kfs, AnimationTargetType.CAMERA, cid, AnimatableProperty.CAMERA_ZOOM, 0.0, 1.18, EasingType.LINEAR, dur)
                kfs = AnimationEvaluator.upsertKeyframe(kfs, AnimationTargetType.CAMERA, cid, AnimatableProperty.CAMERA_PAN_X, 0.0, -80.0, EasingType.EASE_IN_OUT, dur)
                kfs = AnimationEvaluator.upsertKeyframe(kfs, AnimationTargetType.CAMERA, cid, AnimatableProperty.CAMERA_PAN_X, dur, 80.0, EasingType.EASE_IN_OUT, dur)
                scene.copy(
                    cameraMovement = "Parallax Pan Right",
                    cameraZoom = 1.18,
                    animationKeyframes = kfs,
                    cinematics = scene.cinematics.copy(
                        cameraPreset = preset.label,
                        cameraPosX = 0.0,
                        cameraPosY = 0.0,
                        cameraRotationDeg = 0.0
                    )
                )
            }
        }
    }

    /**
     * Resets camera framing to 1.0x fit-to-scene and clears camera keyframes.
     */
    fun resetCameraToFit(scene: StudioScene): StudioScene {
        val clearedKfs = AnimationEvaluator.clearTargetKeyframes(
            scene.animationKeyframes,
            AnimationTargetType.CAMERA,
            AnimationEvaluator.CAMERA_TARGET_ID
        )
        return scene.copy(
            cameraMovement = "Static Lock",
            cameraZoom = 1.0,
            animationKeyframes = clearedKfs,
            cinematics = scene.cinematics.copy(
                cameraPreset = CinematicCameraPreset.WIDE_SHOT.label,
                cameraPosX = 0.0,
                cameraPosY = 0.0,
                cameraRotationDeg = 0.0,
                cameraShakeIntensity = 0.0
            )
        )
    }

    /**
     * Applies a [LightingPreset] to [scene]'s cinematics settings while preserving camera, depth, and atmosphere settings.
     */
    fun applyLightingPreset(scene: StudioScene, preset: LightingPreset): StudioScene {
        return scene.copy(
            cinematics = scene.cinematics.copy(
                lightingPreset = preset.label,
                exposure = preset.exposure,
                contrast = preset.contrast,
                saturation = preset.saturation,
                temperature = preset.temperature,
                lightSourceX = preset.lightSourceX,
                lightSourceY = preset.lightSourceY,
                lightGlowIntensity = preset.lightGlowIntensity,
                shadowIntensity = preset.shadowIntensity,
                bloomIntensity = preset.bloomIntensity
            )
        )
    }

    /**
     * Computes deterministic 2D multiplane parallax pan, perspective zoom differential,
     * depth-of-field blur radius, and aerial haze alpha for a single compositing layer.
     */
    fun computeLayerDepthTransform(
        layerParallaxFactor: Double,
        camZoom: Double,
        camPanX: Double,
        camPanY: Double,
        cinematics: SceneCinematics
    ): LayerDepthTransform {
        val safeParallax = layerParallaxFactor.coerceIn(0.0, 2.0)
        val strength = if (cinematics.parallaxEnabled) cinematics.parallaxStrength.coerceIn(0.0, 2.0) else 0.0
        val effectiveDepth = (1.0 + (safeParallax - 1.0) * strength).coerceIn(0.05, 2.2)

        val layerPanX = camPanX * effectiveDepth
        val layerPanY = camPanY * effectiveDepth
        val zoomSensitivity = (0.45 + 0.55 * effectiveDepth).coerceIn(0.25, 1.85)
        val layerZoom = (1.0 + (camZoom - 1.0) * zoomSensitivity).coerceIn(0.45, 3.0)

        val focusPlane = cinematics.depthFocusPlane.coerceIn(0.2, 1.3)
        val focusDist = abs(safeParallax - focusPlane)
        val blurRadius = (focusDist * cinematics.depthBlurIntensity.coerceIn(0.0, 1.0) * 12.0).coerceIn(0.0, 12.0)
        val depthHaze = if (safeParallax < focusPlane) {
            ((focusPlane - safeParallax) * cinematics.depthHazeIntensity.coerceIn(0.0, 1.0) * 0.45).coerceIn(0.0, 0.45)
        } else {
            0.0
        }

        return LayerDepthTransform(
            effectiveDepthFactor = effectiveDepth,
            panX = layerPanX,
            panY = layerPanY,
            zoomScale = layerZoom,
            blurRadiusDp = blurRadius,
            depthHazeAlpha = depthHaze
        )
    }

    /**
     * Computes deterministic smooth handheld camera micro-shake offsets at [timeSec].
     * Returns zero when [shakeIntensity] <= 0.001.
     */
    fun computeHandheldShake(timeSec: Double, shakeIntensity: Double): CameraShakeOffset {
        val amp = shakeIntensity.coerceIn(0.0, 1.0)
        if (amp <= 0.001 || timeSec.isNaN() || timeSec.isInfinite()) {
            return CameraShakeOffset(0.0, 0.0, 0.0)
        }
        val t = timeSec.coerceAtLeast(0.0)
        val sx = (sin(t * 7.3) * 0.55 + sin(t * 13.7 + 1.1) * 0.30 + cos(t * 21.1) * 0.15) * amp * 14.0
        val sy = (cos(t * 6.1 + 0.7) * 0.55 + sin(t * 11.9 + 2.3) * 0.30 + sin(t * 19.3) * 0.15) * amp * 10.0
        val rot = (sin(t * 5.3 + 0.4) * 0.65 + cos(t * 10.7) * 0.35) * amp * 1.8
        return CameraShakeOffset(offsetX = sx, offsetY = sy, rotationDeg = rot)
    }

    /**
     * Computes a standard 4x5 (20-element) RGBA color grading matrix combining
     * Exposure, Contrast, Saturation, and Color Temperature (cool blue ↔ warm amber).
     */
    fun computeColorGradeMatrix(cinematics: SceneCinematics): FloatArray {
        val exposure = cinematics.exposure.coerceIn(-0.5, 0.5)
        val contrast = cinematics.contrast.coerceIn(0.6, 1.6)
        val sat = cinematics.saturation.coerceIn(0.0, 1.8)
        val temp = cinematics.temperature.coerceIn(-1.0, 1.0)

        // Exposure acts as brightness gain + gentle lift
        val gain = (1.0 + exposure * 0.85).coerceIn(0.45, 1.65)
        val rTemp = (1.0 + temp * 0.22).coerceIn(0.70, 1.30)
        val gTemp = (1.0 + temp * 0.04).coerceIn(0.90, 1.10)
        val bTemp = (1.0 - temp * 0.24).coerceIn(0.68, 1.32)

        // Rec.709 luminance weights for saturation matrix
        val invSat = 1.0 - sat
        val rLum = 0.2126 * invSat
        val gLum = 0.7152 * invSat
        val bLum = 0.0722 * invSat

        val rScale = gain * contrast * rTemp
        val gScale = gain * contrast * gTemp
        val bScale = gain * contrast * bTemp

        val contrastOffset = 128.0 * (1.0 - contrast) + exposure * 28.0

        return floatArrayOf(
            ((rLum + sat) * rScale).toFloat(), (gLum * rScale).toFloat(), (bLum * rScale).toFloat(), 0f, contrastOffset.toFloat(),
            (rLum * gScale).toFloat(), ((gLum + sat) * gScale).toFloat(), (bLum * gScale).toFloat(), 0f, contrastOffset.toFloat(),
            (rLum * bScale).toFloat(), (gLum * bScale).toFloat(), ((bLum + sat) * bScale).toFloat(), 0f, contrastOffset.toFloat(),
            0f, 0f, 0f, 1f, 0f
        )
    }

    fun isNeutralColorGrade(cinematics: SceneCinematics): Boolean {
        return abs(cinematics.exposure) < 0.005 &&
            abs(cinematics.contrast - 1.0) < 0.005 &&
            abs(cinematics.saturation - 1.0) < 0.005 &&
            abs(cinematics.temperature) < 0.005
    }

    /**
     * Resolves effective VFX values, preserving full backwards compatibility with legacy
     * Phase 1/2 `effectPreset` + `effectIntensity` scenes when Phase 4 sliders are at default 0.
     */
    fun resolveEffectiveVfx(scene: StudioScene): ResolvedVfxState {
        val c = scene.cinematics
        val legacyIntensity = scene.effectIntensity.coerceIn(0.0, 1.0)
        val legacyVignette = if (scene.effectPreset == "Cinematic Vignette") legacyIntensity * 0.75 else 0.0
        val legacyGrain = if (scene.effectPreset == "Film Grain") legacyIntensity * 0.70 else 0.0
        val legacySepia = if (scene.effectPreset == "Warm Sepia") legacyIntensity * 0.35 else 0.0
        val legacyArchivalDust = if (scene.effectPreset == "Archival Dust") legacyIntensity else 0.0

        return ResolvedVfxState(
            vignetteIntensity = maxOf(c.vignetteIntensity.coerceIn(0.0, 1.0), legacyVignette),
            filmGrainIntensity = maxOf(c.filmGrainIntensity.coerceIn(0.0, 1.0), legacyGrain),
            bloomIntensity = c.bloomIntensity.coerceIn(0.0, 1.0),
            sepiaOverlayAlpha = legacySepia,
            legacyArchivalDustAlpha = legacyArchivalDust,
            letterboxMode = LetterboxMode.fromLabelOrDefault(c.letterboxAspect),
            cameraShakeIntensity = c.cameraShakeIntensity.coerceIn(0.0, 1.0)
        )
    }

    /**
     * Generates deterministic, time-based atmosphere particles for [mode] at [timeSec].
     * Strictly stateless and capped at [MAX_ATMOSPHERE_PARTICLES] for smooth low-end Android rendering.
     */
    fun computeAtmosphereParticles(
        mode: AtmosphereMode,
        timeSec: Double,
        intensity: Double,
        speed: Double,
        windStrength: Double
    ): List<AtmosphereParticle> {
        val amp = intensity.coerceIn(0.0, 1.0)
        if (mode == AtmosphereMode.NONE || amp <= 0.01) return emptyList()
        val safeTime = if (timeSec.isNaN() || timeSec.isInfinite()) 0.0 else timeSec.coerceAtLeast(0.0)
        val safeSpeed = speed.coerceIn(0.2, 2.5)
        val wind = windStrength.coerceIn(-1.0, 1.0)

        val count = when (mode) {
            AtmosphereMode.RAIN -> (amp * MAX_ATMOSPHERE_PARTICLES).toInt().coerceIn(8, MAX_ATMOSPHERE_PARTICLES)
            AtmosphereMode.SNOW -> (amp * 46).toInt().coerceIn(6, 46)
            AtmosphereMode.DUST_MOTES -> (amp * 36).toInt().coerceIn(5, 36)
            AtmosphereMode.EMBERS -> (amp * 34).toInt().coerceIn(5, 34)
            AtmosphereMode.ARCHIVAL_DUST -> (amp * 40).toInt().coerceIn(6, 40)
            AtmosphereMode.MIST -> (amp * 8).toInt().coerceIn(3, 8)
            AtmosphereMode.SUNBEAMS -> (amp * 5).toInt().coerceIn(2, 5)
            AtmosphereMode.NONE -> 0
        }

        return List(count) { i ->
            val h1 = hashUnit(i * 19 + 7)
            val h2 = hashUnit(i * 37 + 13)
            val h3 = hashUnit(i * 53 + 29)
            when (mode) {
                AtmosphereMode.RAIN -> {
                    val fallSpeed = (0.75 + h3 * 0.55) * safeSpeed
                    val y = wrapUnit(h2 + safeTime * fallSpeed)
                    val x = wrapUnit(h1 + safeTime * wind * 0.35 * safeSpeed)
                    AtmosphereParticle(
                        normX = x,
                        normY = y,
                        sizeNorm = 0.002 + h3 * 0.0015,
                        lengthNorm = 0.045 + h3 * 0.04,
                        alpha = (0.28 + h3 * 0.42) * amp,
                        angleDeg = wind * 22.0
                    )
                }
                AtmosphereMode.SNOW -> {
                    val fallSpeed = (0.10 + h3 * 0.14) * safeSpeed
                    val sway = sin(safeTime * (1.2 + h3) + i * 1.7) * 0.025
                    val y = wrapUnit(h2 + safeTime * fallSpeed)
                    val x = wrapUnit(h1 + safeTime * wind * 0.12 * safeSpeed + sway)
                    AtmosphereParticle(
                        normX = x,
                        normY = y,
                        sizeNorm = 0.004 + h3 * 0.007,
                        lengthNorm = 0.0,
                        alpha = (0.35 + h3 * 0.55) * amp,
                        angleDeg = 0.0
                    )
                }
                AtmosphereMode.DUST_MOTES -> {
                    val driftX = sin(safeTime * (0.35 + h3 * 0.4) * safeSpeed + i * 2.1) * 0.04
                    val driftY = cos(safeTime * (0.28 + h1 * 0.35) * safeSpeed + i * 1.3) * 0.035
                    val x = wrapUnit(h1 + safeTime * wind * 0.03 * safeSpeed + driftX)
                    val y = wrapUnit(h2 + driftY)
                    val twinkle = 0.55 + 0.45 * sin(safeTime * 1.8 + i * 2.7)
                    AtmosphereParticle(
                        normX = x,
                        normY = y,
                        sizeNorm = 0.003 + h3 * 0.005,
                        lengthNorm = 0.0,
                        alpha = (0.25 + h3 * 0.45) * amp * twinkle,
                        angleDeg = 0.0
                    )
                }
                AtmosphereMode.EMBERS -> {
                    val riseSpeed = (0.16 + h3 * 0.22) * safeSpeed
                    val sway = sin(safeTime * (2.2 + h1) + i * 1.9) * 0.03
                    val y = wrapUnit(h2 - safeTime * riseSpeed)
                    val x = wrapUnit(h1 + safeTime * wind * 0.18 * safeSpeed + sway)
                    val glow = 0.65 + 0.35 * sin(safeTime * 4.5 + i * 3.1)
                    AtmosphereParticle(
                        normX = x,
                        normY = y,
                        sizeNorm = 0.0035 + h3 * 0.0055,
                        lengthNorm = 0.014 + h3 * 0.015,
                        alpha = (0.45 + h3 * 0.50) * amp * glow,
                        angleDeg = wind * 18.0
                    )
                }
                AtmosphereMode.ARCHIVAL_DUST -> {
                    val frameStep = (safeTime * 12.0 * safeSpeed).toInt()
                    val x = hashUnit(i * 73 + frameStep * 17)
                    val y = hashUnit(i * 131 + frameStep * 29)
                    AtmosphereParticle(
                        normX = x,
                        normY = y,
                        sizeNorm = if (i % 6 == 0) 0.004 else 0.002,
                        lengthNorm = if (i % 9 == 0) 0.16 else 0.0,
                        alpha = (0.20 + h3 * 0.35) * amp,
                        angleDeg = 0.0
                    )
                }
                AtmosphereMode.MIST -> {
                    val drift = (0.04 + h3 * 0.05) * safeSpeed * (if (wind >= 0) 1.0 else -1.0)
                    val x = wrapUnit(h1 + safeTime * drift * (0.4 + abs(wind)))
                    val y = 0.25 + h2 * 0.65
                    AtmosphereParticle(
                        normX = x,
                        normY = y,
                        sizeNorm = 0.28 + h3 * 0.26,
                        lengthNorm = 0.0,
                        alpha = (0.14 + h3 * 0.18) * amp,
                        angleDeg = 0.0
                    )
                }
                AtmosphereMode.SUNBEAMS -> {
                    val pulse = 0.78 + 0.22 * sin(safeTime * 0.9 * safeSpeed + i * 1.4)
                    val x = 0.12 + (i.toDouble() / count.coerceAtLeast(1)) * 0.76
                    AtmosphereParticle(
                        normX = x,
                        normY = 0.0,
                        sizeNorm = 0.08 + h3 * 0.08,
                        lengthNorm = 1.15,
                        alpha = (0.14 + h3 * 0.14) * amp * pulse,
                        angleDeg = 18.0 + wind * 15.0
                    )
                }
                AtmosphereMode.NONE -> AtmosphereParticle(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
            }
        }
    }

    private fun hashUnit(seed: Int): Double {
        val x = sin(seed.toDouble() * 12.9898 + 78.233) * 43758.5453
        return x - kotlin.math.floor(x)
    }

    private fun wrapUnit(value: Double): Double {
        val mod = value % 1.0
        return if (mod < 0.0) mod + 1.0 else mod
    }
}
