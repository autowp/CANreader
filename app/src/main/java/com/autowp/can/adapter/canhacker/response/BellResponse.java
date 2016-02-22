package com.autowp.can.adapter.canhacker.response;

import com.autowp.Hex;

public class BellResponse extends Response {
    final public static char CODE = 0x07;
    
    public BellResponse()
    {
    }
    
    public String toString()
    {
        return new String(new char[] { CODE });
    }
}
