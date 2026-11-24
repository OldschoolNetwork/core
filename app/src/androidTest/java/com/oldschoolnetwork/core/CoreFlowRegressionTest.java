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
    @Test public void onboardingKeepsVerificationOnRotation() throws Exception {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        OnboardingActivity activity = (OnboardingActivity)instrumentation.startActivitySync(
                new Intent(instrumentation.getTargetContext(),OnboardingActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try {
            ViewFlipper pages = (ViewFlipper)activity.findViewById(R.id.onboardingFlipper);
            android.webkit.WebView mail = (android.webkit.WebView)activity.findViewById(R.id.mailArtwork);
            instrumentation.runOnMainSync(() -> {
                pages.setDisplayedChild(5);
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            });
            long deadline = android.os.SystemClock.elapsedRealtime() + 10000;
            String state = "";
            while (android.os.SystemClock.elapsedRealtime() < deadline) {
                java.util.concurrent.ArrayBlockingQueue<String> result = new java.util.concurrent.ArrayBlockingQueue<>(1);
                instrumentation.runOnMainSync(() -> mail.evaluateJavascript(
                    "document.getElementById('submit') ? document.getElementById('submit').disabled : document.title+' '+location.protocol+' '+(document.body ? document.body.innerText.slice(0,160) : document.readyState)",result::offer));
                state = result.poll(2,java.util.concurrent.TimeUnit.SECONDS);
                if ("true".equals(state)) break;
                Thread.sleep(100);
            }
            assertEquals("The local form must not accept a code before a server challenge.","true",state);
            instrumentation.runOnMainSync(() -> {
                assertFalse(activity.isFinishing());
                assertEquals(5,pages.getDisplayedChild());
                assertTrue(mail.getSettings().getBlockNetworkLoads());
            });
        } finally {instrumentation.runOnMainSync(activity::finish);}
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
