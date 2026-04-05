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
import com.eypa.app.model.message.ChatRecord;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_LEFT = 0;
    private static final int TYPE_RIGHT = 1;

    private List<ChatRecord> chatRecords = new ArrayList<>();
    private String targetAvatar;
    private String myAvatar;

    public void setAvatars(String targetAvatar, String myAvatar) {
        this.targetAvatar = targetAvatar;
        this.myAvatar = myAvatar;
    }

    public void setRecords(List<ChatRecord> records) {
        this.chatRecords = records;
        notifyDataSetChanged();
    }

    public void addRecords(List<ChatRecord> records) {
        int start = this.chatRecords.size();
        this.chatRecords.addAll(records);
        notifyItemRangeInserted(start, records.size());
    }

    public void addRecordToBottom(ChatRecord record) {
        this.chatRecords.add(0, record);
        notifyItemInserted(0);
    }

    @Override
    public int getItemViewType(int position) {
        ChatRecord record = chatRecords.get(position);
        return record.isMe() ? TYPE_RIGHT : TYPE_LEFT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_RIGHT) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_right, parent, false);
            return new RightViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_left, parent, false);
            return new LeftViewHolder(view);
        }
    }

    private long parseTime(String timeString) {
        if (timeString == null) return 0;
        try {
            java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
            java.util.Date date = format.parse(timeString);
            return date != null ? date.getTime() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatRecord record = chatRecords.get(position);

        boolean showTime = true;
        if (position < chatRecords.size() - 1) {
            ChatRecord older = chatRecords.get(position + 1);
            long currentMs = parseTime(record.getCreateTime());
            long olderMs = parseTime(older.getCreateTime());
            if (currentMs > 0 && olderMs > 0 && (currentMs - olderMs) <= 3 * 60 * 1000 && (currentMs - olderMs) >= 0) {
                showTime = false;
            }
        }

        if (holder instanceof RightViewHolder) {
            ((RightViewHolder) holder).bind(record, myAvatar, showTime);
        } else if (holder instanceof LeftViewHolder) {
            ((LeftViewHolder) holder).bind(record, targetAvatar, showTime);
        }
    }

    @Override
    public int getItemCount() {
        return chatRecords.size();
    }

    static class LeftViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime;
        TextView tvContent;
        ImageView ivAvatar;

        public LeftViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvContent = itemView.findViewById(R.id.tv_content);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
        }

        public void bind(ChatRecord record, String avatarUrl, boolean showTime) {
            tvContent.setText(record.getContent());
            tvTime.setText(record.getCreateTime());
            tvTime.setVisibility(showTime ? View.VISIBLE : View.GONE);

            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(avatarUrl)
                        .circleCrop()
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .into(ivAvatar);
            } else {
                ivAvatar.setImageResource(R.drawable.ic_person);
            }
        }
    }

    static class RightViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime;
        TextView tvContent;
        ImageView ivAvatar;

        public RightViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvContent = itemView.findViewById(R.id.tv_content);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
        }

        public void bind(ChatRecord record, String avatarUrl, boolean showTime) {
            tvContent.setText(record.getContent());
            tvTime.setText(record.getCreateTime());
            tvTime.setVisibility(showTime ? View.VISIBLE : View.GONE);

            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(avatarUrl)
                        .circleCrop()
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .into(ivAvatar);
            } else {
                ivAvatar.setImageResource(R.drawable.ic_person);
            }
        }
    }
}
