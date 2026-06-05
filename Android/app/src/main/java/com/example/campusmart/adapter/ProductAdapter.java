package com.example.campusmart.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.campusmart.GoodsDetailActivity;
import com.example.campusmart.R;
import com.example.campusmart.util.ImageUrlUtils;

import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {
    private List<Product> productList;
    private static final RequestOptions PRODUCT_OPTIONS = new RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .dontAnimate()
            .centerCrop();

    private static final RequestOptions AVATAR_OPTIONS = new RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .circleCrop()
            .dontAnimate();

    public static class Product {
        String title;
        int imageRes;
        String publisher;
        int avatarRes;
        String imageUrl;
        String avatarUrl;
        Long goodID;

        public Product(String title, int imageRes, String publisher, int avatarRes,
                       String imageUrl, String avatarUrl, Long goodID) {
            this.title = title;
            this.imageRes = imageRes;
            this.publisher = publisher;
            this.avatarRes = avatarRes;
            this.imageUrl = imageUrl;
            this.avatarUrl = avatarUrl;
            this.goodID = goodID;
        }
    }

    public ProductAdapter(List<Product> productList) {
        this.productList = productList;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.tvTitle.setText(product.title);
        holder.tvPublisher.setText(product.publisher);

        Context context = holder.itemView.getContext();
        int placeholder = product.imageRes > 0 ? product.imageRes : R.drawable.placeholder;
        String imageUrl = ImageUrlUtils.normalize(context, product.imageUrl);
        String avatarUrl = ImageUrlUtils.normalize(context, product.avatarUrl);

        Glide.with(holder.ivProduct.getContext())
                .load(imageUrl)
                .apply(PRODUCT_OPTIONS)
                .placeholder(placeholder)
                .error(R.drawable.placeholder)
                .override(400, 400)
                .into(holder.ivProduct);

        int avatarPlaceholder = product.avatarRes > 0 ? product.avatarRes : R.drawable.avatar_placeholder;
        Glide.with(holder.ivAvatar.getContext())
                .load(avatarUrl)
                .apply(AVATAR_OPTIONS)
                .placeholder(avatarPlaceholder)
                .error(R.drawable.avatar_placeholder)
                .skipMemoryCache(false)
                .override(100, 100)
                .into(holder.ivAvatar);


        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, GoodsDetailActivity.class);
            intent.putExtra("product_title", product.title);
            intent.putExtra("product_image_url", imageUrl);
            intent.putExtra("product_publisher", product.publisher);
            intent.putExtra("product_avatar_url", avatarUrl);
            intent.putExtra("product_good_id", product.goodID);
            context.startActivity(intent);
        });
    }

    @Override
    public void onViewRecycled(@NonNull ProductViewHolder holder) {
        super.onViewRecycled(holder);
        Glide.with(holder.itemView.getContext()).clear(holder.ivProduct);
        Glide.with(holder.itemView.getContext()).clear(holder.ivAvatar);
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProduct;
        TextView tvTitle;
        ImageView ivAvatar;
        TextView tvPublisher;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProduct = itemView.findViewById(R.id.iv_product);
            tvTitle = itemView.findViewById(R.id.tv_title);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            tvPublisher = itemView.findViewById(R.id.tv_publisher);
        }
    }
}