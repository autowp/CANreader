# CANreader

CANreader is an Android client for monitoring and transmitting CAN frames through adapters that implement the CanHacker protocol.

## Features

- Monitor received CAN messages and inspect individual messages.
- Create, edit, schedule, and manage transmit frames.
- Support standard 11-bit and extended 29-bit identifiers, data frames, and RTR frames.
- Import and export CanHacker `.txl` transmit lists.
- Connect over USB serial, Bluetooth Classic SPP, or a CanHacker-compatible UDP network endpoint. A loopback adapter is available for testing.

The app requires Android 6.0 (API 23) or later. USB host/OTG support is required only for USB connections. Bluetooth and network connections do not require USB OTG.

## Adapters and hardware

- [Supported adapters and connection types](adapters.md)
- [CANreader Arduino hardware](canreader-device.md)
- [Seeed Studio CAN BUS Shield](seeed-can-bus-shield.md)
- [CanHacker devices](canhacker.md)
- [ELM327 status](ELM327.md)

## Safety and troubleshooting

- [Known issues](known-issues.md)
- [Connecting to a vehicle CAN bus](car.md)
- [Disclaimer and safety notice](disclaimer.md)
- [Android compatibility](android.md)

## App

- [CANreader for Android](canreader-android.md)
- [License](../../LICENSE)

See the [project README](../../README.md) for build instructions.
