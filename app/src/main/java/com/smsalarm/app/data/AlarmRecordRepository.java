package com.smsalarm.app.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 闹钟触发记录持久化存储（SharedPreferences + Gson）
 */
public class AlarmRecordRepository {

    private static final String PREF_NAME = "sms_alarm_records";
    private static final String KEY_RECORDS = "records";
    private static final int MAX_RECORDS = 200; // 最多保存 200 条记录

    private static AlarmRecordRepository instance;
    private final SharedPreferences prefs;
    private final Gson gson;

    private AlarmRecordRepository(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public static synchronized AlarmRecordRepository getInstance(Context context) {
        if (instance == null) {
            instance = new AlarmRecordRepository(context);
        }
        return instance;
    }

    /** 获取所有记录（按时间倒序：最新的在前） */
    public List<AlarmRecord> getAllRecords() {
        List<AlarmRecord> records = loadFromPrefs();
        Collections.sort(records, (a, b) -> Long.compare(b.getTriggerTime(), a.getTriggerTime()));
        return records;
    }

    /** 保存一条触发记录 */
    public void addRecord(AlarmRecord record) {
        List<AlarmRecord> records = loadFromPrefs();
        records.add(record);
        // 超过上限时删除最旧的记录
        if (records.size() > MAX_RECORDS) {
            Collections.sort(records, (a, b) -> Long.compare(a.getTriggerTime(), b.getTriggerTime()));
            records = records.subList(records.size() - MAX_RECORDS, records.size());
        }
        persist(records);
    }

    /** 清空所有记录 */
    public void clearAll() {
        prefs.edit().remove(KEY_RECORDS).apply();
    }

    /** 删除指定 id 的某一条记录 */
    public void deleteRecord(String id) {
        if (id == null) return;
        List<AlarmRecord> records = loadFromPrefs();
        boolean removed = records.removeIf(r -> id.equals(r.getId()));
        if (removed) {
            persist(records);
        }
    }

    /** 获取记录总数 */
    public int getCount() {
        return loadFromPrefs().size();
    }

    private List<AlarmRecord> loadFromPrefs() {
        String json = prefs.getString(KEY_RECORDS, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<List<AlarmRecord>>() {}.getType();
        List<AlarmRecord> result = gson.fromJson(json, type);
        return result != null ? result : new ArrayList<>();
    }

    private void persist(List<AlarmRecord> records) {
        prefs.edit().putString(KEY_RECORDS, gson.toJson(records)).apply();
    }
}
