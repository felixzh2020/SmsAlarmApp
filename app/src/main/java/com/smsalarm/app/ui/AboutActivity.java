package com.smsalarm.app.ui;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.smsalarm.app.R;
import com.smsalarm.app.databinding.ActivityAboutBinding;

/**
 * 关于页面：展示应用信息、开发者署名、微信公众号
 */
public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityAboutBinding binding = ActivityAboutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("关于");
        }

        // 读取版本号（用 PackageManager，不依赖 BuildConfig）
        String versionName = "";
        int versionCode = 0;
        try {
            PackageInfo info = getPackageManager()
                .getPackageInfo(getPackageName(), 0);
            versionName = info.versionName;
            versionCode = info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // 不会发生
        }
        binding.tvVersion.setText("v" + versionName + "（" + versionCode + "）");
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
