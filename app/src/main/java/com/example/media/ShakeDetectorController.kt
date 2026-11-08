package com.example.media

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

/**
 * Reveals the hidden developer panel only after five distinct alternating shake
 * pulses. Linear acceleration is preferred; a gravity-filtered accelerometer is
 * used on devices that do not expose a linear-acceleration sensor.
 */
@Composable
fun ObserveDeviceShake(
    enabled: Boolean = true,
    onShakeDetected: () -> Unit
) {
    val context = LocalContext.current
    val currentOnShake by rememberUpdatedState(onShakeDetected)

    DisposableEffect(context, enabled) {
        if (!enabled) return@DisposableEffect onDispose { }

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            ?: return@DisposableEffect onDispose { }
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            ?: return@DisposableEffect onDispose { }
        val usesRawAccelerometer = sensor.type == Sensor.TYPE_ACCELEROMETER

        val tracker = ShakeSequenceTracker()
        var gravityX = 0f
        var gravityY = 0f
        var gravityZ = 0f
        var gravityInitialized = false
        var lastTriggeredAtMs = 0L

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                val values = event?.values ?: return
                if (values.size < 3) return

                val nowMs = event.timestamp / 1_000_000L
                if (nowMs - lastTriggeredAtMs < 2_000L) return

                val x: Float
                val y: Float
                val z: Float
                if (usesRawAccelerometer) {
                    // Low-pass gravity estimate; subtract it to isolate movement.
                    if (!gravityInitialized) {
                        gravityX = values[0]
                        gravityY = values[1]
                        gravityZ = values[2]
                        gravityInitialized = true
                    }
                    val alpha = 0.80f
                    gravityX = alpha * gravityX + (1f - alpha) * values[0]
                    gravityY = alpha * gravityY + (1f - alpha) * values[1]
                    gravityZ = alpha * gravityZ + (1f - alpha) * values[2]
                    x = values[0] - gravityX
                    y = values[1] - gravityY
                    z = values[2] - gravityZ
                } else {
                    x = values[0]
                    y = values[1]
                    z = values[2]
                }

                if (tracker.addSample(x, y, z, nowMs)) {
                    lastTriggeredAtMs = nowMs
                    tracker.reset()
                    currentOnShake()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sensorManager.unregisterListener(listener) }
    }
}
