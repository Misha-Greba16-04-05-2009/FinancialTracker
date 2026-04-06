package com.example.financialtracker;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class SavingGoal implements Serializable {
    private String id;
    private String name;
    private String description;
    private double targetAmount;
    private double currentAmount;
    private Date targetDate;
    private Date createdDate;
    private int priority; // 1 - высокий, 2 - средний, 3 - низкий
    private String color;
    private String icon;
    private transient long timestamp;

    // Стандартные иконки для целей
    private static final String[] GOAL_ICONS = {
            "🏠", "🚗", "✈️", "🎓", "💍", "📱", "💻", "🎮",
            "🎸", "🏖️", "🛒", "🎁", "💎", "🏆", "🌟", "💰"
    };

    // Стандартные цвета для целей
    private static final String[] GOAL_COLORS = {
            "#FF5252", "#FF9800", "#FFEB3B", "#4CAF50", "#2196F3",
            "#3F51B5", "#9C27B0", "#E91E63", "#795548", "#607D8B"
    };

    public SavingGoal() {
        this.id = String.valueOf(System.currentTimeMillis());
        this.createdDate = new Date();
        this.currentAmount = 0;
        this.priority = 2; // Средний по умолчанию
        this.timestamp = this.createdDate.getTime();
        this.icon = GOAL_ICONS[(int) (System.currentTimeMillis() % GOAL_ICONS.length)];
        this.color = GOAL_COLORS[(int) (System.currentTimeMillis() % GOAL_COLORS.length)];
    }

    public SavingGoal(String name, String description, double targetAmount, Date targetDate) {
        this();
        this.name = name;
        this.description = description;
        this.targetAmount = targetAmount;
        this.targetDate = targetDate;
    }

    // ==================== GETTERS AND SETTERS ====================

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getTargetAmount() { return targetAmount; }
    public void setTargetAmount(double targetAmount) { this.targetAmount = targetAmount; }

    public double getCurrentAmount() { return currentAmount; }
    public void setCurrentAmount(double currentAmount) { this.currentAmount = currentAmount; }

    public Date getTargetDate() { return targetDate; }
    public void setTargetDate(Date targetDate) { this.targetDate = targetDate; }

    public Date getCreatedDate() { return createdDate; }
    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
        this.timestamp = createdDate.getTime();
    }

    public int getPriority() { return priority; }
    public void setPriority(int priority) {
        this.priority = Math.max(1, Math.min(3, priority)); // Ограничиваем 1-3
    }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        this.createdDate = new Date(timestamp);
    }

    // Метод для восстановления даты
    public void restoreDate() {
        if (timestamp > 0) {
            this.createdDate = new Date(timestamp);
        }
    }

    // ==================== МЕТОДЫ ДЛЯ РАБОТЫ С СУММАМИ ====================

    // Добавить сумму к цели
    public void addAmount(double amount) {
        if (amount > 0) {
            this.currentAmount += amount;
            // Не позволяем превысить целевую сумму (можно изменить при необходимости)
            if (this.currentAmount > this.targetAmount) {
                this.currentAmount = this.targetAmount;
            }
        }
    }

    // Снять сумму с цели (НОВЫЙ МЕТОД)
    public boolean withdrawAmount(double amount) {
        if (amount > 0 && amount <= this.currentAmount) {
            this.currentAmount -= amount;
            return true;
        }
        return false;
    }

    // Установить определенную сумму (сбросить и установить новую)
    public void setAmount(double amount) {
        if (amount >= 0) {
            this.currentAmount = Math.min(amount, this.targetAmount);
        }
    }

    // ==================== РАСЧЕТНЫЕ МЕТОДЫ ====================

    // Получить оставшуюся сумму для достижения цели
    public double getRemainingAmount() {
        return Math.max(0, targetAmount - currentAmount);
    }

    // Получить процент выполнения цели
    public double getProgressPercentage() {
        if (targetAmount <= 0) return 0;
        double percentage = (currentAmount / targetAmount) * 100;
        return Math.min(100, percentage); // Не больше 100%
    }

    // Проверить, достигнута ли цель
    public boolean isAchieved() {
        return currentAmount >= targetAmount;
    }

    // Получить оставшееся количество дней до целевой даты
    public int getDaysRemaining() {
        if (targetDate == null) return -1;

        Calendar targetCal = Calendar.getInstance();
        targetCal.setTime(targetDate);

        Calendar nowCal = Calendar.getInstance();

        // Если целевая дата уже прошла
        if (nowCal.after(targetCal)) {
            return 0;
        }

        long diff = targetCal.getTimeInMillis() - nowCal.getTimeInMillis();
        return (int) (diff / (1000 * 60 * 60 * 24));
    }

    // Получить необходимую ежедневную сумму для достижения цели вовремя
    public double getDailyRequiredAmount() {
        int daysRemaining = getDaysRemaining();
        double remainingAmount = getRemainingAmount();

        if (daysRemaining <= 0 || remainingAmount <= 0) {
            return 0;
        }

        return remainingAmount / daysRemaining;
    }

    // ==================== ФОРМАТИРОВАННЫЕ МЕТОДЫ ====================

    // Форматированная целевая сумма
    public String getFormattedTargetAmount() {
        return String.format(Locale.getDefault(), "%.2f руб.", targetAmount);
    }

    // Форматированная текущая сумма
    public String getFormattedCurrentAmount() {
        return String.format(Locale.getDefault(), "%.2f руб.", currentAmount);
    }

    // Форматированная оставшаяся сумма
    public String getFormattedRemainingAmount() {
        return String.format(Locale.getDefault(), "%.2f руб.", getRemainingAmount());
    }

    // Форматированный прогресс
    public String getFormattedProgress() {
        return String.format(Locale.getDefault(), "%.1f%%", getProgressPercentage());
    }

    // Форматированная целевая дата
    public String getFormattedTargetDate() {
        if (targetDate == null) return "Не установлена";

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        return sdf.format(targetDate);
    }

    // Форматированная дата создания
    public String getFormattedCreatedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        return sdf.format(createdDate);
    }

    // ==================== МЕТОДЫ ДЛЯ ОТОБРАЖЕНИЯ ====================

    // Получить текст приоритета
    public String getPriorityText() {
        switch (priority) {
            case 1: return "Высокий";
            case 2: return "Средний";
            case 3: return "Низкий";
            default: return "Не указан";
        }
    }

    // Получить цвет приоритета
    public int getPriorityColor() {
        switch (priority) {
            case 1: return 0xFFF44336; // Красный
            case 2: return 0xFFFF9800; // Оранжевый
            case 3: return 0xFF4CAF50; // Зеленый
            default: return 0xFF757575; // Серый
        }
    }

    // Получить статус цели
    public String getStatus() {
        if (isAchieved()) {
            return "✅ Достигнута";
        }

        int daysRemaining = getDaysRemaining();
        if (daysRemaining < 0) {
            return "⏰ Просрочена";
        } else if (daysRemaining == 0) {
            return "⏰ Сегодня последний день";
        } else if (daysRemaining < 7) {
            return "⚠️ Осталось " + daysRemaining + " дней";
        } else if (daysRemaining < 30) {
            return "📅 Осталось " + daysRemaining + " дней";
        } else {
            return "📅 Осталось " + (daysRemaining / 30) + " месяцев";
        }
    }

    // Получить цвет статуса
    public int getStatusColor() {
        if (isAchieved()) {
            return 0xFF4CAF50; // Зеленый
        }

        int daysRemaining = getDaysRemaining();
        if (daysRemaining < 0) {
            return 0xFFF44336; // Красный
        } else if (daysRemaining < 7) {
            return 0xFFFF9800; // Оранжевый
        } else {
            return 0xFF2196F3; // Синий
        }
    }

    // Получить рекомендацию по пополнению
    public String getRecommendation() {
        double dailyRequired = getDailyRequiredAmount();
        int daysRemaining = getDaysRemaining();

        if (isAchieved()) {
            return "Цель достигнута! 🎉";
        }

        if (daysRemaining <= 0) {
            return "Срок достижения цели истек";
        }

        if (dailyRequired > 0) {
            return String.format(Locale.getDefault(),
                    "Для достижения цели вовремя нужно откладывать %.2f руб. в день",
                    dailyRequired);
        }

        return "Регулярно пополняйте цель для её достижения";
    }

    // ==================== МЕТОДЫ ДЛЯ ПРОВЕРКИ И ВАЛИДАЦИИ ====================

    // Проверить, валидна ли цель
    public boolean isValid() {
        return name != null && !name.trim().isEmpty() &&
                targetAmount > 0 &&
                targetDate != null &&
                targetDate.after(new Date());
    }

    // Проверить, просрочена ли цель
    public boolean isOverdue() {
        return !isAchieved() && getDaysRemaining() < 0;
    }

    // Проверить, близка ли цель к завершению (осталось меньше 10%)
    public boolean isNearCompletion() {
        return !isAchieved() && getProgressPercentage() >= 90;
    }

    // Проверить, только ли создана цель (мало накоплений)
    public boolean isJustStarted() {
        return getProgressPercentage() < 10;
    }

    // ==================== МЕТОДЫ ДЛЯ КЛОНИРОВАНИЯ И СРАВНЕНИЯ ====================

    // Создать копию цели
    public SavingGoal copy() {
        SavingGoal copy = new SavingGoal();
        copy.id = this.id;
        copy.name = this.name;
        copy.description = this.description;
        copy.targetAmount = this.targetAmount;
        copy.currentAmount = this.currentAmount;
        copy.targetDate = this.targetDate != null ? (Date) this.targetDate.clone() : null;
        copy.createdDate = this.createdDate != null ? (Date) this.createdDate.clone() : null;
        copy.priority = this.priority;
        copy.color = this.color;
        copy.icon = this.icon;
        copy.timestamp = this.timestamp;
        return copy;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        SavingGoal goal = (SavingGoal) obj;
        return id.equals(goal.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return String.format(Locale.getDefault(),
                "SavingGoal{id='%s', name='%s', target=%.2f, current=%.2f, progress=%.1f%%, status=%s}",
                id, name, targetAmount, currentAmount, getProgressPercentage(), getStatus());
    }

    // ==================== МЕТОДЫ ДЛЯ ГЕНЕРАЦИИ ИКОНОК И ЦВЕТОВ ====================

    // Сгенерировать случайную иконку
    public static String generateRandomIcon() {
        int index = (int) (System.currentTimeMillis() % GOAL_ICONS.length);
        return GOAL_ICONS[index];
    }

    // Сгенерировать случайный цвет
    public static String generateRandomColor() {
        int index = (int) (System.currentTimeMillis() % GOAL_COLORS.length);
        return GOAL_COLORS[index];
    }

    // Получить иконку по имени цели
    public static String getIconForGoalName(String goalName) {
        if (goalName == null || goalName.isEmpty()) {
            return generateRandomIcon();
        }

        goalName = goalName.toLowerCase();

        if (goalName.contains("дом") || goalName.contains("квартира") || goalName.contains("жилье")) {
            return "🏠";
        } else if (goalName.contains("авто") || goalName.contains("машина") || goalName.contains("car")) {
            return "🚗";
        } else if (goalName.contains("путешествие") || goalName.contains("отпуск") || goalName.contains("отдых")) {
            return "✈️";
        } else if (goalName.contains("образование") || goalName.contains("учеба") || goalName.contains("курс")) {
            return "🎓";
        } else if (goalName.contains("свадьба") || goalName.contains("брак")) {
            return "💍";
        } else if (goalName.contains("телефон") || goalName.contains("смартфон")) {
            return "📱";
        } else if (goalName.contains("компьютер") || goalName.contains("ноутбук")) {
            return "💻";
        } else if (goalName.contains("игра") || goalName.contains("консоль")) {
            return "🎮";
        } else if (goalName.contains("инструмент") || goalName.contains("гитара")) {
            return "🎸";
        } else if (goalName.contains("подарок") || goalName.contains("сюрприз")) {
            return "🎁";
        } else {
            return generateRandomIcon();
        }
    }
}