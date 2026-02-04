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

import com.bumptech.glide.Glide;
import com.eypa.app.R;
import com.eypa.app.model.ContentItem;
import com.eypa.app.ui.detail.DetailActivity;
import com.eypa.app.utils.TimeAgoUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PostsAdapter extends RecyclerView.Adapter<PostsAdapter.PostViewHolder> {

    private final List<ContentItem> postList;
    private final Map<Integer, String> categoryMap;

    public PostsAdapter(List<ContentItem> postList, Map<Integer, String> categoryMap) {
        this.postList = postList;
        this.categoryMap = categoryMap;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_post_card, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        ContentItem post = postList.get(position);

        String imageUrl = post.getBestImageUrl();
        if (imageUrl != null) {
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .into(holder.coverImage);
        } else {
            holder.coverImage.setImageResource(R.drawable.placeholder_image);
        }

        holder.title.setText(post.getTitle());
        holder.date.setText(TimeAgoUtils.getRelativeTime(holder.itemView.getContext(), post.getDate()));
        holder.viewCount.setText(formatCount(post.getViewCount()));
        holder.likeCount.setText(formatCount(post.getLikeCount()));

        if (post.getCategories() != null && !post.getCategories().isEmpty()) {
            int categoryId = post.getCategories().get(0);
            String categoryName = categoryMap.get(categoryId);
            if (categoryName != null) {
                holder.category.setText(categoryName);
            } else {
                holder.category.setText("分类: " + categoryId);
            }
        } else {
            holder.category.setText("");
        }

        TypedValue typedValue = new TypedValue();
        holder.itemView.getContext().getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true);
        int colorPrimary = typedValue.data;
        
        Drawable background = holder.category.getBackground();
        if (background != null) {
            background = DrawableCompat.wrap(background.mutate());
            int alphaColor = ColorUtils.setAlphaComponent(colorPrimary, 204);
            DrawableCompat.setTint(background, alphaColor);
            holder.category.setBackground(background);
        }

        // 添加点击事件 - 使用公开的 EXTRA_POST_ID
        holder.itemView.setOnClickListener(v -> {
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
        return postList.size();
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
}
