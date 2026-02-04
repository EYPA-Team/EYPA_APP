package com.eypa.app.ui.detail;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eypa.app.api.ApiClient;
import com.eypa.app.model.Comment;
import com.eypa.app.model.ContentItem;
import com.eypa.app.ui.detail.model.CommentBlock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DetailViewModel extends ViewModel {

    private final MutableLiveData<ContentItem> postData = new MutableLiveData<>();
    private final MutableLiveData<List<CommentBlock>> commentBlocks = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(true);

    public LiveData<ContentItem> getPostData() {
        return postData;
    }

    public LiveData<List<CommentBlock>> getCommentBlocks() {
        return commentBlocks;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
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
        // 传入 100 作为 per_page 参数，获取更多评论（解决只显示9条的问题）
        ApiClient.getApiService().getComments(postId, 100).enqueue(new Callback<List<Comment>>() {
            @Override
            public void onResponse(Call<List<Comment>> call, Response<List<Comment>> response) {
                List<CommentBlock> rootCommentBlocks = new ArrayList<>();
                if (response.isSuccessful() && response.body() != null) {
                    List<Comment> rootComments = buildCommentTree(response.body());
                    for (Comment rootComment : rootComments) {
                        rootCommentBlocks.add(new CommentBlock(rootComment, 0));
                    }
                }
                commentBlocks.setValue(rootCommentBlocks); // LiveData 只发布顶层评论

                // 只有当两个请求都结束后才停止加载动画
                if (postData.getValue() != null || !response.isSuccessful()) {
                    isLoading.setValue(false);
                }
            }
            @Override
            public void onFailure(Call<List<Comment>> call, Throwable t) {
                commentBlocks.setValue(new ArrayList<>());
                isLoading.setValue(false);
            }
        });
    }

    /**
     * 将扁平的评论列表构建成树形结构
     * @param flatComments 从API获取的原始评论列表
     * @return 只包含顶层评论的列表，每个评论对象内部都包含了它的子评论
     */
    private List<Comment> buildCommentTree(List<Comment> flatComments) {
        Map<Integer, Comment> commentMap = new HashMap<>();
        List<Comment> rootComments = new ArrayList<>();

        Collections.sort(flatComments, (c1, c2) -> c1.getDate().compareTo(c2.getDate()));

        for (Comment comment : flatComments) {
            commentMap.put(comment.getId(), comment);
        }

        // 遍历所有评论，构建层级关系
        for (Comment comment : flatComments) {
            if (comment.getParent() == 0) {
                // 如果 parent 是 0，说明是顶层评论
                rootComments.add(comment);
            } else {
                Comment parent = commentMap.get(comment.getParent());
                if (parent != null) {
                    // children 列表已初始化
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(comment);
                }
            }
        }
        return rootComments;
    }
}