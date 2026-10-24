package com.oldschoolnetwork.core;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.*;

public final class AccountApi {
    public interface Callback { void complete(JSONObject result, String error); }
    private AccountApi() {}
    public static JSONObject request(Context context, String action, JSONObject values) throws Exception {
        android.content.SharedPreferences prefs=context.getSharedPreferences("ON_SECURE_PREFS",0);
        String id=prefs.getString("on_id",""),token=prefs.getString("server_token","");
        if(!prefs.getBoolean("logged_in",false)||id.isEmpty()||token.isEmpty())throw new IOException("Sign in to manage your account.");
        JSONObject result=requestWithToken(action,values,id,token);
        if(!id.equals(prefs.getString("on_id","")))throw new IOException("The active account changed. Try again.");
        return result;
    }
    public static JSONObject requestWithToken(String action,JSONObject values,String id,String token)throws Exception{
        if(id==null||id.isEmpty()||token==null||token.isEmpty())throw new IOException("Sign in to manage your account.");
        values.put("on_id",id);values.put("token",token);
        HttpURLConnection connection=(HttpURLConnection)new URL("https://api.oldschoolnetwork.online/v1/on-account/api/ecosystem.php?action="+action).openConnection();
        try{
            connection.setRequestMethod("POST");connection.setConnectTimeout(10000);connection.setReadTimeout(15000);connection.setDoOutput(true);connection.setRequestProperty("Content-Type","application/json; charset=UTF-8");
            try(OutputStream out=connection.getOutputStream()){out.write(values.toString().getBytes("UTF-8"));}
            int status=connection.getResponseCode();InputStream stream=status>=400?connection.getErrorStream():connection.getInputStream();
            if(stream==null)throw new IOException("Server unavailable (HTTP "+status+")");
            StringBuilder body=new StringBuilder();try(BufferedReader reader=new BufferedReader(new InputStreamReader(stream,"UTF-8"))){String line;while((line=reader.readLine())!=null)body.append(line);}
            JSONObject result=new JSONObject(body.toString());String state=result.optString("status");
            if(status>=400||(!state.equals("ok")&&!state.equals("success")))throw new IOException(result.optString("message","Could not verify this account. Sign in again."));
            return result;
        }finally{connection.disconnect();}
    }
    public static void requestAsync(Context context,String action,JSONObject values,Callback callback){
        Context app=context.getApplicationContext();Handler main=new Handler(Looper.getMainLooper());
        new Thread(()->{try{JSONObject result=request(app,action,values);main.post(()->callback.complete(result,null));}catch(Exception failure){String message=failure.getMessage();main.post(()->callback.complete(null,message==null?"Could not connect. Try again.":message));}},"on-account-"+action).start();
    }
}
