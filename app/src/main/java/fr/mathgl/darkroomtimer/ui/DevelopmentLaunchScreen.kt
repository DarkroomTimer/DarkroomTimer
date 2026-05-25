package fr.mathgl.darkroomtimer.ui

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.mathgl.darkroomtimer.development.DevelopmentProfile
import fr.mathgl.darkroomtimer.storage.room.AppDatabase
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedBright
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedDim
import fr.mathgl.darkroomtimer.ui.theme.DarkroomSurfaceElevated
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first

/**
 * Écran principal du mode Développement.
 * Affiche le profil par défaut (ou le profil sélectionné depuis la liste).
 * Le bouton "← Sélectionner un profil" revient à la liste de sélection.
 */
@Composable
fun DevelopmentLaunchScreen(
    initialProfile: DevelopmentProfile?,
    onLaunchSession: (DevelopmentProfile) -> Unit,
    onSelectProfile: () -> Unit
) {
    val context = LocalContext.current
    var resolvedProfile by remember { mutableStateOf(initialProfile) }
    var isLoading by remember { mutableStateOf(initialProfile == null) }

    LaunchedEffect(initialProfile) {
        if (initialProfile != null) {
            resolvedProfile = initialProfile
            isLoading = false
        } else {
            isLoading = true
            val db = AppDatabase.getDatabase(
                context.applicationContext as Application,
                CoroutineScope(Dispatchers.Default)
            )
            val entities = db.developmentDao().getAllProfiles().first()
            val profiles = entities.map { it.toDomain() }.sortedBy { it.name }
            resolvedProfile = profiles.firstOrNull()
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Développement",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = DarkroomRedBright
            )
            TextButton(onClick = onSelectProfile) {
                Text("← Sélectionner un profil", color = DarkroomRedBright)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = DarkroomRedBright)
                }
            }
            resolvedProfile == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Aucun profil disponible",
                            color = DarkroomRedDim,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onSelectProfile,
                            colors = ButtonDefaults.buttonColors(containerColor = DarkroomRedBright)
                        ) {
                            Text("Créer un profil", fontSize = 14.sp)
                        }
                    }
                }
            }
            else -> {
                val profile = resolvedProfile!!

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkroomSurfaceElevated)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = profile.name,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkroomRedBright
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${profile.stepCount()} étapes • ${profile.navigationMode.name}",
                            fontSize = 14.sp,
                            color = DarkroomRedDim
                        )
                        if (profile.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = profile.preview(),
                                fontSize = 12.sp,
                                color = DarkroomRedDim,
                                maxLines = 3
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { onLaunchSession(profile) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    enabled = profile.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkroomRedBright,
                        disabledContainerColor = DarkroomRedDim
                    )
                ) {
                    Text(
                        text = "LANCER LA SESSION",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
