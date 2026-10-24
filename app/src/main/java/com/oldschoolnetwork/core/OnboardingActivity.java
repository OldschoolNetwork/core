package com.oldschoolnetwork.core;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.Base64;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewFlipper;

import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.Random;

public class OnboardingActivity extends Activity {
    public void openAccountRecovery(android.view.View view) { startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://oldschoolnetwork.online/account/recovery/"))); }
    public void openAccountHelp(android.view.View view) {
        try { startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://oldschoolnetwork.online/downloads/docs/categories/account/"))); }
        catch (android.content.ActivityNotFoundException error) { android.widget.Toast.makeText(this, "Open oldschoolnetwork.online/downloads/docs/categories/account/ for account help.", android.widget.Toast.LENGTH_LONG).show(); }
    }


    private final int SCREEN_VERIFY = 0;
    private final int SCREEN_LOGIN_EMAIL = 1;
    private final int SCREEN_LOGIN_PASS = 2;
    private final int SCREEN_CREATE_EMAIL = 3;
    private final int SCREEN_CREATE_PASS = 4;
    private final int SCREEN_OTP = 5;
    private final int SCREEN_DETAILS = 6;
    private final int SCREEN_TERMS = 7;
    private final int SCREEN_CONFIRM = 8;
    private final int SCREEN_SETUP = 9;
    private final int SCREEN_PARAMS = 10;
    private final int SCREEN_WELCOME = 11;

    private boolean isLoginFlow = false;
    private String validatedRealEmail = "";
    private String currentOnId = "";
    private String currentUsername = "";
    private String currentName = "";
    private String currentSurname = "";
    private String currentPhone = "";
    private String currentAvatar = "";

    private AccountAuthenticatorResponse authResponse;
    private Bundle authResultBundle;

    private ViewFlipper flipper;
    private boolean acceptedCurrentTerms;
    private WebView termsWebView;
    private ImageView btnNextTerms, btnCreatePasswordVis, btnLoginPasswordVis, ivFinalAvatar, ivLoginAvatar, btnConfirmPasswordVis;
    private TextView tvWelcomeText, tvOtpHeader, tvLoginWelcomeTitle, tvLoginSubtitleEmail, tvVerifyStatus, tvSetupStatus;

    // Ошибки
    private TextView tvLoginEmailError, tvLoginPassError, tvCreateEmailError, tvCreatePassError, tvDetailsError;
    private TextView tvNameCounter, tvSurnameCounter, tvUsernameCounter;

    private static final int MAX_NAME_LEN = 50;
    private static final int MAX_SURNAME_LEN = 50;
    private static final int MAX_USERNAME_LEN = 25;

    // Полоски
    private ProgressBar pbLoginEmail, pbLoginPass, pbCreateEmail, verifyProgress, setupProgress;

    private EditText etLoginEmail, etLoginPassword;
    private EditText etCreateEmail, etCreatePassword, etRegName, etRegSurname, etRegUsername;

    private TextView tvConfirmEmail;
    private EditText etConfirmPassword;
    private Button btnNextConfirm, btnTryAgainVerify, btnTryAgainSetup;
    private LinearLayout btnStart;

    private CheckBox cbCloud, cbLoc, cbData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_onboarding);

        if (getIntent() != null) {
            authResponse = getIntent().getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
            if (authResponse != null) {
                authResponse.onRequestContinued();
            }
        }

        initViews();
        setupWebViews();
        setupNavigation();
        setupPasswordToggles();
        checkDeviceSecurityAndNetwork();
    }

