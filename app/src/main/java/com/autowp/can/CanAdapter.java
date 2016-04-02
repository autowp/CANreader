package com.autowp.can;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public abstract class CanAdapter {

    public enum ConnectionState { DISCONNECTED, CONNECTING, CONNECTED, DISCONNECTING }

    public interface CanAdapterEventListener {
        void handleErrorEvent(CanAdapterException e);
        void handleConnectionStateChanged(ConnectionState connection);
    }

    public interface OnCanMessageTransferListener {
        void handleCanMessageReceivedEvent(CanMessage message);
        void handleCanMessageSentEvent(CanMessage message);
    }

    public interface OnCanFrameTransferListener {
        void handleCanFrameReceivedEvent(CanFrame frame);
        void handleCanFrameSentEvent(CanFrame frame);
    }

    static final int PCITYPE_SINGLE_FRAME = 0;
    static final int PCITYPE_FIRST_FRAME = 1;
    static final int PCITYPE_CONSECUTIVE_FRAME = 2;
    static final int PCITYPE_FLOW_CONTROL_FRAME = 3;

    protected ConnectionState connectionState = ConnectionState.DISCONNECTED;

    HashMap<Integer, MultiFrameBuffer> mMultiframeBuffers = new HashMap<>();



    private List<OnCanFrameTransferListener> mCanFrameTransferListeners =
            new ArrayList<>();

    private List<CanAdapter.OnCanMessageTransferListener> mCanMessageTransferListeners =
            new ArrayList<>();

    private List<CanAdapterEventListener> mCanAdapterEventListeners = new ArrayList<>();

    protected CanBusSpecs specs;

    public CanAdapter(CanBusSpecs specs)
    {
        this.specs = specs;
    }

    protected abstract void doSend(CanFrame frame) throws CanAdapterException;

    public final void send(CanFrame frame) throws CanAdapterException
    {
        if (connectionState == CanAdapter.ConnectionState.DISCONNECTED) {
            throw new CanAdapterException("CanHacker is disconnected");
        }

        doSend(frame);

        fireCanFrameSentEvent(frame);
    }

    protected abstract void doConnect() throws CanAdapterException;
    
    public final void connect(final Runnable callback) throws CanAdapterException
    {
        if (connectionState != ConnectionState.DISCONNECTED) {
            throw new CanAdapterException("Attepmt to connect when not disconnected");
        }
        System.out.println("zconnect");

        connectionState = ConnectionState.CONNECTING;
        fireConnectionStateChangedEvent();

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    System.out.println("doConnect");
                    doConnect();

                    connectionState = ConnectionState.CONNECTED;
                    fireConnectionStateChangedEvent();
                    System.out.println("zcallback");
                    if (callback != null) {
                        callback.run();
                    }

                } catch (CanAdapterException e) {
                    fireErrorEvent(e);
                    connectionState = ConnectionState.DISCONNECTED;
                    fireConnectionStateChangedEvent();
                    System.err.println("Adapter error: " + e.getMessage());
                }
            }
        });
        t.start();
    }

    protected abstract void doDisconnect() throws CanAdapterException;
    
    public final void disconnect(final Runnable callback) throws CanAdapterException
    {
        System.out.println("zdisconnect");
        switch(connectionState) {
            case CONNECTED:
                connectionState = ConnectionState.DISCONNECTING;
                fireConnectionStateChangedEvent();

                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {

                        try {
                            System.out.println("zdoDisconnect");
                            doDisconnect();
                        } catch (CanAdapterException e) {
                            fireErrorEvent(e);
                            System.err.println("Adapter error: " + e.getMessage());
                        }

                        connectionState = ConnectionState.DISCONNECTED;
                        fireConnectionStateChangedEvent();

                        System.out.println("zDISCONNECTED");

                        if (callback != null) {
                            callback.run();
                        }

                    }
                });
                t.start();
                break;
            case DISCONNECTED:
                if (callback != null) {
                    callback.run();
                }
                break;
            case DISCONNECTING:
            case CONNECTING:
                throw new CanAdapterException("Attempt to disconnect while connecting/disconnecting");
        }
    }

    public boolean isConnected() {
        return connectionState == ConnectionState.CONNECTED;
    }

    private class MultiFrameBuffer {
        int currentLength;
        int lastCounter;
        byte[] buffer;

        public MultiFrameBuffer(int expectedLength)
        {
            this.currentLength = 0;
            this.buffer = new byte[expectedLength];
            this.lastCounter = -1; // initial value to match first 0
        }

        public void append(byte[] data, int cycleCounter) throws CanAdapterException
        {
            if (currentLength + data.length > buffer.length) {
                throw new CanAdapterException("Buffer overflow detected");
            }

            if (cycleCounter != (lastCounter + 1) % 16) {
                throw new CanAdapterException("Cycle counter breaks from " + lastCounter + " to " + cycleCounter);
            }

            System.arraycopy(data, 0, buffer, currentLength, data.length);

            currentLength += data.length;

            lastCounter = cycleCounter;
        }

        public boolean isComplete()
        {
            return buffer.length == currentLength;
        }

        public byte[] getData()
        {
            return buffer;
        }
    }



    public CanAdapter.ConnectionState getConnectionState()
    {
        return connectionState;
    }

    public synchronized void addEventListener(OnCanFrameTransferListener listener) {
        mCanFrameTransferListeners.add(listener);
    }

    public synchronized void removeEventListener(OnCanFrameTransferListener listener){
        mCanFrameTransferListeners.remove(listener);
    }

    private synchronized void fireCanFrameSentEvent(CanFrame frame)
    {
        for (OnCanFrameTransferListener listener : mCanFrameTransferListeners) {
            listener.handleCanFrameSentEvent(frame);
        }
    }

    private synchronized void fireCanFrameReceivedEvent(CanFrame frame)
    {
        for (OnCanFrameTransferListener listener : mCanFrameTransferListeners) {
            listener.handleCanFrameReceivedEvent(frame);
        }
    }

    public synchronized void addEventListener(OnCanMessageTransferListener listener) {
        mCanMessageTransferListeners.add(listener);
    }

    public synchronized void removeEventListener(OnCanMessageTransferListener listener){
        mCanMessageTransferListeners.remove(listener);
    }

    private synchronized void fireCanMessageSentEvent(CanMessage message)
    {
        for (OnCanMessageTransferListener listener : mCanMessageTransferListeners) {
            listener.handleCanMessageSentEvent(message);
        }
    }

    private synchronized void fireCanMessageReceivedEvent(CanMessage message)
    {
        for (OnCanMessageTransferListener listener : mCanMessageTransferListeners) {
            listener.handleCanMessageReceivedEvent(message);
        }
    }

    protected synchronized void fireErrorEvent(CanAdapterException e)
    {
        for (CanAdapterEventListener listener : mCanAdapterEventListeners) {
            listener.handleErrorEvent(e);
        }
    }

    public synchronized void addEventListener(CanAdapterEventListener listener)
    {
        mCanAdapterEventListeners.add(listener);
    }
    
    public synchronized void removeEventListener(CanAdapterEventListener listener)
    {
        mCanAdapterEventListeners.remove(listener);
    }

    private synchronized void fireConnectionStateChangedEvent()
    {
        System.out.print("fireConnectionStateChangedEvent ");
        System.out.println(connectionState);
        for (CanAdapterEventListener listener : mCanAdapterEventListeners) {
            listener.handleConnectionStateChanged(connectionState);
        }
    }
    
    public void setBusSpecs(CanBusSpecs specs)
    {
        this.specs = specs;
    }

    public void receive(CanFrame frame) {
        try {
            int arbID = frame.getId();
            // check is multiFrame
            if (specs.isMultiFrame(arbID)) {

                byte[] data = frame.getData();
                if (data.length <= 0) {
                    throw new CanAdapterException("Unexpected zero size can message");
                }

                int pciType = (data[0] & 0xF0) >>> 4;

                switch (pciType) {
                    case CanAdapter.PCITYPE_SINGLE_FRAME: {
                        int dataLength = data[0] & 0x0F;
                        byte[] messageData = new byte[dataLength];
                        System.arraycopy(data, 1, messageData, 0, dataLength);
                        fireCanMessageReceivedEvent(
                                new CanMessage(arbID, messageData, frame.isExtended())
                        );
                        break;
                    }

                    case CanAdapter.PCITYPE_FIRST_FRAME: {
                        int dataLengthHigh = data[0] & 0x0F;
                        int dataLengthLow = (int) data[1] & 0xFF;

                        int dataLength = (dataLengthHigh << 8) + dataLengthLow;

                        byte[] messageData = new byte[data.length - 2];
                        System.arraycopy(data, 2, messageData, 0, data.length - 2);

                        MultiFrameBuffer buffer = new MultiFrameBuffer(dataLength);
                        buffer.append(messageData, 0);

                        mMultiframeBuffers.put(arbID, buffer);
                        break;
                    }

                    case CanAdapter.PCITYPE_CONSECUTIVE_FRAME: {

                        int index = data[0] & 0x0F;
                        byte[] messageData = new byte[data.length - 1];
                        System.arraycopy(data, 1, messageData, 0, data.length - 1);

                        MultiFrameBuffer buffer = mMultiframeBuffers.get(arbID);
                        if (buffer == null) {
                            throw new CanAdapterException("Buffer for " + arbID + " not found");
                        }

                        buffer.append(messageData, index);

                        if (buffer.isComplete()) {
                            fireCanMessageReceivedEvent(
                                    new CanMessage(arbID, buffer.getData(), frame.isExtended())
                            );
                            mMultiframeBuffers.remove(arbID);
                        }

                        break;
                    }

                    case CanAdapter.PCITYPE_FLOW_CONTROL_FRAME:
                        // TODO:
                        break;

                    default:
                        throw new CanAdapterException("Unexpected PCITYPE " + pciType);
                }

            } else {
                fireCanMessageReceivedEvent(
                        new CanMessage(frame)
                );
            }
        } catch (CanAdapterException ex) {
            fireErrorEvent(ex);
            ex.printStackTrace();
        }

        fireCanFrameReceivedEvent(frame);
    }
}
