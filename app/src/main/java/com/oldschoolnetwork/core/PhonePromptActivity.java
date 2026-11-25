package com.oldschoolnetwork.core;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public final class PhonePromptActivity extends Activity {
    private static final String REQUEST = "request";
    private final List<Button> choices = new ArrayList<>();
    private LinearLayout content;
    private TextView status;
    private String requestId;

    static Intent show(Activity activity, JSONObject prompt) {
        return new Intent(activity, PhonePromptActivity.class).putExtra(REQUEST, prompt.toString());
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        JSONObject prompt;
        try {
            prompt = new JSONObject(getIntent().getStringExtra(REQUEST));
            requestId = prompt.getString("id");
            if (prompt.optLong("expires") * 1000 <= System.currentTimeMillis()) {
                finish();
                return;
            }
        } catch (Exception invalid) {
            finish();
            return;
        }
        ScrollView screen = new ScrollView(this);
        screen.setFillViewport(true);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_VERTICAL);
        content.setPadding(dp(24), dp(24), dp(24), dp(24));
        content.setBackgroundResource(R.drawable.bg_core_main);
        screen.addView(content);
        setContentView(screen);
        text("ON Account", 17, 0xff42cbe9);
        text("Confirm a sign-in", 30, Color.WHITE);
        text("Service: " + prompt.optString("client_name"), 16, Color.WHITE);
        text("Requested: " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(new Date(prompt.optLong("issued") * 1000)), 14, 0xffb9c5ce);
        String browser = prompt.optString("browser");
        if (!browser.isEmpty()) text(browser.substring(0, Math.min(140, browser.length())), 13, 0xffb9c5ce);
        text("Choose the number you see on the other screen.", 16, Color.WHITE);
        LinearLayout numbers = new LinearLayout(this);
        content.addView(numbers);
        JSONArray picks = prompt.optJSONArray("picks");
        if (picks != null) for (int i = 0; i < picks.length(); i++) {
            final String number = picks.optString(i);
            Button button = new Button(this);
            button.setText(number);
            button.setContentDescription("Confirm with number " + number);
            button.setOnClickListener(view -> respond(false, number));
            choices.add(button);
            numbers.addView(button, new LinearLayout.LayoutParams(0, dp(55), 1));
        }
        choices.add(action("No, it wasn't me", () -> respond(true, "")));
        status = text("", 14, Color.WHITE);
    }

    private void respond(boolean deny, String number) {
        final JSONObject data = new JSONObject();
        try {
            data.put("request", requestId);
            data.put("decision", deny ? "deny" : "approve");
            data.put("number", number);
            data.put("device", android.os.Build.MODEL);
        } catch (Exception invalid) {
            return;
        }
        for (Button choice : choices) choice.setEnabled(false);
        AccountApi.requestAsync(this, "account_prompt_device", data, (reply, error) -> {
            if (isFinishing()) return;
            if (error != null) {
                status.setText(error);
                for (Button choice : choices) choice.setEnabled(true);
                return;
            }
            choices.clear();
            content.removeAllViews();
            if ("approved".equals(reply.optString("decision"))) {
                text("Sign-in confirmed", 28, Color.WHITE);
                text("Return to the other screen to continue.", 16, Color.WHITE);
            } else {
                text("Sign-in blocked", 28, Color.WHITE);
                text("Review active devices and recent activity, or change your password.", 16, Color.WHITE);
                action("Open account security", () -> open("https://oldschoolnetwork.online/account/edit/"));
                action("Reset password", () -> open("https://oldschoolnetwork.online/account/recovery/#reset"));
            }
            action("Done", this::finish);
        });
    }

    private void open(String url) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
        catch (android.content.ActivityNotFoundException noBrowser) {
            text("Open ON Account in your web browser to review security.", 14, Color.WHITE);
        }
    }

    private TextView text(String value, int size, int color) {
        TextView field = new TextView(this);
        field.setText(value);
        field.setTextSize(size);
        field.setTextColor(color);
        field.setPadding(0, dp(5), 0, dp(8));
        content.addView(field, new LinearLayout.LayoutParams(-1, -2));
        return field;
    }

    private Button action(String label, Runnable callback) {
        Button button = new Button(this);
        button.setText(label);
        button.setOnClickListener(view -> callback.run());
        content.addView(button, new LinearLayout.LayoutParams(-1, -2));
        return button;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
