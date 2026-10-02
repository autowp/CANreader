package com.autowp;

import java.util.Arrays;

/**
 * Created by autow on 31.01.2016.
 */
public class Hex {

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static byte[] hexStringToByteArray(String s) {
        return parseHex(s.toCharArray());
    }

    public static byte[] hexStringToByteArray(byte[] s) {
        char[] chars = new char[s.length];
        for (int i = 0; i < s.length; i++) {
            chars[i] = (char) (s[i] & 0xFF);
        }

        return parseHex(chars);
    }

    private static byte[] parseHex(char[] chars) {
        byte[] data = new byte[(chars.length + 1) / 2];
        int byteCount = 0;
        int highNibble = -1;

        for (char character : chars) {
            if (Character.isWhitespace(character)) {
                continue;
            }

            int nibble = Character.digit(character, 16);
            if (nibble < 0) {
                throw new IllegalArgumentException("Unexpected character `" + character + "`");
            }

            if (highNibble < 0) {
                highNibble = nibble;
            } else {
                data[byteCount++] = (byte) ((highNibble << 4) | nibble);
                highNibble = -1;
            }
        }

        if (highNibble >= 0) {
            throw new IllegalArgumentException("Hex string must contain an even number of digits");
        }

        return Arrays.copyOf(data, byteCount);
    }

    public static String byteArrayToHexString(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static int bytesToInt(byte[] bytes) {
        int result = 0;
        for (int i = 0; i < bytes.length; i++) {
            int nibble = Character.digit((char) (bytes[i] & 0xFF), 16);
            if (nibble < 0) {
                throw new IllegalArgumentException(
                        "Unexpected character `" + (char) (bytes[i] & 0xFF) + "`"
                );
            }

            result <<= 4;
            result += nibble;
        }

        return result;
    }
}
