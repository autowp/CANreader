package com.autowp.can.adapter.canhacker.response;

import com.autowp.Hex;

public class BellResponse extends Response {
    final public static char CODE = 0x07;
    
    public BellResponse(byte[] bytes) throws ResponseException
    {
        if (bytes.length != 1) {
            String hex = Hex.byteArrayToHexString(bytes);
            throw new ResponseException("Bell response must be 1 bytes long. `" + hex + "` received");
        }
        
        if (bytes[0] != CODE) {
            throw new ResponseException("Bell response must be 0x07 character");
        }
    }
    
    public String toString()
    {
        return new String(new char[] { CODE });
    }
}
