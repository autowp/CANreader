package com.autowp.canreader;

import com.autowp.Hex;
import com.autowp.can.CanFrame;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.INIConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * Created by autowp on 22.03.2016.
 */
public class TxListFile {

    private static final String PREFIX = "Message";
    private static final String RTR = "RTR";
    private static final String ID = "Id";
    private static final String SECTION = "TxList";

    public static final String EXTENSION = "txl";

    public static void write(OutputStream outputStream, List<TransmitCanFrame> list)
            throws ConfigurationException, IOException {

        INIConfiguration iniConfObj = new INIConfiguration();

        Configuration section = iniConfObj.getSection(SECTION);

        int i = 0;
        for (TransmitCanFrame frame : list) {
            String prefix = PREFIX + i;

            CanFrame canFrame = frame.getCanFrame();

            section.addProperty(prefix + ID, String.format(Locale.ROOT, "%03X", canFrame.getId()));
            section.addProperty(prefix + "DLC", String.format(Locale.ROOT, "%d", canFrame.getDLC()));
            String dataStr;
            if (canFrame.isRTR()) {
                dataStr = RTR;
            } else {
                byte[] data = canFrame.getData();
                StringBuilder dataBuilder = new StringBuilder(data.length * 3);
                for (int j = 0; j < data.length; j++) {
                    if (j > 0) {
                        dataBuilder.append(' ');
                    }
                    dataBuilder.append(String.format(Locale.ROOT, "%02X", data[j] & 0xFF));
                }
                dataStr = dataBuilder.toString();
            }
            section.addProperty(prefix + "Data", dataStr);
            section.addProperty(prefix + "Period", String.format(Locale.ROOT, "%d", frame.getPeriod()));

            section.addProperty(prefix + "Comment", "");
            section.addProperty(prefix + "Mode", "1000");
            section.addProperty(prefix + "TriggerId", "");
            section.addProperty(prefix + "TriggerData", "0");


            i++;
        }

        section.addProperty(PREFIX + i + ID, "-1");

        OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
        iniConfObj.write(writer);
        writer.flush();

    }

    public static List<TransmitCanFrame> read(InputStream inputStream)
            throws ConfigurationException, IOException {

        INIConfiguration iniConfObj = new INIConfiguration();
        iniConfObj.read(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        Configuration section = iniConfObj.getSection(SECTION);

        List<TransmitCanFrame> list = new java.util.ArrayList<>();

        for (int i = 0; ; i++) {
            String prefix = PREFIX + i;
            try {
                String idStr = (String) section.getProperty(prefix + ID);
                if (idStr == null || idStr.trim().equalsIgnoreCase("-1")) {
                    break;
                }

                boolean isExt = idStr.length() > 3;
                int id = Integer.parseInt(idStr, 16);
                String dlcStr = (String) section.getProperty(prefix + "DLC");
                byte dlc = Byte.parseByte(dlcStr);
                String dataStr = (String) section.getProperty(prefix + "Data");
                boolean isRTR = dataStr.equalsIgnoreCase(RTR);
                byte[] data = new byte[0];
                if (!isRTR) {
                    data = Hex.hexStringToByteArray(dataStr);
                    if (data.length != dlc) {
                        throw new IllegalArgumentException("DLC does not match data length");
                    }
                }
                int period = Integer.parseInt((String) section.getProperty(prefix + "Period"));

                CanFrame canFrame;
                if (isRTR) {
                    canFrame = new CanFrame(id, dlc, isExt);
                } else {
                    canFrame = new CanFrame(id, data, isExt);
                }

                TransmitCanFrame frame = new TransmitCanFrame(canFrame, period);

                list.add(frame);
            } catch (Exception e) {
                throw new ConfigurationException("Invalid transmit entry " + prefix, e);
            }
        }

        return list;
    }
}
