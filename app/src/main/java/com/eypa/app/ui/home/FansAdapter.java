package com.eypa.app.ui.home;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.content.Intent;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.eypa.app.R;
import com.eypa.app.api.ApiClient;
import com.eypa.app.api.ContentApiService;
import com.eypa.app.model.user.FanItem;
import com.eypa.app.model.user.FollowRequest;
import com.eypa.app.model.user.FollowResponse;
import android.widget.ImageView;
import com.eypa.app.utils.UserManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FansAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ITEM = 0;
    private static final int TYPE_FOOTER = 1;

    private List<FanItem> fans = new ArrayList<>();
    private Context context;
    private OnItemClickListener listener;
    private boolean isLoadingFooterVisible = false;

    public interface OnItemClickListener {
        void onItemClick(FanItem fan);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setFans(List<FanItem> fans) {
        this.fans = fans;
        notifyDataSetChanged();
    }

    public void addFans(List<FanItem> newFans) {
        int start = fans.size();
        fans.addAll(newFans);
        notifyItemRangeInserted(start, newFans.size());
    }

    public void setLoadingFooterVisible(boolean visible) {
        if (isLoadingFooterVisible == visible) return;
        isLoadingFooterVisible = visible;
        if (visible) {
            notifyItemInserted(fans.size());
        } else {
            notifyItemRemoved(fans.size());
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (isLoadingFooterVisible && position == fans.size()) {
            return TYPE_FOOTER;
        }
        return TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        if (viewType == TYPE_FOOTER) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_loading_footer, parent, false);
            return new FooterViewHolder(view);
        }
        View view = LayoutInflater.from(context).inflate(R.layout.item_fan, parent, false);
        return new FanViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof FanViewHolder) {
            FanItem fan = fans.get(position);
            ((FanViewHolder) holder).bind(fan);
        }
    }

    @Override
    public int getItemCount() {
        return fans.size() + (isLoadingFooterVisible ? 1 : 0);
    }

    static class FooterViewHolder extends RecyclerView.ViewHolder {
        public FooterViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    class FanViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvName;
        TextView tvLevel;
        TextView tvDesc;
        Button btnFollow;

        public FanViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            tvName = itemView.findViewById(R.id.tv_name);
            tvLevel = itemView.findViewById(R.id.tv_level);
            tvDesc = itemView.findViewById(R.id.tv_desc);
            btnFollow = itemView.findViewById(R.id.btn_follow);

            itemView.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onItemClick(fans.get(getAdapterPosition()));
                }
            });
        }

        void bind(FanItem fan) {
            tvName.setText(fan.getName());
            tvDesc.setText(fan.getDesc());

            Glide.with(context)
                    .load(fan.getAvatar())
                    .transform(new CircleCrop())
                    .placeholder(R.drawable.ic_avatar_placeholder)
                    .error(R.drawable.ic_avatar_placeholder)
                    .into(ivAvatar);

            if (fan.getLevel() != null) {
                tvLevel.setVisibility(View.VISIBLE);
                tvLevel.setText(fan.getLevel().getName());
            } else {
                tvLevel.setVisibility(View.GONE);
            }

            updateFollowButton(fan.isFollowing());

            btnFollow.setOnClickListener(v -> {
                if (!UserManager.getInstance(context).isLoggedIn().getValue()) {
                    context.startActivity(new Intent(context, LoginActivity.class));
                    return;
                }
                
                toggleFollow(fan);
            });
        }

        private void updateFollowButton(boolean isFollowing) {
            if (isFollowing) {
                btnFollow.setText("已关注");
                btnFollow.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray));
                btnFollow.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
            } else {
                btnFollow.setText("关注");
                int primaryColor = getThemeColor(com.google.android.material.R.attr.colorPrimary);
                btnFollow.setTextColor(primaryColor);
                btnFollow.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
            }
        }
        
        private int getThemeColor(int attr) {
            android.util.TypedValue typedValue = new android.util.TypedValue();
            context.getTheme().resolveAttribute(attr, typedValue, true);
            return typedValue.data;
        }

        private void toggleFollow(FanItem fan) {
            btnFollow.setEnabled(false);
            ContentApiService apiService = ApiClient.getClient().create(ContentApiService.class);
            String token = UserManager.getInstance(context).getToken();
            
            int userId;
            try {
                userId = Integer.parseInt(fan.getId());
            } catch (NumberFormatException e) {
                Toast.makeText(context, "无效的用户ID", Toast.LENGTH_SHORT).show();
                btnFollow.setEnabled(true);
                return;
            }

            FollowRequest request = new FollowRequest(token, userId);
            apiService.followUser(request).enqueue(new Callback<FollowResponse>() {
                @Override
                public void onResponse(Call<FollowResponse> call, Response<FollowResponse> response) {
                    btnFollow.setEnabled(true);
            if (response.isSuccessful() && response.body() != null) {
                        if (response.body().getCode() == 200) {
                            boolean newStatus = !fan.isFollowing();
                            fan.setFollowing(newStatus);
                            updateFollowButton(newStatus);
                        }
                    }
                }

                @Override
                public void onFailure(Call<FollowResponse> call, Throwable t) {
                    btnFollow.setEnabled(true);
                }
            });
        }
    }
}
