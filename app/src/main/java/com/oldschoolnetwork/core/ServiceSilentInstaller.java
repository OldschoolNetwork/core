package com.oldschoolnetwork.core;

import android.app.PendingIntent;
import android.content.*;
import android.content.pm.*;
import android.net.Uri;
import android.os.Build;
import java.io.*;
import java.util.*;
import org.json.JSONObject;

/** Persists the transaction before asking Android to replace Core's own process. */
public final class ServiceSilentInstaller {
    public static void install(Context context,JSONObject release,File apk)throws Exception {
        if(!CoreInstallation.full(context))throw new IOException("Full installation is required.");
        File folder=new File(context.getCacheDir(),"service-updates").getCanonicalFile();
        if(!folder.equals(apk.getCanonicalFile().getParentFile()))throw new IOException("Unsupported APK path.");
        ServiceApkVerifier.verify(context,release,apk);
        String nonce=UUID.randomUUID().toString();
        ServiceTransferState.read(context).edit().putString("release",release.toString()).putString("install_nonce",nonce)
            .putLong("install_started",System.currentTimeMillis()).putString("id",release.getString("id"))
            .putString("path",apk.getAbsolutePath()).putString("phase","installing").putString("error","").commit();
        try {
            if(Build.VERSION.SDK_INT>=21 && context.checkCallingOrSelfPermission("android.permission.INSTALL_PACKAGES")==PackageManager.PERMISSION_GRANTED) {
                commitSession(context,apk,nonce);
            } else {
                String path="'"+apk.getCanonicalPath().replace("'","'\\''")+"'";
                Process process=new ProcessBuilder("su","-c","test \"$(id -u)\" = 0 && pm install -r "+path).redirectErrorStream(true).start();
                Timer timeout=new Timer(true);timeout.schedule(new TimerTask(){public void run(){process.destroy();}},90000);
                try {
                    StringBuilder result=new StringBuilder();try(BufferedReader reader=new BufferedReader(new InputStreamReader(process.getInputStream()))){String line;while((line=reader.readLine())!=null)if(result.length()<4000)result.append(line).append('\n');}
                    if(process.waitFor()!=0||!result.toString().contains("Success"))throw new IOException("Root installation was not confirmed. "+result);
                    recover(context,false);
                }finally{timeout.cancel();}
            }
            long deadline=System.currentTimeMillis()+90000;
            while("installing".equals(ServiceTransferState.read(context).getString("phase","")) && System.currentTimeMillis()<deadline) {
                Thread.sleep(300);recover(context,false);
            }
            if("installing".equals(ServiceTransferState.read(context).getString("phase","")))throw new IOException("Installation timed out. Open Updates to retry.");
            if(!"installed".equals(ServiceTransferState.read(context).getString("phase","")))throw new IOException(ServiceTransferState.read(context).getString("error","Android requires confirmation."));
        } catch(Exception error) {
            fail(context,error.getMessage());throw error;
        }
    }
    private static void commitSession(Context context,File apk,String nonce)throws Exception {
        PackageInstaller installer=context.getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams params=new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);params.setSize(apk.length());
        int id=installer.createSession(params);
        try(PackageInstaller.Session session=installer.openSession(id)) {
            try(InputStream in=new FileInputStream(apk);OutputStream out=session.openWrite("base.apk",0,apk.length())) {
                byte[] buffer=new byte[32768];int count;while((count=in.read(buffer))!=-1)out.write(buffer,0,count);session.fsync(out);
            }
            Intent result=new Intent(context,ServiceInstallReceiver.class).setAction("com.oldschoolnetwork.core.SERVICE_INSTALLED").setData(Uri.parse("on-install://result/"+nonce));
            int flags=PendingIntent.FLAG_UPDATE_CURRENT;if(Build.VERSION.SDK_INT>=31)flags|=0x02000000;
            PendingIntent callback=PendingIntent.getBroadcast(context,id,result,flags);
            session.commit(callback.getIntentSender());
        } catch(Exception error){try{installer.abandonSession(id);}catch(Exception ignored){}throw error;}
    }
    public static synchronized void fail(Context context,String message) {
        if(!"installing".equals(ServiceTransferState.read(context).getString("phase","")))return;
        ServiceTransferState.read(context).edit().putString("phase","ready").putString("error",message==null?"Android requires confirmation.":message).remove("install_nonce").commit();
    }
    public static synchronized void recover(Context context,boolean afterRestart) {
        SharedPreferences state=ServiceTransferState.read(context);
        String phase=state.getString("phase","");
        if(!"installing".equals(phase)&&!"ready".equals(phase))return;
        try {
            JSONObject release=new JSONObject(state.getString("release",""));
            PackageInfo installed=context.getPackageManager().getPackageInfo(ServiceUpdates.packageName(release.getString("id")),PackageManager.GET_SIGNATURES);
            if(installed.versionCode>=release.getInt("version_code") && installed.signatures!=null && installed.signatures.length==1
                && ServiceUpdates.CERTIFICATE.equalsIgnoreCase(ServiceApkVerifier.digest(installed.signatures[0].toByteArray()))) {
                state.edit().putString("phase","installed").putString("error","").remove("install_nonce").commit();
                android.app.NotificationManager manager=(android.app.NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
                if(android.os.Build.VERSION.SDK_INT>=26)manager.createNotificationChannel(new android.app.NotificationChannel("on_service_downloads","Service downloads",android.app.NotificationManager.IMPORTANCE_LOW));
                int flags=android.app.PendingIntent.FLAG_UPDATE_CURRENT;if(android.os.Build.VERSION.SDK_INT>=23)flags|=android.app.PendingIntent.FLAG_IMMUTABLE;
                manager.notify(4205,new androidx.core.app.NotificationCompat.Builder(context,"on_service_downloads").setSmallIcon(android.R.drawable.stat_sys_download_done).setContentTitle(release.optString("name","ON service")).setContentText("Update installed.").setAutoCancel(true).setContentIntent(android.app.PendingIntent.getActivity(context,4205,new Intent(context,UpdateActivity.class),flags)).build());
            }else if("installing".equals(phase)&&afterRestart && System.currentTimeMillis()-state.getLong("install_started",0)>120000)fail(context,"Installation was interrupted. The verified APK is ready to retry.");
        }catch(Exception ignored){if(afterRestart)fail(context,"Could not confirm installation. Open Updates to retry.");}
    }
}
