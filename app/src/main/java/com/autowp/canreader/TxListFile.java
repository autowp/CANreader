package com.autowp.canreader;

import com.autowp.Hex;
import com.autowp.can.CanFrame;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created by autow on 22.03.2016.
 */
public class TxListFile {

    private static final String PREFIX = "Message";
    private static final String RTR = "RTR";
    private static final String ID = "Id";
    private static final String SECTION = "TxList";

    public static final String EXTENSION = "txl";

    public static void write(OutputStream outputStream, ArrayList<TransmitCanFrame> list) throws ConfigurationException {

        HierarchicalINIConfiguration iniConfObj = new HierarchicalINIConfiguration();

        SubnodeConfiguration section = iniConfObj.getSection(SECTION);

        int i = 0;
        for (TransmitCanFrame frame : list) {
            String prefix = PREFIX + i;

            CanFrame canFrame = frame.getCanFrame();

            section.addProperty(prefix + ID, String.format("%03X", canFrame.getId()));
            section.addProperty(prefix + "DLC", String.format("%d", canFrame.getDLC()));
            String dataStr;
            if (canFrame.isRTR()) {
                dataStr = RTR;
            } else {
                dataStr = "";
                byte[] data = canFrame.getData();
                for (int j=0; j < data.length; j++) {
                    dataStr += String.format("%02X", data[j]);
                    if (j < data.length-1) {
                        dataStr += " ";
                    }
                }
            }
            section.addProperty(prefix + "Data", dataStr);
            section.addProperty(prefix + "Period", String.format("%d", frame.getPeriod()));

            section.addProperty(prefix + "Comment", "");
            section.addProperty(prefix + "Mode", "1000");
            section.addProperty(prefix + "TriggerId", "");
            section.addProperty(prefix + "TriggerData", "0");


            i++;
        }

        section.addProperty(PREFIX + i + ID, "-1");

        iniConfObj.save(outputStream);

    }

    public static List<TransmitCanFrame> read(InputStream inputStream) throws ConfigurationException {

        HierarchicalINIConfiguration iniConfObj = new HierarchicalINIConfiguration();
        iniConfObj.load(inputStream);

        SubnodeConfiguration section = iniConfObj.getSection(SECTION);

        ArrayList<TransmitCanFrame> list = new ArrayList<>();

        for (int i=0; true; i++) {
            String prefix = PREFIX + i;

            try {

                String idStr = (String) section.getProperty(prefix + ID);

                if (idStr.equalsIgnoreCase("-1")) {
                    break;
                }

                boolean isExt = idStr.length() > 3;
                int id = Integer.parseInt(idStr, 16);

                if (id < 0) {
                    break;
                }

                String dlcStr = (String) section.getProperty(prefix + "DLC");
                System.out.print("DLC=");
                System.out.println(dlcStr);
                byte dlc = Byte.parseByte(dlcStr);
                String dataStr = (String) section.getProperty(prefix + "Data");
                boolean isRTR = dataStr.equalsIgnoreCase(RTR);
                byte[] data = new byte[0];
                if (!isRTR) {
                    data = Hex.hexStringToByteArray(dataStr);
                }
                int period = Integer.parseInt((String) section.getProperty(prefix + "Period"));

                System.out.print("Data=");
                System.out.println(Hex.byteArrayToHexString(data));

                CanFrame canFrame;
                if (isRTR) {
                    canFrame = new CanFrame(id, dlc, isExt);
                } else {
                    canFrame = new CanFrame(id, data, isExt);
                }

                TransmitCanFrame frame = new TransmitCanFrame(canFrame, period);

                list.add(frame);

            } catch (Exception e) {
                e.printStackTrace();
            }

            /*Message0Comment=
            Message0Mode=1000
            Message0TriggerId=
            Message0TriggerData=0*/
        }

        return list;
    }
}
