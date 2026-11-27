package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.audio.AudioFileStorageManager
import com.example.audio.AudioPlaybackEngine
import com.example.audio.StudioTtsEngine
import com.example.data.CharacterImageService
import com.example.data.SceneVisualAiService
import com.example.data.ScriptAiService
import com.example.data.StudioRepository
import com.example.data.configureAppCheck
import com.example.ui.auth.AuthScreen
import com.example.ui.screens.MotionStoryStudioScreen
import com.example.ui.theme.MotionStoryStudioTheme
import com.example.viewmodel.StudioViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent before any UI or Firebase calls
        configureAppCheck(this, intent)

        // STEP 2: Enable dark edge-to-edge chrome matching the MotionStory Studio workspace
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        // STEP 3: Render auth-gated MotionStory Studio application
        setContent {
            MotionStoryStudioTheme {
                MotionStoryAppRoot()
            }
        }
    }
}

@Composable
private fun MotionStoryAppRoot() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        val uid = user.uid
        key(uid) {
            val studioViewModel: StudioViewModel = viewModel(
                key = uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) {
                            "APPLICATION_KEY missing from CreationExtras"
                        }
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        StudioViewModel(
                            repository = StudioRepository(db),
                            aiService = ScriptAiService(),
                            characterImageService = CharacterImageService(),
                            visualAiService = SceneVisualAiService(),
                            currentUserId = uid,
                            audioStorageManager = AudioFileStorageManager(app),
                            ttsEngine = StudioTtsEngine(app),
                            audioPlaybackEngine = AudioPlaybackEngine()
                        )
                    }
                }
            )
            MotionStoryStudioScreen(viewModel = studioViewModel)
        }
    }
}
