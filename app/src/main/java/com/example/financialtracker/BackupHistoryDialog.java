package com.example.financialtracker;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.util.List;

public class BackupHistoryDialog extends DialogFragment {

    private BackupManager backupManager;
    private MainActivity mainActivity;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) context;
            backupManager = new BackupManager(context);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());

        View view = getLayoutInflater().inflate(R.layout.dialog_backup_history, null);
        ListView listView = view.findViewById(R.id.backupHistoryList);

        List<BackupManager.BackupEntry> history = backupManager.getBackupHistory();

        if (history.isEmpty()) {
            TextView emptyText = new TextView(getContext());
            emptyText.setText("История бэкапов пуста");
            emptyText.setPadding(50, 50, 50, 50);
            emptyText.setTextSize(16);
            listView.addHeaderView(emptyText);
        }

        BackupHistoryAdapter adapter = new BackupHistoryAdapter(requireContext(), history);
        listView.setAdapter(adapter);

        builder.setView(view)
                .setTitle("📋 История бэкапов")
                .setPositiveButton("Закрыть", null);

        return builder.create();
    }

    private class BackupHistoryAdapter extends ArrayAdapter<BackupManager.BackupEntry> {

        public BackupHistoryAdapter(Context context, List<BackupManager.BackupEntry> entries) {
            super(context, 0, entries);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_backup_history, parent, false);
            }

            BackupManager.BackupEntry entry = getItem(position);

            TextView dateText = convertView.findViewById(R.id.backupDateText);
            TextView sizeText = convertView.findViewById(R.id.backupSizeText);
            TextView restoreButton = convertView.findViewById(R.id.restoreButton);

            if (entry != null) {
                dateText.setText(entry.date);
                sizeText.setText(entry.getFormattedSize());

                restoreButton.setOnClickListener(v -> confirmRestore(entry));
            }

            return convertView;
        }
    }

    private void confirmRestore(BackupManager.BackupEntry entry) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Восстановление из бэкапа")
                .setMessage("Восстановить данные от " + entry.date + "?\n\n" +
                        "Размер: " + entry.getFormattedSize() + "\n\n" +
                        "⚠️ Текущие данные будут заменены!")
                .setPositiveButton("Восстановить", (dialog, which) -> {
                    boolean success = backupManager.restoreAutoBackup();
                    if (success && mainActivity != null) {
                        mainActivity.loadSavedData();
                        mainActivity.updateFragments();
                        mainActivity.updateNavHeader();
                        mainActivity.updateAccountsFragment();
                        Toast.makeText(getContext(), "✅ Данные восстановлены", Toast.LENGTH_SHORT).show();
                        dismiss();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }
}