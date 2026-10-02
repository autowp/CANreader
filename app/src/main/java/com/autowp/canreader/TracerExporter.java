package com.autowp.canreader;

import com.autowp.can.CanMessage;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Экспорт трейсов в различные форматы.
 */
public class TracerExporter {

    public enum Format {
        ASC("CANalyzer", ".asc"),
        DBC("CANdb++", ".dbc"),
        CSV("CAN bus analyzer", ".csv"),
        LOG("CanHacker Trace", ".trc");

        private final String name;
        private final String extension;

        Format(String name, String extension) {
            this.name = name;
            this.extension = extension;
        }

        public String getName() { return name; }
        public String getExtension() { return extension; }
    }

    /**
     * Экспорт в формат CANalyzer ASC
     */
    public static void exportASC(List<TracerMessage> messages, OutputStream out) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("// CANalyzer ASC Export\n");
        sb.append("// Date: ").append(new Date()).append("\n\n");
        sb.append("begin Triggerword\n");
        sb.append("    Timestamp Absolute\n");
        sb.append("    Message \"Raw\"\n");
        sb.append("    Direction FromAny\n");
        sb.append("end Triggerword\n\n");
        sb.append("begin Trace\n");
        sb.append("    Triggerwords Triggerword\n");
        sb.append("    Result ASCII\n");
        sb.append("end Trace\n\n");

        SimpleDateFormat sdf = new SimpleDateFormat("MM.dd.yyyy HH:mm:ss.SSS", Locale.getDefault());

        for (TracerMessage msg : messages) {
            CanMessage frame = msg.getCanMessage();
            Date ts = msg.getTimestamp();
            if (ts == null) continue;

            String timeStr = sdf.format(ts);
            String idStr = frame.isExtended()
                    ? String.format(Locale.ROOT, "%08X", frame.getId())
                    : String.format(Locale.ROOT, "%03X", frame.getId());

            sb.append(timeStr).append(" ").append(idStr).append(" ");
            sb.append(frame.isExtended() ? "Extended" : "Std").append(" ");
            sb.append(frame.isRTR() ? "Remote" : "Data").append(" ");
            sb.append(frame.getDLC()).append("\n");

            if (!frame.isRTR()) {
                byte[] data = frame.getData();
                for (int i = 0; i < data.length; i++) {
                    sb.append(String.format(Locale.ROOT, "%02X", data[i] & 0xFF));
                    if (i < data.length - 1) sb.append(" ");
                }
                sb.append("\n");
            }
        }

        sb.append("end Trace\n");
        out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    /**
     * Экспорт в формат CANdb++ DBC
     */
    public static void exportDBC(List<TracerMessage> messages, OutputStream out) throws IOException {
        StringBuilder sb = new StringBuilder();

        // DBC header
        sb.append("VERSION\n\n");
        sb.append("ASCIITABLE:\n\n");
        sb.append("NS_ :\n\n    NS_DESC_\n");
        sb.append("    CM_\n\n    BO_ \n");
        sb.append("    SG_ \n\n    BA_DEF_ \n");
        sb.append("    BA_\n\n    VAL_ \n\n");

        // Collect unique CAN IDs
        java.util.Set<Integer> uniqueIds = new java.util.LinkedHashSet<>();
        for (TracerMessage msg : messages) {
            uniqueIds.add(msg.getCanMessage().getId());
        }

        int msgId = 1;
        for (int canId : uniqueIds) {
            boolean isExtended = false;
            for (TracerMessage msg : messages) {
                if (msg.getCanMessage().getId() == canId && msg.getCanMessage().isExtended()) {
                    isExtended = true;
                    break;
                }
            }

            String idStr = isExtended
                    ? String.format(Locale.ROOT, "%08X", canId)
                    : String.format(Locale.ROOT, "%03X", canId);

            // Find max DLC for this ID
            byte maxDlc = 0;
            for (TracerMessage msg : messages) {
                if (msg.getCanMessage().getId() == canId) {
                    maxDlc = (byte) Math.max(maxDlc, msg.getCanMessage().getDLC());
                }
            }

            sb.append("BO_ ").append(msgId).append(" Msg_").append(idStr).append(": ").append(canId).append(" ");
            sb.append(maxDlc).append("\n");

            // Add signal for each byte
            byte[] sampleData = null;
            for (TracerMessage msg : messages) {
                if (msg.getCanMessage().getId() == canId) {
                    sampleData = msg.getCanMessage().getData();
                    break;
                }
            }

            if (sampleData != null) {
                for (int i = 0; i < sampleData.length && i < 8; i++) {
                    sb.append("    SG_ Byte").append(i).append(" : ").append(i).append("|").append(8).append("@0+");
                    sb.append("(1,0)|0,0\n");
                }
            }

            msgId++;
        }

        // Attribute definitions
        sb.append("BA_DEF_ BO_ \"BusType\" STRING;\n\n");
        sb.append("BA_DEF_ SG_ \"Min\" FLOAT 0 1683461433.67;\n\n");
        sb.append("BA_ \"Min\" SG_ 1 Byte0 0;\n\n");

        out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    /**
     * Экспорт в формат CAN bus analyzer (CSV)
     */
    public static void exportCSV(List<TracerMessage> messages, OutputStream out) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("Time,ID,Direction,Length,Data\n");

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());

        for (TracerMessage msg : messages) {
            CanMessage frame = msg.getCanMessage();
            Date ts = msg.getTimestamp();
            if (ts == null) continue;

            String timeStr = sdf.format(ts);
            String idStr = frame.isExtended()
                    ? String.format(Locale.ROOT, "%08X", frame.getId())
                    : String.format(Locale.ROOT, "%03X", frame.getId());

            String direction = frame.isRTR() ? "R" : "T";
            String dataStr = frame.isRTR() ? "" : "";
            byte[] data = frame.isRTR() ? new byte[0] : frame.getData();

            for (int i = 0; i < data.length; i++) {
                dataStr += String.format(Locale.ROOT, "%02X ", data[i] & 0xFF);
            }

            sb.append(timeStr).append(",").append(idStr).append(",").append(direction)
                    .append(",").append(frame.getDLC()).append(",").append(dataStr.trim()).append("\n");
        }

