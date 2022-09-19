package com.spotify.queueit.main;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jakewharton.rxbinding.widget.RxTextView;
import com.spotify.queueit.utils.PreviewPlayer;
import com.spotify.queueit.R;
import com.spotify.queueit.auth.TokenActivity;
import com.spotify.queueit.requests.VolleyRequest;
import com.spotify.queueit.utils.KeyboardEditText;
import com.spotify.queueit.utils.MessageUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements
        RecyclerViewAdapterSongs.ItemClickListener,
        AddToQueueDialog.onAddQueueListener {

    ImageButton cancel;
    KeyboardEditText editText;
    TextView recents, default_message;
    CardView searchView;
    RequestQueue volleyQueue;
    String token;
    RecyclerView recyclerView;
    RecyclerViewAdapterSongs adapter;
    ArrayList<Song> songs;
    Map<String, Song> recentlyAdded;
    PreviewPlayer previewPlayer;
    SharedPreferences sharedPreferences_recentlyadded;
    SharedPreferences sharedPreferences_login;
    NowPlaying nowPlaying;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        volleyQueue = Volley.newRequestQueue(this);
        searchView = findViewById(R.id.commands);
        default_message = findViewById(R.id.default_message);

        sharedPreferences_recentlyadded = getSharedPreferences("Queueit_recentlyadded", MODE_PRIVATE);
        sharedPreferences_login = getSharedPreferences("Queueit_login", MODE_PRIVATE);
        String recentlyAddedJSONString = sharedPreferences_recentlyadded.getString("recentlyAdded", "nosong");

        recents = findViewById(R.id.recents);

        if(recentlyAddedJSONString.equals("nosong")) {
            Log.d("test", "nosong");
            recents.setVisibility(View.GONE);
            default_message.setVisibility(View.VISIBLE);
            recentlyAdded = new LinkedHashMap<>();
        }
        else {
            Log.d("test", "yessong");
            recents.setVisibility(View.VISIBLE);
            default_message.setVisibility(View.GONE);
            Type type = new TypeToken<Map<String, Song>>() {}.getType();
            recentlyAdded = new Gson().fromJson(recentlyAddedJSONString, type);
        }
        songs = new ArrayList<>();

        previewPlayer = new PreviewPlayer(this, songs);

        nowPlaying = new NowPlaying(findViewById(R.id.nowplaying), volleyQueue);
        nowPlaying.setPreviewPlayer(previewPlayer);

        recyclerView = findViewById(R.id.recyclerView_songs);
        setRecyclerView();

        cancel = findViewById(R.id.cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //if( ((InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE)).isAcceptingText() ) {
                //    editText.setText("");
                //} else
                    exitSearch(true);
            }
        });

        recents.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Attenzione!")
                        .setMessage("Vuoi eliminare tutte le canzoni dalle ricerche recenti?")
                        .setNegativeButton("no", (dialog, which) -> {})
                        .setPositiveButton("si", (dialog, which) -> {
                            recents.setVisibility(View.GONE);
                            default_message.setVisibility(View.VISIBLE);
                            recentlyAdded.clear();
                            songs.clear();
                            adapter.notifyDataSetChanged();
                        })
                        .show();
            }
        });

        editText = (KeyboardEditText) findViewById(R.id.edit_search);
        editText.reset(Gravity.CENTER, "Cerca");
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String q = editText.getText().toString();
                    if (!q.trim().isEmpty()){
                        search(q);
                    }
                    return true;
                }
                return false;
            }
        });
        editText.setOnKeyboardListener(new KeyboardEditText.KeyboardListener() {
            @Override
            public void onStateChanged(KeyboardEditText keyboardEditText, boolean showing) {
                if(showing) {
                    ObjectAnimator animator = ObjectAnimator.ofFloat(searchView, "cardElevation", 100);
                    animator.setDuration(300);
                    animator.start();

                    editText.setGravity(Gravity.START);
                    editText.setHint("");
                    cancel.setVisibility(View.VISIBLE);
                }
                else {
                    exitSearch(editText.getText().toString().trim().isEmpty());
                }
            }
        });
        editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    hideKeyboard(v);
                }
            }
        });

        RxTextView.textChanges(editText)
                .debounce(300, TimeUnit.MILLISECONDS)
                .subscribe(textChanged -> {
                    String q = editText.getText().toString();
                    if (q.trim().isEmpty()){
                        songs.clear();
                        songs.addAll(recentlyAdded.values());
                        Collections.reverse(songs);
                        adapter.setCanDeleteSongs(true);
                        runOnUiThread(() -> {
                            if(!recentlyAdded.isEmpty()) {
                                recents.setVisibility(View.VISIBLE);
                                default_message.setVisibility(View.GONE);
                            } else {
                                default_message.setVisibility(View.VISIBLE);
                            }
                            adapter.notifyDataSetChanged();
                        });
                    }
                    else{
                        runOnUiThread(() -> search(q));
                        Log.d("query", q);
                    }
                    if(previewPlayer != null && previewPlayer.getPlayingPos()!=-1 && previewPlayer.isPlaying())
                        runOnUiThread(() -> previewPlayer.stopPlaying());
                });

        token = sharedPreferences_login.getString("token", "");
        nowPlaying.setToken(token);
        nowPlaying.requestNowPlaying(null, false);
    }



    private void setRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                layoutManager.getOrientation());
        recyclerView.addItemDecoration(dividerItemDecoration);
        adapter = new RecyclerViewAdapterSongs(this, songs);
        adapter.setPreviewPlayer(previewPlayer);
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);

        recyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                editText.clearFocus();
                return false;
            }
        });
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (!recyclerView.canScrollVertically(1)) {
                    String q = editText.getText().toString();
                    if (!q.trim().isEmpty() && songs.size()<=40){
                        search(q, songs.size());
                        Log.d("query", q);
                    }
                }
            }
        });
    }

    private void exitSearch(boolean reset){
        int elevation = 30;
        if(reset) {
            elevation = 0;
            cancel.setVisibility(View.GONE);
            editText.reset(Gravity.CENTER, "Cerca");
        }

        ObjectAnimator animator = ObjectAnimator.ofFloat(searchView, "cardElevation", elevation);
        animator.setDuration(300);
        animator.start();
        editText.clearFocus();
    }

    public void hideKeyboard(View v) {
        InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    private void search(String q){
        search(q, 0);
    }

    private void search(String q, int offset){
        VolleyRequest volleyRequest = new VolleyRequest(volleyQueue, token, "https://api.spotify.com/v1/search");
        volleyRequest.setQuery(q, offset);
        volleyRequest.get(new VolleyRequest.VolleyCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                try{
                    recents.setVisibility(View.GONE);
                    default_message.setVisibility(View.GONE);
                    if(offset==0) {
                        songs.clear();
                        adapter.notifyDataSetChanged();
                    }

                    JSONArray items = response.getJSONObject("tracks").getJSONArray("items");
                    JSONObject jobj;
                    for(int i=0; i<items.length(); i++){
                        jobj = items.getJSONObject(i);
                        songs.add(new Song(jobj));
                    }
                    adapter.setCanDeleteSongs(false);
                    adapter.refresh();
                }
                catch (JSONException e){
                    e.printStackTrace();
                }

            }

            @Override
            public void onError(VolleyError error) {
                VolleyRequest.showError(MainActivity.this, error);
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d("test", "machecazzo");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onResume() {
        token = sharedPreferences_login.getString("token", "");
        Log.d("token", token);
        nowPlaying.setToken(token);

        if(sharedPreferences_login.getBoolean("logged", false)) {
            String username = " "+sharedPreferences_login.getString("username", "");
            String intro = getResources().getString(R.string.default_login);
            String description = intro + username;
            SpannableStringBuilder str = new SpannableStringBuilder(description);
            str.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
                    intro.length(), description.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            default_message.setText(str);
        } else
            default_message.setText(getResources().getString(R.string.default_logout));
        super.onResume();
    }

    @Override
    protected void onPause() {
        editText.clearFocus();
        if(previewPlayer != null && previewPlayer.isPlaying())
            previewPlayer.stopPlaying();
        super.onPause();
    }

    @Override
    protected void onStop() {
        if(!recentlyAdded.isEmpty()) {
            String json = new Gson().toJson(recentlyAdded, Map.class);
            Log.d("test", json);
            SharedPreferences.Editor editor = sharedPreferences_recentlyadded.edit();
            editor.putString("recentlyAdded", json);
            editor.commit();
        }
        else {
            sharedPreferences_recentlyadded.edit().clear().apply();
        }
        super.onStop();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.profilo) {
            Intent intent = new Intent(MainActivity.this, TokenActivity.class);
            startActivity(intent);
            return true;
        }
        return false;
    }

    @Override
    public void onItemClick(View view, int position) {
        editText.clearFocus();
        if(sharedPreferences_login.getBoolean("logged",false)) {
            AddToQueueDialog dialog = new AddToQueueDialog(songs.get(position));
            dialog.setOnAddQueueListener(this);
            dialog.show(getSupportFragmentManager(), "addtoqueue_dialog");
        } else {
            MessageUtils.showToast(this, "Not logged in");
        }
    }

    @Override
    public void onItemLongClick(View view, int position) {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        String intro = "Vuoi eliminare ";
        String outro = " dalle ricerche recenti?";
        String description = intro + songs.get(position).getName() + outro;
        SpannableStringBuilder str = new SpannableStringBuilder(description);
        str.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
                intro.length(), description.length()-outro.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        new AlertDialog.Builder(this)
                .setTitle("Attenzione!")
                .setMessage(str)
                .setNegativeButton("no", (dialog, which) -> {})
                .setPositiveButton("si", (dialog, which) -> {
                    recentlyAdded.remove(songs.get(position).getUri());
                    songs.remove(position);
                    adapter.notifyItemRemoved(position);
                    if(recentlyAdded.isEmpty()) {
                        recents.setVisibility(View.GONE);
                        default_message.setVisibility(View.VISIBLE);
                    }
                })
                .show();
    }


    @Override
    public void addQueue(Song song, AddToQueueDialog dialog) {
        recentlyAdded.remove(song.getUri());
        recentlyAdded.put(song.getUri(), song);

        VolleyRequest volleyRequest = new VolleyRequest(volleyQueue, token, "https://api.spotify.com/v1/me/player/queue");
        volleyRequest.setSongUri(song.getUri());
        volleyRequest.post(new VolleyRequest.VolleyCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                dialog.done();
                //MessageUtils.showSnackbar(recyclerView, "Aggiunta ✅");
            }

            @Override
            public void onError(VolleyError error) {
                dialog.dismiss();
                Log.e("err", error.toString());
                VolleyRequest.showError(MainActivity.this, error);
            }
        });
    }
}