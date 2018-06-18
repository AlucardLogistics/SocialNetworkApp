package com.logistics.alucard.socialnetwork.Models;

public class Messages {

    private String message, type, from, to, message_id;
    private long time;
    private boolean seen;

    public Messages(String message, String type, String from, String to, String message_id, long time, boolean seen) {
        this.message = message;
        this.type = type;
        this.from = from;
        this.to = to;
        this.message_id = message_id;
        this.time = time;
        this.seen = seen;
    }

    public Messages() {
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getMessage_id() {
        return message_id;
    }

    public void setMessage_id(String message_id) {
        this.message_id = message_id;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public boolean isSeen() {
        return seen;
    }

    public void setSeen(boolean seen) {
        this.seen = seen;
    }
}
