package fr.mathgl.darkroomtimer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.mathgl.darkroomtimer.math.BurnDodgeType
import fr.mathgl.darkroomtimer.math.ContrastGrade
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedBright
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedDim
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedFaint
import fr.mathgl.darkroomtimer.ui.theme.DarkroomSurfaceElevated

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BurnDodgeDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        label: String,
        type: BurnDodgeType,
        numerator: Int,
        denominator: Int,
        contrastGrade: ContrastGrade
    ) -> Unit
) {
    var label by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(BurnDodgeType.BURN) }
    var denominator by remember { mutableIntStateOf(3) }

    val validFractions = listOf(12, 6, 4, 3, 2, 1)
    val fractionLabels = listOf("1/12", "1/6", "1/4", "1/3", "1/2", "1")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter un ajustement", color = DarkroomRedBright) },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                // Type selection
                Text("Type:", fontSize = 12.sp, color = DarkroomRedDim)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { type = BurnDodgeType.BURN },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == BurnDodgeType.BURN) DarkroomRedBright else DarkroomRedDim
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("BURN", fontSize = 12.sp)
                    }
                    Button(
                        onClick = { type = BurnDodgeType.DODGE },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == BurnDodgeType.DODGE) DarkroomRedBright else DarkroomRedDim
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("DODGE", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Label input
                Text("Zone (optionnel):", fontSize = 12.sp, color = DarkroomRedDim)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it.take(32) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkroomRedBright,
                        unfocusedBorderColor = DarkroomRedFaint
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Fraction selection
                Text("Fraction de stop:", fontSize = 12.sp, color = DarkroomRedDim)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    fractionLabels.forEachIndexed { idx, labelText ->
                        Button(
                            onClick = { denominator = validFractions[idx] },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (validFractions[idx] == denominator) DarkroomRedBright else DarkroomRedDim
                            ),
                            modifier = Modifier.weight(1f).height(36.dp)
                        ) {
                            Text(labelText, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Grade selection
                Text("Grade de contraste:", fontSize = 12.sp, color = DarkroomRedDim)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ContrastGrade.entries.forEach { grade ->
                        Button(
                            onClick = { /* Grade selection handled in parent */ },
                            enabled = false,
                            colors = ButtonDefaults.buttonColors(containerColor = DarkroomRedDim),
                            modifier = Modifier.weight(1f).height(36.dp)
                        ) {
                            Text(grade.label, fontSize = 9.sp)
                        }
                    }
                }
                Text(
                    text = "Grade actuel: ${ContrastGrade.DEFAULT.label} (fixe pour la session)",
                    fontSize = 10.sp,
                    color = DarkroomRedFaint,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Preview
                val sign = if (type == BurnDodgeType.BURN) "+" else "-"
                Text(
                    text = "Ajustement: $sign$denominator stop",
                    fontSize = 12.sp,
                    color = DarkroomRedDim,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(label, type, 1, denominator, ContrastGrade.DEFAULT) },
                colors = ButtonDefaults.buttonColors(containerColor = DarkroomRedBright)
            ) {
                Text("Valider")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = DarkroomRedDim)
            }
        }
    )
}
