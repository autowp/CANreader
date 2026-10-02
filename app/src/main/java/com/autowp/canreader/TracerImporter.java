package com.autowp.canreader;

import com.autowp.Hex;
import com.autowp.can.CanMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Импорт трейсов из различных форматов.
 */
public class TracerImporter {

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
     * Импорт из формата CANalyzer ASC
     */
    public static List<TracerMessage> importASC(InputStream inputStream) throws IOException {
        List<TracerMessage> messages = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

        String line;
        SimpleDateFormat sdf = new SimpleDateFormat("MM.dd.yyyy HH:mm:ss.SSS", Locale.getDefault());
        Date now = new Date();

        Pattern dataPattern = Pattern.compile("^\\s*(\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2}:\\d{2}\\.\\d+)\\s+(\\d{3,8}[Xx]?)\\s+(Std|Extended)\\s+(Data|Remote)\\s+(\\d+)\\s*(.*)?$");

        while ((line = reader.readLine()) != null) {
            if (line.startsWith("//") || line.trim().isEmpty() || line.startsWith("begin") || line.startsWith("end")) {
                continue;
            }

            Matcher m = dataPattern.matcher(line);
            if (m.matches()) {
                try {
                    String timeStr = m.group(1);
                    String idStr = m.group(2).toUpperCase(Locale.ROOT);
                    String type = m.group(3).toUpperCase(Locale.ROOT);
                    String direction = m.group(4).toUpperCase(Locale.ROOT);
                    int dlc = Integer.parseInt(m.group(5));
                    String dataHex = m.group(6).trim();

                    int id = Integer.parseInt(idStr, 16);
                    boolean isExtended = type.equals("EXTENDED");
                    boolean isRtr = direction.equals("REMOTE");

                    byte[] data = new byte[0];
                    if (!isRtr && !dataHex.isEmpty()) {
                        data = Hex.hexStringToByteArray(dataHex);
                    }

                    CanMessage frame = isRtr
                            ? new CanMessage(id, (byte) dlc, isExtended)
                            : new CanMessage(id, data, isExtended);

                    // Use current time as fallback
                    messages.add(new TracerMessage(frame, System.currentTimeMillis()));
                } catch (Exception e) {
                    // Skip invalid lines
                }
            }
        }
        reader.close();
        return messages;
    }

    /**
     * Импорт из формата CAN bus analyzer (CSV)
     */
    public static List<TracerMessage> importCSV(InputStream inputStream) throws IOException {
        List<TracerMessage> messages = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

        String line;
        boolean headerSkipped = false;
        Date now = new Date();

        while ((line = reader.readLine()) != null) {
            if (!headerSkipped) {
                if (line.toLowerCase().contains("time") || line.toLowerCase().contains("timestamp")) {
                    headerSkipped = true;
                    continue;
                }
                headerSkipped = true;
            }

            if (line.trim().isEmpty()) continue;

            String[] parts = line.split(",");
            if (parts.length >= 4) {
                try {
                    String timeStr = parts[0].trim();
                    String idStr = parts[1].trim().toUpperCase(Locale.ROOT);
                    String direction = parts[2].trim().toUpperCase(Locale.ROOT);
                    int dlc = Integer.parseInt(parts[3].trim());
                    String dataHex = parts.length > 4 ? parts[4].trim() : "";

                    int id = Integer.parseInt(idStr, 16);
                    boolean isExtended = idStr.length() > 3;
                    boolean isRtr = direction.equals("R");

                    byte[] data = new byte[0];
                    if (!isRtr && !dataHex.isEmpty()) {
                        data = Hex.hexStringToByteArray(dataHex);
                    }

                    CanMessage frame = isRtr
                            ? new CanMessage(id, (byte) dlc, isExtended)
                            : new CanMessage(id, data, isExtended);

                    messages.add(new TracerMessage(frame, System.currentTimeMillis()));
                } catch (Exception e) {
                    // Skip invalid lines
                }
            }
        }
        reader.close();
        return messages;
    }

