package com.smsalarm.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.smsalarm.app.R;

/**
 * 欢迎界面（Splash Screen）
 * 展示 3 秒后自动跳转到 MainActivity
 */
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DURATION_MS = 3000L;

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        handler.postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        }, SPLASH_DURATION_MS);
    }

    @Override
    protected void onDestroy() {
        // 防止 Activity 已销毁后仍然执行跳转
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
