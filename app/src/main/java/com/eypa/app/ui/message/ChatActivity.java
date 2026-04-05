package com.eypa.app.ui.message;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageButton;
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
import com.eypa.app.model.message.ChatRecord;
import com.eypa.app.model.message.ChatRecordRequest;
import com.eypa.app.model.message.ChatRecordResponse;
import com.eypa.app.model.message.ChatSendRequest;
import com.eypa.app.utils.ThemeUtils;
import com.eypa.app.utils.UserManager;

import java.util.Collections;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity {

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private EditText etMessage;
    private android.widget.Button btnSend;

    private ChatAdapter adapter;
    private ContentApiService apiService;

    private int targetId;
    private String targetName;
    private String targetAvatar;

    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasNextPage = true;

    private void applyCustomTheme() {
        int themeId = getSharedPreferences("AppSettings", MODE_PRIVATE).getInt("ThemeId", R.style.Theme_EYPA_APP);
        setTheme(themeId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applyTheme(this);
        applyCustomTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        targetId = getIntent().getIntExtra("target_id", -1);
        targetName = getIntent().getStringExtra("target_name");
        targetAvatar = getIntent().getStringExtra("target_avatar");

        if (targetId == -1) {
            Toast.makeText(this, "无效的用户", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(targetName != null ? targetName : "聊天");
        }

        apiService = ApiClient.getClient().create(ContentApiService.class);

        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        recyclerView = findViewById(R.id.recycler_view);
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);

        adapter = new ChatAdapter();
        
        String myAvatar = "";
        if (UserManager.getInstance(this).getUserProfile().getValue() != null) {
            myAvatar = UserManager.getInstance(this).getUserProfile().getValue().getAvatar();
        }
        adapter.setAvatars(targetAvatar, myAvatar);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setReverseLayout(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (hasNextPage && !isLoading) {
                loadMoreData();
            } else {
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy < 0) {
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

        btnSend.setOnClickListener(v -> {
            String content = etMessage.getText().toString().trim();
            if (content.isEmpty()) {
                return;
            }
            
            String token = UserManager.getInstance(ChatActivity.this).getToken();
            if (token == null || token.isEmpty()) {
                Toast.makeText(ChatActivity.this, "请先登录", Toast.LENGTH_SHORT).show();
                return;
            }

            btnSend.setEnabled(false);
            ChatSendRequest request = new ChatSendRequest(token, targetId, content);
            apiService.sendChatMessage(request).enqueue(new Callback<com.eypa.app.model.message.ChatSendResponse>() {
                @Override
                public void onResponse(Call<com.eypa.app.model.message.ChatSendResponse> call, Response<com.eypa.app.model.message.ChatSendResponse> response) {
                    btnSend.setEnabled(true);
                    if (response.isSuccessful() && response.body() != null) {
                        com.eypa.app.model.message.ChatSendResponse sendResponse = response.body();
                        if (sendResponse.getCode() == 200 && sendResponse.getData() != null) {
                            etMessage.setText("");
                            adapter.addRecordToBottom(sendResponse.getData());
                            recyclerView.scrollToPosition(0);
                        } else {
                            Toast.makeText(ChatActivity.this, sendResponse.getMsg(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(ChatActivity.this, "发送失败", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<com.eypa.app.model.message.ChatSendResponse> call, Throwable t) {
                    btnSend.setEnabled(true);
                    Toast.makeText(ChatActivity.this, "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        refreshData();
    }

    private void refreshData() {
        currentPage = 1;
        hasNextPage = true;
        loadData(true);
    }

    private void loadMoreData() {
        currentPage++;
        loadData(false);
    }

    private void loadData(boolean isRefresh) {
        if (isLoading) return;
        isLoading = true;
        
        if (isRefresh) {
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

        ChatRecordRequest request = new ChatRecordRequest(token, targetId, currentPage);
        apiService.getChatRecords(request).enqueue(new Callback<ChatRecordResponse>() {
            @Override
            public void onResponse(Call<ChatRecordResponse> call, Response<ChatRecordResponse> response) {
                isLoading = false;
                swipeRefreshLayout.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    ChatRecordResponse chatResponse = response.body();
                    if (chatResponse.getCode() == 200) {
                        if (chatResponse.getData() != null) {
                            Collections.reverse(chatResponse.getData());
                            if (isRefresh) {
                                adapter.setRecords(chatResponse.getData());
                            } else {
                                adapter.addRecords(chatResponse.getData());
                            }
                        }

                        if (chatResponse.getPagination() != null) {
                            hasNextPage = chatResponse.getPagination().isHasNext();
                        } else {
                            hasNextPage = false;
                        }
                    } else {
                        Toast.makeText(ChatActivity.this, chatResponse.getMsg(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(ChatActivity.this, "获取聊天记录失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ChatRecordResponse> call, Throwable t) {
                isLoading = false;
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(ChatActivity.this, "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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
