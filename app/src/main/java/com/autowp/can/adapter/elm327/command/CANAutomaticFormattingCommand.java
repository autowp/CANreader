package com.autowp.can.adapter.elm327.command;

public class CANAutomaticFormattingCommand extends Command {

    private final boolean enabled;

    public CANAutomaticFormattingCommand(boolean enabled)
    {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return "AT CAF" + (this.enabled ? "1" : "0");
    }

}
