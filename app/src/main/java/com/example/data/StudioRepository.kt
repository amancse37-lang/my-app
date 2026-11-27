package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class StudioRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeProjects(userId: String): Flow<List<StudioProject>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Authenticated UID mismatch." }
        val path = "users/$uid/projects"
        emitAll(
            db.collection("users").document(uid).collection("projects")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        StudioProject::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeScenes(userId: String, projectId: String): Flow<List<StudioScene>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Authenticated UID mismatch." }
        require(isValidDocId(projectId)) { "Invalid Project ID." }
        val path = "users/$uid/projects/$projectId/scenes"
        emitAll(
            db.collection("users").document(uid)
                .collection("projects").document(projectId)
                .collection("scenes")
                .orderBy("orderIndex", Query.Direction.ASCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        StudioScene::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeCharacters(userId: String): Flow<List<StudioCharacter>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Authenticated UID mismatch." }
        val path = "users/$uid/characters"
        emitAll(
            db.collection("users").document(uid).collection("characters")
                .orderBy("charCode", Query.Direction.ASCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        StudioCharacter::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeCharacterReferences(
        userId: String,
        characterId: String
    ): Flow<List<CharacterReferenceSheet>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Authenticated UID mismatch." }
        require(isValidDocId(characterId)) { "Invalid Character ID." }
        val path = "users/$uid/characters/$characterId/references"
        emitAll(
            db.collection("users").document(uid)
                .collection("characters").document(characterId)
                .collection("references")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CharacterReferenceSheet::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeAssets(userId: String): Flow<List<StudioAsset>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Authenticated UID mismatch." }
        val path = "users/$uid/assets"
        emitAll(
            db.collection("users").document(uid).collection("assets")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        StudioAsset::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeAudioAssets(userId: String): Flow<List<StudioAudioAsset>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Authenticated UID mismatch." }
        val path = "users/$uid/audioAssets"
        emitAll(
            db.collection("users").document(uid).collection("audioAssets")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        StudioAudioAsset::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun seedStarterWorkspaceIfEmpty(userId: String): Result<String?> {
        var path = "users/$userId/projects"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Authenticated UID mismatch." }

            val existingProjects = db.collection("users").document(uid)
                .collection("projects").limit(1).get().await()
            if (!existingProjects.isEmpty) {
                return Result.success(existingProjects.documents.first().id)
            }

            // 1. Seed starter reusable characters with full 16-field identity profiles
            val charColl = db.collection("users").document(uid).collection("characters")
            val char1Ref = charColl.document("chr_narrator_01")
            val char2Ref = charColl.document("chr_cartographer_02")

            val char1 = StudioCharacter(
                charCode = "CHR-01",
                name = "Dr. Aarav Mehta",
                role = "Lead Historian & Narrator",
                visualAge = "Mid 40s",
                faceDescription = "Warm brown eyes, neatly trimmed salt-and-pepper beard, gentle crow's feet, thoughtful brow.",
                hairStyle = "Slightly wavy swept-back medium-short hair",
                hairColor = "Dark charcoal with silver temples",
                clothing = "Structured herringbone tweed jacket over dark crewneck sweater with patterned wool scarf",
                clothingColors = "Charcoal, Ochre, Deep Rust",
                bodyProportions = "Medium build, upright scholar posture, expressive hands",
                accessories = "Round tortoise-shell spectacles, brass pocket compass",
                personality = "Reflective, eloquent, empathetic archival storyteller",
                appearanceNotes = "Maintain identical round tortoise-shell glasses, greying temples, charcoal tweed jacket, and ochre scarf across all scenes.",
                wardrobePalette = "Charcoal, Ochre, Deep Rust",
                expressionDefault = "Thinking",
                visualStyle = "Painterly 2D Documentary",
                referenceImageBase64 = "",
                avatarPreset = "narrator",
                accentColorHex = "#F5A623"
            )
            val char2 = StudioCharacter(
                charCode = "CHR-02",
                name = "Meera Sen",
                role = "Archival Cartographer",
                visualAge = "Early 30s",
                faceDescription = "Expressive dark almond eyes, warm bronze skin, focused intelligent gaze.",
                hairStyle = "Low braided bun with loose side strands",
                hairColor = "Deep espresso black",
                clothing = "Handloom cotton kurta with draped woven indigo shawl and leather satchel",
                clothingColors = "Indigo, Slate, Warm Sand",
                bodyProportions = "Slender build, poised alert stance",
                accessories = "Brass fountain pen, leather-bound map journal, small silver studs",
                personality = "Observant, determined, detail-oriented field researcher",
                appearanceNotes = "Keep indigo handloom shawl, low braided bun, and leather map satchel identical across all shots.",
                wardrobePalette = "Indigo, Slate, Warm Sand",
                expressionDefault = "Calm",
                visualStyle = "Painterly 2D Documentary",
                referenceImageBase64 = "",
                avatarPreset = "scholar",
                accentColorHex = "#26D0CE"
            )

            // 2. Seed starter assets & locations
            val assetColl = db.collection("users").document(uid).collection("assets")
            val starterAssets = listOf(
                "loc_himalayas" to StudioAsset(
                    name = "Himalayan Dawn Valley",
                    category = "Location",
                    tags = "mountains,dawn,establishing,parallax",
                    notes = "Multi-plane dawn mist over snow ridges for Chapter I.",
                    presetKey = "himalayas",
                    localUri = "",
                    durationLabel = "1920x1080"
                ),
                "loc_old_delhi" to StudioAsset(
                    name = "Old Delhi Heritage Lane",
                    category = "Location",
                    tags = "bazaar,twilight,lanterns,historic",
                    notes = "Carved sandstone arches with warm brass lantern glow.",
                    presetKey = "old_delhi",
                    localUri = "",
                    durationLabel = "1920x1080"
                ),
                "loc_archive_study" to StudioAsset(
                    name = "Historian's Archive Study",
                    category = "Background",
                    tags = "interior,maps,lamp,night",
                    notes = "Dimly lit archive study with parchment maps and green lamp.",
                    presetKey = "archive_study",
                    localUri = "",
                    durationLabel = "1920x1080"
                ),
                "prop_brass_compass" to StudioAsset(
                    name = "Antique Brass Compass",
                    category = "Prop",
                    tags = "navigation,antique,closeup",
                    notes = "Foreground cutaway object for transition beats.",
                    presetKey = "prop_compass",
                    localUri = "",
                    durationLabel = "PNG Layer"
                ),
                "audio_strings_drone" to StudioAsset(
                    name = "Himalayan Strings Drone",
                    category = "Audio",
                    tags = "score,strings,atmospheric",
                    notes = "Slow minor-key documentary opening bed.",
                    presetKey = "music_drone",
                    localUri = "",
                    durationLabel = "01:45"
                ),
                "sfx_wind_pass" to StudioAsset(
                    name = "High Altitude Wind",
                    category = "SFX",
                    tags = "wind,mountain,ambience",
                    notes = "Subtle stereo breeze under establishing narration.",
                    presetKey = "sfx_wind",
                    localUri = "",
                    durationLabel = "00:18"
                ),
                "mg_chapter_card" to StudioAsset(
                    name = "Documentary Chapter Card",
                    category = "MotionGraphic",
                    tags = "title,chapter,lower-third",
                    notes = "Amber rule accent with Space Grotesk documentary title.",
                    presetKey = "Chapter Card",
                    localUri = "",
                    durationLabel = "00:05"
                )
            )

            // 3. Seed starter documentary project & 3 multi-character cinematic scenes
            val projRef = db.collection("users").document(uid)
                .collection("projects").document("proj_silk_road_01")
            path = projRef.path

            val starterProject = StudioProject(
                name = "Echoes of the Silk Road",
                scriptText = StudioPresets.sampleEnglishScript,
                scriptLanguage = "Bilingual",
                aspectRatio = "16:9",
                fps = 24L,
                resolution = "1080p"
            )

            val starterScenes = listOf(
                "scene_01" to StudioScene(
                    orderIndex = 0L,
                    title = "Himalayan Dawn Establishing",
                    description = "Wide establishing parallax over snow-capped Himalayan trade passes as Dr. Aarav points north and Meera listens.",
                    visualSummary = "Golden dawn sunbeams over slate-blue valleys and mountain hamlet.",
                    narrationText = "Long before highways crossed these valleys, every mountain pass carried ink, silk, and human memory.",
                    subtitleText = "Long before highways crossed these valleys, every pass carried human memory.",
                    durationSec = 8.0,
                    backgroundPreset = "himalayas",
                    backgroundTintHex = "#1B2838",
                    characterId = char1Ref.id,
                    characterCast = listOf(
                        SceneCharacterPlacement(
                            characterId = char1Ref.id,
                            expression = "Thinking",
                            action = "Pointing",
                            poseView = "Three-Quarter View",
                            posX = -110.0,
                            posY = 18.0,
                            scale = 1.0,
                            rotationDeg = 0.0
                        ),
                        SceneCharacterPlacement(
                            characterId = char2Ref.id,
                            expression = "Calm",
                            action = "Listening",
                            poseView = "Side View",
                            posX = 110.0,
                            posY = 20.0,
                            scale = 0.94,
                            rotationDeg = 0.0
                        )
                    ),
                    posX = -110.0,
                    posY = 18.0,
                    scale = 1.0,
                    rotationDeg = 0.0,
                    animationStyle = "Subtle Drift",
                    cameraMovement = "Slow Push In",
                    cameraZoom = 1.15,
                    effectPreset = "Film Grain",
                    effectIntensity = 0.35,
                    motionGraphicTitle = "CHAPTER I · THE HIGH PASSES (1842)",
                    motionGraphicStyle = "Chapter Card",
                    musicCue = "Himalayan Strings Drone",
                    sfxCue = "High Altitude Wind",
                    objectProp = "Brass Compass"
                ),
                "scene_02" to StudioScene(
                    orderIndex = 1L,
                    title = "Old Delhi Twilight Bazaar",
                    description = "Camera tracks down lantern-lit sandstone arches as Meera walks through the heritage lane.",
                    visualSummary = "Warm amber lanterns glowing against indigo carved arches.",
                    narrationText = "In the narrow lanes of Shahjahanabad, calligraphers and mapmakers traded tales brought down from the northern frontier.",
                    subtitleText = "In the narrow lanes of Shahjahanabad, calligraphers traded frontier tales.",
                    durationSec = 9.0,
                    backgroundPreset = "old_delhi",
                    backgroundTintHex = "#11262A",
                    characterId = char2Ref.id,
                    characterCast = listOf(
                        SceneCharacterPlacement(
                            characterId = char2Ref.id,
                            expression = "Surprised",
                            action = "Looking around",
                            poseView = "Full Body",
                            posX = 85.0,
                            posY = 15.0,
                            scale = 0.95,
                            rotationDeg = 0.0
                        )
                    ),
                    posX = 85.0,
                    posY = 15.0,
                    scale = 0.95,
                    rotationDeg = 0.0,
                    animationStyle = "Expressive Gesture",
                    cameraMovement = "Parallax Pan Left",
                    cameraZoom = 1.18,
                    effectPreset = "Cinematic Vignette",
                    effectIntensity = 0.45,
                    motionGraphicTitle = "SHAHJAHANABAD · 1848",
                    motionGraphicStyle = "Date Stamp",
                    musicCue = "Sitar & Cello Documentary Pulse",
                    sfxCue = "Bazaar Twilight Footsteps",
                    objectProp = "Hanging Brass Lantern"
                ),
                "scene_03" to StudioScene(
                    orderIndex = 2L,
                    title = "The Historian's Archive Desk",
                    description = "Close documentary framing over parchment maps as Dr. Aarav and Meera study faded borders.",
                    visualSummary = "Warm pools of light over hand-drawn maps and vintage volumes.",
                    narrationText = "When we trace these faded borders today, we are not just studying geography — we are listening to the voices that survived.",
                    subtitleText = "When we trace faded borders, we listen to the voices that survived.",
                    durationSec = 10.0,
                    backgroundPreset = "archive_study",
                    backgroundTintHex = "#2A1E17",
                    characterId = char1Ref.id,
                    characterCast = listOf(
                        SceneCharacterPlacement(
                            characterId = char1Ref.id,
                            expression = "Determined",
                            action = "Talking",
                            poseView = "Portrait",
                            posX = -90.0,
                            posY = 12.0,
                            scale = 1.05,
                            rotationDeg = 0.0
                        ),
                        SceneCharacterPlacement(
                            characterId = char2Ref.id,
                            expression = "Thinking",
                            action = "Holding objects",
                            poseView = "Three-Quarter View",
                            posX = 95.0,
                            posY = 14.0,
                            scale = 0.98,
                            rotationDeg = 0.0
                        )
                    ),
                    posX = -90.0,
                    posY = 12.0,
                    scale = 1.05,
                    rotationDeg = 0.0,
                    animationStyle = "Interview Talk",
                    cameraMovement = "Ken Burns Pull Out",
                    cameraZoom = 1.22,
                    effectPreset = "Warm Sepia",
                    effectIntensity = 0.30,
                    motionGraphicTitle = "Dr. Aarav Mehta & Meera Sen · Historians",
                    motionGraphicStyle = "Lower Third",
                    musicCue = "Archival Piano Reflection",
                    sfxCue = "Paper Map Rustle & Clock Tick",
                    objectProp = "Magnifying Glass"
                )
            )

            val batch = db.batch()
            batch.set(char1Ref, characterCreateMap(uid, char1))
            batch.set(char2Ref, characterCreateMap(uid, char2))
            starterAssets.forEach { (assetId, asset) ->
                batch.set(assetColl.document(assetId), assetCreateMap(uid, asset))
            }
            batch.set(projRef, projectCreateMap(uid, starterProject))
            starterScenes.forEach { (sceneId, scene) ->
                batch.set(projRef.collection("scenes").document(sceneId), sceneCreateMap(uid, scene))
            }
            batch.commit().await()
            Result.success(projRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun createProject(name: String, language: String = "Bilingual"): Result<String> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/projects"
        return try {
            val uid = requireUserId()
            val cleanName = name.trim().take(120).ifBlank { "Untitled Documentary" }
            val projRef = db.collection("users").document(uid).collection("projects").document()
            path = projRef.path

            val project = StudioProject(
                name = cleanName,
                scriptText = "",
                scriptLanguage = if (language in StudioPresets.languages) language else "Bilingual",
                aspectRatio = "16:9",
                fps = 24L,
                resolution = "1080p"
            )
            val initialScene = StudioScene(
                orderIndex = 0L,
                title = "Opening Establishing Shot",
                description = "Opening documentary frame introducing the story setting.",
                visualSummary = "Wide atmospheric 2D animated establishing shot.",
                narrationText = "",
                subtitleText = "",
                durationSec = 6.0,
                backgroundPreset = "himalayas"
            )
            val sceneRef = projRef.collection("scenes").document()
            val batch = db.batch()
            batch.set(projRef, projectCreateMap(uid, project))
            batch.set(sceneRef, sceneCreateMap(uid, initialScene))
            batch.commit().await()
            Result.success(projRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveProjectWithScenes(
        project: StudioProject,
        scenes: List<StudioScene>
    ): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/projects/${project.id}"
        return try {
            val uid = requireUserId()
            require(isValidDocId(project.id)) { "Invalid Project ID." }
            val projRef = db.collection("users").document(uid)
                .collection("projects").document(project.id)
            path = projRef.path

            val scenesColl = projRef.collection("scenes")
            val existingDocs = scenesColl.get().await()
            val existingIds = existingDocs.documents.map { it.id }.toSet()

            val normalizedScenes = scenes.mapIndexed { index, scene ->
                scene.copy(orderIndex = index.toLong())
            }
            val retainedIds = normalizedScenes.mapNotNull { it.id.takeIf(String::isNotBlank) }.toSet()

            val batch = db.batch()
            batch.update(projRef, projectUpdateMap(project))

            (existingIds - retainedIds).forEach { removedId ->
                batch.delete(scenesColl.document(removedId))
            }

            normalizedScenes.forEach { scene ->
                val isExisting = scene.id.isNotBlank() && scene.id in existingIds
                if (isExisting) {
                    batch.update(scenesColl.document(scene.id), sceneUpdateMap(scene))
                } else {
                    val targetRef = if (scene.id.isNotBlank() && isValidDocId(scene.id)) {
                        scenesColl.document(scene.id)
                    } else {
                        scenesColl.document()
                    }
                    batch.set(targetRef, sceneCreateMap(uid, scene))
                }
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteProject(projectId: String): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/projects/$projectId"
        return try {
            val uid = requireUserId()
            require(isValidDocId(projectId)) { "Invalid Project ID." }
            val projRef = db.collection("users").document(uid)
                .collection("projects").document(projectId)
            path = projRef.path
            val sceneDocs = projRef.collection("scenes").get().await()
            val batch = db.batch()
            sceneDocs.documents.forEach { doc -> batch.delete(doc.reference) }
            batch.delete(projRef)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveCharacter(character: StudioCharacter): Result<String> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/characters"
        return try {
            val uid = requireUserId()
            val coll = db.collection("users").document(uid).collection("characters")
            val isUpdate = character.id.isNotBlank()
            if (isUpdate) {
                require(isValidDocId(character.id)) { "Invalid Character ID." }
            }
            val docRef = if (isUpdate) coll.document(character.id) else coll.document()
            path = docRef.path
            if (isUpdate) {
                docRef.update(characterUpdateMap(character)).await()
            } else {
                docRef.set(characterCreateMap(uid, character)).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(
                e,
                if (character.id.isNotBlank()) OperationType.UPDATE else OperationType.CREATE,
                path
            )
            Result.failure(e)
        }
    }

    suspend fun saveCharacterReference(
        characterId: String,
        reference: CharacterReferenceSheet,
        alsoSetAsPrimary: Boolean
    ): Result<String> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/characters/$characterId/references"
        return try {
            val uid = requireUserId()
            require(isValidDocId(characterId)) { "Invalid Character ID." }
            require(reference.imageBase64.length in 16..320000) {
                "Reference image payload must be between 16 and 320,000 characters."
            }
            val charRef = db.collection("users").document(uid)
                .collection("characters").document(characterId)
            val sheetRef = charRef.collection("references").document()
            path = sheetRef.path

            val batch = db.batch()
            batch.set(sheetRef, characterRefCreateMap(uid, reference))
            if (alsoSetAsPrimary) {
                batch.update(
                    charRef,
                    mapOf(
                        "referenceImageBase64" to reference.imageBase64.take(320000),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(sheetRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun setPrimaryCharacterReference(
        characterId: String,
        imageBase64: String
    ): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/characters/$characterId"
        return try {
            val uid = requireUserId()
            require(isValidDocId(characterId)) { "Invalid Character ID." }
            val charRef = db.collection("users").document(uid)
                .collection("characters").document(characterId)
            path = charRef.path
            charRef.update(
                mapOf(
                    "referenceImageBase64" to imageBase64.take(320000),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCharacterReference(
        characterId: String,
        referenceId: String
    ): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/characters/$characterId/references/$referenceId"
        return try {
            val uid = requireUserId()
            require(isValidDocId(characterId) && isValidDocId(referenceId)) { "Invalid reference ID." }
            val docRef = db.collection("users").document(uid)
                .collection("characters").document(characterId)
                .collection("references").document(referenceId)
            path = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCharacter(characterId: String): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/characters/$characterId"
        return try {
            val uid = requireUserId()
            require(isValidDocId(characterId)) { "Invalid Character ID." }
            val docRef = db.collection("users").document(uid)
                .collection("characters").document(characterId)
            path = docRef.path
            val refs = docRef.collection("references").get().await()
            val batch = db.batch()
            refs.documents.forEach { batch.delete(it.reference) }
            batch.delete(docRef)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveAsset(asset: StudioAsset): Result<String> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/assets"
        return try {
            val uid = requireUserId()
            val coll = db.collection("users").document(uid).collection("assets")
            val isUpdate = asset.id.isNotBlank()
            if (isUpdate) {
                require(isValidDocId(asset.id)) { "Invalid Asset ID." }
            }
            val docRef = if (isUpdate) coll.document(asset.id) else coll.document()
            path = docRef.path
            if (isUpdate) {
                docRef.update(assetUpdateMap(asset)).await()
            } else {
                docRef.set(assetCreateMap(uid, asset)).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(
                e,
                if (asset.id.isNotBlank()) OperationType.UPDATE else OperationType.CREATE,
                path
            )
            Result.failure(e)
        }
    }

    suspend fun deleteAsset(assetId: String): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/assets/$assetId"
        return try {
            val uid = requireUserId()
            require(isValidDocId(assetId)) { "Invalid Asset ID." }
            val docRef = db.collection("users").document(uid)
                .collection("assets").document(assetId)
            path = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveAudioAsset(asset: StudioAudioAsset): Result<String> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/audioAssets"
        return try {
            val uid = requireUserId()
            val coll = db.collection("users").document(uid).collection("audioAssets")
            val isUpdate = asset.id.isNotBlank()
            if (isUpdate) {
                require(isValidDocId(asset.id)) { "Invalid Audio Asset ID." }
            }
            val docRef = if (isUpdate) coll.document(asset.id) else coll.document()
            path = docRef.path
            if (isUpdate) {
                docRef.update(audioAssetUpdateMap(asset)).await()
            } else {
                docRef.set(audioAssetCreateMap(uid, asset)).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(
                e,
                if (asset.id.isNotBlank()) OperationType.UPDATE else OperationType.CREATE,
                path
            )
            Result.failure(e)
        }
    }

    suspend fun deleteAudioAsset(audioAssetId: String): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/audioAssets/$audioAssetId"
        return try {
            val uid = requireUserId()
            require(isValidDocId(audioAssetId)) { "Invalid Audio Asset ID." }
            val docRef = db.collection("users").document(uid)
                .collection("audioAssets").document(audioAssetId)
            path = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- STRICT RULE-ALIGNED WRITE PAYLOAD BUILDERS (NO 'id' FIELD) ---

    private fun projectCreateMap(uid: String, p: StudioProject): Map<String, Any> = mapOf(
        "userId" to uid,
        "name" to p.name.trim().take(120).ifBlank { "Untitled Documentary" },
        "scriptText" to p.scriptText.take(25000),
        "scriptLanguage" to if (p.scriptLanguage in StudioPresets.languages) p.scriptLanguage else "Bilingual",
        "aspectRatio" to if (p.aspectRatio in StudioPresets.aspectRatios) p.aspectRatio else "16:9",
        "fps" to if (p.fps in StudioPresets.frameRates) p.fps else 24L,
        "resolution" to if (p.resolution in StudioPresets.resolutions) p.resolution else "1080p",
        "storyAnalysis" to storyAnalysisToMap(p.storyAnalysis),
        "projectAudioClips" to audioClipsToMaps(p.projectAudioClips, maxItems = 20),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun projectUpdateMap(p: StudioProject): Map<String, Any> = mapOf(
        "name" to p.name.trim().take(120).ifBlank { "Untitled Documentary" },
        "scriptText" to p.scriptText.take(25000),
        "scriptLanguage" to if (p.scriptLanguage in StudioPresets.languages) p.scriptLanguage else "Bilingual",
        "aspectRatio" to if (p.aspectRatio in StudioPresets.aspectRatios) p.aspectRatio else "16:9",
        "fps" to if (p.fps in StudioPresets.frameRates) p.fps else 24L,
        "resolution" to if (p.resolution in StudioPresets.resolutions) p.resolution else "1080p",
        "storyAnalysis" to storyAnalysisToMap(p.storyAnalysis),
        "projectAudioClips" to audioClipsToMaps(p.projectAudioClips, maxItems = 20),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    // Pure payload helpers live on companion object for direct JVM unit testing.

    private fun sceneCreateMap(uid: String, s: StudioScene): Map<String, Any> {
        val cast = s.effectiveCast()
        val primary = cast.firstOrNull()
        return mapOf(
            "userId" to uid,
            "orderIndex" to s.orderIndex.coerceIn(0L, 500L),
            "title" to s.title.trim().take(120).ifBlank { "Untitled Scene" },
            "description" to s.description.take(2000),
            "visualSummary" to s.visualSummary.take(1000),
            "narrationText" to s.narrationText.take(3000),
            "subtitleText" to s.subtitleText.take(1000),
            "durationSec" to s.durationSec.coerceIn(1.0, 300.0),
            "backgroundPreset" to s.backgroundPreset.take(128),
            "backgroundTintHex" to s.backgroundTintHex.take(16),
            "characterId" to (primary?.characterId ?: s.characterId).take(128),
            "characterCast" to castListToMaps(cast),
            "sceneElements" to sceneElementsToMaps(s.sceneElements),
            "layerStates" to layerStatesToMaps(s.layerStates),
            "animationKeyframes" to animationKeyframesToMaps(s.animationKeyframes),
            "characterActingCues" to actingCuesToMaps(s.characterActingCues),
            "cinematics" to cinematicsToMap(s.cinematics),
            "scenePlan" to scenePlanToMap(s.scenePlan),
            "dialogueLines" to dialogueLinesToMaps(s.dialogueLines),
            "audioClips" to audioClipsToMaps(s.audioClips),
            "lipSyncCues" to lipSyncCuesToMaps(s.lipSyncCues),
            "soundSuggestions" to soundSuggestionsToMaps(s.soundSuggestions),
            "motionGraphics" to motionGraphicsToMaps(s.motionGraphics),
            "subtitleCues" to subtitleCuesToMaps(s.subtitleCues),
            "subtitleStyle" to subtitleStyleToMap(s.subtitleStyle),
            "outgoingTransition" to sceneTransitionToMap(s.outgoingTransition),
            "graphicSuggestions" to graphicSuggestionsToMaps(s.graphicSuggestions),
            "posX" to (primary?.posX ?: s.posX).coerceIn(-1000.0, 1000.0),
            "posY" to (primary?.posY ?: s.posY).coerceIn(-1000.0, 1000.0),
            "scale" to (primary?.scale ?: s.scale).coerceIn(0.2, 3.0),
            "rotationDeg" to (primary?.rotationDeg ?: s.rotationDeg).coerceIn(-360.0, 360.0),
            "animationStyle" to s.animationStyle.take(64),
            "cameraMovement" to s.cameraMovement.take(64),
            "cameraZoom" to s.cameraZoom.coerceIn(0.5, 3.0),
            "effectPreset" to s.effectPreset.take(64),
            "effectIntensity" to s.effectIntensity.coerceIn(0.0, 1.0),
            "motionGraphicTitle" to s.motionGraphicTitle.take(120),
            "motionGraphicStyle" to s.motionGraphicStyle.take(64),
            "musicCue" to s.musicCue.take(120),
            "sfxCue" to s.sfxCue.take(120),
            "objectProp" to s.objectProp.take(120),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    private fun sceneUpdateMap(s: StudioScene): Map<String, Any> {
        val cast = s.effectiveCast()
        val primary = cast.firstOrNull()
        return mapOf(
            "orderIndex" to s.orderIndex.coerceIn(0L, 500L),
            "title" to s.title.trim().take(120).ifBlank { "Untitled Scene" },
            "description" to s.description.take(2000),
            "visualSummary" to s.visualSummary.take(1000),
            "narrationText" to s.narrationText.take(3000),
            "subtitleText" to s.subtitleText.take(1000),
            "durationSec" to s.durationSec.coerceIn(1.0, 300.0),
            "backgroundPreset" to s.backgroundPreset.take(128),
            "backgroundTintHex" to s.backgroundTintHex.take(16),
            "characterId" to (primary?.characterId ?: s.characterId).take(128),
            "characterCast" to castListToMaps(cast),
            "sceneElements" to sceneElementsToMaps(s.sceneElements),
            "layerStates" to layerStatesToMaps(s.layerStates),
            "animationKeyframes" to animationKeyframesToMaps(s.animationKeyframes),
            "characterActingCues" to actingCuesToMaps(s.characterActingCues),
            "cinematics" to cinematicsToMap(s.cinematics),
            "scenePlan" to scenePlanToMap(s.scenePlan),
            "dialogueLines" to dialogueLinesToMaps(s.dialogueLines),
            "audioClips" to audioClipsToMaps(s.audioClips),
            "lipSyncCues" to lipSyncCuesToMaps(s.lipSyncCues),
            "soundSuggestions" to soundSuggestionsToMaps(s.soundSuggestions),
            "motionGraphics" to motionGraphicsToMaps(s.motionGraphics),
            "subtitleCues" to subtitleCuesToMaps(s.subtitleCues),
            "subtitleStyle" to subtitleStyleToMap(s.subtitleStyle),
            "outgoingTransition" to sceneTransitionToMap(s.outgoingTransition),
            "graphicSuggestions" to graphicSuggestionsToMaps(s.graphicSuggestions),
            "posX" to (primary?.posX ?: s.posX).coerceIn(-1000.0, 1000.0),
            "posY" to (primary?.posY ?: s.posY).coerceIn(-1000.0, 1000.0),
            "scale" to (primary?.scale ?: s.scale).coerceIn(0.2, 3.0),
            "rotationDeg" to (primary?.rotationDeg ?: s.rotationDeg).coerceIn(-360.0, 360.0),
            "animationStyle" to s.animationStyle.take(64),
            "cameraMovement" to s.cameraMovement.take(64),
            "cameraZoom" to s.cameraZoom.coerceIn(0.5, 3.0),
            "effectPreset" to s.effectPreset.take(64),
            "effectIntensity" to s.effectIntensity.coerceIn(0.0, 1.0),
            "motionGraphicTitle" to s.motionGraphicTitle.take(120),
            "motionGraphicStyle" to s.motionGraphicStyle.take(64),
            "musicCue" to s.musicCue.take(120),
            "sfxCue" to s.sfxCue.take(120),
            "objectProp" to s.objectProp.take(120),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    private fun characterCreateMap(uid: String, c: StudioCharacter): Map<String, Any> {
        val colors = c.clothingColors.ifBlank { c.wardrobePalette }.take(160)
        return mapOf(
            "userId" to uid,
            "charCode" to c.charCode.trim().take(32).ifBlank { "CHR-01" },
            "name" to c.name.trim().take(100).ifBlank { "Unnamed Character" },
            "role" to c.role.take(120),
            "visualAge" to c.visualAge.take(64),
            "faceDescription" to c.faceDescription.take(600),
            "hairStyle" to c.hairStyle.take(160),
            "hairColor" to c.hairColor.take(80),
            "clothing" to c.clothing.take(400),
            "clothingColors" to colors,
            "bodyProportions" to c.bodyProportions.take(240),
            "accessories" to c.accessories.take(300),
            "personality" to c.personality.take(400),
            "appearanceNotes" to c.appearanceNotes.take(1500),
            "wardrobePalette" to colors,
            "expressionDefault" to c.expressionDefault.take(64).ifBlank { "Calm" },
            "visualStyle" to c.visualStyle.take(100).ifBlank { "Painterly 2D Documentary" },
            "referenceImageBase64" to c.referenceImageBase64.take(320000),
            "avatarPreset" to c.avatarPreset.take(64),
            "accentColorHex" to c.accentColorHex.take(16),
            "voiceLanguage" to c.voiceLanguage.take(32).ifBlank { "English" },
            "voiceId" to c.voiceId.take(160),
            "voicePitch" to c.voicePitch.let { if (it.isNaN() || it.isInfinite()) 1.0 else it.coerceIn(0.5, 2.0) },
            "voiceRate" to c.voiceRate.let { if (it.isNaN() || it.isInfinite()) 1.0 else it.coerceIn(0.5, 2.0) },
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    private fun characterUpdateMap(c: StudioCharacter): Map<String, Any> {
        val colors = c.clothingColors.ifBlank { c.wardrobePalette }.take(160)
        return mapOf(
            "charCode" to c.charCode.trim().take(32).ifBlank { "CHR-01" },
            "name" to c.name.trim().take(100).ifBlank { "Unnamed Character" },
            "role" to c.role.take(120),
            "visualAge" to c.visualAge.take(64),
            "faceDescription" to c.faceDescription.take(600),
            "hairStyle" to c.hairStyle.take(160),
            "hairColor" to c.hairColor.take(80),
            "clothing" to c.clothing.take(400),
            "clothingColors" to colors,
            "bodyProportions" to c.bodyProportions.take(240),
            "accessories" to c.accessories.take(300),
            "personality" to c.personality.take(400),
            "appearanceNotes" to c.appearanceNotes.take(1500),
            "wardrobePalette" to colors,
            "expressionDefault" to c.expressionDefault.take(64).ifBlank { "Calm" },
            "visualStyle" to c.visualStyle.take(100).ifBlank { "Painterly 2D Documentary" },
            "referenceImageBase64" to c.referenceImageBase64.take(320000),
            "avatarPreset" to c.avatarPreset.take(64),
            "accentColorHex" to c.accentColorHex.take(16),
            "voiceLanguage" to c.voiceLanguage.take(32).ifBlank { "English" },
            "voiceId" to c.voiceId.take(160),
            "voicePitch" to c.voicePitch.let { if (it.isNaN() || it.isInfinite()) 1.0 else it.coerceIn(0.5, 2.0) },
            "voiceRate" to c.voiceRate.let { if (it.isNaN() || it.isInfinite()) 1.0 else it.coerceIn(0.5, 2.0) },
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    private fun characterRefCreateMap(uid: String, r: CharacterReferenceSheet): Map<String, Any> = mapOf(
        "userId" to uid,
        "viewType" to r.viewType.take(64).ifBlank { "Front View" },
        "expression" to r.expression.take(64).ifBlank { "Neutral" },
        "poseAction" to r.poseAction.take(80).ifBlank { "Standing" },
        "visualStyle" to r.visualStyle.take(100).ifBlank { "Painterly 2D Documentary" },
        "promptSummary" to r.promptSummary.take(1000),
        "imageBase64" to r.imageBase64.take(320000),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun assetCreateMap(uid: String, a: StudioAsset): Map<String, Any> {
        return buildMap {
            put("userId", uid)
            putAll(assetFieldsToMap(a))
            put("createdAt", FieldValue.serverTimestamp())
            put("updatedAt", FieldValue.serverTimestamp())
        }
    }

    private fun assetUpdateMap(a: StudioAsset): Map<String, Any> {
        return buildMap {
            putAll(assetFieldsToMap(a))
            put("updatedAt", FieldValue.serverTimestamp())
        }
    }

    private fun audioAssetCreateMap(uid: String, a: StudioAudioAsset): Map<String, Any> {
        return buildMap {
            put("userId", uid)
            putAll(audioAssetFieldsToMap(a))
            put("createdAt", FieldValue.serverTimestamp())
            put("updatedAt", FieldValue.serverTimestamp())
        }
    }

    private fun audioAssetUpdateMap(a: StudioAudioAsset): Map<String, Any> {
        return buildMap {
            putAll(audioAssetFieldsToMap(a))
            put("updatedAt", FieldValue.serverTimestamp())
        }
    }

    private fun isValidDocId(id: String): Boolean {
        return id.length in 1..128 && id.matches(Regex("^[a-zA-Z0-9_\\-]+$"))
    }

    companion object {
        private val validAssetCategories = setOf(
            "Background", "Location", "Character", "Prop", "Image", "Audio", "SFX", "MotionGraphic"
        )

        fun assetFieldsToMap(a: StudioAsset): Map<String, Any> {
            return mapOf(
                "name" to a.name.trim().take(120).ifBlank { "Studio Asset" },
                "category" to if (a.category in validAssetCategories) a.category else "Background",
                "tags" to a.tags.take(200),
                "notes" to a.notes.take(1000),
                "presetKey" to a.presetKey.take(128),
                "localUri" to sanitizeMediaUriString(a.localUri).take(500),
                "durationLabel" to a.durationLabel.take(32),
                "imageBase64" to a.imageBase64.take(320000),
                "hasTransparency" to a.hasTransparency,
                "visualStyle" to a.visualStyle.take(100),
                "promptUsed" to a.promptUsed.take(1500),
                "graphicPayloadJson" to a.graphicPayloadJson.take(32000)
            )
        }

        fun sanitizeMediaUriString(raw: String): String {
            val trimmed = raw.trim()
            if (trimmed.isBlank()) return ""
            return if (trimmed.startsWith("content://") || trimmed.startsWith("https://")) trimmed else ""
        }

        fun castListToMaps(cast: List<SceneCharacterPlacement>): List<Map<String, Any>> {
            return cast.take(12).map { m ->
                mapOf(
                    "characterId" to m.characterId.take(128),
                    "expression" to m.expression.take(64),
                    "action" to m.action.take(80),
                    "poseView" to m.poseView.take(64),
                    "posX" to m.posX.coerceIn(-1000.0, 1000.0),
                    "posY" to m.posY.coerceIn(-1000.0, 1000.0),
                    "scale" to m.scale.coerceIn(0.2, 3.0),
                    "rotationDeg" to m.rotationDeg.coerceIn(-360.0, 360.0),
                    "layer" to m.layer.take(32).ifBlank { "CHARACTERS" },
                    "zIndex" to m.zIndex.coerceIn(-100, 100),
                    "visible" to m.visible,
                    "opacity" to m.opacity.coerceIn(0.0, 1.0),
                    "flipX" to m.flipX
                )
            }
        }

        fun sceneElementsToMaps(elements: List<SceneVisualElement>): List<Map<String, Any>> {
            return elements.take(24).mapIndexed { idx, el ->
                mapOf(
                    "elementId" to el.elementId.trim().take(64).ifBlank { "el_$idx" },
                    "name" to el.name.trim().take(120).ifBlank { "Visual Element" },
                    "elementType" to el.elementType.trim().take(32).ifBlank { "PROP" },
                    "assetId" to el.assetId.trim().take(128),
                    "assetPresetKey" to el.assetPresetKey.trim().take(128),
                    "assetUri" to sanitizeMediaUriString(el.assetUri).take(500),
                    "layer" to el.layer.trim().take(32).ifBlank { "PROPS" },
                    "posX" to el.posX.coerceIn(-1000.0, 1000.0),
                    "posY" to el.posY.coerceIn(-1000.0, 1000.0),
                    "scale" to el.scale.coerceIn(0.2, 3.0),
                    "rotationDeg" to el.rotationDeg.coerceIn(-360.0, 360.0),
                    "zIndex" to el.zIndex.coerceIn(-100, 100),
                    "visible" to el.visible,
                    "opacity" to el.opacity.coerceIn(0.0, 1.0),
                    "flipX" to el.flipX
                )
            }
        }

        fun layerStatesToMaps(layers: List<LayerState>): List<Map<String, Any>> {
            val normalized = com.example.compositor.SceneCompositor.normalizeLayerStates(layers)
            return normalized.take(6).map { l ->
                mapOf(
                    "layerKey" to l.layerKey.take(32),
                    "visible" to l.visible,
                    "order" to l.order.coerceIn(0, 5),
                    "opacity" to l.opacity.coerceIn(0.0, 1.0),
                    "parallaxFactor" to l.parallaxFactor.coerceIn(0.0, 2.0)
                )
            }
        }

        fun animationKeyframesToMaps(keyframes: List<AnimationKeyframe>): List<Map<String, Any>> {
            return keyframes
                .take(com.example.animation.AnimationEvaluator.MAX_KEYFRAMES_PER_SCENE)
                .mapIndexed { idx, kf ->
                    val prop = com.example.animation.AnimatableProperty.fromKeyOrNull(kf.property)
                    val clampedVal = prop?.clampValue(kf.value) ?: kf.value.coerceIn(-1000.0, 1000.0)
                    val safeTime = if (kf.timeSec.isNaN() || kf.timeSec.isInfinite()) 0.0 else kf.timeSec.coerceIn(0.0, 300.0)
                    mapOf(
                        "keyframeId" to kf.keyframeId.trim().take(64).ifBlank { "kf_$idx" },
                        "targetType" to kf.targetType.trim().take(24).ifBlank { "CHARACTER" },
                        "targetId" to kf.targetId.trim().take(128),
                        "property" to kf.property.trim().take(32).ifBlank { "POS_X" },
                        "timeSec" to safeTime,
                        "value" to clampedVal,
                        "easing" to kf.easing.trim().take(24).ifBlank { "EASE_IN_OUT" }
                    )
                }
        }

        fun mapsToAnimationKeyframes(raw: List<Map<String, Any>>): List<AnimationKeyframe> {
            return raw.mapIndexed { idx, map ->
                AnimationKeyframe(
                    keyframeId = (map["keyframeId"] as? String)?.take(64).orEmpty().ifBlank { "kf_$idx" },
                    targetType = (map["targetType"] as? String)?.take(24).orEmpty().ifBlank { "CHARACTER" },
                    targetId = (map["targetId"] as? String)?.take(128).orEmpty(),
                    property = (map["property"] as? String)?.take(32).orEmpty().ifBlank { "POS_X" },
                    timeSec = (map["timeSec"] as? Number)?.toDouble()?.coerceIn(0.0, 300.0) ?: 0.0,
                    value = (map["value"] as? Number)?.toDouble() ?: 0.0,
                    easing = (map["easing"] as? String)?.take(24).orEmpty().ifBlank { "EASE_IN_OUT" }
                )
            }
        }

        fun actingCuesToMaps(cues: List<CharacterActingCue>): List<Map<String, Any>> {
            return cues
                .take(com.example.animation.AnimationEvaluator.MAX_ACTING_CUES_PER_SCENE)
                .mapIndexed { idx, cue ->
                    val safeTime = if (cue.timeSec.isNaN() || cue.timeSec.isInfinite()) 0.0 else cue.timeSec.coerceIn(0.0, 300.0)
                    mapOf(
                        "cueId" to cue.cueId.trim().take(64).ifBlank { "cue_$idx" },
                        "characterId" to cue.characterId.trim().take(128),
                        "timeSec" to safeTime,
                        "expression" to cue.expression.trim().take(64).ifBlank { "Neutral" },
                        "action" to cue.action.trim().take(80).ifBlank { "Standing" },
                        "poseView" to cue.poseView.trim().take(64).ifBlank { "Three-Quarter View" },
                        "referenceSheetId" to cue.referenceSheetId.trim().take(128)
                    )
                }
        }

        fun mapsToActingCues(raw: List<Map<String, Any>>): List<CharacterActingCue> {
            return raw.mapIndexed { idx, map ->
                CharacterActingCue(
                    cueId = (map["cueId"] as? String)?.take(64).orEmpty().ifBlank { "cue_$idx" },
                    characterId = (map["characterId"] as? String)?.take(128).orEmpty(),
                    timeSec = (map["timeSec"] as? Number)?.toDouble()?.coerceIn(0.0, 300.0) ?: 0.0,
                    expression = (map["expression"] as? String)?.take(64).orEmpty().ifBlank { "Neutral" },
                    action = (map["action"] as? String)?.take(80).orEmpty().ifBlank { "Standing" },
                    poseView = (map["poseView"] as? String)?.take(64).orEmpty().ifBlank { "Three-Quarter View" },
                    referenceSheetId = (map["referenceSheetId"] as? String)?.take(128).orEmpty()
                )
            }
        }

        fun cinematicsToMap(c: SceneCinematics): Map<String, Any> {
            fun safeDouble(v: Double, min: Double, max: Double, fallback: Double): Double {
                return if (v.isNaN() || v.isInfinite()) fallback else v.coerceIn(min, max)
            }
            return mapOf(
                "cameraPreset" to c.cameraPreset.trim().take(64).ifBlank { "Wide Shot" },
                "cameraPosX" to safeDouble(c.cameraPosX, -240.0, 240.0, 0.0),
                "cameraPosY" to safeDouble(c.cameraPosY, -160.0, 160.0, 0.0),
                "cameraRotationDeg" to safeDouble(c.cameraRotationDeg, -45.0, 45.0, 0.0),
                "parallaxEnabled" to c.parallaxEnabled,
                "parallaxStrength" to safeDouble(c.parallaxStrength, 0.0, 2.0, 1.0),
                "depthFocusPlane" to safeDouble(c.depthFocusPlane, 0.2, 1.3, 0.85),
                "depthBlurIntensity" to safeDouble(c.depthBlurIntensity, 0.0, 1.0, 0.0),
                "depthHazeIntensity" to safeDouble(c.depthHazeIntensity, 0.0, 1.0, 0.0),
                "lightingPreset" to c.lightingPreset.trim().take(64).ifBlank { "Natural Documentary" },
                "exposure" to safeDouble(c.exposure, -0.5, 0.5, 0.0),
                "contrast" to safeDouble(c.contrast, 0.6, 1.6, 1.0),
                "saturation" to safeDouble(c.saturation, 0.0, 1.8, 1.0),
                "temperature" to safeDouble(c.temperature, -1.0, 1.0, 0.0),
                "lightSourceX" to safeDouble(c.lightSourceX, -1.0, 1.0, 0.35),
                "lightSourceY" to safeDouble(c.lightSourceY, -1.0, 1.0, -0.65),
                "lightGlowIntensity" to safeDouble(c.lightGlowIntensity, 0.0, 1.0, 0.0),
                "shadowIntensity" to safeDouble(c.shadowIntensity, 0.0, 1.0, 0.0),
                "atmosphereMode" to c.atmosphereMode.trim().take(64).ifBlank { "None" },
                "atmosphereIntensity" to safeDouble(c.atmosphereIntensity, 0.0, 1.0, 0.0),
                "atmosphereSpeed" to safeDouble(c.atmosphereSpeed, 0.2, 2.5, 1.0),
                "windStrength" to safeDouble(c.windStrength, -1.0, 1.0, 0.15),
                "vignetteIntensity" to safeDouble(c.vignetteIntensity, 0.0, 1.0, 0.0),
                "filmGrainIntensity" to safeDouble(c.filmGrainIntensity, 0.0, 1.0, 0.0),
                "bloomIntensity" to safeDouble(c.bloomIntensity, 0.0, 1.0, 0.0),
                "cameraShakeIntensity" to safeDouble(c.cameraShakeIntensity, 0.0, 1.0, 0.0),
                "letterboxAspect" to c.letterboxAspect.trim().take(40).ifBlank { "None" }
            )
        }

        fun mapToCinematics(map: Map<String, Any>): SceneCinematics {
            fun num(key: String, min: Double, max: Double, default: Double): Double {
                val raw = (map[key] as? Number)?.toDouble() ?: return default
                return if (raw.isNaN() || raw.isInfinite()) default else raw.coerceIn(min, max)
            }
            return SceneCinematics(
                cameraPreset = (map["cameraPreset"] as? String)?.take(64).orEmpty().ifBlank { "Wide Shot" },
                cameraPosX = num("cameraPosX", -240.0, 240.0, 0.0),
                cameraPosY = num("cameraPosY", -160.0, 160.0, 0.0),
                cameraRotationDeg = num("cameraRotationDeg", -45.0, 45.0, 0.0),
                parallaxEnabled = (map["parallaxEnabled"] as? Boolean) ?: true,
                parallaxStrength = num("parallaxStrength", 0.0, 2.0, 1.0),
                depthFocusPlane = num("depthFocusPlane", 0.2, 1.3, 0.85),
                depthBlurIntensity = num("depthBlurIntensity", 0.0, 1.0, 0.0),
                depthHazeIntensity = num("depthHazeIntensity", 0.0, 1.0, 0.0),
                lightingPreset = (map["lightingPreset"] as? String)?.take(64).orEmpty().ifBlank { "Natural Documentary" },
                exposure = num("exposure", -0.5, 0.5, 0.0),
                contrast = num("contrast", 0.6, 1.6, 1.0),
                saturation = num("saturation", 0.0, 1.8, 1.0),
                temperature = num("temperature", -1.0, 1.0, 0.0),
                lightSourceX = num("lightSourceX", -1.0, 1.0, 0.35),
                lightSourceY = num("lightSourceY", -1.0, 1.0, -0.65),
                lightGlowIntensity = num("lightGlowIntensity", 0.0, 1.0, 0.0),
                shadowIntensity = num("shadowIntensity", 0.0, 1.0, 0.0),
                atmosphereMode = (map["atmosphereMode"] as? String)?.take(64).orEmpty().ifBlank { "None" },
                atmosphereIntensity = num("atmosphereIntensity", 0.0, 1.0, 0.0),
                atmosphereSpeed = num("atmosphereSpeed", 0.2, 2.5, 1.0),
                windStrength = num("windStrength", -1.0, 1.0, 0.15),
                vignetteIntensity = num("vignetteIntensity", 0.0, 1.0, 0.0),
                filmGrainIntensity = num("filmGrainIntensity", 0.0, 1.0, 0.0),
                bloomIntensity = num("bloomIntensity", 0.0, 1.0, 0.0),
                cameraShakeIntensity = num("cameraShakeIntensity", 0.0, 1.0, 0.0),
                letterboxAspect = (map["letterboxAspect"] as? String)?.take(40).orEmpty().ifBlank { "None" }
            )
        }

        private val validAudioCategories = setOf("Narration", "Dialogue", "Music", "SFX", "Ambient")
        private val validAudioSourceTypes = setOf("IMPORTED", "ANDROID_TTS", "GEMINI_TTS")
        private val validAudioStorageStates = setOf("LOCAL_PERSISTENT", "MISSING_LOCAL")
        val validAudioMimeTypes = setOf(
            "audio/mpeg", "audio/mp3", "audio/wav", "audio/x-wav",
            "audio/mp4", "audio/m4a", "audio/aac", "audio/ogg",
            "audio/webm", "audio/flac"
        )

        private fun safeDouble(v: Double, min: Double, max: Double, fallback: Double): Double {
            return if (v.isNaN() || v.isInfinite()) fallback else v.coerceIn(min, max)
        }

        fun audioAssetFieldsToMap(a: StudioAudioAsset): Map<String, Any> {
            val mime = a.mimeType.trim().lowercase().let {
                if (it in validAudioMimeTypes) it else "audio/wav"
            }
            val safePeaks = a.waveformPeaks.take(48).map { safeDouble(it, 0.0, 1.0, 0.0) }
            return mapOf(
                "name" to a.name.trim().take(120).ifBlank { "Studio Audio" },
                "category" to if (a.category in validAudioCategories) a.category else "Narration",
                "sourceType" to if (a.sourceType in validAudioSourceTypes) a.sourceType else "IMPORTED",
                "storageState" to if (a.storageState in validAudioStorageStates) a.storageState else "LOCAL_PERSISTENT",
                "localFilePath" to a.localFilePath.trim().take(400),
                "mimeType" to mime,
                "fileSizeBytes" to a.fileSizeBytes.coerceIn(0L, 15_728_640L),
                "durationSec" to safeDouble(a.durationSec, 0.0, 1800.0, 0.0),
                "language" to a.language.trim().take(32).ifBlank { "English" },
                "voiceId" to a.voiceId.trim().take(160),
                "characterId" to a.characterId.trim().take(128),
                "waveformPeaks" to safePeaks,
                "notes" to a.notes.trim().take(600)
            )
        }

        fun storyAnalysisToMap(s: StoryAnalysisSummary): Map<String, Any> {
            return mapOf(
                "detectedLanguage" to s.detectedLanguage.trim().take(32).ifBlank { "Bilingual" },
                "storyLogline" to s.storyLogline.trim().take(600),
                "eraOrPeriod" to s.eraOrPeriod.trim().take(120),
                "emotionalArc" to s.emotionalArc.trim().take(300),
                "locations" to s.locations.take(16).map { it.trim().take(100) }.filter { it.isNotBlank() },
                "characterRelationships" to s.characterRelationships.take(16).map { it.trim().take(180) }.filter { it.isNotBlank() },
                "narrativeBeats" to s.narrativeBeats.take(16).map { it.trim().take(160) }.filter { it.isNotBlank() },
                "estimatedTotalDurationSec" to safeDouble(s.estimatedTotalDurationSec, 0.0, 7200.0, 0.0),
                "approved" to s.approved
            )
        }

        fun mapToStoryAnalysis(map: Map<String, Any>): StoryAnalysisSummary {
            fun strList(key: String): List<String> =
                (map[key] as? List<*>)?.mapNotNull { it as? String }?.filter { it.isNotBlank() }.orEmpty()
            return StoryAnalysisSummary(
                detectedLanguage = (map["detectedLanguage"] as? String)?.take(32).orEmpty().ifBlank { "Bilingual" },
                storyLogline = (map["storyLogline"] as? String)?.take(600).orEmpty(),
                eraOrPeriod = (map["eraOrPeriod"] as? String)?.take(120).orEmpty(),
                emotionalArc = (map["emotionalArc"] as? String)?.take(300).orEmpty(),
                locations = strList("locations"),
                characterRelationships = strList("characterRelationships"),
                narrativeBeats = strList("narrativeBeats"),
                estimatedTotalDurationSec = safeDouble((map["estimatedTotalDurationSec"] as? Number)?.toDouble() ?: 0.0, 0.0, 7200.0, 0.0),
                approved = (map["approved"] as? Boolean) ?: false
            )
        }

        fun scenePlanToMap(p: ScenePlanMetadata): Map<String, Any> {
            return mapOf(
                "location" to p.location.trim().take(120),
                "environment" to p.environment.trim().take(240),
                "timeOfDay" to p.timeOfDay.trim().take(64),
                "weather" to p.weather.trim().take(64),
                "emotionalTone" to p.emotionalTone.trim().take(100),
                "visualDirection" to p.visualDirection.trim().take(600),
                "cameraSuggestion" to p.cameraSuggestion.trim().take(120),
                "lightingSuggestion" to p.lightingSuggestion.trim().take(120),
                "atmosphereSuggestion" to p.atmosphereSuggestion.trim().take(120),
                "vfxSuggestion" to p.vfxSuggestion.trim().take(120),
                "transitionSuggestion" to p.transitionSuggestion.trim().take(64).ifBlank { "Cut" },
                "audioCuesSummary" to p.audioCuesSummary.trim().take(300),
                "suggestedProps" to p.suggestedProps.take(10).map { it.trim().take(80) }.filter { it.isNotBlank() },
                "approved" to p.approved
            )
        }

        fun mapToScenePlan(map: Map<String, Any>): ScenePlanMetadata {
            val props = (map["suggestedProps"] as? List<*>)?.mapNotNull { it as? String }?.filter { it.isNotBlank() }.orEmpty()
            return ScenePlanMetadata(
                location = (map["location"] as? String)?.take(120).orEmpty(),
                environment = (map["environment"] as? String)?.take(240).orEmpty(),
                timeOfDay = (map["timeOfDay"] as? String)?.take(64).orEmpty(),
                weather = (map["weather"] as? String)?.take(64).orEmpty(),
                emotionalTone = (map["emotionalTone"] as? String)?.take(100).orEmpty(),
                visualDirection = (map["visualDirection"] as? String)?.take(600).orEmpty(),
                cameraSuggestion = (map["cameraSuggestion"] as? String)?.take(120).orEmpty(),
                lightingSuggestion = (map["lightingSuggestion"] as? String)?.take(120).orEmpty(),
                atmosphereSuggestion = (map["atmosphereSuggestion"] as? String)?.take(120).orEmpty(),
                vfxSuggestion = (map["vfxSuggestion"] as? String)?.take(120).orEmpty(),
                transitionSuggestion = (map["transitionSuggestion"] as? String)?.take(64).orEmpty().ifBlank { "Cut" },
                audioCuesSummary = (map["audioCuesSummary"] as? String)?.take(300).orEmpty(),
                suggestedProps = props,
                approved = (map["approved"] as? Boolean) ?: false
            )
        }

        fun dialogueLinesToMaps(lines: List<DialogueLine>): List<Map<String, Any>> {
            return lines.take(40).mapIndexed { idx, line ->
                mapOf(
                    "lineId" to line.lineId.trim().take(64).ifBlank { "dlg_$idx" },
                    "characterId" to line.characterId.trim().take(128),
                    "speakerName" to line.speakerName.trim().take(100),
                    "text" to line.text.trim().take(1200),
                    "language" to line.language.trim().take(32).ifBlank { "English" },
                    "startSec" to safeDouble(line.startSec, 0.0, 300.0, 0.0),
                    "durationSec" to safeDouble(line.durationSec, 0.3, 180.0, 3.0),
                    "audioAssetId" to line.audioAssetId.trim().take(128),
                    "clipId" to line.clipId.trim().take(64),
                    "voiceId" to line.voiceId.trim().take(160),
                    "emotion" to line.emotion.trim().take(64).ifBlank { "Neutral" },
                    "orderIndex" to line.orderIndex.coerceIn(0, 100)
                )
            }
        }

        fun mapsToDialogueLines(raw: List<Map<String, Any>>): List<DialogueLine> {
            return raw.mapIndexed { idx, map ->
                DialogueLine(
                    lineId = (map["lineId"] as? String)?.take(64).orEmpty().ifBlank { "dlg_$idx" },
                    characterId = (map["characterId"] as? String)?.take(128).orEmpty(),
                    speakerName = (map["speakerName"] as? String)?.take(100).orEmpty(),
                    text = (map["text"] as? String)?.take(1200).orEmpty(),
                    language = (map["language"] as? String)?.take(32).orEmpty().ifBlank { "English" },
                    startSec = safeDouble((map["startSec"] as? Number)?.toDouble() ?: 0.0, 0.0, 300.0, 0.0),
                    durationSec = safeDouble((map["durationSec"] as? Number)?.toDouble() ?: 3.0, 0.3, 180.0, 3.0),
                    audioAssetId = (map["audioAssetId"] as? String)?.take(128).orEmpty(),
                    clipId = (map["clipId"] as? String)?.take(64).orEmpty(),
                    voiceId = (map["voiceId"] as? String)?.take(160).orEmpty(),
                    emotion = (map["emotion"] as? String)?.take(64).orEmpty().ifBlank { "Neutral" },
                    orderIndex = (map["orderIndex"] as? Number)?.toInt()?.coerceIn(0, 100) ?: idx
                )
            }
        }

        fun audioClipsToMaps(clips: List<SceneAudioClip>, maxItems: Int = 40): List<Map<String, Any>> {
            return clips.take(maxItems).mapIndexed { idx, clip ->
                val srcDur = safeDouble(clip.sourceDurationSec, 0.1, 1800.0, 5.0)
                val trimStart = safeDouble(clip.trimStartSec, 0.0, srcDur, 0.0)
                val rawTrimEnd = if (clip.trimEndSec <= 0.0) srcDur else clip.trimEndSec
                val trimEnd = safeDouble(rawTrimEnd, trimStart + 0.1, srcDur.coerceAtLeast(trimStart + 0.1), srcDur)
                mapOf(
                    "clipId" to clip.clipId.trim().take(64).ifBlank { "clip_$idx" },
                    "audioAssetId" to clip.audioAssetId.trim().take(128),
                    "title" to clip.title.trim().take(120).ifBlank { "Audio Clip" },
                    "trackType" to if (clip.trackType in StudioPresets.audioTrackTypes) clip.trackType else "NARRATION",
                    "scope" to if (clip.scope == "PROJECT") "PROJECT" else "SCENE",
                    "startSec" to safeDouble(clip.startSec, 0.0, 3600.0, 0.0),
                    "sourceDurationSec" to srcDur,
                    "trimStartSec" to trimStart,
                    "trimEndSec" to trimEnd,
                    "volume" to safeDouble(clip.volume, 0.0, 1.5, 1.0),
                    "fadeInSec" to safeDouble(clip.fadeInSec, 0.0, 30.0, 0.0),
                    "fadeOutSec" to safeDouble(clip.fadeOutSec, 0.0, 30.0, 0.0),
                    "loopEnabled" to clip.loopEnabled,
                    "muted" to clip.muted,
                    "dialogueLineId" to clip.dialogueLineId.trim().take(64),
                    "characterId" to clip.characterId.trim().take(128)
                )
            }
        }

        fun mapsToAudioClips(raw: List<Map<String, Any>>): List<SceneAudioClip> {
            return raw.mapIndexed { idx, map ->
                val srcDur = safeDouble((map["sourceDurationSec"] as? Number)?.toDouble() ?: 5.0, 0.1, 1800.0, 5.0)
                SceneAudioClip(
                    clipId = (map["clipId"] as? String)?.take(64).orEmpty().ifBlank { "clip_$idx" },
                    audioAssetId = (map["audioAssetId"] as? String)?.take(128).orEmpty(),
                    title = (map["title"] as? String)?.take(120).orEmpty().ifBlank { "Audio Clip" },
                    trackType = (map["trackType"] as? String)?.take(24).orEmpty().ifBlank { "NARRATION" },
                    scope = (map["scope"] as? String)?.take(16).orEmpty().ifBlank { "SCENE" },
                    startSec = safeDouble((map["startSec"] as? Number)?.toDouble() ?: 0.0, 0.0, 3600.0, 0.0),
                    sourceDurationSec = srcDur,
                    trimStartSec = safeDouble((map["trimStartSec"] as? Number)?.toDouble() ?: 0.0, 0.0, srcDur, 0.0),
                    trimEndSec = safeDouble((map["trimEndSec"] as? Number)?.toDouble() ?: srcDur, 0.0, srcDur, srcDur),
                    volume = safeDouble((map["volume"] as? Number)?.toDouble() ?: 1.0, 0.0, 1.5, 1.0),
                    fadeInSec = safeDouble((map["fadeInSec"] as? Number)?.toDouble() ?: 0.0, 0.0, 30.0, 0.0),
                    fadeOutSec = safeDouble((map["fadeOutSec"] as? Number)?.toDouble() ?: 0.0, 0.0, 30.0, 0.0),
                    loopEnabled = (map["loopEnabled"] as? Boolean) ?: false,
                    muted = (map["muted"] as? Boolean) ?: false,
                    dialogueLineId = (map["dialogueLineId"] as? String)?.take(64).orEmpty(),
                    characterId = (map["characterId"] as? String)?.take(128).orEmpty()
                )
            }
        }

        fun lipSyncCuesToMaps(cues: List<VisemeCue>): List<Map<String, Any>> {
            return cues.take(240).mapIndexed { idx, cue ->
                mapOf(
                    "cueId" to cue.cueId.trim().take(64).ifBlank { "vis_$idx" },
                    "characterId" to cue.characterId.trim().take(128),
                    "dialogueLineId" to cue.dialogueLineId.trim().take(64),
                    "timeSec" to safeDouble(cue.timeSec, 0.0, 300.0, 0.0),
                    "durationSec" to safeDouble(cue.durationSec, 0.02, 10.0, 0.12),
                    "viseme" to cue.viseme.trim().take(24).ifBlank { "SILENT" },
                    "energy" to safeDouble(cue.energy, 0.0, 1.0, 0.0)
                )
            }
        }

        fun mapsToLipSyncCues(raw: List<Map<String, Any>>): List<VisemeCue> {
            return raw.mapIndexed { idx, map ->
                VisemeCue(
                    cueId = (map["cueId"] as? String)?.take(64).orEmpty().ifBlank { "vis_$idx" },
                    characterId = (map["characterId"] as? String)?.take(128).orEmpty(),
                    dialogueLineId = (map["dialogueLineId"] as? String)?.take(64).orEmpty(),
                    timeSec = safeDouble((map["timeSec"] as? Number)?.toDouble() ?: 0.0, 0.0, 300.0, 0.0),
                    durationSec = safeDouble((map["durationSec"] as? Number)?.toDouble() ?: 0.12, 0.02, 10.0, 0.12),
                    viseme = (map["viseme"] as? String)?.take(24).orEmpty().ifBlank { "SILENT" },
                    energy = safeDouble((map["energy"] as? Number)?.toDouble() ?: 0.0, 0.0, 1.0, 0.0)
                )
            }
        }

        fun soundSuggestionsToMaps(suggestions: List<AiSoundSuggestion>): List<Map<String, Any>> {
            return suggestions.take(20).mapIndexed { idx, s ->
                mapOf(
                    "suggestionId" to s.suggestionId.trim().take(64).ifBlank { "snd_$idx" },
                    "category" to if (s.category in setOf("MUSIC", "SFX", "AMBIENT")) s.category else "SFX",
                    "title" to s.title.trim().take(100),
                    "description" to s.description.trim().take(300),
                    "suggestedStartSec" to safeDouble(s.suggestedStartSec, 0.0, 300.0, 0.0),
                    "suggestedVolume" to safeDouble(s.suggestedVolume, 0.0, 1.5, 0.7),
                    "loopEnabled" to s.loopEnabled,
                    "applied" to s.applied
                )
            }
        }

        fun mapsToSoundSuggestions(raw: List<Map<String, Any>>): List<AiSoundSuggestion> {
            return raw.mapIndexed { idx, map ->
                AiSoundSuggestion(
                    suggestionId = (map["suggestionId"] as? String)?.take(64).orEmpty().ifBlank { "snd_$idx" },
                    category = (map["category"] as? String)?.take(24).orEmpty().ifBlank { "SFX" },
                    title = (map["title"] as? String)?.take(100).orEmpty(),
                    description = (map["description"] as? String)?.take(300).orEmpty(),
                    suggestedStartSec = safeDouble((map["suggestedStartSec"] as? Number)?.toDouble() ?: 0.0, 0.0, 300.0, 0.0),
                    suggestedVolume = safeDouble((map["suggestedVolume"] as? Number)?.toDouble() ?: 0.7, 0.0, 1.5, 0.7),
                    loopEnabled = (map["loopEnabled"] as? Boolean) ?: false,
                    applied = (map["applied"] as? Boolean) ?: false
                )
            }
        }

        // --- PHASE 6: MOTION GRAPHICS, SUBTITLES, INFOGRAPHICS, MAPS & TRANSITIONS ---

        fun motionGraphicsToMaps(graphics: List<MotionGraphicElement>): List<Map<String, Any>> {
            return graphics.take(30).mapIndexed { idx, g ->
                mapOf(
                    "graphicId" to g.graphicId.trim().take(64).ifBlank { "gfx_$idx" },
                    "title" to g.title.trim().take(120).ifBlank { "Graphic ${idx + 1}" },
                    "category" to g.category.trim().take(32).ifBlank { "TEXT" },
                    "templateType" to g.templateType.trim().take(48).ifBlank { "MAIN_TITLE" },
                    "startSec" to safeDouble(g.startSec, 0.0, 300.0, 0.0),
                    "durationSec" to safeDouble(g.durationSec, 0.3, 300.0, 4.0),
                    "layer" to g.layer.trim().take(24).ifBlank { "OVERLAY" },
                    "attachToCamera" to g.attachToCamera,
                    "posX" to safeDouble(g.posX, -480.0, 480.0, 0.0),
                    "posY" to safeDouble(g.posY, -270.0, 270.0, 0.0),
                    "scale" to safeDouble(g.scale, 0.25, 3.0, 1.0),
                    "rotationDeg" to safeDouble(g.rotationDeg, -180.0, 180.0, 0.0),
                    "widthFraction" to safeDouble(g.widthFraction, 0.15, 0.98, 0.68),
                    "heightFraction" to safeDouble(g.heightFraction, 0.1, 0.95, 0.42),
                    "zIndex" to g.zIndex.coerceIn(-100, 100),
                    "visible" to g.visible,
                    "opacity" to safeDouble(g.opacity, 0.0, 1.0, 1.0),
                    "primaryText" to g.primaryText.take(600),
                    "secondaryText" to g.secondaryText.take(400),
                    "tertiaryText" to g.tertiaryText.take(200),
                    "quaternaryText" to g.quaternaryText.take(200),
                    "fontFamilyKey" to g.fontFamilyKey.trim().take(32).ifBlank { "SPACE_GROTESK" },
                    "fontSizeSp" to safeDouble(g.fontSizeSp, 8.0, 72.0, 22.0),
                    "textColorHex" to g.textColorHex.trim().take(16).ifBlank { "#F2F4F8" },
                    "accentColorHex" to g.accentColorHex.trim().take(16).ifBlank { "#F5A623" },
                    "backgroundColorHex" to g.backgroundColorHex.trim().take(16).ifBlank { "#12141A" },
                    "backgroundOpacity" to safeDouble(g.backgroundOpacity, 0.0, 1.0, 0.82),
                    "outlineColorHex" to g.outlineColorHex.trim().take(16).ifBlank { "#0A0C10" },
                    "outlineWidthDp" to safeDouble(g.outlineWidthDp, 0.0, 6.0, 0.0),
                    "shadowEnabled" to g.shadowEnabled,
                    "alignment" to g.alignment.trim().take(16).ifBlank { "CENTER" },
                    "letterSpacingSp" to safeDouble(g.letterSpacingSp, -2.0, 12.0, 0.8),
                    "lineSpacingMult" to safeDouble(g.lineSpacingMult, 0.8, 2.5, 1.18),
                    "layoutPreset" to g.layoutPreset.trim().take(32).ifBlank { "DOCUMENTARY_BAR" },
                    "entranceAnimation" to g.entranceAnimation.trim().take(32).ifBlank { "FADE_IN" },
                    "exitAnimation" to g.exitAnimation.trim().take(32).ifBlank { "FADE_OUT" },
                    "entranceDurationSec" to safeDouble(g.entranceDurationSec, 0.0, 5.0, 0.65),
                    "exitDurationSec" to safeDouble(g.exitDurationSec, 0.0, 5.0, 0.45),
                    "animationEasing" to g.animationEasing.trim().take(24).ifBlank { "EASE_OUT" },
                    "infographicType" to g.infographicType.trim().take(32).ifBlank { "BAR_CHART" },
                    "dataUnit" to g.dataUnit.trim().take(24),
                    "dataPoints" to g.dataPoints.take(16).mapIndexed { dpIdx, dp ->
                        mapOf(
                            "id" to dp.id.trim().take(48).ifBlank { "dp_$dpIdx" },
                            "label" to dp.label.trim().take(80),
                            "value" to safeDouble(dp.value, -1e9, 1e9, 0.0),
                            "secondaryValue" to safeDouble(dp.secondaryValue, -1e9, 1e9, 0.0),
                            "colorHex" to dp.colorHex.trim().take(16).ifBlank { "#F5A623" },
                            "highlighted" to dp.highlighted,
                            "calloutText" to dp.calloutText.trim().take(120)
                        )
                    },
                    "mapBackgroundUri" to sanitizeMediaUriString(g.mapBackgroundUri).take(500),
                    "mapPresetStyle" to g.mapPresetStyle.trim().take(32).ifBlank { "DARK_ATLAS" },
                    "mapZoomStart" to safeDouble(g.mapZoomStart, 0.5, 3.0, 1.0),
                    "mapZoomEnd" to safeDouble(g.mapZoomEnd, 0.5, 3.0, 1.25),
                    "mapPanStartX" to safeDouble(g.mapPanStartX, -1.0, 1.0, 0.0),
                    "mapPanStartY" to safeDouble(g.mapPanStartY, -1.0, 1.0, 0.0),
                    "mapPanEndX" to safeDouble(g.mapPanEndX, -1.0, 1.0, 0.0),
                    "mapPanEndY" to safeDouble(g.mapPanEndY, -1.0, 1.0, 0.0),
                    "mapMarkers" to g.mapMarkers.take(16).mapIndexed { mIdx, m ->
                        mapOf(
                            "markerId" to m.markerId.trim().take(48).ifBlank { "mk_$mIdx" },
                            "label" to m.label.trim().take(80),
                            "regionTag" to m.regionTag.trim().take(64),
                            "xNorm" to safeDouble(m.xNorm, 0.02, 0.98, 0.5),
                            "yNorm" to safeDouble(m.yNorm, 0.02, 0.98, 0.5),
                            "markerStyle" to m.markerStyle.trim().take(24).ifBlank { "PIN" },
                            "colorHex" to m.colorHex.trim().take(16).ifBlank { "#F5A623" },
                            "appearAtSec" to safeDouble(m.appearAtSec, 0.0, 120.0, 0.3),
                            "highlighted" to m.highlighted
                        )
                    },
                    "mapRoutes" to g.mapRoutes.take(12).mapIndexed { rIdx, r ->
                        mapOf(
                            "routeId" to r.routeId.trim().take(48).ifBlank { "rt_$rIdx" },
                            "label" to r.label.trim().take(80),
                            "distanceCaption" to r.distanceCaption.trim().take(48),
                            "fromX" to safeDouble(r.fromX, 0.02, 0.98, 0.25),
                            "fromY" to safeDouble(r.fromY, 0.02, 0.98, 0.65),
                            "toX" to safeDouble(r.toX, 0.02, 0.98, 0.75),
                            "toY" to safeDouble(r.toY, 0.02, 0.98, 0.35),
                            "colorHex" to r.colorHex.trim().take(16).ifBlank { "#26D0CE" },
                            "showArrow" to r.showArrow,
                            "startSec" to safeDouble(r.startSec, 0.0, 120.0, 0.5),
                            "durationSec" to safeDouble(r.durationSec, 0.1, 60.0, 2.0)
                        )
                    }
                )
            }
        }

        @Suppress("UNCHECKED_CAST")
        fun mapsToMotionGraphics(raw: List<Map<String, Any>>): List<MotionGraphicElement> {
            return raw.mapIndexed { idx, map ->
                val dataPoints = (map["dataPoints"] as? List<*>)
                    ?.mapNotNull { it as? Map<String, Any> }
                    ?.mapIndexed { dpIdx, dp ->
                        InfographicDataPoint(
                            id = (dp["id"] as? String)?.take(48).orEmpty().ifBlank { "dp_$dpIdx" },
                            label = (dp["label"] as? String)?.take(80).orEmpty(),
                            value = safeDouble((dp["value"] as? Number)?.toDouble() ?: 0.0, -1e9, 1e9, 0.0),
                            secondaryValue = safeDouble((dp["secondaryValue"] as? Number)?.toDouble() ?: 0.0, -1e9, 1e9, 0.0),
                            colorHex = (dp["colorHex"] as? String)?.take(16).orEmpty().ifBlank { "#F5A623" },
                            highlighted = (dp["highlighted"] as? Boolean) ?: false,
                            calloutText = (dp["calloutText"] as? String)?.take(120).orEmpty()
                        )
                    }.orEmpty()
                val markers = (map["mapMarkers"] as? List<*>)
                    ?.mapNotNull { it as? Map<String, Any> }
                    ?.mapIndexed { mIdx, m ->
                        MapMarkerItem(
                            markerId = (m["markerId"] as? String)?.take(48).orEmpty().ifBlank { "mk_$mIdx" },
                            label = (m["label"] as? String)?.take(80).orEmpty(),
                            regionTag = (m["regionTag"] as? String)?.take(64).orEmpty(),
                            xNorm = safeDouble((m["xNorm"] as? Number)?.toDouble() ?: 0.5, 0.02, 0.98, 0.5),
                            yNorm = safeDouble((m["yNorm"] as? Number)?.toDouble() ?: 0.5, 0.02, 0.98, 0.5),
                            markerStyle = (m["markerStyle"] as? String)?.take(24).orEmpty().ifBlank { "PIN" },
                            colorHex = (m["colorHex"] as? String)?.take(16).orEmpty().ifBlank { "#F5A623" },
                            appearAtSec = safeDouble((m["appearAtSec"] as? Number)?.toDouble() ?: 0.3, 0.0, 120.0, 0.3),
                            highlighted = (m["highlighted"] as? Boolean) ?: false
                        )
                    }.orEmpty()
                val routes = (map["mapRoutes"] as? List<*>)
                    ?.mapNotNull { it as? Map<String, Any> }
                    ?.mapIndexed { rIdx, r ->
                        MapRouteItem(
                            routeId = (r["routeId"] as? String)?.take(48).orEmpty().ifBlank { "rt_$rIdx" },
                            label = (r["label"] as? String)?.take(80).orEmpty(),
                            distanceCaption = (r["distanceCaption"] as? String)?.take(48).orEmpty(),
                            fromX = safeDouble((r["fromX"] as? Number)?.toDouble() ?: 0.25, 0.02, 0.98, 0.25),
                            fromY = safeDouble((r["fromY"] as? Number)?.toDouble() ?: 0.65, 0.02, 0.98, 0.65),
                            toX = safeDouble((r["toX"] as? Number)?.toDouble() ?: 0.75, 0.02, 0.98, 0.75),
                            toY = safeDouble((r["toY"] as? Number)?.toDouble() ?: 0.35, 0.02, 0.98, 0.35),
                            colorHex = (r["colorHex"] as? String)?.take(16).orEmpty().ifBlank { "#26D0CE" },
                            showArrow = (r["showArrow"] as? Boolean) ?: true,
                            startSec = safeDouble((r["startSec"] as? Number)?.toDouble() ?: 0.5, 0.0, 120.0, 0.5),
                            durationSec = safeDouble((r["durationSec"] as? Number)?.toDouble() ?: 2.0, 0.1, 60.0, 2.0)
                        )
                    }.orEmpty()

                MotionGraphicElement(
                    graphicId = (map["graphicId"] as? String)?.take(64).orEmpty().ifBlank { "gfx_$idx" },
                    title = (map["title"] as? String)?.take(120).orEmpty().ifBlank { "Graphic ${idx + 1}" },
                    category = (map["category"] as? String)?.take(32).orEmpty().ifBlank { "TEXT" },
                    templateType = (map["templateType"] as? String)?.take(48).orEmpty().ifBlank { "MAIN_TITLE" },
                    startSec = safeDouble((map["startSec"] as? Number)?.toDouble() ?: 0.0, 0.0, 300.0, 0.0),
                    durationSec = safeDouble((map["durationSec"] as? Number)?.toDouble() ?: 4.0, 0.3, 300.0, 4.0),
                    layer = (map["layer"] as? String)?.take(24).orEmpty().ifBlank { "OVERLAY" },
                    attachToCamera = (map["attachToCamera"] as? Boolean) ?: false,
                    posX = safeDouble((map["posX"] as? Number)?.toDouble() ?: 0.0, -480.0, 480.0, 0.0),
                    posY = safeDouble((map["posY"] as? Number)?.toDouble() ?: 0.0, -270.0, 270.0, 0.0),
                    scale = safeDouble((map["scale"] as? Number)?.toDouble() ?: 1.0, 0.25, 3.0, 1.0),
                    rotationDeg = safeDouble((map["rotationDeg"] as? Number)?.toDouble() ?: 0.0, -180.0, 180.0, 0.0),
                    widthFraction = safeDouble((map["widthFraction"] as? Number)?.toDouble() ?: 0.68, 0.15, 0.98, 0.68),
                    heightFraction = safeDouble((map["heightFraction"] as? Number)?.toDouble() ?: 0.42, 0.1, 0.95, 0.42),
                    zIndex = (map["zIndex"] as? Number)?.toInt()?.coerceIn(-100, 100) ?: 0,
                    visible = (map["visible"] as? Boolean) ?: true,
                    opacity = safeDouble((map["opacity"] as? Number)?.toDouble() ?: 1.0, 0.0, 1.0, 1.0),
                    primaryText = (map["primaryText"] as? String)?.take(600).orEmpty(),
                    secondaryText = (map["secondaryText"] as? String)?.take(400).orEmpty(),
                    tertiaryText = (map["tertiaryText"] as? String)?.take(200).orEmpty(),
                    quaternaryText = (map["quaternaryText"] as? String)?.take(200).orEmpty(),
                    fontFamilyKey = (map["fontFamilyKey"] as? String)?.take(32).orEmpty().ifBlank { "SPACE_GROTESK" },
                    fontSizeSp = safeDouble((map["fontSizeSp"] as? Number)?.toDouble() ?: 22.0, 8.0, 72.0, 22.0),
                    textColorHex = (map["textColorHex"] as? String)?.take(16).orEmpty().ifBlank { "#F2F4F8" },
                    accentColorHex = (map["accentColorHex"] as? String)?.take(16).orEmpty().ifBlank { "#F5A623" },
                    backgroundColorHex = (map["backgroundColorHex"] as? String)?.take(16).orEmpty().ifBlank { "#12141A" },
                    backgroundOpacity = safeDouble((map["backgroundOpacity"] as? Number)?.toDouble() ?: 0.82, 0.0, 1.0, 0.82),
                    outlineColorHex = (map["outlineColorHex"] as? String)?.take(16).orEmpty().ifBlank { "#0A0C10" },
                    outlineWidthDp = safeDouble((map["outlineWidthDp"] as? Number)?.toDouble() ?: 0.0, 0.0, 6.0, 0.0),
                    shadowEnabled = (map["shadowEnabled"] as? Boolean) ?: true,
                    alignment = (map["alignment"] as? String)?.take(16).orEmpty().ifBlank { "CENTER" },
                    letterSpacingSp = safeDouble((map["letterSpacingSp"] as? Number)?.toDouble() ?: 0.8, -2.0, 12.0, 0.8),
                    lineSpacingMult = safeDouble((map["lineSpacingMult"] as? Number)?.toDouble() ?: 1.18, 0.8, 2.5, 1.18),
                    layoutPreset = (map["layoutPreset"] as? String)?.take(32).orEmpty().ifBlank { "DOCUMENTARY_BAR" },
                    entranceAnimation = (map["entranceAnimation"] as? String)?.take(32).orEmpty().ifBlank { "FADE_IN" },
                    exitAnimation = (map["exitAnimation"] as? String)?.take(32).orEmpty().ifBlank { "FADE_OUT" },
                    entranceDurationSec = safeDouble((map["entranceDurationSec"] as? Number)?.toDouble() ?: 0.65, 0.0, 5.0, 0.65),
                    exitDurationSec = safeDouble((map["exitDurationSec"] as? Number)?.toDouble() ?: 0.45, 0.0, 5.0, 0.45),
                    animationEasing = (map["animationEasing"] as? String)?.take(24).orEmpty().ifBlank { "EASE_OUT" },
                    infographicType = (map["infographicType"] as? String)?.take(32).orEmpty().ifBlank { "BAR_CHART" },
                    dataUnit = (map["dataUnit"] as? String)?.take(24).orEmpty(),
                    dataPoints = dataPoints,
                    mapBackgroundUri = sanitizeMediaUriString((map["mapBackgroundUri"] as? String).orEmpty()).take(500),
                    mapPresetStyle = (map["mapPresetStyle"] as? String)?.take(32).orEmpty().ifBlank { "DARK_ATLAS" },
                    mapZoomStart = safeDouble((map["mapZoomStart"] as? Number)?.toDouble() ?: 1.0, 0.5, 3.0, 1.0),
                    mapZoomEnd = safeDouble((map["mapZoomEnd"] as? Number)?.toDouble() ?: 1.25, 0.5, 3.0, 1.25),
                    mapPanStartX = safeDouble((map["mapPanStartX"] as? Number)?.toDouble() ?: 0.0, -1.0, 1.0, 0.0),
                    mapPanStartY = safeDouble((map["mapPanStartY"] as? Number)?.toDouble() ?: 0.0, -1.0, 1.0, 0.0),
                    mapPanEndX = safeDouble((map["mapPanEndX"] as? Number)?.toDouble() ?: 0.0, -1.0, 1.0, 0.0),
                    mapPanEndY = safeDouble((map["mapPanEndY"] as? Number)?.toDouble() ?: 0.0, -1.0, 1.0, 0.0),
                    mapMarkers = markers,
                    mapRoutes = routes
                )
            }
        }

        fun subtitleCuesToMaps(cues: List<SubtitleCue>): List<Map<String, Any>> {
            return cues.take(80).mapIndexed { idx, cue ->
                val start = safeDouble(cue.startSec, 0.0, 300.0, 0.0)
                val end = safeDouble(cue.endSec, start + 0.1, 300.0, (start + 2.5).coerceAtMost(300.0))
                mapOf(
                    "cueId" to cue.cueId.trim().take(64).ifBlank { "sub_$idx" },
                    "sceneId" to cue.sceneId.trim().take(128),
                    "dialogueLineId" to cue.dialogueLineId.trim().take(64),
                    "speakerName" to cue.speakerName.trim().take(100),
                    "text" to cue.text.trim().take(500),
                    "startSec" to start,
                    "endSec" to end,
                    "positionYNorm" to safeDouble(cue.positionYNorm, 0.05, 0.96, 0.88)
                )
            }
        }

        fun mapsToSubtitleCues(raw: List<Map<String, Any>>): List<SubtitleCue> {
            return raw.mapIndexed { idx, map ->
                val start = safeDouble((map["startSec"] as? Number)?.toDouble() ?: 0.0, 0.0, 300.0, 0.0)
                val end = safeDouble((map["endSec"] as? Number)?.toDouble() ?: (start + 2.5), start + 0.1, 300.0, start + 2.5)
                SubtitleCue(
                    cueId = (map["cueId"] as? String)?.take(64).orEmpty().ifBlank { "sub_$idx" },
                    sceneId = (map["sceneId"] as? String)?.take(128).orEmpty(),
                    dialogueLineId = (map["dialogueLineId"] as? String)?.take(64).orEmpty(),
                    speakerName = (map["speakerName"] as? String)?.take(100).orEmpty(),
                    text = (map["text"] as? String)?.take(500).orEmpty(),
                    startSec = start,
                    endSec = end,
                    positionYNorm = safeDouble((map["positionYNorm"] as? Number)?.toDouble() ?: 0.88, 0.05, 0.96, 0.88)
                )
            }
        }

        fun subtitleStyleToMap(s: SubtitleStyleConfig): Map<String, Any> = mapOf(
            "stylePreset" to s.stylePreset.trim().take(32).ifBlank { "DOCUMENTARY_CLEAN" },
            "fontFamilyKey" to s.fontFamilyKey.trim().take(32).ifBlank { "PLUS_JAKARTA" },
            "fontSizeSp" to safeDouble(s.fontSizeSp, 8.0, 36.0, 13.0),
            "textColorHex" to s.textColorHex.trim().take(16).ifBlank { "#F2F4F8" },
            "backgroundColorHex" to s.backgroundColorHex.trim().take(16).ifBlank { "#0A0C10" },
            "backgroundOpacity" to safeDouble(s.backgroundOpacity, 0.0, 1.0, 0.78),
            "outlineEnabled" to s.outlineEnabled,
            "outlineColorHex" to s.outlineColorHex.trim().take(16).ifBlank { "#000000" },
            "alignment" to s.alignment.trim().take(16).ifBlank { "CENTER" },
            "verticalPosition" to s.verticalPosition.trim().take(16).ifBlank { "BOTTOM" }
        )

        fun mapToSubtitleStyle(map: Map<String, Any>): SubtitleStyleConfig = SubtitleStyleConfig(
            stylePreset = (map["stylePreset"] as? String)?.take(32).orEmpty().ifBlank { "DOCUMENTARY_CLEAN" },
            fontFamilyKey = (map["fontFamilyKey"] as? String)?.take(32).orEmpty().ifBlank { "PLUS_JAKARTA" },
            fontSizeSp = safeDouble((map["fontSizeSp"] as? Number)?.toDouble() ?: 13.0, 8.0, 36.0, 13.0),
            textColorHex = (map["textColorHex"] as? String)?.take(16).orEmpty().ifBlank { "#F2F4F8" },
            backgroundColorHex = (map["backgroundColorHex"] as? String)?.take(16).orEmpty().ifBlank { "#0A0C10" },
            backgroundOpacity = safeDouble((map["backgroundOpacity"] as? Number)?.toDouble() ?: 0.78, 0.0, 1.0, 0.78),
            outlineEnabled = (map["outlineEnabled"] as? Boolean) ?: true,
            outlineColorHex = (map["outlineColorHex"] as? String)?.take(16).orEmpty().ifBlank { "#000000" },
            alignment = (map["alignment"] as? String)?.take(16).orEmpty().ifBlank { "CENTER" },
            verticalPosition = (map["verticalPosition"] as? String)?.take(16).orEmpty().ifBlank { "BOTTOM" }
        )

        fun sceneTransitionToMap(t: SceneTransitionConfig): Map<String, Any> = mapOf(
            "type" to t.type.trim().uppercase().take(24).ifBlank { "CUT" },
            "durationSec" to safeDouble(t.durationSec, 0.2, 3.0, 0.8),
            "direction" to t.direction.trim().uppercase().take(16).ifBlank { "LEFT" },
            "easing" to t.easing.trim().uppercase().take(24).ifBlank { "EASE_IN_OUT" },
            "feather" to safeDouble(t.feather, 0.0, 1.0, 0.25)
        )

        fun mapToSceneTransition(map: Map<String, Any>): SceneTransitionConfig = SceneTransitionConfig(
            type = (map["type"] as? String)?.trim()?.uppercase()?.take(24).orEmpty().ifBlank { "CUT" },
            durationSec = safeDouble((map["durationSec"] as? Number)?.toDouble() ?: 0.8, 0.2, 3.0, 0.8),
            direction = (map["direction"] as? String)?.trim()?.uppercase()?.take(16).orEmpty().ifBlank { "LEFT" },
            easing = (map["easing"] as? String)?.trim()?.uppercase()?.take(24).orEmpty().ifBlank { "EASE_IN_OUT" },
            feather = safeDouble((map["feather"] as? Number)?.toDouble() ?: 0.25, 0.0, 1.0, 0.25)
        )

        fun graphicSuggestionsToMaps(suggestions: List<AiGraphicSuggestion>): List<Map<String, Any>> {
            return suggestions.take(20).mapIndexed { idx, s ->
                mapOf(
                    "suggestionId" to s.suggestionId.trim().take(64).ifBlank { "gs_$idx" },
                    "category" to s.category.trim().take(32).ifBlank { "TITLE" },
                    "templateType" to s.templateType.trim().take(48).ifBlank { "CHAPTER_TITLE" },
                    "title" to s.title.trim().take(120),
                    "rationale" to s.rationale.trim().take(300),
                    "primaryText" to s.primaryText.trim().take(240),
                    "secondaryText" to s.secondaryText.trim().take(200),
                    "suggestedStartSec" to safeDouble(s.suggestedStartSec, 0.0, 300.0, 0.3),
                    "suggestedDurationSec" to safeDouble(s.suggestedDurationSec, 0.5, 60.0, 4.0),
                    "infographicType" to s.infographicType.trim().take(32),
                    "suggestedDataSummary" to s.suggestedDataSummary.trim().take(300),
                    "applied" to s.applied
                )
            }
        }

        fun mapsToGraphicSuggestions(raw: List<Map<String, Any>>): List<AiGraphicSuggestion> {
            return raw.mapIndexed { idx, map ->
                AiGraphicSuggestion(
                    suggestionId = (map["suggestionId"] as? String)?.take(64).orEmpty().ifBlank { "gs_$idx" },
                    category = (map["category"] as? String)?.take(32).orEmpty().ifBlank { "TITLE" },
                    templateType = (map["templateType"] as? String)?.take(48).orEmpty().ifBlank { "CHAPTER_TITLE" },
                    title = (map["title"] as? String)?.take(120).orEmpty(),
                    rationale = (map["rationale"] as? String)?.take(300).orEmpty(),
                    primaryText = (map["primaryText"] as? String)?.take(240).orEmpty(),
                    secondaryText = (map["secondaryText"] as? String)?.take(200).orEmpty(),
                    suggestedStartSec = safeDouble((map["suggestedStartSec"] as? Number)?.toDouble() ?: 0.3, 0.0, 300.0, 0.3),
                    suggestedDurationSec = safeDouble((map["suggestedDurationSec"] as? Number)?.toDouble() ?: 4.0, 0.5, 60.0, 4.0),
                    infographicType = (map["infographicType"] as? String)?.take(32).orEmpty(),
                    suggestedDataSummary = (map["suggestedDataSummary"] as? String)?.take(300).orEmpty(),
                    applied = (map["applied"] as? Boolean) ?: false
                )
            }
        }
    }
}
