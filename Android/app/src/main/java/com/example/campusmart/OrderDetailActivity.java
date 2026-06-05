package com.example.campusmart;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.bumptech.glide.Glide;
import com.example.campusmart.entity.Goods;
import com.example.campusmart.result.Result;
import com.example.campusmart.util.ImageUrlUtils;
import com.example.campusmart.vo.OrderVo;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OrderDetailActivity extends AppCompatActivity {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final int COLOR_BLUE = Color.rgb(22, 135, 247);
    private static final int COLOR_PINK = Color.rgb(217, 54, 97);
    private static final int COLOR_GRAY = Color.rgb(108, 113, 117);
    private static final int COLOR_LINE = Color.rgb(218, 218, 218);
    private static final long RECEIVE_DEADLINE_DAYS = 7;
    private static final int MAX_STATUS_REFRESH_ATTEMPTS = 4;

    private ImageView ivBack;
    private ImageView ivGoodsImg;
    private TextView tvOrderTitle;
    private TextView tvOrderMessage;
    private TextView tvOrderHint;
    private TextView tvGoodsTitle;
    private TextView tvGoodsPrice;
    private TextView tvOrderMeta;
    private TextView tvSecondaryAction;
    private TextView[] stepCircles;
    private TextView[] stepLabels;
    private View[] stepLines;
    private AppCompatButton btnPrimaryAction;

    private OkHttpClient client;
    private Gson gson;
    private Handler handler;
    private Runnable countdownRunnable;
    private String baseUrl;
    private String token;
    private long currentUserId;
    private long orderId;
    private OrderVo order;
    private Goods goods;
    private String cachedImageUrl;
    private int statusRefreshAttempts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        initNetwork();
        initViews();
        readIntentData();
        bindEvents();

        if (orderId <= 0) {
            Toast.makeText(this, "Invalid order", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadOrder();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (orderId > 0 && order != null) {
            loadOrder();
        }
    }

    @Override
    protected void onDestroy() {
        stopCountdown();
        super.onDestroy();
    }

    private void initNetwork() {
        client = new OkHttpClient();
        gson = new Gson();
        handler = new Handler(Looper.getMainLooper());
        baseUrl = getResources().getString(R.string.base_url);

        SharedPreferences sp = getSharedPreferences("user_info", MODE_PRIVATE);
        token = sp.getString("token", "");
        currentUserId = sp.getLong("user_id", 0);
    }

    private void initViews() {
        ivBack = findViewById(R.id.iv_back);
        ivGoodsImg = findViewById(R.id.iv_goods_img);
        tvOrderTitle = findViewById(R.id.tv_order_title);
        tvOrderMessage = findViewById(R.id.tv_order_message);
        tvOrderHint = findViewById(R.id.tv_order_hint);
        tvGoodsTitle = findViewById(R.id.tv_goods_title);
        tvGoodsPrice = findViewById(R.id.tv_goods_price);
        tvOrderMeta = findViewById(R.id.tv_order_meta);
        tvSecondaryAction = findViewById(R.id.tv_secondary_action);
        btnPrimaryAction = findViewById(R.id.btn_primary_action);

        stepCircles = new TextView[]{
                findViewById(R.id.tv_step_1_circle),
                findViewById(R.id.tv_step_2_circle),
                findViewById(R.id.tv_step_3_circle),
                findViewById(R.id.tv_step_4_circle)
        };
        stepLabels = new TextView[]{
                findViewById(R.id.tv_step_1_label),
                findViewById(R.id.tv_step_2_label),
                findViewById(R.id.tv_step_3_label),
                findViewById(R.id.tv_step_4_label)
        };
        stepLines = new View[]{
                findViewById(R.id.view_step_1_2),
                findViewById(R.id.view_step_2_3),
                findViewById(R.id.view_step_3_4)
        };
    }

    private void readIntentData() {
        Intent intent = getIntent();
        orderId = intent.getLongExtra("orderId", -1);
        cachedImageUrl = firstNonEmpty(
                intent.getStringExtra("goodImageUrl"),
                intent.getStringExtra("product_image_url")
        );

        String title = firstNonEmpty(
                intent.getStringExtra("goodTitle"),
                intent.getStringExtra("product_title")
        );
        if (title != null) {
            tvGoodsTitle.setText(title);
        }

        double price = intent.getDoubleExtra("goodPrice", 0D);
        if (price > 0) {
            tvGoodsPrice.setText("¥ " + new DecimalFormat("0.00").format(price));
        }

        loadGoodsImage(cachedImageUrl);
    }

    private void bindEvents() {
        ivBack.setOnClickListener(v -> finish());
        btnPrimaryAction.setOnClickListener(v -> handlePrimaryAction());
        tvSecondaryAction.setOnClickListener(v -> handleSecondaryAction());
    }

    private void loadOrder() {
        if (!ensureLoggedIn()) {
            return;
        }

        Request request = new Request.Builder()
                .url(baseUrl + "/app/orders/" + orderId)
                .addHeader("access-token", token)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(OrderDetailActivity.this, "Load order failed", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body() != null ? response.body().string() : "";
                Result<OrderVo> result = parseResult(responseData, new TypeToken<Result<OrderVo>>() {
                }.getType());

                runOnUiThread(() -> {
                    if (response.isSuccessful() && result != null && result.getCode() == 200 && result.getData() != null) {
                        order = result.getData();
                        renderOrder();
                        loadGoods(order.getGoodsId());
                    } else {
                        showError("Load Order Failed", result != null ? result.getMessage() : "Server error: " + response.code());
                    }
                });
            }
        });
    }

    private void loadGoods(Long goodsId) {
        if (goodsId == null || goodsId <= 0) {
            return;
        }

        Request.Builder requestBuilder = new Request.Builder()
                .url(baseUrl + "/app/goods/selectById?id=" + goodsId)
                .get();
        if (token != null && !token.isEmpty()) {
            requestBuilder.addHeader("access-token", token);
        }

        client.newCall(requestBuilder.build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(OrderDetailActivity.this, "Load goods failed", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body() != null ? response.body().string() : "";
                Result<Goods> result = parseResult(responseData, new TypeToken<Result<Goods>>() {
                }.getType());

                runOnUiThread(() -> {
                    if (response.isSuccessful() && result != null && result.getCode() == 200 && result.getData() != null) {
                        goods = result.getData();
                        renderGoods();
                    }
                });
            }
        });
    }

    private void renderOrder() {
        stopCountdown();

        String status = safe(order.getStatus());
        if (OrderVo.STATUS_CREATED.equals(status)) {
            renderCreated();
        } else if (OrderVo.STATUS_PAID.equals(status)) {
            renderPaid();
        } else if (OrderVo.STATUS_SETTLED.equals(status)) {
            renderSettled();
        } else if (OrderVo.STATUS_CANCELLED.equals(status)) {
            renderCancelled();
        } else {
            renderUnknown(status);
        }

        tvGoodsPrice.setText("¥ " + formatAmount(order.getAmount()));
        tvOrderMeta.setText("Order No: " + safe(order.getOrderNo()) + "\nCreated: " + formatServerTime(order.getCreateTime()));
    }

    private void renderGoods() {
        if (goods == null) {
            return;
        }
        if (goods.getTitle() != null && !goods.getTitle().isEmpty()) {
            tvGoodsTitle.setText(goods.getTitle());
        }
        if ((order == null || isEmpty(order.getAmount())) && goods.getPrice() != null) {
            tvGoodsPrice.setText("¥ " + new DecimalFormat("0.00").format(goods.getPrice()));
        }
    }

    private void renderCreated() {
        showStatusCopy(true);
        tvOrderTitle.setText("Wait to pay");
        tvOrderMessage.setText(isBuyer() ? "Please Pay Promptly!" : "Waiting for buyer payment.");
        renderSteps(2);
        startPaymentCountdown();

        if (isBuyer()) {
            showPrimaryAction("Pay", true);
            tvSecondaryAction.setText("Cancel order");
            tvSecondaryAction.setVisibility(View.VISIBLE);
        } else {
            showPrimaryAction("Waiting payment", false);
            tvSecondaryAction.setVisibility(View.GONE);
        }
    }

    private void renderPaid() {
        showStatusCopy(true);
        tvOrderTitle.setText("Wait to receive");
        tvOrderMessage.setText(isBuyer() ? "Transaction successful, awaiting buyer's receipt!" : "Buyer has paid. Waiting for receipt confirmation.");
        renderSteps(3);
        startReceiveCountdown();

        if (isBuyer()) {
            showPrimaryAction("Confirm receipt", true);
        } else {
            showPrimaryAction("Waiting buyer receipt", false);
        }
        tvSecondaryAction.setVisibility(View.GONE);
    }

    private void renderSettled() {
        showStatusCopy(false);
        tvOrderTitle.setText("Payment Successful");
        renderSteps(4);
        showPrimaryAction("Historical Purchase", true);
        tvSecondaryAction.setVisibility(View.GONE);
    }

    private void renderCancelled() {
        showStatusCopy(true);
        tvOrderTitle.setText("Order Cancelled");
        tvOrderMessage.setText("This order has been cancelled.");
        tvOrderHint.setText(isEmpty(order.getCancelReason()) ? "You can place a new order from the product detail page." : "Reason: " + order.getCancelReason());
        renderSteps(1);
        showPrimaryAction("Buy again", order.getGoodsId() != null && order.getGoodsId() > 0);
        tvSecondaryAction.setVisibility(View.GONE);
    }

    private void renderUnknown(String status) {
        showStatusCopy(true);
        tvOrderTitle.setText("Order Detail");
        tvOrderMessage.setText("Order status: " + status);
        tvOrderHint.setText("Please refresh later.");
        renderSteps(1);
        showPrimaryAction("Refresh", true);
        tvSecondaryAction.setVisibility(View.GONE);
    }

    private void renderSteps(int currentStep) {
        for (int i = 0; i < stepCircles.length; i++) {
            int step = i + 1;
            if (step < currentStep) {
                stepCircles[i].setBackgroundResource(R.drawable.bg_step_blue);
                stepCircles[i].setText("\u2713");
                stepLabels[i].setTextColor(COLOR_BLUE);
            } else if (step == currentStep) {
                stepCircles[i].setBackgroundResource(R.drawable.bg_step_pink);
                stepCircles[i].setText(String.valueOf(step));
                stepLabels[i].setTextColor(COLOR_PINK);
            } else {
                stepCircles[i].setBackgroundResource(R.drawable.bg_step_gray);
                stepCircles[i].setText(String.valueOf(step));
                stepLabels[i].setTextColor(COLOR_GRAY);
            }
        }

        for (int i = 0; i < stepLines.length; i++) {
            stepLines[i].setBackgroundColor(i + 1 < currentStep ? COLOR_BLUE : COLOR_LINE);
        }
    }

    private void showStatusCopy(boolean visible) {
        int visibility = visible ? View.VISIBLE : View.GONE;
        tvOrderMessage.setVisibility(visibility);
        tvOrderHint.setVisibility(visibility);
    }

    private void startPaymentCountdown() {
        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                long target = parseServerTimeMillis(order != null ? order.getPayExpireAt() : null);
                long remain = target > 0 ? target - System.currentTimeMillis() : 0;
                if (remain <= 0) {
                    tvOrderHint.setText("Payment time has expired. Please refresh the order status.");
                } else {
                    tvOrderHint.setText("Please settle your payment within " + formatDuration(remain) + " to avoid order cancellation.");
                    handler.postDelayed(this, 1000);
                }
            }
        };
        countdownRunnable.run();
    }

    private void startReceiveCountdown() {
        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                long paidAt = parseServerTimeMillis(order != null ? order.getPaidAt() : null);
                long target = paidAt > 0 ? paidAt + TimeUnit.DAYS.toMillis(RECEIVE_DEADLINE_DAYS) : 0;
                long remain = target > 0 ? target - System.currentTimeMillis() : 0;
                if (remain > 0) {
                    tvOrderHint.setText("Please confirm receipt of your goods within " + formatReceiveDuration(remain) + ".");
                    handler.postDelayed(this, 60000);
                } else {
                    tvOrderHint.setText("Please confirm receipt of your goods within 7 days.");
                }
            }
        };
        countdownRunnable.run();
    }

    private void stopCountdown() {
        if (countdownRunnable != null && handler != null) {
            handler.removeCallbacks(countdownRunnable);
        }
        countdownRunnable = null;
    }

    private void handlePrimaryAction() {
        if (order == null) {
            loadOrder();
            return;
        }

        String status = safe(order.getStatus());
        if (OrderVo.STATUS_CREATED.equals(status)) {
            if (isBuyer()) {
                payOrder();
            }
        } else if (OrderVo.STATUS_PAID.equals(status)) {
            if (isBuyer()) {
                confirmReceiptDialog();
            }
        } else if (OrderVo.STATUS_SETTLED.equals(status)) {
            startActivity(new Intent(this, HistoricalPurchaseActivity.class));
        } else if (OrderVo.STATUS_CANCELLED.equals(status)) {
            openGoodsDetail();
        } else {
            loadOrder();
        }
    }

    private void handleSecondaryAction() {
        if (order != null && OrderVo.STATUS_CREATED.equals(order.getStatus()) && isBuyer()) {
            new AlertDialog.Builder(this)
                    .setTitle("Cancel Order")
                    .setMessage("Are you sure you want to cancel this order?")
                    .setPositiveButton("Cancel order", (dialog, which) -> cancelOrder())
                    .setNegativeButton("Keep order", null)
                    .show();
        }
    }

    private void payOrder() {
        if (!ensureBuyerActionReady()) {
            return;
        }

        Request request = new Request.Builder()
                .url(baseUrl + "/app/orders/" + orderId + "/pay/alipay")
                .addHeader("access-token", token)
                .post(RequestBody.create("", JSON))
                .build();

        setActionLoading("Paying...");
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    renderOrder();
                    Toast.makeText(OrderDetailActivity.this, "Payment request failed", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body() != null ? response.body().string() : "";
                Result<PaymentInfo> result = parseResult(responseData, new TypeToken<Result<PaymentInfo>>() {
                }.getType());

                runOnUiThread(() -> {
                    renderOrder();
                    if (response.isSuccessful() && result != null && result.getCode() == 200 && result.getData() != null) {
                        showSandboxPayDialog(result.getData());
                    } else {
                        showError("Payment Failed", result != null ? result.getMessage() : "Server error: " + response.code());
                    }
                });
            }
        });
    }

    private void showSandboxPayDialog(PaymentInfo paymentInfo) {
        String amount = getOrderStringParam(paymentInfo.orderString, "total_amount");
        if (isEmpty(paymentInfo.payNo) || isEmpty(amount)) {
            showError("Payment Failed", "Payment parameters are incomplete.");
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Sandbox Payment")
                .setMessage("Pay No: " + paymentInfo.payNo + "\nAmount: ¥ " + amount + "\n\nClick Mock Paid to simulate Alipay success.")
                .setPositiveButton("Mock Paid", (dialog, which) -> mockAlipayPaid(paymentInfo.payNo, amount))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void mockAlipayPaid(String payNo, String amount) {
        AlipayNotifyRequest body = new AlipayNotifyRequest(
                payNo,
                "MOCK_TRADE_" + System.currentTimeMillis(),
                amount,
                "TRADE_SUCCESS"
        );
        Request request = new Request.Builder()
                .url(baseUrl + "/app/payments/alipay/notify")
                .post(RequestBody.create(gson.toJson(body), JSON))
                .build();

        setActionLoading("Syncing...");
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    renderOrder();
                    Toast.makeText(OrderDetailActivity.this, "Payment callback failed", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body() != null ? response.body().string() : "";
                boolean success = response.isSuccessful() && "success".equalsIgnoreCase(responseData.trim());
                runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(OrderDetailActivity.this, "Payment successful, syncing order status", Toast.LENGTH_SHORT).show();
                        refreshPaidStatusWithRetry();
                    } else {
                        renderOrder();
                        showError("Payment Failed", "Payment callback was not accepted.");
                    }
                });
            }
        });
    }

    private void refreshPaidStatusWithRetry() {
        statusRefreshAttempts = 0;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isFinishing()) {
                    return;
                }
                statusRefreshAttempts++;
                loadOrder();
                if (statusRefreshAttempts < MAX_STATUS_REFRESH_ATTEMPTS
                        && order != null
                        && !OrderVo.STATUS_PAID.equals(order.getStatus())
                        && !OrderVo.STATUS_SETTLED.equals(order.getStatus())) {
                    handler.postDelayed(this, 1500);
                }
            }
        }, 1200);
    }

    private void confirmReceiptDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Receipt")
                .setMessage("Are you sure you have received the goods?")
                .setPositiveButton("Confirm", (dialog, which) -> confirmReceipt())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmReceipt() {
        if (!ensureBuyerActionReady()) {
            return;
        }

        Request request = new Request.Builder()
                .url(baseUrl + "/app/orders/" + orderId + "/confirm-receipt")
                .addHeader("access-token", token)
                .post(RequestBody.create("", JSON))
                .build();

        setActionLoading("Confirming...");
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    renderOrder();
                    Toast.makeText(OrderDetailActivity.this, "Confirm receipt failed", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body() != null ? response.body().string() : "";
                Result<OrderVo> result = parseResult(responseData, new TypeToken<Result<OrderVo>>() {
                }.getType());
                runOnUiThread(() -> {
                    if (response.isSuccessful() && result != null && result.getCode() == 200 && result.getData() != null) {
                        order = result.getData();
                        renderOrder();
                        Toast.makeText(OrderDetailActivity.this, "Receipt confirmed", Toast.LENGTH_SHORT).show();
                    } else {
                        renderOrder();
                        showError("Confirm Receipt Failed", result != null ? result.getMessage() : "Server error: " + response.code());
                    }
                });
            }
        });
    }

    private void cancelOrder() {
        if (!ensureBuyerActionReady()) {
            return;
        }

        Request request = new Request.Builder()
                .url(baseUrl + "/app/orders/" + orderId + "/cancel")
                .addHeader("access-token", token)
                .post(RequestBody.create("", JSON))
                .build();

        setActionLoading("Cancelling...");
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    renderOrder();
                    Toast.makeText(OrderDetailActivity.this, "Cancel order failed", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body() != null ? response.body().string() : "";
                Result<OrderVo> result = parseResult(responseData, new TypeToken<Result<OrderVo>>() {
                }.getType());
                runOnUiThread(() -> {
                    if (response.isSuccessful() && result != null && result.getCode() == 200 && result.getData() != null) {
                        order = result.getData();
                        renderOrder();
                        Toast.makeText(OrderDetailActivity.this, "Order cancelled", Toast.LENGTH_SHORT).show();
                    } else {
                        renderOrder();
                        showError("Cancel Order Failed", result != null ? result.getMessage() : "Server error: " + response.code());
                    }
                });
            }
        });
    }

    private void openGoodsDetail() {
        if (order == null || order.getGoodsId() == null || order.getGoodsId() <= 0) {
            return;
        }
        Intent intent = new Intent(this, GoodsDetailActivity.class);
        intent.putExtra("product_good_id", order.getGoodsId());
        if (!isEmpty(cachedImageUrl)) {
            intent.putExtra("product_image_url", cachedImageUrl);
        }
        startActivity(intent);
    }

    private void showPrimaryAction(String text, boolean enabled) {
        btnPrimaryAction.setText(text);
        btnPrimaryAction.setEnabled(enabled);
        btnPrimaryAction.setAlpha(enabled ? 1F : 0.72F);
    }

    private void setActionLoading(String text) {
        btnPrimaryAction.setText(text);
        btnPrimaryAction.setEnabled(false);
        tvSecondaryAction.setVisibility(View.GONE);
    }

    private boolean ensureLoggedIn() {
        if (currentUserId <= 0 || token == null || token.isEmpty()) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return false;
        }
        return true;
    }

    private boolean ensureBuyerActionReady() {
        if (!ensureLoggedIn()) {
            return false;
        }
        if (order == null || order.getOrderId() == null || order.getOrderId() <= 0) {
            Toast.makeText(this, "Order information is invalid", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!isBuyer()) {
            Toast.makeText(this, "Only the buyer can perform this action", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private boolean isBuyer() {
        return order != null && order.getBuyerId() != null && order.getBuyerId() == currentUserId;
    }

    private void loadGoodsImage(String rawUrl) {
        String normalized = ImageUrlUtils.normalize(this, rawUrl);
        if (!isEmpty(normalized)) {
            Glide.with(this)
                    .load(normalized)
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.placeholder)
                    .centerCrop()
                    .into(ivGoodsImg);
        } else {
            ivGoodsImg.setImageResource(R.drawable.placeholder);
        }
    }

    private String getOrderStringParam(String orderString, String name) {
        if (orderString == null || orderString.isEmpty()) {
            return "";
        }
        return Uri.parse("https://campusmart.pay/?" + orderString).getQueryParameter(name);
    }

    private String formatAmount(String amount) {
        if (isEmpty(amount)) {
            return "0.00";
        }
        try {
            return new DecimalFormat("0.00").format(Double.parseDouble(amount));
        } catch (NumberFormatException e) {
            return amount;
        }
    }

    private String formatDuration(long millis) {
        long totalSeconds = Math.max(0, millis / 1000);
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return minutes + "min" + String.format(Locale.US, "%02ds", seconds);
    }

    private String formatReceiveDuration(long millis) {
        long days = Math.max(1, (long) Math.ceil(millis / (double) TimeUnit.DAYS.toMillis(1)));
        return days + (days == 1 ? " day" : " days");
    }

    private String formatServerTime(String raw) {
        long millis = parseServerTimeMillis(raw);
        if (millis <= 0) {
            return "--";
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date(millis));
    }

    private long parseServerTimeMillis(String raw) {
        if (isEmpty(raw)) {
            return 0;
        }
        String value = raw.trim();
        String normalized = removeFractionalSeconds(value);
        String[] patterns = new String[]{
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd HH:mm:ss"
        };

        for (String pattern : patterns) {
            try {
                SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
                Date date = format.parse(normalized);
                if (date != null) {
                    return date.getTime();
                }
            } catch (Exception ignored) {
            }
        }
        return 0;
    }

    private String removeFractionalSeconds(String value) {
        int dotIndex = value.indexOf('.');
        if (dotIndex < 0) {
            return value;
        }

        int suffixIndex = -1;
        for (int i = dotIndex + 1; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '+' || c == '-' || c == 'Z') {
                suffixIndex = i;
                break;
            }
        }
        if (suffixIndex >= 0) {
            return value.substring(0, dotIndex) + value.substring(suffixIndex);
        }
        return value.substring(0, dotIndex);
    }

    private <T> Result<T> parseResult(String responseData, Type type) {
        try {
            return gson.fromJson(responseData, type);
        } catch (Exception e) {
            return null;
        }
    }

    private void showError(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(!isEmpty(message) ? message : "Unknown error")
                .setPositiveButton("OK", null)
                .show();
    }

    private String firstNonEmpty(String first, String second) {
        if (!isEmpty(first)) {
            return first;
        }
        return !isEmpty(second) ? second : null;
    }

    private String safe(String value) {
        return value != null ? value : "";
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static class PaymentInfo {
        private String payNo;
        private String orderString;
    }

    private static class AlipayNotifyRequest {
        private final String out_trade_no;
        private final String trade_no;
        private final String total_amount;
        private final String trade_status;

        private AlipayNotifyRequest(String outTradeNo, String tradeNo, String totalAmount, String tradeStatus) {
            this.out_trade_no = outTradeNo;
            this.trade_no = tradeNo;
            this.total_amount = totalAmount;
            this.trade_status = tradeStatus;
        }
    }
}
