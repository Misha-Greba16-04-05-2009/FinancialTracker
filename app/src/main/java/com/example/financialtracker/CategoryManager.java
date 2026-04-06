package com.example.financialtracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CategoryManager {
    private static final String PREFS_NAME = "CategoryPrefs";
    private static final String KEY_INCOME_CATEGORIES = "income_categories";
    private static final String KEY_EXPENSE_CATEGORIES = "expense_categories";
    private static final String KEY_CATEGORY_COLORS = "category_colors";

    private SharedPreferences sharedPreferences;
    private Gson gson;

    // Стандартные категории доходов с иконками
    private static final List<String> DEFAULT_INCOME_CATEGORIES = Arrays.asList(
            "💼 Зарплата", "💻 Фриланс", "📈 Инвестиции", "🎁 Подарки",
            "🔄 Возврат долга", "🏆 Премия", "📚 Стипендия", "💰 Прочие доходы"
    );

    // Стандартные категории расходов с иконками
    private static final List<String> DEFAULT_EXPENSE_CATEGORIES = Arrays.asList(
            "🍕 Еда", "🚗 Транспорт", "🏠 Жилье", "🎬 Развлечения",
            "👕 Одежда", "🏥 Здоровье", "📚 Образование", "🎁 Подарки",
            "☕ Кафе", "📱 Связь", "💡 Коммуналка", "💸 Прочие расходы"
    );

    public CategoryManager(Context context) {
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    // Получить все категории доходов
    public List<String> getIncomeCategories() {
        String json = sharedPreferences.getString(KEY_INCOME_CATEGORIES, "");
        if (json.isEmpty()) {
            // Возвращаем стандартные категории при первом запуске
            saveIncomeCategories(new ArrayList<>(DEFAULT_INCOME_CATEGORIES));
            return new ArrayList<>(DEFAULT_INCOME_CATEGORIES);
        }

        Type type = new TypeToken<List<String>>(){}.getType();
        List<String> categories = gson.fromJson(json, type);
        return categories != null ? categories : new ArrayList<>(DEFAULT_INCOME_CATEGORIES);
    }

    // Получить все категории расходов
    public List<String> getExpenseCategories() {
        String json = sharedPreferences.getString(KEY_EXPENSE_CATEGORIES, "");
        if (json.isEmpty()) {
            // Возвращаем стандартные категории при первом запуске
            saveExpenseCategories(new ArrayList<>(DEFAULT_EXPENSE_CATEGORIES));
            return new ArrayList<>(DEFAULT_EXPENSE_CATEGORIES);
        }

        Type type = new TypeToken<List<String>>(){}.getType();
        List<String> categories = gson.fromJson(json, type);
        return categories != null ? categories : new ArrayList<>(DEFAULT_EXPENSE_CATEGORIES);
    }

    // Сохранить категории доходов
    private void saveIncomeCategories(List<String> categories) {
        String json = gson.toJson(categories);
        sharedPreferences.edit().putString(KEY_INCOME_CATEGORIES, json).apply();
    }

    // Сохранить категории расходов
    private void saveExpenseCategories(List<String> categories) {
        String json = gson.toJson(categories);
        sharedPreferences.edit().putString(KEY_EXPENSE_CATEGORIES, json).apply();
    }

    // Добавить новую категорию доходов
    public boolean addIncomeCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return false;
        }

        // Добавляем иконку если её нет
        String formattedCategory = category.trim();
        if (!formattedCategory.contains(" ")) {
            formattedCategory = "💰 " + formattedCategory;
        }

        List<String> categories = getIncomeCategories();
        if (!categories.contains(formattedCategory)) {
            categories.add(formattedCategory);
            saveIncomeCategories(categories);
            return true;
        }
        return false;
    }

    // Добавить новую категорию расходов
    public boolean addExpenseCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return false;
        }

        // Добавляем иконку если её нет
        String formattedCategory = category.trim();
        if (!formattedCategory.contains(" ")) {
            formattedCategory = "💸 " + formattedCategory;
        }

        List<String> categories = getExpenseCategories();
        if (!categories.contains(formattedCategory)) {
            categories.add(formattedCategory);
            saveExpenseCategories(categories);
            return true;
        }
        return false;
    }

    // Удалить категорию доходов
    public boolean removeIncomeCategory(String category) {
        List<String> categories = getIncomeCategories();
        if (categories.remove(category)) {
            saveIncomeCategories(categories);
            return true;
        }
        return false;
    }

    // Удалить категорию расходов
    public boolean removeExpenseCategory(String category) {
        List<String> categories = getExpenseCategories();
        if (categories.remove(category)) {
            saveExpenseCategories(categories);
            return true;
        }
        return false;
    }

    // Получить цвет для категории
    public int getCategoryColor(String category, boolean isIncome) {
        // Генерируем цвет на основе хэша строки
        return generateColorFromString(category, isIncome);
    }

    // Сгенерировать цвет на основе строки
    private int generateColorFromString(String str, boolean isIncome) {
        int hash = str.hashCode();

        // Используем абсолютное значение хэша
        hash = Math.abs(hash);

        if (isIncome) {
            // Зеленые оттенки для доходов
            int r = 30 + (hash % 40);      // 30-70
            int g = 100 + ((hash / 100) % 100); // 100-200
            int b = 30 + ((hash / 10000) % 40); // 30-70
            return Color.rgb(r, g, b);
        } else {
            // Красно-синие оттенки для расходов
            int r = 150 + (hash % 100);    // 150-250
            int g = 30 + ((hash / 100) % 40);  // 30-70
            int b = 30 + ((hash / 10000) % 40); // 30-70
            return Color.rgb(r, g, b);
        }
    }

    // Получить статистику по категориям за период
    public Map<String, CategoryStats> getCategoryStats(List<Transaction> transactions,
                                                       int year, int month) {
        Map<String, CategoryStats> stats = new HashMap<>();

        for (Transaction transaction : transactions) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(transaction.getDate());
            int transactionYear = calendar.get(Calendar.YEAR);
            int transactionMonth = calendar.get(Calendar.MONTH) + 1;

            // Фильтруем по году и месяцу
            if ((year == -1 || transactionYear == year) &&
                    (month == -1 || transactionMonth == month)) {

                String category = transaction.getCategory();
                CategoryStats categoryStats = stats.get(category);

                if (categoryStats == null) {
                    categoryStats = new CategoryStats(category, transaction.isIncome());
                    stats.put(category, categoryStats);
                }

                categoryStats.amount += transaction.getAmount();
                categoryStats.count++;
                categoryStats.transactions.add(transaction);
            }
        }

        return stats;
    }

    // Получить статистику по категориям (для StatisticsFragment)
    public String getCategoryStatistics(List<Transaction> transactions) {
        Map<String, CategoryStats> incomeStats = getCategoryStats(transactions, -1, -1);

        double totalIncome = 0;
        double totalExpenses = 0;

        for (CategoryStats stat : incomeStats.values()) {
            if (stat.isIncome) {
                totalIncome += stat.amount;
            } else {
                totalExpenses += stat.amount;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📊 ОБЩАЯ СТАТИСТИКА ПО КАТЕГОРИЯМ\n\n");

        if (totalIncome > 0) {
            sb.append("📈 ДОХОДЫ:\n");
            for (CategoryStats stat : incomeStats.values()) {
                if (stat.isIncome) {
                    double percentage = (stat.amount / totalIncome) * 100;
                    sb.append(String.format(Locale.getDefault(), "  %s: %.2f руб. (%.1f%%)\n",
                            stat.getNameWithoutIcon(), stat.amount, percentage));
                }
            }
            sb.append("\n");
        }

        if (totalExpenses > 0) {
            sb.append("📉 РАСХОДЫ:\n");
            for (CategoryStats stat : incomeStats.values()) {
                if (!stat.isIncome) {
                    double percentage = (stat.amount / totalExpenses) * 100;
                    sb.append(String.format(Locale.getDefault(), "  %s: %.2f руб. (%.1f%%)\n",
                            stat.getNameWithoutIcon(), stat.amount, percentage));
                }
            }
        }

        return sb.toString();
    }

    // Класс для статистики категории
    public static class CategoryStats {
        public String category;
        public boolean isIncome;
        public double amount;
        public int count;
        public List<Transaction> transactions;

        public CategoryStats(String category, boolean isIncome) {
            this.category = category;
            this.isIncome = isIncome;
            this.amount = 0;
            this.count = 0;
            this.transactions = new ArrayList<>();
        }

        public String getFormattedAmount() {
            return String.format(Locale.getDefault(), "%.2f руб.", amount);
        }

        public String getIcon() {
            // Извлекаем иконку из категории
            if (category.contains(" ")) {
                return category.split(" ")[0];
            }
            return isIncome ? "💰" : "💸";
        }

        public String getNameWithoutIcon() {
            // Убираем иконку из названия категории
            if (category.contains(" ")) {
                return category.substring(category.indexOf(" ") + 1);
            }
            return category;
        }
    }
}