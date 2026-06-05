package com.example.campusmart;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ImageView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.campusmart.adapter.GoodsDetailAdapter;
import com.example.campusmart.entity.Goods;
import com.example.campusmart.result.Result;
import com.example.campusmart.util.ImageUrlUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GoodsDetailActivity extends AppCompatActivity {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private ImageView ivBack, ivLike, ivCollection;
    private TextView tvLikeCount, tvCollectionCount;
    private Button btnBuy;
    private RecyclerView rvGoodsDetail;
    private GoodsDetailAdapter adapter;
    private OkHttpClient client;
    private Gson gson;
    private String baseUrl;
    private String token;
    private Long goodId;
    private Goods goods;
    private boolean isLiked = false;
    private boolean isCollected = false;
    private int likeCount = 0;
    private int collectionCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goods_detail);

        Intent intent = getIntent();
        goodId = intent.getLongExtra("product_good_id", -1);

        client = new OkHttpClient();
        gson = new Gson();
        baseUrl = getResources().getString(R.string.base_url);

        SharedPreferences sp = getSharedPreferences("user_info", MODE_PRIVATE);
        token = sp.getString("token", null);

        initViews();
        initRecyclerView(new ArrayList<>());
        setClickEvents();

        if (goodId != -1) {
            loadGoodsDetail(goodId);
        } else {
            Toast.makeText(this, "Invalid product ID", Toast.LENGTH_SHORT).show();
        }
    }

    private void initViews() {
        ivBack = findViewById(R.id.iv_back);
        ivLike = findViewById(R.id.iv_like);
        ivCollection = findViewById(R.id.iv_collection);
        tvLikeCount = findViewById(R.id.tv_like_count);
        tvCollectionCount = findViewById(R.id.tv_collection_count);
        btnBuy = findViewById(R.id.btn_buy);
        rvGoodsDetail = findViewById(R.id.rv_goods_detail);
    }

    private void initRecyclerView(List<GoodsDetailAdapter.GoodsDetailItem> dataList) {
        adapter = new GoodsDetailAdapter(dataList);
        rvGoodsDetail.setLayoutManager(new LinearLayoutManager(this));
        rvGoodsDetail.setAdapter(adapter);
    }

    private void setClickEvents() {
        ivBack.setOnClickListener(v -> finish());
        ivLike.setOnClickListener(v -> toggleLike());
        ivCollection.setOnClickListener(v -> toggleCollection());
        btnBuy.setOnClickListener(v -> {
            if (goods == null) {
                Toast.makeText(this, "商品信息加载中，请稍后", Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferences sp = getSharedPreferences("user_info", MODE_PRIVATE);
            Long currentUserId = sp.getLong("user_id", 0);
            String selfAvatarUrl = ImageUrlUtils.normalize(this, sp.getString("avatar_url", ""));

            if (currentUserId <= 0 || token == null || token.isEmpty()) {
                Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
                return;
            }

            Long sellerId = goods.getPublishUserID();
            String sellerNickname = getIntent().getStringExtra("product_publisher");
            String otherAvatarUrl = ImageUrlUtils.normalize(this, getIntent().getStringExtra("product_avatar_url"));
            String productImageUrl = ImageUrlUtils.normalize(this, getIntent().getStringExtra("product_image_url"));


            Intent intent = new Intent(GoodsDetailActivity.this, ChatActivity.class);
            intent.putExtra("currentUserId", currentUserId);
            intent.putExtra("otherUserId", sellerId);
            intent.putExtra("goodId", goodId);
            intent.putExtra("token", token);
            intent.putExtra("otherNickname", sellerNickname != null ? sellerNickname : "卖家");
            intent.putExtra("goodTitle", goods.getTitle() != null ? goods.getTitle() : "");
            intent.putExtra("selfAvatarUrl", selfAvatarUrl);
            intent.putExtra("otherAvatarUrl", otherAvatarUrl != null ? otherAvatarUrl : "");
            intent.putExtra("goodImageUrl", productImageUrl != null ? productImageUrl : "");
            intent.putExtra("goodPrice", goods.getPrice() != null ? goods.getPrice().doubleValue() : 0D);
            intent.putExtra("isBuyer", true);
            startActivity(intent);
        });
    }

    private void loadGoodsDetail(Long goodId) {
        String url = baseUrl + "/app/goods/selectById?id=" + goodId;

        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .get();

        if (token != null && !token.isEmpty()) {
            requestBuilder.addHeader("access-token", token);
        }

        Request request = requestBuilder.build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(GoodsDetailActivity.this, "Network error", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.code() == 601 || response.code() == 602) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(GoodsDetailActivity.this, "Login expired, please login again", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    Type type = new TypeToken<Result<Goods>>(){}.getType();
                    Result<Goods> result = gson.fromJson(responseData, type);

                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (result != null && result.getCode() == 200 && result.getData() != null) {
                            updateUI(result.getData());
                        } else {
                            Toast.makeText(GoodsDetailActivity.this,
                                    "Load failed: " + (result != null ? result.getMessage() : "Unknown error"),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    new Handler(Looper.getMainLooper()).post(() ->
                            Toast.makeText(GoodsDetailActivity.this, "Server error: " + response.code(), Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    private void updateUI(Goods goods) {
        this.goods = goods;

        // 加载点赞和收藏状态及数量
        loadLikeStatus();
        loadCollectionStatus();

        List<GoodsDetailAdapter.GoodsDetailItem> dataList = new ArrayList<>();

        Intent intent = getIntent();
        String publisher = intent.getStringExtra("product_publisher");
        String avatarUrl = ImageUrlUtils.normalize(this, intent.getStringExtra("product_avatar_url"));
        String productImageUrl = ImageUrlUtils.normalize(this, intent.getStringExtra("product_image_url"));

        dataList.add(new GoodsDetailAdapter.GoodsDetailItem(
                GoodsDetailAdapter.TYPE_USER_INFO,
                R.drawable.avatar_placeholder,
                publisher != null ? publisher : "Unknown",
                goods.getAppearance() != null ? goods.getAppearance() : "Unknown"
        ));

        dataList.add(new GoodsDetailAdapter.GoodsDetailItem(
                GoodsDetailAdapter.TYPE_PRICE_TITLE,
                "¥ " + goods.getPrice(), // 价格格式化
                goods.getTitle() != null ? goods.getTitle() : "No title"
        ));

        dataList.add(new GoodsDetailAdapter.GoodsDetailItem(
                GoodsDetailAdapter.TYPE_DESC,
                goods.getItemDescription() != null ? goods.getItemDescription() : "No description"
        ));

        dataList.add(new GoodsDetailAdapter.GoodsDetailItem(
                GoodsDetailAdapter.TYPE_GOODS_IMG,
                R.drawable.placeholder
        ));

        adapter = new GoodsDetailAdapter(dataList) {
            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                super.onBindViewHolder(holder, position);
                if (getItemViewType(position) == TYPE_GOODS_IMG && holder instanceof GoodsImgViewHolder) {
                    GoodsImgViewHolder imgHolder = (GoodsImgViewHolder) holder;
                    if (productImageUrl != null && !productImageUrl.isEmpty()) {
                        Glide.with(GoodsDetailActivity.this)
                                .load(productImageUrl)
                                .apply(new RequestOptions()
                                        .placeholder(R.drawable.placeholder)
                                        .error(R.drawable.placeholder)
                                        .centerCrop())
                                .into(imgHolder.ivGoods);
                    }
                }
                else if (getItemViewType(position) == TYPE_USER_INFO && holder instanceof UserInfoViewHolder) {
                    UserInfoViewHolder userHolder = (UserInfoViewHolder) holder;
                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        Glide.with(GoodsDetailActivity.this)
                                .load(avatarUrl)
                                .apply(new RequestOptions()
                                        .placeholder(R.drawable.avatar_placeholder)
                                        .error(R.drawable.avatar_placeholder)
                                        .circleCrop())
                                .into(userHolder.ivAvatar);
                    }
                }
            }
        };
        rvGoodsDetail.setAdapter(adapter);
    }

    // ==================== 点赞功能 ====================

    /**
     * 加载点赞状态和数量
     * GET /app/like/count?goodID={id}
     * GET /app/like/check?goodID={id}
     */
    private void loadLikeStatus() {
        if (goodId == null || goodId <= 0) return;

        // 获取点赞数
        String countUrl = baseUrl + "/app/like/count?goodID=" + goodId;
        Request countRequest = new Request.Builder()
                .url(countUrl)
                .addHeader("access-token", token)
                .get()
                .build();

        client.newCall(countRequest).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    Result<Integer> result = gson.fromJson(response.body().string(),
                            new TypeToken<Result<Integer>>(){}.getType());
                    if (result != null && result.getCode() == 200 && result.getData() != null) {
                        likeCount = result.getData();
                        runOnUiThread(() -> tvLikeCount.setText(String.valueOf(likeCount)));
                    }
                }
            }
        });

        // 检查是否已点赞
        if (token != null && !token.isEmpty()) {
            String checkUrl = baseUrl + "/app/like/check?goodID=" + goodId;
            Request checkRequest = new Request.Builder()
                    .url(checkUrl)
                    .addHeader("access-token", token)
                    .get()
                    .build();

            client.newCall(checkRequest).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {}

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful() && response.body() != null) {
                        Result<Boolean> result = gson.fromJson(response.body().string(),
                                new TypeToken<Result<Boolean>>(){}.getType());
                        if (result != null && result.getCode() == 200 && result.getData() != null) {
                            isLiked = result.getData();
                            runOnUiThread(() -> updateLikeUI());
                        }
                    }
                }
            });
        }
    }

    /**
     * 切换点赞状态
     * POST /app/like/toggle?goodID={id}
     */
    private void toggleLike() {
        if (token == null || token.isEmpty()) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = baseUrl + "/app/like/toggle?goodID=" + goodId;
        Request request = new Request.Builder()
                .url(url)
                .addHeader("access-token", token)
                .post(RequestBody.create("", JSON))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(GoodsDetailActivity.this, "操作失败", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    Result<LikeResultVo> result = gson.fromJson(response.body().string(),
                            new TypeToken<Result<LikeResultVo>>(){}.getType());
                    if (result != null && result.getCode() == 200 && result.getData() != null) {
                        LikeResultVo vo = result.getData();
                        isLiked = vo.getIsLiked();
                        likeCount = vo.getLikeCount();
                        runOnUiThread(() -> {
                            updateLikeUI();
                            Toast.makeText(GoodsDetailActivity.this,
                                    isLiked ? "Liked!" : "Unliked!", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            }
        });
    }

    private void updateLikeUI() {
        tvLikeCount.setText(String.valueOf(likeCount));
        // 根据状态切换图标颜色（如果有选中/未选中图标资源可替换）
        ivLike.setAlpha(isLiked ? 1.0f : 0.5f);
    }

    // ==================== 收藏功能 ====================

    /**
     * 加载收藏状态和数量
     * GET /app/collection/count?goodID={id}
     * GET /app/collection/check?goodID={id}
     */
    private void loadCollectionStatus() {
        if (goodId == null || goodId <= 0) return;

        // 获取收藏数
        String countUrl = baseUrl + "/app/collection/count?goodID=" + goodId;
        Request countRequest = new Request.Builder()
                .url(countUrl)
                .addHeader("access-token", token)
                .get()
                .build();

        client.newCall(countRequest).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    Result<Integer> result = gson.fromJson(response.body().string(),
                            new TypeToken<Result<Integer>>(){}.getType());
                    if (result != null && result.getCode() == 200 && result.getData() != null) {
                        collectionCount = result.getData();
                        runOnUiThread(() -> tvCollectionCount.setText(String.valueOf(collectionCount)));
                    }
                }
            }
        });

        // 检查是否已收藏
        if (token != null && !token.isEmpty()) {
            String checkUrl = baseUrl + "/app/collection/check?goodID=" + goodId;
            Request checkRequest = new Request.Builder()
                    .url(checkUrl)
                    .addHeader("access-token", token)
                    .get()
                    .build();

            client.newCall(checkRequest).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {}

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful() && response.body() != null) {
                        Result<Boolean> result = gson.fromJson(response.body().string(),
                                new TypeToken<Result<Boolean>>(){}.getType());
                        if (result != null && result.getCode() == 200 && result.getData() != null) {
                            isCollected = result.getData();
                            runOnUiThread(() -> updateCollectionUI());
                        }
                    }
                }
            });
        }
    }

    /**
     * 切换收藏状态
     * POST /app/collection/toggle?goodID={id}
     */
    private void toggleCollection() {
        if (token == null || token.isEmpty()) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = baseUrl + "/app/collection/toggle?goodID=" + goodId;
        Request request = new Request.Builder()
                .url(url)
                .addHeader("access-token", token)
                .post(RequestBody.create("", JSON))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(GoodsDetailActivity.this, "操作失败", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    Result<CollectionResultVo> result = gson.fromJson(response.body().string(),
                            new TypeToken<Result<CollectionResultVo>>(){}.getType());
                    if (result != null && result.getCode() == 200 && result.getData() != null) {
                        CollectionResultVo vo = result.getData();
                        isCollected = vo.getIsCollected();
                        collectionCount = vo.getCollectionCount();
                        runOnUiThread(() -> {
                            updateCollectionUI();
                            Toast.makeText(GoodsDetailActivity.this,
                                    isCollected ? "Collected!" : "Uncollected!", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            }
        });
    }

    private void updateCollectionUI() {
        tvCollectionCount.setText(String.valueOf(collectionCount));
        ivCollection.setAlpha(isCollected ? 1.0f : 0.5f);
    }

    private static class LikeResultVo {
        private Integer likeCount;
        private Boolean isLiked;

        private Integer getLikeCount() {
            return likeCount != null ? likeCount : 0;
        }

        private Boolean getIsLiked() {
            return isLiked != null && isLiked;
        }
    }

    private static class CollectionResultVo {
        private Integer collectionCount;
        private Boolean isCollected;

        private Integer getCollectionCount() {
            return collectionCount != null ? collectionCount : 0;
        }

        private Boolean getIsCollected() {
            return isCollected != null && isCollected;
        }
    }
}