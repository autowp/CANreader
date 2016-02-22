package com.autowp.can;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;


public abstract class CanAdapter {
    protected CanClient.ConnectionState connectionState = CanClient.ConnectionState.DISCONNECTED;

    public abstract void send(CanFrame message) throws CanAdapterException;
    
    public abstract void connect(final Runnable callback) throws CanAdapterException;
    
    public abstract void disconnect(final Runnable callback) throws CanAdapterException;
    
    public boolean isConnected() {
        return connectionState == CanClient.ConnectionState.CONNECTED;
    }
    
    public interface CanAdapterEventListener {
        void handleCanFrameReceivedEvent(CanFrameEvent e);
        
        void handleCanFrameSentEvent(CanFrameEvent e);
        
        void handleErrorEvent(CanAdapterException e);

        void handleConnectionStateChanged(CanClient.ConnectionState connection);
    }
    
    @SuppressWarnings("serial")
    public class CanFrameEvent extends EventObject {
        protected CanFrame frame;

        public CanFrameEvent(Object source, CanFrame frame) {
            super(source);
            this.frame = frame;
        }
        
        public CanFrame getFrame() {
            return frame;
        }
    }
    
    private List<CanAdapterEventListener> canAdapterEventListeners = new ArrayList<>();
    
    protected CanBusSpecs specs;
    
    public synchronized void addEventListener(CanAdapterEventListener listener) 
    {
        canAdapterEventListeners.add(listener);
    }
    
    public synchronized void removeEventListener(CanAdapterEventListener listener)
    {
        canAdapterEventListeners.remove(listener);
    }
    
    protected synchronized void fireFrameSentEvent(CanFrame frame)
    {
        CanFrameEvent event = new CanFrameEvent(this, frame);
        for (CanAdapterEventListener canFrameEventListener : canAdapterEventListeners) {
            canFrameEventListener.handleCanFrameSentEvent(event);
        }
    }
    
    protected synchronized void fireFrameReceivedEvent(CanFrame frame)
    {
        CanFrameEvent event = new CanFrameEvent(this, frame);
        for (CanAdapterEventListener canFrameEventListener : canAdapterEventListeners) {
            canFrameEventListener.handleCanFrameReceivedEvent(event);
        }
    }
    
    protected synchronized void fireErrorEvent(CanAdapterException e)
    {
        for (CanAdapterEventListener canFrameEventListener : canAdapterEventListeners) {
            canFrameEventListener.handleErrorEvent(e);
        }
    }

    protected synchronized void fireConnectionChangedEvent()
    {
        for (CanAdapterEventListener canFrameEventListener : canAdapterEventListeners) {
            canFrameEventListener.handleConnectionStateChanged(connectionState);
        }
    }
    
    public void setBusSpecs(CanBusSpecs specs)
    {
        this.specs = specs;
    }

    public void receive(CanFrame frame) {
        fireFrameReceivedEvent(frame);
    }
}
