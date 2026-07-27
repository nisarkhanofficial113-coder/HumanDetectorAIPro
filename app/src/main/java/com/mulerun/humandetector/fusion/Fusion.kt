package com.mulerun.humandetector.fusion

import com.mulerun.humandetector.model.RawDetection
import com.mulerun.humandetector.model.SensorKind
import com.mulerun.humandetector.model.Target
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Cross-sensor tracker + fuser. Correlates raw detections by (distance, bearing) proximity
 * regardless of sensor kind, so that a BLE ping from a phone and an ultrasonic "near" event
 * describing the same person coalesce into a single target with fused confidence.
 *
 * The confidence-fusion follows the "noisy-OR" combination:
 *   c_out = 1 − Π (1 − c_i)
 * which is the correct way to combine independent probabilistic evidence.
 */
class Fusion(
    private val trackTimeoutMs: Long = 4000L,
    private val mergeDistanceM: Double = 1.8,
    private val mergeAngleDeg: Double = 25.0
) {
    private data class Track(
        val id: Long,
        var xM: Double,
        var yM: Double,        // Cartesian for merge math; y forward (0°)
        var azimuthDeg: Double?, // preserve null if no sensor gave bearing
        var distanceM: Double,
        var conf: Float,
        val kinds: MutableSet<SensorKind>,
        var lastSeenMs: Long,
        val sourceIds: MutableMap<SensorKind, String>
    )

    private var nextId = 1L
    private val tracks = mutableListOf<Track>()

    /** Add a batch of raw detections, remove stale tracks, return the current target list. */
    @Synchronized
    fun ingest(detections: List<RawDetection>, nowMs: Long = System.currentTimeMillis()): List<Target> {
        for (d in detections) mergeOne(d, nowMs)
        val iter = tracks.iterator()
        while (iter.hasNext()) if (nowMs - iter.next().lastSeenMs > trackTimeoutMs) iter.remove()
        return tracks.map {
            Target(
                id = it.id,
                distanceM = it.distanceM,
                azimuthDeg = it.azimuthDeg,
                confidence = it.conf,
                kinds = it.kinds.toSet(),
                lastSeenMs = it.lastSeenMs
            )
        }
    }

    private fun mergeOne(d: RawDetection, nowMs: Long) {
        val dist = d.distanceM ?: return keepBearingless(d, nowMs)
        val az = d.azimuthDeg
        val (x, y) = toXY(dist, az ?: 0.0)

        // 1) try to match same source id first (stable across frames for a given sensor)
        var match = tracks.firstOrNull { it.sourceIds[d.kind] == d.id }
        // 2) otherwise match by proximity in (distance, bearing) space
        if (match == null) {
            match = tracks.firstOrNull { t ->
                val closeDist = abs(t.distanceM - dist) < mergeDistanceM
                val closeAz = when {
                    t.azimuthDeg == null || az == null -> true
                    else -> angularDistance(t.azimuthDeg!!, az) < mergeAngleDeg
                }
                closeDist && closeAz
            }
        }

        if (match == null) {
            tracks += Track(
                id = nextId++,
                xM = x, yM = y,
                azimuthDeg = az,
                distanceM = dist,
                conf = d.confidence,
                kinds = mutableSetOf(d.kind),
                lastSeenMs = nowMs,
                sourceIds = mutableMapOf(d.kind to d.id)
            )
        } else {
            // Low-pass filter position; combine confidence with noisy-OR.
            match.xM = match.xM * 0.6 + x * 0.4
            match.yM = match.yM * 0.6 + y * 0.4
            match.distanceM = hypot(match.xM, match.yM)
            if (az != null) match.azimuthDeg = az
            match.conf = 1f - (1f - match.conf) * (1f - d.confidence)
            match.kinds += d.kind
            match.sourceIds[d.kind] = d.id
            match.lastSeenMs = nowMs
        }
    }

    /** Handle bearing-less, range-only-null detections (e.g. BLE without distance) — attach to
     *  the nearest existing track or create a bearingless one at ~5m. */
    private fun keepBearingless(d: RawDetection, nowMs: Long) {
        val distGuess = 5.0
        val existing = tracks.firstOrNull { it.sourceIds[d.kind] == d.id }
        if (existing != null) {
            existing.conf = 1f - (1f - existing.conf) * (1f - d.confidence)
            existing.kinds += d.kind
            existing.lastSeenMs = nowMs
        } else {
            tracks += Track(
                id = nextId++,
                xM = 0.0, yM = distGuess,
                azimuthDeg = null,
                distanceM = distGuess,
                conf = d.confidence,
                kinds = mutableSetOf(d.kind),
                lastSeenMs = nowMs,
                sourceIds = mutableMapOf(d.kind to d.id)
            )
        }
    }

    private fun toXY(dist: Double, az: Double): Pair<Double, Double> {
        val rad = Math.toRadians(az)
        return dist * sin(rad) to dist * cos(rad)
    }
    private fun angularDistance(a: Double, b: Double): Double {
        var d = abs(a - b) % 360.0
        if (d > 180) d = 360.0 - d
        return d
    }
}
