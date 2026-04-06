package com.example.financialtracker;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AnalyticsData implements Serializable {
    private Date startDate;
    private Date endDate;
    private String periodType; // "day", "week", "month", "year", "custom"

    // Общая статистика
    private double totalIncome;
    private double totalExpense;
    private double balance;
    private int transactionCount;

    // Сравнение с прошлым периодом
    private double previousIncome;
    private double previousExpense;
    private double incomeChangePercent;
    private double expenseChangePercent;
    private double balanceChangePercent;

    // Категории
    private Map<String, Double> incomeByCategory;
    private Map<String, Double> expenseByCategory;
    private Map<String, Integer> incomeCountByCategory;
    private Map<String, Integer> expenseCountByCategory;

    // Привычки
    private List<HabitInsight> habits;

    // Прогноз
    private double predictedBalance;
    private double predictedIncome;
    private double predictedExpense;
    private int predictionDays;

    // Ежедневные данные для графиков
    private List<DailyData> dailyData;
    private List<WeeklyData> weeklyData;
    private List<MonthlyData> monthlyData;

    public AnalyticsData() {
        this.incomeByCategory = new HashMap<>();
        this.expenseByCategory = new HashMap<>();
        this.incomeCountByCategory = new HashMap<>();
        this.expenseCountByCategory = new HashMap<>();
        this.habits = new ArrayList<>();
        this.dailyData = new ArrayList<>();
        this.weeklyData = new ArrayList<>();
        this.monthlyData = new ArrayList<>();
    }

    // Getters and Setters
    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }

    public Date getEndDate() { return endDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }

    public String getPeriodType() { return periodType; }
    public void setPeriodType(String periodType) { this.periodType = periodType; }

    public String getFormattedPeriod() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        if (periodType.equals("day")) {
            return sdf.format(startDate);
        } else if (periodType.equals("week")) {
            return "Неделя " + sdf.format(startDate) + " - " + sdf.format(endDate);
        } else if (periodType.equals("month")) {
            SimpleDateFormat monthFormat = new SimpleDateFormat("LLLL yyyy", Locale.getDefault());
            return monthFormat.format(startDate);
        } else if (periodType.equals("year")) {
            SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy", Locale.getDefault());
            return yearFormat.format(startDate);
        } else {
            return sdf.format(startDate) + " - " + sdf.format(endDate);
        }
    }

    public double getTotalIncome() { return totalIncome; }
    public void setTotalIncome(double totalIncome) { this.totalIncome = totalIncome; }

    public double getTotalExpense() { return totalExpense; }
    public void setTotalExpense(double totalExpense) { this.totalExpense = totalExpense; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public int getTransactionCount() { return transactionCount; }
    public void setTransactionCount(int transactionCount) { this.transactionCount = transactionCount; }

    public double getPreviousIncome() { return previousIncome; }
    public void setPreviousIncome(double previousIncome) { this.previousIncome = previousIncome; }

    public double getPreviousExpense() { return previousExpense; }
    public void setPreviousExpense(double previousExpense) { this.previousExpense = previousExpense; }

    public double getIncomeChangePercent() { return incomeChangePercent; }
    public void setIncomeChangePercent(double incomeChangePercent) { this.incomeChangePercent = incomeChangePercent; }

    public double getExpenseChangePercent() { return expenseChangePercent; }
    public void setExpenseChangePercent(double expenseChangePercent) { this.expenseChangePercent = expenseChangePercent; }

    public double getBalanceChangePercent() { return balanceChangePercent; }
    public void setBalanceChangePercent(double balanceChangePercent) { this.balanceChangePercent = balanceChangePercent; }

    public Map<String, Double> getIncomeByCategory() { return incomeByCategory; }
    public void setIncomeByCategory(Map<String, Double> incomeByCategory) { this.incomeByCategory = incomeByCategory; }

    public Map<String, Double> getExpenseByCategory() { return expenseByCategory; }
    public void setExpenseByCategory(Map<String, Double> expenseByCategory) { this.expenseByCategory = expenseByCategory; }

    public Map<String, Integer> getIncomeCountByCategory() { return incomeCountByCategory; }
    public void setIncomeCountByCategory(Map<String, Integer> incomeCountByCategory) { this.incomeCountByCategory = incomeCountByCategory; }

    public Map<String, Integer> getExpenseCountByCategory() { return expenseCountByCategory; }
    public void setExpenseCountByCategory(Map<String, Integer> expenseCountByCategory) { this.expenseCountByCategory = expenseCountByCategory; }

    public List<HabitInsight> getHabits() { return habits; }
    public void setHabits(List<HabitInsight> habits) { this.habits = habits; }
    public void addHabit(HabitInsight habit) { this.habits.add(habit); }

    public double getPredictedBalance() { return predictedBalance; }
    public void setPredictedBalance(double predictedBalance) { this.predictedBalance = predictedBalance; }

    public double getPredictedIncome() { return predictedIncome; }
    public void setPredictedIncome(double predictedIncome) { this.predictedIncome = predictedIncome; }

    public double getPredictedExpense() { return predictedExpense; }
    public void setPredictedExpense(double predictedExpense) { this.predictedExpense = predictedExpense; }

    public int getPredictionDays() { return predictionDays; }
    public void setPredictionDays(int predictionDays) { this.predictionDays = predictionDays; }

    public List<DailyData> getDailyData() { return dailyData; }
    public void setDailyData(List<DailyData> dailyData) { this.dailyData = dailyData; }
    public void addDailyData(DailyData data) { this.dailyData.add(data); }

    public List<WeeklyData> getWeeklyData() { return weeklyData; }
    public void setWeeklyData(List<WeeklyData> weeklyData) { this.weeklyData = weeklyData; }

    public List<MonthlyData> getMonthlyData() { return monthlyData; }
    public void setMonthlyData(List<MonthlyData> monthlyData) { this.monthlyData = monthlyData; }

    // Вспомогательные методы
    public String getIncomeChangeText() {
        if (incomeChangePercent > 0) {
            return String.format(Locale.getDefault(), "📈 Доходы выросли на %.1f%%", incomeChangePercent);
        } else if (incomeChangePercent < 0) {
            return String.format(Locale.getDefault(), "📉 Доходы упали на %.1f%%", Math.abs(incomeChangePercent));
        } else {
            return "📊 Доходы не изменились";
        }
    }

    public String getExpenseChangeText() {
        if (expenseChangePercent > 0) {
            return String.format(Locale.getDefault(), "⚠️ Расходы выросли на %.1f%%", expenseChangePercent);
        } else if (expenseChangePercent < 0) {
            return String.format(Locale.getDefault(), "✅ Расходы сократились на %.1f%%", Math.abs(expenseChangePercent));
        } else {
            return "📊 Расходы не изменились";
        }
    }

    public String getTopExpenseCategory() {
        String topCategory = "";
        double maxAmount = 0;
        for (Map.Entry<String, Double> entry : expenseByCategory.entrySet()) {
            if (entry.getValue() > maxAmount) {
                maxAmount = entry.getValue();
                topCategory = entry.getKey();
            }
        }
        if (!topCategory.isEmpty()) {
            double percent = (maxAmount / totalExpense) * 100;
            return String.format(Locale.getDefault(),
                    "💰 Больше всего тратите на %s: %.2f руб. (%.1f%%)",
                    getCategoryName(topCategory), maxAmount, percent);
        }
        return "💰 Нет данных по расходам";
    }

    public String getTopIncomeCategory() {
        String topCategory = "";
        double maxAmount = 0;
        for (Map.Entry<String, Double> entry : incomeByCategory.entrySet()) {
            if (entry.getValue() > maxAmount) {
                maxAmount = entry.getValue();
                topCategory = entry.getKey();
            }
        }
        if (!topCategory.isEmpty()) {
            double percent = (maxAmount / totalIncome) * 100;
            return String.format(Locale.getDefault(),
                    "📈 Основной доход от %s: %.2f руб. (%.1f%%)",
                    getCategoryName(topCategory), maxAmount, percent);
        }
        return "📈 Нет данных по доходам";
    }

    private String getCategoryName(String category) {
        if (category.contains(" ")) {
            return category.substring(category.indexOf(" ") + 1);
        }
        return category;
    }

    public String getPredictionText() {
        double difference = predictedBalance - balance;
        if (difference > 0) {
            return String.format(Locale.getDefault(),
                    "📈 Через %d дней баланс вырастет на %.2f руб. и составит %.2f руб.",
                    predictionDays, difference, predictedBalance);
        } else if (difference < 0) {
            return String.format(Locale.getDefault(),
                    "⚠️ Через %d дней баланс упадет на %.2f руб. и составит %.2f руб.",
                    predictionDays, Math.abs(difference), predictedBalance);
        } else {
            return String.format(Locale.getDefault(),
                    "📊 Через %d дней баланс останется на уровне %.2f руб.",
                    predictionDays, predictedBalance);
        }
    }

    // Внутренние классы для данных
    public static class DailyData implements Serializable {
        private Date date;
        private double income;
        private double expense;
        private double balance;

        public DailyData(Date date, double income, double expense, double balance) {
            this.date = date;
            this.income = income;
            this.expense = expense;
            this.balance = balance;
        }

        public Date getDate() { return date; }
        public double getIncome() { return income; }
        public double getExpense() { return expense; }
        public double getBalance() { return balance; }

        public String getFormattedDate() {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM", Locale.getDefault());
            return sdf.format(date);
        }
    }

    public static class WeeklyData implements Serializable {
        private int weekNumber;
        private Date startDate;
        private Date endDate;
        private double income;
        private double expense;
        private double balance;

        public WeeklyData(int weekNumber, Date startDate, Date endDate,
                          double income, double expense, double balance) {
            this.weekNumber = weekNumber;
            this.startDate = startDate;
            this.endDate = endDate;
            this.income = income;
            this.expense = expense;
            this.balance = balance;
        }

        public int getWeekNumber() { return weekNumber; }
        public Date getStartDate() { return startDate; }
        public Date getEndDate() { return endDate; }
        public double getIncome() { return income; }
        public double getExpense() { return expense; }
        public double getBalance() { return balance; }

        public String getFormattedWeek() {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM", Locale.getDefault());
            return "Неделя " + weekNumber + ": " + sdf.format(startDate) + "-" + sdf.format(endDate);
        }
    }

    public static class MonthlyData implements Serializable {
        private int month;
        private int year;
        private double income;
        private double expense;
        private double balance;

        public MonthlyData(int month, int year, double income, double expense, double balance) {
            this.month = month;
            this.year = year;
            this.income = income;
            this.expense = expense;
            this.balance = balance;
        }

        public int getMonth() { return month; }
        public int getYear() { return year; }
        public double getIncome() { return income; }
        public double getExpense() { return expense; }
        public double getBalance() { return balance; }

        public String getFormattedMonth() {
            String[] monthNames = {"Янв", "Фев", "Мар", "Апр", "Май", "Июн",
                    "Июл", "Авг", "Сен", "Окт", "Ноя", "Дек"};
            return monthNames[month - 1] + " " + year;
        }
    }

    public static class HabitInsight implements Serializable {
        private String title;
        private String description;
        private String category;
        private String pattern;
        private double averageAmount;
        private int frequency;
        private String recommendation;
        private int importance; // 1-5

        public HabitInsight(String title, String description, String category,
                            String pattern, double averageAmount, int frequency,
                            String recommendation, int importance) {
            this.title = title;
            this.description = description;
            this.category = category;
            this.pattern = pattern;
            this.averageAmount = averageAmount;
            this.frequency = frequency;
            this.recommendation = recommendation;
            this.importance = importance;
        }

        // Getters
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getCategory() { return category; }
        public String getPattern() { return pattern; }
        public double getAverageAmount() { return averageAmount; }
        public int getFrequency() { return frequency; }
        public String getRecommendation() { return recommendation; }
        public int getImportance() { return importance; }

        public String getFormattedAmount() {
            return String.format(Locale.getDefault(), "%.2f руб.", averageAmount);
        }

        public int getColor() {
            switch (importance) {
                case 5: return 0xFFF44336; // Красный
                case 4: return 0xFFFF9800; // Оранжевый
                case 3: return 0xFFFFEB3B; // Желтый
                case 2: return 0xFF2196F3; // Синий
                default: return 0xFF4CAF50; // Зеленый
            }
        }
    }
}