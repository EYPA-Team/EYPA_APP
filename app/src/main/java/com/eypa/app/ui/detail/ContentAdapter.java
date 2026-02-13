package com.eypa.app.ui.detail;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.Spanned;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.eypa.app.R;
import com.eypa.app.api.ApiClient;
import com.eypa.app.api.ContentApiService;
import com.eypa.app.model.ContentItem;
import com.eypa.app.model.PostActionRequest;
import com.eypa.app.model.PostActionResponse;
import com.eypa.app.ui.home.LoginActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.eypa.app.ui.detail.components.CategoryTagView;
import com.eypa.app.ui.detail.components.StatsView;
import com.eypa.app.ui.detail.model.ContentBlock;
import com.eypa.app.ui.detail.model.DownloadBlock;
import com.eypa.app.ui.detail.model.HeaderBlock;
import com.eypa.app.ui.detail.model.ImageBlock;
import com.eypa.app.ui.detail.model.QuoteBlock;
import com.eypa.app.ui.detail.model.TextBlock;
import com.eypa.app.ui.home.AuthorProfileActivity;
import com.eypa.app.ui.home.TagContentActivity;
import com.eypa.app.utils.HtmlUtils;
import com.eypa.app.utils.TimeAgoUtils;
import com.eypa.app.utils.UserManager;
import com.eypa.app.model.user.UserProfile;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ContentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_AUTHOR_HEADER = 0;
    private static final int TYPE_INFO_HEADER = 1;
    private static final int TYPE_EPISODE_SELECTOR = 2;
    private static final int TYPE_TEXT = 3;
    private static final int TYPE_IMAGE = 4;
    private static final int TYPE_DOWNLOAD = 5;
    private static final int TYPE_HEADER_BLOCK = 6; // 正文标题 (H1-H6)
    private static final int TYPE_QUOTE_BLOCK = 7;  // 引用块

    private ContentItem post;
    private final List<ContentBlock> contentBlocks;
    private final Consumer<ContentItem.Episode> episodeClickListener;
    private OnAuthorFollowClickListener onAuthorFollowClickListener;

    public interface OnAuthorFollowClickListener {
        void onFollowClick(int authorId);
    }

    public void setOnAuthorFollowClickListener(OnAuthorFollowClickListener listener) {
        this.onAuthorFollowClickListener = listener;
    }

    public ContentAdapter(List<ContentBlock> contentBlocks, Consumer<ContentItem.Episode> episodeClickListener) {
        this.contentBlocks = contentBlocks;
        this.episodeClickListener = episodeClickListener;
    }

    public void setPost(ContentItem post) {
        this.post = post;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (post == null) return -1;
        if (position == 0) return TYPE_AUTHOR_HEADER;
        if (position == 1) return TYPE_INFO_HEADER;

        List<ContentItem.Episode> episodes = post.getAllEpisodes();
        boolean hasEpisodes = episodes != null && episodes.size() > 1;

        if (position == 2 && hasEpisodes) {
            return TYPE_EPISODE_SELECTOR;
        }

        int contentBlockIndex = position - (hasEpisodes ? 3 : 2);
        if (contentBlockIndex < 0 || contentBlockIndex >= contentBlocks.size()) return -1;

        ContentBlock block = contentBlocks.get(contentBlockIndex);
        if (block instanceof TextBlock) return TYPE_TEXT;
        if (block instanceof ImageBlock) return TYPE_IMAGE;
        if (block instanceof DownloadBlock) return TYPE_DOWNLOAD;
        if (block instanceof HeaderBlock) return TYPE_HEADER_BLOCK;
        if (block instanceof QuoteBlock) return TYPE_QUOTE_BLOCK;

        return -1;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case TYPE_AUTHOR_HEADER:
                return new AuthorHeaderViewHolder(inflater.inflate(R.layout.item_content_author_header, parent, false), onAuthorFollowClickListener);
            case TYPE_INFO_HEADER:
                // 这是文章顶部的元信息区域
                return new InfoHeaderViewHolder(inflater.inflate(R.layout.item_content_header, parent, false));
            case TYPE_EPISODE_SELECTOR:
                return new EpisodeSelectorViewHolder(inflater.inflate(R.layout.item_episode_selector, parent, false));
            case TYPE_TEXT:
                return new TextViewHolder((TextView) inflater.inflate(R.layout.item_content_text, parent, false));
            case TYPE_IMAGE:
                return new ImageViewHolder((ImageView) inflater.inflate(R.layout.item_content_image, parent, false));
            case TYPE_DOWNLOAD:
                return new DownloadViewHolder(inflater.inflate(R.layout.item_content_download, parent, false));
            case TYPE_HEADER_BLOCK:
                // 这是文章正文中的小标题 (H1-H6)，使用新创建的布局
                return new HeaderBlockViewHolder(inflater.inflate(R.layout.item_body_header, parent, false));
            case TYPE_QUOTE_BLOCK:
                return new QuoteBlockViewHolder(inflater.inflate(R.layout.item_content_quote, parent, false));
            default:
                return new RecyclerView.ViewHolder(new View(parent.getContext())) {};
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (post == null) return;
        boolean hasEpisodes = post.getAllEpisodes() != null && post.getAllEpisodes().size() > 1;
        int contentBlockIndex = position - (hasEpisodes ? 3 : 2);

        switch (holder.getItemViewType()) {
            case TYPE_AUTHOR_HEADER:
                ((AuthorHeaderViewHolder) holder).bind(post);
                break;
            case TYPE_INFO_HEADER:
                ((InfoHeaderViewHolder) holder).bind(post);
                break;
            case TYPE_EPISODE_SELECTOR:
                ((EpisodeSelectorViewHolder) holder).bind(post.getAllEpisodes(), episodeClickListener::accept);
                break;
            case TYPE_TEXT:
                ((TextViewHolder) holder).bind((TextBlock) contentBlocks.get(contentBlockIndex));
                break;
            case TYPE_IMAGE:
                ((ImageViewHolder) holder).bind((ImageBlock) contentBlocks.get(contentBlockIndex));
                break;
            case TYPE_DOWNLOAD:
                ((DownloadViewHolder) holder).bind((DownloadBlock) contentBlocks.get(contentBlockIndex));
                break;
            case TYPE_HEADER_BLOCK:
                ((HeaderBlockViewHolder) holder).bind((HeaderBlock) contentBlocks.get(contentBlockIndex));
                break;
            case TYPE_QUOTE_BLOCK:
                ((QuoteBlockViewHolder) holder).bind((QuoteBlock) contentBlocks.get(contentBlockIndex));
                break;
        }
    }

    @Override
    public int getItemCount() {
        if (post == null) return 0;
        int count = 2; // AuthorHeader + InfoHeader
        if (post.getAllEpisodes() != null && post.getAllEpisodes().size() > 1) {
            count++; // EpisodeSelector
        }
        count += contentBlocks.size();
        return count;
    }

    // --- ViewHolders ---

    static class AuthorHeaderViewHolder extends RecyclerView.ViewHolder {
        ImageView avatarView;
        TextView nameView;
        Button followButton;
        OnAuthorFollowClickListener listener;

        AuthorHeaderViewHolder(@NonNull View itemView, OnAuthorFollowClickListener listener) {
            super(itemView);
            this.listener = listener;
            avatarView = itemView.findViewById(R.id.author_avatar);
            nameView = itemView.findViewById(R.id.author_name);
            followButton = itemView.findViewById(R.id.follow_button);
        }

        void bind(ContentItem post) {
            ContentItem.Author author = post.getAuthor();
            if (author != null && author.getName() != null && !author.getName().isEmpty()) {
                nameView.setText(HtmlCompat.fromHtml(author.getName(), HtmlCompat.FROM_HTML_MODE_LEGACY));
            } else {
                nameView.setText("未知作者");
            }

            if (author != null && author.getAvatarUrls() != null && author.getAvatarUrls().getMedium() != null) {
                Glide.with(itemView.getContext()).load(author.getAvatarUrls().getMedium()).circleCrop().placeholder(R.drawable.ic_person).into(avatarView);
            } else {
                avatarView.setImageResource(R.drawable.ic_person);
            }

            avatarView.setOnClickListener(v -> {
                if (author != null) {
                    AuthorProfileActivity.start(itemView.getContext(), author.getId());
                }
            });

            if (author != null) {
                boolean isMe = false;
                UserProfile userProfile = UserManager.getInstance(itemView.getContext()).getUserProfile().getValue();
                if (userProfile != null && userProfile.getId() != null) {
                    try {
                        int currentUserId = Integer.parseInt(userProfile.getId());
                        if (currentUserId == author.getId()) {
                            isMe = true;
                        }
                    } catch (NumberFormatException e) {
                        // 不做处理
                    }
                }

                if (isMe) {
                    followButton.setVisibility(View.GONE);
                } else {
                    followButton.setVisibility(View.VISIBLE);
                    if (author.isFollowing()) {
                        followButton.setText("已关注");
                        followButton.setTextColor(itemView.getResources().getColor(android.R.color.darker_gray));
                    } else {
                        followButton.setText("关注");
                        TypedValue typedValue = new TypedValue();
                        itemView.getContext().getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true);
                        followButton.setTextColor(typedValue.data);
                    }

                    followButton.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onFollowClick(author.getId());
                        }
                    });
                }
            } else {
                followButton.setVisibility(View.GONE);
            }
        }
    }

    static class InfoHeaderViewHolder extends RecyclerView.ViewHolder {
        TextView titleView, dateView, likeCountView, favoriteCountView;
        CategoryTagView categoryView, tagsView;
        StatsView statsView;
        LinearLayout shareContainer, likeContainer, favoriteContainer;
        ImageView likeIcon, favoriteIcon;

        InfoHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.title);
            dateView = itemView.findViewById(R.id.date);
            categoryView = itemView.findViewById(R.id.category_container);
            tagsView = itemView.findViewById(R.id.tags_container);
            statsView = itemView.findViewById(R.id.stats_view);
            
            likeContainer = itemView.findViewById(R.id.action_like_container);
            likeIcon = itemView.findViewById(R.id.action_like_icon);
            likeCountView = itemView.findViewById(R.id.action_like_count);
            
            favoriteContainer = itemView.findViewById(R.id.action_favorite_container);
            favoriteIcon = itemView.findViewById(R.id.action_favorite_icon);
            favoriteCountView = itemView.findViewById(R.id.action_favorite_count);
            
            shareContainer = itemView.findViewById(R.id.action_share_container);
        }

        void bind(ContentItem post) {
            titleView.setText(HtmlCompat.fromHtml(post.getTitle(), HtmlCompat.FROM_HTML_MODE_LEGACY));
            dateView.setText(TimeAgoUtils.getRelativeTime(itemView.getContext(), post.getDate()));
            categoryView.setCategories(post.getCategoriesWithNames());
            tagsView.setTags(post.getTagsWithNames());
            tagsView.setOnTagClickListener(tagName -> {
                TagContentActivity.start(itemView.getContext(), tagName);
            });
            statsView.setStats(post.getViewCount(), post.getLikeCount(), post.getCommentCount());

            if (post.getLikeCount() > 0) {
                likeCountView.setText(formatCount(post.getLikeCount()));
            } else {
                likeCountView.setText("点赞");
            }

            if (post.isLiked()) {
                TypedValue typedValue = new TypedValue();
                itemView.getContext().getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true);
                int themeColor = typedValue.data;
                likeIcon.setColorFilter(themeColor);
                likeCountView.setTextColor(themeColor);
            } else {
                likeIcon.clearColorFilter();
                TypedValue typedValue = new TypedValue();
                itemView.getContext().getTheme().resolveAttribute(android.R.attr.textColorSecondary, typedValue, true);
                likeCountView.setTextColor(typedValue.data);
            }

            if (post.getFavoriteCount() > 0) {
                favoriteCountView.setText(formatCount(post.getFavoriteCount()));
            } else {
                favoriteCountView.setText("收藏");
            }

            if (post.isFavorited()) {
                TypedValue typedValue = new TypedValue();
                itemView.getContext().getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true);
                int themeColor = typedValue.data;
                favoriteIcon.setColorFilter(themeColor);
                favoriteCountView.setTextColor(themeColor);
            } else {
                favoriteIcon.clearColorFilter();
                TypedValue typedValue = new TypedValue();
                itemView.getContext().getTheme().resolveAttribute(android.R.attr.textColorSecondary, typedValue, true);
                favoriteCountView.setTextColor(typedValue.data);
            }

            if (shareContainer != null) {
                shareContainer.setOnClickListener(v -> showShareSheet(v.getContext(), post));
            }

            if (likeContainer != null) {
                likeContainer.setOnClickListener(v -> handleLikeAction(v.getContext(), post));
            }

            if (favoriteContainer != null) {
                favoriteContainer.setOnClickListener(v -> handleFavoriteAction(v.getContext(), post));
            }
        }

        private void handleLikeAction(Context context, ContentItem post) {
            if (!UserManager.getInstance(context).isLoggedIn().getValue()) {
                context.startActivity(new Intent(context, LoginActivity.class));
                return;
            }

            String token = UserManager.getInstance(context).getToken();
            PostActionRequest request = new PostActionRequest(token, post.getId());
            ContentApiService apiService = ApiClient.getClient().create(ContentApiService.class);

            apiService.likePost(request).enqueue(new Callback<PostActionResponse>() {
                @Override
                public void onResponse(Call<PostActionResponse> call, Response<PostActionResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        PostActionResponse.Data data = response.body().getData();
                        if (data != null) {
                            post.setLiked(data.isLiked());
                            post.setLikeCount(data.getLikeCount());
                            bind(post);
                        }
                    } else {
                        Toast.makeText(context, "操作失败", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<PostActionResponse> call, Throwable t) {
                    Toast.makeText(context, "网络错误", Toast.LENGTH_SHORT).show();
                }
            });
        }

        private void handleFavoriteAction(Context context, ContentItem post) {
            if (!UserManager.getInstance(context).isLoggedIn().getValue()) {
                context.startActivity(new Intent(context, LoginActivity.class));
                return;
            }

            String token = UserManager.getInstance(context).getToken();
            PostActionRequest request = new PostActionRequest(token, post.getId());
            ContentApiService apiService = ApiClient.getClient().create(ContentApiService.class);

            apiService.favoritePost(request).enqueue(new Callback<PostActionResponse>() {
                @Override
                public void onResponse(Call<PostActionResponse> call, Response<PostActionResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        PostActionResponse.Data data = response.body().getData();
                        if (data != null) {
                            post.setFavorited(data.isFavorited());
                            post.setFavoriteCount(data.getFavoriteCount());
                            bind(post);
                        }
                    } else {
                        Toast.makeText(context, "操作失败", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<PostActionResponse> call, Throwable t) {
                    Toast.makeText(context, "网络错误", Toast.LENGTH_SHORT).show();
                }
            });
        }

        private void showShareSheet(Context context, ContentItem post) {
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
            View sheetView = LayoutInflater.from(context).inflate(R.layout.layout_share_sheet, null);
            bottomSheetDialog.setContentView(sheetView);

            TextView copyLink = sheetView.findViewById(R.id.action_copy_link);
            TextView cancel = sheetView.findViewById(R.id.action_cancel);

            copyLink.setOnClickListener(v -> {
                String link = "https://eqmemory.cn/" + post.getId() + ".html";
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Link", link);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(context, "链接已复制", Toast.LENGTH_SHORT).show();
                bottomSheetDialog.dismiss();
            });

            cancel.setOnClickListener(v -> bottomSheetDialog.dismiss());

            bottomSheetDialog.show();
        }

        private String formatCount(int count) {
            if (count >= 1000) {
                return String.format(Locale.getDefault(), "%.1fK", count / 1000.0);
            }
            return String.valueOf(count);
        }
    }

    static class EpisodeSelectorViewHolder extends RecyclerView.ViewHolder {
        RecyclerView episodesRecyclerView;
        EpisodesAdapter episodesAdapter;

        public EpisodeSelectorViewHolder(@NonNull View itemView) {
            super(itemView);
            episodesRecyclerView = itemView.findViewById(R.id.episodes_recycler_view);
        }

        public void bind(List<ContentItem.Episode> episodes, EpisodesAdapter.OnEpisodeClickListener listener) {
            if (episodesAdapter == null) {
                // 第一次绑定时创建 Adapter
                episodesAdapter = new EpisodesAdapter(new java.util.ArrayList<>(), listener);
                episodesRecyclerView.setLayoutManager(new LinearLayoutManager(itemView.getContext(), LinearLayoutManager.HORIZONTAL, false));
                episodesRecyclerView.setAdapter(episodesAdapter);

                // 处理滑动冲突
                episodesRecyclerView.setOnTouchListener((v, event) -> {
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    return false;
                });
            }

            // 更新数据
            episodesAdapter.updateData(episodes);
        }
    }

    static class TextViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        TextViewHolder(@NonNull TextView itemView) { super(itemView); this.textView = itemView; }
        void bind(TextBlock block) { HtmlUtils.setHtmlText(textView, block.getContent().toString()); }
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageViewHolder(@NonNull ImageView itemView) { super(itemView); this.imageView = itemView; }
        void bind(ImageBlock block) {
            Glide.with(itemView.getContext()).load(block.getImageUrl()).placeholder(R.drawable.placeholder_image).error(R.drawable.placeholder_image).into(imageView);
            imageView.setOnClickListener(v -> {
                Context context = itemView.getContext();
                if (context instanceof AppCompatActivity) {
                    ImageViewerFragment.newInstance(block.getImageUrl())
                            .show(((AppCompatActivity) context).getSupportFragmentManager(), "image_viewer");
                }
            });
        }
    }

    static class DownloadViewHolder extends RecyclerView.ViewHolder {
        TextView fileNameView, fileTypeView, fileSizeView;
        Button downloadButton;
        DownloadViewHolder(@NonNull View itemView) {
            super(itemView);
            fileNameView = itemView.findViewById(R.id.file_name);
            fileTypeView = itemView.findViewById(R.id.file_type);
            fileSizeView = itemView.findViewById(R.id.file_size);
            downloadButton = itemView.findViewById(R.id.download_button);
        }
        void bind(DownloadBlock block) {
            fileNameView.setText(block.getFileName());
            fileTypeView.setText(block.getFileType());
            fileSizeView.setText(block.getFileSize());
            downloadButton.setOnClickListener(v -> {
                String url = block.getDownloadUrl();
                if (url != null && !url.trim().isEmpty()) {
                    try {
                        itemView.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    } catch (Exception e) {
                        Toast.makeText(itemView.getContext(), "无法打开链接", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(itemView.getContext(), "下载链接无效", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    static class HeaderBlockViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        HeaderBlockViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.header_text);
        }
        void bind(HeaderBlock block) {
            textView.setText(block.getText());
            // 根据标题级别动态调整字体大小
            // H1(Level 1) ~ 20sp, H6(Level 6) ~ 14sp
            float size = 22 - (block.getLevel() * 1.5f);
            if (size < 14) size = 14;
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        }
    }

    static class QuoteBlockViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        QuoteBlockViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.quote_text);
        }
        void bind(QuoteBlock block) {
            HtmlUtils.setHtmlText(textView, block.getContent().toString());
        }
    }
}