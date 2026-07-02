package com.smsalarm.app.ui;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.smsalarm.app.data.AlarmRecordRepository;
import com.smsalarm.app.data.AlarmRecord;
import com.smsalarm.app.databinding.ActivityAlarmLogBinding;

import java.util.List;

/**
 * 闹钟触发记录界面
 */
public class AlarmLogActivity extends AppCompatActivity {

    private ActivityAlarmLogBinding binding;
    private AlarmLogAdapter adapter;
    private AlarmRecordRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAlarmLogBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("闹钟记录");
        }

        repository = AlarmRecordRepository.getInstance(this);

        adapter = new AlarmLogAdapter();
        binding.rvAlarmLog.setLayoutManager(new LinearLayoutManager(this));
        binding.rvAlarmLog.setAdapter(adapter);

        // 清空记录按钮
        binding.tvClearRecords.setOnClickListener(v -> confirmClearRecords());

        refreshList();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshList();
    }

    private void refreshList() {
        List<AlarmRecord> records = repository.getAllRecords();
        adapter.setRecords(records);
        binding.tvEmptyLog.setVisibility(records.isEmpty() ? View.VISIBLE : View.GONE);
        binding.tvRecordCount.setText("触发记录：" + records.size() + " 条");
    }

    private void confirmClearRecords() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("清空记录")
            .setMessage("确定要清空所有触发记录吗？此操作不可恢复。")
            .setPositiveButton("清空", (d, w) -> {
                repository.clearAll();
                refreshList();
            })
            .setNegativeButton("取消", null)
            .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
