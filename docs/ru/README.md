# CANreader

CANreader - приложение и устройство для взаимодействия с сетью CAN. Аналог CanHacker. Для работы требуется CAN-адаптер.

## Возможности

- Отправки и получений CAN фреймов
- RTR фреймы
- Стандартные (11bit) и расширенные (11 bit) CAN фреймы
- Поддержка двух типов CAN сейтей: high-speed & fault tolerant (только [CANreader-FT](canreader-device.md))
- Совместимость с CanHacker (приложение для windows): подержка файлов TxList, Trace [запланировано] & RxList [запланировано], единый протокол.
- Поддержка нескольких адаптеров (на данный момент [CANreader](canreader-device.md), [CanHacker](canhacker.md), [Seeedstudio CAN shield](seeed-can-bus-shield.md))
- Несколько типов физического соединения: 
    - последоватьельный порт через USB ([поддержка нескольких чипсетов](https://github.com/felHR85/UsbSerial))
    - ethernet ([CANreader](canreader-device.md) & [Seeed shield only](seeed-can-bus-shield.md)) [запланировано]. WiFi используя точку доступа
    - Bluetooth ([CANreader](canreader-device.md) & [Seeed shield only](seeed-can-bus-shield.md)) [запланировано]

## Требования

- [Android](android.md)
- [CANreader (устройство)](canreader-device.md) или один из [поддерживаемых адаптеров](adapters.md)

## Другое

- [Известные проблемы](known-issues.md)
- [CAN bus in car](car.md)
- [Отказ от ответственности](disclaimer.md)
- [Лицензия](../../LICENSE.md)