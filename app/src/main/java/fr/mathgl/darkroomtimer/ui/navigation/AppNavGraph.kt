package fr.mathgl.darkroomtimer.ui.navigation

import android.app.Application
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import fr.mathgl.darkroomtimer.development.DevelopmentListViewModel
import fr.mathgl.darkroomtimer.storage.PreferenceManager
import fr.mathgl.darkroomtimer.storage.room.AppDatabase
import androidx.navigation.NavType
import androidx.navigation.navArgument
import fr.mathgl.darkroomtimer.ui.BurnDodgeEntryEditorScreen
import fr.mathgl.darkroomtimer.ui.BurnDodgeStepsScreen
import fr.mathgl.darkroomtimer.ui.CountdownScreen
import fr.mathgl.darkroomtimer.ui.CountdownViewModel
import fr.mathgl.darkroomtimer.ui.DevelopmentFlowViewModel
import fr.mathgl.darkroomtimer.ui.DevelopmentLaunchScreen
import fr.mathgl.darkroomtimer.ui.DevelopmentProfileEditorScreen
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
        && currentRoute != AppRoutes.DEVELOPMENT_PROFILE_EDITOR
        && currentRoute != AppRoutes.BURN_DODGE_STEPS
        && currentRoute?.startsWith(AppRoutes.BURN_DODGE_ENTRY_EDITOR) != true

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
                    CountdownScreen(
                        onNavigateToBurnDodgeSteps = {
                            navController.navigate(AppRoutes.BURN_DODGE_STEPS)
                        }
                    )
                }

                composable(AppRoutes.BURN_DODGE_STEPS) { backStackEntry ->
                    val expositionEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(AppRoutes.EXPOSITION)
                    }
                    val vm: CountdownViewModel = viewModel(expositionEntry)
                    BurnDodgeStepsScreen(
                        viewModel = vm,
                        onBack = { navController.popBackStack() },
                        onNavigateToAddEntry = {
                            navController.navigate(AppRoutes.BURN_DODGE_ENTRY_EDITOR)
                        },
                        onNavigateToEditEntry = { id ->
                            navController.navigate("${AppRoutes.BURN_DODGE_ENTRY_EDITOR}?entryId=$id")
                        }
                    )
                }

                composable(
                    route = "${AppRoutes.BURN_DODGE_ENTRY_EDITOR}?entryId={entryId}",
                    arguments = listOf(
                        navArgument("entryId") { type = NavType.IntType; defaultValue = -1 }
                    )
                ) { backStackEntry ->
                    val entryId = backStackEntry.arguments?.getInt("entryId") ?: -1
                    val expositionEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(AppRoutes.EXPOSITION)
                    }
                    val vm: CountdownViewModel = viewModel(expositionEntry)
                    BurnDodgeEntryEditorScreen(
                        entryId = entryId,
                        viewModel = vm,
                        onBack = { navController.popBackStack() }
                    )
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
                        val context = LocalContext.current
                        val prefManager = remember { PreferenceManager.getInstance(context) }

                        DevelopmentLaunchScreen(
                            initialProfile = selectedProfile,
                            defaultProfileId = prefManager.defaultDevelopmentProfileId,
                            onLaunchSession = { profile ->
                                devVM.startSession(profile)
                                devVM.sessionStart()
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
                        val context = LocalContext.current
                        val prefManager = remember { PreferenceManager.getInstance(context) }
                        var defaultProfileId by remember { mutableStateOf(prefManager.defaultDevelopmentProfileId) }

                        DevelopmentProfileListScreen(
                            defaultProfileId = defaultProfileId,
                            onSelectProfile = { profile ->
                                devVM.setSelectedProfile(profile)
                                navController.popBackStack()
                            },
                            onEditProfile = { profile ->
                                devVM.setEditingProfile(profile)
                                navController.navigate(AppRoutes.DEVELOPMENT_PROFILE_EDITOR)
                            },
                            onNewProfile = {
                                devVM.setEditingProfile(null)
                                navController.navigate(AppRoutes.DEVELOPMENT_PROFILE_EDITOR)
                            },
                            onSetDefault = { id ->
                                prefManager.defaultDevelopmentProfileId = id
                                defaultProfileId = id
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
                                currentStepIndex = maxOf(0, s.currentStepIndex),
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

                    composable(AppRoutes.DEVELOPMENT_PROFILE_EDITOR) { backStackEntry ->
                        val devGraphEntry = remember(backStackEntry) {
                            navController.getBackStackEntry(AppRoutes.DEVELOPMENT_GRAPH)
                        }
                        val devVM: DevelopmentFlowViewModel = viewModel(devGraphEntry)
                        val editingProfile by devVM.editingProfile.collectAsState()

                        val context = LocalContext.current
                        val app = context.applicationContext as Application
                        val scope = rememberCoroutineScope()
                        val listVM = remember(app) {
                            val db = AppDatabase.getDatabase(app, scope)
                            DevelopmentListViewModel(app, db.developmentDao())
                        }

                        DevelopmentProfileEditorScreen(
                            profile = editingProfile,
                            onSave = { profile ->
                                listVM.saveProfile(profile)
                                devVM.clearEditingProfile()
                                navController.popBackStack()
                            },
                            onCancel = {
                                devVM.clearEditingProfile()
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }
}
