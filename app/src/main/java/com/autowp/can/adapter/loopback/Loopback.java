package com.autowp.can.adapter.loopback;

import com.autowp.can.CanAdapter;
import com.autowp.can.CanAdapterException;
import com.autowp.can.CanFrame;

/**
 * Created by autowp on 24.03.2016.
 */
public class Loopback extends CanAdapter {
    @Override
    public void send(CanFrame frame) throws CanAdapterException {
        fireFrameReceivedEvent(frame);
    }

    @Override
    public void connect(Runnable callback) throws CanAdapterException {
        callback.run();
    }

    @Override
    public void disconnect(Runnable callback) throws CanAdapterException {
        callback.run();
    }
}
