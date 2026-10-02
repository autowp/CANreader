# CanHacker-compatible devices

CANreader uses the CanHacker command protocol. A device sold as “CanHacker” is usable only when its interface and firmware are compatible with that protocol.

The Android app can connect through:

- USB serial, with Android USB host/OTG support.
- Bluetooth Classic SPP, after pairing the adapter.
- A compatible UDP network endpoint.

The app does not connect to Windows desktop software itself. Confirm the device's supported protocol, serial settings, CAN bit rate, and transport with its manufacturer or firmware documentation. See [supported adapters](adapters.md).
