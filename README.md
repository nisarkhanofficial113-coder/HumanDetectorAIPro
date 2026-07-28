# Human Detection App

Real-time human detection Android application using CameraX and Google ML Kit Object Detection.

## Requirements

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK API 34
- Minimum device: Android 7.0 (API 24)

## Build

```bash
./gradlew assembleDebug
```

The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Open in Android Studio

`File > Open` and select this project's root directory. Android Studio will
sync Gradle automatically.

## Features

- Live camera preview (CameraX)
- On-device object detection with ML Kit (filtered to persons)
- Bounding-box overlay with confidence percentage
- Real-time count of detected humans
