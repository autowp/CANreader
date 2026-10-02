package com.autowp.canreader;

import com.autowp.can.CanFrame;
import com.autowp.can.CanMessage;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;

public class MonitorCanMessageTest {

    @Test
    public void periodAveragesIntervalsBetweenReceivedFrames() throws Exception {
        MonitorCanMessage monitorMessage = new MonitorCanMessage(
                new CanMessage(new CanFrame(0x123, new byte[] {0x01}, false)),
                0
        );

        monitorMessage.addTime(new Date(1000));
        monitorMessage.addTime(new Date(1100));
        assertEquals(100, monitorMessage.getPeriod());

        monitorMessage.addTime(new Date(1300));
        assertEquals(150, monitorMessage.getPeriod());
    }
}
