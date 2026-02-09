package com.eypa.app.ui.home;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.eypa.app.R;
import com.eypa.app.api.ApiClient;
import com.eypa.app.model.ContentItem;
import com.eypa.app.model.user.AuthorFansResponse;
import com.eypa.app.model.user.AuthorListRequest;
import com.eypa.app.model.user.FanItem;
import com.eypa.app.utils.UserManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthorContentFragment extends Fragment {

    private static final String ARG_USER_ID = "arg_user_id";
    private static final String ARG_TYPE = "arg_type";
    
    public static final int TYPE_POSTS = 0;
    public static final int TYPE_FAVORITES = 1;
    public static final int TYPE_FANS = 2;

    private int userId;
    private int type;
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean isFirstLoad = true;
    
    private RecyclerView recyclerView;
    private View loadingMask;
    private TextView tvEmpty;
    
    private AuthorContentAdapter adapter;
    private FansAdapter fansAdapter;
    private List<ContentItem> contentList = new ArrayList<>();
    private List<FanItem> fansList = new ArrayList<>();

    public static AuthorContentFragment newInstance(int userId, int type) {
        AuthorContentFragment fragment = new AuthorContentFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_USER_ID, userId);
        args.putInt(ARG_TYPE, type);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userId = getArguments().getInt(ARG_USER_ID);
            type = getArguments().getInt(ARG_TYPE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_author_content, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        loadData();
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_view);
        loadingMask = view.findViewById(R.id.loading_mask);
        tvEmpty = view.findViewById(R.id.tv_empty);

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        recyclerView.setLayoutManager(layoutManager);
        
        if (type == TYPE_FANS) {
            fansAdapter = new FansAdapter();
            fansAdapter.setFans(fansList);
            fansAdapter.setOnItemClickListener(fan -> {
                AuthorProfileActivity.start(requireContext(), Integer.parseInt(fan.getId()));
            });
            recyclerView.setAdapter(fansAdapter);
        } else {
            adapter = new AuthorContentAdapter(contentList);
            recyclerView.setAdapter(adapter);
        }

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                if (!isLoading && (visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                        && firstVisibleItemPosition >= 0
                        && totalItemCount >= 10) {
                    loadData();
                }
            }
        });
    }

    private void loadData() {
        isLoading = true;
        if (isFirstLoad) {
            loadingMask.setVisibility(View.VISIBLE);
            loadingMask.setAlpha(1f);
            tvEmpty.setVisibility(View.GONE);
        } else {
            if (type == TYPE_FANS) {
                fansAdapter.setLoadingFooterVisible(true);
            } else {
                adapter.setLoadingFooterVisible(true);
            }
        }

        String token = UserManager.getInstance(requireContext()).getToken();
        AuthorListRequest request = new AuthorListRequest(userId, currentPage, token);

        if (type == TYPE_FANS) {
            ApiClient.getApiService().getAuthorFans(request).enqueue(new Callback<AuthorFansResponse>() {
                @Override
                public void onResponse(Call<AuthorFansResponse> call, Response<AuthorFansResponse> response) {
                    if (isAdded()) {
                        isLoading = false;
                        fansAdapter.setLoadingFooterVisible(false);
                        
                        if (response.isSuccessful() && response.body() != null && response.body().getCode() == 200) {
                            List<FanItem> items = response.body().getData();
                            if (items != null && !items.isEmpty()) {
                                fansAdapter.addFans(items);
                                if (isFirstLoad) {
                                    hideLoadingMask();
                                }
                                if (response.body().getPagination() != null && response.body().getPagination().isHasNext()) {
                                    currentPage++;
                                } else {
                                    currentPage++;
                                }
                            } else {
                                if (isFirstLoad) {
                                    hideLoadingMask();
                                    tvEmpty.setVisibility(View.VISIBLE);
                                }
                            }
                        } else {
                            handleError();
                        }
                        isFirstLoad = false;
                    }
                }

                @Override
                public void onFailure(Call<AuthorFansResponse> call, Throwable t) {
                    if (isAdded()) {
                        isLoading = false;
                        fansAdapter.setLoadingFooterVisible(false);
                        handleError();
                        isFirstLoad = false;
                    }
                }
            });
        } else {
            Call<List<ContentItem>> call;
            if (type == TYPE_POSTS) {
                call = ApiClient.getApiService().getAuthorPosts(request);
            } else {
                call = ApiClient.getApiService().getAuthorFavorites(request);
            }

            call.enqueue(new Callback<List<ContentItem>>() {
                @Override
                public void onResponse(Call<List<ContentItem>> call, Response<List<ContentItem>> response) {
                    if (isAdded()) {
                        isLoading = false;
                        adapter.setLoadingFooterVisible(false);

                        if (response.isSuccessful() && response.body() != null) {
                            List<ContentItem> items = response.body();
                            if (!items.isEmpty()) {
                                int startPos = contentList.size();
                                contentList.addAll(items);
                                if (isFirstLoad) {
                                    hideLoadingMask();
                                    adapter.notifyDataSetChanged();
                                } else {
                                    adapter.notifyItemRangeInserted(startPos, items.size());
                                }
                                currentPage++;
                            } else {
                                if (isFirstLoad) {
                                    hideLoadingMask();
                                    tvEmpty.setVisibility(View.VISIBLE);
                                }
                            }
                        } else {
                            handleError();
                        }
                        isFirstLoad = false;
                    }
                }

                @Override
                public void onFailure(Call<List<ContentItem>> call, Throwable t) {
                    if (isAdded()) {
                        isLoading = false;
                        adapter.setLoadingFooterVisible(false);
                        handleError();
                        isFirstLoad = false;
                    }
                }
            });
        }
    }

    private void hideLoadingMask() {
        loadingMask.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction(() -> loadingMask.setVisibility(View.GONE))
                .start();
    }

    private void handleError() {
        if (isFirstLoad) {
            hideLoadingMask();
            Toast.makeText(getContext(), "加载失败", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "加载更多失败", Toast.LENGTH_SHORT).show();
        }
    }
}
