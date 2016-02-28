package com.autowp.can.adapter.elm327.command;

public class CanSilentMonitorCommand extends Command {
    protected boolean enabled;
    
    public CanSilentMonitorCommand(boolean enabled)
    {
        this.enabled = enabled;
    }
    
    @Override
    public String toString() {
        return "AT CSM" + (this.enabled ? "1" : "0");
    }
}
