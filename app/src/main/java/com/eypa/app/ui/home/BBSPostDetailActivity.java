package com.eypa.app.ui.home;

import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
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
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.ViewModelProvider;

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
import com.eypa.app.ui.detail.ImageViewerFragment;
import com.eypa.app.utils.HtmlUtils;
import com.eypa.app.utils.ThemeUtils;
import com.eypa.app.utils.UserManager;
import com.eypa.app.utils.VerticalImageSpan;

import android.graphics.Canvas;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.BitmapDrawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.text.util.Linkify;
import androidx.core.text.HtmlCompat;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import androidx.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.inputmethod.InputMethodManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.tabs.TabLayout;
import com.eypa.app.utils.ReportDialogUtils;
import com.eypa.app.model.user.UserProfile;

public class BBSPostDetailActivity extends AppCompatActivity {

    public static final String EXTRA_POST_ID = "extra_post_id";
    
    private int postId;
    private TextView tvTitle, tvContent, tvAuthorName, tvPlate, tvToolbarTitle;
    private ImageView ivAvatar, ivCover;
    private NestedScrollView nsvContent;
    private View loadingView;
    private View layoutLoginRequired;
    private View btnLogin;
    private Button btnFollow;
    private BBSPost mCurrentPost;

    private TabLayout tabLayout;
    private View commentContainer;
    private View commentInputContainer;
    private EditText editComment;
    private Button btnSend;
    private DetailViewModel viewModel;
    private boolean isCommentsFragmentAdded = false;

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

        tvTitle = findViewById(R.id.tv_title);
        tvToolbarTitle = findViewById(R.id.tv_toolbar_title);
        tvContent = findViewById(R.id.tv_content);
        tvAuthorName = findViewById(R.id.tv_author_name);
        tvPlate = findViewById(R.id.tv_plate);
        ivAvatar = findViewById(R.id.iv_avatar);
        ivCover = findViewById(R.id.iv_cover);
        nsvContent = findViewById(R.id.nsv_content);
        loadingView = findViewById(R.id.loading_view);
        layoutLoginRequired = findViewById(R.id.layout_login_required);
        btnLogin = findViewById(R.id.btn_login);
        btnFollow = findViewById(R.id.btn_follow);

        tabLayout = findViewById(R.id.tab_layout);
        commentContainer = findViewById(R.id.comment_container);
        commentInputContainer = findViewById(R.id.comment_input_container);
        editComment = findViewById(R.id.edit_comment);
        btnSend = findViewById(R.id.btn_send);

        viewModel = new ViewModelProvider(this).get(DetailViewModel.class);

        initTabs();
        setupCommentInput();
        observeViewModel();

