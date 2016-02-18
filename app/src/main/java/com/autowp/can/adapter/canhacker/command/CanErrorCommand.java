package com.autowp.can.adapter.canhacker.command;

/**
 * This command reads the error flags from CAN controller. 
 * Return: Fxx[CR] or [BEL]
 * 
 * Bit 0 Not used
 * Bit 1 Not used
 * Bit 2 Error warning
 * Bit 3 Data overrun
 * Bit 4 Not used
 * Bit 5 Error passive
 * Bit 6 Arbitration Lost
 * Bit 7 Bus error
 * 
 * The red error indication LED will blink if an error interrupt was triggered from SJA1000. 
 * A bus error will generate a constant red LED light.
 * Command F and command S will clear a bus error indication.
 * For detailed error description see the SJA1000 datasheet. 
 */
public class CanErrorCommand extends SimpleCommand {
    public CanErrorCommand()
    {
        this.name = 'F';
    }
}
