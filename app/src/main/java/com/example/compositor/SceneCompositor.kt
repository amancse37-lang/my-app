package com.example.compositor

import com.example.animation.AnimationEvaluator
import com.example.animation.EvaluatedCameraState
import com.example.cinematics.CinematicEngine
import com.example.cinematics.LayerDepthTransform
import com.example.data.CharacterReferenceSheet
import com.example.data.LayerState
import com.example.data.SceneCharacterPlacement
import com.example.data.SceneVisualElement
import com.example.data.StudioAsset
import com.example.data.StudioCharacter
import com.example.data.StudioScene
import com.example.data.TimelineTrackType

/**
 * Canonical 6-layer 2D scene compositing stack.
 *
 * `defaultParallaxFactor` drives active 2D multiplane depth parallax, depth-of-field
 * focus separation, and aerial haze through [CinematicEngine.computeLayerDepthTransform].
 */
enum class SceneLayerType(
    val label: String,
    val shortCode: String,
    val defaultOrder: Int,
    val defaultParallaxFactor: Double
) {
    BACKGROUND("Background", "BG", 0, 0.25),
    MIDGROUND("Midground", "MG", 1, 0.55),
    CHARACTERS("Characters", "CH", 2, 0.85),
    PROPS("Props & Objects", "PR", 3, 0.95),
    FOREGROUND("Foreground", "FG", 4, 1.15),
    OVERLAY("Overlays", "OV", 5, 1.0);

    companion object {
        fun fromKeyOrDefault(raw: String, fallback: SceneLayerType = PROPS): SceneLayerType {
            return entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) } ?: fallback
        }

        fun defaultLayerStates(): List<LayerState> {
            return entries.sortedBy { it.defaultOrder }.map { layer ->
                LayerState(
                    layerKey = layer.name,
                    visible = true,
                    order = layer.defaultOrder,
                    opacity = 1.0,
                    parallaxFactor = layer.defaultParallaxFactor
                )
            }
        }
    }
}

/**
 * Currently selected visual target on the 2D compositor stage.
 */
sealed interface SelectedStageTarget {
    data class CharacterTarget(val characterId: String) : SelectedStageTarget
    data class ElementTarget(val elementId: String) : SelectedStageTarget
    data class GraphicTarget(val graphicId: String) : SelectedStageTarget
}

/**
 * Canonical virtual 16:9 stage coordinate space and deterministic transform math.
 *
 * Stage center is (0, 0); horizontal range maps cleanly to [-480, 480] and
 * vertical range maps to [-270, 270], keeping full compatibility with legacy
 * scene coordinates (e.g. posX = -110..110, posY = 12..20) and future
 * keyframe / off-screen video rendering.
 */
object StageTransformMath {
    const val VIRTUAL_STAGE_WIDTH = 960.0
    const val VIRTUAL_STAGE_HEIGHT = 540.0

    const val MIN_POS_X = -480.0
    const val MAX_POS_X = 480.0
    const val MIN_POS_Y = -270.0
    const val MAX_POS_Y = 270.0
    const val MIN_SCALE = 0.25
    const val MAX_SCALE = 3.0
    const val MIN_ROTATION = -180.0
    const val MAX_ROTATION = 180.0

    fun stageToCanvasPx(
        posX: Double,
        posY: Double,
        canvasWidthPx: Float,
        canvasHeightPx: Float
    ): Pair<Float, Float> {
        val px = ((posX / VIRTUAL_STAGE_WIDTH) * canvasWidthPx).toFloat()
        val py = ((posY / VIRTUAL_STAGE_HEIGHT) * canvasHeightPx).toFloat()
        return px to py
    }

    fun canvasPxDeltaToStageDelta(
        deltaPxX: Float,
        deltaPxY: Float,
        canvasWidthPx: Float,
        canvasHeightPx: Float
    ): Pair<Double, Double> {
        if (canvasWidthPx <= 1f || canvasHeightPx <= 1f) return 0.0 to 0.0
        val dx = (deltaPxX.toDouble() / canvasWidthPx.toDouble()) * VIRTUAL_STAGE_WIDTH
        val dy = (deltaPxY.toDouble() / canvasHeightPx.toDouble()) * VIRTUAL_STAGE_HEIGHT
        return dx to dy
    }

