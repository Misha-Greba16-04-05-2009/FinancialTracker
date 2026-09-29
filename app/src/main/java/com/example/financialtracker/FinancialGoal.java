package com.example.financialtracker;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FinancialGoal implements Serializable {
    private String id;
    private String name;
    private double targetAmount;
    private double currentAmount;
    private Date deadline;
    private Date createdDate;
    private String priority;
    private String notes;
    private boolean completed;
    private String category; // Связь с категорией расходов
    private boolean autoSave; // Автоматическое откладывание

    // Конструкторы
    public FinancialGoal(String name, double targetAmount, double currentAmount,
                         Date deadline, String priority, String notes) {
        this.id = generateId();
        this.name = name;
        this.targetAmount = targetAmount;
        this.currentAmount = currentAmount;
        this.deadline = deadline;
        this.createdDate = new Date();
        this.priority = priority != null ? priority : "Средний";
        this.notes = notes != null ? notes : "";
        this.completed = targetAmount > 0 && currentAmount >= targetAmount;
        this.autoSave = false;
    }

    private String generateId() {
        return "goal_" + System.currentTimeMillis() + "_" + Math.random();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getTargetAmount() { return targetAmount; }
    public void setTargetAmount(double targetAmount) { this.targetAmount = targetAmount; }

    public double getCurrentAmount() { return currentAmount; }
    public void setCurrentAmount(double currentAmount) {
        this.currentAmount = Math.max(0, currentAmount);
        // Статус "выполнена" следует за суммой в обе стороны
        this.completed = this.targetAmount > 0 && this.currentAmount >= this.targetAmount;
    }

    public Date getDeadline() { return deadline; }
    public void setDeadline(Date deadline) { this.deadline = deadline; }

    public Date getCreatedDate() { return createdDate; }
    public void setCreatedDate(Date createdDate) { this.createdDate = createdDate; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getNotes() { return notes != null ? notes : ""; }
    public void setNotes(String notes) { this.notes = notes; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public boolean isAutoSave() { return autoSave; }
    public void setAutoSave(boolean autoSave) { this.autoSave = autoSave; }

    // Вспомогательные методы
    public double getRemainingAmount() {
        return Math.max(0, targetAmount - currentAmount);
    }

    public double getProgressPercentage() {
        if (targetAmount == 0) return 0;
        double percentage = (currentAmount / targetAmount) * 100;
        return Math.min(100, percentage);
    }

    public String getFormattedDeadline() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        return sdf.format(deadline);
    }

    public String getFormattedCreatedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        return sdf.format(createdDate);
    }

    public int getPriorityColor() {
        switch (priority == null ? "" : priority) {
            case "Критический": return 0xFFF44336; // Красный
            case "Высокий": return 0xFFFF9800; // Оранжевый
            case "Средний": return 0xFF2196F3; // Синий (жёлтый не читался на белом фоне)
            case "Низкий":
            default: return 0xFF4CAF50; // Зеленый
        }
    }

    public String getPriorityIcon() {
        switch (priority == null ? "" : priority) {
            case "Критический": return "🔥";
            case "Высокий": return "⚠️";
            case "Средний": return "📊";
            case "Низкий": return "💤";
            default: return "🎯";
        }
    }

    public String getProgressStatus() {
        if (completed) return "✅ Выполнено";

        double progress = getProgressPercentage();
        if (progress >= 80) return "🔥 Почти у цели";
        if (progress >= 50) return "👍 Хороший прогресс";
        if (progress >= 20) return "💪 На пути к цели";
        return "🚀 Начало пути";
    }

    public boolean isOverdue() {
        return !completed && deadline != null && new Date().after(deadline);
    }

    // ИСПРАВЛЕННЫЙ МЕТОД
    public long getDaysRemaining() {
        if (deadline == null) return 0;
        long diff = deadline.getTime() - new Date().getTime();
        return (long) Math.ceil(diff / (1000.0 * 60 * 60 * 24));
    }

    /** Сколько нужно откладывать в месяц, чтобы успеть к сроку. */
    public double getMonthlyRequired() {
        long days = getDaysRemaining();
        if (completed || days <= 0) return 0;
        double months = Math.max(1.0, days / 30.0);
        return getRemainingAmount() / months;
    }

    public String getDaysText() {
        if (completed) return "✅ Выполнена";
        if (isOverdue()) {
            long days = Math.abs(getDaysRemaining());
            return "⚠️ Просрочено на " + days + " дн.";
        }
        long days = getDaysRemaining();
        if (days == 0) return "🔥 Сегодня крайний срок!";
        if (days < 0) return "⚠️ Просрочена";
        return "⏳ Осталось " + days + " дн.";
    }

    @Override
    public String toString() {
        return String.format(Locale.getDefault(),
                "%s: %.2f / %.2f руб. (%.1f%%) - %s",
                name, currentAmount, targetAmount, getProgressPercentage(),
                isCompleted() ? "Выполнено" : "В процессе");
    }
}