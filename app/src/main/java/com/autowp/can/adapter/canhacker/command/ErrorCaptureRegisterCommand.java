package com.autowp.can.adapter.canhacker.command;

/**
 * Read last stored Error Capture register content from last bus error interrupt.
 * Return: Exx[CR] 
 */
public class ErrorCaptureRegisterCommand extends SimpleCommand {
    public ErrorCaptureRegisterCommand()
    {
        this.name = "E";
    }
}
