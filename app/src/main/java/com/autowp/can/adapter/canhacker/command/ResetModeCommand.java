package com.autowp.can.adapter.canhacker.command;

/**
 * This command switches the CAN controller from operational in reset mode. 
 * The controller is no longer involved in bus activities.
 * Command is only active if controller was set to operational mode with command O before.
 * Return: [CR] or [BEL] 
 */
public class ResetModeCommand extends SimpleCommand {
    public ResetModeCommand()
    {
        this.name = "C";
    }
}
