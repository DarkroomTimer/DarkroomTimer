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
import fr.mathgl.darkroomtimer.ui.theme.DarkroomSurface
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.mathgl.darkroomtimer.math.BurnDodgeType
import fr.mathgl.darkroomtimer.system.CountdownTimer
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

        // Timer display
        Text(
            text = state.displayTime,
            fontSize = 80.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = DarkroomRedBright,
            modifier = Modifier.clickable(
                enabled = state.timerState != TimerState.RUNNING
            ) { viewModel.openTimeEditor() }
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

        // Time adjustment (only when not RUNNING)
        if (state.timerState != TimerState.RUNNING) {
            TimeAdjustRow(onAdjust = { viewModel.adjustTime(it) })
            Spacer(modifier = Modifier.height(24.dp))
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

    if (state.showTimeEditor) {
        TimeEditorSheet(
            currentMs = state.configuredTimeMs,
            onConfirm = { m, s, t -> viewModel.setTimeFromInput(m, s, t) },
            onDismiss = { viewModel.closeTimeEditor() }
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
private fun TimeAdjustRow(onAdjust: (Long) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TimeAdjustButton(label = "+100s") { onAdjust(100_000L) }
        TimeAdjustButton(label = "+10s")  { onAdjust(10_000L) }
        TimeAdjustButton(label = "+1s")   { onAdjust(1_000L) }
        TimeAdjustButton(label = "+0.1s") { onAdjust(100L) }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TimeAdjustButton(label = "-100s") { onAdjust(-100_000L) }
        TimeAdjustButton(label = "-10s")  { onAdjust(-10_000L) }
        TimeAdjustButton(label = "-1s")   { onAdjust(-1_000L) }
        TimeAdjustButton(label = "-0.1s") { onAdjust(-100L) }
    }
}

@Composable
private fun TimeAdjustButton(label: String, onClick: () -> Unit) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeEditorSheet(
    currentMs: Long,
    onConfirm: (minutes: Int, seconds: Int, tenths: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val totalTenths = currentMs / 100
    val initTenths = (totalTenths % 10).toInt()
    val totalSeconds = totalTenths / 10
    val initSeconds = (totalSeconds % 60).toInt()
    val initMinutes = (totalSeconds / 60).toInt()

    var minutes by remember { mutableStateOf(initMinutes) }
    var seconds by remember { mutableStateOf(initSeconds) }
    var tenths  by remember { mutableStateOf(initTenths) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = DarkroomSurface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Régler le temps", color = DarkroomRedBright, fontSize = 18.sp, fontWeight = FontWeight.Bold)

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TimeSpinner(label = "min", value = minutes, range = 0..16, onValueChange = { minutes = it })
                Text(":", color = DarkroomRedBright, fontSize = 32.sp)
                TimeSpinner(label = "s", value = seconds, range = 0..59, onValueChange = { seconds = it })
                Text(".", color = DarkroomRedBright, fontSize = 32.sp)
                TimeSpinner(label = "1/10", value = tenths, range = 0..9, onValueChange = { tenths = it })
            }

            Text(
                text = CountdownTimer.formatTime(minutes * 60_000L + seconds * 1_000L + tenths * 100L),
                color = DarkroomRedBright,
                fontSize = 48.sp,
                fontFamily = FontFamily.Monospace
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TextButton(onClick = onDismiss) {
                    Text("Annuler", color = DarkroomRedDim)
                }
                Button(
                    onClick = { onConfirm(minutes, seconds, tenths) },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkroomRedBright)
                ) {
                    Text("Valider")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TimeSpinner(label: String, value: Int, range: IntRange, onValueChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = { if (value < range.last) onValueChange(value + 1) }) {
            Text("▲", color = DarkroomRedBright, fontSize = 16.sp)
        }
        Text(
            text = "%02d".format(value),
            color = DarkroomRedBright,
            fontSize = 32.sp,
            fontFamily = FontFamily.Monospace
        )
        IconButton(onClick = { if (value > range.first) onValueChange(value - 1) }) {
            Text("▼", color = DarkroomRedBright, fontSize = 16.sp)
        }
        Text(label, color = DarkroomRedDim, fontSize = 10.sp)
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
