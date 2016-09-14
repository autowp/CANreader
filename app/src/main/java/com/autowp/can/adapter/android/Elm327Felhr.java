package com.autowp.can.adapter.android;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import com.autowp.can.CanAdapter;
import com.autowp.can.CanBusSpecs;
import com.autowp.can.adapter.canhacker.command.Command;
import com.autowp.can.adapter.elm327.Elm327;
import com.autowp.can.adapter.elm327.Elm327Exception;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.util.Arrays;

/**
 * Created by autow on 28.02.2016.
 */
public class Elm327Felhr extends Elm327 {
    private UsbManager mUsbManager;
    private UsbDevice mUsbDevice;
    private UsbSerialDevice mSerial;

    public Elm327Felhr(CanBusSpecs specs, final UsbManager usbManager, final UsbDevice usbDevice) throws Elm327FelhrException {
        super(specs);

        mUsbManager = usbManager;
        mUsbDevice = usbDevice;
    }

    protected synchronized void doConnect() throws Elm327Exception {
        UsbDeviceConnection connection = mUsbManager.openDevice(mUsbDevice);
        if (connection == null) {
            throw new Elm327FelhrException("Opening device failed");
        }

        mSerial = UsbSerialDevice.createUsbSerialDevice(mUsbDevice, connection);

        if (mSerial == null) {
            throw new Elm327FelhrException("Driver not found");
        }

        mSerial.syncOpen();
        mSerial.setBaudRate(BAUDRATE);
        mSerial.setDataBits(UsbSerialInterface.DATA_BITS_8);
        mSerial.setParity(UsbSerialInterface.PARITY_NONE);

        //mSerial.read(mCallback);
        try {
            super.doConnect();
        } catch (Elm327Exception e) {
            mSerial.syncClose();
            mSerial = null;

            connection.close();

            throw e;
        }
    }

    protected synchronized void doDisconnect() throws Elm327Exception {
        super.doDisconnect();

        if (mSerial != null) {
            mSerial.syncClose();
            mSerial = null;
        }
    }

    private synchronized Elm327Felhr send(final Command c) throws Elm327FelhrException
    {
        byte[] command = c.getBytes();
        byte[] data = new byte[command.length + 1];
        System.arraycopy(command, 0, data, 0, command.length);
        data[data.length-1] = Byte.parseByte("\n");

        mSerial.syncWrite(data, 60000);

        return this;
    }

    @Override
    protected byte[] readBytes(final int timeout) {
        System.out.println("readBytes");
        byte[] buffer = new byte[64];
        int readCount = mSerial.syncRead(buffer, timeout);
        return Arrays.copyOfRange(buffer, 0, readCount);
    }
}
