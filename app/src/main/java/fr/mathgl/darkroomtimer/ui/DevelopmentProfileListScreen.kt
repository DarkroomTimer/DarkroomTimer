package fr.mathgl.darkroomtimer.ui

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedBright
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedDim
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedFaint
import fr.mathgl.darkroomtimer.ui.theme.DarkroomSurface
import fr.mathgl.darkroomtimer.ui.theme.DarkroomSurfaceElevated
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.mathgl.darkroomtimer.development.*
import fr.mathgl.darkroomtimer.storage.room.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevelopmentProfileListScreen(
    onNavigateToSession: (DevelopmentProfile) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var database by remember { mutableStateOf<AppDatabase?>(null) }
    var viewModel by remember { mutableStateOf<DevelopmentListViewModel?>(null) }

    // Initialize database and viewModel
    LaunchedEffect(Unit) {
        database = AppDatabase.getDatabase(context.applicationContext as Application, CoroutineScope(Dispatchers.Default))
        viewModel = database?.let {
            DevelopmentListViewModel(context.applicationContext as Application, it.developmentDao())
        }
        viewModel?.loadProfiles()
    }

    val profiles by (viewModel?.profiles ?: flowOf<List<DevelopmentProfile>>(emptyList())).collectAsState(initial = emptyList())
    val isLoading by (viewModel?.isLoading ?: flowOf(false)).collectAsState(initial = false)
    val selectedProfile by (viewModel?.selectedProfile ?: flowOf<DevelopmentProfile?>(null)).collectAsState(initial = null)
    val showEditor by (viewModel?.showEditor ?: flowOf(false)).collectAsState(initial = false)

    var showProfileEditor by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<DevelopmentProfile?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var profileToDelete by remember { mutableStateOf<DevelopmentProfile?>(null) }

    // Handle editor state from ViewModel
    LaunchedEffect(showEditor, selectedProfile) {
        if (showEditor) {
            editingProfile = selectedProfile
            showProfileEditor = true
        }
    }

    // Handle profile selection for session
    LaunchedEffect(selectedProfile) {
        if (selectedProfile != null && !showEditor) {
            onNavigateToSession(selectedProfile!!)
            viewModel?.deselectProfile()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Profils de D&#xe9;veloppement",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = DarkroomRedBright
            )
            TextButton(onClick = onBack) {
                Text("&#8592; Retour", color = DarkroomRedBright)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = DarkroomRedBright)
            }
        } else if (profiles.isEmpty()) {
            Text(
                text = "Aucun profil cr&#xe9;&#xe9;",
                color = DarkroomRedDim,
                fontSize = 16.sp,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(profiles) { profile ->
                    ProfileItem(
                        profile = profile,
                        onClick = { viewModel?.selectProfile(profile) },
                        onEdit = {
                            editingProfile = profile
                            showProfileEditor = true
                        },
                        onDelete = {
                            profileToDelete = profile
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                editingProfile = null
                showProfileEditor = true
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DarkroomRedBright)
        ) {
            Text("+ Nouveau Profil", fontSize = 18.sp)
        }
    }

    // Profile editor dialog
    if (showProfileEditor) {
        DevelopmentProfileEditorDialog(
            profile = editingProfile,
            onSave = { profile ->
                viewModel?.saveProfile(profile)
                showProfileEditor = false
                editingProfile = null
            },
            onDismiss = {
                showProfileEditor = false
                editingProfile = null
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteDialog && profileToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                profileToDelete = null
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        profileToDelete?.let { viewModel?.deleteProfile(it) }
                        showDeleteDialog = false
                        profileToDelete = null
                    }
                ) {
                    Text("SUPPRIMER", color = DarkroomRedBright)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    profileToDelete = null
                }) {
                    Text("ANNULER", color = DarkroomRedDim)
                }
            },
            title = { Text("Supprimer le profil", color = DarkroomRedBright) },
            text = {
                Text(
                    text = "&#xc9;tes-vous s&#xe9;ur de vouloir supprimer \"${profileToDelete?.name}\" ?",
                    color = DarkroomRedDim
                )
            },
            containerColor = DarkroomSurfaceElevated
        )
    }
}

@Composable
private fun ProfileItem(
    profile: DevelopmentProfile,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DarkroomSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.then(Modifier.fillMaxWidth())) {
                Text(
                    text = profile.name,
                    color = DarkroomRedBright,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = profile.preview(),
                    color = DarkroomRedDim,
                    fontSize = 12.sp,
                    maxLines = 1
                )
                Text(
                    text = "${profile.stepCount()} &#xe9;tapes • ${profile.navigationMode.name}",
                    color = DarkroomRedDim,
                    fontSize = 11.sp
                )
            }
            Row {
                TextButton(onClick = onEdit) {
                    Text("&#9998;", color = DarkroomRedBright, fontSize = 18.sp)
                }
                TextButton(onClick = onDelete) {
                    Text("&#10005;", color = DarkroomRedBright, fontSize = 18.sp)
                }
            }
        }
    }
}

