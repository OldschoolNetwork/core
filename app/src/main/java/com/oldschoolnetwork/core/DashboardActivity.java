package com.oldschoolnetwork.core;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.tabs.TabLayout;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private NonSwipeableViewPager viewPager;
    private TextView tvUsername;
    private ImageView ivAvatar;
    private ImageView btnRefresh;

    private ONAccount onAccount;

    private List<SearchItem> allSearchItems = new ArrayList<>();
    private SearchAdapter globalSearchAdapter = new SearchAdapter();

    public SearchAdapter getSearchAdapter() {
        return globalSearchAdapter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ServiceUpdates.schedule(this);
        SystemAccounts.restoreAsync(this);
        setContentView(R.layout.activity_dashboard);

        onAccount = new ONAccount(this);

        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        tvUsername = findViewById(R.id.tv_username);
        ivAvatar = findViewById(R.id.iv_avatar);
        btnRefresh = findViewById(R.id.btn_refresh);

        btnRefresh.setOnClickListener(v -> {
            v.animate().rotationBy(360f).setDuration(500).start();
            syncSessionData();
        });

        buildSearchCatalog();
        setupViewPager();
    }

    @Override
    protected void onResume() {
        super.onResume();
        validateSessionIntegrity();

        // Магия: если разлогинились (кнопкой Sign Out), проверяем наличие других аккаунтов
        checkFallbackAccount();

        updateUI();
        syncSessionData();
    }

    private void validateSessionIntegrity() { SystemAccounts.restoreAsync(this); }

    // НОВЫЙ МЕТОД: Авто-прыжок на оставшиеся аккаунты
    private void checkFallbackAccount() {
        if(onAccount.isLoggedIn())return;
        AccountManager manager=AccountManager.get(this);
        for(Account account:manager.getAccountsByType(ONAccount.ACCOUNT_TYPE)){
            String token=manager.getUserData(account,"server_token");if(token==null||token.isEmpty())continue;
            SharedPreferences.Editor editor=getSharedPreferences("ON_SECURE_PREFS",Context.MODE_PRIVATE).edit().putBoolean("logged_in",true).putString("server_token",token).putString("email",SystemAccounts.privateLogin(this,account));
            for(String key:new String[]{"username","name","surname","on_id","avatar_base64"}){String value=manager.getUserData(account,key);editor.putString(key,value==null?"":value);}editor.apply();return;
        }
    }

    private void syncSessionData() {
        if (!onAccount.isLoggedIn() || onAccount.getEmail().isEmpty()) {
            return;
        }

        final String currentEmail = onAccount.getEmail();
        final String currentToken=onAccount.getToken();

        new Thread(() -> {
            try {
                URL url = new URL("https://api.oldschoolnetwork.online/v1/on-account/api/ecosystem.php?action=session_check");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setConnectTimeout(5000);
                conn.setDoOutput(true);

                String payload = new JSONObject().put("token",currentToken).toString();
                OutputStream os = conn.getOutputStream();
                os.write(payload.getBytes("UTF-8"));
                os.close();

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                    reader.close();

                    JSONObject res = new JSONObject(response.toString());
                    if(!currentToken.equals(onAccount.getToken()))return;
                    if (res.optJSONObject("user")!=null) {
                        JSONObject userObj = res.optJSONObject("user");
                        if (userObj != null) {
                            SharedPreferences.Editor ed = getSharedPreferences("ON_SECURE_PREFS", Context.MODE_PRIVATE).edit();
                            ed.putBoolean("logged_in", true);
                            ed.putString("email", userObj.optString("email", currentEmail));
                            ed.putString("on_id", userObj.optString("on_id", onAccount.getOnId()));
                            ed.putString("username", userObj.optString("username", onAccount.getUsername()));
                            ed.putString("name", userObj.optString("name", onAccount.getName()));
                            ed.putString("surname", userObj.optString("surname", onAccount.getSurname()));
                            ed.putString("phone", userObj.optString("phone", onAccount.getPhoneNumber()));
                            ed.putString("avatar_base64", userObj.optString("avatar_base64", ""));
                            ed.commit();
                        }
                        runOnUiThread(this::updateUI);
                    } else if ("deleted".equals(res.optString("status"))
                            || (res.has("exists") && !res.optBoolean("exists") && !res.has("error") && !"error".equals(res.optString("status")))) {
                        // Аккаунт не существует на сервере (Удален)
                        runOnUiThread(this::handleDeletedAccount);
                    }
                }
            } catch (Exception e) {
                // Игнорируем ошибку (если нет интернета, просто остаемся в текущей сессии)
            }
        }).start();
    }

    private void handleDeletedAccount() {
        String deletedEmail = onAccount.getEmail();

        // 1. Сбрасываем локальную сессию мертвого аккаунта
        onAccount.logout();

        // 2. Выпиливаем его из системного AccountManager
        AccountManager am = AccountManager.get(this);
        Account[] accounts = am.getAccountsByType(ONAccount.ACCOUNT_TYPE);
        for (Account acc : accounts) {
            if (acc.name.equals(deletedEmail)) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                    am.removeAccountExplicitly(acc);
                } else {
                    am.removeAccount(acc, null, null);
                }
                break;
            }
        }

        // 3. Автоматически пытаемся переключиться на другой аккаунт
        checkFallbackAccount();

        // 4. Обновляем интерфейс
        updateUI();

        // 5. Если удалось переключиться — сразу тянем данные нового аккаунта
        if (onAccount.isLoggedIn()) {
            syncSessionData();
        }

        // 6. Уведомляем юзера
        new android.app.AlertDialog.Builder(this)
                .setTitle("Account Deleted")
                .setMessage("Your account was deleted from OldSchool Network servers by an administrator. You will no longer be able to log into it.")
                .setPositiveButton("OK", null)
                .setCancelable(false)
                .show();
    }

    private void updateUI() {
        View btnAccountHeader = findViewById(R.id.btn_account_header);
        View btnManageProfile = findViewById(R.id.btn_manage_profile);
        TextView tvEmail = findViewById(R.id.tv_email);
        TextView tvOnId = findViewById(R.id.tv_on_id);

        try {
            if (onAccount.isLoggedIn() && !onAccount.getEmail().isEmpty()) {
                String fullName = (onAccount.getName() + " " + onAccount.getSurname()).trim();
                String displayUsername = !onAccount.getUsername().isEmpty() ? "@" + onAccount.getUsername() : onAccount.getEmail();
                tvUsername.setText(fullName.isEmpty() ? displayUsername : fullName);

                if (tvEmail != null) {
                    tvEmail.setVisibility(View.VISIBLE);
                    tvEmail.setText(!onAccount.getUsername().isEmpty()
                            ? "@" + onAccount.getUsername() + " • " + onAccount.getEmail()
                            : onAccount.getEmail());
                }
                if (tvOnId != null) {
                    tvOnId.setVisibility(View.VISIBLE);
                    tvOnId.setText("ID: " + onAccount.getOnId());
                }
                if (btnManageProfile != null) {
                    btnManageProfile.setVisibility(View.VISIBLE);
                    btnManageProfile.setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this, ProfilesActivity.class)));
                }

                String base64 = onAccount.getAvatar();
                if (base64 != null && !base64.isEmpty()) {
                    byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    if (bitmap != null) {
                        ivAvatar.setImageBitmap(bitmap);
                    } else {
                        ivAvatar.setImageResource(R.drawable.default_avatar);
                    }
                } else {
                    ivAvatar.setImageResource(R.drawable.default_avatar);
                }

                btnAccountHeader.setOnClickListener(v -> showAccountSwitcher());
            } else {
                tvUsername.setText("Log in");
                ivAvatar.setImageResource(R.drawable.unknown_avatar);

                if (tvEmail != null) tvEmail.setVisibility(View.GONE);
                if (tvOnId != null) tvOnId.setVisibility(View.GONE);
                if (btnManageProfile != null) btnManageProfile.setVisibility(View.GONE);

                btnAccountHeader.setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this, OnboardingActivity.class)));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showAccountSwitcher() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.layout_account_switcher, null);
        LinearLayout container = view.findViewById(R.id.account_list_container);

        view.findViewById(R.id.btn_close_switcher).setOnClickListener(v -> dialog.dismiss());

        View btnAddAccount = view.findViewById(R.id.btn_add_account);
        if (btnAddAccount != null) {
            btnAddAccount.setOnClickListener(v -> {
                dialog.dismiss();
                startActivity(new Intent(DashboardActivity.this, OnboardingActivity.class));
            });
        }

        View btnAccountSettings = view.findViewById(R.id.btn_account_settings);
        if (btnAccountSettings != null) {
            btnAccountSettings.setOnClickListener(v -> {
                dialog.dismiss();
                startActivity(new Intent(android.provider.Settings.ACTION_SYNC_SETTINGS));
            });
        }

        AccountManager am = AccountManager.get(this);
        Account[] sysAccounts = am.getAccountsByType(ONAccount.ACCOUNT_TYPE);
        List<Account> accountList = new ArrayList<>();
        for (Account a : sysAccounts) {
            accountList.add(a);
        }
        if (onAccount.isLoggedIn() && !onAccount.getEmail().isEmpty()) {
            boolean found = false;
            for (Account a : accountList) {
                if (SystemAccounts.privateLogin(this,a).equalsIgnoreCase(onAccount.getEmail())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                accountList.add(0, SystemAccounts.publicAccount(onAccount.getUsername()));
            }
        }

        for (Account acc : accountList) {
            View itemView = getLayoutInflater().inflate(R.layout.item_dialog_account, null);
            TextView name = itemView.findViewById(R.id.item_account_name);
            TextView email = itemView.findViewById(R.id.item_account_email);
            ImageView avatar = itemView.findViewById(R.id.item_account_avatar);

            email.setText(acc.name);
            name.setText("Fetching data...");

            new Thread(() -> {
                try {
                    URL url = new URL("https://api.oldschoolnetwork.online/v1/on-account/api/ecosystem.php?action=session_check");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    String savedToken=am.getUserData(acc,"server_token");if(savedToken==null&&acc.name.equals("@"+onAccount.getUsername()))savedToken=onAccount.getToken();
                    String payload = new JSONObject().put("token",savedToken==null?"":savedToken).toString();
                    OutputStream os = conn.getOutputStream();
                    os.write(payload.getBytes("UTF-8"));
                    os.close();

                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                    reader.close();

                    JSONObject res = new JSONObject(response.toString());
                    if (res.optJSONObject("user")!=null) {
                        JSONObject userObj = res.optJSONObject("user");
                        if (userObj != null) {
                            String fetchedName = userObj.optString("name", "");
                            String fetchedSurname = userObj.optString("surname", "");
                            String fetchedUsername = userObj.optString("username", "");
                            String fetchedAvatar = userObj.optString("avatar_base64", "");

                            String fullName = fetchedName + " " + fetchedSurname;
                            final String displayStr = fullName.trim().isEmpty() ? fetchedUsername : fullName.trim();

                            runOnUiThread(() -> {
                                name.setText(displayStr);
                                if (!fetchedAvatar.isEmpty()) {
                                    byte[] bytes = Base64.decode(fetchedAvatar, Base64.DEFAULT);
                                    avatar.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
                                } else {
                                    avatar.setImageResource(R.drawable.default_avatar);
                                }
                            });
                        }
                    } else if ("deleted".equals(res.optString("status"))
                            || (res.has("exists") && !res.optBoolean("exists") && !res.has("error") && !"error".equals(res.optString("status")))) {
                        runOnUiThread(() -> {
                            name.setText("Account Deleted");
                            name.setTextColor(Color.RED);
                        });
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> name.setText(acc.name.split("@")[0]));
                }
            }).start();

            itemView.setOnClickListener(v -> {
                if (name.getText().toString().equals("Account Deleted")) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                        am.removeAccountExplicitly(acc);
                    } else {
                        am.removeAccount(acc, null, null);
                    }
                    Toast.makeText(this, "Deleted account removed.", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    return;
                }

                SharedPreferences prefs = getSharedPreferences("ON_SECURE_PREFS", Context.MODE_PRIVATE);
                SharedPreferences.Editor ed = prefs.edit();
                ed.clear();
                ed.putBoolean("logged_in", true);
                String selectedToken=am.getUserData(acc,"server_token");
                if(selectedToken==null||selectedToken.isEmpty()){dialog.dismiss();startActivity(new Intent(this,OnboardingActivity.class));return;}
                ed.putString("server_token",selectedToken);
                ed.putString("email",SystemAccounts.privateLogin(this,acc));
                for(String key:new String[]{"username","name","surname","on_id","avatar_base64"}){String value=am.getUserData(acc,key);ed.putString(key,value==null?"":value);}
                ed.commit();

                Toast.makeText(this, "Switched to: " + acc.name, Toast.LENGTH_SHORT).show();
                updateUI();
                syncSessionData();
                dialog.dismiss();
            });

            container.addView(itemView);
        }

        dialog.setContentView(view);
        try {
            View parent = (View) view.getParent();
            if (parent != null) parent.setBackgroundColor(Color.TRANSPARENT);
        } catch (Exception e) {}
        dialog.show();
    }

    private void openStoreLibrary() {
        try {
            Intent intent = new Intent();
            intent.setClassName("com.oldschoolnetwork.store", "com.oldschoolnetwork.store.LibraryActivity");
            startActivity(intent);
        } catch (Exception e) {
            try {
                Intent fallback = new Intent();
                fallback.setClassName("com.oldschoolnetwork.store", "com.oldschoolnetwork.store.StoreMainActivity");
                startActivity(fallback);
            } catch (Exception ex) {
                startActivity(new Intent(this, UpdateActivity.class));
            }
        }
    }

    private void buildSearchCatalog() {
        allSearchItems.clear();
        boolean hasStore=ServiceUpdates.installed(this,"store")>0;
        allSearchItems.add(new SearchItem("Services", hasStore?"Library":"Download ON Store", hasStore?"Manage owned and installed apps":"Get ON Store for this device", R.drawable.ic_navbar_apps, "#FFFFFFFF", this::openStoreLibrary));
        allSearchItems.add(new SearchItem("Security", "Sign Out", "Sign out from this device", R.drawable.ic_logout, "#FFFF5555", () -> viewPager.setCurrentItem(2, true)));
        allSearchItems.add(new SearchItem("Security", "Delete account", "Permanently remove your ON account and data", R.drawable.ic_trash_can, "#FFFF5555", () -> viewPager.setCurrentItem(2, true)));
        allSearchItems.add(new SearchItem("Settings", "About ON Services Core", "Version, developers, etc.", R.drawable.ic_info, "#FFFFFFFF", () -> startActivity(new Intent(DashboardActivity.this, AboutActivity.class))));
        allSearchItems.add(new SearchItem("Settings", "My Profiles", "Manage profiles, linked to your ON Account", R.drawable.ic_profile, "#FFFFFFFF", () -> startActivity(new Intent(DashboardActivity.this, AccountPanelActivity.class).putExtra("panel","profiles"))));
        allSearchItems.add(new SearchItem("Settings", "System Update", "Check for new updates for services", R.drawable.ic_update, "#FFFFFFFF", () -> startActivity(new Intent(DashboardActivity.this, UpdateActivity.class))));
    }

    private void setupViewPager() {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(viewPager);

        tabLayout.getTabAt(0).setText("");
        tabLayout.getTabAt(0).setIcon(R.drawable.ic_search);

        com.google.android.material.appbar.AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (appBarLayout != null) {
                    appBarLayout.setExpanded(true, true);
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        viewPager.setCurrentItem(1, false);
    }

    public class SearchItem {
        String category, title, subtitle, colorHex;
        int iconRes;
        int color;
        Runnable onClick;
        SearchItem(String category, String title, String subtitle, int iconRes, String colorHex, Runnable onClick) {
            this.category = category; this.title = title; this.subtitle = subtitle;
            this.iconRes = iconRes; this.colorHex = colorHex;
            this.color = Color.parseColor(colorHex);
            this.onClick = onClick;
        }
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final String[] tabTitles = new String[]{"", "Services", "Security", "Settings"};

        public ViewPagerAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0: return new SearchFragment();
                case 1: return new ServicesFragment();
                case 2: return new SecurityFragment();
                case 3: return new SettingsFragment();
                default: return new Fragment();
            }
        }

        @Override public int getCount() { return tabTitles.length; }
        @Override public CharSequence getPageTitle(int position) { return tabTitles[position]; }
    }

    public class SearchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final int TYPE_CATEGORY = 0;
        private final int TYPE_ITEM = 1;

        private List<Object> displayList = new ArrayList<>();
        private String currentQuery = "";

        public void filter(String query) {
            currentQuery = query;
            displayList.clear();
            if (query.isEmpty()) {
                notifyDataSetChanged();
                return;
            }

            String qLower = query.toLowerCase();
            String lastCategory = "";

            for (SearchItem item : allSearchItems) {
                if (item.title.toLowerCase().contains(qLower) || item.subtitle.toLowerCase().contains(qLower)) {
                    if (!item.category.equals(lastCategory)) {
                        displayList.add(item.category);
                        lastCategory = item.category;
                    }
                    displayList.add(item);
                }
            }
            notifyDataSetChanged();
        }

        @Override public int getItemViewType(int position) {
            return displayList.get(position) instanceof String ? TYPE_CATEGORY : TYPE_ITEM;
        }

        @NonNull @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == TYPE_CATEGORY) {
                return new RecyclerView.ViewHolder(inflater.inflate(R.layout.item_search_category, parent, false)) {};
            } else {
                return new RecyclerView.ViewHolder(inflater.inflate(R.layout.item_search_result, parent, false)) {};
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (getItemViewType(position) == TYPE_CATEGORY) {
                TextView tvCat = holder.itemView.findViewById(R.id.tv_category);
                tvCat.setText((String) displayList.get(position));
                tvCat.setTextColor(Color.parseColor("#00BCD4"));
            } else {
                SearchItem item = (SearchItem) displayList.get(position);
                TextView tvTitle = holder.itemView.findViewById(R.id.tv_result_title);
                TextView tvSubtitle = holder.itemView.findViewById(R.id.tv_result_subtitle);
                ImageView ivIcon = holder.itemView.findViewById(R.id.iv_result_icon);
                ImageView ivArrow = holder.itemView.findViewById(R.id.iv_result_arrow);

                ivIcon.setImageResource(item.iconRes);

                int itemColor = item.color;
                ivIcon.setColorFilter(itemColor);
                ivArrow.setColorFilter(itemColor);
                tvTitle.setTextColor(itemColor);

                highlightText(tvTitle, item.title, Color.parseColor("#00BCD4"));

                if (item.subtitle.isEmpty()) {
                    tvSubtitle.setVisibility(View.GONE);
                } else {
                    tvSubtitle.setVisibility(View.VISIBLE);
                    highlightText(tvSubtitle, item.subtitle, Color.parseColor("#00BCD4"));
                }

                holder.itemView.setOnClickListener(v -> {
                    if (item.onClick != null) item.onClick.run();
                });
            }
        }

        private void highlightText(TextView tv, String text, int highlightColor) {
            SpannableString spannable = new SpannableString(text);
            int startIndex = text.toLowerCase().indexOf(currentQuery.toLowerCase());
            if (startIndex >= 0 && !currentQuery.isEmpty()) {
                spannable.setSpan(new ForegroundColorSpan(highlightColor),
                        startIndex, startIndex + currentQuery.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            tv.setText(spannable);
        }

        @Override public int getItemCount() { return displayList.size(); }
    }
}