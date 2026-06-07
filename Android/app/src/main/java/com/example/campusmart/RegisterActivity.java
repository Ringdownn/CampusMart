package com.example.campusmart;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import com.example.campusmart.entity.RegisterVo;
import com.example.campusmart.result.Result;
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

public class RegisterActivity extends AppCompatActivity {
    private static final long SEND_CODE_COUNTDOWN_MILLIS = 60_000L;
    private static final long COUNTDOWN_INTERVAL_MILLIS = 1_000L;
    private static final String SEND_CODE_TEXT = "send";

    private EditText etSchoolName;
    private EditText etStudentId;
    private EditText etUsername;
    private EditText etPassword;
    private EditText etPhoneNumber;
    private EditText etVerificationCode;
    private AppCompatButton btnSendCode;
    private OkHttpClient client;
    private Gson gson;
    private String BASE_URL;
    private CountDownTimer sendCodeTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etSchoolName = findViewById(R.id.et_school_name);
        etStudentId = findViewById(R.id.et_student_id);
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        etPhoneNumber = findViewById(R.id.et_phone);
        etVerificationCode = findViewById(R.id.et_verification_code);

        TextView tvLogin = findViewById(R.id.tv_login);
        btnSendCode = findViewById(R.id.btn_send_code);
        AppCompatButton btnRegister = findViewById(R.id.btn_register);

        BASE_URL = getResources().getString(R.string.base_url);
        client = new OkHttpClient();
        gson = new Gson();

        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });

        btnRegister.setOnClickListener(v -> register());
        btnSendCode.setOnClickListener(v -> sendVerificationCode());
    }

    private void sendVerificationCode() {
        String phoneNumber = normalizePhoneNumber(etPhoneNumber.getText().toString());
        if (phoneNumber.isEmpty()) {
            Toast.makeText(this, "Enter phone number", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!phoneNumber.matches("\\d{11}")) {
            Toast.makeText(this, "Invalid phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSendCode.setEnabled(false);
        btnSendCode.setText("sending");
        Request request = new Request.Builder()
                .url(BASE_URL + "/app/login/getCode?phone=" + Uri.encode(phoneNumber))
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    btnSendCode.setEnabled(true);
                    btnSendCode.setText(SEND_CODE_TEXT);
                    Toast.makeText(RegisterActivity.this, "Send failed. Check network.", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body() != null ? response.body().string() : "";
                Result<?> result = gson.fromJson(responseData, new TypeToken<Result<?>>(){}.getType());
                runOnUiThread(() -> {
                    if (response.isSuccessful() && result != null && result.getCode() == 200) {
                        Toast.makeText(RegisterActivity.this, "Code sent", Toast.LENGTH_SHORT).show();
                        startSendCodeCountdown();
                    } else {
                        btnSendCode.setEnabled(true);
                        btnSendCode.setText(SEND_CODE_TEXT);
                        Toast.makeText(RegisterActivity.this,
                                result != null ? result.getMessage() : "Send failed",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void startSendCodeCountdown() {
        if (sendCodeTimer != null) {
            sendCodeTimer.cancel();
        }

        btnSendCode.setEnabled(false);
        btnSendCode.setText((SEND_CODE_COUNTDOWN_MILLIS / 1000) + "s");
        sendCodeTimer = new CountDownTimer(SEND_CODE_COUNTDOWN_MILLIS, COUNTDOWN_INTERVAL_MILLIS) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                btnSendCode.setText(seconds + "s");
            }

            @Override
            public void onFinish() {
                btnSendCode.setEnabled(true);
                btnSendCode.setText(SEND_CODE_TEXT);
                sendCodeTimer = null;
            }
        };
        sendCodeTimer.start();
    }

    private void register() {
        String schoolName = etSchoolName.getText().toString().trim();
        String studentId = etStudentId.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String phoneNumber = normalizePhoneNumber(etPhoneNumber.getText().toString());
        String verificationCode = etVerificationCode.getText().toString().trim();

        if (schoolName.isEmpty() || studentId.isEmpty() ||
                username.isEmpty() || password.isEmpty() || phoneNumber.isEmpty() || verificationCode.isEmpty()) {
            Toast.makeText(this, "Fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!phoneNumber.matches("\\d{11}")) {
            Toast.makeText(this, "Invalid phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        RegisterVo registerVo = new RegisterVo();
        registerVo.setSchoolName(schoolName);
        registerVo.setStudentID(Long.valueOf(studentId));
        registerVo.setUsername(username);
        registerVo.setPassword(password);
        registerVo.setPhone(Long.valueOf(phoneNumber));
        registerVo.setCode(verificationCode);

        String registerUrl = BASE_URL + "/app/register";
        RequestBody requestBody = RequestBody.create(
                gson.toJson(registerVo),
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(registerUrl)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(RegisterActivity.this, "Network error", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    Result<?> registerResult = gson.fromJson(
                            responseData,
                            new TypeToken<Result<?>>(){}.getType()
                    );

                    runOnUiThread(() -> {
                        if (registerResult.getCode() == 200) {
                            Toast.makeText(RegisterActivity.this, "Registered", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                            finish();
                        } else {
                            Toast.makeText(RegisterActivity.this,
                                    registerResult.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(RegisterActivity.this, "Register failed", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    private String normalizePhoneNumber(String rawPhone) {
        if (rawPhone == null) {
            return "";
        }
        String phone = rawPhone.trim()
                .replace(" ", "")
                .replace("-", "");
        if (phone.startsWith("+86") && phone.length() > 3) {
            return phone.substring(3);
        }
        if (phone.startsWith("86") && phone.length() == 13) {
            return phone.substring(2);
        }
        return phone;
    }

    @Override
    protected void onDestroy() {
        if (sendCodeTimer != null) {
            sendCodeTimer.cancel();
            sendCodeTimer = null;
        }
        super.onDestroy();
    }
}