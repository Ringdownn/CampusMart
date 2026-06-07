package com.example.campusmart;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.campusmart.adapter.ChatAdapter;
import com.example.campusmart.entity.Message;
import com.example.campusmart.result.Result;
import com.example.campusmart.util.AlipayPaymentHelper;
import com.example.campusmart.util.ImageUrlUtils;
import com.example.campusmart.util.OrderMessageNotifier;
import com.example.campusmart.vo.MessageVo;
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
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class ChatActivity extends AppCompatActivity {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private RecyclerView rvChat;
    private EditText etInput;
    private ChatAdapter adapter;
    private List<ChatAdapter.ChatMsg> msgList;
    private TextView tvTitle;
    private View chatRoot;
    private int chatRootPaddingLeft;
    private int chatRootPaddingTop;
    private int chatRootPaddingRight;
    private int chatRootPaddingBottom;

    private Long currentUserId;
    private Long otherUserId;
    private Long goodId;
    private String otherNickname;
    private String goodTitle;
    private String token;

    private OkHttpClient client;
    private Gson gson;
    private String baseUrl;
    private String selfAvatarUrl;
    private String otherAvatarUrl;
    private ImageView ivGoodsImg;
    private View layoutGoodsInfo;
    private TextView tvGoodsTitle;
    private TextView tvGoodsPrice;
    private TextView tvGoodsStatus;
    private Button btnGoodsAction;
    private String goodImageUrl;
    private double goodPrice;
    private boolean isBuyer;
    private Long orderId;
    private String orderStatus;
    private WebSocket webSocket;
    private boolean webSocketConnected = false;
    private boolean paymentNoticePending = false;
    private boolean paymentNoticeSent = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        setContentView(R.layout.activity_chat);

        // 初始化控件
        ImageView ivBack = findViewById(R.id.iv_back);
        chatRoot = findViewById(R.id.chat_root);
        rvChat = findViewById(R.id.rv_chat);
        etInput = findViewById(R.id.et_input);
        tvTitle = findViewById(R.id.tv_title);
        // 初始化商品信息栏控件
        layoutGoodsInfo = findViewById(R.id.layout_goods_info);
        ivGoodsImg = findViewById(R.id.iv_goods_img);
        tvGoodsTitle = findViewById(R.id.tv_goods_title);
        tvGoodsPrice = findViewById(R.id.tv_goods_price);
        btnGoodsAction = findViewById(R.id.btn_goods_action);
        tvGoodsStatus = findViewById(R.id.tv_goods_status);
        // 初始化发送按钮
        findViewById(R.id.btn_send).setOnClickListener(v -> sendMessage());

        // 初始化网络工具
        client = new OkHttpClient();
        gson = new Gson();
        baseUrl = getResources().getString(R.string.base_url);

        // 获取传递过来的参数
        getIntentData();

        // 初始化消息数据列表
        initMsgData();

        // 设置标题为对方昵称
        if (goodTitle != null && !goodTitle.isEmpty()) {
            tvTitle.setText(otherNickname + " - " + goodTitle);
        } else {
            tvTitle.setText(otherNickname);
        }

        // 初始化RecyclerView
        adapter = new ChatAdapter(msgList, selfAvatarUrl, otherAvatarUrl);
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setAdapter(adapter);

        // 返回按钮点击事件
        ivBack.setOnClickListener(v -> finish());

        // 初始化商品信息栏
        initGoodsInfoBar();
        setupKeyboardAwareInputBar();

        // 加载历史聊天记录
        loadChatHistory();
        connectWebSocket();
    }

    private void getIntentData() {
        currentUserId = getIntent().getLongExtra("currentUserId", 0);
        otherUserId = getIntent().getLongExtra("otherUserId", 0);
        goodId = getIntent().getLongExtra("goodId", 0);
        otherNickname = getIntent().getStringExtra("otherNickname");
        goodTitle = getIntent().getStringExtra("goodTitle");
        token = getIntent().getStringExtra("token");
        selfAvatarUrl = getIntent().getStringExtra("selfAvatarUrl");
        otherAvatarUrl = getIntent().getStringExtra("otherAvatarUrl");
        goodImageUrl = getIntent().getStringExtra("goodImageUrl");
        selfAvatarUrl = ImageUrlUtils.normalize(this, selfAvatarUrl);
        otherAvatarUrl = ImageUrlUtils.normalize(this, otherAvatarUrl);
        goodImageUrl = ImageUrlUtils.normalize(this, goodImageUrl);
        goodPrice = getIntent().getDoubleExtra("goodPrice", 0);
        isBuyer = getIntent().getBooleanExtra("isBuyer", true);
    }

    private void initMsgData() {
        msgList = new ArrayList<>();
    }

    private void setupKeyboardAwareInputBar() {
        if (chatRoot == null) {
            return;
        }

        chatRootPaddingLeft = chatRoot.getPaddingLeft();
        chatRootPaddingTop = chatRoot.getPaddingTop();
        chatRootPaddingRight = chatRoot.getPaddingRight();
        chatRootPaddingBottom = chatRoot.getPaddingBottom();

        chatRoot.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect visibleFrame = new Rect();
            chatRoot.getWindowVisibleDisplayFrame(visibleFrame);

            int rootHeight = chatRoot.getRootView().getHeight();
            int hiddenHeight = rootHeight - visibleFrame.bottom;
            int keyboardThreshold = (int) (rootHeight * 0.15f);
            int extraBottomPadding = hiddenHeight > keyboardThreshold ? hiddenHeight : 0;
            int targetBottomPadding = chatRootPaddingBottom + extraBottomPadding;

            if (chatRoot.getPaddingBottom() == targetBottomPadding) {
                return;
            }

            chatRoot.setPadding(
                    chatRootPaddingLeft,
                    chatRootPaddingTop,
                    chatRootPaddingRight,
                    targetBottomPadding
            );

            if (extraBottomPadding > 0 && msgList != null && !msgList.isEmpty()) {
                rvChat.post(() -> rvChat.scrollToPosition(msgList.size() - 1));
            }
        });
    }

    /**
     * 初始化商品信息栏
     * 1. 显示商品图片、名称、价格
     * 2. 查询订单状态
     * 3. 根据角色和状态显示对应按钮/文本
     */
    private void initGoodsInfoBar() {
        // 显示商品基本信息
        tvGoodsTitle.setText(goodTitle != null ? goodTitle : "Good name");
        tvGoodsPrice.setText(String.format("¥ %.2f", goodPrice));
        if (goodImageUrl != null && !goodImageUrl.isEmpty()) {
            Glide.with(this).load(ImageUrlUtils.normalize(this, goodImageUrl)).placeholder(R.drawable.placeholder).into(ivGoodsImg);
        }
        layoutGoodsInfo.setOnClickListener(v -> openOrderDetail());

        // 查询订单状态（预留接口）
        loadOrderStatus();
    }

    private void openOrderDetail() {
        if (orderId == null || orderId <= 0) {
            Toast.makeText(this, "Please create an order first", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(ChatActivity.this, OrderDetailActivity.class);
        intent.putExtra("orderId", orderId);
        intent.putExtra("goodTitle", goodTitle);
        intent.putExtra("goodImageUrl", goodImageUrl);
        intent.putExtra("goodPrice", goodPrice);
        startActivity(intent);
    }

    /**
     * 查询订单状态
     * GET /app/orders/by-goods?goodsId={}&buyerId={}&sellerId={}
     */
    private void loadOrderStatus() {
        if (currentUserId == 0 || otherUserId == 0 || goodId == 0 || token == null || token.isEmpty()) {
            updateGoodsActionUI(null);
            return;
        }

        long buyerId = isBuyer ? currentUserId : otherUserId;
        long sellerId = isBuyer ? otherUserId : currentUserId;
        requestLatestOrderStatus(buyerId, sellerId, true);
    }

    private void requestLatestOrderStatus(long buyerId, long sellerId, boolean allowSwap) {
        String url = baseUrl + "/app/orders/by-goods?goodsId=" + goodId
                + "&buyerId=" + buyerId
                + "&sellerId=" + sellerId;
        Request request = new Request.Builder()
                .url(url)
                .addHeader("access-token", token)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    orderId = null;
                    updateGoodsActionUI(null);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    Result<OrderInfo> result = parseResult(responseData, new TypeToken<Result<OrderInfo>>(){}.getType());
                    OrderInfo orderInfo = result != null ? result.getData() : null;
                    if (result != null && result.getCode() == 200 && orderInfo != null) {
                        runOnUiThread(() -> {
                            String previousStatus = orderStatus;
                            orderId = orderInfo.orderId;
                            isBuyer = currentUserId.equals(orderInfo.buyerId);
                            updateGoodsActionUI(orderInfo.status);
                            maybeNotifyPaymentSuccess(previousStatus, orderInfo.status);
                        });
                    } else if (allowSwap) {
                        requestLatestOrderStatus(sellerId, buyerId, false);
                    } else {
                        runOnUiThread(() -> {
                            orderId = null;
                            updateGoodsActionUI(null);
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        orderId = null;
                        updateGoodsActionUI(null);
                    });
                }
            }
        });
    }

    /**
     * 根据角色和订单状态更新商品信息栏的按钮/文本
     */
    private void updateGoodsActionUI(String status) {
        this.orderStatus = status;
        String safeStatus = status != null ? status : "";

        if (isBuyer) {
            // 买家视角
            switch (safeStatus) {
                case "CREATED":
                    // 已创建订单，待付款
                    showActionButton("Pay", this::onPayClick);
                    break;
                case "PAID":
                    // 已付款，待确认收货
                    showActionButton("Confirm receipt", this::onConfirmReceiptClick);
                    break;
                case "SETTLED":
                    // 已完成
                    showStatusText("Completed");
                    break;
                case "CANCELLED":
                    // 已取消，可重新购买
                    showActionButton("Buy it!", this::onBuyClick);
                    break;
                default:
                    // 无订单
                    showActionButton("Buy it!", this::onBuyClick);
                    break;
            }
        } else {
            // Seller视角
            switch (safeStatus) {
                case "CREATED":
                    showStatusText("Waiting payment");
                    break;
                case "PAID":
                    showStatusText("Waiting buyer receipt");
                    break;
                case "SETTLED":
                    showStatusText("Completed");
                    break;
                case "CANCELLED":
                    showStatusText("Order cancelled");
                    break;
                default:
                    // 无订单，Seller不显示按钮
                    btnGoodsAction.setVisibility(Button.GONE);
                    tvGoodsStatus.setVisibility(TextView.GONE);
                    break;
            }
        }
    }

    /**
     * 显示操作按钮
     */
    private void showActionButton(String text, Runnable onClick) {
        btnGoodsAction.setText(text);
        btnGoodsAction.setEnabled(true);
        btnGoodsAction.setVisibility(Button.VISIBLE);
        tvGoodsStatus.setVisibility(TextView.GONE);
        btnGoodsAction.setOnClickListener(v -> onClick.run());
    }

    /**
     * 显示状态文本（禁用状态）
     */
    private void showStatusText(String text) {
        tvGoodsStatus.setText(text);
        tvGoodsStatus.setVisibility(TextView.VISIBLE);
        btnGoodsAction.setVisibility(Button.GONE);
    }

    /**
     * 点击"Buy it!" - 创建订单
     */
    private void onBuyClick() {
        if (!ensureBuyerActionReady()) {
            return;
        }

        String url = baseUrl + "/app/orders";
        CreateOrderRequest body = new CreateOrderRequest(goodId);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("access-token", token)
                .post(RequestBody.create(gson.toJson(body), JSON))
                .build();

        setActionLoading("Creating...");
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    updateGoodsActionUI(orderStatus);
                    Toast.makeText(ChatActivity.this, "Create failed. Check network.", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body() != null ? response.body().string() : "";
                Result<OrderInfo> result = parseResult(responseData, new TypeToken<Result<OrderInfo>>(){}.getType());

                runOnUiThread(() -> {
                    if (response.isSuccessful() && result != null && result.getCode() == 200 && result.getData() != null) {
                        OrderInfo order = result.getData();
                        orderId = order.orderId;
                        isBuyer = currentUserId.equals(order.buyerId);
                        paymentNoticePending = false;
                        paymentNoticeSent = false;
                        updateGoodsActionUI(order.status);
                        notifySeller(OrderMessageNotifier.ORDER_CREATED_MESSAGE);
                        Toast.makeText(ChatActivity.this, "Order created. Please pay.", Toast.LENGTH_SHORT).show();
                    } else {
                        updateGoodsActionUI(orderStatus);
                        showError("Create Order Failed", result != null ? result.getMessage() : "Server error");
                    }
                });
            }
        });
    }

    /**
     * 点击"Pay" - 获取支付宝沙箱支付参数
     */
    private void onPayClick() {
        if (!ensureBuyerActionReady() || orderId == null || orderId <= 0) {
            Toast.makeText(this, "Invalid order. Reopen chat.", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = baseUrl + "/app/orders/" + orderId + "/pay/alipay";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("access-token", token)
                .post(RequestBody.create("", JSON))
                .build();

        setActionLoading("Paying...");
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    updateGoodsActionUI(orderStatus);
                    Toast.makeText(ChatActivity.this, "Payment failed. Check network.", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body() != null ? response.body().string() : "";
                Result<PaymentInfo> result = parseResult(responseData, new TypeToken<Result<PaymentInfo>>(){}.getType());

                runOnUiThread(() -> {
                    if (response.isSuccessful() && result != null && result.getCode() == 200 && result.getData() != null) {
                        startAlipayPayment(result.getData());
                    } else {
                        updateGoodsActionUI(orderStatus);
                        showError("Payment Failed", result != null ? result.getMessage() : "Server error");
                    }
                });
            }
        });
    }

    /**
     * 点击"Confirm receipt" - 确认收货
     */
    private void onConfirmReceiptClick() {
        if (!ensureBuyerActionReady() || orderId == null || orderId <= 0) {
            Toast.makeText(this, "Invalid order. Reopen chat.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Confirm Receipt")
                .setMessage("Are you sure you have received the goods?")
                .setPositiveButton("Confirm", (dialog, which) -> confirmReceipt())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void startAlipayPayment(PaymentInfo paymentInfo) {
        if (paymentInfo == null || paymentInfo.orderString == null || paymentInfo.orderString.trim().isEmpty()) {
            updateGoodsActionUI(orderStatus);
            showError("Payment Failed", "Invalid payment info.");
            return;
        }

        setActionLoading("Opening Alipay...");
        AlipayPaymentHelper.pay(this, paymentInfo.orderString, payResult -> {
            if (payResult.isSuccess() || payResult.isProcessing()) {
                paymentNoticePending = true;
                Toast.makeText(ChatActivity.this, payResult.message(), Toast.LENGTH_SHORT).show();
                btnGoodsAction.postDelayed(() -> ChatActivity.this.loadOrderStatus(), 1500);
                btnGoodsAction.postDelayed(() -> ChatActivity.this.loadOrderStatus(), 3500);
            } else {
                if (payResult.isCancelled()) {
                    Toast.makeText(ChatActivity.this, payResult.message(), Toast.LENGTH_SHORT).show();
                } else {
                    showError("Payment Failed", payResult.message());
                }
                updateGoodsActionUI(orderStatus);
            }
        });
    }

    private void confirmReceipt() {
        String url = baseUrl + "/app/orders/" + orderId + "/confirm-receipt";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("access-token", token)
                .post(RequestBody.create("", JSON))
                .build();

        setActionLoading("Confirming...");
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    updateGoodsActionUI(orderStatus);
                    Toast.makeText(ChatActivity.this, "Confirm failed. Check network.", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body() != null ? response.body().string() : "";
                Result<OrderInfo> result = parseResult(responseData, new TypeToken<Result<OrderInfo>>(){}.getType());

                runOnUiThread(() -> {
                    if (response.isSuccessful() && result != null && result.getCode() == 200 && result.getData() != null) {
                        OrderInfo order = result.getData();
                        orderId = order.orderId;
                        updateGoodsActionUI(order.status);
                        notifySeller(OrderMessageNotifier.ORDER_RECEIVED_MESSAGE);
                        Toast.makeText(ChatActivity.this, "Receipt confirmed!", Toast.LENGTH_SHORT).show();
                    } else {
                        updateGoodsActionUI(orderStatus);
                        showError("Confirm Receipt Failed", result != null ? result.getMessage() : "Server error");
                    }
                });
            }
        });
    }

    private boolean ensureBuyerActionReady() {
        if (!isBuyer) {
            Toast.makeText(this, "Seller cannot do this.", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (currentUserId == 0 || goodId == 0 || token == null || token.isEmpty()) {
            Toast.makeText(this, "Invalid action.", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void setActionLoading(String text) {
        btnGoodsAction.setText(text);
        btnGoodsAction.setEnabled(false);
    }

    private void maybeNotifyPaymentSuccess(String previousStatus, String currentStatus) {
        if (!paymentNoticePending || paymentNoticeSent || !isBuyer) {
            return;
        }
        boolean wasUnpaid = previousStatus == null || "CREATED".equals(previousStatus);
        boolean isPaid = "PAID".equals(currentStatus) || "SETTLED".equals(currentStatus);
        if (wasUnpaid && isPaid) {
            paymentNoticePending = false;
            paymentNoticeSent = true;
            notifySeller(OrderMessageNotifier.ORDER_PAID_MESSAGE);
        }
    }

    private void notifySeller(String content) {
        OrderMessageNotifier.notifySeller(
                this,
                client,
                baseUrl,
                token,
                currentUserId,
                otherUserId,
                goodId,
                content
        );
    }

    private void showError(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message != null && !message.isEmpty() ? message : "Unknown error")
                .setPositiveButton("OK", null)
                .show();
    }

    private <T> Result<T> parseResult(String responseData, Type type) {
        try {
            return gson.fromJson(responseData, type);
        } catch (Exception e) {
            return null;
        }
    }

    private void loadChatHistory() {
        if (currentUserId == 0 || otherUserId == 0 || goodId == 0 || token == null || token.isEmpty()) {
            Toast.makeText(this, "Invalid chat.", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = baseUrl + "/app/messages/list?senderId=" + currentUserId
                + "&receiverId=" + otherUserId
                + "&goodId=" + goodId;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("access-token", token)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(ChatActivity.this, "Load failed. Check network.", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    Result<List<MessageVo>> result = gson.fromJson(
                            responseData,
                            new TypeToken<Result<List<MessageVo>>>(){}.getType()
                    );

                    runOnUiThread(() -> {
                        if (result.getCode() == 200 && result.getData() != null) {
                            updateChatList(result.getData());
                        } else {
                            Toast.makeText(ChatActivity.this, "Load failed: " + result.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(ChatActivity.this, "Server error", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    private void sendMessage() {
        String content = etInput.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "Enter a message.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUserId == 0 || otherUserId == 0 || goodId == 0 || token == null || token.isEmpty()) {
            Toast.makeText(this, "Send failed.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (webSocket == null || !webSocketConnected) {
            connectWebSocket();
            Toast.makeText(this, "Connecting. Try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        Message message = new Message(
                currentUserId,
                otherUserId,
                goodId,
                content
        );

        boolean accepted = webSocket.send(gson.toJson(message));
        if (accepted) {
            etInput.setText("");
        } else {
            Toast.makeText(this, "Not connected.", Toast.LENGTH_SHORT).show();
        }
    }

    private void connectWebSocket() {
        if (currentUserId == 0 || token == null || token.isEmpty()) {
            return;
        }
        if (webSocket != null && webSocketConnected) {
            return;
        }

        String wsUrl = toWebSocketUrl(baseUrl) + "/ws?userId=" + currentUserId + "&token=" + Uri.encode(token);
        Request request = new Request.Builder()
                .url(wsUrl)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                webSocketConnected = true;
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                Message message = gson.fromJson(text, Message.class);
                if (message == null || message.getMessageContent() == null) {
                    return;
                }

                Long senderId = message.getSenderID();
                Long receiverId = message.getReceiverID();
                Long messageGoodId = message.getGoodID();
                boolean belongsToCurrentChat =
                        goodId.equals(messageGoodId) &&
                                ((currentUserId.equals(senderId) && otherUserId.equals(receiverId)) ||
                                        (currentUserId.equals(receiverId) && otherUserId.equals(senderId)));
                if (!belongsToCurrentChat) {
                    return;
                }

                runOnUiThread(() -> addMessageToList(currentUserId.equals(senderId), message.getMessageContent()));
            }

            @Override
            public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                webSocketConnected = false;
            }

            @Override
            public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, Response response) {
                webSocketConnected = false;
                if (!isFinishing()) {
                    runOnUiThread(() ->
                            Toast.makeText(ChatActivity.this, "Chat failed. Check network.", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    private String toWebSocketUrl(String httpUrl) {
        if (httpUrl.startsWith("https://")) {
            return "wss://" + httpUrl.substring("https://".length());
        }
        if (httpUrl.startsWith("http://")) {
            return "ws://" + httpUrl.substring("http://".length());
        }
        return httpUrl;
    }

    private void addMessageToList(boolean isSelf, String content) {
        msgList.add(new ChatAdapter.ChatMsg(isSelf, content));
        adapter.notifyItemInserted(msgList.size() - 1);
        rvChat.scrollToPosition(msgList.size() - 1);
    }

    private void updateChatList(List<MessageVo> messageVos) {
        for (MessageVo vo : messageVos) {
            boolean isSelf = vo.getSenderID().equals(currentUserId);
            msgList.add(new ChatAdapter.ChatMsg(isSelf, vo.getMessageContent()));
        }
        adapter.notifyDataSetChanged();
        // 滚动到最后一条消息
        if (msgList.size() > 0) {
            rvChat.scrollToPosition(msgList.size() - 1);
        }
    }

    @Override
    protected void onDestroy() {
        if (webSocket != null) {
            webSocket.close(1000, "chat closed");
        }
        super.onDestroy();
    }

    private static class OrderInfo {
        private Long orderId;
        private Long buyerId;
        private String status;
    }

    private static class CreateOrderRequest {
        private final Long goodsId;

        private CreateOrderRequest(Long goodsId) {
            this.goodsId = goodsId;
        }
    }

    private static class PaymentInfo {
        private String payNo;
        private String orderString;
    }
}
