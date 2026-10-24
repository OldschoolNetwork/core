package com.oldschoolnetwork.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * ON Core — BootReceiver
 *
 * Запускает ONCoreService при загрузке устройства.
 * Требует разрешение RECEIVE_BOOT_COMPLETED в манифесте.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Intent serviceIntent = new Intent(context, ONCoreService.class);
            if(android.os.Build.VERSION.SDK_INT<26)context.startService(serviceIntent);
            ServiceUpdates.schedule(context);
            ServiceSilentInstaller.recover(context,true);
        }
    }
}
