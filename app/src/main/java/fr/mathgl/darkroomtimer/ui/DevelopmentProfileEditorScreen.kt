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
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedBright
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedDim
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedFaint
import fr.mathgl.darkroomtimer.ui.theme.DarkroomSurface
import fr.mathgl.darkroomtimer.ui.theme.DarkroomSurfaceElevated
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
                color = DarkroomRedBright
            )
            TextButton(onClick = onCancel) {
                Text("← Retour", color = DarkroomRedBright)
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
                color = DarkroomRedBright,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (steps.isEmpty()) {
                Text(
                    text = "Aucune étape ajoutée",
                    color = DarkroomRedDim,
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
                colors = ButtonDefaults.buttonColors(containerColor = DarkroomSurface)
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
            colors = ButtonDefaults.buttonColors(containerColor = DarkroomRedBright)
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
            containerColor = if (selected) DarkroomRedBright else DarkroomRedDim
        )
    ) {
        Text(label, fontSize = 14.sp, color = if (selected) DarkroomRedBright else DarkroomRedDim)
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
        colors = CardDefaults.cardColors(containerColor = DarkroomSurface)
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
                        color = DarkroomRedBright,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = step.name,
                        color = DarkroomRedBright,
                        fontSize = 14.sp
                    )
                }
                Text(
                    text = "${step.durationSeconds}s • ${if (step.type == DevelopmentStepType.BATH) "Bain" else "Pause"}",
                    color = DarkroomRedDim,
                    fontSize = 11.sp
                )
            }
            TextButton(onClick = onDelete) {
                Text("✕", color = DarkroomRedBright, fontSize = 16.sp)
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
                text = if (step != null) "Modifier l'étape" else "Nouvelle étape",
                color = DarkroomRedBright
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Step type selector
                Row {
                    Text(
                        text = "Type: ",
                        color = DarkroomRedBright,
                        fontSize = 14.sp
                    )
                    Row {
                        TextButton(onClick = { stepType = 0 }) {
                            Text(
                                text = "Bain",
                                color = if (stepType == 0) DarkroomRedBright else DarkroomRedDim,
                                fontSize = 14.sp
                            )
                        }
                        TextButton(onClick = { stepType = 1 }) {
                            Text(
                                text = "Pause",
                                color = if (stepType == 1) DarkroomRedBright else DarkroomRedDim,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom", color = DarkroomRedBright) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkroomRedBright,
                        unfocusedBorderColor = DarkroomRedFaint,
                        cursorColor = DarkroomRedBright
                    )
                )

                // Duration
                OutlinedTextField(
                    value = durationSeconds,
                    onValueChange = { durationSeconds = it },
                    label = { Text("Durée (secondes)", color = DarkroomRedBright) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkroomRedBright,
                        unfocusedBorderColor = DarkroomRedFaint,
                        cursorColor = DarkroomRedBright
                    )
                )

                // Pre-end alert (only for BathStep)
                if (stepType == 0) {
                    OutlinedTextField(
                        value = preEndAlertSeconds,
                        onValueChange = { preEndAlertSeconds = it },
                        label = { Text("Alerte pré-fin (secondes)", color = DarkroomRedBright) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DarkroomRedBright,
                            unfocusedBorderColor = DarkroomRedFaint,
                            cursorColor = DarkroomRedBright
                        )
                    )
                }
            }
        },
        containerColor = DarkroomSurfaceElevated
    )
}
