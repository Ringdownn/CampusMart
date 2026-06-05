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
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_OTHER = 0;
    private static final int TYPE_SELF = 1;
    private List<ChatMsg> msgList;
    private String selfAvatarUrl;
    private String otherAvatarUrl;

    public ChatAdapter(List<ChatMsg> msgList, String selfAvatarUrl, String otherAvatarUrl) {
        this.msgList = msgList;
        this.selfAvatarUrl = selfAvatarUrl;
        this.otherAvatarUrl = otherAvatarUrl;
    }

    @Override
    public int getItemViewType(int position) {
        return msgList.get(position).isSelf ? TYPE_SELF : TYPE_OTHER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_OTHER) {
            return new OtherViewHolder(inflater.inflate(R.layout.item_msg_other, parent, false));
        } else {
            return new SelfViewHolder(inflater.inflate(R.layout.item_msg_self, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMsg msg = msgList.get(position);
        if (holder instanceof OtherViewHolder) {
            ((OtherViewHolder) holder).tvOtherMsg.setText(msg.content);
            Glide.with(holder.itemView.getContext())
                    .load(ImageUrlUtils.normalize(holder.itemView.getContext(), otherAvatarUrl))
                    .placeholder(R.drawable.avatar_slow)
                    .error(R.drawable.avatar_slow)
                    .circleCrop()
                    .into(((OtherViewHolder) holder).ivOtherAvatar);
        } else {
            ((SelfViewHolder) holder).tvSelfMsg.setText(msg.content);
            Glide.with(holder.itemView.getContext())
                    .load(ImageUrlUtils.normalize(holder.itemView.getContext(), selfAvatarUrl))
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.placeholder)
                    .circleCrop()
                    .into(((SelfViewHolder) holder).ivSelfAvatar);
        }
    }

    @Override
    public int getItemCount() {
        return msgList.size();
    }

    static class OtherViewHolder extends RecyclerView.ViewHolder {
        ImageView ivOtherAvatar;
        TextView tvOtherMsg;
        public OtherViewHolder(@NonNull View itemView) {
            super(itemView);
            ivOtherAvatar = itemView.findViewById(R.id.iv_other_avatar);
            tvOtherMsg = itemView.findViewById(R.id.tv_other_msg);
        }
    }

    static class SelfViewHolder extends RecyclerView.ViewHolder {
        TextView tvSelfMsg;
        ImageView ivSelfAvatar;
        public SelfViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSelfMsg = itemView.findViewById(R.id.tv_self_msg);
            ivSelfAvatar = itemView.findViewById(R.id.iv_self_avatar);
        }
    }

    public static class ChatMsg {
        public boolean isSelf;
        public String content;
        public ChatMsg(boolean isSelf, String content) {
            this.isSelf = isSelf;
            this.content = content;
        }
    }
}