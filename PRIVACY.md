# Privacy Policy for OpenBehindYou

**Effective date:** 2026-11-17

OpenBehindYou is designed as a local-only privacy guard. This policy describes exactly what the app does with information.

## Summary

- **No network access.** The app does not request the Android `INTERNET` or `ACCESS_NETWORK_STATE` permissions. The merged manifest explicitly removes them.
- **No image storage.** Camera frames are processed in memory and immediately discarded.
- **No account, analytics, advertising, or crash uploads.**
- **No overlay or accessibility access.**
- The only persisted setting is the selected sensitivity level.

## Camera and face detection

With your permission, OpenBehindYou uses the front-facing camera while the Guard foreground service is running. Camera frames are passed to the bundled Google ML Kit face detector for local inference. The ML Kit model is packaged inside the app; it does not download a model or send images to a server.

The detector produces face bounding boxes, tracking IDs, and head-orientation angles. These values are used only for the local green/amber/red heuristic and are not stored. Frames are closed as soon as analysis finishes.

Android displays its camera privacy indicator while the camera is in use, and OpenBehindYou displays an ongoing foreground-service notification. You can stop Guard from the app or from that notification.

## Data stored on the device

The app stores a single preference in Android DataStore: the sensitivity preset (Low, Medium, or High). Android cloud backup and device-transfer backup are disabled for the app, so this preference is not backed up by the system.

No photos, videos, face templates, embeddings, logs of detected people, or location data are written to storage.

## Third-party components

- **Google ML Kit face detection (bundled)** performs inference on-device. No cloud API is used.
- **AndroidX/CameraX, DataStore, and Jetpack Compose** are local libraries.

Some transitive libraries include local Play Services support classes. The app has no network permission, so they cannot transmit data even if such code is present.

## Permissions

| Permission | Purpose |
|---|---|
| `CAMERA` | Local face analysis while Guard is on. |
| `POST_NOTIFICATIONS` | Ongoing status indicator and heads-up alerts on Android 13+. |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_CAMERA` | Keep the camera guard running while you use other apps. |
| `VIBRATE` | Short haptic alert. |

The app does not request storage, microphone, location, contacts, accessibility, screen capture, or `SYSTEM_ALERT_WINDOW`.

## Your controls

- Turn Guard on/off at any time in the app.
- Stop Guard from the ongoing notification.
- Revoke camera or notification permission in Android settings.
- Clear app storage to delete the sensitivity preference.
- Uninstall the app to remove all local data.

## Children

The app is not directed at children and collects no personal information.

## Changes

If this policy changes, the updated version will be published with the app source. The privacy invariants above—no network permission, no image persistence, no overlay—are core project constraints.

## Contact

Open an issue in the project repository for privacy questions. Do not include sensitive personal data in an issue.
