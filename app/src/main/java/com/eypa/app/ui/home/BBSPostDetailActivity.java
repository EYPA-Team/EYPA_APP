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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.bumptech.glide.Glide;
import com.eypa.app.R;
import com.eypa.app.api.ApiClient;
import com.eypa.app.model.bbs.BBSPost;
import com.eypa.app.model.bbs.BBSPostDetailRequest;
import com.eypa.app.model.bbs.BBSPostDetailResponse;
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
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.eypa.app.utils.ReportDialogUtils;
import com.eypa.app.model.user.UserProfile;

public class BBSPostDetailActivity extends AppCompatActivity {

    public static final String EXTRA_POST_ID = "extra_post_id";
    
    private int postId;
    private TextView tvTitle, tvContent, tvAuthorName, tvPlate;
    private ImageView ivAvatar;
    private View loadingView;
    private View layoutLoginRequired;
    private View btnLogin;
    private BBSPost mCurrentPost;

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
        tvContent = findViewById(R.id.tv_content);
        tvAuthorName = findViewById(R.id.tv_author_name);
        tvPlate = findViewById(R.id.tv_plate);
        ivAvatar = findViewById(R.id.iv_avatar);
        loadingView = findViewById(R.id.loading_view);
        layoutLoginRequired = findViewById(R.id.layout_login_required);
        btnLogin = findViewById(R.id.btn_login);
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

        tvTitle.setText(post.getTitle());

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

        Linkify.addLinks(ssb, Linkify.WEB_URLS);

        textView.setText(ssb);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
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
                Glide.with(textView.getContext())
                        .load(source)
                        .into(new DrawableTarget(urlDrawable, textView, source));
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
        }
    }

    private static class UrlDrawable extends BitmapDrawable {
        private Drawable drawable;

        @SuppressWarnings("deprecation")
        public UrlDrawable() {
            super();
        }

        public void setDrawable(Drawable drawable) {
            this.drawable = drawable;
        }

        @Override
        public void draw(Canvas canvas) {
            if (drawable != null) {
                drawable.draw(canvas);
            }
        }
    }
}
