package com.example

import com.example.audio.AudioTimelineEngine
import com.example.audio.AudioValidationResult
import com.example.audio.LipSyncEngine
import com.example.audio.MouthViseme
import com.example.data.AiSoundSuggestion
import com.example.data.CharacterActingCue
import com.example.data.DialogueLine
import com.example.data.SceneAudioClip
import com.example.data.SceneCharacterPlacement
import com.example.data.ScenePlanMetadata
import com.example.data.ScriptAiService
import com.example.data.StoryAnalysisSummary
import com.example.data.StudioAudioAsset
import com.example.data.StudioRepository
import com.example.data.StudioScene
import com.example.data.TimelineTrackType
import com.example.data.VisemeCue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase5StoryAudioLipSyncTest {

    // -------------------------------------------------------------------------
    // 1. AI Story Intelligence JSON Parsing (English & Hindi Devanagari)
    // -------------------------------------------------------------------------

    @Test
    fun parseBreakdownJson_parsesEnglishStoryAnalysisScenesDialogueAndSounds() {
        val json = """
        {
          "storyAnalysis": {
            "detectedLanguage": "English",
            "storyLogline": "An archival expedition traces a lost mountain trade pass.",
            "eraOrPeriod": "1882 Himalayan Frontier",
            "emotionalArc": "Curiosity to quiet awe",
            "locations": ["Mountain Pass", "Archive Study"],
            "characterRelationships": ["Dr. Meera mentors Arun"],
            "narrativeBeats": ["Discovery of journal", "Crossing the ridge"]
          },
          "identifiedCharacters": [
            {
              "existingCharCode": "CHR-01",
              "name": "Dr. Meera Sen",
              "role": "Historian",
              "visualAge": "Late 30s",
              "faceDescription": "Warm expressive eyes",
              "hairStyle": "Low bun",
              "hairColor": "Black",
              "clothing": "Olive field jacket",
              "clothingColors": "Olive and cream",
              "bodyProportions": "Average build",
              "accessories": "Brass compass",
              "personality": "Analytical and empathetic",
              "defaultEmotion": "Focused",
              "visualStyle": "Painterly 2D Documentary",
              "consistencyNotes": "Keep scarf and brass compass consistent.",
              "recurring": true
            }
          ],
          "scenes": [
            {
              "title": "The Hidden Ledger",
              "description": "Meera unfolds a weathered map on a candlelit desk.",
              "visualSummary": "Warm lamp illuminates antique cartography.",
              "narrationText": "For forty years, the winter trade pass remained unmarked on imperial maps.",
              "subtitleText": "The winter trade pass remained unmarked for forty years.",
              "durationSec": 9.5,
              "backgroundPreset": "archive_study",
              "cameraMovement": "Slow Push In",
              "animationStyle": "Subtle Drift",
              "effectPreset": "Film Grain",
              "motionGraphicTitle": "ARCHIVE · 1882",
              "motionGraphicStyle": "Lower Third",
              "musicCue": "Minimal Piano Reflection",
              "sfxCue": "Soft Paper Rustle",
              "objectProp": "Folded Map",
              "suggestedCharacterCode": "CHR-01",
              "location": "Archive Study",
              "environment": "Wood-paneled study lined with ledgers",
              "timeOfDay": "Night",
              "weather": "Clear",
              "emotionalTone": "Contemplative",
              "visualDirection": "Low-key lantern glow from screen right.",
              "cameraSuggestion": "Slow Push In",
              "lightingSuggestion": "Candlelight Interior",
              "atmosphereSuggestion": "Dust Motes",
              "vfxSuggestion": "Subtle Film Grain",
              "transitionSuggestion": "Cross Dissolve",
              "audioCuesSummary": "Soft paper rustle over sparse piano",
              "suggestedProps": ["Folded Map", "Brass Lantern"],
              "cast": [
                {
                  "characterName": "Dr. Meera Sen",
                  "existingCharCode": "CHR-01",
                  "expression": "Focused",
                  "action": "Inspecting",
                  "poseView": "Three-Quarter View",
                  "stagePosition": "Center"
                }
              ],
              "dialogueLines": [
                {
                  "speakerName": "Dr. Meera Sen",
                  "existingCharCode": "CHR-01",
                  "text": "Look closely at the northern ridge line.",
                  "emotion": "Focused",
                  "startSec": 1.2,
                  "durationSec": 3.2
                }
              ],
              "soundSuggestions": [
                {
                  "category": "SFX",
                  "title": "Parchment Paper Rustle",
                  "description": "Gentle unfolding of aged ledger paper",
                  "suggestedStartSec": 0.5,
                  "suggestedVolume": 0.65,
                  "loopEnabled": false
                },
                {
                  "category": "AMBIENT",
                  "title": "Quiet Library Room Tone",
                  "description": "Soft nocturnal stillness with distant clock tick",
                  "suggestedStartSec": 0.0,
                  "suggestedVolume": 0.35,
                  "loopEnabled": true
                }
              ]
            }
          ]
        }
        """.trimIndent()

        val result = ScriptAiService.parseBreakdownJson(json)
        assertTrue(result.isSuccess)
        val breakdown = result.getOrThrow()

        assertEquals("English", breakdown.storyAnalysis.detectedLanguage)
        assertEquals("1882 Himalayan Frontier", breakdown.storyAnalysis.eraOrPeriod)
        assertEquals(2, breakdown.storyAnalysis.locations.size)
        assertEquals(1, breakdown.identifiedCharacters.size)
        assertEquals(1, breakdown.scenes.size)

        val scene = breakdown.scenes.first()
        assertEquals("The Hidden Ledger", scene.title)
        assertEquals(9.5, scene.durationSec, 0.01)
        assertEquals("Cross Dissolve", scene.scenePlan.transitionSuggestion)
        assertEquals("Dust Motes", scene.scenePlan.atmosphereSuggestion)
        assertEquals(1, scene.dialogueLines.size)
        assertEquals("Dr. Meera Sen", scene.dialogueLines.first().speakerName)
        assertEquals(2, scene.soundSuggestions.size)
        assertEquals("SFX", scene.soundSuggestions.first().category)
        assertFalse(scene.soundSuggestions.first().loopEnabled)
    }

    @Test
    fun parseBreakdownJson_preservesHindiDevanagariText() {
        val json = """
        {
          "storyAnalysis": {
            "detectedLanguage": "Hindi",
            "storyLogline": "पुरानी दिल्ली की गलियों में एक भूले हुए नक़्शे की खोज।",
            "eraOrPeriod": "1857 पुरानी दिल्ली",
            "emotionalArc": "रहस्य से गर्व तक",
            "locations": ["चाँदनी चौक"],
            "characterRelationships": ["मीरा और दादाजी"],
            "narrativeBeats": ["नक़्शा मिलना"]
          },
          "identifiedCharacters": [],
          "scenes": [
            {
              "title": "चाँदनी चौक की सुबह",
              "description": "सुबह की धुंध में पुरानी हवेली।",
              "visualSummary": "सुनहरी धूप और कबूतर।",
              "narrationText": "सन् अठारह सौ सत्तावन की उस सर्द सुबह में, पुरानी दिल्ली की गलियाँ शांत थीं।",
              "subtitleText": "पुरानी दिल्ली की गलियाँ शांत थीं।",
              "durationSec": 8.0,
              "backgroundPreset": "old_delhi",
              "dialogueLines": [
                {
                  "speakerName": "मीरा",
                  "text": "यह नक़्शा हमें सच तक ले जाएगा।",
                  "emotion": "Determined",
                  "startSec": 1.0,
                  "durationSec": 3.0
                }
              ]
            }
          ]
        }
        """.trimIndent()

        val result = ScriptAiService.parseBreakdownJson(json)
        assertTrue(result.isSuccess)
        val breakdown = result.getOrThrow()
        assertEquals("Hindi", breakdown.storyAnalysis.detectedLanguage)
        assertTrue(breakdown.scenes.first().narrationText.contains("पुरानी दिल्ली"))
        assertTrue(breakdown.scenes.first().dialogueLines.first().text.contains("नक़्शा"))
    }

    @Test
    fun parseSoundSuggestionsJson_clampsBoundsAndFiltersInvalidEntries() {
        val json = """
        {
          "suggestions": [
            {
              "category": "MUSIC",
              "title": "Solo Sarangi Drone",
              "description": "Melancholic documentary string texture",
              "suggestedStartSec": -5.0,
              "suggestedVolume": 9.0,
              "loopEnabled": true
            },
            {
              "category": "INVALID_CAT",
              "title": "Footsteps on Gravel",
              "description": "Slow measured walking steps",
              "suggestedStartSec": 2.4,
              "suggestedVolume": 0.8,
              "loopEnabled": false
            }
          ]
        }
        """.trimIndent()

        val parsed = ScriptAiService.parseSoundSuggestionsJson(json)
        assertTrue(parsed.isSuccess)
        val list = parsed.getOrThrow()
        assertEquals(2, list.size)
        assertEquals("MUSIC", list[0].category)
        assertEquals(0.0, list[0].suggestedStartSec, 0.001)
        assertEquals(1.2, list[0].suggestedVolume, 0.001)
        assertEquals("SFX", list[1].category) // fallback for invalid category
    }

    // -------------------------------------------------------------------------
    // 2. Audio Import Validation, Speech Duration & Narration Sync Diagnostics
    // -------------------------------------------------------------------------

    @Test
    fun validateAudioImport_enforcesMimeAllowlistAnd15MbCap() {
        assertTrue(AudioTimelineEngine.validateAudioImport("audio/mpeg", 2_500_000L) is AudioValidationResult.Valid)
        assertTrue(AudioTimelineEngine.validateAudioImport("audio/wav", 1024L) is AudioValidationResult.Valid)
        assertTrue(AudioTimelineEngine.validateAudioImport("audio/mp4", AudioTimelineEngine.MAX_AUDIO_FILE_BYTES) is AudioValidationResult.Valid)

        assertTrue(AudioTimelineEngine.validateAudioImport("video/mp4", 1024L) is AudioValidationResult.Invalid)
        assertTrue(AudioTimelineEngine.validateAudioImport("application/octet-stream", 1024L) is AudioValidationResult.Invalid)
        assertTrue(AudioTimelineEngine.validateAudioImport("audio/wav", 0L) is AudioValidationResult.Invalid)
        assertTrue(
            AudioTimelineEngine.validateAudioImport(
                "audio/mpeg",
                AudioTimelineEngine.MAX_AUDIO_FILE_BYTES + 1L
            ) is AudioValidationResult.Invalid
        )
    }

    @Test
    fun estimateSpeechDurationAndNarrationSync_detectsMissingAndMismatch() {
        val engDur = AudioTimelineEngine.estimateSpeechDurationSec(
            "In the quiet winter of 1882, explorers crossed the mountain pass toward the ancient archive.",
            language = "English"
        )
        val hiDur = AudioTimelineEngine.estimateSpeechDurationSec(
            "सन् अठारह सौ बयासी की सर्दियों में यात्रियों ने हिमालय का प्राचीन दर्रा पार किया।",
            language = "Hindi"
        )
        assertTrue("English duration should be > 4s, got $engDur", engDur > 4.0)
        assertTrue("Hindi duration should be > 4s, got $hiDur", hiDur > 4.0)

        val scenes = listOf(
            StudioScene(
                id = "s1",
                title = "Aligned Scene",
                narrationText = "In the quiet winter of 1882, explorers crossed the mountain pass.",
                durationSec = 5.5
            ),
            StudioScene(
                id = "s2",
                title = "Missing Narration Scene",
                narrationText = "",
                durationSec = 6.0
            ),
            StudioScene(
                id = "s3",
                title = "Too Short For Long Narration",
                narrationText = "This is a much longer documentary narration passage explaining the full historical context of the expedition across three river valleys and two high-altitude passes before winter snow closed the caravan road.",
                durationSec = 4.0
            )
        )

        val report = AudioTimelineEngine.analyzeNarrationSync(scenes, "English")
        assertEquals(1, report.missingNarrationCount)
        assertTrue(report.durationMismatchCount >= 1)
        assertNull(report.sceneStatuses[0].warning)
        assertNotNull(report.sceneStatuses[1].warning)
        assertNotNull(report.sceneStatuses[2].warning)
    }

    // -------------------------------------------------------------------------
    // 3. Audio Clip Trimming, Move, Fade Gain Envelope & Multi-Track Playhead
    // -------------------------------------------------------------------------

    @Test
    fun audioClipTrimMoveAndFadeEnvelope_evaluatesDeterministically() {
        val baseClip = SceneAudioClip(
            clipId = "c1",
            audioAssetId = "a1",
            title = "Score",
            trackType = "MUSIC",
            startSec = 2.0,
            sourceDurationSec = 10.0,
            trimStartSec = 1.0,
            trimEndSec = 9.0,
            volume = 0.8,
            fadeInSec = 2.0,
            fadeOutSec = 2.0,
            loopEnabled = false,
            muted = false
        )

        assertEquals(8.0, AudioTimelineEngine.trimmedSourceDuration(baseClip), 0.001)
        assertEquals(8.0, AudioTimelineEngine.effectiveTimelineDuration(baseClip, 20.0), 0.001)

        // Before start -> 0
        assertEquals(0f, AudioTimelineEngine.computeClipGainAtTime(baseClip, 1.5, 20.0), 0.001f)
        // Halfway through fade-in (t=3.0 => elapsed=1.0 / 2.0 = 0.5 * 0.8 = 0.4)
        assertEquals(0.4f, AudioTimelineEngine.computeClipGainAtTime(baseClip, 3.0, 20.0), 0.01f)
        // Sustain (t=6.0 => full 0.8)
        assertEquals(0.8f, AudioTimelineEngine.computeClipGainAtTime(baseClip, 6.0, 20.0), 0.01f)
        // Halfway through fade-out (clip ends at 10.0; t=9.0 => 1.0s left / 2.0 = 0.5 * 0.8 = 0.4)
        assertEquals(0.4f, AudioTimelineEngine.computeClipGainAtTime(baseClip, 9.0, 20.0), 0.01f)
        // Muted clip -> 0
        assertEquals(0f, AudioTimelineEngine.computeClipGainAtTime(baseClip.copy(muted = true), 6.0, 20.0), 0.001f)

        // Move & trim safety clamps
        val moved = AudioTimelineEngine.moveClip(baseClip, -5.0, 15.0)
        assertEquals(0.0, moved.startSec, 0.001)
        val trimmed = AudioTimelineEngine.trimClip(baseClip, 8.0, 2.0)
        assertTrue(trimmed.trimEndSec >= trimmed.trimStartSec + AudioTimelineEngine.MIN_CLIP_DURATION_SEC)
    }

    @Test
    fun resolveActiveClipsAtPlayhead_handlesMultiTrackLoopMuteAndPriorityCap() {
        val assets = listOf(
            StudioAudioAsset(id = "a_nar", name = "Narration", localFilePath = "/tmp/nar.wav", durationSec = 6.0),
            StudioAudioAsset(id = "a_dlg", name = "Dialogue", localFilePath = "/tmp/dlg.wav", durationSec = 4.0),
            StudioAudioAsset(id = "a_sfx", name = "Wind SFX", localFilePath = "/tmp/sfx.wav", durationSec = 5.0),
            StudioAudioAsset(id = "a_amb", name = "Room Tone", localFilePath = "/tmp/amb.wav", durationSec = 4.0),
            StudioAudioAsset(id = "a_mus", name = "Project Score", localFilePath = "/tmp/mus.mp3", durationSec = 4.0)
        )
        val scene1 = StudioScene(
            id = "sc1",
            title = "Scene 1",
            durationSec = 10.0,
            audioClips = listOf(
                SceneAudioClip(clipId = "c_nar", audioAssetId = "a_nar", trackType = "NARRATION", startSec = 0.0, sourceDurationSec = 6.0, trimEndSec = 6.0),
                SceneAudioClip(clipId = "c_dlg", audioAssetId = "a_dlg", trackType = "DIALOGUE", startSec = 1.0, sourceDurationSec = 4.0, trimEndSec = 4.0),
                SceneAudioClip(clipId = "c_sfx", audioAssetId = "a_sfx", trackType = "SFX", startSec = 0.5, sourceDurationSec = 5.0, trimEndSec = 5.0),
                SceneAudioClip(clipId = "c_amb", audioAssetId = "a_amb", trackType = "AMBIENT", startSec = 0.0, sourceDurationSec = 4.0, trimEndSec = 4.0, loopEnabled = true)
            )
        )
        val projectMusic = SceneAudioClip(
            clipId = "c_mus",
            audioAssetId = "a_mus",
            trackType = "MUSIC",
            scope = "PROJECT",
            startSec = 0.0,
            sourceDurationSec = 4.0,
            trimStartSec = 1.0,
            trimEndSec = 3.0, // trimmed loop length = 2.0s
            volume = 0.4,
            loopEnabled = true
        )

        // At t = 3.5s, 5 tracks are audible, cap = 4 keeps Dialogue, Narration, SFX, Ambient (drops lowest-priority Music)
        val capped = AudioTimelineEngine.resolveActiveClipsAtPlayhead(
            scenes = listOf(scene1),
            projectAudioClips = listOf(projectMusic),
            audioAssets = assets,
            globalPlayheadSec = 3.5,
            mutedTracks = emptySet(),
            maxConcurrentTracks = 4
        )
        assertEquals(4, capped.size)
        assertEquals(TimelineTrackType.DIALOGUE, capped[0].trackType)
        assertEquals(TimelineTrackType.NARRATION, capped[1].trackType)

        // Mute SFX & Ambient -> Music enters and wraps cleanly via trim + loop modulo:
        // elapsed=3.5, trimmedDur=2.0 -> 3.5 % 2.0 = 1.5 -> + trimStart(1.0) = 2.5s = 2500ms
        val withMute = AudioTimelineEngine.resolveActiveClipsAtPlayhead(
            scenes = listOf(scene1),
            projectAudioClips = listOf(projectMusic),
            audioAssets = assets,
            globalPlayheadSec = 3.5,
            mutedTracks = setOf(TimelineTrackType.SOUND_EFFECTS, TimelineTrackType.AMBIENT),
            maxConcurrentTracks = 4
        )
        assertEquals(3, withMute.size)
        val musicWin = withMute.first { it.clipId == "c_mus" }
        assertEquals(2500, musicWin.sourceSeekMs)
        assertEquals(0.4f, musicWin.effectiveGain, 0.01f)
    }

    // -------------------------------------------------------------------------
    // 4. Lip-Sync Engine (English + Hindi Devanagari + Waveform Gating + Acting Sync)
    // -------------------------------------------------------------------------

    @Test
    fun lipSyncEngine_mapsEnglishAndHindiVisemesAndGatesSilenceFromRealWaveform() {
        assertEquals(MouthViseme.A_OPEN, LipSyncEngine.mapCharToViseme('a'))
        assertEquals(MouthViseme.M_CLOSED, LipSyncEngine.mapCharToViseme('m'))
        assertEquals(MouthViseme.O_ROUND, LipSyncEngine.mapCharToViseme('o'))
        assertEquals(MouthViseme.A_OPEN, LipSyncEngine.mapCharToViseme('आ'))
        assertEquals(MouthViseme.M_CLOSED, LipSyncEngine.mapCharToViseme('म'))
        assertEquals(MouthViseme.O_ROUND, LipSyncEngine.mapCharToViseme('ओ'))
        assertEquals(MouthViseme.SILENT, LipSyncEngine.mapCharToViseme('।'))

        val line = DialogueLine(
            lineId = "dlg_1",
            characterId = "char_meera",
            speakerName = "Dr. Meera",
            text = "Hello world",
            startSec = 1.0,
            durationSec = 2.0
        )
        // First half loud (0.8), second half below 0.08 silence gate (0.02)
        val peaks = listOf(0.8, 0.75, 0.85, 0.7, 0.02, 0.01, 0.03, 0.02)
        val cues = LipSyncEngine.generateVisemeCuesForDialogue(line, waveformPeaks = peaks)
        assertTrue(cues.isNotEmpty())
        assertTrue("Expected audible non-silent visemes in loud region", cues.take(3).any { it.viseme != MouthViseme.SILENT.name })
        assertTrue("Expected real waveform silence gating in quiet tail", cues.takeLast(2).all { it.viseme == MouthViseme.SILENT.name })
    }

    @Test
    fun lipSyncEngine_evaluatesSpeakingStateAndSyncsPhase3ActingCuesNonDestructively() {
        val dlg = DialogueLine(
            lineId = "dlg_hi",
            characterId = "char_1",
            speakerName = "मीरा",
            text = "हम इतिहास की सच्चाई खोज रहे हैं।",
            language = "Hindi",
            startSec = 2.0,
            durationSec = 3.0,
            emotion = "Determined"
        )
        val manualCue = CharacterActingCue(
            cueId = "user_custom_cue",
            characterId = "char_1",
            timeSec = 0.5,
            expression = "Curious",
            action = "Inspecting",
            poseView = "Front View"
        )
        val baseScene = StudioScene(
            id = "sc_lip",
            durationSec = 8.0,
            characterCast = listOf(
                SceneCharacterPlacement(characterId = "char_1", expression = "Calm", action = "Standing")
            ),
            dialogueLines = listOf(dlg),
            characterActingCues = listOf(manualCue)
        )
        val withVisemes = baseScene.copy(
            lipSyncCues = LipSyncEngine.generateSceneLipSyncCues(baseScene, emptyList())
        )

        // Before dialogue -> not speaking
        val before = LipSyncEngine.evaluateSpeakingStatesAtTime(withVisemes, 1.0)["char_1"]
        assertNotNull(before)
        assertFalse(before!!.speaking)
        assertEquals(MouthViseme.SILENT, before.activeViseme)
        assertNull(LipSyncEngine.activeDialogueSubtitleAtTime(withVisemes, 1.0))

        // During dialogue -> speaking with active subtitle
        val during = LipSyncEngine.evaluateSpeakingStatesAtTime(withVisemes, 3.0)["char_1"]
        assertNotNull(during)
        assertTrue(during!!.speaking)
        assertEquals("dlg_hi", during.activeDialogueLineId)
        assertEquals("मीरा: हम इतिहास की सच्चाई खोज रहे हैं।", LipSyncEngine.activeDialogueSubtitleAtTime(withVisemes, 3.0))

        // Sync to Phase 3 acting cues preserves user_custom_cue and inserts Talking + return cues
        val synced = LipSyncEngine.buildSpeakingActingCues(withVisemes)
        assertTrue(synced.any { it.cueId == "user_custom_cue" })
        assertTrue(synced.any { it.cueId == "dlg_act_start_dlg_hi" && it.action == "Talking" && it.expression == "Determined" })
        assertTrue(synced.any { it.cueId == "dlg_act_end_dlg_hi" && it.timeSec == 5.0 })
    }

    // -------------------------------------------------------------------------
    // 5. Repository Phase 5 Serialization Round-Trip & Legacy Backward Compat
    // -------------------------------------------------------------------------

    @Test
    fun repositorySerialization_roundTripsAllPhase5ModelsAndClampsCorruptValues() {
        val analysis = StoryAnalysisSummary(
            detectedLanguage = "Bilingual",
            storyLogline = "Tracing monsoon cartographers",
            eraOrPeriod = "1920s India",
            emotionalArc = "Wonder to revelation",
            locations = listOf("Shimla", "Old Delhi"),
            characterRelationships = listOf("Mentor & Archivist"),
            narrativeBeats = listOf("Arrival", "Discovery"),
            estimatedTotalDurationSec = 42.5,
            approved = true
        )
        val analysisRoundTrip = StudioRepository.mapToStoryAnalysis(StudioRepository.storyAnalysisToMap(analysis))
        assertEquals(analysis, analysisRoundTrip)

        val plan = ScenePlanMetadata(
            location = "Shimla Ridge",
            environment = "Mist-covered cedar ridge",
            timeOfDay = "Dawn",
            weather = "Fog",
            emotionalTone = "Reflective",
            visualDirection = "Slow crane reveal",
            cameraSuggestion = "Wide Establishing",
            lightingSuggestion = "Golden Hour",
            atmosphereSuggestion = "Floating Mist",
            vfxSuggestion = "Soft Vignette",
            transitionSuggestion = "Fade to Black",
            audioCuesSummary = "Distant mountain wind",
            suggestedProps = listOf("Brass Compass", "Map Tube"),
            approved = true
        )
        assertEquals(plan, StudioRepository.mapToScenePlan(StudioRepository.scenePlanToMap(plan)))

        val dialogue = listOf(
            DialogueLine(
                lineId = "d1",
                characterId = "c1",
                speakerName = "Arun",
                text = "The valley opens ahead.",
                language = "English",
                startSec = Double.NaN, // must clamp safely to 0.0
                durationSec = 2.8,
                audioAssetId = "a1",
                clipId = "clip1",
                voiceId = "en-in-x-enc",
                emotion = "Hopeful",
                orderIndex = 0
            )
        )
        val dlgRT = StudioRepository.mapsToDialogueLines(StudioRepository.dialogueLinesToMaps(dialogue))
        assertEquals(1, dlgRT.size)
        assertEquals(0.0, dlgRT.first().startSec, 0.001)
        assertEquals("Arun", dlgRT.first().speakerName)

        val clips = listOf(
            SceneAudioClip(
                clipId = "cl1",
                audioAssetId = "a1",
                title = "Ambient Wind",
                trackType = "AMBIENT",
                scope = "PROJECT",
                startSec = 0.0,
                sourceDurationSec = 12.0,
                trimStartSec = 1.0,
                trimEndSec = 10.0,
                volume = Double.POSITIVE_INFINITY, // must clamp to fallback 1.0
                fadeInSec = 1.5,
                fadeOutSec = 2.0,
                loopEnabled = true
            )
        )
        val clipsRT = StudioRepository.mapsToAudioClips(StudioRepository.audioClipsToMaps(clips))
        assertEquals(1, clipsRT.size)
        assertEquals(1.0, clipsRT.first().volume, 0.001)
        assertEquals("PROJECT", clipsRT.first().scope)
        assertTrue(clipsRT.first().loopEnabled)

        val cues = listOf(
            VisemeCue("v1", "c1", "d1", 1.2, 0.15, "A_OPEN", 0.78)
        )
        assertEquals(cues, StudioRepository.mapsToLipSyncCues(StudioRepository.lipSyncCuesToMaps(cues)))

        val suggestions = listOf(
            AiSoundSuggestion("s1", "MUSIC", "Solo Bansuri", "Gentle flute", 0.0, 0.4, true, false)
        )
        assertEquals(suggestions, StudioRepository.mapsToSoundSuggestions(StudioRepository.soundSuggestionsToMaps(suggestions)))

        val audioMap = StudioRepository.audioAssetFieldsToMap(
            StudioAudioAsset(
                name = "Narrator Take 1",
                category = "Narration",
                sourceType = "ANDROID_TTS",
                mimeType = "audio/wav",
                fileSizeBytes = 240_000L,
                durationSec = 6.2,
                waveformPeaks = List(80) { 0.5 } // must cap at 48 peaks
            )
        )
        @Suppress("UNCHECKED_CAST")
        assertEquals(48, (audioMap["waveformPeaks"] as List<Double>).size)
        assertEquals("audio/wav", audioMap["mimeType"])

        // Legacy empty map backward compatibility
        assertEquals(StoryAnalysisSummary(), StudioRepository.mapToStoryAnalysis(emptyMap()))
        assertEquals(ScenePlanMetadata(), StudioRepository.mapToScenePlan(emptyMap()))
        assertTrue(StudioRepository.mapsToDialogueLines(emptyList()).isEmpty())
        assertTrue(StudioRepository.mapsToAudioClips(emptyList()).isEmpty())
    }
}
