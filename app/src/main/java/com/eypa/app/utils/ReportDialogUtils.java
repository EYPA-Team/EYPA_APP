package com.eypa.app.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.eypa.app.R;
import com.eypa.app.api.ApiClient;
import com.eypa.app.model.user.ReportRequest;
import com.eypa.app.model.user.ReportResponse;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReportDialogUtils {

    private static final String[] REASONS = {
            "发布垃圾广告",
            "辱骂/人身攻击",
            "色情/低俗内容",
            "政治敏感",
            "违法违规",
            "其他"
    };

    public static void showReportDialog(Context context, int userId, String url) {
        if (!UserManager.getInstance(context).isLoggedIn().getValue()) {
            Toast.makeText(context, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.layout_report_dialog, null);
        dialog.setContentView(view);

        Spinner spinnerReason = view.findViewById(R.id.spinner_reason);
        EditText etDesc = view.findViewById(R.id.et_desc);
        Button btnSubmit = view.findViewById(R.id.btn_submit);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, REASONS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerReason.setAdapter(adapter);

        spinnerReason.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = REASONS[position];
                if ("其他".equals(selected)) {
                    etDesc.setHint("详细描述 (必填)");
                } else {
                    etDesc.setHint("详细描述 (选填)");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        btnSubmit.setOnClickListener(v -> {
            String reason = (String) spinnerReason.getSelectedItem();
            String desc = etDesc.getText().toString().trim();

            if ("其他".equals(reason) && desc.isEmpty()) {
                etDesc.setError("请填写详细描述");
                return;
            }
            etDesc.setError(null);

            submitReport(context, dialog, userId, reason, desc, url);
        });

        dialog.show();
    }

    private static void submitReport(Context context, BottomSheetDialog dialog, int userId, String reason, String desc, String url) {
        String token = UserManager.getInstance(context).getToken();
        if (token == null) return;

        ReportRequest request = new ReportRequest(token, userId, reason, desc, url);
        ApiClient.getApiService().reportUser(request).enqueue(new Callback<ReportResponse>() {
            @Override
            public void onResponse(Call<ReportResponse> call, Response<ReportResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().isSuccess()) {
                        Toast.makeText(context, "举报成功，我们会尽快处理", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    } else {
                        Toast.makeText(context, response.body().getMsg(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(context, "举报失败，请稍后重试", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ReportResponse> call, Throwable t) {
                Toast.makeText(context, "网络错误", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
