package com.autowp.can.adapter.android;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import com.autowp.can.CanAdapterException;
import com.autowp.can.CanFrameException;
import com.autowp.can.adapter.canhacker.CanHacker;
import com.autowp.can.adapter.canhacker.command.BitRateCommand;
import com.autowp.can.adapter.canhacker.command.Command;
import com.autowp.can.adapter.canhacker.command.CommandException;
import com.autowp.can.adapter.canhacker.command.OperationalModeCommand;
import com.autowp.can.adapter.canhacker.command.ResetModeCommand;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.IOException;

/**
 * Created by autow on 12.02.2016.
 */
public class CanHackerFelhrUsb extends CanHacker {
    private UsbManager mUsbManager;
    private UsbDevice mUsbDevice;
    private UsbSerialDevice mSerial;

    private UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() {
        @Override
        public void onReceivedData(byte[] bytes)
        {
            processBytes(bytes);
        }
    };

    public CanHackerFelhrUsb(UsbManager usbManager, UsbDevice usbDevice) throws CanHackerUsbException {
        super();

        mUsbManager = usbManager;
        mUsbDevice = usbDevice;
    }

    @Override
    public synchronized void connect() throws CanAdapterException {

        UsbDeviceConnection connection = mUsbManager.openDevice(mUsbDevice);
        if (connection == null) {
            throw new CanHackerUsbException("Opening device failed");
        }

        mSerial = UsbSerialDevice.createUsbSerialDevice(mUsbDevice, connection);


        if (mSerial == null) {
            throw new CanHackerUsbException("Driver not found");
        }

        mSerial.open();
        mSerial.setBaudRate(BAUDRATE);
        mSerial.setDataBits(UsbSerialInterface.DATA_BITS_8);
        mSerial.setParity(UsbSerialInterface.PARITY_NONE);

        mSerial.read(mCallback);

        BitRateCommand.BitRate busSpeed;
        switch (this.specs.getSpeed()) {
            case 10:   busSpeed = BitRateCommand.BitRate.S0; break;
            case 20:   busSpeed = BitRateCommand.BitRate.S1; break;
            case 50:   busSpeed = BitRateCommand.BitRate.S2; break;
            case 100:  busSpeed = BitRateCommand.BitRate.S3; break;
            case 125:  busSpeed = BitRateCommand.BitRate.S4; break;
            case 250:  busSpeed = BitRateCommand.BitRate.S5; break;
            case 500:  busSpeed = BitRateCommand.BitRate.S6; break;
            case 800:  busSpeed = BitRateCommand.BitRate.S7; break;
            case 1000: busSpeed = BitRateCommand.BitRate.S8; break;
            default:
                throw new CanHackerUsbException("Unsupported bus speed");
        }

        try {
            Thread.sleep(1000); //TODO: do it asychronious
        } catch (InterruptedException e) {
            throw new CanHackerUsbException(e.getMessage());
        }
        this.send(new ResetModeCommand());
        try {
            this.send(new BitRateCommand(busSpeed));
        } catch (CommandException e) {
            throw new CanHackerUsbException(e.getMessage());
        }
        this.send(new OperationalModeCommand());
    }

    @Override
    public synchronized void disconnect() {
        if (mSerial != null) {
            System.out.println("canhackerusb disconnect");
            mSerial.close();
            mSerial = null;
        }

    }

    @Override
    public boolean isConnected() {
        return mSerial != null;
    }

    public synchronized CanHackerFelhrUsb send(Command c) throws CanHackerUsbException
    {
        if (!this.isConnected()) {
            throw new CanHackerUsbException("CanHacker is not connected");
        }

        String command = c.toString() + COMMAND_DELIMITER;

        try {

            byte[] data = command.getBytes("ISO-8859-1");

            mSerial.writeNow(data);

            fireCommandSendEvent(c);

        } catch (IOException e) {
            throw new CanHackerUsbException("I/O error: " + e.getMessage());
        } catch (CanFrameException e) {
            throw new CanHackerUsbException("Can frame error: " + e.getMessage());
        }

        return this;
    }
}