    fun applyDragDelta(
        posX: Double,
        posY: Double,
        deltaStageX: Double,
        deltaStageY: Double
    ): Pair<Double, Double> {
        val nx = (posX + deltaStageX).coerceIn(MIN_POS_X, MAX_POS_X)
        val ny = (posY + deltaStageY).coerceIn(MIN_POS_Y, MAX_POS_Y)
        return nx to ny
    }

    fun applyResizeFactor(currentScale: Double, factor: Double): Double {
        if (factor.isNaN() || factor.isInfinite() || factor <= 0.0) return currentScale.coerceIn(MIN_SCALE, MAX_SCALE)
        return (currentScale * factor).coerceIn(MIN_SCALE, MAX_SCALE)
    }

    fun normalizeRotation(deg: Double): Double {
        if (deg.isNaN() || deg.isInfinite()) return 0.0
        var r = deg % 360.0
        if (r > 180.0) r -= 360.0
        if (r < -180.0) r += 360.0
        return r.coerceIn(MIN_ROTATION, MAX_ROTATION)
    }

    fun clampOpacity(value: Double): Double {
        if (value.isNaN() || value.isInfinite()) return 1.0
        return value.coerceIn(0.0, 1.0)
    }

    fun clampZIndex(value: Int): Int = value.coerceIn(-100, 100)
}

/**
 * Deterministic render nodes produced by [SceneCompositor] for a single frame/state.
 */
sealed interface CompositeRenderNode {
    val nodeId: String
    val layer: SceneLayerType
    val zIndex: Int
    val opacity: Float

    data class BackgroundBaseNode(
        override val nodeId: String = "bg_base",
        override val layer: SceneLayerType = SceneLayerType.BACKGROUND,
        override val zIndex: Int = Int.MIN_VALUE,
        override val opacity: Float = 1f,
        val backgroundPreset: String,
        val backgroundTintHex: String,
        val resolvedImageBase64: String = ""
    ) : CompositeRenderNode

    data class CharacterNode(
        override val nodeId: String,
        override val layer: SceneLayerType,
        override val zIndex: Int,
        override val opacity: Float,
        val character: StudioCharacter,
        val placement: SceneCharacterPlacement,
        val hasCustomReferenceImage: Boolean,
        val resolvedImageBase64: String = character.referenceImageBase64,
        val activeExpression: String = placement.expression,
        val activeAction: String = placement.action,
        val activePoseView: String = placement.poseView,
        val matchedReferenceSheetId: String? = null,
        val hasTransformKeyframes: Boolean = false,
        val hasVerticalOrRotationKeyframes: Boolean = false
    ) : CompositeRenderNode

    data class VisualElementNode(
        override val nodeId: String,
        override val layer: SceneLayerType,
        override val zIndex: Int,
        override val opacity: Float,
        val element: SceneVisualElement,
        val resolvedUri: String,
        val resolvedPresetKey: String,
        val resolvedImageBase64: String = "",
        val hasVisualAsset: Boolean,
        val hasTransformKeyframes: Boolean = false
    ) : CompositeRenderNode

    data class OverlayStackNode(
        override val nodeId: String = "overlay_stack",
        override val layer: SceneLayerType = SceneLayerType.OVERLAY,
        override val zIndex: Int = 0,
        override val opacity: Float = 1f
    ) : CompositeRenderNode
}

data class ResolvedLayerPlan(
    val layerType: SceneLayerType,
    val order: Int,
    val visible: Boolean,
    val opacity: Float,
    val parallaxFactor: Double,
    val nodes: List<CompositeRenderNode>,
    val depthTransform: LayerDepthTransform? = null
)

data class SceneCompositionPlan(
    val orderedLayers: List<ResolvedLayerPlan>,
    val visibleCharacterCount: Int,
    val visibleVisualElementCount: Int,
    val evaluatedCamera: EvaluatedCameraState = EvaluatedCameraState(
        zoom = 1.0,
        panX = 0.0,
        panY = 0.0,
        hasCameraKeyframes = false
    ),
    val evaluatedTimeSec: Double? = null
) {
    fun flattenedVisibleNodes(): List<CompositeRenderNode> {
        return orderedLayers
            .filter { it.visible && it.opacity > 0.001f }
            .flatMap { it.nodes }
    }
}

/**
 * Pure, deterministic 2D scene compositor engine.
 *
 * Designed as an extensible core so future phases (keyframe interpolation,
 * character pose switching, camera rigs, scene transitions, and deterministic
 * off-screen video rendering) can evaluate scenes through the same pipeline.
 */
object SceneCompositor {

