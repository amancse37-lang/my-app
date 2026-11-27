package com.example

import com.example.compositor.CompositeRenderNode
import com.example.compositor.SceneCompositor
import com.example.compositor.SceneLayerType
import com.example.compositor.StageTransformMath
import com.example.data.LayerState
import com.example.data.SceneCharacterPlacement
import com.example.data.SceneVisualElement
import com.example.data.StudioAsset
import com.example.data.StudioCharacter
import com.example.data.StudioRepository
import com.example.data.StudioScene
import com.example.data.TimelineTrackType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneCompositorTest {

    private val charA = StudioCharacter(
        id = "char_a",
        charCode = "CHR-01",
        name = "Dr. Aarav Mehta",
        referenceImageBase64 = "fakeBase64PngData"
    )

    private val charB = StudioCharacter(
        id = "char_b",
        charCode = "CHR-02",
        name = "Meera Nair",
        referenceImageBase64 = ""
    )

    @Test
    fun normalizeLayerStates_migratesLegacyEmptyLayersToCanonicalSixLayerStack() {
        val layers = SceneCompositor.normalizeLayerStates(emptyList())
        assertEquals(6, layers.size)
        assertEquals(
            listOf("BACKGROUND", "MIDGROUND", "CHARACTERS", "PROPS", "FOREGROUND", "OVERLAY"),
            layers.map { it.layerKey }
        )
        assertEquals(listOf(0, 1, 2, 3, 4, 5), layers.map { it.order })
        assertTrue(layers.all { it.visible && it.opacity == 1.0 })
    }

    @Test
    fun layerVisibilityAndReordering_updatesCompositingStackDeterministically() {
        val initial = SceneLayerType.defaultLayerStates()
        val hiddenProps = SceneCompositor.toggleLayerVisibility(initial, SceneLayerType.PROPS)
        assertFalse(hiddenProps.first { it.layerKey == "PROPS" }.visible)

        // Move CHARACTERS forward (+1) so it renders above PROPS
        val reordered = SceneCompositor.moveLayerOrder(initial, SceneLayerType.CHARACTERS, +1)
        val charOrder = reordered.first { it.layerKey == "CHARACTERS" }.order
        val propsOrder = reordered.first { it.layerKey == "PROPS" }.order
        assertTrue(charOrder > propsOrder)
        assertEquals(listOf(0, 1, 2, 3, 4, 5), reordered.map { it.order })

        val dimmed = SceneCompositor.updateLayerOpacity(reordered, SceneLayerType.MIDGROUND, 0.45)
        assertEquals(0.45, dimmed.first { it.layerKey == "MIDGROUND" }.opacity, 0.001)
    }

    @Test
    fun stageTransformMath_mapsCoordinatesAndClampsDragResizeRotate() {
        val (px, py) = StageTransformMath.stageToCanvasPx(
            posX = 240.0,
            posY = -135.0,
            canvasWidthPx = 960f,
            canvasHeightPx = 540f
        )
        assertEquals(240f, px, 0.01f)
        assertEquals(-135f, py, 0.01f)

        val (dx, dy) = StageTransformMath.canvasPxDeltaToStageDelta(
            deltaPxX = 96f,
            deltaPxY = -54f,
            canvasWidthPx = 480f,
            canvasHeightPx = 270f
        )
        assertEquals(192.0, dx, 0.01)
        assertEquals(-108.0, dy, 0.01)

        val (clampedX, clampedY) = StageTransformMath.applyDragDelta(450.0, -260.0, 100.0, -50.0)
        assertEquals(StageTransformMath.MAX_POS_X, clampedX, 0.001)
        assertEquals(StageTransformMath.MIN_POS_Y, clampedY, 0.001)

        assertEquals(StageTransformMath.MAX_SCALE, StageTransformMath.applyResizeFactor(2.5, 2.0), 0.001)
        assertEquals(StageTransformMath.MIN_SCALE, StageTransformMath.applyResizeFactor(0.3, 0.1), 0.001)

        assertEquals(-170.0, StageTransformMath.normalizeRotation(190.0), 0.001)
        assertEquals(170.0, StageTransformMath.normalizeRotation(-190.0), 0.001)
    }

    @Test
    fun buildCompositionPlan_rendersMultipleCharactersWithIndependentTransformsAndZOrder() {
        val scene = StudioScene(
            id = "scene_multi_cast",
            backgroundPreset = "himalayas",
            characterCast = listOf(
                SceneCharacterPlacement(
                    characterId = "char_a",
                    posX = -120.0,
                    posY = 10.0,
                    scale = 1.1,
                    rotationDeg = -5.0,
                    layer = "CHARACTERS",
                    zIndex = 5,
                    visible = true,
                    opacity = 1.0,
                    flipX = false
                ),
                SceneCharacterPlacement(
                    characterId = "char_b",
                    posX = 110.0,
                    posY = 15.0,
                    scale = 0.9,
                    rotationDeg = 8.0,
                    layer = "CHARACTERS",
                    zIndex = 1,
                    visible = true,
                    opacity = 0.8,
                    flipX = true
                )
            )
        )

        val plan = SceneCompositor.buildCompositionPlan(
            scene = scene,
            characters = listOf(charA, charB),
            assets = emptyList(),
            mutedTracks = emptySet()
        )

        assertEquals(2, plan.visibleCharacterCount)
        val charLayer = plan.orderedLayers.first { it.layerType == SceneLayerType.CHARACTERS }
        val charNodes = charLayer.nodes.filterIsInstance<CompositeRenderNode.CharacterNode>()
        assertEquals(2, charNodes.size)
        // Lower zIndex (char_b, z=1) draws before higher zIndex (char_a, z=5)
        assertEquals("char_b", charNodes[0].character.id)
        assertEquals("char_a", charNodes[1].character.id)
        assertTrue(charNodes[0].placement.flipX)
        assertFalse(charNodes[1].placement.flipX)
        assertEquals(0.8f, charNodes[0].opacity, 0.001f)
    }

    @Test
    fun buildCompositionPlan_filtersUnrenderablePropsAndRespectsLayerVisibilityAndTimelineMutes() {
        val linkedVisualAsset = StudioAsset(
            id = "asset_img_1",
            name = "Archival Map",
            category = "Image",
            localUri = "content://media/picker/0/item/42"
        )
        val scene = StudioScene(
            id = "scene_elements",
            backgroundPreset = "archive_study",
            characterCast = listOf(
                SceneCharacterPlacement(characterId = "char_a", visible = true)
            ),
            sceneElements = listOf(
                SceneVisualElement(
                    elementId = "el_valid_uri",
                    name = "Compass PNG",
                    assetUri = "content://media/picker/0/item/99",
                    layer = "PROPS",
                    zIndex = 2,
                    visible = true
                ),
                SceneVisualElement(
                    elementId = "el_linked_asset",
                    name = "Linked Map",
                    assetId = "asset_img_1",
                    layer = "MIDGROUND",
                    zIndex = 1,
                    visible = true
                ),
                SceneVisualElement(
                    elementId = "el_fake_no_asset",
                    name = "Text Only Prop",
                    assetUri = "",
                    assetPresetKey = "nonexistent_preset",
                    layer = "PROPS",
                    visible = true
                ),
                SceneVisualElement(
                    elementId = "el_unsafe_file_uri",
                    name = "Unsafe Local File",
                    assetUri = "file:///etc/passwd",
                    layer = "FOREGROUND",
                    visible = true
                )
            )
        )

        val fullPlan = SceneCompositor.buildCompositionPlan(
            scene = scene,
            characters = listOf(charA),
            assets = listOf(linkedVisualAsset),
            mutedTracks = emptySet()
        )
        // Only the 2 real visual assets render; text-only and file:// URI are excluded
        assertEquals(2, fullPlan.visibleVisualElementCount)
        val renderedIds = fullPlan.flattenedVisibleNodes()
            .filterIsInstance<CompositeRenderNode.VisualElementNode>()
            .map { it.element.elementId }
        assertEquals(listOf("el_linked_asset", "el_valid_uri"), renderedIds)

        // Muting OBJECT track hides PROPS layer elements while keeping MIDGROUND visible
        val mutedObjectPlan = SceneCompositor.buildCompositionPlan(
            scene = scene,
            characters = listOf(charA),
            assets = listOf(linkedVisualAsset),
            mutedTracks = setOf(TimelineTrackType.OBJECT)
        )
        assertEquals(1, mutedObjectPlan.visibleVisualElementCount)
        assertFalse(mutedObjectPlan.orderedLayers.first { it.layerType == SceneLayerType.PROPS }.visible)

        // Hiding MIDGROUND layer via layerStates excludes midground visual element
        val hiddenMidScene = scene.copy(
            layerStates = SceneCompositor.toggleLayerVisibility(emptyList(), SceneLayerType.MIDGROUND)
        )
        val hiddenMidPlan = SceneCompositor.buildCompositionPlan(
            scene = hiddenMidScene,
            characters = listOf(charA),
            assets = listOf(linkedVisualAsset),
            mutedTracks = emptySet()
        )
        assertEquals(1, hiddenMidPlan.visibleVisualElementCount)
    }

    @Test
    fun repositoryPersistenceMappers_clampBoundsAndSanitizeUnsafeUris() {
        val rawElements = (1..30).map { i ->
            SceneVisualElement(
                elementId = "el_$i",
                name = "Element $i",
                assetUri = if (i == 1) "file:///unsafe/path.png" else "content://media/item/$i",
                posX = 9999.0,
                posY = -9999.0,
                scale = 10.0,
                rotationDeg = 720.0,
                zIndex = 500,
                opacity = 2.5,
                flipX = true
            )
        }

        val mappedElements = StudioRepository.sceneElementsToMaps(rawElements)
        // Enforces Firestore rule ceiling of 24 elements
        assertEquals(24, mappedElements.size)
        // Unsafe file:// URI stripped to empty string
        assertEquals("", mappedElements[0]["assetUri"])
        // Safe content:// URI preserved
        assertEquals("content://media/item/2", mappedElements[1]["assetUri"])
        // Clamped transforms
        assertEquals(1000.0, mappedElements[0]["posX"])
        assertEquals(-1000.0, mappedElements[0]["posY"])
        assertEquals(3.0, mappedElements[0]["scale"])
        assertEquals(360.0, mappedElements[0]["rotationDeg"])
        assertEquals(100, mappedElements[0]["zIndex"])
        assertEquals(1.0, mappedElements[0]["opacity"])
        assertEquals(true, mappedElements[0]["flipX"])

        val mappedLayers = StudioRepository.layerStatesToMaps(
            listOf(LayerState(layerKey = "PROPS", visible = false, order = 0, opacity = 0.6))
        )
        // Always emits full canonical 6-layer stack
        assertEquals(6, mappedLayers.size)
        val propsMap = mappedLayers.first { it["layerKey"] == "PROPS" }
        assertEquals(false, propsMap["visible"])
        assertEquals(0.6, propsMap["opacity"])

        val mappedCast = StudioRepository.castListToMaps(
            listOf(
                SceneCharacterPlacement(
                    characterId = "char_a",
                    layer = "FOREGROUND",
                    zIndex = 999,
                    visible = false,
                    opacity = -1.0,
                    flipX = true
                )
            )
        )
        assertEquals("FOREGROUND", mappedCast[0]["layer"])
        assertEquals(100, mappedCast[0]["zIndex"])
        assertEquals(false, mappedCast[0]["visible"])
        assertEquals(0.0, mappedCast[0]["opacity"])
        assertEquals(true, mappedCast[0]["flipX"])
    }
}
