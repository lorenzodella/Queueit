package com.spotify.queueit.main;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.airbnb.lottie.LottieAnimationView;
import com.spotify.queueit.R;

public class AddToQueueDialog extends AppCompatDialogFragment {
    Song s;
    ImageView image;
    TextView song, artists, message;
    onAddQueueListener onAddQueueListener;
    AlertDialog myDialog;

    public AddToQueueDialog(Song song){
        this.s = song;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.layout_addtoqueue_dialog, null);

        SharedPreferences sharedPreferences_login = getActivity().getSharedPreferences("Queueit_login", Context.MODE_PRIVATE);
        String user = sharedPreferences_login.getString("username","");

        image = view.findViewById(R.id.song_image);
        image.setImageBitmap(s.getImageBmp());
        song = view.findViewById(R.id.song);
        song.setText(s.getName());
        artists = view.findViewById(R.id.artists);
        artists.setText(s.getArtistsString());

        message = view.findViewById(R.id.message);
        String intro = "Vuoi aggiungere questa canzone alla coda di ";
        String description = intro + user + "?";
        SpannableStringBuilder str = new SpannableStringBuilder(description);
        str.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
                intro.length(), description.length()-1,Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        message.setText(str);

        builder.setView(view)
                .setNegativeButton("annulla", (dialog, which) -> {})
                .setPositiveButton("conferma", (dialog, which) -> {});

        myDialog = builder.create();
        return myDialog;
    }

    public void done(){
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View v = inflater.inflate(R.layout.layout_checkmark_dialog, null);
        LottieAnimationView anim = v.findViewById(R.id.checkmark_anim);
        anim.setMaxProgress(0.6f);
        anim.addAnimatorListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                dismiss();
            }
        });
        myDialog.setContentView(v);
        Window window = myDialog.getWindow();
        window.setDimAmount(0.2f);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    public void setOnAddQueueListener(AddToQueueDialog.onAddQueueListener onAddQueueListener) {
        this.onAddQueueListener = onAddQueueListener;
    }

    public interface onAddQueueListener {
        void addQueue(Song song, AddToQueueDialog dialog);
    }

    @Override
    public void onStart() {
        super.onStart();
        myDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(view -> {
            if(onAddQueueListener != null) onAddQueueListener.addQueue(s, this);
        });
    }
}
