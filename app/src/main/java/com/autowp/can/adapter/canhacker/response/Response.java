package com.autowp.can.adapter.canhacker.response;

import com.autowp.Hex;
import com.autowp.can.CanFrameException;
import com.autowp.can.adapter.canhacker.CanHacker;
import com.autowp.can.adapter.canhacker.CanHackerException;

abstract public class Response {
    
    abstract public String toString();
    
    public static Response fromBytes(byte[] bytes) throws CanHackerException, CanFrameException {
        if (bytes.length <= 0) {
            throw new ResponseException("Invalid response: empty bytes");
        }
        switch ((char)bytes[0]) {
            case BellResponse.CODE: // bell
                return new BellResponse(bytes);
            case VersionResponse.CODE:
                return new VersionResponse(bytes);
            case CanErrorResponse.CODE:
                return new CanErrorResponse(bytes);
            case FirmwareVersionResponse.CODE:
                return new FirmwareVersionResponse(bytes);
            case CanHacker.COMMAND_11BIT:
            case CanHacker.COMMAND_11BIT_RTR:
            case CanHacker.COMMAND_29BIT:
            case CanHacker.COMMAND_29BIT_RTR:
                return new FrameResponse(bytes);
        }
        
        String hex = Hex.byteArrayToHexString(bytes);
        
        throw new ResponseException("Invalid response: response type not found. `" + hex + "`");
    }
}
