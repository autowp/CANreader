package com.autowp;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class HexTest {

    @Test
    public void parsesHexStringWithWhitespace() throws Exception {
        assertArrayEquals(new byte[] {0x0A, (byte) 0xFF}, Hex.hexStringToByteArray(" 0a FF\t"));
    }

    @Test
    public void parsesAsciiHexBytes() throws Exception {
        assertArrayEquals(new byte[] {0x0A, (byte) 0xFF},
                Hex.hexStringToByteArray(new byte[] {'0', 'A', 'F', 'F'}));
    }

    @Test
    public void formatsBytesAsUppercaseHex() {
        assertEquals("0AFF", Hex.byteArrayToHexString(new byte[] {0x0A, (byte) 0xFF}));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsOddNumberOfHexDigits() throws Exception {
        Hex.hexStringToByteArray("ABC ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonHexCharacters() throws Exception {
        Hex.hexStringToByteArray("0G");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonHexCharactersWhenParsingInteger() {
        Hex.bytesToInt(new byte[] {'1', 'G'});
    }
}