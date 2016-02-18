package com.autowp.can.adapter.canhacker.command;

import com.autowp.can.CanFrame;
import com.autowp.can.adapter.canhacker.CanHacker;

/**
 * tiiiLDDDDDDDDDDDDDDDD[CR]
 * 
 * This command transmits a standard 11 Bit CAN frame. 
 * It works only if controller is in operational mode after command O.
 * 
 * iii = Identifier in hexadecimal (000-7FF)
 * L   = Data length code (0-8)
 * DD  = Data byte value in hexadecimal (00-FF). 
 * 
 * Number of given data bytes will be checked against given data length code.
 * 
 * Return: [CR] or [BEL] 
 */
public class TransmitCommand extends Command {
    protected CanFrame frame;
    
    public TransmitCommand(CanFrame frame) throws CommandException
    {
        this.frame = frame;
    }
    
    @Override
    public byte[] getBytes() {
        return CanHacker.assembleTransmit(frame);
    }

    public CanFrame getFrame() {
        return frame;
    }
}
