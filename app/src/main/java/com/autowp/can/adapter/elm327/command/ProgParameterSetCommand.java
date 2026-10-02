package com.autowp.can.adapter.elm327.command;

import com.autowp.Hex;

import java.util.Locale;

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
        String strPP = Hex.byteArrayToHexString(new byte[] {pp}).toUpperCase(Locale.ROOT);
        String strValue = Hex.byteArrayToHexString(new byte[] {value}).toUpperCase(Locale.ROOT);

        return "AT PP" + strPP + "SV" + strValue;
    }

}
