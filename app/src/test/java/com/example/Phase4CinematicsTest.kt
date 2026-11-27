package com.example

import com.example.animation.AnimatableProperty
import com.example.animation.AnimationEvaluator
import com.example.animation.AnimationTargetType
import com.example.animation.EasingType
import com.example.cinematics.AtmosphereMode
import com.example.cinematics.CinematicCameraPreset
import com.example.cinematics.CinematicEngine
import com.example.cinematics.LetterboxMode
import com.example.cinematics.LightingPreset
import com.example.compositor.SceneCompositor
import com.example.compositor.SceneLayerType
import com.example.data.SceneCinematics
import com.example.data.StudioHistorySnapshot
import com.example.data.StudioProject
import com.example.data.StudioRepository
import com.example.data.StudioScene
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class Phase4CinematicsTest {

    private fun sampleScene(durationSec: Double = 8.0): StudioScene {
        return StudioScene(
            id = "sc_phase4_test",
            title = "Himalayan Archival Sequence",
            durationSec = durationSec,
            cameraMovement = "Static Lock",
            cameraZoom = 1.0
        )
    }

    @Test
    fun allElevenCameraPresetsApplyDistinctFramingOrEditableKeyframes() {
        val base = sampleScene(8.0)

        CinematicCameraPreset.entries.forEach { preset ->
            val updated = CinematicEngine.applyCameraPreset(base, preset)
            assertEquals(preset.label, updated.cinematics.cameraPreset)

            val camStart = AnimationEvaluator.evaluateCamera(updated, 0.0, cameraMuted = false)
            val camEnd = AnimationEvaluator.evaluateCamera(updated, 8.0, cameraMuted = false)

            if (preset.isAnimatedMotion) {
                assertTrue(
                    "Animated preset ${preset.label} must author real camera keyframes",
                    updated.animationKeyframes.any {
                        it.targetType == AnimationTargetType.CAMERA.name
                    }
                )
                assertTrue("Animated preset ${preset.label} must evaluate as keyframed", camStart.hasCameraKeyframes)
                val changedOverTime =
                    abs(camEnd.zoom - camStart.zoom) > 0.01 ||
                        abs(camEnd.panX - camStart.panX) > 1.0 ||
                        abs(camEnd.panY - camStart.panY) > 1.0
                assertTrue("Animated preset ${preset.label} must move over time", changedOverTime)
            } else {
                assertFalse("Static preset ${preset.label} should not leave stale keyframes", camStart.hasCameraKeyframes)
                assertEquals(camStart.zoom, camEnd.zoom, 0.0001)
                assertEquals(camStart.panX, camEnd.panX, 0.0001)
                assertEquals(camStart.panY, camEnd.panY, 0.0001)
                assertEquals(camStart.rotationDeg, camEnd.rotationDeg, 0.0001)
            }
        }

        // Verify specific framing properties
        val closeUp = CinematicEngine.applyCameraPreset(base, CinematicCameraPreset.CLOSE_UP)
        assertEquals(1.55, AnimationEvaluator.evaluateCamera(closeUp, 2.0, false).zoom, 0.001)

        val overhead = CinematicEngine.applyCameraPreset(base, CinematicCameraPreset.OVERHEAD)
        assertEquals(-6.0, AnimationEvaluator.evaluateCamera(overhead, 2.0, false).rotationDeg, 0.001)

        val reset = CinematicEngine.resetCameraToFit(
            CinematicEngine.applyCameraPreset(base, CinematicCameraPreset.DRAMATIC_ZOOM)
        )
        val resetCam = AnimationEvaluator.evaluateCamera(reset, 4.0, false)
        assertEquals(1.0, resetCam.zoom, 0.001)
        assertEquals(0.0, resetCam.panX, 0.001)
        assertEquals(0.0, resetCam.panY, 0.001)
        assertEquals(0.0, resetCam.rotationDeg, 0.001)
        assertTrue(reset.animationKeyframes.isEmpty())
    }

    @Test
    fun cameraRotationKeyframesAndDeterministicHandheldShakeEvaluateAccurately() {
        val cid = AnimationEvaluator.CAMERA_TARGET_ID
        var kfs = AnimationEvaluator.upsertKeyframe(
            emptyList(), AnimationTargetType.CAMERA, cid,
            AnimatableProperty.CAMERA_ROTATION, 0.0, -10.0, EasingType.LINEAR, 8.0
        )
        kfs = AnimationEvaluator.upsertKeyframe(
            kfs, AnimationTargetType.CAMERA, cid,
            AnimatableProperty.CAMERA_ROTATION, 8.0, 10.0, EasingType.LINEAR, 8.0
        )
        val scene = sampleScene(8.0).copy(
            animationKeyframes = kfs,
            cinematics = SceneCinematics(cameraShakeIntensity = 0.0)
        )

        assertEquals(-10.0, AnimationEvaluator.evaluateCamera(scene, 0.0, false).rotationDeg, 0.001)
        assertEquals(0.0, AnimationEvaluator.evaluateCamera(scene, 4.0, false).rotationDeg, 0.001)
        assertEquals(10.0, AnimationEvaluator.evaluateCamera(scene, 8.0, false).rotationDeg, 0.001)

        // Handheld shake is deterministic at identical timestamps and varies smoothly over time
        val shakeA1 = CinematicEngine.computeHandheldShake(2.35, 0.65)
        val shakeA2 = CinematicEngine.computeHandheldShake(2.35, 0.65)
        val shakeB = CinematicEngine.computeHandheldShake(3.10, 0.65)
        assertEquals(shakeA1, shakeA2)
        assertNotEquals(shakeA1.offsetX, shakeB.offsetX, 0.001)

        val zeroShake = CinematicEngine.computeHandheldShake(2.35, 0.0)
        assertEquals(0.0, zeroShake.offsetX, 0.0001)
        assertEquals(0.0, zeroShake.offsetY, 0.0001)
        assertEquals(0.0, zeroShake.rotationDeg, 0.0001)
    }

    @Test
    fun multiplaneLayerDepthParallaxDoFAndHazeDifferentiateLayers() {
        val cinematics = SceneCinematics(
            parallaxEnabled = true,
            parallaxStrength = 1.0,
            depthFocusPlane = 0.85,
            depthBlurIntensity = 0.8,
            depthHazeIntensity = 0.7
        )

        val bgDepth = CinematicEngine.computeLayerDepthTransform(
            layerParallaxFactor = SceneLayerType.BACKGROUND.defaultParallaxFactor, // 0.25
            camZoom = 1.4,
            camPanX = 100.0,
            camPanY = -40.0,
            cinematics = cinematics
        )
        val charDepth = CinematicEngine.computeLayerDepthTransform(
            layerParallaxFactor = SceneLayerType.CHARACTERS.defaultParallaxFactor, // 0.85 (on focus plane)
            camZoom = 1.4,
            camPanX = 100.0,
            camPanY = -40.0,
            cinematics = cinematics
        )
        val fgDepth = CinematicEngine.computeLayerDepthTransform(
            layerParallaxFactor = SceneLayerType.FOREGROUND.defaultParallaxFactor, // 1.15
            camZoom = 1.4,
            camPanX = 100.0,
            camPanY = -40.0,
            cinematics = cinematics
        )

        // Foreground pans faster than characters, which pan faster than background
        assertTrue(bgDepth.panX < charDepth.panX)
        assertTrue(charDepth.panX < fgDepth.panX)
        assertTrue(bgDepth.zoomScale < charDepth.zoomScale)
        assertTrue(charDepth.zoomScale < fgDepth.zoomScale)

        // Focus plane (0.85) stays crisp; distant background receives blur + aerial depth haze
        assertEquals(0.0, charDepth.blurRadiusDp, 0.001)
        assertEquals(0.0, charDepth.depthHazeAlpha, 0.001)
        assertTrue(bgDepth.blurRadiusDp > 3.0)
        assertTrue(bgDepth.depthHazeAlpha > 0.1)
        assertEquals(0.0, fgDepth.depthHazeAlpha, 0.001) // Foreground closer than focus plane gets no haze

        // Disabling parallax locks all layers to uniform 1.0x camera pan
        val lockedCinematics = cinematics.copy(parallaxEnabled = false)
        val bgLocked = CinematicEngine.computeLayerDepthTransform(0.25, 1.4, 100.0, -40.0, lockedCinematics)
        val fgLocked = CinematicEngine.computeLayerDepthTransform(1.15, 1.4, 100.0, -40.0, lockedCinematics)
        assertEquals(bgLocked.panX, fgLocked.panX, 0.001)
        assertEquals(bgLocked.zoomScale, fgLocked.zoomScale, 0.001)

        // Compositor plan includes non-null depthTransform for non-overlay layers and null for OVERLAY
        val updatedLayers = SceneCompositor.updateLayerParallax(emptyList(), SceneLayerType.MIDGROUND, 0.68)
        assertEquals(0.68, updatedLayers.first { it.layerKey == SceneLayerType.MIDGROUND.name }.parallaxFactor, 0.001)
        val plan = SceneCompositor.buildCompositionPlan(
            scene = sampleScene().copy(layerStates = updatedLayers, cinematics = cinematics),
            characters = emptyList(),
            assets = emptyList(),
            mutedTracks = emptySet(),
            timeSec = 2.0
        )
        assertNotNull(plan.orderedLayers.first { it.layerType == SceneLayerType.BACKGROUND }.depthTransform)
        assertEquals(null, plan.orderedLayers.first { it.layerType == SceneLayerType.OVERLAY }.depthTransform)
    }

    @Test
    fun lightingPresetsAndColorGradingMatrixProduceValidTransformations() {
        val base = sampleScene()
        assertTrue(CinematicEngine.isNeutralColorGrade(base.cinematics))

        val neutralMatrix = CinematicEngine.computeColorGradeMatrix(SceneCinematics())
        assertEquals(20, neutralMatrix.size)
        assertEquals(1f, neutralMatrix[0], 0.001f)
        assertEquals(1f, neutralMatrix[6], 0.001f)
        assertEquals(1f, neutralMatrix[12], 0.001f)
        assertEquals(1f, neutralMatrix[18], 0.001f)

        // Warm Golden Hour increases Red relative to Blue
        val golden = CinematicEngine.applyLightingPreset(base, LightingPreset.GOLDEN_HOUR)
        assertFalse(CinematicEngine.isNeutralColorGrade(golden.cinematics))
        val goldenMat = CinematicEngine.computeColorGradeMatrix(golden.cinematics)
        assertTrue("Warm grade should boost red gain over blue", goldenMat[0] > goldenMat[12])

        // Cool Blue Hour increases Blue relative to Red
        val blue = CinematicEngine.applyLightingPreset(base, LightingPreset.BLUE_HOUR)
        val blueMat = CinematicEngine.computeColorGradeMatrix(blue.cinematics)
        assertTrue("Cool grade should boost blue gain over red", blueMat[12] > blueMat[0])

        // Noir High-Contrast lowers saturation dramatically and increases contrast
        val noir = CinematicEngine.applyLightingPreset(base, LightingPreset.NOIR)
        assertTrue(noir.cinematics.contrast > 1.3)
        assertTrue(noir.cinematics.saturation < 0.3)

        // All 8 lighting presets apply cleanly
        assertEquals(8, LightingPreset.entries.size)
        LightingPreset.entries.forEach { preset ->
            val applied = CinematicEngine.applyLightingPreset(base, preset)
            assertEquals(preset.label, applied.cinematics.lightingPreset)
            assertEquals(20, CinematicEngine.computeColorGradeMatrix(applied.cinematics).size)
        }
    }

    @Test
    fun atmosphereParticlesAreDeterministicBoundedAndRespondToTimeAndWind() {
        assertTrue(
            CinematicEngine.computeAtmosphereParticles(AtmosphereMode.NONE, 2.0, 0.8, 1.0, 0.0).isEmpty()
        )

        AtmosphereMode.entries.filter { it != AtmosphereMode.NONE }.forEach { mode ->
            val p1 = CinematicEngine.computeAtmosphereParticles(mode, 1.5, 1.0, 1.0, 0.4)
            val p2 = CinematicEngine.computeAtmosphereParticles(mode, 1.5, 1.0, 1.0, 0.4)
            val pLater = CinematicEngine.computeAtmosphereParticles(mode, 3.25, 1.0, 1.0, 0.4)

            assertTrue("${mode.label} must emit particles", p1.isNotEmpty())
            assertTrue(
                "${mode.label} count ${p1.size} must stay <= ${CinematicEngine.MAX_ATMOSPHERE_PARTICLES}",
                p1.size <= CinematicEngine.MAX_ATMOSPHERE_PARTICLES
            )
            assertEquals("${mode.label} must be 100% deterministic for identical timestamps", p1, p2)
            assertNotEquals("${mode.label} must evolve over time", p1, pLater)
            p1.forEach { pt ->
                assertTrue(pt.normX in 0.0..1.0)
                assertTrue(pt.normY in 0.0..1.0)
                assertTrue(pt.alpha in 0.0..1.0)
            }
        }

        // Rain angle responds to wind polarity
        val rainRight = CinematicEngine.computeAtmosphereParticles(AtmosphereMode.RAIN, 1.0, 0.6, 1.0, 0.8)
        val rainLeft = CinematicEngine.computeAtmosphereParticles(AtmosphereMode.RAIN, 1.0, 0.6, 1.0, -0.8)
        assertTrue(rainRight.first().angleDeg > 0.0)
        assertTrue(rainLeft.first().angleDeg < 0.0)
    }

    @Test
    fun postProcessingVfxMergesPhase4AndLegacyEffectsAndComputesLetterboxBars() {
        val legacyScene = sampleScene().copy(
            effectPreset = "Cinematic Vignette",
            effectIntensity = 0.8,
            cinematics = SceneCinematics(
                vignetteIntensity = 0.1,
                filmGrainIntensity = 0.45,
                bloomIntensity = 0.30,
                letterboxAspect = LetterboxMode.ANAMORPHIC_239.label
            )
        )
        val vfx = CinematicEngine.resolveEffectiveVfx(legacyScene)
        // Legacy vignette (0.8 * 0.75 = 0.60) overrides smaller Phase 4 value (0.10)
        assertEquals(0.60, vfx.vignetteIntensity, 0.001)
        assertEquals(0.45, vfx.filmGrainIntensity, 0.001)
        assertEquals(0.30, vfx.bloomIntensity, 0.001)
        assertEquals(LetterboxMode.ANAMORPHIC_239, vfx.letterboxMode)

        assertEquals(0.0, LetterboxMode.NONE.barHeightFraction(), 0.0001)
        assertTrue(LetterboxMode.CINEMA_185.barHeightFraction() > 0.015)
        assertTrue(LetterboxMode.ANAMORPHIC_239.barHeightFraction() > LetterboxMode.CINEMA_185.barHeightFraction())
        assertTrue(LetterboxMode.ANAMORPHIC_239.barHeightFraction() < 0.18)
    }

    @Test
    fun sceneCinematicsFirestoreSerializationRoundTripsAndClampsOutOfBoundsValues() {
        val original = SceneCinematics(
            cameraPreset = CinematicCameraPreset.OVERHEAD.label,
            cameraPosX = 42.5,
            cameraPosY = -28.0,
            cameraRotationDeg = -6.0,
            parallaxEnabled = true,
            parallaxStrength = 1.35,
            depthFocusPlane = 0.80,
            depthBlurIntensity = 0.45,
            depthHazeIntensity = 0.32,
            lightingPreset = LightingPreset.GOLDEN_HOUR.label,
            exposure = 0.12,
            contrast = 1.15,
            saturation = 1.22,
            temperature = 0.55,
            lightSourceX = 0.7,
            lightSourceY = -0.5,
            lightGlowIntensity = 0.44,
            shadowIntensity = 0.30,
            atmosphereMode = AtmosphereMode.EMBERS.label,
            atmosphereIntensity = 0.65,
            atmosphereSpeed = 1.4,
            windStrength = -0.35,
            vignetteIntensity = 0.50,
            filmGrainIntensity = 0.25,
            bloomIntensity = 0.35,
            cameraShakeIntensity = 0.22,
            letterboxAspect = LetterboxMode.CINEMA_185.label
        )

        val serialized = StudioRepository.cinematicsToMap(original)
        assertTrue("Serialized map must stay within Firestore 32-key bound", serialized.size <= 32)
        val restored = StudioRepository.mapToCinematics(serialized)
        assertEquals(original, restored)

        // Malformed / out-of-range values clamp safely without NaN/Infinity
        val corruptMap = mapOf<String, Any>(
            "cameraPosX" to Double.NaN,
            "cameraRotationDeg" to 999.0,
            "contrast" to -5.0,
            "vignetteIntensity" to 4.2,
            "parallaxEnabled" to false
        )
        val safeRestored = StudioRepository.mapToCinematics(corruptMap)
        assertEquals(0.0, safeRestored.cameraPosX, 0.001)
        assertEquals(45.0, safeRestored.cameraRotationDeg, 0.001)
        assertEquals(0.6, safeRestored.contrast, 0.001)
        assertEquals(1.0, safeRestored.vignetteIntensity, 0.001)
        assertFalse(safeRestored.parallaxEnabled)
    }

    @Test
    fun undoRedoHistorySnapshotsPreserveCinematicsAndLegacyScenesLoadDefaults() {
        val before = sampleScene()
        val after = CinematicEngine.applyLightingPreset(
            CinematicEngine.applyCameraPreset(before, CinematicCameraPreset.DRAMATIC_ZOOM),
            LightingPreset.CANDLELIGHT
        )
        val snapshot = StudioHistorySnapshot(
            project = StudioProject(id = "proj_1", name = "Doc"),
            scenes = listOf(before),
            selectedSceneId = before.id
        )
        assertEquals("Natural Documentary", snapshot.scenes.first().cinematics.lightingPreset)
        assertEquals("Warm Candlelight", after.cinematics.lightingPreset)
        assertEquals("Dramatic Zoom", after.cinematics.cameraPreset)

        // Empty legacy map deserializes to safe neutral defaults
        val legacyCinematics = StudioRepository.mapToCinematics(emptyMap())
        assertEquals(SceneCinematics(), legacyCinematics)
        assertTrue(CinematicEngine.isNeutralColorGrade(legacyCinematics))
    }
}
