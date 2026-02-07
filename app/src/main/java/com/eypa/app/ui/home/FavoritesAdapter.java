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

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.FavoritesViewHolder> {

    private final List<ContentItem> favoritesList;

    public FavoritesAdapter(List<ContentItem> favoritesList) {
        this.favoritesList = favoritesList;
    }

    @NonNull
    @Override
    public FavoritesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new FavoritesViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoritesViewHolder holder, int position) {
        ContentItem item = favoritesList.get(position);

        holder.title.setText(item.getTitle());
        holder.viewCount.setText(formatCount(item.getViewCount()));
        holder.likeCount.setText(formatCount(item.getLikeCount()));
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
            Intent intent = new Intent(context, DetailActivity.class);
            intent.putExtra(DetailActivity.EXTRA_POST_ID, item.getId());
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
        return favoritesList.size();
    }

    static class FavoritesViewHolder extends RecyclerView.ViewHolder {
        ImageView coverImage;
        TextView title;
        TextView viewCount;
        TextView likeCount;
        TextView date;

        public FavoritesViewHolder(@NonNull View itemView) {
            super(itemView);
            coverImage = itemView.findViewById(R.id.iv_cover);
            title = itemView.findViewById(R.id.tv_title);
            viewCount = itemView.findViewById(R.id.tv_views);
            likeCount = itemView.findViewById(R.id.tv_likes);
            date = itemView.findViewById(R.id.tv_date);
        }
    }
}
