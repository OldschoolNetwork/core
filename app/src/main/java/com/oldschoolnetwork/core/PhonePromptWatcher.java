package com.oldschoolnetwork.core;

import android.app.Activity;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONObject;

final class PhonePromptWatcher {
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Activity activity;
    private final Runnable check = this::check;
    private boolean running;
    private boolean busy;
    private String shown = "";

    PhonePromptWatcher(Activity activity) { this.activity = activity; }

    void start() {
        running = true;
        main.removeCallbacks(check);
        main.post(check);
    }

    void stop() {
        running = false;
        main.removeCallbacks(check);
    }

    private void check() {
        if (!running || busy) return;
        if (!new ONAccount(activity).isLoggedIn()) {
            main.postDelayed(check, 8000);
            return;
        }
        JSONObject data = new JSONObject();
        try { data.put("device", Build.MANUFACTURER + " " + Build.MODEL); }
        catch (org.json.JSONException ignored) { return; }
        busy = true;
        AccountApi.requestAsync(activity, "account_prompt_device", data, (reply, error) -> {
            busy = false;
            if (!running || activity.isFinishing()) return;
            if (reply != null) {
                final JSONObject prompt = reply.optJSONObject("prompt");
                if (prompt != null && !shown.equals(prompt.optString("id"))) {
                    shown = prompt.optString("id");
                    activity.startActivity(PhonePromptActivity.show(activity, prompt));
                    return;
                }
            }
            main.postDelayed(check, error == null ? 5000 : 15000);
        });
    }
}
