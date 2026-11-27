const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_studio_01";
const BOB_UID = "bob_studio_02";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProjectPayload(uid, createdAt) {
  return {
    userId: uid,
    name: "Echoes of the Silk Road",
    scriptText: "Scene 1: Dawn breaks over the Himalayan pass...",
    scriptLanguage: "Bilingual",
    aspectRatio: "16:9",
    fps: 24,
    resolution: "1080p",
    createdAt,
    updatedAt: createdAt,
  };
}

function validScenePayload(uid, createdAt, orderIndex = 0) {
  return {
    userId: uid,
    orderIndex,
    title: "Himalayan Dawn Establishing",
    description: "Wide parallax sweep across snow-dusted passes.",
    visualSummary: "Misty dawn peaks with golden light rays.",
    narrationText: "For centuries, these high passes carried stories across continents.",
    subtitleText: "For centuries, these high passes carried stories across continents.",
    durationSec: 8.0,
    backgroundPreset: "himalayas",
    backgroundTintHex: "#1B2838",
    characterId: "char_1",
    characterCast: [
      {
        characterId: "char_1",
        expression: "Thinking",
        action: "Pointing",
        poseView: "Three-Quarter View",
        posX: -90.0,
        posY: 16.0,
        scale: 1.0,
        rotationDeg: 0.0,
      },
      {
        characterId: "char_2",
        expression: "Calm",
        action: "Listening",
        poseView: "Side View",
        posX: 110.0,
        posY: 16.0,
        scale: 0.95,
        rotationDeg: 0.0,
      },
    ],
    posX: -90.0,
    posY: 16.0,
    scale: 1.0,
    rotationDeg: 0.0,
    animationStyle: "Subtle Drift",
    cameraMovement: "Slow Push In",
    cameraZoom: 1.15,
    effectPreset: "Film Grain",
    effectIntensity: 0.35,
    motionGraphicTitle: "CHAPTER I · THE PASS",
    motionGraphicStyle: "Chapter Card",
    musicCue: "Himalayan Strings Drone",
    sfxCue: "High Altitude Wind",
    objectProp: "Brass Compass",
    createdAt,
    updatedAt: createdAt,
  };
}

function validCharacterPayload(uid, createdAt) {
  return {
    userId: uid,
    charCode: "CHR-01",
    name: "Dr. Aarav Mehta",
    role: "Lead Historian & Narrator",
    visualAge: "Mid 40s",
    faceDescription: "Warm brown eyes, neat salt-and-pepper beard, thoughtful brow lines.",
    hairStyle: "Slightly wavy swept-back short hair",
    hairColor: "Salt-and-pepper dark charcoal",
    clothing: "Tailored herringbone tweed jacket over dark turtleneck and woven wool scarf",
    clothingColors: "Charcoal, Ochre, Deep Rust",
    bodyProportions: "Average build, upright scholar posture",
    accessories: "Round tortoise-shell spectacles, vintage brass pocket watch",
    personality: "Reflective, curious, empathetic documentary storyteller",
    appearanceNotes: "Always retain round tortoise-shell glasses and patterned ochre scarf across scenes.",
    wardrobePalette: "Charcoal, Ochre, Deep Rust",
    expressionDefault: "Thinking",
    visualStyle: "Painterly 2D Documentary",
    referenceImageBase64: "iVBORw0KGgoAAAANSUhEUgAAAAEAAAAB",
    avatarPreset: "narrator",
    accentColorHex: "#F5A623",
    createdAt,
    updatedAt: createdAt,
  };
}

function validCharacterRefPayload(uid, createdAt) {
  return {
    userId: uid,
    viewType: "Three-Quarter View",
    expression: "Determined",
    poseAction: "Pointing",
    visualStyle: "Painterly 2D Documentary",
    promptSummary: "CHR-01 Dr. Aarav Mehta - Three-Quarter View - Determined - Pointing",
    imageBase64: "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwC",
    createdAt,
    updatedAt: createdAt,
  };
}

