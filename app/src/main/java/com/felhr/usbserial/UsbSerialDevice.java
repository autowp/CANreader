package com.felhr.usbserial;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import com.felhr.deviceids.CH34xIds;
import com.felhr.deviceids.CP210xIds;
import com.felhr.deviceids.FTDISioIds;
import com.felhr.deviceids.PL2303Ids;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbRequest;

public abstract class UsbSerialDevice implements UsbSerialInterface
{
    private static final String CLASS_ID = UsbSerialDevice.class.getSimpleName();

    // Get Android version if version < 4.3 It is not going to be asynchronous read operations
    private static final boolean mr1Version = android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
    protected final UsbDevice device;
    protected final UsbDeviceConnection connection;

    protected static final int USB_TIMEOUT = 5000;

    protected SerialBuffer serialBuffer;

    protected WriteThread writeThread;

    protected AbstractReadThread readThread;

    private UsbEndpoint outEndpoint;
    private UsbRequest inRequest;
    private UsbEndpoint inEndpoint;

    public UsbSerialDevice(UsbDevice device, UsbDeviceConnection connection)
    {
        this.device = device;
        this.connection = connection;
        serialBuffer = new SerialBuffer(mr1Version);
    }

    public static UsbSerialDevice createUsbSerialDevice(UsbDevice device, UsbDeviceConnection connection)
    {
        return createUsbSerialDevice(device, connection, -1);
    }

    public static UsbSerialDevice createUsbSerialDevice(UsbDevice device, UsbDeviceConnection connection, int iface)
    {
		/*
		 * It checks given vid and pid and will return a custom driver or a CDC serial driver.
		 * When CDC is returned open() method is even more important, its response will inform about if it can be really
		 * opened as a serial device with a generic CDC serial driver
		 */
        int vid = device.getVendorId();
        int pid = device.getProductId();

        if(FTDISioIds.isDeviceSupported(vid, pid))
            return new FTDISerialDevice(device, connection, iface);
        else if(CP210xIds.isDeviceSupported(vid, pid))
            return new CP2102SerialDevice(device, connection, iface);
        else if(PL2303Ids.isDeviceSupported(vid, pid))
            return new PL2303SerialDevice(device, connection, iface);
        else if(CH34xIds.isDeviceSupported(vid, pid))
            return new CH34xSerialDevice(device, connection, iface);
        else if(isCdcDevice(device))
            return new CDCSerialDevice(device, connection, iface);
        else
            return null;
    }

    // Common Usb Serial Operations (I/O Asynchronous)
    @Override
    public abstract boolean open();

    @Override
    public void write(byte[] buffer)
    {
        serialBuffer.putWriteBuffer(buffer);
    }

    public synchronized void writeNow(byte[] data)
    {
        connection.bulkTransfer(outEndpoint, data, data.length, USB_TIMEOUT);
    }

    public synchronized byte[] readNow(int size, int timeout) {
        if (mr1Version) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(size);
            inRequest.queue(byteBuffer, size);

            UsbRequest request = connection.requestWait();
            if(request != null && request.getEndpoint().getType() == UsbConstants.USB_ENDPOINT_XFER_BULK
                    && request.getEndpoint().getDirection() == UsbConstants.USB_DIR_IN)
            {

                byte[] data = new byte[byteBuffer.position()];
                byteBuffer.position(0);
                byteBuffer.get(data, 0, data.length);
                byteBuffer.clear();

                return processReceivedData(data);
            }

        } else {
            int numberBytes;
            byte[] buffer = new byte[size];
            if (inEndpoint != null) {
                numberBytes = connection.bulkTransfer(inEndpoint, buffer, size, 10000);
                if (numberBytes > 0)
                {
                    byte[] data = Arrays.copyOfRange(buffer, 0, numberBytes);

                    return processReceivedData(data);
                }
            }
        }

