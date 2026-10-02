package com.autowp.canreader;

import com.autowp.can.CanFrame;

import org.apache.commons.configuration2.ex.ConfigurationException;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TxListFileTest {

    @Test
    public void roundTripsDataAndRemoteFrames() throws Exception {
        ArrayList<TransmitCanFrame> input = new ArrayList<>();
        input.add(new TransmitCanFrame(
                new CanFrame(0x123, new byte[] {0x01, (byte) 0x80, (byte) 0xFF}, false), 100));
        input.add(new TransmitCanFrame(new CanFrame(0x1ABCDE, (byte) 8, true), 250));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        TxListFile.write(output, input);
        List<TransmitCanFrame> result = TxListFile.read(new ByteArrayInputStream(output.toByteArray()));

        assertEquals(2, result.size());
        assertEquals(input.get(0).getCanFrame().toString(), result.get(0).getCanFrame().toString());
        assertEquals(100, result.get(0).getPeriod());
        assertTrue(result.get(1).getCanFrame().isRTR());
        assertEquals(8, result.get(1).getCanFrame().getDLC());
        assertEquals(250, result.get(1).getPeriod());
    }

    @Test(expected = ConfigurationException.class)
    public void rejectsInvalidTransmitEntry() throws Exception {
        byte[] invalid = "[TxList]\nMessage0Id=123\nMessage0DLC=2\nMessage0Data=01\nMessage0Period=10\n"
                .getBytes(java.nio.charset.StandardCharsets.UTF_8);
        TxListFile.read(new ByteArrayInputStream(invalid));
    }
}