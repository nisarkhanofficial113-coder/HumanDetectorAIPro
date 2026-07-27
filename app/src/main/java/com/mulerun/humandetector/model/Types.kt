package com.mulerun.humandetector.model

/** The physical technology that produced a detection. */
enum class SensorKind(val label: String) {
    LIDAR("LiDAR"),
    TOF("ToF"),
    UWB("UWB"),
    MMWAVE("mmWave"),
    BLE("BLE"),
    WIFI_AWARE("Wi-Fi Aware"),
    ULTRASONIC("Ultrasonic"),
    CAMERA_AI("Camera AI")
}

/** Runtime availability of a sensor kind. Only AVAILABLE kinds are ever polled. */
enum class SensorAvailability { AVAILABLE, MISSING_HW, PERMISSION_REQUIRED, DISABLED_BY_USER }

data class SensorStatus(
    val kind: SensorKind,
    val availability: SensorAvailability,
    val note: String = ""
) {
    val isLive: Boolean get() = availability == SensorAvailability.AVAILABLE
}

/**
 * A single raw detection produced by a sensor. All fields are optional except kind + confidence
 * because different sensors provide different signals; the fusion layer merges them.
 *
 *  - `distanceM`: metres from device, if the hardware physically measures it.
 *  - `azimuthDeg`: 0..360, where 0 = phone top / true heading forward; null if unknown.
 *  - `id`: opaque source id (BLE MAC, UWB address, TF-Lite track id, …). Used by the tracker
 *    to correlate observations from the same source across frames.
 */
data class RawDetection(
    val kind: SensorKind,
    val id: String,
    val distanceM: Double?,     // null when the sensor cannot measure range
    val azimuthDeg: Double?,    // null when the sensor cannot measure bearing
    val confidence: Float,      // 0..1
    val timestampMs: Long = System.currentTimeMillis(),
    val rssi: Int? = null,      // BLE only, dBm
    val meta: String? = null
)

/** A fused, tracked target shown on the radar. */
data class Target(
    val id: Long,
    val distanceM: Double,
    val azimuthDeg: Double?,   // null => unknown bearing (rendered as a ring hint)
    val confidence: Float,
    val kinds: Set<SensorKind>,
    val lastSeenMs: Long
)
