package com.mulerun.humandetector.sensors

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.wifi.aware.WifiAwareManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.mulerun.humandetector.model.SensorAvailability
import com.mulerun.humandetector.model.SensorKind
import com.mulerun.humandetector.model.SensorStatus

/**
 * Runtime capability probe. Returns an authoritative list of what this specific device
 * physically supports. If a technology (LiDAR, UWB, mmWave, ToF) is not present the entry
 * will be MISSING_HW — the UI must show "N/A" and no data is ever fabricated for it.
 */
object Capabilities {

    fun probeAll(ctx: Context): Map<SensorKind, SensorStatus> {
        val map = LinkedHashMap<SensorKind, SensorStatus>()
        map[SensorKind.LIDAR]      = probeLidar(ctx)
        map[SensorKind.TOF]        = probeTof(ctx)
        map[SensorKind.UWB]        = probeUwb(ctx)
        map[SensorKind.MMWAVE]     = probeMmWave(ctx)
        map[SensorKind.BLE]        = probeBle(ctx)
        map[SensorKind.WIFI_AWARE] = probeWifiAware(ctx)
        map[SensorKind.ULTRASONIC] = probeUltrasonic(ctx)
        map[SensorKind.CAMERA_AI]  = probeCamera(ctx)
        return map
    }

    /**
     * Android has no first-party LiDAR API. Some vendors expose it through the ARCore Depth API
     * or a private sensor type. Without an SDK dependency we can only report as unavailable to
     * satisfy the "no fabricated detections" rule.
     */
    private fun probeLidar(ctx: Context): SensorStatus =
        SensorStatus(
            SensorKind.LIDAR,
            SensorAvailability.MISSING_HW,
            "No public LiDAR API on this device"
        )

    /**
     * Time-of-Flight cameras are exposed via CameraCharacteristics.DEPTH_OUTPUT on supported
     * hardware. We probe once to know if depth streams are physically available.
     */
    private fun probeTof(ctx: Context): SensorStatus {
        return try {
            val cm = ctx.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
            val hasDepth = cm.cameraIdList.any { id ->
                val ch = cm.getCameraCharacteristics(id)
                val caps = ch.get(android.hardware.camera2.CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                caps?.contains(android.hardware.camera2.CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT) == true
            }
            if (hasDepth) SensorStatus(SensorKind.TOF, SensorAvailability.AVAILABLE, "Depth camera present")
            else SensorStatus(SensorKind.TOF, SensorAvailability.MISSING_HW, "No depth camera on device")
        } catch (t: Throwable) {
            SensorStatus(SensorKind.TOF, SensorAvailability.MISSING_HW, t.message ?: "unavailable")
        }
    }

    private fun probeUwb(ctx: Context): SensorStatus {
        val pm = ctx.packageManager
        val hasFeature = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pm.hasSystemFeature("android.hardware.uwb")
        } else false
        if (!hasFeature) return SensorStatus(SensorKind.UWB, SensorAvailability.MISSING_HW, "No UWB hardware")

        val perm = if (Build.VERSION.SDK_INT >= 33)
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.UWB_RANGING)
        else PackageManager.PERMISSION_GRANTED
        return if (perm == PackageManager.PERMISSION_GRANTED)
            SensorStatus(SensorKind.UWB, SensorAvailability.AVAILABLE, "UWB available")
        else SensorStatus(SensorKind.UWB, SensorAvailability.PERMISSION_REQUIRED, "UWB_RANGING required")
    }

    /** mmWave radar has no public Android API. Always report unavailable. */
    private fun probeMmWave(ctx: Context): SensorStatus =
        SensorStatus(SensorKind.MMWAVE, SensorAvailability.MISSING_HW, "No public mmWave API")

    private fun probeBle(ctx: Context): SensorStatus {
        val hasBle = ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
        if (!hasBle) return SensorStatus(SensorKind.BLE, SensorAvailability.MISSING_HW, "No BLE hardware")
        val needed = if (Build.VERSION.SDK_INT >= 31)
            arrayOf(Manifest.permission.BLUETOOTH_SCAN)
        else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val granted = needed.all { ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED }
        return if (granted) SensorStatus(SensorKind.BLE, SensorAvailability.AVAILABLE, "BLE ready")
        else SensorStatus(SensorKind.BLE, SensorAvailability.PERMISSION_REQUIRED, "BLE scan permission required")
    }

    private fun probeWifiAware(ctx: Context): SensorStatus {
        val hasFeature = ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE)
        if (!hasFeature) return SensorStatus(SensorKind.WIFI_AWARE, SensorAvailability.MISSING_HW, "No Wi-Fi Aware")
        val mgr = ctx.getSystemService(Context.WIFI_AWARE_SERVICE) as? WifiAwareManager
        if (mgr == null || !mgr.isAvailable) return SensorStatus(SensorKind.WIFI_AWARE, SensorAvailability.MISSING_HW, "Wi-Fi Aware not enabled")
        val perm = if (Build.VERSION.SDK_INT >= 33)
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.NEARBY_WIFI_DEVICES)
        else ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
        return if (perm == PackageManager.PERMISSION_GRANTED)
            SensorStatus(SensorKind.WIFI_AWARE, SensorAvailability.AVAILABLE, "Wi-Fi Aware ready")
        else SensorStatus(SensorKind.WIFI_AWARE, SensorAvailability.PERMISSION_REQUIRED, "Nearby-Wi-Fi permission required")
    }

    /**
     * Some Android SoCs report an "Ultrasonic" (TYPE_PROXIMITY on ultrasonic transducers) sensor,
     * but there's no public API for full echolocation. We treat the proximity sensor as an
     * "ultrasonic-like" binary presence detector only if it exists.
     */
    private fun probeUltrasonic(ctx: Context): SensorStatus {
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val prox = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        return if (prox != null)
            SensorStatus(SensorKind.ULTRASONIC, SensorAvailability.AVAILABLE, "Proximity sensor present")
        else SensorStatus(SensorKind.ULTRASONIC, SensorAvailability.MISSING_HW, "No proximity/ultrasonic sensor")
    }

    private fun probeCamera(ctx: Context): SensorStatus {
        val hasCam = ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
        if (!hasCam) return SensorStatus(SensorKind.CAMERA_AI, SensorAvailability.MISSING_HW, "No camera")
        val perm = ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA)
        return if (perm == PackageManager.PERMISSION_GRANTED)
            SensorStatus(SensorKind.CAMERA_AI, SensorAvailability.AVAILABLE, "Camera ready (fallback)")
        else SensorStatus(SensorKind.CAMERA_AI, SensorAvailability.PERMISSION_REQUIRED, "Camera permission required")
    }
}
