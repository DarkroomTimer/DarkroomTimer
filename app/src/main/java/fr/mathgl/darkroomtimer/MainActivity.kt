package fr.mathgl.darkroomtimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedBright
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedDim
import fr.mathgl.darkroomtimer.ui.theme.DarkroomSurface
import fr.mathgl.darkroomtimer.development.DevelopmentProfile
import fr.mathgl.darkroomtimer.development.DevelopmentSession
import fr.mathgl.darkroomtimer.storage.PreferenceManager
import fr.mathgl.darkroomtimer.system.LuminosityManager
import fr.mathgl.darkroomtimer.ui.*
import fr.mathgl.darkroomtimer.ui.theme.DarkroomTimerTheme

enum class AppTab { EXPOSITION, TESTSTRIP, DEVELOPMENT, SETTINGS }
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
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    MainScreen()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val prefs = PreferenceManager.getInstance(this)
        luminosityManager.setConfig(
            LuminosityManager.Config(
                mode = if (prefs.luminosityMode == "FIXED") LuminosityManager.Mode.FIXED
                       else LuminosityManager.Mode.ADAPTIVE,
                minBrightness = prefs.luminosityMin,
                maxBrightness = prefs.luminosityMax,
                fixedBrightness = prefs.luminosityFixed
            )
        )
        luminosityManager.start()
    }
    override fun onStop() { super.onStop(); luminosityManager.stop() }
}

@Composable
fun MainScreen() {
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.EXPOSITION) }

    var devFlowState by rememberSaveable { mutableStateOf(DevelopmentFlowState.LAUNCH) }
    var selectedProfile by remember { mutableStateOf<DevelopmentProfile?>(null) }
    var developmentSession by remember { mutableStateOf<DevelopmentSession?>(null) }

    var showEnlargerProfiles by rememberSaveable { mutableStateOf(false) }

    if (showEnlargerProfiles) {
        EnlargerProfilesScreen(onBack = { showEnlargerProfiles = false })
        return
    }

    Scaffold(
        containerColor = Color.Black,
        bottomBar = {
            NavigationBar(containerColor = DarkroomSurface) {
                NavigationBarItem(
                    selected = selectedTab == AppTab.EXPOSITION,
                    onClick = { selectedTab = AppTab.EXPOSITION },
                    icon = { Icon(Icons.Default.Timer, contentDescription = "Exposition") },
                    label = { Text("Exposition") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DarkroomRedBright,
                        selectedTextColor = DarkroomRedBright,
                        indicatorColor = DarkroomSurface,
                        unselectedIconColor = DarkroomRedDim,
                        unselectedTextColor = DarkroomRedDim
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.TESTSTRIP,
                    onClick = { selectedTab = AppTab.TESTSTRIP },
                    icon = { Icon(Icons.Default.GridOn, contentDescription = "Teststrip") },
                    label = { Text("Teststrip") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DarkroomRedBright,
                        selectedTextColor = DarkroomRedBright,
                        indicatorColor = DarkroomSurface,
                        unselectedIconColor = DarkroomRedDim,
                        unselectedTextColor = DarkroomRedDim
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.DEVELOPMENT,
                    onClick = { selectedTab = AppTab.DEVELOPMENT },
                    icon = { Icon(Icons.Default.Science, contentDescription = "Développement") },
                    label = { Text("Développement") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DarkroomRedBright,
                        selectedTextColor = DarkroomRedBright,
                        indicatorColor = DarkroomSurface,
                        unselectedIconColor = DarkroomRedDim,
                        unselectedTextColor = DarkroomRedDim
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.SETTINGS,
                    onClick = { selectedTab = AppTab.SETTINGS },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Réglages") },
                    label = { Text("Réglages") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DarkroomRedBright,
                        selectedTextColor = DarkroomRedBright,
                        indicatorColor = DarkroomSurface,
                        unselectedIconColor = DarkroomRedDim,
                        unselectedTextColor = DarkroomRedDim
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                AppTab.EXPOSITION -> CountdownScreen()
                AppTab.TESTSTRIP -> TeststripScreen(onBack = { selectedTab = AppTab.EXPOSITION })
                AppTab.DEVELOPMENT -> DevelopmentOverlay(
                    devFlowState = devFlowState,
                    selectedProfile = selectedProfile,
                    developmentSession = developmentSession,
                    onDevFlowStateChange = { devFlowState = it },
                    onSelectedProfileChange = { selectedProfile = it },
                    onDevelopmentSessionChange = { developmentSession = it },
                    onExit = {
                        developmentSession = null
                        selectedProfile = null
                        devFlowState = DevelopmentFlowState.LAUNCH
                    }
                )
                AppTab.SETTINGS -> SettingsScreen(
                    onNavigateToEnlargerProfiles = { showEnlargerProfiles = true }
                )
            }
        }
    }
}

@Composable
private fun DevelopmentOverlay(
    devFlowState: DevelopmentFlowState,
    selectedProfile: DevelopmentProfile?,
    developmentSession: DevelopmentSession?,
    onDevFlowStateChange: (DevelopmentFlowState) -> Unit,
    onSelectedProfileChange: (DevelopmentProfile?) -> Unit,
    onDevelopmentSessionChange: (DevelopmentSession?) -> Unit,
    onExit: () -> Unit
) {
    when (devFlowState) {
        DevelopmentFlowState.LIST -> {
            DevelopmentProfileListScreen(
                onSelectProfile = { profile ->
                    onSelectedProfileChange(profile)
                    onDevFlowStateChange(DevelopmentFlowState.LAUNCH)
                },
                onBack = onExit
            )
        }
        DevelopmentFlowState.LAUNCH -> {
            DevelopmentLaunchScreen(
                initialProfile = selectedProfile,
                onLaunchSession = { profile ->
                    onSelectedProfileChange(profile)
                    onDevelopmentSessionChange(DevelopmentSession(profile))
                    onDevFlowStateChange(DevelopmentFlowState.SESSION)
                },
                onSelectProfile = { onDevFlowStateChange(DevelopmentFlowState.LIST) }
            )
        }
        DevelopmentFlowState.SESSION -> {
            val session = developmentSession
            if (session != null) {
                val snapshot by session.stateFlow.collectAsState()

                LaunchedEffect(snapshot.state) {
                    while (session.isRunning) {
                        kotlinx.coroutines.delay(1000)
                        session.tick()
                    }
                }

                DevelopmentSessionScreen(
                    stepName = snapshot.currentStep?.name ?: "Étape",
                    stepElapsedSeconds = snapshot.currentStep?.elapsedSeconds ?: 0L,
                    stepRemainingSeconds = snapshot.currentStep?.let { it.remainingSeconds(it.elapsedSeconds) } ?: 0,
                    progress = snapshot.progress,
                    state = snapshot.state,
                    totalSteps = snapshot.totalSteps,
                    currentStepIndex = if (snapshot.currentStepIndex >= 0) snapshot.currentStepIndex + 1 else 0,
                    onStart = { session.start() },
                    onPause = { session.pause() },
                    onResume = { session.resume() },
                    onNextStep = { session.nextStep() },
                    onCancel = onExit
                )
            } else {
                LaunchedEffect(Unit) {
                    onDevFlowStateChange(DevelopmentFlowState.LAUNCH)
                }
            }
        }
    }
}
