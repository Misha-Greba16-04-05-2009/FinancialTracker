package com.example.financialtracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BudgetManager {
    private static final String PREFS_NAME = "BudgetPrefs";
    private static final String KEY_BUDGETS = "budgets";
    private static final String KEY_GOALS = "saving_goals";
    private static final String KEY_BUDGET_HISTORY = "budget_history";
    private static final String KEY_NOTIFICATIONS = "budget_notifications";
    private static final String KEY_LAST_CHECK_DATE = "last_budget_check_date";
    private static final String KEY_TRANSACTION_UPDATES = "transaction_updates";

    private SharedPreferences sharedPreferences;
    private Gson gson;
    private CategoryManager categoryManager;
    private Context context;

    public BudgetManager(Context context) {
        this.context = context;
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        this.categoryManager = new CategoryManager(context);
    }

    // ==================== МЕТОДЫ ДЛЯ БЮДЖЕТОВ ====================

    // Сохранить все бюджеты
    public void saveBudgets(List<Budget> budgets) {
        String json = gson.toJson(budgets);
        sharedPreferences.edit().putString(KEY_BUDGETS, json).apply();
    }

    // Загрузить все бюджеты
    public List<Budget> loadBudgets() {
        String json = sharedPreferences.getString(KEY_BUDGETS, "");
        if (json.isEmpty()) {
            return new ArrayList<>();
        }

        Type type = new TypeToken<List<Budget>>(){}.getType();
        List<Budget> budgets = gson.fromJson(json, type);

        // Восстанавливаем даты
        if (budgets != null) {
            for (Budget budget : budgets) {
                budget.restoreDate();
            }
        }

        return budgets != null ? budgets : new ArrayList<>();
    }

    // Добавить новый бюджет
    public boolean addBudget(Budget budget) {
        List<Budget> budgets = loadBudgets();

        // Проверяем, нет ли уже бюджета на эту категорию в этот период
        for (Budget existingBudget : budgets) {
            if (existingBudget.getCategory().equals(budget.getCategory()) &&
                    existingBudget.getPeriod().equals(budget.getPeriod())) {
                return false; // Бюджет уже существует
            }
        }

        budgets.add(budget);
        saveBudgets(budgets);
        return true;
    }

    // Обновить бюджет
    public void updateBudget(Budget budget) {
        List<Budget> budgets = loadBudgets();
        for (int i = 0; i < budgets.size(); i++) {
            if (budgets.get(i).getId().equals(budget.getId())) {
                budgets.set(i, budget);
                break;
            }
        }
        saveBudgets(budgets);
    }

    // Удалить бюджет
    public boolean removeBudget(String budgetId) {
        List<Budget> budgets = loadBudgets();
        for (Budget budget : budgets) {
            if (budget.getId().equals(budgetId)) {
                budgets.remove(budget);
                saveBudgets(budgets);
                return true;
            }
        }
        return false;
    }

    // Получить бюджет по категории и периоду
    public Budget getBudgetForCategory(String category, String period) {
        List<Budget> budgets = loadBudgets();
        for (Budget budget : budgets) {
            if (budget.getCategory().equals(category) &&
                    budget.getPeriod().equals(period)) {
                return budget;
            }
        }
        return null;
    }

    // Получить все бюджеты по периоду
    public List<Budget> getBudgetsByPeriod(String period) {
        List<Budget> allBudgets = loadBudgets();
        List<Budget> filteredBudgets = new ArrayList<>();

        for (Budget budget : allBudgets) {
            if (budget.getPeriod().equals(period)) {
                filteredBudgets.add(budget);
            }
        }

        return filteredBudgets;
    }

    // Обновить потраченные суммы в бюджетах на основе транзакций
    public void updateBudgetsFromTransactions(List<Transaction> transactions) {
        List<Budget> budgets = loadBudgets();
        if (budgets.isEmpty()) {
            return; // Нет бюджетов для обновления
        }

        Map<String, Double> categorySpentMap = new HashMap<>();

        // Инициализируем карту потраченных сумм
        for (Budget budget : budgets) {
            categorySpentMap.put(budget.getCategory() + "_" + budget.getPeriod(), 0.0);
            budget.setSpent(0); // Сбрасываем потраченные суммы
        }

        // Считаем потраченные суммы по категориям и периодам
        Calendar cal = Calendar.getInstance();
        for (Transaction transaction : transactions) {
            if (!transaction.isIncome()) { // Только расходы
                String category = transaction.getCategory();
                String period = getPeriodForDate(transaction.getDate());

                // Ищем подходящий бюджет для этой категории и периода
                for (Budget budget : budgets) {
                    if (budget.getCategory().equals(category) &&
                            isDateInBudgetPeriod(transaction.getDate(), budget)) {

                        String key = budget.getCategory() + "_" + budget.getPeriod();
                        double currentSpent = categorySpentMap.getOrDefault(key, 0.0);
                        categorySpentMap.put(key, currentSpent + transaction.getAmount());
                        break;
                    }
                }
            }
        }

        // Обновляем бюджеты
        for (Budget budget : budgets) {
            String key = budget.getCategory() + "_" + budget.getPeriod();
            double spent = categorySpentMap.getOrDefault(key, 0.0);
            budget.setSpent(spent);
        }

        saveBudgets(budgets);

        // Проверяем превышение бюджета и показываем уведомления
        checkBudgetExceededAndNotify();
    }

    // Улучшенный метод для обновления конкретного бюджета при добавлении транзакции
    public void updateBudgetForTransaction(Transaction transaction) {
        if (transaction.isIncome()) {
            return; // Бюджеты только для расходов
        }

        List<Budget> budgets = loadBudgets();
        if (budgets.isEmpty()) {
            return;
        }

        String transactionCategory = transaction.getCategory();
        Date transactionDate = transaction.getDate();

        boolean budgetUpdated = false;

        for (Budget budget : budgets) {
            // Проверяем совпадение категории
            if (budget.getCategory().equals(transactionCategory)) {
                // Проверяем, подходит ли дата под период бюджета
                if (isDateInBudgetPeriod(transactionDate, budget)) {
                    // Обновляем потраченную сумму
                    double newSpent = budget.getSpent() + transaction.getAmount();
                    budget.setSpent(newSpent);

                    // Сохраняем обновленный бюджет
                    updateBudget(budget);
                    budgetUpdated = true;

                    // Логируем обновление
                    logBudgetUpdate(budget, transaction);

                    // Проверяем и показываем уведомление
                    checkAndNotifyBudgetStatus(budget);
                }
            }
        }

        if (budgetUpdated) {
            // Сохраняем информацию о транзакции для отслеживания
            saveTransactionUpdate(transaction);
        }
    }

    // Проверяет, находится ли дата в периоде бюджета
    private boolean isDateInBudgetPeriod(Date date, Budget budget) {
        if (budget.getStartDate() == null || budget.getEndDate() == null) {
            return true; // Если даты не установлены, считаем что подходит
        }

        Calendar dateCal = Calendar.getInstance();
        dateCal.setTime(date);

        Calendar startCal = Calendar.getInstance();
        startCal.setTime(budget.getStartDate());

        Calendar endCal = Calendar.getInstance();
        endCal.setTime(budget.getEndDate());

        return !dateCal.before(startCal) && !dateCal.after(endCal);
    }

    // Проверка превышения бюджета и показ уведомлений
    private void checkBudgetExceededAndNotify() {
        List<Budget> budgets = loadBudgets();
        List<String> exceededBudgets = new ArrayList<>();
        List<String> warningBudgets = new ArrayList<>();

        for (Budget budget : budgets) {
            double usagePercentage = budget.getUsagePercentage();

            if (budget.isExceeded()) {
                exceededBudgets.add(budget.getCategoryWithoutIcon());
                saveNotification(budget, "ПРЕВЫШЕН");
            } else if (usagePercentage >= 80) {
                warningBudgets.add(budget.getCategoryWithoutIcon());
                saveNotification(budget, "ПРИБЛИЖАЕТСЯ К ЛИМИТУ");
            }
        }

        // Показываем уведомления только если проверяем не слишком часто
        if (shouldShowNotification()) {
            showBudgetNotifications(exceededBudgets, warningBudgets);
            updateLastCheckDate();
        }
    }

    // Проверяет, нужно ли показывать уведомление (не чаще чем раз в день)
    private boolean shouldShowNotification() {
        String lastCheckStr = sharedPreferences.getString(KEY_LAST_CHECK_DATE, "");
        if (lastCheckStr.isEmpty()) {
            return true;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
            Date lastCheckDate = sdf.parse(lastCheckStr);
            Date today = new Date();

            // Проверяем, прошел ли хотя бы один день
            Calendar lastCal = Calendar.getInstance();
            lastCal.setTime(lastCheckDate);

            Calendar todayCal = Calendar.getInstance();
            todayCal.setTime(today);

            return lastCal.get(Calendar.YEAR) != todayCal.get(Calendar.YEAR) ||
                    lastCal.get(Calendar.DAY_OF_YEAR) != todayCal.get(Calendar.DAY_OF_YEAR);
        } catch (Exception e) {
            return true;
        }
    }

    // Обновляет дату последней проверки
    private void updateLastCheckDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        String todayStr = sdf.format(new Date());
        sharedPreferences.edit().putString(KEY_LAST_CHECK_DATE, todayStr).apply();
    }

    // Показывает уведомления о бюджетах
    private void showBudgetNotifications(List<String> exceededBudgets, List<String> warningBudgets) {
        if (exceededBudgets.isEmpty() && warningBudgets.isEmpty()) {
            return;
        }

        StringBuilder message = new StringBuilder();

        if (!exceededBudgets.isEmpty()) {
            message.append("⚠️ ПРЕВЫШЕН БЮДЖЕТ:\n");
            for (String category : exceededBudgets) {
                message.append("• ").append(category).append("\n");
            }
            message.append("\n");
        }

        if (!warningBudgets.isEmpty()) {
            message.append("📊 БЮДЖЕТ ПРИБЛИЖАЕТСЯ К ЛИМИТУ:\n");
            for (String category : warningBudgets) {
                message.append("• ").append(category).append("\n");
            }
        }

        if (context != null && message.length() > 0) {
            Toast.makeText(context, message.toString(), Toast.LENGTH_LONG).show();
        }
    }

    // Проверяет и уведомляет о статусе бюджета
    private void checkAndNotifyBudgetStatus(Budget budget) {
        double usagePercentage = budget.getUsagePercentage();

        if (budget.isExceeded()) {
            showBudgetNotification(budget, "ПРЕВЫШЕН");
        } else if (usagePercentage >= 90) {
            showBudgetNotification(budget, "ПОЧТИ ПРЕВЫШЕН");
        } else if (usagePercentage >= 70) {
            showBudgetNotification(budget, "ВЫСОКОЕ ИСПОЛЬЗОВАНИЕ");
        }
    }

    // Показывает уведомление о бюджете
    private void showBudgetNotification(Budget budget, String status) {
        String message = String.format(Locale.getDefault(),
                "%s: %s\n" +
                        "💰 Лимит: %.2f руб.\n" +
                        "💸 Потрачено: %.2f руб.\n" +
                        "✅ Осталось: %.2f руб.\n" +
                        "📈 Использовано: %.1f%%",
                status,
                budget.getCategoryWithoutIcon(),
                budget.getAmount(),
                budget.getSpent(),
                budget.getRemaining(),
                budget.getUsagePercentage());

        if (context != null) {
            // Используем Toast для немедленного уведомления
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();

            // Также сохраняем в историю уведомлений
            saveNotification(budget, status);
        }
    }

    // Сохранить уведомление о бюджете
    private void saveNotification(Budget budget, String type) {
        String historyJson = sharedPreferences.getString(KEY_NOTIFICATIONS, "[]");
        Type tokenType = new TypeToken<List<String>>(){}.getType();
        List<String> notifications = gson.fromJson(historyJson, tokenType);

        if (notifications == null) {
            notifications = new ArrayList<>();
        }

        String notification = String.format(Locale.getDefault(),
                "[%s] %s: %s. Потрачено: %.2f из %.2f руб. (%.1f%%)",
                new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(new Date()),
                type,
                budget.getCategoryWithoutIcon(),
                budget.getSpent(),
                budget.getAmount(),
                budget.getUsagePercentage());

        notifications.add(notification);

        // Сохраняем только последние 50 уведомлений
        if (notifications.size() > 50) {
            notifications = notifications.subList(notifications.size() - 50, notifications.size());
        }

        String json = gson.toJson(notifications);
        sharedPreferences.edit().putString(KEY_NOTIFICATIONS, json).apply();
    }

    // Логирует обновление бюджета
    private void logBudgetUpdate(Budget budget, Transaction transaction) {
        String historyJson = sharedPreferences.getString(KEY_BUDGET_HISTORY, "{}");
        Type type = new TypeToken<Map<String, List<String>>>(){}.getType();
        Map<String, List<String>> history = gson.fromJson(historyJson, type);

        if (history == null) {
            history = new HashMap<>();
        }

        List<String> updates = history.get(budget.getId());
        if (updates == null) {
            updates = new ArrayList<>();
        }

        String update = String.format(Locale.getDefault(),
                "[%s] +%.2f руб. | Теперь: %.2f/%.2f руб. (%.1f%%) | %s",
                new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(new Date()),
                transaction.getAmount(),
                budget.getSpent(),
                budget.getAmount(),
                budget.getUsagePercentage(),
                transaction.getDescription());

        updates.add(update);

        // Сохраняем только последние 20 обновлений
        if (updates.size() > 20) {
            updates = updates.subList(updates.size() - 20, updates.size());
        }

        history.put(budget.getId(), updates);
        String json = gson.toJson(history);
        sharedPreferences.edit().putString(KEY_BUDGET_HISTORY, json).apply();
    }

    // Сохраняет информацию об обновлении транзакцией
    private void saveTransactionUpdate(Transaction transaction) {
        String updatesJson = sharedPreferences.getString(KEY_TRANSACTION_UPDATES, "[]");
        Type type = new TypeToken<List<String>>(){}.getType();
        List<String> updates = gson.fromJson(updatesJson, type);

        if (updates == null) {
            updates = new ArrayList<>();
        }

        String update = String.format(Locale.getDefault(),
                "[%s] %s: %.2f руб. (%s)",
                new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(new Date()),
                transaction.getCategoryWithoutIcon(),
                transaction.getAmount(),
                transaction.getDescription());

        updates.add(update);

        // Сохраняем только последние 50 обновлений
        if (updates.size() > 50) {
            updates = updates.subList(updates.size() - 50, updates.size());
        }

        String json = gson.toJson(updates);
        sharedPreferences.edit().putString(KEY_TRANSACTION_UPDATES, json).apply();
    }

    // Получить историю уведомлений
    public List<String> getNotifications() {
        String json = sharedPreferences.getString(KEY_NOTIFICATIONS, "[]");
        Type type = new TypeToken<List<String>>(){}.getType();
        List<String> notifications = gson.fromJson(json, type);
        return notifications != null ? notifications : new ArrayList<>();
    }

    // Получить историю обновлений бюджета
    public List<String> getBudgetUpdateHistory(String budgetId) {
        String historyJson = sharedPreferences.getString(KEY_BUDGET_HISTORY, "{}");
        Type type = new TypeToken<Map<String, List<String>>>(){}.getType();
        Map<String, List<String>> history = gson.fromJson(historyJson, type);

        if (history != null) {
            List<String> updates = history.get(budgetId);
            if (updates != null) {
                updates.sort((a, b) -> b.compareTo(a));
                return updates;
            }
        }

        return new ArrayList<>();
    }

    // Получить историю обновлений транзакциями
    public List<String> getTransactionUpdates() {
        String json = sharedPreferences.getString(KEY_TRANSACTION_UPDATES, "[]");
        Type type = new TypeToken<List<String>>(){}.getType();
        List<String> updates = gson.fromJson(json, type);
        return updates != null ? updates : new ArrayList<>();
    }

    // Проверить и показать уведомления вручную (для тестирования)
    public void checkAndShowNotificationsNow() {
        List<Budget> budgets = loadBudgets();
        List<String> exceededBudgets = new ArrayList<>();
        List<String> warningBudgets = new ArrayList<>();

        for (Budget budget : budgets) {
            double usagePercentage = budget.getUsagePercentage();

            if (budget.isExceeded()) {
                exceededBudgets.add(budget.getCategoryWithoutIcon());
            } else if (usagePercentage >= 80) {
                warningBudgets.add(budget.getCategoryWithoutIcon());
            }
        }

        showBudgetNotifications(exceededBudgets, warningBudgets);
    }

    // Автоматическая корректировка бюджета
    public Budget autoAdjustBudget(Budget budget) {
        if (budget.isAutoAdjust() && budget.isExceeded()) {
            // Увеличиваем бюджет на 10% если превышен более чем на 20%
            double usagePercentage = budget.getUsagePercentage();
            if (usagePercentage > 120) {
                double newAmount = budget.getAmount() * 1.1;
                budget.setAmount(newAmount);
                updateBudget(budget);

                // Сохраняем в историю
                saveBudgetAdjustment(budget.getId(), budget.getAmount(), newAmount,
                        "Автоматическая корректировка: превышение на " +
                                String.format(Locale.getDefault(), "%.1f%%", usagePercentage));
            }
        }
        return budget;
    }

    // Сохранить корректировку бюджета в историю
    private void saveBudgetAdjustment(String budgetId, double oldAmount,
                                      double newAmount, String reason) {
        String historyJson = sharedPreferences.getString(KEY_BUDGET_HISTORY, "{}");
        Type type = new TypeToken<Map<String, List<String>>>(){}.getType();
        Map<String, List<String>> history = gson.fromJson(historyJson, type);

        if (history == null) {
            history = new HashMap<>();
        }

        List<String> adjustments = history.get(budgetId);
        if (adjustments == null) {
            adjustments = new ArrayList<>();
        }

        String adjustment = String.format(Locale.getDefault(),
                "[%s] %.2f → %.2f руб. | %s",
                new Date().toString(), oldAmount, newAmount, reason);

        adjustments.add(adjustment);
        history.put(budgetId, adjustments);

        String json = gson.toJson(history);
        sharedPreferences.edit().putString(KEY_BUDGET_HISTORY, json).apply();
    }

    // Получить статистику по бюджетам
    public String getBudgetsStatistics() {
        List<Budget> budgets = loadBudgets();

        if (budgets.isEmpty()) {
            return "Бюджеты не настроены";
        }

        double totalBudget = 0;
        double totalSpent = 0;
        int activeBudgets = 0;
        int exceededBudgets = 0;
        int warningBudgets = 0;

        for (Budget budget : budgets) {
            totalBudget += budget.getAmount();
            totalSpent += budget.getSpent();
            activeBudgets++;

            if (budget.isExceeded()) {
                exceededBudgets++;
            } else if (budget.getUsagePercentage() >= 80) {
                warningBudgets++;
            }
        }

        double usagePercentage = totalBudget > 0 ? (totalSpent / totalBudget) * 100 : 0;

        return String.format(Locale.getDefault(),
                "📊 СТАТИСТИКА БЮДЖЕТОВ:\n\n" +
                        "• Всего бюджетов: %d\n" +
                        "• Общий лимит: %.2f руб.\n" +
                        "• Потрачено: %.2f руб. (%.1f%%)\n" +
                        "• Превышено бюджетов: %d\n" +
                        "• Близко к лимиту: %d\n" +
                        "• В пределах: %d",
                activeBudgets, totalBudget, totalSpent, usagePercentage,
                exceededBudgets, warningBudgets, activeBudgets - exceededBudgets - warningBudgets);
    }

    // Получить бюджет по ID
    public Budget getBudgetById(String budgetId) {
        List<Budget> budgets = loadBudgets();
        for (Budget budget : budgets) {
            if (budget.getId().equals(budgetId)) {
                return budget;
            }
        }
        return null;
    }

    // Получить бюджеты для определенной категории
    public List<Budget> getBudgetsForCategory(String category) {
        List<Budget> allBudgets = loadBudgets();
        List<Budget> filteredBudgets = new ArrayList<>();

        for (Budget budget : allBudgets) {
            if (budget.getCategory().equals(category)) {
                filteredBudgets.add(budget);
            }
        }

        return filteredBudgets;
    }

    // Получить общую статистику по использованию бюджетов
    public Map<String, Double> getBudgetUsageStatistics() {
        List<Budget> budgets = loadBudgets();
        Map<String, Double> statistics = new HashMap<>();

        double totalBudget = 0;
        double totalSpent = 0;
        int exceededCount = 0;
        int warningCount = 0;
        int normalCount = 0;

        for (Budget budget : budgets) {
            totalBudget += budget.getAmount();
            totalSpent += budget.getSpent();

            if (budget.isExceeded()) {
                exceededCount++;
            } else if (budget.getUsagePercentage() >= 80) {
                warningCount++;
            } else {
                normalCount++;
            }
        }

        statistics.put("total_budget", totalBudget);
        statistics.put("total_spent", totalSpent);
        statistics.put("exceeded_count", (double) exceededCount);
        statistics.put("warning_count", (double) warningCount);
        statistics.put("normal_count", (double) normalCount);
        statistics.put("total_count", (double) budgets.size());

        if (totalBudget > 0) {
            statistics.put("usage_percentage", (totalSpent / totalBudget) * 100);
        } else {
            statistics.put("usage_percentage", 0.0);
        }

        return statistics;
    }

    // Метод для принудительной проверки бюджетов
    public void forceBudgetCheck() {
        updateLastCheckDate(); // Сбрасываем таймер
        checkBudgetExceededAndNotify();
    }

    // ==================== МЕТОДЫ ДЛЯ ЦЕЛЕЙ ====================

    // Сохранить все цели
    public void saveGoals(List<SavingGoal> goals) {
        String json = gson.toJson(goals);
        sharedPreferences.edit().putString(KEY_GOALS, json).apply();
    }

    // Загрузить все цели
    public List<SavingGoal> loadGoals() {
        String json = sharedPreferences.getString(KEY_GOALS, "");
        if (json.isEmpty()) {
            return new ArrayList<>();
        }

        Type type = new TypeToken<List<SavingGoal>>(){}.getType();
        List<SavingGoal> goals = gson.fromJson(json, type);

        // Восстанавливаем даты
        if (goals != null) {
            for (SavingGoal goal : goals) {
                goal.restoreDate();
            }
        }

        return goals != null ? goals : new ArrayList<>();
    }

    // Добавить новую цель
    public boolean addGoal(SavingGoal goal) {
        List<SavingGoal> goals = loadGoals();

        // Проверяем, нет ли уже цели с таким именем
        for (SavingGoal existingGoal : goals) {
            if (existingGoal.getName().equals(goal.getName())) {
                return false;
            }
        }

        goals.add(goal);
        saveGoals(goals);
        return true;
    }

    // Обновить цель
    public void updateGoal(SavingGoal goal) {
        List<SavingGoal> goals = loadGoals();
        for (int i = 0; i < goals.size(); i++) {
            if (goals.get(i).getId().equals(goal.getId())) {
                goals.set(i, goal);
                break;
            }
        }
        saveGoals(goals);
    }

    // Удалить цель
    public boolean removeGoal(String goalId) {
        List<SavingGoal> goals = loadGoals();
        for (SavingGoal goal : goals) {
            if (goal.getId().equals(goalId)) {
                goals.remove(goal);
                saveGoals(goals);
                return true;
            }
        }
        return false;
    }

    // Пополнить цель (перевод денег на цель)
    public boolean addToGoal(String goalId, double amount, String accountName) {
        List<SavingGoal> goals = loadGoals();
        for (SavingGoal goal : goals) {
            if (goal.getId().equals(goalId)) {
                goal.addAmount(amount);
                saveGoals(goals);

                // Логируем перевод
                logTransferToGoal(goal, amount, accountName);
                return true;
            }
        }
        return false;
    }

    // Снять деньги с цели
    public boolean withdrawFromGoal(String goalId, double amount, String accountName) {
        List<SavingGoal> goals = loadGoals();
        for (SavingGoal goal : goals) {
            if (goal.getId().equals(goalId)) {
                if (goal.getCurrentAmount() >= amount) {
                    goal.withdrawAmount(amount);
                    saveGoals(goals);

                    // Логируем снятие
                    logWithdrawalFromGoal(goal, amount, accountName);
                    return true;
                } else {
                    return false; // Недостаточно средств на цели
                }
            }
        }
        return false;
    }

    // Логировать перевод на цель
    private void logTransferToGoal(SavingGoal goal, double amount, String accountName) {
        String historyJson = sharedPreferences.getString(KEY_BUDGET_HISTORY, "{}");
        Type type = new TypeToken<Map<String, List<String>>>(){}.getType();
        Map<String, List<String>> history = gson.fromJson(historyJson, type);

        if (history == null) {
            history = new HashMap<>();
        }

        List<String> transfers = history.get("goal_transfers");
        if (transfers == null) {
            transfers = new ArrayList<>();
        }

        String transfer = String.format(Locale.getDefault(),
                "[%s] 📥 Пополнение цели '%s': +%.2f руб. со счета '%s'",
                new Date().toString(), goal.getName(), amount, accountName);

        transfers.add(transfer);
        history.put("goal_transfers", transfers);

        String json = gson.toJson(history);
        sharedPreferences.edit().putString(KEY_BUDGET_HISTORY, json).apply();
    }

    // Логировать снятие с цели
    private void logWithdrawalFromGoal(SavingGoal goal, double amount, String accountName) {
        String historyJson = sharedPreferences.getString(KEY_BUDGET_HISTORY, "{}");
        Type type = new TypeToken<Map<String, List<String>>>(){}.getType();
        Map<String, List<String>> history = gson.fromJson(historyJson, type);

        if (history == null) {
            history = new HashMap<>();
        }

        List<String> withdrawals = history.get("goal_withdrawals");
        if (withdrawals == null) {
            withdrawals = new ArrayList<>();
        }

        String withdrawal = String.format(Locale.getDefault(),
                "[%s] 📤 Снятие с цели '%s': -%.2f руб. на счет '%s'",
                new Date().toString(), goal.getName(), amount, accountName);

        withdrawals.add(withdrawal);
        history.put("goal_withdrawals", withdrawals);

        String json = gson.toJson(history);
        sharedPreferences.edit().putString(KEY_BUDGET_HISTORY, json).apply();
    }

    // Получить цели по приоритету
    public List<SavingGoal> getGoalsByPriority(int priority) {
        List<SavingGoal> allGoals = loadGoals();
        List<SavingGoal> filteredGoals = new ArrayList<>();

        for (SavingGoal goal : allGoals) {
            if (goal.getPriority() == priority) {
                filteredGoals.add(goal);
            }
        }

        return filteredGoals;
    }

    // Получить статистику по целям
    public String getGoalsStatistics() {
        List<SavingGoal> goals = loadGoals();

        if (goals.isEmpty()) {
            return "Цели накоплений не установлены";
        }

        double totalTarget = 0;
        double totalSaved = 0;
        int achievedGoals = 0;
        int activeGoals = 0;
        int overdueGoals = 0;

        for (SavingGoal goal : goals) {
            totalTarget += goal.getTargetAmount();
            totalSaved += goal.getCurrentAmount();
            activeGoals++;

            if (goal.isAchieved()) {
                achievedGoals++;
            }

            if (goal.getDaysRemaining() < 0 && !goal.isAchieved()) {
                overdueGoals++;
            }
        }

        double progressPercentage = totalTarget > 0 ? (totalSaved / totalTarget) * 100 : 0;

        return String.format(Locale.getDefault(),
                "🎯 Статистика целей:\n\n" +
                        "• Всего целей: %d\n" +
                        "• Общая цель: %.2f руб.\n" +
                        "• Накоплено: %.2f руб. (%.1f%%)\n" +
                        "• Достигнуто целей: %d\n" +
                        "• Просрочено: %d\n" +
                        "• Активные цели: %d",
                activeGoals, totalTarget, totalSaved, progressPercentage,
                achievedGoals, overdueGoals, activeGoals - achievedGoals);
    }

    // Получить историю операций с целями
    public List<String> getGoalHistory() {
        String historyJson = sharedPreferences.getString(KEY_BUDGET_HISTORY, "{}");
        Type type = new TypeToken<Map<String, List<String>>>(){}.getType();
        Map<String, List<String>> history = gson.fromJson(historyJson, type);

        List<String> allHistory = new ArrayList<>();

        if (history != null) {
            List<String> transfers = history.get("goal_transfers");
            List<String> withdrawals = history.get("goal_withdrawals");

            if (transfers != null) {
                allHistory.addAll(transfers);
            }

            if (withdrawals != null) {
                allHistory.addAll(withdrawals);
            }
        }

        // Сортируем по дате (новые сначала)
        allHistory.sort((a, b) -> b.compareTo(a));

        return allHistory;
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    // Получить период для даты
    private String getPeriodForDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        // Для простоты используем месячный период
        // Можно расширить для других периодов
        return "monthly";
    }

    // Генерация цвета для бюджета
    public int generateBudgetColor(String category) {
        int hash = category.hashCode();
        hash = Math.abs(hash);

        // Генерируем цвета в пастельных тонах
        int r = 100 + (hash % 100);      // 100-200
        int g = 150 + ((hash / 100) % 100); // 150-250
        int b = 200 + ((hash / 10000) % 55); // 200-255

        return Color.rgb(r, g, b);
    }

    // Получить рекомендованный бюджет для категории
    public double getRecommendedBudget(String category, List<Transaction> pastTransactions) {
        double total = 0;
        int count = 0;

        Calendar cal = Calendar.getInstance();
        for (Transaction transaction : pastTransactions) {
            if (!transaction.isIncome() && transaction.getCategory().equals(category)) {
                cal.setTime(transaction.getDate());
                int month = cal.get(Calendar.MONTH);
                int currentMonth = Calendar.getInstance().get(Calendar.MONTH);

                // Учитываем только транзакции за последние 3 месяца
                if (Math.abs(month - currentMonth) <= 3) {
                    total += transaction.getAmount();
                    count++;
                }
            }
        }

        if (count == 0) return 0;

        // Рекомендуем среднюю сумму + 10%
        double average = total / count;
        return average * 1.1;
    }
}