package com.eypa.app.ui.detail;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.eypa.app.R;
import com.eypa.app.model.Comment;
import com.eypa.app.ui.detail.model.CommentBlock;
import com.eypa.app.utils.HtmlUtils;
import com.eypa.app.utils.TimeAgoUtils;

import java.util.ArrayList;
import java.util.List;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentViewHolder> {

    private final List<CommentBlock> displayedComments = new ArrayList<>();
    private OnCommentActionListener actionListener;

    public interface OnCommentActionListener {
        void onLike(Comment comment);
        void onShare(Comment comment);
        void onMore(Comment comment);
    }

    public void setOnCommentActionListener(OnCommentActionListener listener) {
        this.actionListener = listener;
    }

    public void submitList(List<CommentBlock> rootComments) {
        displayedComments.clear();
        if (rootComments != null) {
            displayedComments.addAll(rootComments);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_content_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        // 这里获取 block 只是为了绑定数据显示，点击事件里不能直接用这个 position
        CommentBlock block = displayedComments.get(position);

        holder.bind(block, actionListener);

        // --- V V V 关键修复：解决位置错乱 Bug V V V ---
        holder.contentContainer.setOnClickListener(v -> {
            // 获取当前点击时的实时位置，而不是绑定时的旧位置
            int currentPosition = holder.getBindingAdapterPosition();

            // 确保位置有效（防止在动画过程中点击导致的崩溃）
            if (currentPosition != RecyclerView.NO_POSITION && currentPosition < displayedComments.size()) {
                // 获取当前位置对应的最新数据对象
                CommentBlock currentBlock = displayedComments.get(currentPosition);
                handleCommentClick(currentBlock, currentPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return displayedComments.size();
    }

    private void handleCommentClick(CommentBlock clickedBlock, int position) {
        Comment comment = clickedBlock.getComment();
        if (comment.getChildren() == null || comment.getChildren().isEmpty()) {
            return;
        }

        boolean isNowExpanded = !clickedBlock.isExpanded();
        clickedBlock.setExpanded(isNowExpanded);

        if (isNowExpanded) {
            List<CommentBlock> repliesToAdd = new ArrayList<>();
            addRepliesRecursively(comment.getChildren(), clickedBlock.getDepth() + 1, repliesToAdd);

            if (!repliesToAdd.isEmpty()) {
                // 因为 position 是实时的，所以这里 +1 一定是该条目的正下方
                displayedComments.addAll(position + 1, repliesToAdd);
                notifyItemRangeInserted(position + 1, repliesToAdd.size());
            }
        } else {
            int repliesToRemoveCount = countAllChildren(comment);
            if (repliesToRemoveCount > 0) {
                for (int i = 0; i < repliesToRemoveCount; i++) {
                    displayedComments.remove(position + 1);
                }
                notifyItemRangeRemoved(position + 1, repliesToRemoveCount);
            }
        }
        // 刷新点击的这一项（更新展开/收起文字）
        notifyItemChanged(position);
    }

    private void addRepliesRecursively(List<Comment> children, int depth, List<CommentBlock> targetList) {
        for (Comment child : children) {
            CommentBlock childBlock = new CommentBlock(child, depth);
            targetList.add(childBlock);
            if (childBlock.isExpanded() && !child.getChildren().isEmpty()) {
                addRepliesRecursively(child.getChildren(), depth + 1, targetList);
            }
        }
    }

    private int countAllChildren(Comment parent) {
        int count = parent.getChildren().size();
        for (Comment child : parent.getChildren()) {
            if (findBlockForComment(child).isExpanded()) {
                count += countAllChildren(child);
            }
        }
        return count;
    }

    private CommentBlock findBlockForComment(Comment commentToFind) {
        for (CommentBlock block : displayedComments) {
            if (block.getComment().getId() == commentToFind.getId()) {
                return block;
            }
        }
        return new CommentBlock(commentToFind, 0);
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        LinearLayout contentContainer;
        ImageView avatarView;
        TextView authorView, dateView, contentView, expandRepliesView;
        View likeContainer, shareBtn, moreBtn;

        CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            contentContainer = itemView.findViewById(R.id.comment_container);
            avatarView = itemView.findViewById(R.id.comment_avatar);
            authorView = itemView.findViewById(R.id.comment_author);
            dateView = itemView.findViewById(R.id.comment_date);
            contentView = itemView.findViewById(R.id.comment_content);
            expandRepliesView = itemView.findViewById(R.id.expand_replies_view);

            likeContainer = itemView.findViewById(R.id.action_like_container);
            shareBtn = itemView.findViewById(R.id.action_share_btn);
            moreBtn = itemView.findViewById(R.id.action_more_btn);
        }

        void bind(CommentBlock block, OnCommentActionListener listener) {
            Comment comment = block.getComment();
            Context context = itemView.getContext();

            authorView.setText(comment.getAuthorName());
            dateView.setText(TimeAgoUtils.getRelativeTime(context, comment.getDate()));

            if (comment.getContent() != null && comment.getContent().getRendered() != null) {
                HtmlUtils.setHtmlText(contentView, comment.getContent().getRendered());
            } else {
                contentView.setText("");
            }

            Glide.with(context).load(comment.getAvatarUrl()).circleCrop()
                    .placeholder(R.drawable.ic_person).error(R.drawable.ic_person).into(avatarView);

            // 缩进逻辑
            int depth = block.getDepth();
            float density = context.getResources().getDisplayMetrics().density;
            int leftIndent = (int) (depth * 24 * density);
            int originalPaddingStart = (int) (12 * density);
            int originalPaddingEnd = (int) (12 * density);
            int originalPaddingTop = (int) (8 * density);
            int originalPaddingBottom = (int) (8 * density);

            contentContainer.setPadding(originalPaddingStart + leftIndent,
                    originalPaddingTop,
                    originalPaddingEnd,
                    originalPaddingBottom);

            if (comment.getChildren() != null && !comment.getChildren().isEmpty()) {
                expandRepliesView.setVisibility(View.VISIBLE);
                if (block.isExpanded()) {
                    expandRepliesView.setText("— 收起回复");
                } else {
                    expandRepliesView.setText(String.format("— 展开%d条回复", comment.getChildren().size()));
                }
            } else {
                expandRepliesView.setVisibility(View.GONE);
            }

            if (listener != null) {
                likeContainer.setOnClickListener(v -> listener.onLike(comment));
                shareBtn.setOnClickListener(v -> listener.onShare(comment));
                moreBtn.setOnClickListener(v -> listener.onMore(comment));
            }
        }
    }
}