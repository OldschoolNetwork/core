package com.oldschoolnetwork.core;

import android.app.*;
import android.content.*;
import android.os.Build;
import org.json.*;
import java.net.*;
import java.io.*;
import java.util.*;

public final class ServiceUpdates {
    public static final String MANIFEST="https://api.oldschoolnetwork.online/v1/on-account/api/ecosystem.php?action=service_releases";
    public static final String CERTIFICATE="F8239CD9DEB269E9D4D86893732C27DEF1936737539C31983D75F1251A49F2D2";
    public static String packageName(String id){return "core".equals(id)?"com.oldschoolnetwork.core":"store".equals(id)?"com.oldschoolnetwork.store":"";}
    public static List<JSONObject> fetch() throws Exception {
        HttpURLConnection connection=(HttpURLConnection)new URL(MANIFEST).openConnection();
        try{connection.setConnectTimeout(3000);connection.setReadTimeout(4000);connection.setUseCaches(false);
            StringBuilder text=new StringBuilder();try(BufferedReader reader=new BufferedReader(new InputStreamReader(connection.getInputStream(),"UTF-8"))){String line;while((line=reader.readLine())!=null){text.append(line);if(text.length()>1024*1024)throw new IOException("Release manifest is too large.");}}
            JSONArray releases=new JSONObject(text.toString()).getJSONArray("releases");List<JSONObject> result=new ArrayList<>();for(int i=0;i<releases.length();i++)result.add(releases.getJSONObject(i));return result;
        }finally{connection.disconnect();}
    }
    public static JSONObject latest(List<JSONObject> releases,String id){
        JSONObject best=null;
        for(JSONObject release:releases){
            if(!id.equals(release.optString("id"))||!packageName(id).equals(release.optString("package_name"))||!release.optBoolean("available")||!"release".equals(release.optString("signing"))||release.optInt("min_sdk",Integer.MAX_VALUE)>Build.VERSION.SDK_INT||!release.optString("url").startsWith("https://")||!release.optString("sha256").matches("(?i)[a-f0-9]{64}"))continue;
            String abi=release.optString("abi","universal");org.json.JSONArray declared=release.optJSONArray("abis");boolean compatible="universal".equals(abi);
            String[] device=Build.VERSION.SDK_INT>=21?Build.SUPPORTED_ABIS:new String[]{Build.CPU_ABI,Build.CPU_ABI2};
            for(String value:device){if(value.equals(abi))compatible=true;if(declared!=null)for(int i=0;i<declared.length();i++)if(value.equals(declared.optString(i)))compatible=true;}
            if(compatible&&(best==null||release.optInt("version_code")>best.optInt("version_code")))best=release;
        }
        return best;
    }
    public static int installed(Context context,String id){try{return context.getPackageManager().getPackageInfo(packageName(id),0).versionCode;}catch(Exception ignored){return 0;}}
    public static void schedule(Context context){
        Intent intent=new Intent(context,UpdateCheckReceiver.class);int flags=PendingIntent.FLAG_UPDATE_CURRENT;if(Build.VERSION.SDK_INT>=23)flags|=PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pending=PendingIntent.getBroadcast(context,4204,intent,flags);AlarmManager manager=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        manager.cancel(pending);
        if(context.getSharedPreferences("ONUpdates",0).getBoolean("enabled",true))manager.setInexactRepeating(AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+60000,AlarmManager.INTERVAL_DAY,pending);
    }
}
