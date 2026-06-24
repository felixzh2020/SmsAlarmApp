package com.smsalarm.app.ui;

import android.database.Cursor;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.smsalarm.app.R;
import com.smsalarm.app.data.MatchType;
import com.smsalarm.app.data.RuleRepository;
import com.smsalarm.app.data.SmsRule;
import com.smsalarm.app.databinding.ActivityRuleEditBinding;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 规则编辑界面（新建 / 编辑）
 * 支持设置最大响铃时长，含自定义铃声选择器
 */
public class RuleEditActivity extends AppCompatActivity {

    public static final String EXTRA_RULE_ID = "rule_id";

    private static final int[] DURATION_OPTIONS_MS = {
        60000, 120000, 180000, 300000, 600000, 900000, 1800000
    };

    private ActivityRuleEditBinding binding;
    private RuleRepository repository;
    private SmsRule currentRule;
    private String selectedToneUri = null;
    private android.widget.Spinner spinnerDuration;

    private List<String> ringtoneNames;
    private List<Uri> ringtoneUris;
    private Ringtone previewRingtone = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRuleEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        repository = RuleRepository.getInstance(this);
        spinnerDuration = findViewById(R.id.spinnerDuration);

        String ruleId = getIntent().getStringExtra(EXTRA_RULE_ID);
        if (ruleId != null) {
            currentRule = repository.getRuleById(ruleId);
            setTitle("编辑规则");
        }
        if (currentRule == null) {
            currentRule = new SmsRule();
            setTitle("新建规则");
        }

