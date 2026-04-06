package com.example.financialtracker;

import android.util.Log;

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

public class AnalyticsEngine {
    private static final String TAG = "AnalyticsEngine";

    private DataManager dataManager;

    public AnalyticsEngine(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    // ==================== ОСНОВНОЙ АНАЛИЗ ====================

    public AnalyticsData analyzePeriod(Date startDate, Date endDate, String periodType) {
        AnalyticsData data = new AnalyticsData();
        data.setStartDate(startDate);
        data.setEndDate(endDate);
        data.setPeriodType(periodType);

        List<Transaction> transactions = dataManager.loadTransactions();
        List<Transaction> periodTransactions = filterTransactionsByDate(transactions, startDate, endDate);

        // Базовая статистика
        calculateBasicStats(data, periodTransactions);

        // Анализ по категориям
        calculateCategoryStats(data, periodTransactions);

        // Сравнение с прошлым периодом
        calculateComparison(data, transactions, startDate, endDate);

        // Анализ привычек
        analyzeHabits(data, transactions);

        // Прогноз баланса
        predictBalance(data, transactions);

        // Ежедневные/еженедельные данные
        generateDailyData(data, periodTransactions, startDate, endDate);

        return data;
    }

    public AnalyticsData analyzeMonth(int year, int month) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, 1, 0, 0, 0);
        Date startDate = cal.getTime();

        cal.set(year, month - 1, cal.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);
        Date endDate = cal.getTime();

