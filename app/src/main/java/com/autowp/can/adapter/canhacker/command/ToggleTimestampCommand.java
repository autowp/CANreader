package com.autowp.can.adapter.canhacker.command;

/**
 * Z[CR]
 * 
 * This command will toggle the time stamp setting for receiving frames. 
 * Time stamping is disabled by default, but a change of this setting 
 * will be stored in EEPROM and remembered for the next time. 
 * So this command needs to be issued only if necessary.
 * 
 * If time stamping is enabled for received frames, an incoming frame 
 * includes 2 more bytes at the end which is a time stamp in milliseconds.
 * The time stamp starts at 0x0000 and overflows at 0xEA5F which is equal to 59999ms.
 * Each increment time stamp indicates 1ms within the 60000ms frame.
 * The time stamp counter resets if this setting is turned ON. 
 */
public class ToggleTimestampCommand extends SimpleCommand {
    public ToggleTimestampCommand()
    {
        this.name = 'Z';
    }
}
