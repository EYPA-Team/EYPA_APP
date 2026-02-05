package com.eypa.app.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import com.eypa.app.api.ApiClient;
import com.eypa.app.model.UpdateInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UpdateManager {

    private static final String TAG = "UpdateManager";
    private static final String PREFS_NAME = "AppSettings";
    private static final String KEY_SHOW_UPDATE_DIALOG = "ShowUpdateDialog";
    private static final String KEY_IGNORE_VERSION_CODE = "IgnoreVersionCode";

    private final Context context;
    private final SharedPreferences sharedPreferences;
    private AlertDialog progressDialog;
    private ProgressBar progressBar;
    private TextView progressText;
    private final Handler mainHandler;

    public UpdateManager(Context context) {
        this.context = context;
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void checkUpdate() {
        checkUpdate(false);
    }

    public void checkUpdate(boolean isManual) {
        if (isManual) {
            Toast.makeText(context, "正在检查更新...", Toast.LENGTH_SHORT).show();
        }
        ApiClient.getApiService().checkUpdate().enqueue(new Callback<UpdateInfo>() {
            @Override
            public void onResponse(Call<UpdateInfo> call, Response<UpdateInfo> response) {
                if (response.isSuccessful() && response.body() != null) {
                    handleUpdateInfo(response.body(), isManual);
                } else if (isManual) {
                    Toast.makeText(context, "检查更新失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UpdateInfo> call, Throwable t) {
                Log.e(TAG, "Check update failed", t);
                if (isManual) {
                    Toast.makeText(context, "检查更新失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void handleUpdateInfo(UpdateInfo updateInfo, boolean isManual) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            int currentVersionCode = pInfo.versionCode;

            if (updateInfo.getVersionCode() > currentVersionCode) {
                boolean showDialog = sharedPreferences.getBoolean(KEY_SHOW_UPDATE_DIALOG, true);
                int ignoredVersion = sharedPreferences.getInt(KEY_IGNORE_VERSION_CODE, -1);

                if (isManual || updateInfo.isForceUpdate() || (showDialog && updateInfo.getVersionCode() != ignoredVersion)) {
                    showUpdateDialog(updateInfo);
                }
            } else if (isManual) {
                Toast.makeText(context, "当前已是最新版本", Toast.LENGTH_SHORT).show();
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void showUpdateDialog(UpdateInfo updateInfo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("发现新版本: " + updateInfo.getVersionName());
        builder.setMessage(updateInfo.getUpdateLog());
        builder.setPositiveButton("立即更新", (dialog, which) -> {
            showDownloadProgressDialog();
            downloadApk(updateInfo.getDownloadUrl());
        });

        if (!updateInfo.isForceUpdate()) {
            builder.setNegativeButton("暂不更新", null);
            builder.setNeutralButton("不再提醒此版本", (dialog, which) -> {
                sharedPreferences.edit().putInt(KEY_IGNORE_VERSION_CODE, updateInfo.getVersionCode()).apply();
            });
        } else {
            builder.setCancelable(false);
        }

        builder.show();
    }

    private void showDownloadProgressDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("正在下载更新");
        builder.setCancelable(false);

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 40);

        progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        layout.addView(progressBar);

        progressText = new TextView(context);
        progressText.setText("0%");
        progressText.setGravity(Gravity.CENTER);
        progressText.setPadding(0, 20, 0, 0);
        layout.addView(progressText);

        builder.setView(layout);
        progressDialog = builder.create();
        progressDialog.show();
    }

    private void downloadApk(String url) {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(url).build();
                okhttp3.Response response = client.newCall(request).execute();
                ResponseBody body = response.body();

                if (body != null) {
                    long contentLength = body.contentLength();
                    InputStream inputStream = body.byteStream();
                    
                    File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "eypa_update.apk");
                    FileOutputStream outputStream = new FileOutputStream(file);

                    byte[] buffer = new byte[4096];
                    long total = 0;
                    int count;

                    while ((count = inputStream.read(buffer)) != -1) {
                        total += count;
                        outputStream.write(buffer, 0, count);

                        if (contentLength > 0) {
                            int progress = (int) (total * 100 / contentLength);
                            mainHandler.post(() -> {
                                if (progressBar != null) progressBar.setProgress(progress);
                                if (progressText != null) progressText.setText(progress + "%");
                            });
                        }
                    }

                    outputStream.flush();
                    outputStream.close();
                    inputStream.close();

                    mainHandler.post(() -> {
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        installApk(file);
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    Toast.makeText(context, "下载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void installApk(File file) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            uri = Uri.fromFile(file);
        }
        
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        context.startActivity(intent);
    }
}
