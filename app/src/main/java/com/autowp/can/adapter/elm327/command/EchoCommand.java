package com.autowp.can.adapter.elm327.command;

public class EchoCommand extends Command {

    private final boolean enabled;

    public EchoCommand(boolean enabled)
    {
        this.enabled = enabled;
    }
    
    @Override
    public String toString() {
        return "AT E" + (this.enabled ? "1" : "0");
    }

}
