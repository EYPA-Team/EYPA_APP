package com.eypa.app.ui.detail;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.eypa.app.R;
import com.eypa.app.model.ContentItem;
import java.util.List;

public class EpisodesAdapter extends RecyclerView.Adapter<EpisodesAdapter.EpisodeViewHolder> {

    private final List<ContentItem.Episode> episodes;
    private final OnEpisodeClickListener listener;

    public interface OnEpisodeClickListener {
        void onEpisodeClick(ContentItem.Episode episode);
    }

    public EpisodesAdapter(List<ContentItem.Episode> episodes, OnEpisodeClickListener listener) {
        this.episodes = episodes;
        this.listener = listener;
    }

    public void updateData(List<ContentItem.Episode> newEpisodes) {
        this.episodes.clear();
        this.episodes.addAll(newEpisodes);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EpisodeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_episode_chip, parent, false);
        return new EpisodeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EpisodeViewHolder holder, int position) {
        ContentItem.Episode episode = episodes.get(position);
        holder.bind(episode, listener);
    }

    @Override
    public int getItemCount() {
        return episodes.size();
    }

    static class EpisodeViewHolder extends RecyclerView.ViewHolder {
        TextView titleView;

        public EpisodeViewHolder(@NonNull View itemView) {
            super(itemView);
            titleView = (TextView) itemView;
        }

        public void bind(ContentItem.Episode episode, OnEpisodeClickListener listener) {
            titleView.setText(episode.getTitle());
            titleView.setActivated(episode.isPlaying());
            itemView.setOnClickListener(v -> listener.onEpisodeClick(episode));
        }
    }
}