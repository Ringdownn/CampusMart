package com.example.campusmart.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusmart.ChatActivity;
import com.example.campusmart.R;
import com.example.campusmart.adapter.MessageAdapter;
import com.example.campusmart.util.ImageUrlUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import com.example.campusmart.vo.RecentChatVo;
import com.example.campusmart.result.Result;


import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MessageFragment extends Fragment {
    private RecyclerView rvMessages;
    private MessageAdapter adapter;
    private OkHttpClient client;
    private Gson gson;
    private String baseUrl;
    private Long userId;
    private String token;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_message, container, false);

        rvMessages = view.findViewById(R.id.rv_messages);
        rvMessages.setLayoutManager(new LinearLayoutManager(getContext()));

        client = new OkHttpClient();
        gson = new Gson();
        baseUrl = getResources().getString(R.string.base_url);

        loadUserInfo();

        // 加载最近对话列表
        if (userId != null && userId > 0 && token != null && !token.isEmpty()) {
            loadRecentChats();
        } else {
            Toast.makeText(getContext(), "Please log in", Toast.LENGTH_SHORT).show();
        }

        return view;
    }

    private void loadUserInfo() {
        SharedPreferences sp = getContext().getSharedPreferences("user_info", getContext().MODE_PRIVATE);
        userId = sp.getLong("user_id", 0);
        token = sp.getString("token", null);
    }

    private void loadRecentChats() {
        String url = baseUrl + "/app/messages/recent?userId=" + userId;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("access-token", token)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Network error
                getActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Load failed. Check network.", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    Result<List<RecentChatVo>> result = gson.fromJson(
                            responseData,
                            new TypeToken<Result<List<RecentChatVo>>>(){}.getType()
                    );

                    getActivity().runOnUiThread(() -> {
                        if (result.getCode() == 200 && result.getData() != null) {
                            adapter = new MessageAdapter(result.getData());
                            rvMessages.setAdapter(adapter);


                            adapter.setOnItemClickListener(position -> {
                                RecentChatVo chat = adapter.getItem(position);
                                Intent intent = new Intent(getContext(), ChatActivity.class);
                                intent.putExtra("currentUserId", userId);
                                intent.putExtra("otherUserId", Long.parseLong(chat.getOtherID()));
                                intent.putExtra("goodId", chat.getGoodID() != null ? chat.getGoodID() : 0);
                                intent.putExtra("token", token);
                                intent.putExtra("otherNickname", chat.getOtherNickname());
                                intent.putExtra("goodTitle", chat.getGoodTitle() != null ? chat.getGoodTitle() : "");

                                SharedPreferences sp = getContext().getSharedPreferences("user_info", getContext().MODE_PRIVATE);
                                String selfAvatarUrl = ImageUrlUtils.normalize(requireContext(), sp.getString("avatar_url", ""));
                                intent.putExtra("selfAvatarUrl", selfAvatarUrl);

                                intent.putExtra("otherAvatarUrl", ImageUrlUtils.normalize(requireContext(), chat.getOtherAvatarURL()));
                                intent.putExtra("goodImageUrl", ImageUrlUtils.normalize(requireContext(), chat.getGoodPictureURL()));

                                startActivity(intent);
                            });
                        } else {
                            Toast.makeText(getContext(), "Load failed: " + result.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Server error", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }
}