function validAssetPayload(uid, createdAt) {
  return {
    userId: uid,
    name: "Old Delhi Twilight Archway",
    category: "Location",
    tags: "heritage,street,twilight",
    notes: "Warm lantern lighting for chapter 2 flashback.",
    presetKey: "old_delhi",
    localUri: "",
    durationLabel: "1920x1080",
    createdAt,
    updatedAt: createdAt,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: denied all reads and writes including character references", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 2000);

  await assertFails(unauthDb.collection(`users/${ALICE_UID}/characters`).get());
  await assertFails(
    unauthDb
      .collection(`users/${ALICE_UID}/characters/char_1/references`)
      .doc("ref_1")
      .set(validCharacterRefPayload(ALICE_UID, now))
  );
});

test("Authenticated owner: 16-field Character Profile, reference turnaround sheets, and multi-character scene cast succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const updated = new Date(Date.now() - 1000);

  // Project + multi-character scene
  const projRef = aliceDb.collection(`users/${ALICE_UID}/projects`).doc("proj_1");
  await assertSucceeds(projRef.set(validProjectPayload(ALICE_UID, created)));
  const sceneRef = aliceDb.collection(`users/${ALICE_UID}/projects/proj_1/scenes`).doc("scene_1");
  await assertSucceeds(sceneRef.set(validScenePayload(ALICE_UID, created, 0)));
  await assertSucceeds(
    sceneRef.update({
      characterCast: [
        {
          characterId: "char_1",
          expression: "Determined",
          action: "Talking",
          poseView: "Front View",
          posX: 0.0,
          posY: 0.0,
          scale: 1.1,
          rotationDeg: 0.0,
        },
      ],
      updatedAt: updated,
    })
  );

  // Full character profile + reference turnaround subcollection
  const charRef = aliceDb.collection(`users/${ALICE_UID}/characters`).doc("char_1");
  await assertSucceeds(charRef.set(validCharacterPayload(ALICE_UID, created)));
  await assertSucceeds(
    charRef.update({
      hairStyle: "Neatly combed",
      accessories: "Round glasses, leather satchel",
      updatedAt: updated,
    })
  );

  const sheetRef = aliceDb.collection(`users/${ALICE_UID}/characters/char_1/references`).doc("ref_01");
  await assertSucceeds(sheetRef.set(validCharacterRefPayload(ALICE_UID, created)));
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/characters/char_1/references`).get());
  await assertSucceeds(sheetRef.delete());

  // Asset check preserved
  const assetRef = aliceDb.collection(`users/${ALICE_UID}/assets`).doc("asset_1");
  await assertSucceeds(assetRef.set(validAssetPayload(ALICE_UID, created)));
});

test("Cross-user isolation: Bob cannot access or modify Alice's characters or reference sheets", async () => {
  const created = new Date(Date.now() - 10000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection(`users/${ALICE_UID}/characters`).doc("char_1").set(validCharacterPayload(ALICE_UID, created));
    await db
      .collection(`users/${ALICE_UID}/characters/char_1/references`)
      .doc("ref_01")
      .set(validCharacterRefPayload(ALICE_UID, created));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection(`users/${ALICE_UID}/characters`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/characters/char_1/references`).get());
  await assertFails(
    bobDb.collection(`users/${ALICE_UID}/characters/char_1/references`).doc("ref_01").delete()
  );
});

