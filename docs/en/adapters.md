# Supported adapters and connections

CANreader communicates with CAN hardware using the CanHacker command protocol. A connector or CAN controller being electrically compatible is not sufficient: the attached device or its firmware must implement that protocol.

## Connection types

| Connection | Requirements | Notes |
| --- | --- | --- |
| USB serial | Android USB host/OTG and a supported USB serial device | Select the USB device and CAN bitrate in the app. USB serial baud rate is configured separately. |
| Bluetooth | A paired Bluetooth Classic SPP adapter implementing the CanHacker protocol | Bluetooth Low Energy-only devices are not supported by this connection. |
| Network (UDP) | A reachable CanHacker-compatible UDP endpoint | The app uses UDP port `11111` by default. Configure the endpoint address and CAN bitrate to match the adapter. |
| Loopback | None | A local test connection; it does not communicate with a physical CAN bus. |

## Known hardware examples

- [CANreader Arduino hardware](canreader-device.md): use the firmware linked on that page and a supported connection interface.
- [Seeed Studio CAN BUS Shield](seeed-can-bus-shield.md): requires a compatible Arduino board and CanHacker-protocol firmware/bridge.
- [CanHacker devices](canhacker.md): supported when exposed through a compatible USB serial or Bluetooth SPP interface.

The app does not currently offer an ELM327 connection workflow. See [ELM327 status](ELM327.md). Not every product sold under a compatible device name implements the same protocol; check the adapter documentation before connecting.
