package com.autowp.can.adapter.elm327.command;

import android.text.TextUtils;

import com.autowp.Hex;
import com.autowp.can.adapter.elm327.Elm327Exception;

public class TransmitCommand extends Command {

    private int id;

    private byte[] data;

    private final int MAX_DATA_LENGTH = 8;
    
    public TransmitCommand(byte[] newData) throws Elm327Exception
    {
        if (newData.length > MAX_DATA_LENGTH) {
            throw new Elm327Exception("Data length must be in " + MAX_DATA_LENGTH + " bytes");
        }
        
        this.data = new byte[newData.length];

        System.arraycopy(newData, 0, this.data, 0, newData.length);
    }
    
    public byte[] getData()
    {
        return data;
    }
    
    @Override
    public String toString() {
        String[] strings = Hex.byteArrayToHexString(this.data).toUpperCase().split("(?<=\\G.{2})");
        return TextUtils.join(" ", strings);
    }

}
