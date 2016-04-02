package com.autowp.can.adapter.loopback;

import com.autowp.can.CanAdapter;
import com.autowp.can.CanAdapterException;
import com.autowp.can.CanBusSpecs;
import com.autowp.can.CanFrame;

/**
 * Created by autowp on 24.03.2016.
 */
public class Loopback extends CanAdapter {
    public Loopback(CanBusSpecs specs) {
        super(specs);
    }

    @Override
    public void doSend(CanFrame frame) throws CanAdapterException {
        receive(frame);
    }

    @Override
    public void doConnect() throws CanAdapterException {

    }

    @Override
    public void doDisconnect() throws CanAdapterException {

    }
}
