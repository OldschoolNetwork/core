package com.oldschoolnetwork.core;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

public final class SystemAccounts {
    private static final java.util.concurrent.atomic.AtomicBoolean restoring=new java.util.concurrent.atomic.AtomicBoolean();
    public static void restoreAsync(Context context){Context app=context.getApplicationContext();if(restoring.compareAndSet(false,true))new Thread(()->{try{restore(app);}finally{restoring.set(false);}},"on-system-accounts").start();}
    public static String privateLogin(Context context,Account account){String email=AccountManager.get(context).getUserData(account,"email");return email==null||email.isEmpty()?account.name:email;}
    public static Account publicAccount(String username){return new Account("@"+username,ONAccount.ACCOUNT_TYPE);}
    public static void visibility(Context context,AccountManager manager,Account account){
        if(Build.VERSION.SDK_INT<26)return;
        manager.setAccountVisibility(account,AccountManager.PACKAGE_NAME_KEY_LEGACY_VISIBLE,AccountManager.VISIBILITY_NOT_VISIBLE);
        manager.setAccountVisibility(account,AccountManager.PACKAGE_NAME_KEY_LEGACY_NOT_VISIBLE,AccountManager.VISIBILITY_NOT_VISIBLE);
        // Settings is the platform's account manager; third-party apps receive no blanket visibility.
        java.util.Set<String> settingsPackages = new java.util.HashSet<>();
        settingsPackages.add("com.android.settings");
        for (String action : new String[]{android.provider.Settings.ACTION_ADD_ACCOUNT, android.provider.Settings.ACTION_SYNC_SETTINGS}) {
            for (android.content.pm.ResolveInfo handler : context.getPackageManager().queryIntentActivities(new android.content.Intent(action), 0)) {
                if (handler.activityInfo != null) settingsPackages.add(handler.activityInfo.packageName);
            }
        }
        for (String packageName : settingsPackages) try {
            android.content.pm.ApplicationInfo info = context.getPackageManager().getApplicationInfo(packageName, 0);
            if ((info.flags & (android.content.pm.ApplicationInfo.FLAG_SYSTEM | android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0)
                manager.setAccountVisibility(account, packageName, AccountManager.VISIBILITY_VISIBLE);
        } catch (Exception ignored) {}
    }
    public static void restore(Context context){
        try{
            AccountManager manager=AccountManager.get(context);
            SharedPreferences prefs=context.getSharedPreferences("ON_SECURE_PREFS",Context.MODE_PRIVATE);
            if(prefs.getBoolean("logged_in",false)&&!prefs.getString("email","").isEmpty()){
                String username=prefs.getString("username","");
                if(username.isEmpty())return;
                Account account=publicAccount(username);boolean found=false;
                for(Account existing:manager.getAccountsByType(ONAccount.ACCOUNT_TYPE))if(existing.equals(account))found=true;
                if(!found){Bundle data=new Bundle();data.putString("email",prefs.getString("email",""));data.putString("username",username);data.putString("on_id",prefs.getString("on_id",""));data.putString("server_token",prefs.getString("server_token",""));manager.addAccountExplicitly(account,null,data);}
            }
            // Account names can be visible to apps on older Android. Keep email in private authenticator data.
            for(Account old:manager.getAccountsByType(ONAccount.ACCOUNT_TYPE)){
                if(old.name.startsWith("@")||!old.name.contains("@"))continue;
                String username=manager.getUserData(old,"username");
                if((username==null||username.isEmpty())&&old.name.equals(prefs.getString("email","")))username=prefs.getString("username","");
                if(username==null||username.isEmpty())continue;
                Account replacement=publicAccount(username);Bundle data=new Bundle();
                for(String key:new String[]{"on_id","username","name","surname","server_token","avatar_base64"}){String value=manager.getUserData(old,key);if(value!=null)data.putString(key,value);}
                data.putString("email",old.name);data.putString("username",username);
                String password=manager.getPassword(old);
                boolean added=manager.addAccountExplicitly(replacement,password,data);
                if(!added){for(String key:data.keySet())manager.setUserData(replacement,key,data.getString(key));if(password!=null)manager.setPassword(replacement,password);}
                visibility(context,manager,replacement);
                if(Build.VERSION.SDK_INT>=22)manager.removeAccountExplicitly(old);else manager.removeAccount(old,null,null).getResult();
            }
            java.util.Map<String,Account> canonical=new java.util.LinkedHashMap<>();
            Account[] all=manager.getAccountsByType(ONAccount.ACCOUNT_TYPE);
            String activeId=prefs.getString("on_id",""),activeName="@"+prefs.getString("username","");
            for(Account candidate:all){String id=manager.getUserData(candidate,"on_id");if(id==null||id.isEmpty())continue;Account existing=canonical.get(id);if(existing==null||candidate.name.equals(activeName)&&id.equals(activeId))canonical.put(id,candidate);}
            for(Account candidate:all){
                String id=manager.getUserData(candidate,"on_id");Account keep=id==null?null:canonical.get(id);if(keep==null||keep.equals(candidate))continue;
                for(String key:new String[]{"email","username","name","surname","server_token","avatar_base64"}){String old=manager.getUserData(candidate,key),current=manager.getUserData(keep,key);if((current==null||current.isEmpty())&&old!=null)manager.setUserData(keep,key,old);}
                String oldPassword=manager.getPassword(candidate);if(manager.getPassword(keep)==null&&oldPassword!=null)manager.setPassword(keep,oldPassword);
                if(Build.VERSION.SDK_INT>=22)manager.removeAccountExplicitly(candidate);else manager.removeAccount(candidate,null,null).getResult();
            }
            if(prefs.getBoolean("logged_in",false))for(Account candidate:manager.getAccountsByType(ONAccount.ACCOUNT_TYPE))if(candidate.name.equals(activeName)){
                for(String key:new String[]{"email","username","name","surname","on_id","server_token","avatar_base64"})manager.setUserData(candidate,key,prefs.getString(key,""));
            }
            for(Account account:manager.getAccountsByType(ONAccount.ACCOUNT_TYPE))visibility(context,manager,account);
        }catch(Exception error){android.util.Log.w("ONCore","System account access is not available",error);}
    }
}
