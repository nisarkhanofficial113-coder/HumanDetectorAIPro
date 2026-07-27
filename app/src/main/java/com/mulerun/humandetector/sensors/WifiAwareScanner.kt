package com.mulerun.humandetector.sensors

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.aware.AttachCallback
import android.net.wifi.aware.DiscoverySessionCallback
import android.net.wifi.aware.PeerHandle
import android.net.wifi.aware.SubscribeConfig
import android.net.wifi.aware.SubscribeDiscoverySession
import android.net.wifi.aware.WifiAwareManager
import android.net.wifi.aware.WifiAwareSession
import android.os.Build
import androidx.core.content.ContextCompat
import com.mulerun.humandetector.model.RawDetection
import com.mulerun.humandetector.model.SensorKind
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Wi-Fi Aware (NAN) subscriber. Emits low-confidence proximity detections when a peer publishes
 * the "HumanDetectorAI" service; when both devices participate, distance and (on 802.11mc-capable
 * hardware) round-trip time can produce metric ranges. Without a peer we emit nothing.
 *
 * This code path is only invoked when Capabilities marks WIFI_AWARE as AVAILABLE.
 */
class WifiAwareScanner(private val ctx: Context) : SensorScanner {

    private var session: WifiAwareSession? = null
    private var discovery: SubscribeDiscoverySession? = null

    @SuppressLint("MissingPermission")
    override fun start(): Flow<RawDetection> = callbackFlow {
        val mgr = ctx.getSystemService(Context.WIFI_AWARE_SERVICE) as? WifiAwareManager
        if (mgr == null || !mgr.isAvailable) { close(); return@callbackFlow }

        val perm = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.NEARBY_WIFI_DEVICES
                   else Manifest.permission.ACCESS_FINE_LOCATION
        if (ContextCompat.checkSelfPermission(ctx, perm) != PackageManager.PERMISSION_GRANTED) {
            close(); return@callbackFlow
        }

        val attachCb = object : AttachCallback() {
            override fun onAttached(s: WifiAwareSession) {
                session = s
                val cfg = SubscribeConfig.Builder()
                    .setServiceName("HumanDetectorAI")
                    .build()
                s.subscribe(cfg, object : DiscoverySessionCallback() {
                    override fun onSubscribeStarted(s: SubscribeDiscoverySession) { discovery = s }
                    override fun onServiceDiscovered(
                        peerHandle: PeerHandle,
                        serviceSpecificInfo: ByteArray?,
                        matchFilter: MutableList<ByteArray>?
                    ) {
                        // A peer answering our subscribe means at least one nearby cooperating
                        // device — a real observation. We don't have raw range without RTT,
                        // so emit an unknown-distance detection with moderate confidence.
                        trySend(
                            RawDetection(
                                kind = SensorKind.WIFI_AWARE,
                                id = peerHandle.toString(),
                                distanceM = null,
                                azimuthDeg = null,
                                confidence = 0.55f,
                                meta = "peer discovered"
                            )
                        )
                    }
                    override fun onMessageReceived(peerHandle: PeerHandle, message: ByteArray) {
                        trySend(
                            RawDetection(
                                kind = SensorKind.WIFI_AWARE,
                                id = peerHandle.toString(),
                                distanceM = null,
                                azimuthDeg = null,
                                confidence = 0.6f,
                                meta = "peer message"
                            )
                        )
                    }
                }, null)
            }
            override fun onAttachFailed() { close() }
        }
        mgr.attach(attachCb, null)

        awaitClose {
            try { discovery?.close() } catch (_: Throwable) {}
            try { session?.close() } catch (_: Throwable) {}
            discovery = null; session = null
        }
    }

    override fun stop() { /* handled by awaitClose */ }
}
