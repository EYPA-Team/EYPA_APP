package com.eypa.app.ui.home;

import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import androidx.core.text.HtmlCompat;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.eypa.app.R;
import com.eypa.app.api.ApiClient;
import com.eypa.app.db.HistoryManager;
import com.eypa.app.model.ContentItem;
import com.eypa.app.model.bbs.BBSPost;
import com.eypa.app.model.bbs.BBSPostDetailRequest;
import com.eypa.app.model.bbs.BBSPostDetailResponse;
import com.eypa.app.model.user.AuthorInfoRequest;
import com.eypa.app.model.user.AuthorInfoResponse;
import com.eypa.app.model.user.FollowRequest;
import com.eypa.app.model.user.FollowResponse;
import com.eypa.app.ui.detail.DetailCommentsFragment;
import com.eypa.app.ui.detail.DetailViewModel;
import com.eypa.app.utils.ThemeUtils;
import com.eypa.app.utils.UserManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.eypa.app.utils.ReportDialogUtils;
import com.eypa.app.model.user.UserProfile;

public class BBSPostDetailActivity extends AppCompatActivity {

    public static final String EXTRA_POST_ID = "extra_post_id";
    
    private int postId;
    private TextView tvToolbarTitle;
    private View loadingView;
    private BBSPost mCurrentPost;

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private View commentInputContainer;
    private EditText editComment;
    private Button btnSend;
    private DetailViewModel viewModel;

