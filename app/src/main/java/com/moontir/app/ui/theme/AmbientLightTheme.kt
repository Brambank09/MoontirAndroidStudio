package com.moontir.app.ui.theme

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Thresholds (in lux) with hysteresis so theme transitions stay calm
 * and avoid rapid flickering near the boundary.
 * - Dim room / evening (< 20 lux) -> Dark theme
 * - Lit room / daylight (> 35 lux) -> Light theme
 */
const val AMBIENT_DARK_THRESHOLD_LUX = 20f
const val AMBIENT_LIGHT_THRESHOLD_LUX = 35f
const val AMBIENT_INITIAL_THRESHOLD_LUX = 25f

data class AmbientLightReading(
    val hasSensor: Boolean,
    val lux: Float?,
    val isDark: Boolean?
)

/**
 * Resolves whether dark theme should be active based on the latest ambient lux reading
 * and the previous dark-mode state (for hysteresis).
 */
fun resolveAmbientDarkTheme(lux: Float, currentlyDark: Boolean?): Boolean {
    return when {
        lux < AMBIENT_DARK_THRESHOLD_LUX -> true
        lux > AMBIENT_LIGHT_THRESHOLD_LUX -> false
        currentlyDark != null -> currentlyDark
        else -> lux < AMBIENT_INITIAL_THRESHOLD_LUX
    }
}

/**
 * Lifecycle-aware ambient light sensor observer.
 * Registers Sensor.TYPE_LIGHT only while [enabled] is true and the Activity is resumed,
 * unregistering automatically on pause/dispose to avoid background battery drain.
 */
@Composable
fun rememberAmbientLightState(enabled: Boolean): AmbientLightReading {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val sensorManager = remember(context) {
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    }
    val lightSensor = remember(sensorManager) {
        sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)
    }

    var luxValue by remember { mutableStateOf<Float?>(null) }
    var darkBySensor by remember { mutableStateOf<Boolean?>(null) }

    DisposableEffect(enabled, lightSensor, lifecycleOwner) {
        if (!enabled || sensorManager == null || lightSensor == null) {
            return@DisposableEffect onDispose {}
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                val rawLux = event?.values?.firstOrNull() ?: return
                if (rawLux.isNaN() || rawLux < 0f) return
                luxValue = rawLux
                darkBySensor = resolveAmbientDarkTheme(rawLux, darkBySensor)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        var isRegistered = false
        fun registerSensor() {
            if (!isRegistered) {
                isRegistered = sensorManager.registerListener(
                    listener,
                    lightSensor,
                    SensorManager.SENSOR_DELAY_UI
                )
            }
        }

        fun unregisterSensor() {
            if (isRegistered) {
                sensorManager.unregisterListener(listener)
                isRegistered = false
            }
        }

        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            registerSensor()
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> registerSensor()
                Lifecycle.Event.ON_PAUSE -> unregisterSensor()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            unregisterSensor()
        }
    }

    return AmbientLightReading(
        hasSensor = lightSensor != null,
        lux = luxValue,
        isDark = darkBySensor
    )
}
