package fr.mathgl.darkroomtimer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.mathgl.darkroomtimer.system.RelayState
import fr.mathgl.darkroomtimer.system.TimerState

@Composable
fun CountdownScreen(
    viewModel: CountdownViewModel = viewModel()
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

        // Timer display
        Text(
            text = state.displayTime,
            fontSize = 80.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Selected grade display
        Text(
            text = "Grade ${state.selectedGrade.label}",
            fontSize = 24.sp,
            color = Color(0xFFCC2200)
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
            safelightOn = state.relayState.safelight == RelayState.ON
        )

        Spacer(modifier = Modifier.height(24.dp))

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
}

@Composable
private fun RelayIndicators(enlargerOn: Boolean, safelightOn: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        RelayBadge(label = "Agrandisseur", isOn = enlargerOn)
        RelayBadge(label = "Safelight", isOn = safelightOn)
    }
}

@Composable
private fun RelayBadge(label: String, isOn: Boolean) {
    val color = if (isOn) Color(0xFFCC2200) else Color(0xFF444444)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(color, shape = RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 12.sp, color = Color(0xFFAAAAAA))
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
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCC2200)),
        modifier = Modifier.height(36.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Text(text = label, fontSize = 11.sp)
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCC2200)),
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF884400)),
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCC2200)),
                    modifier = Modifier
                        .height(56.dp)
                        .width(120.dp)
                ) {
                    Text("RESUME", fontSize = 18.sp)
                }
                Button(
                    onClick = onStop,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF444444)),
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
