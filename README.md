# CANreader

CANreader is an Android client for monitoring and transmitting CAN frames through a compatible adapter. It uses the CanHacker protocol; it is not a generic OBD-II or ELM327 application.

The app supports standard 11-bit and extended 29-bit CAN identifiers, data and remote-transmission-request (RTR) frames, a receive monitor, and transmit lists. It can import and export CanHacker `.txl` transmit-list files.

## Requirements

- Android 6.0 (API 23) or later.
- A CAN adapter that implements the CanHacker protocol.
- USB host/OTG is needed only when using a USB serial adapter. Bluetooth Classic SPP and UDP network connections are also available; a loopback connection is provided for testing.

See the [English documentation](docs/en/README.md) or [Russian documentation](docs/ru/README.md) for adapter and hardware details.

## Build

Install JDK 17 or newer, Android SDK Platform 37, and Android SDK Build-Tools 37.0.0. Open this directory in Android Studio and allow Gradle to sync. The project uses Gradle 9.8.0 and Android Gradle Plugin 9.4.1; dependency versions are managed in `gradle/libs.versions.toml`.

The Android, Java, Gradle, and XML extensions are listed in `.vscode/extensions.json`. Keep machine-specific SDK paths in the ignored `local.properties` file.

Run these commands from the repository root:

```sh
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
```
