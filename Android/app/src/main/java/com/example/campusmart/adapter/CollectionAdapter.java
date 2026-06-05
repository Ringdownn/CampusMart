package com.example.campusmart.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.campusmart.R;
import java.util.List;

public class CollectionAdapter extends RecyclerView.Adapter<CollectionAdapter.GoodsViewHolder> {
    public static class Goods {
        public int imgRes;
        public String title;
        public String desc;
        public String newDegree;
        public String price;
        public String pictureURL;
        public Long goodId;

        public Goods(int imgRes, String title, String desc, String newDegree, String price) {
            this.imgRes = imgRes;
            this.title = title;
            this.desc = desc;
            this.newDegree = newDegree;
            this.price = price;
        }
    }

    public interface ImageLoader {
        void loadImage(ImageView imageView, String url);
    }

    private List<Goods> goodsList;
    private OnButtonClickListener listener;
    private ImageLoader imageLoader;

    public interface OnButtonClickListener {
        void onDetailClick(int position);
    }

    public CollectionAdapter(List<Goods> goodsList, OnButtonClickListener listener) {
        this.goodsList = goodsList;
        this.listener = listener;
    }

    public void setImageLoader(ImageLoader imageLoader) {
        this.imageLoader = imageLoader;
    }

    @NonNull
    @Override
    public GoodsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_collections_history, parent, false);
        return new GoodsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GoodsViewHolder holder, int position) {
        Goods goods = goodsList.get(position);

        if (goods.pictureURL != null && !goods.pictureURL.isEmpty() && imageLoader != null) {
            imageLoader.loadImage(holder.ivGoodsImg, goods.pictureURL);
        } else {
            holder.ivGoodsImg.setImageResource(goods.imgRes);
        }

        holder.tvTitle.setText(goods.title);
        holder.tvDesc.setText(goods.desc);
        holder.tvNewDegree.setText(goods.newDegree);
        holder.tvPrice.setText(goods.price);
        holder.btnDetail.setOnClickListener(v -> listener.onDetailClick(position));
    }

    @Override
    public int getItemCount() {
        return goodsList.size();
    }

    static class GoodsViewHolder extends RecyclerView.ViewHolder {
        ImageView ivGoodsImg;
        TextView tvTitle, tvDesc, tvNewDegree, tvPrice;
        Button btnDetail;

        public GoodsViewHolder(@NonNull View itemView) {
            super(itemView);
            ivGoodsImg = itemView.findViewById(R.id.iv_goods_img);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvDesc = itemView.findViewById(R.id.tv_desc);
            tvNewDegree = itemView.findViewById(R.id.tv_new_degree);
            tvPrice = itemView.findViewById(R.id.tv_price);
            btnDetail = itemView.findViewById(R.id.btn_detail);
        }
    }
}
