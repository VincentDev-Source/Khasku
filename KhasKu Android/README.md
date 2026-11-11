# KhasKu Android

Native Java app for Android 14 and newer. Budget and transaction data stay in app-private storage. The app declares no internet permission and makes no network requests.

## Build

Requires JDK 17 and Android SDK Platform 35. From this folder run `gradlew.bat --no-problems-report :app:assembleDebug`. Gradle may need network access once to fetch the Android Gradle Plugin; the installed app itself has no internet permission and works offline.

The debug APK is created at `app/build/outputs/apk/debug/app-debug.apk`.
