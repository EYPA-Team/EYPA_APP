package com.eypa.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.eypa.app.api.ApiClient;
import com.eypa.app.api.ContentApiService;
import com.eypa.app.model.user.LoginResponse;
import com.eypa.app.model.user.TokenRequest;
import com.eypa.app.model.user.UserProfile;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserManager {
    private static UserManager instance;
    private static final String PREF_NAME = "UserPrefs";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_USER_PROFILE = "user_profile";

    private final SharedPreferences prefs;
    private final MutableLiveData<UserProfile> userProfileLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoggedInLiveData = new MutableLiveData<>();

    private UserManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        loadUserFromPrefs();
    }

    public static synchronized UserManager getInstance(Context context) {
        if (instance == null) {
            instance = new UserManager(context);
        }
        return instance;
    }

    private void loadUserFromPrefs() {
        String token = prefs.getString(KEY_TOKEN, null);
        String profileJson = prefs.getString(KEY_USER_PROFILE, null);

        if (token != null && profileJson != null) {
            try {
                UserProfile profile = new Gson().fromJson(profileJson, UserProfile.class);
                userProfileLiveData.setValue(profile);
                isLoggedInLiveData.setValue(true);
                refreshProfile();
            } catch (Exception e) {
                logout();
            }
        } else {
            isLoggedInLiveData.setValue(false);
        }
    }

    public void saveUser(UserProfile profile) {
        if (profile == null) return;

        UserProfile currentProfile = userProfileLiveData.getValue();
        if (currentProfile != null) {
            if (currentProfile.getLevel() != null && profile.getLevel() == null) {
                profile.setLevel(currentProfile.getLevel());
            }
            if (currentProfile.getVip() != null && profile.getVip() == null) {
                profile.setVip(currentProfile.getVip());
            }
        }
        
        SharedPreferences.Editor editor = prefs.edit();
        if (profile.getToken() != null) {
            editor.putString(KEY_TOKEN, profile.getToken());
        }
        editor.putString(KEY_USER_PROFILE, new Gson().toJson(profile));
        editor.apply();

        userProfileLiveData.setValue(profile);
        isLoggedInLiveData.setValue(true);
    }

    public void logout() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(KEY_TOKEN);
        editor.remove(KEY_USER_PROFILE);
        editor.apply();

        userProfileLiveData.setValue(null);
        isLoggedInLiveData.setValue(false);
    }

    public void refreshProfile() {
        String token = prefs.getString(KEY_TOKEN, null);
        if (token == null) return;

        ContentApiService apiService = ApiClient.getClient().create(ContentApiService.class);
        apiService.getUserProfile(new TokenRequest(token)).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    saveUser(response.body().getData());
                } else {
                    if (response.code() == 401 || (response.body() != null && "invalid_token".equals(response.body().getMessage()))) {
                        logout();
                    }
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                // 不做处理
            }
        });
    }

    public LiveData<UserProfile> getUserProfile() {
        return userProfileLiveData;
    }

    public LiveData<Boolean> isLoggedIn() {
        return isLoggedInLiveData;
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }
}
