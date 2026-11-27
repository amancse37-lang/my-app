package com.example

import com.example.data.CharacterImageService
import com.example.data.SceneCharacterPlacement
import com.example.data.StudioCharacter
import com.example.data.StudioPresets
import com.example.data.StudioScene
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun effectiveCast_fallsBackToLegacyCharacterId() {
        val legacyScene = StudioScene(
            id = "scene_legacy",
            characterId = "char_1",
            characterCast = emptyList(),
            posX = -45.0,
            posY = 12.0,
            scale = 1.15,
            rotationDeg = 5.0
        )
        val cast = legacyScene.effectiveCast()
        assertEquals(1, cast.size)
        assertEquals("char_1", cast.first().characterId)
        assertEquals(-45.0, cast.first().posX, 0.001)
    }

    @Test
    fun effectiveCast_preservesMultiCharacterPlacements() {
        val multiScene = StudioScene(
            id = "scene_multi",
            characterId = "char_1",
            characterCast = listOf(
                SceneCharacterPlacement(characterId = "char_1", expression = "Thinking", action = "Pointing"),
                SceneCharacterPlacement(characterId = "char_2", expression = "Calm", action = "Listening")
            )
        )
        val cast = multiScene.effectiveCast()
        assertEquals(2, cast.size)
        assertEquals("Pointing", cast[0].action)
        assertEquals("Listening", cast[1].action)
    }

    @Test
    fun characterPresets_coverAllRequiredEmotionsActionsAndViews() {
        assertEquals(11, StudioPresets.characterEmotions.size)
        assertEquals(16, StudioPresets.characterActions.size)
        assertEquals(5, StudioPresets.characterViews.size)
        assertTrue("Determined" in StudioPresets.characterEmotions)
        assertTrue("Interacting with other characters" in StudioPresets.characterActions)
        assertTrue("Three-Quarter View" in StudioPresets.characterViews)
    }

    @Test
    fun characterDesignPrompt_includesIdentityLockAndRequestedShot() {
        val service = CharacterImageService()
        val character = StudioCharacter(
            charCode = "CHR-01",
            name = "Dr. Aarav Mehta",
            role = "Lead Historian",
            visualAge = "Mid 40s",
            faceDescription = "Warm brown eyes, neat salt-and-pepper beard",
            hairStyle = "Swept-back wavy",
            hairColor = "Dark charcoal",
            clothing = "Herringbone tweed jacket with ochre scarf",
            clothingColors = "Charcoal, Ochre",
            accessories = "Round tortoise-shell spectacles",
            appearanceNotes = "Keep glasses and ochre scarf identical across all scenes."
        )
        val prompt = service.buildCharacterDesignPrompt(
            character = character,
            viewType = "Side View",
            expression = "Worried",
            poseAction = "Pointing",
            visualStyle = "Painterly 2D Documentary",
            hasAnchorReference = true
        )
        assertTrue(prompt.contains("CRITICAL IDENTITY LOCK"))
        assertTrue(prompt.contains("CHR-01"))
        assertTrue(prompt.contains("Side View"))
        assertTrue(prompt.contains("Worried"))
        assertTrue(prompt.contains("Pointing"))
        assertTrue(prompt.contains("Round tortoise-shell spectacles"))
    }
}