        loadRingtoneList();
        setupDurationSpinner();
        fillForm();
        setupListeners();
    }

    /**
     * 加载系统铃声列表（直接从 Cursor 读标题，不实例化 Ringtone，速度快）
     * 查询 TYPE_RINGTONE / TYPE_NOTIFICATION / TYPE_ALARM 三种类型，去重
     */
    private void loadRingtoneList() {
        ringtoneNames = new ArrayList<>();
        ringtoneUris = new ArrayList<>();

        // 第一项：使用系统默认
        ringtoneNames.add("使用系统默认铃声");
        ringtoneUris.add(null);

        int titleColumnIdx = -1;
        Set<String> addedUris = new HashSet<>();

        int[] types = {
            RingtoneManager.TYPE_RINGTONE,
            RingtoneManager.TYPE_NOTIFICATION,
            RingtoneManager.TYPE_ALARM
        };

        for (int type : types) {
            try {
                RingtoneManager manager = new RingtoneManager(this);
                manager.setType(type);
                Cursor cursor = manager.getCursor();
                if (cursor == null) continue;

                // 获取 TITLE 列的索引（只取一次）
                if (titleColumnIdx < 0) {
                    titleColumnIdx = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
                }

                while (cursor.moveToNext()) {
                    int pos = cursor.getPosition();
                    Uri uri = manager.getRingtoneUri(pos);
                    if (uri == null) continue;

                    String uriStr = uri.toString();
                    if (addedUris.contains(uriStr)) continue;
                    addedUris.add(uriStr);

                    // 直接从 Cursor 读标题，不创建 Ringtone 对象
                    String name = "未知铃声";
                    if (titleColumnIdx >= 0) {
                        try { name = cursor.getString(titleColumnIdx); } catch (Exception ignored) {}
                    }
                    ringtoneNames.add(name);
                    ringtoneUris.add(uri);
                }
                cursor.close();
            } catch (Exception e) {
                Log.e("RuleEdit", "加载类型 " + type + " 铃声失败", e);
            }
        }

        Log.d("RuleEdit", "加载到 " + (ringtoneNames.size() - 1) + " 个系统铃声");
    }

    private void stopPreviewRingtone() {
        if (previewRingtone != null) {
            try { previewRingtone.stop(); } catch (Exception ignored) {}
            previewRingtone = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopPreviewRingtone();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPreviewRingtone();
    }

    private void setupDurationSpinner() {
        List<String> labels = new ArrayList<>();
        for (int ms : DURATION_OPTIONS_MS) {
            labels.add((ms / 60000) + " 分钟");
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDuration.setAdapter(adapter);
        spinnerDuration.setSelection(3);
    }

    private void fillForm() {
        binding.etRuleName.setText(currentRule.getName());
        binding.etSenderPattern.setText(currentRule.getSenderPattern());
        binding.etContentPattern.setText(currentRule.getContentPattern());
        binding.switchRegex.setChecked(currentRule.isUseRegex());
        binding.switchVibrate.setChecked(currentRule.isVibrate());
        selectedToneUri = currentRule.getAlarmTone();

        switch (currentRule.getMatchType()) {
            case SENDER:  binding.rgMatchType.check(R.id.rbSender);  break;
            case CONTENT: binding.rgMatchType.check(R.id.rbContent); break;
            case BOTH:    binding.rgMatchType.check(R.id.rbBoth);    break;
        }
        updateFieldVisibility();
        updateToneNameDisplay();

        int ruleDuration = currentRule.getMaxRingDurationMs();
        int selectIndex = 3;
        if (ruleDuration > 0) {
            for (int i = 0; i < DURATION_OPTIONS_MS.length; i++) {
                if (DURATION_OPTIONS_MS[i] == ruleDuration) {
                    selectIndex = i;
                    break;
                }
            }
        }
        spinnerDuration.setSelection(selectIndex);
    }

    private void updateToneNameDisplay() {
        if (selectedToneUri != null && !selectedToneUri.isEmpty()) {
            try {
                Uri uri = Uri.parse(selectedToneUri);
                Ringtone ringtone = RingtoneManager.getRingtone(this, uri);
                if (ringtone != null) {
                    binding.tvToneName.setText(ringtone.getTitle(this));
                } else {
                    binding.tvToneName.setText("未知铃声");
                }
            } catch (Exception e) {
                binding.tvToneName.setText("未知铃声");
            }
        } else {
            binding.tvToneName.setText("使用系统默认铃声");
        }
    }

    private void setupListeners() {
        binding.rgMatchType.setOnCheckedChangeListener((group, checkedId) -> updateFieldVisibility());
        binding.btnSelectTone.setOnClickListener(v -> showRingtonePicker());
        binding.btnSave.setOnClickListener(v -> saveRule());
    }

    private void showRingtonePicker() {
        String[] items = ringtoneNames.toArray(new String[0]);
        int total = items.length;

        // 找到当前选中项
        int checkedItem = 0;
        if (selectedToneUri != null && !selectedToneUri.isEmpty()) {
            Uri currentUri = Uri.parse(selectedToneUri);
            for (int i = 0; i < ringtoneUris.size(); i++) {
                Uri uri = ringtoneUris.get(i);
                if (uri != null && uri.equals(currentUri)) {
                    checkedItem = i;
                    break;
                }
            }
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
            .setTitle("选择闹钟铃声")
            .setSingleChoiceItems(items, checkedItem, (dialog, which) -> {
                stopPreviewRingtone();

                Uri uri = ringtoneUris.get(which);
                if (uri != null) {
                    selectedToneUri = uri.toString();
                    // 播放预览
                    try {
                        previewRingtone = RingtoneManager.getRingtone(
                            RuleEditActivity.this, uri);
                        if (previewRingtone != null) {
                            previewRingtone.play();
                        }
                    } catch (Exception e) {
                        Log.e("RuleEdit", "播放铃声预览失败", e);
                    }
                } else {
                    selectedToneUri = null;
                }
                updateToneNameDisplay();
                dialog.dismiss();
            })
            .setNegativeButton("取消", (dialog, which) -> stopPreviewRingtone());

        builder.show();
    }

    private void updateFieldVisibility() {
        int checked = binding.rgMatchType.getCheckedRadioButtonId();
        boolean showSender  = (checked == R.id.rbSender  || checked == R.id.rbBoth);
        boolean showContent = (checked == R.id.rbContent || checked == R.id.rbBoth);
        binding.tilSender.setVisibility(showSender  ? android.view.View.VISIBLE : android.view.View.GONE);
        binding.tilContent.setVisibility(showContent ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private void saveRule() {
        String name = binding.etRuleName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            binding.tilRuleName.setError("请输入规则名称");
            return;
        }

        int checked = binding.rgMatchType.getCheckedRadioButtonId();
        MatchType matchType;
        if      (checked == R.id.rbSender)  matchType = MatchType.SENDER;
        else if (checked == R.id.rbContent) matchType = MatchType.CONTENT;
        else                                matchType = MatchType.BOTH;

        String sender  = binding.etSenderPattern.getText().toString().trim();
        String content = binding.etContentPattern.getText().toString().trim();

        if ((matchType == MatchType.SENDER || matchType == MatchType.BOTH)
                && TextUtils.isEmpty(sender)) {
            binding.tilSender.setError("请输入发信人关键词");
            return;
        }
        if ((matchType == MatchType.CONTENT || matchType == MatchType.BOTH)
                && TextUtils.isEmpty(content)) {
            binding.tilContent.setError("请输入内容关键词");
            return;
        }

        int durationIndex = spinnerDuration.getSelectedItemPosition();
        int maxRingMs = (durationIndex >= 0 && durationIndex < DURATION_OPTIONS_MS.length)
                ? DURATION_OPTIONS_MS[durationIndex] : 300000;

        currentRule.setName(name);
        currentRule.setMatchType(matchType);
        currentRule.setSenderPattern(sender);
        currentRule.setContentPattern(content);
        currentRule.setUseRegex(binding.switchRegex.isChecked());
        currentRule.setVibrate(binding.switchVibrate.isChecked());
        currentRule.setAlarmTone(selectedToneUri);
        currentRule.setMaxRingDurationMs(maxRingMs);

        stopPreviewRingtone();
        repository.saveRule(currentRule);
        Toast.makeText(this, "规则已保存", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        stopPreviewRingtone();
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        stopPreviewRingtone();
        super.onBackPressed();
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        stopPreviewRingtone();
    }
}
