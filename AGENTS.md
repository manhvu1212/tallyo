# Tallyo — Native Android

Tallyo is now a native Android app (Kotlin + Jetpack Compose). The previous Expo/React Native code has been removed. A native iOS port is planned but not yet started.

## Project layout
- `android/` — Android app (Gradle Kotlin DSL, Compose, Room, Material 3).
- `ios/` — reserved for the future native iOS port.
- `PRIVACY_POLICY.md` — published privacy policy.

## Toolchain
- Kotlin 2.0.21, AGP 8.7.3, Gradle 8.10.2.
- minSdk 26, targetSdk/compileSdk 35.
- Java 17 toolchain.
- Compose BOM 2024.12.01, Material 3 1.3.x, Navigation Compose 2.8.x.
- Room 2.6.1 via KSP.
- Per-app locales via `AppCompatDelegate` (API < 33) / `LocaleManager` (API 33+); supported locales declared in `res/xml/locales_config.xml`.

## Build & run
- Open `android/` in Android Studio (Ladybug or newer) and use the `app` run config.
- Or from CLI inside `android/`: `gradlew assembleDebug` (Windows: `gradlew.bat`).
- Release signing reads `NMV_KEYSTORE` / `NMV_KEYSTORE_PASS` from the user-level `~/.gradle/gradle.properties`; the alias is hardcoded as `tallyo` in `app/build.gradle.kts`.

## Conventions
- Compose-first: no XML layouts beyond resources (themes, splash, adaptive icons, locale config).
- ViewModels per screen, repository-backed flows via Room.
- Strings live in `res/values/strings.xml` (English default) and `res/values-vi/strings.xml`. Plural strings use `<plurals>` so quantity selection respects locale rules.
- Colors come from `ui/theme/Color.kt`; do not introduce new palette entries lightly.