    val bundledDrawablePresetKeys: Set<String> = setOf("himalayas", "old_delhi", "archive_study")

    fun normalizeLayerStates(raw: List<LayerState>): List<LayerState> {
        val byKey = linkedMapOf<SceneLayerType, LayerState>()
        raw.forEach { state ->
            val parsed = SceneLayerType.entries.firstOrNull {
                it.name.equals(state.layerKey.trim(), ignoreCase = true)
            } ?: return@forEach
            if (parsed !in byKey) {
                byKey[parsed] = state.copy(
                    layerKey = parsed.name,
                    opacity = StageTransformMath.clampOpacity(state.opacity),
                    parallaxFactor = state.parallaxFactor.coerceIn(0.0, 2.0)
                )
            }
        }
        SceneLayerType.entries.forEach { layer ->
            if (layer !in byKey) {
                byKey[layer] = LayerState(
                    layerKey = layer.name,
                    visible = true,
                    order = layer.defaultOrder,
                    opacity = 1.0,
                    parallaxFactor = layer.defaultParallaxFactor
                )
            }
        }
        return byKey.values
            .sortedWith(compareBy<LayerState> { it.order }.thenBy {
                SceneLayerType.fromKeyOrDefault(it.layerKey, SceneLayerType.BACKGROUND).defaultOrder
            })
            .mapIndexed { idx, state -> state.copy(order = idx) }
    }

    fun toggleLayerVisibility(raw: List<LayerState>, target: SceneLayerType): List<LayerState> {
        return normalizeLayerStates(raw).map { layer ->
            if (layer.layerKey == target.name) layer.copy(visible = !layer.visible) else layer
        }
    }

    fun updateLayerOpacity(
        raw: List<LayerState>,
        target: SceneLayerType,
        opacity: Double
    ): List<LayerState> {
        val clamped = StageTransformMath.clampOpacity(opacity)
        return normalizeLayerStates(raw).map { layer ->
            if (layer.layerKey == target.name) layer.copy(opacity = clamped) else layer
        }
    }

    fun updateLayerParallax(
        raw: List<LayerState>,
        target: SceneLayerType,
        parallaxFactor: Double
    ): List<LayerState> {
        val safe = if (parallaxFactor.isNaN() || parallaxFactor.isInfinite()) {
            target.defaultParallaxFactor
        } else {
            parallaxFactor.coerceIn(0.0, 2.0)
        }
        return normalizeLayerStates(raw).map { layer ->
            if (layer.layerKey == target.name) layer.copy(parallaxFactor = safe) else layer
        }
    }

    /**
     * Moves [target] layer up (+1 = closer to front/viewer) or down (-1 = further back)
     * in the compositing stack, keeping canonical 0..5 ordering.
     */
    fun moveLayerOrder(raw: List<LayerState>, target: SceneLayerType, delta: Int): List<LayerState> {
        val normalized = normalizeLayerStates(raw).toMutableList()
        val idx = normalized.indexOfFirst { it.layerKey == target.name }
        val newIdx = idx + delta
        if (idx == -1 || newIdx !in normalized.indices) return normalized
        val moved = normalized.removeAt(idx)
        normalized.add(newIdx, moved)
        return normalized.mapIndexed { i, state -> state.copy(order = i) }
    }

