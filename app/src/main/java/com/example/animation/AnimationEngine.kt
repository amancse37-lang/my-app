package com.example.animation

import com.example.compositor.StageTransformMath
import com.example.data.AnimationKeyframe
import com.example.data.CharacterActingCue
import com.example.data.CharacterReferenceSheet
import com.example.data.SceneCharacterPlacement
import com.example.data.SceneVisualElement
import com.example.data.StudioCharacter
import com.example.data.StudioScene
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Target category for a keyframe track inside a [StudioScene].
 */
enum class AnimationTargetType(val label: String) {
    CHARACTER("Character"),
    ELEMENT("Element"),
    CAMERA("Camera"),
    GRAPHIC("Graphic");

    companion object {
        fun fromKeyOrNull(raw: String): AnimationTargetType? {
            return entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) }
        }
    }
}

/**
 * All animatable scalar/discrete properties supported by Phase 3 keyframe tracks.
 */
enum class AnimatableProperty(
    val label: String,
    val shortLabel: String,
    val unit: String,
    val minValue: Double,
    val maxValue: Double,
    val defaultStep: Double,
    val discrete: Boolean
) {
    POS_X("Position X", "X", "px", StageTransformMath.MIN_POS_X, StageTransformMath.MAX_POS_X, 10.0, false),
    POS_Y("Position Y", "Y", "px", StageTransformMath.MIN_POS_Y, StageTransformMath.MAX_POS_Y, 10.0, false),
    SCALE("Scale", "Scale", "x", StageTransformMath.MIN_SCALE, StageTransformMath.MAX_SCALE, 0.05, false),
    ROTATION("Rotation", "Rot", "°", StageTransformMath.MIN_ROTATION, StageTransformMath.MAX_ROTATION, 5.0, false),
    OPACITY("Opacity", "Alpha", "", 0.0, 1.0, 0.05, false),
    FLIP_X("Flip Horizontal", "Flip", "", 0.0, 1.0, 1.0, true),
    CAMERA_ZOOM("Camera Zoom", "Zoom", "x", 0.5, 2.5, 0.05, false),
    CAMERA_PAN_X("Camera Pan X", "Pan X", "px", -240.0, 240.0, 10.0, false),
    CAMERA_PAN_Y("Camera Pan Y", "Pan Y", "px", -160.0, 160.0, 10.0, false),
    CAMERA_ROTATION("Camera Rotation", "Roll", "°", -45.0, 45.0, 2.0, false);

    fun clampValue(value: Double): Double {
        if (value.isNaN() || value.isInfinite()) return minValue
        return if (discrete) {
            if (value >= 0.5) 1.0 else 0.0
        } else {
            value.coerceIn(minValue, maxValue)
        }
    }

    fun formatValue(value: Double): String {
        return when (this) {
            POS_X, POS_Y, CAMERA_PAN_X, CAMERA_PAN_Y -> "${value.roundToInt()}px"
            SCALE, CAMERA_ZOOM -> "${String.format("%.2f", value)}x"
            ROTATION, CAMERA_ROTATION -> "${value.roundToInt()}°"
            OPACITY -> "${(value.coerceIn(0.0, 1.0) * 100.0).roundToInt()}%"
            FLIP_X -> if (value >= 0.5) "Flipped" else "Normal"
        }
    }

    companion object {
        val transformProperties: List<AnimatableProperty> = listOf(
            POS_X, POS_Y, SCALE, ROTATION, OPACITY, FLIP_X
        )

        val cameraProperties: List<AnimatableProperty> = listOf(
            CAMERA_ZOOM, CAMERA_PAN_X, CAMERA_PAN_Y, CAMERA_ROTATION
        )

        fun forTargetType(targetType: AnimationTargetType): List<AnimatableProperty> {
            return when (targetType) {
                AnimationTargetType.CHARACTER,
                AnimationTargetType.ELEMENT,
                AnimationTargetType.GRAPHIC -> transformProperties
                AnimationTargetType.CAMERA -> cameraProperties
            }
        }

        fun fromKeyOrNull(raw: String): AnimatableProperty? {
            return entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) }
        }
    }
}

/**
 * Deterministic time-based interpolation curves between adjacent keyframes.
 */
enum class EasingType(val label: String) {
    LINEAR("Linear"),
    EASE_IN("Ease In"),
    EASE_OUT("Ease Out"),
    EASE_IN_OUT("Ease In-Out"),
    STEP("Step / Hold");

