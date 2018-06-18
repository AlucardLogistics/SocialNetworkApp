package com.logistics.alucard.socialnetwork.Models;

public class AppLogs {

    private String log, log_id;
    private long time;

    public AppLogs(String log, String log_id, long time) {
        this.log = log;
        this.log_id = log_id;
        this.time = time;
    }

    public AppLogs() {
    }

    public String getLog() {
        return log;
    }

    public void setLog(String log) {
        this.log = log;
    }

    public String getLog_id() {
        return log_id;
    }

    public void setLog_id(String log_id) {
        this.log_id = log_id;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
