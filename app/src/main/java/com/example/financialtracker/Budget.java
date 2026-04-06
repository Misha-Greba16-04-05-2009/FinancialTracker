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
        Calendar calendar = Calendar.getInstance();
        this.startDate = calendar.getTime();

        switch (period) {
            case "неделя":
                calendar.add(Calendar.DAY_OF_YEAR, 7);
                break;
            case "месяц":
                calendar.add(Calendar.MONTH, 1);
                break;
            case "год":
                calendar.add(Calendar.YEAR, 1);
                break;
            default:
                calendar.add(Calendar.MONTH, 1);
        }
        this.endDate = calendar.getTime();
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