    fun apply(rawT: Double): Double {
        if (rawT.isNaN() || rawT.isInfinite()) return 0.0
        val t = rawT.coerceIn(0.0, 1.0)
        return when (this) {
            LINEAR -> t
            EASE_IN -> t * t
            EASE_OUT -> 1.0 - (1.0 - t) * (1.0 - t)
            EASE_IN_OUT -> {
                if (t < 0.5) {
                    2.0 * t * t
                } else {
                    val u = -2.0 * t + 2.0
                    1.0 - (u * u) / 2.0
                }
            }
            STEP -> if (t >= 1.0) 1.0 else 0.0
        }
    }

    companion object {
        val selectableCurves: List<EasingType> = listOf(
            LINEAR, EASE_IN, EASE_OUT, EASE_IN_OUT
        )

        fun fromKeyOrDefault(raw: String, fallback: EasingType = EASE_IN_OUT): EasingType {
            return entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) } ?: fallback
        }
    }
}

/**
 * Evaluated transform values for a character or visual element at a specific timestamp.
 */
data class EvaluatedTransform(
    val posX: Double,
    val posY: Double,
    val scale: Double,
    val rotationDeg: Double,
    val opacity: Double,
    val flipX: Boolean,
    val hasTransformKeyframes: Boolean,
    val hasVerticalOrRotationKeyframes: Boolean
)

/**
 * Evaluated camera state for a scene at a specific timestamp, including
 * keyframed or preset framing, rotation, and optional deterministic handheld shake.
 */
data class EvaluatedCameraState(
    val zoom: Double,
    val panX: Double,
    val panY: Double,
    val hasCameraKeyframes: Boolean,
    val rotationDeg: Double = 0.0
)

/**
 * Evaluated character acting cue state & deterministically resolved reference sheet image.
 */
data class EvaluatedActingState(
    val expression: String,
    val action: String,
    val poseView: String,
    val resolvedReferenceImageBase64: String,
    val matchedReferenceSheetId: String?,
    val matchedReferenceSheet: Boolean,
    val activeCueId: String?
)

/**
 * Complete, pure, deterministic animation snapshot of a [StudioScene] at [timeSec].
 */
data class EvaluatedSceneFrame(
    val timeSec: Double,
    val durationSec: Double,
    val camera: EvaluatedCameraState,
    val characterTransforms: Map<String, EvaluatedTransform>,
    val characterActing: Map<String, EvaluatedActingState>,
    val elementTransforms: Map<String, EvaluatedTransform>
)

/**
 * Pure, stateless, time-based keyframe evaluator and character acting resolver.
 *
 * All evaluations are strictly deterministic functions of `(scene, characters, referenceSheets, timeSec)`:
 * seeking forward, backward, pausing, or jumping directly to any timestamp `t` yields identical outputs
 * and never mutates base placement or element values.
 */
object AnimationEvaluator {
    const val CAMERA_TARGET_ID = "camera_main"
    const val MAX_KEYFRAMES_PER_SCENE = 200
    const val MAX_ACTING_CUES_PER_SCENE = 60
    const val TIME_SNAP_EPSILON_SEC = 0.05

    /**
     * Evaluates a single [property] for [targetType]/[targetId] at [timeSec] seconds within a scene.
     * Falls back to [baseValue] when no matching keyframes exist.
     */
    fun evaluateProperty(
        keyframes: List<AnimationKeyframe>,
        targetType: AnimationTargetType,
        targetId: String,
        property: AnimatableProperty,
        timeSec: Double,
        baseValue: Double
    ): Double {
        val safeTime = if (timeSec.isNaN() || timeSec.isInfinite()) 0.0 else timeSec.coerceAtLeast(0.0)
        val matching = keyframes
            .asSequence()
            .filter { kf ->
                kf.targetType.equals(targetType.name, ignoreCase = true) &&
                    kf.targetId == targetId &&
                    kf.property.equals(property.name, ignoreCase = true)
            }
            .sortedWith(compareBy<AnimationKeyframe> { it.timeSec }.thenBy { it.keyframeId })
            .toList()

        if (matching.isEmpty()) return property.clampValue(baseValue)

        val first = matching.first()
        if (safeTime <= first.timeSec) return property.clampValue(first.value)

        val last = matching.last()
        if (safeTime >= last.timeSec) return property.clampValue(last.value)

        for (i in 0 until matching.lastIndex) {
            val k0 = matching[i]
            val k1 = matching[i + 1]
            if (safeTime >= k0.timeSec && safeTime <= k1.timeSec) {
                val dt = k1.timeSec - k0.timeSec
                if (dt <= 1e-9) {
                    return property.clampValue(k1.value)
                }
                val rawT = ((safeTime - k0.timeSec) / dt).coerceIn(0.0, 1.0)
                if (property.discrete) {
                    val v = if (rawT >= 1.0) k1.value else k0.value
                    return property.clampValue(v)
                }
                val easing = EasingType.fromKeyOrDefault(k0.easing, EasingType.EASE_IN_OUT)
                val easedT = easing.apply(rawT)
                val interpolated = k0.value + (k1.value - k0.value) * easedT
                return property.clampValue(interpolated)
            }
        }
        return property.clampValue(last.value)
    }

