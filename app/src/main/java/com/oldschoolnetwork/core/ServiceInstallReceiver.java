package com.oldschoolnetwork.core;

import android.content.*;
import android.content.pm.PackageInstaller;

public final class ServiceInstallReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent) {
        if(Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
            ServiceSilentInstaller.recover(context,true);ServiceUpdates.schedule(context);return;
        }
        String nonce=ServiceTransferState.read(context).getString("install_nonce","");
        if(nonce.isEmpty()||intent.getData()==null||!nonce.equals(intent.getData().getLastPathSegment()))return;
        int result=intent.getIntExtra(PackageInstaller.EXTRA_STATUS,PackageInstaller.STATUS_FAILURE);
        if(result==PackageInstaller.STATUS_SUCCESS)ServiceSilentInstaller.recover(context,false);
        else ServiceSilentInstaller.fail(context,result==PackageInstaller.STATUS_PENDING_USER_ACTION?"Android requires confirmation. Tap Install downloaded APK.":intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE));
    }
}