test("Security bounds: reject oversized characterCast (>12), ghost fields, and empty reference image", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Oversized cast (>12)
  const tooMany = Array.from({ length: 13 }, (_, i) => ({ characterId: `char_${i}` }));
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/projects/proj_1/scenes`).doc("scene_big").set({
      ...validScenePayload(ALICE_UID, created),
      characterCast: tooMany,
    })
  );

  // Empty reference image rejected
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/characters/char_1/references`).doc("ref_empty").set({
      ...validCharacterRefPayload(ALICE_UID, created),
      imageBase64: "",
    })
  );

  // Ghost field rejected
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/characters`).doc("char_ghost").set({
      ...validCharacterPayload(ALICE_UID, created),
      isAdmin: true,
    })
  );
});

test("Scene composition persistence: sceneElements & layerStates succeed and enforce bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const updated = new Date(Date.now() - 1000);

  const projRef = aliceDb.collection(`users/${ALICE_UID}/projects`).doc("proj_comp");
  await assertSucceeds(projRef.set(validProjectPayload(ALICE_UID, created)));

  const sceneRef = aliceDb.collection(`users/${ALICE_UID}/projects/proj_comp/scenes`).doc("scene_comp_1");
  await assertSucceeds(
    sceneRef.set({
      ...validScenePayload(ALICE_UID, created, 0),
      sceneElements: [
        {
          elementId: "el_prop_01",
          name: "Imported Compass PNG",
          elementType: "PROP",
          assetId: "asset_img_1",
          assetPresetKey: "",
          assetUri: "content://media/picker/0/item/1",
          layer: "PROPS",
          posX: 40.0,
          posY: 60.0,
          scale: 0.85,
          rotationDeg: -10.0,
          zIndex: 1,
          visible: true,
          opacity: 0.95,
          flipX: false,
        },
      ],
      layerStates: [
        { layerKey: "BACKGROUND", visible: true, order: 0, opacity: 1.0, parallaxFactor: 0.25 },
        { layerKey: "MIDGROUND", visible: true, order: 1, opacity: 1.0, parallaxFactor: 0.55 },
        { layerKey: "CHARACTERS", visible: true, order: 2, opacity: 1.0, parallaxFactor: 0.85 },
        { layerKey: "PROPS", visible: true, order: 3, opacity: 1.0, parallaxFactor: 0.95 },
        { layerKey: "FOREGROUND", visible: true, order: 4, opacity: 1.0, parallaxFactor: 1.15 },
        { layerKey: "OVERLAY", visible: true, order: 5, opacity: 1.0, parallaxFactor: 1.0 },
      ],
    })
  );

  // Partial update to layerStates & sceneElements succeeds
  await assertSucceeds(
    sceneRef.update({
      layerStates: [
        { layerKey: "BACKGROUND", visible: true, order: 0, opacity: 1.0, parallaxFactor: 0.25 },
        { layerKey: "PROPS", visible: false, order: 1, opacity: 0.8, parallaxFactor: 0.95 },
      ],
      updatedAt: updated,
    })
  );

  // Reject >24 sceneElements
  const tooManyElements = Array.from({ length: 25 }, (_, i) => ({ elementId: `el_${i}` }));
  await assertFails(
    sceneRef.update({
      sceneElements: tooManyElements,
      updatedAt: updated,
    })
  );

  // Reject >6 layerStates
  const tooManyLayers = Array.from({ length: 7 }, (_, i) => ({ layerKey: `L_${i}`, order: i }));
  await assertFails(
    sceneRef.update({
      layerStates: tooManyLayers,
      updatedAt: updated,
    })
  );
});

test("AI visual assets persistence: imageBase64, hasTransparency, visualStyle, promptUsed succeed and enforce bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const updated = new Date(Date.now() - 1000);

  const bgAssetRef = aliceDb.collection(`users/${ALICE_UID}/assets`).doc("asset_ai_bg_1");
  await assertSucceeds(
    bgAssetRef.set({
      ...validAssetPayload(ALICE_UID, created),
      name: "Himalayan Dawn Pass (AI)",
      category: "Background",
      presetKey: "ai_generated",
      imageBase64: "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwC",
      hasTransparency: false,
      visualStyle: "Painterly 2D Documentary",
      promptUsed: "Wide 16:9 2D documentary Himalayan pass at dawn, misty peaks, golden rays",
    })
  );

  // Update prop metadata & transparency flag succeeds
  await assertSucceeds(
    bgAssetRef.update({
      category: "Prop",
      hasTransparency: true,
      updatedAt: updated,
    })
  );

  // Reject oversized imageBase64 (>320,000 chars)
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/assets`).doc("asset_ai_huge").set({
      ...validAssetPayload(ALICE_UID, created),
      imageBase64: "A".repeat(320001),
    })
  );

  // Reject non-boolean hasTransparency
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/assets`).doc("asset_bad_alpha").set({
      ...validAssetPayload(ALICE_UID, created),
      hasTransparency: "yes",
    })
  );
});

test("Phase 3 animation persistence: animationKeyframes & characterActingCues succeed and enforce bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const updated = new Date(Date.now() - 1000);

  const projRef = aliceDb.collection(`users/${ALICE_UID}/projects`).doc("proj_anim");
  await assertSucceeds(projRef.set(validProjectPayload(ALICE_UID, created)));

  const sceneRef = aliceDb.collection(`users/${ALICE_UID}/projects/proj_anim/scenes`).doc("scene_anim_1");
  await assertSucceeds(
    sceneRef.set({
      ...validScenePayload(ALICE_UID, created, 0),
      animationKeyframes: [
        {
          keyframeId: "kf_1",
          targetType: "CHARACTER",
          targetId: "char_1",
          property: "POS_X",
          timeSec: 0.0,
          value: -260.0,
          easing: "EASE_IN_OUT",
        },
        {
          keyframeId: "kf_2",
          targetType: "CHARACTER",
          targetId: "char_1",
          property: "POS_X",
          timeSec: 4.0,
          value: 140.0,
          easing: "EASE_IN_OUT",
        },
        {
          keyframeId: "kf_cam_1",
          targetType: "CAMERA",
          targetId: "camera_main",
          property: "CAMERA_ZOOM",
          timeSec: 0.0,
          value: 1.0,
          easing: "LINEAR",
        },
      ],
      characterActingCues: [
        {
          cueId: "cue_1",
          characterId: "char_1",
          timeSec: 2.0,
          expression: "Determined",
          action: "Pointing",
          poseView: "Front View",
          referenceSheetId: "ref_01",
        },
      ],
    })
  );

  // Partial update of animationKeyframes & characterActingCues succeeds
  await assertSucceeds(
    sceneRef.update({
      animationKeyframes: [
        {
          keyframeId: "kf_cam_2",
          targetType: "CAMERA",
          targetId: "camera_main",
          property: "CAMERA_PAN_X",
          timeSec: 5.0,
          value: 60.0,
          easing: "EASE_OUT",
        },
      ],
      characterActingCues: [],
      updatedAt: updated,
    })
  );

  // Reject >200 animationKeyframes
  const tooManyKeyframes = Array.from({ length: 201 }, (_, i) => ({
    keyframeId: `kf_${i}`,
    targetType: "CHARACTER",
    targetId: "char_1",
    property: "POS_X",
    timeSec: i * 0.02,
    value: 0.0,
    easing: "LINEAR",
  }));
  await assertFails(
    sceneRef.update({
      animationKeyframes: tooManyKeyframes,
      updatedAt: updated,
    })
  );

  // Reject >60 characterActingCues
  const tooManyCues = Array.from({ length: 61 }, (_, i) => ({
    cueId: `cue_${i}`,
    characterId: "char_1",
    timeSec: i * 0.1,
    expression: "Calm",
    action: "Standing",
    poseView: "Front View",
    referenceSheetId: "",
  }));
  await assertFails(
    sceneRef.update({
      characterActingCues: tooManyCues,
      updatedAt: updated,
    })
  );
});

test("Phase 4 cinematics persistence: camera framing, depth/parallax, lighting, atmosphere & VFX succeed and enforce bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const updated = new Date(Date.now() - 1000);

  const projRef = aliceDb.collection(`users/${ALICE_UID}/projects`).doc("proj_cine");
  await assertSucceeds(projRef.set(validProjectPayload(ALICE_UID, created)));

  const sceneRef = aliceDb.collection(`users/${ALICE_UID}/projects/proj_cine/scenes`).doc("scene_cine_1");
  await assertSucceeds(
    sceneRef.set({
      ...validScenePayload(ALICE_UID, created, 0),
      cinematics: {
        cameraPreset: "Golden Establishing",
        cameraPosX: -24.0,
        cameraPosY: -12.0,
        cameraRotationDeg: -3.5,
        parallaxEnabled: true,
        parallaxStrength: 1.35,
        depthFocusPlane: 0.85,
        depthBlurIntensity: 0.4,
        depthHazeIntensity: 0.3,
        lightingPreset: "Golden Hour",
        exposure: 0.08,
        contrast: 1.12,
        saturation: 1.15,
        temperature: 0.55,
        lightSourceX: 0.65,
        lightSourceY: -0.55,
        lightGlowIntensity: 0.45,
        shadowIntensity: 0.25,
        atmosphereMode: "Rain",
        atmosphereIntensity: 0.6,
        atmosphereSpeed: 1.2,
        windStrength: 0.35,
        vignetteIntensity: 0.4,
        filmGrainIntensity: 0.25,
        bloomIntensity: 0.3,
        cameraShakeIntensity: 0.2,
        letterboxAspect: "2.39:1 Anamorphic",
      },
    })
  );

  // Partial update of cinematics succeeds
  await assertSucceeds(
    sceneRef.update({
      cinematics: {
        cameraPreset: "Close-Up",
        lightingPreset: "Noir High-Contrast",
        atmosphereMode: "Snow",
        letterboxAspect: "1.85:1 Cinema",
      },
      updatedAt: updated,
    })
  );

  // Reject non-map cinematics
  await assertFails(
    sceneRef.update({
      cinematics: "invalid_string",
      updatedAt: updated,
    })
  );

  // Reject oversized cinematics map (>32 keys)
  const oversized = {};
  for (let i = 0; i < 33; i++) {
    oversized[`k_${i}`] = i;
  }
  await assertFails(
    sceneRef.update({
      cinematics: oversized,
      updatedAt: updated,
    })
  );
});

test("Phase 5 story intelligence, dialogue, audio clips, lip-sync & character voice persistence succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const updated = new Date(Date.now() - 1000);

  const projRef = aliceDb.collection(`users/${ALICE_UID}/projects`).doc("proj_p5");
  await assertSucceeds(
    projRef.set({
      ...validProjectPayload(ALICE_UID, created),
      storyAnalysis: {
        detectedLanguage: "Bilingual",
        storyLogline: "Tracing ancient Himalayan trade routes through archival maps.",
        eraOrPeriod: "1842 & Contemporary",
        emotionalArc: "Wonder -> Nostalgia -> Reflection",
        locations: ["Himalayan Pass", "Old Delhi Bazaar"],
        characterRelationships: ["Dr. Aarav mentors Meera Sen"],
        narrativeBeats: ["Discovery", "Journey", "Archival Synthesis"],
        estimatedTotalDurationSec: 26.0,
        approved: true,
      },
      projectAudioClips: [
        {
          clipId: "pclip_bgm_1",
          audioAssetId: "aud_bgm_1",
          title: "Himalayan Strings Master",
          trackType: "MUSIC",
          scope: "PROJECT",
          startSec: 0.0,
          sourceDurationSec: 45.0,
          trimStartSec: 0.0,
          trimEndSec: 45.0,
          volume: 0.35,
          fadeInSec: 1.5,
          fadeOutSec: 2.0,
          loopEnabled: true,
          muted: false,
          dialogueLineId: "",
          characterId: "",
        },
      ],
    })
  );

  const sceneRef = aliceDb.collection(`users/${ALICE_UID}/projects/proj_p5/scenes`).doc("scene_p5_1");
  await assertSucceeds(
    sceneRef.set({
      ...validScenePayload(ALICE_UID, created, 0),
      scenePlan: {
        location: "Himalayan Pass",
        environment: "Alpine ridge with mist",
        timeOfDay: "Dawn",
        weather: "Misty",
        emotionalTone: "Contemplative",
        visualDirection: "Wide parallax pan across ridges",
        cameraSuggestion: "Golden Establishing",
        lightingSuggestion: "Golden Hour",
        atmosphereSuggestion: "Fog / Mist",
        vfxSuggestion: "Subtle Film Grain",
        transitionSuggestion: "Cross Dissolve",
        audioCuesSummary: "High altitude wind + strings",
        suggestedProps: ["Brass Compass", "Leather Map Journal"],
        approved: true,
      },
      dialogueLines: [
        {
          lineId: "dlg_1",
          characterId: "char_1",
          speakerName: "Dr. Aarav Mehta",
          text: "Long before highways crossed these valleys, every pass carried human memory.",
          language: "English",
          startSec: 0.5,
          durationSec: 4.5,
          audioAssetId: "aud_tts_1",
          clipId: "clip_dlg_1",
          voiceId: "en-us-x-tpf-local",
          emotion: "Thinking",
          orderIndex: 0,
        },
      ],
      audioClips: [
        {
          clipId: "clip_dlg_1",
          audioAssetId: "aud_tts_1",
          title: "Aarav Line 1",
          trackType: "DIALOGUE",
          scope: "SCENE",
          startSec: 0.5,
          sourceDurationSec: 4.5,
          trimStartSec: 0.0,
          trimEndSec: 4.5,
          volume: 1.0,
          fadeInSec: 0.1,
          fadeOutSec: 0.15,
          loopEnabled: false,
          muted: false,
          dialogueLineId: "dlg_1",
          characterId: "char_1",
        },
      ],
      lipSyncCues: [
        {
          cueId: "vis_0",
          characterId: "char_1",
          dialogueLineId: "dlg_1",
          timeSec: 0.5,
          durationSec: 0.12,
          viseme: "L_T_N",
          energy: 0.65,
        },
        {
          cueId: "vis_1",
          characterId: "char_1",
          dialogueLineId: "dlg_1",
          timeSec: 0.62,
          durationSec: 0.12,
          viseme: "O_ROUND",
          energy: 0.82,
        },
      ],
      soundSuggestions: [
        {
          suggestionId: "snd_1",
          category: "AMBIENT",
          title: "Mountain Ridge Wind",
          description: "Soft whistling wind across snow ridges",
          suggestedStartSec: 0.0,
          suggestedVolume: 0.4,
          loopEnabled: true,
          applied: false,
        },
      ],
    })
  );

  // Character voice preferences update succeeds
  const charRef = aliceDb.collection(`users/${ALICE_UID}/characters`).doc("char_voice_1");
  await assertSucceeds(
    charRef.set({
      ...validCharacterPayload(ALICE_UID, created),
      voiceLanguage: "Hindi",
      voiceId: "hi-in-x-hia-local",
      voicePitch: 0.95,
      voiceRate: 1.05,
    })
  );
  await assertSucceeds(
    charRef.update({
      voicePitch: 1.1,
      voiceRate: 0.9,
      updatedAt: updated,
    })
  );

  // Reject oversized dialogueLines (>40)
  const tooManyLines = Array.from({ length: 41 }, (_, i) => ({ lineId: `dlg_${i}`, text: "Hi" }));
  await assertFails(
    sceneRef.update({
      dialogueLines: tooManyLines,
      updatedAt: updated,
    })
  );
});

test("Phase 5 audioAssets collection: enforces ownership, MIME allowlist, 15MB cap, and rejects raw blobs", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const updated = new Date(Date.now() - 1000);

  const validAudioAsset = {
    userId: ALICE_UID,
    name: "Scene 1 Narration (ElevenLabs WAV)",
    category: "Narration",
    sourceType: "IMPORTED",
    storageState: "LOCAL_PERSISTENT",
    localFilePath: "/data/user/0/com.aistudio/files/motionstory_audio/aud_1.wav",
    mimeType: "audio/wav",
    fileSizeBytes: 2457600,
    durationSec: 12.4,
    language: "Hindi",
    voiceId: "",
    characterId: "",
    waveformPeaks: [0.1, 0.45, 0.82, 0.61, 0.25, 0.74],
    notes: "Imported narration recording",
    createdAt: created,
    updatedAt: created,
  };

  const audioRef = aliceDb.collection(`users/${ALICE_UID}/audioAssets`).doc("aud_1");
  await assertSucceeds(audioRef.set(validAudioAsset));
  await assertSucceeds(
    audioRef.update({
      name: "Scene 1 Narration Master",
      notes: "Trimmed intro silence",
      updatedAt: updated,
    })
  );

  // Cross-user isolation: Bob denied read/write/delete
  await assertFails(bobDb.collection(`users/${ALICE_UID}/audioAssets`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/audioAssets`).doc("aud_1").delete());

  // Reject unsupported MIME type (e.g. executable/script)
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/audioAssets`).doc("aud_bad_mime").set({
      ...validAudioAsset,
      mimeType: "application/javascript",
    })
  );

  // Reject oversized file (> 15 MB = 15728640 bytes)
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/audioAssets`).doc("aud_too_big").set({
      ...validAudioAsset,
      fileSizeBytes: 15728641,
    })
  );

  // Reject ghost raw audio blob field in Firestore
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/audioAssets`).doc("aud_blob").set({
      ...validAudioAsset,
      audioBase64: "UklGRiQAAABXQVZFZm10IBAAAAABAAEA",
    })
  );

  // Reject oversized waveformPeaks (> 48)
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/audioAssets`).doc("aud_peaks").set({
      ...validAudioAsset,
      waveformPeaks: Array.from({ length: 49 }, () => 0.5),
    })
  );

  await assertSucceeds(audioRef.delete());
});

