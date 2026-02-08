package com.eypa.app.ui.detail;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.eypa.app.api.ApiClient;
import com.eypa.app.model.Comment;
import com.eypa.app.model.CommentsRequest;
import com.eypa.app.model.CommentsResponse;
import com.eypa.app.model.ContentItem;
import com.eypa.app.model.DeleteCommentRequest;
import com.eypa.app.model.LikeCommentRequest;
import com.eypa.app.model.LikeCommentResponse;
import com.eypa.app.model.SubmitCommentRequest;
import com.eypa.app.model.SubmitCommentResponse;
import com.eypa.app.ui.detail.model.CommentBlock;
import com.eypa.app.utils.UserManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DetailViewModel extends AndroidViewModel {

    private final MutableLiveData<ContentItem> postData = new MutableLiveData<>();
    private final MutableLiveData<List<CommentBlock>> commentBlocks = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(true);
    private final MutableLiveData<Integer> totalCommentCount = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> navigateToLogin = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> commentItemUpdated = new MutableLiveData<>();
    private final MutableLiveData<Integer> commentItemRemoved = new MutableLiveData<>();
    private final MutableLiveData<CommentBlock> commentItemAdded = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoadMoreLoading = new MutableLiveData<>(false);

    private String currentSortType = "date";
    private Integer currentOnlyAuthor = 0;
    
    // 分页相关
    private int currentPage = 1;
    private boolean hasMoreComments = true;
    private boolean isCommentsLoading = false;

    public DetailViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<ContentItem> getPostData() {
        return postData;
    }

    public LiveData<Boolean> getNavigateToLogin() {
        return navigateToLogin;
    }

    public LiveData<Integer> getCommentItemUpdated() {
        return commentItemUpdated;
    }

    public LiveData<Integer> getCommentItemRemoved() {
        return commentItemRemoved;
    }

    public LiveData<CommentBlock> getCommentItemAdded() {
        return commentItemAdded;
    }

    public LiveData<Boolean> getIsLoadMoreLoading() {
        return isLoadMoreLoading;
    }

    public void onLoginNavigationHandled() {
        navigateToLogin.setValue(false);
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
        refreshComments(postId);
    }

    public void updateCommentFilter(int postId, String type, Integer onlyAuthor) {
        this.currentSortType = type;
        this.currentOnlyAuthor = onlyAuthor;
        isLoading.setValue(true);
        refreshComments(postId);
    }
    
    public void loadMoreComments(int postId) {
        if (isCommentsLoading || !hasMoreComments) {
            return;
        }
        loadCommentsInternal(postId, currentPage + 1);
    }
    
    public void refreshComments(int postId) {
        currentPage = 1;
        hasMoreComments = true;
        loadCommentsInternal(postId, 1);
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

    private void loadCommentsInternal(int postId, int page) {
        isCommentsLoading = true;
        if (page > 1) {
            isLoadMoreLoading.setValue(true);
        }
        CommentsRequest request = new CommentsRequest(postId, page);
        request.setType(currentSortType);
        request.setOnlyAuthor(currentOnlyAuthor);
        
        String token = UserManager.getInstance(getApplication()).getToken();
        if (token != null) {
            request.setToken(token);
        }

        ApiClient.getApiService().getComments(request).enqueue(new Callback<CommentsResponse>() {
            @Override
            public void onResponse(Call<CommentsResponse> call, Response<CommentsResponse> response) {
                isCommentsLoading = false;
                isLoadMoreLoading.setValue(false);
                List<CommentBlock> newBlocks = new ArrayList<>();
                
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    CommentsResponse.Pagination pagination = response.body().getPagination();
                    if (pagination != null) {
                        hasMoreComments = pagination.isHasNext();
                        currentPage = pagination.getPage();
                        totalCommentCount.setValue(pagination.getTotalCount());
                    } else {
                        hasMoreComments = false;
                    }

                    List<Comment> comments = response.body().getData();
                    if (comments != null) {
                        processComments(comments);
                        for (Comment rootComment : comments) {
                            newBlocks.add(new CommentBlock(rootComment, 0));
                        }
                    }
                    
                    if (page == 1) {
                        commentBlocks.setValue(newBlocks);
                    } else {
                        List<CommentBlock> currentBlocks = commentBlocks.getValue();
                        if (currentBlocks == null) {
                            currentBlocks = new ArrayList<>();
                        }
                        currentBlocks.addAll(newBlocks);
                        commentBlocks.setValue(currentBlocks);
                    }
                } else {
                    if (page == 1) {
                        commentBlocks.setValue(new ArrayList<>());
                    }
                    hasMoreComments = false;
                }

                // 只有当两个请求都结束后才停止加载动画
                if (postData.getValue() != null || !response.isSuccessful()) {
                    isLoading.setValue(false);
                }
            }
            @Override
            public void onFailure(Call<CommentsResponse> call, Throwable t) {
                isCommentsLoading = false;
                isLoadMoreLoading.setValue(false);
                if (page == 1) {
                    commentBlocks.setValue(new ArrayList<>());
                }
                isLoading.setValue(false);
            }
        });
    }

    public void likeComment(Comment comment) {
        if (UserManager.getInstance(getApplication()).getToken() == null) {
            navigateToLogin.setValue(true);
            return;
        }

        LikeCommentRequest request = new LikeCommentRequest(UserManager.getInstance(getApplication()).getToken(), comment.getId());
        ApiClient.getApiService().likeComment(request).enqueue(new Callback<LikeCommentResponse>() {
            @Override
            public void onResponse(Call<LikeCommentResponse> call, Response<LikeCommentResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    LikeCommentResponse.Data data = response.body().getData();
                    updateCommentLikeStatus(comment.getId(), data.getLikeCount(), data.isLiked());
                }
            }

            @Override
            public void onFailure(Call<LikeCommentResponse> call, Throwable t) {
                // 不做处理
            }
        });
    }

    public void deleteComment(Comment comment) {
        if (UserManager.getInstance(getApplication()).getToken() == null) {
            navigateToLogin.setValue(true);
            return;
        }

        DeleteCommentRequest request = new DeleteCommentRequest(UserManager.getInstance(getApplication()).getToken(), comment.getId());
        ApiClient.getApiService().deleteComment(request).enqueue(new Callback<LikeCommentResponse>() {
            @Override
            public void onResponse(Call<LikeCommentResponse> call, Response<LikeCommentResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getCode() == 200) {
                    removeCommentFromList(comment.getId());
                }
            }

            @Override
            public void onFailure(Call<LikeCommentResponse> call, Throwable t) {
                // 不做处理
            }
        });
    }

    public void submitComment(int postId, String content, int parentId) {
        if (UserManager.getInstance(getApplication()).getToken() == null) {
            navigateToLogin.setValue(true);
            return;
        }

        SubmitCommentRequest request = new SubmitCommentRequest(UserManager.getInstance(getApplication()).getToken(), postId, content, parentId);
        ApiClient.getApiService().submitComment(request).enqueue(new Callback<SubmitCommentResponse>() {
            @Override
            public void onResponse(Call<SubmitCommentResponse> call, Response<SubmitCommentResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getCode() == 200) {
                    Comment newComment = response.body().getData();
                    if (newComment != null) {
                        addCommentToList(newComment);
                    }
                }
            }

            @Override
            public void onFailure(Call<SubmitCommentResponse> call, Throwable t) {
                // 不做处理
            }
        });
    }

    private void addCommentToList(Comment newComment) {
        commentItemAdded.setValue(new CommentBlock(newComment, -1));
        
        Integer currentCount = totalCommentCount.getValue();
        if (currentCount != null) {
            totalCommentCount.setValue(currentCount + 1);
        }
    }

    private void removeCommentFromList(int commentId) {
        List<CommentBlock> blocks = commentBlocks.getValue();
        if (blocks != null) {
            commentItemRemoved.setValue(commentId);
            
            Integer currentCount = totalCommentCount.getValue();
            if (currentCount != null && currentCount > 0) {
                totalCommentCount.setValue(currentCount - 1);
            }
        }
    }

    private void updateCommentLikeStatus(int commentId, int likeCount, boolean isLiked) {
        List<CommentBlock> blocks = commentBlocks.getValue();
        if (blocks != null) {
            for (CommentBlock block : blocks) {
                if (updateCommentInTree(block.getComment(), commentId, likeCount, isLiked)) {
                    commentItemUpdated.setValue(commentId);
                    break;
                }
            }
        }
    }

    private boolean updateCommentInTree(Comment root, int targetId, int likeCount, boolean isLiked) {
        if (root.getId() == targetId) {
            if (root.getInteraction() == null) {
                root.setInteraction(new Comment.Interaction());
            }
            root.getInteraction().setLikeCount(likeCount);
            root.getInteraction().setLiked(isLiked);
            return true;
        }

        if (root.getChildren() != null) {
            for (Comment child : root.getChildren()) {
                if (updateCommentInTree(child, targetId, likeCount, isLiked)) {
                    return true;
                }
            }
        }
        return false;
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

        sortComments(comments);

        for (Comment root : comments) {
            List<Comment> flattenedDescendants = root.getChildren();
            if (flattenedDescendants != null && !flattenedDescendants.isEmpty()) {
                reconstructCommentTree(root, flattenedDescendants);
            }
        }
    }

    private void reconstructCommentTree(Comment root, List<Comment> flattenedDescendants) {
        Map<Integer, Comment> descendantMap = new HashMap<>();
        for (Comment c : flattenedDescendants) {
            descendantMap.put(c.getId(), c);
            if (c.getChildren() == null) {
                c.setChildren(new ArrayList<>());
            } else {
                c.getChildren().clear();
            }
        }

        List<Comment> directChildren = new ArrayList<>();

        for (Comment c : flattenedDescendants) {
            int parentId = c.getParentId();
            if (parentId == root.getId()) {
                directChildren.add(c);
            } else {
                Comment parent = descendantMap.get(parentId);
                if (parent != null) {
                    parent.getChildren().add(c);
                }
            }
        }

        sortComments(directChildren);
        for (Comment c : flattenedDescendants) {
            if (c.getChildren() != null && !c.getChildren().isEmpty()) {
                sortComments(c.getChildren());
            }
        }

        root.setChildren(directChildren);
    }

    private void sortComments(List<Comment> comments) {
        if ("date".equals(currentSortType)) {
            Collections.sort(comments, (c1, c2) -> {
                String d1 = c1.getDate() != null ? c1.getDate() : "";
                String d2 = c2.getDate() != null ? c2.getDate() : "";
                return d2.compareTo(d1);
            });
        }
    }
}