/**
 * Dialog pour &#xe9;diter un profil de d&#xe9;veloppement.
 * Permet de cr&#xe9;er ou modifier un profil avec ses &#xe9;tapes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevelopmentProfileEditorDialog(
    profile: DevelopmentProfile?,
    onSave: (DevelopmentProfile) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(profile?.name ?: "") }
    var navigationMode by remember { mutableStateOf(profile?.navigationMode ?: DevelopmentNavigationMode.MANUAL) }
    var steps by remember { mutableStateOf(mutableListOf<DevelopmentStep>()) }

    // Initialize steps from profile
    LaunchedEffect(profile) {
        steps = profile?.steps?.toMutableList() ?: mutableListOf()
    }

    var showStepDialog by remember { mutableStateOf(false) }
    var editingStepIndex by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val newProfile = DevelopmentProfile(
                            id = profile?.id ?: 0,
                            name = name,
                            navigationMode = navigationMode,
                            steps = steps.toList(),
                            createdAt = profile?.createdAt ?: System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        onSave(newProfile)
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = DarkroomRedBright)
            ) {
                Text("ENREGISTRER")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ANNULER")
            }
        },
        title = {
            Text(
                text = if (profile != null) "&#xc9;diter le Profil" else "Nouveau Profil",
                color = DarkroomRedBright
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                // Profile name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom du profil", color = DarkroomRedBright) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkroomRedBright,
                        unfocusedBorderColor = DarkroomRedFaint,
                        cursorColor = DarkroomRedBright
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Navigation mode selector
                Text(
                    text = "Mode de navigation",
                    color = DarkroomRedBright,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NavigationModeDialogButton(
                        selected = navigationMode == DevelopmentNavigationMode.MANUAL,
                        label = "Manuel",
                        onClick = { navigationMode = DevelopmentNavigationMode.MANUAL }
                    )
                    NavigationModeDialogButton(
                        selected = navigationMode == DevelopmentNavigationMode.AUTOMATIC,
                        label = "Auto",
                        onClick = { navigationMode = DevelopmentNavigationMode.AUTOMATIC }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Steps list
                Text(
                    text = "&#xe9;tapes (${steps.count()})",
                    color = DarkroomRedBright,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (steps.isEmpty()) {
                    Text(
                        text = "Aucune &#xe9;tape ajout&#xe9;e",
                        color = DarkroomRedDim,
                        fontSize = 12.sp
                    )
                } else {
                    val stepsList = steps.toList()
                    stepsList.forEachIndexed { index, step ->
                        StepDialogItem(
                            step = step,
                            index = index,
                            onEdit = { editingStepIndex = index },
                            onDelete = {
                                val newList = steps.toMutableList()
                                newList.removeAt(index)
                                steps = newList
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        editingStepIndex = null
                        showStepDialog = true
                    },
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkroomSurface)
                ) {
                    Text("+ Ajouter &#xe9;tape", fontSize = 12.sp)
                }
            }
        },
        containerColor = DarkroomSurfaceElevated
    )

    // Step editor dialog
    if (showStepDialog) {
        val currentEditingIndex = editingStepIndex
        val stepToEdit = if (currentEditingIndex != null && currentEditingIndex in steps.indices) {
            steps[currentEditingIndex]
        } else null
        StepEditorDialog(
            step = stepToEdit,
            onSave = { newStep ->
                val idx = editingStepIndex
                if (idx != null && idx in steps.indices) {
                    val newList = steps.toMutableList()
                    newList[idx] = newStep
                    steps = newList
                } else {
                    steps.add(newStep)
                }
                showStepDialog = false
            },
            onDismiss = { showStepDialog = false }
        )
    }
}

@Composable
private fun NavigationModeDialogButton(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.then(
            Modifier.fillMaxWidth().height(40.dp)
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) DarkroomRedBright else DarkroomRedDim
        )
    ) {
        Text(label, fontSize = 12.sp, color = if (selected) DarkroomRedBright else DarkroomRedDim)
    }
}

@Composable
private fun StepDialogItem(
    step: DevelopmentStep,
    index: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        colors = CardDefaults.cardColors(containerColor = DarkroomSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.then(Modifier.fillMaxWidth())) {
                Row {
                    Text(
                        text = "${index + 1}. ",
                        color = DarkroomRedBright,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = step.name,
                        color = DarkroomRedBright,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
                Text(
                    text = "${step.durationSeconds}s • ${if (step is DevelopmentStep.BathStep) "Bain" else "Pause"}",
                    color = DarkroomRedDim,
                    fontSize = 10.sp
                )
            }
            TextButton(onClick = onDelete) {
                Text("&#10005;", color = DarkroomRedBright, fontSize = 14.sp)
            }
        }
    }
    Spacer(modifier = Modifier.height(6.dp))
}
