package com.autowp.can.adapter.canhacker.command;

/**
 * V[CR]
 * 
 * Read hardware and firmware version from device.
 * 
 * Return: Vhhff[CR]
 * 
 * hh = hardware version
 * ff = firmware version 
 */
public class VersionCommand extends SimpleCommand {
    public VersionCommand()
    {
        this.name = "V";
    }
}
