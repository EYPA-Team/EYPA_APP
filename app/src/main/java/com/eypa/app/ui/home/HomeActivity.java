package com.eypa.app.ui.home;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.eypa.app.R;
import com.eypa.app.model.user.UserProfile;
import com.eypa.app.utils.ThemeUtils;
import com.eypa.app.utils.UpdateManager;
import com.eypa.app.utils.UserManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class HomeActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;
    private boolean isDarkMode = false;
    private BottomNavigationView bottomNavigationView;
    private HomeFragment homeFragment;
    private ForumFragment forumFragment;
    private ShopFragment shopFragment;
    private ProfileFragment profileFragment;
    private Fragment activeFragment;
    private View toolbarAvatarContainer;
    private ImageView toolbarAvatar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applyTheme(this);
        sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        isDarkMode = sharedPreferences.getBoolean("DarkMode", false);
        applyCustomTheme();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        setupToolbar();

        if (savedInstanceState != null) {
            homeFragment = (HomeFragment) getSupportFragmentManager().findFragmentByTag("HOME");
            forumFragment = (ForumFragment) getSupportFragmentManager().findFragmentByTag("FORUM");
            shopFragment = (ShopFragment) getSupportFragmentManager().findFragmentByTag("SHOP");
            profileFragment = (ProfileFragment) getSupportFragmentManager().findFragmentByTag("PROFILE");
        }

        if (homeFragment == null) homeFragment = new HomeFragment();
        if (forumFragment == null) forumFragment = new ForumFragment();
        if (shopFragment == null) shopFragment = new ShopFragment();
        if (profileFragment == null) profileFragment = new ProfileFragment();

        if (savedInstanceState == null) {
            activeFragment = homeFragment;
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.nav_host_fragment, homeFragment, "HOME")
                    .commit();
        } else {
            Fragment home = getSupportFragmentManager().findFragmentByTag("HOME");
            Fragment forum = getSupportFragmentManager().findFragmentByTag("FORUM");
            Fragment shop = getSupportFragmentManager().findFragmentByTag("SHOP");
            Fragment profile = getSupportFragmentManager().findFragmentByTag("PROFILE");
            if (home != null && !home.isHidden()) activeFragment = home;
            else if (forum != null && !forum.isHidden()) activeFragment = forum;
            else if (shop != null && !shop.isHidden()) activeFragment = shop;
            else if (profile != null && !profile.isHidden()) activeFragment = profile;
            else activeFragment = homeFragment;
        }

        setupBottomNavigation();
        checkUpdate();

        if (toolbarAvatarContainer != null) {
            toolbarAvatarContainer.setVisibility(activeFragment instanceof ProfileFragment ? View.GONE : View.VISIBLE);
        }
    }

    private void checkUpdate() {
        new UpdateManager(this).checkUpdate();
    }

    private void applyCustomTheme() {
        int themeId = sharedPreferences.getInt("ThemeId", R.style.Theme_EYPA_APP);
        setTheme(themeId);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        toolbarAvatarContainer = findViewById(R.id.cv_toolbar_avatar);
        toolbarAvatar = findViewById(R.id.iv_toolbar_avatar);

        toolbarAvatarContainer.setOnClickListener(v -> {
            if (!UserManager.getInstance(this).isLoggedIn().getValue()) {
                startActivity(new Intent(this, LoginActivity.class));
            } else {
                bottomNavigationView.setSelectedItemId(R.id.navigation_profile);
            }
        });

        UserManager.getInstance(this).getUserProfile().observe(this, this::updateToolbarAvatar);
    }

    private void updateToolbarAvatar(UserProfile user) {
        if (user != null && user.getAvatar() != null && !user.getAvatar().isEmpty()) {
            toolbarAvatar.setPadding(0, 0, 0, 0);
            toolbarAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Glide.with(this)
                    .load(user.getAvatar())
                    .circleCrop()
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(toolbarAvatar);
        } else {
            int padding = (int) (4 * getResources().getDisplayMetrics().density);
            toolbarAvatar.setPadding(padding, padding, padding, padding);
            toolbarAvatar.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            toolbarAvatar.setImageResource(R.drawable.ic_person);
        }
    }

    private void setupBottomNavigation() {
        bottomNavigationView = findViewById(R.id.bottom_nav_view);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                switchFragment(homeFragment, "HOME");
                return true;
            } else if (itemId == R.id.navigation_forum) {
                switchFragment(forumFragment, "FORUM");
                return true;
            } else if (itemId == R.id.navigation_shop) {
                switchFragment(shopFragment, "SHOP");
                return true;
            } else if (itemId == R.id.navigation_profile) {
                switchFragment(profileFragment, "PROFILE");
                return true;
            }
            return false;
        });
    }

    private void switchFragment(Fragment targetFragment, String tag) {
        if (activeFragment == targetFragment) return;

        if (activeFragment instanceof HomeFragment) {
            ((HomeFragment) activeFragment).closeSearch();
        }

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.anim.fade_in, R.anim.fade_out);

        if (!targetFragment.isAdded()) {
            transaction.add(R.id.nav_host_fragment, targetFragment, tag);
        }

        if (activeFragment != null) {
            transaction.hide(activeFragment);
        }

        transaction.show(targetFragment).commit();
        activeFragment = targetFragment;

        if (toolbarAvatarContainer != null) {
            toolbarAvatarContainer.setVisibility(targetFragment instanceof ProfileFragment ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public void onBackPressed() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (currentFragment instanceof HomeFragment) {
            if (((HomeFragment) currentFragment).handleBackPressed()) {
                return;
            }
        }
        super.onBackPressed();
    }

    public void setBottomNavigationVisibility(int visibility) {
        if (bottomNavigationView != null) {
            bottomNavigationView.setVisibility(visibility);
        }
    }
}
