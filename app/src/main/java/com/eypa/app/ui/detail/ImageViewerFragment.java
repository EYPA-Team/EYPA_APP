package com.eypa.app.ui.detail;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.eypa.app.R;
import com.eypa.app.ui.widget.ZoomableImageView;

public class ImageViewerFragment extends DialogFragment {

    private static final String ARG_IMAGE_URL = "image_url";

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

        if (imageUrl != null) {
            Glide.with(this)
                    .load(imageUrl)
                    .into(zoomableImageView);
        }

        btnClose.setOnClickListener(v -> dismiss());
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
