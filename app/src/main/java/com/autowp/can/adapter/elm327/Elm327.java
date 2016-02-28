package com.autowp.can.adapter.elm327;

import com.autowp.can.CanAdapter;
import com.autowp.can.CanAdapterException;
import com.autowp.can.CanClient;
import com.autowp.can.CanFrame;

import com.autowp.can.adapter.elm327.command.Command;
import com.autowp.can.adapter.elm327.command.DefaultsCommand;
import com.autowp.can.adapter.elm327.command.EchoCommand;
import com.autowp.can.adapter.elm327.command.MonitorAllCommand;
import com.autowp.can.adapter.elm327.command.ResetCommand;
import com.autowp.can.adapter.elm327.command.SetHeaderCommand;
import com.autowp.can.adapter.elm327.command.TransmitCommand;
import com.autowp.can.adapter.elm327.response.Response;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


public abstract class Elm327 extends CanAdapter {

    protected static final int BAUDRATE = 38400;

    private static final int DEFAULT_TIMEOUT = 5000;

    private static final char COMMAND_DELIMITER = '\r';
    
    public final byte PP_CAN_ERROR_CHECKING = 0x2A;
    public final byte PP_PROTOCOL_B_CAN_OPTIONS = 0x2C;
    public final byte PP_PROTOCOL_B_BAUDRATE_DIVISOR = 0x2D;
    
    public final byte PP_CAN_OPTIONS_ID_LENGTH_29 = 0x00;
    public final byte PP_CAN_OPTIONS_ID_LENGTH_11 = (byte) 0x80;
    
    public final byte PP_CAN_OPTIONS_DATE_LENGTH_8 = 0x00;
    public final byte PP_CAN_OPTIONS_DATE_LENGTH_VARIABLE = 0x40;
    
    public final byte PP_CAN_OPTIONS_RCV_ID_LENGTH_DEFAULT = 0x00;
    public final byte PP_CAN_OPTIONS_RCV_ID_LENGTH_BOTH = 0x20;
    
    public final byte PP_CAN_OPTIONS_BAUDRATE_MULTIPLIER_NONE = 0x00;
    public final byte PP_CAN_OPTIONS_BAUDRATE_MULTIPLIER_8_7 = 0x10;
    
    public final byte PP_CAN_OPTIONS_DATA_FOMATTING_NONE = 0x00;
    public final byte PP_CAN_OPTIONS_DATA_FOMATTING_ISO15765_4 = 0x01;
    public final byte PP_CAN_OPTIONS_DATA_FOMATTING_SAE_J1939 = 0x02;

    private final BlockingQueue<Response> responses = new LinkedBlockingQueue<>();

    private byte[] buffer = new byte[1024];
    private int bufferPos = 0;

    private boolean mCollectReponses = true;

    private Thread mReceiveThread;
    
    @Override
    public void send(CanFrame message) throws Elm327Exception {
        int id = message.getId();
        byte b0 = (byte)(id & 0x000F);
        byte b1 = (byte)((id & 0x00F0) >> 4);
        byte b2 = (byte)((id & 0x0F00) >> 8);

        this.send(new SetHeaderCommand(new byte[] {b0, b1, b2}));
        this.send(new TransmitCommand(message.getData()));
        
        this.fireFrameSentEvent(message);
    }

    /*public synchronized Elm327 send(final Command c) throws Elm327Exception
    {

        return this.send(c);
    }*/
    
    public synchronized Elm327 send(Command c) throws Elm327Exception
    {
        if (!this.isConnected()) {
            throw new Elm327Exception("ELM327 is not connected");
        }
        
        String command = c.toString() + "\n\r";
        
        System.out.println("Command: " + command);
        
        /*try {
            this.serialPort.writeString(command);

        } catch (CanFrameException e) {
            throw new Elm327Exception("Can frame error: " + e.getMessage());
        }*/
                
        return this;
    }

