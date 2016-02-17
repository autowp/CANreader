package com.autowp.can;

import android.os.Bundle;

public class CanMessage {
    public static final String EXTRA_IS_EXTENDED = "is_extended";
    public static final String EXTRA_IS_RTR = "is_rtr";
    public static final String EXTRA_ID = "id";
    public static final String EXTRA_DATA = "data";

    protected int id;
    
    protected byte[] data;

    private boolean rtr = false;
    private boolean extended = false;

    public CanMessage(int id, byte[] data)
    {
        this.id = id;
        this.data = new byte[data.length];
        System.arraycopy(data, 0, this.data, 0, data.length);
    }

    public CanMessage(int id, byte[] data, boolean rtr, boolean extended)
    {
        this.id = id;
        this.data = new byte[data.length];
        System.arraycopy(data, 0, this.data, 0, data.length);

        this.rtr = rtr;
        this.extended = extended;
    }
    
    public int getId()
    {
        return id;
    }
    
    public byte[] getData()
    {
        return data;
    }

    public boolean isExtended()
    {
        return extended;
    }

    public boolean isRTR()
    {
        return rtr;
    }

    public void setExtended(boolean value)
    {
        extended = value;
    }

    public void setRTR(boolean value)
    {
        rtr = value;
    }

    public Bundle toBundle()
    {
        Bundle bundle = new Bundle();

        bundle.putBoolean(EXTRA_IS_EXTENDED, extended);
        bundle.putBoolean(EXTRA_IS_RTR, rtr);
        bundle.putInt(EXTRA_ID, id);
        bundle.putByteArray(EXTRA_DATA, data);

        return bundle;
    }

    public static CanMessage fromBundle(Bundle bundle)
    {
        return new CanMessage(
                bundle.getInt(EXTRA_ID),
                bundle.getByteArray(EXTRA_DATA),
                bundle.getBoolean(EXTRA_IS_RTR),
                bundle.getBoolean(EXTRA_IS_EXTENDED)
        );
    }
}
