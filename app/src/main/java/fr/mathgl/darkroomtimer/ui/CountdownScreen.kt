package fr.mathgl.darkroomtimer.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedBright
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedDim
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedFaint
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedMedium
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.mathgl.darkroomtimer.math.FStopMath
import fr.mathgl.darkroomtimer.system.ConnectionState
import fr.mathgl.darkroomtimer.system.RelayState
import fr.mathgl.darkroomtimer.system.TimerState

@Composable
fun CountdownScreen(
    viewModel: CountdownViewModel = viewModel(factory = CountdownViewModel.Factory),
    onNavigateToBurnDodgeSteps: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Exposition",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = DarkroomRedBright,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        DigitTimePicker(
            valueMs = state.displayTimeMs,
            onValueChange = { newMs ->
                if (state.timerState == TimerState.PAUSED) viewModel.setRemainingTime(newMs)
                else viewModel.setBaseTime(newMs)
            },
            enabled = state.timerState != TimerState.RUNNING,
            format = DigitTimeFormat.MINUTES_SECONDS_TENTHS
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (state.timerState == TimerState.STOPPED) {
            FStopCorrectionSection(
                fStopCorrectionNumerator = state.fStopCorrectionNumerator,
                fStopCorrectionDenominator = state.fStopCorrectionDenominator,
                targetTimeDisplay = state.displayTime,
                onApplyDelta = { n, d -> viewModel.applyFStopDelta(n, d) },
                onReset = { viewModel.resetFStopCorrection() },
                onSetAsBase = { viewModel.setFStopCorrectionAsBase() }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        val startEnabled = state.relayType == "NULL" || state.relayType == "DEMO" ||
                           state.connectionState is ConnectionState.Connected
        BottomControlBar(
            timerState = state.timerState,
            startEnabled = startEnabled,
            enlargerOn = state.relayState.enlarger == RelayState.ON,
            safelightOn = state.relayState.safelight == RelayState.ON,
            enlargerOverride = state.enlargerOverride,
            safelightOverride = state.safelightOverride,
            overrideEnabled = state.timerState != TimerState.RUNNING,
            isMetronomeEnabled = state.isMetronomeEnabled,
            hasBurnDodgeSteps = state.burnDodgeEntries.isNotEmpty(),
            connectionState = state.connectionState,
            relayType = state.relayType,
            errorMessage = state.errorMessage,
            onStart = { viewModel.start() },
            onPause = { viewModel.pause() },
            onResume = { viewModel.resume() },
            onStop = { viewModel.stop() },
            onToggleEnlarger = { viewModel.toggleEnlargerOverride() },
            onToggleSafelight = { viewModel.toggleSafelightOverride() },
            onToggleMetronome = { viewModel.toggleMetronome() },
            onNavigateToBurnDodgeSteps = onNavigateToBurnDodgeSteps
        )
    }
}

@Composable
private fun BottomControlBar(
    timerState: TimerState,
    startEnabled: Boolean,
    enlargerOn: Boolean,
    safelightOn: Boolean,
    enlargerOverride: Boolean,
    safelightOverride: Boolean,
    overrideEnabled: Boolean,
    isMetronomeEnabled: Boolean,
    hasBurnDodgeSteps: Boolean,
    connectionState: ConnectionState,
    relayType: String,
    errorMessage: String?,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onToggleEnlarger: () -> Unit,
    onToggleSafelight: () -> Unit,
    onToggleMetronome: () -> Unit,
    onNavigateToBurnDodgeSteps: () -> Unit
) {
    var showConnectionDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        when (timerState) {
            TimerState.STOPPED -> {
                Button(
                    onClick = onStart,
                    enabled = startEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkroomRedBright,
                        disabledContainerColor = DarkroomRedDim
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("START", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
            TimerState.RUNNING -> {
                Button(
                    onClick = onPause,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkroomRedDim),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("PAUSE", fontSize = 18.sp)
                }
            }
            TimerState.PAUSED -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onResume,
                        colors = ButtonDefaults.buttonColors(containerColor = DarkroomRedBright),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                    ) {
                        Text("RESUME", fontSize = 18.sp)
                    }
                    Button(
                        onClick = onStop,
                        colors = ButtonDefaults.buttonColors(containerColor = DarkroomRedDim),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                    ) {
                        Text("STOP", fontSize = 16.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RelayButton(
                label = "Safelight",
                isOn = safelightOn,
                hasOverride = safelightOverride,
                clickEnabled = overrideEnabled,
                onClick = onToggleSafelight,
                modifier = Modifier.weight(1f)
            )
            RelayButton(
                label = "Agrandisseur",
                isOn = enlargerOn,
                hasOverride = enlargerOverride,
                clickEnabled = overrideEnabled,
                onClick = onToggleEnlarger,
                modifier = Modifier.weight(1f)
            )
        }

        val linkIconTint = when {
            relayType == "NULL" || relayType == "DEMO" -> DarkroomRedDim
            connectionState is ConnectionState.Connected  -> DarkroomRedBright
            connectionState is ConnectionState.Connecting -> DarkroomRedMedium
            connectionState is ConnectionState.Error      -> DarkroomRedBright
            else                                          -> DarkroomRedDim
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggleMetronome,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (isMetronomeEnabled) Icons.Default.MusicNote else Icons.Default.MusicOff,
                    contentDescription = if (isMetronomeEnabled) "Désactiver le métronome" else "Activer le métronome",
                    tint = if (isMetronomeEnabled) DarkroomRedBright else DarkroomRedFaint
                )
            }
            IconButton(
                onClick = onNavigateToBurnDodgeSteps,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = "Dodge and Burn",
                    tint = if (hasBurnDodgeSteps) DarkroomRedBright else DarkroomRedFaint
                )
            }
            IconButton(
                onClick = { showConnectionDialog = true },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (connectionState is ConnectionState.Connected) Icons.Default.Link else Icons.Default.LinkOff,
                    contentDescription = "Statut connexion",
                    tint = linkIconTint
                )
            }
        }
    }

    if (showConnectionDialog) {
        val stateLabel = when {
            relayType == "NULL"  -> "Simulation (sans matériel)"
            relayType == "DEMO"  -> "Démonstration"
            connectionState is ConnectionState.Connected  -> "Connecté"
            connectionState is ConnectionState.Connecting -> "Connexion en cours…"
            connectionState is ConnectionState.Error      -> "Erreur"
            else                                          -> "Déconnecté"
        }
        AlertDialog(
            onDismissRequest = { showConnectionDialog = false },
            title = { Text("Connexion", color = DarkroomRedBright) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Driver : $relayType", fontSize = 14.sp, color = DarkroomRedDim)
                    Text("État : $stateLabel", fontSize = 14.sp, color = DarkroomRedDim)
                    if (errorMessage != null) {
                        Text("Erreur : $errorMessage", fontSize = 13.sp, color = Color.Red)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showConnectionDialog = false }) {
                    Text("OK", color = DarkroomRedBright)
                }
            },
            containerColor = Color.Black,
            titleContentColor = DarkroomRedBright,
            textContentColor = DarkroomRedDim
        )
    }
}

