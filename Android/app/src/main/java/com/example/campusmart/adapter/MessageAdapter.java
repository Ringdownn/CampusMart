package com.example.campusmart.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.campusmart.R;
import com.example.campusmart.util.ImageUrlUtils;
import com.example.campusmart.vo.RecentChatVo;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    private List<RecentChatVo> chatList;
    private OnItemClickListener listener;

    public MessageAdapter(List<RecentChatVo> chatList) {
        this.chatList = chatList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecentChatVo chat = chatList.get(position);
        holder.tvUsername.setText(chat.getOtherNickname());
        if (chat.getGoodTitle() != null && !chat.getGoodTitle().isEmpty()) {
            holder.tvContent.setText("[" + chat.getGoodTitle() + "] " + chat.getLastestMessage());
        } else {
            holder.tvContent.setText(chat.getLastestMessage());
        }

        if (chat.getLastestMessageTime() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.US);
            holder.tvTime.setText(sdf.format(chat.getLastestMessageTime()));
        }

        String avatarUrl = chat.getOtherAvatarURL();
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            avatarUrl = ImageUrlUtils.normalize(holder.itemView.getContext(), avatarUrl);
            Glide.with(holder.itemView.getContext())
                    .load(avatarUrl)
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.placeholder)
                    .circleCrop()
                    .into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(R.drawable.placeholder);
        }
    }

    @Override
    public int getItemCount() {
        return chatList == null ? 0 : chatList.size();
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public RecentChatVo getItem(int position) {
        return chatList.get(position);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvUsername;
        TextView tvTime;
        TextView tvContent;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            tvUsername = itemView.findViewById(R.id.tv_username);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvContent = itemView.findViewById(R.id.tv_content);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onItemClick(position);
                    }
                }
            });
        }
    }
}