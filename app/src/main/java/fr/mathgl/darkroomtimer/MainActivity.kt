package fr.mathgl.darkroomtimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import fr.mathgl.darkroomtimer.development.DevelopmentProfile
import fr.mathgl.darkroomtimer.development.DevelopmentSession
import fr.mathgl.darkroomtimer.development.DevelopmentSessionState
import fr.mathgl.darkroomtimer.system.LuminosityManager
import fr.mathgl.darkroomtimer.ui.CountdownScreen
import fr.mathgl.darkroomtimer.ui.DevelopmentSessionScreen
import fr.mathgl.darkroomtimer.ui.TeststripScreen
import fr.mathgl.darkroomtimer.ui.theme.DarkroomTimerTheme

enum class AppMode { COUNTDOWN, TESTSTRIP, DEVELOPMENT }

class MainActivity : ComponentActivity() {
    private lateinit var luminosityManager: LuminosityManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        luminosityManager = LuminosityManager(this)
        luminosityManager.setWindow(window)

        enableEdgeToEdge()
        setContent {
            DarkroomTimerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    ModeSelectorScreen()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        luminosityManager.start()
    }

    override fun onStop() {
        super.onStop()
        luminosityManager.stop()
    }
}

@Composable
fun ModeSelectorScreen() {
    var selectedMode by rememberSaveable { mutableStateOf<AppMode?>(null) }
    var selectedDevelopmentProfileState by rememberSaveable { mutableStateOf<DevelopmentProfile?>(null) }

    if (selectedMode == null) {
        // Mode selection screen
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "DarkroomTimer",
                color = Color.White,
                fontSize = 32.sp
            )
            Spacer(modifier = Modifier.width(0.dp))
            Spacer(modifier = Modifier.width(0.dp))
            Button(
                onClick = { selectedMode = AppMode.COUNTDOWN },
                modifier = Modifier.width(200.dp)
            ) {
                Text("Countdown", color = Color.Black)
            }
            Spacer(modifier = Modifier.width(0.dp))
            Spacer(modifier = Modifier.width(0.dp))
            Button(
                onClick = { selectedMode = AppMode.TESTSTRIP },
                modifier = Modifier.width(200.dp)
            ) {
                Text("Teststrip", color = Color.Black)
            }
            Spacer(modifier = Modifier.width(0.dp))
            Spacer(modifier = Modifier.width(0.dp))
            Button(
                onClick = { selectedMode = AppMode.DEVELOPMENT },
                modifier = Modifier.width(200.dp)
            ) {
                Text("Développement", color = Color.Black)
            }
        }
    } else {
        when (selectedMode) {
            AppMode.COUNTDOWN -> CountdownScreen()
            AppMode.TESTSTRIP -> TeststripScreen(onBack = { selectedMode = null })
            AppMode.DEVELOPMENT -> {
                val profile = selectedDevelopmentProfileState
                if (profile != null) {
                    val firstStep = profile.steps.firstOrNull()
                    DevelopmentSessionScreen(
                        stepName = firstStep?.name ?: "Étape",
                        stepElapsedSeconds = 0,
                        stepRemainingSeconds = firstStep?.durationSeconds ?: 0,
                        progress = 0,
                        state = DevelopmentSessionState.CONFIGURED,
                        totalSteps = profile.stepCount(),
                        currentStepIndex = -1,
                        onStart = { /* TODO: Start session */ },
                        onPause = { /* TODO: Pause session */ },
                        onResume = { /* TODO: Resume session */ },
                        onNextStep = { /* TODO: Next step */ },
                        onCancel = { selectedDevelopmentProfileState = null }
                    )
                } else {
                    // Show profile selection placeholder
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Mode Développement",
                            color = Color.White,
                            fontSize = 24.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Sélectionnez un profil pour commencer",
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = { /* TODO: Navigate to profile list */ },
                            modifier = Modifier.width(200.dp)
                        ) {
                            Text("Choisir un profil", color = Color.Black)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(onClick = { selectedMode = null }) {
                            Text("← Retour", color = Color(0xFFCC2200))
                        }
                    }
                }
            }
            else -> ModeSelectorScreen()
        }
    }
}
