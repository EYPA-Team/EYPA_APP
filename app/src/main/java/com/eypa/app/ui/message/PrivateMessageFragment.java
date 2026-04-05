package com.eypa.app.ui.message;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.eypa.app.R;
import com.eypa.app.api.ApiClient;
import com.eypa.app.api.ContentApiService;
import com.eypa.app.model.message.MessageRequest;
import com.eypa.app.model.message.MessageResponse;
import com.eypa.app.utils.UserManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PrivateMessageFragment extends Fragment {

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private MessageAdapter adapter;
    private ContentApiService apiService;

    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasNextPage = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_private_message, container, false);

        apiService = ApiClient.getClient().create(ContentApiService.class);

        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        recyclerView = view.findViewById(R.id.recycler_view);

        adapter = new MessageAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(item -> {
            if (getContext() != null) {
                Intent intent = new Intent(getContext(), ChatActivity.class);
                if (item.getTargetUser() != null) {
                    intent.putExtra("target_id", item.getTargetUser().getId());
                    intent.putExtra("target_name", item.getTargetUser().getName());
                    intent.putExtra("target_avatar", item.getTargetUser().getAvatar());
                }
                startActivity(intent);
            }
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

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshData();
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
        } else if (!isRefresh && showIndicator) {
            adapter.setLoadingFooterVisible(true);
        }

        if (getContext() == null) return;
        String token = UserManager.getInstance(getContext()).getToken();
        if (token == null || token.isEmpty()) {
            isLoading = false;
            swipeRefreshLayout.setRefreshing(false);
            adapter.setLoadingFooterVisible(false);
            return;
        }

        MessageRequest request = new MessageRequest(token, currentPage);
        apiService.getMessages(request).enqueue(new Callback<MessageResponse>() {
            @Override
            public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                isLoading = false;
                swipeRefreshLayout.setRefreshing(false);
                adapter.setLoadingFooterVisible(false);

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
                    } else if (getContext() != null) {
                        Toast.makeText(getContext(), messageResponse.getMsg(), Toast.LENGTH_SHORT).show();
                    }
                } else if (getContext() != null) {
                    Toast.makeText(getContext(), "获取私信失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<MessageResponse> call, Throwable t) {
                isLoading = false;
                swipeRefreshLayout.setRefreshing(false);
                adapter.setLoadingFooterVisible(false);
                if (getContext() != null) {
                    Toast.makeText(getContext(), "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