    private void initViews() {
        flipper = (ViewFlipper) findViewById(R.id.onboardingFlipper);
        findViewById(R.id.btnSendOtp).setOnClickListener(v -> new WebAppInterface().onOtpSuccess());
        termsWebView = (WebView) findViewById(R.id.termsWebView);

        btnNextTerms = (ImageView) findViewById(R.id.btnNextTerms);
        btnCreatePasswordVis = (ImageView) findViewById(R.id.btnCreatePasswordVis);
        btnLoginPasswordVis = (ImageView) findViewById(R.id.btnLoginPasswordVis);
        ivFinalAvatar = (ImageView) findViewById(R.id.ivFinalAvatar);
        ivLoginAvatar = (ImageView) findViewById(R.id.ivLoginAvatar);
        btnStart = (LinearLayout) findViewById(R.id.btnStart);

        tvWelcomeText = (TextView) findViewById(R.id.tvWelcomeText);
        tvOtpHeader = (TextView) findViewById(R.id.tvOtpHeader);
        tvLoginWelcomeTitle = (TextView) findViewById(R.id.tvLoginWelcomeTitle);
        tvLoginSubtitleEmail = (TextView) findViewById(R.id.tvLoginSubtitleEmail);
        tvVerifyStatus = (TextView) findViewById(R.id.tvVerifyStatus);
        tvSetupStatus = (TextView) findViewById(R.id.tvSetupStatus);

        etLoginEmail = (EditText) findViewById(R.id.etLoginEmail);
        etLoginPassword = (EditText) findViewById(R.id.etLoginPassword);
        etCreateEmail = (EditText) findViewById(R.id.etCreateEmail);
        etCreatePassword = (EditText) findViewById(R.id.etCreatePassword);
        etRegName = (EditText) findViewById(R.id.etRegName);
        etRegSurname = (EditText) findViewById(R.id.etRegSurname);
        etRegUsername = (EditText) findViewById(R.id.etRegUsername);

        tvConfirmEmail = (TextView) findViewById(R.id.tvConfirmEmail);
        etConfirmPassword = (EditText) findViewById(R.id.etConfirmPassword);
        btnConfirmPasswordVis = (ImageView) findViewById(R.id.btnConfirmPasswordVis);
        btnNextConfirm = (Button) findViewById(R.id.btnNextConfirm);
        btnTryAgainVerify = (Button) findViewById(R.id.btnTryAgainVerify);
        btnTryAgainSetup = (Button) findViewById(R.id.btnTryAgainSetup);

        tvLoginEmailError = (TextView) findViewById(R.id.tvLoginEmailError);
        tvLoginPassError = (TextView) findViewById(R.id.tvLoginPassError);
        tvCreateEmailError = (TextView) findViewById(R.id.tvCreateEmailError);
        tvCreatePassError = (TextView) findViewById(R.id.tvCreatePassError);
        tvDetailsError = (TextView) findViewById(R.id.tvDetailsError);
        tvNameCounter = (TextView) findViewById(R.id.tvNameCounter);
        tvSurnameCounter = (TextView) findViewById(R.id.tvSurnameCounter);
        tvUsernameCounter = (TextView) findViewById(R.id.tvUsernameCounter);

        pbLoginEmail = (ProgressBar) findViewById(R.id.pbLoginEmail);
        pbLoginPass = (ProgressBar) findViewById(R.id.pbLoginPass);
        pbCreateEmail = (ProgressBar) findViewById(R.id.pbCreateEmail);
        verifyProgress = (ProgressBar) findViewById(R.id.verifyProgress);
        setupProgress = (ProgressBar) findViewById(R.id.setupProgress);

        cbCloud = (CheckBox) findViewById(R.id.cbCloud);
        cbLoc = (CheckBox) findViewById(R.id.cbLoc);
        cbData = (CheckBox) findViewById(R.id.cbData);

        setupCounter(etRegName, tvNameCounter, MAX_NAME_LEN);
        setupCounter(etRegSurname, tvSurnameCounter, MAX_SURNAME_LEN);
        setupCounter(etRegUsername, tvUsernameCounter, MAX_USERNAME_LEN);
    }

