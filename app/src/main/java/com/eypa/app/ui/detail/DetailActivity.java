package com.eypa.app.ui.detail;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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
import com.eypa.app.utils.ThemeUtils;
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
    private TextView tvSeekOverlay;
    private TextView tvSpeedOverlay;
    private TextView tvGestureOverlay;
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
    
    private View commentInputContainer;
    private EditText editComment;
    private Button btnSend;
    private int replyToCommentId = 0;
    private int editCommentId = 0;
    
    private GestureDetector gestureDetector;
    private boolean isSeeking = false;
    private boolean isLongPressing = false;
    private boolean isVolumeControl = false;
    private boolean isBrightnessControl = false;
    private long gestureStartPosition = 0;
    private long seekTargetPosition = 0;
    private AudioManager audioManager;
    private int initialVolume = -1;
    private float initialBrightness = -1f;
    private int gestureStartVolume = 0;
    private float gestureStartBrightness = 0f;

    public static void start(Context context, int postId) {
        Intent intent = new Intent(context, DetailActivity.class);
        intent.putExtra(EXTRA_POST_ID, postId);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applyTheme(this);
        SharedPreferences sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        int themeId = sharedPreferences.getInt("ThemeId", R.style.Theme_EYPA_APP);
        setTheme(themeId);

        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode = 
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        
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
        
        if (player != null) {
            int index = currentEpisodes.indexOf(clickedEpisode);
            if (index != -1) {
                player.seekTo(index, 0);
                player.play();
            } else {
                playbackPosition = 0;
                currentWindow = 0;
                initializePlayer(currentVideoUrl, true);
            }
        } else {
            playbackPosition = 0;
            currentWindow = 0;
            initializePlayer(currentVideoUrl, true);
        }

        updateEpisodeUi(clickedEpisode);
    }

    private void updateEpisodeUi(ContentItem.Episode episode) {
        ContentItem post = viewModel.getPostData().getValue();
        if (post != null) {
            post.updatePlayingEpisode(episode);
        }

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
        tvSeekOverlay = findViewById(R.id.tv_seek_overlay);
        tvSpeedOverlay = findViewById(R.id.tv_speed_overlay);
        tvGestureOverlay = findViewById(R.id.tv_gesture_overlay);
        playButtonOverlay = findViewById(R.id.play_button_overlay);
        collapsingToolbar = findViewById(R.id.collapsing_toolbar);
        mediaContainer = findViewById(R.id.media_container);
        fullscreenContainer = findViewById(R.id.fullscreen_container);
        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);
        appBarLayout = findViewById(R.id.app_bar);
        contentTabsAndPager = findViewById(R.id.content_tabs_and_pager);
        
        commentInputContainer = findViewById(R.id.comment_input_container);
        editComment = findViewById(R.id.edit_comment);
        btnSend = findViewById(R.id.btn_send);
        
        appBarLayout.setVisibility(View.INVISIBLE);
        contentTabsAndPager.setVisibility(View.INVISIBLE);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            int imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            int navBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            
            if (commentInputContainer != null) {
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) commentInputContainer.getLayoutParams();
                params.bottomMargin = imeHeight;
                commentInputContainer.setLayoutParams(params);
            }
            
            return insets;
        });

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        initialVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        initialBrightness = lp.screenBrightness;
        
        btnSend.setOnClickListener(v -> {
            String content = editComment.getText().toString().trim();
            if (content.isEmpty()) {
                Toast.makeText(this, "请输入评论内容", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (editCommentId > 0) {
                viewModel.editComment(editCommentId, content);
                editComment.setText("");
                editComment.clearFocus();
                editCommentId = 0;
                editComment.setHint("说点什么吧...");
                viewModel.setEditComment(null);
                
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(editComment.getWindowToken(), 0);
                return;
            }
            
            if (viewModel.getPostData().getValue() != null) {
                int postId = viewModel.getPostData().getValue().getId();
                viewModel.submitComment(postId, content, replyToCommentId);
                editComment.setText("");
                editComment.clearFocus();
                
                if (replyToCommentId > 0) {
                    replyToCommentId = 0;
                    editComment.setHint("说点什么吧...");
                    viewModel.setReplyToComment(null);
                }
                
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(editComment.getWindowToken(), 0);
            }
        });
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
        
        toolbar.setClickable(false);
        toolbar.setOnTouchListener((v, event) -> false);
    }

    private void setupViewPager() {
        viewPager.setAdapter(new DetailPagerAdapter(this));
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) tab.setText("内容");
            else tab.setText("评论");
        }).attach();
        
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (position == 1) {
                    commentInputContainer.setVisibility(View.VISIBLE);
                    commentInputContainer.setAlpha(0f);
                    commentInputContainer.setTranslationY(getResources().getDisplayMetrics().density * 60);
                    commentInputContainer.animate()
                            .alpha(1f)
                            .translationY(0f)
                            .setDuration(300)
                            .setListener(null)
                            .start();
                } else {
                    if (commentInputContainer.getVisibility() == View.VISIBLE) {
                        commentInputContainer.animate()
                                .alpha(0f)
                                .translationY(commentInputContainer.getHeight())
                                .setDuration(300)
                                .withEndAction(() -> commentInputContainer.setVisibility(View.GONE))
                                .start();
                        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (getCurrentFocus() != null) {
                            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                        }
                    }
                }
            }
        });
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
        viewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading) {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setAlpha(1f);
                appBarLayout.setVisibility(View.INVISIBLE);
                contentTabsAndPager.setVisibility(View.INVISIBLE);
            } else {
                if (progressBar.getVisibility() == View.VISIBLE) {
                    int aniDuration = 300;

                    progressBar.animate()
                            .alpha(0f)
                            .setDuration(aniDuration)
                            .withEndAction(() -> progressBar.setVisibility(View.GONE))
                            .start();

                    appBarLayout.setAlpha(0f);
                    appBarLayout.setVisibility(View.VISIBLE);
                    appBarLayout.animate()
                            .alpha(1f)
                            .setDuration(aniDuration)
                            .start();

                    contentTabsAndPager.setAlpha(0f);
                    contentTabsAndPager.setVisibility(View.VISIBLE);
                    contentTabsAndPager.animate()
                            .alpha(1f)
                            .setDuration(aniDuration)
                            .start();
                } else {
                    progressBar.setVisibility(View.GONE);
                    appBarLayout.setVisibility(View.VISIBLE);
                    appBarLayout.setAlpha(1f);
                    contentTabsAndPager.setVisibility(View.VISIBLE);
                    contentTabsAndPager.setAlpha(1f);
                }
            }
        });
        viewModel.getPostData().observe(this, this::displayHeaderInfo);
        viewModel.getTotalCommentCount().observe(this, count -> {
            TabLayout.Tab commentTab = tabLayout.getTabAt(1);
            if (commentTab != null) {
                if (count != 0) {
                    commentTab.setText("评论 " + count);
                } else {
                    commentTab.setText("评论");
                }
            }
        });
        
        viewModel.getReplyToComment().observe(this, comment -> {
            if (comment != null) {
                replyToCommentId = comment.getId();
                editCommentId = 0;
                String authorName = comment.getAuthorName();
                editComment.setHint("回复 " + authorName + ":");
                editComment.requestFocus();
                
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(editComment, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            } else {
                replyToCommentId = 0;
                if (editCommentId == 0) {
                    editComment.setHint("说点什么吧...");
                }
            }
        });
        
        viewModel.getEditComment().observe(this, comment -> {
            if (comment != null) {
                editCommentId = comment.getId();
                replyToCommentId = 0;
                editComment.setText(HtmlCompat.fromHtml(comment.getContent().getRaw(), HtmlCompat.FROM_HTML_MODE_LEGACY).toString());
                editComment.setSelection(editComment.getText().length());
                editComment.setHint("编辑评论...");
                editComment.requestFocus();
                
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(editComment, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            } else {
                editCommentId = 0;
                if (replyToCommentId == 0) {
                    editComment.setHint("说点什么吧...");
                }
            }
        });
    }

    private void displayHeaderInfo(ContentItem post) {
        if (post == null) return;

        com.eypa.app.db.HistoryManager.getInstance(this).addHistory(post);

        postTitle = HtmlCompat.fromHtml(post.getTitle(), HtmlCompat.FROM_HTML_MODE_LEGACY).toString();

        currentEpisodes.clear();
        currentEpisodes.addAll(post.getAllEpisodes());

        String firstVideoUrl = (!currentEpisodes.isEmpty()) ? currentEpisodes.get(0).getUrl() : null;

        if (currentVideoUrl == null && firstVideoUrl != null) {
            currentVideoUrl = firstVideoUrl;
        }

        if (currentVideoUrl != null && !isValidVideoUrl(currentVideoUrl)) {
            currentVideoUrl = null;
        }

        String imageUrl = post.getBestImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            coverImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Glide.with(this)
                    .load(imageUrl)
                    .error(R.drawable.placeholder_image)
                    .into(coverImage);
        } else {
            coverImage.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            coverImage.setImageResource(R.drawable.placeholder_image);
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
        } else {
            if (imageUrl != null && !imageUrl.isEmpty()) {
                coverImage.setOnClickListener(v -> {
                    ImageViewerFragment.newInstance(imageUrl)
                            .show(getSupportFragmentManager(), "image_viewer");
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

        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                if (playerView != null && playerView.getParent() != null) {
                    playerView.getParent().requestDisallowInterceptTouchEvent(true);
                }
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (player != null) {
                    if (player.isPlaying()) {
                        player.pause();
                    } else {
                        player.play();
                    }
                    return true;
                }
                return false;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (playerView != null) {
                    if (playerView.isControllerFullyVisible()) {
                        playerView.hideController();
                    } else {
                        playerView.showController();
                    }
                    return true;
                }
                return false;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (player == null) return false;
                
                float totalDistanceX = e2.getX() - e1.getX();
                float totalDistanceY = e1.getY() - e2.getY();

                if (Math.abs(totalDistanceX) > Math.abs(totalDistanceY)) {
                    if (!isVolumeControl && !isBrightnessControl) {
                        if (!isSeeking) {
                            isSeeking = true;
                            gestureStartPosition = player.getCurrentPosition();
                            tvSeekOverlay.setVisibility(View.VISIBLE);
                            playerView.getParent().requestDisallowInterceptTouchEvent(true);
                        }

                        long deltaMs = (long) (totalDistanceX / 20.0 * 1000);
                        
                        seekTargetPosition = Math.max(0, Math.min(player.getDuration(), gestureStartPosition + deltaMs));

                        updateSeekOverlay(deltaMs);
                        return true;
                    }
                } else {
                    if (!isSeeking) {
                        if (!isVolumeControl && !isBrightnessControl) {
                            if (e1.getX() > playerView.getWidth() / 2) {
                                isVolumeControl = true;
                                gestureStartVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                            } else {
                                isBrightnessControl = true;
                                WindowManager.LayoutParams lp = getWindow().getAttributes();
                                gestureStartBrightness = lp.screenBrightness;
                                if (gestureStartBrightness < 0) {
                                    try {
                                        int sysBrightness = android.provider.Settings.System.getInt(getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS);
                                        gestureStartBrightness = sysBrightness / 255.0f;
                                    } catch (android.provider.Settings.SettingNotFoundException e) {
                                        gestureStartBrightness = 0.5f;
                                    }
                                }
                            }
                            tvGestureOverlay.setVisibility(View.VISIBLE);
                            playerView.getParent().requestDisallowInterceptTouchEvent(true);
                        }

                        float percent = totalDistanceY / playerView.getHeight();
                        
                        if (isVolumeControl) {
                            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                            int deltaVolume = (int) (maxVolume * percent);
                            int targetVolume = Math.max(0, Math.min(maxVolume, gestureStartVolume + deltaVolume));
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0);
                            tvGestureOverlay.setText(String.format("音量 %d%%", (int) ((targetVolume / (float) maxVolume) * 100)));
                        } else if (isBrightnessControl) {
                            float targetBrightness = Math.max(0.01f, Math.min(1.0f, gestureStartBrightness + percent));
                            WindowManager.LayoutParams lp = getWindow().getAttributes();
                            lp.screenBrightness = targetBrightness;
                            getWindow().setAttributes(lp);
                            tvGestureOverlay.setText(String.format("亮度 %d%%", (int) (targetBrightness * 100)));
                        }
                        return true;
                    }
                }
                return false;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                if (player != null && player.isPlaying() && !isSeeking) {
                    isLongPressing = true;
                    player.setPlaybackParameters(new androidx.media3.common.PlaybackParameters(2.0f));
                    tvSpeedOverlay.setVisibility(View.VISIBLE);
                    playerView.getParent().requestDisallowInterceptTouchEvent(true);
                }
            }
        });

        playerView.setOnTouchListener((v, event) -> {
            if (gestureDetector.onTouchEvent(event)) {
                return true;
            }

            if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                if (isSeeking) {
                    isSeeking = false;
                    if (player != null) {
                        player.seekTo(seekTargetPosition);
                    }
                    tvSeekOverlay.setVisibility(View.GONE);
                    return true;
                }

                if (isLongPressing) {
                    isLongPressing = false;
                    if (player != null) {
                        player.setPlaybackParameters(new androidx.media3.common.PlaybackParameters(1.0f));
                    }
                    tvSpeedOverlay.setVisibility(View.GONE);
                    return true;
                }

                if (isVolumeControl || isBrightnessControl) {
                    isVolumeControl = false;
                    isBrightnessControl = false;
                    tvGestureOverlay.setVisibility(View.GONE);
                    return true;
                }
            }
            return false;
        });
    }

    private void updateSeekOverlay(long deltaMs) {
        if (player == null) return;
        String current = formatTime(seekTargetPosition);
        String total = formatTime(player.getDuration());
        
        tvSeekOverlay.setText(String.format("%s / %s", current, total));
    }

    private String formatTime(long ms) {
        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private boolean isValidVideoUrl(String url) {
        if (url == null || url.isEmpty()) return false;
        String lowerUrl = url.toLowerCase();
        int queryIndex = lowerUrl.indexOf("?");
        if (queryIndex != -1) {
            lowerUrl = lowerUrl.substring(0, queryIndex);
        }
        
        return lowerUrl.endsWith(".mp4") || lowerUrl.endsWith(".m3u8") || 
               lowerUrl.endsWith(".webm") || lowerUrl.endsWith(".mkv") || 
               lowerUrl.endsWith(".avi") || lowerUrl.endsWith(".flv") || 
               lowerUrl.endsWith(".mov") || lowerUrl.endsWith(".rmvb") || 
               lowerUrl.endsWith(".ts") || lowerUrl.endsWith(".mpd");
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

            @Override
            public void onMediaItemTransition(MediaItem mediaItem, int reason) {
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO || 
                    reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK) {
                    int index = player.getCurrentMediaItemIndex();
                    if (index >= 0 && index < currentEpisodes.size()) {
                        ContentItem.Episode newEpisode = currentEpisodes.get(index);
                        if (currentVideoUrl == null || !currentVideoUrl.equals(newEpisode.getUrl())) {
                            currentVideoUrl = newEpisode.getUrl();
                            updateEpisodeUi(newEpisode);
                        }
                    }
                }
            }
        });

        playerView.setPlayer(player);

        List<MediaItem> mediaItems = new ArrayList<>();
        int startIndex = 0;
        if (!currentEpisodes.isEmpty()) {
            for (int i = 0; i < currentEpisodes.size(); i++) {
                mediaItems.add(MediaItem.fromUri(currentEpisodes.get(i).getUrl()));
                if (currentEpisodes.get(i).getUrl().equals(videoUrl)) {
                    startIndex = i;
                }
            }
            player.setMediaItems(mediaItems, startIndex, playbackPosition);
        } else {
            MediaItem mediaItem = MediaItem.fromUri(videoUrl);
            player.setMediaItem(mediaItem);
            player.seekTo(currentWindow, playbackPosition);
        }

        player.setPlayWhenReady(startPlayback);
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
            
            if (commentInputContainer != null) {
                commentInputContainer.setVisibility(View.GONE);
            }

            if (playerView.getParent() != fullscreenContainer) {
                ((ViewGroup) playerView.getParent()).removeView(playerView);
                fullscreenContainer.addView(playerView, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
            
            if (tvSeekOverlay.getParent() != fullscreenContainer) {
                ((ViewGroup) tvSeekOverlay.getParent()).removeView(tvSeekOverlay);
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.gravity = android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL;
                params.topMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 32, getResources().getDisplayMetrics());
                fullscreenContainer.addView(tvSeekOverlay, params);
            }

            if (tvSpeedOverlay.getParent() != fullscreenContainer) {
                ((ViewGroup) tvSpeedOverlay.getParent()).removeView(tvSpeedOverlay);
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.gravity = android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL;
                params.topMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 32, getResources().getDisplayMetrics());
                fullscreenContainer.addView(tvSpeedOverlay, params);
            }

            if (tvGestureOverlay.getParent() != fullscreenContainer) {
                ((ViewGroup) tvGestureOverlay.getParent()).removeView(tvGestureOverlay);
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.gravity = android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL;
                params.topMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 32, getResources().getDisplayMetrics());
                fullscreenContainer.addView(tvGestureOverlay, params);
            }

            WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
            windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        } else {
            appBarLayout.setVisibility(View.VISIBLE);
            contentTabsAndPager.setVisibility(View.VISIBLE);
            fullscreenContainer.setVisibility(View.GONE);
            
            if (commentInputContainer != null && viewPager != null && viewPager.getCurrentItem() == 1) {
                commentInputContainer.setVisibility(View.VISIBLE);
            }

            if (playerView.getParent() != mediaContainer) {
                ((ViewGroup) playerView.getParent()).removeView(playerView);
                // --- 竖屏模式下，填满 XML 中定义的固定高度容器 ---
                mediaContainer.addView(playerView, 0, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
            
            if (tvSeekOverlay.getParent() != mediaContainer) {
                ((ViewGroup) tvSeekOverlay.getParent()).removeView(tvSeekOverlay);
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.gravity = android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL;
                params.topMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 32, getResources().getDisplayMetrics());
                mediaContainer.addView(tvSeekOverlay, params);
            }

            if (tvSpeedOverlay.getParent() != mediaContainer) {
                ((ViewGroup) tvSpeedOverlay.getParent()).removeView(tvSpeedOverlay);
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.gravity = android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL;
                params.topMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 32, getResources().getDisplayMetrics());
                mediaContainer.addView(tvSpeedOverlay, params);
            }

            if (tvGestureOverlay.getParent() != mediaContainer) {
                ((ViewGroup) tvGestureOverlay.getParent()).removeView(tvGestureOverlay);
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.gravity = android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL;
                params.topMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 32, getResources().getDisplayMetrics());
                mediaContainer.addView(tvGestureOverlay, params);
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
        
        if (initialVolume != -1) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, initialVolume, 0);
        }
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = initialBrightness;
        getWindow().setAttributes(lp);
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
