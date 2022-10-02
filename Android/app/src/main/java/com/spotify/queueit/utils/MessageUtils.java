package com.spotify.queueit.utils;

import android.content.Context;
import android.view.View;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;
import com.spotify.queueit.R;

public class MessageUtils {
    private static Toast mToast;

    public static void showToast (Context context, String message){
        if (mToast != null)
            mToast.cancel();
        mToast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        mToast.show();
    }

    public static void showDefaultSnackbar(View view, String message){
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
    }

    public static void showLeftSnackbar(View view, String message){
        Snackbar snack = Snackbar.make(view,message,Snackbar.LENGTH_SHORT);
        snack.getView().setBackground(ContextCompat.getDrawable(view.getContext(), R.drawable.container_snackbar));
        snack.show();
    }
}
