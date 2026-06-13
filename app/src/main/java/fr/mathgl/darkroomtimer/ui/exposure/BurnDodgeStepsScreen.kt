package fr.mathgl.darkroomtimer.ui.exposure

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.mathgl.darkroomtimer.math.BurnDodgeEntry
import fr.mathgl.darkroomtimer.math.BurnDodgeType
import fr.mathgl.darkroomtimer.system.CountdownTimer
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedBright
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedDim
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedFaint
import fr.mathgl.darkroomtimer.ui.theme.DarkroomSurface

@Composable
fun BurnDodgeStepsScreen(
    viewModel: CountdownViewModel,
    onBack: () -> Unit,
    onNavigateToAddEntry: () -> Unit = {},
    onNavigateToEditEntry: (entryId: Int) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var deletingEntryId by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Retour",
                    tint = DarkroomRedBright
                )
            }
            Text(
                text = "Dodge and Burn",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = DarkroomRedBright
            )
            IconButton(onClick = onNavigateToAddEntry) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Ajouter",
                    tint = DarkroomRedBright
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Base time info
        val baseDisplay = CountdownTimer.formatTime(state.baseTimeMs)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = DarkroomRedFaint)
            Text(
                text = "  Base : $baseDisplay  ",
                fontSize = 11.sp,
                color = DarkroomRedDim,
                fontFamily = FontFamily.Monospace
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = DarkroomRedFaint)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // List
        if (state.burnDodgeEntries.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aucune étape définie",
                    color = DarkroomRedFaint,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(state.burnDodgeEntries, key = { _, e -> e.id }) { index, entry ->
                    BurnDodgeStepCard(
                        entry = entry,
                        baseTimeMs = state.baseTimeMs,
                        isFirst = index == 0,
                        isLast = index == state.burnDodgeEntries.lastIndex,
                        onMoveUp = { viewModel.moveBurnDodgeEntryUp(entry.id) },
                        onMoveDown = { viewModel.moveBurnDodgeEntryDown(entry.id) },
                        onEdit = { onNavigateToEditEntry(entry.id) },
                        onDelete = { deletingEntryId = entry.id }
                    )
                }
            }
        }
    }

    deletingEntryId?.let { id ->
        AlertDialog(
            onDismissRequest = { deletingEntryId = null },
            containerColor = Color.Black,
            title = { Text("Supprimer l'étape ?", color = DarkroomRedBright) },
            text = { Text("Cette action est irréversible.", color = DarkroomRedDim, fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeBurnDodgeEntry(id)
                        deletingEntryId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkroomRedBright)
                ) {
                    Text("Supprimer", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingEntryId = null }) {
                    Text("Annuler", color = DarkroomRedDim)
                }
            }
        )
    }
}

@Composable
private fun BurnDodgeStepCard(
    entry: BurnDodgeEntry,
    baseTimeMs: Long,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val adjustmentMs = entry.adjustmentTimeMs(baseTimeMs)
    val sign = if (entry.type == BurnDodgeType.BURN) "+" else "-"
    val adjustDisplay = "$sign${formatAdjustmentMs(adjustmentMs)}"
    val typeColor = if (entry.type == BurnDodgeType.BURN) DarkroomRedBright else DarkroomRedDim

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkroomSurface, RoundedCornerShape(8.dp))
            .border(1.dp, DarkroomRedFaint, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Reorder arrows
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
                onClick = onMoveUp,
                enabled = !isFirst,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.KeyboardArrowUp,
                    contentDescription = "Monter",
                    tint = if (!isFirst) DarkroomRedDim else DarkroomRedFaint,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onMoveDown,
                enabled = !isLast,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "Descendre",
                    tint = if (!isLast) DarkroomRedDim else DarkroomRedFaint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Type badge
        Text(
            text = if (entry.type == BurnDodgeType.BURN) "BURN" else "DODGE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = typeColor,
            modifier = Modifier.width(42.dp)
        )

        // Details
        Column(modifier = Modifier.weight(1f)) {
            if (entry.label.isNotBlank()) {
                Text(
                    text = "\"${entry.label}\"",
                    fontSize = 12.sp,
                    color = DarkroomRedBright,
                    fontWeight = FontWeight.Medium
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = entry.fractionLabel,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = DarkroomRedDim
                )
                Text(
                    text = "G${entry.contrastGrade.label}",
                    fontSize = 11.sp,
                    color = DarkroomRedFaint
                )
            }
        }

        // Calculated time
        Text(
            text = adjustDisplay,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = typeColor,
            modifier = Modifier.width(52.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )

        // Edit & delete
        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.Edit,
                contentDescription = "Modifier",
                tint = DarkroomRedDim,
                modifier = Modifier.size(16.dp)
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Supprimer",
                tint = DarkroomRedFaint,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private fun formatAdjustmentMs(ms: Long): String {
    val totalTenths = ms / 100
    val seconds = totalTenths / 10
    val tenths = totalTenths % 10
    return if (seconds >= 10) "${seconds}s" else "${seconds}.${tenths}s"
}
