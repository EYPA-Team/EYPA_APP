package com.eypa.app.ui.message;

import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.eypa.app.R;
import com.eypa.app.model.message.NotificationItem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private List<NotificationItem> items = new ArrayList<>();
    private Set<String> expandedIds = new HashSet<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(NotificationItem item);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<NotificationItem> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public void addItems(List<NotificationItem> newItems) {
        if (newItems != null && !newItems.isEmpty()) {
            int startPosition = this.items.size();
            this.items.addAll(newItems);
            notifyItemRangeInserted(startPosition, newItems.size());
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationItem item = items.get(position);

        if (item.getSender() != null) {
            holder.tvSenderName.setText(item.getSender().getName());
            Glide.with(holder.itemView.getContext())
                    .load(item.getSender().getAvatar())
                    .placeholder(R.drawable.ic_avatar_placeholder)
                    .circleCrop()
                    .into(holder.ivAvatar);
        } else {
            holder.tvSenderName.setText("系统通知");
            holder.ivAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
        }

        if (item.getTypeText() != null && !item.getTypeText().isEmpty()) {
            holder.tvTypeText.setVisibility(View.VISIBLE);
            holder.tvTypeText.setText(item.getTypeText());
        } else {
            holder.tvTypeText.setVisibility(View.GONE);
        }

        holder.tvDate.setText(item.getDateHuman());

        if (item.getTitle() != null && !item.getTitle().isEmpty()) {
            holder.tvTitle.setVisibility(View.VISIBLE);
            holder.tvTitle.setText(item.getTitle());
        } else {
            holder.tvTitle.setVisibility(View.GONE);
        }

        if (item.getContent() != null && !item.getContent().isEmpty()) {
            holder.tvContent.setVisibility(View.VISIBLE);
            holder.tvContent.setText(Html.fromHtml(item.getContent(), Html.FROM_HTML_MODE_COMPACT));
            
            if (expandedIds.contains(item.getId())) {
                holder.tvContent.setMaxLines(Integer.MAX_VALUE);
                holder.tvContent.setEllipsize(null);
            } else {
                holder.tvContent.setMaxLines(3);
                holder.tvContent.setEllipsize(android.text.TextUtils.TruncateAt.END);
            }
        } else {
            holder.tvContent.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (expandedIds.contains(item.getId())) {
                expandedIds.remove(item.getId());
            } else {
                expandedIds.add(item.getId());
            }
            notifyItemChanged(position);
            
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvSenderName;
        TextView tvTypeText;
        TextView tvDate;
        TextView tvTitle;
        TextView tvContent;

        ViewHolder(View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            tvSenderName = itemView.findViewById(R.id.tv_sender_name);
            tvTypeText = itemView.findViewById(R.id.tv_type_text);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvContent = itemView.findViewById(R.id.tv_content);
        }
    }
}
