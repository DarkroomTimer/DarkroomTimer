package fr.mathgl.darkroomtimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import fr.mathgl.darkroomtimer.system.LuminosityManager
import fr.mathgl.darkroomtimer.ui.CountdownScreen
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
                Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                    CountdownScreen()
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
