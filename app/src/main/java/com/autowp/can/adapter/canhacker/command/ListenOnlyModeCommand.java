package com.autowp.can.adapter.canhacker.command;

/**
 * This command will switch the CAN controller in Listen Only mode. 
 * No channel open command (O) is required after issuing L.
 * Use the close channel command C to return to reset mode,
 * for re-init of SJA1000 send a set bit rate command s or S.
 * 
 * Return: [CR] 
 */
public class ListenOnlyModeCommand extends SimpleCommand {
    public ListenOnlyModeCommand()
    {
        this.name = "L";
    }
}
