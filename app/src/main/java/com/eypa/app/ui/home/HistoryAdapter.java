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
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.eypa.app.R;
import com.eypa.app.model.ContentItem;
import com.eypa.app.ui.detail.DetailActivity;
import com.eypa.app.utils.TimeAgoUtils;

import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private final List<ContentItem> historyList;

    public HistoryAdapter(List<ContentItem> historyList) {
        this.historyList = historyList;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        ContentItem item = historyList.get(position);

        holder.title.setText(item.getTitle());
        holder.viewCount.setText(formatCount(item.getViewCount()));
        
        if (item.getType() == 1) {
            holder.likeCount.setText(formatCount(item.getLikeCount()));
            holder.likeIcon.setImageResource(R.drawable.ic_comment_outline);
        } else {
            holder.likeCount.setText(formatCount(item.getLikeCount()));
            holder.likeIcon.setImageResource(R.drawable.ic_thumb_up_outline);
        }
        
        holder.date.setText(TimeAgoUtils.getRelativeTime(holder.itemView.getContext(), item.getDate()));

        String imageUrl = item.getBestImageUrl();
        holder.coverImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.placeholder_image_small)
                    .error(R.drawable.placeholder_image_small)
                    .into(holder.coverImage);
        } else {
            holder.coverImage.setImageResource(R.drawable.placeholder_image_small);
        }

        holder.itemView.setOnClickListener(v -> {
            Context context = v.getContext();
            if (item.getType() == 1) {
                Intent intent = new Intent(context, BBSPostDetailActivity.class);
                intent.putExtra(BBSPostDetailActivity.EXTRA_POST_ID, item.getId());
                context.startActivity(intent);
            } else {
                Intent intent = new Intent(context, DetailActivity.class);
                intent.putExtra(DetailActivity.EXTRA_POST_ID, item.getId());
                context.startActivity(intent);
            }
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
        return historyList.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        ImageView coverImage;
        TextView title;
        TextView viewCount;
        TextView likeCount;
        ImageView likeIcon;
        TextView date;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            coverImage = itemView.findViewById(R.id.iv_cover);
            title = itemView.findViewById(R.id.tv_title);
            viewCount = itemView.findViewById(R.id.tv_views);
            likeCount = itemView.findViewById(R.id.tv_likes);
            likeIcon = itemView.findViewById(R.id.iv_likes_icon);
            date = itemView.findViewById(R.id.tv_date);
        }
    }
}