    private void setupCounter(final EditText editText, final TextView counterView, final int maxLen) {
        counterView.setText("0/" + maxLen);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                int len = s.length();
                counterView.setText(len + "/" + maxLen);
                if (len > maxLen) {
                    counterView.setTextColor(0xFFFF5252);
                } else {
                    counterView.setTextColor(0xFF888888);
                }
            }
        });
    }

    private boolean isOverLimit(EditText editText, int maxLen) {
        return editText.getText().toString().length() > maxLen;
    }

    private void setBase64Avatar(String base64, ImageView imageView) {
        if (base64 != null && !base64.trim().isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(base64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                if (bitmap != null) {
                    int size = Math.min(bitmap.getWidth(), bitmap.getHeight());
                    int x = (bitmap.getWidth() - size) / 2;
                    int y = (bitmap.getHeight() - size) / 2;
                    Bitmap squaredBitmap = Bitmap.createBitmap(bitmap, x, y, size, size);

                    RoundedBitmapDrawable roundedBitmapDrawable = RoundedBitmapDrawableFactory.create(getResources(), squaredBitmap);
                    roundedBitmapDrawable.setCircular(true);
                    imageView.setImageDrawable(roundedBitmapDrawable);
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        imageView.setImageResource(R.drawable.ic_profile_placeholder);
    }

    private void checkDeviceSecurityAndNetwork() {
        new Thread(() -> {
            try {
                boolean isRooted = new File("/system/xbin/su").exists() || new File("/system/bin/su").exists();
                boolean isCustom = android.os.Build.TAGS != null && android.os.Build.TAGS.contains("test-keys");
                String region = Locale.getDefault().getCountry();

                URL url = new URL("https://api.oldschoolnetwork.online/v1/on-account/api/check_user.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(5000);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                String payload = "{\"login\":\"ping\", \"root\":" + isRooted + ", \"custom\":" + isCustom + ", \"region\":\"" + region + "\"}";
                OutputStream os = conn.getOutputStream();
                os.write(payload.getBytes("UTF-8"));
                os.close();

                if (conn.getResponseCode() == 200) {
                    runOnUiThread(() -> {if(!isFinishing()&&flipper.getDisplayedChild()==SCREEN_VERIFY)flipper.setDisplayedChild(SCREEN_LOGIN_EMAIL);});
                } else {
                    throw new Exception("Server down");
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    tvVerifyStatus.setText("Connection failed.\nCheck your internet.");
                    tvVerifyStatus.setTextColor(0xFFFF5252);
                    verifyProgress.setVisibility(View.GONE);
                    btnTryAgainVerify.setVisibility(View.VISIBLE);
                });
            }
        }).start();
    }

    private boolean isLoginFormatValid(String login) {
        return login.trim().length() >= 3;
    }

    private void setupNavigation() {
        btnTryAgainVerify.setOnClickListener(v -> {
            btnTryAgainVerify.setVisibility(View.GONE);
            verifyProgress.setVisibility(View.VISIBLE);
            tvVerifyStatus.setText("Verifying details...");
            tvVerifyStatus.setTextColor(0xFF00BCD4);
            checkDeviceSecurityAndNetwork();
        });

        btnTryAgainSetup.setOnClickListener(v -> {
            btnTryAgainSetup.setVisibility(View.GONE);
            setupProgress.setVisibility(View.VISIBLE);
            tvSetupStatus.setText("Setting up services for you.\nThis may take a while.");
            tvSetupStatus.setTextColor(0xFF00BCD4);
            registerUserAPI();
        });

        findViewById(R.id.btnGoToCreate).setOnClickListener(v -> flipper.setDisplayedChild(SCREEN_CREATE_EMAIL));

        findViewById(R.id.btnNextLogin).setOnClickListener(v -> {
            String login = etLoginEmail.getText().toString().trim();
            if (!isLoginFormatValid(login)) {
                tvLoginEmailError.setText("Invalid Email, Username, Phone or ON ID format");
                tvLoginEmailError.setVisibility(View.VISIBLE);
                return;
            }
            tvLoginEmailError.setVisibility(View.GONE);
            checkUserExistsAPI(login, true);
        });

        findViewById(R.id.btnBackLogin).setOnClickListener(v -> finish());

        findViewById(R.id.btnNextLoginPass).setOnClickListener(v -> {
            String pass = etLoginPassword.getText().toString();
            if (pass.isEmpty()) {
                tvLoginPassError.setText("Enter your password");
                tvLoginPassError.setVisibility(View.VISIBLE);
                return;
            }
            tvLoginPassError.setVisibility(View.GONE);
            verifyPasswordAPI(validatedRealEmail.isEmpty() ? etLoginEmail.getText().toString().trim() : validatedRealEmail, pass);
        });

        findViewById(R.id.btnBackLoginPass).setOnClickListener(v -> flipper.setDisplayedChild(SCREEN_LOGIN_EMAIL));

        findViewById(R.id.btnNextCreateEmail).setOnClickListener(v -> {
            String email = etCreateEmail.getText().toString().trim();
            if (!email.contains("@") || !email.contains(".")) {
                tvCreateEmailError.setText("Invalid email address");
                tvCreateEmailError.setVisibility(View.VISIBLE);
                return;
            }
            tvCreateEmailError.setVisibility(View.GONE);
            checkUserExistsAPI(email, false);
        });

        findViewById(R.id.btnBackCreateEmail).setOnClickListener(v -> flipper.setDisplayedChild(SCREEN_LOGIN_EMAIL));

        findViewById(R.id.btnNextCreatePass).setOnClickListener(v -> {
            String pass = etCreatePassword.getText().toString();
            if (pass.length() < 8 || !pass.matches(".*[a-zA-Z].*") || !pass.matches(".*\\d.*")) {
                tvCreatePassError.setVisibility(View.VISIBLE);
                return;
            }
            tvCreatePassError.setVisibility(View.GONE);

            isLoginFlow = false;
            tvOtpHeader.setText("Verification");
            flipper.setDisplayedChild(SCREEN_OTP);

            findViewById(R.id.btnSendOtp).requestFocus(View.FOCUS_DOWN);
        });

        findViewById(R.id.btnBackCreatePass).setOnClickListener(v -> flipper.setDisplayedChild(SCREEN_CREATE_EMAIL));

        findViewById(R.id.btnBackOtp).setOnClickListener(v -> flipper.setDisplayedChild(isLoginFlow ? SCREEN_LOGIN_PASS : SCREEN_CREATE_PASS));

        findViewById(R.id.btnNextDetails).setOnClickListener(v -> {
            if(etRegName.getText().toString().trim().isEmpty()) {
                tvDetailsError.setVisibility(View.VISIBLE);
                return;
            }
            if (isOverLimit(etRegName, MAX_NAME_LEN)
                    || isOverLimit(etRegSurname, MAX_SURNAME_LEN)
                    || isOverLimit(etRegUsername, MAX_USERNAME_LEN)) {
                return;
            }
            tvDetailsError.setVisibility(View.GONE);
            processUsernameLogic();
            flipper.setDisplayedChild(SCREEN_TERMS);
            acceptedCurrentTerms = false; btnNextTerms.setClickable(false); btnNextTerms.setAlpha(0.3f);
            loadCurrentTerms();
            termsWebView.requestFocus(View.FOCUS_DOWN);
        });

        findViewById(R.id.btnBackDetails).setOnClickListener(v -> flipper.setDisplayedChild(SCREEN_CREATE_PASS));

        btnNextTerms.setOnClickListener(v -> {
            tvConfirmEmail.setText(etCreateEmail.getText().toString());
            etConfirmPassword.setText(etCreatePassword.getText().toString());
            flipper.setDisplayedChild(SCREEN_CONFIRM);
        });

        findViewById(R.id.btnBackTerms).setOnClickListener(v -> flipper.setDisplayedChild(SCREEN_DETAILS));

        btnNextConfirm.setOnClickListener(v -> {
            flipper.setDisplayedChild(SCREEN_SETUP);
            registerUserAPI();
        });

        findViewById(R.id.btnBackConfirm).setOnClickListener(v -> flipper.setDisplayedChild(SCREEN_TERMS));

        findViewById(R.id.btnNextParams).setOnClickListener(v -> {
            saveUserPreferences();
            SharedPreferences prefs = getSharedPreferences("ON_SECURE_PREFS", Context.MODE_PRIVATE);

            String displayName;
            String userEmail = isLoginFlow ? validatedRealEmail : etCreateEmail.getText().toString().trim();

            if (isLoginFlow) {
                String name = prefs.getString("name", "");
                String surname = prefs.getString("surname", "");
                String fullName = (name + " " + surname).trim();

                if (!fullName.isEmpty()) {
                    displayName = fullName;
                } else {
                    displayName = prefs.getString("username", userEmail);
                }
            } else {
                String name = etRegName.getText().toString().trim();
                String surname = etRegSurname.getText().toString().trim();
                displayName = (name + " " + surname).trim();
            }

            tvWelcomeText.setText("Welcome, " + displayName + "!");

            // Динамически запрашиваем актуальную аватарку с сервера именно для этого юзера
            fetchAndShowWelcomeData(userEmail);

            flipper.setDisplayedChild(SCREEN_WELCOME);
        });

        findViewById(R.id.btnBackParams).setOnClickListener(v -> flipper.setDisplayedChild(isLoginFlow ? SCREEN_OTP : SCREEN_TERMS));

        btnStart.setOnClickListener(v -> {
            if (getSharedPreferences("ON_SECURE_PREFS", Context.MODE_PRIVATE).getString("server_token", "").isEmpty()) {
                android.widget.Toast.makeText(this, "Finishing sign-in. Please wait or retry if the connection failed.", android.widget.Toast.LENGTH_LONG).show();
                addAccountToSystem();
                return;
            }
            finish();
        });
    }

    // НОВЫЙ МЕТОД: Запрашиваем 100% актуальную аву с сервера перед экраном Welcome
    private void fetchAndShowWelcomeData(String email) {
        if (currentAvatar != null && !currentAvatar.isEmpty()) {
            setBase64Avatar(currentAvatar, ivFinalAvatar);
        } else {
            ivFinalAvatar.setImageResource(R.drawable.ic_profile_placeholder);
        }

        new Thread(() -> {
            try {
                URL url = new URL("https://api.oldschoolnetwork.online/v1/on-account/api/check_user.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("login", email);

                OutputStream os = conn.getOutputStream();
                os.write(jsonParam.toString().getBytes("UTF-8"));
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
                        String fetchedAvatar = userObj.optString("avatar_base64", "");
                        currentAvatar = fetchedAvatar;

                        // Сразу сохраняем актуальную аву в кэш для Dashboard
                        SharedPreferences.Editor ed = getSharedPreferences("ON_SECURE_PREFS", Context.MODE_PRIVATE).edit();
                        ed.putString("avatar_base64", fetchedAvatar);
                        ed.commit();

                        runOnUiThread(() -> setBase64Avatar(fetchedAvatar, ivFinalAvatar));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void checkUserExistsAPI(final String login, final boolean isForLogin) {
        runOnUiThread(() -> {
            if (isForLogin) pbLoginEmail.setVisibility(View.VISIBLE);
            else pbCreateEmail.setVisibility(View.VISIBLE);
        });

        new Thread(() -> {
            try {
                URL url = new URL("https://api.oldschoolnetwork.online/v1/on-account/api/check_user.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("login", login);

                OutputStream os = conn.getOutputStream();
                os.write(jsonParam.toString().getBytes("UTF-8"));
                os.close();

                InputStream is = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                final JSONObject res = new JSONObject(response.toString());
                final boolean exists = res.optBoolean("exists", false);
                final JSONObject userObj = res.optJSONObject("user");

                final String realEmail = (userObj != null && !userObj.optString("email", "").isEmpty())
                        ? userObj.optString("email", login)
                        : res.optString("email", login);
                final String avatarBase64 = (userObj != null && !userObj.optString("avatar_base64", "").isEmpty())
                        ? userObj.optString("avatar_base64", "")
                        : res.optString("avatar_base64", "");

                if (userObj != null) {
                    currentOnId = userObj.optString("on_id", currentOnId);
                    currentUsername = userObj.optString("username", currentUsername);
                    currentName = userObj.optString("name", currentName);
                    currentSurname = userObj.optString("surname", currentSurname);
                    currentPhone = userObj.optString("phone", currentPhone);
                    currentAvatar = avatarBase64;
                }

                runOnUiThread(() -> {
                    pbLoginEmail.setVisibility(View.GONE);
                    pbCreateEmail.setVisibility(View.GONE);

                    if (isForLogin) {
                        if (exists) {
                            isLoginFlow = true;
                            validatedRealEmail = realEmail;
                            tvLoginSubtitleEmail.setText(realEmail);

                            setBase64Avatar(avatarBase64, ivLoginAvatar);

                            flipper.setDisplayedChild(SCREEN_LOGIN_PASS);
                        } else {
                            tvLoginEmailError.setText("Account not found. Please click Create ON Account.");
                            tvLoginEmailError.setVisibility(View.VISIBLE);
                        }
                    } else {
                        if (exists) {
                            tvCreateEmailError.setText("Account already exists. Please log in.");
                            tvCreateEmailError.setVisibility(View.VISIBLE);
                        } else {
                            flipper.setDisplayedChild(SCREEN_CREATE_PASS);
                        }
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    pbLoginEmail.setVisibility(View.GONE);
                    pbCreateEmail.setVisibility(View.GONE);
                    TextView targetError = isForLogin ? tvLoginEmailError : tvCreateEmailError;
                    targetError.setText("Network error. Try again.");
                    targetError.setVisibility(View.VISIBLE);
                });
            }
        }).start();
    }

    private void verifyPasswordAPI(final String email, final String password) {
        runOnUiThread(() -> pbLoginPass.setVisibility(View.VISIBLE));

        new Thread(() -> {
            try {
                URL url = new URL("https://api.oldschoolnetwork.online/v1/on-account/api/login.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("login", email);
                jsonParam.put("password", password);

                OutputStream os = conn.getOutputStream();
                os.write(jsonParam.toString().getBytes("UTF-8"));
                os.close();

                InputStream is = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                final JSONObject res = new JSONObject(response.toString());
                final String status = res.optString("status", "");

                runOnUiThread(() -> {
                    pbLoginPass.setVisibility(View.GONE);
                    if (status.equals("success")) {
                        JSONObject userObj = res.optJSONObject("user");
                        if (userObj != null) {
                            validatedRealEmail = userObj.optString("email", email);
                            currentOnId = userObj.optString("on_id", currentOnId);
                            currentUsername = userObj.optString("username", currentUsername);
                            currentName = userObj.optString("name", currentName);
                            currentSurname = userObj.optString("surname", currentSurname);
                            currentPhone = userObj.optString("phone", currentPhone);
                            currentAvatar = userObj.optString("avatar_base64", currentAvatar);
                            for (Account existing : AccountManager.get(this).getAccountsByType(ONAccount.ACCOUNT_TYPE)) {
                                String id=AccountManager.get(this).getUserData(existing,"on_id"), nick=AccountManager.get(this).getUserData(existing,"username"), mail=AccountManager.get(this).getUserData(existing,"email");
                                if ((!currentOnId.isEmpty() && currentOnId.equalsIgnoreCase(id)) || existing.name.equalsIgnoreCase("@"+currentUsername) || (nick!=null&&nick.equalsIgnoreCase(currentUsername)) || (mail!=null&&mail.equalsIgnoreCase(validatedRealEmail))) {
                                    verifyingSession=false;
                                    new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Account already added").setMessage("This ON Account is already on this device. Select it from Switch account.").setPositiveButton("OK",(dialog,which)->finish()).show();
                                    return;
                                }
                            }


                            SharedPreferences.Editor ed = getSharedPreferences("ON_SECURE_PREFS", Context.MODE_PRIVATE).edit();
                            ed.putString("email", validatedRealEmail);
                            ed.putString("on_id", currentOnId);
                            ed.putString("username", currentUsername);
                            ed.putString("name", currentName);
                            ed.putString("surname", currentSurname);
                            ed.putString("phone", currentPhone);
                            ed.putString("avatar_base64", currentAvatar);
                            ed.commit();
                        }

                        pendingServerToken = res.optString("token", "");
                        tvOtpHeader.setText("2-step verification");
                        flipper.setDisplayedChild(SCREEN_OTP);

                        findViewById(R.id.btnSendOtp).requestFocus(View.FOCUS_DOWN);
                    } else {
                        tvLoginPassError.setText("Incorrect password. Try again.");
                        tvLoginPassError.setVisibility(View.VISIBLE);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    pbLoginPass.setVisibility(View.GONE);
                    tvLoginPassError.setText("Network error. Try again.");
                    tvLoginPassError.setVisibility(View.VISIBLE);
                });
            }
        }).start();
    }

    private void loadCurrentTerms() {
        new Thread(() -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL("https://api.oldschoolnetwork.online/v1/on-account/web/terms.html").openConnection();
                connection.setConnectTimeout(15000); connection.setReadTimeout(20000);
                StringBuilder html = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                    String line; while ((line=reader.readLine())!=null) html.append(line).append('\n');
                } finally { connection.disconnect(); }
                if (!html.toString().contains("var termsVersion='2026-10-19.1'")) throw new java.io.IOException("The server has not published the current rules yet.");
                runOnUiThread(() -> termsWebView.loadDataWithBaseURL("https://api.oldschoolnetwork.online/v1/on-account/web/terms.html", html.toString(), "text/html", "UTF-8", null));
            } catch (Exception error) { runOnUiThread(() -> {
                String message = error.getMessage() != null && error.getMessage().contains("current rules")
                        ? "The server has not published the current Platform Rules yet. Registration will be available after the server update."
                        : "Could not load the Platform Rules. Check your connection and return to this step to try again.";
                termsWebView.loadData("<html><body><p>" + message + "</p></body></html>", "text/html", "UTF-8");
                acceptedCurrentTerms=false; btnNextTerms.setClickable(false); btnNextTerms.setAlpha(0.3f);
            }); }
        }, "on-core-terms").start();
    }
    private void registerUserAPI() {
        if (!acceptedCurrentTerms) { flipper.setDisplayedChild(SCREEN_TERMS); loadCurrentTerms(); return; }
        new Thread(() -> {
            try {
                URL url = new URL("https://api.oldschoolnetwork.online/v1/on-account/api/register.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("terms_accepted", true); jsonParam.put("terms_version", "2026-10-19.1");
                jsonParam.put("email", etCreateEmail.getText().toString().trim());
                jsonParam.put("password", etCreatePassword.getText().toString());
                jsonParam.put("name", etRegName.getText().toString().trim());
                jsonParam.put("surname", etRegSurname.getText().toString().trim());
                jsonParam.put("username", etRegUsername.getText().toString().replace("@","").trim());

                OutputStream os = conn.getOutputStream();
                os.write(jsonParam.toString().getBytes("UTF-8"));
                os.close();

                InputStream is = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                final JSONObject res = new JSONObject(response.toString());

                runOnUiThread(() -> {
                    if ("success".equals(res.optString("status"))) {
                        currentOnId = res.optString("on_id", "");
                        currentName = etRegName.getText().toString().trim();
                        currentSurname = etRegSurname.getText().toString().trim();
                        currentUsername = etRegUsername.getText().toString().replace("@", "").trim();
                        currentPhone = "";
                        currentAvatar = "";
                        getSharedPreferences("ON_SECURE_PREFS", Context.MODE_PRIVATE)
                                .edit().putString("on_id", currentOnId).commit();

                        addAccountToSystem(etCreateEmail.getText().toString().trim(), etCreatePassword.getText().toString());
                        flipper.setDisplayedChild(SCREEN_PARAMS);
                    } else {
                        String serverError = res.optString("message", "Unknown server error");
                        tvSetupStatus.setText("Server Error:\n" + serverError);
                        tvSetupStatus.setTextColor(0xFFFF5252);
                        setupProgress.setVisibility(View.GONE);
                        btnTryAgainSetup.setVisibility(View.VISIBLE);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    tvSetupStatus.setText("Network error.\nPlease try again later.");
                    tvSetupStatus.setTextColor(0xFFFF5252);
                    setupProgress.setVisibility(View.GONE);
                    btnTryAgainSetup.setVisibility(View.VISIBLE);
                });
            }
        }).start();
    }

    private void saveUserPreferences() {
        SharedPreferences prefs = getSharedPreferences("ON_SECURE_PREFS", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("cloud_sync", cbCloud.isChecked());
        editor.putBoolean("location_enabled", cbLoc.isChecked());
        editor.putBoolean("analytics_enabled", cbData.isChecked());
        editor.commit();
    }

    private void addAccountToSystem() {
        String userEmail = isLoginFlow ? validatedRealEmail : etCreateEmail.getText().toString().trim();
        String password = isLoginFlow ? etLoginPassword.getText().toString() : etCreatePassword.getText().toString();
        addAccountToSystem(userEmail, password);
    }

    private boolean verifyingSession;
    private String pendingServerToken="";
    private void addAccountToSystem(String userEmail, String password) {
        if (isLoginFlow && !pendingServerToken.isEmpty()) { persistAccountToSystem(userEmail,password,pendingServerToken); pendingServerToken=""; return; }
        if (verifyingSession) return;
        verifyingSession = true;
        new Thread(()->{
            try{
                JSONObject response=CoreSession.login(userEmail,password);
                JSONObject user=response.getJSONObject("user");
                final String token=response.getString("token");
                runOnUiThread(()->{verifyingSession=false; currentOnId=user.optString("on_id",currentOnId);currentUsername=user.optString("username",currentUsername);persistAccountToSystem(userEmail,password,token);});
            }catch(Exception error){runOnUiThread(()->{verifyingSession=false; android.widget.Toast.makeText(this,error.getMessage(),android.widget.Toast.LENGTH_LONG).show();});}
        },"on-verify-session").start();
    }
    private void persistAccountToSystem(String userEmail, String password,String serverToken) {
        try {
            SharedPreferences prefs = getSharedPreferences("ON_SECURE_PREFS", Context.MODE_PRIVATE);

            if (currentOnId == null || currentOnId.isEmpty()) {
                currentOnId = prefs.getString("on_id", "");
            }
            if (isLoginFlow) {
                if (currentName == null || currentName.isEmpty()) currentName = prefs.getString("name", "");
                if (currentSurname == null || currentSurname.isEmpty()) currentSurname = prefs.getString("surname", "");
                if (currentUsername == null || currentUsername.isEmpty()) currentUsername = prefs.getString("username", "");
                if (currentPhone == null || currentPhone.isEmpty()) currentPhone = prefs.getString("phone", "");
                if (currentAvatar == null || currentAvatar.isEmpty()) currentAvatar = prefs.getString("avatar_base64", "");
            } else {
                currentName = etRegName.getText().toString().trim();
                currentSurname = etRegSurname.getText().toString().trim();
                currentUsername = etRegUsername.getText().toString().replace("@", "").trim();
                currentPhone = "";
                currentAvatar = "";
            }

            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            editor.putBoolean("logged_in", true);
            editor.putString("server_token",serverToken);
            editor.putString("email", userEmail);
            editor.putString("on_id", currentOnId);
            editor.putString("name", currentName);
            editor.putString("surname", currentSurname);
            editor.putString("username", currentUsername);
            editor.putString("phone", currentPhone);
            editor.putString("avatar_base64", currentAvatar);
            editor.commit();

            AccountManager am = AccountManager.get(this);
            Account account = SystemAccounts.publicAccount(currentUsername);
            Bundle ud = new Bundle();
            ud.putString("email",userEmail);
            ud.putString("on_id", currentOnId);
            ud.putString("username", currentUsername);
            ud.putString("name", currentName);
            ud.putString("surname", currentSurname);

            boolean added = am.addAccountExplicitly(account, password, ud);
            if (!added) {
                am.setPassword(account, password);
                am.setUserData(account,"email",userEmail);
                am.setUserData(account, "on_id", currentOnId);
                am.setUserData(account, "username", currentUsername);
                am.setUserData(account, "name", currentName);
                am.setUserData(account, "surname", currentSurname);
            }
            am.setUserData(account,"server_token",serverToken);
            SystemAccounts.visibility(this,am,account);

            if (authResponse != null) {
                Bundle result = new Bundle();
                result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
                result.putString(AccountManager.KEY_ACCOUNT_TYPE, ONAccount.ACCOUNT_TYPE);
                authResultBundle = result;
                authResponse.onResult(result);
            }
            setResult(Activity.RESULT_OK);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void finish() {
        if (authResponse != null) {
            if (authResultBundle == null) {
                authResponse.onError(AccountManager.ERROR_CODE_CANCELED, "Canceled");
            }
            authResponse = null;
        }
        super.finish();
    }

    private void setupPasswordToggles() {
        View.OnClickListener togglePass = v -> {
            EditText targetEt;
            if (v.getId() == R.id.btnCreatePasswordVis) targetEt = etCreatePassword;
            else if (v.getId() == R.id.btnConfirmPasswordVis) targetEt = etConfirmPassword;
            else targetEt = etLoginPassword;

            ImageView targetBtn = (ImageView) v;

            if (targetEt.getTransformationMethod() instanceof PasswordTransformationMethod) {
                targetEt.setTransformationMethod(null);
                targetEt.setSelection(targetEt.getText().length());
                targetBtn.setImageResource(R.drawable.ic_eye_closed);
            } else {
                targetEt.setTransformationMethod(new PasswordTransformationMethod());
                targetEt.setSelection(targetEt.getText().length());
                targetBtn.setImageResource(R.drawable.ic_eye_open);
            }
        };
        btnCreatePasswordVis.setOnClickListener(togglePass);
        btnLoginPasswordVis.setOnClickListener(togglePass);
        btnConfirmPasswordVis.setOnClickListener(togglePass);
    }

    private void processUsernameLogic() {
        String name = etRegName.getText().toString().toLowerCase();
        String email = etCreateEmail.getText().toString().toLowerCase();
        String username = etRegUsername.getText().toString().toLowerCase().replace("@", "");
        String finalUsername = "";

        if (username.trim().isEmpty()) {
            String cleanName = name.replaceAll("[^a-z0-9._]", "");
            String emailPart = email.contains("@") ? email.split("@")[0] : email;
            String cleanEmail = emailPart.replaceAll("[^a-z0-9._]", "");

            finalUsername = cleanName + "_" + cleanEmail;

            if (!finalUsername.matches(".*\\d.*")) {
                finalUsername += (10 + new Random().nextInt(90));
            }
        } else {
            finalUsername = username.replaceAll("[^a-z0-9._]", "");
        }

        if (finalUsername.length() > 25) {
            finalUsername = finalUsername.substring(0, 25);
        }

        if (finalUsername.isEmpty()) {
            finalUsername = "user_" + (1000 + new Random().nextInt(9000));
        }

        etRegUsername.setText("@" + finalUsername);
    }

    private void setupWebViews() {
        WebSettings termsSettings = termsWebView.getSettings();
        termsSettings.setJavaScriptEnabled(true);
        termsSettings.setDomStorageEnabled(true);
        termsWebView.setWebViewClient(new WebViewClient());
        termsWebView.addJavascriptInterface(new WebAppInterface(), "ONCore");
    }

    private class WebAppInterface {
        @JavascriptInterface
        public void onOtpSuccess() {
            runOnUiThread(() -> {
                if (isLoginFlow) {
                    addAccountToSystem();
                    flipper.setDisplayedChild(SCREEN_PARAMS);
                } else {
                    flipper.setDisplayedChild(SCREEN_DETAILS);
                }
            });
        }

        @JavascriptInterface
        public void onTermsAccepted() {
            runOnUiThread(() -> {
                acceptedCurrentTerms = true;
                btnNextTerms.setAlpha(1.0f);
                btnNextTerms.setClickable(true);
            });
        }

        @JavascriptInterface
        public void onTermsRevoked() {
            runOnUiThread(() -> {
                acceptedCurrentTerms = false;
                btnNextTerms.setAlpha(0.3f);
                btnNextTerms.setClickable(false);
            });
        }
    }
}
