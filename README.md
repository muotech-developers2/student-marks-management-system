# Student Marks Management System

An Android app for managing class profiles, students, subjects, assessments, marks, reports, and Excel exports. Data is stored locally on the device.

## Build and test

Requires Android SDK platform 36 and a Java version supported by the configured Android Gradle plugin.

```sh
./gradlew testDebugUnitTest --console=plain
./gradlew :app:assembleDebug --console=plain
```

The app supports Android 8.0 (API 26) and later. The GitHub `v1.1` release includes an installable debug-signed APK for testing. It is not a production-signed distribution build.