    /**
     * Импорт из формата CanHacker Trace v2 (.trc)
     * Формат строки: TimeStamp TAB Channel TAB Flags TAB MsgID TAB DLC TAB Data TAB CRC TAB ASCII TAB Comment
     * Flags: 0001=29-bit, 0002=Error, 0004=CAN FD, 0008=RTR, 0020=BRS
     */
    public static List<TracerMessage> importLOG(InputStream inputStream) throws IOException {
        List<TracerMessage> messages = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

        String line;
        Date now = new Date();

        while ((line = reader.readLine()) != null) {
            // Пропускаем заголовок (строки начинающиеся с @ или #)
            if (line.trim().isEmpty() || line.startsWith("@") || line.startsWith("#")) {
                continue;
            }

            // Разделяем по табуляции
            String[] parts = line.split("\\t", -1);
            if (parts.length < 5) continue;

            try {
                // TimeStamp: секунды,миллисекунды
                String timestampStr = parts[0].trim();
                String[] tsParts = timestampStr.split(",");
                long seconds = Long.parseLong(tsParts[0].trim());
                long millis = tsParts.length > 1 ? Long.parseLong(tsParts[1].trim()) : 0;
                long timestamp = seconds * 1000 + millis;

                // Channel (игнорируем)
                // int channel = Integer.parseInt(parts[1].trim());

                // Flags
                int flags = Integer.parseInt(parts[2].trim(), 16);
                boolean isExtended = (flags & 0x0001) != 0;
                boolean isRtr = (flags & 0x0008) != 0;
                boolean isError = (flags & 0x0002) != 0;

                // MsgID
                String idStr = parts[3].trim();
                int id = Integer.parseInt(idStr, 16);

                // DLC
                int dlc = Integer.parseInt(parts[4].trim());

                // Data
                byte[] data = new byte[0];
                if (!isRtr && parts.length > 5 && !parts[5].trim().isEmpty()) {
                    data = Hex.hexStringToByteArray(parts[5].trim());
                }

                CanMessage frame = isRtr
                        ? new CanMessage(id, (byte) dlc, isExtended)
                        : new CanMessage(id, data, isExtended);

                messages.add(new TracerMessage(frame, timestamp));
            } catch (Exception e) {
                // Skip invalid lines
            }
        }
        reader.close();
        return messages;
    }

    /**
     * DBC файлы не импортируются — они описывают структуру, а не трейсы
     */
    public static List<TracerMessage> importDBC(InputStream inputStream) throws IOException {
        // DBC format is for database description, not trace data
        // Return empty list with a comment
        return new ArrayList<>();
    }

    /**
     * Импорт из формата CanHacker Trace .trc
     */
    public static List<TracerMessage> importTrc(InputStream inputStream) throws IOException {
        List<TracerMessage> messages = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

        String line;
        while ((line = reader.readLine()) != null) {
            // Skip header lines
            if (line.startsWith("@ TEXT") || line.startsWith("#")) {
                continue;
            }
            
            // Parse data lines
            String[] parts = line.split("\t");
            if (parts.length < 6) {
                continue;
            }
            
            try {
                // Parse timestamp
                String[] timeParts = parts[0].trim().split(",");
                long seconds = Long.parseLong(timeParts[0]);
                long millis = Long.parseLong(timeParts[1]);
                long timestamp = (seconds * 1000) + millis;

                // Flags
                int flags = Integer.parseInt(parts[2].trim(), 16);
                boolean isExtended = (flags & 0x0001) != 0;
                boolean isRtr = (flags & 0x0008) != 0;

                // MsgID
                String idStr = parts[3].trim();
                int id = Integer.parseInt(idStr, 16);

                // DLC
                int dlc = Integer.parseInt(parts[4].trim());

                // Data
                byte[] data = new byte[0];
                if (!isRtr && parts.length > 5 && !parts[5].trim().isEmpty()) {
                    data = Hex.hexStringToByteArray(parts[5].trim());
                }

                CanMessage frame = isRtr
                        ? new CanMessage(id, (byte) dlc, isExtended)
                        : new CanMessage(id, data, isExtended);

                messages.add(new TracerMessage(frame, timestamp));
            } catch (Exception e) {
                // Skip invalid lines
            }
        }
        reader.close();
        return messages;
    }
}
