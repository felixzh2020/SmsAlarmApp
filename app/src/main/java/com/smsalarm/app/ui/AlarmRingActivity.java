package com.smsalarm.app.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.smsalarm.app.R;
import com.smsalarm.app.databinding.ActivityAlarmRingBinding;
import com.smsalarm.app.service.AlarmService;

/**
 * 闹钟响铃界面：全屏显示，提供关闭按钮，显示倒计时
 * 倒计时归零后自动关闭（防止用户漏看）
 */
public class AlarmRingActivity extends AppCompatActivity {

    private ActivityAlarmRingBinding binding;

    // 倒计时广播接收器
    private BroadcastReceiver tickReceiver;
    private long maxRingDurationMs = AlarmService.DEFAULT_MAX_RING_DURATION_MS;
    private long remainingMs = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 锁屏上方显示 + 点亮屏幕
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            // API 27+ 使用新 API
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            // API 26 使用窗口标志
            getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
            );
        }
        // 保持屏幕常亮（所有版本通用）
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        binding = ActivityAlarmRingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String ruleName  = getIntent().getStringExtra(AlarmService.EXTRA_RULE_NAME);
        String smsSender = getIntent().getStringExtra(AlarmService.EXTRA_SMS_SENDER);
        String smsBody   = getIntent().getStringExtra(AlarmService.EXTRA_SMS_BODY);
        int duration   = getIntent().getIntExtra(AlarmService.EXTRA_MAX_RING_DURATION_MS, 0);
        maxRingDurationMs = duration > 0 ? duration : AlarmService.DEFAULT_MAX_RING_DURATION_MS;
        remainingMs = maxRingDurationMs;

        binding.tvAlarmTitle.setText(ruleName != null ? ruleName : "短信闹钟");
        binding.tvSmsSender.setText("发信人：" + (smsSender != null ? smsSender : "未知"));
        binding.tvSmsBody.setText(smsBody != null ? smsBody : "");

        updateCountdownUi();

        binding.btnDismiss.setOnClickListener(v -> dismissAlarm());

        // 注册倒计时广播接收器
        tickReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (AlarmService.ACTION_TICK.equals(intent.getAction())) {
                    remainingMs = intent.getLongExtra(AlarmService.EXTRA_REMAINING_MS, 0);
                    updateCountdownUi();
                    if (remainingMs <= 0) {
                        // 倒计时归零，自动关闭
                        dismissAlarm();
                    }
                }
            }
        };
        registerReceiver(tickReceiver, new IntentFilter(AlarmService.ACTION_TICK), Context.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String ruleName  = intent.getStringExtra(AlarmService.EXTRA_RULE_NAME);
        String smsSender = intent.getStringExtra(AlarmService.EXTRA_SMS_SENDER);
        String smsBody   = intent.getStringExtra(AlarmService.EXTRA_SMS_BODY);
        binding.tvAlarmTitle.setText(ruleName != null ? ruleName : "短信闹钟");
        binding.tvSmsSender.setText("发信人：" + (smsSender != null ? smsSender : "未知"));
        binding.tvSmsBody.setText(smsBody != null ? smsBody : "");
    }

    private void updateCountdownUi() {
        long totalSeconds = Math.max(0, remainingMs) / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        String timeStr = String.format("%d:%02d", minutes, seconds);
        binding.tvCountdown.setText("将在 " + timeStr + " 后自动关闭");
    }

    private void dismissAlarm() {
        // 停止闹钟服务
        Intent stopIntent = new Intent(this, AlarmService.class);
        stopIntent.setAction(AlarmService.ACTION_STOP);
        startService(stopIntent);
        finish();
    }

    @Override
    public void onBackPressed() {
        // 拦截返回键，必须点击关闭按钮或等待倒计时归零
    }

    @Override
    protected void onDestroy() {
        if (tickReceiver != null) {
            unregisterReceiver(tickReceiver);
            tickReceiver = null;
        }
        super.onDestroy();
    }
}
