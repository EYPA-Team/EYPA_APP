package com.eypa.app.ui.message;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.eypa.app.R;
import com.eypa.app.model.message.MessageItem;

import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ITEM = 0;
    private static final int TYPE_FOOTER = 1;

    private List<MessageItem> messageList = new ArrayList<>();
    private OnItemClickListener listener;
    private boolean isLoadingFooterVisible = false;

    public interface OnItemClickListener {
        void onItemClick(MessageItem item);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setMessages(List<MessageItem> messages) {
        this.messageList = messages;
        notifyDataSetChanged();
    }

    public void addMessages(List<MessageItem> messages) {
        if (messages != null && !messages.isEmpty()) {
            int startPosition = this.messageList.size();
            this.messageList.addAll(messages);
            notifyItemRangeInserted(startPosition, messages.size());
        }
    }

    public void setLoadingFooterVisible(boolean visible) {
        if (this.isLoadingFooterVisible != visible) {
            this.isLoadingFooterVisible = visible;
            if (visible) {
                notifyItemInserted(messageList.size());
            } else {
                notifyItemRemoved(messageList.size());
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (isLoadingFooterVisible && position == messageList.size()) {
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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_ITEM) {
            MessageItem item = messageList.get(position);
            MessageViewHolder messageHolder = (MessageViewHolder) holder;
            messageHolder.bind(item);
            messageHolder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(item);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size() + (isLoadingFooterVisible ? 1 : 0);
    }

    static class FooterViewHolder extends RecyclerView.ViewHolder {
        public FooterViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        View vUnreadDot;
        TextView tvName;
        TextView tvTime;
        TextView tvLastMessage;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            vUnreadDot = itemView.findViewById(R.id.v_unread_dot);
            tvName = itemView.findViewById(R.id.tv_name);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvLastMessage = itemView.findViewById(R.id.tv_last_message);
        }

        public void bind(MessageItem item) {
            if (item.getTargetUser() != null) {
                tvName.setText(item.getTargetUser().getName());
                Glide.with(itemView.getContext())
                        .load(item.getTargetUser().getAvatar())
                        .circleCrop()
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .into(ivAvatar);
            }

            tvLastMessage.setText(item.getLastMessage());
            tvTime.setText(item.getDateHuman() != null ? item.getDateHuman() : item.getLastTime());

            vUnreadDot.setVisibility(item.isHasUnread() ? View.VISIBLE : View.GONE);
        }
    }
}