        return analyzePeriod(startDate, endDate, "month");
    }

    public AnalyticsData analyzeWeek(int year, int week) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.WEEK_OF_YEAR, week);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        Date startDate = cal.getTime();

        cal.add(Calendar.DAY_OF_WEEK, 6);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        Date endDate = cal.getTime();

        return analyzePeriod(startDate, endDate, "week");
    }

    public AnalyticsData analyzeYear(int year) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, 0, 1, 0, 0, 0);
        Date startDate = cal.getTime();

        cal.set(year, 11, 31, 23, 59, 59);
        Date endDate = cal.getTime();

        return analyzePeriod(startDate, endDate, "year");
    }

    // ==================== ФИЛЬТРАЦИЯ ====================

    private List<Transaction> filterTransactionsByDate(List<Transaction> transactions,
                                                       Date startDate, Date endDate) {
        List<Transaction> filtered = new ArrayList<>();

        for (Transaction transaction : transactions) {
            Date date = transaction.getDate();
            if ((date.equals(startDate) || date.after(startDate)) &&
                    (date.equals(endDate) || date.before(endDate))) {
                filtered.add(transaction);
            }
        }

        return filtered;
    }

    // ==================== БАЗОВАЯ СТАТИСТИКА ====================

    private void calculateBasicStats(AnalyticsData data, List<Transaction> transactions) {
        double totalIncome = 0;
        double totalExpense = 0;

        for (Transaction transaction : transactions) {
            if (transaction.isIncome()) {
                totalIncome += transaction.getAmount();
            } else {
                totalExpense += transaction.getAmount();
            }
        }

        data.setTotalIncome(totalIncome);
        data.setTotalExpense(totalExpense);
        data.setBalance(totalIncome - totalExpense);
        data.setTransactionCount(transactions.size());
    }

    // ==================== СТАТИСТИКА ПО КАТЕГОРИЯМ ====================

    private void calculateCategoryStats(AnalyticsData data, List<Transaction> transactions) {
        Map<String, Double> incomeByCategory = new HashMap<>();
        Map<String, Double> expenseByCategory = new HashMap<>();
        Map<String, Integer> incomeCountByCategory = new HashMap<>();
        Map<String, Integer> expenseCountByCategory = new HashMap<>();

        for (Transaction transaction : transactions) {
            String category = transaction.getCategory();

            if (transaction.isIncome()) {
                incomeByCategory.put(category,
                        incomeByCategory.getOrDefault(category, 0.0) + transaction.getAmount());
                incomeCountByCategory.put(category,
                        incomeCountByCategory.getOrDefault(category, 0) + 1);
            } else {
                expenseByCategory.put(category,
                        expenseByCategory.getOrDefault(category, 0.0) + transaction.getAmount());
                expenseCountByCategory.put(category,
                        expenseCountByCategory.getOrDefault(category, 0) + 1);
            }
        }

        data.setIncomeByCategory(incomeByCategory);
        data.setExpenseByCategory(expenseByCategory);
        data.setIncomeCountByCategory(incomeCountByCategory);
        data.setExpenseCountByCategory(expenseCountByCategory);
    }

    // ==================== СРАВНЕНИЕ С ПРОШЛЫМ ПЕРИОДОМ ====================

    private void calculateComparison(AnalyticsData data, List<Transaction> allTransactions,
                                     Date startDate, Date endDate) {
        // Вычисляем предыдущий период такой же длительности
        long periodLength = endDate.getTime() - startDate.getTime();
        Date previousStartDate = new Date(startDate.getTime() - periodLength);
        Date previousEndDate = new Date(startDate.getTime() - 1);

        List<Transaction> previousTransactions = filterTransactionsByDate(
                allTransactions, previousStartDate, previousEndDate);

        double previousIncome = 0;
        double previousExpense = 0;

        for (Transaction transaction : previousTransactions) {
            if (transaction.isIncome()) {
                previousIncome += transaction.getAmount();
            } else {
                previousExpense += transaction.getAmount();
            }
        }

        data.setPreviousIncome(previousIncome);
        data.setPreviousExpense(previousExpense);

        // Вычисляем процент изменения
        if (previousIncome > 0) {
            double change = ((data.getTotalIncome() - previousIncome) / previousIncome) * 100;
            data.setIncomeChangePercent(change);
        }

        if (previousExpense > 0) {
            double change = ((data.getTotalExpense() - previousExpense) / previousExpense) * 100;
            data.setExpenseChangePercent(change);
        }

        double previousBalance = previousIncome - previousExpense;
        double currentBalance = data.getBalance();
        if (previousBalance != 0) {
            double change = ((currentBalance - previousBalance) / Math.abs(previousBalance)) * 100;
            data.setBalanceChangePercent(change);
        }
    }

    // ==================== АНАЛИЗ ПРИВЫЧЕК ====================

    private void analyzeHabits(AnalyticsData data, List<Transaction> allTransactions) {
        List<AnalyticsData.HabitInsight> habits = new ArrayList<>();

        // Анализ еженедельных паттернов
        habits.addAll(analyzeWeeklyPatterns(allTransactions));

        // Анализ ежедневных паттернов
        habits.addAll(analyzeDailyPatterns(allTransactions));

        // Анализ категорий с высокими тратами
        habits.addAll(analyzeHighSpendingCategories(allTransactions));

        // Анализ импульсивных покупок
        habits.addAll(analyzeImpulsivePurchases(allTransactions));

        // Анализ регулярных доходов
        habits.addAll(analyzeRegularIncome(allTransactions));

        // Сортируем по важности
        Collections.sort(habits, (h1, h2) -> Integer.compare(h2.getImportance(), h1.getImportance()));

        // Ограничиваем количество
        if (habits.size() > 10) {
            habits = habits.subList(0, 10);
        }

        data.setHabits(habits);
    }

    private List<AnalyticsData.HabitInsight> analyzeWeeklyPatterns(List<Transaction> transactions) {
        List<AnalyticsData.HabitInsight> habits = new ArrayList<>();

        // Анализ трат по дням недели
        Map<Integer, Double> expenseByDayOfWeek = new HashMap<>();
        Map<Integer, Integer> countByDayOfWeek = new HashMap<>();

        for (Transaction transaction : transactions) {
            if (!transaction.isIncome()) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(transaction.getDate());
                int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

                expenseByDayOfWeek.put(dayOfWeek,
                        expenseByDayOfWeek.getOrDefault(dayOfWeek, 0.0) + transaction.getAmount());
                countByDayOfWeek.put(dayOfWeek,
                        countByDayOfWeek.getOrDefault(dayOfWeek, 0) + 1);
            }
        }

        String[] dayNames = {"Воскресенье", "Понедельник", "Вторник", "Среда",
                "Четверг", "Пятница", "Суббота"};

        for (Map.Entry<Integer, Double> entry : expenseByDayOfWeek.entrySet()) {
            int day = entry.getKey();
            double total = entry.getValue();
            int count = countByDayOfWeek.getOrDefault(day, 0);

            if (count >= 3) { // Минимум 3 транзакции для паттерна
                double average = total / count;
                double otherDaysAvg = getAverageExpenseForOtherDays(expenseByDayOfWeek, day, countByDayOfWeek);

                if (average > otherDaysAvg * 1.5 && otherDaysAvg > 0) {
                    String title = "Высокие траты по " + dayNames[day-1];
                    String description = String.format(Locale.getDefault(),
                            "По %s вы тратите в среднем %.2f руб., что на %.0f%% больше, чем в другие дни.",
                            dayNames[day-1].toLowerCase(), average,
                            ((average / otherDaysAvg) - 1) * 100);

                    habits.add(new AnalyticsData.HabitInsight(
                            title, description, "Расходы",
                            "Еженедельный паттерн", average, count,
                            "Попробуйте планировать бюджет на этот день заранее.", 4
                    ));
                }
            }
        }

        return habits;
    }

    private double getAverageExpenseForOtherDays(Map<Integer, Double> expenseByDayOfWeek,
                                                 int excludeDay,
                                                 Map<Integer, Integer> countByDayOfWeek) {
        double total = 0;
        int count = 0;

        for (Map.Entry<Integer, Double> entry : expenseByDayOfWeek.entrySet()) {
            if (entry.getKey() != excludeDay) {
                total += entry.getValue();
                count += countByDayOfWeek.getOrDefault(entry.getKey(), 0);
            }
        }

        return count > 0 ? total / count : 0;
    }

    private List<AnalyticsData.HabitInsight> analyzeDailyPatterns(List<Transaction> transactions) {
        List<AnalyticsData.HabitInsight> habits = new ArrayList<>();

        // Анализ трат по часам
        Map<Integer, Double> expenseByHour = new HashMap<>();
        Map<Integer, Integer> countByHour = new HashMap<>();

        for (Transaction transaction : transactions) {
            if (!transaction.isIncome()) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(transaction.getDate());
                int hour = cal.get(Calendar.HOUR_OF_DAY);

                expenseByHour.put(hour,
                        expenseByHour.getOrDefault(hour, 0.0) + transaction.getAmount());
                countByHour.put(hour,
                        countByHour.getOrDefault(hour, 0) + 1);
            }
        }

        for (Map.Entry<Integer, Double> entry : expenseByHour.entrySet()) {
            int hour = entry.getKey();
            double total = entry.getValue();
            int count = countByHour.getOrDefault(hour, 0);

            if (count >= 3) {
                double average = total / count;
                double otherHoursAvg = getAverageExpenseForOtherHours(expenseByHour, hour, countByHour);

                if (average > otherHoursAvg * 2 && otherHoursAvg > 0) {
                    String timeRange;
                    if (hour >= 0 && hour < 6) timeRange = "ночью";
                    else if (hour >= 6 && hour < 12) timeRange = "утром";
                    else if (hour >= 12 && hour < 18) timeRange = "днем";
                    else timeRange = "вечером";

                    String title = "Импульсивные покупки " + timeRange;
                    String description = String.format(Locale.getDefault(),
                            "Вы часто совершаете покупки %s в %d часов. Средний чек: %.2f руб.",
                            timeRange, hour, average);

                    habits.add(new AnalyticsData.HabitInsight(
                            title, description, "Расходы",
                            "Временной паттерн", average, count,
                            "Попробуйте откладывать покупки на следующий день.", 3
                    ));
                }
            }
        }

        return habits;
    }

    private double getAverageExpenseForOtherHours(Map<Integer, Double> expenseByHour,
                                                  int excludeHour,
                                                  Map<Integer, Integer> countByHour) {
        double total = 0;
        int count = 0;

        for (Map.Entry<Integer, Double> entry : expenseByHour.entrySet()) {
            if (entry.getKey() != excludeHour) {
                total += entry.getValue();
                count += countByHour.getOrDefault(entry.getKey(), 0);
            }
        }

        return count > 0 ? total / count : 0;
    }

    private List<AnalyticsData.HabitInsight> analyzeHighSpendingCategories(List<Transaction> transactions) {
        List<AnalyticsData.HabitInsight> habits = new ArrayList<>();

        Map<String, Double> expenseByCategory = new HashMap<>();
        Map<String, Integer> countByCategory = new HashMap<>();

        for (Transaction transaction : transactions) {
            if (!transaction.isIncome()) {
                String category = transaction.getCategoryWithoutIcon();
                expenseByCategory.put(category,
                        expenseByCategory.getOrDefault(category, 0.0) + transaction.getAmount());
                countByCategory.put(category,
                        countByCategory.getOrDefault(category, 0) + 1);
            }
        }

        double totalExpense = expenseByCategory.values().stream().mapToDouble(Double::doubleValue).sum();

        for (Map.Entry<String, Double> entry : expenseByCategory.entrySet()) {
            String category = entry.getKey();
            double amount = entry.getValue();
            int count = countByCategory.getOrDefault(category, 0);
            double percent = (amount / totalExpense) * 100;

            if (percent > 30 && count >= 3) {
                String title = "Высокая доля расходов на " + category;
                String description = String.format(Locale.getDefault(),
                        "На '%s' вы тратите %.1f%% всех расходов (%.2f руб.)",
                        category, percent, amount);

                habits.add(new AnalyticsData.HabitInsight(
                        title, description, category,
                        "Категория расходов", amount / count, count,
                        "Попробуйте установить бюджет на эту категорию.", 5
                ));
            }
        }

        return habits;
    }

    private List<AnalyticsData.HabitInsight> analyzeImpulsivePurchases(List<Transaction> transactions) {
        List<AnalyticsData.HabitInsight> habits = new ArrayList<>();

        // Категории, которые часто бывают импульсивными
        String[] impulsiveCategories = {"Развлечения", "Кафе", "Рестораны", "Покупки", "Хобби"};

        for (String category : impulsiveCategories) {
            double total = 0;
            int count = 0;

            for (Transaction transaction : transactions) {
                if (!transaction.isIncome() &&
                        transaction.getCategoryWithoutIcon().equalsIgnoreCase(category)) {
                    total += transaction.getAmount();
                    count++;
                }
            }

            if (count >= 5) {
                double average = total / count;
                String title = "Регулярные траты на " + category;
                String description = String.format(Locale.getDefault(),
                        "Вы совершили %d покупок в категории '%s' на общую сумму %.2f руб.",
                        count, category, total);

                habits.add(new AnalyticsData.HabitInsight(
                        title, description, category,
                        "Импульсивные покупки", average, count,
                        "Попробуйте установить дневной лимит на эту категорию.", 4
                ));
            }
        }

        return habits;
    }

    private List<AnalyticsData.HabitInsight> analyzeRegularIncome(List<Transaction> transactions) {
        List<AnalyticsData.HabitInsight> habits = new ArrayList<>();

        Map<String, List<Date>> incomeDatesByCategory = new HashMap<>();

        for (Transaction transaction : transactions) {
            if (transaction.isIncome()) {
                String category = transaction.getCategoryWithoutIcon();
                if (!incomeDatesByCategory.containsKey(category)) {
                    incomeDatesByCategory.put(category, new ArrayList<>());
                }
                incomeDatesByCategory.get(category).add(transaction.getDate());
            }
        }

        for (Map.Entry<String, List<Date>> entry : incomeDatesByCategory.entrySet()) {
            String category = entry.getKey();
            List<Date> dates = entry.getValue();

            if (dates.size() >= 3) {
                // Проверяем регулярность (примерно одинаковые интервалы)
                boolean isRegular = checkRegularIntervals(dates);

                if (isRegular) {
                    String title = "Регулярный доход: " + category;
                    String description = "У вас регулярные поступления в категории '" + category + "'";

                    habits.add(new AnalyticsData.HabitInsight(
                            title, description, category,
                            "Регулярный доход", 0, dates.size(),
                            "Отлично! Регулярный доход помогает планировать бюджет.", 3
                    ));
                }
            }
        }

        return habits;
    }

    private boolean checkRegularIntervals(List<Date> dates) {
        if (dates.size() < 3) return false;

        Collections.sort(dates);
        List<Long> intervals = new ArrayList<>();

        for (int i = 1; i < dates.size(); i++) {
            intervals.add(dates.get(i).getTime() - dates.get(i-1).getTime());
        }

        // Проверяем, что интервалы примерно одинаковые (разброс не более 20%)
        long avgInterval = intervals.stream().mapToLong(Long::longValue).sum() / intervals.size();

        for (long interval : intervals) {
            double diff = Math.abs(interval - avgInterval) / (double) avgInterval;
            if (diff > 0.2) { // Разброс более 20%
                return false;
            }
        }

        return true;
    }

    // ==================== ПРОГНОЗ БАЛАНСА ====================

    private void predictBalance(AnalyticsData data, List<Transaction> allTransactions) {
        // Используем данные за последние 90 дней для прогноза
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -90);
        Date startDate = cal.getTime();
        Date endDate = new Date();

        List<Transaction> recentTransactions = filterTransactionsByDate(
                allTransactions, startDate, endDate);

        // Вычисляем средние ежедневные доходы и расходы
        double dailyIncome = 0;
        double dailyExpense = 0;
        int daysWithData = 0;

        Map<Date, Double> incomeByDay = new HashMap<>();
        Map<Date, Double> expenseByDay = new HashMap<>();

        for (Transaction transaction : recentTransactions) {
            Date date = transaction.getDate();
            if (transaction.isIncome()) {
                incomeByDay.put(date, incomeByDay.getOrDefault(date, 0.0) + transaction.getAmount());
            } else {
                expenseByDay.put(date, expenseByDay.getOrDefault(date, 0.0) + transaction.getAmount());
            }
        }

        daysWithData = incomeByDay.size() + expenseByDay.size();

        if (daysWithData > 0) {
            double totalIncome = incomeByDay.values().stream().mapToDouble(Double::doubleValue).sum();
            double totalExpense = expenseByDay.values().stream().mapToDouble(Double::doubleValue).sum();

            dailyIncome = totalIncome / 90;
            dailyExpense = totalExpense / 90;

            // Прогноз на 30 дней вперед
            int predictionDays = 30;
            double currentBalance = dataManager.getBalance();
            double predictedBalance = currentBalance + (dailyIncome - dailyExpense) * predictionDays;

            data.setPredictedBalance(predictedBalance);
            data.setPredictedIncome(dailyIncome * predictionDays);
            data.setPredictedExpense(dailyExpense * predictionDays);
            data.setPredictionDays(predictionDays);
        }
    }

    // ==================== ЕЖЕДНЕВНЫЕ ДАННЫЕ ====================

    private void generateDailyData(AnalyticsData data, List<Transaction> transactions,
                                   Date startDate, Date endDate) {
        List<AnalyticsData.DailyData> dailyData = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        cal.setTime(startDate);

        while (!cal.getTime().after(endDate)) {
            Date currentDate = cal.getTime();
            double dailyIncome = 0;
            double dailyExpense = 0;

            for (Transaction transaction : transactions) {
                if (isSameDay(transaction.getDate(), currentDate)) {
                    if (transaction.isIncome()) {
                        dailyIncome += transaction.getAmount();
                    } else {
                        dailyExpense += transaction.getAmount();
                    }
                }
            }

            dailyData.add(new AnalyticsData.DailyData(
                    currentDate, dailyIncome, dailyExpense, dailyIncome - dailyExpense));

            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        data.setDailyData(dailyData);
    }

    private boolean isSameDay(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }
}