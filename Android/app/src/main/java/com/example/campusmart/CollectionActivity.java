package com.example.campusmart;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.campusmart.adapter.CollectionAdapter;
import com.example.campusmart.common.page.PageImpl;
import com.example.campusmart.result.Result;
import com.example.campusmart.util.ImageUrlUtils;
import com.example.campusmart.vo.GoodsVo;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CollectionActivity extends Activity implements CollectionAdapter.OnButtonClickListener {
    private RecyclerView rvCollection;
    private CollectionAdapter adapter;
    private List<CollectionAdapter.Goods> goodsList;
    private OkHttpClient client;
    private Gson gson;
    private String BASE_URL;
    private long userId;
    private String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collection);

        ImageView ivBack = findViewById(R.id.iv_back);
        rvCollection = findViewById(R.id.rv_collection_history);

        initNetwork();
        initRecyclerView();
        loadCollections();

        ivBack.setOnClickListener(v -> finish());
    }

    private void initNetwork() {
        client = new OkHttpClient();
        gson = new Gson();
        BASE_URL = getResources().getString(R.string.base_url);

        SharedPreferences sp = getSharedPreferences("user_info", MODE_PRIVATE);
        token = sp.getString("token", "");
        userId = sp.getLong("user_id", 0);
    }

    private void initRecyclerView() {
        goodsList = new ArrayList<>();
        adapter = new CollectionAdapter(goodsList, this);
        adapter.setImageLoader((imageView, url) -> {
            if (url != null && !url.isEmpty()) {
                Glide.with(CollectionActivity.this)
                        .load(ImageUrlUtils.normalize(CollectionActivity.this, url))
                        .placeholder(R.drawable.placeholder)
                        .error(R.drawable.placeholder)
                        .centerCrop()
                        .into(imageView);
            }
        });
        rvCollection.setLayoutManager(new LinearLayoutManager(this));
        rvCollection.setAdapter(adapter);
    }

    private void loadCollections() {
        if (userId == 0 || token.isEmpty()) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String url = BASE_URL + "/app/collection/list?userID=" + userId + "&current=1&size=20";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("access-token", token)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(CollectionActivity.this, "Network error", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();

                    Type type = new TypeToken<Result<PageImpl<GoodsVo>>>(){}.getType();
                    Result<PageImpl<GoodsVo>> result = gson.fromJson(responseData, type);

                    if (result != null && result.getCode() == 200 && result.getData() != null) {
                        updateGoodsList(result.getData().getRecords());
                    } else {
                        runOnUiThread(() ->
                                Toast.makeText(CollectionActivity.this, "Load failed", Toast.LENGTH_SHORT).show()
                        );
                    }
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(CollectionActivity.this, "Server error", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    private void updateGoodsList(List<GoodsVo> goodsVos) {
        goodsList.clear();
        for (GoodsVo vo : goodsVos) {
            CollectionAdapter.Goods goods = new CollectionAdapter.Goods(
                    R.drawable.placeholder,
                    vo.getTitle(),
                    vo.getItemDescription(),
                    vo.getAppearance(),
                    "¥ " + vo.getPrice()
            );
            goods.pictureURL = vo.getPictureURL();
            goods.goodId = vo.getGoodID();
            goodsList.add(goods);
        }

        runOnUiThread(() -> adapter.notifyDataSetChanged());
    }

    @Override
    public void onDetailClick(int position) {
        CollectionAdapter.Goods goods = goodsList.get(position);
        Intent intent = new Intent(CollectionActivity.this, GoodsDetailActivity.class);
        intent.putExtra("product_good_id", goods.goodId);
        intent.putExtra("product_image_url", ImageUrlUtils.normalize(this, goods.pictureURL));
        startActivity(intent);
    }
}
