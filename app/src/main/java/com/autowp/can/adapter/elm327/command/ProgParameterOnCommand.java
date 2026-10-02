package com.autowp.can.adapter.elm327.command;

import com.autowp.Hex;

import java.util.Locale;

public class ProgParameterOnCommand extends Command {

    protected byte pp;

    public ProgParameterOnCommand(byte pp)
    {
        this.pp = pp;
    }
    
    @Override
    public String toString() {
        String strPP = Hex.byteArrayToHexString(new byte[] {pp}).toUpperCase(Locale.ROOT);

        return "AT PP" + strPP + "SVON";
    }

}
