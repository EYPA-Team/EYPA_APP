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
}
