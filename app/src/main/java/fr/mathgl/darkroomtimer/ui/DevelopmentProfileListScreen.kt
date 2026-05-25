package fr.mathgl.darkroomtimer.ui

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedBright
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedDim
import fr.mathgl.darkroomtimer.ui.theme.DarkroomSurface
import fr.mathgl.darkroomtimer.ui.theme.DarkroomSurfaceElevated
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.mathgl.darkroomtimer.development.*
import fr.mathgl.darkroomtimer.storage.room.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf

@Composable
fun DevelopmentProfileListScreen(
    onSelectProfile: (DevelopmentProfile) -> Unit,
    onEditProfile: (DevelopmentProfile) -> Unit,
    onNewProfile: () -> Unit,
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

    var showDeleteDialog by remember { mutableStateOf(false) }
    var profileToDelete by remember { mutableStateOf<DevelopmentProfile?>(null) }

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
                text = "Profils de Développement",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = DarkroomRedBright
            )
            TextButton(onClick = onBack) {
                Text("← Retour", color = DarkroomRedBright)
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
                text = "Aucun profil créé",
                color = DarkroomRedDim,
                fontSize = 16.sp,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(profiles) { profile ->
                    ProfileItem(
                        profile = profile,
                        onClick = { onSelectProfile(profile) },
                        onEdit = { onEditProfile(profile) },
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
            onClick = onNewProfile,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DarkroomRedBright)
        ) {
            Text("+ Nouveau Profil", fontSize = 18.sp)
        }
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
            Column(modifier = Modifier.weight(1f)) {
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
                    Text("Edit", color = DarkroomRedBright, fontSize = 18.sp)
                }
                TextButton(onClick = onDelete) {
                    Text("Delete", color = DarkroomRedBright, fontSize = 18.sp)
                }
            }
        }
    }
}
