package com.example.financialtracker;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransfersFragment extends Fragment {

    private MainActivity mainActivity;
    private DataManager dataManager;

    private RecyclerView transfersRecyclerView;
    private Button newTransferButton;
    private TextView emptyStateText;
    private LinearLayout loadingLayout;

    private List<Transfer> transfers = new ArrayList<>();
    private TransfersAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transfers, container, false);

        mainActivity = (MainActivity) getActivity();
        if (mainActivity == null) {
            return view;
        }

        dataManager = mainActivity.getDataManager();

        initViews(view);
        setupListeners();
        loadTransfers();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTransfers();
    }

    private void initViews(View view) {
        transfersRecyclerView = view.findViewById(R.id.transfersRecyclerView);
        newTransferButton = view.findViewById(R.id.newTransferButton);
        emptyStateText = view.findViewById(R.id.emptyStateText);
        loadingLayout = view.findViewById(R.id.loadingLayout);

        transfersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    private void setupListeners() {
        newTransferButton.setOnClickListener(v -> showNewTransferDialog());
    }

    private void loadTransfers() {
        if (dataManager == null) return;

        if (loadingLayout != null) {
            loadingLayout.setVisibility(View.VISIBLE);
        }

        transfers = dataManager.loadTransfers();

        if (loadingLayout != null) {
            loadingLayout.setVisibility(View.GONE);
        }

        if (transfers == null) {
            transfers = new ArrayList<>();
        }

        if (transfers.isEmpty()) {
            emptyStateText.setVisibility(View.VISIBLE);
            transfersRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateText.setVisibility(View.GONE);
            transfersRecyclerView.setVisibility(View.VISIBLE);

            // Сортируем по дате (сначала новые)
            Collections.sort(transfers, (t1, t2) -> t2.getDate().compareTo(t1.getDate()));

            adapter = new TransfersAdapter(transfers, new OnTransferClickListener() {
                @Override
                public void onTransferClick(Transfer transfer) {
                    showTransferDetails(transfer);
                }

                @Override
                public void onDeleteTransfer(Transfer transfer) {
                    confirmDeleteTransfer(transfer);
                }
            });

            transfersRecyclerView.setAdapter(adapter);
        }
    }

    private void showNewTransferDialog() {
        if (getContext() == null || dataManager == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("💸 Новый перевод");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_transfer, null);

        Spinner fromAccountSpinner = dialogView.findViewById(R.id.fromAccountSpinner);
        Spinner toAccountSpinner = dialogView.findViewById(R.id.toAccountSpinner);
        EditText amountEditText = dialogView.findViewById(R.id.amountEditText);
        EditText descriptionEditText = dialogView.findViewById(R.id.descriptionEditText);
        EditText notesEditText = dialogView.findViewById(R.id.notesEditText);
        LinearLayout dateLayout = dialogView.findViewById(R.id.dateLayout);
        TextView dateTextView = dialogView.findViewById(R.id.dateTextView);
        Button convertButton = dialogView.findViewById(R.id.convertButton);
        EditText commissionEditText = dialogView.findViewById(R.id.commissionEditText);

        // Скрываем ненужные элементы конвертации
        TextView convertedAmountText = dialogView.findViewById(R.id.convertedAmountText);
        TextView rateInfoText = dialogView.findViewById(R.id.rateInfoText);
        if (convertedAmountText != null) convertedAmountText.setVisibility(View.GONE);
        if (rateInfoText != null) rateInfoText.setVisibility(View.GONE);

        // Загружаем счета
        List<Account> accounts = dataManager.loadAccounts();
        if (accounts == null || accounts.isEmpty()) {
            Toast.makeText(getContext(), "Сначала создайте счета", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> accountNames = new ArrayList<>();
        for (Account account : accounts) {
            accountNames.add(account.getName() + " - " + account.getFormattedBalance());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, accountNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        fromAccountSpinner.setAdapter(adapter);
        toAccountSpinner.setAdapter(adapter);

        // Дата по умолчанию
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        final Date[] selectedDate = {new Date()};
        dateTextView.setText(sdf.format(selectedDate[0]));

        dateLayout.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            cal.setTime(selectedDate[0]);

            DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                    (view, year, month, dayOfMonth) -> {
                        Calendar selected = Calendar.getInstance();
                        selected.set(year, month, dayOfMonth);
                        selectedDate[0] = selected.getTime();
                        dateTextView.setText(sdf.format(selectedDate[0]));
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        convertButton.setOnClickListener(v -> {
            int fromPos = fromAccountSpinner.getSelectedItemPosition();
            int toPos = toAccountSpinner.getSelectedItemPosition();
            fromAccountSpinner.setSelection(toPos);
            toAccountSpinner.setSelection(fromPos);
        });

        builder.setView(dialogView);
        builder.setPositiveButton("Перевести", (dialog, which) -> {
            try {
                int fromPosition = fromAccountSpinner.getSelectedItemPosition();
                int toPosition = toAccountSpinner.getSelectedItemPosition();

                if (fromPosition == toPosition) {
                    Toast.makeText(getContext(), "Выберите разные счета", Toast.LENGTH_SHORT).show();
                    return;
                }

                Account fromAccount = accounts.get(fromPosition);
                Account toAccount = accounts.get(toPosition);

                String amountStr = amountEditText.getText().toString();
                if (amountStr.isEmpty()) {
                    Toast.makeText(getContext(), "Введите сумму", Toast.LENGTH_SHORT).show();
                    return;
                }

                double amount = Double.parseDouble(amountStr);
                if (amount <= 0) {
                    Toast.makeText(getContext(), "Сумма должна быть больше 0", Toast.LENGTH_SHORT).show();
                    return;
                }

                double commission = 0.0;
                String commissionStr = commissionEditText.getText().toString();
                if (!commissionStr.isEmpty()) {
                    commission = Double.parseDouble(commissionStr);
                }

                if (fromAccount.getBalance() < amount + commission) {
                    Toast.makeText(getContext(),
                            "Недостаточно средств. Доступно: " + fromAccount.getFormattedBalance(),
                            Toast.LENGTH_LONG).show();
                    return;
                }

                Transfer transfer = new Transfer(
                        fromAccount.getName(),
                        toAccount.getName(),
                        amount,
                        descriptionEditText.getText().toString(),
                        selectedDate[0],
                        notesEditText.getText().toString(),
                        commission
                );

                fromAccount.setBalance(fromAccount.getBalance() - (amount + commission));
                toAccount.setBalance(toAccount.getBalance() + amount);

                dataManager.saveAccounts(accounts);
                dataManager.addTransfer(transfer);

                loadTransfers();
                mainActivity.updateNavHeader();

                String message = String.format(Locale.getDefault(),
                        "✅ Перевод выполнен\n%.2f ₽", amount);

                if (commission > 0) {
                    message += String.format(Locale.getDefault(), "\nКомиссия: %.2f ₽", commission);
                }

                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();

            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Некорректная сумма", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(getContext(), "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        });

        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void showTransferDetails(Transfer transfer) {
        if (getContext() == null) return;

        new AlertDialog.Builder(getContext())
                .setTitle("Перевод")
                .setMessage(transfer.getFullDescription())
                .setPositiveButton("OK", null)
                .setNeutralButton("Удалить", (dialog, which) -> confirmDeleteTransfer(transfer))
                .show();
    }

    private void confirmDeleteTransfer(final Transfer transfer) {
        if (getContext() == null || dataManager == null) return;

        new AlertDialog.Builder(getContext())
                .setTitle("Удаление перевода")
                .setMessage("Вы уверены, что хотите удалить этот перевод?\n\n" +
                        "Балансы счетов НЕ будут восстановлены!")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    List<Transfer> transfers = dataManager.loadTransfers();
                    transfers.remove(transfer);
                    dataManager.saveTransfers(transfers);
                    loadTransfers();
                    mainActivity.updateNavHeader();
                    Toast.makeText(getContext(), "Перевод удален", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    // ============ ИНТЕРФЕЙС ДЛЯ КЛИКОВ ============

    private interface OnTransferClickListener {
        void onTransferClick(Transfer transfer);
        void onDeleteTransfer(Transfer transfer);
    }

    // ============ АДАПТЕР ============

    private class TransfersAdapter extends RecyclerView.Adapter<TransfersAdapter.ViewHolder> {

        private List<Transfer> transfers;
        private OnTransferClickListener listener;

        TransfersAdapter(List<Transfer> transfers, OnTransferClickListener listener) {
            this.transfers = transfers;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_transfer, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Transfer t = transfers.get(position);

            holder.fromText.setText("📤 " + t.getFromAccount());
            holder.toText.setText("📥 " + t.getToAccount());

            // ============ ИСПРАВЛЕННОЕ ОТОБРАЖЕНИЕ СУММЫ ============
            // Используем getFormattedAmount() который возвращает "X.XX руб."
            holder.amountText.setText(t.getFormattedAmount());

            holder.dateText.setText(t.getFormattedDateShort());

            holder.itemView.setOnClickListener(v -> listener.onTransferClick(t));

            holder.deleteButton.setOnClickListener(v -> listener.onDeleteTransfer(t));
        }

        @Override
        public int getItemCount() {
            return transfers.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView fromText, toText, amountText, dateText;
            ImageView deleteButton;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                fromText = itemView.findViewById(R.id.fromText);
                toText = itemView.findViewById(R.id.toText);
                amountText = itemView.findViewById(R.id.amountText);
                dateText = itemView.findViewById(R.id.dateText);
                deleteButton = itemView.findViewById(R.id.deleteButton);
            }
        }
    }
}