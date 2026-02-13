// app/src/main/java/com/eypa/app/api/ContentApiService.java
package com.eypa.app.api;

import com.eypa.app.model.Category;
import com.eypa.app.model.CommentsRequest;
import com.eypa.app.model.CommentsResponse;
import com.eypa.app.model.ContentItem;
import com.eypa.app.model.DeleteCommentRequest;
import com.eypa.app.model.EditCommentRequest;
import com.eypa.app.model.LikeCommentRequest;
import com.eypa.app.model.LikeCommentResponse;
import com.eypa.app.model.PostActionRequest;
import com.eypa.app.model.PostActionResponse;
import com.eypa.app.model.PostDetailRequest;
import com.eypa.app.model.SliderItem;
import com.eypa.app.model.SubmitCommentRequest;
import com.eypa.app.model.SubmitCommentResponse;
import com.eypa.app.model.Tag;
import com.eypa.app.model.UpdateInfo;
import com.eypa.app.model.user.AuthorFansResponse;
import com.eypa.app.model.user.AuthorInfoRequest;
import com.eypa.app.model.user.AuthorInfoResponse;
import com.eypa.app.model.user.AuthorListRequest;
import com.eypa.app.model.user.FavoritesRequest;
import com.eypa.app.model.user.FollowRequest;
import com.eypa.app.model.user.FollowResponse;
import com.eypa.app.model.user.LoginRequest;
import com.eypa.app.model.user.LoginResponse;
import com.eypa.app.model.user.RegisterRequest;
import com.eypa.app.model.user.ReportRequest;
import com.eypa.app.model.user.ReportResponse;
import com.eypa.app.model.user.ResetPasswordRequest;
import com.eypa.app.model.user.SendCodeRequest;
import com.eypa.app.model.user.TokenRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ContentApiService {
    // 首页文章列表
    @GET("eu-json/app/v1/posts")
    Call<List<ContentItem>> getContentItems(
            @Query("page") int page,
            @Query("per_page") int perPage,
            @Query("orderby") String orderby,
            @Query("seed") String seed,
            @Query("categories") Integer categoryId
    );

    // 首页轮播图
    @GET("eu-json/app/v1/slider")
    Call<List<SliderItem>> getSlider();

    // 搜索文章
    @GET("eu-json/app/v1/search")
    Call<List<ContentItem>> searchPosts(
            @Query("keyword") String query,
            @Query("page") int page
    );

    // 获取分类列表
    @GET("eu-json/wp/v2/categories")
    Call<List<Category>> getCategories(
            @Query("per_page") Integer perPage,
            @Query("orderby") String orderby,
            @Query("hide_empty") Boolean hideEmpty,
            @Query("include") String includeIds
    );

    // 检查应用更新
    @GET("eu-json/app/v1/check-update")
    Call<UpdateInfo> checkUpdate();

    // 文章详情
    @POST("eu-json/app/v1/post/detail")
    Call<ContentItem> getPostDetail(@Body PostDetailRequest request);

    // 根据文章ID获取评论列表
    @POST("eu-json/app/v1/comments")
    Call<CommentsResponse> getComments(@Body CommentsRequest request);

    // 点赞/取消点赞评论
    @POST("eu-json/app/v1/comment/like")
    Call<LikeCommentResponse> likeComment(@Body LikeCommentRequest request);

    // 删除评论
    @POST("eu-json/app/v1/comment/delete")
    Call<LikeCommentResponse> deleteComment(@Body DeleteCommentRequest request);

    // 发表/回复评论
    @POST("eu-json/app/v1/comment/submit")
    Call<SubmitCommentResponse> submitComment(@Body SubmitCommentRequest request);

    // 编辑评论
    @POST("eu-json/app/v1/comment/edit")
    Call<SubmitCommentResponse> editComment(@Body EditCommentRequest request);

    // 用户登录
    @POST("eu-json/app/v1/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    // 获取/刷新个人信息
    @POST("eu-json/app/v1/user/profile")
    Call<LoginResponse> getUserProfile(@Body TokenRequest request);

    // 发送验证码
    @POST("eu-json/app/v1/send-code")
    Call<LoginResponse> sendCode(@Body SendCodeRequest request);

    // 注册账号
    @POST("eu-json/app/v1/register")
    Call<LoginResponse> register(@Body RegisterRequest request);

    // 重置密码
    @POST("eu-json/app/v1/reset-password")
    Call<LoginResponse> resetPassword(@Body ResetPasswordRequest request);

    // 获取用户收藏列表
    @POST("eu-json/app/v1/user/favorites")
    Call<List<ContentItem>> getFavorites(@Body FavoritesRequest request);

    // 获取作者信息
    @POST("eu-json/app/v1/author/info")
    Call<AuthorInfoResponse> getAuthorInfo(@Body AuthorInfoRequest request);

    // 关注/取消关注用户
    @POST("eu-json/app/v1/user/follow")
    Call<FollowResponse> followUser(@Body FollowRequest request);

    // 获取作者发布的文章列表
    @POST("eu-json/app/v1/author/posts")
    Call<List<ContentItem>> getAuthorPosts(@Body AuthorListRequest request);

    // 获取作者收藏的文章列表
    @POST("eu-json/app/v1/author/favorites")
    Call<List<ContentItem>> getAuthorFavorites(@Body AuthorListRequest request);

    // 获取作者关注列表
    @POST("eu-json/app/v1/author/following")
    Call<AuthorFansResponse> getAuthorFans(@Body AuthorListRequest request);

    // 点赞/取消点赞文章
    @POST("eu-json/app/v1/post/like")
    Call<PostActionResponse> likePost(@Body PostActionRequest request);

    // 收藏/取消收藏文章
    @POST("eu-json/app/v1/post/favorite")
    Call<PostActionResponse> favoritePost(@Body PostActionRequest request);

    // 举报用户/内容
    @POST("eu-json/app/v1/user/report")
    Call<ReportResponse> reportUser(@Body ReportRequest request);
}
