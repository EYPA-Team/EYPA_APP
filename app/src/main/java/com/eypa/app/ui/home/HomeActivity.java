package com.eypa.app.ui.home;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.eypa.app.R;
import com.eypa.app.utils.ThemeUtils;
import com.eypa.app.utils.UpdateManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class HomeActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;
    private boolean isDarkMode = false;
    private BottomNavigationView bottomNavigationView;
    private HomeFragment homeFragment;
    private ProfileFragment profileFragment;
    private Fragment activeFragment;

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
            profileFragment = (ProfileFragment) getSupportFragmentManager().findFragmentByTag("PROFILE");
        }

        if (homeFragment == null) homeFragment = new HomeFragment();
        if (profileFragment == null) profileFragment = new ProfileFragment();

        if (savedInstanceState == null) {
            activeFragment = homeFragment;
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.nav_host_fragment, homeFragment, "HOME")
                    .commit();
        } else {
            Fragment home = getSupportFragmentManager().findFragmentByTag("HOME");
            Fragment profile = getSupportFragmentManager().findFragmentByTag("PROFILE");
            if (home != null && !home.isHidden()) activeFragment = home;
            else if (profile != null && !profile.isHidden()) activeFragment = profile;
            else activeFragment = homeFragment;
        }

        setupBottomNavigation();
        checkUpdate();
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
    }

    private void setupBottomNavigation() {
        bottomNavigationView = findViewById(R.id.bottom_nav_view);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                switchFragment(homeFragment, "HOME");
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
