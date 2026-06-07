package com.example.campusmart;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.campusmart.adapter.HistoricalPurchaseAdapter;
import com.example.campusmart.entity.Goods;
import com.example.campusmart.result.Result;
import com.example.campusmart.util.ImageUrlUtils;
import com.example.campusmart.vo.OrderVo;
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

public class HistoricalPurchaseActivity extends AppCompatActivity implements HistoricalPurchaseAdapter.OnDetailClickListener {
    private static final String TAB_BUYER = "buyer";
    private static final String TAB_SELLER = "seller";

    private RecyclerView rvHistoricalPurchase;
    private HistoricalPurchaseAdapter adapter;
    private List<HistoricalPurchaseAdapter.Purchase> purchaseList;
    private TextView tvTagBuyer;
    private TextView tvTagSeller;
    private OkHttpClient client;
    private Gson gson;
    private String baseUrl;
    private String token;
    private long userId;
    private boolean firstResume = true;
    private String currentTab = TAB_BUYER;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historical_purchase);

        // 初始化控件
        ImageView ivBack = findViewById(R.id.iv_back);
        tvTagBuyer = findViewById(R.id.tv_tag_buyer);
        tvTagSeller = findViewById(R.id.tv_tag_seller);
        rvHistoricalPurchase = findViewById(R.id.rv_collection_history);

        initNetwork();
        initRecyclerView();
        initTabs();
        loadOrders();

        // 返回按钮点击事件
        ivBack.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (firstResume) {
            firstResume = false;
            return;
        }
        if (adapter != null) {
            loadOrders();
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

    private void initRecyclerView() {
        purchaseList = new ArrayList<>();
        adapter = new HistoricalPurchaseAdapter(purchaseList, this);
        adapter.setImageLoader((imageView, url) -> {
            if (url != null && !url.isEmpty()) {
                Glide.with(HistoricalPurchaseActivity.this)
                        .load(ImageUrlUtils.normalize(HistoricalPurchaseActivity.this, url))
                        .placeholder(R.drawable.placeholder)
                        .error(R.drawable.placeholder)
                        .centerCrop()
                        .into(imageView);
            }
        });
        rvHistoricalPurchase.setLayoutManager(new LinearLayoutManager(this));
        rvHistoricalPurchase.setAdapter(adapter);
    }

    private void initTabs() {
        updateTabUi();
        tvTagBuyer.setOnClickListener(v -> switchTab(TAB_BUYER));
        tvTagSeller.setOnClickListener(v -> switchTab(TAB_SELLER));
    }

    private void switchTab(String tab) {
        if (currentTab.equals(tab)) {
            return;
        }
        currentTab = tab;
        updateTabUi();
        loadOrders();
    }

    private void updateTabUi() {
        boolean buyerSelected = TAB_BUYER.equals(currentTab);
        tvTagBuyer.setBackgroundResource(buyerSelected ? R.drawable.bg_tag_selected : R.drawable.bg_tag_unselected);
        tvTagBuyer.setTextColor(buyerSelected ? Color.WHITE : Color.rgb(85, 85, 85));
        tvTagSeller.setBackgroundResource(buyerSelected ? R.drawable.bg_tag_unselected : R.drawable.bg_tag_selected);
        tvTagSeller.setTextColor(buyerSelected ? Color.rgb(85, 85, 85) : Color.WHITE);
    }

    private void loadOrders() {
        if (userId == 0 || token.isEmpty()) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Request request = new Request.Builder()
                .url(baseUrl + "/app/orders/" + currentTab)
                .addHeader("access-token", token)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(HistoricalPurchaseActivity.this, "Load orders failed", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body() != null ? response.body().string() : "";
                Type type = new TypeToken<Result<List<OrderVo>>>() {
                }.getType();
                Result<List<OrderVo>> result = parseResult(responseData, type);

                runOnUiThread(() -> {
                    if (response.isSuccessful() && result != null && result.getCode() == 200 && result.getData() != null) {
                        updateOrders(result.getData());
                    } else {
                        Toast.makeText(HistoricalPurchaseActivity.this, "Load orders failed", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void updateOrders(List<OrderVo> orders) {
        purchaseList.clear();
        for (OrderVo order : orders) {
            HistoricalPurchaseAdapter.Purchase purchase = new HistoricalPurchaseAdapter.Purchase(
                    R.drawable.placeholder,
                    order.getOrderId(),
                    order.getGoodsId(),
                    "Order " + safe(order.getOrderNo()),
                    "Created: " + formatTime(order.getCreateTime()),
                    "",
                    "¥ " + formatAmount(order.getAmount()),
                    displayStatus(order.getStatus())
            );
            purchaseList.add(purchase);
            loadGoodsForOrder(purchase);
        }
        adapter.notifyDataSetChanged();

        if (orders.isEmpty()) {
            Toast.makeText(this, TAB_BUYER.equals(currentTab) ? "No purchase history" : "No sales history", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadGoodsForOrder(HistoricalPurchaseAdapter.Purchase purchase) {
        if (purchase.goodsId == null || purchase.goodsId <= 0) {
            return;
        }

        Request.Builder requestBuilder = new Request.Builder()
                .url(baseUrl + "/app/goods/selectById?id=" + purchase.goodsId)
                .get();
        if (token != null && !token.isEmpty()) {
            requestBuilder.addHeader("access-token", token);
        }

        client.newCall(requestBuilder.build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body() != null ? response.body().string() : "";
                Result<Goods> result = parseResult(responseData, new TypeToken<Result<Goods>>() {
                }.getType());

                if (response.isSuccessful() && result != null && result.getCode() == 200 && result.getData() != null) {
                    Goods goods = result.getData();
                    runOnUiThread(() -> {
                        if (goods.getTitle() != null && !goods.getTitle().isEmpty()) {
                            purchase.title = goods.getTitle();
                        }
                        if (goods.getItemDescription() != null && !goods.getItemDescription().isEmpty()) {
                            purchase.desc = goods.getItemDescription();
                        }
                        if (goods.getPictureURL() != null && !goods.getPictureURL().isEmpty()) {
                            purchase.pictureURL = goods.getPictureURL();
                        }
                        adapter.notifyDataSetChanged();
                    });
                }
            }
        });
    }

    // 详情按钮点击回调
    @Override
    public void onDetailClick(int position) {
        if (position < 0 || position >= purchaseList.size()) {
            return;
        }
        HistoricalPurchaseAdapter.Purchase purchase = purchaseList.get(position);
        if (purchase.orderId == null || purchase.orderId <= 0) {
            Toast.makeText(this, "Invalid order", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, OrderDetailActivity.class);
        intent.putExtra("orderId", purchase.orderId);
        intent.putExtra("goodTitle", purchase.title);
        if (purchase.pictureURL != null && !purchase.pictureURL.isEmpty()) {
            intent.putExtra("goodImageUrl", ImageUrlUtils.normalize(this, purchase.pictureURL));
        }
        startActivity(intent);
    }

    private <T> Result<T> parseResult(String responseData, Type type) {
        try {
            return gson.fromJson(responseData, type);
        } catch (Exception e) {
            return null;
        }
    }

    private String displayStatus(String status) {
        if (OrderVo.STATUS_CREATED.equals(status)) {
            return "To pay";
        }
        if (OrderVo.STATUS_PAID.equals(status)) {
            return "To receive";
        }
        if (OrderVo.STATUS_SETTLED.equals(status)) {
            return "Successful";
        }
        if (OrderVo.STATUS_CANCELLED.equals(status)) {
            return "Cancelled";
        }
        return safe(status);
    }

    private String formatAmount(String amount) {
        if (amount == null || amount.trim().isEmpty()) {
            return "0.00";
        }
        return amount;
    }

    private String formatTime(String value) {
        if (value == null || value.length() < 10) {
            return "--";
        }
        return value.substring(0, 10);
    }

    private String safe(String value) {
        return value != null ? value : "";
    }
}