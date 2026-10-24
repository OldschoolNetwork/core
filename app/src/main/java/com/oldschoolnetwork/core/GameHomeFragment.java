package com.oldschoolnetwork.core;

import androidx.fragment.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class GameHomeFragment extends Fragment implements ServiceAwareFragment {

    private View rootView;

    // Прямой доступ к памяти вместо AIDL
    private ONAccount onAccount;

    // Метод интерфейса оставляем пустым, чтобы не ломать архитектуру
    @Override public void setCoreService(IONCoreService service) {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_gamehome, container, false);

        // Инициализируем наш прямой доступ к аккаунту
        onAccount = new ONAccount(getActivity());

        try {
            // Берем данные мгновенно и напрямую из ONAccount
            if (onAccount.isLoggedIn()) {
                ((TextView) rootView.findViewById(R.id.tv_game_id))
                        .setText(onAccount.getOnId());
                ((TextView) rootView.findViewById(R.id.tv_gamehome_status))
                        .setText("Ready");
            } else {
                ((TextView) rootView.findViewById(R.id.tv_gamehome_status))
                        .setText("Sign in required");
            }
        } catch (Exception e) { /* ignore */ }

        // Кнопка Cloud Saves
        View rowCloud = rootView.findViewById(R.id.row_cloud_saves);
        if (rowCloud != null) {
            rowCloud.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    startActivity(new Intent(getActivity(), CloudSavesActivity.class));
                }
            });
        }

        // Кнопка Achievements
        View rowAchievements = rootView.findViewById(R.id.row_achievements);
        if (rowAchievements != null) {
            rowAchievements.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    startActivity(new Intent(getActivity(), AchievementsActivity.class));
                }
            });
        }

        // Кнопка Leaderboards
        View rowLeaderboards = rootView.findViewById(R.id.row_leaderboards);
        if (rowLeaderboards != null) {
            rowLeaderboards.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    startActivity(new Intent(getActivity(), LeaderboardsActivity.class));
                }
            });
        }

        return rootView;
    }

    // Метод глобального поиска оставляем пустым, так как Дашборд ищет сам по каталогу
    @Override
    public void onSearch(String query) {
        // Оставляем пустым
    }
}