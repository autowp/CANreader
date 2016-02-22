package com.autowp.can.adapter.canhacker;

import com.autowp.Hex;
import com.autowp.can.CanAdapter;
import com.autowp.can.CanAdapterException;
import com.autowp.can.CanClient;
import com.autowp.can.CanFrame;
import com.autowp.can.CanFrameException;
import com.autowp.can.adapter.canhacker.command.BitRateCommand;
import com.autowp.can.adapter.canhacker.command.Command;
import com.autowp.can.adapter.canhacker.command.CommandException;
import com.autowp.can.adapter.canhacker.command.OperationalModeCommand;
import com.autowp.can.adapter.canhacker.command.ResetModeCommand;
import com.autowp.can.adapter.canhacker.command.TransmitCommand;
import com.autowp.can.adapter.canhacker.response.BellResponse;
import com.autowp.can.adapter.canhacker.response.FrameResponse;
import com.autowp.can.adapter.canhacker.response.OkResponse;
import com.autowp.can.adapter.canhacker.response.Response;

import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public abstract class CanHacker extends CanAdapter {

    protected static final int BAUDRATE = 115200;

    final public static char COMMAND_11BIT = 't';
    final public static char COMMAND_11BIT_RTR = 'r';
    final public static char COMMAND_29BIT = 'T';
    final public static char COMMAND_29BIT_RTR = 'R';
    
    public static final char COMMAND_DELIMITER = '\r';
    
    protected static final char BELL = (char)0x07;
    private static final int DEFAULT_TIMEOUT = 5000;
    private static final int INITIAL_RESET_RETRY_INTERVAL = 250;
    private static final int INITIAL_RESET_TIMEOUT = 15000;

    private byte[] buffer = new byte[1024];
    private int bufferPos = 0;

    private boolean mCollectReponses = true;

    private final BlockingQueue<Response> responses = new LinkedBlockingQueue<>();

    private class ReceiveRunnable implements Runnable {

        @Override
        public void run() {
            while(!Thread.interrupted()) {
                byte[] data = readBytes(DEFAULT_TIMEOUT);
                if (data != null) {
                    processBytes(data);
                }
            }
        }
    }

    private Thread mReceiveThread;

    /**
     if (command instanceof TransmitCommand) {
     TransmitCommand transmitCommand = (TransmitCommand)command;

     fireFrameSentEvent(transmitCommand.getFrame());
     }
     */

    private void clearResponses() {
        synchronized (responses) {
            bufferPos = 0;
            responses.clear();
        }
    }

    @Override
    public void send(final CanFrame frame) throws CanHackerException
    {
        try {
            TransmitCommand command = new TransmitCommand(frame);
            //sendAndWaitOk(command, 500);
            send(command);
        } catch (CommandException e) {
            throw new CanHackerException("Command error: " + e.getMessage());
        }
    }
    
    public abstract CanHacker send(final Command c) throws CanHackerException;

    /*public Response receive(int timeout) {


        System.out.println(System.currentTimeMillis());
        long endTime = System.currentTimeMillis() + timeout;
        Response response = null;
        while(response == null) {
            if ((timeout > 0) && (System.currentTimeMillis() >= endTime)) {
                System.out.println("timeout");
                System.out.println(endTime);
                System.out.println(System.currentTimeMillis());
                break;
            }
            System.out.println(String.format("not timeout: %d of %d", System.currentTimeMillis(), endTime));
            byte[] data = readBytes(timeout);
            if (data != null) {
                processBytes(data);
                response = responses.poll();
            }
        }

        return response;
    }*/

    protected abstract byte[] readBytes(final int timeout);

    protected void processBytes(final byte[] bytes) {
        for (byte aByte : bytes) {

            if (bufferPos >= buffer.length) {
                String str = new String(buffer);
                System.out.println("Error: buffer overflow");
                System.out.println(str);
                bufferPos = 0;
            }

            switch (aByte) {
                case BELL:
                    bufferPos = 0;
                    if (mCollectReponses) {
                        responses.add(new BellResponse());
                    }
                    break;
                case COMMAND_DELIMITER:
                    if (bufferPos > 0) {
                        byte[] commandBytes = new byte[bufferPos];
                        System.arraycopy(buffer, 0, commandBytes, 0, bufferPos);
                        bufferPos = 0;
                        try {
                            Response response = Response.fromBytes(commandBytes);
                            if (response instanceof FrameResponse) {
                                fireFrameReceivedEvent(((FrameResponse)response).getFrame());
                            } else {
                                if (mCollectReponses) {
                                    responses.add(response);
                                }
                            }

                        } catch (CanFrameException e) {
                            fireErrorEvent(new CanHackerException("CanFrame error: " + e.getMessage()));
                        } catch (CanHackerException e) {
                            fireErrorEvent(new CanHackerException("CanHacker error: " + e.getMessage()));
                        }
                    } else {
                        bufferPos = 0;
                        if (mCollectReponses) {
                            responses.add(new OkResponse());
                        }
                    }

                    break;
                default:
                    buffer[bufferPos++] = aByte;
                    break;
            }
        }
    }

    public static CanFrame parseFrame(final byte[] bytes) throws CanHackerException, CanFrameException {
        if (bytes.length < 5) {
            throw new CanHackerException("Frame response must be >= 5 bytes long");
        }

        boolean isExtended;
        boolean isRTR;

        switch (bytes[0]) {
            case COMMAND_11BIT:
                isExtended = false;
                isRTR = false;
                break;
            case COMMAND_11BIT_RTR:
                isExtended = false;
                isRTR = true;
                break;
            case COMMAND_29BIT:
                isExtended = true;
                isRTR = false;
                break;
            case COMMAND_29BIT_RTR:
                isExtended = true;
                isRTR = true;
                break;
            default:
                throw new CanHackerException("Frame response starts with unexpected character");
        }

        int idLength = isExtended ? 8 : 3;
        int id = Hex.bytesToInt(Arrays.copyOfRange(bytes, 1, 1+idLength));

        int maxLength = 1 + idLength + 1 + 2 * CanFrame.MAX_DLC + FrameResponse.TIMESTAMP_LENGTH_CHARS;
        if (bytes.length > maxLength) {
            String hex = Hex.byteArrayToHexString(bytes);
            throw new CanHackerException(
                    String.format("Frame response must be <= %d bytes long. `%s`", maxLength, hex)
            );
        }

        byte dlc = (byte) Character.digit(bytes[1+idLength], 16);

        byte[] data = null;
        int dlcStart = 1 + idLength + 1;
        if (!isRTR) {
            if (bytes.length < dlcStart + dlc * 2) {
                throw new CanHackerException(
                    String.format("Frame response data length smaller than data length byte value (%d) %s", dlc, new String(bytes))
                );
            }

            try {
                data = Hex.hexStringToByteArray(Arrays.copyOfRange(bytes, dlcStart, dlcStart + dlc * 2));
            } catch (Exception e) {
                throw new CanHackerException("Failed to parse frame data: " + e.getMessage());
            }

            if (data.length != dlc) {
                throw new CanHackerException("Actual data length differ than DLC");
            }
        }

        int tsOffset = dlcStart + dlc * 2;

        if (bytes.length == tsOffset + FrameResponse.TIMESTAMP_LENGTH_CHARS) {
            int timestamp = Hex.bytesToInt(
                    Arrays.copyOfRange(bytes, tsOffset, tsOffset + FrameResponse.TIMESTAMP_LENGTH_CHARS)
            );
        }
        //TODO: save timestamp

        if (isRTR) {
            return new CanFrame(id, dlc, isExtended);
        } else {
            return new CanFrame(id, data, isExtended);
        }

    }

    public static byte[] assembleTransmit(final CanFrame frame) {
        return assembleTransmitString(frame).getBytes();
    }

    public static String assembleTransmitString(final CanFrame frame) {
        String format;
        String name;
        if (frame.isExtended()) {
            format = "%s%08X%1X";
            if (frame.isRTR()) {
                name = "R";
            } else {
                name = "T";
            }
        } else {
            format = "%s%03X%1X";
            if (frame.isRTR()) {
                name = "r";
            } else {
                name = "t";
            }
        }

        String result = String.format(
                format,
                name,
                frame.getId(),
                frame.getDLC()
        );

        if (!frame.isRTR()) {
            result += Hex.byteArrayToHexString(frame.getData());
        }

        return result;
    }

    private Response sendAndWaitResponse(final Command command, int timeout) throws CanHackerException {
        send(command);

        if (timeout <= 0) {
            timeout = DEFAULT_TIMEOUT;
        }

        Response response = null;

        try {
            response = responses.poll(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return response;
    }

    private void sendAndWaitOk(final Command command, final int timeout) throws CanHackerException {
        Response response = sendAndWaitResponse(command, timeout);
        if (response == null) {
            throw new CanHackerException("Response timeout");
        }
        if (!(response instanceof OkResponse)) {
            throw new CanHackerException(
                    String.format("Not proper response `%s` -> `%s`", command.toString(), response.toString())
            );
        }
    }

    protected synchronized void doConnect() throws CanHackerException {

        mCollectReponses = true;

        if (mReceiveThread == null) {
            mReceiveThread = new Thread(new ReceiveRunnable());
            mReceiveThread.start();
        }

        BitRateCommand.BitRate busSpeed;
        switch (specs.getSpeed()) {
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
                throw new CanHackerException("Unsupported bus speed");

        }

        // try to reset about 15 seconds for long device bootup like arduino
        Response response = null;
        for (int i=0; i<INITIAL_RESET_TIMEOUT / INITIAL_RESET_RETRY_INTERVAL; i++) {
            clearResponses();
            response = sendAndWaitResponse(new ResetModeCommand(), INITIAL_RESET_RETRY_INTERVAL);
            if (response != null) {
                break;
            }
        }

        if (response == null) {
            throw new CanHackerException("C response timeout");
        }

        if (!(response instanceof OkResponse) && !(response instanceof BellResponse)) {
            throw new CanHackerException(
                    String.format("Not proper response for C `%s`", response.toString())
            );
        }

        clearResponses();
        sendAndWaitOk(new BitRateCommand(busSpeed), 3000);
        clearResponses();
        sendAndWaitOk(new OperationalModeCommand(), 3000);
        clearResponses();

        mCollectReponses = false;
    }

    public void connect(final Runnable callback) throws CanAdapterException
    {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    connectionState = CanClient.ConnectionState.CONNECTING;
                    fireConnectionChangedEvent();

                    doConnect();

                    connectionState = CanClient.ConnectionState.CONNECTED;
                    fireConnectionChangedEvent();

                    if (callback != null) {
                        callback.run();
                    }

                } catch (CanHackerException e) {

                    connectionState = CanClient.ConnectionState.DISCONNECTED;
                    fireConnectionChangedEvent();

                    e.printStackTrace();
                }
            }
        });
        t.start();
    }

    protected void doDisconnect() throws CanAdapterException {

        mCollectReponses = true;

        Response response = sendAndWaitResponse(new ResetModeCommand(), 3000);
        if (response == null) {
            throw new CanHackerException("C response timeout");
        }

        if (!(response instanceof OkResponse) && !(response instanceof BellResponse)) {
            throw new CanHackerException(
                    String.format("Not proper response for C `%s`", response.toString())
            );
        }

        if (mReceiveThread != null) {
            mReceiveThread.interrupt();
            mReceiveThread = null;
        }

        mCollectReponses = false;
    }

    public void disconnect(final Runnable callback) throws CanAdapterException
    {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    connectionState = CanClient.ConnectionState.DISCONNECTING;
                    fireConnectionChangedEvent();

                    doDisconnect();

                    connectionState = CanClient.ConnectionState.DISCONNECTED;
                    fireConnectionChangedEvent();

                    if (callback != null) {
                        callback.run();
                    }

                } catch (CanAdapterException e) {

                    connectionState = CanClient.ConnectionState.DISCONNECTED;
                    fireConnectionChangedEvent();

                    e.printStackTrace();
                }
            }
        });
        t.start();
    }
}
