package com.autowp.canreader;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.autowp.can.CanAdapter;
import com.autowp.can.CanAdapterException;
import com.autowp.can.CanFrame;
import com.autowp.can.CanMessage;
import com.autowp.can.adapter.android.CanHackerFelhr;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CanReaderService extends Service
        implements CanAdapter.OnCanFrameTransferListener, CanAdapter.OnCanMessageTransferListener,
            CanAdapter.CanAdapterEventListener {

    private static final String TAG = "CanReaderService";
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public void handleErrorEvent(final CanAdapterException e) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(CanReaderService.this, e.getMessage(),Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void handleConnectionStateChanged(CanAdapter.ConnectionState connection) {
        switch (connection) {
            case DISCONNECTED:
                break;
            case CONNECTING:
                break;
            case CONNECTED:
                if (mSpeedMeterTimerTask == null) {
                    mSpeedMeterTimerTask = new SpeedMeterTimerTask();
                    timer.schedule(mSpeedMeterTimerTask, 0, SPEED_METER_PERIOD);
                }
                break;
            case DISCONNECTING:
                break;
        }

        triggerConnectionStateChanged();
    }

    @Override
    public void handleCanMessageReceivedEvent(CanMessage message) {
        mainHandler.post(() -> receive(message));
    }

    @Override
    public void handleCanMessageSentEvent(CanMessage message) {

    }

    @Override
    public void handleCanFrameReceivedEvent(CanFrame frame) {

    }

    @Override
    public void handleCanFrameSentEvent(CanFrame frame) {

    }

    public interface OnConnectionStateChangedListener {
        void handleConnectedStateChanged(CanAdapter.ConnectionState connection);
    }

    private final List<OnConnectionStateChangedListener> connectionStateChangedListeners = new ArrayList<>();

    private ArrayList<TransmitCanFrame> transmitFrames = new ArrayList<>();

    private ArrayList<MonitorCanMessage> monitorFrames = new ArrayList<>();

    private final List<OnTransmitChangeListener> transmitListeners = new ArrayList<>();

    private final List<OnMonitorChangedListener> monitorListeners = new ArrayList<>();

    private CanAdapter canAdapter;

    private final AtomicInteger sentCount = new AtomicInteger();

    private final AtomicInteger receivedCount = new AtomicInteger();

    private ScheduledExecutorService threadsPool = Executors.newScheduledThreadPool(1);

    private static final int SPEED_METER_PERIOD = 500;
    private SpeedMeterTimerTask mSpeedMeterTimerTask;

    private class UsbBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(intent.getAction())) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    if (canAdapter instanceof CanHackerFelhr) {
                        UsbDevice adapterDevice = ((CanHackerFelhr)canAdapter).getUsbDevice();
                        if (device.equals(adapterDevice)) {
                            setCanAdapter(null);
                        }
                    }
                }
            }

        }
    }

    private UsbBroadcastReceiver mUsbReceiver;

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
        private int previousSentCount = 0;
        private int previousReceivedCount = 0;

        public SpeedMeterTimerTask() {
            this.previousSentCount = sentCount.get();
        }

        public void run() {
            double seconds = (double)SPEED_METER_PERIOD / 1000.0;

            int currentSentCount = sentCount.get();
            double dxSent = currentSentCount - previousSentCount;
            triggerTransmitSpeed(dxSent / seconds);
            this.previousSentCount = currentSentCount;

            int currentReceivedCount = receivedCount.get();
            double dxReceived = currentReceivedCount - previousReceivedCount;
            triggerMonitorSpeed(dxReceived / seconds);
            this.previousReceivedCount = currentReceivedCount;
        }
    }

    private void triggerTransmitSpeed(double speed) {
        synchronized (transmitListeners) {
            for (OnTransmitChangeListener listener : transmitListeners) {
                listener.handleSpeedChanged(speed);
            }
        }
    }

    private void triggerMonitorSpeed(double speed) {
        synchronized (monitorListeners) {
            for (OnMonitorChangedListener listener : monitorListeners) {
                listener.handleSpeedChanged(speed);
            }
        }
    }

    public interface OnTransmitChangeListener {
        void handleTransmitUpdated();
        void handleTransmitUpdated(final TransmitCanFrame frame);
        void handleSpeedChanged(double speed);
    }

    public interface OnMonitorChangedListener {
        void handleMonitorUpdated();

        void handleMonitorUpdated(final MonitorCanMessage message);

        void handleSpeedChanged(double speed);
    }

    private void toast(final String message)
    {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
                toast.show();
            }
        });
    }

    @Override
    public void onDestroy()
    {
        if (mUsbReceiver != null) {
            unregisterReceiver(mUsbReceiver);
            mUsbReceiver = null;
        }
        timer.cancel();
        threadsPool.shutdownNow();
        super.onDestroy();
    }

    public void setCanAdapter(final CanAdapter adapter) {
        stopAllTransmits();

        if (mSpeedMeterTimerTask != null) {
            mSpeedMeterTimerTask.cancel();
            mSpeedMeterTimerTask = null;
        }

        if (mUsbReceiver != null) {
            unregisterReceiver(mUsbReceiver);
            mUsbReceiver = null;
        }

        try {
            Runnable connectRunable = new Runnable() {
                @Override
                public void run() {
                    triggerConnectionStateChanged();

                    try {
                        canAdapter = adapter;

                        if (canAdapter != null) {

                            canAdapter.addEventListener((CanAdapter.OnCanFrameTransferListener) CanReaderService.this);
                            canAdapter.addEventListener((CanAdapter.OnCanMessageTransferListener) CanReaderService.this);
                            canAdapter.addEventListener((CanAdapter.CanAdapterEventListener) CanReaderService.this);

                            canAdapter.connect(new Runnable() {
                                @Override
                                public void run() {
                                    mUsbReceiver = new UsbBroadcastReceiver();
                                    IntentFilter filter = new IntentFilter();
                                    filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
                                    registerReceiver(mUsbReceiver, filter);

                                    triggerConnectionStateChanged();
                                }
                            });
                        }

                    } catch (CanAdapterException e) {
                        canAdapter = null;
                        triggerConnectionStateChanged();
                        Log.e(TAG, "Unable to connect CAN adapter", e);
                        toast(e.getMessage());
                    }
                }
            };

            if (canAdapter != null) {

                canAdapter.removeEventListener((CanAdapter.OnCanFrameTransferListener) this);
                canAdapter.removeEventListener((CanAdapter.OnCanMessageTransferListener) this);
                canAdapter.removeEventListener((CanAdapter.CanAdapterEventListener) this);

                canAdapter.disconnect(connectRunable);
            } else {
                connectRunable.run();
            }

        } catch (CanAdapterException e) {
            Log.e(TAG, "Unable to disconnect CAN adapter", e);
            toast(e.getMessage());
        }


        //
    }

    public void addListener(OnConnectionStateChangedListener listener) {

        synchronized (connectionStateChangedListeners) {
            connectionStateChangedListeners.add(listener);
        }
    }

    public void removeListener(OnConnectionStateChangedListener listener) {

        synchronized (connectionStateChangedListeners) {
            connectionStateChangedListeners.remove(listener);
        }
    }

    public void addListener(OnMonitorChangedListener listener) {

        synchronized (monitorListeners) {
            monitorListeners.add(listener);
        }
    }

    public void removeListener(OnMonitorChangedListener listener) {

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

    public void transmit(TransmitCanFrame frame)
    {
        if (!send(frame.getCanFrame())) {
            return;
        }
        frame.incCount();
        sentCount.incrementAndGet();
        triggerTransmit(frame);
    }

    private void triggerConnectionStateChanged()
    {
        synchronized (connectionStateChangedListeners) {
            CanAdapter.ConnectionState state = getConnectionState();
            for (OnConnectionStateChangedListener listener : connectionStateChangedListeners) {
                listener.handleConnectedStateChanged(state);
            }
        }
    }

    private void triggerMonitor()
    {
        synchronized (monitorListeners) {
            for (OnMonitorChangedListener listener : monitorListeners) {
                listener.handleMonitorUpdated();
            }
        }
    }

    private void triggerMonitor(MonitorCanMessage message)
    {
        synchronized (monitorListeners) {
            for (OnMonitorChangedListener listener : monitorListeners) {
                listener.handleMonitorUpdated(message);
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

    public MonitorCanMessage getMonitorCanMessage(int id)
    {
        for (MonitorCanMessage message : monitorFrames) {
            if (message.getCanMessage().getId() == id) {
                return message;
            }
        }

        return null;
    }

    public void add(final TransmitCanFrame frame)
    {
        transmitFrames.add(frame);
        triggerTransmit();
    }

    public void removeMonitor(int position)
    {
        MonitorCanMessage frame = monitorFrames.get(position);
        removeMonitor(frame);
    }

    public void removeMonitor(MonitorCanMessage frame)
    {
        monitorFrames.remove(frame);
        triggerMonitor();
    }

    public void removeTransmit(int position)
    {
        TransmitCanFrame frame = transmitFrames.get(position);
        removeTransmit(frame);
    }

    public void removeTransmit(TransmitCanFrame frame)
    {
        Future<?> future = frame.getFuture();
        if (future != null) {
            future.cancel(true);
            frame.setFuture(null);
        }
        frame.setEnabled(false);
        transmitFrames.remove(frame);
        triggerTransmit();
    }

    private void receive(CanMessage canMessage)
    {
        receivedCount.incrementAndGet();
        boolean found = false;
        for (MonitorCanMessage monitorFrame : monitorFrames) {
            if (monitorFrame.getCanMessage().getId() == canMessage.getId()) {
                monitorFrame.setCanMessage(canMessage);
                monitorFrame.incCount();
                monitorFrame.addTime(new Date());
                triggerMonitor(monitorFrame);
                found = true;
                break;
            }
        }
        if (!found) {
            MonitorCanMessage monitorFrame = new MonitorCanMessage(canMessage, 0);
            monitorFrame.incCount();
            monitorFrames.add(monitorFrame);
            triggerMonitor(monitorFrame);
        }
        triggerMonitor();

    }

    private boolean send(CanFrame frame)
    {
        CanAdapter adapter = canAdapter;
        if (adapter == null) {
            Log.w(TAG, "Cannot send CAN frame while disconnected");
            return false;
        }
        try {
            adapter.send(frame);
            return true;
        } catch (CanAdapterException e) {
            Log.e(TAG, "Unable to send CAN frame", e);
            return false;
        }
    }

    public void startTransmit(TransmitCanFrame frame)
    {
        if (frame.getFuture() == null) {
            if (frame.getPeriod() <= 0) {
                if (frame.isEnabled()) {
                    frame.setEnabled(false);
                    triggerTransmit(frame);
                }
                return;
            }
            TransmitRunnable runnable = new TransmitRunnable(frame);
            Future<?> future = threadsPool.scheduleWithFixedDelay(runnable, 0, frame.getPeriod(), TimeUnit.MILLISECONDS);

            frame.setFuture(future);
            frame.setEnabled(true);
            triggerTransmit(frame);
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
        if (future != null) {
            future.cancel(true);
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

    public CanAdapter.ConnectionState getConnectionState()
    {
        if (canAdapter == null) {
            return CanAdapter.ConnectionState.DISCONNECTED;
        }
        return canAdapter.getConnectionState();
    }

    public void resetTransmit(final TransmitCanFrame frame) {
        frame.resetCount();
        triggerTransmit(frame);
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

    public void setTransmitFrames(List<TransmitCanFrame> list) {
        clearTransmits();
        for (TransmitCanFrame frame : list) {
            add(frame);
        }
    }

}
