# NEON AIM CLIENT ANDROID

Android companion app with a neon purple/blue floating visual panel.

## Features
- Android permission flow for display-over-other-apps.
- Movable floating panel.
- Adjustable visual FOV ring size, saved locally.
- GitHub Actions workflow builds a debug APK and uploads it as an artifact.

## Important limitation
This is a visual overlay/demo only. It does not read or modify another app's memory, automate aiming, control Sandstrike, or bypass anti-cheat. The FOV ring is a visual guide and is not connected to game targeting.

## Build
GitHub Actions runs on pushes to main and via the Actions tab using the Android APK workflow.
Download the neon-aim-client-debug artifact after a successful run.
