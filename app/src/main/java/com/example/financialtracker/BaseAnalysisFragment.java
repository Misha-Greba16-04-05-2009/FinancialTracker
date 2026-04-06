package com.example.financialtracker;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public abstract class BaseAnalysisFragment extends Fragment implements AnalysisBaseFragment {

    protected LinearLayout categoriesContainer;
    protected Spinner yearSpinner;
    protected Spinner monthSpinner;
    protected TextView summaryTextView;
    protected TextView summaryTitle;
    protected TextView emptyTextView;
    protected TextView titleTextView;
    protected TextView addCategoryText;
    protected androidx.cardview.widget.CardView addCategoryButton;

    protected MainActivity mainActivity;
    protected int selectedYear = -1;
    protected int selectedMonth = 0;

    protected final String[] monthNames = {
            "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
            "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
    };

    protected abstract boolean isIncomeAnalysis();
    protected abstract String getTitleText();
    protected abstract String getEmptyMessage();
    protected abstract int getPrimaryColor();
    protected abstract String getCategoryTypeText();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_analysis_base, container, false);

        mainActivity = (MainActivity) getActivity();

        initViews(view);
        setupSpinners();
        setupListeners();

        // Устанавливаем текущий год и месяц
        Calendar calendar = Calendar.getInstance();
        selectedYear = calendar.get(Calendar.YEAR);
        selectedMonth = calendar.get(Calendar.MONTH) + 1;

        updateAnalysis();

        return view;
    }

    private void initViews(View view) {
        categoriesContainer = view.findViewById(R.id.categoriesContainer);
        yearSpinner = view.findViewById(R.id.yearSpinner);
        monthSpinner = view.findViewById(R.id.monthSpinner);
        summaryTextView = view.findViewById(R.id.summaryTextView);
        summaryTitle = view.findViewById(R.id.summaryTitle);
        emptyTextView = view.findViewById(R.id.emptyTextView);
        titleTextView = view.findViewById(R.id.titleTextView);
        addCategoryText = view.findViewById(R.id.addCategoryText);
        addCategoryButton = view.findViewById(R.id.addCategoryButton);

        // Устанавливаем заголовок
        titleTextView.setText(getTitleText());
        titleTextView.setTextColor(getPrimaryColor());

        // Настраиваем кнопку добавления категории
        addCategoryText.setText(getCategoryTypeText());
        addCategoryButton.setCardBackgroundColor(getPrimaryColor());

        // Настраиваем заголовок статистики
        summaryTitle.setText("📊 " + (isIncomeAnalysis() ? "Статистика доходов" : "Статистика расходов"));
    }

    protected void setupSpinners() {
        if (mainActivity == null) return;

        // Получаем список годов от 2000 до текущего года + 5 лет вперед
        List<String> yearOptions = generateYearOptions();

        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, yearOptions);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        yearSpinner.setAdapter(yearAdapter);

        // Устанавливаем текущий год
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        String currentYearStr = String.valueOf(currentYear);
        int position = yearOptions.indexOf(currentYearStr);
        if (position != -1) {
            yearSpinner.setSelection(position);
        }

        // Настраиваем спиннер месяцев
        List<String> monthOptions = new ArrayList<>();
        monthOptions.add("Все месяцы");
        for (String month : monthNames) {
            monthOptions.add(month);
        }

        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, monthOptions);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        monthSpinner.setAdapter(monthAdapter);

        // Устанавливаем текущий месяц
        if (selectedMonth > 0 && selectedMonth <= 12) {
            monthSpinner.setSelection(selectedMonth); // 0 = все месяцы, 1-12 = конкретные месяцы
        }
    }

    protected List<String> generateYearOptions() {
        List<String> years = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        int currentYear = calendar.get(Calendar.YEAR);

        // Добавляем "Все годы" в начало
        years.add("Все годы");

        // Добавляем года от 2000 до текущего + 5 лет
        for (int year = 2000; year <= currentYear + 5; year++) {
            years.add(String.valueOf(year));
        }

        return years;
    }

    protected void setupListeners() {
        yearSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = (String) parent.getItemAtPosition(position);
                if (selected.equals("Все годы")) {
                    selectedYear = -1;
                } else {
                    try {
                        selectedYear = Integer.parseInt(selected);
                    } catch (NumberFormatException e) {
                        selectedYear = -1;
                    }
                }
                updateAnalysis();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        monthSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedMonth = position;
                updateAnalysis();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        addCategoryButton.setOnClickListener(v -> showAddCategoryDialog());
    }

    @Override
    public void updateData(int month, int year) {
        selectedMonth = month;
        selectedYear = year;

        // Обновляем выбор в спиннерах
        if (yearSpinner != null && monthSpinner != null) {
            updateSpinnerSelections();
        }

        updateAnalysis();
    }

    protected void updateSpinnerSelections() {
        // Обновляем год
        if (selectedYear != -1) {
            ArrayAdapter<String> yearAdapter = (ArrayAdapter<String>) yearSpinner.getAdapter();
            if (yearAdapter != null) {
                String yearStr = String.valueOf(selectedYear);
                for (int i = 0; i < yearAdapter.getCount(); i++) {
                    if (yearAdapter.getItem(i).equals(yearStr)) {
                        yearSpinner.setSelection(i);
                        break;
                    }
                }
            }
        } else {
            yearSpinner.setSelection(0); // "Все годы"
        }

        // Обновляем месяц
        if (selectedMonth >= 0 && selectedMonth <= 12) {
            monthSpinner.setSelection(selectedMonth);
        }
    }

    @Override
    public void loadCategories() {
        updateAnalysis();
    }

    public void updateAnalysis() {
        if (mainActivity == null || categoriesContainer == null) return;

        categoriesContainer.removeAllViews();

        List<Transaction> filteredTransactions = getFilteredTransactions();

        if (filteredTransactions.isEmpty()) {
            emptyTextView.setVisibility(View.VISIBLE);
            emptyTextView.setText(getEmptyMessage());
            summaryTextView.setText("Нет данных для отображения");
            return;
        }

        emptyTextView.setVisibility(View.GONE);
        updateSummary(filteredTransactions);
        showCategoryAnalysis(filteredTransactions);
    }

    protected List<Transaction> getFilteredTransactions() {
        List<Transaction> filtered = new ArrayList<>();

        for (Transaction transaction : mainActivity.getTransactions()) {
            if (transaction.isIncome() == isIncomeAnalysis()) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(transaction.getDate());
                int year = cal.get(Calendar.YEAR);
                int month = cal.get(Calendar.MONTH) + 1;

                boolean matches = true;

                if (selectedYear != -1) {
                    matches = matches && (year == selectedYear);
                }

                if (selectedMonth > 0) {
                    matches = matches && (month == selectedMonth);
                }

                if (matches) {
                    filtered.add(transaction);
                }
            }
        }

        Collections.sort(filtered, (t1, t2) -> t2.getDate().compareTo(t1.getDate()));
        return filtered;
    }

    protected void updateSummary(List<Transaction> transactions) {
        double total = 0;
        int count = transactions.size();

        for (Transaction transaction : transactions) {
            total += transaction.getAmount();
        }

        String periodText = getPeriodText();
        String summary = String.format(Locale.getDefault(),
                "💰 Общая сумма: %.2f руб.\n" +
                        "📊 Количество операций: %d\n" +
                        "📅 Средняя сумма: %.2f руб.\n" +
                        "⏰ Период: %s",
                total,
                count,
                count > 0 ? total / count : 0,
                periodText);

        summaryTextView.setText(summary);
    }

    protected String getPeriodText() {
        if (selectedYear == -1 && selectedMonth == 0) {
            return "за всё время";
        } else if (selectedYear != -1 && selectedMonth == 0) {
            return "за " + selectedYear + " год";
        } else if (selectedYear == -1 && selectedMonth > 0) {
            return "за " + monthNames[selectedMonth - 1].toLowerCase();
        } else {
            return "за " + monthNames[selectedMonth - 1].toLowerCase() + " " + selectedYear;
        }
    }

    protected void showCategoryAnalysis(List<Transaction> transactions) {
        Map<String, CategoryStats> categoryStats = new HashMap<>();

        for (Transaction transaction : transactions) {
            String category = transaction.getCategory();
            CategoryStats stats = categoryStats.get(category);
            if (stats == null) {
                stats = new CategoryStats(category);
                categoryStats.put(category, stats);
            }
            stats.amount += transaction.getAmount();
            stats.count++;
        }

        List<CategoryStats> sortedStats = new ArrayList<>(categoryStats.values());

        Collections.sort(sortedStats, (s1, s2) -> {
            return Double.compare(s2.amount, s1.amount); // сортировка по убыванию
        });

        double totalAmount = 0;
        for (CategoryStats stat : sortedStats) {
            totalAmount += stat.amount;
        }

        for (CategoryStats stat : sortedStats) {
            addCategoryView(stat, totalAmount);
        }
    }

    protected void addCategoryView(CategoryStats stat, double totalAmount) {
        LinearLayout categoryLayout = new LinearLayout(getContext());
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(8, 8, 8, 8);
        categoryLayout.setLayoutParams(layoutParams);
        categoryLayout.setOrientation(LinearLayout.VERTICAL);
        categoryLayout.setPadding(20, 16, 20, 16);
        categoryLayout.setBackgroundResource(R.drawable.category_card_background);

        // Верхняя строка: иконка, название и сумма
        LinearLayout topRow = new LinearLayout(getContext());
        topRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        topRow.setOrientation(LinearLayout.HORIZONTAL);

        // Иконка категории
        TextView iconView = new TextView(getContext());
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                40,
                40
        );
        iconParams.gravity = android.view.Gravity.CENTER;
        iconView.setLayoutParams(iconParams);
        iconView.setText(stat.getIcon());
        iconView.setTextSize(18);
        iconView.setGravity(android.view.Gravity.CENTER);

        // Название и сумма
        LinearLayout infoLayout = new LinearLayout(getContext());
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
        );
        infoParams.setMargins(12, 0, 0, 0);
        infoLayout.setLayoutParams(infoParams);
        infoLayout.setOrientation(LinearLayout.VERTICAL);

        // Название категории
        TextView nameView = new TextView(getContext());
        nameView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        nameView.setText(stat.getNameWithoutIcon());
        nameView.setTextSize(15);
        nameView.setTypeface(null, Typeface.BOLD);
        nameView.setTextColor(Color.parseColor("#424242"));

        // Сумма
        TextView amountView = new TextView(getContext());
        amountView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        amountView.setText(stat.getFormattedAmount());
        amountView.setTextSize(14);
        amountView.setTextColor(getPrimaryColor());
        amountView.setPadding(0, 4, 0, 0);

        // Процент
        TextView percentView = new TextView(getContext());
        LinearLayout.LayoutParams percentParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        percentView.setLayoutParams(percentParams);

        double percentage = totalAmount > 0 ? (stat.amount / totalAmount) * 100 : 0;
        percentView.setText(String.format(Locale.getDefault(), "%.1f%%", percentage));
        percentView.setTextSize(15);
        percentView.setTypeface(null, Typeface.BOLD);
        percentView.setTextColor(Color.parseColor("#757575"));

        infoLayout.addView(nameView);
        infoLayout.addView(amountView);

        topRow.addView(iconView);
        topRow.addView(infoLayout);
        topRow.addView(percentView);

        // Детали
        TextView detailsView = new TextView(getContext());
        detailsView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        detailsView.setText(String.format(Locale.getDefault(),
                "📊 %d операций", stat.count));
        detailsView.setTextSize(12);
        detailsView.setTextColor(Color.parseColor("#757575"));
        detailsView.setPadding(0, 8, 0, 12);

        // Прогресс-бар
        View progressBar = new View(getContext());
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                0,
                8,
                (float) percentage
        );
        progressBar.setLayoutParams(progressParams);
        progressBar.setBackgroundColor(mainActivity.getCategoryManager()
                .getCategoryColor(stat.category, isIncomeAnalysis()));

        categoryLayout.addView(topRow);
        categoryLayout.addView(detailsView);
        categoryLayout.addView(progressBar);

        categoriesContainer.addView(categoryLayout);
    }

    protected void showAddCategoryDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext());
        builder.setTitle("Добавить " + getCategoryTypeText().toLowerCase());

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_category_simple, null);
        final android.widget.EditText categoryNameInput = dialogView.findViewById(R.id.categoryNameInput);

        builder.setView(dialogView);

        builder.setPositiveButton("Добавить", (dialog, which) -> {
            String categoryName = categoryNameInput.getText().toString().trim();
            if (!categoryName.isEmpty()) {
                boolean success;
                if (isIncomeAnalysis()) {
                    success = mainActivity.getCategoryManager().addIncomeCategory(categoryName);
                } else {
                    success = mainActivity.getCategoryManager().addExpenseCategory(categoryName);
                }

                if (success) {
                    Toast.makeText(getContext(), "Категория добавлена", Toast.LENGTH_SHORT).show();
                    updateAnalysis();
                } else {
                    Toast.makeText(getContext(), "Категория уже существует", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    protected static class CategoryStats {
        String category;
        double amount;
        int count;

        CategoryStats(String category) {
            this.category = category;
            this.amount = 0;
            this.count = 0;
        }

        public String getFormattedAmount() {
            return String.format(Locale.getDefault(), "%.2f руб.", amount);
        }

        public String getIcon() {
            if (category.contains(" ")) {
                return category.split(" ")[0];
            }
            return "💰";
        }

        public String getNameWithoutIcon() {
            if (category.contains(" ")) {
                return category.substring(category.indexOf(" ") + 1);
            }
            return category;
        }
    }
}