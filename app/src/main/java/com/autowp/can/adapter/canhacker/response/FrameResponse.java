package com.autowp.can.adapter.canhacker.response;

import com.autowp.can.CanFrame;
import com.autowp.can.CanFrameException;
import com.autowp.can.adapter.canhacker.CanHacker;
import com.autowp.can.adapter.canhacker.CanHackerException;

public class FrameResponse extends Response {

    final public static int TIMESTAMP_LENGTH_CHARS = 4;
    
    private CanFrame frame;

    protected int timestamp;
    
    public FrameResponse(byte[] bytes) throws CanHackerException, CanFrameException {
        frame = CanHacker.parseFrame(bytes);
        System.out.println(frame.isRTR());
    }

    @Override
    public String toString() {
        return CanHacker.assembleTransmitString(frame);
    }

    public CanFrame getFrame()
    {
        return frame;
    }
    
}