    private Response sendAndWaitResponse(final Command command, int timeout) throws Elm327Exception {
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

    protected synchronized void doConnect() throws Elm327Exception {
        this.send(new ResetCommand());
        this.send(new DefaultsCommand());
        this.send(new EchoCommand(false));
        this.send(new MonitorAllCommand());
    }

    @Override
    public void connect(final Runnable callback) throws Elm327Exception {
        if (this.isConnected()) {
            return;
        }

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

                } catch (Elm327Exception e) {

                    connectionState = CanClient.ConnectionState.DISCONNECTED;
                    fireConnectionChangedEvent();

                    e.printStackTrace();
                }
            }
        });
        t.start();

        /*

        serialPort.addEventListener(new SerialReader(), SerialPort.MASK_RXCHAR);*/


            /*byte divisor = 0; // baudrate = 500 / divisor
            switch (this.specs.getSpeed()) {
                case 20:   divisor = 25; break;
                case 25:   divisor = 20; break;
                case 50:   divisor = 10; break;
                case 100:  divisor = 5; break;
                case 125:  divisor = 4; break;
                case 250:  divisor = 2; break;
                case 500:  divisor = 1; break;
                default:
                    throw new Elm327Exception("Unsupported bus speed");
            }*/

        //
        //this.send(new CanSilentMonitorCommand(false));
            /*this.send(new ResetCommand(), new WaitForResponse() {
                @Override
                public boolean match(Response response) {
                    System.out.println("Matching: " + response.toString());
                    System.out.println(response.toString().equals("ELM327 v1.5"));
                    System.out.println(response.toString().length());
                    System.out.println("ELM327 v1.5".length());
                    return response.toString().equals("ELM327 v1.5");
                }

                @Override
                public void execute(Response response) throws Elm327Exception {
                    System.out.println("execute");
                    send(new DefaultsCommand());
                }
            });

            /*
            this.send(new ProgParameterSetCommand(PP_CAN_ERROR_CHECKING, (byte)0x38));
            this.send(new ProgParameterOnCommand(PP_CAN_ERROR_CHECKING));*/

            /*byte options = PP_CAN_OPTIONS_ID_LENGTH_11
                         | PP_CAN_OPTIONS_DATE_LENGTH_VARIABLE
                         | PP_CAN_OPTIONS_RCV_ID_LENGTH_DEFAULT
                         | PP_CAN_OPTIONS_BAUDRATE_MULTIPLIER_NONE
                         | PP_CAN_OPTIONS_DATA_FOMATTING_ISO15765_4;
            this.send(new ProgParameterSetCommand(PP_PROTOCOL_B_CAN_OPTIONS, options));
            this.send(new ProgParameterOnCommand(PP_PROTOCOL_B_CAN_OPTIONS));

            this.send(new ProgParameterSetCommand(PP_PROTOCOL_B_BAUDRATE_DIVISOR, divisor));
            this.send(new ProgParameterOnCommand(PP_PROTOCOL_B_BAUDRATE_DIVISOR));*/

            /*this.send(new SetProtocolCommand(SetProtocolCommand.USER1_CAN));

            */

    }

    protected void doDisconnect() throws Elm327Exception {

        mCollectReponses = true;

        Response response = sendAndWaitResponse(new ResetCommand(), 3000);
        if (response == null) {
            throw new Elm327Exception("C response timeout");
        }

        if (mReceiveThread != null) {
            mReceiveThread.interrupt();
            mReceiveThread = null;
        }

        mCollectReponses = false;
    }

    @Override
    public void disconnect(final Runnable callback) throws CanAdapterException {
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

    protected abstract byte[] readBytes(final int timeout);

    /**
     * Handles the input coming from the serial port. A new line character
     * is treated as the end of a block in this example. 
     */
    /*private class SerialReader implements SerialPortEventListener
    {
        private byte[] buffer = new byte[1024];
        private int bufferPos = 0;
        
        @Override
        public void serialEvent(SerialPortEvent event) {
            System.out.println("event");
            if (event.isRXCHAR() && event.getEventValue() > 0) {
                try {
                    byte[] bytes = serialPort.readBytes();
                    System.out.println(new String(bytes));
                    for (int i=0; i<bytes.length; i++) {
                        byte data = bytes[i];
                        char dataChar = (char)data;
                        if (dataChar != COMMAND_DELIMITER) {
                            buffer[bufferPos++] = data;
                        }
                        
                        if (dataChar == COMMAND_DELIMITER) {
                            if (bufferPos > 0) {
                                byte[] commandBytes = new byte[bufferPos];
                                System.arraycopy(buffer, 0, commandBytes, 0, bufferPos);
                                Response response = Response.fromBytes(commandBytes);
                                fireResponseReceivedEvent(response);
                            }
                            bufferPos = 0;
                        }
                        
                        byte[] commandBytes = new byte[bufferPos];
                        System.arraycopy(buffer, 0, commandBytes, 0, bufferPos);
                    }
                } catch (SerialPortException | ResponseException | CanFrameException | Elm327Exception e) {
                    e.printStackTrace();
                    System.exit(-1);
                }
            }
        }
    }*/
}
