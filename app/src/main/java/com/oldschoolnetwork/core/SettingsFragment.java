package com.oldschoolnetwork.core;

import androidx.fragment.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class SettingsFragment extends Fragment implements ServiceAwareFragment {

    private View rootView;

    @Override public void setCoreService(IONCoreService service) {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_settings, container, false);

        rootView.findViewById(R.id.row_about).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(getActivity(), AboutActivity.class));
            }
        });
        rootView.findViewById(R.id.row_profiles).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(getActivity(), AccountPanelActivity.class).putExtra("panel","profiles"));
            }
        });
        rootView.findViewById(R.id.row_update).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(getActivity(), UpdateActivity.class));
            }
        });

        View rowVault = rootView.findViewById(R.id.row_password_vault_settings);
        if (rowVault != null) rowVault.setVisibility(View.GONE);
        View rowNotif = rootView.findViewById(R.id.row_notifications);
        if (rowNotif != null) rowNotif.setOnClickListener(v->startActivity(new Intent(requireContext(),AccountPanelActivity.class)));
        rootView.findViewById(R.id.row_account_support).setOnClickListener(v->startActivity(new Intent(requireContext(),AccountPanelActivity.class).putExtra("panel","support")));
        View rowDev = rootView.findViewById(R.id.row_developer);
        if (rowDev != null) rowDev.setVisibility(View.GONE);

        return rootView;
    }

    @Override
    public void onSearch(String query) {}
}