package com.eypa.app.ui.detail;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eypa.app.api.ApiClient;
import com.eypa.app.model.Comment;
import com.eypa.app.model.CommentsRequest;
import com.eypa.app.model.CommentsResponse;
import com.eypa.app.model.ContentItem;
import com.eypa.app.ui.detail.model.CommentBlock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DetailViewModel extends ViewModel {

    private final MutableLiveData<ContentItem> postData = new MutableLiveData<>();
    private final MutableLiveData<List<CommentBlock>> commentBlocks = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(true);
    private final MutableLiveData<Integer> totalCommentCount = new MutableLiveData<>(0);

    public LiveData<ContentItem> getPostData() {
        return postData;
    }

    public LiveData<List<CommentBlock>> getCommentBlocks() {
        return commentBlocks;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<Integer> getTotalCommentCount() {
        return totalCommentCount;
    }

    public void loadPostAndComments(int postId) {
        isLoading.setValue(true);
        loadPost(postId);
        loadComments(postId);
    }

    private void loadPost(int postId) {
        // 包含 author_info 字段，确保作者信息能正确显示
        ApiClient.getApiService().getPostWithCustomFields(postId, "id,title,date,content,author,categories,tags,zib_other_data,view_count,like_count,comment_count,_embedded,author_info", "wp:featuredmedia,author,wp:term")
                .enqueue(new Callback<ContentItem>() {
                    @Override
                    public void onResponse(Call<ContentItem> call, Response<ContentItem> response) {
                        if (response.isSuccessful()) {
                            postData.setValue(response.body());
                        }
                        // 只有当两个请求都结束后才停止加载动画
                        if (commentBlocks.getValue() != null || !response.isSuccessful()) {
                            isLoading.setValue(false);
                        }
                    }
                    @Override
                    public void onFailure(Call<ContentItem> call, Throwable t) {
                        postData.setValue(null);
                        isLoading.setValue(false);
                    }
                });
    }

    private void loadComments(int postId) {
        CommentsRequest request = new CommentsRequest(postId, 1);
        ApiClient.getApiService().getComments(request).enqueue(new Callback<CommentsResponse>() {
            @Override
            public void onResponse(Call<CommentsResponse> call, Response<CommentsResponse> response) {
                List<CommentBlock> rootCommentBlocks = new ArrayList<>();
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Comment> comments = response.body().getData();
                    if (comments != null) {
                        processComments(comments);

                        for (Comment rootComment : comments) {
                            rootCommentBlocks.add(new CommentBlock(rootComment, 0));
                        }
                        totalCommentCount.setValue(calculateTotalCommentCount(comments));
                    }
                }
                commentBlocks.setValue(rootCommentBlocks);

                // 只有当两个请求都结束后才停止加载动画
                if (postData.getValue() != null || !response.isSuccessful()) {
                    isLoading.setValue(false);
                }
            }
            @Override
            public void onFailure(Call<CommentsResponse> call, Throwable t) {
                commentBlocks.setValue(new ArrayList<>());
                isLoading.setValue(false);
            }
        });
    }

    private int calculateTotalCommentCount(List<Comment> comments) {
        if (comments == null) {
            return 0;
        }
        int count = 0;
        for (Comment comment : comments) {
            count++;
            count += calculateTotalCommentCount(comment.getChildren());
        }
        return count;
    }

    private void processComments(List<Comment> comments) {
        if (comments == null || comments.isEmpty()) {
            return;
        }

        Collections.sort(comments, (c1, c2) -> {
            String d1 = c1.getDate() != null ? c1.getDate() : "";
            String d2 = c2.getDate() != null ? c2.getDate() : "";
            return d1.compareTo(d2);
        });

        for (Comment comment : comments) {
            List<Comment> allDescendants = comment.getChildren();
            if (allDescendants != null && !allDescendants.isEmpty()) {
                List<Comment> directChildren = new ArrayList<>();
                for (Comment child : allDescendants) {
                    if (child.getParentId() == comment.getId()) {
                        directChildren.add(child);
                    }
                }
                comment.setChildren(directChildren);

                processComments(directChildren);
            }
        }
    }
}