        return null;
    }

    @Override
    public int read(UsbReadCallback mCallback)
    {
        readThread.setCallback(mCallback);
        if(mr1Version)
        {
            ((WorkerThread)readThread).getUsbRequest().queue(serialBuffer.getReadBuffer(), SerialBuffer.DEFAULT_READ_BUFFER_SIZE);
        }else
        {
            //readThread.start();
        }
        return 0;
    }


    @Override
    public abstract void close();

    // Serial port configuration
    @Override
    public abstract void setBaudRate(int baudRate);
    @Override
    public abstract void setDataBits(int dataBits);
    @Override
    public abstract void setStopBits(int stopBits);
    @Override
    public abstract void setParity(int parity);
    @Override
    public abstract void setFlowControl(int flowControl);

    //Debug options
    public void debug(boolean value)
    {
        if(serialBuffer != null)
            serialBuffer.debug(value);
    }

    private boolean isFTDIDevice()
    {
        return (this instanceof FTDISerialDevice);
    }

    public static boolean isCdcDevice(UsbDevice device)
    {
        int iIndex = device.getInterfaceCount();
        for(int i=0;i<=iIndex-1;i++)
        {
            UsbInterface iface = device.getInterface(i);
            if(iface.getInterfaceClass() == UsbConstants.USB_CLASS_CDC_DATA)
                return true;
        }
        return false;
    }

    protected byte[] processReceivedData(byte[] data) {
        if(isFTDIDevice()) //TODO: move to FTDISerialDevice
        {
            ((FTDISerialDevice) this).ftdiUtilities.checkModemStatus(data); //Check the Modem status

            if(data.length > 2)
            {
                data = ((FTDISerialDevice) this).ftdiUtilities.adaptArray(data);
            }
            else {
                data = new byte[0];
            }
        }

        return data;
    }

    protected abstract class AbstractReadThread extends Thread {

        protected UsbSerialDevice usbSerialDevice;
        protected UsbReadCallback callback;
        protected AtomicBoolean working;

        public AbstractReadThread(UsbSerialDevice usbSerialDevice)
        {
            this.usbSerialDevice = usbSerialDevice;
            working = new AtomicBoolean(true);
        }

        public void setCallback(UsbReadCallback callback)
        {
            this.callback = callback;
        }

        protected void processReceivedData(byte[] data) {
            data = usbSerialDevice.processReceivedData(data);
            if (data.length > 0) {
                if (callback != null) {
                    callback.onReceivedData(data);
                }
            }
        }

        public void stopThread()
        {
            working.set(false);
        }
    }

    /*
     * WorkerThread waits for request notifications from IN endpoint
     */
    protected class WorkerThread extends AbstractReadThread
    {
        private UsbRequest requestIN;

        public WorkerThread(UsbSerialDevice usbSerialDevice) {
            super(usbSerialDevice);
        }


        @Override
        public void run()
        {
            while(working.get())
            {
                UsbRequest request = connection.requestWait();
                if(request != null && request.getEndpoint().getType() == UsbConstants.USB_ENDPOINT_XFER_BULK
                        && request.getEndpoint().getDirection() == UsbConstants.USB_DIR_IN)
                {
                    byte[] data = serialBuffer.getDataReceived();

                    serialBuffer.clearReadBuffer();

                    processReceivedData(data);

                    // Queue a new request
                    requestIN.queue(serialBuffer.getReadBuffer(), SerialBuffer.DEFAULT_READ_BUFFER_SIZE);
                }
            }
        }

        public void setUsbRequest(UsbRequest request)
        {
            this.requestIN = request;
        }

        public UsbRequest getUsbRequest()
        {
            return requestIN;
        }
    }

    protected class WriteThread extends Thread
    {
        private UsbEndpoint outEndpoint;
        private AtomicBoolean working;

        public WriteThread()
        {
            working = new AtomicBoolean(true);
        }

        @Override
        public void run()
        {
            while(working.get())
            {
                byte[] data = serialBuffer.getWriteBuffer();
                connection.bulkTransfer(outEndpoint, data, data.length, USB_TIMEOUT);
            }
        }

        public void setUsbEndpoint(UsbEndpoint outEndpoint)
        {
            this.outEndpoint = outEndpoint;
        }

        public void stopWriteThread()
        {
            working.set(false);
        }
    }

    protected class ReadThread extends AbstractReadThread
    {
        private UsbEndpoint inEndpoint;

        public ReadThread(UsbSerialDevice usbSerialDevice) {
            super(usbSerialDevice);
        }

        @Override
        public void run()
        {
            while(working.get())
            {
                int numberBytes;
                if(inEndpoint != null)
                    numberBytes = connection.bulkTransfer(inEndpoint, serialBuffer.getBufferCompatible(),
                            SerialBuffer.DEFAULT_READ_BUFFER_SIZE, 0);
                else
                    numberBytes = 0;

                if(numberBytes > 0)
                {
                    byte[] dataReceived = serialBuffer.getDataReceivedCompatible(numberBytes);

                    processReceivedData(dataReceived);
                }
            }
        }

        public void setUsbEndpoint(UsbEndpoint inEndpoint)
        {
            this.inEndpoint = inEndpoint;
        }
    }

    protected void setThreadsParams(UsbRequest request, UsbEndpoint endpoint)
    {
        outEndpoint = endpoint;
        inRequest = request;
        inEndpoint = request.getEndpoint();
        /*if(mr1Version)
        {
            ((WorkerThread)readThread).setUsbRequest(request);
        }else
        {
            ((ReadThread)readThread).setUsbEndpoint(inEndpoint);
        }
        writeThread.setUsbEndpoint(endpoint);*/
    }

    /*
     * Kill workingThread; This must be called when closing a device
     */
    protected void killWorkingThread()
    {
        if(readThread != null)
        {
            readThread.stopThread();
            readThread = null;
        }
    }

    /*
     * Restart workingThread if it has been killed before
     */
    protected void restartWorkingThread()
    {
        /*if (readThread == null) {
            if (mr1Version) {
                readThread = new WorkerThread(this);
            } else {
                readThread = new ReadThread(this);
            }
            readThread.start();
            while (!readThread.isAlive()) {
            } // Busy waiting
        }*/
    }

    protected void killWriteThread()
    {
        if(writeThread != null)
        {
            writeThread.stopWriteThread();
            writeThread = null;
            serialBuffer.resetWriteBuffer();
        }
    }

    protected void restartWriteThread()
    {
        if(writeThread == null)
        {
            writeThread = new WriteThread();
            writeThread.start();
            while(!writeThread.isAlive()){} // Busy waiting
        }
    }
}
