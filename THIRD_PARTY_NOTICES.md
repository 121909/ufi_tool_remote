# Third-Party Notices

This project depends on third-party source and binary components. Review this
file before publishing source archives, APKs, or other binary releases.

## Android and JVM Dependencies

The Gradle build pulls these dependencies from Maven repositories:

- Android Gradle Plugin
- Kotlin, Kotlin serialization, and Kotlin coroutines
- AndroidX Core, Lifecycle, WorkManager, Activity Compose, and Compose UI
- Material 3 and Material Icons Extended
- OkHttp and MockWebServer
- JUnit

Refer to each dependency's published POM and license metadata for exact license
terms.

## EasyTier Native Libraries

The repository currently includes prebuilt arm64-v8a native libraries under
`app/src/main/jniLibs/arm64-v8a`:

- `libeasytier_android_jni.so`
- `libeasytier_ffi.so`
- `libboringtun_easytier-bb313973f1d8b687.so`
- `librustls_platform_verifier-2cb5197849bf9ef7.so`
- `libtun_easytier-255704813ad23ae3.so`

These binaries are used by the EasyTier Android integration. The upstream
EasyTier project is published at <https://github.com/EasyTier/EasyTier> and is
reported by GitHub as `LGPL-3.0`.

Before publishing binary releases, confirm the exact source revision used to
build these `.so` files and satisfy the applicable LGPL and transitive native
dependency obligations. At minimum, keep this notice, provide corresponding
source or build instructions for the native binaries where required, and do not
represent the bundled binaries as original project code.
