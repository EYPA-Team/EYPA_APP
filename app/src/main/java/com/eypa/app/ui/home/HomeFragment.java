package com.eypa.app.ui.home;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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
    private LinearLayout errorLayout;
    private Button btnRetry;
    private PostsAdapter adapter;
    private List<ContentItem> postList = new ArrayList<>();
    private Map<Integer, String> categoryMap = new HashMap<>();
    private int currentPage = 1;
    private String currentSeed;
    private boolean isLoading = false;
    private boolean isFirstLoad = true;
    private long refreshStartTime = 0;
    private int retryCount = 0;
    private Integer currentCategoryId = null;

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
    private MenuItem filterItem;

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
        errorLayout = view.findViewById(R.id.error_layout);
        btnRetry = view.findViewById(R.id.btn_retry);

        btnRetry.setOnClickListener(v -> {
            errorLayout.setVisibility(View.GONE);
            currentSeed = String.valueOf(System.currentTimeMillis());
            currentPage = 1;
            retryCount = 0;
            loadPosts();
        });

        TypedValue typedValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true);
        swipeRefreshLayout.setColorSchemeColors(typedValue.data);

        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new PostsAdapter(postList, categoryMap);
        recyclerView.setAdapter(adapter);

        if (isFirstLoad) {
            recyclerView.setAlpha(0f);
        } else {
            recyclerView.setAlpha(1f);
        }

        swipeRefreshLayout.setOnRefreshListener(() -> {
            currentSeed = String.valueOf(System.currentTimeMillis());
            currentPage = 1;
            refreshStartTime = System.currentTimeMillis();
            recyclerView.animate().alpha(0f).setDuration(300).start();
            retryCount = 0;
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
                    retryCount = 0;
                    loadPosts();
                }
            }
        });
        
        recyclerView.setOverScrollMode(View.OVER_SCROLL_ALWAYS);

        // --- 搜索视图初始化 ---
        searchResultsContainer = view.findViewById(R.id.search_results_container);
        searchRecyclerView = view.findViewById(R.id.search_recycler_view);
        searchProgressBar = view.findViewById(R.id.search_progress_bar);
        searchEmptyView = view.findViewById(R.id.search_empty_view);

        searchAdapter = new PostsAdapter(searchResults, categoryMap);
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
        if (isFirstLoad) {
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setAlpha(1f);
            errorLayout.setVisibility(View.GONE);
        } else if (currentPage > 1) {
            adapter.setLoadingFooterVisible(true);
        }

        ApiClient.getApiService().getContentItems(
                currentPage, 10, "id,title,date,categories,jetpack_featured_media_url,_embedded,zib_other_data,view_count,like_count,author_info",
                "rand", currentSeed, "wp:featuredmedia", currentCategoryId
        ).enqueue(new Callback<List<ContentItem>>() {
            @Override
            public void onResponse(@NonNull Call<List<ContentItem>> call, @NonNull Response<List<ContentItem>> response) {
                if (isAdded()) {
                    swipeRefreshLayout.setRefreshing(false);
                    adapter.setLoadingFooterVisible(false);
                    
                    if (response.isSuccessful() && response.body() != null) {
                        retryCount = 0;
                        postList.addAll(response.body());
                        Set<Integer> allCategoryIds = postList.stream()
                                .filter(p -> p.getCategories() != null)
                                .flatMap(p -> p.getCategories().stream())
                                .collect(Collectors.toSet());
                        
                        if (isFirstLoad) {
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
                            
                            adapter.notifyDataSetChanged();
                            fetchCategories(allCategoryIds);
                        } else {
                            if (currentPage == 1) {
                                long elapsedTime = System.currentTimeMillis() - refreshStartTime;
                                long remainingTime = 300 - elapsedTime;
                                if (remainingTime < 0) remainingTime = 0;

                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                    if (isAdded()) {
                                        postList.clear();
                                        categoryMap.clear();
                                        postList.addAll(response.body());
                                        adapter.notifyDataSetChanged();

                                        recyclerView.animate().alpha(1f).setDuration(300).start();
                                        fetchCategories(allCategoryIds);
                                    }
                                }, remainingTime);
                            } else {
                                fetchCategories(allCategoryIds);
                            }
                        }
                    } else {
                        handleLoadFailure(response.code() + "");
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<ContentItem>> call, @NonNull Throwable t) {
                if (isAdded()) {
                    adapter.setLoadingFooterVisible(false);
                    handleLoadFailure(t.getMessage());
                }
            }
        });
    }

    private void handleLoadFailure(String errorMsg) {
        if (isFirstLoad && retryCount < 3) {
            retryCount++;
            Log.d("HomeFragment", "Loading failed, retrying... (" + retryCount + "/3)");
            new Handler(Looper.getMainLooper()).postDelayed(this::loadPosts, 1000);
        } else {
            swipeRefreshLayout.setRefreshing(false);
            isLoading = false;
            progressBar.setVisibility(View.GONE);
            recyclerView.animate().alpha(1f).setDuration(300).start();
            if (isFirstLoad) {
                errorLayout.setVisibility(View.VISIBLE);
            }
            Log.e("API_FAILURE", "Load failed: " + errorMsg);
        }
    }

    private void performSearch(String query) {
        currentQuery = query;
        searchPage = 1;
        searchResults.clear();
        searchAdapter.notifyDataSetChanged();
        searchEmptyView.setVisibility(View.GONE);

        isSearching = true;
        searchProgressBar.setVisibility(View.VISIBLE);
        
        searchRecyclerView.setAlpha(0f);

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
                            fetchCategoriesForSearch(items);
                            
                            searchRecyclerView.animate()
                                    .alpha(1f)
                                    .setDuration(300)
                                    .start();
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
                            fetchCategoriesForSearch(items);
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
            if (!isFirstLoad && progressBar.getVisibility() == View.VISIBLE && progressBar.getAlpha() == 1f) {
                progressBar.setVisibility(View.GONE);
            }
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
                    if (progressBar.getVisibility() == View.VISIBLE && progressBar.getAlpha() == 1f) {
                        progressBar.setVisibility(View.GONE);
                    }
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<Category>> call, @NonNull Throwable t) {
                if (isAdded()) {
                    isLoading = false;
                    if (progressBar.getVisibility() == View.VISIBLE && progressBar.getAlpha() == 1f) {
                        progressBar.setVisibility(View.GONE);
                    }
                }
            }
        });
    }

    private void fetchCategoriesForSearch(List<ContentItem> items) {
        Set<Integer> categoryIds = items.stream()
                .filter(p -> p.getCategories() != null)
                .flatMap(p -> p.getCategories().stream())
                .collect(Collectors.toSet());

        if (categoryIds.isEmpty()) return;

        Set<Integer> missingIds = categoryIds.stream()
                .filter(id -> !categoryMap.containsKey(id))
                .collect(Collectors.toSet());

        if (missingIds.isEmpty()) {
            searchAdapter.notifyDataSetChanged();
            return;
        }

        String ids = missingIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        ApiClient.getApiService().getCategoriesByIds(ids, missingIds.size()).enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(@NonNull Call<List<Category>> call, @NonNull Response<List<Category>> response) {
                if (isAdded()) {
                    if (response.isSuccessful() && response.body() != null) {
                        for (Category category : response.body()) {
                            categoryMap.put(category.getId(), category.getName());
                        }
                        searchAdapter.notifyDataSetChanged();
                        adapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Category>> call, @NonNull Throwable t) {
                // 不做处理
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.home_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        filterItem = menu.findItem(R.id.action_filter);
        
        mSearchView = (SearchView) searchItem.getActionView();
        mSearchView.setQueryHint("搜索文章...");

        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(@NonNull MenuItem item) {
                searchResultsContainer.setVisibility(View.VISIBLE);
                if (filterItem != null) {
                    filterItem.setVisible(false);
                }
                if (getActivity() instanceof HomeActivity) {
                    ((HomeActivity) getActivity()).setBottomNavigationVisibility(View.GONE);
                }
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(@NonNull MenuItem item) {
                searchResultsContainer.setVisibility(View.GONE);
                searchResults.clear();
                searchAdapter.notifyDataSetChanged();
                if (filterItem != null) {
                    filterItem.setVisible(true);
                }
                if (getActivity() instanceof HomeActivity) {
                    ((HomeActivity) getActivity()).setBottomNavigationVisibility(View.VISIBLE);
                }
                requireActivity().invalidateOptionsMenu();
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

        if (id == R.id.action_filter) {
            if (searchResultsContainer.getVisibility() == View.VISIBLE) return true;
            showCategoryFilterDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showCategoryFilterDialog() {
        Toast.makeText(requireContext(), "请稍后...", Toast.LENGTH_SHORT).show();
        
        ApiClient.getApiService().getAllCategories(100, "count", true).enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(@NonNull Call<List<Category>> call, @NonNull Response<List<Category>> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null) {
                    List<Category> categories = response.body();
                    showFilterDialog(categories);
                } else {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "获取分区失败", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Category>> call, @NonNull Throwable t) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "网络错误", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showFilterDialog(List<Category> categories) {
        String[] items = new String[categories.size() + 1];
        items[0] = "全部";
        for (int i = 0; i < categories.size(); i++) {
            items[i + 1] = categories.get(i).getName();
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("选择分区")
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        currentCategoryId = null;
                    } else {
                        currentCategoryId = categories.get(which - 1).getId();
                    }
                    
                    currentSeed = String.valueOf(System.currentTimeMillis());
                    swipeRefreshLayout.setRefreshing(true);
                    currentPage = 1;
                    postList.clear();
                    adapter.notifyDataSetChanged();
                    refreshStartTime = System.currentTimeMillis();
                    recyclerView.animate().alpha(0f).setDuration(300).start();
                    loadPosts();
                })
                .show();
    }

    public boolean handleBackPressed() {
        if (mSearchView != null && !mSearchView.isIconified()) {
            closeSearch();
            return true;
        }
        return false;
    }

    public void closeSearch() {
        if (mSearchView != null && !mSearchView.isIconified()) {
            mSearchView.setQuery("", false);
            mSearchView.setIconified(true);
        }
        
        if (searchResultsContainer != null) {
            searchResultsContainer.setVisibility(View.GONE);
        }
        
        if (searchResults != null) {
            searchResults.clear();
            if (searchAdapter != null) {
                searchAdapter.notifyDataSetChanged();
            }
        }
    }
}
