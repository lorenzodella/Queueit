package com.spotify.queueit.requests;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;

import java.io.IOException;
import java.net.URL;

public class RequestImage extends Thread
{
    String sURL;
    ImageListener imageListener;
    Activity context;

    public RequestImage(Activity context, String image) {
        this.context = context;
        this.sURL = image;
    }

    public void setImageListener(ImageListener imageListener){
        this.imageListener = imageListener;
    }

    public void run()
    {
        try {
            Bitmap bmp = BitmapFactory.decodeStream(new URL(sURL).openConnection().getInputStream());

            if(imageListener != null)
                context.runOnUiThread(() -> imageListener.onImageReady(bmp, sURL));

        } catch (IOException e) {
            if(imageListener != null)
                context.runOnUiThread(() -> imageListener.onImageError(sURL.equals("noimage") ? "noimage" : e.getMessage()));
            e.printStackTrace();
        }
    }

    public interface ImageListener {
        void onImageReady(Bitmap image, String sURL);
        void onImageError(String error);
    }
}
