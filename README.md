# UFI Remote

Kotlin + Jetpack Compose Android client for devices running UFI-TOOLS Full.

UFI Remote focuses on repeated operational use: live device status, SMS
management, quick replies, a home-screen widget, and EasyTier-backed remote
access for devices that are not on the same LAN.

## Features

- Device dashboard for connection status, SIM/network data, traffic counters,
  connected clients, bands, and cell information.
- SMS list, SMS detail view, compose flow, delete/send actions, and quick
  replies.
- App widget for unread SMS, refresh, quick reply, and SMS detail shortcuts.
- EasyTier foreground service integration with status parsing and relay-aware
  peer display.
- Local settings storage in Android app-private data.

This project is not affiliated with UFI-TOOLS, OpenList, or EasyTier.

## Requirements

- JDK 17
- Android SDK with API 35 and build-tools 35.0.0
- Gradle 8.9, or the checked-in Gradle wrapper
- Windows PowerShell for scripts under `scripts/`

The repository does not commit `local.properties`. Android Studio can create it
automatically, or you can copy `local.properties.example` and adjust `sdk.dir`.

## Build

With the Gradle wrapper:

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
```

With the isolated Windows toolchain scripts:

```powershell
.\scripts\install-android-build-tools.ps1
.\scripts\build-d.ps1 -Task test
.\scripts\build-d.ps1 -Task assembleDebug
```

The Windows build script keeps JDK, Gradle, Android SDK, and Gradle caches under
`D:\ufi-remote-build-tools\android-build-tools` by default. Override
`-ToolsRoot` if you want another location.

## App Configuration

In the app settings screen, enter:

- UFI-TOOLS base URL, for example `http://192.168.0.1:2333`
- UFI-TOOLS token
- Factory web admin password
- Optional EasyTier network settings

Do not commit real device URLs, tokens, passwords, EasyTier network secrets, or
OpenList tokens. Local helper files such as `.token`, `.env`, and
`local.properties` are ignored by Git.

## Upload a Debug APK to OpenList

Build the debug APK first:

```powershell
.\scripts\build-d.ps1 -Task assembleDebug
```

Upload it:

```powershell
.\scripts\upload-apk.ps1 `
  -Server http://192.168.1.100:5244 `
  -Token "your-token" `
  -LocalFile .\app\build\outputs\apk\debug\app-debug.apk `
  -RemotePath /codex/app-debug.apk
```

`-Token` can be omitted when the OpenList server allows anonymous uploads. You
can also provide it through the `OPENLIST_TOKEN` environment variable. The
script uses `PUT /api/fs/put` with a `File-Path` header.

## Repository Layout

```text
app/src/main/java/com/example/ufitoolsremote/
  data/       repositories and settings persistence
  easytier/   EasyTier config, foreground service, and status parser
  model/      UI and repository data models
  network/    UFI-TOOLS HTTP client and codecs
  ui/         MainViewModel and UI state
  widget/     Android app widget
scripts/      Windows build, toolchain, and upload helpers
design-system/ design notes for UI work
```

## Native Libraries

The app currently includes arm64-v8a native libraries under
`app/src/main/jniLibs/arm64-v8a` for EasyTier Android integration. See
`THIRD_PARTY_NOTICES.md` before publishing source or binary releases.

## Contributing

See `CONTRIBUTING.md`.
