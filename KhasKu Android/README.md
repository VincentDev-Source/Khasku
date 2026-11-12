# KhasKu Android

Native Java app for Android 14 and newer. Budget and transaction data stay in app-private storage. The app declares no internet permission and makes no network requests.

## Download

[Download the latest APK](https://github.com/VincentDev-Source/Khasku/raw/refs/heads/main/KhasKu%20Android/app/build/outputs/apk/debug/app-debug.apk) from the `main` branch.

## Ledger

KhasKu starts with a 50/30/20 allocation for needs, wants, and an emergency fund; the percentages remain editable. Transactions can be assigned to multiple named wallets, and custom categories can be added to any of the three budget groups. Tap a transaction in the history to edit or delete it. Existing transactions are kept and assigned to the default wallet when the ledger is first opened after this update.

## Build

Requires JDK 17 and Android SDK Platform 35. From this folder run `gradlew.bat --no-problems-report :app:assembleDebug`. Gradle may need network access once to fetch the Android Gradle Plugin; the installed app itself has no internet permission and works offline.

The debug APK is created at `app/build/outputs/apk/debug/app-debug.apk`.
