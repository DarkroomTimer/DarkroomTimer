package fr.mathgl.darkroomtimer.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.mathgl.darkroomtimer.math.IncrementType
import fr.mathgl.darkroomtimer.math.TeststripEngine
import fr.mathgl.darkroomtimer.math.TeststripMode
import fr.mathgl.darkroomtimer.system.TeststripState
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedBright
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedDim
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedFaint
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedMedium

private val FSTOP_STEPS = listOf(
    12 to "1/12",
    6  to "1/6",
    4  to "1/4",
    3  to "1/3",
    2  to "1/2",
    1  to "1",
)

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

    LaunchedEffect(state.isSessionComplete) {
        if (state.isSessionComplete) viewModel.abandon()
    }

    if (state.sessionState == TeststripState.INIT) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Teststrip",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = DarkroomRedBright
            )

            Text(
                text = "Configuration",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = DarkroomRedBright
            )

            Text("Temps de base:", fontSize = 14.sp, color = DarkroomRedDim)
            DigitTimePicker(
                valueMs = state.baseTimeMs,
                onValueChange = { viewModel.updateBaseTime(it) },
                format = DigitTimeFormat.MINUTES_SECONDS_TENTHS,
                digitHeight = 52.dp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Nombre de patches:", fontSize = 14.sp, color = DarkroomRedDim)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { if (state.patchCount > 3) viewModel.updatePatchCount(state.patchCount - 1) },
                        modifier = Modifier.size(36.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkroomRedBright),
                        border = BorderStroke(1.dp, DarkroomRedFaint),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("-", fontSize = 18.sp)
                    }
                    Text(
                        text = "${state.patchCount}",
                        modifier = Modifier.padding(horizontal = 12.dp),
                        fontSize = 16.sp,
                        color = DarkroomRedBright,
                        fontFamily = FontFamily.Monospace
                    )
                    OutlinedButton(
                        onClick = { if (state.patchCount < 12) viewModel.updatePatchCount(state.patchCount + 1) },
                        modifier = Modifier.size(36.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkroomRedBright),
                        border = BorderStroke(1.dp, DarkroomRedFaint),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("+", fontSize = 18.sp)
                    }
                }
            }

            Text("Mode:", fontSize = 14.sp, color = DarkroomRedDim)
            SegmentedControl(
                options = listOf(
                    TeststripMode.INCREMENTAL to "Incrémental",
                    TeststripMode.SEPARATE to "Séparé"
                ),
                selectedOption = state.mode,
                onOptionSelected = { viewModel.updateMode(it) }
            )

            Text("Type d'incrément:", fontSize = 14.sp, color = DarkroomRedDim)
            SegmentedControl(
                options = listOf(
                    IncrementType.F_STOP to "f-stop",
                    IncrementType.SECONDS to "Secondes"
                ),
                selectedOption = state.incrementType,
                onOptionSelected = { viewModel.updateIncrementType(it) }
            )

            if (state.incrementType == IncrementType.F_STOP) {
                Text("Incrément:", fontSize = 14.sp, color = DarkroomRedDim)
                FStopStepSelector(
                    currentDenominator = state.denominator,
                    onStepSelected = { den -> viewModel.updateStopFraction(1, den) }
                )
            } else {
                Text("Incrément:", fontSize = 14.sp, color = DarkroomRedDim)
                DigitTimePicker(
                    valueMs = state.incrementMs,
                    onValueChange = { viewModel.updateIncrementMs(it) },
                    format = DigitTimeFormat.MINUTES_SECONDS_TENTHS,
                    digitHeight = 52.dp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

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
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
                TextButton(onClick = { viewModel.abandon() }) {
                    Text("← Retour", color = DarkroomRedBright)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val stateRows = sessionStateText(
                    state.sessionState,
                    state.currentPatchIndex,
                    state.patchCount,
                    state.mode,
                    state.incrementType,
                    state.numerator,
                    state.denominator,
                    state.incrementMs
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    stateRows.filterIsInstance<RowContent.ModeBadge>().firstOrNull()?.let { badge ->
                        RowContentText(badge.mode.name)
                    }
                    stateRows.filterIsInstance<RowContent.IncrementBadge>().firstOrNull()?.let { badge ->
                        RowContentText(
                            when (badge.incrementType) {
                                IncrementType.F_STOP -> "${badge.numerator}/${badge.denominator} stop"
                                IncrementType.SECONDS -> TeststripEngine.formatStopTime(badge.incrementMs)
                            }
                        )
                    }
                    stateRows.filterIsInstance<RowContent.Text>().firstOrNull()?.let { textRow ->
                        Text(
                            text = textRow.text,
                            fontSize = 16.sp,
                            color = when (state.sessionState) {
                                TeststripState.EXPOSING -> DarkroomRedBright
                                TeststripState.BETWEEN_PATCHES -> DarkroomRedMedium
                                else -> DarkroomRedDim
                            }
                        )
                    }
                }
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

            val lazyListState = rememberLazyListState()

            val scrollToIndex = if (state.sessionState == TeststripState.BETWEEN_PATCHES)
                state.selectedPatchIndex else state.currentPatchIndex
            LaunchedEffect(scrollToIndex) {
                lazyListState.animateScrollToItem(maxOf(0, scrollToIndex - 1))
            }

            LazyRow(
                state = lazyListState,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                itemsIndexed(state.patchTimesMs) { index, timeMs ->
                    PatchItem(
                        patchNumber = index + 1,
                        timeMs = timeMs,
                        differentialMs = state.differentialTimesMs[index],
                        isExposed = index in state.exposedPatches,
                        isCurrent = if (state.sessionState == TeststripState.BETWEEN_PATCHES)
                            index == state.selectedPatchIndex
                        else
                            index == state.currentPatchIndex,
                        modifier = Modifier
                            .width(110.dp)
                            .fillParentMaxHeight()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (state.sessionState == TeststripState.EXPOSING) {
                DigitTimePicker(
                    valueMs = state.remainingTimeMs,
                    onValueChange = {},
                    enabled = false,
                    format = DigitTimeFormat.MINUTES_SECONDS_TENTHS,
                    digitHeight = 80.dp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = { viewModel.pause() },
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkroomRedDim)
                    ) {
                        Text("PAUSE", fontSize = 18.sp)
                    }
                }
            }

            if (state.sessionState == TeststripState.PAUSED) {
                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = { viewModel.resume() },
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkroomRedBright)
                    ) {
                        Text("REPRENDRE", fontSize = 18.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (state.sessionState == TeststripState.BETWEEN_PATCHES) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.selectPreviousPatch() },
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkroomRedMedium),
                        border = BorderStroke(1.dp, DarkroomRedFaint)
                    ) {
                        Text("◀", fontSize = 20.sp)
                    }
                    Button(
                        onClick = { viewModel.nextPatch() },
                        modifier = Modifier.weight(3f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkroomRedBright)
                    ) {
                        Text("GO", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { viewModel.selectNextPatch() },
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkroomRedMedium),
                        border = BorderStroke(1.dp, DarkroomRedFaint)
                    ) {
                        Text("▶", fontSize = 20.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { viewModel.abandon() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkroomRedDim),
                    border = BorderStroke(1.dp, DarkroomRedFaint)
                ) {
                    Text("Annuler", fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FStopStepSelector(
    currentDenominator: Int,
    onStepSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(40.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FSTOP_STEPS.forEach { (denominator, label) ->
            val isSelected = denominator == currentDenominator
            OutlinedButton(
                onClick = { onStepSelected(denominator) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isSelected) DarkroomRedMedium else Color.Transparent,
                    contentColor = if (isSelected) Color.Black else DarkroomRedBright
                ),
                border = BorderStroke(1.dp, if (isSelected) DarkroomRedMedium else DarkroomRedFaint),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
private fun sessionStateText(
    state: TeststripState,
    patchIndex: Int,
    patchCount: Int,
    mode: TeststripMode,
    incrementType: IncrementType,
    numerator: Int,
    denominator: Int,
    incrementMs: Long
): List<RowContent> {
    return when (state) {
        TeststripState.INIT -> emptyList()
        TeststripState.EXPOSING -> listOf(
            RowContent.ModeBadge(mode),
            RowContent.IncrementBadge(incrementType, numerator, denominator, incrementMs),
            RowContent.Text("Patch ${patchIndex + 1} / $patchCount")
        )
        TeststripState.BETWEEN_PATCHES -> listOf(
            RowContent.ModeBadge(mode),
            RowContent.IncrementBadge(incrementType, numerator, denominator, incrementMs),
            RowContent.Text("Patch ${patchIndex + 1} terminé")
        )
        TeststripState.PAUSED -> listOf(
            RowContent.ModeBadge(mode),
            RowContent.IncrementBadge(incrementType, numerator, denominator, incrementMs),
            RowContent.Text("PAUSÉ")
        )
    }
}

sealed class RowContent {
    data class ModeBadge(val mode: TeststripMode) : RowContent()
    data class IncrementBadge(val incrementType: IncrementType, val numerator: Int, val denominator: Int, val incrementMs: Long) : RowContent()
    data class Text(val text: String) : RowContent()
}

@Composable
private fun RowContentText(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        color = DarkroomRedDim
    )
}
