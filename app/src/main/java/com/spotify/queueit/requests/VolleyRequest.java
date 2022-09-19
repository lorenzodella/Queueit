package com.spotify.queueit.requests;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.spotify.queueit.utils.MessageUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class VolleyRequest {
    RequestQueue queue;
    String token;
    String url;
    String songUri;

    public VolleyRequest(RequestQueue queue, String token){
        this.queue = queue;
        this.token = token;
        this.url = "https://api.spotify.com/v1/me";
    }

    public VolleyRequest(RequestQueue queue, String token, String url){
        this.queue = queue;
        this.token = token;
        this.url = url;
    }

    public void setQuery(String q, int offset){
        url += "?q=" + q + "&type=track&market=IT&limit=10&offset=" + offset;
    }

    public void setSongUri(String songUri) {
        url += "?uri=" + songUri;
    }

    public interface VolleyCallback {
        void onSuccess(JSONObject response);
        void onError(VolleyError error);
    }

    public void get(final VolleyCallback callback) {
        request(callback, Request.Method.GET);
    }

    public void post(final VolleyCallback callback){
        request(callback, Request.Method.POST);
    }

    private void request(final VolleyCallback callback, int method) {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (method, url, null, callback::onSuccess, callback::onError) {

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                String auth = "Bearer " + token;
                headers.put("Authorization", auth);
                return headers;
            }

            @Override
            protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                try {
                    String jsonString = new String(response.data,
                            HttpHeaderParser.parseCharset(response.headers, PROTOCOL_CHARSET));

                    JSONObject result = null;

                    if (jsonString.length() > 0)
                        result = new JSONObject(jsonString);

                    return Response.success(result,
                            HttpHeaderParser.parseCacheHeaders(response));
                } catch (UnsupportedEncodingException | JSONException e) {
                    return Response.error(new ParseError(e));
                }
            }
        };
        jsonObjectRequest.setShouldCache(false);
        queue.add(jsonObjectRequest);
    }

    public static void showError(Context context, VolleyError error){
        showErrorAndExecuteCommand(context, error, null);
    }

    public static void showErrorAndExecuteCommand(Context context, VolleyError error, @Nullable Command command){
        try {
            String body = new String(error.networkResponse.data,"UTF-8");
            Log.e("error", body);
            JSONObject data = new JSONObject(body);

            if(command!=null) command.executeCommand();

            MessageUtils.showToast(context, data.getJSONObject("error").getString("message"));
        } catch (UnsupportedEncodingException | JSONException e) {
            e.printStackTrace();
        }
    }

    public interface Command{
        void executeCommand();
    }
}
