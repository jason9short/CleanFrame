# CleanFrame for Android TV

An Android TV launcher wrapper for the shared `app/` UI. The build copies `app/` into the APK's assets and swaps in `android/web/device.js`. That file talks to `FrameBridge.java` instead of Tizen's application APIs.

Tested on an Android 16 phone emulator in landscape: rendering, remote navigation, Back, the installed-app list with icons, and app launch. It has not yet been run on an NVIDIA Shield or other Android TV hardware.

## Build

Needs JDK 17+ and the Android SDK (API 36). Create `android/local.properties` containing `sdk.dir=/path/to/Android/sdk`, then run:

```sh
cd android
./gradlew assembleDebug
# app/build/outputs/apk/debug/app-debug.apk
```

## Install on a Shield / Android TV

1. On the TV, open Settings → Device Preferences → About and click **Build** seven times. Then turn on **Network debugging** in Developer options.
2. Connect and install:
   ```sh
   adb connect <tv-ip>:5555
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
3. Make CleanFrame the default home screen. Either:
   - Set it as the target of Projectivy Launcher's "override launcher" setting (reversible, no ADB needed). Or:
   - Disable the stock launcher. Check the package name first:
     ```sh
     adb shell cmd package query-activities -a android.intent.action.MAIN -c android.intent.category.HOME
     adb shell pm disable-user --user 0 com.google.android.tvlauncher   # or com.google.android.apps.tv.launcherx
     ```
     To restore it: `adb shell pm enable com.google.android.tvlauncher`

Only disable the stock launcher after CleanFrame has opened and shown your apps. While it is disabled, CleanFrame's "Android TV Home" control has nothing to open.

## Platform mapping

| CleanFrame control | Android TV |
| --- | --- |
| App list | Apps with a TV (Leanback) launch entry, with icons served from `/cf-icon/<package>.png` |
| Built-in Home apps | Mapped from Tizen IDs to Android packages, falling back to a match on the app's name (`android/web/device.js`) |
| Art Mode | Screensaver (starts it, or opens screensaver settings) |
| Samsung Home | The stock Android TV / Google TV launcher |
| TV settings | Android Settings |
| Back key | CleanFrame's own Back navigation; it never closes the launcher |
