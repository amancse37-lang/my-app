package com.example

import com.example.animation.AnimatableProperty
import com.example.animation.AnimationTargetType
import com.example.animation.EasingType
import com.example.data.AiGraphicSuggestion
import com.example.data.AnimationKeyframe
import com.example.data.DialogueLine
import com.example.data.InfographicDataPoint
import com.example.data.MapMarkerItem
import com.example.data.MapRouteItem
import com.example.data.MotionGraphicElement
import com.example.data.SceneAudioClip
import com.example.data.SceneTransitionConfig
import com.example.data.ScriptAiService
import com.example.data.StudioHistorySnapshot
import com.example.data.StudioPresets
import com.example.data.StudioProject
import com.example.data.StudioRepository
import com.example.data.StudioScene
import com.example.data.SubtitleCue
import com.example.data.SubtitleStyleConfig
import com.example.data.TimelineTrackType
import com.example.graphics.MotionGraphicsEngine
import com.example.graphics.SubtitleEngine
import com.example.graphics.TransitionFlowEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure JVM JUnit 4 verification suite for Phase 6:
 * Motion Graphics, Animated Typography, Infographics, Documentary Maps,
 * Subtitles & SRT Export, Dual-Scene Transitions, Story Flow, AI Graphic Suggestions,
 * Asset Library Template Serialization, Firestore Round-Trip, Undo/Redo & Legacy Compatibility.
 */
class Phase6MotionGraphicsTransitionsTest {

    private fun sampleScene(
        id: String = "scene_1",
        title: String = "The High Passes",
        durationSec: Double = 8.0,
        outgoingTransition: SceneTransitionConfig = SceneTransitionConfig()
    ): StudioScene = StudioScene(
        id = id,
        title = title,
        description = "Mist curls over the northern mountain trade route.",
        narrationText = "Long before highways crossed these valleys, every pass carried silk and memory. Historians mapped every mile.",
        durationSec = durationSec,
        outgoingTransition = outgoingTransition
    )

    @Test
    fun `01 all 13 documentary templates instantiate with distinct valid layout defaults`() {
        val scene = sampleScene()
        assertEquals(13, StudioPresets.documentaryGraphicTemplates.size)

        StudioPresets.documentaryGraphicTemplates.forEach { (key, _) ->
            val gfx = MotionGraphicsEngine.createTemplateGraphic(key, scene, startSec = 0.5)
            assertTrue("Template $key should have non-blank primaryText", gfx.primaryText.isNotBlank())
            assertTrue("Template $key duration must be positive", gfx.durationSec >= 1.0)
            assertEquals(0.5, gfx.startSec, 0.001)
            val evalMid = MotionGraphicsEngine.evaluateGraphicAtTime(gfx, emptyList(), 1.5)
            assertTrue("Template $key must be visible inside its window", evalMid.visible)
            assertTrue("Template $key opacity must be finite", !evalMid.opacity.isNaN() && evalMid.opacity > 0f)
        }
    }

