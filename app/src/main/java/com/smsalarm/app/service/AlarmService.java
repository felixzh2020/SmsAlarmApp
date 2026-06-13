package com.smsalarm.app.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.smsalarm.app.R;
import com.smsalarm.app.ui.AlarmRingActivity;

/**
 * 前台服务：播放闹钟铃声 + 震动
 * 支持最大响铃时长保护，超时自动停止
 */
public class AlarmService extends Service {

    private static final String TAG = "AlarmService";
    private static final String CHANNEL_ID = "sms_alarm_channel";
    private static final int NOTIFICATION_ID = 1001;

    // 默认最大响铃时长：5 分钟
    public static final int DEFAULT_MAX_RING_DURATION_MS = 5 * 60 * 1000;

    // 广播：每秒通知 AlarmRingActivity 更新倒计时
    public static final String ACTION_TICK = "com.smsalarm.app.ALARM_TICK";
    public static final String EXTRA_REMAINING_MS = "remaining_ms";

    public static final String EXTRA_RULE_ID = "rule_id";
    public static final String EXTRA_RULE_NAME = "rule_name";
    public static final String EXTRA_SMS_SENDER = "sms_sender";
    public static final String EXTRA_SMS_BODY = "sms_body";
    public static final String EXTRA_ALARM_TONE = "alarm_tone";
    public static final String EXTRA_VIBRATE = "vibrate";
    public static final String EXTRA_MAX_RING_DURATION_MS = "max_ring_duration_ms";

    public static final String ACTION_STOP = "com.smsalarm.app.STOP_ALARM";

    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;

    // 倒计时控制
    private Handler tickHandler;
    private long ringStartTime = 0;
    private long maxRingDurationMs = DEFAULT_MAX_RING_DURATION_MS;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopAlarm();
            return START_NOT_STICKY;
        }

        String ruleName  = intent != null ? intent.getStringExtra(EXTRA_RULE_NAME) : "短信闹钟";
        String smsSender = intent != null ? intent.getStringExtra(EXTRA_SMS_SENDER) : "";
        String smsBody   = intent != null ? intent.getStringExtra(EXTRA_SMS_BODY) : "";
        String alarmToneUri = intent != null ? intent.getStringExtra(EXTRA_ALARM_TONE) : null;
        boolean vibrate = intent == null || intent.getBooleanExtra(EXTRA_VIBRATE, true);

        // 读取最大响铃时长（规则里配置的，0 表示用默认）
        int ruleDuration = intent != null ? intent.getIntExtra(EXTRA_MAX_RING_DURATION_MS, 0) : 0;
        maxRingDurationMs = (ruleDuration > 0) ? ruleDuration : DEFAULT_MAX_RING_DURATION_MS;

        // 启动前台通知
        Notification notification = buildNotification(ruleName, smsSender, smsBody);
        startForeground(NOTIFICATION_ID, notification);

        // 播放铃声
        playAlarmTone(alarmToneUri);

        // 震动
        if (vibrate) {
            startVibration();
        }

        // 启动响铃界面
        Intent ringIntent = new Intent(this, AlarmRingActivity.class);
        ringIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        ringIntent.putExtra(EXTRA_RULE_NAME, ruleName);
        ringIntent.putExtra(EXTRA_SMS_SENDER, smsSender);
        ringIntent.putExtra(EXTRA_SMS_BODY, smsBody);
        ringIntent.putExtra(EXTRA_MAX_RING_DURATION_MS, (int) maxRingDurationMs);
        startActivity(ringIntent);

        // 启动倒计时（每秒广播一次剩余时间）
        startCountdown();

        return START_STICKY;
    }

    private void startCountdown() {
        ringStartTime = System.currentTimeMillis();
        tickHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                long elapsed = System.currentTimeMillis() - ringStartTime;
                long remaining = maxRingDurationMs - elapsed;

                if (remaining <= 0) {
                    // 超时，自动停止
                    Log.i(TAG, "响铃超时（" + (maxRingDurationMs / 1000) + "s），自动停止");
                    stopAlarm();
                    return;
                }

                // 广播剩余时间给 AlarmRingActivity
                Intent tick = new Intent(ACTION_TICK);
                tick.putExtra(EXTRA_REMAINING_MS, remaining);
                sendBroadcast(tick);

                // 每秒触发一次
                sendEmptyMessageDelayed(0, 1000);
            }
        };
        tickHandler.sendEmptyMessage(0);
    }

    private void stopCountdown() {
        if (tickHandler != null) {
            tickHandler.removeCallbacksAndMessages(null);
            tickHandler = null;
        }
    }

    private void playAlarmTone(String toneUriStr) {
        stopMediaPlayer();
        try {
            Uri toneUri;
            if (toneUriStr != null && !toneUriStr.isEmpty()) {
                toneUri = Uri.parse(toneUriStr);
            } else {
                toneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                if (toneUri == null) {
                    toneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                }
            }

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(
                new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
            );
            mediaPlayer.setDataSource(this, toneUri);
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();
            Log.d(TAG, "铃声已播放: " + toneUri);
        } catch (Exception e) {
            Log.e(TAG, "播放铃声失败", e);
        }
    }

    private void startVibration() {
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = {0, 800, 400, 800, 400};
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
        }
    }

    public void stopAlarm() {
        stopCountdown();
        stopMediaPlayer();
        if (vibrator != null) {
            vibrator.cancel();
            vibrator = null;
        }
        stopForeground(true);
        stopSelf();
    }

    private void stopMediaPlayer() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private Notification buildNotification(String ruleName, String sender, String body) {
        // 点击通知打开响铃界面
        Intent openIntent = new Intent(this, AlarmRingActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        openIntent.putExtra(EXTRA_RULE_NAME, ruleName);
        openIntent.putExtra(EXTRA_SMS_SENDER, sender);
        openIntent.putExtra(EXTRA_SMS_BODY, body);
        PendingIntent openPending = PendingIntent.getActivity(
                this, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // 停止按钮
        Intent stopIntent = new Intent(this, AlarmService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPending = PendingIntent.getService(
                this, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("🔔 短信闹钟：" + ruleName)
                .setContentText("来自：" + sender)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("来自：" + sender + "\n内容：" + body))
                .setSmallIcon(R.drawable.ic_alarm)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setContentIntent(openPending)
                .addAction(R.drawable.ic_stop, "关闭闹钟", stopPending)
                .setOngoing(true)
                .setAutoCancel(false)
                .build();
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "短信闹钟", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("短信触发的闹钟通知");
        channel.enableVibration(false); // 震动由 Service 直接控制
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        stopAlarm();
        super.onDestroy();
    }
}
