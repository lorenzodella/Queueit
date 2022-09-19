package com.spotify.queueit;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.spotify.queueit.main.MainActivity;
import com.spotify.queueit.main.Song;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

public class SplashActivity extends AppCompatActivity {
    Map<String, Song> recentlyAdded;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        finish();
    }

    public void requestAllImages(){
        SharedPreferences sharedPreferences_recentlyadded = getSharedPreferences("Queueit_recentlyadded", MODE_PRIVATE);
        String recentlyAddedJSONString = sharedPreferences_recentlyadded.getString("recentlyAdded", "nosong");

        if(!recentlyAddedJSONString.equals("nosong")) {
            recentlyAdded = new LinkedHashMap<>();

        } else {
            Type type = new TypeToken<Map<String, Song>>() {}.getType();
            recentlyAdded = new Gson().fromJson(recentlyAddedJSONString, type);
        }
    }

    @Override
    public void finish() {
        startActivity(new Intent(SplashActivity.this, MainActivity.class));
        super.finish();
    }
}
