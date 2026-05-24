package fr.mathgl.darkroomtimer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.mathgl.darkroomtimer.development.DevelopmentSessionState

@Composable
fun DevelopmentSessionScreen(
    stepName: String,
    stepElapsedSeconds: Long,
    stepRemainingSeconds: Int,
    progress: Int,
    state: DevelopmentSessionState,
    totalSteps: Int,
    currentStepIndex: Int,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onNextStep: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header avec progression
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Étape ${currentStepIndex + 1} / $totalSteps",
                color = Color.White,
                fontSize = 16.sp
            )
            Text(
                text = "$progress%",
                color = Color(0xFFCC2200),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Barre de progression
        LinearProgressIndicator(
            progress = progress / 100f,
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = Color(0xFFCC2200),
            trackColor = Color(0xFF333333)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Nom de l'étape
        Text(
            text = stepName,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Timer
        when (state) {
            DevelopmentSessionState.ACTIVE,
            DevelopmentSessionState.PAUSED -> {
                Text(
                    text = formatTime(stepRemainingSeconds.toLong() * 1000),
                    color = Color.White,
                    fontSize = 72.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "écoulé: ${stepElapsedSeconds}s",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
            DevelopmentSessionState.COMPLETED -> {
                Text(
                    text = "✓ COMPLÉTÉ",
                    color = Color(0xFF44AA44),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            else -> {}
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Contrôles
        when (state) {
            DevelopmentSessionState.CONFIGURED -> {
                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF44AA44))
                ) {
                    Text("DÉMARRER", fontSize = 20.sp)
                }
            }

            DevelopmentSessionState.ACTIVE -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onPause,
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF884400))
                    ) {
                        Text("PAUSE", fontSize = 18.sp)
                    }
                    Button(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("QUITTER", fontSize = 14.sp)
                    }
                }
            }

            DevelopmentSessionState.PAUSED -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onResume,
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF44AA44))
                    ) {
                        Text("REPRENDRE", fontSize = 14.sp)
                    }
                    Button(
                        onClick = onNextStep,
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCC2200))
                    ) {
                        Text("SUIVANT", fontSize = 14.sp)
                    }
                    Button(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("QUITTER", fontSize = 12.sp)
                    }
                }
            }

            DevelopmentSessionState.COMPLETED -> {
                Button(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF44AA44))
                ) {
                    Text("TERMINER", fontSize = 20.sp)
                }
            }
            else -> {}
        }
    }
}

private fun formatTime(ms: Long): String {
    val seconds = ms / 1000
    val minutes = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(minutes, secs)
}