test("Phase 6 motion graphics, subtitles, infographics, maps & transitions persistence succeed and enforce bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const updated = new Date(Date.now() - 1000);

  const projRef = aliceDb.collection(`users/${ALICE_UID}/projects`).doc("proj_p6");
  await assertSucceeds(projRef.set(validProjectPayload(ALICE_UID, created)));

  const sceneRef = aliceDb.collection(`users/${ALICE_UID}/projects/proj_p6/scenes`).doc("scene_p6_1");
  await assertSucceeds(
    sceneRef.set({
      ...validScenePayload(ALICE_UID, created, 0),
      motionGraphics: [
        {
          graphicId: "gfx_title_1",
          title: "Chapter I Title",
          category: "TITLE",
          templateType: "CHAPTER_TITLE",
          startSec: 0.2,
          durationSec: 4.5,
          primaryText: "CHAPTER I · THE HIGH PASSES",
          secondaryText: "HIMALAYAN ARCHIVES · 1842",
          entranceAnimation: "TYPEWRITER",
          exitAnimation: "FADE_OUT",
        },
        {
          graphicId: "gfx_bar_1",
          title: "Caravan Volume Chart",
          category: "INFOGRAPHIC",
          templateType: "INFOGRAPHIC",
          infographicType: "BAR_CHART",
          startSec: 1.0,
          durationSec: 5.0,
          dataPoints: [
            { id: "dp_1", label: "1840", value: 120.0, colorHex: "#F5A623" },
            { id: "dp_2", label: "1860", value: 245.0, colorHex: "#26D0CE" },
          ],
        },
        {
          graphicId: "gfx_map_1",
          title: "Northern Pass Route",
          category: "MAP",
          templateType: "MAP_GRAPHIC",
          startSec: 2.0,
          durationSec: 5.0,
          mapPresetStyle: "ARCHIVAL_PARCHMENT",
          mapMarkers: [{ markerId: "m1", label: "Leh", xNorm: 0.3, yNorm: 0.4 }],
          mapRoutes: [{ routeId: "r1", fromX: 0.3, fromY: 0.4, toX: 0.7, toY: 0.6, showArrow: true }],
        },
      ],
      subtitleCues: [
        {
          cueId: "sub_1",
          sceneId: "scene_p6_1",
          speakerName: "Narrator",
          text: "Long before highways crossed these valleys.",
          startSec: 0.0,
          endSec: 3.2,
          positionYNorm: 0.88,
        },
      ],
      subtitleStyle: {
        stylePreset: "DOCUMENTARY_CLEAN",
        fontFamilyKey: "PLUS_JAKARTA",
        fontSizeSp: 13.0,
        textColorHex: "#F2F4F8",
        backgroundColorHex: "#0A0C10",
        backgroundOpacity: 0.8,
        outlineEnabled: true,
        outlineColorHex: "#000000",
        alignment: "CENTER",
        verticalPosition: "BOTTOM",
      },
      outgoingTransition: {
        type: "CROSSFADE",
        durationSec: 1.0,
        direction: "LEFT",
        easing: "EASE_IN_OUT",
        feather: 0.25,
      },
      graphicSuggestions: [
        {
          suggestionId: "gs_1",
          category: "LOWER_THIRD",
          templateType: "LOWER_THIRD",
          title: "Introduce Dr. Aarav Mehta",
          rationale: "First on-screen appearance of lead historian.",
          primaryText: "Dr. Aarav Mehta",
          secondaryText: "Lead Historian · Himalayan Archives",
          suggestedStartSec: 0.5,
          suggestedDurationSec: 4.0,
          applied: false,
        },
      ],
    })
  );

  // Partial update of outgoingTransition & subtitleCues succeeds
  await assertSucceeds(
    sceneRef.update({
      outgoingTransition: {
        type: "WIPE",
        durationSec: 1.2,
        direction: "RIGHT",
        easing: "EASE_OUT",
        feather: 0.2,
      },
      updatedAt: updated,
    })
  );

  // Reject oversized motionGraphics (> 30)
  const tooManyGraphics = Array.from({ length: 31 }, (_, i) => ({ graphicId: `gfx_${i}` }));
  await assertFails(
    sceneRef.update({
      motionGraphics: tooManyGraphics,
      updatedAt: updated,
    })
  );

  // Reject oversized subtitleCues (> 80)
  const tooManySubs = Array.from({ length: 81 }, (_, i) => ({ cueId: `sub_${i}`, text: "Line" }));
  await assertFails(
    sceneRef.update({
      subtitleCues: tooManySubs,
      updatedAt: updated,
    })
  );
});

