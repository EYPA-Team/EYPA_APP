package com.eypa.app.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.eypa.app.R;
import com.eypa.app.api.ApiClient;
import com.eypa.app.api.ContentApiService;
import com.eypa.app.model.user.LoginResponse;
import com.eypa.app.model.user.RegisterRequest;
import com.eypa.app.model.user.SendCodeRequest;
import com.eypa.app.model.user.UserProfile;
import com.eypa.app.utils.ThemeUtils;
import com.eypa.app.utils.UserManager;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etUsername;
    private TextInputEditText etEmail;
    private TextInputEditText etCode;
    private TextInputEditText etPassword;
    private Button btnSendCode;
    private Button btnRegister;
    private ProgressBar progressBar;
    private ContentApiService apiService;
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applyTheme(this);
        android.content.SharedPreferences sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        int themeId = sharedPreferences.getInt("ThemeId", R.style.Theme_EYPA_APP);
        setTheme(themeId);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        apiService = ApiClient.getClient().create(ContentApiService.class);

        initViews();
        setupToolbar();
        setupListeners();
    }

    private void initViews() {
        etUsername = findViewById(R.id.et_username);
        etEmail = findViewById(R.id.et_email);
        etCode = findViewById(R.id.et_code);
        etPassword = findViewById(R.id.et_password);
        btnSendCode = findViewById(R.id.btn_send_code);
        btnRegister = findViewById(R.id.btn_register);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(R.attr.toolbarIconTint, typedValue, true);
        int iconColor = typedValue.data;
        if (toolbar.getNavigationIcon() != null) {
            toolbar.getNavigationIcon().setTint(iconColor);
        }

        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupListeners() {
        btnSendCode.setOnClickListener(v -> sendCode());
        btnRegister.setOnClickListener(v -> register());
    }

    private void sendCode() {
        String email = etEmail.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("请输入邮箱");
            return;
        }

        btnSendCode.setEnabled(false);
        
        SendCodeRequest request = new SendCodeRequest(email, "signup");
        apiService.sendCode(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();
                    if (loginResponse.isSuccess()) {
                        Toast.makeText(RegisterActivity.this, "验证码已发送", Toast.LENGTH_SHORT).show();
                        startCountDown();
                    } else {
                        btnSendCode.setEnabled(true);
                        Toast.makeText(RegisterActivity.this, loginResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    btnSendCode.setEnabled(true);
                    Toast.makeText(RegisterActivity.this, "发送失败，账号已经注册？", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                btnSendCode.setEnabled(true);
                Toast.makeText(RegisterActivity.this, "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startCountDown() {
        countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                btnSendCode.setText(millisUntilFinished / 1000 + "s");
            }

            @Override
            public void onFinish() {
                btnSendCode.setText("获取验证码");
                btnSendCode.setEnabled(true);
            }
        }.start();
    }

    private void register() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String code = etCode.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username)) {
            etUsername.setError("请输入用户名");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("请输入邮箱");
            return;
        }
        if (TextUtils.isEmpty(code)) {
            etCode.setError("请输入验证码");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("请输入密码");
            return;
        }

        setLoading(true);

        RegisterRequest request = new RegisterRequest(username, email, password, code);
        apiService.register(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();
                    if (loginResponse.isSuccess()) {
                        handleRegisterSuccess(loginResponse.getData());
                    } else {
                        Toast.makeText(RegisterActivity.this, loginResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(RegisterActivity.this, "注册失败，验证码错误？", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                setLoading(false);
                Toast.makeText(RegisterActivity.this, "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleRegisterSuccess(UserProfile user) {
        if (user != null && user.getToken() != null) {
            UserManager.getInstance(this).saveUser(user);
            Toast.makeText(this, "注册成功", Toast.LENGTH_SHORT).show();
            
            Intent intent = new Intent(this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "注册成功，但返回数据异常，请尝试手动登录", Toast.LENGTH_SHORT).show();
        }
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!loading);
        etUsername.setEnabled(!loading);
        etEmail.setEnabled(!loading);
        etCode.setEnabled(!loading);
        etPassword.setEnabled(!loading);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
