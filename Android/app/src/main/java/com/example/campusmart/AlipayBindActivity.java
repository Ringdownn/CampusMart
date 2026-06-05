package com.example.campusmart;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.campusmart.result.Result;
import com.example.campusmart.util.ImageUrlUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AlipayBindActivity extends AppCompatActivity {
    private ImageView ivAvatar;
    private EditText etBuyerId;
    private EditText etBuyerAccount;
    private Button btnBind;
    private OkHttpClient client;
    private Gson gson;
    private String BASE_URL;
    private long userId;
    private String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alipay_bind);

        ImageView ivBack = findViewById(R.id.iv_back);
        ivAvatar = findViewById(R.id.iv_avatar);
        etBuyerId = findViewById(R.id.et_buyer_id);
        etBuyerAccount = findViewById(R.id.et_buyer_account);
        btnBind = findViewById(R.id.btn_bind);

        initNetwork();
        loadUserAvatar();

        ivBack.setOnClickListener(v -> finish());
        btnBind.setOnClickListener(v -> bindAlipay());
    }

    private void initNetwork() {
        client = new OkHttpClient();
        gson = new Gson();
        BASE_URL = getResources().getString(R.string.base_url);

        SharedPreferences sp = getSharedPreferences("user_info", MODE_PRIVATE);
        token = sp.getString("token", "");
        userId = sp.getLong("user_id", 0);
    }

    private void loadUserAvatar() {
        SharedPreferences sp = getSharedPreferences("user_info", MODE_PRIVATE);
        String avatarUrl = ImageUrlUtils.normalize(this, sp.getString("avatar_url", ""));
        if (!avatarUrl.isEmpty()) {
            Glide.with(this)
                    .load(avatarUrl)
                    .placeholder(R.drawable.avatar_marry)
                    .error(R.drawable.avatar_marry)
                    .circleCrop()
                    .into(ivAvatar);
        }
    }

    private void bindAlipay() {
        String buyerId = etBuyerId.getText().toString().trim();
        String buyerAccount = etBuyerAccount.getText().toString().trim();

        if (buyerId.isEmpty()) {
            Toast.makeText(this, "Please enter sandbox buyer ID", Toast.LENGTH_SHORT).show();
            return;
        }

        if (buyerAccount.isEmpty()) {
            Toast.makeText(this, "Please enter sandbox buyer account", Toast.LENGTH_SHORT).show();
            return;
        }

        if (userId == 0 || token.isEmpty()) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        BindRequest requestBody = new BindRequest();
        requestBody.alipayUserId = buyerId;
        requestBody.alipayLoginId = buyerAccount;
        requestBody.nickname = "";

        RequestBody body = RequestBody.create(
                gson.toJson(requestBody),
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(BASE_URL + "/app/alipay/bind/mock")
                .addHeader("access-token", token)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        showErrorDialog("Network error, please try again")
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    Result<BindResponse> result = gson.fromJson(
                            responseData,
                            new TypeToken<Result<BindResponse>>(){}.getType()
                    );

                    if (result != null && result.getCode() == 200) {
                        runOnUiThread(() -> showSuccessDialog());
                    } else {
                        String msg = result != null ? result.getMessage() : "Bind failed";
                        runOnUiThread(() -> showErrorDialog(msg));
                    }
                } else {
                    runOnUiThread(() ->
                            showErrorDialog("Server error: " + response.code())
                    );
                }
            }
        });
    }

    private void showSuccessDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Bind Successful")
                .setMessage("Your Alipay account has been bound successfully.")
                .setPositiveButton("OK", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private void showErrorDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Bind Failed")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private static class BindRequest {
        String alipayUserId;
        String alipayLoginId;
        String nickname;
    }

    private static class BindResponse {
        boolean bound;
        String alipayUserId;
        String alipayLoginId;
        String nickname;
    }
}
