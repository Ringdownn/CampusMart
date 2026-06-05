package com.example.campusmart.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusmart.R;
import com.example.campusmart.vo.WalletFlowVo;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WalletFlowAdapter extends RecyclerView.Adapter<WalletFlowAdapter.FlowViewHolder> {
    private static final int COLOR_INCOME = Color.rgb(228, 169, 54);
    private static final int COLOR_WITHDRAW = Color.rgb(17, 17, 17);

    private final List<WalletFlowVo> flows;

    public WalletFlowAdapter(List<WalletFlowVo> flows) {
        this.flows = flows;
    }

    @NonNull
    @Override
    public FlowViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_wallet_flow, parent, false);
        return new FlowViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FlowViewHolder holder, int position) {
        WalletFlowVo flow = flows.get(position);
        boolean income = isIncome(flow.getFlowType());
        holder.tvTitle.setText(income ? "Income" : "Withdraw");
        holder.tvTime.setText(formatTime(flow.getCreateTime()));
        holder.tvAmount.setText((income ? "+" : "-") + formatAmount(flow.getAmount()));
        holder.tvAmount.setTextColor(income ? COLOR_INCOME : COLOR_WITHDRAW);
        holder.tvBalance.setText("Balance " + formatAmount(flow.getBalanceAfter()));
    }

    @Override
    public int getItemCount() {
        return flows.size();
    }

    private boolean isIncome(String flowType) {
        return "SELLER_INCOME".equals(flowType);
    }

    private String formatAmount(String amount) {
        if (amount == null || amount.trim().isEmpty()) {
            return "0.00";
        }
        try {
            return new DecimalFormat("0.##").format(Double.parseDouble(amount));
        } catch (NumberFormatException e) {
            return amount;
        }
    }

    private String formatTime(String raw) {
        long millis = parseServerTimeMillis(raw);
        if (millis <= 0) {
            return "--";
        }
        return new SimpleDateFormat("dd MMM, HH:mm", Locale.US).format(new Date(millis));
    }

    private long parseServerTimeMillis(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return 0;
        }
        String value = raw.trim();
        int dotIndex = value.indexOf('.');
        if (dotIndex >= 0) {
            int suffixIndex = -1;
            for (int i = dotIndex + 1; i < value.length(); i++) {
                char c = value.charAt(i);
                if (c == '+' || c == '-' || c == 'Z') {
                    suffixIndex = i;
                    break;
                }
            }
            value = suffixIndex >= 0 ? value.substring(0, dotIndex) + value.substring(suffixIndex) : value.substring(0, dotIndex);
        }

        String[] patterns = new String[]{
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd HH:mm:ss"
        };
        for (String pattern : patterns) {
            try {
                Date date = new SimpleDateFormat(pattern, Locale.US).parse(value);
                if (date != null) {
                    return date.getTime();
                }
            } catch (Exception ignored) {
            }
        }
        return 0;
    }

    static class FlowViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvTime;
        TextView tvAmount;
        TextView tvBalance;

        FlowViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_flow_title);
            tvTime = itemView.findViewById(R.id.tv_flow_time);
            tvAmount = itemView.findViewById(R.id.tv_flow_amount);
            tvBalance = itemView.findViewById(R.id.tv_flow_balance);
        }
    }
}
