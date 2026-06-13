package com.smsalarm.app.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.smsalarm.app.R;
import com.smsalarm.app.data.RuleRepository;
import com.smsalarm.app.data.SmsRule;
import com.smsalarm.app.databinding.ActivityMainBinding;

import java.util.List;

/**
 * 主界面：展示规则列表
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private RuleAdapter adapter;
    private RuleRepository repository;

    private final ActivityResultLauncher<String[]> permissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            boolean allGranted = true;
            for (Boolean granted : result.values()) {
                if (!granted) { allGranted = false; break; }
            }
            if (!allGranted) {
                Snackbar.make(binding.getRoot(),
                            "需要短信权限才能监听短信，请在系统设置中授权",
                            Snackbar.LENGTH_LONG).show();
            }
        });

    private final ActivityResultLauncher<Intent> editRuleLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            refreshList();
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        repository = RuleRepository.getInstance(this);

        // 设置 RecyclerView
        adapter = new RuleAdapter(
            rule -> openEditRule(rule.getId()),     // 点击编辑
            this::confirmDeleteRule,                 // 长按删除
            (rule, enabled) -> {                     // 开关切换
                rule.setEnabled(enabled);
                repository.saveRule(rule);
            }
        );
        binding.rvRules.setLayoutManager(new LinearLayoutManager(this));
        binding.rvRules.setAdapter(adapter);

        // FAB 新建规则
        binding.fabAdd.setOnClickListener(v -> openEditRule(null));

        // 请求权限
        requestRequiredPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshList();
    }

    private void refreshList() {
        List<SmsRule> rules = repository.getAllRules();
        adapter.setRules(rules);
        binding.tvEmpty.setVisibility(rules.isEmpty()
                ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private void openEditRule(String ruleId) {
        Intent intent = new Intent(this, RuleEditActivity.class);
        if (ruleId != null) intent.putExtra(RuleEditActivity.EXTRA_RULE_ID, ruleId);
        editRuleLauncher.launch(intent);
    }

    private void confirmDeleteRule(SmsRule rule) {
        new MaterialAlertDialogBuilder(this)
            .setTitle("删除规则")
            .setMessage("确定要删除规则「" + rule.getName() + "」吗？")
            .setPositiveButton("删除", (d, w) -> {
                repository.deleteRule(rule.getId());
                refreshList();
                Snackbar.make(binding.getRoot(), "已删除", Snackbar.LENGTH_SHORT).show();
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void requestRequiredPermissions() {
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS,
                Manifest.permission.POST_NOTIFICATIONS
            };
        } else {
            permissions = new String[]{
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS
            };
        }

        boolean allGranted = true;
        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(this, perm)
                    != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        if (!allGranted) {
            permissionLauncher.launch(permissions);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_test) {
            showDialog();
            return true;
        }
        if (id == R.id.action_about) {
            startActivity(new Intent(this, AboutActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDialog() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("短信匹配说明")
            .setMessage("App运行后将自动监听收件箱。收到短信时若匹配任意已启用规则，则立即触发闹钟。\n\n当前已启用规则数：" + repository.getEnabledRules().size())
            .setPositiveButton("知道了", null)
            .show();
    }
}
