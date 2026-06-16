package com.example.financialtracker;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    private TextView balanceTextView;
    private TextView totalIncomeTextView;
    private TextView totalExpenseTextView;
    private TextView savingsBalanceTextView;
    private TextView investmentsBalanceTextView;
    private TextView creditBalanceTextView;
    private LinearLayout recentTransactionsContainer;
    private LinearLayout accountsSummaryContainer;
    private Button addTransactionButton;
    private Button addGoalButton;
    private MainActivity mainActivity;
    private CbrRateManager rateManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        mainActivity = (MainActivity) getActivity();

        if (mainActivity != null) {
            rateManager = mainActivity.getRateManager();
        }

        initViews(view);
        updateDashboard();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateDashboard();
    }

    private void initViews(View view) {
        balanceTextView = view.findViewById(R.id.balanceTextView);
        totalIncomeTextView = view.findViewById(R.id.totalIncomeTextView);
        totalExpenseTextView = view.findViewById(R.id.totalExpenseTextView);
        savingsBalanceTextView = view.findViewById(R.id.savingsBalanceTextView);
        investmentsBalanceTextView = view.findViewById(R.id.investmentsBalanceTextView);
        creditBalanceTextView = view.findViewById(R.id.creditBalanceTextView);
        recentTransactionsContainer = view.findViewById(R.id.recentTransactionsContainer);
        accountsSummaryContainer = view.findViewById(R.id.accountsSummaryContainer);
        addTransactionButton = view.findViewById(R.id.addTransactionButton);
        addGoalButton = view.findViewById(R.id.addGoalButton);

        addTransactionButton.setOnClickListener(v -> showAddTransactionDialog());
        addGoalButton.setOnClickListener(v -> showAddGoalDialog());
    }

    public void updateDashboard() {
        if (mainActivity == null) return;

        DataManager dataManager = mainActivity.getDataManager();

        double totalBalance = mainActivity.getBalance();

        String baseCurrencySymbol = "₽";
        balanceTextView.setText(String.format(Locale.getDefault(),
                "%s %.2f", baseCurrencySymbol, totalBalance));

        totalIncomeTextView.setText(String.format(Locale.getDefault(),
                "%.2f руб.", mainActivity.getTotalIncome()));
        totalExpenseTextView.setText(String.format(Locale.getDefault(),
                "%.2f руб.", mainActivity.getTotalExpenses()));

        double savingsBalance = 0;
        double investmentsBalance = 0;
        double creditBalance = 0;

        List<Account> accounts = dataManager.loadAccounts();
        for (Account account : accounts) {
            double balance = account.getBalance();

            switch (account.getType()) {
                case "Сберегательный":
                    savingsBalance += balance;
                    break;
                case "Инвестиционный":
                    investmentsBalance += balance;
                    break;
                case "Кредитный":
                    creditBalance += balance;
                    break;
            }
        }

        savingsBalanceTextView.setText(String.format(Locale.getDefault(),
                "%.2f %s", savingsBalance, baseCurrencySymbol));
        investmentsBalanceTextView.setText(String.format(Locale.getDefault(),
                "%.2f %s", investmentsBalance, baseCurrencySymbol));
        creditBalanceTextView.setText(String.format(Locale.getDefault(),
                "%.2f %s", creditBalance, baseCurrencySymbol));

        updateRecentTransactions();
        updateAccountsSummary();
    }

    private void updateRecentTransactions() {
        if (recentTransactionsContainer == null) return;

        recentTransactionsContainer.removeAllViews();

        List<Transaction> allTransactions = mainActivity.getTransactions();
        List<Transaction> recentTransactions = new ArrayList<>();

        int count = Math.min(allTransactions.size(), 5);
        for (int i = allTransactions.size() - 1; i >= 0 && recentTransactions.size() < count; i--) {
            recentTransactions.add(allTransactions.get(i));
        }

        if (recentTransactions.isEmpty()) {
            TextView emptyText = new TextView(getContext());
            emptyText.setText("Нет последних транзакций");
            emptyText.setTextSize(14);
            emptyText.setTextColor(Color.GRAY);
            emptyText.setGravity(android.view.Gravity.CENTER);
            emptyText.setPadding(0, 20, 0, 20);
            recentTransactionsContainer.addView(emptyText);
            return;
        }

        for (Transaction transaction : recentTransactions) {
            addTransactionView(transaction);
        }
    }

    private void addTransactionView(Transaction transaction) {
        if (getContext() == null) return;

        LinearLayout transactionLayout = new LinearLayout(getContext());
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.bottomMargin = 10;
        transactionLayout.setLayoutParams(layoutParams);
        transactionLayout.setOrientation(LinearLayout.HORIZONTAL);
        transactionLayout.setPadding(15, 10, 15, 10);
        transactionLayout.setBackgroundResource(R.drawable.card_background);

        TextView iconView = new TextView(getContext());
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(40, 40);
        iconView.setLayoutParams(iconParams);
        iconView.setText(transaction.isIncome() ? "📈" : "📉");
        iconView.setTextSize(18);
        iconView.setGravity(android.view.Gravity.CENTER);

        LinearLayout infoLayout = new LinearLayout(getContext());
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
        );
        infoParams.setMargins(10, 0, 10, 0);
        infoLayout.setLayoutParams(infoParams);
        infoLayout.setOrientation(LinearLayout.VERTICAL);

        TextView descriptionView = new TextView(getContext());
        descriptionView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        descriptionView.setText(transaction.getDescription());
        descriptionView.setTextSize(14);
        descriptionView.setTypeface(null, Typeface.BOLD);
        descriptionView.setTextColor(Color.BLACK);

        TextView categoryView = new TextView(getContext());
        categoryView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        categoryView.setText(transaction.getCategoryIcon() + " " + transaction.getCategoryWithoutIcon());
        categoryView.setTextSize(12);
        categoryView.setTextColor(mainActivity.getCategoryManager().getCategoryColor(
                transaction.getCategory(), transaction.isIncome()));
        categoryView.setPadding(0, 2, 0, 0);

        infoLayout.addView(descriptionView);
        infoLayout.addView(categoryView);

        LinearLayout amountDateLayout = new LinearLayout(getContext());
        amountDateLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        amountDateLayout.setOrientation(LinearLayout.VERTICAL);

        TextView amountView = new TextView(getContext());
        amountView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        amountView.setTextSize(14);
        amountView.setTypeface(null, Typeface.BOLD);
        amountView.setTextColor(transaction.isIncome() ?
                Color.parseColor("#4CAF50") : Color.parseColor("#F44336"));

        amountView.setText(transaction.getFormattedAmount());

        TextView dateView = new TextView(getContext());
        dateView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        dateView.setText(transaction.getFormattedDateShort());
        dateView.setTextSize(10);
        dateView.setTextColor(Color.GRAY);
        dateView.setPadding(0, 2, 0, 0);
        dateView.setGravity(android.view.Gravity.RIGHT);

        amountDateLayout.addView(amountView);
        amountDateLayout.addView(dateView);

        transactionLayout.addView(iconView);
        transactionLayout.addView(infoLayout);
        transactionLayout.addView(amountDateLayout);

        recentTransactionsContainer.addView(transactionLayout);
    }

    private void updateAccountsSummary() {
        if (accountsSummaryContainer == null) return;

        accountsSummaryContainer.removeAllViews();

        List<Account> accounts = mainActivity.getDataManager().loadAccounts();

        if (accounts.isEmpty()) {
            TextView emptyText = new TextView(getContext());
            emptyText.setText("Нет счетов для отображения");
            emptyText.setTextSize(14);
            emptyText.setTextColor(Color.GRAY);
            emptyText.setGravity(android.view.Gravity.CENTER);
            emptyText.setPadding(0, 20, 0, 20);
            accountsSummaryContainer.addView(emptyText);
            return;
        }

        int accountsToShow = Math.min(accounts.size(), 3);
        for (int i = 0; i < accountsToShow; i++) {
            addAccountSummaryView(accounts.get(i));
        }
    }

    private void addAccountSummaryView(Account account) {
        if (getContext() == null) return;

        LinearLayout accountLayout = new LinearLayout(getContext());
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.bottomMargin = 8;
        accountLayout.setLayoutParams(layoutParams);
        accountLayout.setOrientation(LinearLayout.HORIZONTAL);
        accountLayout.setPadding(12, 8, 12, 8);
        accountLayout.setBackgroundResource(R.drawable.card_background);

        TextView iconView = new TextView(getContext());
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(36, 36);
        iconView.setLayoutParams(iconParams);
        iconView.setText(account.getIcon());
        iconView.setTextSize(16);
        iconView.setGravity(android.view.Gravity.CENTER);

        LinearLayout infoLayout = new LinearLayout(getContext());
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
        );
        infoParams.setMargins(8, 0, 8, 0);
        infoLayout.setLayoutParams(infoParams);
        infoLayout.setOrientation(LinearLayout.VERTICAL);

        TextView nameView = new TextView(getContext());
        nameView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        nameView.setText(account.getName());
        nameView.setTextSize(13);
        nameView.setTextColor(Color.BLACK);

        TextView typeView = new TextView(getContext());
        typeView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        typeView.setTextSize(11);
        typeView.setTextColor(Color.GRAY);
        typeView.setPadding(0, 2, 0, 0);

        infoLayout.addView(nameView);
        infoLayout.addView(typeView);

        TextView balanceView = new TextView(getContext());
        balanceView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        balanceView.setText(account.getFormattedBalance());
        balanceView.setTextSize(13);
        balanceView.setTypeface(null, Typeface.BOLD);
        balanceView.setTextColor(account.getBalanceColor());

        LinearLayout balanceContainer = new LinearLayout(getContext());
        balanceContainer.setOrientation(LinearLayout.HORIZONTAL);
        balanceContainer.addView(balanceView);

        accountLayout.addView(iconView);
        accountLayout.addView(infoLayout);
        accountLayout.addView(balanceContainer);

        accountsSummaryContainer.addView(accountLayout);
    }

    private void showAddGoalDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("🎯 Добавить финансовую цель");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_goal, null);

        final EditText goalNameInput = dialogView.findViewById(R.id.goalNameInput);
        final EditText targetAmountInput = dialogView.findViewById(R.id.targetAmountInput);
        final EditText currentAmountInput = dialogView.findViewById(R.id.currentAmountInput);
        final Spinner prioritySpinner = dialogView.findViewById(R.id.prioritySpinner);
        final EditText notesInput = dialogView.findViewById(R.id.notesInput);
        final LinearLayout deadlineLayout = dialogView.findViewById(R.id.deadlineLayout);
        final TextView deadlineTextView = dialogView.findViewById(R.id.deadlineTextView);

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 30);
        final Date[] selectedDeadline = {calendar.getTime()};
        deadlineTextView.setText(sdf.format(selectedDeadline[0]));

        deadlineLayout.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            cal.setTime(selectedDeadline[0]);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    getContext(),
                    (view, year, month, dayOfMonth) -> {
                        Calendar selected = Calendar.getInstance();
                        selected.set(year, month, dayOfMonth);
                        selectedDeadline[0] = selected.getTime();
                        deadlineTextView.setText(sdf.format(selectedDeadline[0]));
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        String[] priorities = {"Низкий", "Средний", "Высокий", "Критический"};
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, priorities);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        prioritySpinner.setAdapter(priorityAdapter);

        builder.setView(dialogView);
        builder.setPositiveButton("Добавить", (dialog, which) -> {
            try {
                String goalName = goalNameInput.getText().toString().trim();
                String targetAmountStr = targetAmountInput.getText().toString().trim();
                String currentAmountStr = currentAmountInput.getText().toString().trim();
                String priority = (String) prioritySpinner.getSelectedItem();
                String notes = notesInput.getText().toString().trim();

                if (goalName.isEmpty()) {
                    Toast.makeText(getContext(), "Введите название цели", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (targetAmountStr.isEmpty()) {
                    Toast.makeText(getContext(), "Введите целевую сумму", Toast.LENGTH_SHORT).show();
                    return;
                }

                double targetAmount = Double.parseDouble(targetAmountStr);
                if (targetAmount <= 0) {
                    Toast.makeText(getContext(), "Целевая сумма должна быть больше 0", Toast.LENGTH_SHORT).show();
                    return;
                }

                double currentAmount = 0.0;
                if (!currentAmountStr.isEmpty()) {
                    currentAmount = Double.parseDouble(currentAmountStr);
                    if (currentAmount < 0) {
                        Toast.makeText(getContext(), "Текущая сумма не может быть отрицательной", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (currentAmount > targetAmount) {
                        Toast.makeText(getContext(), "Текущая сумма не может превышать целевую", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                FinancialGoal goal = new FinancialGoal(
                        goalName,
                        targetAmount,
                        currentAmount,
                        selectedDeadline[0],
                        priority,
                        notes
                );

                DataManager dataManager = mainActivity.getDataManager();
                List<FinancialGoal> goals = dataManager.loadGoals();
                if (goals == null) {
                    goals = new ArrayList<>();
                }

                goals.add(goal);
                dataManager.saveGoals(goals);

                Toast.makeText(getContext(),
                        "Цель '" + goalName + "' добавлена",
                        Toast.LENGTH_SHORT).show();

                showGoalProgressNotification(goal);

            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Некорректная сумма", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(getContext(), "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void showGoalProgressNotification(FinancialGoal goal) {
        double progress = goal.getProgressPercentage();
        String message = String.format(Locale.getDefault(),
                "🎯 Цель: %s\n" +
                        "💰 Целевая сумма: %.2f руб.\n" +
                        "💵 Текущая сумма: %.2f руб.\n" +
                        "📈 Прогресс: %.1f%%\n" +
                        "📅 Срок: %s",
                goal.getName(),
                goal.getTargetAmount(),
                goal.getCurrentAmount(),
                progress,
                goal.getFormattedDeadline());

        if (progress >= 100) {
            message += "\n\n✅ Цель достигнута! Поздравляем!";
        } else if (progress >= 80) {
            message += "\n\n🔥 Почти у цели! Осталось совсем немного.";
        } else if (progress >= 50) {
            message += "\n\n👍 Хороший прогресс! Продолжайте в том же духе.";
        } else if (progress >= 20) {
            message += "\n\n💪 Начало положено! Не сдавайтесь.";
        } else {
            message += "\n\n🚀 Только начинаем! Вперед к цели!";
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Прогресс цели");
        builder.setMessage(message);
        builder.setPositiveButton("OK", null);

        if (progress >= 100) {
            builder.setNeutralButton("Отметить выполненной", (dialog, which) -> markGoalAsCompleted(goal));
        }

        builder.show();
    }

    private void markGoalAsCompleted(FinancialGoal goal) {
        goal.setCompleted(true);

        DataManager dataManager = mainActivity.getDataManager();
        List<FinancialGoal> goals = dataManager.loadGoals();
        dataManager.saveGoals(goals);

        Toast.makeText(getContext(),
                "Цель '" + goal.getName() + "' отмечена как выполненная!",
                Toast.LENGTH_SHORT).show();
    }

    private void showAddTransactionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("➕ Добавить транзакцию");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_transaction_enhanced, null);

        final TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        final TextView operationsInfo = dialogView.findViewById(R.id.operationsInfo);
        final Button incomeTypeButton = dialogView.findViewById(R.id.incomeTypeButton);
        final Button expenseTypeButton = dialogView.findViewById(R.id.expenseTypeButton);
        final Spinner accountSpinner = dialogView.findViewById(R.id.accountSpinner);
        final Spinner categorySpinner = dialogView.findViewById(R.id.categorySpinner);
        final EditText newCategoryEditText = dialogView.findViewById(R.id.newCategoryEditText);
        final Button cashButton = dialogView.findViewById(R.id.cashButton);
        final Button cardButton = dialogView.findViewById(R.id.cardButton);
        final Button electronicButton = dialogView.findViewById(R.id.electronicButton);
        final EditText amountEditText = dialogView.findViewById(R.id.amountEditText);
        final EditText notesEditText = dialogView.findViewById(R.id.notesEditText);
        final LinearLayout dateLayout = dialogView.findViewById(R.id.dateLayout);
        final TextView dateTextView = dialogView.findViewById(R.id.dateTextView);

        final Spinner currencySpinner = dialogView.findViewById(R.id.currencySpinner);
        if (currencySpinner != null) {
            currencySpinner.setVisibility(View.GONE);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        dateTextView.setText(sdf.format(new Date()));

        List<Account> accounts = mainActivity.getDataManager().loadAccounts();
        List<String> accountNames = new ArrayList<>();
        accountNames.add("Без счета");

        for (Account account : accounts) {
            accountNames.add(account.getName() + " - " + account.getFormattedBalance());
        }

        if (accountNames.isEmpty()) {
            accountNames.add("Нет доступных счетов");
        }

        ArrayAdapter<String> accountAdapter = new ArrayAdapter<>(
                getContext(), android.R.layout.simple_spinner_item, accountNames);
        accountAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        accountSpinner.setAdapter(accountAdapter);

        List<String> incomeCategories = mainActivity.getCategoryManager().getIncomeCategories();
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                getContext(), android.R.layout.simple_spinner_item, incomeCategories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        final boolean[] isIncome = {true};
        final String[] paymentType = {"Карта"};
        final Date[] selectedDate = {new Date()};

        incomeTypeButton.setOnClickListener(v -> {
            isIncome[0] = true;
            incomeTypeButton.setBackgroundResource(R.drawable.button_type_selected);
            expenseTypeButton.setBackgroundResource(R.drawable.button_type_unselected);

            List<String> incomeCategories1 = mainActivity.getCategoryManager().getIncomeCategories();
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    getContext(), android.R.layout.simple_spinner_item, incomeCategories1);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            categorySpinner.setAdapter(adapter);
        });

        expenseTypeButton.setOnClickListener(v -> {
            isIncome[0] = false;
            expenseTypeButton.setBackgroundResource(R.drawable.button_type_selected);
            incomeTypeButton.setBackgroundResource(R.drawable.button_type_unselected);

            List<String> expenseCategories = mainActivity.getCategoryManager().getExpenseCategories();
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    getContext(), android.R.layout.simple_spinner_item, expenseCategories);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            categorySpinner.setAdapter(adapter);
        });

        cashButton.setOnClickListener(v -> {
            paymentType[0] = "Наличные";
            cashButton.setBackgroundResource(R.drawable.button_payment_selected);
            cardButton.setBackgroundResource(R.drawable.button_payment_unselected);
            electronicButton.setBackgroundResource(R.drawable.button_payment_unselected);
        });

        cardButton.setOnClickListener(v -> {
            paymentType[0] = "Карта";
            cardButton.setBackgroundResource(R.drawable.button_payment_selected);
            cashButton.setBackgroundResource(R.drawable.button_payment_unselected);
            electronicButton.setBackgroundResource(R.drawable.button_payment_unselected);
        });

        electronicButton.setOnClickListener(v -> {
            paymentType[0] = "Электронные деньги";
            electronicButton.setBackgroundResource(R.drawable.button_payment_selected);
            cashButton.setBackgroundResource(R.drawable.button_payment_unselected);
            cardButton.setBackgroundResource(R.drawable.button_payment_unselected);
        });

        dateLayout.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    getContext(),
                    (view, year1, month1, dayOfMonth) -> {
                        Calendar selectedCalendar = Calendar.getInstance();
                        selectedCalendar.set(year1, month1, dayOfMonth);
                        selectedDate[0] = selectedCalendar.getTime();
                        dateTextView.setText(String.format(Locale.getDefault(), "%02d.%02d.%d", dayOfMonth, month1 + 1, year1));
                    },
                    year, month, day
            );
            datePickerDialog.show();
        });

        incomeTypeButton.performClick();
        cardButton.performClick();

        builder.setView(dialogView);
        builder.setPositiveButton("Добавить", (dialog, which) -> {
            try {
                String amountStr = amountEditText.getText().toString().trim();
                if (amountStr.isEmpty()) {
                    Toast.makeText(getContext(), "Введите сумму", Toast.LENGTH_SHORT).show();
                    return;
                }

                double amount = Double.parseDouble(amountStr);
                if (amount <= 0) {
                    Toast.makeText(getContext(), "Сумма должна быть больше 0", Toast.LENGTH_SHORT).show();
                    return;
                }

                String category = (String) categorySpinner.getSelectedItem();
                if (category == null) {
                    Toast.makeText(getContext(), "Выберите категорию", Toast.LENGTH_SHORT).show();
                    return;
                }

                String description = "Операция";
                String notes = notesEditText.getText().toString().trim();

                String accountInfo = (String) accountSpinner.getSelectedItem();
                String accountName = "Без счета";
                if (accountInfo != null && !accountInfo.equals("Без счета") && accountInfo.contains(" - ")) {
                    accountName = accountInfo.substring(0, accountInfo.indexOf(" - "));
                }

                Transaction transaction = new Transaction(
                        description,
                        amount,
                        isIncome[0],
                        selectedDate[0],
                        category,
                        notes,
                        accountName,
                        paymentType[0]
                );

                mainActivity.addTransaction(transaction);

                Toast.makeText(getContext(),
                        (isIncome[0] ? "📈 Доход" : "📉 Расход") + " добавлен",
                        Toast.LENGTH_SHORT).show();

                updateDashboard();

            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Некорректная сумма", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(getContext(), "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Отмена", null);
        builder.show();
    }
}