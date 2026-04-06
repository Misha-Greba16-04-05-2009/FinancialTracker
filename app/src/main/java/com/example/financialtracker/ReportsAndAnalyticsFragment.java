package com.example.financialtracker;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ReportsAndAnalyticsFragment extends Fragment {

    private MainActivity mainActivity;
    private DataManager dataManager;
    private CategoryManager categoryManager;
    private CbrRateManager rateManager;

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private ReportsPagerAdapter pagerAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reports_and_analytics, container, false);

        mainActivity = (MainActivity) getActivity();
        if (mainActivity == null) {
            return view;
        }

        dataManager = mainActivity.getDataManager();
        categoryManager = mainActivity.getCategoryManager();
        rateManager = mainActivity.getRateManager();

        initViews(view);
        setupViewPager();

        return view;
    }

    private void initViews(View view) {
        viewPager = view.findViewById(R.id.viewPager);
        tabLayout = view.findViewById(R.id.tabLayout);
    }

    private void setupViewPager() {
        pagerAdapter = new ReportsPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText("📊 Аналитика");
                            break;
                        case 1:
                            tab.setText("📈 Расходы");
                            break;
                        case 2:
                            tab.setText("📉 Доходы");
                            break;
                        case 3:
                            tab.setText("📋 Отчёты");
                            break;
                    }
                }
        ).attach();
    }

    // ============ АДАПТЕР ДЛЯ ВКЛАДОК ============

    private static class ReportsPagerAdapter extends FragmentStateAdapter {

        public ReportsPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new AnalyticsPieChartsFragment(); // Новый фрагмент с диаграммами
                case 1:
                    return new ExpenseAnalysisFragment();
                case 2:
                    return new IncomeAnalysisFragment();
                case 3:
                    return new ReportsFragment();
                default:
                    return new AnalyticsPieChartsFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 4;
        }
    }

    // ============ ФРАГМЕНТ ОБЗОРА АНАЛИТИКИ ============

    public static class AnalyticsOverviewFragment extends Fragment {

        private MainActivity mainActivity;
        private LinearLayout statsContainer;
        private TextView periodTitle;
        private CardView summaryCard;
        private TextView totalIncomeValue;
        private TextView totalExpenseValue;
        private TextView balanceValue;
        private LinearLayout insightsContainer;

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_analytics_overview, container, false);

            mainActivity = (MainActivity) getActivity();

            initViews(view);
            loadAnalytics();

            return view;
        }

        @Override
        public void onResume() {
            super.onResume();
            loadAnalytics();
        }

        private void initViews(View view) {
            statsContainer = view.findViewById(R.id.statsContainer);
            periodTitle = view.findViewById(R.id.periodTitle);
            summaryCard = view.findViewById(R.id.summaryCard);
            totalIncomeValue = view.findViewById(R.id.totalIncomeValue);
            totalExpenseValue = view.findViewById(R.id.totalExpenseValue);
            balanceValue = view.findViewById(R.id.balanceValue);
            insightsContainer = view.findViewById(R.id.insightsContainer);
        }

        private void loadAnalytics() {
            if (mainActivity == null) return;

            Calendar calendar = Calendar.getInstance();
            int currentMonth = calendar.get(Calendar.MONTH) + 1;
            int currentYear = calendar.get(Calendar.YEAR);

            SimpleDateFormat sdf = new SimpleDateFormat("LLLL yyyy", Locale.getDefault());
            periodTitle.setText(sdf.format(calendar.getTime()));

            List<Transaction> allTransactions = mainActivity.getTransactions();
            List<Transaction> monthTransactions = new ArrayList<>();

            // Фильтруем транзакции за текущий месяц
            for (Transaction transaction : allTransactions) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(transaction.getDate());
                int transactionYear = cal.get(Calendar.YEAR);
                int transactionMonth = cal.get(Calendar.MONTH) + 1;

                if (transactionYear == currentYear && transactionMonth == currentMonth) {
                    monthTransactions.add(transaction);
                }
            }

            // Считаем доходы и расходы
            double totalIncome = 0;
            double totalExpense = 0;

            for (Transaction t : monthTransactions) {
                if (t.isIncome()) {
                    totalIncome += t.getAmount();
                } else {
                    totalExpense += t.getAmount();
                }
            }

            double balance = totalIncome - totalExpense;

            totalIncomeValue.setText(String.format(Locale.getDefault(),
                    "+%.2f ₽", totalIncome));
            totalExpenseValue.setText(String.format(Locale.getDefault(),
                    "-%.2f ₽", totalExpense));
            balanceValue.setText(String.format(Locale.getDefault(),
                    "%.2f ₽", balance));

            totalIncomeValue.setTextColor(Color.parseColor("#4CAF50"));
            totalExpenseValue.setTextColor(Color.parseColor("#F44336"));
            balanceValue.setTextColor(balance >= 0 ?
                    Color.parseColor("#4CAF50") : Color.parseColor("#F44336"));

            // Добавляем инсайты
            insightsContainer.removeAllViews();
            addInsight("📊 Всего операций: " + monthTransactions.size());

            if (totalIncome > 0) {
                double ratio = (totalExpense / totalIncome) * 100;
                if (ratio > 100) {
                    addInsight("⚠️ Расходы превышают доходы на " +
                            String.format(Locale.getDefault(), "%.1f%%", ratio - 100));
                } else {
                    addInsight("💰 Вы откладываете " +
                            String.format(Locale.getDefault(), "%.1f%% доходов", 100 - ratio));
                }
            }

            if (monthTransactions.isEmpty()) {
                addInsight("💡 Добавьте транзакции для анализа");
            }
        }

        private void addInsight(String text) {
            TextView insightView = new TextView(getContext());
            insightView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            insightView.setText("• " + text);
            insightView.setTextSize(14);
            insightView.setTextColor(Color.DKGRAY);
            insightView.setPadding(0, 8, 0, 8);
            insightsContainer.addView(insightView);
        }
    }

    // ============ ФРАГМЕНТ ОТЧЁТОВ ============

    public static class ReportsFragment extends Fragment {

        private MainActivity mainActivity;
        private LinearLayout reportsContainer;
        private Button generatePdfButton;
        private Button exportCsvButton;
        private Button monthlyReportButton;
        private Button yearlyReportButton;
        private Button categoryReportButton;

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_reports, container, false);

            mainActivity = (MainActivity) getActivity();

            initViews(view);
            setupListeners();

            return view;
        }

        private void initViews(View view) {
            reportsContainer = view.findViewById(R.id.reportsContainer);
            generatePdfButton = view.findViewById(R.id.generatePdfButton);
            exportCsvButton = view.findViewById(R.id.exportCsvButton);
            monthlyReportButton = view.findViewById(R.id.monthlyReportButton);
            yearlyReportButton = view.findViewById(R.id.yearlyReportButton);
            categoryReportButton = view.findViewById(R.id.categoryReportButton);
        }

        private void setupListeners() {
            generatePdfButton.setOnClickListener(v -> showComingSoon("PDF отчёты"));
            exportCsvButton.setOnClickListener(v -> showComingSoon("Экспорт в CSV"));
            monthlyReportButton.setOnClickListener(v -> showMonthlyReportDialog());
            yearlyReportButton.setOnClickListener(v -> showYearlyReportDialog());
            categoryReportButton.setOnClickListener(v -> showCategoryReportDialog());
        }

        private void showComingSoon(String feature) {
            Toast.makeText(getContext(), "⚙️ " + feature + " в разработке", Toast.LENGTH_SHORT).show();
        }

        private void showMonthlyReportDialog() {
            if (getContext() == null || mainActivity == null) return;

            Calendar cal = Calendar.getInstance();
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH);

            android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(
                    getContext(),
                    (view, selectedYear, selectedMonth, dayOfMonth) ->
                            generateMonthlyReport(selectedYear, selectedMonth + 1),
                    year, month, 1
            );
            datePickerDialog.show();
        }

        private void generateMonthlyReport(int year, int month) {
            List<Transaction> allTransactions = mainActivity.getTransactions();
            List<Transaction> monthTransactions = new ArrayList<>();

            double totalIncome = 0;
            double totalExpense = 0;

            for (Transaction t : allTransactions) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(t.getDate());
                int tYear = cal.get(Calendar.YEAR);
                int tMonth = cal.get(Calendar.MONTH) + 1;

                if (tYear == year && tMonth == month) {
                    monthTransactions.add(t);
                    if (t.isIncome()) {
                        totalIncome += t.getAmount();
                    } else {
                        totalExpense += t.getAmount();
                    }
                }
            }

            String[] monthNames = {"Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
                    "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"};

            String report = String.format(Locale.getDefault(),
                    "📊 ОТЧЁТ ЗА %s %d\n\n" +
                            "📈 Доходы: %.2f ₽\n" +
                            "📉 Расходы: %.2f ₽\n" +
                            "💰 Баланс: %.2f ₽\n" +
                            "📝 Всего операций: %d\n\n" +
                            "✅ Сгенерирован: %s",
                    monthNames[month - 1], year,
                    totalIncome, totalExpense, totalIncome - totalExpense,
                    monthTransactions.size(),
                    new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(new java.util.Date()));

            showReportDialog("Месячный отчёт", report);
        }

        private void showYearlyReportDialog() {
            if (getContext() == null || mainActivity == null) return;

            Calendar cal = Calendar.getInstance();
            int currentYear = cal.get(Calendar.YEAR);

            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
            builder.setTitle("Выберите год");

            String[] years = new String[5];
            for (int i = 0; i < 5; i++) {
                years[i] = String.valueOf(currentYear - i);
            }

            builder.setItems(years, (dialog, which) -> {
                int selectedYear = currentYear - which;
                generateYearlyReport(selectedYear);
            });

            builder.setNegativeButton("Отмена", null);
            builder.show();
        }

        private void generateYearlyReport(int year) {
            List<Transaction> allTransactions = mainActivity.getTransactions();
            List<Transaction> yearTransactions = new ArrayList<>();

            double totalIncome = 0;
            double totalExpense = 0;

            for (Transaction t : allTransactions) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(t.getDate());
                int tYear = cal.get(Calendar.YEAR);

                if (tYear == year) {
                    yearTransactions.add(t);
                    if (t.isIncome()) {
                        totalIncome += t.getAmount();
                    } else {
                        totalExpense += t.getAmount();
                    }
                }
            }

            String report = String.format(Locale.getDefault(),
                    "📊 ГОДОВОЙ ОТЧЁТ %d\n\n" +
                            "📈 Доходы: %.2f ₽\n" +
                            "📉 Расходы: %.2f ₽\n" +
                            "💰 Баланс: %.2f ₽\n" +
                            "📝 Всего операций: %d\n\n" +
                            "✅ Сгенерирован: %s",
                    year,
                    totalIncome, totalExpense, totalIncome - totalExpense,
                    yearTransactions.size(),
                    new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(new java.util.Date()));

            showReportDialog("Годовой отчёт", report);
        }

        private void showCategoryReportDialog() {
            if (getContext() == null || mainActivity == null) return;

            List<Transaction> allTransactions = mainActivity.getTransactions();

            StringBuilder report = new StringBuilder();
            report.append("📊 ОТЧЁТ ПО КАТЕГОРИЯМ\n\n");

            // Доходы по категориям
            report.append("📈 ДОХОДЫ:\n");
            java.util.Map<String, Double> incomeByCategory = new java.util.HashMap<>();
            double totalIncome = 0;

            for (Transaction t : allTransactions) {
                if (t.isIncome()) {
                    String category = t.getCategoryWithoutIcon();
                    incomeByCategory.put(category,
                            incomeByCategory.getOrDefault(category, 0.0) + t.getAmount());
                    totalIncome += t.getAmount();
                }
            }

            for (java.util.Map.Entry<String, Double> entry : incomeByCategory.entrySet()) {
                double percent = totalIncome > 0 ? (entry.getValue() / totalIncome) * 100 : 0;
                report.append(String.format(Locale.getDefault(),
                        "  • %s: %.2f ₽ (%.1f%%)\n",
                        entry.getKey(), entry.getValue(), percent));
            }

            // Расходы по категориям
            report.append("\n📉 РАСХОДЫ:\n");
            java.util.Map<String, Double> expenseByCategory = new java.util.HashMap<>();
            double totalExpense = 0;

            for (Transaction t : allTransactions) {
                if (!t.isIncome()) {
                    String category = t.getCategoryWithoutIcon();
                    expenseByCategory.put(category,
                            expenseByCategory.getOrDefault(category, 0.0) + t.getAmount());
                    totalExpense += t.getAmount();
                }
            }

            for (java.util.Map.Entry<String, Double> entry : expenseByCategory.entrySet()) {
                double percent = totalExpense > 0 ? (entry.getValue() / totalExpense) * 100 : 0;
                report.append(String.format(Locale.getDefault(),
                        "  • %s: %.2f ₽ (%.1f%%)\n",
                        entry.getKey(), entry.getValue(), percent));
            }

            report.append(String.format(Locale.getDefault(),
                    "\n📊 ИТОГО:\n" +
                            "  Доходы: %.2f ₽\n" +
                            "  Расходы: %.2f ₽\n" +
                            "  Баланс: %.2f ₽",
                    totalIncome, totalExpense, totalIncome - totalExpense));

            showReportDialog("Отчёт по категориям", report.toString());
        }

        private void showReportDialog(String title, String content) {
            if (getContext() == null) return;

            ScrollView scrollView = new ScrollView(getContext());
            TextView textView = new TextView(getContext());
            textView.setPadding(30, 20, 30, 20);
            textView.setText(content);
            textView.setTextSize(14);
            scrollView.addView(textView);

            new android.app.AlertDialog.Builder(getContext())
                    .setTitle(title)
                    .setView(scrollView)
                    .setPositiveButton("OK", null)
                    .setNeutralButton("Экспорт", (dialog, which) ->
                            Toast.makeText(getContext(), "Экспорт в разработке", Toast.LENGTH_SHORT).show())
                    .show();
        }
    }
}