package com.oldschoolnetwork.core;
import android.content.Context;
import android.content.pm.*;
import java.io.*;
import java.security.MessageDigest;
import org.json.JSONObject;
public final class ServiceApkVerifier {
    static String digest(byte[] bytes)throws Exception{StringBuilder text=new StringBuilder();for(byte value:MessageDigest.getInstance("SHA-256").digest(bytes))text.append(String.format(java.util.Locale.US,"%02X",value&255));return text.toString();}
    public static void verify(Context context,JSONObject release,File file)throws Exception{
        MessageDigest hash=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(file)){byte[] buffer=new byte[32768];int count;while((count=in.read(buffer))!=-1)hash.update(buffer,0,count);}
        StringBuilder checksum=new StringBuilder();for(byte value:hash.digest())checksum.append(String.format(java.util.Locale.US,"%02x",value&255));if(!checksum.toString().equalsIgnoreCase(release.getString("sha256")))throw new IOException("APK checksum does not match.");
        PackageInfo apk=context.getPackageManager().getPackageArchiveInfo(file.getAbsolutePath(),PackageManager.GET_SIGNATURES);
        if(apk==null||!ServiceUpdates.packageName(release.getString("id")).equals(apk.packageName)||apk.versionCode!=release.getInt("version_code")||apk.signatures==null||apk.signatures.length!=1||!ServiceUpdates.CERTIFICATE.equalsIgnoreCase(digest(apk.signatures[0].toByteArray())))throw new IOException("APK identity or ON signing certificate does not match.");
        try{PackageInfo existing=context.getPackageManager().getPackageInfo(apk.packageName,PackageManager.GET_SIGNATURES);if(existing.versionCode>apk.versionCode)throw new IOException("A newer version is already installed.");if(!java.util.Arrays.equals(existing.signatures,apk.signatures))throw new IOException("The installed app uses another signing key. Migrate it before updating.");}catch(PackageManager.NameNotFoundException ignored){}
    }
}
