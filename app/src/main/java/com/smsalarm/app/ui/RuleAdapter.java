package com.smsalarm.app.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smsalarm.app.data.MatchType;
import com.smsalarm.app.data.SmsRule;
import com.smsalarm.app.databinding.ItemRuleBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * 规则列表适配器
 */
public class RuleAdapter extends RecyclerView.Adapter<RuleAdapter.ViewHolder> {

    public interface OnRuleClick { void onClick(SmsRule rule); }
    public interface OnRuleLongClick { void onLongClick(SmsRule rule); }
    public interface OnRuleToggle { void onToggle(SmsRule rule, boolean enabled); }

    private List<SmsRule> rules = new ArrayList<>();
    private final OnRuleClick clickListener;
    private final OnRuleLongClick longClickListener;
    private final OnRuleToggle toggleListener;

    public RuleAdapter(OnRuleClick click, OnRuleLongClick longClick, OnRuleToggle toggle) {
        this.clickListener = click;
        this.longClickListener = longClick;
        this.toggleListener = toggle;
    }

    public void setRules(List<SmsRule> rules) {
        this.rules = rules;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRuleBinding binding = ItemRuleBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(rules.get(position));
    }

    @Override
    public int getItemCount() { return rules.size(); }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemRuleBinding b;

        ViewHolder(ItemRuleBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }

        void bind(SmsRule rule) {
            b.tvRuleName.setText(rule.getName());

            // 显示匹配条件摘要
            String desc = buildDesc(rule);
            b.tvRuleDesc.setText(desc);

            // 启用开关
            b.switchEnabled.setOnCheckedChangeListener(null);
            b.switchEnabled.setChecked(rule.isEnabled());
            b.switchEnabled.setOnCheckedChangeListener((btn, checked) ->
                    toggleListener.onToggle(rule, checked));

            // 点击 / 长按
            b.getRoot().setOnClickListener(v -> clickListener.onClick(rule));
            b.getRoot().setOnLongClickListener(v -> {
                longClickListener.onLongClick(rule);
                return true;
            });
        }

        private String buildDesc(SmsRule rule) {
            StringBuilder sb = new StringBuilder();
            switch (rule.getMatchType()) {
                case SENDER:
                    sb.append("发信人包含「").append(rule.getSenderPattern()).append("」");
                    break;
                case CONTENT:
                    sb.append("内容包含「").append(rule.getContentPattern()).append("」");
                    break;
                case BOTH:
                    sb.append("发信人「").append(rule.getSenderPattern())
                      .append("」且内容「").append(rule.getContentPattern()).append("」");
                    break;
            }
            if (rule.isUseRegex()) sb.append("（正则）");
            return sb.toString();
        }
    }
}
