package com.oldschoolnetwork.core;

import android.content.Context;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.*;

/** Exchanges credentials for a server-issued token; never derives tokens from an email. */
public final class CoreSession {
    private static final String BASE = "https://api.oldschoolnetwork.online/v1/on-account/api/";
    static JSONObject request(String path, JSONObject request) throws Exception {
        HttpURLConnection connection=(HttpURLConnection)new URL(BASE+path).openConnection();
        connection.setInstanceFollowRedirects(false);
        try{
            connection.setRequestMethod("POST");connection.setConnectTimeout(8000);connection.setReadTimeout(12000);connection.setDoOutput(true);connection.setRequestProperty("Content-Type","application/json; charset=UTF-8");
            try(OutputStream out=connection.getOutputStream()){out.write(request.toString().getBytes("UTF-8"));}
            StringBuilder body=new StringBuilder();try(BufferedReader reader=new BufferedReader(new InputStreamReader(connection.getInputStream(),"UTF-8"))){String line;while((line=reader.readLine())!=null)body.append(line);}
            return new JSONObject(body.toString());
        }finally{connection.disconnect();}
    }
}
