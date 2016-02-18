package com.autowp.can.adapter.canhacker.command;

/**
 * v[CR]
 * 
 * Read detailed firmware version from device.
 * 
 * Return: vmami[CR]
 * 
 * ma = major version number
 * mi = minor version number 
 */
public class FirmwareVersionCommand extends SimpleCommand {
    public FirmwareVersionCommand()
    {
        this.name = 'v';
    }
}
