package com.eypa.app.ui.home;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.eypa.app.R;
import com.eypa.app.api.ApiClient;
import com.eypa.app.api.ContentApiService;
import com.eypa.app.model.user.AuthorInfoRequest;
import com.eypa.app.model.user.AuthorInfoResponse;
import com.eypa.app.model.user.FollowRequest;
import com.eypa.app.model.user.FollowResponse;
import com.eypa.app.ui.detail.components.StatsView;
import com.eypa.app.ui.widget.ZoomableImageView;
import com.eypa.app.utils.UserManager;
import com.google.android.material.appbar.CollapsingToolbarLayout;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthorProfileActivity extends AppCompatActivity {

    private static final String EXTRA_USER_ID = "extra_user_id";

    private int userId;
    private ContentApiService apiService;

    private ImageView ivCover;
    private ZoomableImageView ivAvatar;
    private TextView tvName;
    private TextView tvLevel;
    private TextView tvVip;
    private TextView tvDesc;
    private LinearLayout layoutAuth;
    private TextView tvAuthInfo;
    private Button btnAction;
    private StatsView viewStats;
    private LinearLayout layoutMedals;
    private TextView tvMedalsCount;
    private View loadingMask;
    private CollapsingToolbarLayout collapsingToolbarLayout;

    public static void start(Context context, int userId) {
        Intent intent = new Intent(context, AuthorProfileActivity.class);
        intent.putExtra(EXTRA_USER_ID, userId);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyCustomTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_author_profile);

        userId = getIntent().getIntExtra(EXTRA_USER_ID, -1);
        if (userId == -1) {
            finish();
            return;
        }

        apiService = ApiClient.getClient().create(ContentApiService.class);

        initViews();
        loadData();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }

        collapsingToolbarLayout = findViewById(R.id.toolbar_layout);
        ivCover = findViewById(R.id.iv_cover);
        ivAvatar = findViewById(R.id.iv_avatar);
        tvName = findViewById(R.id.tv_name);
        tvLevel = findViewById(R.id.tv_level);
        tvVip = findViewById(R.id.tv_vip);
        tvDesc = findViewById(R.id.tv_desc);
        layoutAuth = findViewById(R.id.layout_auth);
        tvAuthInfo = findViewById(R.id.tv_auth_info);
        btnAction = findViewById(R.id.btn_action);
        viewStats = findViewById(R.id.view_stats);
        layoutMedals = findViewById(R.id.layout_medals);
        tvMedalsCount = findViewById(R.id.tv_medals_count);
        loadingMask = findViewById(R.id.loading_mask);
    }

    private void applyCustomTheme() {
        android.content.SharedPreferences sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        int themeId = sharedPreferences.getInt("ThemeId", R.style.Theme_EYPA_APP);
        setTheme(themeId);
    }

    private void loadData() {
        loadingMask.setVisibility(View.VISIBLE);
        loadingMask.setAlpha(1f);
        String token = UserManager.getInstance(this).getToken();
        
        AuthorInfoRequest request = new AuthorInfoRequest(userId, token);
        apiService.getAuthorInfo(request).enqueue(new Callback<AuthorInfoResponse>() {
            @Override
            public void onResponse(Call<AuthorInfoResponse> call, Response<AuthorInfoResponse> response) {
                hideLoadingMask();
                if (response.isSuccessful() && response.body() != null && response.body().getCode() == 200) {
                    updateUI(response.body().getData());
                } else {
                    Toast.makeText(AuthorProfileActivity.this, "加载失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AuthorInfoResponse> call, Throwable t) {
                hideLoadingMask();
                Toast.makeText(AuthorProfileActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void hideLoadingMask() {
        loadingMask.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction(() -> loadingMask.setVisibility(View.GONE))
                .start();
    }

    private void updateUI(AuthorInfoResponse.Data data) {
        if (data == null) return;

        AuthorInfoResponse.BaseInfo base = data.getBase();
        if (base != null) {
            tvName.setText(base.getName());
            collapsingToolbarLayout.setTitle(base.getName());
            Glide.with(this)
                .load(base.getAvatar())
                .placeholder(R.drawable.ic_avatar_placeholder)
                .error(R.drawable.ic_avatar_placeholder)
                .circleCrop()
                .into(ivAvatar);
            
            if (base.getLevel() != null) {
                tvLevel.setText(base.getLevel().getName());
            }
            
            if (base.getVip() != null) {
                tvVip.setVisibility(View.VISIBLE);
                tvVip.setText(base.getVip().getName());
            } else {
                tvVip.setVisibility(View.GONE);
            }
        }

        AuthorInfoResponse.ProfileInfo profile = data.getProfile();
        if (profile != null) {
            Glide.with(this).load(profile.getCover()).into(ivCover);
            tvDesc.setText(profile.getDesc());
            
            if (profile.getAuth() != null) {
                layoutAuth.setVisibility(View.VISIBLE);
                tvAuthInfo.setText(profile.getAuth().getTitle() + "：" + profile.getAuth().getDesc());
            } else {
                layoutAuth.setVisibility(View.GONE);
            }

            if (profile.getMedals() != null && !profile.getMedals().isEmpty()) {
                layoutMedals.setVisibility(View.VISIBLE);
                tvMedalsCount.setText(String.valueOf(profile.getMedals().size()));
                layoutMedals.setOnClickListener(v -> 
                    MedalsActivity.start(this, new ArrayList<>(profile.getMedals()))
                );
            } else {
                layoutMedals.setVisibility(View.GONE);
            }
        }

        AuthorInfoResponse.StatsInfo stats = data.getStats();
        if (stats != null) {
            viewStats.setStats(
                stats.getViews(),
                0,
                stats.getComments()
            );
        }

        AuthorInfoResponse.InteractionInfo interaction = data.getInteraction();
        if (interaction != null) {
            if (interaction.isMe()) {
                btnAction.setVisibility(View.GONE);
            } else {
                btnAction.setVisibility(View.VISIBLE);
                updateFollowButton(interaction.isFollowing());
                btnAction.setOnClickListener(v -> handleFollowAction());
            }
        }
    }

    private void handleFollowAction() {
        if (!UserManager.getInstance(this).isLoggedIn().getValue()) {
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }

        String token = UserManager.getInstance(this).getToken();
        FollowRequest request = new FollowRequest(token, userId);
        
        btnAction.setEnabled(false);
        apiService.followUser(request).enqueue(new Callback<FollowResponse>() {
            @Override
            public void onResponse(Call<FollowResponse> call, Response<FollowResponse> response) {
                btnAction.setEnabled(true);
                if (response.isSuccessful() && response.body() != null && response.body().getCode() == 200) {
                    FollowResponse.Data data = response.body().getData();
                    if (data != null) {
                        updateFollowButton(data.isFollowing());
                        Toast.makeText(AuthorProfileActivity.this, response.body().getMsg(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(AuthorProfileActivity.this, "操作失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<FollowResponse> call, Throwable t) {
                btnAction.setEnabled(true);
                Toast.makeText(AuthorProfileActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateFollowButton(boolean isFollowing) {
        if (isFollowing) {
            btnAction.setText("已关注");
            btnAction.setTextColor(getResources().getColor(android.R.color.darker_gray));
        } else {
            btnAction.setText("关注");
            android.util.TypedValue typedValue = new android.util.TypedValue();
            getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true);
            btnAction.setTextColor(typedValue.data);
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
