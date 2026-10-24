package com.oldschoolnetwork.core;

import android.content.Context;
import android.content.SharedPreferences;

public class ONAccount {

    public static final String ACCOUNT_TYPE = "com.oldschoolnetwork.core";
    public static final String AUTH_TOKEN_TYPE = "on_auth_token";

    private final SharedPreferences prefs;

    public ONAccount(Context context) {
        prefs = context.getSharedPreferences("ON_SECURE_PREFS", Context.MODE_PRIVATE);
    }

    public boolean isLoggedIn() { return prefs.getBoolean("logged_in", false) && !prefs.getString("server_token", "").isEmpty(); }
    public String getEmail()    { return prefs.getString("email", ""); }
    public String getUsername() { return prefs.getString("username", ""); }
    public String getName()     { return prefs.getString("name", ""); }
    public String getSurname()  { return prefs.getString("surname", ""); }
    public String getPhoneNumber(){ return prefs.getString("phone", ""); }
    public String getAvatar()   { return prefs.getString("avatar_base64", ""); }

    // Генерируем красивые ID
    public String getOnId()     { return prefs.getString("on_id", "ON_" + getUsername().toUpperCase()); }
    public String getAtId()     { return "@" + getUsername(); }
    public String getToken()    { return isLoggedIn() ? prefs.getString("server_token", "") : ""; }

    public String getDisplayName() {
        return getUsername() + " (" + getAtId() + ")";
    }

    // --- НОВЫЕ МЕТОДЫ СОХРАНЕНИЯ ---

    public void setAvatar(String base64Image) {
        prefs.edit().putString("avatar_base64", base64Image).commit();
    }

    public void updateProfileInfo(String email, String name, String surname, String username, String phone) {
        SharedPreferences.Editor editor = prefs.edit();
        if (email != null && !email.isEmpty()) editor.putString("email", email);
        if (name != null) editor.putString("name", name);
        if (surname != null) editor.putString("surname", surname);
        if (username != null && !username.isEmpty()) editor.putString("username", username);
        if (phone != null) editor.putString("phone", phone);
        editor.commit();
    }

    public void logout() {
        prefs.edit().clear().commit();
    }
}