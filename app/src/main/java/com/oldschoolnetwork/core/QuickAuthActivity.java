package com.oldschoolnetwork.core;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.LayoutTransition;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Base64;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuickAuthActivity extends Activity {

    private View rootDimBackground;
    private LinearLayout bottomSheetContainer;
    private View stateSelectAccount, statePermissions, stateLoading;
    private View headerIcons;
    private ImageView ivCallingAppIcon;
    private ProgressBar authProgressBar;
    private TextView tvAppSubtitle;

    private LinearLayout llAccountsContainer;
    private LinearLayout llPermissionsContainer;
    private View btnAccountTemplate;

    private ImageView ivPermSelectedAvatar, ivLoadingAvatar;
    private TextView tvPermSelectedUsername, tvPermSelectedEmail, tvLoadingUsername;

    private String requestingAppId = "Unknown App";
    private boolean isClosing = false;

    private Account selectedAccount = null;
    private RequestOptions circleOptions;

    private static class CachedProfile {
        String email = "";
        String onId = "";
        String username = "";
        String name = "";
        String surname = "";
        String phone = "";
        String avatarBase64 = "";
        boolean loadedFromServer = false;
    }

    private final Map<String, CachedProfile> profileCache = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(android.R.style.Theme_Translucent_NoTitleBar);
        setContentView(R.layout.activity_quick_auth);

        rootDimBackground = findViewById(R.id.rootDimBackground);
        bottomSheetContainer = findViewById(R.id.bottomSheetContainer);
        headerIcons = findViewById(R.id.headerIcons);
        ivCallingAppIcon = findViewById(R.id.ivCallingAppIcon);

        stateSelectAccount = findViewById(R.id.stateSelectAccount);
        statePermissions = findViewById(R.id.statePermissions);
        stateLoading = findViewById(R.id.stateLoading);
        authProgressBar = findViewById(R.id.authProgressBar);
        tvAppSubtitle = findViewById(R.id.tvAppSubtitle);

        llAccountsContainer = findViewById(R.id.llAccountsContainer);
        llPermissionsContainer = findViewById(R.id.llPermissionsContainer);
        btnAccountTemplate = findViewById(R.id.btnAccountTemplate);

        ivPermSelectedAvatar = findViewById(R.id.ivPermSelectedAvatar);
        ivLoadingAvatar = findViewById(R.id.ivLoadingAvatar);
        tvPermSelectedUsername = findViewById(R.id.tvPermSelectedUsername);
        tvPermSelectedEmail = findViewById(R.id.tvPermSelectedEmail);
        tvLoadingUsername = findViewById(R.id.tvLoadingUsername);

        circleOptions = new RequestOptions()
                .circleCrop()
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder);

        loadCallingAppInfo();

        LayoutTransition layoutTransition = new LayoutTransition();
        layoutTransition.setDuration(250);
        bottomSheetContainer.setLayoutTransition(layoutTransition);

        setupEntryAnimation();
        setupClickListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (stateSelectAccount.getVisibility() == View.VISIBLE) {
            loadAccountsFromSystemAndAPI();
        }
    }

    private void loadCallingAppInfo() {
        String callingPkg = getCallingPackage();
        if (callingPkg != null) {
            PackageManager pm = getPackageManager();
            try {
                ApplicationInfo appInfo = pm.getApplicationInfo(callingPkg, 0);
                requestingAppId = (String) pm.getApplicationLabel(appInfo);
                Drawable appIcon = pm.getApplicationIcon(appInfo);

                Glide.with(this).load(appIcon)
                        .apply(new RequestOptions().transform(new RoundedCorners(dpToPx(12))))
                        .into(ivCallingAppIcon);

            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
        tvAppSubtitle.setText("To work with " + requestingAppId);
    }

    private void loadAccountsFromSystemAndAPI() {
        llAccountsContainer.removeAllViews();
        AccountManager am = AccountManager.get(this);
        Account[] sysAccounts = am.getAccountsByType(ONAccount.ACCOUNT_TYPE);
        List<Account> accounts = new ArrayList<>();
        java.util.Set<String> identities = new java.util.HashSet<>();
        for (Account a : sysAccounts) {
            String id = am.getUserData(a, "on_id");
            String key = id != null && !id.isEmpty() ? "id:" + id : "login:" + SystemAccounts.privateLogin(this, a).toLowerCase(java.util.Locale.US);
            if (identities.add(key)) accounts.add(a);
        }
        ONAccount currentSession = new ONAccount(this);

        if (accounts.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("No ON accounts found. Please add one.");
            emptyText.setTextColor(0xFF888888);
            emptyText.setPadding(32, 32, 32, 32);
            llAccountsContainer.addView(emptyText);
            return;
        }

        for (final Account acc : accounts) {
            LinearLayout row = new LinearLayout(this);
            row.setLayoutParams(btnAccountTemplate.getLayoutParams());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(32, 32, 32, 32);
            row.setBackgroundResource(R.drawable.bg_holo_selectable);
            row.setClickable(true);

            ImageView avatar = new ImageView(this);
            avatar.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(48), dpToPx(48)));
            avatar.setImageResource(R.drawable.ic_profile_placeholder);

            LinearLayout textLayout = new LinearLayout(this);
            textLayout.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            textParams.setMargins(dpToPx(16), 0, 0, 0);
            textLayout.setLayoutParams(textParams);

            CachedProfile initialProfile = new CachedProfile();
            initialProfile.email = SystemAccounts.privateLogin(this, acc);
            if (currentSession.isLoggedIn() && (initialProfile.email.equalsIgnoreCase(currentSession.getEmail()) || acc.name.equalsIgnoreCase("@" + currentSession.getUsername()))) {
                initialProfile.onId = currentSession.getOnId();
                initialProfile.username = currentSession.getUsername();
                initialProfile.name = currentSession.getName();
                initialProfile.surname = currentSession.getSurname();
                initialProfile.phone = currentSession.getPhoneNumber();
                initialProfile.avatarBase64 = currentSession.getAvatar();
            } else {
                try {
                    String udId = am.getUserData(acc, "on_id");
                    String udUser = am.getUserData(acc, "username");
                    String udName = am.getUserData(acc, "name");
                    String udSurname = am.getUserData(acc, "surname");
                    if (udId != null) initialProfile.onId = udId;
                    if (udUser != null) initialProfile.username = udUser;
                    if (udName != null) initialProfile.name = udName;
                    if (udSurname != null) initialProfile.surname = udSurname;
                } catch (Exception ignored) {}
            }
            profileCache.put(acc.name, initialProfile);

            String initialFullName = (initialProfile.name + " " + initialProfile.surname).trim();
            String initialDisplay = !initialFullName.isEmpty()
                    ? initialFullName
                    : (!initialProfile.username.isEmpty() ? "@" + initialProfile.username : acc.name.split("@")[0]);

            TextView tvUser = new TextView(this);
            tvUser.setTextColor(0xFFFFFFFF);
            tvUser.setTextSize(20);
            tvUser.setText(initialDisplay);

            TextView tvEmail = new TextView(this);
            tvEmail.setTextColor(0xFF888888);
            tvEmail.setTextSize(14);
            tvEmail.setText(initialProfile.email);

            if (initialProfile.avatarBase64 != null && !initialProfile.avatarBase64.isEmpty()) {
                try {
                    byte[] initBytes = Base64.decode(initialProfile.avatarBase64, Base64.DEFAULT);
                    Glide.with(this).load(initBytes).apply(circleOptions).into(avatar);
                } catch (Exception ignored) {}
            }

            textLayout.addView(tvUser);
            textLayout.addView(tvEmail);
            row.addView(avatar);
            row.addView(textLayout);
            llAccountsContainer.addView(row);

            row.setOnClickListener(v -> {
                selectedAccount = acc;
                CachedProfile cp = profileCache.get(acc.name);
                String disp = tvUser.getText().toString();
                byte[] avBytes = null;
                if (cp != null && cp.avatarBase64 != null && !cp.avatarBase64.isEmpty()) {
                    try {
                        avBytes = Base64.decode(cp.avatarBase64, Base64.DEFAULT);
                    } catch (Exception ignored) {}
                }
                preparePermissionsScreen(disp, cp != null ? cp.email : SystemAccounts.privateLogin(this, acc), avBytes);
                stateSelectAccount.setVisibility(View.GONE);
                statePermissions.setVisibility(View.VISIBLE);
            });

            new Thread(() -> {
                try {
                    URL url = new URL("https://api.oldschoolnetwork.online/v1/on-account/api/check_user.php");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setConnectTimeout(2500);
                    conn.setReadTimeout(2500);
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    String payload = "{\"login\":\"" + acc.name + "\"}";
                    OutputStream os = conn.getOutputStream();
                    os.write(payload.getBytes("UTF-8"));
                    os.close();

                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                    reader.close();

                    JSONObject res = new JSONObject(response.toString());
                    if (res.optBoolean("exists")) {
                        JSONObject userObj = res.optJSONObject("user");
                        if (userObj != null) {
                            CachedProfile cp = profileCache.get(acc.name);
                            if (cp == null) cp = new CachedProfile();
                            cp.email = userObj.optString("email", acc.name);
                            cp.onId = userObj.optString("on_id", cp.onId);
                            cp.username = userObj.optString("username", cp.username);
                            cp.name = userObj.optString("name", cp.name);
                            cp.surname = userObj.optString("surname", cp.surname);
                            cp.phone = userObj.optString("phone", cp.phone);
                            cp.avatarBase64 = userObj.optString("avatar_base64", cp.avatarBase64);
                            cp.loadedFromServer = true;
                            profileCache.put(acc.name, cp);

                            String fullName = (cp.name + " " + cp.surname).trim();
                            final String displayStr = fullName.isEmpty() ? "@" + cp.username : fullName;
                            final String finalAvatarBase64 = cp.avatarBase64;

                            runOnUiThread(() -> {
                                if (isFinishing()) return;
                                tvUser.setText(displayStr);
                                if (finalAvatarBase64 != null && !finalAvatarBase64.isEmpty()) {
                                    byte[] avatarBytes = Base64.decode(finalAvatarBase64, Base64.DEFAULT);
                                    Glide.with(QuickAuthActivity.this).load(avatarBytes).apply(circleOptions).into(avatar);
                                }
                            });
                        }
                    } else if ("deleted".equals(res.optString("status"))
                            || (res.has("exists") && !res.optBoolean("exists") && !res.has("error") && !"error".equals(res.optString("status")))) {
                        runOnUiThread(() -> {
                            tvUser.setText("Account Deleted");
                            tvUser.setTextColor(Color.RED);
                            row.setOnClickListener(null);
                        });
                    }
                } catch (Exception ignored) {
                }
            }).start();

            View divider = new View(this);
            divider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1)));
            divider.setBackgroundColor(0x33FFFFFF);
            llAccountsContainer.addView(divider);
        }
    }

    private void preparePermissionsScreen(String displayUser, String email, byte[] avatarBytes) {
        tvPermSelectedUsername.setText(displayUser);
        tvPermSelectedEmail.setText(email);
        tvLoadingUsername.setText(displayUser);

        if (avatarBytes != null) {
            Glide.with(this).load(avatarBytes).apply(circleOptions).into(ivPermSelectedAvatar);
            Glide.with(this).load(avatarBytes).apply(circleOptions).into(ivLoadingAvatar);
        } else {
            ivPermSelectedAvatar.setImageResource(R.drawable.ic_profile_placeholder);
            ivLoadingAvatar.setImageResource(R.drawable.ic_profile_placeholder);
        }

        llPermissionsContainer.removeAllViews();
        // Only show ON Account Basic Profile (hide unreleased GameHome & Cloud scopes)
        addPermissionRow("ON Account Basic Profile (email, ON ID, username, and profile picture)", R.drawable.ic_profile_placeholder, avatarBytes);
    }

    private void addPermissionRow(String text, int defaultIconRes, byte[] avatarBytes) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8));

        ImageView icon = new ImageView(this);
        icon.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(32), dpToPx(32)));
        icon.setImageResource(defaultIconRes);

        if (avatarBytes != null) {
            Glide.with(this).load(avatarBytes).apply(circleOptions).into(icon);
        }

        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(0xFFFFFFFF);
        tv.setTextSize(12);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        params.setMargins(dpToPx(16), 0, 0, 0);
        tv.setLayoutParams(params);

        row.addView(icon);
        row.addView(tv);
        llPermissionsContainer.addView(row);
    }

    private void setupClickListeners() {
        findViewById(R.id.btnAddAccount).setOnClickListener(v -> {
            Intent intent = new Intent(QuickAuthActivity.this, OnboardingActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btnOpenDashboard).setOnClickListener(v -> {
            Intent intent = new Intent(QuickAuthActivity.this, DashboardActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btnCancelAuth).setOnClickListener(v -> {
            statePermissions.setVisibility(View.GONE);
            stateSelectAccount.setVisibility(View.VISIBLE);
        });

        findViewById(R.id.btnContinueAuth).setOnClickListener(v -> {
            if (selectedAccount == null) return;
            headerIcons.setVisibility(View.GONE);
            statePermissions.setVisibility(View.GONE);
            stateLoading.setVisibility(View.VISIBLE);
            authProgressBar.setVisibility(View.VISIBLE);

            final String targetEmail = selectedAccount.name;
            CachedProfile cached = profileCache.get(targetEmail);
            new Thread(() -> {
                CachedProfile profile = cached != null ? cached : new CachedProfile();
                profile.email = targetEmail;
                try {
                    android.accounts.AccountManager manager=android.accounts.AccountManager.get(this);
                    JSONObject response=CoreSession.login(SystemAccounts.privateLogin(this,selectedAccount),manager.getPassword(selectedAccount));
                    JSONObject user=response.getJSONObject("user");
                    profile.email=user.optString("email",targetEmail);profile.onId=user.optString("on_id","");profile.username=user.optString("username","");profile.name=user.optString("name","");profile.surname=user.optString("surname","");profile.phone=user.optString("phone","");profile.avatarBase64=user.optString("avatar_base64","");
                    manager.setUserData(selectedAccount,"server_token",response.getString("token"));
                } catch(Exception error) {
                    runOnUiThread(()->{stateLoading.setVisibility(View.GONE);stateSelectAccount.setVisibility(View.VISIBLE);android.widget.Toast.makeText(this,"Sign in again to verify this account. "+error.getMessage(),android.widget.Toast.LENGTH_LONG).show();});return;
                }

                final CachedProfile finalProfile = profile;
                runOnUiThread(() -> {
                    if (isFinishing() || (android.os.Build.VERSION.SDK_INT >= 17 && isDestroyed())) return;
                    persistAndFinish(finalProfile);
                });
            }).start();
        });
    }

    private void persistAndFinish(CachedProfile profile) {
        SharedPreferences prefs = getSharedPreferences("ON_SECURE_PREFS", Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = prefs.edit();
        ed.putBoolean("logged_in", true);
        String serverToken=android.accounts.AccountManager.get(this).getUserData(selectedAccount,"server_token");
        ed.putString("server_token",serverToken);
        ed.putString("email", profile.email);
        ed.putString("on_id", profile.onId != null ? profile.onId : "");
        ed.putString("username", profile.username != null ? profile.username : "");
        ed.putString("name", profile.name != null ? profile.name : "");
        ed.putString("surname", profile.surname != null ? profile.surname : "");
        ed.putString("phone", profile.phone != null ? profile.phone : "");
        ed.putString("avatar_base64", profile.avatarBase64 != null ? profile.avatarBase64 : "");
        ed.commit();

        Intent resultIntent = new Intent();
        resultIntent.putExtra("ON_ACCOUNT", profile.email);
        resultIntent.putExtra("ON_ID", profile.onId != null ? profile.onId : "");
        resultIntent.putExtra("ON_USERNAME", profile.username != null ? profile.username : "");
        resultIntent.putExtra("ON_NAME", profile.name != null ? profile.name : "");
        resultIntent.putExtra("ON_SURNAME", profile.surname != null ? profile.surname : "");
        resultIntent.putExtra("ON_AVATAR", profile.avatarBase64 != null ? profile.avatarBase64 : "");
        resultIntent.putExtra("AUTH_TOKEN", serverToken);

        String caller=getCallingPackage();
        boolean trusted=caller!=null && (caller.equals("com.oldschoolnetwork.store")||caller.equals("com.oldschoolnetwork.services.classicinstaller")) && getPackageManager().checkSignatures(getPackageName(),caller)==android.content.pm.PackageManager.SIGNATURE_MATCH;
        if(!trusted){resultIntent.removeExtra("ON_ACCOUNT");resultIntent.removeExtra("ON_ID");resultIntent.removeExtra("ON_NAME");resultIntent.removeExtra("ON_SURNAME");resultIntent.removeExtra("AUTH_TOKEN");}
        setResult(Activity.RESULT_OK, resultIntent);
        closeWithAnimation();
    }

    private void finishWithCancel() {
        setResult(Activity.RESULT_CANCELED);
        closeWithAnimation();
    }

    private void setupEntryAnimation() {
        rootDimBackground.setAlpha(0f);
        bottomSheetContainer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @SuppressWarnings("deprecation")
            @Override
            public void onGlobalLayout() {
                bottomSheetContainer.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                bottomSheetContainer.setTranslationY(bottomSheetContainer.getHeight());
                rootDimBackground.animate().alpha(1f).setDuration(250).start();
                bottomSheetContainer.animate()
                        .translationY(0f)
                        .setDuration(300)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
            }
        });
    }

    private void closeWithAnimation() {
        if (isClosing) return;
        isClosing = true;

        bottomSheetContainer.animate()
                .translationY(bottomSheetContainer.getHeight())
                .setDuration(250)
                .setInterpolator(new AccelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        rootDimBackground.animate()
                                .alpha(0f)
                                .setDuration(200)
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        finish();
                                        overridePendingTransition(0, 0);
                                    }
                                }).start();
                    }
                }).start();
    }

    @Override
    public void onBackPressed() {
        if (isClosing) return;
        if (statePermissions.getVisibility() == View.VISIBLE) {
            statePermissions.setVisibility(View.GONE);
            stateSelectAccount.setVisibility(View.VISIBLE);
        } else if (stateLoading.getVisibility() == View.VISIBLE) {
            return;
        } else {
            finishWithCancel();
        }
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }
}