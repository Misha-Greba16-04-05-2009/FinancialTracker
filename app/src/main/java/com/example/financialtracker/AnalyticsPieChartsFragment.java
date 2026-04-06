package com.example.financialtracker;

import android.graphics.Color;
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
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AnalyticsPieChartsFragment extends Fragment {

    private MainActivity mainActivity;
    private CategoryManager categoryManager;

    // UI элементы
    private Spinner yearSpinner;
    private Spinner monthSpinner;
    private TextView periodTitle;
    private TextView totalIncomeValue;
    private TextView totalExpenseValue;
    private TextView balanceValue;
    private TextView transactionCountText;
    private TextView emptyStateTextView;

    private CardView incomeChartCard;
    private CardView expenseChartCard;
    private PieChart incomePieChart;
    private PieChart expensePieChart;
    private TextView incomeEmptyText;
    private TextView expenseEmptyText;
    private LinearLayout incomeLegendContainer;
    private LinearLayout expenseLegendContainer;

    private int selectedYear;
    private int selectedMonth;
    private String periodText = "";

    private final String[] monthNames = {
            "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
            "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_analytics_pie_charts, container, false);

        mainActivity = (MainActivity) getActivity();
        if (mainActivity == null) {
            return view;
        }

        categoryManager = mainActivity.getCategoryManager();

        initViews(view);
        setupSpinners();
        setupListeners();

        // Устанавливаем текущий месяц и год
        Calendar calendar = Calendar.getInstance();
        selectedYear = calendar.get(Calendar.YEAR);
        selectedMonth = calendar.get(Calendar.MONTH) + 1;

        updateCharts();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateCharts();
    }

    private void initViews(View view) {
        yearSpinner = view.findViewById(R.id.yearSpinner);
        monthSpinner = view.findViewById(R.id.monthSpinner);
        periodTitle = view.findViewById(R.id.periodTitle);
        totalIncomeValue = view.findViewById(R.id.totalIncomeValue);
        totalExpenseValue = view.findViewById(R.id.totalExpenseValue);
        balanceValue = view.findViewById(R.id.balanceValue);
        transactionCountText = view.findViewById(R.id.transactionCountText);
        emptyStateTextView = view.findViewById(R.id.emptyStateTextView);

        incomeChartCard = view.findViewById(R.id.incomeChartCard);
        expenseChartCard = view.findViewById(R.id.expenseChartCard);
        incomePieChart = view.findViewById(R.id.incomePieChart);
        expensePieChart = view.findViewById(R.id.expensePieChart);
        incomeEmptyText = view.findViewById(R.id.incomeEmptyText);
        expenseEmptyText = view.findViewById(R.id.expenseEmptyText);
        incomeLegendContainer = view.findViewById(R.id.incomeLegendContainer);
        expenseLegendContainer = view.findViewById(R.id.expenseLegendContainer);
    }

    private void setupSpinners() {
        // Настройка спиннера месяцев
        List<String> monthOptions = new ArrayList<>();
        monthOptions.add("Все месяцы");
        for (String month : monthNames) {
            monthOptions.add(month);
        }

        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, monthOptions);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        monthSpinner.setAdapter(monthAdapter);
        monthSpinner.setSelection(selectedMonth);

        // Настройка спиннера годов
        List<String> yearOptions = new ArrayList<>();
        yearOptions.add("Все годы");
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        for (int year = currentYear; year >= 2020; year--) {
            yearOptions.add(String.valueOf(year));
        }

        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, yearOptions);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        yearSpinner.setAdapter(yearAdapter);
        yearSpinner.setSelection(1); // Выбираем текущий год (после "Все годы")
    }

    private void setupListeners() {
        monthSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedMonth = position;
                updateCharts();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        yearSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = (String) parent.getItemAtPosition(position);
                if (selected.equals("Все годы")) {
                    selectedYear = -1;
                } else {
                    selectedYear = Integer.parseInt(selected);
                }
                updateCharts();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void updateCharts() {
        List<Transaction> filteredTransactions = getFilteredTransactions();

        if (filteredTransactions.isEmpty()) {
            showEmptyState();
            return;
        }

        hideEmptyState();

        // Обновляем заголовок периода
        updatePeriodTitle();

        // Считаем общую статистику
        calculateTotalStats(filteredTransactions);

        // Анализируем по категориям
        Map<String, Double> incomeByCategory = analyzeByCategory(filteredTransactions, true);
        Map<String, Double> expenseByCategory = analyzeByCategory(filteredTransactions, false);

        // Обновляем диаграммы
        updateIncomeChart(incomeByCategory);
        updateExpenseChart(expenseByCategory);
    }

    private List<Transaction> getFilteredTransactions() {
        List<Transaction> allTransactions = mainActivity.getTransactions();
        List<Transaction> filtered = new ArrayList<>();

        for (Transaction transaction : allTransactions) {
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

        return filtered;
    }

    private void updatePeriodTitle() {
        if (selectedYear == -1 && selectedMonth == 0) {
            periodText = "за всё время";
        } else if (selectedYear != -1 && selectedMonth == 0) {
            periodText = "за " + selectedYear + " год";
        } else if (selectedYear == -1 && selectedMonth > 0) {
            periodText = "за " + monthNames[selectedMonth - 1].toLowerCase();
        } else {
            periodText = "за " + monthNames[selectedMonth - 1].toLowerCase() + " " + selectedYear;
        }

        periodTitle.setText("Аналитика " + periodText);
    }

    private void calculateTotalStats(List<Transaction> transactions) {
        double totalIncome = 0;
        double totalExpense = 0;

        for (Transaction t : transactions) {
            if (t.isIncome()) {
                totalIncome += t.getAmount();
            } else {
                totalExpense += t.getAmount();
            }
        }

        double balance = totalIncome - totalExpense;

        totalIncomeValue.setText(String.format(Locale.getDefault(), "%.2f ₽", totalIncome));
        totalExpenseValue.setText(String.format(Locale.getDefault(), "%.2f ₽", totalExpense));
        balanceValue.setText(String.format(Locale.getDefault(), "%.2f ₽", balance));
        transactionCountText.setText("Всего операций: " + transactions.size());

        // Устанавливаем цвета
        totalIncomeValue.setTextColor(Color.parseColor("#4CAF50"));
        totalExpenseValue.setTextColor(Color.parseColor("#F44336"));
        balanceValue.setTextColor(balance >= 0 ?
                Color.parseColor("#4CAF50") : Color.parseColor("#F44336"));
    }

    private Map<String, Double> analyzeByCategory(List<Transaction> transactions, boolean isIncome) {
        Map<String, Double> categoryMap = new HashMap<>();

        for (Transaction t : transactions) {
            if (t.isIncome() == isIncome) {
                String category = t.getCategoryWithoutIcon();
                categoryMap.put(category, categoryMap.getOrDefault(category, 0.0) + t.getAmount());
            }
        }

        return categoryMap;
    }

    private void updateIncomeChart(Map<String, Double> incomeByCategory) {
        if (incomeByCategory.isEmpty()) {
            incomePieChart.setVisibility(View.GONE);
            incomeEmptyText.setVisibility(View.VISIBLE);
            incomeLegendContainer.setVisibility(View.GONE);
            return;
        }

        incomePieChart.setVisibility(View.VISIBLE);
        incomeEmptyText.setVisibility(View.GONE);
        incomeLegendContainer.setVisibility(View.VISIBLE);

        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Double> entry : incomeByCategory.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Доходы по категориям");

        // Используем зеленые оттенки для доходов
        int[] incomeColors = {
                Color.parseColor("#4CAF50"), Color.parseColor("#8BC34A"),
                Color.parseColor("#CDDC39"), Color.parseColor("#FFC107"),
                Color.parseColor("#FF9800"), Color.parseColor("#FF5722"),
                Color.parseColor("#795548"), Color.parseColor("#9C27B0"),
                Color.parseColor("#2196F3"), Color.parseColor("#00BCD4")
        };
        dataSet.setColors(incomeColors);

        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setSliceSpace(2f);
        dataSet.setSelectionShift(5f);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter());
        data.setValueTextSize(12f);
        data.setValueTextColor(Color.WHITE);

        Description description = new Description();
        description.setText("");
        incomePieChart.setDescription(description);
        incomePieChart.setHoleRadius(40f);
        incomePieChart.setTransparentCircleRadius(45f);
        incomePieChart.setDrawEntryLabels(true);
        incomePieChart.setEntryLabelColor(Color.BLACK);
        incomePieChart.setEntryLabelTextSize(10f);
        incomePieChart.setData(data);
        incomePieChart.invalidate();
        incomePieChart.animateY(1000);

        updateLegend(incomeLegendContainer, incomeByCategory, true);
    }

    private void updateExpenseChart(Map<String, Double> expenseByCategory) {
        if (expenseByCategory.isEmpty()) {
            expensePieChart.setVisibility(View.GONE);
            expenseEmptyText.setVisibility(View.VISIBLE);
            expenseLegendContainer.setVisibility(View.GONE);
            return;
        }

        expensePieChart.setVisibility(View.VISIBLE);
        expenseEmptyText.setVisibility(View.GONE);
        expenseLegendContainer.setVisibility(View.VISIBLE);

        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Double> entry : expenseByCategory.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Расходы по категориям");

        // Используем красные оттенки для расходов
        int[] expenseColors = {
                Color.parseColor("#F44336"), Color.parseColor("#FF5252"),
                Color.parseColor("#FF7043"), Color.parseColor("#FF8A65"),
                Color.parseColor("#FFA726"), Color.parseColor("#FFB74D"),
                Color.parseColor("#E91E63"), Color.parseColor("#EC407A"),
                Color.parseColor("#AB47BC"), Color.parseColor("#7E57C2")
        };
        dataSet.setColors(expenseColors);

        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setSliceSpace(2f);
        dataSet.setSelectionShift(5f);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter());
        data.setValueTextSize(12f);
        data.setValueTextColor(Color.WHITE);

        Description description = new Description();
        description.setText("");
        expensePieChart.setDescription(description);
        expensePieChart.setHoleRadius(40f);
        expensePieChart.setTransparentCircleRadius(45f);
        expensePieChart.setDrawEntryLabels(true);
        expensePieChart.setEntryLabelColor(Color.BLACK);
        expensePieChart.setEntryLabelTextSize(10f);
        expensePieChart.setData(data);
        expensePieChart.invalidate();
        expensePieChart.animateY(1000);

        updateLegend(expenseLegendContainer, expenseByCategory, false);
    }

    private void updateLegend(LinearLayout legendContainer, Map<String, Double> data, boolean isIncome) {
        legendContainer.removeAllViews();

        double total = 0;
        for (double amount : data.values()) {
            total += amount;
        }

        int index = 0;
        for (Map.Entry<String, Double> entry : data.entrySet()) {
            double percentage = (entry.getValue() / total) * 100;

            LinearLayout legendItem = new LinearLayout(requireContext());
            legendItem.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            legendItem.setOrientation(LinearLayout.HORIZONTAL);
            legendItem.setPadding(0, 8, 0, 8);

            // Цветной квадратик
            View colorView = new View(requireContext());
            colorView.setLayoutParams(new LinearLayout.LayoutParams(24, 24));

            int[] incomeColors = {
                    Color.parseColor("#4CAF50"), Color.parseColor("#8BC34A"),
                    Color.parseColor("#CDDC39"), Color.parseColor("#FFC107"),
                    Color.parseColor("#FF9800"), Color.parseColor("#FF5722"),
                    Color.parseColor("#795548"), Color.parseColor("#9C27B0"),
                    Color.parseColor("#2196F3"), Color.parseColor("#00BCD4")
            };

            int[] expenseColors = {
                    Color.parseColor("#F44336"), Color.parseColor("#FF5252"),
                    Color.parseColor("#FF7043"), Color.parseColor("#FF8A65"),
                    Color.parseColor("#FFA726"), Color.parseColor("#FFB74D"),
                    Color.parseColor("#E91E63"), Color.parseColor("#EC407A"),
                    Color.parseColor("#AB47BC"), Color.parseColor("#7E57C2")
            };

            if (isIncome) {
                colorView.setBackgroundColor(incomeColors[index % incomeColors.length]);
            } else {
                colorView.setBackgroundColor(expenseColors[index % expenseColors.length]);
            }

            // Название категории
            TextView categoryText = new TextView(requireContext());
            categoryText.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            categoryText.setText(entry.getKey());
            categoryText.setTextSize(14);
            categoryText.setTextColor(Color.BLACK);
            categoryText.setPadding(12, 0, 0, 0);

            // Сумма и процент
            TextView amountText = new TextView(requireContext());
            amountText.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            amountText.setText(String.format(Locale.getDefault(),
                    "%.2f ₽ (%.1f%%)", entry.getValue(), percentage));
            amountText.setTextSize(14);
            amountText.setTextColor(isIncome ?
                    Color.parseColor("#4CAF50") : Color.parseColor("#F44336"));

            legendItem.addView(colorView);
            legendItem.addView(categoryText);
            legendItem.addView(amountText);

            legendContainer.addView(legendItem);
            index++;
        }
    }

    private void showEmptyState() {
        emptyStateTextView.setVisibility(View.VISIBLE);
        incomeChartCard.setVisibility(View.GONE);
        expenseChartCard.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        emptyStateTextView.setVisibility(View.GONE);
        incomeChartCard.setVisibility(View.VISIBLE);
        expenseChartCard.setVisibility(View.VISIBLE);
    }
}