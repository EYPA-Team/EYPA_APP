package com.eypa.app.ui.home;

import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.bumptech.glide.Glide;
import com.eypa.app.R;
import com.eypa.app.api.ApiClient;
import com.eypa.app.model.bbs.BBSPost;
import com.eypa.app.model.bbs.BBSPostDetailRequest;
import com.eypa.app.model.bbs.BBSPostDetailResponse;
import com.eypa.app.utils.HtmlUtils;
import com.eypa.app.utils.ThemeUtils;
import com.eypa.app.utils.UserManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BBSPostDetailActivity extends AppCompatActivity {

    public static final String EXTRA_POST_ID = "extra_post_id";
    
    private int postId;
    private TextView tvTitle, tvContent, tvAuthorName, tvPlate;
    private ImageView ivAvatar;
    private View loadingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applyTheme(this);
        int themeId = getSharedPreferences("AppSettings", MODE_PRIVATE).getInt("ThemeId", R.style.Theme_EYPA_APP);
        setTheme(themeId);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bbs_post_detail);

        postId = getIntent().getIntExtra(EXTRA_POST_ID, -1);
        if (postId == -1) {
            finish();
            return;
        }

        initViews();
        loadDetail();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }

        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        int color = (currentNightMode == Configuration.UI_MODE_NIGHT_YES) ? Color.WHITE : Color.BLACK;

        Drawable upArrow = ContextCompat.getDrawable(this, androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        if (upArrow != null) {
            upArrow = DrawableCompat.wrap(upArrow);
            DrawableCompat.setTint(upArrow, color);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setHomeAsUpIndicator(upArrow);
            }
        }

        tvTitle = findViewById(R.id.tv_title);
        tvContent = findViewById(R.id.tv_content);
        tvAuthorName = findViewById(R.id.tv_author_name);
        tvPlate = findViewById(R.id.tv_plate);
        ivAvatar = findViewById(R.id.iv_avatar);
        loadingView = findViewById(R.id.loading_view);
    }

    private void loadDetail() {
        loadingView.setVisibility(View.VISIBLE);
        
        String token = ""; 
        if (UserManager.getInstance(this).isLoggedIn().getValue()) {
            token = UserManager.getInstance(this).getToken();
        }

        BBSPostDetailRequest request = new BBSPostDetailRequest(postId, token);
        ApiClient.getApiService().getBBSPostDetail(request).enqueue(new Callback<BBSPostDetailResponse>() {
            @Override
            public void onResponse(@NonNull Call<BBSPostDetailResponse> call, @NonNull Response<BBSPostDetailResponse> response) {
                loadingView.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    displayPost(response.body().getData());
                } else {
                    Toast.makeText(BBSPostDetailActivity.this, "加载失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<BBSPostDetailResponse> call, @NonNull Throwable t) {
                loadingView.setVisibility(View.GONE);
            }
        });
    }

    private void displayPost(BBSPost post) {
        if (post == null) return;

        tvTitle.setText(post.getTitle());
        
        if (post.getContent() != null && post.getContent().rendered != null) {
            HtmlUtils.setHtmlText(tvContent, post.getContent().rendered);
            
            TypedValue typedValue = new TypedValue();
            getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true);
            tvContent.setLinkTextColor(typedValue.data);
        }

        if (post.getAuthorInfo() != null) {
            tvAuthorName.setText(post.getAuthorInfo().name);
            Glide.with(this)
                    .load(post.getAuthorInfo().avatar)
                    .circleCrop()
                    .into(ivAvatar);

            View.OnClickListener profileListener = v -> {
                try {
                    int authorId = Integer.parseInt(post.getAuthorInfo().id);
                    AuthorProfileActivity.start(this, authorId);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            };
            ivAvatar.setOnClickListener(profileListener);
            tvAuthorName.setOnClickListener(profileListener);
        }

        if (post.getPlate() != null) {
            tvPlate.setText(post.getPlate().name);
            tvPlate.setVisibility(View.VISIBLE);
        } else {
            tvPlate.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
