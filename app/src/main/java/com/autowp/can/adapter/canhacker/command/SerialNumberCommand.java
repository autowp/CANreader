package com.autowp.can.adapter.canhacker.command;

/**
 * Read serial number from device.
 * 
 * Return: Nxxxx[CR]
 * xxxx = Serial number in alphanumeric characters. 
 */
public class SerialNumberCommand extends SimpleCommand {
    public SerialNumberCommand()
    {
        this.name = 'N';
    }
}
