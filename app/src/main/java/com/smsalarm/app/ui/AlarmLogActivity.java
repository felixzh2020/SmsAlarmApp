package com.smsalarm.app.ui;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
        setupSwipeToDelete();

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

    /** 左滑删除：滑出后弹出确认框，确认则从存储删除，取消则记录回滚 */
    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback swipeCallback =
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView,
                                          @NonNull RecyclerView.ViewHolder viewHolder,
                                          @NonNull RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                        int position = viewHolder.getBindingAdapterPosition();
                        if (position == RecyclerView.NO_POSITION) return;
                        AlarmRecord removed = adapter.removeAt(position);
                        if (removed == null) return;

                        new MaterialAlertDialogBuilder(AlarmLogActivity.this)
                                .setTitle("删除记录")
                                .setMessage("确定要删除这条触发记录吗？此操作不可恢复。")
                                .setPositiveButton("删除", (d, w) -> {
                                    repository.deleteRecord(removed.getId());
                                    refreshList();
                                })
                                .setNegativeButton("取消", (d, w) -> adapter.addAt(position, removed))
                                .setOnCancelListener(dialog -> adapter.addAt(position, removed))
                                .show();
                    }
                };
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.rvAlarmLog);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
