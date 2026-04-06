package com.example.financialtracker;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdvancedAnalyticsFragment extends Fragment {

    private MainActivity mainActivity;
    private DataManager dataManager;
    private CbrRateManager rateManager;
    private CategoryManager categoryManager;

    // UI элементы
    private Spinner periodSpinner;
    private Button datePickerButton;
    private Button compareButton;
    private Button exportButton;
    private ScrollView contentScrollView;

    private CardView summaryCard;
    private TextView periodTitle;
    private TextView totalIncomeValue;
    private TextView totalExpenseValue;
    private TextView balanceValue;
    private TextView incomeChangeText;
    private TextView expenseChangeText;

    private CardView chartsCard;
    private PieChart expensePieChart;
    private PieChart incomePieChart;
    private BarChart categoryBarChart;
    private LineChart trendLineChart;

    private CardView insightsCard;
    private LinearLayout insightsContainer;

    private AnalyticsData currentData;
    private int selectedYear = -1;
    private int selectedMonth = -1;

    // Для безопасности - флаг, что графики загружены
    private boolean chartsReady = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_advanced_analytics, container, false);

        mainActivity = (MainActivity) getActivity();
        if (mainActivity == null) {
            return view;
        }

        dataManager = mainActivity.getDataManager();
        categoryManager = mainActivity.getCategoryManager();

        // ПОЛУЧАЕМ rateManager ИЗ MainActivity
        rateManager = mainActivity.getRateManager();

        initViews(view);
        setupListeners();

        loadCurrentMonthData();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (currentData == null) {
            loadCurrentMonthData();
        } else {
            updateUI();
        }
    }

    private void initViews(View view) {
        try {
            periodSpinner = view.findViewById(R.id.periodSpinner);
            datePickerButton = view.findViewById(R.id.datePickerButton);
            compareButton = view.findViewById(R.id.compareButton);
            exportButton = view.findViewById(R.id.exportButton);
            contentScrollView = view.findViewById(R.id.contentScrollView);

            summaryCard = view.findViewById(R.id.summaryCard);
            periodTitle = view.findViewById(R.id.periodTitle);
            totalIncomeValue = view.findViewById(R.id.totalIncomeValue);
            totalExpenseValue = view.findViewById(R.id.totalExpenseValue);
            balanceValue = view.findViewById(R.id.balanceValue);
            incomeChangeText = view.findViewById(R.id.incomeChangeText);
            expenseChangeText = view.findViewById(R.id.expenseChangeText);

            chartsCard = view.findViewById(R.id.chartsCard);
            expensePieChart = view.findViewById(R.id.expensePieChart);
            incomePieChart = view.findViewById(R.id.incomePieChart);
            categoryBarChart = view.findViewById(R.id.categoryBarChart);
            trendLineChart = view.findViewById(R.id.trendLineChart);

            insightsCard = view.findViewById(R.id.insightsCard);
            insightsContainer = view.findViewById(R.id.insightsContainer);

            chartsReady = true;
        } catch (Exception e) {
            chartsReady = false;
            e.printStackTrace();
        }
    }

    private void setupListeners() {
        datePickerButton.setOnClickListener(v -> showDatePickerDialog());
        compareButton.setOnClickListener(v -> showComparisonDialog());
        exportButton.setOnClickListener(v -> exportReport());
    }

    private void loadCurrentMonthData() {
        try {
            Calendar cal = Calendar.getInstance();
            selectedYear = cal.get(Calendar.YEAR);
            selectedMonth = cal.get(Calendar.MONTH) + 1;

            // Создаём аналитику на основе транзакций
            currentData = generateAnalyticsForMonth(selectedYear, selectedMonth);
            updateUI();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Ошибка загрузки данных");
        }
    }

    private AnalyticsData generateAnalyticsForMonth(int year, int month) {
        AnalyticsData data = new AnalyticsData();

        try {
            List<Transaction> allTransactions = mainActivity.getTransactions();
            List<Transaction> monthTransactions = new ArrayList<>();

            // Фильтруем транзакции за месяц
            Calendar cal = Calendar.getInstance();
            for (Transaction transaction : allTransactions) {
                cal.setTime(transaction.getDate());
                int transactionYear = cal.get(Calendar.YEAR);
                int transactionMonth = cal.get(Calendar.MONTH) + 1;

                if (transactionYear == year && transactionMonth == month) {
                    monthTransactions.add(transaction);
                }
            }

            // Считаем доходы и расходы
            double totalIncome = 0;
            double totalExpense = 0;

            for (Transaction t : monthTransactions) {
                double amount = t.getAmount();
                if (t.isIncome()) {
                    totalIncome += amount;
                } else {
                    totalExpense += amount;
                }
            }

            data.setTotalIncome(totalIncome);
            data.setTotalExpense(totalExpense);
            data.setBalance(totalIncome - totalExpense);
            data.setTransactionCount(monthTransactions.size());

            // Анализ по категориям
            Map<String, Double> expenseByCategory = new java.util.HashMap<>();
            Map<String, Double> incomeByCategory = new java.util.HashMap<>();

            for (Transaction t : monthTransactions) {
                double amount = t.getAmount();
                String category = t.getCategory();
                if (t.isIncome()) {
                    incomeByCategory.put(category, incomeByCategory.getOrDefault(category, 0.0) + amount);
                } else {
                    expenseByCategory.put(category, expenseByCategory.getOrDefault(category, 0.0) + amount);
                }
            }

            data.setExpenseByCategory(expenseByCategory);
            data.setIncomeByCategory(incomeByCategory);

            // Период
            SimpleDateFormat sdf = new SimpleDateFormat("LLLL yyyy", Locale.getDefault());
            cal.set(year, month - 1, 1);
            data.setFormattedPeriod(sdf.format(cal.getTime()));

        } catch (Exception e) {
            e.printStackTrace();
        }

        return data;
    }

    private void updateUI() {
        if (currentData == null) return;

        try {
            updateSummaryCard();

            if (chartsReady) {
                updateExpensePieChart();
                updateIncomePieChart();
                updateCategoryBarChart();
                updateTrendLineChart();
            }

            updateInsightsCard();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Ошибка обновления интерфейса");
        }
    }

    private void updateSummaryCard() {
        try {
            periodTitle.setText(currentData.getFormattedPeriod());

            totalIncomeValue.setText(String.format(Locale.getDefault(),
                    "+%.2f ₽", currentData.getTotalIncome()));
            totalExpenseValue.setText(String.format(Locale.getDefault(),
                    "-%.2f ₽", currentData.getTotalExpense()));
            balanceValue.setText(String.format(Locale.getDefault(),
                    "%.2f ₽", currentData.getBalance()));

            // Цвета
            totalIncomeValue.setTextColor(Color.parseColor("#4CAF50"));
            totalExpenseValue.setTextColor(Color.parseColor("#F44336"));

            if (currentData.getBalance() >= 0) {
                balanceValue.setTextColor(Color.parseColor("#4CAF50"));
            } else {
                balanceValue.setTextColor(Color.parseColor("#F44336"));
            }

            // Изменения (заглушка)
            incomeChangeText.setText("📊 По сравнению с прошлым месяцем");
            expenseChangeText.setText("📊 По сравнению с прошлым месяцем");
            incomeChangeText.setTextColor(Color.parseColor("#757575"));
            expenseChangeText.setTextColor(Color.parseColor("#757575"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateExpensePieChart() {
        try {
            if (expensePieChart == null) return;

            List<PieEntry> entries = new ArrayList<>();

            for (Map.Entry<String, Double> entry : currentData.getExpenseByCategory().entrySet()) {
                String category = entry.getKey();
                if (category.contains(" ")) {
                    category = category.substring(category.indexOf(" ") + 1);
                }
                if (category.length() > 15) {
                    category = category.substring(0, 12) + "...";
                }
                entries.add(new PieEntry(entry.getValue().floatValue(), category));
            }

            if (entries.isEmpty()) {
                entries.add(new PieEntry(1, "Нет расходов"));
            }

            PieDataSet dataSet = new PieDataSet(entries, "Расходы по категориям");
            dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
            dataSet.setValueTextSize(12f);
            dataSet.setValueTextColor(Color.WHITE);

            PieData data = new PieData(dataSet);
            data.setValueFormatter(new PercentFormatter()); // ← ИСПРАВЛЕНО!

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

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateIncomePieChart() {
        try {
            if (incomePieChart == null) return;

            List<PieEntry> entries = new ArrayList<>();

            for (Map.Entry<String, Double> entry : currentData.getIncomeByCategory().entrySet()) {
                String category = entry.getKey();
                if (category.contains(" ")) {
                    category = category.substring(category.indexOf(" ") + 1);
                }
                if (category.length() > 15) {
                    category = category.substring(0, 12) + "...";
                }
                entries.add(new PieEntry(entry.getValue().floatValue(), category));
            }

            if (entries.isEmpty()) {
                entries.add(new PieEntry(1, "Нет доходов"));
            }

            PieDataSet dataSet = new PieDataSet(entries, "Доходы по категориям");
            dataSet.setColors(ColorTemplate.COLORFUL_COLORS);
            dataSet.setValueTextSize(12f);
            dataSet.setValueTextColor(Color.WHITE);

            PieData data = new PieData(dataSet);
            data.setValueFormatter(new PercentFormatter()); // ← ИСПРАВЛЕНО!

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

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateCategoryBarChart() {
        try {
            if (categoryBarChart == null) return;

            List<BarEntry> entries = new ArrayList<>();
            List<String> labels = new ArrayList<>();

            int index = 0;
            for (Map.Entry<String, Double> entry : currentData.getExpenseByCategory().entrySet()) {
                String category = entry.getKey();
                if (category.contains(" ")) {
                    category = category.substring(category.indexOf(" ") + 1);
                }
                entries.add(new BarEntry(index, entry.getValue().floatValue()));
                labels.add(category.length() > 10 ? category.substring(0, 10) + "..." : category);
                index++;

                if (index >= 7) break;
            }

            if (entries.isEmpty()) {
                entries.add(new BarEntry(0, 0));
                labels.add("Нет данных");
            }

            BarDataSet dataSet = new BarDataSet(entries, "Расходы по категориям");
            dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
            dataSet.setValueTextSize(10f);

            BarData data = new BarData(dataSet);

            Description description = new Description();
            description.setText("");
            categoryBarChart.setDescription(description);
            categoryBarChart.setData(data);

            XAxis xAxis = categoryBarChart.getXAxis();
            xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setGranularity(1f);
            xAxis.setLabelRotationAngle(45f);

            YAxis leftAxis = categoryBarChart.getAxisLeft();
            leftAxis.setAxisMinimum(0f);

            categoryBarChart.getAxisRight().setEnabled(false);
            categoryBarChart.animateY(1000);
            categoryBarChart.invalidate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateTrendLineChart() {
        try {
            if (trendLineChart == null) return;

            // Простая заглушка для графика
            List<Entry> entries = new ArrayList<>();
            entries.add(new Entry(0, 0));

            LineDataSet dataSet = new LineDataSet(entries, "Динамика");
            dataSet.setColor(Color.parseColor("#2196F3"));
            dataSet.setCircleColor(Color.parseColor("#2196F3"));
            dataSet.setLineWidth(2f);

            LineData lineData = new LineData(dataSet);

            Description description = new Description();
            description.setText("Динамика за период");
            trendLineChart.setDescription(description);
            trendLineChart.setData(lineData);
            trendLineChart.invalidate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateInsightsCard() {
        try {
            insightsContainer.removeAllViews();

            List<String> insights = new ArrayList<>();

            // Топ категория расходов
            insights.add(getTopExpenseCategory());

            // Топ категория доходов
            insights.add(getTopIncomeCategory());

            // Количество транзакций
            insights.add(String.format(Locale.getDefault(),
                    "📊 Всего операций: %d", currentData.getTransactionCount()));

            // Соотношение расходов к доходам
            if (currentData.getTotalIncome() > 0) {
                double ratio = (currentData.getTotalExpense() / currentData.getTotalIncome()) * 100;
                if (ratio > 100) {
                    insights.add(String.format(Locale.getDefault(),
                            "⚠️ Расходы превышают доходы на %.1f%%", ratio - 100));
                } else {
                    insights.add(String.format(Locale.getDefault(),
                            "💰 Вы откладываете %.1f%% доходов", 100 - ratio));
                }
            }

            for (String insight : insights) {
                TextView textView = new TextView(getContext());
                textView.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                textView.setText("• " + insight);
                textView.setTextSize(14);
                textView.setTextColor(Color.DKGRAY);
                textView.setPadding(0, 4, 0, 4);
                insightsContainer.addView(textView);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getTopExpenseCategory() {
        try {
            String topCategory = "";
            double maxAmount = 0;
            for (Map.Entry<String, Double> entry : currentData.getExpenseByCategory().entrySet()) {
                if (entry.getValue() > maxAmount) {
                    maxAmount = entry.getValue();
                    topCategory = entry.getKey();
                }
            }
            if (!topCategory.isEmpty()) {
                String categoryName = topCategory.contains(" ") ?
                        topCategory.substring(topCategory.indexOf(" ") + 1) : topCategory;
                double percent = (maxAmount / currentData.getTotalExpense()) * 100;
                return String.format(Locale.getDefault(),
                        "💰 Больше всего тратите на %s: %.2f ₽ (%.1f%%)",
                        categoryName, maxAmount, percent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "💰 Нет данных по расходам";
    }

    private String getTopIncomeCategory() {
        try {
            String topCategory = "";
            double maxAmount = 0;
            for (Map.Entry<String, Double> entry : currentData.getIncomeByCategory().entrySet()) {
                if (entry.getValue() > maxAmount) {
                    maxAmount = entry.getValue();
                    topCategory = entry.getKey();
                }
            }
            if (!topCategory.isEmpty()) {
                String categoryName = topCategory.contains(" ") ?
                        topCategory.substring(topCategory.indexOf(" ") + 1) : topCategory;
                double percent = (maxAmount / currentData.getTotalIncome()) * 100;
                return String.format(Locale.getDefault(),
                        "📈 Основной доход от %s: %.2f ₽ (%.1f%%)",
                        categoryName, maxAmount, percent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "📈 Нет данных по доходам";
    }

    private void showDatePickerDialog() {
        try {
            Calendar cal = Calendar.getInstance();

            android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(
                    getContext(),
                    (view, year, month, dayOfMonth) -> {
                        selectedYear = year;
                        selectedMonth = month + 1;
                        currentData = generateAnalyticsForMonth(year, month + 1);
                        updateUI();
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
            );

            datePickerDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Ошибка открытия календаря");
        }
    }

    private void showComparisonDialog() {
        Toast.makeText(getContext(), "Функция сравнения периодов в разработке", Toast.LENGTH_SHORT).show();
    }

    private void exportReport() {
        Toast.makeText(getContext(), "Функция экспорта в разработке", Toast.LENGTH_SHORT).show();
    }

    private void showError(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    // ============ ВСПОМОГАТЕЛЬНЫЙ КЛАСС ============

    private static class AnalyticsData {
        private double totalIncome;
        private double totalExpense;
        private double balance;
        private int transactionCount;
        private Map<String, Double> expenseByCategory;
        private Map<String, Double> incomeByCategory;
        private String formattedPeriod;

        public double getTotalIncome() { return totalIncome; }
        public void setTotalIncome(double totalIncome) { this.totalIncome = totalIncome; }

        public double getTotalExpense() { return totalExpense; }
        public void setTotalExpense(double totalExpense) { this.totalExpense = totalExpense; }

        public double getBalance() { return balance; }
        public void setBalance(double balance) { this.balance = balance; }

        public int getTransactionCount() { return transactionCount; }
        public void setTransactionCount(int transactionCount) { this.transactionCount = transactionCount; }

        public Map<String, Double> getExpenseByCategory() { return expenseByCategory; }
        public void setExpenseByCategory(Map<String, Double> expenseByCategory) { this.expenseByCategory = expenseByCategory; }

        public Map<String, Double> getIncomeByCategory() { return incomeByCategory; }
        public void setIncomeByCategory(Map<String, Double> incomeByCategory) { this.incomeByCategory = incomeByCategory; }

        public String getFormattedPeriod() { return formattedPeriod; }
        public void setFormattedPeriod(String formattedPeriod) { this.formattedPeriod = formattedPeriod; }
    }
}