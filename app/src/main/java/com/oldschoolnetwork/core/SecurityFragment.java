package com.oldschoolnetwork.core;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class SecurityFragment extends Fragment {

    private View rootView;
    private ONAccount onAccount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Подключаем твою верстку с R.id.scroll_security
        rootView = inflater.inflate(R.layout.fragment_security, container, false);
        onAccount = new ONAccount(requireContext());

        View rowSmartAuth = rootView.findViewById(R.id.row_smartauth_protection);
        if (rowSmartAuth != null) rowSmartAuth.setOnClickListener(v->startActivity(new Intent(requireContext(),AccountPanelActivity.class)));
        View rowVault = rootView.findViewById(R.id.row_password_vault);
        if (rowVault != null) rowVault.setVisibility(View.GONE);
        View rowSessions = rootView.findViewById(R.id.row_sessions);
        if (rowSessions != null) { rowSessions.setVisibility(View.VISIBLE); rowSessions.setOnClickListener(v->startActivity(new Intent(requireContext(),SessionsActivity.class))); }

        // Находим кнопку Sign Out
        View btnSignOut = rootView.findViewById(R.id.row_sign_out);
        if (btnSignOut != null) {
            btnSignOut.setOnClickListener(v -> {
                if (onAccount.isLoggedIn()) {
                    showHoloDialogPhase1();
                } else {
                    Toast.makeText(getActivity(), "You are already logged out", Toast.LENGTH_SHORT).show();
                }
            });
        }

        View btnDeleteAccount = rootView.findViewById(R.id.row_delete_account);
        if (btnDeleteAccount != null) {
            btnDeleteAccount.setOnClickListener(v -> {
                if (!onAccount.isLoggedIn()) {
                    Toast.makeText(getActivity(), "Please log in to manage your account", Toast.LENGTH_SHORT).show();
                    return;
                }
                new AlertDialog.Builder(requireContext(), android.R.style.Theme_Holo_Dialog)
                        .setTitle("Delete ON Account")
                        .setMessage("To permanently delete your Oldschool Network account (" + onAccount.getEmail() + ") and all associated data, or to remove this account from your device, select an option below.")
                        .setPositiveButton("Remove from Device", (d, w) -> executeRealLogoff())
                        .setNeutralButton("Open Web Portal", (d, w) -> {
                            try {
                                startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://oldschoolnetwork.online/account/edit/")));
                            } catch (Exception ignored) {}
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }
        return rootView;
    }

    // Первый диалог
    private void showHoloDialogPhase1() {
        showTimerDialog(
                "Warning",
                "Are you sure you want to sign out? All local data for this account will be erased from this device.",
                this::showHoloDialogPhase2
        );
    }

    // Второй диалог
    private void showHoloDialogPhase2() {
        showTimerDialog(
                "Final Warning",
                "Are you ABSOLUTELY sure? You will need to go through onboarding to sign in again.",
                this::showHoloDialogPhase3
        );
    }

    // Третий диалог
    private void showHoloDialogPhase3() {
        showTimerDialog(
                "Confirm Deletion",
                "This action cannot be undone. Your account will be wiped from this device and the session closed.",
                this::executeRealLogoff
        );
    }

    // Метод генерации Holo-диалога с таймером 5 секунд
    private void showTimerDialog(String title, String message, Runnable onConfirm) {
        AlertDialog dialog = new AlertDialog.Builder(requireContext(), android.R.style.Theme_Holo_Dialog)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Wait...", null)
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .setCancelable(false)
                .create();

        dialog.setOnShowListener(d -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setEnabled(false);

            new CountDownTimer(5000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    positiveButton.setText("OK (" + ((millisUntilFinished / 1000) + 1) + "s)");
                }

                @Override
                public void onFinish() {
                    positiveButton.setText("OK");
                    positiveButton.setEnabled(true);
                    positiveButton.setOnClickListener(v -> {
                        dialog.dismiss();
                        onConfirm.run();
                    });
                }
            }.start();
        });
        dialog.show();
    }

    // РЕАЛЬНЫЙ логаут со всеми стираниями
    private void executeRealLogoff() {
        android.content.Context app=requireContext().getApplicationContext();String email=onAccount.getEmail(),id=onAccount.getOnId(),token=onAccount.getToken();
        AccountManager manager=AccountManager.get(app);
        for(Account account:manager.getAccountsByType(ONAccount.ACCOUNT_TYPE))if(SystemAccounts.privateLogin(app,account).equalsIgnoreCase(email)){
            if(Build.VERSION.SDK_INT>=22)manager.removeAccountExplicitly(account);else manager.removeAccount(account,null,null);
        }
        onAccount.logout();
        new Thread(()->{try{org.json.JSONObject request=new org.json.JSONObject();request.put("revoke_current",true);AccountApi.requestWithToken("account_revoke_session",request,id,token);}catch(Exception failure){android.util.Log.w("ONCore","Server session termination could not be confirmed");}},"on-account-sign-out").start();
        Toast.makeText(requireContext(),"Signed out on this device.",Toast.LENGTH_SHORT).show();
        startActivity(new Intent(requireContext(),DashboardActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
    }
}
