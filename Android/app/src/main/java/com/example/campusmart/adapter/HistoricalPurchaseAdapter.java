package com.example.campusmart.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusmart.R;

import java.util.List;

public class HistoricalPurchaseAdapter extends RecyclerView.Adapter<HistoricalPurchaseAdapter.PurchaseViewHolder> {
    public static class Purchase {
        public int imgRes;
        public Long orderId;
        public Long goodsId;
        public String pictureURL;
        public String title;
        public String desc;
        public String newDegree;
        public String price;
        public String status;

        public Purchase(int imgRes, Long orderId, Long goodsId, String title, String desc, String newDegree, String price, String status) {
            this.imgRes = imgRes;
            this.orderId = orderId;
            this.goodsId = goodsId;
            this.title = title;
            this.desc = desc;
            this.newDegree = newDegree;
            this.price = price;
            this.status = status;
        }
    }

    private List<Purchase> purchaseList;
    private OnDetailClickListener listener;
    private ImageLoader imageLoader;

    public interface OnDetailClickListener {
        void onDetailClick(int position);
    }

    public interface ImageLoader {
        void loadImage(ImageView imageView, String url);
    }

    public HistoricalPurchaseAdapter(List<Purchase> purchaseList, OnDetailClickListener listener) {
        this.purchaseList = purchaseList;
        this.listener = listener;
    }

    public void setImageLoader(ImageLoader imageLoader) {
        this.imageLoader = imageLoader;
    }

    @NonNull
    @Override
    public PurchaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_collections_history, parent, false);
        return new PurchaseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PurchaseViewHolder holder, int position) {
        Purchase purchase = purchaseList.get(position);
        if (purchase.pictureURL != null && !purchase.pictureURL.isEmpty() && imageLoader != null) {
            imageLoader.loadImage(holder.ivGoodsImg, purchase.pictureURL);
        } else {
            holder.ivGoodsImg.setImageResource(purchase.imgRes);
        }
        holder.tvTitle.setText(purchase.title);
        holder.tvDesc.setText(purchase.desc);
        holder.tvNewDegree.setText(purchase.status);
        holder.tvPrice.setText(purchase.price);
        holder.btnDetail.setOnClickListener(v -> listener.onDetailClick(position));
        holder.itemView.setOnClickListener(v -> listener.onDetailClick(position));
    }

    @Override
    public int getItemCount() {
        return purchaseList.size();
    }

    static class PurchaseViewHolder extends RecyclerView.ViewHolder {
        ImageView ivGoodsImg;
        TextView tvTitle, tvDesc, tvNewDegree, tvPrice;
        Button btnDetail;

        public PurchaseViewHolder(@NonNull View itemView) {
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