package com.eypa.app.ui.message;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.eypa.app.R;
import com.eypa.app.api.ApiClient;
import com.eypa.app.api.ContentApiService;
import com.eypa.app.model.message.MessageRequest;
import android.content.Intent;
import com.eypa.app.model.message.MessageResponse;
import com.eypa.app.utils.ThemeUtils;
import com.eypa.app.utils.UserManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessageActivity extends AppCompatActivity {

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private MessageAdapter adapter;
    private ContentApiService apiService;

    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasNextPage = true;

    private android.os.Handler pollHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            silentRefreshData();
            pollHandler.postDelayed(this, 10000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("消息");
        }

        apiService = ApiClient.getClient().create(ContentApiService.class);

        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        recyclerView = findViewById(R.id.recycler_view);

        adapter = new MessageAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(item -> {
            Intent intent = new Intent(this, ChatActivity.class);
            if (item.getTargetUser() != null) {
                intent.putExtra("target_id", item.getTargetUser().getId());
                intent.putExtra("target_name", item.getTargetUser().getName());
                intent.putExtra("target_avatar", item.getTargetUser().getAvatar());
            }
            startActivity(intent);
        });

        swipeRefreshLayout.setOnRefreshListener(this::refreshData);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int pastVisibleItems = layoutManager.findFirstVisibleItemPosition();

                    if (!isLoading && hasNextPage) {
                        if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                            loadMoreData();
                        }
                    }
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshData();
        pollHandler.postDelayed(pollRunnable, 10000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        pollHandler.removeCallbacks(pollRunnable);
    }

    private void silentRefreshData() {
        currentPage = 1;
        hasNextPage = true;
        loadData(true, false);
    }

    private void refreshData() {
        currentPage = 1;
        hasNextPage = true;
        loadData(true, true);
    }

    private void loadMoreData() {
        currentPage++;
        loadData(false, true);
    }

    private void loadData(boolean isRefresh, boolean showIndicator) {
        if (isLoading) return;
        isLoading = true;
        if (isRefresh && showIndicator) {
            swipeRefreshLayout.setRefreshing(true);
        }

        String token = UserManager.getInstance(this).getToken();
        if (token == null || token.isEmpty()) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            isLoading = false;
            swipeRefreshLayout.setRefreshing(false);
            finish();
            return;
        }

        MessageRequest request = new MessageRequest(token, currentPage);
        apiService.getMessages(request).enqueue(new Callback<MessageResponse>() {
            @Override
            public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                isLoading = false;
                swipeRefreshLayout.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    MessageResponse messageResponse = response.body();
                    if (messageResponse.getCode() == 200) {
                        if (isRefresh) {
                            adapter.setMessages(messageResponse.getData());
                        } else {
                            adapter.addMessages(messageResponse.getData());
                        }

                        if (messageResponse.getPagination() != null) {
                            hasNextPage = messageResponse.getPagination().isHasNext();
                        } else {
                            hasNextPage = false;
                        }
                    } else {
                        Toast.makeText(MessageActivity.this, messageResponse.getMsg(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MessageActivity.this, "获取消息失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<MessageResponse> call, Throwable t) {
                isLoading = false;
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(MessageActivity.this, "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
