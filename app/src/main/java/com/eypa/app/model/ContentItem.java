package com.eypa.app.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class ContentItem {
    // transient 关键字拒绝序列化或反序列化这个字段
    private transient List<Episode> episodesCache = null;

    private int id;
    @SerializedName("title")
    private Title title;
    @SerializedName("link")
    private String link;
    @SerializedName("date")
    private String date;
    @SerializedName("content")
    private Content content;
    @SerializedName("excerpt")
    private Excerpt excerpt;
    @SerializedName("author_info")
    private Author authorInfo;
    @SerializedName("_embedded")
    private Embedded embedded;
    private List<Integer> categories;
    private List<Integer> tags;
    @SerializedName("view_count")
    private int viewCount = 0;
    @SerializedName("like_count")
    private int likeCount = 0;
    @SerializedName("favorite_count")
    private int favoriteCount = 0;
    @SerializedName("comment_count")
    private int commentCount = 0;

    @SerializedName("is_liked")
    private boolean isLiked;
    @SerializedName("is_favorited")
    private boolean isFavorited;

    private int type = 0;

    @SerializedName("zib_other_data")
    private ZibOtherData zibOtherData;

    @SerializedName("cover_image")
    private String apiCoverImage;
    
    public static class ZibOtherData {
        @SerializedName("featured_video")
        private String featuredVideo;
        @SerializedName("featured_video_title")
        private String featuredVideoTitle;
        @SerializedName("featured_video_episode")
        private List<VideoEpisode> featuredVideoEpisode;
        @SerializedName("cover_image")
        private String coverImage;
        @SerializedName("thumbnail_url")
        private String thumbnailUrl;
        @SerializedName("featured_music")
        private List<FeaturedMusic> featuredMusic;
    }

    public static class FeaturedMusic {
        @SerializedName("name")
        private String name;
        @SerializedName("url")
        private String url;
        @SerializedName("cover")
        private String cover;
        @SerializedName("artist")
        private String artist;

        public String getName() { return name; }
        public String getUrl() { return url; }
        public String getCover() { return cover; }
        public String getArtist() { return artist; }
    }

    public static class VideoEpisode {
        @SerializedName("title")
        private String title;
        @SerializedName("url")
        private String url;

        public String getTitle() {
            return title != null ? title : "";
        }
        public String getUrl() {
            return url;
        }
    }

    public static class Episode {
        private final String title;
        private final String url;
        private boolean isPlaying = false;

        public Episode(String title, String url) {
            this.title = title;
            this.url = url;
        }
        public String getTitle() {
            return title;
        }
        public String getUrl() {
            return url;
        }
        public boolean isPlaying() {
            return isPlaying;
        }
        public void setPlaying(boolean playing) {
            isPlaying = playing;
        }
    }

    public List<FeaturedMusic> getFeaturedMusic() {
        if (zibOtherData != null) {
            return zibOtherData.featuredMusic;
        }
        return null;
    }

    public List<Episode> getAllEpisodes() {
        if (episodesCache != null) {
            return episodesCache;
        }

        List<Episode> allEpisodes = new ArrayList<>();
        if (zibOtherData == null) {
            this.episodesCache = allEpisodes;
            return this.episodesCache;
        }

        if (zibOtherData.featuredVideo != null && !zibOtherData.featuredVideo.isEmpty()) {
            String p1Title = (zibOtherData.featuredVideoTitle != null && !zibOtherData.featuredVideoTitle.isEmpty())
                    ? zibOtherData.featuredVideoTitle : "P1";
            allEpisodes.add(new Episode(p1Title, zibOtherData.featuredVideo));
        }

        if (zibOtherData.featuredVideoEpisode != null) {
            for (VideoEpisode videoEpisode : zibOtherData.featuredVideoEpisode) {
                if (videoEpisode.getUrl() != null && !videoEpisode.getUrl().isEmpty()) {
                    allEpisodes.add(new Episode(videoEpisode.getTitle(), videoEpisode.getUrl()));
                }
            }
        }

        if (!allEpisodes.isEmpty()) {
            allEpisodes.get(0).setPlaying(true);
        }

        this.episodesCache = allEpisodes;
        return this.episodesCache;
    }

    public void updatePlayingEpisode(Episode newPlayingEpisode) {
        List<Episode> episodes = getAllEpisodes();
        for (Episode ep : episodes) {
            ep.setPlaying(ep.getUrl() != null && ep.getUrl().equals(newPlayingEpisode.getUrl()));
        }
    }

    public static class Title {
        @SerializedName("rendered")
        private String rendered;
        public String getRendered() { return rendered; }
        public void setRendered(String rendered) { this.rendered = rendered; }
    }
    public static class Content {
        @SerializedName("rendered")
        private String rendered;
        public String getRendered() { return rendered; }
    }
    public static class Excerpt {
        @SerializedName("rendered")
        private String rendered;
        public String getRendered() { return rendered; }
    }
    public static class Embedded {
        @SerializedName("wp:featuredmedia")
        public List<FeaturedMedia> wpFeaturedmedia;
        @SerializedName("author")
        public List<Author> author;
        @SerializedName("wp:term")
        public List<List<Term>> wpTerm;
    }
    public static class FeaturedMedia {
        @SerializedName("source_url")
        public String sourceUrl;
    }
    public static class Author {
        @SerializedName("id")
        private int id;
        @SerializedName("name")
        private String name;
        @SerializedName("avatar_urls")
        private AvatarUrls avatarUrls;
        private boolean isFollowing;

        public int getId() { return id; }
        public String getName() { return name; }
        public AvatarUrls getAvatarUrls() { return avatarUrls; }
        public boolean isFollowing() { return isFollowing; }
        public void setFollowing(boolean following) { isFollowing = following; }
        public static class AvatarUrls {
            @SerializedName("96")
            private String small;
            @SerializedName("300")
            private String medium;
            public String getSmall() { return small; }
            public String getMedium() { return medium; }
        }
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getLink() { return link; }
    public String getTitle() { return title != null ? title.getRendered() : ""; }
    public void setTitle(String titleStr) {
        if (this.title == null) {
            this.title = new Title();
        }
        this.title.setRendered(titleStr);
    }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public List<Integer> getCategories() { return categories; }
    public List<Integer> getTags() { return tags; }
    public int getViewCount() { return viewCount; }
    public void setViewCount(int viewCount) { this.viewCount = viewCount; }
    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
    public int getFavoriteCount() { return favoriteCount; }
    public void setFavoriteCount(int favoriteCount) { this.favoriteCount = favoriteCount; }
    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }
    public boolean isLiked() { return isLiked; }
    public void setLiked(boolean liked) { isLiked = liked; }
    public boolean isFavorited() { return isFavorited; }
    public void setFavorited(boolean favorited) { isFavorited = favorited; }
    
    public int getType() { return type; }
    public void setType(int type) { this.type = type; }

    public void setCoverImage(String url) {
        if (this.zibOtherData == null) {
            this.zibOtherData = new ZibOtherData();
        }
        this.zibOtherData.coverImage = url;
    }
    public Content getContent() { return content; }
    public Excerpt getExcerpt() { return excerpt; }
    public Author getAuthor() {
        if (this.authorInfo != null) {
            return this.authorInfo;
        }
        if (embedded != null && embedded.author != null && !embedded.author.isEmpty()) {
            return embedded.author.get(0);
        }
        return null;
    }

    public String getBestImageUrl() {
        if (apiCoverImage != null && !apiCoverImage.trim().isEmpty()) {
            return ensureHttps(apiCoverImage);
        }

        if (zibOtherData != null && zibOtherData.coverImage != null && !zibOtherData.coverImage.trim().isEmpty()) {
            return ensureHttps(zibOtherData.coverImage);
        }
        if (zibOtherData != null && zibOtherData.thumbnailUrl != null && !zibOtherData.thumbnailUrl.trim().isEmpty()) {
            return ensureHttps(zibOtherData.thumbnailUrl);
        }

        if (embedded != null && embedded.wpFeaturedmedia != null && !embedded.wpFeaturedmedia.isEmpty()) {
            FeaturedMedia media = embedded.wpFeaturedmedia.get(0);
            if (media != null && media.sourceUrl != null && !media.sourceUrl.trim().isEmpty()) {
                return ensureHttps(media.sourceUrl);
            }
        }

        return null;
    }

    private String ensureHttps(String url) {
        if (url == null) return null;
        url = url.trim();
        return url.startsWith("http://") ? url.replace("http://", "https://") : url;
    }

    public List<Category> getCategoriesWithNames() {
        List<Category> result = new ArrayList<>();
        if (embedded != null && embedded.wpTerm != null) {
            for (List<Term> termList : embedded.wpTerm) {
                for (Term term : termList) {
                    if (term instanceof Category) {
                        result.add((Category) term);
                    }
                }
            }
        }
        return result;
    }

    public List<Tag> getTagsWithNames() {
        List<Tag> result = new ArrayList<>();
        if (embedded != null && embedded.wpTerm != null) {
            for (List<Term> termList : embedded.wpTerm) {
                for (Term term : termList) {
                    if (term instanceof Tag) {
                        result.add((Tag) term);
                    }
                }
            }
        }
        return result;
    }
}
