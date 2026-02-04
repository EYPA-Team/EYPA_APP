package com.eypa.app.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.eypa.app.R;
import com.eypa.app.api.ApiClient;
import com.eypa.app.model.Category;
import com.eypa.app.model.ContentItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    // 主页列表相关
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private PostsAdapter adapter;
    private List<ContentItem> postList = new ArrayList<>();
    private Map<Integer, String> categoryMap = new HashMap<>();
    private int currentPage = 1;
    private String currentSeed;
    private boolean isLoading = false;

    // 搜索相关
    private FrameLayout searchResultsContainer;
    private RecyclerView searchRecyclerView;
    private ProgressBar searchProgressBar;
    private TextView searchEmptyView;
    private PostsAdapter searchAdapter;
    private List<ContentItem> searchResults = new ArrayList<>();
    private int searchPage = 1;
    private boolean isSearching = false;
    private String currentQuery = "";
    private SearchView mSearchView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        currentSeed = String.valueOf(System.currentTimeMillis());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        loadPosts();
    }

    private void initViews(View view) {
        // --- 主页视图初始化 ---
        recyclerView = view.findViewById(R.id.recycler_view);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        progressBar = view.findViewById(R.id.progress_bar);

        TypedValue typedValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true);
        swipeRefreshLayout.setColorSchemeColors(typedValue.data);

        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new PostsAdapter(postList, categoryMap);
        recyclerView.setAdapter(adapter);

        swipeRefreshLayout.setOnRefreshListener(() -> {
            currentSeed = String.valueOf(System.currentTimeMillis());
            currentPage = 1;
            postList.clear();
            categoryMap.clear();
            adapter.notifyDataSetChanged();
            loadPosts();
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int[] lastVisibleItems = layoutManager.findLastVisibleItemPositions(null);
                int lastVisibleItem = Math.max(lastVisibleItems[0], lastVisibleItems[1]);
                if (!isLoading && lastVisibleItem >= postList.size() - 5) {
                    currentPage++;
                    loadPosts();
                }
            }
        });

        // --- 搜索视图初始化 ---
        searchResultsContainer = view.findViewById(R.id.search_results_container);
        searchRecyclerView = view.findViewById(R.id.search_recycler_view);
        searchProgressBar = view.findViewById(R.id.search_progress_bar);
        searchEmptyView = view.findViewById(R.id.search_empty_view);

        searchAdapter = new PostsAdapter(searchResults, new HashMap<>());
        StaggeredGridLayoutManager searchLayoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        searchRecyclerView.setLayoutManager(searchLayoutManager);
        searchRecyclerView.setAdapter(searchAdapter);

        searchRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int[] lastVisibleItems = searchLayoutManager.findLastVisibleItemPositions(null);
                int lastVisibleItem = Math.max(lastVisibleItems[0], lastVisibleItems[1]);
                if (!isSearching && lastVisibleItem >= searchResults.size() - 5) {
                    loadMoreSearchResults();
                }
            }
        });
    }

    private void loadPosts() {
        isLoading = true;
        progressBar.setVisibility(View.VISIBLE);

        ApiClient.getApiService().getContentItems(
                currentPage, 10, "id,title,date,categories,jetpack_featured_media_url,_embedded,zib_other_data,view_count,like_count,author_info",
                "rand", currentSeed, "wp:featuredmedia"
        ).enqueue(new Callback<List<ContentItem>>() {
            @Override
            public void onResponse(@NonNull Call<List<ContentItem>> call, @NonNull Response<List<ContentItem>> response) {
                if (isAdded()) {
                    swipeRefreshLayout.setRefreshing(false);
                    if (response.isSuccessful() && response.body() != null) {
                        postList.addAll(response.body());
                        Set<Integer> allCategoryIds = postList.stream()
                                .filter(p -> p.getCategories() != null)
                                .flatMap(p -> p.getCategories().stream())
                                .collect(Collectors.toSet());
                        fetchCategories(allCategoryIds);
                    } else {
                        isLoading = false;
                        progressBar.setVisibility(View.GONE);
                        Log.e("API_ERROR", "Response unsuccessful: " + response.code());
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<ContentItem>> call, @NonNull Throwable t) {
                if (isAdded()) {
                    swipeRefreshLayout.setRefreshing(false);
                    isLoading = false;
                    progressBar.setVisibility(View.GONE);
                    Log.e("API_FAILURE", "API call failed", t);
                }
            }
        });
    }

    private void performSearch(String query) {
        currentQuery = query;
        searchPage = 1;
        searchResults.clear();
        searchAdapter.notifyDataSetChanged();
        searchEmptyView.setVisibility(View.GONE);

        isSearching = true;
        searchProgressBar.setVisibility(View.VISIBLE);

        ApiClient.getApiService().searchPosts(
                currentQuery, searchPage, 10,
                "id,title,date,categories,jetpack_featured_media_url,_embedded,zib_other_data,view_count,like_count,author_info",
                "wp:featuredmedia"
        ).enqueue(new Callback<List<ContentItem>>() {
            @Override
            public void onResponse(@NonNull Call<List<ContentItem>> call, @NonNull Response<List<ContentItem>> response) {
                if (isAdded()) {
                    isSearching = false;
                    searchProgressBar.setVisibility(View.GONE);
                    if (response.isSuccessful() && response.body() != null) {
                        List<ContentItem> items = response.body();
                        if (items.isEmpty()) {
                            if (searchPage == 1) searchEmptyView.setVisibility(View.VISIBLE);
                        } else {
                            searchResults.addAll(items);
                            searchAdapter.notifyDataSetChanged();
                        }
                    } else {
                        Toast.makeText(requireContext(), "搜索失败", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<ContentItem>> call, @NonNull Throwable t) {
                if (isAdded()) {
                    isSearching = false;
                    searchProgressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "网络错误", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadMoreSearchResults() {
        searchPage++;
        isSearching = true;
        ApiClient.getApiService().searchPosts(
                currentQuery, searchPage, 10,
                "id,title,date,categories,jetpack_featured_media_url,_embedded,zib_other_data,view_count,like_count,author_info",
                "wp:featuredmedia"
        ).enqueue(new Callback<List<ContentItem>>() {
            @Override
            public void onResponse(@NonNull Call<List<ContentItem>> call, @NonNull Response<List<ContentItem>> response) {
                if (isAdded()) {
                    isSearching = false;
                    if (response.isSuccessful() && response.body() != null) {
                        List<ContentItem> items = response.body();
                        if (!items.isEmpty()) {
                            int start = searchResults.size();
                            searchResults.addAll(items);
                            searchAdapter.notifyItemRangeInserted(start, items.size());
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<ContentItem>> call, @NonNull Throwable t) {
                if (isAdded()) {
                    isSearching = false;
                }
            }
        });
    }

    private void fetchCategories(Set<Integer> categoryIds) {
        if (categoryIds.isEmpty()) {
            adapter.notifyDataSetChanged();
            isLoading = false;
            progressBar.setVisibility(View.GONE);
            return;
        }
        String ids = categoryIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        ApiClient.getApiService().getCategoriesByIds(ids, categoryIds.size()).enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(@NonNull Call<List<Category>> call, @NonNull Response<List<Category>> response) {
                if (isAdded()) {
                    if (response.isSuccessful() && response.body() != null) {
                        for (Category category : response.body()) {
                            categoryMap.put(category.getId(), category.getName());
                        }
                    }
                    adapter.notifyDataSetChanged();
                    isLoading = false;
                    progressBar.setVisibility(View.GONE);
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<Category>> call, @NonNull Throwable t) {
                if (isAdded()) {
                    isLoading = false;
                    progressBar.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.home_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        mSearchView = (SearchView) searchItem.getActionView();
        mSearchView.setQueryHint("搜索文章...");

        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(@NonNull MenuItem item) {
                searchResultsContainer.setVisibility(View.VISIBLE);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(@NonNull MenuItem item) {
                searchResultsContainer.setVisibility(View.GONE);
                searchResults.clear();
                searchAdapter.notifyDataSetChanged();
                return true;
            }
        });

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (query != null && !query.trim().isEmpty()) {
                    performSearch(query.trim());
                    mSearchView.clearFocus();
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            if (searchResultsContainer.getVisibility() == View.VISIBLE) return true;

            currentSeed = String.valueOf(System.currentTimeMillis());
            swipeRefreshLayout.setRefreshing(true);
            currentPage = 1;
            postList.clear();
            categoryMap.clear();
            adapter.notifyDataSetChanged();
            loadPosts();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean handleBackPressed() {
        if (mSearchView != null && !mSearchView.isIconified()) {
            mSearchView.setIconified(true);
            return true;
        }
        return false;
    }
}
