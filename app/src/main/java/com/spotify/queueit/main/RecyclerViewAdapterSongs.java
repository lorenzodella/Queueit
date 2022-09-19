package com.spotify.queueit.main;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.spotify.queueit.utils.PreviewPlayer;
import com.spotify.queueit.R;
import com.spotify.queueit.requests.RequestImage;

import java.util.List;

public class RecyclerViewAdapterSongs extends RecyclerView.Adapter<RecyclerViewAdapterSongs.ViewHolder> {

    private List<Song> songs;
    private LayoutInflater mInflater;
    private PreviewPlayer previewPlayer;
    private Context context;
    private ItemClickListener mClickListener;
    private boolean canDeleteSongs;


    // data is passed into the constructor
    public RecyclerViewAdapterSongs(Context context, List<Song> songs) {
        this.context = context;
        this.mInflater = LayoutInflater.from(context);
        this.songs = songs;
        this.canDeleteSongs = false;
    }

    // inflates the row layout from xml when needed
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.recyclerview_row, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Song s = songs.get(position);
        holder.song_name.setText(s.getName());
        holder.artists.setText(s.getArtistsString());
        holder.imageView.setImageBitmap(s.getImageBmp());

        if(s.getImageBmp() == null) {
            RequestImage img = new RequestImage((Activity) context, s.getImageUrl());
            img.setImageListener(new RequestImage.ImageListener() {
                @Override
                public void onImageReady(Bitmap image, String sURL) {
                    s.setImageBmp(image);
                    holder.imageView.setImageBitmap(image);
                }

                @Override
                public void onImageError(String error) {
                    Log.e("error", error);
                }
            });
            img.start();
        }

        if (previewPlayer.getPlayingPos() == position) {
            holder.preview.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_baseline_stop_24));
            previewPlayer.setPlayingButton(holder.preview);
            previewPlayer.setProgressBar(holder.progressBar);
        } else {
            holder.preview.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_baseline_play_arrow_24));
            holder.progressBar.setMax(0);
            holder.progressBar.setProgress(0);
        }
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return songs.size();
    }

    // convenience method for getting data at click position
    Song getItem(int id) {
        return songs.get(id);
    }

    public void setPreviewPlayer(PreviewPlayer player){
        this.previewPlayer = player;
    }

    public void setCanDeleteSongs(boolean canDeleteSongs) {
        this.canDeleteSongs = canDeleteSongs;
    }

    public void refresh(){
        for(int i = getItemCount()-10; i < getItemCount(); i++){
            notifyItemChanged(i);
        }
        //notifyDataSetChanged();
    }

    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        TextView song_name, artists;
        ImageButton preview;
        ProgressBar progressBar;
        ImageView imageView;

        ViewHolder(View itemView) {
            super(itemView);
            song_name = itemView.findViewById(R.id.song);
            artists = itemView.findViewById(R.id.artists);
            imageView = itemView.findViewById(R.id.song_image);
            progressBar = itemView.findViewById(R.id.progress);
            preview = itemView.findViewById(R.id.preview);
            preview.setOnClickListener(this);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if(view instanceof ImageButton){
                if(previewPlayer != null) previewPlayer.onPreviewClick((ImageButton)view, getAdapterPosition(), progressBar);
            }
            else {
                if(mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
            }
        }

        @Override
        public boolean onLongClick(View view) {
            if(canDeleteSongs) {
                if(mClickListener != null) mClickListener.onItemLongClick(view, getAdapterPosition());
            }
            return false;
        }
    }

    public void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    public interface ItemClickListener {
        void onItemClick(View view, int position);
        void onItemLongClick(View view, int position);
    }
}
