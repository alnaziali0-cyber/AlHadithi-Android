package com.example.media

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class CompassReading(
    val azimuthDegrees: Float = 0f,
    val isSensorAvailable: Boolean = true,
    val needsCalibration: Boolean = false
)

object QiblaSensorController {

    fun observeCompass(context: Context): Flow<CompassReading> = callbackFlow {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        if (sensorManager == null) {
            trySend(CompassReading(isSensorAvailable = false))
            close()
            return@callbackFlow
        }

        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (rotationSensor == null && (accelSensor == null || magSensor == null)) {
            trySend(CompassReading(isSensorAvailable = false))
            awaitClose { }
            return@callbackFlow
        }

        val gravity = FloatArray(3)
        val geomagnetic = FloatArray(3)
        var hasGravity = false
        var hasMag = false
        var smoothedAzimuth = 0f
        var initialized = false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                var rawAzimuth: Float? = null
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val rotationMatrix = FloatArray(9)
                    val orientation = FloatArray(3)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    rawAzimuth = ((Math.toDegrees(orientation[0].toDouble()).toFloat() + 360f) % 360f)
                } else {
                    if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        System.arraycopy(event.values, 0, gravity, 0, 3)
                        hasGravity = true
                    } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                        System.arraycopy(event.values, 0, geomagnetic, 0, 3)
                        hasMag = true
                    }
                    if (hasGravity && hasMag) {
                        val r = FloatArray(9)
                        val i = FloatArray(9)
                        if (SensorManager.getRotationMatrix(r, i, gravity, geomagnetic)) {
                            val orientation = FloatArray(3)
                            SensorManager.getOrientation(r, orientation)
                            rawAzimuth = ((Math.toDegrees(orientation[0].toDouble()).toFloat() + 360f) % 360f)
                        }
                    }
                }

                if (rawAzimuth != null) {
                    if (!initialized) {
                        smoothedAzimuth = rawAzimuth
                        initialized = true
                    } else {
                        var delta = rawAzimuth - smoothedAzimuth
                        if (delta > 180f) delta -= 360f
                        if (delta < -180f) delta += 360f
                        smoothedAzimuth = (smoothedAzimuth + 0.18f * delta + 360f) % 360f
                    }
                    val lowAccuracy = event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE ||
                        event.accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW
                    trySend(
                        CompassReading(
                            azimuthDegrees = smoothedAzimuth,
                            isSensorAvailable = true,
                            needsCalibration = lowAccuracy
                        )
                    )
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                val needsCal = accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE ||
                    accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW
                trySend(
                    CompassReading(
                        azimuthDegrees = smoothedAzimuth,
                        isSensorAvailable = true,
                        needsCalibration = needsCal
                    )
                )
            }
        }

        if (rotationSensor != null) {
            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            sensorManager.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_UI)
            sensorManager.registerListener(listener, magSensor, SensorManager.SENSOR_DELAY_UI)
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
