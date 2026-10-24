package com.oldschoolnetwork.core;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class DashboardPanelRegressionTest {
    private void helpViews(View view,java.util.List<View> result){if("Account help".contentEquals(view.getContentDescription()==null?"":view.getContentDescription()))result.add(view);if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)helpViews(((ViewGroup)view).getChildAt(i),result);}
    private boolean contains(View view,String text){if(view instanceof TextView&&((TextView)view).getText().toString().contains(text))return true;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)if(contains(((ViewGroup)view).getChildAt(i),text))return true;return false;}
    @Test public void accountSessionsUseNativeScreenAndRequireAuthentication() throws Exception {
        Instrumentation i=InstrumentationRegistry.getInstrumentation();
        assertFalse(new ONAccount(i.getTargetContext()).isLoggedIn());
        SessionsActivity activity=(SessionsActivity)i.startActivitySync(new Intent(i.getTargetContext(),SessionsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try{Thread.sleep(300);i.waitForIdleSync();i.runOnMainSync(()->{assertTrue(contains(activity.getWindow().getDecorView(),"Active sessions"));assertTrue(contains(activity.getWindow().getDecorView(),"Sign in"));assertTrue(contains(activity.getWindow().getDecorView(),"Retry"));});}
        finally{i.runOnMainSync(activity::finish);}
    }
    @Test public void onboardingHelpOpensExistingAccountDocumentation() throws Exception {
        Instrumentation i=InstrumentationRegistry.getInstrumentation();IntentFilter filter=new IntentFilter(Intent.ACTION_VIEW);filter.addDataScheme("https");filter.addDataAuthority("oldschoolnetwork.online",null);filter.addDataPath("/downloads/docs/categories/account/",android.os.PatternMatcher.PATTERN_LITERAL);
        Instrumentation.ActivityMonitor monitor=i.addMonitor(filter,null,true);
        OnboardingActivity activity=(OnboardingActivity)i.startActivitySync(new Intent(i.getTargetContext(),OnboardingActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try{i.runOnMainSync(()->{java.util.ArrayList<View> help=new java.util.ArrayList<>();helpViews(activity.getWindow().getDecorView(),help);assertEquals(3,help.size());assertTrue(help.get(0).isClickable());help.get(0).performClick();});assertEquals(1,monitor.getHits());}
        finally{i.removeMonitor(monitor);i.runOnMainSync(activity::finish);}
    }
}
