package com.eypa.app.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.eypa.app.R;
import com.eypa.app.api.ApiClient;
import com.eypa.app.model.bbs.BBSPost;
import com.eypa.app.model.bbs.BBSPostListResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForumFragment extends Fragment {

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private BBSPostAdapter adapter;
    private List<BBSPost> postList = new ArrayList<>();
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMore = true;
    private boolean isFirstLoad = true;
    private long refreshStartTime = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_forum, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        recyclerView = view.findViewById(R.id.recycler_view);
        progressBar = view.findViewById(R.id.progress_bar);

        adapter = new BBSPostAdapter(postList, post -> {
            Intent intent = new Intent(getContext(), BBSPostDetailActivity.class);
            intent.putExtra(BBSPostDetailActivity.EXTRA_POST_ID, post.getId());
            startActivity(intent);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        TypedValue typedValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true);
        swipeRefreshLayout.setColorSchemeColors(typedValue.data);

        if (isFirstLoad) {
            recyclerView.setAlpha(0f);
        } else {
            recyclerView.setAlpha(1f);
        }

        swipeRefreshLayout.setOnRefreshListener(() -> {
            currentPage = 1;
            hasMore = true;
            refreshStartTime = System.currentTimeMillis();
            recyclerView.animate().alpha(0f).setDuration(300).start();
            loadPosts();
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (!recyclerView.canScrollVertically(1) && !isLoading && hasMore) {
                    currentPage++;
                    loadPosts();
                }
            }
        });

        loadPosts();
    }

    private void loadPosts() {
        isLoading = true;
        if (isFirstLoad) {
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setAlpha(1f);
        } else if (currentPage > 1) {
             recyclerView.post(() -> {
                if (adapter != null) adapter.setLoadingFooterVisible(true);
            });
        }

        ApiClient.getApiService().getBBSPosts(currentPage).enqueue(new Callback<BBSPostListResponse>() {
            @Override
            public void onResponse(@NonNull Call<BBSPostListResponse> call, @NonNull Response<BBSPostListResponse> response) {
                if (isAdded()) {
                    isLoading = false;
                    swipeRefreshLayout.setRefreshing(false);
                    adapter.setLoadingFooterVisible(false);

                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        List<BBSPost> newData = response.body().getData();
                        
                        if (isFirstLoad) {
                            if (currentPage == 1) {
                                postList.clear();
                            }
                            if (newData != null && !newData.isEmpty()) {
                                postList.addAll(newData);
                                adapter.notifyDataSetChanged();
                            } else {
                                hasMore = false;
                            }
                            
                            isFirstLoad = false;
                            progressBar.animate()
                                    .alpha(0f)
                                    .setDuration(200)
                                    .withEndAction(() -> progressBar.setVisibility(View.GONE))
                                    .start();

                            recyclerView.animate()
                                    .alpha(1f)
                                    .setStartDelay(300)
                                    .setDuration(300)
                                    .start();
                        } else {
                            if (currentPage == 1) {
                                long elapsedTime = System.currentTimeMillis() - refreshStartTime;
                                long remainingTime = 300 - elapsedTime;
                                if (remainingTime < 0) remainingTime = 0;

                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                    if (isAdded()) {
                                        postList.clear();
                                        if (newData != null && !newData.isEmpty()) {
                                            postList.addAll(newData);
                                            adapter.notifyDataSetChanged();
                                        } else {
                                            hasMore = false;
                                        }
                                        recyclerView.animate().alpha(1f).setDuration(300).start();
                                    }
                                }, remainingTime);
                            } else {
                                if (newData != null && !newData.isEmpty()) {
                                    postList.addAll(newData);
                                    adapter.notifyDataSetChanged();
                                } else {
                                    hasMore = false;
                                }
                            }
                        }

                    } else {
                        Toast.makeText(getContext(), "加载失败", Toast.LENGTH_SHORT).show();
                        if (isFirstLoad) {
                             progressBar.setVisibility(View.GONE);
                             recyclerView.setAlpha(1f);
                        } else {
                             recyclerView.animate().alpha(1f).setDuration(300).start();
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<BBSPostListResponse> call, @NonNull Throwable t) {
                if (isAdded()) {
                    isLoading = false;
                    swipeRefreshLayout.setRefreshing(false);
                    adapter.setLoadingFooterVisible(false);
                    Toast.makeText(getContext(), "网络错误", Toast.LENGTH_SHORT).show();
                    
                    if (isFirstLoad) {
                         progressBar.setVisibility(View.GONE);
                         recyclerView.setAlpha(1f);
                    } else {
                         recyclerView.animate().alpha(1f).setDuration(300).start();
                    }
                }
            }
        });
    }
}
