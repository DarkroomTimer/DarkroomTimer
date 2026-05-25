package fr.mathgl.darkroomtimer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.mathgl.darkroomtimer.math.BurnDodgeEntry
import fr.mathgl.darkroomtimer.math.BurnDodgeType
import fr.mathgl.darkroomtimer.system.BurnDodgeManager
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedBright
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedDim
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedFaint
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedMedium
import fr.mathgl.darkroomtimer.ui.theme.DarkroomSurface

@Composable
fun BurnDodgePanel(
    entries: List<BurnDodgeEntry>,
    maxEntriesReached: Boolean,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onAddEntry: () -> Unit,
    onRemoveEntry: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkroomSurface, RoundedCornerShape(8.dp))
            .border(1.dp, DarkroomRedFaint, RoundedCornerShape(8.dp))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Burn & Dodge",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DarkroomRedBright
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (entries.isNotEmpty()) {
                    Text(
                        text = "${entries.size}/${BurnDodgeManager.MAX_ENTRIES}",
                        fontSize = 12.sp,
                        color = DarkroomRedDim
                    )
                }
                Button(
                    onClick = onAddEntry,
                    enabled = !maxEntriesReached,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkroomRedBright),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("+", fontSize = 16.sp)
                }
            }
        }

        if (isExpanded && entries.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                items(entries, key = { it.id }) { entry ->
                    BurnDodgeEntryItem(
                        entry = entry,
                        onRemove = { onRemoveEntry(entry.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BurnDodgeEntryItem(
    entry: BurnDodgeEntry,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (entry.type == BurnDodgeType.BURN) "BURN" else "DODGE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = DarkroomRedBright
            )
            if (entry.label.isNotBlank()) {
                Text(
                    text = "\"${entry.label}\"",
                    fontSize = 12.sp,
                    color = DarkroomRedDim
                )
            }
            Text(
                text = entry.fractionLabel,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = DarkroomRedMedium
            )
            Text(
                text = "Grade ${entry.contrastGrade.label}",
                fontSize = 11.sp,
                color = DarkroomRedFaint
            )
        }
        IconButton(onClick = onRemove) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_delete),
                contentDescription = "Supprimer",
                tint = DarkroomRedDim
            )
        }
    }
}
