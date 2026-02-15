package com.eypa.app.ui.home;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.eypa.app.R;
import com.eypa.app.api.ApiClient;
import com.eypa.app.api.ContentApiService;
import com.eypa.app.model.ContentItem;
import com.eypa.app.model.user.FavoritesRequest;
import com.eypa.app.utils.ThemeUtils;
import com.eypa.app.utils.UserManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FavoritesActivity extends AppCompatActivity {

    private FavoritesAdapter adapter;
    private List<ContentItem> favoritesList = new ArrayList<>();
    private TextView emptyView;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean isLastPage = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applyTheme(this);
        applyCustomTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("我的收藏");
        }

        recyclerView = findViewById(R.id.recycler_view);
        emptyView = findViewById(R.id.tv_empty);
        progressBar = findViewById(R.id.progress_bar);
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        adapter = new FavoritesAdapter(favoritesList);
        recyclerView.setAdapter(adapter);
        
        recyclerView.setAlpha(0f);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                if (!isLoading && !isLastPage) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0) {
                        loadFavorites();
                    }
                }
            }
        });

        loadFavorites();
    }

    private void applyCustomTheme() {
        android.content.SharedPreferences sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        int themeId = sharedPreferences.getInt("ThemeId", R.style.Theme_EYPA_APP);
        setTheme(themeId);
    }

    private void loadFavorites() {
        if (isLoading) return;
        isLoading = true;
        
        if (currentPage == 1) {
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setAlpha(1f);
        }

        String token = UserManager.getInstance(this).getToken();
        if (token == null) {
            finish();
            return;
        }

        ContentApiService apiService = ApiClient.getClient().create(ContentApiService.class);
        FavoritesRequest request = new FavoritesRequest(token, currentPage);
        
        apiService.getFavorites(request).enqueue(new Callback<List<ContentItem>>() {
            @Override
            public void onResponse(Call<List<ContentItem>> call, Response<List<ContentItem>> response) {
                isLoading = false;
                
                if (response.isSuccessful() && response.body() != null) {
                    List<ContentItem> newItems = response.body();
                    if (newItems.isEmpty()) {
                        isLastPage = true;
                        progressBar.setVisibility(View.GONE);
                    } else {
                        if (currentPage == 1) {
                            favoritesList.clear();
                            
                            progressBar.animate()
                                    .alpha(0f)
                                    .setDuration(200)
                                    .withEndAction(() -> progressBar.setVisibility(View.GONE))
                                    .start();

                            recyclerView.animate()
                                    .alpha(1f)
                                    .setDuration(300)
                                    .start();
                        }
                        favoritesList.addAll(newItems);
                        adapter.notifyDataSetChanged();
                        currentPage++;
                    }
                } else {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(FavoritesActivity.this, "加载失败: " + response.message(), Toast.LENGTH_SHORT).show();
                }
                updateEmptyState();
            }

            @Override
            public void onFailure(Call<List<ContentItem>> call, Throwable t) {
                isLoading = false;
                progressBar.setVisibility(View.GONE);
                Toast.makeText(FavoritesActivity.this, "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                updateEmptyState();
            }
        });
    }

    private void updateEmptyState() {
        if (favoritesList.isEmpty()) {
            if (progressBar.getVisibility() == View.GONE) {
                recyclerView.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
            }
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
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
