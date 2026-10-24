package com.oldschoolnetwork.core;

import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AboutActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundResource(R.drawable.bg_core_main);

        // Holo Header Bar
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), dp(14), dp(16), dp(14));

        ImageView ivIcon = new ImageView(this);
        ivIcon.setImageResource(R.drawable.ic_info);
        ivIcon.setColorFilter(Color.parseColor("#33B5E5"));
        header.addView(ivIcon, new LinearLayout.LayoutParams(dp(28), dp(28)));

        TextView tvHeader = new TextView(this);
        tvHeader.setText("About ON Services Core");
        tvHeader.setTextColor(Color.parseColor("#33B5E5"));
        tvHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        tvHeader.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        hp.leftMargin = dp(12);
        header.addView(tvHeader, hp);

        root.addView(header, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // 2dp Holo Blue Divider
        View divider = new View(this);
        divider.setBackgroundColor(Color.parseColor("#33B5E5"));
        root.addView(divider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(2)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setPadding(dp(24), dp(32), dp(24), dp(24));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.on_account_logo);
        content.addView(logo, new LinearLayout.LayoutParams(dp(200), dp(70)));

        TextView tvTitle = new TextView(this);
        tvTitle.setText("Oldschool Network Services Core");
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        tvTitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tp.topMargin = dp(20);
        content.addView(tvTitle, tp);

        String ver = "ONC-alpha-0.50";
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            if (pi.versionName != null && !pi.versionName.isEmpty()) ver = pi.versionName;
        } catch (Exception ignored) {}

        TextView tvVer = new TextView(this);
        tvVer.setText("Version: " + ver);
        tvVer.setTextColor(Color.parseColor("#33B5E5"));
        tvVer.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        vp.topMargin = dp(8);
        content.addView(tvVer, vp);

        TextView tvDesc = new TextView(this);
        tvDesc.setText("System authentication, single sign-on, and account synchronization framework for the Oldschool Network ecosystem (Android 4.0+).\n\n© Oldschool Network Team");
        tvDesc.setTextColor(Color.parseColor("#CCCCCC"));
        tvDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tvDesc.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams dpDesc = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dpDesc.topMargin = dp(24);
        content.addView(tvDesc, dpDesc);

        View spacer = new View(this);
        content.addView(spacer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        Button btnBack = new Button(this);
        btnBack.setText("Back");
        btnBack.setAllCaps(false);
        btnBack.setTextColor(Color.WHITE);
        btnBack.setBackgroundResource(R.drawable.btn_outline_gray);
        btnBack.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        bp.topMargin = dp(24);
        content.addView(btnBack, bp);

        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }
}
