package com.oldschoolnetwork.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public final class AccountApi {
    public interface Callback {
        void complete(JSONObject result, String error);
    }

    interface DetailedCallback {
        void complete(JSONObject result, Exception error);
    }

    static final class SignInRequired extends IOException {
        SignInRequired() {
            super("Sign in to see and manage your account.");
        }
    }

    private AccountApi() {}

    public static JSONObject request(Context context, String action, JSONObject values) throws Exception {
        SharedPreferences prefs = context.getSharedPreferences("ON_SECURE_PREFS", Context.MODE_PRIVATE);
        String id = prefs.getString("on_id", "");
        String token = prefs.getString("server_token", "");
        if (!prefs.getBoolean("logged_in", false) || id.isEmpty() || token.isEmpty()) {
            throw new SignInRequired();
        }
        JSONObject result = requestWithToken(action, values, id, token);
        if (!id.equals(prefs.getString("on_id", "")) || !token.equals(prefs.getString("server_token", ""))) {
            throw new IOException("The active account changed. Try again.");
        }
        return result;
    }

    public static JSONObject requestWithToken(String action, JSONObject values, String id, String token) throws Exception {
        if (id == null || id.isEmpty() || token == null || token.isEmpty()) throw new SignInRequired();
        values.put("on_id", id);
        values.put("token", token);
        HttpURLConnection connection = (HttpURLConnection) new URL(
                "https://api.oldschoolnetwork.online/v1/on-account/api/ecosystem.php?action=" + action).openConnection();
        connection.setInstanceFollowRedirects(false);
        try {
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            try (OutputStream out = connection.getOutputStream()) {
                out.write(values.toString().getBytes("UTF-8"));
            }
            int status = connection.getResponseCode();
            if (status == HttpURLConnection.HTTP_UNAUTHORIZED) throw new SignInRequired();
            if (status >= 500 || (status >= 300 && status < 400)) {
                throw new IOException("ON Account is temporarily unavailable. Try again.");
            }
            InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            if (stream == null) throw new IOException("ON Account is temporarily unavailable. Try again.");
            StringBuilder body = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) body.append(line);
            }
            JSONObject result = new JSONObject(body.toString());
            String state = result.optString("status");
            if (state.equals("guest") || state.equals("unauthorized")) throw new SignInRequired();
            if (status >= 400 || (!state.equals("ok") && !state.equals("success"))) {
                throw new IOException(result.optString("message", state.equals("restricted")
                        ? "This account is restricted." : "ON Account could not complete the request."));
            }
            return result;
        } finally {
            connection.disconnect();
        }
    }

    public static void requestAsync(Context context, String action, JSONObject values, Callback callback) {
        requestAsyncDetailed(context, action, values, (result, error) ->
                callback.complete(result, error == null ? null : error.getMessage() == null
                        ? "Could not connect. Try again." : error.getMessage()));
    }

    static void requestAsyncDetailed(Context context, String action, JSONObject values, DetailedCallback callback) {
        Context app = context.getApplicationContext();
        Handler main = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            try {
                JSONObject result = request(app, action, values);
                main.post(() -> callback.complete(result, null));
            } catch (Exception failure) {
                main.post(() -> callback.complete(null, failure));
            }
        }, "on-account-" + action).start();
    }
}