    /**
     * Evaluates all 6 transform properties for a character or visual element at [timeSec].
     */
    fun evaluateTransform(
        keyframes: List<AnimationKeyframe>,
        targetType: AnimationTargetType,
        targetId: String,
        timeSec: Double,
        basePosX: Double,
        basePosY: Double,
        baseScale: Double,
        baseRotationDeg: Double,
        baseOpacity: Double,
        baseFlipX: Boolean
    ): EvaluatedTransform {
        val targetKfs = keyframes.filter { kf ->
            kf.targetType.equals(targetType.name, ignoreCase = true) && kf.targetId == targetId
        }
        if (targetKfs.isEmpty()) {
            return EvaluatedTransform(
                posX = basePosX.coerceIn(StageTransformMath.MIN_POS_X, StageTransformMath.MAX_POS_X),
                posY = basePosY.coerceIn(StageTransformMath.MIN_POS_Y, StageTransformMath.MAX_POS_Y),
                scale = baseScale.coerceIn(StageTransformMath.MIN_SCALE, StageTransformMath.MAX_SCALE),
                rotationDeg = StageTransformMath.normalizeRotation(baseRotationDeg),
                opacity = StageTransformMath.clampOpacity(baseOpacity),
                flipX = baseFlipX,
                hasTransformKeyframes = false,
                hasVerticalOrRotationKeyframes = false
            )
        }

        val posX = evaluateProperty(targetKfs, targetType, targetId, AnimatableProperty.POS_X, timeSec, basePosX)
        val posY = evaluateProperty(targetKfs, targetType, targetId, AnimatableProperty.POS_Y, timeSec, basePosY)
        val scale = evaluateProperty(targetKfs, targetType, targetId, AnimatableProperty.SCALE, timeSec, baseScale)
        val rot = evaluateProperty(targetKfs, targetType, targetId, AnimatableProperty.ROTATION, timeSec, baseRotationDeg)
        val opacity = evaluateProperty(targetKfs, targetType, targetId, AnimatableProperty.OPACITY, timeSec, baseOpacity)
        val flipVal = evaluateProperty(
            targetKfs,
            targetType,
            targetId,
            AnimatableProperty.FLIP_X,
            timeSec,
            if (baseFlipX) 1.0 else 0.0
        )
        val hasVertOrRot = targetKfs.any { kf ->
            kf.property.equals(AnimatableProperty.POS_Y.name, ignoreCase = true) ||
                kf.property.equals(AnimatableProperty.ROTATION.name, ignoreCase = true) ||
                kf.property.equals(AnimatableProperty.POS_X.name, ignoreCase = true)
        }

        return EvaluatedTransform(
            posX = posX,
            posY = posY,
            scale = scale,
            rotationDeg = StageTransformMath.normalizeRotation(rot),
            opacity = StageTransformMath.clampOpacity(opacity),
            flipX = flipVal >= 0.5,
            hasTransformKeyframes = true,
            hasVerticalOrRotationKeyframes = hasVertOrRot
        )
    }

