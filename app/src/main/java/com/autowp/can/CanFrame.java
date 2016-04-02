package com.autowp.can;

import android.os.Bundle;

import com.autowp.Hex;

public class CanFrame {
    public static final String EXTRA_IS_EXTENDED = "is_extended";
    public static final String EXTRA_IS_RTR = "is_rtr";
    public static final String EXTRA_ID = "id";
    public static final String EXTRA_DATA = "data";
    public static final String EXTRA_DLC = "dlc";

    public static final int MIN_DLC = 1;
    public static final int MAX_DLC = 8;

    public static final int MAX_ID_11BIT = 0x7FF;
    public static final int MAX_ID_29BIT = 0x1FFFFFFF;

    private int id;

    private byte[] data;

    private byte dlc;

    private boolean rtr = false;
    private boolean extended = false;

    /**
     * Data frame
     *
     * @param id
     * @param data
     * @param ext
     */
    public CanFrame(int id, byte data[], boolean ext) throws CanFrameException {
        assertId(id, ext);
        assertDlc((byte) data.length);

        this.id = id;
        this.rtr = false;
        this.extended = ext;

        this.data = new byte[data.length];
        System.arraycopy(data, 0, this.data, 0, data.length);
    }

    /**
     * RTR frame
     *
     * @param id
     * @param dlc
     * @param ext
     */
    public CanFrame(int id, byte dlc, boolean ext) throws CanFrameException {
        assertId(id, ext);
        assertDlc(dlc);

        this.id = id;
        this.rtr = true;
        this.extended = ext;
        this.data = null;
        this.dlc = dlc;
    }

    private static void assertId(int id, boolean ext) throws CanFrameException {
        int max = ext ? MAX_ID_29BIT : MAX_ID_11BIT;
        if (id > max || id < 0) {
            throw new CanFrameException(
                    String.format("Frame id must be between %X and %X", 0, max)
            );
        }
    }

    private static void assertDlc(byte dlc) throws CanFrameException {
        if (dlc > MAX_DLC || dlc < MIN_DLC) {
            throw new CanFrameException(
                    String.format("Frame dlc must be between %X and %X", MIN_DLC, MAX_DLC)
            );
        }
    }

    public int getId() {
        return this.id;
    }

    public boolean isExtended() {
        return extended;
    }

    public boolean isRTR() {
        return rtr;
    }

    public byte[] getData() {
        return this.data;
    }

    public void setExtended(boolean value) {
        extended = value;
    }

    public void setRTR(boolean value) {
        rtr = value;
    }

    public String toString() {
        String idHex = String.format(extended ? "%08X" : "%03X", this.id);

        String dataStr;
        if (rtr) {
            dataStr = "RTR " + getDLC();
        } else {
            dataStr = Hex.byteArrayToHexString(this.data);
        }

        return idHex + " " + dataStr;
    }

    public Bundle toBundle() {
        Bundle bundle = new Bundle();

        bundle.putBoolean(EXTRA_IS_EXTENDED, extended);
        bundle.putBoolean(EXTRA_IS_RTR, rtr);
        bundle.putInt(EXTRA_ID, id);
        if (rtr) {
            bundle.putByte(EXTRA_DLC, dlc);
        } else {
            bundle.putByteArray(EXTRA_DATA, data);
        }

        return bundle;
    }

    public byte getDLC() {
        return rtr ? dlc : (byte) data.length;
    }

    public static CanFrame fromBundle(Bundle bundle) throws CanFrameException {
        boolean rtr = bundle.getBoolean(EXTRA_IS_RTR);
        if (rtr) {
            return new CanFrame(
                    bundle.getInt(EXTRA_ID),
                    bundle.getByte(EXTRA_DLC),
                    bundle.getBoolean(EXTRA_IS_EXTENDED)
            );
        } else {
            return new CanFrame(
                    bundle.getInt(EXTRA_ID),
                    bundle.getByteArray(EXTRA_DATA),
                    bundle.getBoolean(EXTRA_IS_EXTENDED)
            );
        }
    }
}
