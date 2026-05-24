package fr.mathgl.darkroomtimer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.mathgl.darkroomtimer.math.ContrastGrade
import fr.mathgl.darkroomtimer.system.TeststripState

@Composable
fun TeststripScreen(
    viewModel: TeststripViewModel = viewModel(factory = TeststripViewModel.Factory),
    onBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header avec titre et bouton retour
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Teststrip",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            TextButton(onClick = onBack) {
                Text("← Retour", color = Color(0xFFCC2200))
            }
        }

        // Session state indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = sessionStateText(state.sessionState, state.currentPatchIndex, state.patchCount),
                fontSize = 16.sp,
                color = when (state.sessionState) {
                    TeststripState.EXPOSING -> Color(0xFFCC2200)
                    TeststripState.BETWEEN_PATCHES -> Color(0xFF44AA44)
                    else -> Color(0xFFAAAAAA)
                }
            )
            if (state.isSessionComplete) {
                Text(
                    text = "COMPLÉTÉ ✓",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF44AA44)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Configuration (visible seulement en CONFIGURED ou BETWEEN_PATCHES)
        if (state.sessionState != TeststripState.EXPOSING) {
            ConfigurationSection(
                baseTimeMs = state.baseTimeMs,
                numerator = state.numerator,
                denominator = state.denominator,
                onBaseTimeChange = { viewModel.updateBaseTime(it) },
                onStopFractionChange = { num, den -> viewModel.updateStopFraction(num, den) }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Liste des patches - uses weight to take remaining space
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            itemsIndexed(state.patchTimesMs) { index, timeMs ->
                PatchItem(
                    patchNumber = index + 1,
                    timeMs = timeMs,
                    differentialMs = state.differentialTimesMs[index],
                    isExposed = index in state.exposedPatches,
                    isCurrent = index == state.currentPatchIndex,
                    modifier = Modifier.aspectRatio(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Affichage du temps pendant exposition
        if (state.sessionState == TeststripState.EXPOSING) {
            Text(
                text = state.displayTime,
                fontSize = 60.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Temps: ${state.remainingTimeMs / 1000.0}s",
                fontSize = 16.sp,
                color = Color(0xFFAAAAAA)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Contrôles pendant exposition
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (state.sessionState == TeststripState.EXPOSING) {
                    Button(
                        onClick = { viewModel.pause() },
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF884400))
                    ) {
                        Text("PAUSE", fontSize = 18.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Contrôles entre patches
        if (state.sessionState == TeststripState.BETWEEN_PATCHES) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.restartCurrentPatch() },
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCC2200))
                ) {
                    Text("RECOMMENCER", fontSize = 14.sp)
                }
                Button(
                    onClick = { viewModel.nextPatch() },
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF44AA44))
                ) {
                    Text("SUIVANT →", fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { viewModel.abandon() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFAA4444))
            ) {
                Text("ABANDONNER", fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bouton démarrage (CONFIGURED seulement)
        if (state.sessionState == TeststripState.CONFIGURED) {
            Button(
                onClick = { viewModel.startSession() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCC2200))
            ) {
                Text("DÉMARRER", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun sessionStateText(state: TeststripState, patchIndex: Int, patchCount: Int): String {
    return when (state) {
        TeststripState.CONFIGURED -> "En attente"
        TeststripState.EXPOSING -> "Patch ${patchIndex + 1} / $patchCount"
        TeststripState.BETWEEN_PATCHES -> "Patch ${patchIndex + 1} terminé"
        TeststripState.PAUSED -> "PAUSÉ"
    }
}

@Composable
private fun ConfigurationSection(
    baseTimeMs: Long,
    numerator: Int,
    denominator: Int,
    onBaseTimeChange: (Long) -> Unit,
    onStopFractionChange: (Int, Int) -> Unit
) {
    Text(
        text = "Configuration",
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Temps de base
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Base:", fontSize = 14.sp, color = Color(0xFFAAAAAA))
        Text("${baseTimeMs / 1000.0}s", fontSize = 16.sp, color = Color.White, fontFamily = FontFamily.Monospace)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        OutlinedButton(
            onClick = { onBaseTimeChange((baseTimeMs - 1000).coerceIn(100L, 999_000L)) },
            modifier = Modifier.weight(1f).height(40.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCC2200))
        ) {
            Text("-1s", fontSize = 14.sp)
        }
        OutlinedButton(
            onClick = { onBaseTimeChange((baseTimeMs + 1000).coerceIn(100L, 999_000L)) },
            modifier = Modifier.weight(1f).height(40.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCC2200))
        ) {
            Text("+1s", fontSize = 14.sp)
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Incrément f-stop
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Incrément:", fontSize = 14.sp, color = Color(0xFFAAAAAA))
        val (n, d) = simplifyFraction(numerator, denominator)
        Text("${n}/${d} stop", fontSize = 16.sp, color = Color.White, fontFamily = FontFamily.Monospace)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        OutlinedButton(
            onClick = {
                val (n, d) = simplifyFraction(numerator, denominator)
                val (newN, newD) = simplifyFraction(n + 1, d)
                onStopFractionChange(newN, newD)
            },
            modifier = Modifier.weight(1f).height(40.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCC2200))
        ) {
            Text("+1/n", fontSize = 14.sp)
        }
        OutlinedButton(
            onClick = {
                val (n, d) = simplifyFraction(numerator, denominator)
                val (newN, newD) = simplifyFraction(n - 1, d)
                onStopFractionChange(newN, newD)
            },
            modifier = Modifier.weight(1f).height(40.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCC2200))
        ) {
            Text("-1/n", fontSize = 14.sp)
        }
    }
}

private fun simplifyFraction(n: Int, d: Int): Pair<Int, Int> {
    if (d == 0) return Pair(n, d)
    val common = gcd(kotlin.math.abs(n), d)
    return Pair(n / common, d / common)
}

private fun gcd(a: Int, b: Int): Int {
    var x = a; var y = b
    while (y != 0) { val temp = y; y = x % y; x = temp }
    return x
}
