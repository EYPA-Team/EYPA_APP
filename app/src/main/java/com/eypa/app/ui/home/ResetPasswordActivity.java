package com.eypa.app.ui.home;

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
import com.eypa.app.model.user.ResetPasswordRequest;
import com.eypa.app.model.user.SendCodeRequest;
import com.eypa.app.utils.ThemeUtils;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResetPasswordActivity extends AppCompatActivity {

    private TextInputEditText etEmail;
    private TextInputEditText etCode;
    private TextInputEditText etPassword;
    private Button btnSendCode;
    private Button btnResetPassword;
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
        setContentView(R.layout.activity_reset_password);

        apiService = ApiClient.getClient().create(ContentApiService.class);

        initViews();
        setupToolbar();
        setupListeners();
    }

    private void initViews() {
        etEmail = findViewById(R.id.et_email);
        etCode = findViewById(R.id.et_code);
        etPassword = findViewById(R.id.et_password);
        btnSendCode = findViewById(R.id.btn_send_code);
        btnResetPassword = findViewById(R.id.btn_reset_password);
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
        btnResetPassword.setOnClickListener(v -> resetPassword());
    }

    private void sendCode() {
        String email = etEmail.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("请输入邮箱");
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("请输入正确的邮箱格式");
            return;
        }

        btnSendCode.setEnabled(false);
        
        SendCodeRequest request = new SendCodeRequest(email, "reset_password");
        apiService.sendCode(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();
                    if (loginResponse.isSuccess()) {
                        Toast.makeText(ResetPasswordActivity.this, "验证码已发送", Toast.LENGTH_SHORT).show();
                        startCountDown();
                    } else {
                        btnSendCode.setEnabled(true);
                        Toast.makeText(ResetPasswordActivity.this, loginResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    btnSendCode.setEnabled(true);
                    Toast.makeText(ResetPasswordActivity.this, "发送失败，请重试", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                btnSendCode.setEnabled(true);
                Toast.makeText(ResetPasswordActivity.this, "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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

    private void resetPassword() {
        String email = etEmail.getText().toString().trim();
        String code = etCode.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("请输入邮箱");
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("请输入正确的邮箱格式");
            return;
        }
        if (TextUtils.isEmpty(code)) {
            etCode.setError("请输入验证码");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("请输入新密码");
            return;
        }

        setLoading(true);

        ResetPasswordRequest request = new ResetPasswordRequest(email, password, code);
        apiService.resetPassword(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();
                    if (loginResponse.isSuccess()) {
                        Toast.makeText(ResetPasswordActivity.this, "密码修改成功，请重新登录", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(ResetPasswordActivity.this, loginResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(ResetPasswordActivity.this, "重置失败，请重试", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                setLoading(false);
                Toast.makeText(ResetPasswordActivity.this, "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnResetPassword.setEnabled(!loading);
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
