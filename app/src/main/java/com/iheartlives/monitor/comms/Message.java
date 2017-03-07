package com.iheartlives.monitor.comms;

/**
 * The data class that represents a "frame" in our WS protocol
 */

public final class Message {
    //public long timestamp;
    public final String type;
    public final String message;

    public Message(String type, String message) {
        this.type = type;
        this.message = message;
    }

    public static Message make(String type, String message) {
        return new Message(type, message);
    }
}
