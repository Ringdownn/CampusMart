package com.example.campusmart;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import android.app.Activity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.campusmart.adapter.MyGoodsAdapter;
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

public class MyGoodsActivity extends Activity implements MyGoodsAdapter.OnButtonClickListener {
    private RecyclerView rvMyGoods;
    private MyGoodsAdapter adapter;
    private List<MyGoodsAdapter.Goods> goodsList;
    private OkHttpClient client;
    private Gson gson;
    private String BASE_URL;
    private long userId;
    private String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_goods);

        ImageView ivBack = findViewById(R.id.iv_back);
        rvMyGoods = findViewById(R.id.rv_my_goods);

        initNetwork();
        initRecyclerView();
        loadMyGoods();

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
        adapter = new MyGoodsAdapter(goodsList, this);
        adapter.setImageLoader((imageView, url) -> {
            if (url != null && !url.isEmpty()) {
                Glide.with(MyGoodsActivity.this)
                        .load(ImageUrlUtils.normalize(MyGoodsActivity.this, url))
                        .placeholder(R.drawable.placeholder)
                        .error(R.drawable.placeholder)
                        .centerCrop()
                        .into(imageView);
            }
        });
        rvMyGoods.setLayoutManager(new LinearLayoutManager(this));
        rvMyGoods.setAdapter(adapter);
    }

    private void loadMyGoods() {
        if (userId == 0 || token.isEmpty()) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String url = BASE_URL + "/app/goods/poster?posterId=" + userId + "&current=1&size=20";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("access-token", token)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(MyGoodsActivity.this, "Network error", Toast.LENGTH_SHORT).show()
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
                                Toast.makeText(MyGoodsActivity.this, "Load failed", Toast.LENGTH_SHORT).show()
                        );
                    }
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(MyGoodsActivity.this, "Server error", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    private void updateGoodsList(List<GoodsVo> goodsVos) {
        goodsList.clear();
        for (GoodsVo vo : goodsVos) {
            MyGoodsAdapter.Goods goods = new MyGoodsAdapter.Goods(
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
    public void onDeleteClick(int position) {
        MyGoodsAdapter.Goods goods = goodsList.get(position);

        new AlertDialog.Builder(this)
                .setTitle("Delete Confirmation")
                .setMessage("Are you sure you want to delete this item?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteGoods(goods.goodId, position);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteGoods(Long goodId, int position) {
        if (goodId == null || token.isEmpty()) {
            runOnUiThread(() ->
                    Toast.makeText(this, "Invalid goods information", Toast.LENGTH_SHORT).show()
            );
            return;
        }

        String url = BASE_URL + "/app/goods/deleteById?id=" + goodId;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("access-token", token)
                .put(okhttp3.RequestBody.create(new byte[0]))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(MyGoodsActivity.this, "Delete failed: Network error", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();

                    Type type = new TypeToken<Result<Boolean>>(){}.getType();
                    Result<Boolean> result = gson.fromJson(responseData, type);

                    if (result != null && result.getCode() == 200 && result.getData()) {
                        runOnUiThread(() -> {
                            goodsList.remove(position);
                            adapter.notifyItemRemoved(position);
                            adapter.notifyItemRangeChanged(position, goodsList.size());
                            Toast.makeText(MyGoodsActivity.this, "Delete successful", Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        runOnUiThread(() ->
                                Toast.makeText(MyGoodsActivity.this, "Delete failed: Server error", Toast.LENGTH_SHORT).show()
                        );
                    }
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(MyGoodsActivity.this, "Delete failed: Server error", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    @Override
    public void onUpdateClick(int position) {
        MyGoodsAdapter.Goods goods = goodsList.get(position);
        Intent intent = new Intent(MyGoodsActivity.this, UpdateActivity.class);
        intent.putExtra("goodId", goods.goodId);
        startActivity(intent);
    }
}