package com.spotify.queueit.main;

import android.graphics.Bitmap;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class Song {
    private String name;
    private ArrayList<String> artists;
    private String uri;
    private String preview;
    private String imageUrl;
    private transient Bitmap imageBmp;

    public Song(JSONObject s) throws JSONException {
        name = s.getString("name");
        artists = parseArtists(s.getJSONArray("artists"));
        uri = s.getString("uri");
        preview = s.getString("preview_url");
        Log.d("preview",preview);
        imageUrl = s.getJSONObject("album").getJSONArray("images").getJSONObject(1).getString("url");
        imageBmp = null;
    }

    public Song(String name, JSONArray artists) throws JSONException{
        this.name = name;
        this.artists = parseArtists(artists);
    }

    public Song(String name, ArrayList<String> artists){
        this.name = name;
        this.artists = artists;
    }

    public String getName() {
        return name;
    }

    public ArrayList<String> getArtists() {
        return artists;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Bitmap getImageBmp() {
        return imageBmp;
    }

    public void setImageBmp(Bitmap imageBmp) {
        this.imageBmp = imageBmp;
    }

    public String getPreview() {
        return preview;
    }

    public String getUri() {
        return uri;
    }

    public String getArtistsString() {
        return String.join(", ", artists);
    }

    public ArrayList<String> parseArtists(JSONArray jarr) throws JSONException {
        JSONObject a;
        ArrayList<String> artists = new ArrayList<>();
        for(int i=0; i<jarr.length(); i++){
            a = jarr.getJSONObject(i);
            artists.add(a.getString("name"));
        }
        return artists;
    }
}
