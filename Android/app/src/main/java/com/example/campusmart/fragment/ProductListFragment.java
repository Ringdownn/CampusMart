package com.example.campusmart.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusmart.R;
import com.example.campusmart.adapter.ProductAdapter;
import com.example.campusmart.result.Result;
import com.example.campusmart.vo.GoodsVo;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.squareup.picasso.Picasso;
import com.example.campusmart.common.page.IPage;
import com.example.campusmart.common.page.PageImpl;

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

public class ProductListFragment extends Fragment {
    private RecyclerView rvProducts;
    private ProductAdapter productAdapter;
    private List<ProductAdapter.Product> productList = new ArrayList<>();
    private OkHttpClient client;
    private Gson gson;
    private String baseUrl;
    private long currentPage = 1;
    private final long pageSize = 10;
    private boolean isLoading = false;
    private boolean hasMore = true;
    private String token;
    private EditText etSearch;
    private ImageView btnSearch;
    private String currentKeyword = "";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String SEARCH_DATABASE = "campusmart";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_product_list, container, false);

        client = new OkHttpClient();
        gson = new Gson();
        baseUrl = getResources().getString(R.string.base_url);

        SharedPreferences sp = getActivity().getSharedPreferences("user_info", getActivity().MODE_PRIVATE);
        token = sp.getString("token", null);

        etSearch = view.findViewById(R.id.et_search);
        btnSearch = view.findViewById(R.id.btn_search);

        btnSearch.setOnClickListener(v -> performSearch());

        rvProducts = view.findViewById(R.id.rv_products);
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        rvProducts.setLayoutManager(layoutManager);

        productAdapter = new ProductAdapter(productList);
        rvProducts.setAdapter(productAdapter);

        loadGoodsData(currentPage, pageSize, false);

        rvProducts.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                if (!isLoading && hasMore &&
                        (visibleItemCount + firstVisibleItemPosition) >= totalItemCount &&
                        firstVisibleItemPosition >= 0) {
                    currentPage++;
                    loadGoodsData(currentPage, pageSize, !currentKeyword.isEmpty());
                }
            }
        });

        return view;
    }

    private void performSearch() {
        String keyword = etSearch.getText().toString().trim();
        currentKeyword = keyword;
        productList.clear();
        productAdapter.notifyDataSetChanged();

        currentPage = 1;
        hasMore = true;

        loadGoodsData(currentPage, pageSize, true);
    }

    private void loadGoodsData(long current, long size, boolean isSearch) {
        if (isLoading) return;
        isLoading = true;

        Request request;
        if (isSearch && !currentKeyword.isEmpty()) {
            request = buildSearchRequest(current, size);
        } else {
            request = buildGoodsPageRequest(current, size);
        }

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(getContext(), "Network error", Toast.LENGTH_SHORT).show();
                    isLoading = false;
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.code() == 601 || response.code() == 602 || response.code() == 401) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(getContext(), "Login expired, please login again", Toast.LENGTH_SHORT).show();
                        isLoading = false;
                    });
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    if (isSearch && !currentKeyword.isEmpty()) {
                        handleSearchResponse(responseData, current);
                    } else {
                        handleGoodsPageResponse(responseData, current);
                    }
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(getContext(), "Server error: " + response.code(), Toast.LENGTH_SHORT).show();
                        isLoading = false;
                    });
                }
            }
        });
    }

    private Request buildGoodsPageRequest(long current, long size) {
        String url = baseUrl + "/app/goods/page?current=" + current + "&size=" + size;
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .get();

        if (token != null && !token.isEmpty()) {
            requestBuilder.addHeader("access-token", token);
        }

        return requestBuilder.build();
    }

    private Request buildSearchRequest(long current, long size) {
        SearchRequest searchRequest = new SearchRequest(currentKeyword, current, size);
        RequestBody body = RequestBody.create(gson.toJson(searchRequest), JSON);
        return new Request.Builder()
                .url(baseUrl + "/api/query?database=" + SEARCH_DATABASE)
                .post(body)
                .build();
    }

    private void handleGoodsPageResponse(String responseData, long current) {
        Type type = new TypeToken<Result<PageImpl<GoodsVo>>>(){}.getType();
        Result<PageImpl<GoodsVo>> result = gson.fromJson(responseData, type);

        new Handler(Looper.getMainLooper()).post(() -> {
            if (result != null && result.getCode() == 200 && result.getData() != null) {
                List<GoodsVo> goodsList = result.getData().getRecords();
                hasMore = current < result.getData().getPages();
                appendGoods(goodsList);
            } else {
                Toast.makeText(getContext(), "Load failed: " + (result != null ? result.getMessage() : "Unknown error"), Toast.LENGTH_SHORT).show();
            }
            isLoading = false;
        });
    }

    private void handleSearchResponse(String responseData, long current) {
        SearchResponse result = gson.fromJson(responseData, SearchResponse.class);

        new Handler(Looper.getMainLooper()).post(() -> {
            if (result != null && result.state && result.data != null) {
                hasMore = current < result.data.pageCount;
                appendGoods(result.data.toGoodsList());
            } else {
                Toast.makeText(getContext(), "Search failed: " + (result != null ? result.message : "Unknown error"), Toast.LENGTH_SHORT).show();
            }
            isLoading = false;
        });
    }

    private void appendGoods(List<GoodsVo> goodsList) {
        if (goodsList == null) {
            return;
        }
        for (GoodsVo goods : goodsList) {
            productList.add(new ProductAdapter.Product(
                    goods.getTitle(),
                    R.drawable.placeholder,
                    goods.getNickname(),
                    R.drawable.avatar_placeholder,
                    goods.getPictureURL(),
                    goods.getAvatarURL(),
                    goods.getGoodID()
            ));
        }
        productAdapter.notifyDataSetChanged();
    }

    private static class SearchRequest {
        private final String query;
        private final long page;
        private final long limit;
        private final String order = "desc";
        private final String scoreExp = "";

        SearchRequest(String query, long page, long limit) {
            this.query = query;
            this.page = page;
            this.limit = limit;
        }
    }

    private static class SearchResponse {
        private boolean state;
        private String message;
        private SearchData data;
    }

    private static class SearchData {
        private long pageCount;
        private List<SearchDocument> documents;

        private List<GoodsVo> toGoodsList() {
            List<GoodsVo> goodsList = new ArrayList<>();
            if (documents == null) {
                return goodsList;
            }
            for (SearchDocument item : documents) {
                if (item == null) {
                    continue;
                }
                GoodsVo goods = item.document != null ? item.document : new GoodsVo();
                if (goods.getGoodID() == null) {
                    goods.setGoodID(item.id);
                }
                if (goods.getPictureURL() == null || goods.getPictureURL().isEmpty()) {
                    goods.setPictureURL(item.imageURL);
                }
                goodsList.add(goods);
            }
            return goodsList;
        }
    }

    private static class SearchDocument {
        private Long id;
        private String imageURL;
        private GoodsVo document;
    }

}