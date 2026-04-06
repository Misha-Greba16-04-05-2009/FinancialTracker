package com.example.financialtracker;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DateUtils {

    // Названия месяцев на русском
    private static final String[] MONTH_NAMES = {
            "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
            "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
    };

    // Короткие названия месяцев
    private static final String[] MONTH_SHORT_NAMES = {
            "Янв", "Фев", "Мар", "Апр", "Май", "Июн",
            "Июл", "Авг", "Сен", "Окт", "Ноя", "Дек"
    };

    // Получить название месяца по номеру (1-12)
    public static String getMonthName(int month) {
        if (month < 1 || month > 12) return "Неизвестно";
        return MONTH_NAMES[month - 1];
    }

    // Получить короткое название месяца по номеру
    public static String getShortMonthName(int month) {
        if (month < 1 || month > 12) return "???";
        return MONTH_SHORT_NAMES[month - 1];
    }

    // Получить номер месяца по названию
    public static int getMonthNumber(String monthName) {
        for (int i = 0; i < MONTH_NAMES.length; i++) {
            if (MONTH_NAMES[i].equals(monthName)) {
                return i + 1;
            }
        }
        return -1;
    }

    // Получить текущий год
    public static int getCurrentYear() {
        Calendar calendar = Calendar.getInstance();
        return calendar.get(Calendar.YEAR);
    }

    // Получить текущий месяц
    public static int getCurrentMonth() {
        Calendar calendar = Calendar.getInstance();
        return calendar.get(Calendar.MONTH) + 1; // Calendar.MONTH начинается с 0
    }

    // Получить список лет из транзакций
    public static List<Integer> getYearsFromTransactions(List<Transaction> transactions) {
        List<Integer> years = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();

        for (Transaction transaction : transactions) {
            calendar.setTime(transaction.getDate());
            int year = calendar.get(Calendar.YEAR);

            if (!years.contains(year)) {
                years.add(year);
            }
        }

        // Сортируем по убыванию (от новых к старым)
        years.sort((a, b) -> b - a);

        // Если нет транзакций, добавляем текущий год
        if (years.isEmpty()) {
            years.add(getCurrentYear());
        }

        return years;
    }

    // Получить статистику по месяцам
    public static Map<String, MonthStats> getMonthlyStats(List<Transaction> transactions, int year) {
        Map<String, MonthStats> stats = new HashMap<>();

        // Инициализируем все месяцы
        for (int month = 1; month <= 12; month++) {
            String key = year + "-" + String.format(Locale.getDefault(), "%02d", month);
            stats.put(key, new MonthStats(year, month));
        }

        // Заполняем статистику из транзакций
        Calendar calendar = Calendar.getInstance();
        for (Transaction transaction : transactions) {
            calendar.setTime(transaction.getDate());
            int transactionYear = calendar.get(Calendar.YEAR);
            int transactionMonth = calendar.get(Calendar.MONTH) + 1;

            if (transactionYear == year) {
                String key = year + "-" + String.format(Locale.getDefault(), "%02d", transactionMonth);
                MonthStats monthStats = stats.get(key);

                if (transaction.isIncome()) {
                    monthStats.totalIncome += transaction.getAmount();
                    monthStats.incomeCount++;
                } else {
                    monthStats.totalExpenses += transaction.getAmount();
                    monthStats.expenseCount++;
                }

                monthStats.transactions.add(transaction);
            }
        }

        return stats;
    }

    // Получить статистику по годам
    public static Map<Integer, YearStats> getYearlyStats(List<Transaction> transactions) {
        Map<Integer, YearStats> stats = new HashMap<>();

        Calendar calendar = Calendar.getInstance();
        for (Transaction transaction : transactions) {
            calendar.setTime(transaction.getDate());
            int year = calendar.get(Calendar.YEAR);

            YearStats yearStats = stats.get(year);
            if (yearStats == null) {
                yearStats = new YearStats(year);
                stats.put(year, yearStats);
            }

            if (transaction.isIncome()) {
                yearStats.totalIncome += transaction.getAmount();
                yearStats.incomeCount++;
            } else {
                yearStats.totalExpenses += transaction.getAmount();
                yearStats.expenseCount++;
            }

            yearStats.transactions.add(transaction);
        }

        return stats;
    }

    // Получить отформатированную дату
    public static String getFormattedDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        return sdf.format(date);
    }

    // Получить отформатированную дату и время
    public static String getFormattedDateTime(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        return sdf.format(date);
    }

    // Класс для статистики за месяц
    public static class MonthStats {
        public int year;
        public int month;
        public double totalIncome;
        public double totalExpenses;
        public int incomeCount;
        public int expenseCount;
        public List<Transaction> transactions;

        public MonthStats(int year, int month) {
            this.year = year;
            this.month = month;
            this.totalIncome = 0;
            this.totalExpenses = 0;
            this.incomeCount = 0;
            this.expenseCount = 0;
            this.transactions = new ArrayList<>();
        }

        public String getMonthName() {
            return DateUtils.getMonthName(month);
        }

        public double getBalance() {
            return totalIncome - totalExpenses;
        }

        public String getFormattedBalance() {
            return String.format(Locale.getDefault(), "%.2f руб.", getBalance());
        }

        public String getFormattedTotalIncome() {
            return String.format(Locale.getDefault(), "%.2f руб.", totalIncome);
        }

        public String getFormattedTotalExpenses() {
            return String.format(Locale.getDefault(), "%.2f руб.", totalExpenses);
        }
    }

    // Класс для статистики за год
    public static class YearStats {
        public int year;
        public double totalIncome;
        public double totalExpenses;
        public int incomeCount;
        public int expenseCount;
        public List<Transaction> transactions;

        public YearStats(int year) {
            this.year = year;
            this.totalIncome = 0;
            this.totalExpenses = 0;
            this.incomeCount = 0;
            this.expenseCount = 0;
            this.transactions = new ArrayList<>();
        }

        public double getBalance() {
            return totalIncome - totalExpenses;
        }

        public String getFormattedBalance() {
            return String.format(Locale.getDefault(), "%.2f руб.", getBalance());
        }

        public String getFormattedTotalIncome() {
            return String.format(Locale.getDefault(), "%.2f руб.", totalIncome);
        }

        public String getFormattedTotalExpenses() {
            return String.format(Locale.getDefault(), "%.2f руб.", totalExpenses);
        }
    }
}