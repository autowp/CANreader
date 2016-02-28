package com.autowp.can.adapter.elm327.response;

public class UnknownResponse extends Response {
    protected byte[] bytes;
    public UnknownResponse(byte[] bytes)
    {
        this.bytes = bytes;
    }

    @Override
    public String toString() {
        return new String(bytes);
    }

}
