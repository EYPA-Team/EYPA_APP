package com.eypa.app.ui.home;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.eypa.app.R;
import com.eypa.app.api.ApiClient;
import com.eypa.app.api.ContentApiService;
import com.eypa.app.model.user.AuthorInfoRequest;
import com.eypa.app.model.user.AuthorInfoResponse;
import com.eypa.app.model.user.UserProfile;
import com.eypa.app.utils.ThemeUtils;
import com.eypa.app.utils.UserManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private SharedPreferences sharedPreferences;
    private boolean isDarkMode = false;
    private ImageView ivAvatar;
    private TextView tvNickname;
    private TextView tvLevel;
    private TextView tvVip;
    private TextView tvDesc;
    private View layoutStats;
    private TextView tvPoints;
    private TextView tvBalance;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        sharedPreferences = requireActivity().getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        isDarkMode = sharedPreferences.getBoolean("DarkMode", false);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ivAvatar = view.findViewById(R.id.iv_avatar);
        tvNickname = view.findViewById(R.id.tv_nickname);
        tvLevel = view.findViewById(R.id.tv_level);
        tvVip = view.findViewById(R.id.tv_vip);
        tvDesc = view.findViewById(R.id.tv_desc);

        ivAvatar.setOnClickListener(v -> {
            UserProfile profile = UserManager.getInstance(requireContext()).getUserProfile().getValue();
            if (profile != null) {
                try {
                    int userId = Integer.parseInt(profile.getId());
                    AuthorProfileActivity.start(requireContext(), userId);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            } else {
                startActivity(new Intent(requireContext(), LoginActivity.class));
            }
        });
        layoutStats = view.findViewById(R.id.layout_stats);
        tvPoints = view.findViewById(R.id.tv_points);
        tvBalance = view.findViewById(R.id.tv_balance);

        view.findViewById(R.id.card_profile).setOnClickListener(v -> {
            if (!UserManager.getInstance(requireContext()).isLoggedIn().getValue()) {
                startActivity(new Intent(requireContext(), LoginActivity.class));
            } else {
                UserProfile profile = UserManager.getInstance(requireContext()).getUserProfile().getValue();
                if (profile != null) {
                    try {
                        int userId = Integer.parseInt(profile.getId());
                        AuthorProfileActivity.start(requireContext(), userId);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        view.findViewById(R.id.card_favorites).setOnClickListener(v -> {
            if (!UserManager.getInstance(requireContext()).isLoggedIn().getValue()) {
                startActivity(new Intent(requireContext(), LoginActivity.class));
            } else {
                startActivity(new Intent(requireContext(), FavoritesActivity.class));
            }
        });

        view.findViewById(R.id.card_history).setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), HistoryActivity.class));
        });

        UserManager.getInstance(requireContext()).getUserProfile().observe(getViewLifecycleOwner(), this::updateProfileUI);
        
        UserManager.getInstance(requireContext()).refreshProfile();
    }

    private void updateProfileUI(UserProfile user) {
        if (user != null) {
            tvNickname.setText(user.getNickname() != null ? user.getNickname() : user.getUsername());
            
            if (user.getLevel() != null && user.getLevel().getName() != null) {
                tvLevel.setText(user.getLevel().getName());
                tvLevel.setVisibility(View.VISIBLE);
            } else {
                tvLevel.setVisibility(View.GONE);
                try {
                    int userId = Integer.parseInt(user.getId());
                    fetchAuthorInfo(userId);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }

            if (user.getVip() != null && user.getVip().getName() != null) {
                tvVip.setText(user.getVip().getName());
                tvVip.setVisibility(View.VISIBLE);
                
                if ("普通用户".equals(user.getVip().getName())) {
                    tvVip.setTextColor(0xFF757575);
                    tvVip.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE0E0E0));
                } else if ("高级通行证".equals(user.getVip().getName())) {
                    tvVip.setTextColor(0xFFFFA000);
                    tvVip.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFFF8E1));
                } else {
                    tvVip.setTextColor(0xFF757575);
                    tvVip.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE0E0E0));
                }
            } else {
                tvVip.setVisibility(View.GONE);
            }

            tvDesc.setText(user.getDesc() != null ? user.getDesc() : "暂无简介");
            
            if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                Glide.with(this)
                    .load(user.getAvatar())
                    .circleCrop()
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(ivAvatar);
            } else {
                ivAvatar.setImageResource(R.drawable.ic_person);
            }

            layoutStats.setVisibility(View.VISIBLE);
            tvPoints.setText("积分: " + user.getPoints());
            tvBalance.setText("余额: " + user.getBalance() + "元");
        } else {
            tvNickname.setText("点击登录/注册");
            tvLevel.setVisibility(View.GONE);
            tvVip.setVisibility(View.GONE);
            tvDesc.setText("登录后享受更多功能");
            ivAvatar.setImageResource(R.drawable.ic_person);
            layoutStats.setVisibility(View.GONE);
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.profile_menu, menu);
        
        // 获取 Toolbar 图标颜色
        TypedValue typedValue = new TypedValue();
        requireActivity().getTheme().resolveAttribute(R.attr.toolbarIconTint, typedValue, true);
        int iconColor = typedValue.data;

        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isNightMode = currentNightMode == Configuration.UI_MODE_NIGHT_YES;

        MenuItem darkModeItem = menu.findItem(R.id.action_dark_mode);
        if (darkModeItem != null) {
            darkModeItem.setIcon(isNightMode ? R.drawable.ic_light_mode : R.drawable.ic_dark_mode);
            if (darkModeItem.getIcon() != null) {
                darkModeItem.getIcon().setTint(iconColor);
            }
        }

        MenuItem settingsItem = menu.findItem(R.id.action_settings);
        if (settingsItem != null && settingsItem.getIcon() != null) {
            settingsItem.getIcon().setTint(iconColor);
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_dark_mode) {
            toggleDarkMode();
            return true;
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(requireContext(), SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleDarkMode() {
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isNightMode = currentNightMode == Configuration.UI_MODE_NIGHT_YES;
        
        boolean newDarkModeState = !isNightMode;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("DarkMode", newDarkModeState);
        editor.putBoolean("DarkModeFollowSystem", false);
        editor.apply();

        isDarkMode = newDarkModeState;
        requireActivity().invalidateOptionsMenu();

        ThemeUtils.applyTheme(requireContext());
    }

    private void fetchAuthorInfo(int userId) {
        String token = UserManager.getInstance(requireContext()).getToken();
        ContentApiService apiService = ApiClient.getClient().create(ContentApiService.class);
        apiService.getAuthorInfo(new AuthorInfoRequest(userId, token)).enqueue(new Callback<AuthorInfoResponse>() {
            @Override
            public void onResponse(Call<AuthorInfoResponse> call, Response<AuthorInfoResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    AuthorInfoResponse.BaseInfo baseInfo = response.body().getData().getBase();
                    if (baseInfo != null) {
                        UserProfile userProfile = UserManager.getInstance(requireContext()).getUserProfile().getValue();
                        if (userProfile == null) return;

                        boolean needsSave = false;

                        if (baseInfo.getLevel() != null) {
                            AuthorInfoResponse.LevelInfo authorLevel = baseInfo.getLevel();
                            
                            tvLevel.setText(authorLevel.getName());
                            tvLevel.setVisibility(View.VISIBLE);
                            
                            if (userProfile != null) {
                                UserProfile.LevelInfo levelInfo = new UserProfile.LevelInfo();
                                levelInfo.setIndex(authorLevel.getIndex());
                                levelInfo.setName(authorLevel.getName());
                                levelInfo.setIcon(authorLevel.getIcon());
                                
                                userProfile.setLevel(levelInfo);
                                needsSave = true;
                            }
                        }

                        if (baseInfo.getVip() != null) {
                            AuthorInfoResponse.VipInfo authorVip = baseInfo.getVip();
                            
                            tvVip.setText(authorVip.getName());
                            tvVip.setVisibility(View.VISIBLE);
                            
                            if ("普通用户".equals(authorVip.getName())) {
                                tvVip.setTextColor(0xFF757575);
                                tvVip.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE0E0E0));
                            } else if ("高级通行证".equals(authorVip.getName())) {
                                tvVip.setTextColor(0xFFFFA000);
                                tvVip.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFFF8E1));
                            }

                            if (userProfile != null) {
                                UserProfile.VipInfo vipInfo = new UserProfile.VipInfo();
                                vipInfo.setLevel(authorVip.getLevel());
                                vipInfo.setName(authorVip.getName());
                                
                                userProfile.setVip(vipInfo);
                                needsSave = true;
                            }
                        }

                        if (userProfile != null && needsSave) {
                            UserManager.getInstance(requireContext()).saveUser(userProfile);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<AuthorInfoResponse> call, Throwable t) {
                // 不做处理
            }
        });
    }
}
