package com.mulerun.humandetector.sensors

import com.mulerun.humandetector.model.RawDetection
import kotlinx.coroutines.flow.Flow

/** Every hardware backend implements this. Emits detections only from real signals. */
interface SensorScanner {
    fun start(): Flow<RawDetection>
    fun stop()
}
