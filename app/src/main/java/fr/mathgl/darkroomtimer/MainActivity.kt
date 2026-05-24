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
import fr.mathgl.darkroomtimer.system.LuminosityManager
import fr.mathgl.darkroomtimer.ui.CountdownScreen
import fr.mathgl.darkroomtimer.ui.DevelopmentSessionScreen
import fr.mathgl.darkroomtimer.ui.TeststripScreen
import fr.mathgl.darkroomtimer.ui.DevelopmentProfileListScreen
import fr.mathgl.darkroomtimer.ui.DevelopmentLaunchScreen
import fr.mathgl.darkroomtimer.ui.theme.DarkroomTimerTheme

enum class AppMode { COUNTDOWN, TESTSTRIP, DEVELOPMENT }

/**
 * États du flux de développement dans MainActivity.
 * Permet de naviguer entre la liste des profils, l'écran de lancement et la session.
 */
enum class DevelopmentFlowState { LIST, LAUNCH, SESSION }

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

    if (selectedMode == null) {
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
                var developmentFlowState by rememberSaveable { mutableStateOf(DevelopmentFlowState.LIST) }
                var selectedProfile by rememberSaveable { mutableStateOf<DevelopmentProfile?>(null) }
                var developmentSession by remember { mutableStateOf<DevelopmentSession?>(null) }

                LaunchedEffect(developmentSession) {
                    while (developmentSession?.isRunning == true) {
                        kotlinx.coroutines.delay(1000)
                        developmentSession?.tick()
                    }
                }

                when (developmentFlowState) {
                    DevelopmentFlowState.LIST -> {
                        DevelopmentProfileListScreen(
                            onNavigateToSession = { profile ->
                                selectedProfile = profile
                                developmentSession = DevelopmentSession(profile)
                                developmentFlowState = DevelopmentFlowState.SESSION
                            },
                            onBack = { selectedMode = null }
                        )
                    }
                    DevelopmentFlowState.LAUNCH -> {
                        DevelopmentLaunchScreen(
                            onLaunchSession = { profile ->
                                selectedProfile = profile
                                developmentSession = DevelopmentSession(profile)
                                developmentFlowState = DevelopmentFlowState.SESSION
                            },
                            onNavigateToProfiles = {
                                developmentFlowState = DevelopmentFlowState.LIST
                            },
                            onBack = {
                                developmentFlowState = DevelopmentFlowState.LIST
                            }
                        )
                    }
                    DevelopmentFlowState.SESSION -> {
                        val session = developmentSession
                        if (session != null) {
                            val state = session.state
                            val currentStep = session.currentStep
                            DevelopmentSessionScreen(
                                stepName = currentStep?.name ?: "Étape",
                                stepElapsedSeconds = session.currentStepElapsedSeconds,
                                stepRemainingSeconds = session.currentStepRemainingSeconds,
                                progress = session.progress,
                                state = state,
                                totalSteps = session.totalSteps,
                                currentStepIndex = if (session.currentStepIndex >= 0) session.currentStepIndex + 1 else 0,
                                onStart = { session.start() },
                                onPause = { session.pause() },
                                onResume = { session.resume() },
                                onNextStep = { session.nextStep() },
                                onCancel = {
                                    session.cancel()
                                    developmentSession = null
                                    selectedProfile = null
                                    developmentFlowState = DevelopmentFlowState.LIST
                                }
                            )
                        } else {
                            DevelopmentProfileListScreen(
                                onNavigateToSession = { profile ->
                                    selectedProfile = profile
                                    developmentSession = DevelopmentSession(profile)
                                    developmentFlowState = DevelopmentFlowState.SESSION
                                },
                                onBack = { selectedMode = null }
                            )
                        }
                    }
                }
            }
            else -> ModeSelectorScreen()
        }
    }
}