        if (nsvContent != null) {
            nsvContent.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
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
            });
        }
    }

    private void initTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("详情"));
        tabLayout.addTab(tabLayout.newTab().setText("评论"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    crossFade(nsvContent, commentContainer, commentInputContainer, false);
                } else {
                    crossFade(commentContainer, nsvContent, commentInputContainer, true);

                    if (!isCommentsFragmentAdded) {
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.comment_container, new DetailCommentsFragment())
                                .commit();
                        isCommentsFragmentAdded = true;
                    }
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void crossFade(View showView, View hideView, View inputView, boolean showInput) {
        int duration = 200;

        showView.setAlpha(0f);
        showView.setVisibility(View.VISIBLE);

        showView.animate()
                .alpha(1f)
                .setDuration(duration)
                .setListener(null);

        hideView.animate()
                .alpha(0f)
                .setDuration(duration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        hideView.setVisibility(View.GONE);
                    }
                });

        if (showInput) {
            inputView.setAlpha(0f);
            inputView.setVisibility(View.VISIBLE);
            inputView.animate()
                    .alpha(1f)
                    .setDuration(duration)
                    .setListener(null);
        } else {
            inputView.animate()
                    .alpha(0f)
                    .setDuration(duration)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            inputView.setVisibility(View.GONE);
                        }
                    });
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
        
        String token = ""; 
        if (UserManager.getInstance(this).isLoggedIn().getValue()) {
            token = UserManager.getInstance(this).getToken();
        }

        BBSPostDetailRequest request = new BBSPostDetailRequest(postId, token);
        ApiClient.getApiService().getBBSPostDetail(request).enqueue(new Callback<BBSPostDetailResponse>() {
            @Override
            public void onResponse(@NonNull Call<BBSPostDetailResponse> call, @NonNull Response<BBSPostDetailResponse> response) {
                loadingView.animate()
                        .alpha(0f)
                        .setDuration(300)
                        .withEndAction(() -> loadingView.setVisibility(View.GONE))
                        .start();

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    displayPost(response.body().getData());
                } else {
                    Toast.makeText(BBSPostDetailActivity.this, "加载失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<BBSPostDetailResponse> call, @NonNull Throwable t) {
                loadingView.animate()
                        .alpha(0f)
                        .setDuration(300)
                        .withEndAction(() -> loadingView.setVisibility(View.GONE))
                        .start();
                Toast.makeText(BBSPostDetailActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
            }
        });
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
        viewModel.refreshComments(postId);

        if (post.getMedia() != null && post.getMedia().coverImage != null && !post.getMedia().coverImage.isEmpty()) {
            ivCover.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(post.getMedia().coverImage)
                    .into(ivCover);
            
            ivCover.setOnClickListener(v -> {
                ImageViewerFragment.newInstance(post.getMedia().coverImage)
                        .show(getSupportFragmentManager(), "image_viewer");
            });
        } else {
            ivCover.setVisibility(View.GONE);
        }

        tvTitle.setText(post.getTitle());
        if (tvToolbarTitle != null) {
            tvToolbarTitle.setText(post.getTitle());
        }

        boolean isProtected = false;
        if (post.getContent() != null && post.getContent().isProtected) {
            isProtected = true;
        }
        if (post.getPermission() != null && !post.getPermission().canView) {
            isProtected = true;
        }

        if (isProtected) {
            tvContent.setVisibility(View.GONE);
            layoutLoginRequired.setVisibility(View.VISIBLE);
            btnLogin.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(this, LoginActivity.class);
                startActivity(intent);
            });
        } else {
            tvContent.setVisibility(View.VISIBLE);
            layoutLoginRequired.setVisibility(View.GONE);
            if (post.getContent() != null && post.getContent().rendered != null) {
                setHtmlText(tvContent, post.getContent().rendered);
                
                tvContent.setLinkTextColor(ContextCompat.getColor(this, R.color.content_link_color));
            }
        }

        if (post.getAuthorInfo() != null) {
            tvAuthorName.setText(post.getAuthorInfo().name);
            Glide.with(this)
                    .load(post.getAuthorInfo().avatar)
                    .circleCrop()
                    .into(ivAvatar);

            View.OnClickListener profileListener = v -> {
                try {
                    int authorId = Integer.parseInt(post.getAuthorInfo().id);
                    AuthorProfileActivity.start(this, authorId);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            };
            ivAvatar.setOnClickListener(profileListener);
            tvAuthorName.setOnClickListener(profileListener);

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

            if (isMe) {
                btnFollow.setVisibility(View.GONE);
            } else {
                btnFollow.setVisibility(View.VISIBLE);
                updateFollowButtonState(post.getAuthorInfo().isFollowing);
                
                if (Boolean.TRUE.equals(UserManager.getInstance(this).isLoggedIn().getValue())) {
                    try {
                        checkFollowStatus(Integer.parseInt(post.getAuthorInfo().id));
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }

                btnFollow.setOnClickListener(v -> {
                    if (!Boolean.TRUE.equals(UserManager.getInstance(this).isLoggedIn().getValue())) {
                        startActivity(new android.content.Intent(this, LoginActivity.class));
                        return;
                    }
                    followUser(post);
                });
            }
        } else {
            btnFollow.setVisibility(View.GONE);
        }

        if (post.getPlate() != null) {
            tvPlate.setText(post.getPlate().name);
            tvPlate.setVisibility(View.VISIBLE);
        } else {
            tvPlate.setVisibility(View.GONE);
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
        if (isFollowing) {
            btnFollow.setText("已关注");
            btnFollow.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        } else {
            btnFollow.setText("关注");
            TypedValue typedValue = new TypedValue();
            getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true);
            btnFollow.setTextColor(typedValue.data);
        }
    }

    private void checkFollowStatus(int authorId) {
        String token = UserManager.getInstance(this).getToken();
        if (token == null) return;

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
            }

            @Override
            public void onFailure(@NonNull Call<AuthorInfoResponse> call, @NonNull Throwable t) {
                // 不做处理
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

    private static final String SMILIE_BASE_URL = "https://eqmemory.cn/core/views/catfish/img/smilies/";

    private void setHtmlText(TextView textView, String html) {
        if (html == null) {
            textView.setText("");
            return;
        }

        String processedHtml = processCustomSmilies(html);

        Spanned spanned = HtmlCompat.fromHtml(
                processedHtml,
                HtmlCompat.FROM_HTML_MODE_LEGACY,
                new BBSGlideImageGetter(textView),
                null
        );

        SpannableStringBuilder ssb;
        if (spanned instanceof SpannableStringBuilder) {
            ssb = (SpannableStringBuilder) spanned;
        } else {
            ssb = new SpannableStringBuilder(spanned);
        }

        Linkify.addLinks(ssb, Linkify.WEB_URLS);

        ImageSpan[] imageSpans = ssb.getSpans(0, ssb.length(), ImageSpan.class);
        for (ImageSpan span : imageSpans) {
            int start = ssb.getSpanStart(span);
            int end = ssb.getSpanEnd(span);
            int flags = ssb.getSpanFlags(span);

            ssb.removeSpan(span);
            VerticalImageSpan newSpan = new VerticalImageSpan(span.getDrawable());
            ssb.setSpan(newSpan, start, end, flags);

            String source = span.getSource();
            if (source != null) {
                ssb.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View widget) {
                        ImageViewerFragment.newInstance(source)
                                .show(getSupportFragmentManager(), "image_viewer");
                    }
                }, start, end, flags);
            }
        }

        textView.setText(ssb);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setClickable(true);
    }

    private String processCustomSmilies(String input) {
        Pattern pattern = Pattern.compile("\\[g=(.*?)\\]");
        Matcher matcher = pattern.matcher(input);

        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String path = matcher.group(1);
            String imgTag = "<img src=\"" + SMILIE_BASE_URL + path + "\" />";
            matcher.appendReplacement(sb, imgTag);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private class BBSGlideImageGetter implements Html.ImageGetter {
        private final WeakReference<TextView> container;

        public BBSGlideImageGetter(TextView textView) {
            this.container = new WeakReference<>(textView);
        }

        @Override
        public Drawable getDrawable(String source) {
            final UrlDrawable urlDrawable = new UrlDrawable();

            TextView textView = container.get();
            if (textView != null) {
                DrawableTarget target = new DrawableTarget(urlDrawable, textView, source);
                urlDrawable.setDrawableTarget(target);
                Glide.with(textView.getContext())
                        .load(source)
                        .placeholder(R.drawable.placeholder_image)
                        .into(target);
            }

            return urlDrawable;
        }

        private class DrawableTarget extends CustomTarget<Drawable> {
            private final UrlDrawable urlDrawable;
            private final WeakReference<TextView> textViewRef;
            private final String source;

            public DrawableTarget(UrlDrawable urlDrawable, TextView textView, String source) {
                this.urlDrawable = urlDrawable;
                this.textViewRef = new WeakReference<>(textView);
                this.source = source;
            }

            @Override
            public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                TextView textView = textViewRef.get();
                if (textView == null) return;

                int width = resource.getIntrinsicWidth();
                int height = resource.getIntrinsicHeight();
                
                boolean isSmilie = source != null && source.startsWith(SMILIE_BASE_URL);
                
                if (isSmilie) {
                    float scale = 1.5f;
                    resource.setBounds(0, 0, (int)(width * scale), (int)(height * scale));
                    urlDrawable.setBounds(0, 0, (int)(width * scale), (int)(height * scale));
                } else {
                    int tvWidth = textView.getWidth() - textView.getPaddingLeft() - textView.getPaddingRight();
                    if (tvWidth <= 0) {
                        tvWidth = textView.getResources().getDisplayMetrics().widthPixels - textView.getPaddingLeft() - textView.getPaddingRight();
                    }
                    
                    if (tvWidth > 0 && width > tvWidth) {
                        float scale = (float) tvWidth / width;
                        resource.setBounds(0, 0, tvWidth, (int)(height * scale));
                        urlDrawable.setBounds(0, 0, tvWidth, (int)(height * scale));
                    } else {
                        resource.setBounds(0, 0, width, height);
                        urlDrawable.setBounds(0, 0, width, height);
                    }
                }

                urlDrawable.setDrawable(resource);

                if (resource instanceof Animatable) {
                    resource.setCallback(new Drawable.Callback() {
                        @Override
                        public void invalidateDrawable(@NonNull Drawable who) {
                            textView.invalidate();
                        }

                        @Override
                        public void scheduleDrawable(@NonNull Drawable who, @NonNull Runnable what, long when) {
                            textView.postDelayed(what, when - System.currentTimeMillis());
                        }

                        @Override
                        public void unscheduleDrawable(@NonNull Drawable who, @NonNull Runnable what) {
                            textView.removeCallbacks(what);
                        }
                    });
                    ((Animatable) resource).start();
                }

                textView.setText(textView.getText());
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {
            }

            @Override
            public void onLoadStarted(@Nullable Drawable placeholder) {
                if (placeholder != null) {
                    TextView textView = textViewRef.get();
                    if (textView != null) {
                        int width = placeholder.getIntrinsicWidth();
                        int height = placeholder.getIntrinsicHeight();
                        
                        int tvWidth = textView.getWidth() - textView.getPaddingLeft() - textView.getPaddingRight();
                        if (tvWidth <= 0) {
                            tvWidth = textView.getResources().getDisplayMetrics().widthPixels - textView.getPaddingLeft() - textView.getPaddingRight();
                        }
                        
                        if (tvWidth > 0 && width > tvWidth) {
                            float scale = (float) tvWidth / width;
                            placeholder.setBounds(0, 0, tvWidth, (int)(height * scale));
                            urlDrawable.setBounds(0, 0, tvWidth, (int)(height * scale));
                        } else {
                            placeholder.setBounds(0, 0, width, height);
                            urlDrawable.setBounds(0, 0, width, height);
                        }

                        urlDrawable.setDrawable(placeholder);
                        textView.setText(textView.getText());
                    }
                }
            }
        }
    }

    private static class UrlDrawable extends BitmapDrawable {
        private Drawable drawable;
        private Object drawableTarget;

        @SuppressWarnings("deprecation")
        public UrlDrawable() {
            super();
        }

        public void setDrawable(Drawable drawable) {
            this.drawable = drawable;
        }

        public void setDrawableTarget(Object target) {
            this.drawableTarget = target;
        }

        @Override
        public void draw(Canvas canvas) {
            if (drawable != null) {
                drawable.draw(canvas);
            }
        }
    }
}
