package com.spotify.queueit.utils;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import androidx.appcompat.content.res.AppCompatResources;

import com.spotify.queueit.R;
import com.spotify.queueit.main.Song;

import java.io.IOException;
import java.util.ArrayList;

public class PreviewPlayer {
    private MediaPlayer player = null;
    private boolean isPlaying;
    private ArrayList<Song> songs;
    private Context context;
    private ImageButton playingButton;
    private ProgressBar progressBar;
    private MediaObserver observer;
    private int playingPos = -1;
    private Song s;

    public PreviewPlayer(Context context, ArrayList<Song> songs){
        this.context = context;
        this.songs = songs;
        isPlaying = false;
    }

    public void onPreviewClick(ImageButton button, Song s, ProgressBar pb){
        this.s = s;
        onPreviewClick(button, -1, pb);
    }

    public void onPreviewClick(ImageButton button, int pos, ProgressBar pb) {
        if(!isPlaying){
            if(pos!=-1)
                s = songs.get(pos);
            Log.d("control", "play");
            playingPos = pos;
            progressBar = pb;
            player = new MediaPlayer();
            observer = new MediaObserver();
            player.setOnCompletionListener(player -> stopPlaying());
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            try {
                player.setDataSource(s.getPreview());
                player.prepare();
                player.start();
                progressBar.setMax(player.getDuration());
                new Thread(observer).start();

                isPlaying = true;
                button.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_baseline_stop_24));
                playingButton = button;
            } catch (IOException e) {
                MessageUtils.showToast(context, "Preview unaviable");
                e.printStackTrace();
            }
        }
        else{
            Log.d("control", "stop");
            int p = playingPos;
            stopPlaying();
            if(p != pos)
                onPreviewClick(button, pos, pb);
        }
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public int getPlayingPos(){
        return playingPos;
    }

    public void setProgressBar(ProgressBar progressBar){
        this.progressBar = progressBar;
        this.progressBar.setMax(player.getDuration());
        this.progressBar.setProgress(player.getCurrentPosition());
    }

    public void setPlayingButton(ImageButton playingButton) {
        this.playingButton = playingButton;
    }

    public void stopPlaying() {
        playingPos = -1;
        playingButton.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_baseline_play_arrow_24));
        isPlaying = false;
        observer.stop();
        progressBar.setProgress(0);
        player.release();
        player = null;
    }

    private class MediaObserver implements Runnable {
        private boolean stop = false;

        public void stop() {
            stop = true;
        }

        @Override
        public void run() {
            while (!stop) {
                try {
                    ((Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //Log.d("progress", ""+player.getCurrentPosition());
                            if(player!=null) progressBar.setProgress(player.getCurrentPosition());
                        }
                    });
                    Thread.sleep(200);
                }
                catch (Exception e) {
                    Log.e("err", "ERRRR");
                }
            }
        }
    }
}
