package com.example.campusmart;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusmart.adapter.WalletFlowAdapter;
import com.example.campusmart.result.Result;
import com.example.campusmart.vo.WalletFlowVo;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WalletFlowsActivity extends AppCompatActivity {
    private TextView tvTotalIncome;
    private TextView tvTotalWithdraw;
    private WalletFlowAdapter adapter;
    private final List<WalletFlowVo> flows = new ArrayList<>();
    private OkHttpClient client;
    private Gson gson;
    private String baseUrl;
    private String token;
    private long userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet_flows);

        tvTotalIncome = findViewById(R.id.tv_total_income);
        tvTotalWithdraw = findViewById(R.id.tv_total_withdraw);
        RecyclerView rvWalletFlows = findViewById(R.id.rv_wallet_flows);

        initNetwork();
        adapter = new WalletFlowAdapter(flows);
        rvWalletFlows.setLayoutManager(new LinearLayoutManager(this));
        rvWalletFlows.setAdapter(adapter);

        findViewById(R.id.layout_back).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (client != null) {
            loadFlows();
        }
    }

    private void initNetwork() {
        client = new OkHttpClient();
        gson = new Gson();
        baseUrl = getResources().getString(R.string.base_url);

        SharedPreferences sp = getSharedPreferences("user_info", MODE_PRIVATE);
        token = sp.getString("token", "");
        userId = sp.getLong("user_id", 0);
    }

    private void loadFlows() {
        if (userId == 0 || token == null || token.isEmpty()) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Request request = new Request.Builder()
                .url(baseUrl + "/app/wallet/flows?limit=50")
                .addHeader("access-token", token)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(WalletFlowsActivity.this, "Load payment checks failed", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body() != null ? response.body().string() : "";
                Type type = new TypeToken<Result<List<WalletFlowVo>>>() {
                }.getType();
                Result<List<WalletFlowVo>> result = parseResult(responseData, type);

                runOnUiThread(() -> {
                    if (response.isSuccessful() && result != null && result.getCode() == 200 && result.getData() != null) {
                        updateFlows(result.getData());
                    } else {
                        Toast.makeText(WalletFlowsActivity.this, "Load payment checks failed", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void updateFlows(List<WalletFlowVo> data) {
        List<WalletFlowVo> visibleFlows = filterVisibleFlows(data);
        flows.clear();
        flows.addAll(visibleFlows);
        adapter.notifyDataSetChanged();
        updateTotals(visibleFlows);
    }

    private void updateTotals(List<WalletFlowVo> data) {
        double incomeTotal = 0D;
        double withdrawTotal = 0D;
        for (WalletFlowVo flow : data) {
            double amount = parseAmount(flow.getAmount());
            if (isIncome(flow.getFlowType())) {
                incomeTotal += amount;
            } else {
                withdrawTotal += amount;
            }
        }
        tvTotalIncome.setText("$ " + formatAmount(incomeTotal));
        tvTotalWithdraw.setText("$ " + formatAmount(withdrawTotal));
    }

    private boolean isIncome(String flowType) {
        return "SELLER_INCOME".equals(flowType);
    }

    private boolean isWithdraw(String flowType) {
        return "WITHDRAW_OUT".equals(flowType);
    }

    private List<WalletFlowVo> filterVisibleFlows(List<WalletFlowVo> data) {
        List<WalletFlowVo> result = new ArrayList<>();
        for (WalletFlowVo flow : data) {
            String flowType = flow.getFlowType();
            if (isIncome(flowType) || isWithdraw(flowType)) {
                result.add(flow);
            }
        }
        return result;
    }

    private double parseAmount(String amount) {
        if (amount == null || amount.trim().isEmpty()) {
            return 0D;
        }
        try {
            return Double.parseDouble(amount);
        } catch (NumberFormatException e) {
            return 0D;
        }
    }

    private String formatAmount(double amount) {
        return new DecimalFormat("0.##").format(amount);
    }

    private <T> Result<T> parseResult(String responseData, Type type) {
        try {
            return gson.fromJson(responseData, type);
        } catch (Exception e) {
            return null;
        }
    }
}
