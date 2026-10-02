# Android compatibility

CANreader requires Android 6.0 (API 23) or later, matching the app's configured minimum SDK.

USB host/OTG support is required only for USB serial adapters. The phone or tablet must support USB host mode and may need a USB OTG adapter and sufficient power for the attached device.

Bluetooth connections use Bluetooth Classic serial (SPP). Pair the adapter in Android settings first. On Android 12 and later, grant the nearby-device/Bluetooth permission when prompted.

Network connections use UDP and require the phone and adapter to be able to reach one another on the network. They do not require USB OTG.
