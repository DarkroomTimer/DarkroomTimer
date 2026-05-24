package fr.mathgl.darkroomtimer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.mathgl.darkroomtimer.audio.AudioVolume
import fr.mathgl.darkroomtimer.math.ContrastGrade
import fr.mathgl.darkroomtimer.storage.PreferenceManager

@Composable
fun SettingsScreen(
    onNavigateToEnlargerProfiles: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getInstance(context) }

    var buzzerVolume by remember { mutableStateOf(AudioVolume.fromString(prefs.buzzerVolume)) }
    var startBeepEnabled by remember { mutableStateOf(prefs.startBeepEnabled) }
    var metronomeEnabled by remember { mutableStateOf(prefs.metronomeEnabled) }
    var metronomeCadenceMs by remember { mutableStateOf(prefs.metronomeCadenceMs) }
    var defaultGrade by remember { mutableStateOf(prefs.defaultContrastGrade) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // GÉNÉRAL
        SettingsSectionHeader("GÉNÉRAL")
        SettingsDropdown(
            label = "Volume audio",
            options = AudioVolume.values().map { it.name },
            selected = buzzerVolume.name,
            onSelect = { name ->
                val v = AudioVolume.fromString(name)
                buzzerVolume = v
                prefs.buzzerVolume = v.name
            }
        )
        SettingsSwitch(
            label = "Bip au démarrage",
            checked = startBeepEnabled,
            onCheckedChange = { startBeepEnabled = it; prefs.startBeepEnabled = it }
        )

        // MÉTRONOME
        SettingsSectionHeader("MÉTRONOME")
        SettingsSwitch(
            label = "Métronome",
            checked = metronomeEnabled,
            onCheckedChange = { metronomeEnabled = it; prefs.metronomeEnabled = it }
        )
        if (metronomeEnabled) {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(
                    text = "Cadence : $metronomeCadenceMs ms",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Slider(
                    value = metronomeCadenceMs.toFloat(),
                    onValueChange = { v ->
                        metronomeCadenceMs = v.toInt()
                        prefs.metronomeCadenceMs = v.toInt()
                    },
                    valueRange = 500f..3000f,
                    steps = 24,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFCC2200),
                        activeTrackColor = Color(0xFFCC2200)
                    )
                )
            }
        }

        // EXPOSITION
        SettingsSectionHeader("EXPOSITION")
        SettingsDropdown(
            label = "Grade par défaut",
            options = ContrastGrade.entries.map { it.label },
            selected = defaultGrade.label,
            onSelect = { label ->
                val grade = ContrastGrade.entries.first { it.label == label }
                defaultGrade = grade
                prefs.defaultContrastGrade = grade
            }
        )

        // TESTSTRIP
        SettingsSectionHeader("TESTSTRIP")
        Text(
            text = "Réglages configurables directement dans l'écran Teststrip.",
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // AGRANDISSEUR
        SettingsSectionHeader("AGRANDISSEUR")
        TextButton(
            onClick = onNavigateToEnlargerProfiles,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Profils agrandisseur", color = Color.White, fontSize = 16.sp)
                Text("▶", color = Color(0xFFCC2200), fontSize = 16.sp)
            }
        }

        // BLUETOOTH
        SettingsSectionHeader("BLUETOOTH")
        Text(
            text = "Mode compagnon WiFi — configuration relay à venir.",
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // DONNÉES
        SettingsSectionHeader("DONNÉES")
        Text(
            text = "Export / Import JSON — à venir.",
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SettingsSectionHeader(text: String) {
    Text(
        text = text,
        color = Color(0xFFCC2200),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
    )
    HorizontalDivider(color = Color(0xFF222222))
}

@Composable
private fun SettingsSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 16.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFFCC2200)
            )
        )
    }
}

@Composable
private fun SettingsDropdown(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 16.sp)
        Box {
            TextButton(onClick = { expanded = true }) {
                Text(selected, color = Color(0xFFCC2200), fontSize = 16.sp)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color(0xFF1A1A1A))
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option, color = Color.White) },
                        onClick = { onSelect(option); expanded = false }
                    )
                }
            }
        }
    }
}
