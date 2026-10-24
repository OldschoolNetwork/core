package com.oldschoolnetwork.core;
import android.content.*;
import android.graphics.Color;
import android.view.View;
import android.widget.*;
import org.json.*;
import java.util.*;

final class ServiceUpdateNotices {
    static void attach(androidx.fragment.app.Fragment fragment,LinearLayout host){
        View old=host.findViewWithTag("service-update-notices");if(old!=null)host.removeView(old);
        LinearLayout box=new LinearLayout(fragment.requireContext());box.setTag("service-update-notices");box.setOrientation(1);host.addView(box,0);
        android.content.Context context=fragment.requireContext().getApplicationContext();
        new Thread(()->{try{List<JSONObject> releases=ServiceUpdates.fetch();new android.os.Handler(android.os.Looper.getMainLooper()).post(()->{if(!fragment.isAdded()||fragment.getView()==null)return;
            List<JSONObject> updates=new ArrayList<>();for(String id:new String[]{"store","core"}){JSONObject release=ServiceUpdates.latest(releases,id);int installed=ServiceUpdates.installed(context,id);if(installed>0&&release!=null&&release.optInt("version_code")>installed)updates.add(release);}
            if(updates.isEmpty())return;
            Button all=new Button(fragment.requireContext());all.setText("Update all");all.setAllCaps(false);all.setTextColor(Color.CYAN);all.setBackgroundResource(R.drawable.btn_outline_blue);box.addView(all,new LinearLayout.LayoutParams(-1,dp(context,48)));all.setOnClickListener(v->{start(context,new Intent(context,ServiceDownloadService.class).putExtra("update_all",true));fragment.startActivity(new Intent(fragment.requireContext(),UpdateActivity.class));});
            for(JSONObject release:updates){String key="dismiss_"+release.optString("id")+"_"+release.optInt("version_code");android.content.SharedPreferences prefs=context.getSharedPreferences("ONUpdates",0);if(prefs.getBoolean("hide_update_notices",false)||prefs.getBoolean(key,false))continue;
                LinearLayout card=new LinearLayout(fragment.requireContext());card.setOrientation(1);card.setPadding(dp(context,12),dp(context,12),dp(context,12),dp(context,12));card.setBackgroundResource(R.drawable.btn_outline_blue);LinearLayout.LayoutParams layout=new LinearLayout.LayoutParams(-1,-2);layout.setMargins(0,dp(context,12),0,dp(context,12));box.addView(card,layout);
                LinearLayout heading=new LinearLayout(fragment.requireContext());TextView text=new TextView(fragment.requireContext());text.setText(release.optString("name")+" "+release.optString("version")+" is available");text.setTextColor(Color.CYAN);heading.addView(text,new LinearLayout.LayoutParams(0,-2,1));Button close=new Button(fragment.requireContext());close.setText("×");close.setContentDescription("Dismiss update notice");heading.addView(close,new LinearLayout.LayoutParams(dp(context,48),dp(context,48)));card.addView(heading);close.setOnClickListener(v->{prefs.edit().putBoolean(key,true).apply();box.removeView(card);});
                CheckBox hide=new CheckBox(fragment.requireContext());hide.setText("Don't show update notices again");hide.setTextColor(Color.WHITE);card.addView(hide);hide.setOnCheckedChangeListener((v,checked)->{prefs.edit().putBoolean("hide_update_notices",checked).apply();if(checked)card.setVisibility(View.GONE);});
                Button install=new Button(fragment.requireContext());install.setAllCaps(false);install.setText("Install");install.setTextColor(Color.CYAN);install.setBackgroundResource(R.drawable.btn_outline_blue);card.addView(install,new LinearLayout.LayoutParams(-1,dp(context,48)));install.setOnClickListener(v->{start(context,new Intent(context,ServiceDownloadService.class).putExtra("release",release.toString()).putExtra("manual_install",true));fragment.startActivity(new Intent(fragment.requireContext(),UpdateActivity.class));});
            }
        });}catch(Exception error){android.util.Log.w("ONCore","Could not check update notices",error);}},"on-update-notices").start();
    }
    private static int dp(Context c,int n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
    static void start(Context c,Intent intent){if(android.os.Build.VERSION.SDK_INT>=26)c.startForegroundService(intent);else c.startService(intent);}
}
