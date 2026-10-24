package com.oldschoolnetwork.core;

import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ServiceUpdateRegressionTest {
    @Test public void settingsDisableAutomaticInstallationAndForgedResultsAreIgnored() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences settings=context.getSharedPreferences("ONUpdates",0);
        boolean checks=settings.getBoolean("enabled",true),install=settings.getBoolean("automatic_install",true);
        try {
            settings.edit().putBoolean("enabled",false).putBoolean("automatic_install",true).commit();assertFalse(CoreInstallation.automatic(context));
            settings.edit().putBoolean("enabled",true).putBoolean("automatic_install",false).commit();assertFalse(CoreInstallation.automatic(context));
            ServiceTransferState.read(context).edit().putString("phase","installing").putString("install_nonce","private-test-nonce").commit();
            new ServiceInstallReceiver().onReceive(context,new Intent("com.oldschoolnetwork.core.SERVICE_INSTALLED").setData(Uri.parse("on-install://result/wrong-nonce")).putExtra("android.content.pm.extra.STATUS",0));
            assertEquals("installing",ServiceTransferState.read(context).getString("phase",""));
        }finally{settings.edit().putBoolean("enabled",checks).putBoolean("automatic_install",install).commit();ServiceTransferState.read(context).edit().clear().commit();}
    }
    @Test public void fullInstallationUpdatesStoreWithoutOpeningPackageInstaller()throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertTrue("Run on the isolated privileged Core fixture",CoreInstallation.full(context));
        assertEquals(PackageManager.PERMISSION_GRANTED,context.checkCallingOrSelfPermission("android.permission.INSTALL_PACKAGES"));
        int before=ServiceUpdates.installed(context,"store");
        int expected=ServiceUpdates.latest(ServiceUpdates.fetch(),"store").getInt("version_code");assertTrue(before>0&&before<expected);
        context.getSharedPreferences("ONUpdates",0).edit().putBoolean("enabled",true).putBoolean("automatic_install",true).commit();
        ServiceTransferState.read(context).edit().clear().commit();
        context.startService(new Intent(context,ServiceDownloadService.class).putExtra("automatic_check",true));
        long deadline=System.currentTimeMillis()+150000;
        while(System.currentTimeMillis()<deadline&&ServiceUpdates.installed(context,"store")<expected){Thread.sleep(300);}
        assertEquals(ServiceTransferState.read(context).getString("error",""),expected,ServiceUpdates.installed(context,"store"));
        assertTrue(CoreInstallation.full(context));
    }
}
