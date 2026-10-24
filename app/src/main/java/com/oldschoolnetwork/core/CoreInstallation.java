package com.oldschoolnetwork.core;

import android.content.Context;
import android.content.pm.ApplicationInfo;

/** Full setup is an Android system installation, not just an available su binary. */
public final class CoreInstallation {
    public static boolean full(Context context) {
        int flags=context.getApplicationInfo().flags;
        return (flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
    }
    public static boolean automatic(Context context) {
        return full(context) && context.getSharedPreferences("ONUpdates",0).getBoolean("enabled",true)
            && context.getSharedPreferences("ONUpdates",0).getBoolean("automatic_install",true);
    }
}
