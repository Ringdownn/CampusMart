package com.example.campusmart.util;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.example.campusmart.entity.Message;
import com.google.gson.Gson;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public final class OrderMessageNotifier {
    public static final String ORDER_CREATED_MESSAGE = "I placed an order. Please check it.";
    public static final String ORDER_PAID_MESSAGE = "I paid. Please check the order.";
    public static final String ORDER_RECEIVED_MESSAGE = "I received it. Order done.";

    private OrderMessageNotifier() {
    }

    public static void notifySeller(
            Context context,
            OkHttpClient client,
            String baseUrl,
            String token,
            Long buyerId,
            Long sellerId,
            Long goodsId,
            String content
    ) {
        if (context == null || client == null || isEmpty(baseUrl) || isEmpty(token)
                || buyerId == null || buyerId <= 0
                || sellerId == null || sellerId <= 0
                || goodsId == null || goodsId <= 0
                || isEmpty(content)) {
            return;
        }

        String wsUrl = toWebSocketUrl(baseUrl) + "/ws?userId=" + buyerId + "&token=" + Uri.encode(token);
        Request request = new Request.Builder()
                .url(wsUrl)
                .addHeader("Authorization", "Bearer " + token)
                .build();
        Message message = new Message(buyerId, sellerId, goodsId, content);
        String payload = new Gson().toJson(message);

        client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                webSocket.send(payload);
                webSocket.close(1000, "order notice sent");
            }

            @Override
            public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, Response response) {
                webSocket.cancel();
            }
        });
    }

    private static String toWebSocketUrl(String httpUrl) {
        if (httpUrl.startsWith("https://")) {
            return "wss://" + httpUrl.substring("https://".length());
        }
        if (httpUrl.startsWith("http://")) {
            return "ws://" + httpUrl.substring("http://".length());
        }
        return httpUrl;
    }

    private static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
