# CANreader Arduino hardware

The repository contains design files for two CANreader shield variants. The hardware pages are historical project references; verify the schematic, component values, board revision, and firmware before building or connecting a board.

## CANreader-FT

Designed for fault-tolerant CAN transceivers such as TJA1054/TJA1055.

![CANreader-FT, top](../i/ft-top.png)
![CANreader-FT, bottom](../i/ft-bottom.png)

[Open the CANreader-FT schematic](../i/canreader-ft.sch.png).

## CANreader-HS

Designed for high-speed CAN. Check the transceiver fitted to the particular board revision.

![CANreader-HS, top](../i/hs-top.png)
![CANreader-HS, bottom](../i/hs-bottom.png)

[Open the CANreader-HS schematic](../i/canreader-hs.sch.png).

## Controller, firmware, and app connection

The documented build uses an Arduino Nano and a CANreader shield. Firmware is maintained in the [CANreader firmware repository](https://github.com/autowp/can-usb). The firmware and any serial/Bluetooth/network bridge must speak the CanHacker protocol expected by the Android app.

The app supports USB serial, Bluetooth Classic SPP, and a CanHacker-compatible UDP connection. Each transport requires its corresponding hardware/interface; Bluetooth and Ethernet/network hardware are not built into the shield by default. See [supported adapters](adapters.md) and [Android compatibility](android.md).

## Assembly caution

The schematics and board photos are provided as reference material, not as a guarantee that every component list or old board revision is complete. Confirm polarity, pinout, power requirements, CAN transceiver type, and termination against the schematic and the parts in hand before assembly or use.
