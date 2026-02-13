package com.eypa.app.ui.home;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.eypa.app.R;
import com.eypa.app.api.ApiClient;
import com.eypa.app.api.ContentApiService;
import com.eypa.app.model.ContentItem;
import com.eypa.app.utils.CategoryCacheManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TagContentActivity extends AppCompatActivity {

    private static final String EXTRA_TAG_NAME = "extra_tag_name";

    private RecyclerView recyclerView;
    private TextView emptyView;
    private ProgressBar progressBar;
    private PostsAdapter adapter;
    private final List<ContentItem> posts = new ArrayList<>();
    private String tagName;
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMore = true;

    public static void start(Context context, String tagName) {
        Intent intent = new Intent(context, TagContentActivity.class);
        intent.putExtra(EXTRA_TAG_NAME, tagName);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tag_content);

        tagName = getIntent().getStringExtra(EXTRA_TAG_NAME);
        if (tagName == null) {
            finish();
            return;
        }

        initViews();
        loadData(true);
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("#" + tagName);
        }

        recyclerView = findViewById(R.id.recycler_view);
        emptyView = findViewById(R.id.tv_empty);
        progressBar = findViewById(R.id.progress_bar);

        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);

        Map<Integer, String> categoryMap = CategoryCacheManager.getInstance(this).getCategoryMap();
        adapter = new PostsAdapter(posts, categoryMap);
        recyclerView.setAdapter(adapter);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0) {
                    int[] lastVisibleItemPositions = layoutManager.findLastVisibleItemPositions(null);
                    int lastVisibleItem = getLastVisibleItem(lastVisibleItemPositions);
                    int totalItemCount = layoutManager.getItemCount();

                    if (!isLoading && hasMore && totalItemCount <= (lastVisibleItem + 5)) {
                        loadData(false);
                    }
                }
            }
        });
    }

    private int getLastVisibleItem(int[] lastVisibleItemPositions) {
        int maxSize = 0;
        for (int i = 0; i < lastVisibleItemPositions.length; i++) {
            if (i == 0) {
                maxSize = lastVisibleItemPositions[i];
            } else if (lastVisibleItemPositions[i] > maxSize) {
                maxSize = lastVisibleItemPositions[i];
            }
        }
        return maxSize;
    }

    private void loadData(boolean refresh) {
        if (isLoading) return;
        isLoading = true;

        if (refresh) {
            currentPage = 1;
            hasMore = true;
            posts.clear();
            adapter.notifyDataSetChanged();
            progressBar.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        } else {
            adapter.setLoadingFooterVisible(true);
        }

        ContentApiService apiService = ApiClient.getClient().create(ContentApiService.class);
        apiService.searchPosts(tagName, currentPage).enqueue(new Callback<List<ContentItem>>() {
            @Override
            public void onResponse(Call<List<ContentItem>> call, Response<List<ContentItem>> response) {
                isLoading = false;
                progressBar.setVisibility(View.GONE);
                adapter.setLoadingFooterVisible(false);

                if (response.isSuccessful() && response.body() != null) {
                    List<ContentItem> newPosts = response.body();
                    if (newPosts.isEmpty()) {
                        hasMore = false;
                        if (currentPage == 1) {
                            emptyView.setVisibility(View.VISIBLE);
                        }
                    } else {
                        posts.addAll(newPosts);
                        adapter.notifyDataSetChanged();
                        currentPage++;
                    }
                } else {
                    Toast.makeText(TagContentActivity.this, "加载失败", Toast.LENGTH_SHORT).show();
                    if (currentPage == 1) {
                        emptyView.setVisibility(View.VISIBLE);
                        emptyView.setText("加载失败，请重试");
                    }
                }
            }

            @Override
            public void onFailure(Call<List<ContentItem>> call, Throwable t) {
                isLoading = false;
                progressBar.setVisibility(View.GONE);
                adapter.setLoadingFooterVisible(false);
                Toast.makeText(TagContentActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
                if (currentPage == 1) {
                    emptyView.setVisibility(View.VISIBLE);
                    emptyView.setText("网络错误，请重试");
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
