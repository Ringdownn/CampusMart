package com.example.campusmart;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campusmart.result.Result;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.DecimalFormat;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class WithdrawConfirmActivity extends AppCompatActivity {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private TextView tvAlipayAccount;
    private EditText etWithdrawAmount;
    private OkHttpClient client;
    private Gson gson;
    private String baseUrl;
    private String token;
    private long userId;
    private double availableAmount;
    private boolean alipayBound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_withdraw_confirm);

        tvAlipayAccount = findViewById(R.id.tv_alipay_account);
        etWithdrawAmount = findViewById(R.id.et_withdraw_amount);

        initNetwork();
        availableAmount = getIntent().getDoubleExtra("availableAmount", 0D);
        loadWallet();
        loadAlipayBind();

        findViewById(R.id.layout_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_confirm_withdraw).setOnClickListener(v -> submitWithdraw());
    }

    private void initNetwork() {
        client = new OkHttpClient();
        gson = new Gson();
        baseUrl = getResources().getString(R.string.base_url);

        SharedPreferences sp = getSharedPreferences("user_info", MODE_PRIVATE);
        token = sp.getString("token", "");
        userId = sp.getLong("user_id", 0);
    }

    private void loadWallet() {
        if (!ensureLoggedIn()) {
            return;
        }

        Request request = new Request.Builder()
                .url(baseUrl + "/app/wallet")
                .addHeader("access-token", token)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(WithdrawConfirmActivity.this, "Load wallet failed", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body() != null ? response.body().string() : "";
                Result<WalletInfo> result = parseResult(responseData, new TypeToken<Result<WalletInfo>>() {
                }.getType());
                runOnUiThread(() -> {
                    if (response.isSuccessful() && result != null && result.getCode() == 200 && result.getData() != null) {
                        availableAmount = parseAmount(result.getData().availableAmount);
                    }
                });
            }
        });
    }

    private void loadAlipayBind() {
        if (!ensureLoggedIn()) {
            return;
        }

        Request request = new Request.Builder()
                .url(baseUrl + "/app/alipay/bind")
                .addHeader("access-token", token)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(WithdrawConfirmActivity.this, "Load Alipay account failed", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body() != null ? response.body().string() : "";
                Result<AlipayBindInfo> result = parseResult(responseData, new TypeToken<Result<AlipayBindInfo>>() {
                }.getType());
                runOnUiThread(() -> {
                    if (response.isSuccessful() && result != null && result.getCode() == 200 && result.getData() != null) {
                        AlipayBindInfo bindInfo = result.getData();
                        alipayBound = bindInfo.bound;
                        tvAlipayAccount.setText(alipayBound ? maskAccount(firstNonEmpty(bindInfo.alipayLoginId, bindInfo.alipayUserId)) : "Not bound");
                    } else {
                        tvAlipayAccount.setText("Not bound");
                    }
                });
            }
        });
    }

    private void submitWithdraw() {
        if (!ensureLoggedIn()) {
            return;
        }
        if (!alipayBound) {
            Toast.makeText(this, "Please bind Alipay first", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, AlipayBindActivity.class));
            return;
        }

        String amountText = etWithdrawAmount.getText().toString().trim();
        double amount = parseAmount(amountText);
        if (amount <= 1D) {
            Toast.makeText(this, "Withdrawal amount must be greater than 1", Toast.LENGTH_SHORT).show();
            return;
        }
        if (amount > availableAmount) {
            Toast.makeText(this, "Withdrawal amount cannot exceed balance", Toast.LENGTH_SHORT).show();
            return;
        }

        WithdrawRequest body = new WithdrawRequest(formatAmount(amount));
        Request request = new Request.Builder()
                .url(baseUrl + "/app/wallet/withdraw")
                .addHeader("access-token", token)
                .post(RequestBody.create(gson.toJson(body), JSON))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(WithdrawConfirmActivity.this, "Withdraw failed", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body() != null ? response.body().string() : "";
                Result<WithdrawResponse> result = parseResult(responseData, new TypeToken<Result<WithdrawResponse>>() {
                }.getType());
                runOnUiThread(() -> {
                    if (response.isSuccessful() && result != null && result.getCode() == 200 && result.getData() != null) {
                        Toast.makeText(WithdrawConfirmActivity.this, "Withdraw successful", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(WithdrawConfirmActivity.this, result != null ? result.getMessage() : "Withdraw failed", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private boolean ensureLoggedIn() {
        if (userId == 0 || token == null || token.isEmpty()) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return false;
        }
        return true;
    }

    private <T> Result<T> parseResult(String responseData, Type type) {
        try {
            return gson.fromJson(responseData, type);
        } catch (Exception e) {
            return null;
        }
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

    private String firstNonEmpty(String first, String second) {
        if (first != null && !first.trim().isEmpty()) {
            return first.trim();
        }
        return second != null ? second.trim() : "";
    }

    private String maskAccount(String account) {
        if (account == null || account.isEmpty()) {
            return "--";
        }
        if (account.length() <= 7) {
            return account.charAt(0) + "****" + account.charAt(account.length() - 1);
        }
        return account.substring(0, 3) + "-****-" + account.substring(account.length() - 4);
    }

    private static class WalletInfo {
        private String availableAmount;
    }

    private static class AlipayBindInfo {
        private boolean bound;
        private String alipayUserId;
        private String alipayLoginId;
    }

    private static class WithdrawRequest {
        private final String amount;

        private WithdrawRequest(String amount) {
            this.amount = amount;
        }
    }

    private static class WithdrawResponse {
        private String availableAmount;
    }
}
