package com.eypa.app.ui.home;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.eypa.app.R;
import com.eypa.app.model.user.AuthorInfoResponse;

import java.util.ArrayList;
import java.util.List;

public class MedalsActivity extends AppCompatActivity {

    private static final String EXTRA_MEDALS = "extra_medals";

    public static void start(Context context, ArrayList<AuthorInfoResponse.MedalInfo> medals) {
        Intent intent = new Intent(context, MedalsActivity.class);
        intent.putExtra(EXTRA_MEDALS, medals);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyCustomTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medals);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        RecyclerView rvMedals = findViewById(R.id.rv_medals);
        rvMedals.setLayoutManager(new LinearLayoutManager(this));

        ArrayList<AuthorInfoResponse.MedalInfo> medals = (ArrayList<AuthorInfoResponse.MedalInfo>) getIntent().getSerializableExtra(EXTRA_MEDALS);
        if (medals != null) {
            MedalsAdapter adapter = new MedalsAdapter(medals);
            rvMedals.setAdapter(adapter);
        }
    }
    
    private void applyCustomTheme() {
        android.content.SharedPreferences sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        int themeId = sharedPreferences.getInt("ThemeId", R.style.Theme_EYPA_APP);
        setTheme(themeId);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static class MedalsAdapter extends RecyclerView.Adapter<MedalsAdapter.ViewHolder> {
        private final List<AuthorInfoResponse.MedalInfo> medals;

        public MedalsAdapter(List<AuthorInfoResponse.MedalInfo> medals) {
            this.medals = medals;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_medal_list, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AuthorInfoResponse.MedalInfo medal = medals.get(position);
            holder.tvName.setText(medal.getName());
            if (medal.getDesc() != null && !medal.getDesc().isEmpty()) {
                holder.tvDesc.setText(medal.getDesc());
                holder.tvDesc.setVisibility(View.VISIBLE);
            } else {
                holder.tvDesc.setVisibility(View.GONE);
            }
            
             if (medal.getIcon() != null && !medal.getIcon().isEmpty()) {
                 if (medal.getIcon().toLowerCase().endsWith(".svg")) {
                     Glide.with(holder.itemView.getContext())
                             .as(android.graphics.drawable.PictureDrawable.class)
                             .load(medal.getIcon())
                             .placeholder(R.drawable.ic_medal)
                             .error(R.drawable.ic_medal)
                             .listener(new com.eypa.app.utils.svg.SvgSoftwareLayerSetter())
                             .into(holder.ivIcon);
                 } else {
                     Glide.with(holder.itemView.getContext())
                             .load(medal.getIcon())
                             .placeholder(R.drawable.ic_medal)
                             .error(R.drawable.ic_medal)
                             .into(holder.ivIcon);
                 }
             } else {
                 holder.ivIcon.setImageResource(R.drawable.ic_medal);
             }
        }

        @Override
        public int getItemCount() {
            return medals.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivIcon;
            TextView tvName;
            TextView tvDesc;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivIcon = itemView.findViewById(R.id.iv_medal_icon);
                tvName = itemView.findViewById(R.id.tv_medal_name);
                tvDesc = itemView.findViewById(R.id.tv_medal_desc);
            }
        }
    }
}