    fun isSafeVisualUri(rawUri: String): Boolean {
        val trimmed = rawUri.trim()
        return trimmed.startsWith("content://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
    }

    fun hasRenderableVisualAsset(
        elementUri: String,
        elementPresetKey: String,
        linkedAsset: StudioAsset?
    ): Boolean {
        val uri = elementUri.ifBlank { linkedAsset?.localUri.orEmpty() }
        val preset = elementPresetKey.ifBlank { linkedAsset?.presetKey.orEmpty() }
        val base64 = linkedAsset?.imageBase64.orEmpty()
        return base64.isNotBlank() || isSafeVisualUri(uri) || preset in bundledDrawablePresetKeys
    }

    fun resolveBackgroundAssetId(backgroundPreset: String): String? {
        return if (backgroundPreset.startsWith("asset:")) {
            backgroundPreset.removePrefix("asset:").trim().takeIf { it.isNotBlank() }
        } else {
            null
        }
    }

    fun buildCompositionPlan(
        scene: StudioScene,
        characters: List<StudioCharacter>,
        assets: List<StudioAsset>,
        mutedTracks: Set<TimelineTrackType>,
        timeSec: Double? = null,
        characterReferencesByCharId: Map<String, List<CharacterReferenceSheet>> = emptyMap()
    ): SceneCompositionPlan {
        val normalizedLayers = normalizeLayerStates(scene.layerStates)
        val charById = characters.associateBy { it.id }
        val assetById = assets.associateBy { it.id }

        val evaluatedFrame = timeSec?.let { t ->
            AnimationEvaluator.evaluateSceneAtTime(
                scene = scene,
                characters = characters,
                referenceSheetsByCharId = characterReferencesByCharId,
                timeSec = t,
                cameraMuted = TimelineTrackType.CAMERA in mutedTracks
            )
        }

        val cameraState = evaluatedFrame?.camera ?: AnimationEvaluator.evaluateCamera(
            scene = scene,
            timeSec = 0.0,
            cameraMuted = TimelineTrackType.CAMERA in mutedTracks
        )

        val effectiveCast = scene.effectiveCast()
        var visibleChars = 0
        var visibleElements = 0

        val resolvedLayers = normalizedLayers.map { layerState ->
            val layerType = SceneLayerType.fromKeyOrDefault(layerState.layerKey, SceneLayerType.BACKGROUND)
            val mutedByTimeline = when (layerType) {
                SceneLayerType.BACKGROUND -> TimelineTrackType.BACKGROUND in mutedTracks
                SceneLayerType.CHARACTERS -> TimelineTrackType.CHARACTER in mutedTracks
                SceneLayerType.PROPS -> TimelineTrackType.OBJECT in mutedTracks
                SceneLayerType.MIDGROUND, SceneLayerType.FOREGROUND -> false
                SceneLayerType.OVERLAY -> false
            }
            val effectiveLayerVisible = layerState.visible && !mutedByTimeline
            val layerOpacity = StageTransformMath.clampOpacity(layerState.opacity).toFloat()
            val layerDepthTransform = if (layerType == SceneLayerType.OVERLAY) {
                null
            } else {
                CinematicEngine.computeLayerDepthTransform(
                    layerParallaxFactor = layerState.parallaxFactor,
                    camZoom = cameraState.zoom,
                    camPanX = cameraState.panX,
                    camPanY = cameraState.panY,
                    cinematics = scene.cinematics
                )
            }

            val nodes = mutableListOf<CompositeRenderNode>()
            if (effectiveLayerVisible && layerOpacity > 0.001f) {
                if (layerType == SceneLayerType.BACKGROUND) {
                    val bgAssetId = resolveBackgroundAssetId(scene.backgroundPreset)
                    val resolvedBgBase64 = bgAssetId?.let { assetById[it]?.imageBase64 }.orEmpty()
                    nodes.add(
                        CompositeRenderNode.BackgroundBaseNode(
                            backgroundPreset = scene.backgroundPreset,
                            backgroundTintHex = scene.backgroundTintHex,
                            opacity = layerOpacity,
                            resolvedImageBase64 = resolvedBgBase64
                        )
                    )
                }

                // Characters placed on this layer (default CHARACTERS)
                if (TimelineTrackType.CHARACTER !in mutedTracks) {
                    effectiveCast
                        .filter { placement ->
                            placement.visible &&
                                SceneLayerType.fromKeyOrDefault(placement.layer, SceneLayerType.CHARACTERS) == layerType
                        }
                        .forEachIndexed { idx, basePlacement ->
                            val character = charById[basePlacement.characterId] ?: return@forEachIndexed
                            val evalTransform = evaluatedFrame?.characterTransforms?.get(basePlacement.characterId)
                            val evalActing = evaluatedFrame?.characterActing?.get(basePlacement.characterId)

                            val evaluatedPlacement = if (evalTransform != null) {
                                basePlacement.copy(
                                    posX = evalTransform.posX,
                                    posY = evalTransform.posY,
                                    scale = evalTransform.scale,
                                    rotationDeg = evalTransform.rotationDeg,
                                    opacity = evalTransform.opacity,
                                    flipX = evalTransform.flipX,
                                    expression = evalActing?.expression ?: basePlacement.expression,
                                    action = evalActing?.action ?: basePlacement.action,
                                    poseView = evalActing?.poseView ?: basePlacement.poseView
                                )
                            } else {
                                basePlacement
                            }

                            val resolvedImg = evalActing?.resolvedReferenceImageBase64 ?: character.referenceImageBase64
                            val nodeOpacity = (layerOpacity * StageTransformMath.clampOpacity(evaluatedPlacement.opacity).toFloat())
                                .coerceIn(0f, 1f)
                            if (nodeOpacity > 0.001f) {
                                visibleChars++
                                nodes.add(
                                    CompositeRenderNode.CharacterNode(
                                        nodeId = "char_${character.id}",
                                        layer = layerType,
                                        zIndex = StageTransformMath.clampZIndex(evaluatedPlacement.zIndex) * 100 + idx,
                                        opacity = nodeOpacity,
                                        character = character,
                                        placement = evaluatedPlacement,
                                        hasCustomReferenceImage = resolvedImg.isNotBlank(),
                                        resolvedImageBase64 = resolvedImg,
                                        activeExpression = evaluatedPlacement.expression,
                                        activeAction = evaluatedPlacement.action,
                                        activePoseView = evaluatedPlacement.poseView,
                                        matchedReferenceSheetId = evalActing?.matchedReferenceSheetId,
                                        hasTransformKeyframes = evalTransform?.hasTransformKeyframes ?: false,
                                        hasVerticalOrRotationKeyframes = evalTransform?.hasVerticalOrRotationKeyframes ?: false
                                    )
                                )
                            }
                        }
                }

                // Visual elements (props, midground, foreground, custom background layers)
                scene.sceneElements
                    .filter { el ->
                        el.visible &&
                            SceneLayerType.fromKeyOrDefault(el.layer, SceneLayerType.PROPS) == layerType
                    }
                    .forEachIndexed { idx, baseEl ->
                        val evalTransform = evaluatedFrame?.elementTransforms?.get(baseEl.elementId)
                        val el = if (evalTransform != null) {
                            baseEl.copy(
                                posX = evalTransform.posX,
                                posY = evalTransform.posY,
                                scale = evalTransform.scale,
                                rotationDeg = evalTransform.rotationDeg,
                                opacity = evalTransform.opacity,
                                flipX = evalTransform.flipX
                            )
                        } else {
                            baseEl
                        }

                        val linkedAsset = el.assetId.takeIf { it.isNotBlank() }?.let { assetById[it] }
                        val resolvedBase64 = linkedAsset?.imageBase64.orEmpty()
                        val resolvedUri = el.assetUri.ifBlank { linkedAsset?.localUri.orEmpty() }
                            .takeIf { isSafeVisualUri(it) }
                            .orEmpty()
                        val resolvedPreset = el.assetPresetKey.ifBlank { linkedAsset?.presetKey.orEmpty() }
                            .takeIf { it in bundledDrawablePresetKeys }
                            .orEmpty()
                        val hasVisual = resolvedBase64.isNotBlank() || resolvedUri.isNotBlank() || resolvedPreset.isNotBlank()
                        val nodeOpacity = (layerOpacity * StageTransformMath.clampOpacity(el.opacity).toFloat())
                            .coerceIn(0f, 1f)
                        if (hasVisual && nodeOpacity > 0.001f) {
                            visibleElements++
                            nodes.add(
                                CompositeRenderNode.VisualElementNode(
                                    nodeId = el.elementId.ifBlank { "el_$idx" },
                                    layer = layerType,
                                    zIndex = StageTransformMath.clampZIndex(el.zIndex) * 100 + idx,
                                    opacity = nodeOpacity,
                                    element = el,
                                    resolvedUri = resolvedUri,
                                    resolvedPresetKey = resolvedPreset,
                                    resolvedImageBase64 = resolvedBase64,
                                    hasVisualAsset = true,
                                    hasTransformKeyframes = evalTransform?.hasTransformKeyframes ?: false
                                )
                            )
                        }
                    }

                if (layerType == SceneLayerType.OVERLAY) {
                    nodes.add(
                        CompositeRenderNode.OverlayStackNode(
                            opacity = layerOpacity
                        )
                    )
                }
            }

            ResolvedLayerPlan(
                layerType = layerType,
                order = layerState.order,
                visible = effectiveLayerVisible,
                opacity = layerOpacity,
                parallaxFactor = layerState.parallaxFactor,
                nodes = nodes.sortedBy { it.zIndex },
                depthTransform = layerDepthTransform
            )
        }

        return SceneCompositionPlan(
            orderedLayers = resolvedLayers,
            visibleCharacterCount = visibleChars,
            visibleVisualElementCount = visibleElements,
            evaluatedCamera = cameraState,
            evaluatedTimeSec = evaluatedFrame?.timeSec
        )
    }
}
