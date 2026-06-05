package com.example.campusmart;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.gson.Gson;

import com.example.campusmart.vo.LoginVo;
import com.example.campusmart.result.Result;
import com.example.campusmart.entity.User;
import com.example.campusmart.util.ImageUrlUtils;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import com.google.gson.reflect.TypeToken;


public class LoginActivity extends AppCompatActivity {

    private EditText etUsername;
    private EditText etPassword;
    private OkHttpClient client;
    private Gson gson;
    private String BASE_URL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        TextView tvRegister = findViewById(R.id.tv_register);
        AppCompatButton btnLogin = findViewById(R.id.btn_loginLogin);

        BASE_URL = getResources().getString(R.string.base_url);
        client = new OkHttpClient();
        gson = new Gson();

        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            finish();
        });

        btnLogin.setOnClickListener(v -> login());
    }

    private void login() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "username and password can't be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        LoginVo loginVo = new LoginVo();
        loginVo.setUsername(username);
        loginVo.setPassword(password);

        String loginUrl = BASE_URL + "/app/login";
        RequestBody requestBody = RequestBody.create(
                gson.toJson(loginVo),
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(loginUrl)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(LoginActivity.this, "network error", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    Result<String> loginResult = gson.fromJson(
                            responseData,
                            new TypeToken<Result<String>>(){}.getType()
                    );

                    if (loginResult.getCode() == 200) { // 成功状态码
                        String token = loginResult.getData();
                        // 获取用户信息
                        getUserInfo(token);
                    } else {
                        runOnUiThread(() ->
                                Toast.makeText(LoginActivity.this, loginResult.getMessage(), Toast.LENGTH_SHORT).show()
                        );
                    }
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(LoginActivity.this, "login fail", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    private void getUserInfo(String token) {
        String infoUrl = BASE_URL + "/app/info";
        Request request = new Request.Builder()
                .url(infoUrl)
                .addHeader("access-token", token)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(LoginActivity.this, "get user info fail", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    Result<User> userResult = gson.fromJson(
                            responseData,
                            new TypeToken<Result<User>>(){}.getType()
                    );

                    if (userResult.getCode() == 200) {
                        User user = userResult.getData();
                        saveUserInfo(token, user);

                        runOnUiThread(() -> {
                            Toast.makeText(LoginActivity.this, "login successful", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                            finish();
                        });
                    } else {
                        runOnUiThread(() ->
                                Toast.makeText(LoginActivity.this, userResult.getMessage(), Toast.LENGTH_SHORT).show()
                        );
                    }
                }
            }
        });
    }

    // 保存用户信息到SharedPreferences
    private void saveUserInfo(String token, User user) {
        SharedPreferences sp = getSharedPreferences("user_info", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("token", token);
        editor.putLong("user_id", user.getUserID() != null ? user.getUserID() : 0);
        editor.putString("username", user.getUsername());
        editor.putString("nickname", user.getNickname());
        editor.putString("email", user.getEmail());
        editor.putLong("phone", user.getPhone() != null ? user.getPhone() : 0);
        editor.putString("profile_signature", user.getProfileSignature());
        editor.putString("school_name", user.getSchoolName());
        editor.putLong("student_id", user.getStudentID() != null ? user.getStudentID() : 0);
        editor.putString("avatar_url", ImageUrlUtils.normalize(this, user.getAvatarURL()));
        editor.apply();
    }
}