    /**
     * Evaluates camera zoom and pan X/Y at [timeSec].
     *
     * When camera keyframes exist, interpolates them deterministically.
     * When no camera keyframes exist, preserves the Phase 1 [StudioScene.cameraMovement] preset choreography.
     */
    fun evaluateCamera(
        scene: StudioScene,
        timeSec: Double,
        cameraMuted: Boolean
    ): EvaluatedCameraState {
        if (cameraMuted) {
            return EvaluatedCameraState(
                zoom = 1.0,
                panX = 0.0,
                panY = 0.0,
                hasCameraKeyframes = false,
                rotationDeg = 0.0
            )
        }

        val safeTime = if (timeSec.isNaN() || timeSec.isInfinite()) 0.0 else timeSec.coerceAtLeast(0.0)
        val cin = scene.cinematics
        val shake = com.example.cinematics.CinematicEngine.computeHandheldShake(
            timeSec = safeTime,
            shakeIntensity = cin.cameraShakeIntensity
        )

        val basePosX = cin.cameraPosX.coerceIn(-240.0, 240.0)
        val basePosY = cin.cameraPosY.coerceIn(-160.0, 160.0)
        val baseRot = cin.cameraRotationDeg.coerceIn(-45.0, 45.0)

        val cameraKfs = scene.animationKeyframes.filter { kf ->
            kf.targetType.equals(AnimationTargetType.CAMERA.name, ignoreCase = true) &&
                (kf.targetId == CAMERA_TARGET_ID || kf.targetId.isBlank())
        }
        if (cameraKfs.isNotEmpty()) {
            val normalizedKfs = cameraKfs.map {
                if (it.targetId.isBlank()) it.copy(targetId = CAMERA_TARGET_ID) else it
            }
            val zoom = evaluateProperty(
                keyframes = normalizedKfs,
                targetType = AnimationTargetType.CAMERA,
                targetId = CAMERA_TARGET_ID,
                property = AnimatableProperty.CAMERA_ZOOM,
                timeSec = safeTime,
                baseValue = scene.cameraZoom.coerceIn(0.5, 2.5)
            )
            val panX = evaluateProperty(
                keyframes = normalizedKfs,
                targetType = AnimationTargetType.CAMERA,
                targetId = CAMERA_TARGET_ID,
                property = AnimatableProperty.CAMERA_PAN_X,
                timeSec = safeTime,
                baseValue = basePosX
            )
            val panY = evaluateProperty(
                keyframes = normalizedKfs,
                targetType = AnimationTargetType.CAMERA,
                targetId = CAMERA_TARGET_ID,
                property = AnimatableProperty.CAMERA_PAN_Y,
                timeSec = safeTime,
                baseValue = basePosY
            )
            val rot = evaluateProperty(
                keyframes = normalizedKfs,
                targetType = AnimationTargetType.CAMERA,
                targetId = CAMERA_TARGET_ID,
                property = AnimatableProperty.CAMERA_ROTATION,
                timeSec = safeTime,
                baseValue = baseRot
            )
            return EvaluatedCameraState(
                zoom = zoom,
                panX = (panX + shake.offsetX).coerceIn(-280.0, 280.0),
                panY = (panY + shake.offsetY).coerceIn(-200.0, 200.0),
                hasCameraKeyframes = true,
                rotationDeg = (rot + shake.rotationDeg).coerceIn(-50.0, 50.0)
            )
        }

        // Preserve Phase 1 camera preset behavior while honoring Phase 4 static framing (Static Lock zoom/pan/rotation)
        val safeDuration = scene.durationSec.coerceAtLeast(0.1)
        val progress = (safeTime / safeDuration).coerceIn(0.0, 1.0)
        val baseZoom = scene.cameraZoom.coerceIn(0.5, 2.5)
        val zoom = when (scene.cameraMovement) {
            "Slow Push In" -> 1.0 + (baseZoom.coerceAtLeast(0.8) - 1.0) * progress
            "Ken Burns Pull Out" -> baseZoom.coerceAtLeast(0.8) - (baseZoom.coerceAtLeast(0.8) - 1.0) * progress
            "Parallax Pan Left", "Parallax Pan Right" -> baseZoom.coerceAtLeast(1.12)
            "Static Lock" -> baseZoom
            else -> baseZoom
        }
        val panX = when (scene.cameraMovement) {
            "Parallax Pan Left" -> basePosX + (0.5 - progress) * 48.0
            "Parallax Pan Right" -> basePosX + (progress - 0.5) * 48.0
            else -> basePosX
        }
        return EvaluatedCameraState(
            zoom = zoom,
            panX = (panX + shake.offsetX).coerceIn(-280.0, 280.0),
            panY = (basePosY + shake.offsetY).coerceIn(-200.0, 200.0),
            hasCameraKeyframes = false,
            rotationDeg = (baseRot + shake.rotationDeg).coerceIn(-50.0, 50.0)
        )
    }

