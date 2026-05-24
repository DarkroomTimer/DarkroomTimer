package fr.mathgl.darkroomtimer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.mathgl.darkroomtimer.development.DevelopmentProfile
import fr.mathgl.darkroomtimer.development.DevelopmentNavigationMode
import fr.mathgl.darkroomtimer.development.DevelopmentStep
import fr.mathgl.darkroomtimer.development.DevelopmentStepType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevelopmentProfileEditorScreen(
    profile: DevelopmentProfile?,
    onSave: (DevelopmentProfile) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(profile?.name ?: "") }
    var navigationMode by remember { mutableStateOf(profile?.navigationMode ?: DevelopmentNavigationMode.MANUAL) }
    var steps by remember { mutableStateOf<MutableList<DevelopmentStep>>(profile?.steps?.toMutableList() ?: mutableListOf()) }

    var showStepDialog by remember { mutableStateOf(false) }
    var editingStepIndex by remember { mutableStateOf<Int?>(null) }

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
                text = if (profile != null) "Éditer le Profil" else "Nouveau Profil",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            TextButton(onClick = onCancel) {
                Text("← Retour", color = Color(0xFFCC2200))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // Profile name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nom du profil", color = Color.White) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFCC2200),
                    unfocusedBorderColor = Color(0xFF444444),
                    cursorColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation mode selector
            Text(
                text = "Mode de navigation",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NavigationModeButton(
                    selected = navigationMode == DevelopmentNavigationMode.MANUAL,
                    label = "Manuel",
                    onClick = { navigationMode = DevelopmentNavigationMode.MANUAL }
                )
                NavigationModeButton(
                    selected = navigationMode == DevelopmentNavigationMode.AUTOMATIC,
                    label = "Automatique",
                    onClick = { navigationMode = DevelopmentNavigationMode.AUTOMATIC }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Steps list
            Text(
                text = "Étapes",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (steps.isEmpty()) {
                Text(
                    text = "Aucune étape ajoutée",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            } else {
                steps.forEachIndexed { index, step ->
                    StepItem(
                        step = step,
                        index = index,
                        onEdit = { editingStepIndex = index },
                        onDelete = { steps = steps.toMutableList().apply { removeAt(index) } }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    editingStepIndex = null
                    showStepDialog = true
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF224422))
            ) {
                Text("+ Ajouter une étape", fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Save button
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
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = name.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCC2200))
        ) {
            Text("ENREGISTRER", fontSize = 18.sp)
        }
    }

    // Step editor dialog
    if (showStepDialog) {
        val currentEditingIndex = editingStepIndex
        StepEditorDialog(
            step = if (currentEditingIndex != null) steps[currentEditingIndex] else null,
            onSave = { newStep ->
                val idx = editingStepIndex
                if (idx != null) {
                    steps = steps.toMutableList().apply {
                        this[idx] = newStep
                    }
                } else {
                    steps = steps.toMutableList().apply { add(newStep) }
                }
                showStepDialog = false
            },
            onDismiss = { showStepDialog = false }
        )
    }
}

@Composable
private fun NavigationModeButton(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Color(0xFFCC2200) else Color(0xFF333333)
        )
    ) {
        Text(label, fontSize = 14.sp, color = if (selected) Color.White else Color.Gray)
    }
}

@Composable
private fun StepItem(
    step: DevelopmentStep,
    index: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF222222))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row {
                    Text(
                        text = "${index + 1}. ",
                        color = Color(0xFFCC2200),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = step.name,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
                Text(
                    text = "${step.durationSeconds}s • ${if (step.type == DevelopmentStepType.BATH) "Bain" else "Pause"}",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
            TextButton(onClick = onDelete) {
                Text("✕", color = Color.Red, fontSize = 16.sp)
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepEditorDialog(
    step: DevelopmentStep?,
    onSave: (DevelopmentStep) -> Unit,
    onDismiss: () -> Unit
) {
    var stepType by remember { mutableStateOf(if (step?.type == DevelopmentStepType.BATH) 0 else 1) }
    var name by remember { mutableStateOf(step?.name ?: "") }
    var durationSeconds by remember { mutableStateOf(step?.durationSeconds?.toString() ?: "") }
    var preEndAlertSeconds by remember { mutableStateOf(if (step is DevelopmentStep.BathStep) step.preEndAlertSeconds.toString() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val newStep = if (stepType == 0) {
                        DevelopmentStep.BathStep(
                            id = step?.id ?: 0,
                            name = name,
                            durationSeconds = durationSeconds.toIntOrNull() ?: 60,
                            preEndAlertSeconds = preEndAlertSeconds.toIntOrNull() ?: 0
                        )
                    } else {
                        DevelopmentStep.PauseStep(
                            id = step?.id ?: 0,
                            name = name,
                            durationSeconds = durationSeconds.toIntOrNull() ?: 30
                        )
                    }
                    onSave(newStep)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCC2200))
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
                text = if (step != null) "Modifier l'étape" else "Nouvelle étape",
                color = Color.White
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Step type selector
                Row {
                    Text(
                        text = "Type: ",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Row {
                        TextButton(onClick = { stepType = 0 }) {
                            Text(
                                text = "Bain",
                                color = if (stepType == 0) Color(0xFFCC2200) else Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                        TextButton(onClick = { stepType = 1 }) {
                            Text(
                                text = "Pause",
                                color = if (stepType == 1) Color(0xFFCC2200) else Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom", color = Color.White) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFCC2200),
                        unfocusedBorderColor = Color(0xFF444444),
                        cursorColor = Color.White
                    )
                )

                // Duration
                OutlinedTextField(
                    value = durationSeconds,
                    onValueChange = { durationSeconds = it },
                    label = { Text("Durée (secondes)", color = Color.White) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFCC2200),
                        unfocusedBorderColor = Color(0xFF444444),
                        cursorColor = Color.White
                    )
                )

                // Pre-end alert (only for BathStep)
                if (stepType == 0) {
                    OutlinedTextField(
                        value = preEndAlertSeconds,
                        onValueChange = { preEndAlertSeconds = it },
                        label = { Text("Alerte pré-fin (secondes)", color = Color.White) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFCC2200),
                            unfocusedBorderColor = Color(0xFF444444),
                            cursorColor = Color.White
                        )
                    )
                }
            }
        },
        containerColor = Color(0xFF1A1A1A)
    )
}
