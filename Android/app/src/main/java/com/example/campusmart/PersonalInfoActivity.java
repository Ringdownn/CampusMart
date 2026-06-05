package com.example.campusmart;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.campusmart.R;
import com.example.campusmart.entity.User;
import com.example.campusmart.result.Result;
import com.example.campusmart.util.ImageUrlUtils;
import com.google.gson.Gson;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import com.google.gson.reflect.TypeToken;

public class PersonalInfoActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private EditText etNickname, etSignature, etPhone, etEmail;
    private ImageView ivAvatar;
    private OkHttpClient client;
    private Gson gson;
    private String BASE_URL;
    private long userId;
    private String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_info);

        // 初始化控件
        ImageView ivBack = findViewById(R.id.iv_back);
        etNickname = findViewById(R.id.et_nickname);
        etSignature = findViewById(R.id.et_signature);
        etPhone = findViewById(R.id.et_phone);
        etEmail = findViewById(R.id.et_email);
        ivAvatar = findViewById(R.id.iv_avatar);
        Button btnSave = findViewById(R.id.btn_save);
        Button btnBindAlipay = findViewById(R.id.btn_bind_alipay);

        // 初始化网络工具
        BASE_URL = getResources().getString(R.string.base_url);
        client = new OkHttpClient();
        gson = new Gson();

        loadLocalUserInfo();

        ivBack.setOnClickListener(v -> finish());

        ivAvatar.setOnClickListener(v -> openGallery());

        btnSave.setOnClickListener(v -> savePersonalInfo());

        btnBindAlipay.setOnClickListener(v -> {
            Intent intent = new Intent(PersonalInfoActivity.this, AlipayBindActivity.class);
            startActivity(intent);
        });
    }

    private void loadLocalUserInfo() {
        SharedPreferences sp = getSharedPreferences("user_info", MODE_PRIVATE);
        token = sp.getString("token", "");
        userId = sp.getLong("user_id", 0);

        etNickname.setText(sp.getString("nickname", ""));
        etSignature.setText(sp.getString("profile_signature", ""));
        etPhone.setText(String.valueOf(sp.getLong("phone", 0)).replaceFirst("(\\d{3})(\\d{4})(\\d{4})", "$1-$2-$3"));
        etEmail.setText(sp.getString("email", ""));

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

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);
                Bitmap compressedBitmap = compressBitmap(originalBitmap, 700 * 1024);
                if (compressedBitmap != null) {
                    ivAvatar.setImageBitmap(compressedBitmap);
                    uploadAvatar(compressedBitmap);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(this, "图片获取失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private Bitmap compressBitmap(Bitmap bitmap, int maxSize) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int quality = 100;
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);

        while (baos.toByteArray().length > maxSize && quality > 0) {
            baos.reset();
            quality -= 10;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        }

        return BitmapFactory.decodeByteArray(baos.toByteArray(), 0, baos.toByteArray().length);
    }

    private void uploadAvatar(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        byte[] imageBytes = baos.toByteArray();

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "avatar.jpg",
                        RequestBody.create(MediaType.parse("image/jpeg"), imageBytes))
                .addFormDataPart("userID", String.valueOf(userId))
                .build();

        Request request = new Request.Builder()
                .url(BASE_URL + "/app/goods/file/uploadAvatar")
                .addHeader("access-token", token)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(PersonalInfoActivity.this, "头像上传失败", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    Result<String> result = gson.fromJson(
                            responseData,
                            new TypeToken<Result<String>>(){}.getType()
                    );

                    if (result.getCode() == 200) {
                        SharedPreferences sp = getSharedPreferences("user_info", MODE_PRIVATE);
                        sp.edit().putString("avatar_url", ImageUrlUtils.normalize(PersonalInfoActivity.this, result.getData())).apply();

                        runOnUiThread(() ->
                                Toast.makeText(PersonalInfoActivity.this, "头像更新成功", Toast.LENGTH_SHORT).show()
                        );
                    } else {
                        runOnUiThread(() ->
                                Toast.makeText(PersonalInfoActivity.this, "头像更新失败: " + result.getMessage(), Toast.LENGTH_SHORT).show()
                        );
                    }
                }
            }
        });
    }

    private void savePersonalInfo() {
        String nickname = etNickname.getText().toString().trim();
        String signature = etSignature.getText().toString().trim();
        String phone = etPhone.getText().toString().trim().replaceAll("-", "");
        String email = etEmail.getText().toString().trim();

        if (nickname.isEmpty() || phone.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "请完善必填信息", Toast.LENGTH_SHORT).show();
            return;
        }

        User user = new User();
        user.setUserID(userId);
        user.setNickname(nickname);
        user.setProfileSignature(signature);
        try {
            user.setPhone(Long.parseLong(phone));
        } catch (NumberFormatException e) {
            Toast.makeText(this, "手机号格式错误", Toast.LENGTH_SHORT).show();
            return;
        }
        user.setEmail(email);

        RequestBody requestBody = RequestBody.create(
                gson.toJson(user),
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(BASE_URL + "/app/user/updateUserInfoByID")
                .addHeader("access-token", token)
                .put(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(PersonalInfoActivity.this, "信息保存失败", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    Result<Boolean> result = gson.fromJson(
                            responseData,
                            new TypeToken<Result<Boolean>>(){}.getType()
                    );

                    if (result.getCode() == 200 && result.getData()) {
                        SharedPreferences.Editor editor = getSharedPreferences("user_info", MODE_PRIVATE).edit();
                        editor.putString("nickname", nickname);
                        editor.putString("profile_signature", signature);
                        editor.putLong("phone", Long.parseLong(phone));
                        editor.putString("email", email);
                        editor.apply();

                        runOnUiThread(() ->
                                Toast.makeText(PersonalInfoActivity.this, "信息保存成功", Toast.LENGTH_SHORT).show()
                        );
                    } else {
                        runOnUiThread(() ->
                                Toast.makeText(PersonalInfoActivity.this, "信息保存失败: " + result.getMessage(), Toast.LENGTH_SHORT).show()
                        );
                    }
                }
            }
        });
    }
}