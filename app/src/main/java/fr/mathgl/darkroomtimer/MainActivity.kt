package fr.mathgl.darkroomtimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import fr.mathgl.darkroomtimer.system.LuminosityManager
import fr.mathgl.darkroomtimer.ui.CountdownScreen
import fr.mathgl.darkroomtimer.ui.TeststripScreen
import fr.mathgl.darkroomtimer.ui.theme.DarkroomTimerTheme

enum class AppMode { COUNTDOWN, TESTSTRIP }

class MainActivity : ComponentActivity() {
    private lateinit var luminosityManager: LuminosityManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        luminosityManager = LuminosityManager(this)
        luminosityManager.setWindow(window)

        enableEdgeToEdge()
        setContent {
            DarkroomTimerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    ModeSelectorScreen()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        luminosityManager.start()
    }

    override fun onStop() {
        super.onStop()
        luminosityManager.stop()
    }
}

@Composable
fun ModeSelectorScreen() {
    var selectedMode by rememberSaveable { mutableStateOf<AppMode?>(null) }

    if (selectedMode == null) {
        // Mode selection screen
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            androidx.compose.material3.Text(
                text = "DarkroomTimer",
                color = Color.White,
                fontSize = 32.sp
            )
            Spacer(modifier = Modifier.width(0.dp))
            Spacer(modifier = Modifier.width(0.dp))
            Button(
                onClick = { selectedMode = AppMode.COUNTDOWN },
                modifier = Modifier.width(200.dp)
            ) {
                androidx.compose.material3.Text("Countdown", color = Color.Black)
            }
            Spacer(modifier = Modifier.width(0.dp))
            Spacer(modifier = Modifier.width(0.dp))
            Button(
                onClick = { selectedMode = AppMode.TESTSTRIP },
                modifier = Modifier.width(200.dp)
            ) {
                androidx.compose.material3.Text("Teststrip", color = Color.Black)
            }
        }
    } else {
        when (selectedMode) {
            AppMode.COUNTDOWN -> CountdownScreen()
            AppMode.TESTSTRIP -> TeststripScreen(onBack = { selectedMode = null })
            else -> ModeSelectorScreen()
        }
    }
}
