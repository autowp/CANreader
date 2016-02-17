package com.autowp.can.adapter.canhacker.response;

import com.autowp.Hex;

import java.nio.ByteBuffer;

public class CanErrorResponse extends Response {
    final public static char CODE = 'F';
    
    protected int errorCode;

    public CanErrorResponse(byte[] bytes) throws ResponseException
    {
        if (bytes.length != 3) {
            throw new ResponseException("Version response must be 3 bytes long");
        }
        
        byte[] decodedBytes;
        try {
            decodedBytes = Hex.hexStringToByteArray((new String(bytes)).substring(1));
        } catch (Exception e) {
            throw new ResponseException("Decoder error: " + e.getMessage());
        }

        if (decodedBytes.length != 1) {
            throw new ResponseException("Decoded error code must by 1 byte long");
        }
        
        byte[] intBytes = new byte[] {0, 0, 0, 0};
        System.arraycopy(decodedBytes, 0, intBytes, 3, 1);
        
        this.errorCode = ByteBuffer.wrap(intBytes).getInt();
    }
    
    @Override
    public String toString() {
        byte[] codeBytes = new byte[1];
        codeBytes[0] = (byte)this.errorCode;
        
        return CODE + Hex.byteArrayToHexString(codeBytes).toUpperCase();
    }

    public int getErrorCode()
    {
        return this.errorCode;
    }
}
