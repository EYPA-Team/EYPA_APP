package com.eypa.app.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.eypa.app.R;

import java.util.List;

public class SearchHistoryAdapter extends RecyclerView.Adapter<SearchHistoryAdapter.ViewHolder> {

    private List<String> historyList;
    private final OnHistoryItemClickListener listener;

    public interface OnHistoryItemClickListener {
        void onItemClick(String keyword);
        void onDeleteClick(String keyword);
    }

    public SearchHistoryAdapter(List<String> historyList, OnHistoryItemClickListener listener) {
        this.historyList = historyList;
        this.listener = listener;
    }

    public void updateData(List<String> newList) {
        this.historyList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String keyword = historyList.get(position);
        holder.tvKeyword.setText(keyword);
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(keyword);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(keyword);
            }
        });
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvKeyword;
        ImageView btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvKeyword = itemView.findViewById(R.id.tv_keyword);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
