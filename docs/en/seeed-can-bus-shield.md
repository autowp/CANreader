# Seeed Studio CAN BUS Shield

The Seeed Studio CAN BUS Shield is a CAN controller/transceiver board intended to be used with a compatible Arduino board (historically, Arduino Uno). The shield alone is not an Android adapter: the Arduino firmware and the connection interface must also implement the CanHacker protocol.

The legacy project notes link the board to the [CANreader firmware repository](https://github.com/autowp/can-usb). Verify current firmware support, board revision, CAN transceiver, wiring, power, and termination before use.

Android connections require an appropriate USB serial, Bluetooth Classic SPP, or UDP bridge. The app's UDP mode uses port `11111` by default. See [supported adapters](adapters.md) and [Android compatibility](android.md).

For product specifications and current documentation, consult the [Seeed Studio CAN BUS Shield documentation](https://wiki.seeedstudio.com/CAN-BUS_Shield_V2.0/).

> **Safety:** Do not attach an unverified shield/Arduino setup to a vehicle network or transmit frames until you have confirmed the hardware and firmware behavior.
