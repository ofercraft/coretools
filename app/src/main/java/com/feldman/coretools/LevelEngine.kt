package com.feldman.coretools

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

data class LevelAngles(
    val rollDeg: Float = 0f,          // atan2(ay, az)
    val pitchDeg: Float = 0f,         // atan2(-ax, sqrt(ay^2+az^2))
    val horizSignedDeg: Float = 0f,   // -sin(dirRad) * frac * 90
    val vertSignedDeg: Float = 0f,    // -cos(dirRad) * frac * 90
    val tiltRad: Float = 0f,          // atan2(sqrt(ax^2+ay^2), az)
    val dirRad: Float = 0f,           // atan2(-ax, ay)
    val frac: Float = 0f,             // clamp(tiltRad / (PI/2), 0..1)
    val ax: Float = 0f,
    val ay: Float = 0f,
    val az: Float = 0f
) {
    val horizAbsDeg: Int get() = abs(horizSignedDeg).roundToInt().coerceIn(0, 90)
    val vertAbsDeg: Int  get() = abs(vertSignedDeg).roundToInt().coerceIn(0, 90)
}

class LevelEngine(
    context: Context,
    private val sensorDelay: Int = SensorManager.SENSOR_DELAY_UI
) : SensorEventListener, AutoCloseable {

    private val sensorManager = context.applicationContext.getSystemService(SensorManager::class.java)
    private val accel = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val _anglesFlow = MutableStateFlow(LevelAngles())
    val anglesFlow: StateFlow<LevelAngles> = _anglesFlow.asStateFlow()

    @Volatile private var _angles: LevelAngles = LevelAngles()
    val angles: LevelAngles get() = _angles     // synchronous snapshot (if you prefer Flow, see below)

    // --- Optional: Flow form for Compose/Reactive UIs ---
    // If you like StateFlow, uncomment these 3 lines and use collectAsState() in UI.
    // private val _flow = kotlinx.coroutines.flow.MutableStateFlow(LevelAngles())
    // val flow: kotlinx.coroutines.flow.StateFlow<LevelAngles> get() = _flow
    // ----------------------------------------------------

    init { start() }

    fun start() {
        accel?.let { sensorManager?.registerListener(this, it, sensorDelay) }
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val ax = event.values[0]
        val ay = event.values[1]
        val az = event.values[2]

        // Base angles (same as you had)
        val rollRad  = atan2(ay, az)
        val pitchRad = atan2(-ax, sqrt(ay*ay + az*az))

        // Spherical mapping shared by all views
        val tiltRad = atan2(sqrt(ax*ax + ay*ay), az)
        val dirRad  = atan2(-ax, ay)
        val frac    = (tiltRad / (Math.PI.toFloat() / 2f)).coerceIn(0f, 1f)

        val horizSignedDeg = (-sin(dirRad) * frac * 90f)
        val vertSignedDeg  = (-cos(dirRad) * frac * 90f)

        val newAngles = LevelAngles(
            rollDeg        = Math.toDegrees(rollRad.toDouble()).toFloat(),
            pitchDeg       = Math.toDegrees(pitchRad.toDouble()).toFloat(),
            horizSignedDeg = horizSignedDeg,
            vertSignedDeg  = vertSignedDeg,
            tiltRad        = tiltRad,
            dirRad         = dirRad,
            frac           = frac,
            ax = ax, ay = ay, az = az
        )

        _angles = newAngles
        _anglesFlow.value = newAngles
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun close() = stop()
}