    @Test
    fun `02 all entrance and exit text animation presets evaluate deterministically`() {
        val entrances = listOf("NONE", "FADE_IN", "SLIDE_IN", "TYPEWRITER", "ZOOM_IN", "REVEAL", "POP", "RISE", "DROP", "TRACKING_REVEAL")
        val exits = listOf("NONE", "FADE_OUT", "SLIDE_OUT", "ZOOM_OUT")

        entrances.forEach { ent ->
            exits.forEach { exit ->
                val g = MotionGraphicElement(
                    graphicId = "gfx_${ent}_$exit",
                    title = "Test",
                    primaryText = "Documentary Headline",
                    startSec = 1.0,
                    durationSec = 4.0,
                    entranceAnimation = ent,
                    exitAnimation = exit,
                    entranceDurationSec = 0.8,
                    exitDurationSec = 0.6
                )
                val before = MotionGraphicsEngine.evaluateGraphicAtTime(g, emptyList(), 0.5)
                assertFalse("Before startSec graphic must be hidden", before.visible)

                val early = MotionGraphicsEngine.evaluateGraphicAtTime(g, emptyList(), 1.2)
                val settled = MotionGraphicsEngine.evaluateGraphicAtTime(g, emptyList(), 3.5)
                val ending = MotionGraphicsEngine.evaluateGraphicAtTime(g, emptyList(), 4.8)
                val after = MotionGraphicsEngine.evaluateGraphicAtTime(g, emptyList(), 5.5)

                assertTrue("Settled frame should be visible for $ent/$exit", settled.visible)
                assertEquals(1.0f, settled.opacity, 0.01f)
                assertFalse("After endSec graphic must be hidden", after.visible)

                if (ent == "TYPEWRITER") {
                    assertTrue(early.visiblePrimaryChars < g.primaryText.length)
                    assertEquals(g.primaryText.length, settled.visiblePrimaryChars)
                }
                if (ent == "REVEAL") {
                    assertTrue(early.clipRevealFraction < 0.99f)
                    assertEquals(1.0f, settled.clipRevealFraction, 0.01f)
                }
                if (ent == "TRACKING_REVEAL") {
                    assertTrue(early.letterSpacingBoostSp > 0.1f)
                    assertEquals(0f, settled.letterSpacingBoostSp, 0.01f)
                }
                if (exit == "FADE_OUT") {
                    assertTrue(ending.opacity < settled.opacity)
                }
            }
        }
    }

    @Test
    fun `03 Phase 3 keyframes compose cleanly with Phase 6 graphic evaluation`() {
        val gfx = MotionGraphicElement(
            graphicId = "gfx_kf",
            startSec = 0.0,
            durationSec = 6.0,
            posX = -100.0,
            posY = 0.0,
            entranceAnimation = "NONE",
            exitAnimation = "NONE"
        )
        val keyframes = listOf(
            AnimationKeyframe(
                keyframeId = "k1",
                targetType = AnimationTargetType.GRAPHIC.name,
                targetId = "gfx_kf",
                property = AnimatableProperty.POS_X.name,
                timeSec = 0.0,
                value = -100.0,
                easing = EasingType.LINEAR.name
            ),
            AnimationKeyframe(
                keyframeId = "k2",
                targetType = AnimationTargetType.GRAPHIC.name,
                targetId = "gfx_kf",
                property = AnimatableProperty.POS_X.name,
                timeSec = 4.0,
                value = 100.0,
                easing = EasingType.LINEAR.name
            )
        )
        val mid = MotionGraphicsEngine.evaluateGraphicAtTime(gfx, keyframes, 2.0)
        assertTrue(mid.hasKeyframes)
        assertEquals(0.0, mid.posX, 0.5)
    }

    @Test
    fun `04 graphic duplication preserves styling and shifts position safely`() {
        val original = MotionGraphicsEngine.createTemplateGraphic("LOWER_THIRD", sampleScene(), startSec = 1.0)
        val copy = MotionGraphicsEngine.duplicateGraphic(original, "copy1")
        assertTrue(copy.graphicId != original.graphicId)
        assertEquals(original.primaryText, copy.primaryText)
        assertEquals(original.layoutPreset, copy.layoutPreset)
        assertTrue(copy.posX != original.posX || copy.posY != original.posY)
        assertEquals(original.zIndex + 1, copy.zIndex)
    }

