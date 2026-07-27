package com.mulerun.humandetector.viewmodel

import android.app.Application
import android.content.Context
import android.os.BatteryManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mulerun.humandetector.alerts.AlertBus
import com.mulerun.humandetector.fusion.Fusion
import com.mulerun.humandetector.model.RawDetection
import com.mulerun.humandetector.model.SensorAvailability
import com.mulerun.humandetector.model.SensorKind
import com.mulerun.humandetector.model.SensorStatus
import com.mulerun.humandetector.model.Target
import com.mulerun.humandetector.sensors.BleScanner
import com.mulerun.humandetector.sensors.Capabilities
import com.mulerun.humandetector.sensors.ProximityScanner
import com.mulerun.humandetector.sensors.SensorScanner
import com.mulerun.humandetector.sensors.UwbScanner
import com.mulerun.humandetector.sensors.WifiAwareScanner
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Immutable UI state consumed by the Compose radar. */
data class UiState(
    val statuses: Map<SensorKind, SensorStatus> = emptyMap(),
    val targets: List<Target> = emptyList(),
    val enabled: Map<SensorKind, Boolean> = emptyMap(),
    val batteryPct: Int = -1,
    val signalPct: Int = 0,
    val nightMode: Boolean = true,
    val sensitivity: Float = 0.5f,
    val voice: Boolean = true,
    val sound: Boolean = true,
    val vibration: Boolean = true,
    val batterySaver: Boolean = false,
    val scanning: Boolean = false
)

class RadarViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val fusion = Fusion()
    private val alerts = AlertBus(app)

    private val activeScanners = mutableMapOf<SensorKind, SensorScanner>()
    private val activeJobs = mutableMapOf<SensorKind, Job>()
    private val rawBuffer = mutableListOf<RawDetection>()
    private var loopJob: Job? = null

    fun refreshCapabilities() {
        val caps = Capabilities.probeAll(getApplication())
        val enabled = _state.value.enabled.ifEmpty {
            caps.mapValues { it.value.isLive }
        }
        _state.value = _state.value.copy(statuses = caps, enabled = enabled)
    }

    fun startScanning() {
        if (_state.value.scanning) return
        _state.value = _state.value.copy(scanning = true)
        launchScanners()
        loopJob = viewModelScope.launch {
            while (true) {
                val snapshot: List<RawDetection>
                synchronized(rawBuffer) {
                    snapshot = rawBuffer.toList()
                    rawBuffer.clear()
                }
                val filtered = snapshot.filter { it.confidence >= sensitivityFloor() }
                val targets = fusion.ingest(filtered)
                val prevCount = _state.value.targets.size
                _state.value = _state.value.copy(
                    targets = targets,
                    batteryPct = readBattery(),
                    signalPct = (targets.maxOfOrNull { (it.confidence * 100).toInt() } ?: 0)
                )
                if (prevCount != targets.size) alerts.onCountChanged(prevCount, targets.size)
                val period = if (_state.value.batterySaver) 500L else 200L
                delay(period)
            }
        }
    }

    fun stopScanning() {
        loopJob?.cancel(); loopJob = null
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
        activeScanners.clear()
        _state.value = _state.value.copy(scanning = false, targets = emptyList())
    }

    private fun launchScanners() {
        val ctx = getApplication<Application>()
        val caps = _state.value.statuses
        val enabled = _state.value.enabled

        fun spawn(kind: SensorKind, factory: () -> SensorScanner) {
            if (caps[kind]?.availability != SensorAvailability.AVAILABLE) return
            if (enabled[kind] == false) return
            val scanner = factory()
            activeScanners[kind] = scanner
            activeJobs[kind] = viewModelScope.launch {
                scanner.start().collect { det ->
                    synchronized(rawBuffer) { rawBuffer += det }
                }
            }
        }
        spawn(SensorKind.BLE)        { BleScanner(ctx) }
        spawn(SensorKind.UWB)        { UwbScanner(ctx) }
        spawn(SensorKind.WIFI_AWARE) { WifiAwareScanner(ctx) }
        spawn(SensorKind.ULTRASONIC) { ProximityScanner(ctx) }
        // ToF/LiDAR/mmWave: no SDK-side scanners available. If a vendor SDK is added the
        // corresponding scanner class should be dropped in here.
    }

    fun setEnabled(kind: SensorKind, on: Boolean) {
        val map = _state.value.enabled.toMutableMap().also { it[kind] = on }
        _state.value = _state.value.copy(enabled = map)
        if (_state.value.scanning) { stopScanning(); startScanning() }
    }
    fun setSensitivity(v: Float)    { _state.value = _state.value.copy(sensitivity = v.coerceIn(0f, 1f)) }
    fun setVoice(v: Boolean)        { _state.value = _state.value.copy(voice = v); alerts.voiceEnabled = v }
    fun setSound(v: Boolean)        { _state.value = _state.value.copy(sound = v); alerts.soundEnabled = v }
    fun setVibration(v: Boolean)    { _state.value = _state.value.copy(vibration = v); alerts.vibrationEnabled = v }
    fun setNightMode(v: Boolean)    { _state.value = _state.value.copy(nightMode = v) }
    fun setBatterySaver(v: Boolean) { _state.value = _state.value.copy(batterySaver = v) }

    /** Sensitivity 0..1 maps to a confidence floor 0.15..0.7 — higher sensitivity accepts weaker
     *  detections, lower sensitivity requires stronger evidence. */
    private fun sensitivityFloor(): Float =
        (0.7f - 0.55f * _state.value.sensitivity).coerceIn(0.15f, 0.7f)

    private fun readBattery(): Int {
        val bm = getApplication<Application>().getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    override fun onCleared() { stopScanning(); alerts.shutdown() }
}
