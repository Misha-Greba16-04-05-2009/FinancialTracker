package com.example.financialtracker;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DataFragment extends Fragment {

    private LinearLayout transactionsContainer;
    private TextView emptyStateTextView;
    private Button filterButton;
    private Button selectAllButton;
    private Button deleteSelectedButton;
    private Button cancelSelectionButton;
    private LinearLayout selectionBar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView selectedCountText;

    private MainActivity mainActivity;
    private List<Transaction> displayedTransactions = new ArrayList<>();
    private boolean isSelectionMode = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_data, container, false);

        mainActivity = (MainActivity) getActivity();

        initViews(view);
        setupListeners();
        updateTransactionsList();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateTransactionsList();
    }

    private void initViews(View view) {
        transactionsContainer = view.findViewById(R.id.transactionsContainer);
        emptyStateTextView = view.findViewById(R.id.emptyStateTextView);
        filterButton = view.findViewById(R.id.filterButton);
        selectAllButton = view.findViewById(R.id.selectAllButton);
        deleteSelectedButton = view.findViewById(R.id.deleteSelectedButton);
        cancelSelectionButton = view.findViewById(R.id.cancelSelectionButton);
        selectionBar = view.findViewById(R.id.selectionBar);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        selectedCountText = view.findViewById(R.id.selectedCountText);
    }

    private void setupListeners() {
        filterButton.setOnClickListener(v -> showFilterDialog());
        selectAllButton.setOnClickListener(v -> selectAllTransactions());
        deleteSelectedButton.setOnClickListener(v -> confirmDeleteSelected());
        cancelSelectionButton.setOnClickListener(v -> exitSelectionMode());

        swipeRefreshLayout.setOnRefreshListener(() -> {
            updateTransactionsList();
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    public void updateTransactionsList() {
        if (mainActivity == null || transactionsContainer == null) return;

        transactionsContainer.removeAllViews();

        List<Transaction> allTransactions = mainActivity.getTransactions();

        if (allTransactions == null) {
            allTransactions = new ArrayList<>();
        }

        if (allTransactions.isEmpty()) {
            emptyStateTextView.setVisibility(View.VISIBLE);
            transactionsContainer.setVisibility(View.GONE);
            return;
        }

        emptyStateTextView.setVisibility(View.GONE);
        transactionsContainer.setVisibility(View.VISIBLE);

        // Копируем и сортируем (новые сверху)
        displayedTransactions = new ArrayList<>(allTransactions);
        Collections.sort(displayedTransactions, (t1, t2) -> {
            if (t1.getDate() == null && t2.getDate() == null) return 0;
            if (t1.getDate() == null) return 1;
            if (t2.getDate() == null) return -1;
            return t2.getDate().compareTo(t1.getDate());
        });

        for (Transaction transaction : displayedTransactions) {
            addTransactionView(transaction);
        }

        updateSelectionUI();
    }

    // ============ ИСПРАВЛЕННЫЙ МЕТОД addTransactionView ============
    private void addTransactionView(final Transaction transaction) {
        if (getContext() == null || transaction == null) return;

        try {
            LinearLayout transactionLayout = new LinearLayout(getContext());
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            layoutParams.setMargins(16, 8, 16, 8);
            transactionLayout.setLayoutParams(layoutParams);
            transactionLayout.setOrientation(LinearLayout.VERTICAL);
            transactionLayout.setPadding(16, 12, 16, 12);

            // Фон карточки (меняется с темой)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                transactionLayout.setBackgroundColor(getResources().getColor(R.color.card_background, null));
            } else {
                transactionLayout.setBackgroundColor(getResources().getColor(R.color.card_background));
            }
            transactionLayout.setElevation(2f);

            // Верхняя строка
            LinearLayout topRow = new LinearLayout(getContext());
            topRow.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            topRow.setOrientation(LinearLayout.HORIZONTAL);

            // Checkbox для выбора (только в режиме выбора)
            final CheckBox selectCheckBox = new CheckBox(getContext());
            selectCheckBox.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            selectCheckBox.setVisibility(isSelectionMode ? View.VISIBLE : View.GONE);
            selectCheckBox.setChecked(transaction.isSelected());
            selectCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                transaction.setSelected(isChecked);
                updateSelectedCount();
            });

            // Иконка
            TextView iconView = new TextView(getContext());
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(48, 48);
            iconView.setLayoutParams(iconParams);
            iconView.setText(transaction.isIncome() ? "📈" : "📉");
            iconView.setTextSize(20);
            iconView.setGravity(android.view.Gravity.CENTER);

            // Информация
            LinearLayout infoLayout = new LinearLayout(getContext());
            LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1.0f
            );
            infoParams.setMargins(12, 0, 12, 0);
            infoLayout.setLayoutParams(infoParams);
            infoLayout.setOrientation(LinearLayout.VERTICAL);

            // ОПИСАНИЕ (теперь цвет из ресурсов - белый в тёмной теме)
            TextView descriptionView = new TextView(getContext());
            descriptionView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));

            String description = transaction.getDescription();
            if (description == null || description.isEmpty()) {
                description = transaction.isIncome() ? "Доход" : "Расход";
            }
            descriptionView.setText(description);
            descriptionView.setTextSize(15);
            descriptionView.setTypeface(null, Typeface.BOLD);
            descriptionView.setTextColor(getResources().getColor(R.color.text_primary));

            // КАТЕГОРИЯ (цвет из ресурсов)
            TextView categoryView = new TextView(getContext());
            categoryView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));

            String category = transaction.getCategoryIcon() + " " + transaction.getCategoryWithoutIcon();
            categoryView.setText(category);
            categoryView.setTextSize(12);
            categoryView.setTextColor(getResources().getColor(R.color.text_secondary));
            categoryView.setPadding(0, 2, 0, 0);

            infoLayout.addView(descriptionView);
            infoLayout.addView(categoryView);

            // Сумма и дата
            LinearLayout amountDateLayout = new LinearLayout(getContext());
            amountDateLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            amountDateLayout.setOrientation(LinearLayout.VERTICAL);
            amountDateLayout.setGravity(android.view.Gravity.END);

            // СУММА (цвет из ресурсов - зелёный/красный)
            TextView amountView = new TextView(getContext());
            amountView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            amountView.setText(transaction.getFormattedAmount());
            amountView.setTextSize(16);
            amountView.setTypeface(null, Typeface.BOLD);
            if (transaction.isIncome()) {
                amountView.setTextColor(getResources().getColor(R.color.income_color));
            } else {
                amountView.setTextColor(getResources().getColor(R.color.expense_color));
            }

            // ДАТА (цвет из ресурсов)
            TextView dateView = new TextView(getContext());
            dateView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            dateView.setText(transaction.getFormattedDateShort());
            dateView.setTextSize(11);
            dateView.setTextColor(getResources().getColor(R.color.text_hint));
            dateView.setPadding(0, 2, 0, 0);

            amountDateLayout.addView(amountView);
            amountDateLayout.addView(dateView);

            topRow.addView(selectCheckBox);
            topRow.addView(iconView);
            topRow.addView(infoLayout);
            topRow.addView(amountDateLayout);

            // Нижняя строка (счет и тип платежа)
            LinearLayout bottomRow = new LinearLayout(getContext());
            bottomRow.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            bottomRow.setPadding(0, 8, 0, 0);

            String accountName = transaction.getAccountName();
            if (accountName == null || accountName.isEmpty() || accountName.equals("Без счета")) {
                accountName = "Без счета";
            }

            String accountText = transaction.getPaymentIcon() + " " + accountName;

            TextView accountView = new TextView(getContext());
            accountView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            accountView.setText(accountText);
            accountView.setTextSize(11);
            accountView.setTextColor(getResources().getColor(R.color.text_secondary));
            accountView.setPadding(0, 0, 16, 0);

            bottomRow.addView(accountView);

            transactionLayout.addView(topRow);
            transactionLayout.addView(bottomRow);

            // Обработчик клика
            transactionLayout.setOnClickListener(v -> {
                if (isSelectionMode) {
                    selectCheckBox.setChecked(!selectCheckBox.isChecked());
                } else {
                    showTransactionDetails(transaction);
                }
            });

            // Долгое нажатие
            transactionLayout.setOnLongClickListener(v -> {
                if (!isSelectionMode) {
                    enterSelectionMode();
                    selectCheckBox.setChecked(true);
                }
                return true;
            });

            transactionsContainer.addView(transactionLayout);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ============ РЕЖИМ ВЫБОРА ============

    private void enterSelectionMode() {
        isSelectionMode = true;
        selectionBar.setVisibility(View.VISIBLE);
        filterButton.setVisibility(View.GONE);

        for (Transaction transaction : displayedTransactions) {
            transaction.setSelected(false);
        }

        refreshTransactionViews();
        updateSelectedCount();
    }

    private void exitSelectionMode() {
        isSelectionMode = false;
        selectionBar.setVisibility(View.GONE);
        filterButton.setVisibility(View.VISIBLE);

        for (Transaction transaction : displayedTransactions) {
            transaction.setSelected(false);
        }

        refreshTransactionViews();
    }

    private void selectAllTransactions() {
        boolean allSelected = true;
        for (Transaction transaction : displayedTransactions) {
            if (!transaction.isSelected()) {
                allSelected = false;
                break;
            }
        }

        for (Transaction transaction : displayedTransactions) {
            transaction.setSelected(!allSelected);
        }

        refreshTransactionViews();
        updateSelectedCount();
    }

    private void updateSelectedCount() {
        int count = 0;
        for (Transaction transaction : displayedTransactions) {
            if (transaction.isSelected()) {
                count++;
            }
        }
        selectedCountText.setText("Выбрано: " + count);
        deleteSelectedButton.setEnabled(count > 0);
    }

    private void refreshTransactionViews() {
        transactionsContainer.removeAllViews();
        for (Transaction transaction : displayedTransactions) {
            addTransactionView(transaction);
        }
    }

    private void updateSelectionUI() {
        if (isSelectionMode) {
            selectionBar.setVisibility(View.VISIBLE);
            filterButton.setVisibility(View.GONE);
        } else {
            selectionBar.setVisibility(View.GONE);
            filterButton.setVisibility(View.VISIBLE);
        }
        refreshTransactionViews();
    }

    // ============ УДАЛЕНИЕ ============

    private void confirmDeleteSelected() {
        int selectedCount = 0;
        for (Transaction transaction : displayedTransactions) {
            if (transaction.isSelected()) {
                selectedCount++;
            }
        }

        if (selectedCount == 0) {
            Toast.makeText(getContext(), "Нет выбранных транзакций", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(getContext())
                .setTitle("Удаление транзакций")
                .setMessage("Вы уверены, что хотите удалить " + selectedCount + " транзакций?\n\nЭто действие нельзя отменить!")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    deleteSelectedTransactions();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void deleteSelectedTransactions() {
        List<Transaction> allTransactions = mainActivity.getTransactions();
        List<Transaction> toRemove = new ArrayList<>();

        for (Transaction transaction : allTransactions) {
            if (transaction.isSelected()) {
                toRemove.add(transaction);
            }
        }

        for (Transaction transaction : toRemove) {
            allTransactions.remove(transaction);

            // Корректируем доходы/расходы
            if (transaction.isIncome()) {
                mainActivity.updateTotalIncome(mainActivity.getTotalIncome() - transaction.getAmount());
            } else {
                mainActivity.updateTotalExpenses(mainActivity.getTotalExpenses() - transaction.getAmount());
            }
        }

        mainActivity.saveAllData();
        mainActivity.updateFragments();
        mainActivity.updateNavHeader();

        exitSelectionMode();
        updateTransactionsList();

        Toast.makeText(getContext(), "Удалено " + toRemove.size() + " транзакций", Toast.LENGTH_SHORT).show();
    }

    // ============ ДЕТАЛИ ТРАНЗАКЦИИ ============

    private void showTransactionDetails(Transaction transaction) {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Детали транзакции");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_transaction_details, null);

        TextView typeValue = dialogView.findViewById(R.id.typeValue);
        TextView categoryValue = dialogView.findViewById(R.id.categoryValue);
        TextView amountValue = dialogView.findViewById(R.id.amountValue);
        TextView dateValue = dialogView.findViewById(R.id.dateValue);
        TextView accountValue = dialogView.findViewById(R.id.accountValue);
        TextView paymentTypeValue = dialogView.findViewById(R.id.paymentTypeValue);
        TextView descriptionValue = dialogView.findViewById(R.id.descriptionValue);
        TextView notesValue = dialogView.findViewById(R.id.notesValue);

        typeValue.setText(transaction.isIncome() ? "Доход" : "Расход");
        typeValue.setTextColor(transaction.isIncome() ?
                Color.parseColor("#4CAF50") : Color.parseColor("#F44336"));

        categoryValue.setText(transaction.getCategoryIcon() + " " + transaction.getCategoryWithoutIcon());
        amountValue.setText(transaction.getFormattedAmount());

        dateValue.setText(transaction.getFormattedDate());

        String accountName = transaction.getAccountName();
        if (accountName == null || accountName.isEmpty()) {
            accountName = "Не указан";
        }
        accountValue.setText(accountName);

        paymentTypeValue.setText(transaction.getPaymentIcon() + " " +
                (transaction.getPaymentType() != null ? transaction.getPaymentType() : "Карта"));

        descriptionValue.setText(transaction.getDescription() != null ? transaction.getDescription() : "Нет");
        notesValue.setText(transaction.getNotes() != null && !transaction.getNotes().isEmpty() ?
                transaction.getNotes() : "Нет");

        builder.setView(dialogView);
        builder.setPositiveButton("OK", null);
        builder.setNeutralButton("Удалить", (dialog, which) -> {
            confirmDeleteTransaction(transaction);
        });

        builder.show();
    }

    private void confirmDeleteTransaction(final Transaction transaction) {
        new AlertDialog.Builder(getContext())
                .setTitle("Удаление транзакции")
                .setMessage("Вы уверены, что хотите удалить эту транзакцию?")
                .setPositiveButton("Удалить", (dialog, which) -> deleteTransaction(transaction))
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void deleteTransaction(Transaction transaction) {
        List<Transaction> allTransactions = mainActivity.getTransactions();
        allTransactions.remove(transaction);

        if (transaction.isIncome()) {
            mainActivity.updateTotalIncome(mainActivity.getTotalIncome() - transaction.getAmount());
        } else {
            mainActivity.updateTotalExpenses(mainActivity.getTotalExpenses() - transaction.getAmount());
        }

        mainActivity.saveAllData();
        mainActivity.updateFragments();
        mainActivity.updateNavHeader();

        updateTransactionsList();

        Toast.makeText(getContext(), "Транзакция удалена", Toast.LENGTH_SHORT).show();
    }

    // ============ ФИЛЬТРАЦИЯ ============

    private void showFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Фильтр транзакций");

        String[] options = {"Все", "Только доходы", "Только расходы", "По категории", "По дате", "По счету"};

        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    updateTransactionsList();
                    break;
                case 1:
                    filterByType(true);
                    break;
                case 2:
                    filterByType(false);
                    break;
                case 3:
                    showCategoryFilterDialog();
                    break;
                case 4:
                    showDateFilterDialog();
                    break;
                case 5:
                    showAccountFilterDialog();
                    break;
            }
        });

        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void filterByType(boolean isIncome) {
        List<Transaction> allTransactions = mainActivity.getTransactions();
        List<Transaction> filtered = new ArrayList<>();

        for (Transaction t : allTransactions) {
            if (t.isIncome() == isIncome) {
                filtered.add(t);
            }
        }

        displayedTransactions = filtered;
        Collections.sort(displayedTransactions, (t1, t2) -> {
            if (t1.getDate() == null && t2.getDate() == null) return 0;
            if (t1.getDate() == null) return 1;
            if (t2.getDate() == null) return -1;
            return t2.getDate().compareTo(t1.getDate());
        });
        refreshTransactionViews();
    }

    private void showCategoryFilterDialog() {
        if (mainActivity == null) return;

        List<String> categories = new ArrayList<>();
        categories.addAll(mainActivity.getCategoryManager().getExpenseCategories());
        categories.addAll(mainActivity.getCategoryManager().getIncomeCategories());

        String[] categoryArray = categories.toArray(new String[0]);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Выберите категорию");
        builder.setItems(categoryArray, (dialog, which) -> {
            String selectedCategory = categoryArray[which];
            filterByCategory(selectedCategory);
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void filterByCategory(String category) {
        List<Transaction> allTransactions = mainActivity.getTransactions();
        List<Transaction> filtered = new ArrayList<>();

        for (Transaction t : allTransactions) {
            if (t.getCategory() != null && t.getCategory().equals(category)) {
                filtered.add(t);
            }
        }

        displayedTransactions = filtered;
        Collections.sort(displayedTransactions, (t1, t2) -> {
            if (t1.getDate() == null && t2.getDate() == null) return 0;
            if (t1.getDate() == null) return 1;
            if (t2.getDate() == null) return -1;
            return t2.getDate().compareTo(t1.getDate());
        });
        refreshTransactionViews();
    }

    private void showDateFilterDialog() {
        android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(
                getContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar cal = Calendar.getInstance();
                    cal.set(year, month, dayOfMonth);
                    Date selectedDate = cal.getTime();
                    filterByDate(selectedDate);
                },
                Calendar.getInstance().get(Calendar.YEAR),
                Calendar.getInstance().get(Calendar.MONTH),
                Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void filterByDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        String targetDate = sdf.format(date);

        List<Transaction> allTransactions = mainActivity.getTransactions();
        List<Transaction> filtered = new ArrayList<>();

        for (Transaction t : allTransactions) {
            if (t.getDate() != null && sdf.format(t.getDate()).equals(targetDate)) {
                filtered.add(t);
            }
        }

        displayedTransactions = filtered;
        Collections.sort(displayedTransactions, (t1, t2) -> {
            if (t1.getDate() == null && t2.getDate() == null) return 0;
            if (t1.getDate() == null) return 1;
            if (t2.getDate() == null) return -1;
            return t2.getDate().compareTo(t1.getDate());
        });
        refreshTransactionViews();
    }

    private void showAccountFilterDialog() {
        List<Account> accounts = mainActivity.getDataManager().loadAccounts();
        List<String> accountNames = new ArrayList<>();
        accountNames.add("Все счета");
        for (Account a : accounts) {
            accountNames.add(a.getName());
        }

        String[] accountArray = accountNames.toArray(new String[0]);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Выберите счет");
        builder.setItems(accountArray, (dialog, which) -> {
            if (which == 0) {
                updateTransactionsList();
            } else {
                String selectedAccount = accountArray[which];
                filterByAccount(selectedAccount);
            }
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void filterByAccount(String accountName) {
        List<Transaction> allTransactions = mainActivity.getTransactions();
        List<Transaction> filtered = new ArrayList<>();

        for (Transaction t : allTransactions) {
            if (t.getAccountName() != null && t.getAccountName().equals(accountName)) {
                filtered.add(t);
            }
        }

        displayedTransactions = filtered;
        Collections.sort(displayedTransactions, (t1, t2) -> {
            if (t1.getDate() == null && t2.getDate() == null) return 0;
            if (t1.getDate() == null) return 1;
            if (t2.getDate() == null) return -1;
            return t2.getDate().compareTo(t1.getDate());
        });
        refreshTransactionViews();
    }
}