    @Test
    fun `05 all 10 infographic types create valid datasets and sanitize NaN or Infinity`() {
        val scene = sampleScene()
        assertEquals(10, StudioPresets.infographicTypeOptions.size)
        StudioPresets.infographicTypeOptions.forEach { (typeKey, _) ->
            val info = MotionGraphicsEngine.createInfographicTemplate(typeKey, scene)
            assertEquals("INFOGRAPHIC", info.category)
            assertEquals(typeKey, info.infographicType)
            assertTrue("Infographic $typeKey must have >= 1 data point", info.dataPoints.isNotEmpty())
        }

        val badPoints = listOf(
            InfographicDataPoint("p1", "Valid", 120.0, 40.0),
            InfographicDataPoint("p2", "", Double.NaN, Double.POSITIVE_INFINITY),
            InfographicDataPoint("p3", "Negative", -25.0, Double.NEGATIVE_INFINITY)
        )
        val validated = MotionGraphicsEngine.validateInfographicData(badPoints)
        assertEquals(3, validated.points.size)
        assertTrue(validated.sanitizedWarnings.isNotEmpty())
        validated.points.forEach { p ->
            assertFalse(p.value.isNaN() || p.value.isInfinite())
            assertFalse(p.secondaryValue.isNaN() || p.secondaryValue.isInfinite())
            assertTrue(p.label.isNotBlank())
        }

        // Counter animation & pie sweeps deterministic math
        assertEquals(0.0, MotionGraphicsEngine.evaluateAnimatedNumber(500.0, 0f), 0.001)
        assertEquals(250.0, MotionGraphicsEngine.evaluateAnimatedNumber(500.0, 0.5f), 0.001)
        assertEquals(500.0, MotionGraphicsEngine.evaluateAnimatedNumber(500.0, 1.0f), 0.001)

        val pieDataset = MotionGraphicsEngine.validateInfographicData(
            listOf(
                InfographicDataPoint("a", "A", 50.0),
                InfographicDataPoint("b", "B", 50.0)
            )
        )
        val sweeps = MotionGraphicsEngine.evaluatePieSweeps(pieDataset, internalProgress = 1.0f)
        assertEquals(360f, sweeps.sum(), 0.2f)
    }

    @Test
    fun `06 documentary map camera zoom pan route reveal and marker appear timing`() {
        val mapGfx = MotionGraphicsEngine.createMapGraphicTemplate(sampleScene(), startSec = 0.0)
        assertEquals("MAP", mapGfx.category)
        assertTrue(mapGfx.mapMarkers.isNotEmpty())
        assertTrue(mapGfx.mapRoutes.isNotEmpty())

        val camStart = MotionGraphicsEngine.evaluateMapCameraAtTime(mapGfx, 0.0)
        val camEnd = MotionGraphicsEngine.evaluateMapCameraAtTime(mapGfx, mapGfx.durationSec)
        assertEquals(mapGfx.mapZoomStart.toFloat(), camStart.zoom, 0.01f)
        assertEquals(mapGfx.mapZoomEnd.toFloat(), camEnd.zoom, 0.01f)

        val route = mapGfx.mapRoutes.first()
        assertEquals(0f, MotionGraphicsEngine.evaluateRouteRevealFraction(route, 0.0), 0.001f)
        assertEquals(1f, MotionGraphicsEngine.evaluateRouteRevealFraction(route, route.startSec + route.durationSec + 0.5), 0.001f)

        val marker = mapGfx.mapMarkers.first()
        assertEquals(0f, MotionGraphicsEngine.evaluateMarkerAppearProgress(marker, 0.0), 0.001f)
        assertEquals(1f, MotionGraphicsEngine.evaluateMarkerAppearProgress(marker, marker.appearAtSec + 1.0), 0.001f)
    }

    @Test
    fun `07 subtitle segmentation handles English and Hindi Devanagari danda`() {
        val english = "Long before highways crossed these valleys, every pass carried silk and memory. Historians mapped every mile with care!"
        val engChunks = SubtitleEngine.segmentTextIntoChunks(english, maxCharsPerCue = 48)
        assertTrue(engChunks.size >= 2)
        assertTrue(engChunks.all { it.length <= 52 })

        val hindi = "सदियों पहले इन्हीं पहाड़ी दर्रों से कहानियाँ सफर करती थीं। हर पुराने दस्तावेज़ के पीछे एक जीवित कहानी छुपी होती है।"
        val hiChunks = SubtitleEngine.segmentTextIntoChunks(hindi, maxCharsPerCue = 60)
        assertEquals(2, hiChunks.size)
        assertTrue(hiChunks[0].endsWith("।"))
        assertTrue(hiChunks[1].endsWith("।"))
    }

