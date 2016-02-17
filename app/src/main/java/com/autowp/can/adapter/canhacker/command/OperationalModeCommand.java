package com.autowp.can.adapter.canhacker.command;

/**
 * This command switches the CAN controller from reset in operational mode. 
 * The controller is then involved in bus activities. 
 * It works only if the initiated with S or s command before,
 * or controller was set to reset mode with command C.
 * 
 * Return: [CR] or [BEL] 
 */
public class OperationalModeCommand extends SimpleCommand {
    public OperationalModeCommand()
    {
        this.name = "O";
    }
}
