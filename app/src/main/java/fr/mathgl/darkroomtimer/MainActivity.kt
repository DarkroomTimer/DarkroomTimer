package fr.mathgl.darkroomtimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.mathgl.darkroomtimer.development.DevelopmentProfile
import fr.mathgl.darkroomtimer.development.DevelopmentSession
import fr.mathgl.darkroomtimer.system.LuminosityManager
import fr.mathgl.darkroomtimer.ui.*
import fr.mathgl.darkroomtimer.ui.theme.DarkroomTimerTheme

enum class AppTab { EXPOSITION, TESTSTRIP, SETTINGS }
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

    override fun onStart() { super.onStart(); luminosityManager.start() }
    override fun onStop() { super.onStop(); luminosityManager.stop() }
}

@Composable
fun MainScreen() {
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.EXPOSITION) }

    var developmentActive by rememberSaveable { mutableStateOf(false) }
    var devFlowState by rememberSaveable { mutableStateOf(DevelopmentFlowState.LIST) }
    var selectedProfile by rememberSaveable { mutableStateOf<DevelopmentProfile?>(null) }
    var developmentSession by remember { mutableStateOf<DevelopmentSession?>(null) }

    var showEnlargerProfiles by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(developmentSession) {
        while (developmentSession?.isRunning == true) {
            kotlinx.coroutines.delay(1000)
            developmentSession?.tick()
        }
    }

    if (developmentActive) {
        DevelopmentOverlay(
            devFlowState = devFlowState,
            selectedProfile = selectedProfile,
            developmentSession = developmentSession,
            onDevFlowStateChange = { devFlowState = it },
            onSelectedProfileChange = { selectedProfile = it },
            onDevelopmentSessionChange = { developmentSession = it },
            onExit = {
                developmentActive = false
                developmentSession = null
                selectedProfile = null
                devFlowState = DevelopmentFlowState.LIST
            }
        )
        return
    }

    if (showEnlargerProfiles) {
        EnlargerProfilesScreen(onBack = { showEnlargerProfiles = false })
        return
    }

    Scaffold(
        containerColor = Color.Black,
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF0D0D0D)) {
                NavigationBarItem(
                    selected = selectedTab == AppTab.EXPOSITION,
                    onClick = { selectedTab = AppTab.EXPOSITION },
                    icon = { Icon(Icons.Default.Timer, contentDescription = "Exposition") },
                    label = { Text("Exposition") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFCC2200),
                        selectedTextColor = Color(0xFFCC2200),
                        indicatorColor = Color(0xFF1A0A0A),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.TESTSTRIP,
                    onClick = { selectedTab = AppTab.TESTSTRIP },
                    icon = { Icon(Icons.Default.GridOn, contentDescription = "Teststrip") },
                    label = { Text("Teststrip") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFCC2200),
                        selectedTextColor = Color(0xFFCC2200),
                        indicatorColor = Color(0xFF1A0A0A),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.SETTINGS,
                    onClick = { selectedTab = AppTab.SETTINGS },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Réglages") },
                    label = { Text("Réglages") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFCC2200),
                        selectedTextColor = Color(0xFFCC2200),
                        indicatorColor = Color(0xFF1A0A0A),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
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
                AppTab.EXPOSITION -> ExpositionTab(
                    onOpenDevelopment = { developmentActive = true }
                )
                AppTab.TESTSTRIP -> TeststripScreen(onBack = { selectedTab = AppTab.EXPOSITION })
                AppTab.SETTINGS -> SettingsScreen(
                    onNavigateToEnlargerProfiles = { showEnlargerProfiles = true }
                )
            }
        }
    }
}

@Composable
private fun ExpositionTab(onOpenDevelopment: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        CountdownScreen()
        FloatingActionButton(
            onClick = onOpenDevelopment,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = Color(0xFF222222),
            contentColor = Color.White
        ) {
            Text("Dev", fontSize = 12.sp, color = Color.White)
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
                onNavigateToSession = { profile ->
                    onSelectedProfileChange(profile)
                    onDevelopmentSessionChange(DevelopmentSession(profile))
                    onDevFlowStateChange(DevelopmentFlowState.SESSION)
                },
                onBack = onExit
            )
        }
        DevelopmentFlowState.LAUNCH -> {
            DevelopmentLaunchScreen(
                onLaunchSession = { profile ->
                    onSelectedProfileChange(profile)
                    onDevelopmentSessionChange(DevelopmentSession(profile))
                    onDevFlowStateChange(DevelopmentFlowState.SESSION)
                },
                onNavigateToProfiles = { onDevFlowStateChange(DevelopmentFlowState.LIST) },
                onBack = { onDevFlowStateChange(DevelopmentFlowState.LIST) }
            )
        }
        DevelopmentFlowState.SESSION -> {
            val session = developmentSession
            if (session != null) {
                DevelopmentSessionScreen(
                    stepName = session.currentStep?.name ?: "Étape",
                    stepElapsedSeconds = session.currentStepElapsedSeconds,
                    stepRemainingSeconds = session.currentStepRemainingSeconds,
                    progress = session.progress,
                    state = session.state,
                    totalSteps = session.totalSteps,
                    currentStepIndex = if (session.currentStepIndex >= 0) session.currentStepIndex + 1 else 0,
                    onStart = { session.start() },
                    onPause = { session.pause() },
                    onResume = { session.resume() },
                    onNextStep = { session.nextStep() },
                    onCancel = onExit
                )
            } else {
                onDevFlowStateChange(DevelopmentFlowState.LIST)
            }
        }
    }
}
