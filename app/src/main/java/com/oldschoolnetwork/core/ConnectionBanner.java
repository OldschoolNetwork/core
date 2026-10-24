package com.oldschoolnetwork.core;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.HashMap;
import java.util.Map;

/** Shows the offline state without replacing cached account information. */
public final class ConnectionBanner extends BroadcastReceiver implements Application.ActivityLifecycleCallbacks {
    private final Application app;
    private final Map<Activity,TextView> banners=new HashMap<>();
    private ConnectionBanner(Application app){this.app=app;app.registerActivityLifecycleCallbacks(this);app.registerReceiver(this,new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));}
    public static void initialize(Application app){new ConnectionBanner(app);}
    private boolean connected(){ConnectivityManager manager=(ConnectivityManager)app.getSystemService(Context.CONNECTIVITY_SERVICE);NetworkInfo info=manager==null?null:manager.getActiveNetworkInfo();return info!=null&&info.isConnected();}
    private void update(){boolean online=connected();for(TextView bar:banners.values())bar.setVisibility(online?View.GONE:View.VISIBLE);}
    @Override public void onReceive(Context context,Intent intent){update();}
    @Override public void onActivityCreated(Activity activity,Bundle state){
        if(activity instanceof CropActivity)return;
        ViewGroup content=activity.findViewById(android.R.id.content);
        if(content==null||content.getChildCount()!=1)return;
        View original=content.getChildAt(0);content.removeView(original);
        LinearLayout wrapper=new LinearLayout(activity);wrapper.setOrientation(LinearLayout.VERTICAL);
        TextView bar=new TextView(activity);bar.setBackgroundColor(Color.parseColor("#FFEB3B"));bar.setTextColor(Color.parseColor("#202020"));bar.setTextSize(12);bar.setPadding(12,5,12,5);bar.setText("No connection. Using the latest saved data.");
        wrapper.addView(bar,new LinearLayout.LayoutParams(-1,-2));wrapper.addView(original,new LinearLayout.LayoutParams(-1,0,1));content.addView(wrapper,new ViewGroup.LayoutParams(-1,-1));banners.put(activity,bar);update();
    }
    @Override public void onActivityResumed(Activity activity){update();}
    @Override public void onActivityDestroyed(Activity activity){banners.remove(activity);}
    @Override public void onActivityStarted(Activity activity){}
    @Override public void onActivityPaused(Activity activity){}
    @Override public void onActivityStopped(Activity activity){}
    @Override public void onActivitySaveInstanceState(Activity activity,Bundle state){}
}
