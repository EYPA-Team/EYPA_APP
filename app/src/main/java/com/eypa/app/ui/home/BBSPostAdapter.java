package com.eypa.app.ui.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.eypa.app.R;
import com.eypa.app.model.bbs.BBSPost;

import java.util.List;

public class BBSPostAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_ITEM = 1;
    private static final int VIEW_TYPE_LOADING = 2;

    private List<BBSPost> postList;
    private OnItemClickListener listener;
    private boolean isLoadingFooterVisible = false;

    public interface OnItemClickListener {
        void onItemClick(BBSPost post);
    }

    public BBSPostAdapter(List<BBSPost> postList, OnItemClickListener listener) {
        this.postList = postList;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        if (isLoadingFooterVisible && position == getItemCount() - 1) {
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
            return new LoadingViewHolder(view);
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bbs_post, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof LoadingViewHolder) {
            return;
        }

        ViewHolder itemHolder = (ViewHolder) holder;
        BBSPost post = postList.get(position);
        Context context = itemHolder.itemView.getContext();

        if (post.getAuthorInfo() != null) {
            itemHolder.tvAuthorName.setText(post.getAuthorInfo().name);
            Glide.with(context)
                    .load(post.getAuthorInfo().avatar)
                    .circleCrop()
                    .placeholder(R.drawable.ic_person)
                    .into(itemHolder.ivAvatar);

            if (post.getAuthorInfo().level != null) {
                itemHolder.tvLevel.setVisibility(View.VISIBLE);
                itemHolder.tvLevel.setText(post.getAuthorInfo().level.name);
            } else {
                itemHolder.tvLevel.setVisibility(View.GONE);
            }
        }

        if (post.getPlate() != null) {
            itemHolder.tvPlate.setVisibility(View.VISIBLE);
            itemHolder.tvPlate.setText(post.getPlate().name);
        } else {
            itemHolder.tvPlate.setVisibility(View.GONE);
        }

        itemHolder.tvTitle.setText(post.getTitle());

        if (post.getMedia() != null && post.getMedia().coverImage != null && !post.getMedia().coverImage.isEmpty()) {
            itemHolder.ivCover.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(post.getMedia().coverImage)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(R.drawable.placeholder_image)
                    .into(itemHolder.ivCover);
        } else {
            itemHolder.ivCover.setVisibility(View.GONE);
        }

        if (post.getStats() != null) {
            itemHolder.tvViews.setText(String.valueOf(post.getStats().views));
            itemHolder.tvReplies.setText(String.valueOf(post.getStats().replies));
        }

        if (post.getDate() != null) {
            String date = post.getDate();
            if (date.contains(" ")) {
                date = date.split(" ")[0];
            }
            itemHolder.tvDate.setText(date);
        }

        itemHolder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(post);
            }
        });
    }

    @Override
    public int getItemCount() {
        return postList.size() + (isLoadingFooterVisible ? 1 : 0);
    }

    public void setLoadingFooterVisible(boolean visible) {
        if (isLoadingFooterVisible != visible) {
            isLoadingFooterVisible = visible;
            notifyDataSetChanged();
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar, ivCover;
        TextView tvAuthorName, tvLevel, tvPlate, tvTitle, tvDate, tvViews, tvReplies;

        ViewHolder(View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            tvAuthorName = itemView.findViewById(R.id.tv_author_name);
            tvLevel = itemView.findViewById(R.id.tv_level);
            tvPlate = itemView.findViewById(R.id.tv_plate);
            tvTitle = itemView.findViewById(R.id.tv_title);
            ivCover = itemView.findViewById(R.id.iv_cover);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvViews = itemView.findViewById(R.id.tv_views);
            tvReplies = itemView.findViewById(R.id.tv_replies);
        }
    }

    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        public LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
