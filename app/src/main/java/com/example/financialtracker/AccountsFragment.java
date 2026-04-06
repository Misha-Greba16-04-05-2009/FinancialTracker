package com.example.financialtracker;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AccountsFragment extends Fragment {

    private static final String TAG = "AccountsFragment";

    // UI элементы
    private LinearLayout accountsContainer;
    private Button addAccountButton;
    private TextView totalBalanceTextView;
    private TextView accountsCountTextView;
    private SwipeRefreshLayout swipeRefreshLayout;

    // Менеджеры
    private MainActivity mainActivity;
    private DataManager dataManager;

    // Данные
    private List<Account> accounts = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");

        if (getContext() == null) {
            return new View(getContext());
        }

        View view = inflater.inflate(R.layout.fragment_accounts, container, false);

        mainActivity = (MainActivity) getActivity();
        if (mainActivity == null) {
            showError("Ошибка инициализации");
            return view;
        }

        try {
            dataManager = mainActivity.getDataManager();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Ошибка загрузки данных");
            return view;
        }

        initViews(view);
        setupListeners();

        // Принудительная загрузка счетов
        forceLoadAccounts();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        forceLoadAccounts();
    }

    private void initViews(View view) {
        try {
            accountsContainer = view.findViewById(R.id.accountsContainer);
            addAccountButton = view.findViewById(R.id.addAccountButton);
            totalBalanceTextView = view.findViewById(R.id.totalBalanceTextView);
            accountsCountTextView = view.findViewById(R.id.accountsCountTextView);
            swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);

            Log.d(TAG, "Views initialized");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupListeners() {
        if (addAccountButton != null) {
            addAccountButton.setOnClickListener(v -> showAddAccountDialog());
        }

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                forceLoadAccounts();
                swipeRefreshLayout.setRefreshing(false);
            });
        }
    }

    public void refreshAccounts() {
        Log.d(TAG, "refreshAccounts вызван из MainActivity");
        forceLoadAccounts();
    }

    private void forceLoadAccounts() {
        Log.d(TAG, "forceLoadAccounts started");

        if (mainActivity == null || accountsContainer == null || dataManager == null) {
            Log.e(TAG, "forceLoadAccounts: null check failed");
            return;
        }

        try {
            accounts = dataManager.loadAccounts();
            Log.d(TAG, "Loaded accounts: " + (accounts == null ? "null" : accounts.size()));

            if (accounts == null) {
                accounts = new ArrayList<>();
            }

            updateStatistics();
            displayAccounts();

            Log.d(TAG, "forceLoadAccounts completed");
        } catch (Exception e) {
            Log.e(TAG, "Error loading accounts: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateStatistics() {
        if (totalBalanceTextView == null || accountsCountTextView == null) return;

        double total = 0;
        int includedCount = 0;

        for (Account account : accounts) {
            if (account == null) continue;

            if (account.isIncludedInTotalBalance()) {
                total += account.getBalance();
                includedCount++;
            }
        }

        try {
            totalBalanceTextView.setText(String.format(Locale.getDefault(),
                    "≈ %.2f ₽", total));
            accountsCountTextView.setText(String.format(Locale.getDefault(),
                    "Всего: %d | Учитываются: %d", accounts.size(), includedCount));

            Log.d(TAG, "Statistics updated: total=" + total + ", count=" + accounts.size());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void displayAccounts() {
        if (accountsContainer == null) {
            Log.e(TAG, "displayAccounts: accountsContainer is null");
            return;
        }

        accountsContainer.removeAllViews();
        Log.d(TAG, "displayAccounts: accounts size = " + accounts.size());

        if (accounts.isEmpty()) {
            showEmptyState();
            return;
        }

        for (Account account : accounts) {
            if (account != null) {
                addAccountView(account);
            }
        }

        Log.d(TAG, "displayAccounts completed");
    }

    private void showEmptyState() {
        if (getContext() == null || accountsContainer == null) return;

        try {
            TextView emptyText = new TextView(getContext());
            emptyText.setText("Нет добавленных счетов\nНажмите 'Добавить счет' чтобы создать первый");
            emptyText.setTextSize(16);
            emptyText.setTextColor(Color.GRAY);
            emptyText.setGravity(android.view.Gravity.CENTER);
            emptyText.setPadding(0, 100, 0, 100);
            accountsContainer.addView(emptyText);

            Log.d(TAG, "Empty state shown");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addAccountView(final Account account) {
        if (getContext() == null || accountsContainer == null || account == null) {
            Log.e(TAG, "addAccountView: null check failed");
            return;
        }

        try {
            LinearLayout accountLayout = new LinearLayout(getContext());
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            layoutParams.setMargins(16, 8, 16, 8);
            accountLayout.setLayoutParams(layoutParams);
            accountLayout.setOrientation(LinearLayout.VERTICAL);
            accountLayout.setPadding(20, 16, 20, 16);
            accountLayout.setBackgroundResource(R.drawable.card_background);
            accountLayout.setClickable(true);

            // Верхняя строка
            LinearLayout topRow = new LinearLayout(getContext());
            topRow.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            topRow.setOrientation(LinearLayout.HORIZONTAL);

            // Иконка
            TextView iconView = new TextView(getContext());
            iconView.setLayoutParams(new LinearLayout.LayoutParams(48, 48));
            iconView.setText(account.getIcon());
            iconView.setTextSize(24);
            iconView.setGravity(android.view.Gravity.CENTER);

            // Информация
            LinearLayout infoLayout = new LinearLayout(getContext());
            LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1.0f
            );
            infoParams.setMargins(16, 0, 16, 0);
            infoLayout.setLayoutParams(infoParams);
            infoLayout.setOrientation(LinearLayout.VERTICAL);

            TextView nameView = new TextView(getContext());
            nameView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            nameView.setText(account.getName());
            nameView.setTextSize(18);
            nameView.setTextColor(Color.BLACK);

            TextView typeView = new TextView(getContext());
            typeView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            typeView.setText(account.getType() + " • " + account.getPaymentType());
            typeView.setTextSize(14);
            typeView.setTextColor(Color.GRAY);
            typeView.setPadding(0, 4, 0, 0);

            infoLayout.addView(nameView);
            infoLayout.addView(typeView);

            // Баланс
            LinearLayout balanceLayout = new LinearLayout(getContext());
            balanceLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            balanceLayout.setOrientation(LinearLayout.VERTICAL);
            balanceLayout.setGravity(android.view.Gravity.END);

            TextView balanceView = new TextView(getContext());
            balanceView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));

            balanceView.setText(account.getFormattedBalance());
            balanceView.setTextSize(18);
            balanceView.setTextColor(account.getBalanceColor());

            balanceLayout.addView(balanceView);
            topRow.addView(iconView);
            topRow.addView(infoLayout);
            topRow.addView(balanceLayout);

            // Нижняя строка
            LinearLayout bottomRow = new LinearLayout(getContext());
            bottomRow.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            bottomRow.setPadding(0, 8, 0, 0);

            TextView descriptionView = new TextView(getContext());
            descriptionView.setLayoutParams(new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1.0f
            ));
            String desc = account.getDescription();
            descriptionView.setText((desc == null || desc.isEmpty()) ? "Без описания" : desc);
            descriptionView.setTextSize(12);
            descriptionView.setTextColor(Color.DKGRAY);

            TextView statusView = new TextView(getContext());
            statusView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            statusView.setText(account.isIncludedInTotalBalance() ? "✅ Учитывается" : "⏸️ Не учитывается");
            statusView.setTextSize(12);
            statusView.setTextColor(account.isIncludedInTotalBalance() ?
                    Color.parseColor("#4CAF50") : Color.parseColor("#FF9800"));

            bottomRow.addView(descriptionView);
            bottomRow.addView(statusView);

            accountLayout.addView(topRow);
            accountLayout.addView(bottomRow);

            // ИСПРАВЛЕНО: лямбда вместо анонимного класса
            accountLayout.setOnClickListener(v -> showAccountOptionsDialog(account));

            accountsContainer.addView(accountLayout);
            Log.d(TAG, "Account view added: " + account.getName());

        } catch (Exception e) {
            Log.e(TAG, "Error adding account view: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showAddAccountDialog() {
        if (getContext() == null) return;

        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("➕ Добавить новый счет");

            View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_account, null);

            final EditText nameInput = dialogView.findViewById(R.id.nameInput);
            final EditText initialBalanceInput = dialogView.findViewById(R.id.initialBalanceInput);
            final EditText descriptionInput = dialogView.findViewById(R.id.descriptionInput);
            final Spinner typeSpinner = dialogView.findViewById(R.id.typeSpinner);
            final Spinner paymentTypeSpinner = dialogView.findViewById(R.id.paymentTypeSpinner);
            final CheckBox includeInTotalCheckbox = dialogView.findViewById(R.id.includeInTotalCheckbox);
            final LinearLayout dateLayout = dialogView.findViewById(R.id.dateLayout);
            final TextView dateTextView = dialogView.findViewById(R.id.dateTextView);

            // Скрываем выбор валюты
            View currencySpinner = dialogView.findViewById(R.id.currencySpinner);
            if (currencySpinner != null) {
                currencySpinner.setVisibility(View.GONE);
            }

            // Дата
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            final Date[] selectedDate = {new Date()};
            dateTextView.setText(sdf.format(selectedDate[0]));

            // ИСПРАВЛЕНО: лямбда вместо анонимного класса
            dateLayout.setOnClickListener(v -> {
                try {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(selectedDate[0]);

                    DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                            (view, year, month, dayOfMonth) -> {
                                Calendar cal = Calendar.getInstance();
                                cal.set(year, month, dayOfMonth);
                                selectedDate[0] = cal.getTime();
                                dateTextView.setText(sdf.format(selectedDate[0]));
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                    );
                    datePickerDialog.show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            // Типы счетов
            String[] accountTypes = {"Основной", "Сберегательный", "Инвестиционный", "Кредитный"};
            ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(getContext(),
                    android.R.layout.simple_spinner_item, accountTypes);
            typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            typeSpinner.setAdapter(typeAdapter);

            // Типы платежей
            String[] paymentTypes = {"Карта", "Наличные", "Электронные деньги", "Другой"};
            ArrayAdapter<String> paymentAdapter = new ArrayAdapter<>(getContext(),
                    android.R.layout.simple_spinner_item, paymentTypes);
            paymentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            paymentTypeSpinner.setAdapter(paymentAdapter);

            builder.setView(dialogView);

            // ИСПРАВЛЕНО: лямбда вместо анонимного класса
            builder.setPositiveButton("Добавить", (dialog, which) -> {
                try {
                    String name = nameInput.getText().toString().trim();
                    String balanceStr = initialBalanceInput.getText().toString().trim();
                    String description = descriptionInput.getText().toString().trim();
                    String type = (String) typeSpinner.getSelectedItem();
                    String paymentType = (String) paymentTypeSpinner.getSelectedItem();

                    boolean includeInTotal = includeInTotalCheckbox.isChecked();

                    if (name.isEmpty()) {
                        showError("Введите название счета");
                        return;
                    }

                    double initialBalance = 0.0;
                    if (!balanceStr.isEmpty()) {
                        try {
                            initialBalance = Double.parseDouble(balanceStr);
                        } catch (NumberFormatException e) {
                            showError("Некорректная сумма");
                            return;
                        }
                    }

                    Log.d(TAG, "Creating account: " + name + ", balance: " + initialBalance);

                    Account newAccount = new Account(
                            name,
                            type,
                            paymentType,
                            initialBalance,
                            description,
                            selectedDate[0],
                            includeInTotal
                    );

                    List<Account> accounts = dataManager.loadAccounts();
                    if (accounts == null) {
                        accounts = new ArrayList<>();
                    }
                    accounts.add(newAccount);
                    dataManager.saveAccounts(accounts);

                    Log.d(TAG, "Account saved. Total accounts: " + accounts.size());

                    showSuccess("Счет добавлен");

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        forceLoadAccounts();
                        if (mainActivity != null) {
                            mainActivity.updateNavHeader();
                        }
                    }, 100);

                } catch (Exception e) {
                    Log.e(TAG, "Error adding account: " + e.getMessage());
                    showError("Ошибка: " + e.getMessage());
                    e.printStackTrace();
                }
            });

            builder.setNegativeButton("Отмена", null);
            builder.show();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Ошибка открытия диалога");
        }
    }

    private void showAddMoneyDialog(final Account account) {
        if (getContext() == null || account == null) return;

        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("💰 Пополнить счет");

            View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_money, null);
            final EditText amountInput = dialogView.findViewById(R.id.amountInput);
            final TextView currentBalanceText = dialogView.findViewById(R.id.currentBalanceText);

            currentBalanceText.setText("Текущий баланс: " + account.getFormattedBalance());

            builder.setView(dialogView);

            // ИСПРАВЛЕНО: лямбда вместо анонимного класса
            builder.setPositiveButton("Пополнить", (dialog, which) -> {
                try {
                    String amountStr = amountInput.getText().toString().trim();
                    if (amountStr.isEmpty()) {
                        showError("Введите сумму");
                        return;
                    }

                    double amount = Double.parseDouble(amountStr);
                    if (amount <= 0) {
                        showError("Сумма должна быть больше 0");
                        return;
                    }

                    account.setBalance(account.getBalance() + amount);

                    List<Account> allAccounts = dataManager.loadAccounts();
                    for (int i = 0; i < allAccounts.size(); i++) {
                        if (allAccounts.get(i).getId().equals(account.getId())) {
                            allAccounts.set(i, account);
                            break;
                        }
                    }

                    dataManager.saveAccounts(allAccounts);

                    showSuccess(String.format(Locale.getDefault(),
                            "Счет пополнен на %.2f ₽", amount));

                    forceLoadAccounts();

                    if (mainActivity != null) {
                        mainActivity.updateNavHeader();
                    }

                } catch (NumberFormatException e) {
                    showError("Некорректная сумма");
                } catch (Exception e) {
                    showError("Ошибка: " + e.getMessage());
                    e.printStackTrace();
                }
            });

            builder.setNegativeButton("Отмена", null);
            builder.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showWithdrawMoneyDialog(final Account account) {
        if (getContext() == null || account == null) return;

        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("💸 Снять со счета");

            View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_money, null);
            final EditText amountInput = dialogView.findViewById(R.id.amountInput);
            final TextView currentBalanceText = dialogView.findViewById(R.id.currentBalanceText);

            currentBalanceText.setText("Текущий баланс: " + account.getFormattedBalance());

            builder.setView(dialogView);

            // ИСПРАВЛЕНО: лямбда вместо анонимного класса
            builder.setPositiveButton("Снять", (dialog, which) -> {
                try {
                    String amountStr = amountInput.getText().toString().trim();
                    if (amountStr.isEmpty()) {
                        showError("Введите сумму");
                        return;
                    }

                    double amount = Double.parseDouble(amountStr);
                    if (amount <= 0) {
                        showError("Сумма должна быть больше 0");
                        return;
                    }

                    if (account.getBalance() < amount) {
                        showError("Недостаточно средств");
                        return;
                    }

                    account.setBalance(account.getBalance() - amount);

                    List<Account> allAccounts = dataManager.loadAccounts();
                    for (int i = 0; i < allAccounts.size(); i++) {
                        if (allAccounts.get(i).getId().equals(account.getId())) {
                            allAccounts.set(i, account);
                            break;
                        }
                    }

                    dataManager.saveAccounts(allAccounts);

                    showSuccess(String.format(Locale.getDefault(),
                            "Снято %.2f ₽", amount));

                    forceLoadAccounts();

                    if (mainActivity != null) {
                        mainActivity.updateNavHeader();
                    }

                } catch (NumberFormatException e) {
                    showError("Некорректная сумма");
                } catch (Exception e) {
                    showError("Ошибка: " + e.getMessage());
                    e.printStackTrace();
                }
            });

            builder.setNegativeButton("Отмена", null);
            builder.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showEditAccountDialog(final Account account) {
        if (getContext() == null || account == null) return;

        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("✏️ Редактировать счет: " + account.getName());

            View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_account, null);

            final EditText nameInput = dialogView.findViewById(R.id.nameInput);
            final EditText balanceInput = dialogView.findViewById(R.id.initialBalanceInput);
            final EditText descriptionInput = dialogView.findViewById(R.id.descriptionInput);
            final Spinner typeSpinner = dialogView.findViewById(R.id.typeSpinner);
            final Spinner paymentTypeSpinner = dialogView.findViewById(R.id.paymentTypeSpinner);
            final CheckBox includeInTotalCheckbox = dialogView.findViewById(R.id.includeInTotalCheckbox);
            final LinearLayout dateLayout = dialogView.findViewById(R.id.dateLayout);
            final TextView dateTextView = dialogView.findViewById(R.id.dateTextView);

            // Скрываем выбор валюты
            View currencySpinner = dialogView.findViewById(R.id.currencySpinner);
            if (currencySpinner != null) {
                currencySpinner.setVisibility(View.GONE);
            }

            // Заполняем данные
            nameInput.setText(account.getName());
            balanceInput.setText(String.valueOf(account.getBalance()));
            descriptionInput.setText(account.getDescription());
            includeInTotalCheckbox.setChecked(account.isIncludedInTotalBalance());

            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            final Date[] selectedDate = {account.getCreatedDate()};
            dateTextView.setText(sdf.format(selectedDate[0]));

            // ИСПРАВЛЕНО: лямбда вместо анонимного класса
            dateLayout.setOnClickListener(v -> {
                try {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(selectedDate[0]);

                    DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                            (view, year, month, dayOfMonth) -> {
                                Calendar cal = Calendar.getInstance();
                                cal.set(year, month, dayOfMonth);
                                selectedDate[0] = cal.getTime();
                                dateTextView.setText(sdf.format(selectedDate[0]));
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                    );
                    datePickerDialog.show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            // Типы счетов
            String[] accountTypes = {"Основной", "Сберегательный", "Инвестиционный", "Кредитный"};
            ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(getContext(),
                    android.R.layout.simple_spinner_item, accountTypes);
            typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            typeSpinner.setAdapter(typeAdapter);

            for (int i = 0; i < accountTypes.length; i++) {
                if (accountTypes[i].equals(account.getType())) {
                    typeSpinner.setSelection(i);
                    break;
                }
            }

            // Типы платежей
            String[] paymentTypes = {"Карта", "Наличные", "Электронные деньги", "Другой"};
            ArrayAdapter<String> paymentAdapter = new ArrayAdapter<>(getContext(),
                    android.R.layout.simple_spinner_item, paymentTypes);
            paymentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            paymentTypeSpinner.setAdapter(paymentAdapter);

            for (int i = 0; i < paymentTypes.length; i++) {
                if (paymentTypes[i].equals(account.getPaymentType())) {
                    paymentTypeSpinner.setSelection(i);
                    break;
                }
            }

            builder.setView(dialogView);

            // ИСПРАВЛЕНО: лямбда вместо анонимного класса
            builder.setPositiveButton("Сохранить", (dialog, which) -> {
                try {
                    String name = nameInput.getText().toString().trim();
                    String balanceStr = balanceInput.getText().toString().trim();
                    String description = descriptionInput.getText().toString().trim();
                    String type = (String) typeSpinner.getSelectedItem();
                    String paymentType = (String) paymentTypeSpinner.getSelectedItem();

                    boolean includeInTotal = includeInTotalCheckbox.isChecked();

                    if (name.isEmpty()) {
                        showError("Введите название счета");
                        return;
                    }

                    double balance = 0.0;
                    if (!balanceStr.isEmpty()) {
                        try {
                            balance = Double.parseDouble(balanceStr);
                        } catch (NumberFormatException e) {
                            showError("Некорректная сумма");
                            return;
                        }
                    }

                    account.setName(name);
                    account.setBalance(balance);
                    account.setType(type);
                    account.setPaymentType(paymentType);
                    account.setDescription(description);
                    account.setIncludedInTotalBalance(includeInTotal);
                    account.setCreatedDate(selectedDate[0]);

                    List<Account> accounts = dataManager.loadAccounts();

                    for (int i = 0; i < accounts.size(); i++) {
                        if (accounts.get(i).getId().equals(account.getId())) {
                            accounts.set(i, account);
                            break;
                        }
                    }

                    dataManager.saveAccounts(accounts);

                    showSuccess("Счет обновлен");
                    forceLoadAccounts();

                    if (mainActivity != null) {
                        mainActivity.updateNavHeader();
                    }

                } catch (Exception e) {
                    showError("Ошибка: " + e.getMessage());
                    e.printStackTrace();
                }
            });

            builder.setNegativeButton("Отмена", null);
            builder.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void toggleAccountInclusion(Account account) {
        if (account == null) return;

        try {
            account.setIncludedInTotalBalance(!account.isIncludedInTotalBalance());

            List<Account> accounts = dataManager.loadAccounts();

            for (int i = 0; i < accounts.size(); i++) {
                if (accounts.get(i).getId().equals(account.getId())) {
                    accounts.set(i, account);
                    break;
                }
            }

            dataManager.saveAccounts(accounts);

            showSuccess(account.isIncludedInTotalBalance() ?
                    "Счет теперь учитывается в балансе" : "Счет скрыт из баланса");

            forceLoadAccounts();

            if (mainActivity != null) {
                mainActivity.updateNavHeader();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void confirmDeleteAccount(final Account account) {
        if (getContext() == null || account == null) return;

        try {
            new AlertDialog.Builder(getContext())
                    .setTitle("❌ Удаление счета")
                    .setMessage("Вы уверены, что хотите удалить счет \"" + account.getName() + "\"?\n\n" +
                            "Баланс: " + account.getFormattedBalance() + "\n" +
                            "Дата создания: " + account.getFormattedDate() + "\n\n" +
                            "Все транзакции останутся, но без привязки к счету.")
                    // ИСПРАВЛЕНО: лямбда вместо анонимного класса
                    .setPositiveButton("Удалить", (dialog, which) -> {
                        try {
                            List<Account> accounts = dataManager.loadAccounts();
                            accounts.removeIf(a -> a.getId().equals(account.getId()));
                            dataManager.saveAccounts(accounts);

                            showSuccess("Счет удален");
                            forceLoadAccounts();

                            if (mainActivity != null) {
                                mainActivity.updateNavHeader();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAccountOptionsDialog(final Account account) {
        if (getContext() == null || account == null) return;

        try {
            String[] options = {
                    "📊 Информация",
                    "✏️ Редактировать",
                    "💰 Пополнить",
                    "💸 Снять",
                    "⏸️ " + (account.isIncludedInTotalBalance() ? "Скрыть из баланса" : "Показать в балансе"),
                    "❌ Удалить"
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Действия со счетом: " + account.getName());

            // ИСПРАВЛЕНО: лямбда вместо анонимного класса
            builder.setItems(options, (dialog, which) -> {
                try {
                    switch (which) {
                        case 0:
                            showAccountInfo(account);
                            break;
                        case 1:
                            showEditAccountDialog(account);
                            break;
                        case 2:
                            showAddMoneyDialog(account);
                            break;
                        case 3:
                            showWithdrawMoneyDialog(account);
                            break;
                        case 4:
                            toggleAccountInclusion(account);
                            break;
                        case 5:
                            confirmDeleteAccount(account);
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    showError("Ошибка: " + e.getMessage());
                }
            });

            builder.setNegativeButton("Отмена", null);
            builder.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAccountInfo(Account account) {
        if (getContext() == null || account == null) return;

        try {
            String info = String.format(Locale.getDefault(),
                    "💰 Баланс: %s\n\n" +
                            "📊 Тип: %s\n" +
                            "💳 Тип платежа: %s\n" +
                            "📝 Описание: %s\n" +
                            "📅 Дата создания: %s\n" +
                            "📌 Учитывается в балансе: %s",
                    account.getFormattedBalance(),
                    account.getType(),
                    account.getPaymentType(),
                    account.getDescription() == null || account.getDescription().isEmpty() ? "Нет описания" : account.getDescription(),
                    account.getFormattedDate(),
                    account.isIncludedInTotalBalance() ? "Да" : "Нет");

            new AlertDialog.Builder(getContext())
                    .setTitle("📊 Информация о счете")
                    .setMessage(info)
                    .setPositiveButton("OK", null)
                    .show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), "❌ " + message, Toast.LENGTH_SHORT).show();
        }
    }

    private void showSuccess(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), "✅ " + message, Toast.LENGTH_SHORT).show();
        }
    }
}