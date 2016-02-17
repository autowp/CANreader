package com.autowp.canreader;

import android.os.Bundle;

import com.autowp.can.CanFrame;
import com.autowp.can.CanFrameException;

import java.util.TimerTask;
import java.util.concurrent.Future;

/**
 * Created by autow on 31.01.2016.
 */
public class TransmitCanFrame {

    public static final String EXTRA_CAN_FRAME = "can_frame";
    public static final String EXTRA_PERIOD = "period";

    private CanFrame canFrame;

    private int period = 0;

    private int count = 0;

    private boolean enabled = false;

    TimerTask timerTask;
    private Future<?> future;

    public TransmitCanFrame(CanFrame canFrame, int period)
    {
        this.canFrame = canFrame;
        this.period = period;
        this.count = 0;
    }

    public int getPeriod()
    {
        return period;
    }

    public CanFrame getCanFrame()
    {
        return canFrame;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean value) {
        enabled = value;
    }

    public int getCount()
    {
        return count;
    }

    public Bundle toBundle()
    {
        Bundle bundle = new Bundle();

        bundle.putBundle(EXTRA_CAN_FRAME, canFrame.toBundle());
        bundle.putInt(EXTRA_PERIOD, period);

        return bundle;
    }

    public static TransmitCanFrame fromBundle(Bundle bundle) throws CanFrameException {
        CanFrame canFrame = CanFrame.fromBundle(bundle.getBundle(EXTRA_CAN_FRAME));

        return new TransmitCanFrame(canFrame, bundle.getInt(EXTRA_PERIOD));
    }

    public void setFromBundle(Bundle bundle) throws CanFrameException {
        canFrame = CanFrame.fromBundle(bundle.getBundle(EXTRA_CAN_FRAME));

        period = bundle.getInt(EXTRA_PERIOD);
    }

    public TimerTask getTimerTask()
    {
        return timerTask;
    }

    public void setTimerTask(TimerTask value)
    {
        timerTask = value;
    }

    public void incCount()
    {
        count++;
    }

    public void resetCount() {
        count = 0;
    }

    public void setFuture(Future<?> future) {
        this.future = future;
    }

    public Future<?> getFuture() {
        return future;
    }
}
