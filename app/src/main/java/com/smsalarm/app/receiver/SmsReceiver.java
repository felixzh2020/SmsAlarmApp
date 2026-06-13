package com.smsalarm.app.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import com.smsalarm.app.data.RuleRepository;
import com.smsalarm.app.data.SmsRule;
import com.smsalarm.app.service.AlarmService;

import java.util.List;

/**
 * 监听系统短信广播
 */
public class SmsReceiver extends BroadcastReceiver {

    private static final String TAG = "SmsReceiver";
    private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!SMS_RECEIVED.equals(intent.getAction())) return;

        Bundle bundle = intent.getExtras();
        if (bundle == null) return;

        Object[] pdus = (Object[]) bundle.get("pdus");
        String format = bundle.getString("format");
        if (pdus == null || pdus.length == 0) return;

        // 解析短信（多段合并）
        StringBuilder bodyBuilder = new StringBuilder();
        String sender = null;

        for (Object pdu : pdus) {
            SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu, format);
            if (smsMessage != null) {
                if (sender == null) {
                    sender = smsMessage.getOriginatingAddress();
                }
                bodyBuilder.append(smsMessage.getMessageBody());
            }
        }

        String body = bodyBuilder.toString();
        Log.d(TAG, "收到短信 sender=" + sender + " body=" + body);

        // 匹配规则
        List<SmsRule> rules = RuleRepository.getInstance(context).getEnabledRules();
        for (SmsRule rule : rules) {
            if (rule.matches(sender, body)) {
                Log.d(TAG, "规则匹配: " + rule.getName());
                triggerAlarm(context, rule, sender, body);
                break; // 第一条匹配规则触发后停止（避免重复闹钟）
            }
        }
    }

    private void triggerAlarm(Context context, SmsRule rule, String sender, String body) {
        Intent serviceIntent = new Intent(context, AlarmService.class);
        serviceIntent.putExtra(AlarmService.EXTRA_RULE_ID, rule.getId());
        serviceIntent.putExtra(AlarmService.EXTRA_RULE_NAME, rule.getName());
        serviceIntent.putExtra(AlarmService.EXTRA_SMS_SENDER, sender);
        serviceIntent.putExtra(AlarmService.EXTRA_SMS_BODY, body);
        serviceIntent.putExtra(AlarmService.EXTRA_ALARM_TONE, rule.getAlarmTone());
        serviceIntent.putExtra(AlarmService.EXTRA_VIBRATE, rule.isVibrate());
        serviceIntent.putExtra(AlarmService.EXTRA_MAX_RING_DURATION_MS, rule.getMaxRingDurationMs());
        context.startForegroundService(serviceIntent);
    }
}
