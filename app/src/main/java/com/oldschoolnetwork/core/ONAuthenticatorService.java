package com.oldschoolnetwork.core;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class ONAuthenticatorService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        ONAuthenticator authenticator = new ONAuthenticator(this);
        return authenticator.getIBinder();
    }
}