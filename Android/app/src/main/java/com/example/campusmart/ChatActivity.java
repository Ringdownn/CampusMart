package com.example.campusmart;

import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.campusmart.adapter.ChatAdapter;
import com.example.campusmart.entity.Message;
import com.example.campusmart.result.Result;
import com.example.campusmart.vo.MessageVo;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class ChatActivity extends AppCompatActivity {
    private RecyclerView rvChat;
    private EditText etInput;
    private ChatAdapter adapter;
    private List<ChatAdapter.ChatMsg> msgList;
    private TextView tvTitle;

    private Long currentUserId;
    private Long otherUserId;
    private String otherNickname;
    private String token;

    private OkHttpClient client;
    private Gson gson;
    private String baseUrl;
    private String selfAvatarUrl;
    private String otherAvatarUrl;
    private WebSocket webSocket;
    private boolean webSocketConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // 初始化控件
        ImageView ivBack = findViewById(R.id.iv_back);
        rvChat = findViewById(R.id.rv_chat);
        etInput = findViewById(R.id.et_input);
        tvTitle = findViewById(R.id.tv_title);
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
        tvTitle.setText(otherNickname);

        // 初始化RecyclerView
        adapter = new ChatAdapter(msgList, selfAvatarUrl, otherAvatarUrl);
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setAdapter(adapter);

        // 返回按钮点击事件
        ivBack.setOnClickListener(v -> finish());

        // 加载历史聊天记录
        loadChatHistory();
        connectWebSocket();
    }

    private void getIntentData() {
        currentUserId = getIntent().getLongExtra("currentUserId", 0);
        otherUserId = getIntent().getLongExtra("otherUserId", 0);
        otherNickname = getIntent().getStringExtra("otherNickname");
        token = getIntent().getStringExtra("token");
        selfAvatarUrl = getIntent().getStringExtra("selfAvatarUrl");
        otherAvatarUrl = getIntent().getStringExtra("otherAvatarUrl");
    }

    private void initMsgData() {
        msgList = new ArrayList<>();
    }

    private void loadChatHistory() {
        if (currentUserId == 0 || otherUserId == 0 || token == null || token.isEmpty()) {
            Toast.makeText(this, "聊天参数异常", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = baseUrl + "/app/messages/list?senderId=" + currentUserId + "&receiverId=" + otherUserId;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("access-token", token)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(ChatActivity.this, "加载聊天记录失败，请检查网络", Toast.LENGTH_SHORT).show()
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
                            Toast.makeText(ChatActivity.this, "获取聊天记录失败：" + result.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(ChatActivity.this, "服务器响应异常", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    private void sendMessage() {
        String content = etInput.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "请输入消息内容", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUserId == 0 || otherUserId == 0 || token == null || token.isEmpty()) {
            Toast.makeText(this, "发送失败，参数异常", Toast.LENGTH_SHORT).show();
            return;
        }

        if (webSocket == null || !webSocketConnected) {
            connectWebSocket();
            Toast.makeText(this, "正在连接消息服务，请稍后重试", Toast.LENGTH_SHORT).show();
            return;
        }

        Message message = new Message(
                currentUserId,
                otherUserId,
                content
        );

        boolean accepted = webSocket.send(gson.toJson(message));
        if (accepted) {
            etInput.setText("");
        } else {
            Toast.makeText(this, "发送失败，消息连接不可用", Toast.LENGTH_SHORT).show();
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
                boolean belongsToCurrentChat =
                        (currentUserId.equals(senderId) && otherUserId.equals(receiverId)) ||
                                (currentUserId.equals(receiverId) && otherUserId.equals(senderId));
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
                            Toast.makeText(ChatActivity.this, "消息连接失败，请检查网络", Toast.LENGTH_SHORT).show()
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
}