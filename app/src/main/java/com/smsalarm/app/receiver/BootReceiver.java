package com.smsalarm.app.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * 开机自启广播接收器（保持 App 监听能力）
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            Log.d(TAG, "设备启动完成，SmsReceiver 会由系统自动激活");
            // SmsReceiver 是静态注册，系统启动后会自动注册
            // 此处可做其他初始化工作（如重建通知渠道）
        }
    }
}
