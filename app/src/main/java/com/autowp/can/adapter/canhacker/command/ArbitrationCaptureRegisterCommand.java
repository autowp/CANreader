package com.autowp.can.adapter.canhacker.command;

/**
 * Reads last stored arbitration capture register content from last AL interrupt
 * Return: Axx[CR] 
 */
public class ArbitrationCaptureRegisterCommand extends SimpleCommand {
    public ArbitrationCaptureRegisterCommand()
    {
        this.name = 'A';
    }
}
