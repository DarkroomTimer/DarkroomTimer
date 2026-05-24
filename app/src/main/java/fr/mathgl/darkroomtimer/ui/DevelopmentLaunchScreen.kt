package fr.mathgl.darkroomtimer.ui

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.mathgl.darkroomtimer.development.*
import fr.mathgl.darkroomtimer.storage.room.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf

/**
 * Écran de lancement rapide pour le mode Développement.
 * Permet de sélectionner un profil et de confirmer le mode avant de commencer la session.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevelopmentLaunchScreen(
    onLaunchSession: (DevelopmentProfile) -> Unit,
    onNavigateToProfiles: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var database by remember { mutableStateOf<AppDatabase?>(null) }
    var viewModel by remember { mutableStateOf<DevelopmentListViewModel?>(null) }

    // Initialize database and viewModel
    LaunchedEffect(Unit) {
        database = AppDatabase.getDatabase(
            context.applicationContext as Application,
            CoroutineScope(Dispatchers.Default)
        )
        viewModel = database?.let {
            DevelopmentListViewModel(context.applicationContext as Application, it.developmentDao())
        }
        viewModel?.loadProfiles()
    }

    val profiles by (viewModel?.profiles ?: flowOf(emptyList())).collectAsState(initial = emptyList())
    val isLoading by (viewModel?.isLoading ?: flowOf(false)).collectAsState(initial = false)
    val selectedProfile by remember { mutableStateOf<DevelopmentProfile?>(null) }
    var showModeConfirmation by remember { mutableStateOf(false) }

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
                text = "Lancer le Développement",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            TextButton(onClick = onBack) {
                Text("<- Retour", color = Color(0xFFCC2200))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Mode indicator
        Text(
            text = "Sélectionnez un profil pour commencer",
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFCC2200))
            }
        } else if (profiles.isEmpty()) {
            // No profiles available
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Aucun profil disponible",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onNavigateToProfiles,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCC2200))
                    ) {
                        Text("Créer un profil", fontSize = 14.sp)
                    }
                }
            }
        } else {
            // Profile selection list
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(profiles) { profile ->
                    LaunchProfileItem(
                        profile = profile,
                        onClick = { /* Just show selection indicator */ }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Launch button
            Button(
                onClick = {
                    // Select the first profile by default or show a dialog
                    if (profiles.isNotEmpty()) {
                        showModeConfirmation = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = profiles.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (profiles.isNotEmpty()) Color(0xFF44AA44) else Color(0xFF333333)
                )
            ) {
                Text("LANCER LA SESSION", fontSize = 16.sp)
            }
        }
    }

    // Mode confirmation dialog
    if (showModeConfirmation && profiles.isNotEmpty()) {
        // For now, just launch with the first profile
        // In a future implementation, this could show mode selection options
        AlertDialog(
            onDismissRequest = { showModeConfirmation = false },
            confirmButton = {
                Button(
                    onClick = {
                        profiles.firstOrNull()?.let { profile ->
                            onLaunchSession(profile)
                            showModeConfirmation = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF44AA44))
                ) {
                    Text("DÉMARRER", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showModeConfirmation = false }) {
                    Text("ANNULER", color = Color.Gray)
                }
            },
            title = {
                Text(
                    text = "Démarrer la session",
                    color = Color.White
                )
            },
            text = {
                Column {
                    Text(
                        text = "Profil: ${profiles.firstOrNull()?.name ?: ""}",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Mode: ${profiles.firstOrNull()?.navigationMode?.name ?: ""}",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${profiles.firstOrNull()?.stepCount() ?: 0} étapes",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            },
            containerColor = Color(0xFF1A1A1A)
        )
    }
}

/**
 * Item pour afficher un profil dans la liste de sélection rapide.
 */
@Composable
private fun LaunchProfileItem(
    profile: DevelopmentProfile,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF222222))
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
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = profile.preview(),
                    color = Color.Gray,
                    fontSize = 11.sp,
                    maxLines = 1
                )
                Row {
                    Text(
                        text = "${profile.stepCount()} étapes",
                        color = Color(0xFF888888),
                        fontSize = 10.sp
                    )
                    Text(
                        text = "  •  ${if (profile.navigationMode == DevelopmentNavigationMode.AUTOMATIC) "Auto" else "Manuel"}",
                        color = Color(0xFF888888),
                        fontSize = 10.sp
                    )
                }
            }
            // Selection indicator
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        Color(0xFFCC2200),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = ">",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
