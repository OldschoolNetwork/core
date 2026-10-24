package com.oldschoolnetwork.core;

import android.content.Context;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.*;

/** Exchanges credentials for a server-issued token; never derives tokens from an email. */
public final class CoreSession {
    public static JSONObject login(String login,String password) throws Exception {
        if(password==null||password.isEmpty())throw new IOException("Sign in again to verify this account.");
        HttpURLConnection connection=(HttpURLConnection)new URL("https://api.oldschoolnetwork.online/v1/on-account/api/login.php").openConnection();
        try{
            connection.setRequestMethod("POST");connection.setConnectTimeout(8000);connection.setReadTimeout(12000);connection.setDoOutput(true);connection.setRequestProperty("Content-Type","application/json; charset=UTF-8");
            JSONObject request=new JSONObject();request.put("login",login);request.put("password",password);
            try(OutputStream out=connection.getOutputStream()){out.write(request.toString().getBytes("UTF-8"));}
            StringBuilder body=new StringBuilder();try(BufferedReader reader=new BufferedReader(new InputStreamReader(connection.getInputStream(),"UTF-8"))){String line;while((line=reader.readLine())!=null)body.append(line);}
            JSONObject result=new JSONObject(body.toString());if(!"success".equals(result.optString("status"))||result.optString("token").isEmpty())throw new IOException(result.optString("message","Could not verify this account."));return result;
        }finally{connection.disconnect();}
    }
}
