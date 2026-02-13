package com.eypa.app.ui.home;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.eypa.app.R;
import com.eypa.app.db.HistoryManager;
import com.eypa.app.model.ContentItem;

import java.util.List;

public class HistoryFragment extends Fragment {

    private static final String ARG_TYPE = "arg_type";
    private int mType;
    private HistoryAdapter adapter;
    private List<ContentItem> historyList;
    private TextView emptyView;
    private RecyclerView recyclerView;

    public static HistoryFragment newInstance(int type) {
        HistoryFragment fragment = new HistoryFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TYPE, type);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mType = getArguments().getInt(ARG_TYPE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        recyclerView = view.findViewById(R.id.recycler_view);
        emptyView = view.findViewById(R.id.tv_empty);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        loadHistory();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadHistory();
    }

    private void loadHistory() {
        if (getContext() == null) return;
        
        historyList = HistoryManager.getInstance(getContext()).getHistory(mType);
        adapter = new HistoryAdapter(historyList);
        recyclerView.setAdapter(adapter);

        updateEmptyState();
    }

    private void updateEmptyState() {
        if (historyList == null || historyList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            if (mType == 0) {
                emptyView.setText("暂无文章浏览历史");
            } else {
                emptyView.setText("暂无帖子浏览历史");
            }
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    public void clearHistory() {
        if (getContext() == null) return;
    }
}
