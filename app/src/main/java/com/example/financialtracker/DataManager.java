package com.example.financialtracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DataManager {
    private static final String TAG = "DataManager";
    private static final String PREFS_NAME = "FinancialTrackerPrefs";
    private static final String KEY_BALANCE = "balance";
    private static final String KEY_TOTAL_INCOME = "total_income";
    private static final String KEY_TOTAL_EXPENSES = "total_expenses";
    private static final String KEY_TRANSACTIONS = "transactions";
    private static final String KEY_ACCOUNTS = "accounts";
    private static final String KEY_TRANSFERS = "transfers";
    private static final String KEY_GOALS = "goals";
    private static final String KEY_BUDGETS = "budgets";

    private SharedPreferences sharedPreferences;
    private Gson gson;

    public DataManager(Context context) {
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    // ==================== БАЛАНСЫ ====================

    public double getBalance() {
        return sharedPreferences.getFloat(KEY_BALANCE, 0.0f);
    }

    public double getTotalIncome() {
        return sharedPreferences.getFloat(KEY_TOTAL_INCOME, 0.0f);
    }

    public double getTotalExpenses() {
        return sharedPreferences.getFloat(KEY_TOTAL_EXPENSES, 0.0f);
    }

    public void saveTotals(double balance, double totalIncome, double totalExpenses) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat(KEY_BALANCE, (float) balance);
        editor.putFloat(KEY_TOTAL_INCOME, (float) totalIncome);
        editor.putFloat(KEY_TOTAL_EXPENSES, (float) totalExpenses);
        editor.apply();
        Log.d(TAG, "saveTotals: balance=" + balance + ", income=" + totalIncome + ", expenses=" + totalExpenses);
    }

    public double[] loadTotals() {
        double balance = sharedPreferences.getFloat(KEY_BALANCE, 0);
        double income = sharedPreferences.getFloat(KEY_TOTAL_INCOME, 0);
        double expenses = sharedPreferences.getFloat(KEY_TOTAL_EXPENSES, 0);
        return new double[]{balance, income, expenses};
    }

    // ==================== СЧЕТА ====================

    public List<Account> loadAccounts() {
        List<Account> accounts = new ArrayList<>();

        try {
            String accountsJson = sharedPreferences.getString(KEY_ACCOUNTS, "[]");
            Log.d(TAG, "loadAccounts: raw JSON = " + accountsJson);

            JSONArray accountsArray = new JSONArray(accountsJson);
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

            for (int i = 0; i < accountsArray.length(); i++) {
                JSONObject accountObj = accountsArray.getJSONObject(i);

                String name = accountObj.getString("name");
                String type = accountObj.optString("type", "Основной");
                String paymentType = accountObj.optString("paymentType", "Карта");
                double balance = accountObj.getDouble("balance");
                String notes = accountObj.optString("notes", "");

                Log.d(TAG, "Загружен счет: " + name + ", баланс: " + balance + " руб.");

                Date createdDate = new Date();
                try {
                    if (accountObj.has("createdDate")) {
                        String dateStr = accountObj.getString("createdDate");
                        createdDate = sdf.parse(dateStr);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                boolean includedInTotalBalance = accountObj.optBoolean("includedInTotalBalance", true);

                String id = accountObj.optString("id", "");

                Account account = new Account(name, type, paymentType, balance,
                        notes, createdDate, includedInTotalBalance);

                if (!id.isEmpty()) {
                    account.setId(id);
                }

                if (accountObj.has("timestamp")) {
                    account.setTimestamp(accountObj.getLong("timestamp"));
                }

                accounts.add(account);
            }

            Log.d(TAG, "loadAccounts: loaded " + accounts.size() + " accounts");
        } catch (Exception e) {
            Log.e(TAG, "Error loading accounts: " + e.getMessage());
            e.printStackTrace();
        }

        return accounts;
    }

    public void saveAccounts(List<Account> accounts) {
        try {
            JSONArray accountsArray = new JSONArray();
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

            for (Account account : accounts) {
                JSONObject accountObj = new JSONObject();
                accountObj.put("id", account.getId());
                accountObj.put("name", account.getName());
                accountObj.put("type", account.getType());
                accountObj.put("paymentType", account.getPaymentType());
                accountObj.put("balance", account.getBalance());
                accountObj.put("notes", account.getNotes());
                accountObj.put("createdDate", sdf.format(account.getCreatedDate()));
                accountObj.put("includedInTotalBalance", account.isIncludedInTotalBalance());
                accountObj.put("timestamp", account.getTimestamp());
                accountsArray.put(accountObj);

                Log.d(TAG, "Сохранен счет: " + account.getName() + ", баланс: " + account.getBalance() + " руб.");
            }

            String jsonString = accountsArray.toString();
            sharedPreferences.edit().putString(KEY_ACCOUNTS, jsonString).apply();

            Log.d(TAG, "saveAccounts: saved " + accounts.size() + " accounts");
        } catch (Exception e) {
            Log.e(TAG, "Error saving accounts: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public double getTotalAccountsBalance() {
        List<Account> accounts = loadAccounts();
        double total = 0;
        for (Account account : accounts) {
            if (account.isIncludedInTotalBalance()) {
                total += account.getBalance();
            }
        }
        return total;
    }

    public void addAccount(Account account) {
        List<Account> accounts = loadAccounts();
        accounts.add(account);
        saveAccounts(accounts);
        Log.d(TAG, "addAccount: добавлен счет " + account.getName() + " с балансом " + account.getBalance());
    }

    public void updateAccount(Account updatedAccount) {
        List<Account> accounts = loadAccounts();
        for (int i = 0; i < accounts.size(); i++) {
            if (accounts.get(i).getId().equals(updatedAccount.getId())) {
                accounts.set(i, updatedAccount);
                Log.d(TAG, "updateAccount: обновлен счет " + updatedAccount.getName() +
                        ", новый баланс: " + updatedAccount.getBalance());
                break;
            }
        }
        saveAccounts(accounts);
    }

    public void deleteAccount(Account account) {
        List<Account> accounts = loadAccounts();
        accounts.removeIf(a -> a.getId().equals(account.getId()));
        saveAccounts(accounts);
        Log.d(TAG, "deleteAccount: удален счет " + account.getName());
    }

    // ==================== ТРАНЗАКЦИИ ====================

    public List<Transaction> loadTransactions() {
        String json = sharedPreferences.getString(KEY_TRANSACTIONS, "[]");
        Log.d(TAG, "loadTransactions: raw JSON = " + json);

        if (json.isEmpty() || json.equals("[]")) {
            Log.d(TAG, "loadTransactions: нет транзакций");
            return new ArrayList<>();
        }

        Type type = new TypeToken<List<Transaction>>(){}.getType();
        List<Transaction> transactions = gson.fromJson(json, type);

        if (transactions != null) {
            for (Transaction transaction : transactions) {
                transaction.restoreDate();
            }
            Log.d(TAG, "loadTransactions: loaded " + transactions.size() + " transactions");
            return transactions;
        }

        return new ArrayList<>();
    }

    public void saveTransactions(List<Transaction> transactions) {
        String json = gson.toJson(transactions);
        sharedPreferences.edit().putString(KEY_TRANSACTIONS, json).apply();
        Log.d(TAG, "saveTransactions: saved " + transactions.size() + " transactions");
    }

    public void addTransaction(Transaction transaction) {
        List<Transaction> transactions = loadTransactions();
        transactions.add(0, transaction);
        saveTransactions(transactions);
        Log.d(TAG, "addTransaction: добавлена транзакция: " + transaction.getDescription());
    }

    // ==================== БЮДЖЕТЫ ====================

    public List<Budget> loadBudgets() {
        List<Budget> budgets = new ArrayList<>();

        try {
            String budgetsJson = sharedPreferences.getString(KEY_BUDGETS, "[]");
            JSONArray budgetsArray = new JSONArray(budgetsJson);
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

            for (int i = 0; i < budgetsArray.length(); i++) {
                JSONObject budgetObj = budgetsArray.getJSONObject(i);
                String category = budgetObj.getString("category");
                double amount = budgetObj.getDouble("amount");
                double spent = budgetObj.getDouble("spent");
                String period = budgetObj.getString("period");

                Budget budget;

                if (period.equals("произвольный") && budgetObj.has("startDate") && budgetObj.has("endDate")) {
                    Date startDate = sdf.parse(budgetObj.getString("startDate"));
                    Date endDate = sdf.parse(budgetObj.getString("endDate"));
                    budget = new Budget(category, amount, startDate, endDate);
                } else {
                    budget = new Budget(category, amount, period);
                }

                budget.setSpent(spent);

                if (budgetObj.has("id")) {
                    budget.setId(budgetObj.getString("id"));
                }

                if (budgetObj.has("active")) {
                    budget.setActive(budgetObj.getBoolean("active"));
                }

                budgets.add(budget);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return budgets;
    }

    public void saveBudgets(List<Budget> budgets) {
        try {
            JSONArray budgetsArray = new JSONArray();
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

            for (Budget budget : budgets) {
                JSONObject budgetObj = new JSONObject();
                budgetObj.put("id", budget.getId());
                budgetObj.put("category", budget.getCategory());
                budgetObj.put("amount", budget.getAmount());
                budgetObj.put("spent", budget.getSpent());
                budgetObj.put("period", budget.getPeriod());
                budgetObj.put("active", budget.isActive());

                if (budget.getStartDate() != null) {
                    budgetObj.put("startDate", sdf.format(budget.getStartDate()));
                }

                if (budget.getEndDate() != null) {
                    budgetObj.put("endDate", sdf.format(budget.getEndDate()));
                }

                budgetsArray.put(budgetObj);
            }

            sharedPreferences.edit().putString(KEY_BUDGETS, budgetsArray.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addBudget(Budget budget) {
        List<Budget> budgets = loadBudgets();
        budgets.add(budget);
        saveBudgets(budgets);
        Log.d(TAG, "addBudget: добавлен бюджет для " + budget.getCategory());
    }

    public void updateBudget(Budget updatedBudget) {
        List<Budget> budgets = loadBudgets();
        for (int i = 0; i < budgets.size(); i++) {
            if (budgets.get(i).getId().equals(updatedBudget.getId())) {
                budgets.set(i, updatedBudget);
                break;
            }
        }
        saveBudgets(budgets);
        Log.d(TAG, "updateBudget: обновлен бюджет для " + updatedBudget.getCategory());
    }

    public void deleteBudgetById(String budgetId) {
        List<Budget> budgets = loadBudgets();
        budgets.removeIf(b -> b.getId().equals(budgetId));
        saveBudgets(budgets);
        Log.d(TAG, "deleteBudgetById: удален бюджет с id " + budgetId);
    }

    public void resetBudgetSpent(String budgetId) {
        List<Budget> budgets = loadBudgets();
        for (Budget budget : budgets) {
            if (budget.getId().equals(budgetId)) {
                budget.setSpent(0);
                Log.d(TAG, "resetBudgetSpent: сброшены траты для бюджета " + budget.getCategory());
                break;
            }
        }
        saveBudgets(budgets);
    }

    // ==================== ЦЕЛИ ====================

    public List<FinancialGoal> loadGoals() {
        List<FinancialGoal> goals = new ArrayList<>();

        try {
            String goalsJson = sharedPreferences.getString(KEY_GOALS, "[]");
            JSONArray goalsArray = new JSONArray(goalsJson);
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

            for (int i = 0; i < goalsArray.length(); i++) {
                JSONObject goalObj = goalsArray.getJSONObject(i);

                String id = goalObj.has("id") ? goalObj.getString("id") : "";
                String name = goalObj.getString("name");
                double targetAmount = goalObj.getDouble("targetAmount");
                double currentAmount = goalObj.getDouble("currentAmount");
                Date deadline = sdf.parse(goalObj.getString("deadline"));
                Date createdDate = sdf.parse(goalObj.getString("createdDate"));
                String priority = goalObj.getString("priority");
                String notes = goalObj.has("notes") ? goalObj.getString("notes") : "";
                boolean completed = goalObj.getBoolean("completed");
                String category = goalObj.has("category") ? goalObj.getString("category") : "";
                boolean autoSave = goalObj.has("autoSave") && goalObj.getBoolean("autoSave");

                FinancialGoal goal = new FinancialGoal(name, targetAmount, currentAmount,
                        deadline, priority, notes);

                if (!id.isEmpty()) {
                    goal.setId(id);
                }

                goal.setCreatedDate(createdDate);
                goal.setCompleted(completed);
                goal.setCategory(category);
                goal.setAutoSave(autoSave);

                goals.add(goal);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return goals;
    }

    public void saveGoals(List<FinancialGoal> goals) {
        try {
            JSONArray goalsArray = new JSONArray();
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

            for (FinancialGoal goal : goals) {
                JSONObject goalObj = new JSONObject();
                goalObj.put("id", goal.getId());
                goalObj.put("name", goal.getName());
                goalObj.put("targetAmount", goal.getTargetAmount());
                goalObj.put("currentAmount", goal.getCurrentAmount());
                goalObj.put("deadline", sdf.format(goal.getDeadline()));
                goalObj.put("createdDate", sdf.format(goal.getCreatedDate()));
                goalObj.put("priority", goal.getPriority());
                goalObj.put("notes", goal.getNotes());
                goalObj.put("completed", goal.isCompleted());
                goalObj.put("category", goal.getCategory() != null ? goal.getCategory() : "");
                goalObj.put("autoSave", goal.isAutoSave());
                goalsArray.put(goalObj);
            }

            sharedPreferences.edit().putString(KEY_GOALS, goalsArray.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addGoal(FinancialGoal goal) {
        List<FinancialGoal> goals = loadGoals();
        goals.add(goal);
        saveGoals(goals);
        Log.d(TAG, "addGoal: добавлена цель " + goal.getName());
    }

    public void updateGoal(FinancialGoal updatedGoal) {
        List<FinancialGoal> goals = loadGoals();
        for (int i = 0; i < goals.size(); i++) {
            if (goals.get(i).getId().equals(updatedGoal.getId())) {
                goals.set(i, updatedGoal);
                break;
            }
        }
        saveGoals(goals);
        Log.d(TAG, "updateGoal: обновлена цель " + updatedGoal.getName());
    }

    public void deleteGoalById(String goalId) {
        List<FinancialGoal> goals = loadGoals();
        goals.removeIf(g -> g.getId().equals(goalId));
        saveGoals(goals);
        Log.d(TAG, "deleteGoalById: удалена цель с id " + goalId);
    }

    public void addToGoal(String goalId, double amount) {
        List<FinancialGoal> goals = loadGoals();
        for (FinancialGoal goal : goals) {
            if (goal.getId().equals(goalId)) {
                double newAmount = goal.getCurrentAmount() + amount;
                goal.setCurrentAmount(newAmount);
                Log.d(TAG, "addToGoal: добавлено " + amount + " к цели " + goal.getName());
                break;
            }
        }
        saveGoals(goals);
    }

    public void completeGoal(String goalId) {
        List<FinancialGoal> goals = loadGoals();
        for (FinancialGoal goal : goals) {
            if (goal.getId().equals(goalId)) {
                goal.setCompleted(true);
                Log.d(TAG, "completeGoal: цель выполнена " + goal.getName());
                break;
            }
        }
        saveGoals(goals);
    }

    // ==================== ТРАНСФЕРЫ ====================

    public List<Transfer> loadTransfers() {
        String json = sharedPreferences.getString(KEY_TRANSFERS, "[]");
        if (json.isEmpty()) {
            return new ArrayList<>();
        }

        Type type = new TypeToken<List<Transfer>>(){}.getType();
        List<Transfer> transfers = gson.fromJson(json, type);

        if (transfers != null) {
            for (Transfer transfer : transfers) {
                transfer.restoreDate();
            }
            return transfers;
        }

        return new ArrayList<>();
    }

    public void saveTransfers(List<Transfer> transfers) {
        String json = gson.toJson(transfers);
        sharedPreferences.edit().putString(KEY_TRANSFERS, json).apply();
    }

    public void addTransfer(Transfer transfer) {
        List<Transfer> transfers = loadTransfers();
        transfers.add(0, transfer);
        saveTransfers(transfers);
    }

    // ==================== ОЧИСТКА ====================

    public void clearAllData() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
        Log.d(TAG, "All data cleared");
    }

    public long getDataSize() {
        try {
            long size = 0;
            String allData = sharedPreferences.getAll().toString();
            size += allData.getBytes().length;
            return size;
        } catch (Exception e) {
            return 0;
        }
    }

    public double getTotalAccountsBalanceInRub(CbrRateManager rateManager) {
        List<Account> accounts = loadAccounts();
        double total = 0;
        for (Account account : accounts) {
            if (account.isIncludedInTotalBalance()) {
                total += account.getBalance();
            }
        }
        Log.d(TAG, "getTotalAccountsBalanceInRub: total = " + total);
        return total;
    }
}