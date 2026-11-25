package com.oldschoolnetwork.core;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.widget.*;
import androidx.appcompat.content.res.AppCompatResources;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.LinkedHashMap;

/** Native controls for the same authenticated dashboard endpoints used by the website. */
public class AccountPanelActivity extends Activity {
    private static final int HOLO_BLUE = 0xff33b5e5;
    private static final int SIGN_IN = 201;
    private LinearLayout content;
    private String panel;
    private boolean busy;
    protected void onCreate(Bundle state){
        super.onCreate(state);panel=getIntent().getStringExtra("panel");if(panel==null)panel="settings";
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setBackgroundResource(R.drawable.bg_core_main);
        LinearLayout toolbar=new LinearLayout(this);toolbar.setGravity(android.view.Gravity.CENTER_VERTICAL);toolbar.setPadding(dp(8),dp(8),dp(16),dp(8));
        ImageButton back = new ImageButton(this);
        back.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_arrow_left));
        back.setColorFilter(HOLO_BLUE);
        back.setScaleType(ImageView.ScaleType.FIT_CENTER);
        back.setPadding(dp(15), dp(12), dp(15), dp(12));
        back.setBackgroundResource(R.drawable.bg_holo_selectable);
        back.setContentDescription("Back");
        back.setOnClickListener(v -> finish());
        toolbar.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));
        TextView title = label(panel.equals("sessions") ? "Active sessions"
                : panel.equals("support") ? "Account support"
                : panel.equals("profiles") ? "My profiles" : "Account preferences", 22, HOLO_BLUE);
        toolbar.addView(title);
        page.addView(toolbar);
        View divider = new View(this);
        divider.setBackgroundColor(HOLO_BLUE);
        page.addView(divider, new LinearLayout.LayoutParams(-1, dp(2)));
        ScrollView scroll=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(20),dp(16),dp(20),dp(24));scroll.addView(content);page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(page);load();
    }
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private TextView label(String text,int size,int color){TextView view=new TextView(this);view.setText(text);view.setTextSize(size);view.setTextColor(color);view.setPadding(0,dp(6),0,dp(6));return view;}
    private Button button(String text, Runnable action) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(Color.WHITE);
        button.setBackgroundResource(R.drawable.btn_outline_gray);
        if (action != null) button.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        button.setMinHeight(dp(48));
        params.topMargin = dp(8);
        content.addView(button, params);
        return button;
    }

    private void showError(Exception error) {
        content.removeAllViews();
        boolean signIn = error instanceof AccountApi.SignInRequired;
        String message = error instanceof java.io.IOException ? error.getMessage() : null;
        content.addView(label(message == null ? "Could not load your account. Try again." : message, 16, Color.LTGRAY));
        button(signIn ? "Sign in" : "Retry", signIn ? this::signIn : this::load);
    }

    private void signIn() {
        String onId = getSharedPreferences("ON_SECURE_PREFS", MODE_PRIVATE).getString("on_id", "");
        Intent intent = new Intent(this, OnboardingActivity.class);
        if (!onId.isEmpty()) {
            AccountManager manager = AccountManager.get(this);
            for (Account account : manager.getAccountsByType(ONAccount.ACCOUNT_TYPE)) {
                if (onId.equals(manager.getUserData(account, "on_id"))) {
                    intent.putExtra("renew_account", account.name);
                    break;
                }
            }
            intent.putExtra("renew_login", onId);
        }
        startActivityForResult(intent, SIGN_IN);
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == SIGN_IN && result == RESULT_OK) load();
    }

    private void load() {
        content.removeAllViews();
        content.addView(label("Loading…", 16, Color.LTGRAY));
        String action = panel.equals("sessions") ? "account_sessions" : "account_dashboard_data";
        AccountApi.requestAsyncDetailed(this, action, new JSONObject(), (data, error) -> {
            if (isFinishing() || (android.os.Build.VERSION.SDK_INT >= 17 && isDestroyed())) return;
            if (error != null) {
                showError(error);
                return;
            }
            content.removeAllViews();
            if (panel.equals("sessions")) sessions(data.optJSONArray("sessions"), data.optInt("current", -1));
            else if (panel.equals("support")) support(data.optJSONArray("tickets"));
            else if (panel.equals("profiles")) profiles(data.optJSONArray("profiles"));
            else preferences(data.optJSONObject("settings"));
        });
    }

    private void sessions(JSONArray sessions, int currentId) {
        content.addView(label("Devices and browsers signed in to your ON Account", 16, Color.LTGRAY));
        if (sessions == null || sessions.length() == 0) {
            content.addView(label("No active sessions.", 16, Color.WHITE));
            return;
        }
        for (int i = 0; i < sessions.length(); i++) {
            JSONObject session = sessions.optJSONObject(i);
            if (session == null) continue;
            int id = session.optInt("id", -1);
            boolean current = id == currentId;
            String client = session.optString("client_id");
            String device = session.optString("device", "");
            if (device.isEmpty() || "null".equals(device)) {
                device = "on_core".equals(client) ? "ON Core Services" : "ON Account · " + client;
            }
            content.addView(label(device + (current ? " · This device" : ""), 18, current ? HOLO_BLUE : Color.WHITE));
            String summary = "Signed in: " + session.optString("created_at") + "\nExpires: " + session.optString("expires_at");
            content.addView(label(summary, 14, Color.LTGRAY));
            String details = session.optString("user_agent") + "\nIP address: " + session.optString("ip_address");
            TextView info = label(details, 14, Color.LTGRAY);
            info.setVisibility(View.GONE);
            Button expand = button("Show details", null);
            expand.setOnClickListener(v -> {
                boolean show = info.getVisibility() != View.VISIBLE;
                info.setVisibility(show ? View.VISIBLE : View.GONE);
                expand.setText(show ? "Hide details" : "Show details");
            });
            content.addView(info);
            if (!current && id > 0) {
                button("Sign out this session", () -> new AlertDialog.Builder(this)
                        .setTitle("Sign out this session?")
                        .setMessage("This device or browser will need to sign in again.")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Sign out", (dialog, which) -> {
                            JSONObject request = new JSONObject();
                            try {
                                request.put("session_id", id);
                            } catch (org.json.JSONException impossible) {
                                return;
                            }
                            mutate("account_revoke_session", request, this::load);
                        }).show());
            }
            View divider = new View(this);
            divider.setBackgroundColor(0x364e7897);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(1));
            params.topMargin = dp(14);
            content.addView(divider, params);
        }
    }
    private void profiles(JSONArray profiles){
        content.addView(label("Services and profiles linked to this account",16,Color.LTGRAY));
        if(profiles==null||profiles.length()==0){content.addView(label("No linked service profiles yet.",16,Color.WHITE));return;}
        for(int i=0;i<profiles.length();i++){JSONObject item=profiles.optJSONObject(i);if(item==null)continue;content.addView(label(item.optString("service"),20,Color.CYAN));content.addView(label(item.optString("name"),16,Color.WHITE));String url=item.optString("url");if(url.startsWith("https://"))button("Open profile",()->startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(url))));}
    }
    private void preferences(JSONObject settings){
        JSONObject values=settings==null?new JSONObject():settings;
        LinkedHashMap<String,CheckBox> fields=new LinkedHashMap<>();
        String[][] items={{"cloud_sync","Cloud sync"},{"location_enabled","Location features"},{"analytics_enabled","Usage analytics"},{"smart_auth","SmartAuth protection"},{"notifications_enabled","Account notifications"}};
        content.addView(label("These preferences are synchronized with the ON Account website. Service availability depends on the installed services.",14,Color.LTGRAY));
        for(String[] item:items){CheckBox check=new CheckBox(this);check.setText(item[1]);check.setTextColor(Color.WHITE);check.setChecked(values.optInt(item[0],item[0].equals("analytics_enabled")?0:1)==1);fields.put(item[0],check);content.addView(check,new LinearLayout.LayoutParams(-1,dp(52)));}
        button("Save preferences",()->{JSONObject request=new JSONObject();try{for(String key:fields.keySet())request.put(key,fields.get(key).isChecked()?1:0);}catch(Exception ignored){}mutate("account_update_settings",request,()->Toast.makeText(this,"Preferences saved.",Toast.LENGTH_LONG).show());});
    }
    private void support(JSONArray tickets){
        EditText subject=new EditText(this);subject.setHint("Subject");subject.setTextColor(Color.WHITE);subject.setHintTextColor(Color.GRAY);subject.setSingleLine(true);subject.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(160)});content.addView(subject);
        EditText message=new EditText(this);message.setHint("Describe the problem");message.setTextColor(Color.WHITE);message.setHintTextColor(Color.GRAY);message.setMinLines(3);message.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);message.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(4000)});content.addView(message);
        button("Send support request",()->{if(subject.getText().toString().trim().isEmpty()){subject.setError("Enter a subject");return;}if(message.getText().toString().trim().isEmpty()){message.setError("Describe the problem");return;}JSONObject request=new JSONObject();try{request.put("service","ON Account");request.put("subject",subject.getText().toString().trim());request.put("message",message.getText().toString().trim());}catch(Exception ignored){}mutate("submit_support_ticket",request,this::load);});
        content.addView(label("Your support requests",20,Color.CYAN));
        if(tickets==null||tickets.length()==0)content.addView(label("No support requests yet.",14,Color.LTGRAY));
        if(tickets!=null)for(int i=0;i<tickets.length();i++){JSONObject ticket=tickets.optJSONObject(i);if(ticket==null)continue;content.addView(label(ticket.optString("subject")+" · "+ticket.optString("status"),16,Color.WHITE));content.addView(label(ticket.optString("message"),14,Color.LTGRAY));String reply=ticket.optString("admin_reply","");if(!reply.isEmpty()&&!reply.equals("null"))content.addView(label("Support: "+reply,14,Color.CYAN));}
    }
    private void mutate(String action,JSONObject request,Runnable success){
        if(busy)return;busy=true;setEnabled(content,false);
        AccountApi.requestAsync(this,action,request,(data,error)->{if(isFinishing()||(android.os.Build.VERSION.SDK_INT>=17&&isDestroyed()))return;busy=false;setEnabled(content,true);if(error!=null){Toast.makeText(this,error,Toast.LENGTH_LONG).show();return;}success.run();});
    }
    private void setEnabled(android.view.ViewGroup group,boolean enabled){for(int i=0;i<group.getChildCount();i++){View child=group.getChildAt(i);child.setEnabled(enabled);if(child instanceof android.view.ViewGroup)setEnabled((android.view.ViewGroup)child,enabled);}}
}
