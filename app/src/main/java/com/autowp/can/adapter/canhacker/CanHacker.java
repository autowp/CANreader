package com.autowp.can.adapter.canhacker;

import com.autowp.Hex;
import com.autowp.can.CanAdapter;
import com.autowp.can.CanFrame;
import com.autowp.can.CanFrameException;
import com.autowp.can.adapter.canhacker.command.Command;
import com.autowp.can.adapter.canhacker.command.CommandException;
import com.autowp.can.adapter.canhacker.command.TransmitCommand;
import com.autowp.can.adapter.canhacker.response.FrameResponse;
import com.autowp.can.adapter.canhacker.response.Response;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class CanHacker extends CanAdapter {

    protected static final int BAUDRATE = 115200;
    
    /**
     * Gxx
     * Read register content from SJA1000 controller.
     * xx = Register to read (00-7F)
     * Return: Gdd[CR] 
     */
    public static final String G = "G";
    
    /**
     * Mxxxxxxxx[CR]
     * 
     * Set acceptance code register of SJA1000. This command works only if controller is setup with command  and in reset mode.
     * 
     * xxxxxxxx = Acceptance Code in hexadecimal, order ACR0 ACR1 ACR2 ACR3
     * Default value after power-up is 0x00000000 to receive all frames.
     * 
     * Return: [CR] or [BEL] 
     */
    public static final String M = "M";
    
    /**
     * mxxxxxxxx[CR]
     * 
     * Set acceptance mask register of SJA1000. This command works only if 
     * controller is setup with command  and in reset mode.
     * 
     * xxxxxxxx = Acceptance Mask in hexadecimal, order AMR0 AMR1 AMR2 AMR3
     * 
     * Default value after power-up is 0xFFFFFFFF to receive all frames.
     * 
     * Return [CR] or [BEL]
     * 
     * The acceptance filter is defined by the Acceptance Code Registers ACRn 
     * and the Acceptance Mask Registers AMRn. The bit patterns of messages 
     * to be received are defined within the acceptance code registers. 
     * The corresponding acceptance mask registers allow to define certain 
     * bit positions to be dont care.
     * 
     * This device uses dual filter configuration.
     * For details of ACR and AMR usage see the SJA1000 datasheet. 
     */
    public static final String m = "m";
    
    /**
     * sxxyy[CR]
     * 
     * This command will set user defined values for the SJA1000 bit rate register BTR0 and BTR1.
     * It works only after power up or if controller is in reset mode after command C.
     * 
     * xx = hexadecimal value for BTR0 (00-FF)
     * yy = hexadecimal value for BTR1 (00-FF)
     * 
     * Return: [CR] or [BEL] 
     */
    public static final String s = "s";
    
    /**
     * Wrrdd[CR]
     * 
     * Write SJA1000 register with data.
     * The data will be written to specified register without any check!
     * 
     * rr = Register number (00-7F)
     * dd = Data byte (00-FF)
     * 
     * Return: [CR] 
     */
    public static final String W = "W";

    final public static char COMMAND_11BIT = 't';
    final public static char COMMAND_11BIT_RTR = 'r';
    final public static char COMMAND_29BIT = 'T';
    final public static char COMMAND_29BIT_RTR = 'R';
    
    public static final char COMMAND_DELIMITER = '\r';
    
    protected static final char BELL = (char)0x07;

    private byte[] buffer = new byte[1024];
    private int bufferPos = 0;
    
    public interface OnCommandSentListener {
        void handleCommandSentEvent(Command command);
    }
    
    public interface OnResponseReceivedListener {
        void handleResponseReceivedEvent(Response response);
    }
    
    private List<OnCommandSentListener> mCommandSendListeners = new ArrayList<>();
    
    private List<OnResponseReceivedListener> mResponseReceivedListeners = new ArrayList<>();
    
    public synchronized void addEventListener(OnCommandSentListener listener)
    {
        mCommandSendListeners.add(listener);
    }
    
    public synchronized void removeEventListener(OnCommandSentListener listener)
    {
        mCommandSendListeners.remove(listener);
    }
    
    protected synchronized void fireCommandSendEvent(Command command) throws CanFrameException
    {
        for (OnCommandSentListener mCommandSendListener : mCommandSendListeners) {
            mCommandSendListener.handleCommandSentEvent(command);
        }
        
        if (command instanceof TransmitCommand) {
            TransmitCommand transmitCommand = (TransmitCommand)command;
            
            fireFrameSentEvent(transmitCommand.getFrame());
        }
    }
    
    public synchronized void addEventListener(OnResponseReceivedListener listener) 
    {
        mResponseReceivedListeners.add(listener);
    }
    
    public synchronized void removeEventListener(OnResponseReceivedListener listener)   
    {
        mResponseReceivedListeners.remove(listener);
    }
    
    protected synchronized void fireResponseReceivedEvent(Response response) throws CanFrameException 
    {
        for (OnResponseReceivedListener mResponseReceivedListener : mResponseReceivedListeners) {
            mResponseReceivedListener.handleResponseReceivedEvent(response);
        }
        
        if (response instanceof FrameResponse) {
            FrameResponse frameResponse = (FrameResponse)response;

            this.fireFrameReceivedEvent(frameResponse.getFrame());
        }
    }
    
    @Override
    public void send(CanFrame frame) throws CanHackerException
    {
        try {
            TransmitCommand command = new TransmitCommand(frame);
            this.send(command);
        } catch (CommandException e) {
            throw new CanHackerException(e.getMessage());
        }
    }
    
    public abstract CanHacker send(Command c) throws CanHackerException;

    protected void processBytes(byte[] bytes) {
        for (byte aByte : bytes) {

            if (bufferPos >= buffer.length) {
                String str = new String(buffer);
                System.out.println("Error: buffer overflow");
                System.out.println(str);
                bufferPos = 0;
            }

            if (aByte == BELL) {
                // TODO: process that signal
            } else if (aByte == COMMAND_DELIMITER || aByte == '\n') {
                if (bufferPos > 0) {
                    byte[] commandBytes = new byte[bufferPos];
                    System.arraycopy(buffer, 0, commandBytes, 0, bufferPos);
                    try {
                        Response response = Response.fromBytes(commandBytes);
                        fireResponseReceivedEvent(response);
                    } catch (CanFrameException e) {
                        fireErrorEvent(new CanHackerException("CanFrame error: " + e.getMessage()));
                    } catch (CanHackerException e) {
                        fireErrorEvent(new CanHackerException("CanHacker error: " + e.getMessage()));
                    }
                }
                bufferPos = 0;
            } else {
                buffer[bufferPos++] = aByte;
            }
        }
    }

    public static CanFrame parseFrame(byte[] bytes) throws CanHackerException, CanFrameException {
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

    public static byte[] assembleTransmit(CanFrame frame) {
        return assembleTransmitString(frame).getBytes();
    }

    public static String assembleTransmitString(CanFrame frame) {
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
}
