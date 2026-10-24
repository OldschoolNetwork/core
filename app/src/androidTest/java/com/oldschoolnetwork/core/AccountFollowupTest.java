package com.oldschoolnetwork.core;
import android.app.Instrumentation;
import android.content.Intent;
import android.view.*;
import android.widget.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.JSONObject;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class) public class AccountFollowupTest {
 @Test public void settingsSupportIsAStyledRowAndCoreStatusIsRemoved(){Instrumentation i=InstrumentationRegistry.getInstrumentation();i.runOnMainSync(()->{View settings=LayoutInflater.from(i.getTargetContext()).inflate(R.layout.fragment_settings,null);assertNotNull(settings.findViewById(R.id.row_account_support));assertFalse(settings.findViewById(R.id.row_account_support) instanceof Button);View services=LayoutInflater.from(i.getTargetContext()).inflate(R.layout.fragment_services,null);assertNull(services.findViewById(i.getTargetContext().getResources().getIdentifier("tv_core_app_status","id",i.getTargetContext().getPackageName())));});}
 @Test public void updateAllAppearsForNewerInstalledService() throws Exception {Instrumentation i=InstrumentationRegistry.getInstrumentation();UpdateActivity activity=(UpdateActivity)i.startActivitySync(new Intent(i.getTargetContext(),UpdateActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));try{JSONObject release=new JSONObject();release.put("id","core");release.put("package_name",i.getTargetContext().getPackageName());release.put("version_code",BuildConfig.VERSION_CODE+1);release.put("version","QA");release.put("available",true);release.put("signing","release");release.put("min_sdk",14);release.put("url","https://example.invalid/qa.apk");release.put("sha256",new String(new char[64]).replace((char)0,'a'));release.put("abi","universal");java.lang.reflect.Method render=UpdateActivity.class.getDeclaredMethod("render",List.class);render.setAccessible(true);i.runOnMainSync(()->{try{render.invoke(activity,Arrays.asList(release));assertEquals(View.VISIBLE,activity.findViewById(R.id.updateAllServices).getVisibility());assertNotNull(activity.findViewById(R.id.showServiceUpdateNotices));}catch(Exception e){throw new RuntimeException(e);}});}finally{i.runOnMainSync(activity::finish);}}
}
