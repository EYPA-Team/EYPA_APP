package com.eypa.app.ui.detail;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import com.eypa.app.R;
import com.eypa.app.model.Comment;
import com.eypa.app.ui.detail.model.CommentBlock;
import com.eypa.app.ui.home.LoginActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.List;

public class DetailCommentsFragment extends Fragment {

    private DetailViewModel viewModel;
    private RecyclerView recyclerView;
    private TextView noCommentsView;
    private CommentsAdapter adapter;
    private View btnFilter;

    private String currentSortType = "date";
    private boolean currentOnlyAuthor = false;

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
        btnFilter = view.findViewById(R.id.btn_filter);

        btnFilter.setOnClickListener(v -> showFilterSheet());

        adapter = new CommentsAdapter();

        // --- V V V 设置点击监听器 V V V ---
        adapter.setOnCommentActionListener(new CommentsAdapter.OnCommentActionListener() {
            @Override
            public void onLike(Comment comment) {
                viewModel.likeComment(comment);
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

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0) {
                        if (viewModel.getPostData().getValue() != null) {
                            viewModel.loadMoreComments(viewModel.getPostData().getValue().getId());
                        }
                    }
                }
            }
        });

        viewModel = new ViewModelProvider(requireActivity()).get(DetailViewModel.class);
        viewModel.getCommentBlocks().observe(getViewLifecycleOwner(), this::updateComments);
        
        viewModel.getIsLoadMoreLoading().observe(getViewLifecycleOwner(), isLoading -> {
            adapter.setLoading(isLoading);
        });

        viewModel.getNavigateToLogin().observe(getViewLifecycleOwner(), navigate -> {
            if (navigate) {
                startActivity(new Intent(requireContext(), LoginActivity.class));
                viewModel.onLoginNavigationHandled();
            }
        });

        viewModel.getCommentItemUpdated().observe(getViewLifecycleOwner(), commentId -> {
            if (commentId != null) {
                adapter.notifyCommentChanged(commentId);
            }
        });
    }

    private void showFilterSheet() {
        if (getContext() == null) return;

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext());
        View sheetView = LayoutInflater.from(getContext()).inflate(R.layout.layout_comment_filter_sheet, null);

        RadioGroup sortGroup = sheetView.findViewById(R.id.sort_group);
        CheckBox checkOnlyAuthor = sheetView.findViewById(R.id.check_only_author);
        TextView btnApply = sheetView.findViewById(R.id.btn_apply);

        if ("like".equals(currentSortType)) {
            sortGroup.check(R.id.radio_hottest);
        } else {
            sortGroup.check(R.id.radio_latest);
        }
        checkOnlyAuthor.setChecked(currentOnlyAuthor);

        btnApply.setOnClickListener(v -> {
            int checkedId = sortGroup.getCheckedRadioButtonId();
            if (checkedId == R.id.radio_hottest) {
                currentSortType = "like";
            } else {
                currentSortType = "date";
            }
            currentOnlyAuthor = checkOnlyAuthor.isChecked();

            if (viewModel.getPostData().getValue() != null) {
                int postId = viewModel.getPostData().getValue().getId();
                viewModel.updateCommentFilter(postId, currentSortType, currentOnlyAuthor ? 1 : 0);
            }
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.setContentView(sheetView);
        bottomSheetDialog.show();
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