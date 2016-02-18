package com.autowp.canreader;

import android.os.Bundle;

import com.autowp.can.CanFrame;
import com.autowp.can.CanMessage;

import java.util.Date;

/**
 * Created by autow on 31.01.2016.
 */
public class MonitorCanMessage {

    public static final String EXTRA_PERIOD = "period";
    public static final String EXTRA_CAN_FRAME = "can_frame";
    public static final int CHANGE_HIGHLIGHT_PERIOD = 3000;
    private Date time;
    private int period = 0;
    private int count = 0;

    private CanMessage canMessage;

    private ChangeHolder[] changes = new ChangeHolder[8];

    public void setTime(Date time) {
        if (this.time != null) {
            period = (int) (time.getTime() - this.time.getTime());
        }
        this.time = time;
    }

    public class ChangeHolder {
        private Date time;
        private byte oldValue;

        public void triggerChange(byte oldValue)
        {
            this.oldValue = oldValue;
            time = new Date();
        }

        public void clearChange()
        {
            oldValue = 0;
            time = null;
        }

        public boolean isHighlight()
        {
            if (time == null) {
                return false;
            }

            Date now = new Date();
            return now.getTime() - time.getTime() < CHANGE_HIGHLIGHT_PERIOD;
        }
    }




    public MonitorCanMessage(final CanMessage canMessage, final int period)
    {
        this.canMessage = canMessage;
        this.period = period;
        this.count = 0;

        for (int i = 0; i< CanFrame.MAX_DLC; i++) {
            changes[i] = new ChangeHolder();
        }
    }

    public int getPeriod()
    {
        return period;
    }

    public CanMessage getCanMessage()
    {
        return canMessage;
    }

    public void setCanMessage(final CanMessage frame)
    {
        if (canMessage != null && frame != null && !canMessage.isRTR() && !frame.isRTR()) {
            // compare and save difference info
            byte[] aData = canMessage.getData();
            byte[] bData = frame.getData();
            for (int i=0; i<CanFrame.MAX_DLC; i++) {
                if (i < aData.length && i < bData.length) {
                    byte a = aData[i];
                    byte b = bData[i];
                    if (a != b) {
                        changes[i].triggerChange(a);
                    }
                } else {
                    changes[i].clearChange();
                }
            }
        }
        canMessage = frame;
    }
    public ChangeHolder getChangeHolder(int i)
    {
        return changes[i];
    }

    public int getCount()
    {
        return count;
    }

    public Bundle toBundle()
    {
        Bundle bundle = new Bundle();

        bundle.putBundle(EXTRA_CAN_FRAME, canMessage.toBundle());
        bundle.putInt(EXTRA_PERIOD, period);

        return bundle;
    }

    public static MonitorCanMessage fromBundle(Bundle bundle)
    {
        CanMessage canFrame = CanMessage.fromBundle(bundle.getBundle(EXTRA_CAN_FRAME));

        return new MonitorCanMessage(canFrame, bundle.getInt(EXTRA_PERIOD));
    }

    public void setFromBundle(Bundle bundle)
    {
        canMessage = CanMessage.fromBundle(bundle.getBundle(EXTRA_CAN_FRAME));

        period = bundle.getInt(EXTRA_PERIOD);
    }

    public void incCount()
    {
        count++;
    }
}