test("Phase 6 MotionGraphic template asset persistence & legacy Phase 1-5 backward compatibility", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Saving reusable graphic template in Asset Library succeeds
  const tplAssetRef = aliceDb.collection(`users/${ALICE_UID}/assets`).doc("asset_gfx_tpl_1");
  await assertSucceeds(
    tplAssetRef.set({
      ...validAssetPayload(ALICE_UID, created),
      name: "Archival Lower Third Preset",
      category: "MotionGraphic",
      presetKey: "LOWER_THIRD",
      graphicPayloadJson: JSON.stringify({
        graphicId: "tpl_l3",
        category: "LOWER_THIRD",
        primaryText: "Dr. Aarav Mehta",
        secondaryText: "Lead Historian",
      }),
    })
  );

  // Reject oversized graphicPayloadJson (> 32,000 chars)
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/assets`).doc("asset_gfx_huge").set({
      ...validAssetPayload(ALICE_UID, created),
      category: "MotionGraphic",
      graphicPayloadJson: "x".repeat(32001),
    })
  );

  // Legacy Phase 1-5 scene document without any Phase 6 fields still succeeds cleanly
  const projRef = aliceDb.collection(`users/${ALICE_UID}/projects`).doc("proj_legacy_ok");
  await assertSucceeds(projRef.set(validProjectPayload(ALICE_UID, created)));
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/projects/proj_legacy_ok/scenes`).doc("scene_legacy").set(
      validScenePayload(ALICE_UID, created, 0)
    )
  );
});






