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

public class GoalsFragment extends Fragment {

    private LinearLayout goalsContainer;
    private Button addGoalButton;
    private TextView totalGoalsTextView;
    private TextView completedGoalsTextView;
    private TextView overdueGoalsTextView;
    private TextView totalTargetTextView;
    private TextView totalCurrentTextView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Spinner filterSpinner;

    private MainActivity mainActivity;
    private DataManager dataManager;
    private List<FinancialGoal> allGoals = new ArrayList<>();
    private String currentFilter = "Все";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_goals, container, false);

        mainActivity = (MainActivity) getActivity();
        dataManager = mainActivity.getDataManager();

        initViews(view);
        setupListeners();
        loadGoals();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadGoals();
    }

    private void initViews(View view) {
        goalsContainer = view.findViewById(R.id.goalsContainer);
        addGoalButton = view.findViewById(R.id.addGoalButton);
        totalGoalsTextView = view.findViewById(R.id.totalGoalsTextView);
        completedGoalsTextView = view.findViewById(R.id.completedGoalsTextView);
        overdueGoalsTextView = view.findViewById(R.id.overdueGoalsTextView);
        totalTargetTextView = view.findViewById(R.id.totalTargetTextView);
        totalCurrentTextView = view.findViewById(R.id.totalCurrentTextView);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        filterSpinner = view.findViewById(R.id.filterSpinner);

        // Настройка фильтра
        String[] filters = {"Все", "Активные", "Выполненные", "Просроченные", "По приоритету"};
        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, filters);
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterSpinner.setAdapter(filterAdapter);
    }

    private void setupListeners() {
        addGoalButton.setOnClickListener(v -> showAddGoalDialog());

        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadGoals();
            swipeRefreshLayout.setRefreshing(false);
        });

        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentFilter = (String) parent.getItemAtPosition(position);
                filterGoals();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    public void loadGoals() {
        if (mainActivity == null || goalsContainer == null) return;

        allGoals = dataManager.loadGoals();
        if (allGoals == null) {
            allGoals = new ArrayList<>();
        }

        updateStatistics();
        filterGoals();
    }

    private void updateStatistics() {
        int total = allGoals.size();
        int completed = 0;
        int overdue = 0;
        double totalTarget = 0;
        double totalCurrent = 0;

        for (FinancialGoal goal : allGoals) {
            if (goal.isCompleted()) completed++;
            if (goal.isOverdue() && !goal.isCompleted()) overdue++;
            totalTarget += goal.getTargetAmount();
            totalCurrent += goal.getCurrentAmount();
        }

        totalGoalsTextView.setText(String.valueOf(total));
        completedGoalsTextView.setText(String.valueOf(completed));
        overdueGoalsTextView.setText(String.valueOf(overdue));
        totalTargetTextView.setText(String.format(Locale.getDefault(), "%.0f руб.", totalTarget));
        totalCurrentTextView.setText(String.format(Locale.getDefault(), "%.0f руб.", totalCurrent));
    }

    private void filterGoals() {
        List<FinancialGoal> filtered = new ArrayList<>();

        for (FinancialGoal goal : allGoals) {
            switch (currentFilter) {
                case "Активные":
                    if (!goal.isCompleted()) filtered.add(goal);
                    break;
                case "Выполненные":
                    if (goal.isCompleted()) filtered.add(goal);
                    break;
                case "Просроченные":
                    if (goal.isOverdue() && !goal.isCompleted()) filtered.add(goal);
                    break;
                case "Все":
                default:
                    filtered.add(goal);
                    break;
            }
        }

        filtered.sort((g1, g2) -> {
            if (currentFilter.equals("По приоритету")) {
                // Сортировка по приоритету
                int priority1 = getPriorityLevel(g1.getPriority());
                int priority2 = getPriorityLevel(g2.getPriority());
                return Integer.compare(priority2, priority1);
            }

            if (!g1.isCompleted() && g2.isCompleted()) return -1;
            if (g1.isCompleted() && !g2.isCompleted()) return 1;

            if (g1.isOverdue() && !g2.isOverdue()) return -1;
            if (!g1.isOverdue() && g2.isOverdue()) return 1;

            return Double.compare(g2.getProgressPercentage(), g1.getProgressPercentage());
        });

        displayGoals(filtered);
    }

    private int getPriorityLevel(String priority) {
        switch (priority) {
            case "Критический": return 4;
            case "Высокий": return 3;
            case "Средний": return 2;
            case "Низкий": return 1;
            default: return 0;
        }
    }

    private void displayGoals(List<FinancialGoal> goals) {
        goalsContainer.removeAllViews();

        if (goals.isEmpty()) {
            showEmptyState();
            return;
        }

        for (FinancialGoal goal : goals) {
            addGoalView(goal);
        }
    }

    private void showEmptyState() {
        TextView emptyText = new TextView(getContext());
        emptyText.setText("Нет финансовых целей\n\nНажмите 'Добавить цель' чтобы создать первую");
        emptyText.setTextSize(16);
        emptyText.setTextColor(Color.GRAY);
        emptyText.setGravity(android.view.Gravity.CENTER);
        emptyText.setPadding(0, 100, 0, 100);
        goalsContainer.addView(emptyText);
    }

    private void addGoalView(final FinancialGoal goal) {
        if (getContext() == null) return;

        LinearLayout goalLayout = new LinearLayout(getContext());
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(16, 8, 16, 8);
        goalLayout.setLayoutParams(layoutParams);
        goalLayout.setOrientation(LinearLayout.VERTICAL);
        goalLayout.setPadding(20, 16, 20, 16);
        goalLayout.setBackgroundResource(R.drawable.card_background);
        goalLayout.setClickable(true);

        // Верхняя строка: название и приоритет
        LinearLayout topRow = new LinearLayout(getContext());
        topRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        topRow.setOrientation(LinearLayout.HORIZONTAL);

        TextView nameView = new TextView(getContext());
        nameView.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        nameView.setText(goal.getPriorityIcon() + " " + goal.getName());
        nameView.setTextSize(16);
        nameView.setTypeface(null, android.graphics.Typeface.BOLD);
        nameView.setTextColor(goal.isCompleted() ? Color.GRAY : Color.BLACK);

        TextView priorityView = new TextView(getContext());
        priorityView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        priorityView.setText(goal.getPriority());
        priorityView.setTextSize(12);
        priorityView.setTextColor(goal.getPriorityColor());

        topRow.addView(nameView);
        topRow.addView(priorityView);

        // Вторая строка: суммы
        LinearLayout amountRow = new LinearLayout(getContext());
        amountRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        amountRow.setPadding(0, 8, 0, 8);

        TextView currentView = new TextView(getContext());
        currentView.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        currentView.setText(String.format(Locale.getDefault(), "%.2f руб.", goal.getCurrentAmount()));
        currentView.setTextSize(14);
        currentView.setTextColor(goal.isCompleted() ?
                Color.parseColor("#4CAF50") : Color.parseColor("#333333"));

        TextView separatorView = new TextView(getContext());
        separatorView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        separatorView.setText(" / ");
        separatorView.setTextSize(14);
        separatorView.setTextColor(Color.GRAY);

        TextView targetView = new TextView(getContext());
        targetView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        targetView.setText(String.format(Locale.getDefault(), "%.2f руб.", goal.getTargetAmount()));
        targetView.setTextSize(14);
        targetView.setTextColor(Color.GRAY);

        TextView percentageView = new TextView(getContext());
        percentageView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        percentageView.setText(String.format(Locale.getDefault(), " (%.1f%%)", goal.getProgressPercentage()));
        percentageView.setTextSize(12);
        percentageView.setTextColor(goal.getPriorityColor());

        amountRow.addView(currentView);
        amountRow.addView(separatorView);
        amountRow.addView(targetView);
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
        int progressWidth = (int) Math.min(100, goal.getProgressPercentage());
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                progressWidth
        );
        progressBar.setLayoutParams(progressParams);
        progressBar.setBackgroundColor(goal.getPriorityColor());

        progressContainer.addView(progressBar);

        // Нижняя строка: срок и статус
        LinearLayout bottomRow = new LinearLayout(getContext());
        bottomRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        bottomRow.setPadding(0, 8, 0, 0);

        TextView deadlineView = new TextView(getContext());
        deadlineView.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        deadlineView.setText("📅 " + goal.getFormattedDeadline() + " • " + goal.getDaysText());
        deadlineView.setTextSize(11);
        deadlineView.setTextColor(goal.isOverdue() ?
                Color.parseColor("#F44336") : Color.GRAY);
        deadlineView.setLineSpacing(0, 1.0f);

        TextView statusView = new TextView(getContext());
        statusView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        statusView.setText(goal.getProgressStatus());
        statusView.setTextSize(12);
        statusView.setTextColor(goal.getPriorityColor());

        bottomRow.addView(deadlineView);
        bottomRow.addView(statusView);

        goalLayout.addView(topRow);
        goalLayout.addView(amountRow);
        goalLayout.addView(progressContainer);
        goalLayout.addView(bottomRow);

        goalLayout.setOnClickListener(v -> showGoalActionsDialog(goal));

        goalsContainer.addView(goalLayout);
    }

    private void showGoalActionsDialog(final FinancialGoal goal) {
        if (getContext() == null) return;

        List<String> optionsList = new ArrayList<>();
        optionsList.add("📊 Информация");
        optionsList.add("✏️ Редактировать");
        optionsList.add("💰 Добавить средства");

        if (!goal.isCompleted()) {
            optionsList.add("✅ Отметить выполненной");
        }

        optionsList.add("❌ Удалить");

        String[] options = optionsList.toArray(new String[0]);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Действия с целью");
        builder.setItems(options, (dialog, which) -> {
            String selected = options[which];

            if (selected.equals("📊 Информация")) {
                showGoalInfo(goal);
            } else if (selected.equals("✏️ Редактировать")) {
                showEditGoalDialog(goal);
            } else if (selected.equals("💰 Добавить средства")) {
                showAddMoneyDialog(goal);
            } else if (selected.equals("✅ Отметить выполненной")) {
                confirmCompleteGoal(goal);
            } else if (selected.equals("❌ Удалить")) {
                confirmDeleteGoal(goal);
            }
        });

        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void showGoalInfo(FinancialGoal goal) {
        String info = String.format(Locale.getDefault(),
                "🎯 Название: %s\n" +
                        "💰 Целевая сумма: %.2f руб.\n" +
                        "💵 Текущая сумма: %.2f руб.\n" +
                        "✅ Осталось: %.2f руб.\n" +
                        "📈 Прогресс: %.1f%%\n\n" +
                        "📅 Дата создания: %s\n" +
                        "📅 Срок: %s\n" +
                        "⏰ Осталось дней: %d\n\n" +
                        "🎚️ Приоритет: %s\n" +
                        "📌 Статус: %s\n" +
                        "📝 Заметки: %s\n\n" +
                        "🆔 ID: %s",
                goal.getName(),
                goal.getTargetAmount(),
                goal.getCurrentAmount(),
                goal.getRemainingAmount(),
                goal.getProgressPercentage(),
                goal.getFormattedCreatedDate(),
                goal.getFormattedDeadline(),
                goal.getDaysRemaining(),
                goal.getPriority(),
                goal.getProgressStatus(),
                goal.getNotes().isEmpty() ? "Нет" : goal.getNotes(),
                goal.getId().substring(0, 8) + "...");

        new AlertDialog.Builder(getContext())
                .setTitle("📊 Информация о цели")
                .setMessage(info)
                .setPositiveButton("OK", null)
                .show();
    }

    private void confirmCompleteGoal(final FinancialGoal goal) {
        new AlertDialog.Builder(getContext())
                .setTitle("✅ Отметить выполненной")
                .setMessage("Вы уверены, что хотите отметить цель \"" + goal.getName() + "\" как выполненную?")
                .setPositiveButton("Да", (dialog, which) -> {
                    dataManager.completeGoal(goal.getId());
                    loadGoals();
                    Toast.makeText(getContext(), "Цель отмечена как выполненная! Поздравляем!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }


    private void confirmDeleteGoal(final FinancialGoal goal) {
        new AlertDialog.Builder(getContext())
                .setTitle("❌ Удалить цель")
                .setMessage("Вы уверены, что хотите удалить цель \"" + goal.getName() + "\"?\n\n" +
                        "Целевая сумма: " + String.format(Locale.getDefault(), "%.2f руб.", goal.getTargetAmount()) + "\n" +
                        "Накоплено: " + String.format(Locale.getDefault(), "%.2f руб.", goal.getCurrentAmount()) + "\n" +
                        "Прогресс: " + String.format(Locale.getDefault(), "%.1f%%", goal.getProgressPercentage()) + "\n\n" +
                        "Это действие нельзя отменить!")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    dataManager.deleteGoalById(goal.getId());
                    loadGoals();
                    Toast.makeText(getContext(), "Цель удалена", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showAddMoneyDialog(final FinancialGoal goal) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("💰 Добавить средства");
        builder.setMessage("Цель: " + goal.getName() + "\n" +
                "Текущая сумма: " + String.format(Locale.getDefault(), "%.2f руб.", goal.getCurrentAmount()) + "\n" +
                "Осталось: " + String.format(Locale.getDefault(), "%.2f руб.", goal.getRemainingAmount()));

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_money, null);
        final EditText amountInput = dialogView.findViewById(R.id.amountInput);

        builder.setView(dialogView);

        builder.setPositiveButton("Добавить", (dialog, which) -> {
            try {
                String amountStr = amountInput.getText().toString().trim();
                if (amountStr.isEmpty()) {
                    Toast.makeText(getContext(), "Введите сумму", Toast.LENGTH_SHORT).show();
                    return;
                }

                double amount = Double.parseDouble(amountStr);
                if (amount <= 0) {
                    Toast.makeText(getContext(), "Сумма должна быть больше 0", Toast.LENGTH_SHORT).show();
                    return;
                }

                dataManager.addToGoal(goal.getId(), amount);
                loadGoals();

                if (goal.getCurrentAmount() + amount >= goal.getTargetAmount()) {
                    Toast.makeText(getContext(),
                            "🎉 Поздравляем! Цель достигнута!",
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getContext(),
                            String.format(Locale.getDefault(), "Добавлено %.2f руб.", amount),
                            Toast.LENGTH_SHORT).show();
                }

            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Некорректная сумма", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void showEditGoalDialog(final FinancialGoal goal) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("✏️ Редактировать цель");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_goal, null);

        final EditText nameInput = dialogView.findViewById(R.id.goalNameInput);
        final EditText targetAmountInput = dialogView.findViewById(R.id.targetAmountInput);
        final EditText currentAmountInput = dialogView.findViewById(R.id.currentAmountInput);
        final Spinner prioritySpinner = dialogView.findViewById(R.id.prioritySpinner);
        final EditText notesInput = dialogView.findViewById(R.id.notesInput);
        final LinearLayout deadlineLayout = dialogView.findViewById(R.id.deadlineLayout);
        final TextView deadlineTextView = dialogView.findViewById(R.id.deadlineTextView);

        // Заполняем текущими значениями
        nameInput.setText(goal.getName());
        targetAmountInput.setText(String.valueOf(goal.getTargetAmount()));
        currentAmountInput.setText(String.valueOf(goal.getCurrentAmount()));
        notesInput.setText(goal.getNotes());

        // Устанавливаем дату
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        final Date[] selectedDeadline = {goal.getDeadline()};
        deadlineTextView.setText(sdf.format(selectedDeadline[0]));

        // Настраиваем спиннер приоритета
        String[] priorities = {"Низкий", "Средний", "Высокий", "Критический"};
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, priorities);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        prioritySpinner.setAdapter(priorityAdapter);

        // Устанавливаем текущий приоритет
        for (int i = 0; i < priorities.length; i++) {
            if (priorities[i].equals(goal.getPriority())) {
                prioritySpinner.setSelection(i);
                break;
            }
        }

        // Обработчик выбора даты
        deadlineLayout.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            cal.setTime(selectedDeadline[0]);

            DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
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

        builder.setView(dialogView);
        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            try {
                String name = nameInput.getText().toString().trim();
                String targetStr = targetAmountInput.getText().toString().trim();
                String currentStr = currentAmountInput.getText().toString().trim();
                String priority = (String) prioritySpinner.getSelectedItem();
                String notes = notesInput.getText().toString().trim();

                if (name.isEmpty()) {
                    Toast.makeText(getContext(), "Введите название цели", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (targetStr.isEmpty()) {
                    Toast.makeText(getContext(), "Введите целевую сумму", Toast.LENGTH_SHORT).show();
                    return;
                }

                double targetAmount = Double.parseDouble(targetStr);
                if (targetAmount <= 0) {
                    Toast.makeText(getContext(), "Целевая сумма должна быть больше 0", Toast.LENGTH_SHORT).show();
                    return;
                }

                double currentAmount = 0.0;
                if (!currentStr.isEmpty()) {
                    currentAmount = Double.parseDouble(currentStr);
                    if (currentAmount < 0) {
                        Toast.makeText(getContext(), "Текущая сумма не может быть отрицательной", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (currentAmount > targetAmount) {
                        Toast.makeText(getContext(), "Текущая сумма не может превышать целевую", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                goal.setName(name);
                goal.setTargetAmount(targetAmount);
                goal.setCurrentAmount(currentAmount);
                goal.setDeadline(selectedDeadline[0]);
                goal.setPriority(priority);
                goal.setNotes(notes);
                goal.setCompleted(currentAmount >= targetAmount);

                dataManager.updateGoal(goal);
                loadGoals();
                Toast.makeText(getContext(), "Цель обновлена", Toast.LENGTH_SHORT).show();

            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Некорректная сумма", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void showAddGoalDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("🎯 Добавить финансовую цель");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_goal, null);

        final EditText nameInput = dialogView.findViewById(R.id.goalNameInput);
        final EditText targetAmountInput = dialogView.findViewById(R.id.targetAmountInput);
        final EditText currentAmountInput = dialogView.findViewById(R.id.currentAmountInput);
        final Spinner prioritySpinner = dialogView.findViewById(R.id.prioritySpinner);
        final EditText notesInput = dialogView.findViewById(R.id.notesInput);
        final LinearLayout deadlineLayout = dialogView.findViewById(R.id.deadlineLayout);
        final TextView deadlineTextView = dialogView.findViewById(R.id.deadlineTextView);

        // Устанавливаем текущую дату + 30 дней
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 30);
        final Date[] selectedDeadline = {calendar.getTime()};
        deadlineTextView.setText(sdf.format(selectedDeadline[0]));

        // Обработчик выбора даты
        deadlineLayout.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            cal.setTime(selectedDeadline[0]);

            DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
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

        // Настраиваем спиннер приоритета
        String[] priorities = {"Низкий", "Средний", "Высокий", "Критический"};
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, priorities);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        prioritySpinner.setAdapter(priorityAdapter);

        builder.setView(dialogView);
        builder.setPositiveButton("Добавить", (dialog, which) -> {
            try {
                String name = nameInput.getText().toString().trim();
                String targetStr = targetAmountInput.getText().toString().trim();
                String currentStr = currentAmountInput.getText().toString().trim();
                String priority = (String) prioritySpinner.getSelectedItem();
                String notes = notesInput.getText().toString().trim();

                if (name.isEmpty()) {
                    Toast.makeText(getContext(), "Введите название цели", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (targetStr.isEmpty()) {
                    Toast.makeText(getContext(), "Введите целевую сумму", Toast.LENGTH_SHORT).show();
                    return;
                }

                double targetAmount = Double.parseDouble(targetStr);
                if (targetAmount <= 0) {
                    Toast.makeText(getContext(), "Целевая сумма должна быть больше 0", Toast.LENGTH_SHORT).show();
                    return;
                }

                double currentAmount = 0.0;
                if (!currentStr.isEmpty()) {
                    currentAmount = Double.parseDouble(currentStr);
                    if (currentAmount < 0) {
                        Toast.makeText(getContext(), "Текущая сумма не может быть отрицательной", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (currentAmount > targetAmount) {
                        Toast.makeText(getContext(), "Текущая сумма не может превышать целевую", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                FinancialGoal newGoal = new FinancialGoal(
                        name,
                        targetAmount,
                        currentAmount,
                        selectedDeadline[0],
                        priority,
                        notes
                );

                dataManager.addGoal(newGoal);
                loadGoals();
                Toast.makeText(getContext(), "Цель добавлена", Toast.LENGTH_SHORT).show();

            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Некорректная сумма", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Отмена", null);
        builder.show();
    }
}