    @Test
    fun `08 audio-aware subtitle generation distinguishes real audio clips vs speech estimates`() {
        val sceneNoAudio = sampleScene(durationSec = 9.0)
        val (estCues, usedAudio1) = SubtitleEngine.generateSubtitlesForScene(sceneNoAudio)
        assertFalse("Should honestly report estimated speech when no audio clip exists", usedAudio1)
        assertTrue(estCues.isNotEmpty())

        val sceneWithRealClip = sceneNoAudio.copy(
            dialogueLines = listOf(
                DialogueLine(
                    lineId = "dl_1",
                    speakerName = "Dr. Aarav",
                    text = "Every mountain pass carried ink and silk.",
                    startSec = 0.5,
                    durationSec = 3.0,
                    clipId = "clip_real_1"
                )
            ),
            audioClips = listOf(
                SceneAudioClip(
                    clipId = "clip_real_1",
                    trackType = "DIALOGUE",
                    dialogueLineId = "dl_1",
                    startSec = 0.5,
                    sourceDurationSec = 4.2,
                    trimStartSec = 0.0,
                    trimEndSec = 4.2
                )
            )
        )
        val (audioCues, usedAudio2) = SubtitleEngine.generateSubtitlesForScene(sceneWithRealClip)
        assertTrue("Should report real attached audio timing when clip exists", usedAudio2)
        assertEquals(0.5, audioCues.first().startSec, 0.05)
        assertEquals(4.7, audioCues.last().endSec, 0.1)
    }

    @Test
    fun `09 subtitle diagnostics detect overlaps fitting fixes them and splitCue divides cleanly`() {
        val overlapping = listOf(
            SubtitleCue("c1", "s1", text = "First documentary line here", startSec = 0.0, endSec = 4.0),
            SubtitleCue("c2", "s1", text = "Second overlapping caption", startSec = 2.5, endSec = 9.5)
        )
        val diagBefore = SubtitleEngine.analyzeSubtitleDiagnostics(overlapping, sceneDurationSec = 8.0)
        assertEquals(1, diagBefore.overlapCount)
        assertEquals(1, diagBefore.outOfBoundsCount)

        val fixed = SubtitleEngine.fitSubtitlesToDuration(overlapping, sceneDurationSec = 8.0)
        val diagAfter = SubtitleEngine.analyzeSubtitleDiagnostics(fixed, sceneDurationSec = 8.0)
        assertEquals(0, diagAfter.overlapCount)
        assertEquals(0, diagAfter.outOfBoundsCount)

        val split = SubtitleEngine.splitCue(fixed, "c1")
        assertEquals(3, split.size)
        assertTrue(split[0].text.isNotBlank() && split[1].text.isNotBlank())
        assertEquals(split[0].endSec, split[1].startSec, 0.01)
    }

    @Test
    fun `10 SRT timestamp formatting and multi-scene SubRip export are standards compliant`() {
        assertEquals("00:00:00,000", SubtitleEngine.formatSrtTimestamp(0.0))
        assertEquals("00:01:05,250", SubtitleEngine.formatSrtTimestamp(65.25))
        assertEquals("01:02:03,500", SubtitleEngine.formatSrtTimestamp(3723.5))

        val s1 = sampleScene(id = "s1", durationSec = 5.0).copy(
            subtitleCues = listOf(
                SubtitleCue("c1", "s1", speakerName = "Aarav", text = "Opening line.", startSec = 0.5, endSec = 2.5)
            )
        )
        val s2 = sampleScene(id = "s2", durationSec = 6.0).copy(
            subtitleCues = listOf(
                SubtitleCue("c2", "s2", speakerName = "", text = "Second scene line.", startSec = 1.0, endSec = 4.0)
            )
        )
        val srt = SubtitleEngine.generateSrtForProject(listOf(s1, s2))
        assertTrue(srt.contains("1\n00:00:00,500 --> 00:00:02,500\nAarav: Opening line."))
        assertTrue(srt.contains("2\n00:00:06,000 --> 00:00:09,000\nSecond scene line."))
    }

