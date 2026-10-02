package com.autowp.can;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class CanPayloadImmutabilityTest {

    @Test
    public void frameCopiesPayloadOnInputAndOutput() throws Exception {
        byte[] payload = new byte[] {0x12, 0x34};
        CanFrame frame = new CanFrame(0x123, payload, false);

        payload[0] = 0x00;
        byte[] returnedPayload = frame.getData();
        returnedPayload[1] = 0x00;

        assertArrayEquals(new byte[] {0x12, 0x34}, frame.getData());
    }

    @Test
    public void messageCopiesFramePayloadOnInputAndOutput() throws Exception {
        CanFrame frame = new CanFrame(0x123, new byte[] {0x12, 0x34}, false);
        CanMessage message = new CanMessage(frame);

        byte[] returnedPayload = message.getData();
        returnedPayload[0] = 0x00;

        assertArrayEquals(new byte[] {0x12, 0x34}, message.getData());
    }
}