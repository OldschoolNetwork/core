package com.oldschoolnetwork.core;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import org.json.JSONObject;

public final class ServiceDownloadService extends Service {
    private static final int ID=4205;
    private volatile boolean busy;
    private boolean automaticTransfer;
    private NotificationCompat.Builder notification;
    private android.os.PowerManager.WakeLock wakeLock;
    public IBinder onBind(Intent intent){return null;}
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(busy)return START_NOT_STICKY;
        if(intent==null||(intent.getStringExtra("release")==null&&!intent.getBooleanExtra("automatic_check",false)&&!intent.getBooleanExtra("install_ready",false)&&!intent.getBooleanExtra("update_all",false))){stopSelf();return START_NOT_STICKY;}
        busy=true;
        String requestedId="";try{requestedId=new JSONObject(intent.getStringExtra("release")).optString("id");}catch(Exception ignored){}
        final String transferId=requestedId;
        android.os.PowerManager power=(android.os.PowerManager)getSystemService(POWER_SERVICE);
        wakeLock=power.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK,"ONCore:ServiceDownload");wakeLock.acquire(10*60*1000L);
        if(Build.VERSION.SDK_INT>=26)((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(new NotificationChannel("on_service_downloads","Service downloads",NotificationManager.IMPORTANCE_LOW));
        notification=new NotificationCompat.Builder(this,"on_service_downloads").setSmallIcon(android.R.drawable.stat_sys_download).setContentTitle("Downloading ON service").setContentText("Preparing download…").setOngoing(true).setOnlyAlertOnce(true).setProgress(100,0,true);
        startForeground(ID,notification.build());
        new Thread(()->{try{
            ServiceSilentInstaller.recover(this,true);
            if("installing".equals(ServiceTransferState.read(this).getString("phase","")))return;
            if(intent.getBooleanExtra("update_all",false)) {
                java.util.List<JSONObject> releases=ServiceUpdates.fetch();org.json.JSONArray readyItems=new org.json.JSONArray();
                ServiceTransferState.read(this).edit().putBoolean("batch_active",true).putString("batch_ready","[]").commit();
                for(String id:new String[]{"store","core"}){
                    JSONObject release=ServiceUpdates.latest(releases,id);int installed=ServiceUpdates.installed(this,id);
                    if(installed<=0||release==null||release.optInt("version_code")<=installed)continue;
                    transfer(release,false);
                    File file=new File(ServiceTransferState.read(this).getString("path",""));
                    if(CoreInstallation.full(this))installReady(release,file);
                    else {org.json.JSONObject item=new org.json.JSONObject();item.put("release",release);item.put("path",file.getAbsolutePath());readyItems.put(item);ServiceTransferState.read(this).edit().putString("batch_ready",readyItems.toString()).commit();}
                }
            } else if(intent.getBooleanExtra("install_ready",false)) {
                JSONObject release=new JSONObject(ServiceTransferState.read(this).getString("release",""));
                File ready=new File(ServiceTransferState.read(this).getString("path",""));
                installReady(release,ready);
            } else if(intent.getBooleanExtra("automatic_check",false)) {
                if(!CoreInstallation.automatic(this))return;
                java.util.List<JSONObject> releases=ServiceUpdates.fetch();
                notification.setContentText("Services are up to date.");
                // Replacing Core can stop this process, so update Store first.
                for(String id:new String[]{"store","core"}) {
                    if(!CoreInstallation.automatic(this))break;
                    JSONObject release=ServiceUpdates.latest(releases,id);int installed=ServiceUpdates.installed(this,id);
                    if(installed>0&&release!=null&&release.optInt("version_code")>installed)transfer(release,true);
                }
            } else {
                JSONObject release=new JSONObject(intent.getStringExtra("release"));
                transfer(release,false);
                if(intent.getBooleanExtra("manual_install",false)&&CoreInstallation.full(this))installReady(release,new File(ServiceTransferState.read(this).getString("path","")));
            }
        }catch(Exception error){
            android.content.SharedPreferences state=ServiceTransferState.read(this);
            if(!"ready".equals(state.getString("phase","")))ServiceTransferState.write(this,state.getString("id",transferId),"error",0,0,"",error.getMessage());
            notification.setContentTitle("Service update needs attention").setContentText(error.getMessage()).setProgress(0,0,false).setOngoing(false).setAutoCancel(true);
        }finally{
            ServiceTransferState.read(this).edit().putBoolean("batch_active",false).commit();ServiceUpdateBatch.promote(this);
            stopForeground(false);notification.setOngoing(false).setProgress(0,0,false);((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(ID,notification.build());busy=false;if(wakeLock!=null&&wakeLock.isHeld())wakeLock.release();stopSelf();
        }},"on-service-download").start();return START_NOT_STICKY;
    }
    private void transfer(JSONObject requested,boolean automatic)throws Exception {
        automaticTransfer=automatic;
        String id=requested.getString("id");JSONObject current=ServiceUpdates.latest(ServiceUpdates.fetch(),id);
        if(current==null||!current.getString("sha256").equalsIgnoreCase(requested.getString("sha256")))throw new IOException("Release changed. Check for updates again.");
        if(ServiceUpdates.installed(this,id)>=current.getInt("version_code"))return;
        File folder=new File(getCacheDir(),"service-updates");if(!folder.isDirectory()&&!folder.mkdirs())throw new IOException("Cannot create download folder.");
        File partial=new File(folder,id+"-"+current.getInt("version_code")+".part.apk");
        File ready=new File(folder,id+"-"+current.getInt("version_code")+".apk");
        ServiceTransferState.read(this).edit().putString("release",current.toString()).commit();
        notification.setContentTitle(current.optString("name","ON service")).setOngoing(true).setProgress(100,0,true);
        try {
            if(ready.isFile())ServiceApkVerifier.verify(this,current,ready);
            else {
                download(current,partial);ServiceApkVerifier.verify(this,current,partial);
                if(!partial.renameTo(ready))throw new IOException("Cannot finish download.");
            }
        } finally {if(partial.exists())partial.delete();}
        ServiceTransferState.write(this,id,"ready",ready.length(),ready.length(),ready.getAbsolutePath(),"");
        Intent open=new Intent(this,UpdateActivity.class);int flags=PendingIntent.FLAG_UPDATE_CURRENT;if(Build.VERSION.SDK_INT>=23)flags|=PendingIntent.FLAG_IMMUTABLE;
        notification.setContentIntent(PendingIntent.getActivity(this,ID,open,flags)).setAutoCancel(true);
        if(automatic&&CoreInstallation.automatic(this))installReady(current,ready);
        else notification.setContentText("Downloaded and verified. Tap to install.").setProgress(0,0,false).setOngoing(false);
    }
    private void installReady(JSONObject release,File ready)throws Exception {
        notification.setContentTitle(release.optString("name","ON service")).setContentText("Installing…").setProgress(0,0,true).setOngoing(true);
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(ID,notification.build());
        ServiceSilentInstaller.install(this,release,ready);
        notification.setContentText("Update installed.").setProgress(0,0,false).setOngoing(false);
    }
    private void download(JSONObject release,File file)throws Exception{
        URL url=new URL(release.getString("url"));HttpURLConnection connection=null;
        try{
            for(int redirects=0;;redirects++){
                if(!"https".equalsIgnoreCase(url.getProtocol()))throw new IOException("Service downloads require HTTPS.");
                connection=(HttpURLConnection)url.openConnection();connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(10000);connection.setReadTimeout(15000);connection.setRequestProperty("Accept-Encoding","identity");
                int status=connection.getResponseCode();if(status>=300&&status<400){if(redirects>=5)throw new IOException("Too many download redirects.");String location=connection.getHeaderField("Location");if(location==null)throw new IOException("Invalid download redirect.");url=new URL(url,location);connection.disconnect();continue;}if(status!=200)throw new IOException("Download failed (HTTP "+status+").");break;
            }
            long total=release.optLong("size_bytes",0),bytes=0,last=0;
            try(InputStream in=connection.getInputStream();OutputStream out=new FileOutputStream(file)){byte[] buffer=new byte[32768];int count;while((count=in.read(buffer))!=-1){if(automaticTransfer&&!CoreInstallation.automatic(this))throw new IOException("Automatic service updates were disabled.");bytes+=count;if(bytes>200*1024*1024||(total>0&&bytes>total))throw new IOException("Unexpected APK size.");out.write(buffer,0,count);if(System.currentTimeMillis()-last>250){ServiceTransferState.write(this,release.getString("id"),"downloading",bytes,total,"","");notification.setProgress(100,total>0?(int)(bytes*100/total):0,total<=0).setContentText(total>0?(bytes*100/total)+"%":"Downloading…");((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(ID,notification.build());last=System.currentTimeMillis();}}}
            if(total>0&&bytes!=total)throw new IOException("Incomplete service APK.");
        }finally{if(connection!=null)connection.disconnect();}
    }
}
