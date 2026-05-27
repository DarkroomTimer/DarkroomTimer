package fr.mathgl.darkroomtimer.ui

import androidx.compose.foundation.BorderStroke
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
import fr.mathgl.darkroomtimer.math.IncrementType
import fr.mathgl.darkroomtimer.math.TeststripMode
import fr.mathgl.darkroomtimer.system.TeststripState
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedBright
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedDim
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedFaint
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedMedium

@Composable
fun <T> SegmentedControl(
    options: List<Pair<T, String>>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().height(40.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (option, label) ->
            val isSelected = option == selectedOption
            OutlinedButton(
                onClick = { onOptionSelected(option) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isSelected) DarkroomRedMedium else Color.Transparent,
                    contentColor = if (isSelected) Color.Black else DarkroomRedBright
                ),
                border = BorderStroke(1.dp, if (isSelected) DarkroomRedMedium else DarkroomRedFaint)
            ) {
                Text(label, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
fun TeststripScreen(
    viewModel: TeststripViewModel = viewModel(factory = TeststripViewModel.Factory),
    onBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    // Show configuration modal when in INIT state
    if (state.sessionState == TeststripState.INIT) {
        ConfigurationModal(
            baseTimeMs = state.baseTimeMs,
            patchCount = state.patchCount,
            mode = state.mode,
            incrementType = state.incrementType,
            numerator = state.numerator,
            denominator = state.denominator,
            incrementMs = state.incrementMs,
            isRelayConnected = state.isRelayConnected,
            onBaseTimeChange = { viewModel.updateBaseTime(it) },
            onPatchCountChange = { viewModel.updatePatchCount(it) },
            onModeChange = { viewModel.updateMode(it) },
            onIncrementTypeChange = { viewModel.updateIncrementType(it) },
            onStopFractionChange = { num, den -> viewModel.updateStopFraction(num, den) },
            onIncrementMsChange = { viewModel.updateIncrementMs(it) },
            onStartClick = { viewModel.startSession() }
        )
    }

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
                color = DarkroomRedBright
            )
            TextButton(onClick = onBack) {
                Text("← Retour", color = DarkroomRedBright)
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
                    TeststripState.EXPOSING -> DarkroomRedBright
                    TeststripState.BETWEEN_PATCHES -> DarkroomRedMedium
                    else -> DarkroomRedDim
                }
            )
            val errorMsg = state.errorMessage
            if (errorMsg != null) {
                Text(
                    text = errorMsg,
                    fontSize = 14.sp,
                    color = DarkroomRedBright,
                    fontWeight = FontWeight.Medium
                )
            } else if (state.isSessionComplete) {
                Text(
                    text = "COMPLÉTÉ ✓",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkroomRedMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

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
                color = DarkroomRedBright
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Temps: ${state.remainingTimeMs / 1000.0}s",
                fontSize = 16.sp,
                color = DarkroomRedDim
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
                        colors = ButtonDefaults.buttonColors(containerColor = DarkroomRedDim)
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
                    colors = ButtonDefaults.buttonColors(containerColor = DarkroomRedBright)
                ) {
                    Text("RECOMMENCER", fontSize = 14.sp)
                }
                Button(
                    onClick = { viewModel.nextPatch() },
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkroomRedMedium)
                ) {
                    Text("SUIVANT →", fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { viewModel.abandon() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkroomRedDim),
                border = BorderStroke(1.dp, DarkroomRedFaint)
            ) {
                Text("ABANDONNER", fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bouton démarrage (INIT seulement)
        if (state.sessionState == TeststripState.INIT) {
            Button(
                onClick = { viewModel.startSession() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = state.isRelayConnected,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkroomRedBright,
                    disabledContainerColor = DarkroomRedDim
                )
            ) {
                Text(
                    text = "DÉMARRER",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun sessionStateText(state: TeststripState, patchIndex: Int, patchCount: Int): String {
    return when (state) {
        TeststripState.INIT -> "En attente"
        TeststripState.EXPOSING -> "Patch ${patchIndex + 1} / $patchCount"
        TeststripState.BETWEEN_PATCHES -> "Patch ${patchIndex + 1} terminé"
        TeststripState.PAUSED -> "PAUSÉ"
    }
}

@Composable
private fun ConfigurationSection(
    baseTimeMs: Long,
    patchCount: Int,
    mode: TeststripMode,
    incrementType: IncrementType,
    numerator: Int,
    denominator: Int,
    incrementMs: Long,
    onBaseTimeChange: (Long) -> Unit,
    onPatchCountChange: (Int) -> Unit,
    onModeChange: (TeststripMode) -> Unit,
    onIncrementTypeChange: (IncrementType) -> Unit,
    onStopFractionChange: (Int, Int) -> Unit,
    onIncrementMsChange: (Long) -> Unit
) {
    Text(
        text = "Configuration",
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = DarkroomRedBright
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Temps de base
    Text("Base:", fontSize = 14.sp, color = DarkroomRedDim)
    Spacer(modifier = Modifier.height(4.dp))
    DigitTimePicker(
        valueMs = baseTimeMs,
        onValueChange = onBaseTimeChange,
        format = DigitTimeFormat.MINUTES_SECONDS_TENTHS,
        digitHeight = 52.dp
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Nombre de patches
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Nombre de patches:", fontSize = 14.sp, color = DarkroomRedDim)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = { if (patchCount > 3) onPatchCountChange(patchCount - 1) },
                modifier = Modifier.size(36.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkroomRedBright),
                border = BorderStroke(1.dp, DarkroomRedFaint),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("-", fontSize = 18.sp)
            }
            Text(
                text = "$patchCount",
                modifier = Modifier.padding(horizontal = 12.dp),
                fontSize = 16.sp,
                color = DarkroomRedBright,
                fontFamily = FontFamily.Monospace
            )
            OutlinedButton(
                onClick = { if (patchCount < 12) onPatchCountChange(patchCount + 1) },
                modifier = Modifier.size(36.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkroomRedBright),
                border = BorderStroke(1.dp, DarkroomRedFaint),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("+", fontSize = 18.sp)
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Mode d'exposition
    Text("Mode:", fontSize = 14.sp, color = DarkroomRedDim)
    Spacer(modifier = Modifier.height(4.dp))
    SegmentedControl(
        options = listOf(
            TeststripMode.INCREMENTAL to "Incrémental",
            TeststripMode.SEPARATE to "Séparé"
        ),
        selectedOption = mode,
        onOptionSelected = onModeChange
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Type d'incrément
    Text("Type d'incrément:", fontSize = 14.sp, color = DarkroomRedDim)
    Spacer(modifier = Modifier.height(4.dp))
    SegmentedControl(
        options = listOf(
            IncrementType.F_STOP to "f-stop",
            IncrementType.SECONDS to "Secondes"
        ),
        selectedOption = incrementType,
        onOptionSelected = onIncrementTypeChange
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Valeur de l'incrément
    if (incrementType == IncrementType.F_STOP) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Incrément:", fontSize = 14.sp, color = DarkroomRedDim)
            val (n, d) = simplifyFraction(numerator, denominator)
            Text("${n}/${d} stop", fontSize = 16.sp, color = DarkroomRedBright, fontFamily = FontFamily.Monospace)
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
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkroomRedBright),
                border = BorderStroke(1.dp, DarkroomRedFaint)
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
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkroomRedBright),
                border = BorderStroke(1.dp, DarkroomRedFaint)
            ) {
                Text("-1/n", fontSize = 14.sp)
            }
        }
    } else {
        Text("Incrément:", fontSize = 14.sp, color = DarkroomRedDim)
        Spacer(modifier = Modifier.height(4.dp))
        DigitTimePicker(
            valueMs = incrementMs,
            onValueChange = onIncrementMsChange,
            format = DigitTimeFormat.MINUTES_SECONDS_TENTHS,
            digitHeight = 52.dp
        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigurationModal(
    baseTimeMs: Long,
    patchCount: Int,
    mode: TeststripMode,
    incrementType: IncrementType,
    numerator: Int,
    denominator: Int,
    incrementMs: Long,
    isRelayConnected: Boolean,
    onBaseTimeChange: (Long) -> Unit,
    onPatchCountChange: (Int) -> Unit,
    onModeChange: (TeststripMode) -> Unit,
    onIncrementTypeChange: (IncrementType) -> Unit,
    onStopFractionChange: (Int, Int) -> Unit,
    onIncrementMsChange: (Long) -> Unit,
    onStartClick: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = { },
        containerColor = Color.Black,
        contentColor = DarkroomRedBright,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 0.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(DarkroomRedMedium),
                contentAlignment = Alignment.Center
            ) {
                RectangleHandle()
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Configuration",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = DarkroomRedBright
            )

            // Temps de base
            Text("Temps de base:", fontSize = 14.sp, color = DarkroomRedDim)
            DigitTimePicker(
                valueMs = baseTimeMs,
                onValueChange = onBaseTimeChange,
                format = DigitTimeFormat.MINUTES_SECONDS_TENTHS,
                digitHeight = 52.dp
            )

            // Nombre de patches
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Nombre de patches:", fontSize = 14.sp, color = DarkroomRedDim)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { if (patchCount > 3) onPatchCountChange(patchCount - 1) },
                        modifier = Modifier.size(36.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkroomRedBright),
                        border = BorderStroke(1.dp, DarkroomRedFaint),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("-", fontSize = 18.sp)
                    }
                    Text(
                        text = "$patchCount",
                        modifier = Modifier.padding(horizontal = 12.dp),
                        fontSize = 16.sp,
                        color = DarkroomRedBright,
                        fontFamily = FontFamily.Monospace
                    )
                    OutlinedButton(
                        onClick = { if (patchCount < 12) onPatchCountChange(patchCount + 1) },
                        modifier = Modifier.size(36.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkroomRedBright),
                        border = BorderStroke(1.dp, DarkroomRedFaint),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("+", fontSize = 18.sp)
                    }
                }
            }

            // Mode d'exposition
            Text("Mode:", fontSize = 14.sp, color = DarkroomRedDim)
            SegmentedControl(
                options = listOf(
                    TeststripMode.INCREMENTAL to "Incrémental",
                    TeststripMode.SEPARATE to "Séparé"
                ),
                selectedOption = mode,
                onOptionSelected = onModeChange
            )

            // Type d'incrément
            Text("Type d'incrément:", fontSize = 14.sp, color = DarkroomRedDim)
            SegmentedControl(
                options = listOf(
                    IncrementType.F_STOP to "f-stop",
                    IncrementType.SECONDS to "Secondes"
                ),
                selectedOption = incrementType,
                onOptionSelected = onIncrementTypeChange
            )

            // Valeur de l'incrément
            if (incrementType == IncrementType.F_STOP) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Incrément:", fontSize = 14.sp, color = DarkroomRedDim)
                    val (n, d) = simplifyFraction(numerator, denominator)
                    Text("${n}/${d} stop", fontSize = 16.sp, color = DarkroomRedBright, fontFamily = FontFamily.Monospace)
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
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkroomRedBright),
                        border = BorderStroke(1.dp, DarkroomRedFaint)
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
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkroomRedBright),
                        border = BorderStroke(1.dp, DarkroomRedFaint)
                    ) {
                        Text("-1/n", fontSize = 14.sp)
                    }
                }
            } else {
                Text("Incrément:", fontSize = 14.sp, color = DarkroomRedDim)
                DigitTimePicker(
                    valueMs = incrementMs,
                    onValueChange = onIncrementMsChange,
                    format = DigitTimeFormat.MINUTES_SECONDS_TENTHS,
                    digitHeight = 52.dp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Démarrer button
            Button(
                onClick = onStartClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = isRelayConnected,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkroomRedBright,
                    disabledContainerColor = DarkroomRedDim
                )
            ) {
                Text(
                    text = "DÉMARRER",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun RectangleHandle() {
    Box(
        modifier = Modifier
            .width(40.dp)
            .height(4.dp)
            .background(DarkroomRedDim, MaterialTheme.shapes.small)
    )
}
