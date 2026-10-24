package com.oldschoolnetwork.core;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

public class ONCoreService extends Service {

    private ONAccount onAccount;

    private final IONCoreService.Stub binder = new IONCoreService.Stub() {
        @Override
        public String getCoreStatus() throws RemoteException { return "Active"; }

        @Override
        public int getCoreVersion() throws RemoteException { return BuildConfig.VERSION_CODE; }

        @Override
        public boolean isLoggedIn() throws RemoteException { return onAccount.isLoggedIn(); }

        @Override
        public boolean hasRoot() throws RemoteException { return false; }

        @Override
        public String getEmail() throws RemoteException { return trustedCaller() ? onAccount.getEmail() : ""; }

        @Override
        public String getUsername() throws RemoteException { return onAccount.getUsername(); }

        @Override
        public String getAtId() throws RemoteException { return onAccount.getAtId(); }

        @Override
        public String getOnId() throws RemoteException { return trustedCaller() ? onAccount.getOnId() : ""; }

        @Override
        public String getName() throws RemoteException { return trustedCaller() ? onAccount.getName() : ""; }

        @Override
        public String getSurname() throws RemoteException { return trustedCaller() ? onAccount.getSurname() : ""; }

        @Override
        public String getToken() throws RemoteException { return trustedCaller() ? onAccount.getToken() : ""; }

        @Override
        public String getAvatar() throws RemoteException { return onAccount.getAvatar(); }

        @Override
        public String getPhoneNumber() throws RemoteException { return trustedCaller() ? onAccount.getPhoneNumber() : ""; }

        @Override
        public void setAvatar(String base64Image) throws RemoteException {
            enforceTrustedCaller(); onAccount.setAvatar(base64Image);
        }

        @Override
        public void updateProfileInfo(String email, String name, String surname, String username, String phone) throws RemoteException {
            enforceTrustedCaller(); onAccount.updateProfileInfo(email, name, surname, username, phone);
        }

        @Override
        public void logout() throws RemoteException {
            enforceTrustedCaller(); onAccount.logout();
        }
    };

    private boolean trustedCaller(){
        int uid=android.os.Binder.getCallingUid();
        if(uid==android.os.Process.myUid())return true;
        String[] packages=getPackageManager().getPackagesForUid(uid);
        if(packages==null)return false;
        for(String name:packages)if((name.equals("com.oldschoolnetwork.store")||name.equals("com.oldschoolnetwork.services.classicinstaller"))&&getPackageManager().checkSignatures(getPackageName(),name)==android.content.pm.PackageManager.SIGNATURE_MATCH)return true;
        return false;
    }
    private void enforceTrustedCaller(){if(!trustedCaller())throw new SecurityException("Only trusted ON services can modify an account.");}

    @Override
    public void onCreate() {
        super.onCreate();
        onAccount = new ONAccount(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}