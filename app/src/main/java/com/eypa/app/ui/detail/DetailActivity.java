package com.eypa.app.ui.detail;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.text.HtmlCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.eypa.app.R;
import com.eypa.app.model.ContentItem;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;

public class DetailActivity extends AppCompatActivity implements DetailContentFragment.OnEpisodeInteractionListener {

    public static final String EXTRA_POST_ID = "post_id";
    private static final String KEY_PLAYBACK_POSITION = "playback_position";
    private static final String KEY_CURRENT_WINDOW = "current_window";
    private static final String KEY_PLAY_WHEN_READY = "play_when_ready";

    private DetailViewModel viewModel;
    private ProgressBar progressBar;
    private ImageView coverImage;
    private PlayerView playerView;
    private ImageButton playButtonOverlay;
    private CollapsingToolbarLayout collapsingToolbar;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private ExoPlayer player;
    private boolean playWhenReady = true;
    private int currentWindow = 0;
    private long playbackPosition = 0;
    private String currentVideoUrl = null;
    private FrameLayout mediaContainer;
    private FrameLayout fullscreenContainer;
    private Toolbar toolbar;
    private AppBarLayout appBarLayout;
    private View contentTabsAndPager;
    private boolean isTitleShown = false;
    private String postTitle = "";
    private List<ContentItem.Episode> currentEpisodes = new ArrayList<>();

