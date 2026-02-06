// app/src/main/java/com/eypa/app/api/ContentApiService.java
package com.eypa.app.api;

import com.eypa.app.model.Category;
import com.eypa.app.model.Comment;
import com.eypa.app.model.ContentItem;
import com.eypa.app.model.SliderItem;
import com.eypa.app.model.Tag;
import com.eypa.app.model.UpdateInfo;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
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
     * 新增：根据关键词搜索文章
     * @param query 搜索关键词
     * @param page 页码
     * @param perPage 每页数量
     * @param fields 请求的字段
     * @param embed 嵌入的数据
     * @return 文章列表的 Call 对象
     */
    @GET("eu-json/wp/v2/posts")
    Call<List<ContentItem>> searchPosts(
            @Query("search") String query,
            @Query("page") int page,
            @Query("per_page") int perPage,
            @Query("_fields") String fields,
            @Query("_embed") String embed
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
}