    /**
     * Evaluates time-based character acting cues (expression, action, pose view) at [timeSec]
     * and deterministically switches to the best matching generated [CharacterReferenceSheet]
     * image when available, falling back to [StudioCharacter.referenceImageBase64].
     */
    fun evaluateCharacterActing(
        placement: SceneCharacterPlacement,
        character: StudioCharacter,
        actingCues: List<CharacterActingCue>,
        referenceSheets: List<CharacterReferenceSheet>,
        timeSec: Double
    ): EvaluatedActingState {
        val safeTime = if (timeSec.isNaN() || timeSec.isInfinite()) 0.0 else timeSec.coerceAtLeast(0.0)
        val activeCue = actingCues
            .asSequence()
            .filter { it.characterId == placement.characterId && it.timeSec <= safeTime + 1e-6 }
            .sortedWith(compareBy<CharacterActingCue> { it.timeSec }.thenBy { it.cueId })
            .lastOrNull()

        val expression = activeCue?.expression?.takeIf { it.isNotBlank() } ?: placement.expression
        val action = activeCue?.action?.takeIf { it.isNotBlank() } ?: placement.action
        val poseView = activeCue?.poseView?.takeIf { it.isNotBlank() } ?: placement.poseView
        val explicitSheetId = activeCue?.referenceSheetId?.takeIf { it.isNotBlank() }

        val validSheets = referenceSheets.filter { it.imageBase64.isNotBlank() }
        val explicitMatch = explicitSheetId?.let { id -> validSheets.firstOrNull { it.id == id } }

        val bestScoredMatch = explicitMatch ?: validSheets
            .map { sheet ->
                var score = 0
                if (sheet.expression.equals(expression, ignoreCase = true)) score += 4
                if (sheet.viewType.equals(poseView, ignoreCase = true)) score += 3
                if (sheet.poseAction.equals(action, ignoreCase = true)) score += 2
                sheet to score
            }
            .filter { (_, score) -> score > 0 }
            .sortedWith(compareByDescending<Pair<CharacterReferenceSheet, Int>> { it.second }.thenBy { it.first.id })
            .firstOrNull()
            ?.first

        return if (bestScoredMatch != null) {
            EvaluatedActingState(
                expression = expression,
                action = action,
                poseView = poseView,
                resolvedReferenceImageBase64 = bestScoredMatch.imageBase64,
                matchedReferenceSheetId = bestScoredMatch.id,
                matchedReferenceSheet = true,
                activeCueId = activeCue?.cueId
            )
        } else {
            EvaluatedActingState(
                expression = expression,
                action = action,
                poseView = poseView,
                resolvedReferenceImageBase64 = character.referenceImageBase64,
                matchedReferenceSheetId = null,
                matchedReferenceSheet = false,
                activeCueId = activeCue?.cueId
            )
        }
    }

    /**
     * Evaluates the entire [StudioScene] deterministically at [timeSec].
     */
    fun evaluateSceneAtTime(
        scene: StudioScene,
        characters: List<StudioCharacter>,
        referenceSheetsByCharId: Map<String, List<CharacterReferenceSheet>> = emptyMap(),
        timeSec: Double,
        cameraMuted: Boolean = false
    ): EvaluatedSceneFrame {
        val safeDuration = scene.durationSec.coerceAtLeast(0.1)
        val clampedTime = if (timeSec.isNaN() || timeSec.isInfinite()) 0.0 else timeSec.coerceIn(0.0, safeDuration)
        val charById = characters.associateBy { it.id }

        val camera = evaluateCamera(scene, clampedTime, cameraMuted)

        val charTransforms = linkedMapOf<String, EvaluatedTransform>()
        val charActing = linkedMapOf<String, EvaluatedActingState>()
        scene.effectiveCast().forEach { placement ->
            charTransforms[placement.characterId] = evaluateTransform(
                keyframes = scene.animationKeyframes,
                targetType = AnimationTargetType.CHARACTER,
                targetId = placement.characterId,
                timeSec = clampedTime,
                basePosX = placement.posX,
                basePosY = placement.posY,
                baseScale = placement.scale,
                baseRotationDeg = placement.rotationDeg,
                baseOpacity = placement.opacity,
                baseFlipX = placement.flipX
            )
            val character = charById[placement.characterId]
            if (character != null) {
                charActing[placement.characterId] = evaluateCharacterActing(
                    placement = placement,
                    character = character,
                    actingCues = scene.characterActingCues,
                    referenceSheets = referenceSheetsByCharId[character.id].orEmpty(),
                    timeSec = clampedTime
                )
            }
        }

        val elementTransforms = linkedMapOf<String, EvaluatedTransform>()
        scene.sceneElements.forEach { el ->
            elementTransforms[el.elementId] = evaluateTransform(
                keyframes = scene.animationKeyframes,
                targetType = AnimationTargetType.ELEMENT,
                targetId = el.elementId,
                timeSec = clampedTime,
                basePosX = el.posX,
                basePosY = el.posY,
                baseScale = el.scale,
                baseRotationDeg = el.rotationDeg,
                baseOpacity = el.opacity,
                baseFlipX = el.flipX
            )
        }

        return EvaluatedSceneFrame(
            timeSec = clampedTime,
            durationSec = safeDuration,
            camera = camera,
            characterTransforms = charTransforms,
            characterActing = charActing,
            elementTransforms = elementTransforms
        )
    }

    // --- PURE KEYFRAME & ACTING CUE AUTHORING HELPERS ---

