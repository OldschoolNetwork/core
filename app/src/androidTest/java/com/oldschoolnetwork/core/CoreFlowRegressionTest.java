package com.oldschoolnetwork.core;

import android.app.Instrumentation;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.ViewFlipper;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class CoreFlowRegressionTest {
    @Test public void onboardingKeepsStepOnRotationAndHasNativeVerification() throws Exception {
        Instrumentation i=InstrumentationRegistry.getInstrumentation();
        OnboardingActivity activity=(OnboardingActivity)i.startActivitySync(new Intent(i.getTargetContext(),OnboardingActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try {
            i.runOnMainSync(()->{((ViewFlipper)activity.findViewById(R.id.onboardingFlipper)).setDisplayedChild(5);activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);});
            Thread.sleep(700);i.waitForIdleSync();
            i.runOnMainSync(()->{assertFalse(activity.isDestroyed());assertEquals(5,((ViewFlipper)activity.findViewById(R.id.onboardingFlipper)).getDisplayedChild());assertTrue(((android.widget.TextView)activity.findViewById(R.id.otpAvailabilityNotice)).getText().toString().contains("No code is sent"));activity.findViewById(R.id.btnSendOtp).performClick();});
            i.waitForIdleSync();i.runOnMainSync(()->assertEquals(6,((ViewFlipper)activity.findViewById(R.id.onboardingFlipper)).getDisplayedChild()));
        } finally {i.runOnMainSync(activity::finish);}
    }
    @Test public void missingStoreDownloadsAndOffersInstall() throws Exception {
        Instrumentation i=InstrumentationRegistry.getInstrumentation();android.content.Context context=i.getTargetContext();
        try{context.getPackageManager().getPackageInfo("com.oldschoolnetwork.store",0);fail("Run this regression on the test device after uninstalling Store.");}catch(PackageManager.NameNotFoundException expected){}
        ServiceTransferState.read(context).edit().clear().commit();
        JSONObject release=ServiceUpdates.latest(ServiceUpdates.fetch(),"store");assertNotNull(release);
        UpdateActivity activity=(UpdateActivity)i.startActivitySync(new Intent(context,UpdateActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try{
            context.startService(new Intent(context,ServiceDownloadService.class).putExtra("release",release.toString()));
            long deadline=System.currentTimeMillis()+120000;String phase="";
            while(System.currentTimeMillis()<deadline){phase=ServiceTransferState.read(context).getString("phase","");if("ready".equals(phase)||"error".equals(phase))break;Thread.sleep(250);}
            assertEquals(ServiceTransferState.read(context).getString("error",""),"ready",phase);
            assertTrue(new File(ServiceTransferState.read(context).getString("path","")).isFile());Thread.sleep(600);i.waitForIdleSync();
            i.runOnMainSync(()->{assertEquals(View.VISIBLE,activity.findViewById(R.id.installDownloadedService).getVisibility());assertEquals(View.GONE,activity.findViewById(R.id.serviceDownloadProgress).getVisibility());});
        }finally{i.runOnMainSync(activity::finish);}
    }
}
