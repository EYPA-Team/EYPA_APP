package com.eypa.app.ui.detail;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.eypa.app.R;
import com.eypa.app.model.ContentItem;
import com.eypa.app.ui.detail.model.ContentBlock;

import java.util.ArrayList;
import java.util.List;

public class DetailContentFragment extends Fragment {

    private DetailViewModel viewModel;
    private RecyclerView recyclerView;
    private ContentAdapter adapter;
    private final List<ContentBlock> contentBlocks = new ArrayList<>();
    private OnEpisodeInteractionListener mListener;
    public interface OnEpisodeInteractionListener {
        void onSwitchEpisode(ContentItem.Episode episode);
    }

    // 当Fragment附加到Activity时，获取Activity的实例作为监听器
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnEpisodeInteractionListener) {
            mListener = (OnEpisodeInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnEpisodeInteractionListener");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_detail_content, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.content_recycler_view);

        // 初始化Adapter时，传入一个Lambda表达式作为点击回调
        adapter = new ContentAdapter(contentBlocks, episode -> {
            if (mListener != null) {
                mListener.onSwitchEpisode(episode);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        // 获取共享的ViewModel并观察数据变化
        viewModel = new ViewModelProvider(requireActivity()).get(DetailViewModel.class);
        viewModel.getPostData().observe(getViewLifecycleOwner(), this::updateContent);
    }

    private void updateContent(ContentItem post) {
        if (post == null) return;

        // 设置Adapter的头部数据
        adapter.setPost(post);

        // 解析文章内容并更新内容块列表
        if (post.getContent() != null && post.getContent().getRendered() != null) {
            List<ContentBlock> parsedBlocks = ContentParser.parse(post.getContent().getRendered());
            contentBlocks.clear();
            contentBlocks.addAll(parsedBlocks);
        }
        // 通知Adapter刷新整个列表
        adapter.notifyDataSetChanged();
    }

    // 当Fragment从Activity分离时，释放监听器引用，防止内存泄漏
    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}