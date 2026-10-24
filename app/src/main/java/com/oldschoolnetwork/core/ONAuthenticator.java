package com.oldschoolnetwork.core;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class ONAuthenticator extends AbstractAccountAuthenticator {

    private final Context mContext;

    public ONAuthenticator(Context context) {
        super(context);
        mContext = context;
    }

    // ИСПРАВЛЕНО: Именно этот метод заставляет иконку появиться в Настройках Android!
    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException {
        final Intent intent = new Intent(mContext, OnboardingActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        intent.putExtra("from_system_accounts", true);

        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override public Bundle editProperties(AccountAuthenticatorResponse r, String s) { return null; }
    @Override public Bundle confirmCredentials(AccountAuthenticatorResponse r, Account a, Bundle b) throws NetworkErrorException { return null; }
    @Override public Bundle getAuthToken(AccountAuthenticatorResponse r, Account a, String s, Bundle b) throws NetworkErrorException { return null; }
    @Override public String getAuthTokenLabel(String s) { return null; }
    @Override public Bundle updateCredentials(AccountAuthenticatorResponse r, Account a, String s, Bundle b) throws NetworkErrorException { return null; }
    @Override public Bundle hasFeatures(AccountAuthenticatorResponse r, Account a, String[] s) throws NetworkErrorException { Bundle result=new Bundle();result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT,s==null||s.length==0);return result; }
}
