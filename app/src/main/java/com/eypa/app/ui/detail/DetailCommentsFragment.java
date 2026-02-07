package com.eypa.app.ui.detail;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.eypa.app.R;
import com.eypa.app.model.Comment;
import com.eypa.app.ui.detail.model.CommentBlock;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.List;

public class DetailCommentsFragment extends Fragment {

    private DetailViewModel viewModel;
    private RecyclerView recyclerView;
    private TextView noCommentsView;
    private CommentsAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_detail_comments, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.comments_recycler_view);
        noCommentsView = view.findViewById(R.id.no_comments_view);

        adapter = new CommentsAdapter();

        // --- V V V 设置点击监听器 V V V ---
        adapter.setOnCommentActionListener(new CommentsAdapter.OnCommentActionListener() {
            @Override
            public void onLike(Comment comment) {
                Toast.makeText(getContext(), "点赞功能开发中", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onShare(Comment comment) {
                showCommentShareSheet(comment);
            }

            @Override
            public void onMore(Comment comment) {
                showCommentActionSheet(comment);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(requireActivity()).get(DetailViewModel.class);
        viewModel.getCommentBlocks().observe(getViewLifecycleOwner(), this::updateComments);
    }

    private void showCommentActionSheet(Comment comment) {
        if (getContext() == null) return;

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext());
        View sheetView = LayoutInflater.from(getContext()).inflate(R.layout.layout_comment_actions_sheet, null);

        sheetView.findViewById(R.id.action_copy).setOnClickListener(v -> {
            copyToClipboard(comment.getContent().getRendered()); // 注意：这里可能包含HTML标签，建议先清除
            bottomSheetDialog.dismiss();
        });

        sheetView.findViewById(R.id.action_report).setOnClickListener(v -> {
            Toast.makeText(getContext(), "已收到举报", Toast.LENGTH_SHORT).show();
            bottomSheetDialog.dismiss();
        });

        sheetView.findViewById(R.id.action_cancel).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.setContentView(sheetView);
        bottomSheetDialog.show();
    }

    private void showCommentShareSheet(Comment comment) {
        if (getContext() == null) return;

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext());
        View sheetView = LayoutInflater.from(getContext()).inflate(R.layout.layout_comment_share_sheet, null);

        sheetView.findViewById(R.id.action_copy_link).setOnClickListener(v -> {
            if (viewModel.getPostData().getValue() != null) {
                int articleId = viewModel.getPostData().getValue().getId();
                String link = "https://eqmemory.cn/" + articleId + ".html#comment-" + comment.getId();
                copyToClipboard(link);
            }
            bottomSheetDialog.dismiss();
        });

        sheetView.findViewById(R.id.action_cancel).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.setContentView(sheetView);
        bottomSheetDialog.show();
    }

    private void copyToClipboard(String text) {
        // 简单清除HTML标签
        String cleanText = android.text.Html.fromHtml(text).toString();
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Comment", cleanText);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(getContext(), "已复制到剪贴板", Toast.LENGTH_SHORT).show();
    }

    private void updateComments(List<CommentBlock> rootBlocks) {
        if (rootBlocks == null || rootBlocks.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            noCommentsView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            noCommentsView.setVisibility(View.GONE);
            adapter.submitList(rootBlocks);
        }
    }
}