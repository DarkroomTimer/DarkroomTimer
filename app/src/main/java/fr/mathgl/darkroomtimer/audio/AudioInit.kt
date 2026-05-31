package fr.mathgl.darkroomtimer.audio

import android.app.Application
import fr.mathgl.darkroomtimer.storage.PreferenceManager

fun createAudioSystem(application: Application): AudioSystem? = try {
    val prefs = AudioPreferences(PreferenceManager.getInstance(application).prefs)
    AudioSystem(ToneGeneratorAudioEngine(prefs.buzzerVolume), prefs, prefs.buzzerVolume)
} catch (_: Exception) { null }
