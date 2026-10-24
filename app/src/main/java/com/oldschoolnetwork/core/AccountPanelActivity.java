package com.oldschoolnetwork.core;

import android.os.Bundle;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.LinkedHashMap;

/** Native controls for the same authenticated dashboard endpoints used by the website. */
public class AccountPanelActivity extends AppCompatActivity {
    private LinearLayout content;
    private String panel;
    private boolean busy;
    private JSONObject lastData;
    protected void onCreate(Bundle state){
        super.onCreate(state);panel=getIntent().getStringExtra("panel");if(panel==null)panel="settings";
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setBackgroundColor(0xff151515);
        LinearLayout toolbar=new LinearLayout(this);toolbar.setGravity(android.view.Gravity.CENTER_VERTICAL);toolbar.setPadding(dp(8),dp(8),dp(16),dp(8));
        ImageButton back=new androidx.appcompat.widget.AppCompatImageButton(this);back.setImageResource(R.drawable.ic_arrow_right);back.setScaleType(ImageView.ScaleType.CENTER_INSIDE);back.setPadding(dp(14),dp(14),dp(14),dp(14));back.setRotation(180);back.setColorFilter(Color.WHITE);back.setBackgroundColor(Color.TRANSPARENT);back.setContentDescription("Back");back.setOnClickListener(v->finish());toolbar.addView(back,new LinearLayout.LayoutParams(dp(48),dp(48)));
        TextView title=label(panel.equals("sessions")?"Active sessions":panel.equals("support")?"Account support":panel.equals("profiles")?"My profiles":"Account preferences",22,0xff00BCD4);toolbar.addView(title);page.addView(toolbar);
        View divider=new View(this);divider.setBackgroundColor(0xff00BCD4);page.addView(divider,new LinearLayout.LayoutParams(-1,dp(2)));
        ScrollView scroll=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(20),dp(16),dp(20),dp(24));scroll.addView(content);page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(page);load();
    }
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private TextView label(String text,int size,int color){TextView view=new TextView(this);view.setText(text);view.setTextSize(size);view.setTextColor(color);view.setPadding(0,dp(6),0,dp(6));return view;}
    private Button button(String text,Runnable action){Button button=new Button(this);button.setText(text);button.setAllCaps(false);button.setOnClickListener(v->action.run());content.addView(button,new LinearLayout.LayoutParams(-1,dp(52)));return button;}
    private void showError(String error){content.removeAllViews();content.addView(label(error,16,Color.LTGRAY));button("Retry",this::load);}
    private void load(){
        content.removeAllViews();content.addView(label("Loading…",16,Color.LTGRAY));
        AccountApi.requestAsync(this,"account_dashboard_data",new JSONObject(),(data,error)->{
            if(isFinishing()||(android.os.Build.VERSION.SDK_INT>=17&&isDestroyed()))return;
            if(error!=null){showError(error);return;}lastData=data;render(data);
        });
    }
    private void render(JSONObject data){content.removeAllViews();if(panel.equals("sessions"))sessions(data.optJSONArray("sessions"));else if(panel.equals("support"))support(data.optJSONArray("tickets"));else if(panel.equals("profiles"))profiles(data.optJSONArray("profiles"));else preferences(data.optJSONObject("settings"));}
    private void sessions(JSONArray sessions){
        content.addView(label("Sessions connected to your ON Account",16,Color.LTGRAY));
        if(sessions==null||sessions.length()==0)content.addView(label("No active sessions were returned.",16,Color.WHITE));
        if(sessions!=null)for(int i=0;i<sessions.length();i++){
            JSONObject session=sessions.optJSONObject(i);if(session==null)continue;
            content.addView(label(session.optString("client_id","ON Account"),18,Color.WHITE));content.addView(label(session.optString("user_agent")+"\n"+session.optString("ip_address")+"\nSigned in: "+session.optString("created_at"),13,Color.LTGRAY));
            button("Terminate session",()->new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Terminate session?").setMessage("This device or browser will need to sign in again.").setNegativeButton("Cancel",null).setPositiveButton("Terminate",(d,w)->{JSONObject request=new JSONObject();try{request.put("session_id",session.optInt("id"));}catch(Exception ignored){}mutate("account_revoke_session",request,this::load);}).show());
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
