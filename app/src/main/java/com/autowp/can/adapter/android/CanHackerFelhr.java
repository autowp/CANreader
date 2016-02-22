package com.autowp.can.adapter.android;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import com.autowp.can.CanAdapterException;
import com.autowp.can.CanClient;
import com.autowp.can.adapter.canhacker.CanHacker;
import com.autowp.can.adapter.canhacker.CanHackerException;
import com.autowp.can.adapter.canhacker.command.Command;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

/**
 * Created by autow on 12.02.2016.
 */
public class CanHackerFelhr extends CanHacker {
    private UsbManager mUsbManager;
    private UsbDevice mUsbDevice;
    private UsbSerialDevice mSerial;

    public CanHackerFelhr(final UsbManager usbManager, final UsbDevice usbDevice) throws CanHackerFelhrException {
        super();

        mUsbManager = usbManager;
        mUsbDevice = usbDevice;
    }

    protected synchronized void doConnect() throws CanHackerException {
        UsbDeviceConnection connection = mUsbManager.openDevice(mUsbDevice);
        if (connection == null) {
            throw new CanHackerFelhrException("Opening device failed");
        }

        mSerial = UsbSerialDevice.createUsbSerialDevice(mUsbDevice, connection);

        if (mSerial == null) {
            throw new CanHackerFelhrException("Driver not found");
        }

        mSerial.open();
        mSerial.setBaudRate(BAUDRATE);
        mSerial.setDataBits(UsbSerialInterface.DATA_BITS_8);
        mSerial.setParity(UsbSerialInterface.PARITY_NONE);

        //mSerial.read(mCallback);
        try {
            super.doConnect();
        } catch (CanHackerException e) {
            mSerial.close();
            mSerial = null;

            connection.close();

            throw e;
        }
    }

    protected synchronized void doDisconnect() throws CanAdapterException {
        super.doDisconnect();

        if (mSerial != null) {
            mSerial.close();
            mSerial = null;
        }
    }

    public synchronized CanHackerFelhr send(final Command c) throws CanHackerFelhrException
    {
        if (connectionState == CanClient.ConnectionState.DISCONNECTED) {
            throw new CanHackerFelhrException("CanHacker is disconnected");
        }

        byte[] command = c.getBytes();
        byte[] data = new byte[command.length + 1];
        System.arraycopy(command, 0, data, 0, command.length);
        data[data.length-1] = COMMAND_DELIMITER;

        mSerial.writeNow(data);

        return this;
    }

    @Override
    protected byte[] readBytes(final int timeout) {
        return mSerial.readNow(64, timeout);
    }
}
