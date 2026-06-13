package com.smsalarm.app.data;

import java.util.UUID;

/**
 * 短信监听规则
 */
public class SmsRule {

    private String id;
    private String name;            // 规则名称（用于显示）
    private MatchType matchType;    // 匹配类型
    private String senderPattern;  // 发信人匹配关键词（支持部分匹配）
    private String contentPattern; // 消息内容匹配关键词（支持部分匹配）
    private boolean enabled;        // 是否启用
    private boolean useRegex;       // 是否使用正则表达式匹配
    private String alarmTone;       // 铃声 URI，null 表示默认
    private boolean vibrate;        // 是否震动
    private long createTime;
    // 最大响铃时长（毫秒），0 表示使用默认 5 分钟
    private int maxRingDurationMs = 0;

    public SmsRule() {
        this.id = UUID.randomUUID().toString();
        this.enabled = true;
        this.useRegex = true;
        this.vibrate = true;
        this.matchType = MatchType.SENDER;
        this.createTime = System.currentTimeMillis();
    }

    // ---- Getters & Setters ----

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public MatchType getMatchType() { return matchType; }
    public void setMatchType(MatchType matchType) { this.matchType = matchType; }

    public String getSenderPattern() { return senderPattern; }
    public void setSenderPattern(String senderPattern) { this.senderPattern = senderPattern; }

    public String getContentPattern() { return contentPattern; }
    public void setContentPattern(String contentPattern) { this.contentPattern = contentPattern; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isUseRegex() { return useRegex; }
    public void setUseRegex(boolean useRegex) { this.useRegex = useRegex; }

    public String getAlarmTone() { return alarmTone; }
    public void setAlarmTone(String alarmTone) { this.alarmTone = alarmTone; }

    public boolean isVibrate() { return vibrate; }
    public void setVibrate(boolean vibrate) { this.vibrate = vibrate; }

    public int getMaxRingDurationMs() { return maxRingDurationMs; }
    public void setMaxRingDurationMs(int maxRingDurationMs) { this.maxRingDurationMs = maxRingDurationMs; }

    public long getCreateTime() { return createTime; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }

    /**
     * 判断该规则是否与给定短信匹配
     */
    public boolean matches(String sender, String body) {
        if (!enabled) return false;

        return switch (matchType) {
            case SENDER -> matchesSender(sender);
            case CONTENT -> matchesContent(body);
            case BOTH -> matchesSender(sender) && matchesContent(body);
            default -> false;
        };
    }

    private boolean matchesSender(String sender) {
        if (senderPattern == null || senderPattern.isEmpty()) return false;
        if (useRegex) {
            try {
                return sender != null && sender.matches(".*" + senderPattern + ".*");
            } catch (Exception e) {
                return false;
            }
        }
        return sender != null && sender.toLowerCase().contains(senderPattern.toLowerCase());
    }

    private boolean matchesContent(String body) {
        if (contentPattern == null || contentPattern.isEmpty()) return false;
        if (useRegex) {
            try {
                return body != null && body.matches(".*" + contentPattern + ".*");
            } catch (Exception e) {
                return false;
            }
        }
        return body != null && body.toLowerCase().contains(contentPattern.toLowerCase());
    }

    @Override
    public String toString() {
        return "SmsRule{id='" + id + "', name='" + name + "', enabled=" + enabled + "}";
    }
}