    fun hasKeyframesForProperty(
        keyframes: List<AnimationKeyframe>,
        targetType: AnimationTargetType,
        targetId: String,
        property: AnimatableProperty
    ): Boolean {
        return keyframes.any { kf ->
            kf.targetType.equals(targetType.name, ignoreCase = true) &&
                kf.targetId == targetId &&
                kf.property.equals(property.name, ignoreCase = true)
        }
    }

    fun findKeyframeAtTime(
        keyframes: List<AnimationKeyframe>,
        targetType: AnimationTargetType,
        targetId: String,
        property: AnimatableProperty,
        timeSec: Double,
        epsilonSec: Double = TIME_SNAP_EPSILON_SEC
    ): AnimationKeyframe? {
        return keyframes.firstOrNull { kf ->
            kf.targetType.equals(targetType.name, ignoreCase = true) &&
                kf.targetId == targetId &&
                kf.property.equals(property.name, ignoreCase = true) &&
                abs(kf.timeSec - timeSec) <= epsilonSec
        }
    }

    fun upsertKeyframe(
        existing: List<AnimationKeyframe>,
        targetType: AnimationTargetType,
        targetId: String,
        property: AnimatableProperty,
        timeSec: Double,
        value: Double,
        easing: EasingType = EasingType.EASE_IN_OUT,
        sceneDurationSec: Double,
        keyframeIdOverride: String? = null
    ): List<AnimationKeyframe> {
        val clampedTime = roundTimeToTenths(timeSec.coerceIn(0.0, sceneDurationSec.coerceAtLeast(0.1)))
        val clampedValue = property.clampValue(value)
        val effectiveEasing = if (property.discrete) EasingType.STEP else easing
        val normalizedTargetId = if (targetType == AnimationTargetType.CAMERA) CAMERA_TARGET_ID else targetId

        val mutable = existing.toMutableList()
        val matchIdx = when {
            !keyframeIdOverride.isNullOrBlank() -> mutable.indexOfFirst { it.keyframeId == keyframeIdOverride }
            else -> mutable.indexOfFirst { kf ->
                kf.targetType.equals(targetType.name, ignoreCase = true) &&
                    kf.targetId == normalizedTargetId &&
                    kf.property.equals(property.name, ignoreCase = true) &&
                    abs(kf.timeSec - clampedTime) <= TIME_SNAP_EPSILON_SEC
            }
        }

        if (matchIdx >= 0) {
            val prev = mutable[matchIdx]
            mutable[matchIdx] = prev.copy(
                timeSec = clampedTime,
                value = clampedValue,
                easing = effectiveEasing.name
            )
        } else {
            if (mutable.size >= MAX_KEYFRAMES_PER_SCENE) return mutable
            val newId = keyframeIdOverride?.takeIf { it.isNotBlank() }
                ?: "kf_${targetType.name.lowercase().take(3)}_${property.name.lowercase()}_${(clampedTime * 100).roundToInt()}_${mutable.size}"
            mutable.add(
                AnimationKeyframe(
                    keyframeId = newId,
                    targetType = targetType.name,
                    targetId = normalizedTargetId,
                    property = property.name,
                    timeSec = clampedTime,
                    value = clampedValue,
                    easing = effectiveEasing.name
                )
            )
        }

        return mutable.sortedWith(
            compareBy<AnimationKeyframe> { it.timeSec }
                .thenBy { it.targetType }
                .thenBy { it.targetId }
                .thenBy { it.property }
                .thenBy { it.keyframeId }
        )
    }

    fun deleteKeyframe(existing: List<AnimationKeyframe>, keyframeId: String): List<AnimationKeyframe> {
        return existing.filterNot { it.keyframeId == keyframeId }
    }

    fun clearTargetKeyframes(
        existing: List<AnimationKeyframe>,
        targetType: AnimationTargetType,
        targetId: String
    ): List<AnimationKeyframe> {
        val normalizedTargetId = if (targetType == AnimationTargetType.CAMERA) CAMERA_TARGET_ID else targetId
        return existing.filterNot { kf ->
            kf.targetType.equals(targetType.name, ignoreCase = true) &&
                (kf.targetId == normalizedTargetId || (targetType == AnimationTargetType.CAMERA && kf.targetId.isBlank()))
        }
    }

