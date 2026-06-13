package com.smsalarm.app.data;

/**
 * 匹配规则类型
 */
public enum MatchType {
    SENDER,     // 匹配发信人（手机号或名称）
    CONTENT,    // 匹配消息内容
    BOTH        // 同时匹配发信人和内容（AND 逻辑）
}
