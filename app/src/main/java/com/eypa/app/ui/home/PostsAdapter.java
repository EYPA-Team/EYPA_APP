package com.eypa.app.ui.home;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.CircularProgressDrawable;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.eypa.app.R;
import com.eypa.app.model.ContentItem;
import com.eypa.app.model.SliderItem;
import com.eypa.app.ui.detail.DetailActivity;
import com.eypa.app.utils.TimeAgoUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PostsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ITEM = 1;
    private static final int VIEW_TYPE_LOADING = 2;

    private final List<ContentItem> postList;
    private final Map<Integer, String> categoryMap;
    private List<SliderItem> sliderItems = new ArrayList<>();
    private boolean isLoadingFooterVisible = false;

    public PostsAdapter(List<ContentItem> postList, Map<Integer, String> categoryMap) {
        this.postList = postList;
        this.categoryMap = categoryMap;
    }

    public void setSliderItems(List<SliderItem> items) {
        this.sliderItems = items;
        notifyDataSetChanged();
    }

    private boolean hasHeader() {
        return !sliderItems.isEmpty();
    }

    @Override
    public int getItemViewType(int position) {
        if (hasHeader() && position == 0) {
            return VIEW_TYPE_HEADER;
        }
        if (isLoadingFooterVisible && position == getItemCount() - 1) {
            return VIEW_TYPE_LOADING;
        }
        return VIEW_TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.layout_home_slider, parent, false);
            StaggeredGridLayoutManager.LayoutParams layoutParams =
                    new StaggeredGridLayoutManager.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.setFullSpan(true);
            view.setLayoutParams(layoutParams);
            return new SliderViewHolder(view);
        } else if (viewType == VIEW_TYPE_LOADING) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_loading_footer, parent, false);
            StaggeredGridLayoutManager.LayoutParams layoutParams =
                    new StaggeredGridLayoutManager.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.setFullSpan(true);
            view.setLayoutParams(layoutParams);
            return new LoadingViewHolder(view);
        }
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_post_card, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof SliderViewHolder) {
            ((SliderViewHolder) holder).bind(sliderItems);
            return;
        }
        if (holder instanceof LoadingViewHolder) {
            return;
        }

        PostViewHolder postHolder = (PostViewHolder) holder;
        int actualPosition = hasHeader() ? position - 1 : position;
        ContentItem post = postList.get(actualPosition);

        TypedValue typedValue = new TypedValue();
        postHolder.itemView.getContext().getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true);
        int colorPrimary = typedValue.data;

        String imageUrl = post.getBestImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            postHolder.coverImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
            CircularProgressDrawable circularProgressDrawable = new CircularProgressDrawable(postHolder.itemView.getContext());
            circularProgressDrawable.setStrokeWidth(5f);
            circularProgressDrawable.setCenterRadius(30f);
            circularProgressDrawable.setColorSchemeColors(colorPrimary);
            circularProgressDrawable.start();

            Glide.with(postHolder.itemView.getContext())
                    .load(imageUrl)
                    .placeholder(circularProgressDrawable)
                    .error(R.drawable.placeholder_image)
                    .into(postHolder.coverImage);
        } else {
            postHolder.coverImage.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            postHolder.coverImage.setImageResource(R.drawable.placeholder_image);
        }

        postHolder.title.setText(post.getTitle());
        postHolder.date.setText(TimeAgoUtils.getRelativeTime(postHolder.itemView.getContext(), post.getDate()));
        postHolder.viewCount.setText(formatCount(post.getViewCount()));
        postHolder.likeCount.setText(formatCount(post.getLikeCount()));

        if (post.getCategories() != null && !post.getCategories().isEmpty()) {
            int categoryId = post.getCategories().get(0);
            String categoryName = categoryMap.get(categoryId);
            if (categoryName != null) {
                postHolder.category.setText(categoryName);
            } else {
                postHolder.category.setText("分类: " + categoryId);
            }
        } else {
            postHolder.category.setText("");
        }

        Drawable background = postHolder.category.getBackground();
        if (background != null) {
            background = DrawableCompat.wrap(background.mutate());
            int alphaColor = ColorUtils.setAlphaComponent(colorPrimary, 204);
            DrawableCompat.setTint(background, alphaColor);
            postHolder.category.setBackground(background);
        }

        // 添加点击事件 - 使用公开的 EXTRA_POST_ID
        postHolder.itemView.setOnClickListener(v -> {
            Context context = v.getContext();
            Intent intent = new Intent(context, DetailActivity.class);
            intent.putExtra(DetailActivity.EXTRA_POST_ID, post.getId());
            context.startActivity(intent);
        });
    }


    private String formatCount(int count) {
        if (count >= 1000) {
            return String.format(Locale.getDefault(), "%.1fK", count / 1000.0);
        }
        return String.valueOf(count);
    }

    @Override
    public int getItemCount() {
        return postList.size() + (hasHeader() ? 1 : 0) + (isLoadingFooterVisible ? 1 : 0);
    }

    public void setLoadingFooterVisible(boolean visible) {
        if (isLoadingFooterVisible != visible) {
            isLoadingFooterVisible = visible;
            notifyDataSetChanged();
        }
    }

    static class SliderViewHolder extends RecyclerView.ViewHolder {
        ViewPager2 viewPager;
        Handler sliderHandler = new Handler(Looper.getMainLooper());
        Runnable sliderRunnable;

        public SliderViewHolder(@NonNull View itemView) {
            super(itemView);
            viewPager = itemView.findViewById(R.id.slider_view_pager);
        }

        public void bind(List<SliderItem> items) {
            viewPager.setAdapter(new SliderAdapter(items));
            
            if (sliderRunnable != null) sliderHandler.removeCallbacks(sliderRunnable);
            sliderRunnable = new Runnable() {
                @Override
                public void run() {
                    int currentItem = viewPager.getCurrentItem();
                    int nextItem = (currentItem + 1) % items.size();
                    viewPager.setCurrentItem(nextItem);
                    sliderHandler.postDelayed(this, 5000);
                }
            };
            sliderHandler.postDelayed(sliderRunnable, 5000);
            
            viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    sliderHandler.removeCallbacks(sliderRunnable);
                    sliderHandler.postDelayed(sliderRunnable, 5000);
                }
            });
        }
    }

    static class SliderAdapter extends RecyclerView.Adapter<SliderAdapter.SliderItemViewHolder> {
        private final List<SliderItem> items;

        public SliderAdapter(List<SliderItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public SliderItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_slider, parent, false);
            return new SliderItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SliderItemViewHolder holder, int position) {
            SliderItem item = items.get(position);
            holder.title.setText(item.getTitle());
            holder.desc.setText(item.getDesc());
            
            Glide.with(holder.itemView.getContext())
                    .load(item.getImage())
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .into(holder.image);

            holder.itemView.setOnClickListener(v -> {
                String url = item.getUrl();
                if (url != null) {
                    Pattern pattern = Pattern.compile("https://eqmemory\\.cn/(\\d+)\\.html");
                    Matcher matcher = pattern.matcher(url);
                    if (matcher.find()) {
                        String idStr = matcher.group(1);
                        try {
                            int id = Integer.parseInt(idStr);
                            DetailActivity.start(v.getContext(), id);
                        } catch (NumberFormatException e) {
                            openUrl(v.getContext(), url);
                        }
                    } else {
                        openUrl(v.getContext(), url);
                    }
                }
            });
        }

        private void openUrl(Context context, String url) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            context.startActivity(intent);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class SliderItemViewHolder extends RecyclerView.ViewHolder {
            ImageView image;
            TextView title;
            TextView desc;

            public SliderItemViewHolder(@NonNull View itemView) {
                super(itemView);
                image = itemView.findViewById(R.id.slider_image);
                title = itemView.findViewById(R.id.slider_title);
                desc = itemView.findViewById(R.id.slider_desc);
            }
        }
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView coverImage;
        TextView title;
        TextView category;
        TextView date;
        TextView viewCount;
        TextView likeCount;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            coverImage = itemView.findViewById(R.id.cover_image);
            title = itemView.findViewById(R.id.title);
            category = itemView.findViewById(R.id.category);
            date = itemView.findViewById(R.id.date);
            viewCount = itemView.findViewById(R.id.view_count);
            likeCount = itemView.findViewById(R.id.like_count);
        }
    }

    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        public LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
