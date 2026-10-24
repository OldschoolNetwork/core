package com.oldschoolnetwork.core;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import org.json.JSONObject;
import java.io.File;
import java.util.List;

public class UpdateActivity extends AppCompatActivity {
    private TextView status;
    private boolean loading;
    private boolean androidInstaller;
    private File downloaded;
    private final android.os.Handler transferHandler=new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable transferPoll=new Runnable(){public void run(){renderTransfer();transferHandler.postDelayed(this,400);}};
    @Override protected void onResume(){super.onResume();transferHandler.post(transferPoll);}
    @Override protected void onPause(){transferHandler.removeCallbacks(transferPoll);super.onPause();}
    private void renderTransfer(){
        ServiceSilentInstaller.recover(this,true);ServiceUpdateBatch.promote(this);
        android.content.SharedPreferences data=ServiceTransferState.read(this);String phase=data.getString("phase",""),id=data.getString("id","");
        findViewById(R.id.enableServiceNotifications).setVisibility(androidx.core.app.NotificationManagerCompat.from(this).areNotificationsEnabled()?View.GONE:View.VISIBLE);
        ProgressBar progress=findViewById(R.id.serviceDownloadProgress);TextView message=findViewById(R.id.serviceDownloadStatus);
        boolean batchActive=data.getBoolean("batch_active",false);
        boolean installing="installing".equals(phase),active=batchActive||installing||"downloading".equals(phase)||"checking".equals(phase);progress.setVisibility(active?View.VISIBLE:View.GONE);
        findViewById(R.id.updateAllServices).setEnabled(!active);
        findViewById(R.id.installDownloadedService).setVisibility(View.GONE);
        if(installing){progress.setIndeterminate(true);message.setText("Installing…");}
        else if("checking".equals(phase)){progress.setIndeterminate(true);message.setText("Checking service updates…");}
        else if(active){long total=data.getLong("total",0),bytes=data.getLong("bytes",0);progress.setIndeterminate(total<=0);progress.setProgress(total>0?(int)(bytes*100/total):0);message.setText(("store".equals(id)?"ON Store":"ON Core Services")+" · "+(total>0?(bytes*100/total)+"% · ":"")+String.format(java.util.Locale.US,"%.1f MB",bytes/1048576.0));}
        else if("installed".equals(phase)){message.setText("Update installed.");downloaded=null;}
        else if("error".equals(phase)){message.setText("Download failed. "+data.getString("error","")+" Tap Download to retry.");}
        else if("ready".equals(phase)){try{File candidate=new File(data.getString("path","")).getCanonicalFile();File folder=new File(getCacheDir(),"service-updates").getCanonicalFile();if(candidate.isFile()&&folder.equals(candidate.getParentFile())&&candidate.getName().endsWith(".apk")){downloaded=candidate;message.setText("Downloaded and verified. Ready to install.");if(!batchActive)findViewById(R.id.installDownloadedService).setVisibility(View.VISIBLE);}}catch(Exception ignored){}}
        LinearLayout rows=findViewById(R.id.serviceUpdateRows);for(int i=0;i<rows.getChildCount();i++){Button action=rows.getChildAt(i).findViewById(R.id.downloadServiceRelease);if(action!=null&&Boolean.TRUE.equals(action.getTag()))action.setEnabled(!active);}
    }
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);setContentView(R.layout.activity_service_update);
        status=findViewById(R.id.serviceUpdateStatus);
        findViewById(R.id.enableServiceNotifications).setOnClickListener(v->{if(Build.VERSION.SDK_INT>=26)startActivity(new Intent("android.settings.APP_NOTIFICATION_SETTINGS").putExtra("android.provider.extra.APP_PACKAGE",getPackageName()));else startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())));});
        CheckBox automatic=findViewById(R.id.automaticServiceChecks);automatic.setChecked(getSharedPreferences("ONUpdates",0).getBoolean("enabled",true));
        CheckBox installAutomatically=findViewById(R.id.automaticServiceInstall);
        installAutomatically.setChecked(getSharedPreferences("ONUpdates",0).getBoolean("automatic_install",true));
        installAutomatically.setEnabled(CoreInstallation.full(this)&&automatic.isChecked());
        installAutomatically.setOnCheckedChangeListener((button,enabled)->getSharedPreferences("ONUpdates",0).edit().putBoolean("automatic_install",enabled).apply());
        automatic.setOnCheckedChangeListener((button,enabled)->{getSharedPreferences("ONUpdates",0).edit().putBoolean("enabled",enabled).apply();installAutomatically.setEnabled(enabled&&CoreInstallation.full(this));ServiceUpdates.schedule(this);});
        findViewById(R.id.checkServiceUpdates).setOnClickListener(v->check());findViewById(R.id.backServiceUpdates).setOnClickListener(v->finish());
        findViewById(R.id.installDownloadedService).setOnClickListener(v->install());
        String path=getIntent().getStringExtra("downloaded_apk");
        if(path!=null)try{File candidate=new File(path).getCanonicalFile();File allowed=new File(getCacheDir(),"service-updates").getCanonicalFile();if(candidate.getParentFile().equals(allowed)&&candidate.getName().endsWith(".apk")&&candidate.isFile()){downloaded=candidate;findViewById(R.id.installDownloadedService).setVisibility(View.VISIBLE);}}catch(Exception ignored){}
        CheckBox notices=findViewById(R.id.showServiceUpdateNotices);notices.setChecked(!getSharedPreferences("ONUpdates",0).getBoolean("hide_update_notices",false));notices.setOnCheckedChangeListener((button,enabled)->{android.content.SharedPreferences prefs=getSharedPreferences("ONUpdates",0);android.content.SharedPreferences.Editor edit=prefs.edit().putBoolean("hide_update_notices",!enabled);if(enabled)for(String key:prefs.getAll().keySet())if(key.startsWith("dismiss_"))edit.remove(key);edit.apply();});
        findViewById(R.id.updateAllServices).setOnClickListener(v->ServiceUpdateNotices.start(this,new Intent(this,ServiceDownloadService.class).putExtra("update_all",true)));
        check();
    }
    private void check(){if(loading)return;loading=true;status.setText("Checking published service versions…");findViewById(R.id.checkServiceUpdates).setEnabled(false);
        new Thread(()->{try{List<JSONObject> releases=ServiceUpdates.fetch();runOnUiThread(()->render(releases));}catch(Exception error){runOnUiThread(()->status.setText("Could not check updates. "+error.getMessage()));}finally{runOnUiThread(()->{loading=false;findViewById(R.id.checkServiceUpdates).setEnabled(true);});}},"on-service-versions").start();
    }
    private void render(List<JSONObject> releases){if(isFinishing())return;LinearLayout rows=findViewById(R.id.serviceUpdateRows);rows.removeAllViews();status.setText("Core and Store release catalogue");
        boolean updatesAvailable=false;
        for(String id:new String[]{"core","store"}){
            View row=getLayoutInflater().inflate(R.layout.item_service_update,rows,false);JSONObject release=ServiceUpdates.latest(releases,id);int installed=ServiceUpdates.installed(this,id);
            ((TextView)row.findViewById(R.id.serviceReleaseName)).setText("core".equals(id)?"ON Core Services":"ON Store");Button action=row.findViewById(R.id.downloadServiceRelease);
            if(release==null){((TextView)row.findViewById(R.id.serviceReleaseVersion)).setText(installed>0?"Installed. No compatible signed release has been published yet.":"Not installed. No compatible signed release has been published yet.");action.setText("Not available yet");action.setEnabled(false);}
            else{boolean newer=release.optInt("version_code")>installed;if(installed>0&&newer)updatesAvailable=true;((TextView)row.findViewById(R.id.serviceReleaseVersion)).setText((installed>0?"Installed version code: "+installed+" · ":"Not installed · ")+"Available: "+release.optString("version"));action.setText(installed==0?"Download Store":newer?"Download update":"Up to date");action.setTag(newer);action.setEnabled(newer);action.setOnClickListener(v->{Intent intent=new Intent(this,ServiceDownloadService.class).putExtra("release",release.toString());if(Build.VERSION.SDK_INT>=26)startForegroundService(intent);else startService(intent);findViewById(R.id.installDownloadedService).setVisibility(View.GONE);downloaded=null;renderTransfer();});}
            rows.addView(row);
        }
        findViewById(R.id.updateAllServices).setVisibility(updatesAvailable?View.VISIBLE:View.GONE);
    }
    private void install(){if(downloaded==null||!downloaded.isFile())return;try{
        JSONObject release=new JSONObject(ServiceTransferState.read(this).getString("release",""));
        ServiceApkVerifier.verify(this,release,downloaded);
        if(CoreInstallation.full(this)&&!androidInstaller){
            if(!ServiceTransferState.read(this).getString("error","").isEmpty()){
                new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Automatic installation unavailable").setMessage(ServiceTransferState.read(this).getString("error",""))
                    .setPositiveButton("Use Android installer",(dialog,which)->{androidInstaller=true;install();}).setNegativeButton("Cancel",null).show();return;
            }
            Intent service=new Intent(this,ServiceDownloadService.class).putExtra("install_ready",true);
            if(Build.VERSION.SDK_INT>=26)startForegroundService(service);else startService(service);return;
        }
        if(Build.VERSION.SDK_INT>=26&&!getPackageManager().canRequestPackageInstalls()){startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+getPackageName())));status.setText("Allow installation from Core, then tap Install downloaded APK.");return;}
        Uri uri;if(Build.VERSION.SDK_INT<24){getCacheDir().setExecutable(true,false);downloaded.getParentFile().setExecutable(true,false);downloaded.setReadable(true,false);uri=Uri.fromFile(downloaded);}else uri=FileProvider.getUriForFile(this,getPackageName()+".updates",downloaded);
        startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
    }catch(Exception error){status.setText("Could not open installer: "+error.getMessage());}}
}