    // 初始加载状态
    private boolean isPostLoaded = false;
    private boolean isCommentsLoaded = false;
    private boolean isFollowStatusChecked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applyTheme(this);
        int themeId = getSharedPreferences("AppSettings", MODE_PRIVATE).getInt("ThemeId", R.style.Theme_EYPA_APP);
        setTheme(themeId);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bbs_post_detail);

        postId = getIntent().getIntExtra(EXTRA_POST_ID, -1);
        if (postId == -1) {
            finish();
            return;
        }

        initViews();
        loadDetail();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }

        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        int color = (currentNightMode == Configuration.UI_MODE_NIGHT_YES) ? Color.WHITE : Color.BLACK;

        Drawable upArrow = ContextCompat.getDrawable(this, androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        if (upArrow != null) {
            upArrow = DrawableCompat.wrap(upArrow);
            DrawableCompat.setTint(upArrow, color);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setHomeAsUpIndicator(upArrow);
            }
        }

        tvToolbarTitle = findViewById(R.id.tv_toolbar_title);
        loadingView = findViewById(R.id.loading_view);

        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);
        commentInputContainer = findViewById(R.id.comment_input_container);
        editComment = findViewById(R.id.edit_comment);
        btnSend = findViewById(R.id.btn_send);

        viewModel = new ViewModelProvider(this).get(DetailViewModel.class);

        initTabs();
        setupCommentInput();
        observeViewModel();
    }

    private void initTabs() {
        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                if (position == 0) {
                    return new BBSPostContentFragment();
                } else {
                    return new DetailCommentsFragment();
                }
            }

            @Override
            public int getItemCount() {
                return 2;
            }
        });

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("详情");
            } else {
                tab.setText("评论");
            }
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

    public void onContentScroll(int scrollY) {
        int threshold = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, getResources().getDisplayMetrics());
        
        if (scrollY > threshold) {
            if (tvToolbarTitle.getAlpha() == 0f) {
                tvToolbarTitle.animate().alpha(1f).setDuration(200).start();
            }
        } else {
            if (tvToolbarTitle.getAlpha() == 1f) {
                tvToolbarTitle.animate().alpha(0f).setDuration(200).start();
            }
        }
    }

    private void setupCommentInput() {
        btnSend.setOnClickListener(v -> {
            String content = editComment.getText().toString().trim();
            if (content.isEmpty()) {
                Toast.makeText(this, "请输入评论内容", Toast.LENGTH_SHORT).show();
                return;
            }

            if (viewModel.getEditComment().getValue() != null) {
                viewModel.editComment(viewModel.getEditComment().getValue().getId(), content);
            } else {
                int parentId = -1;
                if (viewModel.getReplyToComment().getValue() != null) {
                    parentId = viewModel.getReplyToComment().getValue().getId();
                }
                viewModel.submitComment(postId, content, parentId);
            }
        });
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(this, isLoading -> {
            if (!isLoading) {
                isCommentsLoaded = true;
                checkLocalLoadingState();
            }
        });

        viewModel.getTotalCommentCount().observe(this, count -> {
            TabLayout.Tab tab = tabLayout.getTabAt(1);
            if (tab != null) {
                tab.setText(count > 0 ? "评论 " + count : "评论");
            }
        });

        viewModel.getNavigateToLogin().observe(this, shouldNavigate -> {
            if (shouldNavigate) {
                startActivity(new android.content.Intent(this, LoginActivity.class));
                viewModel.onLoginNavigationHandled();
            }
        });

        viewModel.getCommentItemAdded().observe(this, block -> {
            if (block != null && editComment.getText().length() > 0) {
                editComment.setText("");
                editComment.setHint("说点什么吧...");
                viewModel.setReplyToComment(null);
                
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(editComment.getWindowToken(), 0);
            }
        });

        viewModel.getCommentItemUpdated().observe(this, commentId -> {
            if (viewModel.getEditComment().getValue() != null && 
                viewModel.getEditComment().getValue().getId() == commentId) {
                
                viewModel.setEditComment(null);
                editComment.setText("");
                editComment.setHint("说点什么吧...");
                
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(editComment.getWindowToken(), 0);
            }
        });

        viewModel.getReplyToComment().observe(this, comment -> {
            if (comment != null) {
                if (viewModel.getEditComment().getValue() != null) {
                    viewModel.setEditComment(null);
                }
                editComment.setHint("回复 " + comment.getAuthorName() + ":");
                editComment.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(editComment, InputMethodManager.SHOW_IMPLICIT);
                
                TabLayout.Tab tab = tabLayout.getTabAt(1);
                if (tab != null && !tab.isSelected()) {
                    tab.select();
                }
            } else {
                if (viewModel.getEditComment().getValue() == null) {
                    editComment.setHint("说点什么吧...");
                }
            }
        });

        viewModel.getEditComment().observe(this, comment -> {
            if (comment != null) {
                if (viewModel.getReplyToComment().getValue() != null) {
                    viewModel.setReplyToComment(null);
                }
                String content = "";
                if (comment.getContent() != null) {
                    if (comment.getContent().getRaw() != null) {
                        content = comment.getContent().getRaw();
                    } else if (comment.getContent().getRendered() != null) {
                        content = HtmlCompat.fromHtml(comment.getContent().getRendered(), HtmlCompat.FROM_HTML_MODE_LEGACY).toString().trim();
                    }
                }
                editComment.setText(content);
                editComment.setSelection(editComment.getText().length());
                editComment.setHint("编辑评论...");
                
                editComment.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(editComment, InputMethodManager.SHOW_IMPLICIT);
                
                TabLayout.Tab tab = tabLayout.getTabAt(1);
                if (tab != null && !tab.isSelected()) {
                    tab.select();
                }
            } else {
                if (viewModel.getReplyToComment().getValue() == null) {
                    editComment.setHint("说点什么吧...");
                }
            }
        });
    }

    private void loadDetail() {
        loadingView.setVisibility(View.VISIBLE);
        loadingView.setAlpha(1f);
        
        isPostLoaded = false;
        isCommentsLoaded = false;
        isFollowStatusChecked = false;

        String token = ""; 
        if (UserManager.getInstance(this).isLoggedIn().getValue()) {
            token = UserManager.getInstance(this).getToken();
        }

        BBSPostDetailRequest request = new BBSPostDetailRequest(postId, token);
        ApiClient.getApiService().getBBSPostDetail(request).enqueue(new Callback<BBSPostDetailResponse>() {
            @Override
            public void onResponse(@NonNull Call<BBSPostDetailResponse> call, @NonNull Response<BBSPostDetailResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    isPostLoaded = true;
                    displayPost(response.body().getData());
                    checkLocalLoadingState();
                } else {
                    hideLoadingView();
                    Toast.makeText(BBSPostDetailActivity.this, "加载失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<BBSPostDetailResponse> call, @NonNull Throwable t) {
                hideLoadingView();
                Toast.makeText(BBSPostDetailActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkLocalLoadingState() {
        if (isPostLoaded && isCommentsLoaded && isFollowStatusChecked) {
            hideLoadingView();
        }
    }

    private void hideLoadingView() {
        loadingView.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction(() -> loadingView.setVisibility(View.GONE))
                .start();
    }

    private void displayPost(BBSPost post) {
        if (post == null) return;
        mCurrentPost = post;

        ContentItem historyItem = new ContentItem();
        historyItem.setId(post.getId());
        historyItem.setTitle(post.getTitle());
        historyItem.setDate(post.getDate());
        if (post.getStats() != null) {
            historyItem.setViewCount(post.getStats().views);
            historyItem.setLikeCount(post.getStats().replies);
        }
        if (post.getMedia() != null && post.getMedia().coverImage != null) {
            historyItem.setCoverImage(post.getMedia().coverImage);
        }
        historyItem.setType(1);
        HistoryManager.getInstance(this).addHistory(historyItem);
        
        ContentItem contentItem = new ContentItem();
        contentItem.setId(post.getId());
        contentItem.setTitle(post.getTitle());
        contentItem.setDate(post.getDate());
        
        viewModel.setPostData(contentItem);
        viewModel.setBBSPostData(post);
        viewModel.refreshComments(postId);

        if (tvToolbarTitle != null) {
            tvToolbarTitle.setText(post.getTitle());
        }

        if (post.getAuthorInfo() != null) {
            boolean isMe = false;
            UserProfile userProfile = UserManager.getInstance(this).getUserProfile().getValue();
            if (userProfile != null && userProfile.getId() != null) {
                try {
                    int currentUserId = Integer.parseInt(userProfile.getId());
                    int authorId = Integer.parseInt(post.getAuthorInfo().id);
                    if (currentUserId == authorId) {
                        isMe = true;
                    }
                } catch (NumberFormatException e) {
                    // 不做处理
                }
            }

            if (!isMe && Boolean.TRUE.equals(UserManager.getInstance(this).isLoggedIn().getValue())) {
                try {
                    checkFollowStatus(Integer.parseInt(post.getAuthorInfo().id));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    isFollowStatusChecked = true;
                    checkLocalLoadingState();
                }
            } else {
                isFollowStatusChecked = true;
                checkLocalLoadingState();
            }
        } else {
            isFollowStatusChecked = true;
            checkLocalLoadingState();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.author_profile_menu, menu);
        MenuItem moreItem = menu.findItem(R.id.action_more);
        if (moreItem != null && moreItem.getIcon() != null) {
            int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            int color = (currentNightMode == Configuration.UI_MODE_NIGHT_YES) ? Color.WHITE : Color.BLACK;
            moreItem.getIcon().setTint(color);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_more) {
            showActionSheet();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateFollowButtonState(boolean isFollowing) {
        if (mCurrentPost != null && mCurrentPost.getAuthorInfo() != null) {
             mCurrentPost.getAuthorInfo().isFollowing = isFollowing;
             viewModel.setBBSPostData(mCurrentPost);
        }
    }

    private void checkFollowStatus(int authorId) {
        String token = UserManager.getInstance(this).getToken();
        if (token == null) {
            isFollowStatusChecked = true;
            checkLocalLoadingState();
            return;
        }

        AuthorInfoRequest request = new AuthorInfoRequest(authorId, token);
        ApiClient.getApiService().getAuthorInfo(request).enqueue(new Callback<AuthorInfoResponse>() {
            @Override
            public void onResponse(@NonNull Call<AuthorInfoResponse> call, @NonNull Response<AuthorInfoResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getCode() == 200) {
                    if (response.body().getData() != null && response.body().getData().getInteraction() != null) {
                        boolean isFollowing = response.body().getData().getInteraction().isFollowing();
                        if (mCurrentPost != null && mCurrentPost.getAuthorInfo() != null) {
                            mCurrentPost.getAuthorInfo().isFollowing = isFollowing;
                            updateFollowButtonState(isFollowing);
                        }
                    }
                }
                isFollowStatusChecked = true;
                checkLocalLoadingState();
            }

            @Override
            public void onFailure(@NonNull Call<AuthorInfoResponse> call, @NonNull Throwable t) {
                isFollowStatusChecked = true;
                checkLocalLoadingState();
            }
        });
    }

    private void followUser(BBSPost post) {
        if (post.getAuthorInfo() == null) return;

        try {
            int authorId = Integer.parseInt(post.getAuthorInfo().id);
            String token = UserManager.getInstance(this).getToken();

            FollowRequest request = new FollowRequest(token, authorId);
            ApiClient.getApiService().followUser(request).enqueue(new Callback<FollowResponse>() {
                @Override
                public void onResponse(@NonNull Call<FollowResponse> call, @NonNull Response<FollowResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        if (response.body().getCode() == 200) {
                            boolean newStatus = !post.getAuthorInfo().isFollowing;
                            post.getAuthorInfo().isFollowing = newStatus;
                            updateFollowButtonState(newStatus);
                        }
                    } else {
                        Toast.makeText(BBSPostDetailActivity.this, "操作失败", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<FollowResponse> call, @NonNull Throwable t) {
                    Toast.makeText(BBSPostDetailActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
                }
            });

        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    private void showActionSheet() {
        if (mCurrentPost == null) return;

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.layout_bbs_post_actions_sheet, null);

        View btnReport = sheetView.findViewById(R.id.action_report);
        View dividerReport = sheetView.findViewById(R.id.divider_report);

        UserProfile currentUser = UserManager.getInstance(this).getUserProfile().getValue();
        if (currentUser != null && mCurrentPost.getAuthorInfo() != null) {
            try {
                int currentUserId = Integer.parseInt(currentUser.getId());
                int authorId = Integer.parseInt(mCurrentPost.getAuthorInfo().id);
                if (currentUserId == authorId) {
                    btnReport.setVisibility(View.GONE);
                    if (dividerReport != null) {
                        dividerReport.setVisibility(View.GONE);
                    }
                }
            } catch (NumberFormatException e) {
                // 不做处理
            }
        }

        sheetView.findViewById(R.id.action_copy_link).setOnClickListener(v -> {
            String link = "https://eqmemory.cn/forum-post/" + postId + ".html";
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Link", link);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "链接已复制", Toast.LENGTH_SHORT).show();
            bottomSheetDialog.dismiss();
        });

        btnReport.setOnClickListener(v -> {
            if (!Boolean.TRUE.equals(UserManager.getInstance(this).isLoggedIn().getValue())) {
                bottomSheetDialog.dismiss();
                startActivity(new android.content.Intent(this, LoginActivity.class));
                return;
            }

            if (mCurrentPost.getAuthorInfo() != null) {
                try {
                    int authorId = Integer.parseInt(mCurrentPost.getAuthorInfo().id);
                    String url = "https://eqmemory.cn/forum-post/" + postId + ".html";
                    bottomSheetDialog.dismiss();
                    ReportDialogUtils.showReportDialog(this, authorId, url);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "无法获取作者信息", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "无法获取作者信息", Toast.LENGTH_SHORT).show();
            }
        });

        sheetView.findViewById(R.id.action_cancel).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.setContentView(sheetView);
        bottomSheetDialog.show();
    }
}
