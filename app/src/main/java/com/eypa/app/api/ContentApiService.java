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
    /**
     * [修改] 首页文章列表
     * 变化：
     * URL 变为 eu-json/app/v1/posts
     * 删除了 _fields 和 _embed 参数 (后端已自动处理)
     */
    @GET("eu-json/app/v1/posts")
    Call<List<ContentItem>> getContentItems(
            @Query("page") int page,
            @Query("per_page") int perPage,
            @Query("orderby") String orderby,
            @Query("seed") String seed,
            @Query("categories") Integer categoryId
    );

    /**
     * [修改] 文章详情接口
     * 变化：
     * URL 变为 eu-json/app/v1/post/detail
     * 方法变为 POST (为了传 Token)
     * 参数变为 Body (PostDetailRequest)
     */
    @POST("eu-json/app/v1/post/detail")
    Call<ContentItem> getPostDetail(@Body PostDetailRequest request);

    @GET("eu-json/wp/v2/categories")
    Call<List<Category>> getAllCategories(
            @Query("per_page") int perPage,
            @Query("orderby") String orderby,
            @Query("hide_empty") boolean hideEmpty
    );

    @GET("eu-json/wp/v2/categories")
    Call<List<Category>> getCategories(
            @Query("include") String includeIds
    );

    @GET("eu-json/wp/v2/tags")
    Call<List<Tag>> getTags(
            @Query("include") String includeIds
    );

    @GET("eu-json/wp/v2/categories")
    Call<List<Category>> getCategoriesByIds(
            @Query("include") String includeIds,
            @Query("per_page") int perPage
    );

    /**
     * 根据文章ID获取评论列表
     * @param request 评论请求体
     * @return 响应
     */
    @POST("eu-json/app/v1/comments")
    Call<CommentsResponse> getComments(@Body CommentsRequest request);

    /**
     * 评论点赞/取消点赞
     * @param request 点赞请求体
     * @return 响应
     */
    @POST("eu-json/app/v1/comment/like")
    Call<LikeCommentResponse> likeComment(@Body LikeCommentRequest request);

    /**
     * 删除评论
     * @param request 删除请求体
     * @return 响应
     */
    @POST("eu-json/app/v1/comment/delete")
    Call<LikeCommentResponse> deleteComment(@Body DeleteCommentRequest request);

    /**
     * 发表/回复评论
     * @param request 评论请求体
     * @return 响应
     */
    @POST("eu-json/app/v1/comment/submit")
    Call<SubmitCommentResponse> submitComment(@Body SubmitCommentRequest request);

    /**
     * 编辑评论
     * @param request 编辑请求体
     * @return 响应
     */
    @POST("eu-json/app/v1/comment/edit")
    Call<SubmitCommentResponse> editComment(@Body EditCommentRequest request);

    /**
     * 更新新的搜索API接口
     * 使用 keyword 而不是 search
     * 不需要 _fields 和 _embed 参数
     */
    @GET("eu-json/app/v1/search")
    Call<List<ContentItem>> searchPosts(
            @Query("keyword") String query,
            @Query("page") int page
    );

    /**
     * 获取首页轮播图
     * @return 轮播图列表
     */
    @GET("eu-json/app/v1/slider")
    Call<List<SliderItem>> getSlider();

    /**
     * 检查应用更新
     * @return 更新信息
     */
    @GET("eu-json/app/v1/check-update")
    Call<UpdateInfo> checkUpdate();

    /**
     * 用户登录
     * @param request 登录请求体
     * @return 登录响应
     */
    @POST("eu-json/app/v1/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    /**
     * 获取/刷新个人信息
     * @param request Token请求体
     * @return 个人信息响应
     */
    @POST("eu-json/app/v1/user/profile")
    Call<LoginResponse> getUserProfile(@Body TokenRequest request);

    /**
     * 发送验证码
     * @param request 发送验证码请求体
     * @return 响应
     */
    @POST("eu-json/app/v1/send-code")
    Call<LoginResponse> sendCode(@Body SendCodeRequest request);

    /**
     * 注册账号
     * @param request 注册请求体
     * @return 响应
     */
    @POST("eu-json/app/v1/register")
    Call<LoginResponse> register(@Body RegisterRequest request);

    /**
     * 重置密码
     * @param request 重置密码请求体
     * @return 响应
     */
    @POST("eu-json/app/v1/reset-password")
    Call<LoginResponse> resetPassword(@Body ResetPasswordRequest request);

    /**
     * 获取用户收藏列表
     * @param request 收藏请求体
     * @return 收藏列表
     */
    @POST("eu-json/app/v1/user/favorites")
    Call<List<ContentItem>> getFavorites(@Body FavoritesRequest request);

    /**
     * 获取作者信息
     * @param request 作者信息请求体
     * @return 作者信息
     */
    @POST("eu-json/app/v1/author/info")
    Call<AuthorInfoResponse> getAuthorInfo(@Body AuthorInfoRequest request);

    /**
     * 关注/取消关注用户
     * @param request 关注请求体
     * @return 响应
     */
    @POST("eu-json/app/v1/user/follow")
    Call<FollowResponse> followUser(@Body FollowRequest request);

    /**
     * 获取作者发布的文章列表
     * @param request 请求体
     * @return 文章列表
     */
    @POST("eu-json/app/v1/author/posts")
    Call<List<ContentItem>> getAuthorPosts(@Body AuthorListRequest request);

    /**
     * 获取作者收藏的文章列表
     * @param request 请求体
     * @return 收藏列表
     */
    @POST("eu-json/app/v1/author/favorites")
    Call<List<ContentItem>> getAuthorFavorites(@Body AuthorListRequest request);

    /**
     * 获取作者关注列表
     * @param request 请求体
     * @return 关注列表
     */
    @POST("eu-json/app/v1/author/following")
    Call<AuthorFansResponse> getAuthorFans(@Body AuthorListRequest request);
}
