package fr.mathgl.darkroomtimer.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.mathgl.darkroomtimer.development.DevelopmentProfile
import fr.mathgl.darkroomtimer.development.DevelopmentNavigationMode

@Composable
fun DevelopmentProfileListScreen(
    profiles: List<DevelopmentProfile>,
    onSelectProfile: (DevelopmentProfile) -> Unit,
    onAddProfile: () -> Unit,
    onEditProfile: (DevelopmentProfile) -> Unit,
    onDeleteProfile: (DevelopmentProfile) -> Unit,
    onBack: () -> Unit
) {
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
                color = Color.White
            )
            TextButton(onClick = onBack) {
                Text("← Retour", color = Color(0xFFCC2200))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (profiles.isEmpty()) {
            Text(
                text = "Aucun profil créé",
                color = Color.Gray,
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
                        onClick = { onSelectProfile(profile) },
                        onEdit = { onEditProfile(profile) },
                        onDelete = { onDeleteProfile(profile) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onAddProfile,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCC2200))
        ) {
            Text("+ Nouveau Profil", fontSize = 18.sp)
        }
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
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = profile.preview(),
                    color = Color.Gray,
                    fontSize = 12.sp,
                    maxLines = 1
                )
                Text(
                    text = "${profile.stepCount()} étapes • ${profile.navigationMode.name}",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
            Row {
                TextButton(onClick = onEdit) {
                    Text("✎", color = Color(0xFFCC2200), fontSize = 18.sp)
                }
                TextButton(onClick = onDelete) {
                    Text("✕", color = Color.Red, fontSize = 18.sp)
                }
            }
        }
    }
}
