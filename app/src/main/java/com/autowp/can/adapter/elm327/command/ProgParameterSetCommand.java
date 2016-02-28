package com.autowp.can.adapter.elm327.command;

import com.autowp.Hex;

public class ProgParameterSetCommand extends Command {
    
    protected byte pp;
    protected byte value;

    public ProgParameterSetCommand(byte pp, byte value)
    {
        this.pp = pp;
        this.value = value;
    }
    
    @Override
    public String toString() {
        String strPP = Hex.byteArrayToHexString(new byte[] {pp}).toUpperCase();
        String strValue = Hex.byteArrayToHexString(new byte[] {value}).toUpperCase();

        return "AT PP" + strPP + "SV" + strValue;
    }

}