    fun upsertActingCue(
        existing: List<CharacterActingCue>,
        characterId: String,
        timeSec: Double,
        expression: String,
        action: String,
        poseView: String,
        referenceSheetId: String = "",
        sceneDurationSec: Double,
        cueIdOverride: String? = null
    ): List<CharacterActingCue> {
        val clampedTime = roundTimeToTenths(timeSec.coerceIn(0.0, sceneDurationSec.coerceAtLeast(0.1)))
        val mutable = existing.toMutableList()
        val matchIdx = when {
            !cueIdOverride.isNullOrBlank() -> mutable.indexOfFirst { it.cueId == cueIdOverride }
            else -> mutable.indexOfFirst { cue ->
                cue.characterId == characterId && abs(cue.timeSec - clampedTime) <= TIME_SNAP_EPSILON_SEC
            }
        }
        if (matchIdx >= 0) {
            val prev = mutable[matchIdx]
            mutable[matchIdx] = prev.copy(
                timeSec = clampedTime,
                expression = expression.take(64),
                action = action.take(80),
                poseView = poseView.take(64),
                referenceSheetId = referenceSheetId.take(128)
            )
        } else {
            if (mutable.size >= MAX_ACTING_CUES_PER_SCENE) return mutable
            val newId = cueIdOverride?.takeIf { it.isNotBlank() }
                ?: "cue_${characterId.take(16)}_${(clampedTime * 100).roundToInt()}_${mutable.size}"
            mutable.add(
                CharacterActingCue(
                    cueId = newId,
                    characterId = characterId.take(128),
                    timeSec = clampedTime,
                    expression = expression.take(64),
                    action = action.take(80),
                    poseView = poseView.take(64),
                    referenceSheetId = referenceSheetId.take(128)
                )
            )
        }
        return mutable.sortedWith(compareBy<CharacterActingCue> { it.timeSec }.thenBy { it.cueId })
    }

    fun deleteActingCue(existing: List<CharacterActingCue>, cueId: String): List<CharacterActingCue> {
        return existing.filterNot { it.cueId == cueId }
    }

    /**
     * Returns sorted distinct timestamps of all keyframes (and acting cues for characters)
     * belonging to the selected target, enabling Prev/Next keyframe navigation.
     */
    fun distinctTimesForTarget(
        keyframes: List<AnimationKeyframe>,
        actingCues: List<CharacterActingCue>,
        targetType: AnimationTargetType,
        targetId: String
    ): List<Double> {
        val normalizedId = if (targetType == AnimationTargetType.CAMERA) CAMERA_TARGET_ID else targetId
        val kfTimes = keyframes
            .filter { kf ->
                kf.targetType.equals(targetType.name, ignoreCase = true) &&
                    (kf.targetId == normalizedId || (targetType == AnimationTargetType.CAMERA && kf.targetId.isBlank()))
            }
            .map { it.timeSec }
        val cueTimes = if (targetType == AnimationTargetType.CHARACTER) {
            actingCues.filter { it.characterId == normalizedId }.map { it.timeSec }
        } else {
            emptyList()
        }
        return (kfTimes + cueTimes).distinct().sorted()
    }

    fun findPreviousTimestamp(times: List<Double>, currentTimeSec: Double): Double? {
        return times.lastOrNull { it < currentTimeSec - 0.04 }
    }

    fun findNextTimestamp(times: List<Double>, currentTimeSec: Double): Double? {
        return times.firstOrNull { it > currentTimeSec + 0.04 }
    }

