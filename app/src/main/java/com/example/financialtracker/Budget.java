package com.example.financialtracker;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Budget implements Serializable {
    private String id;
    private String category;
    private double amount;
    private double spent;
    private String period;
    private Date startDate;
    private Date endDate;
    private boolean active;
    private boolean exceededNotified;
    private Date lastResetDate;

    public Budget(String category, double amount, String period) {
        this.id = generateId();
        this.category = category;
        this.amount = amount;
        this.spent = 0.0;
        this.period = period;
        this.active = true;
        this.exceededNotified = false;
        setDefaultDates();
    }

    public Budget(String category, double amount, Date startDate, Date endDate) {
        this.id = generateId();
        this.category = category;
        this.amount = amount;
        this.spent = 0.0;
        this.period = "произвольный";
        this.startDate = startDate;
        this.endDate = endDate;
        this.active = true;
        this.exceededNotified = false;
    }

    private String generateId() {
        return "budget_" + System.currentTimeMillis() + "_" + Math.random();
    }

    private void setDefaultDates() {
        if (period == null) period = "месяц";
        refreshCurrentPeriod();
    }

    /**
     * Для бюджетов "неделя / месяц / год" выставляет текущий календарный период:
     * неделя — с понедельника по воскресенье, месяц — с 1-го по последнее число, год — с 1 января.
     * Произвольный период не меняется.
     */
    public void refreshCurrentPeriod() {
        if ("произвольный".equals(period)) return;
        Calendar start = Calendar.getInstance();
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        Calendar end = (Calendar) start.clone();
        switch (period) {
            case "неделя": {
                int diff = (start.get(Calendar.DAY_OF_WEEK) + 5) % 7; // сколько дней прошло с понедельника
                start.add(Calendar.DAY_OF_MONTH, -diff);
                end = (Calendar) start.clone();
                end.add(Calendar.DAY_OF_MONTH, 7);
                break;
            }
            case "год":
                start.set(Calendar.DAY_OF_YEAR, 1);
                end = (Calendar) start.clone();
                end.add(Calendar.YEAR, 1);
                break;
            case "месяц":
            default:
                start.set(Calendar.DAY_OF_MONTH, 1);
                end = (Calendar) start.clone();
                end.add(Calendar.MONTH, 1);
                break;
        }
        end.add(Calendar.MILLISECOND, -1);
        this.startDate = start.getTime();
        this.endDate = end.getTime();
    }

    /** Попадает ли дата в период бюджета (границы включительно, по целым дням). */
    public boolean containsDate(Date date) {
        if (date == null) return false;
        if (startDate == null || endDate == null) refreshCurrentPeriod();
        if (startDate == null || endDate == null) return true;
        Calendar s = Calendar.getInstance();
        s.setTime(startDate);
        s.set(Calendar.HOUR_OF_DAY, 0); s.set(Calendar.MINUTE, 0);
        s.set(Calendar.SECOND, 0); s.set(Calendar.MILLISECOND, 0);
        Calendar e = Calendar.getInstance();
        e.setTime(endDate);
        e.set(Calendar.HOUR_OF_DAY, 23); e.set(Calendar.MINUTE, 59);
        e.set(Calendar.SECOND, 59); e.set(Calendar.MILLISECOND, 999);
        long t = date.getTime();
        return t >= s.getTimeInMillis() && t <= e.getTimeInMillis();
    }

    /** Сравнение категорий без учёта эмодзи-иконки, регистра и "ё/е". */
    public boolean matchesCategory(String transactionCategory) {
        if (transactionCategory == null || category == null) return false;
        return normalizeCategory(transactionCategory).equals(normalizeCategory(category));
    }

    private static String normalizeCategory(String c) {
        String s = c.trim();
        if (s.contains(" ")) {
            String first = s.substring(0, s.indexOf(' '));
            boolean hasLetter = false;
            for (char ch : first.toCharArray()) if (Character.isLetterOrDigit(ch)) { hasLetter = true; break; }
            if (!hasLetter) s = s.substring(s.indexOf(' ') + 1); // убираем иконку
        }
        return s.toLowerCase(Locale.ROOT).replace('ё', 'е').trim();
    }

    // ИСПРАВЛЕННЫЙ МЕТОД
    public void restoreDate() {
        if (startDate == null || endDate == null) {
            setDefaultDates();
        }
    }

    // ДОБАВЛЯЕМ НЕДОСТАЮЩИЙ МЕТОД
    public boolean isAutoAdjust() {
        return false; // По умолчанию отключено
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getCategoryWithoutIcon() {
        if (category.contains(" ")) {
            return category.substring(category.indexOf(" ") + 1);
        }
        return category;
    }

    public String getCategoryIcon() {
        if (category.contains(" ")) {
            return category.split(" ")[0];
        }
        return "💰";
    }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public double getSpent() { return spent; }
    public void setSpent(double spent) {
        this.spent = Math.max(0, spent);
        this.exceededNotified = false;
    }

    public String getPeriod() { return period; }
    public void setPeriod(String period) {
        this.period = period;
        setDefaultDates();
    }

    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }

    public Date getEndDate() { return endDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isExceededNotified() { return exceededNotified; }
    public void setExceededNotified(boolean exceededNotified) { this.exceededNotified = exceededNotified; }

    public Date getLastResetDate() { return lastResetDate; }
    public void setLastResetDate(Date lastResetDate) { this.lastResetDate = lastResetDate; }

    // Вспомогательные методы
    public double getRemaining() {
        return Math.max(0, amount - spent);
    }

    public double getUsagePercentage() {
        if (amount == 0) return 0;
        return (spent / amount) * 100;
    }

    public boolean isExceeded() {
        return spent > amount;
    }

    public String getPeriodName() {
        switch (period) {
            case "неделя": return "Неделя";
            case "месяц": return "Месяц";
            case "год": return "Год";
            case "произвольный": return "Произвольный";
            default: return period;
        }
    }

    public String getFormattedDates() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        return sdf.format(startDate) + " - " + sdf.format(endDate);
    }

    public String getStatus() {
        if (!active) return "⏸️ Неактивен";
        if (isExceeded()) return "⚠️ Превышен";
        if (getUsagePercentage() >= 90) return "🔥 Критический";
        if (getUsagePercentage() >= 75) return "⚠️ Почти лимит";
        if (getUsagePercentage() >= 50) return "⚡ Активно";
        if (getUsagePercentage() > 0) return "✅ В норме";
        return "💤 Не начат";
    }

    public int getStatusColor() {
        if (!active) return 0xFF9E9E9E;
        if (isExceeded()) return 0xFFF44336;
        if (getUsagePercentage() >= 90) return 0xFFFF9800;
        if (getUsagePercentage() >= 75) return 0xFFFFC107;
        if (getUsagePercentage() >= 50) return 0xFF2196F3;
        if (getUsagePercentage() > 0) return 0xFF4CAF50;
        return 0xFF9E9E9E;
    }

    public void resetSpent() {
        this.spent = 0;
        this.lastResetDate = new Date();
        this.exceededNotified = false;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Budget budget = (Budget) obj;
        return id.equals(budget.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}