package fr.mathgl.darkroomtimer.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import fr.mathgl.darkroomtimer.ui.CountdownScreen
import fr.mathgl.darkroomtimer.ui.DevelopmentFlowViewModel
import fr.mathgl.darkroomtimer.ui.DevelopmentLaunchScreen
import fr.mathgl.darkroomtimer.ui.DevelopmentProfileListScreen
import fr.mathgl.darkroomtimer.ui.DevelopmentSessionScreen
import fr.mathgl.darkroomtimer.ui.EnlargerProfilesScreen
import fr.mathgl.darkroomtimer.ui.SettingsScreen
import fr.mathgl.darkroomtimer.ui.TeststripScreen
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedBright
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedDim
import fr.mathgl.darkroomtimer.ui.theme.DarkroomSurface

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute != AppRoutes.ENLARGER_PROFILES

    val navBarItems = listOf(
        Triple(AppRoutes.EXPOSITION, Icons.Default.Timer, "Exposition"),
        Triple(AppRoutes.TESTSTRIP, Icons.Default.GridOn, "Teststrip"),
        Triple(AppRoutes.DEVELOPMENT_GRAPH, Icons.Default.Science, "Développement"),
        Triple(AppRoutes.SETTINGS, Icons.Default.Settings, "Réglages"),
    )

    Scaffold(
        containerColor = Color.Black,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = DarkroomSurface) {
                    navBarItems.forEach { (route, icon, label) ->
                        val selected = backStackEntry?.destination?.hierarchy
                            ?.any { it.route == route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) },
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
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = AppRoutes.EXPOSITION
            ) {
                composable(AppRoutes.EXPOSITION) {
                    CountdownScreen()
                }

                composable(AppRoutes.TESTSTRIP) {
                    TeststripScreen(
                        onBack = {
                            navController.navigate(AppRoutes.EXPOSITION) {
                                popUpTo(AppRoutes.EXPOSITION) { inclusive = true }
                            }
                        }
                    )
                }

                composable(AppRoutes.SETTINGS) {
                    SettingsScreen(
                        onNavigateToEnlargerProfiles = {
                            navController.navigate(AppRoutes.ENLARGER_PROFILES)
                        }
                    )
                }

                composable(AppRoutes.ENLARGER_PROFILES) {
                    EnlargerProfilesScreen(
                        onBack = { navController.popBackStack() }
                    )
                }

                navigation(
                    startDestination = AppRoutes.DEVELOPMENT_LAUNCH,
                    route = AppRoutes.DEVELOPMENT_GRAPH
                ) {
                    composable(AppRoutes.DEVELOPMENT_LAUNCH) { backStackEntry ->
                        val devGraphEntry = remember(backStackEntry) {
                            navController.getBackStackEntry(AppRoutes.DEVELOPMENT_GRAPH)
                        }
                        val devVM: DevelopmentFlowViewModel = viewModel(devGraphEntry)
                        val selectedProfile by devVM.selectedProfile.collectAsState()

                        DevelopmentLaunchScreen(
                            initialProfile = selectedProfile,
                            onLaunchSession = { profile ->
                                devVM.startSession(profile)
                                navController.navigate(AppRoutes.DEVELOPMENT_SESSION)
                            },
                            onSelectProfile = {
                                navController.navigate(AppRoutes.DEVELOPMENT_LIST)
                            }
                        )
                    }

                    composable(AppRoutes.DEVELOPMENT_LIST) { backStackEntry ->
                        val devGraphEntry = remember(backStackEntry) {
                            navController.getBackStackEntry(AppRoutes.DEVELOPMENT_GRAPH)
                        }
                        val devVM: DevelopmentFlowViewModel = viewModel(devGraphEntry)

                        DevelopmentProfileListScreen(
                            onSelectProfile = { profile ->
                                devVM.setSelectedProfile(profile)
                                navController.popBackStack()
                            },
                            onBack = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable(AppRoutes.DEVELOPMENT_SESSION) { backStackEntry ->
                        val devGraphEntry = remember(backStackEntry) {
                            navController.getBackStackEntry(AppRoutes.DEVELOPMENT_GRAPH)
                        }
                        val devVM: DevelopmentFlowViewModel = viewModel(devGraphEntry)
                        val snapshot by devVM.sessionSnapshot.collectAsState()
                        val s = snapshot

                        if (s != null) {
                            DevelopmentSessionScreen(
                                stepName = s.currentStep?.name ?: "Étape",
                                stepElapsedSeconds = s.currentStep?.elapsedSeconds ?: 0L,
                                stepRemainingSeconds = s.currentStep?.let {
                                    it.remainingSeconds(it.elapsedSeconds)
                                } ?: 0,
                                progress = s.progress,
                                state = s.state,
                                totalSteps = s.totalSteps,
                                currentStepIndex = if (s.currentStepIndex >= 0) s.currentStepIndex + 1 else 0,
                                onStart = { devVM.sessionStart() },
                                onPause = { devVM.sessionPause() },
                                onResume = { devVM.sessionResume() },
                                onNextStep = { devVM.sessionNextStep() },
                                onCancel = {
                                    devVM.cancelSession()
                                    navController.popBackStack(
                                        AppRoutes.DEVELOPMENT_LAUNCH,
                                        inclusive = false
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
