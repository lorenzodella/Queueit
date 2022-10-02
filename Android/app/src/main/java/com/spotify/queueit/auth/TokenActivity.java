package com.spotify.queueit.auth;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.*;
import com.android.volley.toolbox.Volley;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.spotify.queueit.R;
import com.spotify.queueit.requests.RequestImage;
import com.spotify.queueit.requests.VolleyRequest;
import com.spotify.queueit.utils.MessageUtils;
import com.spotify.sdk.android.auth.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TokenActivity extends AppCompatActivity implements RequestImage.ImageListener {

    private static final String CLIENT_ID = "8844631caa214f8eba751cab4f7ed774";
    private static final String REDIRECT_URI = "com.spotify.queueit://callback";
    private static final String SCOPES = "user-read-recently-played,user-read-private,user-read-email,user-modify-playback-state,user-read-currently-playing";

    Button login, logout;
    TextView tokenText, userName, userState, userLetter;
    ImageView tokenQR, userImage, userCircle;
    Toast toast;

    private SharedPreferences.Editor editor;
    private SharedPreferences sharedPreferences_login;
    RequestQueue volleyQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_token);

        volleyQueue = Volley.newRequestQueue(this);

        sharedPreferences_login = getSharedPreferences("Queueit_login", MODE_PRIVATE);
        Log.d("test", String.valueOf(sharedPreferences_login.getBoolean("logged", false)));

        userName = findViewById(R.id.user_name);
        userImage = findViewById(R.id.user_image);
        userState = findViewById(R.id.user_state);
        userCircle = findViewById(R.id.user_circle);
        userLetter = findViewById(R.id.user_letter);

        logout = findViewById(R.id.logout);
        logout.setOnClickListener(view -> {
            if(sharedPreferences_login.getBoolean("logged", false))
                showLogoutAlert();
            else
                logout();
        });

        login = findViewById(R.id.login);
        login.setOnClickListener(view -> authenticateSpotify(false));

        ActivityResultLauncher<Intent> startActivityForResult = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                        Intent data = result.getData();
                        String token = data.getStringExtra("token");
                        getUser(token);
                        setTokenView(token);
                        userState.setText(getResources().getString(R.string.shared));
                        userCircle.setColorFilter(Color.rgb(0, 0, 255));
                    }
                }
        );
        tokenQR = findViewById(R.id.tokenQR);
        tokenQR.setOnClickListener(view -> {
            if(sharedPreferences_login.getBoolean("logged", false))
                MessageUtils.showDefaultSnackbar(tokenQR, "Effettua il logout per scannerizzare un nuovo QR code");
            else {
                Intent mIntent = new Intent(TokenActivity.this, ScanActivity.class);
                startActivityForResult.launch(mIntent);
            }
        });

        tokenText = findViewById(R.id.tokenText);
        String token = sharedPreferences_login.getString("token", getResources().getString(R.string.taptoscan));
        setTokenView(token);
        userName.setText(sharedPreferences_login.getString("username", ""));
        String imageUrl = sharedPreferences_login.getString("userimage", null);
        if(imageUrl != null) {
            RequestImage img = new RequestImage(TokenActivity.this, imageUrl);
            img.setImageListener(this);
            img.start();
        }
        else
            userImage.setImageDrawable(AppCompatResources.getDrawable(TokenActivity.this, R.drawable.user));
        if(sharedPreferences_login.getBoolean("logged", false)) {
            login.setText("Refresh token");
            userState.setText(getResources().getString(R.string.login));
            userCircle.setColorFilter(Color.rgb(0, 255, 0));
        }
        else if(!token.equals(getResources().getString(R.string.taptoscan))){
            userState.setText(getResources().getString(R.string.shared));
            userCircle.setColorFilter(Color.rgb(0, 0, 255));
        }
    }

    private void login(String token){
        getUser(token);
        setTokenView(token);
        login.setText("Refresh token");
        userState.setText(getResources().getString(R.string.login));
        userCircle.setColorFilter(Color.rgb(0, 255, 0));
        editor = sharedPreferences_login.edit();
        editor.putBoolean("logged", true);
        editor.apply();
    }

    private void logout(){
        userName.setText("");
        userState.setText(getResources().getString(R.string.logout));
        userCircle.setColorFilter(Color.rgb(255, 0, 0));
        userLetter.setText("");
        userImage.setImageDrawable(AppCompatResources.getDrawable(TokenActivity.this, R.drawable.user));
        login.setText("Login");
        tokenQR.setImageDrawable(AppCompatResources.getDrawable(TokenActivity.this, R.drawable.tap));
        tokenText.setText(R.string.taptoscan);
        sharedPreferences_login.edit().clear().apply();
    }

    private void showLogoutAlert(){
        new AlertDialog.Builder(this)
                .setTitle("Attenzione!")
                .setMessage(getResources().getString(R.string.logout_alert))
                .setNegativeButton("annulla", (dialog, which) -> {

                })
                .setPositiveButton("ok", (dialog, which) -> {
                    final Handler handler = new Handler(Looper.getMainLooper());
                    handler.postDelayed(() -> logout(), 500);
                    authenticateSpotify(true);
                }).show();
    }

    private void authenticateSpotify(boolean forceShowDialog) {
        AuthorizationRequest.Builder builder =
                new AuthorizationRequest.Builder(CLIENT_ID, AuthorizationResponse.Type.TOKEN, REDIRECT_URI);

        builder.setShowDialog(forceShowDialog);
        builder.setScopes(new String[]{SCOPES});
        AuthorizationRequest request = builder.build();

        //AuthorizationClient.openLoginActivity(this, code, request);
        AuthorizationClient.openLoginInBrowser(this, request);
    }

    private void setTokenView(String token){
        if(!token.equals(getResources().getString(R.string.taptoscan))) {
            MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
            try {
                BitMatrix bitMatrix = multiFormatWriter.encode(token,
                        BarcodeFormat.QR_CODE,
                        800,
                        800);
                BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);

                tokenQR.setImageBitmap(bitmap);
                //getUser(token);
            } catch (WriterException e) {
                e.printStackTrace();
            }
        }
        tokenText.setText(token);
        editor = sharedPreferences_login.edit();
        editor.putString("token", token);
        editor.apply();
    }

    private void getUser(String token){
        VolleyRequest volleyRequest = new VolleyRequest(volleyQueue, token);
        volleyRequest.get(new VolleyRequest.VolleyCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                try{
                    String name = response.getString("display_name");
                    userName.setText(name);

                    JSONArray images = response.getJSONArray("images");
                    String imageUrl = images.isNull(0)
                            ? "noimage"
                            : images.getJSONObject(0).getString("url");

                    RequestImage img = new RequestImage(TokenActivity.this, imageUrl);
                    img.setImageListener(TokenActivity.this);
                    img.start();

                    editor = sharedPreferences_login.edit();
                    editor.putString("userimage", imageUrl);
                    editor.putString("username", name);
                    editor.apply();
                }
                catch (JSONException e){
                    e.printStackTrace();
                }

            }

            @Override
            public void onError(VolleyError error) {
                VolleyRequest.showErrorAndExecuteCommand(TokenActivity.this, error, () -> logout());
            }
        });
    }

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Uri uri = intent.getData();
        Log.d("test","3");
        if (uri != null) {
            AuthorizationResponse response = AuthorizationResponse.fromUri(uri);

            Log.d("test","4");
            switch (response.getType()) {
                // Response was successful and contains auth token
                case TOKEN:
                    Log.d("success","token");
                    login(response.getAccessToken());
                    break;

                // Auth flow returned an error
                case ERROR:
                    Log.d("error",response.getError());
                    break;

                // Most likely auth flow was cancelled
                default:
                    Log.d("exit",""+response.getType());
            }
        }
    }

    @Override
    public void onImageReady(Bitmap image, String sURL) {
        userImage.setImageBitmap(image);
    }

    @Override
    public void onImageError(String error) {
        if(error.equals("noimage")) {
            Bitmap bpm = Bitmap.createBitmap(60,60,Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bpm);
            canvas.drawColor(getResources().getColor(R.color.green));
            userLetter.setText(String.valueOf(userName.getText().charAt(0)));
            onImageReady(bpm, error);
        }
    }

    /*@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        Log.d("test","1");

        // Check if result comes from the correct activity
        if (requestCode == LOGIN_CODE) {
            AuthorizationResponse response = AuthorizationClient.getResponse(resultCode, intent);

            Log.d("test","2");
            switch (response.getType()) {
                // Response was successful and contains auth token
                case TOKEN:
                    Log.d("success","token");
                    text.setText(response.getAccessToken());
                    login.setText("Refresh token");
                    break;

                // Auth flow returned an error
                case ERROR:
                    Log.d("error",response.getError());
                    break;

                // Most likely auth flow was cancelled
                default:
                    Log.d("exit",""+response.getType());
            }
        }
        else if (requestCode == LOGOUT_CODE){
            text.setText("token");
            login.setText("Login");
        }
    }*/

}