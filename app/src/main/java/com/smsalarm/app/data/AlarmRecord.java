package com.smsalarm.app.data;

import java.util.UUID;

/**
 * 闹钟触发记录
 */
public class AlarmRecord {

    private String id;
    private String ruleName;
    private String sender;
    private String body;
    private long triggerTime;

    public AlarmRecord() {
        this.id = UUID.randomUUID().toString();
        this.triggerTime = System.currentTimeMillis();
    }

    public AlarmRecord(String ruleName, String sender, String body) {
        this.id = UUID.randomUUID().toString();
        this.ruleName = ruleName;
        this.sender = sender;
        this.body = body;
        this.triggerTime = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public long getTriggerTime() { return triggerTime; }
    public void setTriggerTime(long triggerTime) { this.triggerTime = triggerTime; }
}