    @Test
    fun `11 all 8 transition types compute deterministic transforms without mutating scene durations`() {
        val types = listOf("CUT", "FADE", "CROSSFADE", "DISSOLVE", "WIPE", "SLIDE", "ZOOM", "BLUR")
        types.forEach { t ->
            val cfg = SceneTransitionConfig(type = t, durationSec = 1.0, direction = "LEFT", feather = 0.5)
            val atStart = TransitionFlowEngine.evaluateTransitionTransforms(cfg, 0f)
            val atMid = TransitionFlowEngine.evaluateTransitionTransforms(cfg, 0.5f)
            val atEnd = TransitionFlowEngine.evaluateTransitionTransforms(cfg, 1f)

            assertFalse(atMid.outgoingAlpha.isNaN() || atMid.incomingAlpha.isNaN())
            when (t) {
                "FADE" -> {
                    assertTrue(atMid.dipBlackOverlayAlpha >= 0.9f || (atMid.outgoingAlpha < 0.1f && atMid.incomingAlpha < 0.1f))
                }
                "CROSSFADE", "DISSOLVE" -> {
                    assertTrue(atMid.outgoingAlpha > 0.1f && atMid.incomingAlpha > 0.1f)
                }
                "WIPE" -> {
                    assertEquals(0f, atStart.incomingWipeClipFraction, 0.01f)
                    assertEquals(0.5f, atMid.incomingWipeClipFraction, 0.01f)
                    assertEquals(1f, atEnd.incomingWipeClipFraction, 0.01f)
                }
                "SLIDE" -> {
                    assertEquals(-0.5f, atMid.outgoingOffsetXFraction, 0.01f)
                    assertEquals(0.5f, atMid.incomingOffsetXFraction, 0.01f)
                }
                "ZOOM" -> {
                    assertTrue(atMid.outgoingScale > 1.0f)
                    assertTrue(atMid.incomingScale < 1.0f)
                }
                "BLUR" -> {
                    assertTrue(atMid.outgoingBlurRadiusDp > 0f && atMid.incomingBlurRadiusDp > 0f)
                }
            }
        }
    }

    @Test
    fun `12 transition resolution activates cleanly across scene boundary and preserves total duration`() {
        val s1 = sampleScene("s1", durationSec = 6.0, outgoingTransition = SceneTransitionConfig(type = "CROSSFADE", durationSec = 1.0))
        val s2 = sampleScene("s2", durationSec = 5.0, outgoingTransition = SceneTransitionConfig(type = "CUT"))
        val scenes = listOf(s1, s2)

        val segments = TransitionFlowEngine.computeStoryFlowSegments(scenes)
        assertEquals(0.0, segments[0].startSec, 0.001)
        assertEquals(6.0, segments[0].endSec, 0.001)
        assertEquals(6.0, segments[1].startSec, 0.001)
        assertEquals(11.0, segments[1].endSec, 0.001)

        // Outside transition window [5.5, 6.5]
        assertNull(TransitionFlowEngine.resolveTransitionAtPlayhead(scenes, 2.0))
        assertNull(TransitionFlowEngine.resolveTransitionAtPlayhead(scenes, 8.0))

        // Inside transition window at boundary 6.0 -> progress = 0.5
        val active = TransitionFlowEngine.resolveTransitionAtPlayhead(scenes, 6.0)
        assertNotNull(active)
        assertEquals("s1", active!!.outgoingScene.id)
        assertEquals("s2", active.incomingScene.id)
        assertEquals(0.5f, active.progress, 0.05f)
        // Scene durations unchanged
        assertEquals(6.0, s1.durationSec, 0.001)
        assertEquals(5.0, s2.durationSec, 0.001)
    }

