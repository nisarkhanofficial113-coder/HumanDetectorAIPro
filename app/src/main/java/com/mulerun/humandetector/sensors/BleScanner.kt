package com.mulerun.humandetector.sensors

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.mulerun.humandetector.model.RawDetection
import com.mulerun.humandetector.model.SensorKind
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.pow

/**
 * BLE scanning as a proxy for nearby humans. Rationale: modern phones/wearables broadcast BLE
 * advertisements almost constantly. We estimate distance via the RSSI path-loss formula
 * (Feasey/log-normal), report as low-medium confidence, and never claim bearing (BLE doesn't
 * provide direction without AoA hardware).
 *
 * We filter to advertisements with reasonably-high RSSI and a device-name (present on most
 * phones/wearables), which is a conservative signal-of-life. If BLE isn't available, this
 * scanner is never constructed.
 */
class BleScanner(private val ctx: Context) : SensorScanner {

    private var scanner: android.bluetooth.le.BluetoothLeScanner? = null
    private var callback: ScanCallback? = null

    @SuppressLint("MissingPermission")
    override fun start(): Flow<RawDetection> = callbackFlow {
        val mgr = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter: BluetoothAdapter? = mgr?.adapter
        if (adapter == null || !adapter.isEnabled) { close(); return@callbackFlow }

        // Re-check permission at scan time to satisfy lint and runtime.
        val perm = if (Build.VERSION.SDK_INT >= 31) Manifest.permission.BLUETOOTH_SCAN
                   else Manifest.permission.ACCESS_FINE_LOCATION
        if (ContextCompat.checkSelfPermission(ctx, perm) != PackageManager.PERMISSION_GRANTED) {
            close(); return@callbackFlow
        }

        scanner = adapter.bluetoothLeScanner ?: run { close(); return@callbackFlow }
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setReportDelay(0)
            .build()
        val filters = emptyList<ScanFilter>()

        callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                emitDetection(result)?.let { trySend(it) }
            }
            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                for (r in results) emitDetection(r)?.let { trySend(it) }
            }
        }
        scanner?.startScan(filters, settings, callback)
        awaitClose {
            try { scanner?.stopScan(callback!!) } catch (_: Throwable) {}
            callback = null
        }
    }

    override fun stop() { /* handled by awaitClose */ }

    /** Return null when the advertisement doesn't look human-carried. */
    private fun emitDetection(result: ScanResult): RawDetection? {
        val rssi = result.rssi
        // Filter obvious background beacons: very low RSSI, or non-connectable advertisers with
        // no name and no manufacturer data (typical iBeacon/Eddystone hardware, HVAC sensors, etc.).
        val name = result.scanRecord?.deviceName
        val hasManufacturerData = (result.scanRecord?.manufacturerSpecificData?.size() ?: 0) > 0
        if (rssi < -95) return null
        if (name.isNullOrBlank() && !hasManufacturerData) return null

        val d = rssiToDistance(rssi)
        // Confidence: stronger + closer + named -> higher confidence, capped low so fusion needs help.
        val conf = (
            0.35f +
            0.35f * ((rssi + 100).coerceAtLeast(0) / 60f).coerceAtMost(1f) +
            (if (!name.isNullOrBlank()) 0.15f else 0f)
        ).coerceIn(0f, 0.85f)

        return RawDetection(
            kind = SensorKind.BLE,
            id = result.device.address,
            distanceM = d,
            azimuthDeg = null,
            confidence = conf,
            rssi = rssi,
            meta = name
        )
    }

    /** Log-distance path-loss model. txPower assumed −59 dBm @ 1m (BLE typical). */
    private fun rssiToDistance(rssi: Int): Double {
        val txPower = -59
        if (rssi == 0) return -1.0
        val ratio = (txPower - rssi) / (10.0 * 2.0) // n=2.0 free space, indoors ~2.7
        return 10.0.pow(ratio).coerceIn(0.2, 200.0)
    }
}
