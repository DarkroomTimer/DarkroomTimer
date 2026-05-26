package fr.mathgl.darkroomtimer.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedBright
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedDim
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedFaint
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.mathgl.darkroomtimer.math.BurnDodgeType
import fr.mathgl.darkroomtimer.math.FStopMath
import fr.mathgl.darkroomtimer.system.ConnectionState
import fr.mathgl.darkroomtimer.system.RelayState
import fr.mathgl.darkroomtimer.system.TimerState

@Composable
fun CountdownScreen(
    viewModel: CountdownViewModel = viewModel(factory = CountdownViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsState()
    var showBurnDodgeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(showBurnDodgeDialog) {
        if (!showBurnDodgeDialog) {
            // Reset panel collapse when dialog closes
            if (state.burnDodgeVisible) {
                viewModel.toggleBurnDodgePanel()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        ConnectionIndicator(
            connectionState = state.connectionState,
            relayType = state.relayType
        )
        val error = state.errorMessage
        if (error != null) {
            Text(
                text = error,
                fontSize = 14.sp,
                color = DarkroomRedBright,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Timer display
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

        // Selected grade display
        Text(
            text = "Grade ${state.selectedGrade.label}",
            fontSize = 24.sp,
            color = DarkroomRedBright
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Grade selector carousel
        GradeSelector(
            selectedGrade = state.selectedGrade,
            onGradeSelected = { viewModel.selectGrade(it) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Relay indicators
        RelayIndicators(
            enlargerOn = state.relayState.enlarger == RelayState.ON,
            safelightOn = state.relayState.safelight == RelayState.ON,
            enlargerOverride = state.enlargerOverride,
            safelightOverride = state.safelightOverride,
            overrideEnabled = state.timerState != TimerState.RUNNING,
            onToggleEnlarger = { viewModel.toggleEnlargerOverride() },
            onToggleSafelight = { viewModel.toggleSafelightOverride() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Burn & Dodge panel (only when timer is RUNNING or PAUSED)
        if (state.timerState == TimerState.RUNNING || state.timerState == TimerState.PAUSED) {
            BurnDodgePanel(
                entries = state.burnDodgeEntries,
                maxEntriesReached = state.maxEntriesReached,
                isExpanded = state.burnDodgeVisible,
                onToggleExpanded = { viewModel.toggleBurnDodgePanel() },
                onAddEntry = { showBurnDodgeDialog = true },
                onRemoveEntry = { viewModel.removeBurnDodgeEntry(it) }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // F-stop correction (only when STOPPED)
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

        // Control buttons
        TimerControlButtons(
            timerState = state.timerState,
            onStart = { viewModel.start() },
            onPause = { viewModel.pause() },
            onResume = { viewModel.resume() },
            onStop = { viewModel.stop() }
        )
    }

    if (showBurnDodgeDialog) {
        BurnDodgeDialog(
            onDismiss = { showBurnDodgeDialog = false },
            onConfirm = { label, type, numerator, denominator, grade ->
                viewModel.addBurnDodgeEntry(label, type, numerator, denominator, grade)
                showBurnDodgeDialog = false
            }
        )
    }

}

@Composable
private fun RelayIndicators(
    enlargerOn: Boolean,
    safelightOn: Boolean,
    enlargerOverride: Boolean,
    safelightOverride: Boolean,
    overrideEnabled: Boolean,
    onToggleEnlarger: () -> Unit,
    onToggleSafelight: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        RelayBadge(
            label = "Agrandisseur",
            isOn = enlargerOn,
            hasOverride = enlargerOverride,
            clickEnabled = overrideEnabled,
            onClick = onToggleEnlarger
        )
        RelayBadge(
            label = "Safelight",
            isOn = safelightOn,
            hasOverride = safelightOverride,
            clickEnabled = overrideEnabled,
            onClick = onToggleSafelight
        )
    }
}

@Composable
private fun RelayBadge(
    label: String,
    isOn: Boolean,
    hasOverride: Boolean,
    clickEnabled: Boolean,
    onClick: () -> Unit
) {
    val dotColor = if (isOn) DarkroomRedBright else DarkroomRedDim
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(enabled = clickEnabled, onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(dotColor, shape = RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 12.sp, color = DarkroomRedDim)
        if (hasOverride) {
            Text(text = "override", fontSize = 9.sp, color = DarkroomRedBright)
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
    // Section divider
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

    // Negative delta buttons row
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FStopDeltaButton(label = "-1",  onClick = { onApplyDelta(-1, 1) })
        FStopDeltaButton(label = "-½",  onClick = { onApplyDelta(-1, 2) })
        FStopDeltaButton(label = "-⅓",  onClick = { onApplyDelta(-1, 3) })
        FStopDeltaButton(label = "-⅙",  onClick = { onApplyDelta(-1, 6) })
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Positive delta buttons row
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FStopDeltaButton(label = "+⅙",  onClick = { onApplyDelta(1, 6) })
        FStopDeltaButton(label = "+⅓",  onClick = { onApplyDelta(1, 3) })
        FStopDeltaButton(label = "+½",  onClick = { onApplyDelta(1, 2) })
        FStopDeltaButton(label = "+1",  onClick = { onApplyDelta(1, 1) })
    }

    // Correction status (shown only when a correction is active)
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
private fun FStopDeltaButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkroomRedBright),
        border = BorderStroke(1.dp, DarkroomRedFaint),
        modifier = Modifier.height(36.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Text(text = label, fontSize = 11.sp)
    }
}

@Composable
private fun ConnectionIndicator(connectionState: ConnectionState, relayType: String) {
    val (dotColor, label) = when {
        relayType == "NULL" || relayType == "DEMO" ->
            DarkroomRedDim to relayType.lowercase()
        connectionState is ConnectionState.Connected ->
            Color(0xFF44AA44) to "connecté"
        connectionState is ConnectionState.Connecting ->
            Color(0xFFAA8800) to "connexion…"
        connectionState is ConnectionState.Error ->
            Color.Red to "erreur"
        else ->
            DarkroomRedDim to "déconnecté"
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(dotColor, shape = RoundedCornerShape(4.dp))
        )
        Text(text = label, fontSize = 11.sp, color = dotColor)
    }
}

@Composable
private fun TimerControlButtons(
    timerState: TimerState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        when (timerState) {
            TimerState.STOPPED -> {
                Button(
                    onClick = onStart,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkroomRedBright),
                    modifier = Modifier
                        .height(56.dp)
                        .width(160.dp)
                ) {
                    Text("START", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
            TimerState.RUNNING -> {
                Button(
                    onClick = onPause,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkroomRedDim),
                    modifier = Modifier
                        .height(56.dp)
                        .width(120.dp)
                ) {
                    Text("PAUSE", fontSize = 18.sp)
                }
            }
            TimerState.PAUSED -> {
                Button(
                    onClick = onResume,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkroomRedBright),
                    modifier = Modifier
                        .height(56.dp)
                        .width(120.dp)
                ) {
                    Text("RESUME", fontSize = 18.sp)
                }
                Button(
                    onClick = onStop,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkroomRedDim),
                    modifier = Modifier
                        .height(56.dp)
                        .width(80.dp)
                ) {
                    Text("STOP", fontSize = 16.sp)
                }
            }
        }
    }
}