    @Test
    fun `13 AI graphic suggestions JSON parses safely and converts to editable graphics`() {
        val json = """
            {
              "suggestions": [
                {
                  "category": "LOWER_THIRD",
                  "templateType": "LOWER_THIRD",
                  "title": "Dr. Aarav Intro",
                  "primaryText": "Dr. Aarav Mehta",
                  "secondaryText": "Lead Historian · Delhi Archive",
                  "rationale": "Introduces main narrator when he speaks",
                  "suggestedStartSec": 1.2,
                  "suggestedDurationSec": 4.0,
                  "infographicType": "",
                  "suggestedDataSummary": ""
                },
                {
                  "category": "INFOGRAPHIC",
                  "templateType": "INFOGRAPHIC",
                  "title": "Trade Volume Growth",
                  "primaryText": "Pass Trade Tonnage",
                  "secondaryText": "1840–1880",
                  "rationale": "Visualizes script trade figures",
                  "suggestedStartSec": 3.0,
                  "suggestedDurationSec": 4.5,
                  "infographicType": "BAR_CHART",
                  "suggestedDataSummary": "1840:120, 1860:245, 1880:390"
                }
              ]
            }
        """.trimIndent()

        val result = ScriptAiService.parseGraphicSuggestionsJson(json, sceneDurationSec = 10.0)
        assertTrue(result.isSuccess)
        val suggestions = result.getOrThrow()
        assertEquals(2, suggestions.size)
        assertEquals("LOWER_THIRD", suggestions[0].templateType)
        assertEquals("BAR_CHART", suggestions[1].infographicType)

        val instantiated = MotionGraphicsEngine.createGraphicFromSuggestion(suggestions[1], sampleScene(durationSec = 10.0))
        assertEquals("INFOGRAPHIC", instantiated.category)
        assertEquals("BAR_CHART", instantiated.infographicType)
        assertEquals(3, instantiated.dataPoints.size)
        assertEquals(245.0, instantiated.dataPoints[1].value, 0.001)
    }

    @Test
    fun `14 graphic template Asset Library JSON serialization round-trips losslessly`() {
        val mapGfx = MotionGraphicsEngine.createMapGraphicTemplate(sampleScene())
        val json = MotionGraphicsEngine.serializeGraphicTemplate(mapGfx)
        assertTrue(json.isNotBlank())

        val restored = MotionGraphicsEngine.deserializeGraphicTemplate(json, newGraphicId = "gfx_restored", startSec = 2.0)
        assertNotNull(restored)
        assertEquals("gfx_restored", restored!!.graphicId)
        assertEquals(2.0, restored.startSec, 0.001)
        assertEquals(mapGfx.primaryText, restored.primaryText)
        assertEquals(mapGfx.mapMarkers.size, restored.mapMarkers.size)
        assertEquals(mapGfx.mapRoutes.size, restored.mapRoutes.size)
        assertEquals(mapGfx.mapZoomEnd, restored.mapZoomEnd, 0.001)
    }

