package com.oldschoolnetwork.core;

import androidx.fragment.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

public class CloudFragment extends Fragment implements ServiceAwareFragment {

    private View rootView;

    // Метод интерфейса оставляем пустым, так как AIDL больше не используется внутри приложения
    @Override public void setCoreService(IONCoreService service) {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_cloud, container, false);

        ProgressBar pb = (ProgressBar) rootView.findViewById(R.id.pb_storage);
        TextView tvUsed = (TextView) rootView.findViewById(R.id.tv_storage_used);

        if (pb != null) {
            pb.setMax(100);
            pb.setProgress(0);
        }
        if (tvUsed != null) {
            tvUsed.setText("0 MB / 512 MB");
        }

        // Переход в Cloud Saves
        View rowSaves = rootView.findViewById(R.id.row_manage_saves);
        if (rowSaves != null) {
            rowSaves.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    startActivity(new Intent(getActivity(), CloudSavesActivity.class));
                }
            });
        }

        // Переход в Backups
        View rowBackup = rootView.findViewById(R.id.row_backup_now);
        if (rowBackup != null) {
            rowBackup.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    startActivity(new Intent(getActivity(), BackupsActivity.class));
                }
            });
        }

        return rootView;
    }

    // Старый локальный поиск больше не нужен, так как поиск стал глобальным фрагментом!
    @Override
    public void onSearch(String query) {
        // Оставляем пустым, чтобы не ломать интерфейс ServiceAwareFragment
    }
}