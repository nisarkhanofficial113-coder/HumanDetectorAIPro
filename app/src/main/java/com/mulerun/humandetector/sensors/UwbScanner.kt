package com.mulerun.humandetector.sensors

import android.content.Context
import androidx.core.uwb.UwbManager
import com.mulerun.humandetector.model.RawDetection
import com.mulerun.humandetector.model.SensorKind
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * UWB scanner. Real UWB ranging requires a paired responder device (e.g. a Bluetooth-paired
 * tag or another Android UWB device) — a phone cannot spontaneously "sense" arbitrary humans
 * with UWB alone. This scanner keeps a manager reference so that when a peer is paired in the
 * future, ranging can begin. Until a peer is present, no detections are emitted (this
 * intentionally satisfies the "no fabrication" rule).
 *
 * We still declare the sensor "available" in Capabilities so users know their device supports
 * UWB — but the scanner is honest about only emitting real ranges.
 */
class UwbScanner(private val ctx: Context) : SensorScanner {

    override fun start(): Flow<RawDetection> = callbackFlow<RawDetection> {
        val mgr = try { UwbManager.createInstance(ctx) } catch (_: Throwable) { null }
        // No paired responders in this bare-bones app: keep the channel open so fusion sees no
        // UWB detections rather than fake ones. When users implement pairing they should call
        // `mgr.controllerSessionScope()` / `mgr.controleeSessionScope()` and forward ranging
        // measurements as RawDetection(kind = UWB, distanceM = distance, azimuthDeg = azimuth,
        // confidence ~ 0.95).
        awaitClose {  }
    }

    override fun stop() { /* handled by awaitClose */ }
}
