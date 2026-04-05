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

public class NotificationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ITEM = 0;
    private static final int TYPE_FOOTER = 1;

    private List<NotificationItem> items = new ArrayList<>();
    private Set<String> expandedIds = new HashSet<>();
    private OnItemClickListener listener;
    private boolean isLoadingFooterVisible = false;

    public interface OnItemClickListener {
        void onItemClick(NotificationItem item);
        void onMarkReadClick(NotificationItem item);
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

    public void setLoadingFooterVisible(boolean visible) {
        if (this.isLoadingFooterVisible != visible) {
            this.isLoadingFooterVisible = visible;
            if (visible) {
                notifyItemInserted(items.size());
            } else {
                notifyItemRemoved(items.size());
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (isLoadingFooterVisible && position == items.size()) {
            return TYPE_FOOTER;
        }
        return TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_FOOTER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_loading_footer, parent, false);
            return new FooterViewHolder(view);
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_ITEM) {
            NotificationItem item = items.get(position);
            ViewHolder itemHolder = (ViewHolder) holder;

            if (item.getSender() != null) {
                itemHolder.tvSenderName.setText(item.getSender().getName());
                Glide.with(itemHolder.itemView.getContext())
                        .load(item.getSender().getAvatar())
                        .placeholder(R.drawable.ic_avatar_placeholder)
                        .circleCrop()
                        .into(itemHolder.ivAvatar);
            } else {
                itemHolder.tvSenderName.setText("系统通知");
                itemHolder.ivAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
            }

            if (item.getTypeText() != null && !item.getTypeText().isEmpty()) {
                itemHolder.tvTypeText.setVisibility(View.VISIBLE);
                itemHolder.tvTypeText.setText(item.getTypeText());
            } else {
                itemHolder.tvTypeText.setVisibility(View.GONE);
            }

            itemHolder.tvDate.setText(item.getDateHuman());

            if (item.getTitle() != null && !item.getTitle().isEmpty()) {
                itemHolder.tvTitle.setVisibility(View.VISIBLE);
                itemHolder.tvTitle.setText(item.getTitle());
            } else {
                itemHolder.tvTitle.setVisibility(View.GONE);
            }

            if (item.getContent() != null && !item.getContent().isEmpty()) {
                itemHolder.tvContent.setVisibility(View.VISIBLE);
                itemHolder.tvContent.setText(Html.fromHtml(item.getContent(), Html.FROM_HTML_MODE_COMPACT));
                
                if (expandedIds.contains(item.getId())) {
                    itemHolder.tvContent.setMaxLines(Integer.MAX_VALUE);
                    itemHolder.tvContent.setEllipsize(null);
                } else {
                    itemHolder.tvContent.setMaxLines(3);
                    itemHolder.tvContent.setEllipsize(android.text.TextUtils.TruncateAt.END);
                }
            } else {
                itemHolder.tvContent.setVisibility(View.GONE);
            }

            if (!item.isRead()) {
                itemHolder.btnMarkRead.setVisibility(View.VISIBLE);
                itemHolder.btnMarkRead.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onMarkReadClick(item);
                    }
                });
            } else {
                itemHolder.btnMarkRead.setVisibility(View.GONE);
                itemHolder.btnMarkRead.setOnClickListener(null);
            }

            itemHolder.itemView.setOnClickListener(v -> {
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
    }

    @Override
    public int getItemCount() {
        return items.size() + (isLoadingFooterVisible ? 1 : 0);
    }

    static class FooterViewHolder extends RecyclerView.ViewHolder {
        public FooterViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvSenderName;
        TextView tvTypeText;
        TextView tvDate;
        TextView tvTitle;
        TextView tvContent;
        TextView btnMarkRead;

        ViewHolder(View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            tvSenderName = itemView.findViewById(R.id.tv_sender_name);
            tvTypeText = itemView.findViewById(R.id.tv_type_text);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvContent = itemView.findViewById(R.id.tv_content);
            btnMarkRead = itemView.findViewById(R.id.btn_mark_read);
        }
    }
}
