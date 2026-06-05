package com.example.campusmart;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.campusmart.result.Result;
import com.example.campusmart.util.ImageUrlUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.DecimalFormat;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WalletActivity extends AppCompatActivity {
    private ImageView ivAvatar;
    private TextView tvAvailableBalance;
    private OkHttpClient client;
    private Gson gson;
    private String baseUrl;
    private String token;
    private long userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet);

        ivAvatar = findViewById(R.id.iv_wallet_avatar);
        tvAvailableBalance = findViewById(R.id.tv_available_balance);
        Button btnCashout = findViewById(R.id.btn_cashout);
        Button btnCheck = findViewById(R.id.btn_check);

        initNetwork();
        loadUserAvatar();

        findViewById(R.id.layout_back).setOnClickListener(v -> finish());
        btnCashout.setOnClickListener(v -> Toast.makeText(this, "Cashout is not available yet", Toast.LENGTH_SHORT).show());
        btnCheck.setOnClickListener(v -> Toast.makeText(this, "Payment check is not available yet", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (client != null) {
            loadWallet();
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

    private void loadUserAvatar() {
        SharedPreferences sp = getSharedPreferences("user_info", MODE_PRIVATE);
        String avatarUrl = ImageUrlUtils.normalize(this, sp.getString("avatar_url", ""));
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            Glide.with(this)
                    .load(avatarUrl)
                    .placeholder(R.drawable.avatar_marry)
                    .error(R.drawable.avatar_marry)
                    .circleCrop()
                    .into(ivAvatar);
        } else {
            Glide.with(this)
                    .load(R.drawable.avatar_marry)
                    .circleCrop()
                    .into(ivAvatar);
        }
    }

    private void loadWallet() {
        if (userId == 0 || token == null || token.isEmpty()) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
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
                runOnUiThread(() -> Toast.makeText(WalletActivity.this, "Load wallet failed", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body() != null ? response.body().string() : "";
                Type type = new TypeToken<Result<WalletInfo>>() {
                }.getType();
                Result<WalletInfo> result = parseResult(responseData, type);

                runOnUiThread(() -> {
                    if (response.isSuccessful() && result != null && result.getCode() == 200 && result.getData() != null) {
                        tvAvailableBalance.setText(formatAmount(result.getData().availableAmount));
                    } else {
                        Toast.makeText(WalletActivity.this, "Load wallet failed", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private <T> Result<T> parseResult(String responseData, Type type) {
        try {
            return gson.fromJson(responseData, type);
        } catch (Exception e) {
            return null;
        }
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

    private static class WalletInfo {
        private String availableAmount;
    }
}
