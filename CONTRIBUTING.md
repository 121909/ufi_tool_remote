# Contributing

## Development Setup

Use one of these build paths:

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
```

or:

```powershell
.\scripts\install-android-build-tools.ps1
.\scripts\build-d.ps1 -Task test
.\scripts\build-d.ps1 -Task assembleDebug
```

Do not commit `local.properties`, `.token`, `.env`, signing keys, or real device
credentials.

## Code Guidelines

- Keep changes scoped to the feature or bug being fixed.
- Preserve existing Compose, repository, and model patterns.
- Keep new device fields nullable unless persistence and migration are handled
  explicitly.
- Add focused unit tests for parsers, codecs, settings, repository behavior, and
  EasyTier status handling.
- Do not replace the bundled native libraries without documenting the upstream
  revision and license obligations in `THIRD_PARTY_NOTICES.md`.

## Pull Request Checklist

- `test` passes.
- `assembleDebug` passes when app code or resources change.
- No generated build output, APKs, tokens, passwords, or signing material are
  included.
- README or third-party notices are updated when setup, upload behavior, or
  bundled binaries change.
