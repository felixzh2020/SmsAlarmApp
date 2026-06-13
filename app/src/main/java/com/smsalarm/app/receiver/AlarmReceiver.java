package com.smsalarm.app.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.smsalarm.app.service.AlarmService;

/**
 * AlarmManager 定时触发接收器（备用）
 */
public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String ruleId = intent.getStringExtra(AlarmService.EXTRA_RULE_ID);
        String ruleName = intent.getStringExtra(AlarmService.EXTRA_RULE_NAME);
        String smsSender = intent.getStringExtra(AlarmService.EXTRA_SMS_SENDER);
        String smsBody = intent.getStringExtra(AlarmService.EXTRA_SMS_BODY);
        String alarmTone = intent.getStringExtra(AlarmService.EXTRA_ALARM_TONE);
        boolean vibrate = intent.getBooleanExtra(AlarmService.EXTRA_VIBRATE, true);

        Intent serviceIntent = new Intent(context, AlarmService.class);
        serviceIntent.putExtra(AlarmService.EXTRA_RULE_ID, ruleId);
        serviceIntent.putExtra(AlarmService.EXTRA_RULE_NAME, ruleName);
        serviceIntent.putExtra(AlarmService.EXTRA_SMS_SENDER, smsSender);
        serviceIntent.putExtra(AlarmService.EXTRA_SMS_BODY, smsBody);
        serviceIntent.putExtra(AlarmService.EXTRA_ALARM_TONE, alarmTone);
        serviceIntent.putExtra(AlarmService.EXTRA_VIBRATE, vibrate);
        context.startForegroundService(serviceIntent);
    }
}
