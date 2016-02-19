package com.autowp.canreader;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

import com.autowp.can.CanAdapter;
import com.autowp.can.CanBusSpecs;
import com.autowp.can.CanClient;
import com.autowp.can.CanClientException;
import com.autowp.can.CanFrame;
import com.autowp.can.CanMessage;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CanReaderService extends Service {

    private ArrayList<TransmitCanFrame> transmitFrames = new ArrayList<>();

    private ArrayList<MonitorCanMessage> monitorFrames = new ArrayList<>();

    private final List<OnStateChangeListener> stateChangeListeners = new ArrayList<>();

    private final List<OnTransmitChangeListener> transmitListeners = new ArrayList<>();

    private final List<OnMonitorChangeListener> monitorListeners = new ArrayList<>();

    private final CanBusSpecs canBusSpecs;

    private final CanClient canClient;

    private int sentCount = 0;

    private ScheduledExecutorService threadsPool = Executors.newScheduledThreadPool(1);

    private static final int SPEED_METER_PERIOD = 500;
    private SpeedMeterTimerTask mSpeedMeterTimerTask;

    private class TransmitRunnable implements Runnable {

        private TransmitCanFrame frame;

        public TransmitRunnable(TransmitCanFrame frame) {
            this.frame = frame;
        }

        @Override
        public void run() {
            if (frame.isEnabled()) {
                transmit(frame);
            }
        }
    }

    private class SpeedMeterTimerTask extends TimerTask {
        private int previousCount = 0;

        public SpeedMeterTimerTask() {
            this.previousCount = sentCount;
        }

        public void run() {
            double seconds = (double)SPEED_METER_PERIOD / 1000.0;
            double dx = sentCount - previousCount;
            triggerTransmitSpeed(dx / seconds);
            this.previousCount = sentCount;
        }
    }

    private void triggerTransmitSpeed(double speed) {
        synchronized (stateChangeListeners) {
            for (OnTransmitChangeListener listener : transmitListeners) {
                listener.handleSpeedChanged(speed);
            }
        }
    }

    public interface OnStateChangeListener {
        void handleStateChanged();
    }

    public interface OnTransmitChangeListener {
        void handleTransmitUpdated();
        void handleTransmitUpdated(final TransmitCanFrame frame);
        void handleSpeedChanged(double speed);
    }

    public interface OnMonitorChangeListener {
        void handleMonitorUpdated();

        void handleMonitorUpdated(final MonitorCanMessage message);
    }

    public void setCanAdapter(CanAdapter adapter) {
        System.out.print("setCanAdapter ");
        System.out.println(adapter);
        stopAllTransmits();
        try {
            canClient.disconnect();
            canClient.setAdapter(adapter);
        } catch (CanClientException e) {
            e.printStackTrace();

            Toast toast = Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT);
            toast.show();
        }


        if (mSpeedMeterTimerTask != null) {
            mSpeedMeterTimerTask.cancel();
            mSpeedMeterTimerTask = null;
        }

        if (adapter != null) {

            canClient.addEventListener(new CanClient.OnCanMessageTransferListener() {
                @Override
                public void handleCanMessageReceivedEvent(CanMessage message) {
                    receive(message);
                }

                @Override
                public void handleCanMessageSentEvent(CanMessage message) {

                }
            });

            canClient.addEventListener(new CanClient.OnCanClientErrorListener() {
                @Override
                public void handleErrorEvent(CanClientException e) {
                    System.out.println(e.getMessage());
                }
            });

            try {
                System.out.println("Connect canClient");
                canClient.connect();
            } catch (CanClientException e) {
                e.printStackTrace();

                Toast toast = Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT);
                toast.show();
            }

            mSpeedMeterTimerTask = new SpeedMeterTimerTask();
            timer.schedule(mSpeedMeterTimerTask, 0, SPEED_METER_PERIOD);
        }
        triggerStateChanged();
    }

    public void addListener(OnStateChangeListener listener) {

        synchronized (stateChangeListeners) {
            stateChangeListeners.add(listener);
        }
    }

    public void removeListener(OnStateChangeListener listener) {

        synchronized (stateChangeListeners) {
            stateChangeListeners.remove(listener);
        }
    }

    public void addListener(OnMonitorChangeListener listener) {

        synchronized (monitorListeners) {
            monitorListeners.add(listener);
        }
    }

    public void removeListener(OnMonitorChangeListener listener) {

        synchronized (monitorListeners) {
            monitorListeners.remove(listener);
        }
    }

    public void addListener(OnTransmitChangeListener listener) {

        synchronized (transmitListeners) {
            transmitListeners.add(listener);
        }
    }

    public void removeListener(OnTransmitChangeListener listener) {

        synchronized (transmitListeners) {
            transmitListeners.remove(listener);
        }
    }

    TransferServiceBinder binder = new TransferServiceBinder();

    Timer timer = new Timer();

    /*private class TransmitTimerTask extends TimerTask {
        private TransmitCanFrame frame;
        public TransmitTimerTask(TransmitCanFrame frame) {
            this.frame = frame;
        }

        public void run() {
            transmit(frame);
        }
    }*/

    public CanReaderService() {
        canBusSpecs = new CanBusSpecs();
        this.canClient = new CanClient(canBusSpecs);
    }

    public void transmit(TransmitCanFrame frame)
    {
        send(frame.getCanFrame());
        frame.incCount();
        sentCount++;
        triggerTransmit(frame);
    }

    private void triggerStateChanged()
    {
        synchronized (stateChangeListeners) {
            for (OnStateChangeListener listener : stateChangeListeners) {
                listener.handleStateChanged();
            }
        }
    }

    private void triggerMonitor()
    {
        synchronized (transmitListeners) {
            for (OnMonitorChangeListener listener : monitorListeners) {
                listener.handleMonitorUpdated();
            }
        }
    }

    private void triggerTransmit()
    {
        synchronized (transmitListeners) {
            for (OnTransmitChangeListener listener : transmitListeners) {
                listener.handleTransmitUpdated();
            }
        }
    }

    private void triggerTransmit(TransmitCanFrame frame) {
        synchronized (transmitListeners) {
            for (OnTransmitChangeListener listener : transmitListeners) {
                listener.handleTransmitUpdated(frame);
            }
        }
    }

    public IBinder onBind(Intent intent) {
        return binder;
    }

    class TransferServiceBinder extends Binder {
        CanReaderService getService() {
            return CanReaderService.this;
        }
    }

    public ArrayList<TransmitCanFrame> getTransmitFrames()
    {
        return transmitFrames;
    }

    public ArrayList<MonitorCanMessage> getMonitorFrames()
    {
        return monitorFrames;
    }

    public void add(final TransmitCanFrame frame)
    {
        transmitFrames.add(frame);
        triggerTransmit();
    }

    public void remove(int position)
    {
        TransmitCanFrame frame = transmitFrames.get(position);
        remove(frame);
    }

    public void remove(TransmitCanFrame frame)
    {
        TimerTask tt = frame.getTimerTask();
        if (tt != null) {
            tt.cancel();
            frame.setTimerTask(null);
        }
        transmitFrames.remove(frame);
        triggerTransmit();
    }

    private void receive(CanMessage canMessage)
    {
        boolean found = false;
        for (MonitorCanMessage monitorFrame : monitorFrames) {
            if (monitorFrame.getCanMessage().getId() == canMessage.getId()) {
                monitorFrame.setCanMessage(canMessage);
                monitorFrame.incCount();
                monitorFrame.setTime(new Date());
                found = true;
                break;
            }
        }
        if (!found) {
            MonitorCanMessage monitorFrame = new MonitorCanMessage(canMessage, 0);
            monitorFrame.incCount();
            monitorFrames.add(monitorFrame);
        }
        triggerMonitor();
    }

    private void send(CanFrame frame)
    {
        try {
            canClient.send(frame);
        } catch (CanClientException e) {
            e.printStackTrace();
        }
        //receive(frame); // TODO: loopback stub
    }

    public void startTransmit(TransmitCanFrame frame)
    {
        TimerTask tTask = frame.getTimerTask();
        if (tTask == null) {
            if (frame.getPeriod() > 0) {
                TransmitRunnable runnable = new TransmitRunnable(frame);
                Future<?> future = threadsPool.scheduleWithFixedDelay(runnable, 0, frame.getPeriod(), TimeUnit.MILLISECONDS);

                frame.setFuture(future);
                frame.setEnabled(true);
                triggerTransmit(frame);
            }
        }
    }

    public void startAllTransmits()
    {
        for (TransmitCanFrame frame : transmitFrames) {
            frame.setEnabled(true);
            startTransmit(frame);
        }
    }

    public void stopTransmit(TransmitCanFrame frame)
    {
        Future<?> future = frame.getFuture();
        //TimerTask tt = frame.getTimerTask();
        if (future != null) {
            future.cancel(true);
            //future.cancel(false);
            frame.setFuture(null);
        }
        if (frame.isEnabled()) {
            frame.setEnabled(false);
            triggerTransmit(frame);
        }
    }

    public void stopAllTransmits()
    {
        for (TransmitCanFrame frame : transmitFrames) {
            stopTransmit(frame);
        }
    }

    public void clearTransmits()
    {
        stopAllTransmits();
        transmitFrames.clear();
        triggerTransmit();
    }

    public void clearMonitor()
    {
        monitorFrames.clear();
        triggerMonitor();
    }

    public boolean isConnected()
    {
        return canClient.isConnected();
    }

    public void setSpeed(int speed) {
        canBusSpecs.setSpeed(speed);
    }

    public void resetTransmits() {
        for (TransmitCanFrame frame : transmitFrames) {
            frame.resetCount();
            triggerTransmit(frame);
        }
    }

    public boolean hasStartedTransmits()
    {
        for (TransmitCanFrame frame : transmitFrames) {
            if (frame.isEnabled()) {
                return true;
            }
        }

        return false;
    }

    public boolean hasStoppedTransmits()
    {
        for (TransmitCanFrame frame : transmitFrames) {
            if (!frame.isEnabled()) {
                return true;
            }
        }

        return false;
    }
}
