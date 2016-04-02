package com.autowp.can.adapter.android;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.hardware.usb.UsbDeviceConnection;

import com.autowp.can.CanAdapterException;
import com.autowp.can.CanBusSpecs;
import com.autowp.can.adapter.canhacker.CanHacker;
import com.autowp.can.adapter.canhacker.CanHackerException;
import com.autowp.can.adapter.canhacker.command.Command;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.UUID;

/**
 * Created by dima on 02.04.16.
 */
public class CanHackerBluetooth extends CanHacker {

    private static final String SERIAL_UUID = "00001101-0000-1000-8000-00805F9B34FB";

    private BluetoothDevice mDevice;
    BluetoothSocket mSocket = null;

    public CanHackerBluetooth(CanBusSpecs specs, final BluetoothDevice device) throws CanHackerFelhrException {
        super(specs);

        mDevice = device;
    }

    protected synchronized void doConnect() throws CanHackerException {

        OutputStream out = null;
        try {
            mSocket = mDevice.createRfcommSocketToServiceRecord(UUID.fromString(SERIAL_UUID));
            mSocket.connect();
        } catch (IOException e) {
            throw new CanHackerBluetoothException("Connect I/O error: " + e.getMessage());
        }

        try {
            super.doConnect();
        } catch (CanHackerException e) {
            try {
                mSocket.close();
            } catch (IOException e1) {
                mSocket = null;
                throw new CanHackerBluetoothException("Connect I/O error: " + e1.getMessage());
            }

            throw e;
        }
    }

    protected synchronized void doDisconnect() throws CanAdapterException {
        super.doDisconnect();

        if (mSocket != null) {
            try {
                mSocket.close();
            } catch (IOException e1) {
                mSocket = null;
                throw new CanHackerBluetoothException("Disconnect I/O error: " + e1.getMessage());
            }
            mSocket = null;
        }
    }

    @Override
    protected CanHacker send(Command c) throws CanHackerException {
        byte[] command = c.getBytes();
        byte[] data = new byte[command.length + 1];
        System.arraycopy(command, 0, data, 0, command.length);
        data[data.length-1] = COMMAND_DELIMITER;

        try {
            mSocket.getOutputStream().write(data);
        } catch (IOException e) {
            throw new CanHackerBluetoothException("Send I/O error: " + e.getMessage());
        }

        return this;
    }

    @Override
    protected byte[] readBytes(int timeout) throws CanHackerBluetoothException {
        if (mSocket == null) {
            throw new CanHackerBluetoothException("Device not connected");
        }

        byte[] buffer = new byte[64];
        try {
            int readCount = mSocket.getInputStream().read(buffer);

            if (readCount < 0) {
                return new byte[0];
            }
            return Arrays.copyOfRange(buffer, 0, readCount);

        } catch (IOException e) {
            throw new CanHackerBluetoothException("Receive I/O error: " + e.getMessage());
        }
    }
}