    /**
     * Generates real, editable transform keyframes for common 2D stage movements
     * (transform-based entry/exit/traversal/fade — NOT skeletal limb animation).
     */
    fun applyCharacterTransformPreset(
        existing: List<AnimationKeyframe>,
        placement: SceneCharacterPlacement,
        sceneDurationSec: Double,
        presetKey: String
    ): List<AnimationKeyframe> {
        val dur = sceneDurationSec.coerceAtLeast(1.0)
        val cid = placement.characterId
        var list = existing
        when (presetKey) {
            "walk_across_ltr" -> {
                list = upsertKeyframe(list, AnimationTargetType.CHARACTER, cid, AnimatableProperty.POS_X, 0.0, -340.0, EasingType.EASE_IN_OUT, dur)
                list = upsertKeyframe(list, AnimationTargetType.CHARACTER, cid, AnimatableProperty.POS_X, dur, 340.0, EasingType.EASE_IN_OUT, dur)
                list = upsertKeyframe(list, AnimationTargetType.CHARACTER, cid, AnimatableProperty.FLIP_X, 0.0, 0.0, EasingType.STEP, dur)
            }
            "enter_from_left" -> {
                val enterEnd = (dur * 0.35).coerceAtLeast(0.8)
                list = upsertKeyframe(list, AnimationTargetType.CHARACTER, cid, AnimatableProperty.POS_X, 0.0, -440.0, EasingType.EASE_OUT, dur)
                list = upsertKeyframe(list, AnimationTargetType.CHARACTER, cid, AnimatableProperty.POS_X, enterEnd, placement.posX, EasingType.EASE_OUT, dur)
                list = upsertKeyframe(list, AnimationTargetType.CHARACTER, cid, AnimatableProperty.OPACITY, 0.0, 0.0, EasingType.EASE_OUT, dur)
                list = upsertKeyframe(list, AnimationTargetType.CHARACTER, cid, AnimatableProperty.OPACITY, (enterEnd * 0.6).coerceAtLeast(0.4), 1.0, EasingType.EASE_OUT, dur)
            }
            "exit_to_right" -> {
                val exitStart = (dur * 0.65).coerceAtLeast(0.5)
                list = upsertKeyframe(list, AnimationTargetType.CHARACTER, cid, AnimatableProperty.POS_X, exitStart, placement.posX, EasingType.EASE_IN, dur)
                list = upsertKeyframe(list, AnimationTargetType.CHARACTER, cid, AnimatableProperty.POS_X, dur, 450.0, EasingType.EASE_IN, dur)
                list = upsertKeyframe(list, AnimationTargetType.CHARACTER, cid, AnimatableProperty.OPACITY, exitStart, 1.0, EasingType.EASE_IN, dur)
                list = upsertKeyframe(list, AnimationTargetType.CHARACTER, cid, AnimatableProperty.OPACITY, dur, 0.0, EasingType.EASE_IN, dur)
            }
            "fade_in" -> {
                val fadeEnd = (dur * 0.25).coerceIn(0.5, 2.0)
                list = upsertKeyframe(list, AnimationTargetType.CHARACTER, cid, AnimatableProperty.OPACITY, 0.0, 0.0, EasingType.EASE_OUT, dur)
                list = upsertKeyframe(list, AnimationTargetType.CHARACTER, cid, AnimatableProperty.OPACITY, fadeEnd, 1.0, EasingType.EASE_OUT, dur)
            }
        }
        return list
    }

    /**
     * Generates real, editable camera keyframes for documentary camera choreography.
     */
    fun applyCameraKeyframePreset(
        existing: List<AnimationKeyframe>,
        sceneDurationSec: Double,
        presetKey: String
    ): List<AnimationKeyframe> {
        val dur = sceneDurationSec.coerceAtLeast(1.0)
        var list = clearTargetKeyframes(existing, AnimationTargetType.CAMERA, CAMERA_TARGET_ID)
        when (presetKey) {
            "push_in" -> {
                list = upsertKeyframe(list, AnimationTargetType.CAMERA, CAMERA_TARGET_ID, AnimatableProperty.CAMERA_ZOOM, 0.0, 1.0, EasingType.EASE_IN_OUT, dur)
                list = upsertKeyframe(list, AnimationTargetType.CAMERA, CAMERA_TARGET_ID, AnimatableProperty.CAMERA_ZOOM, dur, 1.28, EasingType.EASE_IN_OUT, dur)
            }
            "pan_sweep" -> {
                list = upsertKeyframe(list, AnimationTargetType.CAMERA, CAMERA_TARGET_ID, AnimatableProperty.CAMERA_ZOOM, 0.0, 1.18, EasingType.LINEAR, dur)
                list = upsertKeyframe(list, AnimationTargetType.CAMERA, CAMERA_TARGET_ID, AnimatableProperty.CAMERA_PAN_X, 0.0, -75.0, EasingType.EASE_IN_OUT, dur)
                list = upsertKeyframe(list, AnimationTargetType.CAMERA, CAMERA_TARGET_ID, AnimatableProperty.CAMERA_PAN_X, dur, 75.0, EasingType.EASE_IN_OUT, dur)
            }
            "tilt_reveal" -> {
                list = upsertKeyframe(list, AnimationTargetType.CAMERA, CAMERA_TARGET_ID, AnimatableProperty.CAMERA_ZOOM, 0.0, 1.22, EasingType.EASE_OUT, dur)
                list = upsertKeyframe(list, AnimationTargetType.CAMERA, CAMERA_TARGET_ID, AnimatableProperty.CAMERA_ZOOM, dur, 1.08, EasingType.EASE_OUT, dur)
                list = upsertKeyframe(list, AnimationTargetType.CAMERA, CAMERA_TARGET_ID, AnimatableProperty.CAMERA_PAN_Y, 0.0, -45.0, EasingType.EASE_IN_OUT, dur)
                list = upsertKeyframe(list, AnimationTargetType.CAMERA, CAMERA_TARGET_ID, AnimatableProperty.CAMERA_PAN_Y, dur, 30.0, EasingType.EASE_IN_OUT, dur)
            }
        }
        return list
    }

    private fun roundTimeToTenths(value: Double): Double {
        return (value * 100.0).roundToInt() / 100.0
    }
}
