package fr.mathgl.darkroomtimer.ui

import android.app.Application
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import fr.mathgl.darkroomtimer.storage.StorageService
import fr.mathgl.darkroomtimer.storage.room.AppDatabase
import fr.mathgl.darkroomtimer.ui.theme.DarkroomBlack
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedBright
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedDim
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedFaint
import fr.mathgl.darkroomtimer.ui.theme.DarkroomSurface
import fr.mathgl.darkroomtimer.ui.theme.DarkroomSurfaceElevated
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    var luminosityMode by remember { mutableStateOf(prefs.luminosityMode) }
    var luminosityMin by remember { mutableStateOf(prefs.luminosityMin) }
    var luminosityMax by remember { mutableStateOf(prefs.luminosityMax) }
    var luminosityFixed by remember { mutableStateOf(prefs.luminosityFixed) }

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
                    color = DarkroomRedBright,
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
                        thumbColor = DarkroomRedBright,
                        activeTrackColor = DarkroomRedBright,
                        inactiveTrackColor = DarkroomRedFaint,
                        activeTickColor = DarkroomBlack,
                        inactiveTickColor = DarkroomBlack
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
            color = DarkroomRedDim,
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
                Text("Profils agrandisseur", color = DarkroomRedBright, fontSize = 16.sp)
                Text("▶", color = DarkroomRedBright, fontSize = 16.sp)
            }
        }

        // LUMINOSITÉ ÉCRAN
        SettingsSectionHeader("LUMINOSITÉ ÉCRAN")
        SettingsDropdown(
            label = "Mode",
            options = listOf("ADAPTIVE", "FIXED"),
            selected = luminosityMode,
            onSelect = { luminosityMode = it; prefs.luminosityMode = it }
        )
        if (luminosityMode == "ADAPTIVE") {
            LuminositySlider(
                label = "Minimum : ${(luminosityMin * 100).toInt()}%",
                value = luminosityMin,
                onValueChange = { luminosityMin = it; prefs.luminosityMin = it }
            )
            LuminositySlider(
                label = "Maximum : ${(luminosityMax * 100).toInt()}%",
                value = luminosityMax,
                onValueChange = { luminosityMax = it; prefs.luminosityMax = it }
            )
        } else {
            LuminositySlider(
                label = "Luminosité fixe : ${(luminosityFixed * 100).toInt()}%",
                value = luminosityFixed,
                onValueChange = { luminosityFixed = it; prefs.luminosityFixed = it }
            )
        }

        // BLUETOOTH
        SettingsSectionHeader("BLUETOOTH")
        Text(
            text = "Mode compagnon WiFi — configuration relay à venir.",
            color = DarkroomRedDim,
            fontSize = 12.sp,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // DONNÉES
        SettingsSectionHeader("DONNÉES")
        val exportImportScope = rememberCoroutineScope()
        var showImportConfirm by remember { mutableStateOf(false) }
        var pendingImportJson by remember { mutableStateOf("") }
        var exportError by remember { mutableStateOf("") }
        var importError by remember { mutableStateOf("") }

        val importLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            if (uri != null) {
                exportImportScope.launch {
                    val json = withContext(Dispatchers.IO) {
                        runCatching {
                            context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                        }.getOrNull()
                    } ?: ""
                    if (json.isNotBlank()) {
                        pendingImportJson = json
                        showImportConfirm = true
                    } else {
                        importError = "Fichier invalide ou vide."
                    }
                }
            }
        }

        if (exportError.isNotBlank()) {
            Text(exportError, color = DarkroomRedBright, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
        }
        if (importError.isNotBlank()) {
            Text(importError, color = DarkroomRedBright, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
        }

        Button(
            onClick = {
                exportImportScope.launch {
                    val json = withContext(Dispatchers.IO) {
                        runCatching {
                            val db = AppDatabase.getDatabase(
                                context.applicationContext as Application,
                                CoroutineScope(Dispatchers.Default)
                            )
                            StorageService(prefs, db.enlargerProfileDao()).exportBackup()
                        }.getOrNull()
                    }
                    if (json != null) {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_TEXT, json)
                            putExtra(Intent.EXTRA_SUBJECT, "DarkroomTimer Backup")
                        }
                        context.startActivity(Intent.createChooser(intent, "Exporter"))
                        exportError = ""
                    } else {
                        exportError = "Erreur lors de l'export."
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DarkroomSurface)
        ) {
            Text("EXPORTER LES DONNÉES", color = DarkroomRedBright, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { importError = ""; importLauncher.launch("application/json") },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DarkroomSurface)
        ) {
            Text("IMPORTER LES DONNÉES", color = DarkroomRedBright, fontSize = 14.sp)
        }

        if (showImportConfirm) {
            AlertDialog(
                onDismissRequest = { showImportConfirm = false },
                title = { Text("Importer les données ?", color = DarkroomRedBright) },
                text = {
                    Text(
                        "Cette opération va remplacer vos réglages actuels. Continuer ?",
                        color = DarkroomRedBright
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val json = pendingImportJson
                            showImportConfirm = false
                            exportImportScope.launch {
                                val error = withContext(Dispatchers.IO) {
                                    runCatching {
                                        val db = AppDatabase.getDatabase(
                                            context.applicationContext as Application,
                                            CoroutineScope(Dispatchers.Default)
                                        )
                                        StorageService(prefs, db.enlargerProfileDao()).importBackup(json)
                                    }.exceptionOrNull()?.message
                                }
                                importError = if (error != null) "Erreur : $error" else ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkroomRedBright)
                    ) {
                        Text("IMPORTER")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showImportConfirm = false }) { Text("ANNULER") }
                },
                containerColor = DarkroomSurfaceElevated
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SettingsSectionHeader(text: String) {
    Text(
        text = text,
        color = DarkroomRedBright,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
    )
    HorizontalDivider(color = DarkroomSurface)
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
        Text(label, color = DarkroomRedBright, fontSize = 16.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = DarkroomRedBright,
                checkedTrackColor = DarkroomRedDim,
                checkedBorderColor = DarkroomRedBright,
                uncheckedThumbColor = DarkroomRedDim,
                uncheckedTrackColor = DarkroomBlack,
                uncheckedBorderColor = DarkroomRedFaint
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
        Text(label, color = DarkroomRedBright, fontSize = 16.sp)
        Box {
            TextButton(onClick = { expanded = true }) {
                Text(selected, color = DarkroomRedBright, fontSize = 16.sp)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(DarkroomSurfaceElevated)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option, color = DarkroomRedBright) },
                        onClick = { onSelect(option); expanded = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun LuminositySlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(text = label, color = DarkroomRedBright, fontSize = 14.sp)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = DarkroomRedBright,
                activeTrackColor = DarkroomRedBright,
                inactiveTrackColor = DarkroomRedFaint,
                activeTickColor = DarkroomBlack,
                inactiveTickColor = DarkroomBlack
            )
        )
    }
}