        out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    /**
     * Экспорт в формат CanHacker Trace v2 (.log)
     * Формат: @ TEXT @ 2 @ 64 @ 1 @ RowCount @ Time in ms @ Time hh:mm:ss.zzz @
     *         # Device GUID # Device name # Source # Source baudrate # Channel alias # Channel baudrate #
     *         TimeStamp TAB Channel TAB Flags TAB MsgID TAB DLC TAB Data TAB CRC TAB ASCII TAB Comment
     */
    public static void exportLOG(List<TracerMessage> messages, OutputStream out) throws IOException {
        StringBuilder sb = new StringBuilder();

        // Строка 1: заголовок
        int rowCount = messages.size();
        long totalTimeMs = 0;
        SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());
        String timeStr = "00:00:00.000";

        if (!messages.isEmpty()) {
            Date firstTs = messages.get(0).getTimestamp();
            Date lastTs = messages.get(messages.size() - 1).getTimestamp();
            if (firstTs != null && lastTs != null) {
                totalTimeMs = lastTs.getTime() - firstTs.getTime();
                timeStr = sdfTime.format(lastTs);
            }
        }

        sb.append("@ TEXT @ 2 @ 64 @ 1 @ ").append(rowCount)
                .append(" @ ").append(totalTimeMs)
                .append(" @ ").append(timeStr).append(" @\n");

        // Строка 2: информация об устройстве
        sb.append("# ").append(java.util.UUID.randomUUID().toString())
                .append(" # CAN-Hacker v3.x # COM1 # 2 # 1 # 8 #\n");

        // Строки данных
        for (TracerMessage msg : messages) {
            CanMessage frame = msg.getCanMessage();
            if (frame == null) continue;

            // TimeStamp: секунды,миллисекунды
            long ts = msg.getTimestampMs();
            long seconds = ts / 1000;
            long millis = ts % 1000;
            sb.append(seconds).append(",").append(String.format(Locale.ROOT, "%03d", millis));

            // Channel
            sb.append("\t1");

            // Flags: 0001=29-bit, 0008=RTR
            int flags = 0;
            if (frame.isExtended()) flags |= 0x0001;
            if (frame.isRTR()) flags |= 0x0008;
            sb.append("\t").append(String.format(Locale.ROOT, "%04X", flags));

            // MsgID
            String idStr = frame.isExtended()
                    ? String.format(Locale.ROOT, "%08X", frame.getId())
                    : String.format(Locale.ROOT, "%03X", frame.getId());
            sb.append("\t").append(idStr);

            // DLC
            sb.append("\t").append(frame.getDLC());

            // Data
            if (frame.isRTR()) {
                sb.append("\t");
            } else {
                byte[] data = frame.getData();
                for (int i = 0; i < data.length; i++) {
                    sb.append(String.format(Locale.ROOT, "%02X", data[i] & 0xFF));
                    if (i < data.length - 1) sb.append(" ");
                }
                sb.append("\t");
            }

            // CRC (пусто для CAN)
            sb.append("\t");

            // ASCII (пусто)
            sb.append("\t");

            // Comment (пусто)
            sb.append("\n");
        }

        out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    /**
     * Экспорт в формат CanHacker Trace .trc
     */
    public static void exportTrc(List<TracerMessage> messages, OutputStream out) throws IOException {
        StringBuilder sb = new StringBuilder();
        
        // Header line 1
        sb.append("@ TEXT @ 2 @ 64 @ 1 @ ").append(messages.size()).append(" @ 0 @ 00:00:00.000 @\n");
        
        // Header line 2
        sb.append("# DeviceGUID # DeviceName # Source # SourceBaudrate # ChannelAliases # ChannelBaudrates #\n");
        
        // Data lines
        for (TracerMessage msg : messages) {
            CanMessage frame = msg.getCanFrame();
            long ts = msg.getTimestampMs();
            
            // TimeStamp: секунды,миллисекунды
            long seconds = ts / 1000;
            long millis = ts % 1000;
            sb.append(String.format(Locale.ROOT, "%d,%03d", seconds, millis));
            
            // Channel
            sb.append("\t1");
            
            // Flags: 0001=29-bit, 0008=RTR, 0020=BRS
            int flags = 0;
            if (frame.isExtended()) flags |= 0x0001;
            if (frame.isRTR()) flags |= 0x0008;
            sb.append("\t").append(String.format(Locale.ROOT, "%04X", flags));
            
            // MsgID
            String idStr = frame.isExtended()
                    ? String.format(Locale.ROOT, "%08X", frame.getId())
                    : String.format(Locale.ROOT, "%03X", frame.getId());
            sb.append("\t").append(idStr);
            
            // DLC
            sb.append("\t").append(frame.getDLC());
            
            // Data
            if (frame.isRTR()) {
                sb.append("\t");
            } else {
                byte[] data = frame.getData();
                for (int i = 0; i < data.length; i++) {
                    sb.append(String.format(Locale.ROOT, "%02X", data[i] & 0xFF));
                    if (i < data.length - 1) sb.append(" ");
                }
                sb.append("\t");
            }
            
            // CRC (пусто для CAN)
            sb.append("\t");
            
            // ASCII (пусто)
            sb.append("\t");
            
            // Comment (пусто)
            sb.append("\n");
        }
        
        out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        out.flush();
    }
}
