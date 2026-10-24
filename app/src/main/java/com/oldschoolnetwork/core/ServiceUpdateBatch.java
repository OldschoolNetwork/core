package com.oldschoolnetwork.core;
import android.content.Context;
import java.io.File;
import org.json.*;
final class ServiceUpdateBatch {
    static void promote(Context context){
        android.content.SharedPreferences prefs=ServiceTransferState.read(context);if(prefs.getBoolean("batch_active",false)&&"core".equals(prefs.getString("id",""))&&"installed".equals(prefs.getString("phase","")))prefs.edit().putBoolean("batch_active",false).commit();
        if(prefs.getBoolean("batch_active",false)||"installing".equals(prefs.getString("phase",""))||"downloading".equals(prefs.getString("phase","")))return;
        try{JSONArray pending=new JSONArray(prefs.getString("batch_ready","[]")),remaining=new JSONArray();JSONObject first=null;
            for(int i=0;i<pending.length();i++){JSONObject item=pending.getJSONObject(i),release=item.getJSONObject("release");if(ServiceUpdates.installed(context,release.optString("id"))>=release.optInt("version_code"))continue;remaining.put(item);if(first==null)first=item;}
            prefs.edit().putString("batch_ready",remaining.toString()).apply();if(first!=null){JSONObject release=first.getJSONObject("release");File file=new File(first.getString("path"));prefs.edit().putString("release",release.toString()).commit();ServiceTransferState.write(context,release.getString("id"),"ready",file.length(),file.length(),file.getPath(),"");}
        }catch(Exception error){android.util.Log.w("ONCore","Could not restore update batch",error);}
    }
}
