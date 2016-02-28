package com.autowp.can.adapter.elm327.command;

import com.autowp.Hex;

public class SetProtocolCommand extends Command {
    
    public static final byte AUTOMATIC = 0;
    public static final byte SAE_J1850_PWM = 1;
    public static final byte SAE_J1850_VPW = 2;
    public static final byte ISO_9141_2 = 3;
    public static final byte ISO_14230_4_KWP = 4;
    public static final byte ISO_14230_4_KWP_FASTINIT = 5;
    public static final byte ISO_15765_4_CAN_11_500 = 6;
    public static final byte ISO_15765_4_CAN_29_500 = 7;
    public static final byte ISO_15765_4_CAN_11_250 = 8;
    public static final byte ISO_15765_4_CAN_29_250 = 9;
    public static final byte SAE_J1939_CAN = 0xA;
    public static final byte USER1_CAN = 0xB; // 11bit, 125kbps by default
    public static final byte USER2_CAN = 0xC; // 11bit, 50kbps by default
    
    protected byte protocol = AUTOMATIC;
    
    public SetProtocolCommand(byte protocol)
    {
        this.protocol = protocol;
    }
    
    @Override
    public String toString() {
        return "AT SP " + Hex.byteArrayToHexString(new byte[] {protocol}).toUpperCase();
    }
}
