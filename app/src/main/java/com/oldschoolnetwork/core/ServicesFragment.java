package com.oldschoolnetwork.core;

import androidx.fragment.app.Fragment;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.File;

public class ServicesFragment extends Fragment implements ServiceAwareFragment {

    private View rootView;

    @Override
    public void setCoreService(IONCoreService service) {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_services, container, false);

        // 1. ПАРСИНГ И РАСКРАСКА ВЕРСИИ
        TextView tvVersion = rootView.findViewById(R.id.tv_core_version);
        try {
            PackageInfo pInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
            String versionName = pInfo.versionName;
            if (versionName == null || versionName.isEmpty()) {
                versionName = "1.0"; // Фолбэк, если система вернула null
            }

            String fullText = (CoreInstallation.full(getActivity()) ? "FULL, " : "ORDINARY, ") + versionName;
            SpannableString spannable = new SpannableString(fullText);

            // По умолчанию делаем весь текст белым, перебивая цвет из XML
            spannable.setSpan(new ForegroundColorSpan(Color.WHITE), 0, fullText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Ищем ключевые слова и красим только их
            String lower = fullText.toLowerCase();
            if (lower.contains("alpha")) {
                int start = lower.indexOf("alpha");
                spannable.setSpan(new ForegroundColorSpan(Color.parseColor("#FFA500")), start, start + 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (lower.contains("beta")) {
                int start = lower.indexOf("beta");
                spannable.setSpan(new ForegroundColorSpan(Color.parseColor("#0000FF")), start, start + 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (lower.contains("dev")) {
                int start = lower.indexOf("dev");
                spannable.setSpan(new ForegroundColorSpan(Color.parseColor("#00FF00")), start, start + 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            tvVersion.setText(spannable);
        } catch (Exception e) {
            e.printStackTrace();
            // Если произошла ошибка парсинга, всё равно ставим белый текст
            tvVersion.setTextColor(Color.WHITE);
            tvVersion.setText("Unknown installation");
        }

        // 2. ЛОГИКА ПРОВЕРКИ НА ROOT
        TextView tvRoot = rootView.findViewById(R.id.tv_root_status);
        TextView btnRootLearn = rootView.findViewById(R.id.btn_root_learn_more); // Кастуем именно к TextView

        if (checkRoot()) {
            tvRoot.setText("Yes");
            tvRoot.setTextColor(Color.GREEN);
            btnRootLearn.setVisibility(View.GONE);
        } else {
            tvRoot.setText("No");
            tvRoot.setTextColor(Color.RED);
            btnRootLearn.setVisibility(View.VISIBLE);
            btnRootLearn.setTextColor(0xff00BCD4);
            btnRootLearn.setPaintFlags(btnRootLearn.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG); // Строго синий цвет для ссылки (не циан)

            // Клик отправляет на наш главный сайт
            btnRootLearn.setOnClickListener(v -> {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://oldschoolnetwork.online/downloads/docs/full-install/")));
            });
        }

        // 3. ПЕРЕХОДЫ В STORE И КОШЕЛЕК
        rootView.findViewById(R.id.row_library).setOnClickListener(v -> {
            try {
                Intent intent = new Intent();
                intent.setClassName("com.oldschoolnetwork.store", "com.oldschoolnetwork.store.LibraryActivity");
                startActivity(intent);
            } catch (Exception e) {
                try {
                    Intent fallback = new Intent();
                    fallback.setClassName("com.oldschoolnetwork.store", "com.oldschoolnetwork.store.StoreMainActivity");
                    startActivity(fallback);
                } catch (Exception ignored) {
                }
            }
        });

        View rowWallet = rootView.findViewById(R.id.row_wallet);
        if (rowWallet != null) {
            rowWallet.setVisibility(View.GONE);
        }

        return rootView;
    }

    @Override public void onResume(){super.onResume();if(rootView!=null)ServiceUpdateNotices.attach(this,(android.widget.LinearLayout)rootView.findViewById(R.id.tv_core_version).getParent().getParent());}
    private boolean checkRoot() {
        String[] paths = {
                "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su",
                "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su",
                "/system/sd/xbin/su", "/system/bin/failsafe/su", "/data/local/su"
        };
        for (String path : paths) {
            if (new File(path).exists()) return true;
        }
        return false;
    }

    @Override
    public void onSearch(String query) {}
}