package com.autowp.can.adapter.canhacker.response;

public class VersionResponse extends Response {
    final public static char CODE = 'V';
    
    protected String version;
    
    public VersionResponse(byte[] bytes) throws ResponseException
    {
        if (bytes.length != 5) {
            throw new ResponseException("Version response must be 5 bytes long");
        }
        
        this.version = (new String(bytes)).substring(1);
    }
    
    public String getVersion()
    {
        return version;
    }

    @Override
    public String toString() {
        return CODE + this.version;
    }
}
