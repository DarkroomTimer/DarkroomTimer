package fr.mathgl.darkroomtimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import fr.mathgl.darkroomtimer.storage.PreferenceManager
import fr.mathgl.darkroomtimer.system.LuminosityManager
import fr.mathgl.darkroomtimer.ui.navigation.AppNavGraph
import fr.mathgl.darkroomtimer.ui.theme.DarkroomTimerTheme

class MainActivity : ComponentActivity() {
    private lateinit var luminosityManager: LuminosityManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        luminosityManager = LuminosityManager(this)
        luminosityManager.setWindow(window)
        enableEdgeToEdge()
        setContent {
            DarkroomTimerTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    AppNavGraph()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val prefs = PreferenceManager.getInstance(this)
        luminosityManager.setConfig(
            LuminosityManager.Config(
                mode = if (prefs.luminosityMode == "FIXED") LuminosityManager.Mode.FIXED
                       else LuminosityManager.Mode.ADAPTIVE,
                minBrightness = prefs.luminosityMin,
                maxBrightness = prefs.luminosityMax,
                fixedBrightness = prefs.luminosityFixed
            )
        )
        luminosityManager.start()
    }

    override fun onStop() { super.onStop(); luminosityManager.stop() }
}
