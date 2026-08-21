package com.smsalarm.app.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smsalarm.app.R;
import com.smsalarm.app.data.AlarmRecord;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 闹钟触发记录列表适配器
 */
public class AlarmLogAdapter extends RecyclerView.Adapter<AlarmLogAdapter.ViewHolder> {

    private List<AlarmRecord> records = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    public void setRecords(List<AlarmRecord> records) {
        this.records = records != null ? records : new ArrayList<>();
        notifyDataSetChanged();
    }

    /** 移除指定位置记录并返回被移除的对象（用于左滑删除） */
    public AlarmRecord removeAt(int position) {
        if (position < 0 || position >= records.size()) return null;
        AlarmRecord removed = records.remove(position);
        notifyItemRemoved(position);
        return removed;
    }

    /** 在指定位置重新插入记录（用于左滑删除取消时回滚） */
    public void addAt(int position, AlarmRecord record) {
        if (record == null) return;
        int target = Math.max(0, Math.min(position, records.size()));
        records.add(target, record);
        notifyItemInserted(target);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alarm_log, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AlarmRecord record = records.get(position);

        holder.tvRuleName.setText(record.getRuleName() != null ? record.getRuleName() : "短信闹钟");
        holder.tvTime.setText(timeFormat.format(new Date(record.getTriggerTime())));
        holder.tvSender.setText(record.getSender() != null ? record.getSender() : "");
        holder.tvBody.setText(record.getBody() != null ? record.getBody() : "");
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRuleName;
        TextView tvTime;
        TextView tvSender;
        TextView tvBody;

        ViewHolder(View view) {
            super(view);
            tvRuleName = view.findViewById(R.id.tvRecordRuleName);
            tvTime = view.findViewById(R.id.tvRecordTime);
            tvSender = view.findViewById(R.id.tvRecordSender);
            tvBody = view.findViewById(R.id.tvRecordBody);
        }
    }
}
