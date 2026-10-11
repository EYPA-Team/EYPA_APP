package com.eypa.app.ui.detail;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.DialogFragment;

import com.eypa.app.utils.MediaImages;
import com.eypa.app.R;
import com.eypa.app.utils.ResourceDownloads;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import com.eypa.app.ui.widget.ZoomableImageView;

public class ImageViewerFragment extends DialogFragment {

    private static final String ARG_IMAGE_URL = "image_url";
    private String pendingSaveUrl;
    private final ActivityResultLauncher<String> storagePermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted && pendingSaveUrl != null && isAdded()) saveImageToGallery(pendingSaveUrl);
                else if (isAdded()) Toast.makeText(requireContext(), "保存图片需要存储权限", Toast.LENGTH_SHORT).show();
                pendingSaveUrl = null;
            });

    public static ImageViewerFragment newInstance(String imageUrl) {
        ImageViewerFragment fragment = new ImageViewerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_IMAGE_URL, imageUrl);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_image_viewer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String imageUrl = getArguments() != null ? getArguments().getString(ARG_IMAGE_URL) : null;
        ZoomableImageView zoomableImageView = view.findViewById(R.id.zoomable_image_view);
        ImageView btnClose = view.findViewById(R.id.btn_close);
        ImageView btnSave = view.findViewById(R.id.btn_save);

        if (imageUrl != null) {
            MediaImages.with(this)
                    .load(imageUrl)
                    .into(zoomableImageView);
        }

        btnClose.setOnClickListener(v -> dismiss());
        
        btnSave.setOnClickListener(v -> {
            if (imageUrl != null) {
                saveImageToGallery(imageUrl);
            }
        });
    }

    private void saveImageToGallery(String imageUrl) {
        if (Build.VERSION.SDK_INT < 29 && ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            pendingSaveUrl = imageUrl;
            storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return;
        }
        ResourceDownloads.save(requireContext(), imageUrl, null, true, null);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                window.setBackgroundDrawableResource(android.R.color.black);
                window.setWindowAnimations(R.style.DialogAnimation);

                WindowInsetsControllerCompat windowInsetsController =
                        WindowCompat.getInsetsController(window, window.getDecorView());
                windowInsetsController.setSystemBarsBehavior(
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
                windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
            }
        }
    }
}
