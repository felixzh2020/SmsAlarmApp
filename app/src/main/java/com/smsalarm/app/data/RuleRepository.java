package com.smsalarm.app.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * 规则持久化存储（SharedPreferences + Gson）
 */
public class RuleRepository {

    private static final String PREF_NAME = "sms_alarm_rules";
    private static final String KEY_RULES = "rules";

    private static RuleRepository instance;
    private final SharedPreferences prefs;
    private final Gson gson;
    private List<SmsRule> cachedRules;

    private RuleRepository(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public static synchronized RuleRepository getInstance(Context context) {
        if (instance == null) {
            instance = new RuleRepository(context);
        }
        return instance;
    }

    /** 获取所有规则 */
    public List<SmsRule> getAllRules() {
        if (cachedRules == null) {
            cachedRules = loadFromPrefs();
        }
        return new ArrayList<>(cachedRules);
    }

    /** 获取所有启用的规则 */
    public List<SmsRule> getEnabledRules() {
        List<SmsRule> result = new ArrayList<>();
        for (SmsRule rule : getAllRules()) {
            if (rule.isEnabled()) result.add(rule);
        }
        return result;
    }

    /** 保存（新增或更新）规则 */
    public void saveRule(SmsRule rule) {
        List<SmsRule> rules = getAllRules();
        for (int i = 0; i < rules.size(); i++) {
            if (rules.get(i).getId().equals(rule.getId())) {
                rules.set(i, rule);
                persist(rules);
                return;
            }
        }
        rules.add(rule);
        persist(rules);
    }

    /** 删除规则 */
    public void deleteRule(String id) {
        List<SmsRule> rules = getAllRules();
        rules.removeIf(r -> r.getId().equals(id));
        persist(rules);
    }

    /** 根据 ID 获取规则 */
    public SmsRule getRuleById(String id) {
        for (SmsRule rule : getAllRules()) {
            if (rule.getId().equals(id)) return rule;
        }
        return null;
    }

    private List<SmsRule> loadFromPrefs() {
        String json = prefs.getString(KEY_RULES, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<List<SmsRule>>() {}.getType();
        List<SmsRule> result = gson.fromJson(json, type);
        return result != null ? result : new ArrayList<>();
    }

    private void persist(List<SmsRule> rules) {
        cachedRules = rules;
        prefs.edit().putString(KEY_RULES, gson.toJson(rules)).apply();
    }
}
