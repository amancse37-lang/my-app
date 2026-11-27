package com.example

import com.example.compositor.CompositeRenderNode
import com.example.compositor.SceneCompositor
import com.example.compositor.SceneLayerType
import com.example.data.BackgroundGenerationRequest
import com.example.data.PropGenerationRequest
import com.example.data.SceneVisualAiService
import com.example.data.SceneVisualElement
import com.example.data.StudioAsset
import com.example.data.StudioHistorySnapshot
import com.example.data.StudioProject
import com.example.data.StudioRepository
import com.example.data.StudioScene
import com.example.data.SuggestedBackgroundPlan
import com.example.data.SuggestedPropPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase2VisualAiTest {

    private val visualAiService = SceneVisualAiService()

    @Test
    fun buildBackgroundPrompt_includesAllSceneFieldsAndStrictNoTextConstraints() {
        val request = BackgroundGenerationRequest(
            assetName = "Shahjahanabad Twilight Lane",
            sceneTitle = "Old Delhi Twilight Bazaar",
            sceneDescription = "Lantern-lit carved sandstone archway leading toward historic spice stalls.",
            sceneNarration = "In 1848, calligraphers traded maps beneath warm brass lanterns.",
            location = "Shahjahanabad Heritage Lane, Old Delhi",
            eraContext = "1848 Mughal-era Delhi",
            timeOfDay = "Twilight",
            weather = "Dusty Haze",
            visualStyle = "Painterly 2D Documentary",
            moodAtmosphere = "Warm amber lanterns against indigo shadows",
            customPrompt = "Deep atmospheric perspective with empty cobblestone foreground.",
            variationSeed = 2
        )

        val prompt = visualAiService.buildBackgroundPrompt(request)

        assertTrue(prompt.contains("16:9 widescreen"))
        assertTrue(prompt.contains("Painterly 2D Documentary"))
        assertTrue(prompt.contains("Shahjahanabad Heritage Lane, Old Delhi"))
        assertTrue(prompt.contains("1848 Mughal-era Delhi"))
        assertTrue(prompt.contains("Twilight"))
        assertTrue(prompt.contains("Dusty Haze"))
        assertTrue(prompt.contains("Warm amber lanterns against indigo shadows"))
        assertTrue(prompt.contains("Lantern-lit carved sandstone archway"))
        assertTrue(prompt.contains("calligraphers traded maps"))
        assertTrue(prompt.contains("Creative Variation #2"))
        assertTrue(prompt.contains("NO text"))
        assertTrue(prompt.contains("NO watermarks"))
        assertTrue(prompt.contains("NO random characters"))
    }

    @Test
    fun buildPropPrompt_distinguishesTransparentPngVsOpaqueBackdrop() {
        val transparentReq = PropGenerationRequest(
            propName = "Antique Brass Compass",
            propCategory = "Historical Object",
            propDescription = "Engraved 19th-century folding brass pocket compass with glass dial",
            eraContext = "1842 Himalayan expedition",
            visualStyle = "Painterly 2D Documentary",
            requestTransparentPng = true
        )
        val opaqueReq = transparentReq.copy(requestTransparentPng = false)

        val transparentPrompt = visualAiService.buildPropPrompt(transparentReq)
        val opaquePrompt = visualAiService.buildPropPrompt(opaqueReq)

        assertTrue(transparentPrompt.contains("Antique Brass Compass"))
        assertTrue(transparentPrompt.contains("transparent PNG background"))
        assertTrue(transparentPrompt.contains("Single prop only"))
        assertTrue(opaquePrompt.contains("uniform neutral dark studio backdrop"))
        assertFalse(opaquePrompt.contains("transparent PNG background"))
    }

    @Test
    fun defaultBackgroundRequestForScene_infersEraTimeAndWeather() {
        val scene = StudioScene(
            id = "sc_dawn",
            title = "Chapter I · Himalayan Dawn Pass",
            description = "Snowy mountain ridges with morning mist at sunrise.",
            narrationText = "In 1842 traders crossed the pass before noon.",
            effectPreset = "Warm Sepia"
        )
        val req = visualAiService.defaultBackgroundRequestForScene(scene, "Editorial Ink & Wash")

        assertEquals("Dawn", req.timeOfDay)
        assertEquals("Misty", req.weather)
        assertTrue(req.eraContext.contains("1842"))
        assertEquals("Editorial Ink & Wash", req.visualStyle)
        assertTrue(req.customPrompt.isNotBlank())
    }

    @Test
    fun normalizeSuggestions_validatesAndClampsBackgroundPropsAndDetails() {
        val rawBg = SuggestedBackgroundPlan(
            title = "  1842 Himalayan Dawn Valley  ",
            location = "Spiti High Mountain Pass",
            eraContext = "Mid-19th Century Silk Road",
            timeOfDay = "Dawn",
            weather = "Misty",
            moodAtmosphere = "Crisp golden dawn light over slate mist",
            prompt = "Wide 2D painterly documentary vista of snow-covered Himalayan mountain pass.",
            rationale = "Establishes scale and isolation for opening narration."
        )
        val rawProps = listOf(
            SuggestedPropPlan(
                name = "Weathered Silk Trade Chest",
                category = "Historical Object",
                suggestedLayer = "PROPS",
                prompt = "Carved wooden merchant chest reinforced with brass clasps.",
                rationale = "Visualizes the silk trade mentioned in narration."
            ),
            SuggestedPropPlan(
                name = "Stone Prayer Cairn",
                category = "Architecture",
                suggestedLayer = "MIDGROUND",
                prompt = "Stacked slate mountain trail marker with faded cloth.",
                rationale = "Adds midground depth parallax separation."
            ),
            SuggestedPropPlan(
                name = "Pine Foreground Branch",
                category = "Unknown Category",
                suggestedLayer = "INVALID_LAYER",
                prompt = "",
                rationale = "Creates foreground depth."
            ),
            SuggestedPropPlan(
                name = "   ",
                category = "Historical Object",
                suggestedLayer = "PROPS",
                prompt = "Should be filtered out",
                rationale = ""
            )
        )
        val rawDetails = listOf(
            "Cool slate shadows in lower valley contrasting warm amber rim light on peaks",
            "   ",
            "Keep center-left clear for narrator staging"
        )

        val parsed = visualAiService.normalizeSuggestions("scene_01", rawBg, rawProps, rawDetails)

        assertEquals("scene_01", parsed.sceneId)
        assertEquals("1842 Himalayan Dawn Valley", parsed.background.title)
        assertEquals("Dawn", parsed.background.timeOfDay)
        assertEquals("Misty", parsed.background.weather)
        assertEquals(3, parsed.props.size)
        assertEquals("PROPS", parsed.props[0].suggestedLayer)
        assertEquals("MIDGROUND", parsed.props[1].suggestedLayer)
        // Invalid layer and category safely fall back to defaults, blank prompt defaults to name
        assertEquals("PROPS", parsed.props[2].suggestedLayer)
        assertEquals("Documentary Artifact", parsed.props[2].category)
        assertEquals("Pine Foreground Branch", parsed.props[2].prompt)
        assertEquals(2, parsed.environmentalDetails.size)
    }

    @Test
    fun compositor_resolvesAiGeneratedBackgroundAssetAndMultipleAiPropsAcrossLayers() {
        val aiBgAsset = StudioAsset(
            id = "ai_bg_101",
            name = "AI Himalayan Dawn",
            category = "Background",
            imageBase64 = "BASE64_AI_BACKGROUND_PAYLOAD",
            hasTransparency = false,
            visualStyle = "Painterly 2D Documentary"
        )
        val aiProp1 = StudioAsset(
            id = "ai_prop_compass",
            name = "AI Brass Compass",
            category = "Prop",
            imageBase64 = "BASE64_TRANSPARENT_PNG_COMPASS",
            hasTransparency = true,
            visualStyle = "Painterly 2D Documentary"
        )
        val aiProp2 = StudioAsset(
            id = "ai_prop_lantern",
            name = "AI Brass Lantern",
            category = "Prop",
            imageBase64 = "BASE64_OPAQUE_LANTERN",
            hasTransparency = false,
            visualStyle = "Painterly 2D Documentary"
        )

        assertTrue(SceneCompositor.hasRenderableVisualAsset("", "", aiProp1))
        assertEquals("ai_bg_101", SceneCompositor.resolveBackgroundAssetId("asset:ai_bg_101"))
        assertNull(SceneCompositor.resolveBackgroundAssetId("himalayas"))

        val scene = StudioScene(
            id = "scene_ai_test",
            backgroundPreset = "asset:ai_bg_101",
            sceneElements = listOf(
                SceneVisualElement(
                    elementId = "el_compass",
                    name = "AI Brass Compass",
                    assetId = "ai_prop_compass",
                    layer = SceneLayerType.PROPS.name,
                    posX = -90.0,
                    posY = 40.0,
                    scale = 0.8,
                    zIndex = 1
                ),
                SceneVisualElement(
                    elementId = "el_lantern",
                    name = "AI Brass Lantern",
                    assetId = "ai_prop_lantern",
                    layer = SceneLayerType.FOREGROUND.name,
                    posX = 140.0,
                    posY = -60.0,
                    scale = 1.1,
                    zIndex = 2
                )
            )
        )

        val plan = SceneCompositor.buildCompositionPlan(
            scene = scene,
            characters = emptyList(),
            assets = listOf(aiBgAsset, aiProp1, aiProp2),
            mutedTracks = emptySet()
        )

        val nodes = plan.flattenedVisibleNodes()
        val bgNode = nodes.filterIsInstance<CompositeRenderNode.BackgroundBaseNode>().single()
        val elementNodes = nodes.filterIsInstance<CompositeRenderNode.VisualElementNode>()

        assertEquals("BASE64_AI_BACKGROUND_PAYLOAD", bgNode.resolvedImageBase64)
        assertEquals(2, plan.visibleVisualElementCount)
        assertEquals(2, elementNodes.size)
        assertEquals("BASE64_TRANSPARENT_PNG_COMPASS", elementNodes.first { it.nodeId == "el_compass" }.resolvedImageBase64)
        assertEquals("BASE64_OPAQUE_LANTERN", elementNodes.first { it.nodeId == "el_lantern" }.resolvedImageBase64)
        assertEquals(SceneLayerType.PROPS, elementNodes.first { it.nodeId == "el_compass" }.layer)
        assertEquals(SceneLayerType.FOREGROUND, elementNodes.first { it.nodeId == "el_lantern" }.layer)
    }

    @Test
    fun repositoryAssetSerializer_persistsAndBoundsAiVisualFields() {
        val oversizedBase64 = "A".repeat(330_000)
        val oversizedPrompt = "P".repeat(2_000)
        val asset = StudioAsset(
            name = "  AI Archive Desk  ",
            category = "Prop",
            tags = "ai,prop,desk",
            notes = "Generated desk prop",
            presetKey = "ai_prop",
            localUri = "file:///unsafe/local/path.png",
            durationLabel = "PNG α",
            imageBase64 = oversizedBase64,
            hasTransparency = true,
            visualStyle = "Painterly 2D Documentary",
            promptUsed = oversizedPrompt
        )

        val payload = StudioRepository.assetFieldsToMap(asset)

        assertEquals("AI Archive Desk", payload["name"])
        assertEquals("Prop", payload["category"])
        assertEquals("", payload["localUri"]) // Unsafe file:// URI stripped
        assertEquals(320_000, (payload["imageBase64"] as String).length)
        assertEquals(true, payload["hasTransparency"])
        assertEquals("Painterly 2D Documentary", payload["visualStyle"])
        assertEquals(1_500, (payload["promptUsed"] as String).length)
    }

    @Test
    fun undoRedoHistorySnapshot_preservesBackgroundReplacementAndPropPlacement() {
        val project = StudioProject(id = "p1", name = "Silk Road")
        val initialScene = StudioScene(id = "s1", backgroundPreset = "himalayas", sceneElements = emptyList())
        val updatedScene = initialScene.copy(
            backgroundPreset = "asset:ai_bg_101",
            sceneElements = listOf(
                SceneVisualElement(elementId = "el_1", name = "AI Compass", assetId = "ai_prop_1", layer = "PROPS")
            )
        )

        val beforeSnapshot = StudioHistorySnapshot(project = project, scenes = listOf(initialScene), selectedSceneId = "s1")
        val afterSnapshot = StudioHistorySnapshot(project = project, scenes = listOf(updatedScene), selectedSceneId = "s1")

        assertEquals("himalayas", beforeSnapshot.scenes.first().backgroundPreset)
        assertTrue(beforeSnapshot.scenes.first().sceneElements.isEmpty())
        assertEquals("asset:ai_bg_101", afterSnapshot.scenes.first().backgroundPreset)
        assertEquals(1, afterSnapshot.scenes.first().sceneElements.size)
    }
}
