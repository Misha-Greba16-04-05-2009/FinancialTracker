package com.example.financialtracker;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BudgetsFragment extends Fragment {

    private LinearLayout budgetsContainer;
    private Button addBudgetButton;
    private TextView totalBudgetsTextView;
    private TextView activeBudgetsTextView;
    private TextView exceededBudgetsTextView;
    private TextView totalAmountTextView;
    private TextView totalSpentTextView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Spinner filterSpinner;

    private MainActivity mainActivity;
    private DataManager dataManager;
    private List<Budget> allBudgets = new ArrayList<>();
    private String currentFilter = "Все";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_budgets, container, false);

        mainActivity = (MainActivity) getActivity();
        dataManager = mainActivity.getDataManager();

        initViews(view);
        setupListeners();
        loadBudgets();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadBudgets();
    }

    private void initViews(View view) {
        budgetsContainer = view.findViewById(R.id.budgetsContainer);
        addBudgetButton = view.findViewById(R.id.addBudgetButton);
        totalBudgetsTextView = view.findViewById(R.id.totalBudgetsTextView);
        activeBudgetsTextView = view.findViewById(R.id.activeBudgetsTextView);
        exceededBudgetsTextView = view.findViewById(R.id.exceededBudgetsTextView);
        totalAmountTextView = view.findViewById(R.id.totalAmountTextView);
        totalSpentTextView = view.findViewById(R.id.totalSpentTextView);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        filterSpinner = view.findViewById(R.id.filterSpinner);

        // Настройка фильтра
        String[] filters = {"Все", "Активные", "Неактивные", "Превышенные", "По категории"};
        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, filters);
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterSpinner.setAdapter(filterAdapter);
    }

    private void setupListeners() {
        addBudgetButton.setOnClickListener(v -> showAddBudgetDialog());

        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadBudgets();
            swipeRefreshLayout.setRefreshing(false);
        });

        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentFilter = (String) parent.getItemAtPosition(position);
                filterBudgets();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    public void loadBudgets() {
        if (mainActivity == null || budgetsContainer == null) return;

        allBudgets = dataManager.loadBudgets();
        if (allBudgets == null) {
            allBudgets = new ArrayList<>();
        }

        updateStatistics();
        filterBudgets();
    }

    private void updateStatistics() {
        int total = allBudgets.size();
        int active = 0;
        int exceeded = 0;
        double totalAmount = 0;
        double totalSpent = 0;

        for (Budget budget : allBudgets) {
            if (budget.isActive()) active++;
            if (budget.isExceeded()) exceeded++;
            totalAmount += budget.getAmount();
            totalSpent += budget.getSpent();
        }

        totalBudgetsTextView.setText(String.valueOf(total));
        activeBudgetsTextView.setText(String.valueOf(active));
        exceededBudgetsTextView.setText(String.valueOf(exceeded));
        totalAmountTextView.setText(String.format(Locale.getDefault(), "%.0f руб.", totalAmount));
        totalSpentTextView.setText(String.format(Locale.getDefault(), "%.0f руб.", totalSpent));
    }

    private void filterBudgets() {
        List<Budget> filtered = new ArrayList<>();

        for (Budget budget : allBudgets) {
            switch (currentFilter) {
                case "Активные":
                    if (budget.isActive()) filtered.add(budget);
                    break;
                case "Неактивные":
                    if (!budget.isActive()) filtered.add(budget);
                    break;
                case "Превышенные":
                    if (budget.isExceeded()) filtered.add(budget);
                    break;
                case "Все":
                default:
                    filtered.add(budget);
                    break;
            }
        }

        // Сортируем: сначала превышенные, потом активные, потом остальные
        Collections.sort(filtered, (b1, b2) -> {
            if (b1.isExceeded() && !b2.isExceeded()) return -1;
            if (!b1.isExceeded() && b2.isExceeded()) return 1;
            if (b1.isActive() && !b2.isActive()) return -1;
            if (!b1.isActive() && b2.isActive()) return 1;
            return Double.compare(b2.getUsagePercentage(), b1.getUsagePercentage());
        });

        displayBudgets(filtered);
    }

    private void displayBudgets(List<Budget> budgets) {
        budgetsContainer.removeAllViews();

        if (budgets.isEmpty()) {
            showEmptyState();
            return;
        }

        for (Budget budget : budgets) {
            addBudgetView(budget);
        }
    }

    private void showEmptyState() {
        TextView emptyText = new TextView(getContext());
        emptyText.setText("Нет бюджетов для отображения\n\nНажмите 'Добавить бюджет' чтобы создать первый");
        emptyText.setTextSize(16);
        emptyText.setTextColor(Color.GRAY);
        emptyText.setGravity(android.view.Gravity.CENTER);
        emptyText.setPadding(0, 100, 0, 100);
        budgetsContainer.addView(emptyText);
    }

    private void addBudgetView(final Budget budget) {
        if (getContext() == null) return;

        LinearLayout budgetLayout = new LinearLayout(getContext());
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(16, 8, 16, 8);
        budgetLayout.setLayoutParams(layoutParams);
        budgetLayout.setOrientation(LinearLayout.VERTICAL);
        budgetLayout.setPadding(20, 16, 20, 16);
        budgetLayout.setBackgroundResource(R.drawable.card_background);
        budgetLayout.setClickable(true);

        // Верхняя строка: категория и статус
        LinearLayout topRow = new LinearLayout(getContext());
        topRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        topRow.setOrientation(LinearLayout.HORIZONTAL);

        TextView categoryView = new TextView(getContext());
        categoryView.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        categoryView.setText(budget.getCategoryIcon() + " " + budget.getCategoryWithoutIcon());
        categoryView.setTextSize(16);
        categoryView.setTypeface(null, android.graphics.Typeface.BOLD);
        categoryView.setTextColor(Color.BLACK);

        TextView statusView = new TextView(getContext());
        statusView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        statusView.setText(budget.getStatus());
        statusView.setTextSize(12);
        statusView.setTextColor(budget.getStatusColor());

        topRow.addView(categoryView);
        topRow.addView(statusView);

        // Вторая строка: суммы
        LinearLayout amountRow = new LinearLayout(getContext());
        amountRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        amountRow.setPadding(0, 8, 0, 8);

        TextView spentView = new TextView(getContext());
        spentView.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        spentView.setText(String.format(Locale.getDefault(), "%.2f руб.", budget.getSpent()));
        spentView.setTextSize(14);
        spentView.setTextColor(budget.isExceeded() ?
                Color.parseColor("#F44336") : Color.parseColor("#333333"));

        TextView separatorView = new TextView(getContext());
        separatorView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        separatorView.setText(" / ");
        separatorView.setTextSize(14);
        separatorView.setTextColor(Color.GRAY);

        TextView limitView = new TextView(getContext());
        limitView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        limitView.setText(String.format(Locale.getDefault(), "%.2f руб.", budget.getAmount()));
        limitView.setTextSize(14);
        limitView.setTextColor(Color.GRAY);

        TextView percentageView = new TextView(getContext());
        percentageView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        percentageView.setText(String.format(Locale.getDefault(), " (%.1f%%)", budget.getUsagePercentage()));
        percentageView.setTextSize(12);
        percentageView.setTextColor(budget.getStatusColor());

        amountRow.addView(spentView);
        amountRow.addView(separatorView);
        amountRow.addView(limitView);
        amountRow.addView(percentageView);

        // Прогресс-бар
        LinearLayout progressContainer = new LinearLayout(getContext());
        progressContainer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                12
        ));
        progressContainer.setOrientation(LinearLayout.HORIZONTAL);
        progressContainer.setBackgroundColor(Color.parseColor("#E0E0E0"));

        View progressBar = new View(getContext());
        int progressWidth = (int) Math.min(100, budget.getUsagePercentage());
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                progressWidth
        );
        progressBar.setLayoutParams(progressParams);
        progressBar.setBackgroundColor(budget.getStatusColor());

        progressContainer.addView(progressBar);

        // Нижняя строка: период и остаток
        LinearLayout bottomRow = new LinearLayout(getContext());
        bottomRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        bottomRow.setPadding(0, 8, 0, 0);

        TextView periodView = new TextView(getContext());
        periodView.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        periodView.setText("📅 " + budget.getPeriodName() + "\n" + budget.getFormattedDates());
        periodView.setTextSize(11);
        periodView.setTextColor(Color.GRAY);
        periodView.setLineSpacing(0, 1.0f);

        TextView remainingView = new TextView(getContext());
        remainingView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        remainingView.setText("Осталось: " + String.format(Locale.getDefault(), "%.2f руб.", budget.getRemaining()));
        remainingView.setTextSize(12);
        remainingView.setTypeface(null, android.graphics.Typeface.BOLD);
        remainingView.setTextColor(budget.getRemaining() > 0 ?
                Color.parseColor("#4CAF50") : Color.parseColor("#F44336"));

        bottomRow.addView(periodView);
        bottomRow.addView(remainingView);

        budgetLayout.addView(topRow);
        budgetLayout.addView(amountRow);
        budgetLayout.addView(progressContainer);
        budgetLayout.addView(bottomRow);

        // Обработчик клика
        budgetLayout.setOnClickListener(v -> showBudgetActionsDialog(budget));

        budgetsContainer.addView(budgetLayout);
    }

    private void showBudgetActionsDialog(final Budget budget) {
        if (getContext() == null) return;

        String[] options = {
                "📊 Информация",
                "✏️ Редактировать",
                "🔄 Сбросить траты",
                "⏸️ " + (budget.isActive() ? "Деактивировать" : "Активировать"),
                "❌ Удалить"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Действия с бюджетом");
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    showBudgetInfo(budget);
                    break;
                case 1:
                    showEditBudgetDialog(budget);
                    break;
                case 2:
                    confirmResetBudget(budget);
                    break;
                case 3:
                    toggleBudgetActive(budget);
                    break;
                case 4:
                    confirmDeleteBudget(budget);
                    break;
            }
        });

        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void confirmResetBudget(final Budget budget) {
        new AlertDialog.Builder(getContext())
                .setTitle("🔄 Сбросить траты")
                .setMessage("Вы уверены, что хотите сбросить потраченную сумму?\n\n" +
                        "Текущие траты: " + String.format(Locale.getDefault(), "%.2f руб.", budget.getSpent()))
                .setPositiveButton("Сбросить", (dialog, which) -> {
                    dataManager.resetBudgetSpent(budget.getId());
                    loadBudgets();
                    Toast.makeText(getContext(), "Траты сброшены", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void confirmDeleteBudget(final Budget budget) {
        new AlertDialog.Builder(getContext())
                .setTitle("❌ Удалить бюджет")
                .setMessage("Вы уверены, что хотите удалить бюджет для категории \"" +
                        budget.getCategoryWithoutIcon() + "\"?\n\n" +
                        "Лимит: " + String.format(Locale.getDefault(), "%.2f руб.", budget.getAmount()) + "\n" +
                        "Потрачено: " + String.format(Locale.getDefault(), "%.2f руб.", budget.getSpent()) + "\n" +
                        "Период: " + budget.getFormattedDates() + "\n\n" +
                        "Это действие нельзя отменить!")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    dataManager.deleteBudgetById(budget.getId());
                    loadBudgets();
                    Toast.makeText(getContext(), "Бюджет удален", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void toggleBudgetActive(Budget budget) {
        budget.setActive(!budget.isActive());
        dataManager.updateBudget(budget);
        loadBudgets();
        Toast.makeText(getContext(),
                budget.isActive() ? "Бюджет активирован" : "Бюджет деактивирован",
                Toast.LENGTH_SHORT).show();
    }

    private void showBudgetInfo(Budget budget) {
        String info = String.format(Locale.getDefault(),
                "📊 Категория: %s\n" +
                        "💰 Лимит: %.2f руб.\n" +
                        "💸 Потрачено: %.2f руб.\n" +
                        "✅ Осталось: %.2f руб.\n" +
                        "📈 Использовано: %.1f%%\n\n" +
                        "📅 Период: %s\n" +
                        "📅 Даты: %s\n\n" +
                        "📌 Статус: %s\n" +
                        "⏰ Активен: %s\n\n" +
                        "🆔 ID: %s",
                budget.getCategoryWithoutIcon(),
                budget.getAmount(),
                budget.getSpent(),
                budget.getRemaining(),
                budget.getUsagePercentage(),
                budget.getPeriodName(),
                budget.getFormattedDates(),
                budget.getStatus(),
                budget.isActive() ? "Да" : "Нет",
                budget.getId().substring(0, 8) + "...");

        new AlertDialog.Builder(getContext())
                .setTitle("📊 Информация о бюджете")
                .setMessage(info)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showEditBudgetDialog(final Budget budget) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("✏️ Редактировать бюджет");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_budget, null);

        final EditText amountInput = dialogView.findViewById(R.id.amountInput);
        final Spinner periodSpinner = dialogView.findViewById(R.id.periodSpinner);
        final LinearLayout customDateLayout = dialogView.findViewById(R.id.customDateLayout);
        final TextView startDateText = dialogView.findViewById(R.id.startDateText);
        final TextView endDateText = dialogView.findViewById(R.id.endDateText);
        final LinearLayout startDateLayout = dialogView.findViewById(R.id.startDateLayout);
        final LinearLayout endDateLayout = dialogView.findViewById(R.id.endDateLayout);

        // Скрываем выбор категории при редактировании
        View categorySpinner = dialogView.findViewById(R.id.categorySpinner);
        if (categorySpinner != null) {
            categorySpinner.setVisibility(View.GONE);
        }

        // Устанавливаем текущие значения
        amountInput.setText(String.valueOf(budget.getAmount()));

        // Настраиваем спиннер периодов
        String[] periods = {"Неделя", "Месяц", "Год", "Произвольный период"};
        ArrayAdapter<String> periodAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, periods);
        periodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        periodSpinner.setAdapter(periodAdapter);

        // Устанавливаем текущий период
        int periodPosition = 1; // Месяц по умолчанию
        switch (budget.getPeriod()) {
            case "неделя":
                periodPosition = 0;
                break;
            case "месяц":
                periodPosition = 1;
                break;
            case "год":
                periodPosition = 2;
                break;
            case "произвольный":
                periodPosition = 3;
                break;
        }
        periodSpinner.setSelection(periodPosition);
        customDateLayout.setVisibility(periodPosition == 3 ? View.VISIBLE : View.GONE);

        // Устанавливаем даты
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        final Date[] startDate = {budget.getStartDate()};
        final Date[] endDate = {budget.getEndDate()};
        startDateText.setText(sdf.format(startDate[0]));
        endDateText.setText(sdf.format(endDate[0]));

        startDateLayout.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            cal.setTime(startDate[0]);

            DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                    (view, year, month, dayOfMonth) -> {
                        Calendar selected = Calendar.getInstance();
                        selected.set(year, month, dayOfMonth);
                        startDate[0] = selected.getTime();
                        startDateText.setText(sdf.format(startDate[0]));
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        endDateLayout.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            cal.setTime(endDate[0]);

            DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                    (view, year, month, dayOfMonth) -> {
                        Calendar selected = Calendar.getInstance();
                        selected.set(year, month, dayOfMonth);
                        endDate[0] = selected.getTime();
                        endDateText.setText(sdf.format(endDate[0]));
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        periodSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                customDateLayout.setVisibility(position == 3 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        builder.setView(dialogView);
        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            try {
                String amountStr = amountInput.getText().toString().trim();
                if (amountStr.isEmpty()) {
                    Toast.makeText(getContext(), "Введите сумму", Toast.LENGTH_SHORT).show();
                    return;
                }

                double newAmount = Double.parseDouble(amountStr);
                if (newAmount <= 0) {
                    Toast.makeText(getContext(), "Сумма должна быть больше 0", Toast.LENGTH_SHORT).show();
                    return;
                }

                budget.setAmount(newAmount);

                String selectedPeriod = (String) periodSpinner.getSelectedItem();
                if (selectedPeriod.equals("Произвольный период")) {
                    budget.setPeriod("произвольный");
                    budget.setStartDate(startDate[0]);
                    budget.setEndDate(endDate[0]);
                } else {
                    switch (selectedPeriod) {
                        case "Неделя":
                            budget.setPeriod("неделя");
                            break;
                        case "Год":
                            budget.setPeriod("год");
                            break;
                        case "Месяц":
                            budget.setPeriod("месяц");
                            break;
                    }
                }

                dataManager.updateBudget(budget);
                loadBudgets();
                Toast.makeText(getContext(), "Бюджет обновлен", Toast.LENGTH_SHORT).show();

            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Некорректная сумма", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void showAddBudgetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("➕ Создать бюджет");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_budget, null);

        final Spinner categorySpinner = dialogView.findViewById(R.id.categorySpinner);
        final EditText amountInput = dialogView.findViewById(R.id.amountInput);
        final Spinner periodSpinner = dialogView.findViewById(R.id.periodSpinner);
        final LinearLayout customDateLayout = dialogView.findViewById(R.id.customDateLayout);
        final TextView startDateText = dialogView.findViewById(R.id.startDateText);
        final TextView endDateText = dialogView.findViewById(R.id.endDateText);
        final LinearLayout startDateLayout = dialogView.findViewById(R.id.startDateLayout);
        final LinearLayout endDateLayout = dialogView.findViewById(R.id.endDateLayout);

        // Настраиваем спиннер категорий расходов
        CategoryManager categoryManager = mainActivity.getCategoryManager();
        List<String> expenseCategories = categoryManager.getExpenseCategories();
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, expenseCategories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        // Настраиваем спиннер периодов
        String[] periods = {"Неделя", "Месяц", "Год", "Произвольный период"};
        ArrayAdapter<String> periodAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, periods);
        periodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        periodSpinner.setAdapter(periodAdapter);

        // Устанавливаем текущие даты
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        final Date[] startDate = {new Date()};
        final Date[] endDate = {new Date()};
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, 1);
        endDate[0] = calendar.getTime();

        startDateText.setText(sdf.format(startDate[0]));
        endDateText.setText(sdf.format(endDate[0]));

        startDateLayout.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            cal.setTime(startDate[0]);

            DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                    (view, year, month, dayOfMonth) -> {
                        Calendar selected = Calendar.getInstance();
                        selected.set(year, month, dayOfMonth);
                        startDate[0] = selected.getTime();
                        startDateText.setText(sdf.format(startDate[0]));
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        endDateLayout.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            cal.setTime(endDate[0]);

            DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                    (view, year, month, dayOfMonth) -> {
                        Calendar selected = Calendar.getInstance();
                        selected.set(year, month, dayOfMonth);
                        endDate[0] = selected.getTime();
                        endDateText.setText(sdf.format(endDate[0]));
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        periodSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                customDateLayout.setVisibility(position == 3 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        builder.setView(dialogView);
        builder.setPositiveButton("Создать", (dialog, which) -> {
            try {
                String category = (String) categorySpinner.getSelectedItem();
                String amountStr = amountInput.getText().toString().trim();
                String period = (String) periodSpinner.getSelectedItem();

                if (category == null || category.isEmpty()) {
                    Toast.makeText(getContext(), "Выберите категорию", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (amountStr.isEmpty()) {
                    Toast.makeText(getContext(), "Введите сумму бюджета", Toast.LENGTH_SHORT).show();
                    return;
                }

                double amount = Double.parseDouble(amountStr);
                if (amount <= 0) {
                    Toast.makeText(getContext(), "Сумма должна быть больше 0", Toast.LENGTH_SHORT).show();
                    return;
                }

                Budget newBudget;

                if (period.equals("Произвольный период")) {
                    newBudget = new Budget(category, amount, startDate[0], endDate[0]);
                } else {
                    String budgetPeriod;
                    switch (period) {
                        case "Неделя":
                            budgetPeriod = "неделя";
                            break;
                        case "Год":
                            budgetPeriod = "год";
                            break;
                        case "Месяц":
                        default:
                            budgetPeriod = "месяц";
                            break;
                    }
                    newBudget = new Budget(category, amount, budgetPeriod);
                }

                dataManager.addBudget(newBudget);
                loadBudgets();
                Toast.makeText(getContext(), "Бюджет создан", Toast.LENGTH_SHORT).show();

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