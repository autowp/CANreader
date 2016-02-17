package com.autowp;

/**
 * Created by autow on 31.01.2016.
 */
public class Hex {

    final private static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static byte[] hexStringToByteArray(String s) throws Exception {
        return hexStringToByteArray(s.getBytes());
    }

    public static byte[] hexStringToByteArray(byte[] s) throws Exception {
        int len = s.length;

        byte[] data = new byte[len / 2];
        int i = 0;
        int b = 0;
        while (i < len) {
            while (s[i] == ' ') { i++; }
            char highChar = (char) s[i++];
            byte high = (byte) Character.digit(highChar, 16);
            if (high == -1) {
                throw new Exception("unexpected character `" + highChar + "`");
            }

            while (s[i] == ' ') { i++; }
            char lowChar = (char) s[i++];
            byte low = (byte) Character.digit(lowChar, 16);
            if (low == -1) {
                throw new Exception("unexpected character `" + lowChar + "`");
            }

            data[b++] = (byte) ((high << 4) + low);
        }
        return data;
    }

    public static String byteArrayToHexString(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static int bytesToInt(byte[] bytes) {
        int result = 0;
        for (int i=0; i<bytes.length; i++) {
            result <<= 4;
            result += Character.digit(bytes[i], 16);
        }

        return result;
    }
}
