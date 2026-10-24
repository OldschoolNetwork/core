package com.oldschoolnetwork.core;
import android.app.*;
import android.content.*;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import org.json.JSONObject;

public final class UpdateCheckReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){
        if(!context.getSharedPreferences("ONUpdates",0).getBoolean("enabled",true))return;
        if(CoreInstallation.automatic(context)) {
            Intent service=new Intent(context,ServiceDownloadService.class).putExtra("automatic_check",true);
            if(Build.VERSION.SDK_INT>=26)context.startForegroundService(service);else context.startService(service);
            return;
        }
        PendingResult pending=goAsync();new Thread(()->{
            try{
                java.util.List<JSONObject> releases=ServiceUpdates.fetch();StringBuilder names=new StringBuilder();
                for(String id:new String[]{"core","store"}){JSONObject release=ServiceUpdates.latest(releases,id);int installed=ServiceUpdates.installed(context,id);if(installed>0&&release!=null&&release.optInt("version_code")>installed){if(names.length()>0)names.append(", ");names.append(release.optString("name"));}}
                if(names.length()>0){
                    if(Build.VERSION.SDK_INT>=26)((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(new NotificationChannel("on_service_updates","Service updates",NotificationManager.IMPORTANCE_DEFAULT));
                    int flags=PendingIntent.FLAG_UPDATE_CURRENT;if(Build.VERSION.SDK_INT>=23)flags|=PendingIntent.FLAG_IMMUTABLE;
                    NotificationCompat.Builder notification=new NotificationCompat.Builder(context,"on_service_updates").setSmallIcon(android.R.drawable.stat_notify_sync).setContentTitle("ON service updates available").setContentText(names.toString()).setAutoCancel(true).setContentIntent(PendingIntent.getActivity(context,4204,new Intent(context,UpdateActivity.class),flags));
                    NotificationManagerCompat.from(context).notify(4204,notification.build());
                }
            }catch(Exception ignored){}finally{pending.finish();}
        },"on-update-check").start();
    }
}