@Composable
private fun RelayButton(
    label: String,
    isOn: Boolean,
    hasOverride: Boolean,
    clickEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isOn) DarkroomRedBright else Color.Transparent
    val contentColor = if (isOn) Color.Black else DarkroomRedDim
    val borderColor = if (clickEnabled && isOn) DarkroomRedBright else DarkroomRedFaint
    OutlinedButton(
        onClick = onClick,
        enabled = clickEnabled,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = DarkroomRedFaint
        ),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier.height(48.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, fontSize = 11.sp)
            if (hasOverride) {
                Text(text = "override", fontSize = 8.sp)
            }
        }
    }
}

@Composable
private fun FStopCorrectionSection(
    fStopCorrectionNumerator: Int,
    fStopCorrectionDenominator: Int,
    targetTimeDisplay: String,
    onApplyDelta: (Int, Int) -> Unit,
    onReset: () -> Unit,
    onSetAsBase: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = DarkroomRedFaint)
        Text(" F-Stop ", fontSize = 11.sp, color = DarkroomRedDim)
        HorizontalDivider(modifier = Modifier.weight(1f), color = DarkroomRedFaint)
    }

    Spacer(modifier = Modifier.height(8.dp))

    val deltas = listOf(
        1  to "1",
        2  to "½",
        3  to "⅓",
        4  to "¼",
        6  to "⅙",
        12 to "1/12",
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        deltas.forEach { (denom, label) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FStopDeltaButton(
                    label = "-$label",
                    onClick = { onApplyDelta(-1, denom) },
                    modifier = Modifier.weight(1f)
                )
                FStopDeltaButton(
                    label = "+$label",
                    onClick = { onApplyDelta(1, denom) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (fStopCorrectionNumerator != 0) {
        Spacer(modifier = Modifier.height(8.dp))

        val sign = if (fStopCorrectionNumerator > 0) "+" else ""
        val stopLabel = FStopMath.formatStop(fStopCorrectionNumerator, fStopCorrectionDenominator)
        Text(
            text = "Correction : $sign$stopLabel stop → $targetTimeDisplay",
            fontSize = 12.sp,
            color = DarkroomRedBright
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(
                onClick = onReset,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkroomRedDim),
                border = BorderStroke(1.dp, DarkroomRedFaint)
            ) {
                Text("Réinit")
            }
            OutlinedButton(
                onClick = onSetAsBase,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkroomRedDim),
                border = BorderStroke(1.dp, DarkroomRedFaint)
            ) {
                Text("Fixer")
            }
        }
    }
}

@Composable
private fun FStopDeltaButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkroomRedBright),
        border = BorderStroke(1.dp, DarkroomRedFaint),
        modifier = modifier.height(36.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Text(text = label, fontSize = 11.sp)
    }
}

