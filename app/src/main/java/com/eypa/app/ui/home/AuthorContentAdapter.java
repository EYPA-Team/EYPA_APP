package com.eypa.app.ui.home;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.eypa.app.R;
import com.eypa.app.model.ContentItem;
import com.eypa.app.ui.detail.DetailActivity;
import com.eypa.app.utils.TimeAgoUtils;

import java.util.List;
import java.util.Locale;

public class AuthorContentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ITEM = 0;
    private static final int TYPE_FOOTER = 1;

    private final List<ContentItem> items;
    private boolean isLoadingFooterVisible = false;

    public AuthorContentAdapter(List<ContentItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_FOOTER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_loading_footer, parent, false);
            return new FooterViewHolder(view);
        }
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ItemViewHolder) {
            ContentItem item = items.get(position);
            ItemViewHolder itemHolder = (ItemViewHolder) holder;

            itemHolder.title.setText(item.getTitle());
            itemHolder.viewCount.setText(formatCount(item.getViewCount()));
            itemHolder.likeCount.setText(formatCount(item.getLikeCount()));
            itemHolder.date.setText(TimeAgoUtils.getRelativeTime(itemHolder.itemView.getContext(), item.getDate()));

            String imageUrl = item.getBestImageUrl();
            itemHolder.coverImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
            
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(itemHolder.itemView.getContext())
                        .load(imageUrl)
                        .centerCrop()
                        .placeholder(R.drawable.placeholder_image_small)
                        .error(R.drawable.placeholder_image_small)
                        .into(itemHolder.coverImage);
            } else {
                itemHolder.coverImage.setImageResource(R.drawable.placeholder_image_small);
            }

            itemHolder.itemView.setOnClickListener(v -> {
                Context context = v.getContext();
                Intent intent = new Intent(context, DetailActivity.class);
                intent.putExtra(DetailActivity.EXTRA_POST_ID, item.getId());
                context.startActivity(intent);
            });
        }
    }

    private String formatCount(int count) {
        if (count >= 1000) {
            return String.format(Locale.getDefault(), "%.1fK", count / 1000.0);
        }
        return String.valueOf(count);
    }

    @Override
    public int getItemCount() {
        return items.size() + (isLoadingFooterVisible ? 1 : 0);
    }

    @Override
    public int getItemViewType(int position) {
        if (isLoadingFooterVisible && position == items.size()) {
            return TYPE_FOOTER;
        }
        return TYPE_ITEM;
    }

    public void setLoadingFooterVisible(boolean visible) {
        if (isLoadingFooterVisible == visible) return;
        isLoadingFooterVisible = visible;
        if (visible) {
            notifyItemInserted(items.size());
        } else {
            notifyItemRemoved(items.size());
        }
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        ImageView coverImage;
        TextView title;
        TextView viewCount;
        TextView likeCount;
        TextView date;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            coverImage = itemView.findViewById(R.id.iv_cover);
            title = itemView.findViewById(R.id.tv_title);
            viewCount = itemView.findViewById(R.id.tv_views);
            likeCount = itemView.findViewById(R.id.tv_likes);
            date = itemView.findViewById(R.id.tv_date);
        }
    }

    static class FooterViewHolder extends RecyclerView.ViewHolder {
        public FooterViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
