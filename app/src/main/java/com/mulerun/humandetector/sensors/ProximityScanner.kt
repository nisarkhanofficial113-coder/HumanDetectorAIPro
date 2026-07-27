package com.mulerun.humandetector.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.mulerun.humandetector.model.RawDetection
import com.mulerun.humandetector.model.SensorKind
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * "Ultrasonic" / proximity presence. The phone's proximity sensor (often an ultrasonic
 * transducer on modern devices, otherwise IR) is a real, hardware-verified signal for a body
 * a few centimetres from the sensor. When it triggers we emit a high-confidence, short-range
 * detection with unknown bearing. When it clears, no detection is emitted; the tracker times
 * it out.
 */
class ProximityScanner(private val ctx: Context) : SensorScanner {

    override fun start(): Flow<RawDetection> = callbackFlow {
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val prox = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY) ?: run { close(); return@callbackFlow }
        val maxRange = prox.maximumRange.coerceAtLeast(0.01f)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val v = e.values.firstOrNull() ?: return
                // Only emit when the sensor reports "near" — i.e. something is in front of it.
                if (v < maxRange) {
                    trySend(
                        RawDetection(
                            kind = SensorKind.ULTRASONIC,
                            id = "proximity",
                            distanceM = v.toDouble().coerceAtLeast(0.05),
                            azimuthDeg = 0.0, // by definition in front of the phone
                            confidence = 0.9f,
                            meta = "prox=${"%.2f".format(v)}m"
                        )
                    )
                }
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        sm.registerListener(listener, prox, SensorManager.SENSOR_DELAY_UI)
        awaitClose { sm.unregisterListener(listener) }
    }

    override fun stop() {  }
}
