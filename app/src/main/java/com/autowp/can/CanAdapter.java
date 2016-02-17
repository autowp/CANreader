package com.autowp.can;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;


public abstract class CanAdapter {
    public abstract void send(CanFrame message) throws CanAdapterException;
    
    public abstract void connect() throws CanAdapterException;
    
    public abstract void disconnect();
    
    public abstract boolean isConnected();
    
    public interface CanAdapterEventListener {
        void handleCanFrameReceivedEvent(CanFrameEvent e);
        
        void handleCanFrameSentEvent(CanFrameEvent e);
        
        void handleErrorEvent(CanAdapterException e);
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
    
    private List<CanAdapterEventListener> canFrameEventListeners = new ArrayList<>();
    
    protected CanBusSpecs specs;
    
    public synchronized void addEventListener(CanAdapterEventListener listener) 
    {
        canFrameEventListeners.add(listener);
    }
    
    public synchronized void removeEventListener(CanAdapterEventListener listener)
    {
        canFrameEventListeners.remove(listener);
    }
    
    protected synchronized void fireFrameSentEvent(CanFrame frame)
    {
        CanFrameEvent event = new CanFrameEvent(this, frame);
        for (CanAdapterEventListener canFrameEventListener : canFrameEventListeners) {
            canFrameEventListener.handleCanFrameSentEvent(event);
        }
    }
    
    protected synchronized void fireFrameReceivedEvent(CanFrame frame)
    {
        CanFrameEvent event = new CanFrameEvent(this, frame);
        for (CanAdapterEventListener canFrameEventListener : canFrameEventListeners) {
            canFrameEventListener.handleCanFrameReceivedEvent(event);
        }
    }
    
    protected synchronized void fireErrorEvent(CanAdapterException e)
    {
        for (CanAdapterEventListener canFrameEventListener : canFrameEventListeners) {
            canFrameEventListener.handleErrorEvent(e);
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
