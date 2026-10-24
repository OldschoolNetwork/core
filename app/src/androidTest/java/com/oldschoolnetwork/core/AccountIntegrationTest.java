package com.oldschoolnetwork.core;

import android.accounts.*;
import android.content.Intent;
import android.os.Bundle;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class AccountIntegrationTest {
    @Test public void duplicatePublicNamesMergeByOnIdWithoutLosingCredentials() throws Exception {
        android.content.Context context=getInstrumentation().getTargetContext();AccountManager manager=AccountManager.get(context);
        String suffix=String.valueOf(System.currentTimeMillis());Account first=SystemAccounts.publicAccount("qa_old_"+suffix),second=SystemAccounts.publicAccount("qa_new_"+suffix);
        Bundle one=new Bundle();one.putString("on_id","QA-"+suffix);one.putString("email","qa@example.invalid");Bundle two=new Bundle(one);
        assertTrue(manager.addAccountExplicitly(first,"qa-password",one));assertTrue(manager.addAccountExplicitly(second,null,two));
        try{SystemAccounts.restore(context);int count=0;Account remaining=null;for(Account account:manager.getAccountsByType(ONAccount.ACCOUNT_TYPE))if(("QA-"+suffix).equals(manager.getUserData(account,"on_id"))){count++;remaining=account;}assertEquals(1,count);assertNotNull(remaining);assertEquals("qa-password",manager.getPassword(remaining));}
        finally{for(Account account:manager.getAccountsByType(ONAccount.ACCOUNT_TYPE))if(account.equals(first)||account.equals(second))manager.removeAccount(account,null,null).getResult();}
    }
    private android.app.Instrumentation getInstrumentation(){return InstrumentationRegistry.getInstrumentation();}
    @Test public void testAuthenticatorIsRegisteredWithResourceLabel() {
        android.content.Context context=getInstrumentation().getTargetContext();
        AuthenticatorDescription found=null;
        for(AuthenticatorDescription description:AccountManager.get(context).getAuthenticatorTypes())if(ONAccount.ACCOUNT_TYPE.equals(description.type))found=description;
        assertNotNull("ON Account must appear in the platform authenticator registry",found);
        assertTrue("Android Settings needs a resource ID for the account label",found.labelId!=0);
        assertEquals("ON Account",context.getString(found.labelId));
    }
    @Test public void testSystemAddAccountRoutesToOnboarding() throws Exception {
        AccountManager manager=AccountManager.get(getInstrumentation().getTargetContext());
        Bundle result=manager.addAccount(ONAccount.ACCOUNT_TYPE,ONAccount.AUTH_TOKEN_TYPE,null,null,null,null,null).getResult();
        Intent intent=result.getParcelable(AccountManager.KEY_INTENT);
        assertNotNull(intent);assertEquals(OnboardingActivity.class.getName(),intent.getComponent().getClassName());
        assertNotNull(intent.getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE));
    }
    @Test public void testAddedAccountIsAvailableToSystemAccountManager() throws Exception {
        AccountManager manager=AccountManager.get(getInstrumentation().getTargetContext());
        Account account=new Account("ON-QA-"+System.currentTimeMillis(),ONAccount.ACCOUNT_TYPE);
        assertTrue(manager.addAccountExplicitly(account,null,new Bundle()));
        try{boolean found=false;for(Account item:manager.getAccountsByType(ONAccount.ACCOUNT_TYPE))if(account.equals(item))found=true;assertTrue(found);}
        finally{manager.removeAccount(account,null,null).getResult();}
    }
    @Test public void legacyEmailAccountMigratesToPublicUsername() throws Exception {
        android.content.Context context=getInstrumentation().getTargetContext();AccountManager manager=AccountManager.get(context);
        String suffix=String.valueOf(System.currentTimeMillis());Account legacy=new Account("on-qa-"+suffix+"@example.invalid",ONAccount.ACCOUNT_TYPE);Account replacement=SystemAccounts.publicAccount("qa"+suffix);
        Bundle data=new Bundle();data.putString("username","qa"+suffix);data.putString("server_token","qa-token");assertTrue(manager.addAccountExplicitly(legacy,"qa-password",data));
        try{SystemAccounts.restore(context);boolean oldFound=false,newFound=false;for(Account account:manager.getAccountsByType(ONAccount.ACCOUNT_TYPE)){if(account.equals(legacy))oldFound=true;if(account.equals(replacement))newFound=true;}assertFalse(oldFound);assertTrue(newFound);assertEquals(legacy.name,manager.getUserData(replacement,"email"));assertEquals("qa-password",manager.getPassword(replacement));assertEquals("qa-token",manager.getUserData(replacement,"server_token"));}
        finally{for(Account account:manager.getAccountsByType(ONAccount.ACCOUNT_TYPE))if(account.equals(legacy)||account.equals(replacement))manager.removeAccount(account,null,null).getResult();}
    }
}
