# Human Detector AI Pro

Ultra-premium, futuristic Android app for real-time nearby-human detection with
sensor fusion across every technology the device physically supports.

> **Zero fabrication rule.** The app probes each supported sensor at runtime and
> only surfaces detections that come from real, hardware-verified signals. If
> LiDAR / UWB / mmWave / ToF are unavailable on the device, they are labeled
> **N/A** and never emit synthetic targets.

---

## Stack

| Concern            | Choice                                                        |
|--------------------|---------------------------------------------------------------|
| Language           | Kotlin                                                        |
| UI                 | Jetpack Compose + Material 3                                  |
| Architecture       | MVVM (`RadarViewModel` + immutable `UiState`)                 |
| Concurrency        | Kotlin Coroutines + `Flow` (`callbackFlow`) per sensor        |
| Persistence hooks  | DataStore Preferences (wired for future settings persistence) |
| Camera fallback    | CameraX (dependency wired; scanner intentionally left to plug in a TFLite model of your choice) |
| UWB                | `androidx.core.uwb`                                           |
| BLE                | Platform `BluetoothLeScanner`                                 |
| Wi-Fi Aware        | Platform `WifiAwareManager`                                   |
| Ultrasonic proxy   | `SensorManager` `TYPE_PROXIMITY`                              |
| Alerts             | `ToneGenerator`, `VibratorManager`, `TextToSpeech`            |
| Background         | Foreground service (`FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE`)|

## Sensors and how they're treated

| Sensor        | Probe result                                                                                                    | Emits real ranges? |
|---------------|-----------------------------------------------------------------------------------------------------------------|--------------------|
| **LiDAR**     | No first-party Android API — always reported **N/A** unless a vendor SDK is plugged in                          | –                  |
| **ToF**       | Detected via `CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT`; scanner stub for pluggable SDK | Device-dependent   |
| **UWB**       | Detected via system feature `android.hardware.uwb`; ranging requires a paired responder                          | Yes, when peered   |
| **mmWave**    | No public Android API — always **N/A**                                                                          | –                  |
| **BLE**       | Emits distance via RSSI path-loss model with `n=2`; bearing is `null` (BLE cannot resolve direction)             | Distance only      |
| **Wi-Fi Aware**| Real peer discovery only — never fake targets                                                                    | With RTT hardware  |
| **Ultrasonic**| Uses `TYPE_PROXIMITY`; emits high-confidence short-range detections when the sensor triggers                    | Yes                |
| **Camera AI** | CameraX wired; plug your TFLite / MediaPipe person detector into `sensors/CameraScanner.kt` (not shipped)        | Yes (fallback)     |

The fusion layer merges evidence from all live sensors using a **noisy-OR**
combination on confidence and (distance, bearing) proximity for association.

## Build

```
./gradlew :app:assembleDebug
```

Open the project in Android Studio (Iguana or newer). Requires JDK 17.

## Runtime UI

- **Radar canvas** — 360° sweeping green beam, distance rings at 5 / 10 / 25 / 50 / 100 / 200 m, red target dots with confidence, halo pulse on newly-tracked targets.
- **HUD bar** — target count, best-signal %, scan state, battery.
- **Status chips** — one per sensor kind, with `LIVE / PERM / OFF / N/A`.
- **Settings** — per-sensor toggles (disabled for missing hardware), sensitivity slider, voice / sound / vibration / night-mode / battery-saver switches.

## Alerts

- New person → radar flash + vibration + tone + TTS **"One person detected."**
- Additional person → **"Two people detected."** etc.
- Person leaves → different tone + TTS **"One person left."** (or "X remaining." / "All people have left.")

## Extending

- `sensors/*Scanner.kt` — one file per sensor; implement `SensorScanner` and emit
  `RawDetection` objects only when the hardware physically observed something.
- `fusion/Fusion.kt` — track association + noisy-OR confidence merge.
- `alerts/AlertBus.kt` — swap the tone / vibration pattern / voice locale here.
