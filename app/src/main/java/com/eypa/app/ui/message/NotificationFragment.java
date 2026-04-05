package com.eypa.app.ui.message;

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
import com.eypa.app.model.message.NotificationRequest;
import com.eypa.app.model.message.NotificationResponse;
import com.eypa.app.utils.UserManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationFragment extends Fragment {

    private static final String ARG_TAB_TYPE = "tab_type";

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private ContentApiService apiService;

    private String tabType;
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

    public static NotificationFragment newInstance(String tabType) {
        NotificationFragment fragment = new NotificationFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TAB_TYPE, tabType);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            tabType = getArguments().getString(ARG_TAB_TYPE, "news");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_private_message, container, false);

        apiService = ApiClient.getClient().create(ContentApiService.class);

        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        recyclerView = view.findViewById(R.id.recycler_view);

        adapter = new NotificationAdapter();
        adapter.setOnItemClickListener(new NotificationAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(com.eypa.app.model.message.NotificationItem item) {
                // 不做处理
            }

            @Override
            public void onMarkReadClick(com.eypa.app.model.message.NotificationItem item) {
                markAsRead(item.getId());
            }
        });
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

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
        pollHandler.postDelayed(pollRunnable, 10000);
    }

    @Override
    public void onPause() {
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

    private void markAsRead(String msgId) {
        if (getContext() == null) return;
        String token = UserManager.getInstance(getContext()).getToken();
        if (token == null || token.isEmpty()) return;

        com.eypa.app.model.message.MessageReadRequest request = new com.eypa.app.model.message.MessageReadRequest(token, msgId);
        apiService.markMessageRead(request).enqueue(new Callback<com.eypa.app.model.message.MessageReadResponse>() {
            @Override
            public void onResponse(Call<com.eypa.app.model.message.MessageReadResponse> call, Response<com.eypa.app.model.message.MessageReadResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getCode() == 200) {
                    silentRefreshData();
                } else if (getContext() != null) {
                    Toast.makeText(getContext(), response.body() != null ? response.body().getMsg() : "标记已读失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<com.eypa.app.model.message.MessageReadResponse> call, Throwable t) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "网络错误", Toast.LENGTH_SHORT).show();
                }
            }
        });
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

        NotificationRequest request = new NotificationRequest(token, tabType, currentPage);
        apiService.getNotificationList(request).enqueue(new Callback<NotificationResponse>() {
            @Override
            public void onResponse(Call<NotificationResponse> call, Response<NotificationResponse> response) {
                isLoading = false;
                swipeRefreshLayout.setRefreshing(false);
                adapter.setLoadingFooterVisible(false);

                if (response.isSuccessful() && response.body() != null) {
                    NotificationResponse notifResponse = response.body();
                    if (notifResponse.getCode() == 200) {
                        if (isRefresh) {
                            adapter.setItems(notifResponse.getData());
                        } else {
                            adapter.addItems(notifResponse.getData());
                        }

                        if (notifResponse.getPagination() != null) {
                            hasNextPage = notifResponse.getPagination().isHasNext();
                        } else {
                            hasNextPage = false;
                        }
                    } else if (getContext() != null) {
                        Toast.makeText(getContext(), notifResponse.getMsg(), Toast.LENGTH_SHORT).show();
                    }
                } else if (getContext() != null) {
                    Toast.makeText(getContext(), "获取通知失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<NotificationResponse> call, Throwable t) {
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
