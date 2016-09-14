# CANreader

CANreader - application for communication with CAN. CanHacker analog. Useless without CAN-adapter.

## Features

- Send and receive CAN frames
- RTR frames
- Standart (11bit) and extended (11 bit) CAN frames
- Support two CAN networks types: high-speed & fault tolerant ([CANreader-FT](canreader-device.md) only)
- CanHacker (windows application) compatibility: TxList, Trace [planned] & RxList [planned]  files support, same protocol.
- Support various adapters (currently [CANreader](canreader-device.md), [CanHacker](canhacker.md), [Seeedstudio CAN shield](seeed-can-bus-shield.md))
- Various types of connection: 
    - USB serial ([support various chipsets](https://github.com/felHR85/UsbSerial))
    - ethernet ([CANreader](canreader-device.md) & [Seeed shield only](seeed-can-bus-shield.md)) [planned]. WiFi using external access point
    - Bluetooth ([CANreader](canreader-device.md) & [Seeed shield only](seeed-can-bus-shield.md)) [planned]

## Requirements

- [Android](android.md)
- [CANreader (device)](canreader-device.md) or one of [supported adapters](adapters.md)

## Other

- [Known issues](known-issues.md)
- [CAN bus in car](car.md)
- [Disclaimer](disclaimer.md)
- [License](../../LICENSE.md)