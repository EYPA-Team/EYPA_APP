// app/src/main/java/com/eypa/app/api/ContentApiService.java
package com.eypa.app.api;

import com.eypa.app.model.Category;
import com.eypa.app.model.Comment;
import com.eypa.app.model.ContentItem;
import com.eypa.app.model.SliderItem;
import com.eypa.app.model.Tag;
import com.eypa.app.model.UpdateInfo;
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
    @GET("eu-json/wp/v2/posts")
    Call<List<ContentItem>> getContentItems(
            @Query("page") int page,
            @Query("per_page") int perPage,
            @Query("_fields") String fields,
            @Query("orderby") String orderby,
            @Query("seed") String seed,
            @Query("_embed") String embed,
            @Query("categories") Integer categoryId
    );

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

    @GET("eu-json/wp/v2/posts/{id}")
    Call<ContentItem> getPostWithCustomFields(
            @Path("id") int postId,
            @Query("_fields") String fields,
            @Query("_embed") String embed
    );

    @GET("eu-json/wp/v2/categories")
    Call<List<Category>> getCategoriesByIds(
            @Query("include") String includeIds,
            @Query("per_page") int perPage
    );

    /**
     * 根据文章ID获取评论列表
     * @param postId 文章ID
     * @param perPage 每页数量 (新增参数)
     * @return 评论列表的 Call 对象
     */
    @GET("eu-json/wp/v2/comments")
    Call<List<com.eypa.app.model.Comment>> getComments(
            @Query("post") int postId,
            @Query("per_page") int perPage
    );

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
}
