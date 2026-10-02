package com.autowp.canreader;

import com.autowp.can.CanMessage;

import java.util.Date;

/**
 * Represents a single traced CAN message with timestamp.
 */
public class TracerMessage {
    private final CanMessage canMessage;
    private final long timestamp;

    public TracerMessage(CanMessage canMessage, long timestamp) {
        this.canMessage = canMessage;
        this.timestamp = timestamp;
    }

    public CanMessage getCanMessage() {
        return canMessage;
    }

    public CanMessage getCanFrame() {
        return canMessage;
    }

    public Date getTimestamp() {
        return new Date(timestamp);
    }

    public long getTimestampMs() {
        return timestamp;
    }
}
