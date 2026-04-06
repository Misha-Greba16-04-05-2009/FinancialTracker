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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatisticsFragment extends Fragment {

    private LinearLayout statisticsContainer;
    private TextView summaryTextView;
    private MainActivity mainActivity;
    private Spinner yearSpinner;
    private Spinner monthSpinner;

    private int selectedYear = -1;
    private int selectedMonth = 0;

    private final String[] monthNames = {
            "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
            "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_statistics_modern, container, false);

        mainActivity = (MainActivity) getActivity();
        statisticsContainer = view.findViewById(R.id.statisticsContainer);
        summaryTextView = view.findViewById(R.id.summaryTextView);
        yearSpinner = view.findViewById(R.id.yearSpinner);
        monthSpinner = view.findViewById(R.id.monthSpinner);

        setupSpinners();
        setupListeners();

        updateStatistics();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateStatistics();
    }

    private void setupSpinners() {
        if (mainActivity == null) return;

        // Генерация списка годов
        List<String> yearOptions = new ArrayList<>();
        yearOptions.add("Все годы");

        Calendar calendar = Calendar.getInstance();
        int currentYear = calendar.get(Calendar.YEAR);

        // Годы от 2000 до текущего + 5
        for (int year = 2000; year <= currentYear + 5; year++) {
            yearOptions.add(String.valueOf(year));
        }

        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, yearOptions);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        yearSpinner.setAdapter(yearAdapter);

        // Устанавливаем текущий год
        String currentYearStr = String.valueOf(currentYear);
        int position = yearOptions.indexOf(currentYearStr);
        if (position != -1) {
            yearSpinner.setSelection(position);
            selectedYear = currentYear;
        }

        // Настройка месяцев
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
        int currentMonth = calendar.get(Calendar.MONTH) + 1;
        monthSpinner.setSelection(currentMonth);
        selectedMonth = currentMonth;
    }

    private void setupListeners() {
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
                updateStatistics();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        monthSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedMonth = position;
                updateStatistics();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    public void updateStatistics() {
        if (mainActivity == null || statisticsContainer == null || summaryTextView == null) return;

        statisticsContainer.removeAllViews();

        // Общая статистика
        String statistics = getFilteredStatistics();
        summaryTextView.setText(statistics);

        // Добавляем визуализацию
        addCategoryVisualization();
    }

    private String getFilteredStatistics() {
        double totalIncome = 0;
        double totalExpenses = 0;
        int incomeCount = 0;
        int expenseCount = 0;

        for (Transaction transaction : mainActivity.getTransactions()) {
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
                if (transaction.isIncome()) {
                    totalIncome += transaction.getAmount();
                    incomeCount++;
                } else {
                    totalExpenses += transaction.getAmount();
                    expenseCount++;
                }
            }
        }

        double balance = totalIncome - totalExpenses;
        int totalCount = incomeCount + expenseCount;
        String periodText = getPeriodText();

        return String.format(Locale.getDefault(),
                "📊 ОБЩАЯ СТАТИСТИКА %s\n\n" +
                        "💰 Баланс: %.2f руб.\n" +
                        "📈 Доходы: %.2f руб. (%d операций)\n" +
                        "📉 Расходы: %.2f руб. (%d операций)\n" +
                        "📊 Всего операций: %d",
                periodText,
                balance,
                totalIncome, incomeCount,
                totalExpenses, expenseCount,
                totalCount);
    }

    private String getPeriodText() {
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

    private void addCategoryVisualization() {
        if (mainActivity == null || statisticsContainer == null) return;

        Map<String, Double> incomeCategories = new HashMap<>();
        Map<String, Double> expenseCategories = new HashMap<>();

        double totalIncome = 0;
        double totalExpenses = 0;

        for (Transaction transaction : mainActivity.getTransactions()) {
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
                if (transaction.isIncome()) {
                    totalIncome += transaction.getAmount();
                    incomeCategories.put(transaction.getCategory(),
                            incomeCategories.getOrDefault(transaction.getCategory(), 0.0) + transaction.getAmount());
                } else {
                    totalExpenses += transaction.getAmount();
                    expenseCategories.put(transaction.getCategory(),
                            expenseCategories.getOrDefault(transaction.getCategory(), 0.0) + transaction.getAmount());
                }
            }
        }

        // Добавляем карточку общей статистики
        addSummaryCard(totalIncome, totalExpenses);

        // Добавляем графики для доходов
        if (totalIncome > 0) {
            addCategorySection("📈 РАСПРЕДЕЛЕНИЕ ДОХОДОВ", incomeCategories, totalIncome, true);
        }

        // Добавляем графики для расходов
        if (totalExpenses > 0) {
            addCategorySection("📉 РАСПРЕДЕЛЕНИЕ РАСХОДОВ", expenseCategories, totalExpenses, false);
        }
    }

    private void addSummaryCard(double totalIncome, double totalExpenses) {
        if (getContext() == null) return;

        LinearLayout summaryCard = new LinearLayout(getContext());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(16, 16, 16, 16);
        summaryCard.setLayoutParams(cardParams);
        summaryCard.setOrientation(LinearLayout.VERTICAL);
        summaryCard.setPadding(20, 20, 20, 20);
        summaryCard.setBackgroundResource(R.drawable.category_card_background);

        // Заголовок
        TextView title = new TextView(getContext());
        title.setText("📊 Общая статистика " + getPeriodText());
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.parseColor("#2196F3"));
        title.setPadding(0, 0, 0, 12);

        // Доходы
        TextView incomeText = new TextView(getContext());
        incomeText.setText(String.format(Locale.getDefault(), "📈 Доходы: %.2f руб.", totalIncome));
        incomeText.setTextSize(15);
        incomeText.setTextColor(Color.parseColor("#4CAF50"));
        incomeText.setPadding(0, 4, 0, 4);

        // Расходы
        TextView expenseText = new TextView(getContext());
        expenseText.setText(String.format(Locale.getDefault(), "📉 Расходы: %.2f руб.", totalExpenses));
        expenseText.setTextSize(15);
        expenseText.setTextColor(Color.parseColor("#F44336"));
        expenseText.setPadding(0, 4, 0, 4);

        // Баланс
        double balance = totalIncome - totalExpenses;
        TextView balanceText = new TextView(getContext());
        balanceText.setText(String.format(Locale.getDefault(), "💰 Баланс: %.2f руб.", balance));
        balanceText.setTextSize(16);
        balanceText.setTypeface(null, Typeface.BOLD);
        balanceText.setTextColor(balance >= 0 ?
                Color.parseColor("#4CAF50") : Color.parseColor("#F44336"));
        balanceText.setPadding(0, 8, 0, 0);

        summaryCard.addView(title);
        summaryCard.addView(incomeText);
        summaryCard.addView(expenseText);
        summaryCard.addView(balanceText);

        statisticsContainer.addView(summaryCard);
    }

    private void addCategorySection(String title, Map<String, Double> categories,
                                    double total, boolean isIncome) {
        if (getContext() == null || statisticsContainer == null) return;

        LinearLayout sectionCard = new LinearLayout(getContext());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(16, 8, 16, 16);
        sectionCard.setLayoutParams(cardParams);
        sectionCard.setOrientation(LinearLayout.VERTICAL);
        sectionCard.setPadding(20, 20, 20, 20);
        sectionCard.setBackgroundResource(R.drawable.category_card_background);

        // Заголовок раздела
        TextView sectionTitle = new TextView(getContext());
        sectionTitle.setText(title);
        sectionTitle.setTextSize(16);
        sectionTitle.setTypeface(null, Typeface.BOLD);
        sectionTitle.setTextColor(isIncome ?
                Color.parseColor("#4CAF50") : Color.parseColor("#F44336"));
        sectionTitle.setPadding(0, 0, 0, 12);

        sectionCard.addView(sectionTitle);

        // Добавляем каждую категорию
        for (Map.Entry<String, Double> entry : categories.entrySet()) {
            addCategoryBar(sectionCard, entry.getKey(), entry.getValue(), total, isIncome);
        }

        statisticsContainer.addView(sectionCard);
    }

    private void addCategoryBar(LinearLayout parent, String category,
                                double amount, double total, boolean isIncome) {
        if (getContext() == null) return;

        LinearLayout barLayout = new LinearLayout(getContext());
        barLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        barLayout.setOrientation(LinearLayout.VERTICAL);
        barLayout.setPadding(0, 8, 0, 8);

        // Информация о категории
        LinearLayout infoLayout = new LinearLayout(getContext());
        infoLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        infoLayout.setOrientation(LinearLayout.HORIZONTAL);

        // Название
        TextView nameText = new TextView(getContext());
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
        );
        nameText.setLayoutParams(nameParams);

        String displayName = category.contains(" ") ?
                category.substring(category.indexOf(" ") + 1) : category;
        nameText.setText(displayName);
        nameText.setTextSize(14);
        nameText.setTextColor(Color.parseColor("#424242"));

        // Сумма и процент
        TextView amountText = new TextView(getContext());
        amountText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        double percentage = total > 0 ? (amount / total) * 100 : 0;
        amountText.setText(String.format(Locale.getDefault(),
                "%.2f руб. (%.1f%%)", amount, percentage));
        amountText.setTextSize(14);
        amountText.setTextColor(isIncome ?
                Color.parseColor("#4CAF50") : Color.parseColor("#F44336"));

        infoLayout.addView(nameText);
        infoLayout.addView(amountText);

        // Прогресс-бар
        View progressBar = new View(getContext());
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                0,
                6,
                (float) percentage
        );
        progressBar.setLayoutParams(progressParams);
        progressBar.setBackgroundColor(mainActivity.getCategoryManager()
                .getCategoryColor(category, isIncome));

        barLayout.addView(infoLayout);
        barLayout.addView(progressBar);
        parent.addView(barLayout);
    }
}