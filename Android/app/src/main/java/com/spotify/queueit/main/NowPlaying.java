package com.spotify.queueit.main;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.palette.graphics.Palette;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.spotify.queueit.utils.PreviewPlayer;
import com.spotify.queueit.R;
import com.spotify.queueit.requests.RequestImage;
import com.spotify.queueit.requests.VolleyRequest;
import com.spotify.queueit.utils.MessageUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class NowPlaying implements View.OnClickListener {
    CardView cardView;
    TextView song_name, artists, lastUpdate;
    ImageButton preview, refresh, close;
    CircularProgressIndicator progressBar;
    ImageView imageView, open;
    PreviewPlayer previewPlayer;
    Song song;
    RequestQueue volleyQueue;
    String token;
    boolean init;

    public NowPlaying(CardView cardView, RequestQueue volleyQueue) {
        this.cardView = cardView;
        this.volleyQueue = volleyQueue;
        open = cardView.findViewById(R.id.open);
        open.setOnClickListener(view -> requestNowPlaying(null));
        init = false;
    }

    private int getPx(int dp){
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                cardView.getResources().getDisplayMetrics()
        );
    }

    public void initLayout(){
        init = true;
        open.setVisibility(View.GONE);
        View.inflate(cardView.getContext(), R.layout.layout_nowplaying, cardView);
        cardView.setRadius(15);
        cardView.setElevation(getPx(10));
        setCardMargins(20);

        song_name = cardView.findViewById(R.id.song);
        artists = cardView.findViewById(R.id.artists);
        imageView = cardView.findViewById(R.id.song_image);
        lastUpdate = cardView.findViewById(R.id.lastupdate);
        refresh = cardView.findViewById(R.id.refresh);
        refresh.setOnClickListener(view -> {
            Animation rotation = AnimationUtils.loadAnimation(cardView.getContext(), R.anim.button_rotate);
            rotation.setRepeatCount(Animation.INFINITE);
            refresh.startAnimation(rotation);
            requestNowPlaying(rotation);
        });
        close = cardView.findViewById(R.id.close);
        close.setOnClickListener(view -> finish());
        progressBar = cardView.findViewById(R.id.progress);
        progressBar.setIndicatorColor(Color.WHITE);
        preview = cardView.findViewById(R.id.preview);
        preview.setOnClickListener(this);
    }

    private void finish() {
        cardView.removeViewsInLayout(1,cardView.getChildCount()-1);
        if(previewPlayer!=null && previewPlayer.isPlaying()) previewPlayer.stopPlaying();
        init = false;
        song = null;
        new Handler().postDelayed(() -> {
            open.setVisibility(View.VISIBLE);
            cardView.setRadius(100);
            cardView.setElevation(getPx(5));
            //cardView.setCardBackgroundColor(cardView.getResources().getColor(R.color.spotify_green));
            setCardMargins(35);
        }, 100);

    }

    private void setCardMargins(int margins){
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        int px = getPx(margins);
        params.setMargins(px,px,px,px);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        params.addRule(RelativeLayout.ALIGN_PARENT_END);
        cardView.setLayoutParams(params);
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setSong(Song s){
        this.song = s;
        if(previewPlayer != null && previewPlayer.isPlaying()) previewPlayer.stopPlaying();

        SimpleDateFormat formatter = new SimpleDateFormat(" dd-MM-yy HH:mm)", Locale.getDefault());
        lastUpdate.setText(cardView.getResources().getString(R.string.lastupdate));
        lastUpdate.append(formatter.format(Calendar.getInstance().getTime()));

        Bitmap image = s.getImageBmp();
        imageView.setImageBitmap(image);
        song_name.setText(s.getName());
        song_name.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        song_name.setSelected(true);
        artists.setText(s.getArtistsString());

        new Palette.Builder(image).generate(palette -> {
            Palette.Swatch swatch = palette.getLightMutedSwatch();
            if(swatch==null) swatch = palette.getLightVibrantSwatch();
            if(swatch==null) swatch = palette.getMutedSwatch();
            if(swatch==null) swatch = palette.getVibrantSwatch();

            cardView.setCardBackgroundColor(swatch.getRgb());
            song_name.setTextColor(swatch.getTitleTextColor());
            artists.setTextColor(swatch.getBodyTextColor());
            lastUpdate.setTextColor(swatch.getBodyTextColor());
            progressBar.setTrackColor(swatch.getTitleTextColor());
            preview.setColorFilter(swatch.getTitleTextColor(), PorterDuff.Mode.SRC_IN);
            refresh.setColorFilter(swatch.getTitleTextColor(), PorterDuff.Mode.SRC_IN);
            close.setColorFilter(swatch.getTitleTextColor(), PorterDuff.Mode.SRC_IN);
        });
    }

    public void requestNowPlaying(Animation animation){
        requestNowPlaying(animation, true);
    }

    public void requestNowPlaying(Animation animation, boolean showMessage) {
        VolleyRequest volleyRequest = new VolleyRequest(volleyQueue, token, "https://api.spotify.com/v1/me/player/currently-playing");
        volleyRequest.get(new VolleyRequest.VolleyCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if(animation!=null) animation.setRepeatCount(0);
                if(response!=null) {
                    try {
                        Song s = new Song(response.getJSONObject("item"));
                        RequestImage img = new RequestImage((Activity) cardView.getContext(), s.getImageUrl());
                        img.setImageListener(new RequestImage.ImageListener() {
                            @Override
                            public void onImageReady(Bitmap image, String sURL) {
                                if(!init) initLayout();
                                if(song==null || !s.getUri().equals(song.getUri())) {
                                    s.setImageBmp(image);
                                    setSong(s);
                                }
                            }

                            @Override
                            public void onImageError(String error) {
                                Log.e("error", error);
                            }
                        });
                        img.start();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    if(showMessage)
                        MessageUtils.showLeftSnackbar(cardView, "Nessuna canzone in riproduzione");
                    onError(null);
                }
            }
            @Override
            public void onError(VolleyError error) {
                if(init)
                    finish();
                if(error!=null && showMessage)
                    VolleyRequest.showError(cardView.getContext(), error);
            }
        });
    }

    public void setPreviewPlayer(PreviewPlayer previewPlayer) {
        this.previewPlayer = previewPlayer;
    }

    @Override
    public void onClick(View view) {
        if(previewPlayer != null) previewPlayer.onPreviewClick((ImageButton)view, song, progressBar);
    }
}
