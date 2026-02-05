package com.eypa.app.ui.home;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.CircularProgressDrawable;

import com.bumptech.glide.Glide;
import com.eypa.app.R;
import com.eypa.app.model.ContentItem;
import com.eypa.app.ui.detail.DetailActivity;
import com.eypa.app.utils.TimeAgoUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PostsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_ITEM = 0;
    private static final int VIEW_TYPE_LOADING = 1;

    private final List<ContentItem> postList;
    private final Map<Integer, String> categoryMap;
    private boolean isLoadingFooterVisible = false;

    public PostsAdapter(List<ContentItem> postList, Map<Integer, String> categoryMap) {
        this.postList = postList;
        this.categoryMap = categoryMap;
    }

    @Override
    public int getItemViewType(int position) {
        if (isLoadingFooterVisible && position == postList.size()) {
            return VIEW_TYPE_LOADING;
        }
        return VIEW_TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_LOADING) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_loading_footer, parent, false);
            androidx.recyclerview.widget.StaggeredGridLayoutManager.LayoutParams layoutParams =
                    new androidx.recyclerview.widget.StaggeredGridLayoutManager.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.setFullSpan(true);
            view.setLayoutParams(layoutParams);
            return new LoadingViewHolder(view);
        }
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_post_card, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof LoadingViewHolder) {
            return;
        }

        PostViewHolder postHolder = (PostViewHolder) holder;
        ContentItem post = postList.get(position);

        String imageUrl = post.getBestImageUrl();
        if (imageUrl != null) {
            CircularProgressDrawable circularProgressDrawable = new CircularProgressDrawable(postHolder.itemView.getContext());
            circularProgressDrawable.setStrokeWidth(5f);
            circularProgressDrawable.setCenterRadius(30f);
            circularProgressDrawable.start();

            Glide.with(postHolder.itemView.getContext())
                    .load(imageUrl)
                    .placeholder(circularProgressDrawable)
                    .error(R.drawable.placeholder_image)
                    .into(postHolder.coverImage);
        } else {
            postHolder.coverImage.setImageResource(R.drawable.placeholder_image);
        }

        postHolder.title.setText(post.getTitle());
        postHolder.date.setText(TimeAgoUtils.getRelativeTime(postHolder.itemView.getContext(), post.getDate()));
        postHolder.viewCount.setText(formatCount(post.getViewCount()));
        postHolder.likeCount.setText(formatCount(post.getLikeCount()));

        if (post.getCategories() != null && !post.getCategories().isEmpty()) {
            int categoryId = post.getCategories().get(0);
            String categoryName = categoryMap.get(categoryId);
            if (categoryName != null) {
                postHolder.category.setText(categoryName);
            } else {
                postHolder.category.setText("分类: " + categoryId);
            }
        } else {
            postHolder.category.setText("");
        }

        TypedValue typedValue = new TypedValue();
        postHolder.itemView.getContext().getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true);
        int colorPrimary = typedValue.data;
        
        Drawable background = postHolder.category.getBackground();
        if (background != null) {
            background = DrawableCompat.wrap(background.mutate());
            int alphaColor = ColorUtils.setAlphaComponent(colorPrimary, 204);
            DrawableCompat.setTint(background, alphaColor);
            postHolder.category.setBackground(background);
        }

        // 添加点击事件 - 使用公开的 EXTRA_POST_ID
        postHolder.itemView.setOnClickListener(v -> {
            Context context = v.getContext();
            Intent intent = new Intent(context, DetailActivity.class);
            intent.putExtra(DetailActivity.EXTRA_POST_ID, post.getId());
            context.startActivity(intent);
        });
    }


    private String formatCount(int count) {
        if (count >= 1000) {
            return String.format(Locale.getDefault(), "%.1fK", count / 1000.0);
        }
        return String.valueOf(count);
    }

    @Override
    public int getItemCount() {
        return postList.size() + (isLoadingFooterVisible ? 1 : 0);
    }

    public void setLoadingFooterVisible(boolean visible) {
        if (isLoadingFooterVisible != visible) {
            isLoadingFooterVisible = visible;
            if (visible) {
                notifyItemInserted(postList.size());
            } else {
                notifyItemRemoved(postList.size());
            }
        }
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView coverImage;
        TextView title;
        TextView category;
        TextView date;
        TextView viewCount;
        TextView likeCount;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            coverImage = itemView.findViewById(R.id.cover_image);
            title = itemView.findViewById(R.id.title);
            category = itemView.findViewById(R.id.category);
            date = itemView.findViewById(R.id.date);
            viewCount = itemView.findViewById(R.id.view_count);
            likeCount = itemView.findViewById(R.id.like_count);
        }
    }

    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        public LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
