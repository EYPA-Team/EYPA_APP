package com.eypa.app.utils;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.signature.ObjectKey;
import com.eypa.app.api.ResourceClient;

/** Reuse images within the current login session, with a separate cache for each identity. */
public final class MediaImages {
    private MediaImages() { }

    @NonNull
    public static RequestManager with(@NonNull Context context) {
        return configure(Glide.with(context), context);
    }

    @NonNull
    public static RequestManager with(@NonNull Fragment fragment) {
        return configure(Glide.with(fragment), fragment.requireContext());
    }

    private static RequestManager configure(RequestManager manager, Context context) {
        // DATA also lets decoders read a complete local file instead of consuming
        // a one-shot HTTP body. Rebinding a row can use memory or that local file.
        return manager.applyDefaultRequestOptions(new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.DATA).skipMemoryCache(false)
                .signature(new ObjectKey(ResourceClient.get(context).imageCacheIdentity())));
    }
}
