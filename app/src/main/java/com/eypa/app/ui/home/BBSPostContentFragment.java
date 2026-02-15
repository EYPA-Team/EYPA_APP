package com.eypa.app.ui.home;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.text.util.Linkify;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.eypa.app.R;
import com.eypa.app.api.ApiClient;
import com.eypa.app.model.bbs.BBSPost;
import com.eypa.app.model.user.FollowRequest;
import com.eypa.app.model.user.FollowResponse;
import com.eypa.app.ui.detail.DetailViewModel;
import com.eypa.app.ui.detail.ImageViewerFragment;
import com.eypa.app.ui.home.AuthorProfileActivity;
import com.eypa.app.ui.home.LoginActivity;
import com.eypa.app.utils.UserManager;
import com.eypa.app.utils.VerticalImageSpan;
import com.eypa.app.model.user.UserProfile;

import java.lang.ref.WeakReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.graphics.Canvas;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BBSPostContentFragment extends Fragment {

    private TextView tvTitle, tvContent, tvAuthorName, tvPlate;
    private ImageView ivAvatar, ivCover;
    private NestedScrollView nsvContent;
    private View layoutLoginRequired;
    private View btnLogin;
    private Button btnFollow;
    
    private DetailViewModel viewModel;
    private BBSPost mCurrentPost;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bbs_post_content, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        viewModel = new ViewModelProvider(requireActivity()).get(DetailViewModel.class);
        
        viewModel.getBBSPostData().observe(getViewLifecycleOwner(), this::displayPost);

        if (nsvContent != null) {
            nsvContent.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                if (getActivity() instanceof BBSPostDetailActivity) {
                    ((BBSPostDetailActivity) getActivity()).onContentScroll(scrollY);
                }
            });
        }
    }

    private void initViews(View view) {
        tvTitle = view.findViewById(R.id.tv_title);
        tvContent = view.findViewById(R.id.tv_content);
        tvAuthorName = view.findViewById(R.id.tv_author_name);
        tvPlate = view.findViewById(R.id.tv_plate);
        ivAvatar = view.findViewById(R.id.iv_avatar);
        ivCover = view.findViewById(R.id.iv_cover);
        nsvContent = view.findViewById(R.id.nsv_content);
        layoutLoginRequired = view.findViewById(R.id.layout_login_required);
        btnLogin = view.findViewById(R.id.btn_login);
        btnFollow = view.findViewById(R.id.btn_follow);
    }

    private void displayPost(BBSPost post) {
        if (post == null) return;
        mCurrentPost = post;

        if (post.getMedia() != null && post.getMedia().coverImage != null && !post.getMedia().coverImage.isEmpty()) {
            ivCover.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(post.getMedia().coverImage)
                    .into(ivCover);
            
            ivCover.setOnClickListener(v -> {
                ImageViewerFragment.newInstance(post.getMedia().coverImage)
                        .show(getChildFragmentManager(), "image_viewer");
            });
        } else {
            ivCover.setVisibility(View.GONE);
        }

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
                android.content.Intent intent = new android.content.Intent(requireContext(), LoginActivity.class);
                startActivity(intent);
            });
        } else {
            tvContent.setVisibility(View.VISIBLE);
            layoutLoginRequired.setVisibility(View.GONE);
            if (post.getContent() != null && post.getContent().rendered != null) {
                setHtmlText(tvContent, post.getContent().rendered);
                tvContent.setLinkTextColor(ContextCompat.getColor(requireContext(), R.color.content_link_color));
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
                    AuthorProfileActivity.start(requireContext(), authorId);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            };
            ivAvatar.setOnClickListener(profileListener);
            tvAuthorName.setOnClickListener(profileListener);

            boolean isMe = false;
            UserProfile userProfile = UserManager.getInstance(requireContext()).getUserProfile().getValue();
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

                btnFollow.setOnClickListener(v -> {
                    if (!Boolean.TRUE.equals(UserManager.getInstance(requireContext()).isLoggedIn().getValue())) {
                        startActivity(new android.content.Intent(requireContext(), LoginActivity.class));
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

    private void updateFollowButtonState(boolean isFollowing) {
        if (isFollowing) {
            btnFollow.setText("已关注");
            btnFollow.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
        } else {
            btnFollow.setText("关注");
            TypedValue typedValue = new TypedValue();
            requireContext().getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true);
            btnFollow.setTextColor(typedValue.data);
        }
    }

    private void followUser(BBSPost post) {
        if (post.getAuthorInfo() == null) return;

        try {
            int authorId = Integer.parseInt(post.getAuthorInfo().id);
            String token = UserManager.getInstance(requireContext()).getToken();

            FollowRequest request = new FollowRequest(token, authorId);
            ApiClient.getApiService().followUser(request).enqueue(new Callback<FollowResponse>() {
                @Override
                public void onResponse(@NonNull Call<FollowResponse> call, @NonNull Response<FollowResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        if (response.body().getCode() == 200) {
                            boolean newStatus = !post.getAuthorInfo().isFollowing;
                            post.getAuthorInfo().isFollowing = newStatus;
                            updateFollowButtonState(newStatus);
                            
                            viewModel.setBBSPostData(post);
                        }
                    } else {
                        Toast.makeText(requireContext(), "操作失败", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<FollowResponse> call, @NonNull Throwable t) {
                    Toast.makeText(requireContext(), "网络错误", Toast.LENGTH_SHORT).show();
                }
            });

        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
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
                                .show(getChildFragmentManager(), "image_viewer");
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
