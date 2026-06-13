package fr.mathgl.darkroomtimer.ui.exposure

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.mathgl.darkroomtimer.math.BurnDodgeType
import fr.mathgl.darkroomtimer.math.ContrastGrade
import fr.mathgl.darkroomtimer.math.FStopMath
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedBright
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedDim
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedFaint
import fr.mathgl.darkroomtimer.ui.theme.DarkroomSurface

private val FRAC_NUMERATORS   = listOf(0, 1, 1, 1, 1, 1)
private val FRAC_DENOMINATORS = listOf(1, 12, 6, 4, 3, 2)
private val FRAC_LABELS       = listOf("0", "1/12", "1/6", "1/4", "1/3", "1/2")

@Composable
fun BurnDodgeEntryEditorScreen(
    entryId: Int,
    viewModel: CountdownViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val initial = remember(entryId, state.burnDodgeEntries) {
        if (entryId == -1) null
        else state.burnDodgeEntries.firstOrNull { it.id == entryId }
    }

    val isEditing = entryId != -1

    // Decompose initial entry into whole + fraction index
    val (initWhole, initFracIdx) = remember(initial) {
        val entry = initial
        if (entry != null) {
            val whole = entry.numerator / entry.denominator
            val remNum = entry.numerator % entry.denominator
            val fracIndex = FRAC_NUMERATORS.indices.firstOrNull { i ->
                FRAC_NUMERATORS[i] * entry.denominator == remNum * FRAC_DENOMINATORS[i]
            } ?: 0
            Pair(whole, fracIndex)
        } else {
            Pair(0, 2) // default: 0 whole + 1/6
        }
    }

    var label      by remember { mutableStateOf(initial?.label ?: "") }
    var type       by remember { mutableStateOf(initial?.type ?: BurnDodgeType.BURN) }
    var wholeStops by remember { mutableIntStateOf(initWhole) }
    var fracIndex  by remember { mutableIntStateOf(initFracIdx) }
    var gradeIndex by remember { mutableIntStateOf(initial?.contrastGrade?.index ?: ContrastGrade.DEFAULT.index) }

    val isValid = wholeStops > 0 || fracIndex > 0

    val (combinedN, combinedD) = remember(wholeStops, fracIndex) {
        val fn = FRAC_NUMERATORS[fracIndex]
        val fd = FRAC_DENOMINATORS[fracIndex]
        if (fn == 0) Pair(wholeStops, 1)
        else FStopMath.simplify(wholeStops * fd + fn, fd)
    }

    val grades = ContrastGrade.entries
    val grade = ContrastGrade.fromIndex(gradeIndex)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkroomSurface)
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = DarkroomRedBright)
            }
            Text(
                text = if (isEditing) "Modifier l'étape" else "Ajouter une étape",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DarkroomRedBright,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            IconButton(
                onClick = {
                    if (isEditing) {
                        viewModel.updateBurnDodgeEntry(entryId, label, type, combinedN, combinedD, grade)
                    } else {
                        viewModel.addBurnDodgeEntry(label, type, combinedN, combinedD, grade)
                    }
                    onBack()
                },
                enabled = isValid
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Valider",
                    tint = if (isValid) DarkroomRedBright else DarkroomRedFaint
                )
            }
        }

        // Form
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Type ---
            EditorSection("Type") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EditorTypeButton(
                        label = "BURN",
                        selected = type == BurnDodgeType.BURN,
                        onClick = { type = BurnDodgeType.BURN },
                        modifier = Modifier.weight(1f)
                    )
                    EditorTypeButton(
                        label = "DODGE",
                        selected = type == BurnDodgeType.DODGE,
                        onClick = { type = BurnDodgeType.DODGE },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // --- Zone ---
            EditorSection("Zone (optionnel)") {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it.take(32) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkroomRedBright,
                        unfocusedBorderColor = DarkroomRedFaint,
                        focusedTextColor = DarkroomRedBright,
                        unfocusedTextColor = DarkroomRedDim,
                        cursorColor = DarkroomRedBright,
                        focusedLabelColor = DarkroomRedBright,
                        unfocusedLabelColor = DarkroomRedFaint
                    )
                )
            }

            // --- Stops entiers ---
            EditorSection("Stops entiers") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (0..4).forEach { w ->
                        EditorChipButton(
                            label = "$w",
                            selected = wholeStops == w,
                            onClick = { wholeStops = w },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // --- Fraction ---
            EditorSection("Fraction de stop") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FRAC_LABELS.forEachIndexed { idx, lbl ->
                        EditorChipButton(
                            label = lbl,
                            selected = fracIndex == idx,
                            enabled = !(wholeStops == 4 && idx > 0),
                            onClick = { fracIndex = idx },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // --- Grade ---
            EditorSection("Grade de contraste") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { if (gradeIndex > 0) gradeIndex-- },
                        enabled = gradeIndex > 0,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Text(
                            "<",
                            color = if (gradeIndex > 0) DarkroomRedBright else DarkroomRedFaint,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Grade ${grade.label}",
                        color = DarkroomRedBright,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    IconButton(
                        onClick = { if (gradeIndex < grades.lastIndex) gradeIndex++ },
                        enabled = gradeIndex < grades.lastIndex,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Text(
                            ">",
                            color = if (gradeIndex < grades.lastIndex) DarkroomRedBright else DarkroomRedFaint,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // --- Prévisualisation ---
            if (isValid) {
                val sign = if (type == BurnDodgeType.BURN) "+" else "-"
                val stopLabel = FStopMath.formatStop(combinedN, combinedD)
                Text(
                    text = "$sign$stopLabel stop  |  Grade ${grade.label}",
                    fontSize = 14.sp,
                    color = DarkroomRedDim,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = "Sélectionnez au moins 1/12 stop",
                    fontSize = 13.sp,
                    color = DarkroomRedFaint,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun EditorSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, fontSize = 12.sp, color = DarkroomRedDim)
        content()
    }
}

@Composable
private fun EditorTypeButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selected) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = DarkroomRedBright),
            modifier = modifier.height(48.dp)
        ) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkroomRedDim),
            border = BorderStroke(1.dp, DarkroomRedFaint),
            modifier = modifier.height(48.dp)
        ) {
            Text(label, fontSize = 14.sp)
        }
    }
}

@Composable
private fun EditorChipButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    if (selected) {
        Button(
            onClick = onClick,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(containerColor = DarkroomRedBright),
            modifier = modifier.height(40.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            Text(label, fontSize = 11.sp, color = Color.Black)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = DarkroomRedDim,
                disabledContentColor = DarkroomRedFaint
            ),
            border = BorderStroke(1.dp, if (enabled) DarkroomRedFaint else Color(0xFF0A0000)),
            modifier = modifier.height(40.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            Text(label, fontSize = 11.sp)
        }
    }
}
