package com.oldschoolnetwork.core;
public class SessionsActivity extends AccountPanelActivity {
    @Override protected void onCreate(android.os.Bundle state){getIntent().putExtra("panel","sessions");super.onCreate(state);}
}