    public static void start(Context context, int postId) {
        Intent intent = new Intent(context, DetailActivity.class);
        intent.putExtra(EXTRA_POST_ID, postId);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean("DarkMode", false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        int themeId = sharedPreferences.getInt("ThemeId", R.style.Theme_EYPA_APP);
        setTheme(themeId);

        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_detail);

        if (savedInstanceState != null) {
            playbackPosition = savedInstanceState.getLong(KEY_PLAYBACK_POSITION, 0L);
            currentWindow = savedInstanceState.getInt(KEY_CURRENT_WINDOW, 0);
            playWhenReady = savedInstanceState.getBoolean(KEY_PLAY_WHEN_READY, true);
        }

        initViews();
        setupToolbar();
        setupViewPager();
        setupPlayer();
        setupCollapsingToolbarListener();
        updateUiForOrientation(getResources().getConfiguration().orientation);

        viewModel = new ViewModelProvider(this).get(DetailViewModel.class);
        observeViewModel();

        int postId = getIntent().getIntExtra(EXTRA_POST_ID, -1);
        if (postId != -1) {
            viewModel.loadPostAndComments(postId);
        } else {
            finish();
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                } else {
                    setEnabled(false);
                    DetailActivity.super.onBackPressed();
                }
            }
        });
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateUiForOrientation(newConfig.orientation);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (player != null) {
            outState.putLong(KEY_PLAYBACK_POSITION, player.getCurrentPosition());
            outState.putInt(KEY_CURRENT_WINDOW, player.getCurrentMediaItemIndex());
            outState.putBoolean(KEY_PLAY_WHEN_READY, player.getPlayWhenReady());
        } else {
            outState.putLong(KEY_PLAYBACK_POSITION, playbackPosition);
            outState.putInt(KEY_CURRENT_WINDOW, currentWindow);
            outState.putBoolean(KEY_PLAY_WHEN_READY, playWhenReady);
        }
    }

    @Override
    public void onSwitchEpisode(ContentItem.Episode clickedEpisode) {
        ContentItem post = viewModel.getPostData().getValue();
        if (post == null) return;

        if (currentVideoUrl != null && currentVideoUrl.equals(clickedEpisode.getUrl())) {
            return;
        }

        currentVideoUrl = clickedEpisode.getUrl();
        playbackPosition = 0;
        currentWindow = 0;
        initializePlayer(currentVideoUrl, true);

        post.updatePlayingEpisode(clickedEpisode);

        Fragment fragment = getSupportFragmentManager().findFragmentByTag("f0");
        if (fragment instanceof DetailContentFragment) {
            View fragView = fragment.getView();
            if (fragView != null) {
                RecyclerView rv = fragView.findViewById(R.id.content_recycler_view);
                if (rv != null && rv.getAdapter() != null) {
                    rv.getAdapter().notifyDataSetChanged();
                }
            }
        }
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        progressBar = findViewById(R.id.progress_bar);
        coverImage = findViewById(R.id.cover_image);
        playerView = findViewById(R.id.player_view);
        playButtonOverlay = findViewById(R.id.play_button_overlay);
        collapsingToolbar = findViewById(R.id.collapsing_toolbar);
        mediaContainer = findViewById(R.id.media_container);
        fullscreenContainer = findViewById(R.id.fullscreen_container);
        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);
        appBarLayout = findViewById(R.id.app_bar);
        contentTabsAndPager = findViewById(R.id.content_tabs_and_pager);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setTitle("");
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_content), (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            TypedValue tv = new TypedValue();
            int actionBarHeight = getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)
                    ? TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics()) : 0;
            ViewGroup.LayoutParams params = toolbar.getLayoutParams();
            params.height = actionBarHeight + topInset;
            toolbar.setLayoutParams(params);
            toolbar.setPadding(0, topInset, 0, 0);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void setupViewPager() {
        viewPager.setAdapter(new DetailPagerAdapter(this));
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) tab.setText("内容");
            else tab.setText("评论");
        }).attach();
    }

    private void setupCollapsingToolbarListener() {
        appBarLayout.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
            if (Math.abs(verticalOffset) >= appBarLayout.getTotalScrollRange()) {
                if (!isTitleShown) {
                    collapsingToolbar.setTitle(postTitle);
                    isTitleShown = true;
                }
            } else {
                if (isTitleShown) {
                    collapsingToolbar.setTitle("");
                    isTitleShown = false;
                }
            }
        });
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(this, isLoading ->
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE));
        viewModel.getPostData().observe(this, this::displayHeaderInfo);
    }

    private void displayHeaderInfo(ContentItem post) {
        if (post == null) return;
        postTitle = HtmlCompat.fromHtml(post.getTitle(), HtmlCompat.FROM_HTML_MODE_LEGACY).toString();

        currentEpisodes.clear();
        currentEpisodes.addAll(post.getAllEpisodes());

        String firstVideoUrl = (!currentEpisodes.isEmpty()) ? currentEpisodes.get(0).getUrl() : null;

        if (currentVideoUrl == null && firstVideoUrl != null) {
            currentVideoUrl = firstVideoUrl;
        }

        String imageUrl = post.getBestImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this).load(imageUrl).into(coverImage);
        }

        if (currentVideoUrl != null && !currentVideoUrl.isEmpty()) {
            if (playbackPosition > 0) {
                playButtonOverlay.setVisibility(View.GONE);
                coverImage.setVisibility(View.GONE);
                playerView.setVisibility(View.VISIBLE);
                if (player == null) { // 确保旋转后重新初始化播放器
                    initializePlayer(currentVideoUrl, playWhenReady);
                }
            } else {
                playButtonOverlay.setVisibility(View.VISIBLE);
                playButtonOverlay.setOnClickListener(v -> {
                    playButtonOverlay.setVisibility(View.GONE);
                    coverImage.setVisibility(View.GONE);
                    playerView.setVisibility(View.VISIBLE);
                    initializePlayer(currentVideoUrl, true);
                });
            }
        }
    }

    private void setupPlayer() {
        playerView.setFullscreenButtonClickListener(isFullscreen -> {
            if (isFullscreen) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        });
    }

    private void initializePlayer(String videoUrl, boolean startPlayback) {
        if (player != null) player.release();
        player = new ExoPlayer.Builder(this).build();

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_READY || playbackState == Player.STATE_BUFFERING) {
                    // 隐藏标题栏
                    View titleBar = playerView.findViewById(
                            getResources().getIdentifier("exo_title_bar", "id", getPackageName())
                    );
                    if (titleBar != null) {
                        titleBar.setVisibility(View.GONE);
                    }
                }
            }
            // 删除了动态计算高度的 onVideoSizeChanged 方法 ---
        });

        playerView.setPlayer(player);

        MediaItem mediaItem = MediaItem.fromUri(videoUrl);
        player.setMediaItem(mediaItem);
        player.setPlayWhenReady(startPlayback);
        player.seekTo(currentWindow, playbackPosition);
        player.prepare();
    }

    private void releasePlayer() {
        if (player != null) {
            playbackPosition = player.getCurrentPosition();
            currentWindow = player.getCurrentMediaItemIndex();
            playWhenReady = player.getPlayWhenReady();
            player.release();
            player = null;
        }
    }

    private void updateUiForOrientation(int orientation) {
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            appBarLayout.setVisibility(View.GONE);
            contentTabsAndPager.setVisibility(View.GONE);
            fullscreenContainer.setVisibility(View.VISIBLE);

            if (playerView.getParent() != fullscreenContainer) {
                ((ViewGroup) playerView.getParent()).removeView(playerView);
                fullscreenContainer.addView(playerView, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }

            WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
            windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        } else {
            appBarLayout.setVisibility(View.VISIBLE);
            contentTabsAndPager.setVisibility(View.VISIBLE);
            fullscreenContainer.setVisibility(View.GONE);

            if (playerView.getParent() != mediaContainer) {
                ((ViewGroup) playerView.getParent()).removeView(playerView);
                // --- 竖屏模式下，填满 XML 中定义的固定高度容器 ---
                mediaContainer.addView(playerView, 0, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }

            WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (Build.VERSION.SDK_INT > 23) {
            if (playerView.getVisibility() == View.VISIBLE && player == null && currentVideoUrl != null) {
                initializePlayer(currentVideoUrl, playWhenReady);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if ((Build.VERSION.SDK_INT <= 23 || player == null) && playerView.getVisibility() == View.VISIBLE && currentVideoUrl != null) {
            initializePlayer(currentVideoUrl, playWhenReady);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (Build.VERSION.SDK_INT <= 23) {
            releasePlayer();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (Build.VERSION.SDK_INT > 23) {
            releasePlayer();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
