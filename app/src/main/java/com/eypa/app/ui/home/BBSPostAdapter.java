package com.eypa.app.ui.home;

import android.content.Context;
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
import com.eypa.app.model.bbs.BBSPost;

import java.util.List;

public class BBSPostAdapter extends RecyclerView.Adapter<BBSPostAdapter.ViewHolder> {

    private List<BBSPost> postList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(BBSPost post);
    }

    public BBSPostAdapter(List<BBSPost> postList, OnItemClickListener listener) {
        this.postList = postList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bbs_post, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BBSPost post = postList.get(position);
        Context context = holder.itemView.getContext();

        if (post.getAuthorInfo() != null) {
            holder.tvAuthorName.setText(post.getAuthorInfo().name);
            Glide.with(context)
                    .load(post.getAuthorInfo().avatar)
                    .circleCrop()
                    .placeholder(R.drawable.ic_person)
                    .into(holder.ivAvatar);

            if (post.getAuthorInfo().level != null) {
                holder.tvLevel.setVisibility(View.VISIBLE);
                holder.tvLevel.setText(post.getAuthorInfo().level.name);
            } else {
                holder.tvLevel.setVisibility(View.GONE);
            }
        }

        if (post.getPlate() != null) {
            holder.tvPlate.setVisibility(View.VISIBLE);
            holder.tvPlate.setText(post.getPlate().name);
        } else {
            holder.tvPlate.setVisibility(View.GONE);
        }

        holder.tvTitle.setText(post.getTitle());

        if (post.getMedia() != null && post.getMedia().coverImage != null && !post.getMedia().coverImage.isEmpty()) {
            holder.ivCover.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(post.getMedia().coverImage)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(R.drawable.placeholder_image)
                    .into(holder.ivCover);
        } else {
            holder.ivCover.setVisibility(View.GONE);
        }

        if (post.getStats() != null) {
            holder.tvViews.setText(String.valueOf(post.getStats().views));
            holder.tvReplies.setText(String.valueOf(post.getStats().replies));
        }

        if (post.getDate() != null) {
            String date = post.getDate();
            if (date.contains(" ")) {
                date = date.split(" ")[0];
            }
            holder.tvDate.setText(date);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(post);
            }
        });
    }

    @Override
    public int getItemCount() {
        return postList.size();
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
}