    @Test
    fun `15 Firestore repository Phase 6 serializers round-trip and clamp invalid numbers`() {
        val gfx = MotionGraphicsEngine.createInfographicTemplate("COMPARISON", sampleScene()).copy(
            posX = Double.NaN,
            opacity = 5.0
        )
        val gfxMaps = StudioRepository.motionGraphicsToMaps(listOf(gfx))
        val restoredGfx = StudioRepository.mapsToMotionGraphics(gfxMaps).first()
        assertEquals(0.0, restoredGfx.posX, 0.001)
        assertEquals(1.0, restoredGfx.opacity, 0.001)
        assertEquals(gfx.dataPoints.size, restoredGfx.dataPoints.size)

        val cues = listOf(SubtitleCue("c1", "s1", speakerName = "Meera", text = "Archive note.", startSec = 0.5, endSec = 3.0))
        val restoredCues = StudioRepository.mapsToSubtitleCues(StudioRepository.subtitleCuesToMaps(cues))
        assertEquals(1, restoredCues.size)
        assertEquals("Meera", restoredCues[0].speakerName)

        val style = SubtitleEngine.applyStylePreset("HIGH_CONTRAST_YELLOW", SubtitleStyleConfig())
        val restoredStyle = StudioRepository.mapToSubtitleStyle(StudioRepository.subtitleStyleToMap(style))
        assertEquals("HIGH_CONTRAST_YELLOW", restoredStyle.stylePreset)
        assertEquals("#FFE066", restoredStyle.textColorHex)

        val tr = SceneTransitionConfig(type = "WIPE", durationSec = 1.2, direction = "RIGHT", feather = 0.4)
        val restoredTr = StudioRepository.mapToSceneTransition(StudioRepository.sceneTransitionToMap(tr))
        assertEquals("WIPE", restoredTr.type)
        assertEquals("RIGHT", restoredTr.direction)
        assertEquals(1.2, restoredTr.durationSec, 0.001)

        val sug = AiGraphicSuggestion("sg1", "MAP", "MAP_GRAPHIC", "Pass Route", "Northern Pass", "1842", "Shows journey", 1.0, 4.0)
        val restoredSug = StudioRepository.mapsToGraphicSuggestions(StudioRepository.graphicSuggestionsToMaps(listOf(sug)))
        assertEquals(1, restoredSug.size)
        assertEquals("MAP_GRAPHIC", restoredSug[0].templateType)
    }

    @Test
    fun `16 track muting Undo snapshot preservation and Phase 1-5 legacy backward compatibility`() {
        val lowerThird = MotionGraphicsEngine.createTemplateGraphic("LOWER_THIRD", sampleScene())
        assertEquals(TimelineTrackType.LOWER_THIRDS, MotionGraphicsEngine.trackForGraphic(lowerThird))
        assertTrue(MotionGraphicsEngine.isGraphicTrackMuted(lowerThird, setOf(TimelineTrackType.LOWER_THIRDS)))
        assertTrue(MotionGraphicsEngine.isGraphicTrackMuted(lowerThird, setOf(TimelineTrackType.MOTION_GRAPHICS)))
        assertFalse(MotionGraphicsEngine.isGraphicTrackMuted(lowerThird, setOf(TimelineTrackType.MAPS)))

        // Undo/Redo snapshot preserves all Phase 6 scene state
        val sceneWithPhase6 = sampleScene().copy(
            motionGraphics = listOf(lowerThird),
            subtitleCues = listOf(SubtitleCue("c1", "s1", text = "Hello", startSec = 0.0, endSec = 2.0)),
            outgoingTransition = SceneTransitionConfig(type = "DISSOLVE", durationSec = 1.1)
        )
        val snapshot = StudioHistorySnapshot(
            project = StudioProject(id = "p1", name = "Test Project"),
            scenes = listOf(sceneWithPhase6),
            selectedSceneId = sceneWithPhase6.id
        )
        assertEquals(1, snapshot.scenes.first().motionGraphics.size)
        assertEquals("DISSOLVE", snapshot.scenes.first().outgoingTransition.type)

        // Legacy Phase 1-5 scene without Phase 6 fields defaults cleanly without crashing
        val legacyScene = StudioScene(id = "legacy", title = "Old Scene", subtitleText = "Legacy caption", motionGraphicTitle = "Chapter 1")
        assertTrue(legacyScene.motionGraphics.isEmpty())
        assertTrue(legacyScene.subtitleCues.isEmpty())
        assertEquals("CUT", legacyScene.outgoingTransition.type)
        val legacySrt = SubtitleEngine.generateSrtForProject(listOf(legacyScene))
        assertTrue(legacySrt.contains("Legacy caption"))
    }
}
