package com.example

import com.example.animation.AnimatableProperty
import com.example.animation.AnimationEvaluator
import com.example.animation.AnimationTargetType
import com.example.animation.EasingType
import com.example.compositor.CompositeRenderNode
import com.example.compositor.SceneCompositor
import com.example.data.AnimationKeyframe
import com.example.data.CharacterActingCue
import com.example.data.CharacterReferenceSheet
import com.example.data.SceneCharacterPlacement
import com.example.data.SceneVisualElement
import com.example.data.StudioAsset
import com.example.data.StudioCharacter
import com.example.data.StudioHistorySnapshot
import com.example.data.StudioProject
import com.example.data.StudioRepository
import com.example.data.StudioScene
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase3AnimationTest {

    private fun sampleCharacter(id: String = "char_1", code: String = "CHR-01", primaryImg: String = "BASE64_PRIMARY_PORTRAIT"): StudioCharacter {
        return StudioCharacter(
            id = id,
            charCode = code,
            name = "Dr. Aarav Mehta",
            referenceImageBase64 = primaryImg
        )
    }

    private fun sampleScene(
        durationSec: Double = 8.0,
        cast: List<SceneCharacterPlacement> = listOf(
            SceneCharacterPlacement(
                characterId = "char_1",
                expression = "Calm",
                action = "Standing",
                poseView = "Three-Quarter View",
                posX = -100.0,
                posY = 10.0,
                scale = 1.0,
                rotationDeg = 0.0,
                opacity = 1.0,
                flipX = false
            )
        ),
        elements: List<SceneVisualElement> = emptyList(),
        keyframes: List<AnimationKeyframe> = emptyList(),
        actingCues: List<CharacterActingCue> = emptyList()
    ): StudioScene {
        return StudioScene(
            id = "scene_1",
            title = "Test Animated Scene",
            durationSec = durationSec,
            backgroundPreset = "himalayas",
            characterCast = cast,
            sceneElements = elements,
            animationKeyframes = keyframes,
            characterActingCues = actingCues,
            cameraMovement = "Slow Push In",
            cameraZoom = 1.2
        )
    }

    // 1. Keyframe interpolation (linear, clamping before/after, unsorted input, single keyframe)
    @Test
    fun linearKeyframeInterpolationAndBoundaryClamping() {
        val kfs = listOf(
            AnimationKeyframe("kf_b", "CHARACTER", "char_1", "POS_X", 4.0, 200.0, "LINEAR"),
            AnimationKeyframe("kf_a", "CHARACTER", "char_1", "POS_X", 0.0, -200.0, "LINEAR")
        )
        // Unsorted input still evaluates monotonically
        assertEquals(-200.0, AnimationEvaluator.evaluateProperty(kfs, AnimationTargetType.CHARACTER, "char_1", AnimatableProperty.POS_X, -1.0, 0.0), 1e-6)
        assertEquals(-200.0, AnimationEvaluator.evaluateProperty(kfs, AnimationTargetType.CHARACTER, "char_1", AnimatableProperty.POS_X, 0.0, 0.0), 1e-6)
        assertEquals(0.0, AnimationEvaluator.evaluateProperty(kfs, AnimationTargetType.CHARACTER, "char_1", AnimatableProperty.POS_X, 2.0, 0.0), 1e-6)
        assertEquals(100.0, AnimationEvaluator.evaluateProperty(kfs, AnimationTargetType.CHARACTER, "char_1", AnimatableProperty.POS_X, 3.0, 0.0), 1e-6)
        assertEquals(200.0, AnimationEvaluator.evaluateProperty(kfs, AnimationTargetType.CHARACTER, "char_1", AnimatableProperty.POS_X, 4.0, 0.0), 1e-6)
        assertEquals(200.0, AnimationEvaluator.evaluateProperty(kfs, AnimationTargetType.CHARACTER, "char_1", AnimatableProperty.POS_X, 7.5, 0.0), 1e-6)

        // Single keyframe returns its value everywhere
        val single = listOf(AnimationKeyframe("kf_s", "CHARACTER", "char_1", "SCALE", 2.0, 1.5, "LINEAR"))
        assertEquals(1.5, AnimationEvaluator.evaluateProperty(single, AnimationTargetType.CHARACTER, "char_1", AnimatableProperty.SCALE, 0.0, 1.0), 1e-6)
        assertEquals(1.5, AnimationEvaluator.evaluateProperty(single, AnimationTargetType.CHARACTER, "char_1", AnimatableProperty.SCALE, 6.0, 1.0), 1e-6)

        // Empty keyframes fall back to base value
        assertEquals(-100.0, AnimationEvaluator.evaluateProperty(emptyList(), AnimationTargetType.CHARACTER, "char_1", AnimatableProperty.POS_X, 3.2, -100.0), 1e-6)
    }

    // 2. Easing functions (LINEAR, EASE_IN, EASE_OUT, EASE_IN_OUT, STEP)
    @Test
    fun easingCurvesProduceExpectedSmoothShapesAndEndpoints() {
        EasingType.entries.forEach { easing ->
            assertEquals("Start endpoint for $easing", 0.0, easing.apply(0.0), 1e-9)
            assertEquals("End endpoint for $easing", 1.0, easing.apply(1.0), 1e-9)
        }
        // At t = 0.25: EASE_IN < LINEAR < EASE_OUT
        assertEquals(0.25, EasingType.LINEAR.apply(0.25), 1e-9)
        assertEquals(0.0625, EasingType.EASE_IN.apply(0.25), 1e-9)
        assertEquals(0.4375, EasingType.EASE_OUT.apply(0.25), 1e-9)
        assertEquals(0.125, EasingType.EASE_IN_OUT.apply(0.25), 1e-9)
        assertEquals(0.5, EasingType.EASE_IN_OUT.apply(0.5), 1e-9)
        assertEquals(0.875, EasingType.EASE_IN_OUT.apply(0.75), 1e-9)
        assertEquals(0.0, EasingType.STEP.apply(0.99), 1e-9)
        assertEquals(1.0, EasingType.STEP.apply(1.0), 1e-9)

        // Verify easing affects keyframe evaluation between t=0..4
        val easeInKfs = listOf(
            AnimationKeyframe("k1", "CHARACTER", "char_1", "POS_X", 0.0, 0.0, "EASE_IN"),
            AnimationKeyframe("k2", "CHARACTER", "char_1", "POS_X", 4.0, 100.0, "EASE_IN")
        )
        val easeOutKfs = listOf(
            AnimationKeyframe("k1", "CHARACTER", "char_1", "POS_X", 0.0, 0.0, "EASE_OUT"),
            AnimationKeyframe("k2", "CHARACTER", "char_1", "POS_X", 4.0, 100.0, "EASE_OUT")
        )
        val atT1In = AnimationEvaluator.evaluateProperty(easeInKfs, AnimationTargetType.CHARACTER, "char_1", AnimatableProperty.POS_X, 1.0, 0.0)
        val atT1Out = AnimationEvaluator.evaluateProperty(easeOutKfs, AnimationTargetType.CHARACTER, "char_1", AnimatableProperty.POS_X, 1.0, 0.0)
        assertTrue("Ease-In at 25% ($atT1In) must be slower than Ease-Out ($atT1Out)", atT1In < 25.0 && atT1Out > 25.0)
    }

    // 3. Arbitrary fractional timestamp evaluation & all 6 transform properties (posX, posY, scale, rotation, opacity, flipX)
    @Test
    fun evaluatesAllTransformPropertiesAtArbitraryTimestamps() {
        val kfs = listOf(
            AnimationKeyframe("x0", "CHARACTER", "char_1", "POS_X", 0.0, -300.0, "LINEAR"),
            AnimationKeyframe("x1", "CHARACTER", "char_1", "POS_X", 5.0, 200.0, "LINEAR"),
            AnimationKeyframe("y0", "CHARACTER", "char_1", "POS_Y", 0.0, 50.0, "LINEAR"),
            AnimationKeyframe("y1", "CHARACTER", "char_1", "POS_Y", 5.0, -50.0, "LINEAR"),
            AnimationKeyframe("s0", "CHARACTER", "char_1", "SCALE", 0.0, 0.8, "LINEAR"),
            AnimationKeyframe("s1", "CHARACTER", "char_1", "SCALE", 5.0, 1.8, "LINEAR"),
            AnimationKeyframe("r0", "CHARACTER", "char_1", "ROTATION", 0.0, -20.0, "LINEAR"),
            AnimationKeyframe("r1", "CHARACTER", "char_1", "ROTATION", 5.0, 30.0, "LINEAR"),
            AnimationKeyframe("o0", "CHARACTER", "char_1", "OPACITY", 0.0, 0.0, "LINEAR"),
            AnimationKeyframe("o1", "CHARACTER", "char_1", "OPACITY", 2.0, 1.0, "LINEAR"),
            AnimationKeyframe("f0", "CHARACTER", "char_1", "FLIP_X", 0.0, 0.0, "STEP"),
            AnimationKeyframe("f1", "CHARACTER", "char_1", "FLIP_X", 3.0, 1.0, "STEP")
        )
        // Evaluate at arbitrary fractional time t = 1.37s
        val t = 1.37
        val frac5 = t / 5.0
        val eval = AnimationEvaluator.evaluateTransform(
            keyframes = kfs,
            targetType = AnimationTargetType.CHARACTER,
            targetId = "char_1",
            timeSec = t,
            basePosX = 0.0,
            basePosY = 0.0,
            baseScale = 1.0,
            baseRotationDeg = 0.0,
            baseOpacity = 1.0,
            baseFlipX = false
        )
        assertEquals(-300.0 + 500.0 * frac5, eval.posX, 1e-6)
        assertEquals(50.0 - 100.0 * frac5, eval.posY, 1e-6)
        assertEquals(0.8 + 1.0 * frac5, eval.scale, 1e-6)
        assertEquals(-20.0 + 50.0 * frac5, eval.rotationDeg, 1e-6)
        assertEquals(1.37 / 2.0, eval.opacity, 1e-6)
        assertFalse("Before t=3.0, flipX must remain false (discrete step)", eval.flipX)

        // Evaluate after discrete flip at t = 3.25s
        val evalAfterFlip = AnimationEvaluator.evaluateTransform(
            keyframes = kfs,
            targetType = AnimationTargetType.CHARACTER,
            targetId = "char_1",
            timeSec = 3.25,
            basePosX = 0.0,
            basePosY = 0.0,
            baseScale = 1.0,
            baseRotationDeg = 0.0,
            baseOpacity = 1.0,
            baseFlipX = false
        )
        assertTrue("At t=3.25s, flipX must be true", evalAfterFlip.flipX)
        assertEquals(1.0, evalAfterFlip.opacity, 1e-6)
    }

    // 4. Multiple independently animated characters & visual elements simultaneously
    @Test
    fun animatesMultipleCharactersAndVisualElementsIndependently() {
        val c1 = sampleCharacter("char_1", "CHR-01")
        val c2 = sampleCharacter("char_2", "CHR-02")
        val asset = StudioAsset(id = "asset_compass", name = "Compass", category = "Prop", imageBase64 = "BASE64_PROP_PNG")
        val prop = SceneVisualElement(
            elementId = "el_compass",
            name = "Compass",
            assetId = "asset_compass",
            layer = "PROPS",
            posX = 0.0,
            posY = 0.0,
            scale = 1.0,
            rotationDeg = 0.0,
            opacity = 1.0
        )
        val kfs = listOf(
            // Character 1 moves right
            AnimationKeyframe("c1_0", "CHARACTER", "char_1", "POS_X", 0.0, -200.0, "LINEAR"),
            AnimationKeyframe("c1_1", "CHARACTER", "char_1", "POS_X", 4.0, 200.0, "LINEAR"),
            // Character 2 scales up while staying at posX=120
            AnimationKeyframe("c2_0", "CHARACTER", "char_2", "SCALE", 0.0, 0.8, "LINEAR"),
            AnimationKeyframe("c2_1", "CHARACTER", "char_2", "SCALE", 4.0, 1.6, "LINEAR"),
            // Prop rotates 180 degrees and fades in
            AnimationKeyframe("p_r0", "ELEMENT", "el_compass", "ROTATION", 0.0, 0.0, "LINEAR"),
            AnimationKeyframe("p_r1", "ELEMENT", "el_compass", "ROTATION", 4.0, 180.0, "LINEAR"),
            AnimationKeyframe("p_o0", "ELEMENT", "el_compass", "OPACITY", 0.0, 0.2, "LINEAR"),
            AnimationKeyframe("p_o1", "ELEMENT", "el_compass", "OPACITY", 4.0, 1.0, "LINEAR")
        )
        val scene = sampleScene(
            durationSec = 4.0,
            cast = listOf(
                SceneCharacterPlacement(characterId = "char_1", posX = -200.0),
                SceneCharacterPlacement(characterId = "char_2", posX = 120.0, scale = 0.8)
            ),
            elements = listOf(prop),
            keyframes = kfs
        )

        val plan = SceneCompositor.buildCompositionPlan(
            scene = scene,
            characters = listOf(c1, c2),
            assets = listOf(asset),
            mutedTracks = emptySet(),
            timeSec = 2.0
        )
        val nodes = plan.flattenedVisibleNodes()
        val char1Node = nodes.filterIsInstance<CompositeRenderNode.CharacterNode>().first { it.character.id == "char_1" }
        val char2Node = nodes.filterIsInstance<CompositeRenderNode.CharacterNode>().first { it.character.id == "char_2" }
        val propNode = nodes.filterIsInstance<CompositeRenderNode.VisualElementNode>().first { it.element.elementId == "el_compass" }

        assertEquals(0.0, char1Node.placement.posX, 1e-6)
        assertEquals(120.0, char2Node.placement.posX, 1e-6)
        assertEquals(1.2, char2Node.placement.scale, 1e-6)
        assertEquals(90.0, propNode.element.rotationDeg, 1e-6)
        assertEquals(0.6f, propNode.opacity, 1e-4f)
    }

    // 5. Character Acting Cues & deterministic reference sheet switching + fallback
    @Test
    fun characterActingCuesSwitchReferenceSheetsDeterministicallyAndFallbackCleanly() {
        val c1 = sampleCharacter("char_1", "CHR-01", primaryImg = "PRIMARY_IMG_DATA")
        val refDetermined = CharacterReferenceSheet(
            id = "sheet_det",
            viewType = "Front View",
            expression = "Determined",
            poseAction = "Pointing",
            imageBase64 = "SHEET_DETERMINED_IMG"
        )
        val refWorried = CharacterReferenceSheet(
            id = "sheet_worried",
            viewType = "Side View",
            expression = "Worried",
            poseAction = "Thinking",
            imageBase64 = "SHEET_WORRIED_IMG"
        )
        val cues = listOf(
            CharacterActingCue("cue_1", "char_1", 2.0, "Determined", "Pointing", "Front View", ""),
            CharacterActingCue("cue_2", "char_1", 5.0, "Worried", "Thinking", "Side View", ""),
            CharacterActingCue("cue_3", "char_1", 7.0, "Surprised", "Reacting", "Portrait", "")
        )
        val scene = sampleScene(durationSec = 8.0, actingCues = cues)
        val refMap = mapOf("char_1" to listOf(refDetermined, refWorried))

        // At t=1.0 (before first cue): base placement (Calm, Standing, Three-Quarter View) -> no matching sheet -> primary fallback
        val planT1 = SceneCompositor.buildCompositionPlan(scene, listOf(c1), emptyList(), emptySet(), timeSec = 1.0, characterReferencesByCharId = refMap)
        val nodeT1 = planT1.flattenedVisibleNodes().filterIsInstance<CompositeRenderNode.CharacterNode>().first()
        assertEquals("Calm", nodeT1.activeExpression)
        assertEquals("PRIMARY_IMG_DATA", nodeT1.resolvedImageBase64)
        assertNull(nodeT1.matchedReferenceSheetId)

        // At t=3.5 (after cue_1): switches to refDetermined
        val planT35 = SceneCompositor.buildCompositionPlan(scene, listOf(c1), emptyList(), emptySet(), timeSec = 3.5, characterReferencesByCharId = refMap)
        val nodeT35 = planT35.flattenedVisibleNodes().filterIsInstance<CompositeRenderNode.CharacterNode>().first()
        assertEquals("Determined", nodeT35.activeExpression)
        assertEquals("Pointing", nodeT35.activeAction)
        assertEquals("SHEET_DETERMINED_IMG", nodeT35.resolvedImageBase64)
        assertEquals("sheet_det", nodeT35.matchedReferenceSheetId)

        // At t=5.5 (after cue_2): switches to refWorried
        val planT55 = SceneCompositor.buildCompositionPlan(scene, listOf(c1), emptyList(), emptySet(), timeSec = 5.5, characterReferencesByCharId = refMap)
        val nodeT55 = planT55.flattenedVisibleNodes().filterIsInstance<CompositeRenderNode.CharacterNode>().first()
        assertEquals("Worried", nodeT55.activeExpression)
        assertEquals("SHEET_WORRIED_IMG", nodeT55.resolvedImageBase64)
        assertEquals("sheet_worried", nodeT55.matchedReferenceSheetId)

        // At t=7.5 (cue_3 has no matching sheet): updates acting metadata cleanly and falls back to primary portrait
        val planT75 = SceneCompositor.buildCompositionPlan(scene, listOf(c1), emptyList(), emptySet(), timeSec = 7.5, characterReferencesByCharId = refMap)
        val nodeT75 = planT75.flattenedVisibleNodes().filterIsInstance<CompositeRenderNode.CharacterNode>().first()
        assertEquals("Surprised", nodeT75.activeExpression)
        assertEquals("Reacting", nodeT75.activeAction)
        assertEquals("PRIMARY_IMG_DATA", nodeT75.resolvedImageBase64)
        assertNull(nodeT75.matchedReferenceSheetId)
    }

    // 6. Camera animation keyframes + Phase 1 preset compatibility when unkeyframed
    @Test
    fun cameraKeyframesInterpolateSmoothlyAndFallbackToPresetsWhenAbsent() {
        val camKfs = listOf(
            AnimationKeyframe("cz0", "CAMERA", AnimationEvaluator.CAMERA_TARGET_ID, "CAMERA_ZOOM", 0.0, 1.0, "LINEAR"),
            AnimationKeyframe("cz1", "CAMERA", AnimationEvaluator.CAMERA_TARGET_ID, "CAMERA_ZOOM", 4.0, 1.6, "LINEAR"),
            AnimationKeyframe("cx0", "CAMERA", AnimationEvaluator.CAMERA_TARGET_ID, "CAMERA_PAN_X", 0.0, -80.0, "LINEAR"),
            AnimationKeyframe("cx1", "CAMERA", AnimationEvaluator.CAMERA_TARGET_ID, "CAMERA_PAN_X", 4.0, 80.0, "LINEAR"),
            AnimationKeyframe("cy0", "CAMERA", AnimationEvaluator.CAMERA_TARGET_ID, "CAMERA_PAN_Y", 0.0, -40.0, "LINEAR"),
            AnimationKeyframe("cy1", "CAMERA", AnimationEvaluator.CAMERA_TARGET_ID, "CAMERA_PAN_Y", 4.0, 40.0, "LINEAR")
        )
        val keyframedScene = sampleScene(durationSec = 4.0, keyframes = camKfs)
        val camMid = AnimationEvaluator.evaluateCamera(keyframedScene, 2.0, cameraMuted = false)
        assertTrue(camMid.hasCameraKeyframes)
        assertEquals(1.3, camMid.zoom, 1e-6)
        assertEquals(0.0, camMid.panX, 1e-6)
        assertEquals(0.0, camMid.panY, 1e-6)

        val camQuarter = AnimationEvaluator.evaluateCamera(keyframedScene, 1.0, cameraMuted = false)
        assertEquals(1.15, camQuarter.zoom, 1e-6)
        assertEquals(-40.0, camQuarter.panX, 1e-6)
        assertEquals(-20.0, camQuarter.panY, 1e-6)

        // Muted camera track returns neutral camera
        val camMuted = AnimationEvaluator.evaluateCamera(keyframedScene, 2.0, cameraMuted = true)
        assertEquals(1.0, camMuted.zoom, 1e-6)
        assertEquals(0.0, camMuted.panX, 1e-6)

        // Unkeyframed scene preserves Phase 1 Slow Push In preset
        val presetScene = sampleScene(durationSec = 4.0, keyframes = emptyList())
        val presetMid = AnimationEvaluator.evaluateCamera(presetScene, 2.0, cameraMuted = false)
        assertFalse(presetMid.hasCameraKeyframes)
        assertEquals(1.1, presetMid.zoom, 1e-6)
    }

    // 7. Seeking, deterministic reconstruction, and non-mutation of base transforms
    @Test
    fun seekingProducesIdenticalDeterministicFramesWithoutMutatingBaseValues() {
        val c1 = sampleCharacter("char_1", "CHR-01")
        val kfs = AnimationEvaluator.applyCharacterTransformPreset(
            existing = emptyList(),
            placement = SceneCharacterPlacement(characterId = "char_1", posX = -50.0, posY = 12.0),
            sceneDurationSec = 6.0,
            presetKey = "walk_across_ltr"
        )
        val scene = sampleScene(
            durationSec = 6.0,
            cast = listOf(SceneCharacterPlacement(characterId = "char_1", posX = -50.0, posY = 12.0)),
            keyframes = kfs
        )

        val directAt25 = AnimationEvaluator.evaluateSceneAtTime(scene, listOf(c1), emptyMap(), timeSec = 2.5)
        // Simulate scrubbing forward to 5.8s, backward to 0.1s, then back to 2.5s
        AnimationEvaluator.evaluateSceneAtTime(scene, listOf(c1), emptyMap(), timeSec = 5.8)
        AnimationEvaluator.evaluateSceneAtTime(scene, listOf(c1), emptyMap(), timeSec = 0.1)
        val afterSeekAt25 = AnimationEvaluator.evaluateSceneAtTime(scene, listOf(c1), emptyMap(), timeSec = 2.5)

        assertEquals(directAt25, afterSeekAt25)
        // Verify base scene placement was NEVER mutated
        assertEquals(-50.0, scene.characterCast.first().posX, 1e-9)
        assertEquals(12.0, scene.characterCast.first().posY, 1e-9)
        assertNotEquals(-50.0, directAt25.characterTransforms["char_1"]?.posX)
    }

    // 8. Serialization & deserialization round-trip + security bounds clamping
    @Test
    fun animationDataSerializesAndDeserializesCleanlyWithBounds() {
        val kfs = listOf(
            AnimationKeyframe("kf_1", "CHARACTER", "char_1", "POS_X", 1.25, -180.0, "EASE_IN_OUT"),
            AnimationKeyframe("kf_2", "CAMERA", AnimationEvaluator.CAMERA_TARGET_ID, "CAMERA_ZOOM", 4.5, 1.45, "EASE_OUT")
        )
        val cues = listOf(
            CharacterActingCue("cue_1", "char_1", 2.5, "Determined", "Pointing", "Front View", "sheet_1")
        )

        val serializedKfs = StudioRepository.animationKeyframesToMaps(kfs)
        val restoredKfs = StudioRepository.mapsToAnimationKeyframes(serializedKfs)
        assertEquals(kfs, restoredKfs)

        val serializedCues = StudioRepository.actingCuesToMaps(cues)
        val restoredCues = StudioRepository.mapsToActingCues(serializedCues)
        assertEquals(cues, restoredCues)

        // Bounds check: >200 keyframes capped to 200, >60 cues capped to 60
        val oversizedKfs = (0 until 250).map { i ->
            AnimationKeyframe("kf_$i", "CHARACTER", "char_1", "POS_X", i * 0.01, 0.0, "LINEAR")
        }
        assertEquals(AnimationEvaluator.MAX_KEYFRAMES_PER_SCENE, StudioRepository.animationKeyframesToMaps(oversizedKfs).size)

        val oversizedCues = (0 until 80).map { i ->
            CharacterActingCue("cue_$i", "char_1", i * 0.1, "Calm", "Standing", "Front View", "")
        }
        assertEquals(AnimationEvaluator.MAX_ACTING_CUES_PER_SCENE, StudioRepository.actingCuesToMaps(oversizedCues).size)
    }

    // 9. Undo/Redo snapshot preservation & keyframe authoring helpers (upsert/delete/jump)
    @Test
    fun historySnapshotsAndKeyframeAuthoringHelpersWorkCleanly() {
        val baseScene = sampleScene(durationSec = 8.0)
        val snapshotBefore = StudioHistorySnapshot(
            project = StudioProject(id = "p1", name = "Doc"),
            scenes = listOf(baseScene),
            selectedSceneId = baseScene.id
        )

        var kfs = AnimationEvaluator.upsertKeyframe(
            existing = baseScene.animationKeyframes,
            targetType = AnimationTargetType.CHARACTER,
            targetId = "char_1",
            property = AnimatableProperty.POS_X,
            timeSec = 1.0,
            value = -120.0,
            easing = EasingType.EASE_IN_OUT,
            sceneDurationSec = 8.0
        )
        kfs = AnimationEvaluator.upsertKeyframe(
            existing = kfs,
            targetType = AnimationTargetType.CHARACTER,
            targetId = "char_1",
            property = AnimatableProperty.POS_X,
            timeSec = 5.0,
            value = 180.0,
            easing = EasingType.EASE_IN_OUT,
            sceneDurationSec = 8.0
        )
        // Updating within snap epsilon updates existing keyframe rather than duplicating
        kfs = AnimationEvaluator.upsertKeyframe(
            existing = kfs,
            targetType = AnimationTargetType.CHARACTER,
            targetId = "char_1",
            property = AnimatableProperty.POS_X,
            timeSec = 1.02,
            value = -150.0,
            easing = EasingType.LINEAR,
            sceneDurationSec = 8.0
        )
        assertEquals(2, kfs.size)
        assertEquals(-150.0, kfs.first().value, 1e-6)
        assertEquals("LINEAR", kfs.first().easing)

        // Prev / Next timestamp navigation
        val times = AnimationEvaluator.distinctTimesForTarget(kfs, emptyList(), AnimationTargetType.CHARACTER, "char_1")
        assertEquals(listOf(1.02, 5.0), times)
        assertEquals(1.02, AnimationEvaluator.findPreviousTimestamp(times, 3.0)!!, 1e-6)
        assertEquals(5.0, AnimationEvaluator.findNextTimestamp(times, 3.0)!!, 1e-6)

        // Undo snapshot remains untouched by subsequent animation edits
        val editedScene = baseScene.copy(animationKeyframes = kfs)
        assertEquals(0, snapshotBefore.scenes.first().animationKeyframes.size)
        assertEquals(2, editedScene.animationKeyframes.size)
    }

    // 10. Phase 1 Compositor Backwards Compatibility (scenes without animation data behave identically)
    @Test
    fun unkeyframedScenesRemainFullyCompatibleWithPhase1Compositor() {
        val c1 = sampleCharacter("char_1", "CHR-01")
        val scene = sampleScene(durationSec = 6.0, keyframes = emptyList(), actingCues = emptyList())

        val legacyPlan = SceneCompositor.buildCompositionPlan(scene, listOf(c1), emptyList(), emptySet())
        val timedPlan = SceneCompositor.buildCompositionPlan(scene, listOf(c1), emptyList(), emptySet(), timeSec = 3.0)

        val legacyChar = legacyPlan.flattenedVisibleNodes().filterIsInstance<CompositeRenderNode.CharacterNode>().first()
        val timedChar = timedPlan.flattenedVisibleNodes().filterIsInstance<CompositeRenderNode.CharacterNode>().first()

        assertEquals(legacyChar.placement.posX, timedChar.placement.posX, 1e-6)
        assertEquals(legacyChar.placement.posY, timedChar.placement.posY, 1e-6)
        assertEquals(legacyChar.placement.scale, timedChar.placement.scale, 1e-6)
        assertEquals(legacyChar.placement.rotationDeg, timedChar.placement.rotationDeg, 1e-6)
        assertEquals(legacyChar.opacity, timedChar.opacity, 1e-6f)
        assertEquals(legacyChar.resolvedImageBase64, timedChar.resolvedImageBase64)
    }
}
