# OpenBehindYou

[![Android CI](https://github.com/ajkompaw/OpenBehindYou/actions/workflows/android.yml/badge.svg)](https://github.com/ajkompaw/OpenBehindYou/actions/workflows/android.yml)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Min SDK 26](https://img.shields.io/badge/Min%20SDK-26-green.svg)](https://developer.android.com/about/versions/oreo)

> Private, on-device anti-shoulder-surfing detection for Android.

OpenBehindYou uses the front-facing camera, CameraX, and Google ML Kit to notice when a second person may be looking at your screen. All processing happens on the device. The app requests no internet permission, stores no images, and does not use a screen overlay.

## Features

- **Front-camera guard as a camera foreground service** (`GuardService`) that keeps working while you use other apps.
- **Adaptive frame analysis**: ~1.4–1.9 FPS while the scene is quiet, boosting to ~7–11 FPS when motion or a second-face candidate is detected.
- **ML Kit face detection** with `PERFORMANCE_MODE_FAST`, bundled model, tracking, and no network access.
- **Three-state heuristic classification**: Safe (green), Caution (amber), Alert (red).
- **Persistent notification pill** using a custom `RemoteViews` layout, plus an `IMPORTANCE_HIGH` heads-up alert and short haptic pulse when an over-the-shoulder risk is confirmed.
- **No overlay**: no `SYSTEM_ALERT_WINDOW` permission is requested. See [Android platform constraints](#android-platform-constraints).
- **No internet permission**: `INTERNET` and `ACCESS_NETWORK_STATE` are explicitly removed from the merged manifest.
- **No image storage**: frames are analyzed in memory and closed immediately.
- **Compose dashboard and settings** with sensitivity presets persisted in DataStore.
- **Battery-aware design**: no wake lock, no preview, latest-frame-only backpressure, cheap luma motion gate, and ML Kit duty cycling.

## Screens

- **Guard** — live status orb, threat state, background guard toggle, face/FPS/scan-mode metrics, permission warnings, last alert, privacy strip, and a short explanation of the detection pipeline.
- **Settings** — Low/Medium/High sensitivity, adaptive scanning explanation, battery expectations, privacy guarantees, limitations, and license information.

## Architecture

```
app/src/main/java/dev/openbehindyou/
├── OpenBehindYouApplication.kt
├── data/
│   └── SettingsRepository.kt          # DataStore: sensitivity only
├── camera/
│   ├── AdaptiveScanController.kt      # low/high duty-cycle window
│   ├── CameraAnalyzer.kt              # CameraX + ML Kit orchestration
│   └── MotionMeter.kt                 # tiny luma-grid motion gate
├── detector/
│   ├── FaceAssessment.kt              # raw/smoothed report models
│   ├── FaceEvaluator.kt               # primary/secondary face heuristics
│   ├── FaceObservation.kt             # normalized rotation-corrected faces
│   ├── MlKitFaceDetector.kt           # bundled ML Kit wrapper
│   ├── NormalizedRect.kt
│   ├── SafetyLevel.kt
│   ├── Sensitivity.kt                 # DetectionConfig presets
│   └── ThreatSmoother.kt              # confirmations + hold windows
├── service/
│   ├── GuardController.kt             # Compose/notification entry points
│   ├── GuardNotifications.kt          # channels, status pill, alert banner
│   ├── GuardRuntime.kt                # StateFlow bridge to Compose
│   └── GuardService.kt                # camera foreground service
└── ui/
    ├── MainActivity.kt
    ├── MainScreen.kt                  # permissions, tabs, scaffold
    ├── DashboardScreen.kt
    ├── SettingsScreen.kt
    ├── MainViewModel.kt
    ├── UiText.kt
    ├── components/GuardComponents.kt
    └── theme/
```

### Frame pipeline

1. `GuardService` enters foreground mode with `FOREGROUND_SERVICE_TYPE_CAMERA`.
2. CameraX binds `ImageAnalysis` to the service lifecycle using `LENS_FACING_FRONT`, `STRATEGY_KEEP_ONLY_LATEST`, and a 640×480 target.
3. Every frame is offered to a 24×24 luma motion meter. This is cheap and runs at a few FPS.
4. ML Kit runs only when the adaptive controller's next scan time has arrived. Frames that arrive early are closed immediately.
5. Face bounding boxes are normalized against the upright (rotation-corrected) image.
6. `FaceEvaluator` picks the primary face, rejects distant background faces, and classifies the frame.
7. `ThreatSmoother` requires consecutive alert confirmations, then holds the alert for a short window.
8. The service updates the ongoing notification/`StateFlow` and, on a red transition, posts a high-importance alert and vibrates briefly.

There is only one camera session: the service owns the camera and the app intentionally does not show a preview.

## Detection heuristics

The classifier is deliberately explainable and local:

- The largest sufficiently large face is treated as the primary user.
- A secondary face is ignored when it is too small relative to the primary face.
- A secondary face is also ignored when it is too far to the side, too far below the primary head, or otherwise outside the near-head zone.
- A secondary face becomes **Caution** when it is proportionally close to the primary face.
- **Alert** requires a larger secondary face, tight proximity, and a head pose facing toward the screen (absolute yaw/pitch limits). It needs several consecutive detections (two to four depending on sensitivity).
- Motion from the luma gate only produces Caution when a weak second-face candidate is also present; the user's own movement does not raise an alert by itself.

Head pose is **not** true gaze detection. It is a proxy that avoids flagging people who are merely passing behind you while looking sideways.

### Sensitivity presets

| Preset | Secondary/primary area | Alert area | Alert yaw | Confirmations | Low scan | High scan |
|---|---:|---:|---:|---:|---:|---:|
| Low | 30% | 45% | ±20° | 4 | 700 ms | 140 ms |
| Medium | 22% | 34% | ±28° | 3 | 620 ms | 110 ms |
| High | 15% | 25% | ±35° | 2 | 540 ms | 95 ms |

All geometry is relative to the primary face, so results do not depend on camera resolution or screen size.

## Android platform constraints

### No floating overlay

A true always-on floating pill above other apps requires `SYSTEM_ALERT_WINDOW`, which is intrusive and explicitly excluded by the project brief. Instead:

- A persistent, pill-shaped **status notification** shows the live green/amber/red state.
- A high-importance **heads-up notification** appears above the current app on a red alert.
- A short manual vibration accompanies the heads-up alert.

The alert channel can be silenced or blocked by the user; the app warns in the dashboard when notifications are disabled.

### Camera foreground service

Android 14+ requires `FOREGROUND_SERVICE_CAMERA` and a visible user action to start a camera foreground service. OpenBehindYou therefore:

- starts only from the dashboard toggle while the app is visible;
- uses `START_NOT_STICKY` and never auto-starts the camera after a process/service restart;
- declares `android:foregroundServiceType="camera"`;
- stops the camera and notification when Guard is turned off.

Some OEM battery managers may still stop long-running services. Exempting the app from battery optimization can improve reliability; this is outside the app's control and is not requested automatically.

### Permissions

- `CAMERA` — required; runtime permission.
- `POST_NOTIFICATIONS` — requested on Android 13+; without it the guard can still run and vibrate, but banners/status are hidden.
- `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_CAMERA` — required for the camera foreground service.
- `VIBRATE` — short alert haptic.
- No storage, location, microphone, overlay, or network permissions.

## Privacy design

- `INTERNET` and `ACCESS_NETWORK_STATE` are explicitly removed from the merged manifest, even if a dependency contributes them.
- ML Kit face detection uses the **bundled** model; there is no model download and no cloud inference.
- Frames are analyzed in memory and discarded; nothing is saved to disk.
- The only persisted setting is the sensitivity value in DataStore.
- No analytics, crash upload, ads, or remote configuration.
- No `SYSTEM_ALERT_WINDOW`, no accessibility service, no screen capture.

See [PRIVACY.md](PRIVACY.md).

## Build

### Get the code

```bash
git clone https://github.com/ajkompaw/OpenBehindYou.git
cd OpenBehindYou
```

### Download a prebuilt APK

Signed APKs for tagged versions are attached to the [Releases page](https://github.com/ajkompaw/OpenBehindYou/releases) as `OpenBehindYou-vX.Y.Z.apk`.

### Requirements

- JDK 17–22 (the verified local build used JDK 22; CI uses 17)
- Android SDK Platform 35 and Build-Tools 35.0.0
- Gradle wrapper included (Gradle 8.9)
- Android Studio 2024.2+ or command-line Gradle

If Android Studio uses a newer JBR than the Gradle 8.9/AGP 8.7 stack supports, set **Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK** to a JDK 17 or 22 installation.

`local.properties` is intentionally **not** committed — it is git-ignored because it contains machine-specific paths. Android Studio generates it automatically when you open the project. For command-line builds, either set the `ANDROID_HOME` or `ANDROID_SDK_ROOT` environment variable, or create `local.properties` yourself:

```properties
sdk.dir=C\:\\path\\to\\your\\Android\\sdk
```

```bash
# Windows
./gradlew.bat :app:assembleDebug

# macOS / Linux
./gradlew :app:assembleDebug
```

Then install:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Tests and checks

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:assembleRelease
```

### Release signing

Release APKs are signed in CI and published automatically. The keystore is **never** committed — it exists only as GitHub Actions secrets.

**One-time setup** (repository owner):

1. Keep the generated keystore (`keystore/openbehindyou-release.jks`, git-ignored) and its password in a password manager.
2. Add these secrets under **Settings → Secrets and variables → Actions**:
   - `KEYSTORE_B64` — keystore file, base64-encoded as a single line
   - `KEYSTORE_PASSWORD` — keystore password
   - `KEY_ALIAS` — key alias (default: `openbehindyou`)
   - `KEY_PASSWORD` — key password (optional; defaults to the keystore password)

**Cutting a release:**

```bash
git tag v1.0.0
git push origin v1.0.0
```

The Release workflow then builds, signs, verifies the signature with `apksigner`, and attaches `OpenBehindYou-v1.0.0.apk` to a new GitHub Release with generated notes.

**Local signed builds** (optional): export `OBY_STORE_FILE`, `OBY_STORE_PASSWORD`, `OBY_KEY_ALIAS`, and optionally `OBY_KEY_PASSWORD` before `./gradlew :app:assembleRelease`, or set the equivalent `oby.storeFile` / `oby.storePassword` / `oby.keyAlias` / `oby.keyPassword` Gradle properties. Without them, release builds are unsigned — as before. Release builds enable R8 and resource shrinking either way.

## Technology

- Kotlin 2.0.21, Kotlin DSL, Java 17 toolchain
- Jetpack Compose (BOM 2024.12.01), Material 3
- CameraX 1.4.1
- Google ML Kit face detection 16.1.7 (bundled)
- DataStore Preferences 1.1.1
- Min SDK 26, target/compile SDK 35

## Known limitations

- Heuristics cannot know intent or actual gaze. Posters, reflections, crowded rooms, or a person standing beside you can cause false positives.
- Detection assumes your face is visible as the largest, sufficiently large face. Looking away, covering the camera, or holding the phone so only the peeker is in frame reduces reliability.
- A person approaching from directly behind may be outside the front camera's field of view.
- While Guard is running it owns the front camera. Camera/video-call apps may be unable to use it until Guard is stopped.
- A cheater who stays out of frame or uses a camera/recording device is not detected.
- The front camera consumes battery while Guard is on.
- Do not treat an alert as proof of an attack; use it as a prompt to check your surroundings.

## Contributing

Issues and pull requests are welcome. Please keep the privacy invariants intact: no internet permission, no overlay permission, no frame persistence, and no cloud inference. Run tests and lint before opening a PR.

## License

Copyright 2026 ajkompaw.

Licensed under the Apache License, Version 2.0 — see [LICENSE](LICENSE) and [NOTICE](NOTICE).

You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-2.0> unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
