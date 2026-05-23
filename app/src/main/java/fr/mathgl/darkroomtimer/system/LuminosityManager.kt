package fr.mathgl.darkroomtimer.system

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Window
import android.view.WindowManager

/**
 * Manages screen brightness based on ambient light sensor data.
 * This prevents photographic fogging in darkrooms by ensuring the screen is dim.
 */
class LuminosityManager(
    private val context: Context,
    private var window: Window? = null
) : SensorEventListener {

    enum class Mode { ADAPTIVE, FIXED }

    data class Config(
        val mode: Mode = Mode.ADAPTIVE,
        val minBrightness: Float = 0.01f,
        val maxBrightness: Float = 0.1f,
        val fixedBrightness: Float = 0.05f,
        val maxLux: Float = 100f
    )

    private var config = Config()
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    private val calculator = LuminosityCalculator()

    fun setWindow(window: Window?) {
        this.window = window
    }

    fun setConfig(newConfig: Config) {
        this.config = newConfig
    }

    /**
     * Starts the light sensor listener.
     * If the sensor is missing, falls back to Fixed Mode.
     */
    fun start() {
        if (lightSensor == null) {
            config = config.copy(mode = Mode.FIXED)
            applyBrightness(calculator.calculateBrightness(0f, config))
        } else {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    /**
     * Stops the light sensor listener.
     */
    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_LIGHT) {
            val lux = event.values[0]
            val smoothedLux = calculator.updateSmoothingFilter(lux, System.currentTimeMillis())
            val brightness = calculator.calculateBrightness(smoothedLux, config)
            applyBrightness(brightness)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Not needed for brightness management
    }

    private fun applyBrightness(brightness: Float) {
        window?.let {
            val lp = it.attributes
            lp.screenBrightness = brightness
            it.attributes = lp
        }
    }
}

/**
 * Pure logic for smoothing and calculating brightness from Lux values.
 */
class LuminosityCalculator(private val windowSizeMs: Long = 3000L) {
    private val luxSamples = mutableListOf<Pair<Long, Float>>()

    fun updateSmoothingFilter(lux: Float, timestamp: Long): Float {
        luxSamples.add(timestamp to lux)
        val cutoff = timestamp - windowSizeMs
        luxSamples.removeAll { it.first < cutoff }
        return if (luxSamples.isEmpty()) 0f else luxSamples.map { it.second }.average().toFloat()
    }

    fun calculateBrightness(smoothedLux: Float, config: LuminosityManager.Config): Float {
        return when (config.mode) {
            LuminosityManager.Mode.FIXED -> config.fixedBrightness.coerceIn(0f, 1f)
            LuminosityManager.Mode.ADAPTIVE -> {
                val brightness = (smoothedLux / config.maxLux).coerceIn(0f, 1f)
                brightness.coerceIn(config.minBrightness, config.maxBrightness)
            }
        }
    }
}
