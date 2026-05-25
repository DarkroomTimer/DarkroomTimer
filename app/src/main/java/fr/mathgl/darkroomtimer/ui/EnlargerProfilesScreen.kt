package fr.mathgl.darkroomtimer.ui

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import fr.mathgl.darkroomtimer.ui.theme.DarkroomBlack
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedBright
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedDim
import fr.mathgl.darkroomtimer.ui.theme.DarkroomSurfaceElevated
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.mathgl.darkroomtimer.storage.room.AppDatabase
import fr.mathgl.darkroomtimer.storage.room.EnlargerProfileEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun EnlargerProfilesScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var profiles by remember { mutableStateOf<List<EnlargerProfileEntity>>(emptyList()) }
    var editingProfile by remember { mutableStateOf<EnlargerProfileEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    val db = remember {
        AppDatabase.getDatabase(
            context.applicationContext as Application,
            CoroutineScope(Dispatchers.Default)
        )
    }
    val dao = remember { db.enlargerProfileDao() }

    LaunchedEffect(Unit) {
        profiles = dao.getAll().sortedBy { it.id }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkroomBlack)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Profils Agrandisseur",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = DarkroomRedBright
            )
            TextButton(onClick = onBack) {
                Text("← Retour", color = DarkroomRedBright)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(profiles, key = { it.id }) { profile ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkroomSurfaceElevated)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile.name,
                                color = DarkroomRedBright,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Allumage: ${profile.turnOnDelayMs}ms / Extinction: ${profile.turnOffDelayMs}ms",
                                color = DarkroomRedDim,
                                fontSize = 11.sp
                            )
                        }
                        Row {
                            TextButton(onClick = {
                                editingProfile = profile
                                showEditor = true
                            }) {
                                Text("✎", color = DarkroomRedBright, fontSize = 18.sp)
                            }
                            if (profile.id != 0) {
                                TextButton(onClick = {
                                    scope.launch(Dispatchers.IO) {
                                        dao.delete(profile)
                                        val updated = dao.getAll().sortedBy { it.id }
                                        profiles = updated
                                    }
                                }) {
                                    Text("✕", color = DarkroomRedBright, fontSize = 18.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val nextId = (profiles.maxOfOrNull { it.id } ?: 0) + 1
                if (nextId <= 15) {
                    editingProfile = EnlargerProfileEntity(
                        id = nextId,
                        name = "",
                        turnOnDelayMs = 0,
                        riseTimeMs = 0,
                        riseTimeEquivMs = 0,
                        turnOffDelayMs = 0,
                        fallTimeMs = 0,
                        fallTimeEquivMs = 0
                    )
                    showEditor = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DarkroomRedBright),
            enabled = profiles.size < 16
        ) {
            Text("+ Nouveau Profil", fontSize = 16.sp)
        }
    }

    if (showEditor) {
        val profile = editingProfile ?: return
        EnlargerProfileEditorDialog(
            profile = profile,
            onSave = { saved ->
                scope.launch(Dispatchers.IO) {
                    dao.insert(saved)
                    val updated = dao.getAll().sortedBy { it.id }
                    profiles = updated
                }
                showEditor = false
                editingProfile = null
            },
            onDismiss = {
                showEditor = false
                editingProfile = null
            }
        )
    }
}

@Composable
private fun EnlargerProfileEditorDialog(
    profile: EnlargerProfileEntity,
    onSave: (EnlargerProfileEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(profile.name) }
    var turnOnDelayMs by remember { mutableStateOf(profile.turnOnDelayMs.toString()) }
    var riseTimeMs by remember { mutableStateOf(profile.riseTimeMs.toString()) }
    var riseTimeEquivMs by remember { mutableStateOf(profile.riseTimeEquivMs.toString()) }
    var turnOffDelayMs by remember { mutableStateOf(profile.turnOffDelayMs.toString()) }
    var fallTimeMs by remember { mutableStateOf(profile.fallTimeMs.toString()) }
    var fallTimeEquivMs by remember { mutableStateOf(profile.fallTimeEquivMs.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (profile.name.isEmpty()) "Nouveau Profil" else profile.name,
                color = DarkroomRedBright
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileTextField("Nom", name, { name = it }, KeyboardType.Text)
                Text("Allumage", color = DarkroomRedBright, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                ProfileTextField("Délai ON (ms)", turnOnDelayMs, { turnOnDelayMs = it }, KeyboardType.Number)
                ProfileTextField("Temps montée (ms)", riseTimeMs, { riseTimeMs = it }, KeyboardType.Number)
                ProfileTextField("Montée équiv. (ms)", riseTimeEquivMs, { riseTimeEquivMs = it }, KeyboardType.Number)
                Text("Extinction", color = DarkroomRedBright, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                ProfileTextField("Délai OFF (ms)", turnOffDelayMs, { turnOffDelayMs = it }, KeyboardType.Number)
                ProfileTextField("Temps descente (ms)", fallTimeMs, { fallTimeMs = it }, KeyboardType.Number)
                ProfileTextField("Descente équiv. (ms)", fallTimeEquivMs, { fallTimeEquivMs = it }, KeyboardType.Number)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            profile.copy(
                                name = name,
                                turnOnDelayMs = turnOnDelayMs.toIntOrNull() ?: 0,
                                riseTimeMs = riseTimeMs.toIntOrNull() ?: 0,
                                riseTimeEquivMs = riseTimeEquivMs.toIntOrNull() ?: 0,
                                turnOffDelayMs = turnOffDelayMs.toIntOrNull() ?: 0,
                                fallTimeMs = fallTimeMs.toIntOrNull() ?: 0,
                                fallTimeEquivMs = fallTimeEquivMs.toIntOrNull() ?: 0
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DarkroomRedBright)
            ) {
                Text("ENREGISTRER")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ANNULER") }
        },
        containerColor = DarkroomSurfaceElevated
    )
}

@Composable
private fun ProfileTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = DarkroomRedDim, fontSize = 12.sp) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = DarkroomRedBright,
            unfocusedBorderColor = DarkroomRedDim,
            focusedTextColor = DarkroomRedBright,
            unfocusedTextColor = DarkroomRedBright,
            cursorColor = DarkroomRedBright
        )
    )
}
