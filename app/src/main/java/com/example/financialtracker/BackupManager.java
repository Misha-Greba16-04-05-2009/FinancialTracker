package com.example.financialtracker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BackupManager {
    private static final String TAG = "BackupManager";
    private static final String PREFS_NAME = "backup_prefs";
    private static final String KEY_LAST_BACKUP = "last_backup";
    private static final String KEY_AUTO_BACKUP = "auto_backup";
    private static final String KEY_BACKUP_HISTORY = "backup_history";

    private Context context;
    private DataManager dataManager;
    private SharedPreferences sharedPreferences;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat fileDateFormat;

    // Для отслеживания операции экспорта
    private Uri pendingExportUri;

    public BackupManager(Context context) {
        this.context = context;
        this.dataManager = new DataManager(context);
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault());
        this.fileDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
    }

    // ============ ЭКСПОРТ ДАННЫХ ============

    public boolean exportToFile() {
        try {
            String backupData = exportToJson();

            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/json");
            intent.putExtra(Intent.EXTRA_TITLE, generateBackupFileName());

            if (context instanceof MainActivity) {
                ((MainActivity) context).startActivityForResult(intent, 1001);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Export error: " + e.getMessage());
        }
        return false;
    }

    // НОВЫЙ МЕТОД: сохранение данных после выбора URI
    public boolean saveExportData(Uri uri) {
        try {
            String backupData = exportToJson();
            OutputStream outputStream = context.getContentResolver().openOutputStream(uri);
            if (outputStream != null) {
                outputStream.write(backupData.getBytes());
                outputStream.close();

                // Сохраняем в историю
                addToBackupHistory(backupData.length());

                showToast("✅ Данные экспортированы успешно");
                Log.d(TAG, "Export saved to: " + uri.toString());
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving export: " + e.getMessage());
            showToast("❌ Ошибка сохранения: " + e.getMessage());
        }
        return false;
    }

    public String exportToJson() {
        try {
            JSONObject backupObj = new JSONObject();
            backupObj.put("version", 3);
            backupObj.put("exportDate", dateFormat.format(new Date()));
            backupObj.put("appName", "FinancialTracker");
            backupObj.put("device", android.os.Build.MODEL);
            backupObj.put("androidVersion", android.os.Build.VERSION.RELEASE);

            // 1. Транзакции
            List<Transaction> transactions = dataManager.loadTransactions();
            JSONArray transactionsArray = new JSONArray();
            for (Transaction t : transactions) {
                JSONObject obj = new JSONObject();
                obj.put("id", t.getId());
                obj.put("description", t.getDescription());
                obj.put("amount", t.getAmount());
                obj.put("isIncome", t.isIncome());
                obj.put("timestamp", t.getTimestamp());
                obj.put("category", t.getCategory());
                obj.put("notes", t.getNotes());
                obj.put("accountName", t.getAccountName() != null ? t.getAccountName() : "");
                obj.put("paymentType", t.getPaymentType() != null ? t.getPaymentType() : "Карта");
                transactionsArray.put(obj);
            }
            backupObj.put("transactions", transactionsArray);

            // 2. Счета
            List<Account> accounts = dataManager.loadAccounts();
            JSONArray accountsArray = new JSONArray();
            for (Account a : accounts) {
                JSONObject obj = new JSONObject();
                obj.put("id", a.getId());
                obj.put("name", a.getName());
                obj.put("type", a.getType());
                obj.put("paymentType", a.getPaymentType());
                obj.put("balance", a.getBalance());
                obj.put("notes", a.getNotes());
                obj.put("createdDate", dateFormat.format(a.getCreatedDate()));
                obj.put("includedInTotalBalance", a.isIncludedInTotalBalance());
                obj.put("timestamp", a.getTimestamp());
                accountsArray.put(obj);
            }
            backupObj.put("accounts", accountsArray);

            // 3. Переводы
            List<Transfer> transfers = dataManager.loadTransfers();
            JSONArray transfersArray = new JSONArray();
            for (Transfer t : transfers) {
                JSONObject obj = new JSONObject();
                obj.put("id", t.getId());
                obj.put("fromAccount", t.getFromAccount());
                obj.put("toAccount", t.getToAccount());
                obj.put("amount", t.getAmount());
                obj.put("commission", t.getCommission());
                obj.put("description", t.getDescription());
                obj.put("notes", t.getNotes());
                obj.put("timestamp", t.getTimestamp());
                transfersArray.put(obj);
            }
            backupObj.put("transfers", transfersArray);

            // 4. Цели
            List<FinancialGoal> goals = dataManager.loadGoals();
            JSONArray goalsArray = new JSONArray();
            for (FinancialGoal g : goals) {
                JSONObject obj = new JSONObject();
                obj.put("id", g.getId());
                obj.put("name", g.getName());
                obj.put("targetAmount", g.getTargetAmount());
                obj.put("currentAmount", g.getCurrentAmount());
                obj.put("deadline", dateFormat.format(g.getDeadline()));
                obj.put("createdDate", dateFormat.format(g.getCreatedDate()));
                obj.put("priority", g.getPriority());
                obj.put("notes", g.getNotes());
                obj.put("completed", g.isCompleted());
                obj.put("category", g.getCategory() != null ? g.getCategory() : "");
                obj.put("autoSave", g.isAutoSave());
                goalsArray.put(obj);
            }
            backupObj.put("goals", goalsArray);

            // 5. Бюджеты
            List<Budget> budgets = dataManager.loadBudgets();
            JSONArray budgetsArray = new JSONArray();
            for (Budget b : budgets) {
                JSONObject obj = new JSONObject();
                obj.put("id", b.getId());
                obj.put("category", b.getCategory());
                obj.put("amount", b.getAmount());
                obj.put("spent", b.getSpent());
                obj.put("period", b.getPeriod());
                obj.put("startDate", dateFormat.format(b.getStartDate()));
                obj.put("endDate", dateFormat.format(b.getEndDate()));
                obj.put("active", b.isActive());
                budgetsArray.put(obj);
            }
            backupObj.put("budgets", budgetsArray);

            // 6. Пользовательские курсы валют
            if (context instanceof MainActivity) {
                CbrRateManager rateManager = ((MainActivity) context).getRateManager();
                if (rateManager != null) {
                    JSONObject customRatesObj = new JSONObject();
                    List<String> currencies = rateManager.getSupportedCurrencies();
                    for (String currency : currencies) {
                        if (rateManager.isCustomRate(currency)) {
                            customRatesObj.put(currency, rateManager.getRate(currency));
                        }
                    }
                    backupObj.put("customRates", customRatesObj);
                }
            }

            // 7. Тоталы
            double[] totals = dataManager.loadTotals();
            backupObj.put("balance", totals[0]);
            backupObj.put("totalIncome", totals[1]);
            backupObj.put("totalExpenses", totals[2]);

            // 8. Статистика
            JSONObject statsObj = new JSONObject();
            statsObj.put("totalTransactions", transactions.size());
            statsObj.put("totalAccounts", accounts.size());
            statsObj.put("totalGoals", goals.size());
            statsObj.put("totalBudgets", budgets.size());
            statsObj.put("totalTransfers", transfers.size());
            backupObj.put("statistics", statsObj);

            return backupObj.toString(2);

        } catch (Exception e) {
            Log.e(TAG, "Error creating JSON: " + e.getMessage());
            return "{}";
        }
    }

    // ============ ИМПОРТ ДАННЫХ ============

    public boolean importFromFile(Uri fileUri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(fileUri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }
            reader.close();
            inputStream.close();

            boolean success = restoreFromJson(jsonBuilder.toString());

            if (success && context instanceof MainActivity) {
                MainActivity activity = (MainActivity) context;
                activity.runOnUiThread(() -> {
                    activity.updateFragments();
                    activity.updateNavHeader();
                    activity.updateAccountsFragment();
                });
            }

            return success;

        } catch (Exception e) {
            Log.e(TAG, "Import error: " + e.getMessage());
            showToast("Ошибка импорта: " + e.getMessage());
            return false;
        }
    }

    public boolean restoreFromJson(String json) {
        try {
            JSONObject backupObj = new JSONObject(json);

            int version = backupObj.optInt("version", 1);
            Log.d(TAG, "Restoring backup version: " + version);

            // 1. Восстанавливаем транзакции
            if (backupObj.has("transactions")) {
                JSONArray transactionsArray = backupObj.getJSONArray("transactions");
                List<Transaction> transactions = new ArrayList<>();

                for (int i = 0; i < transactionsArray.length(); i++) {
                    JSONObject transactionObj = transactionsArray.getJSONObject(i);

                    String description = transactionObj.getString("description");
                    double amount = transactionObj.getDouble("amount");
                    boolean isIncome = transactionObj.getBoolean("isIncome");

                    // ИСПРАВЛЕНИЕ: используем timestamp вместо date
                    long timestamp = transactionObj.getLong("timestamp");
                    Date date = new Date(timestamp);

                    String category = transactionObj.getString("category");
                    String notes = transactionObj.optString("notes", "");
                    String accountName = transactionObj.optString("accountName", "");
                    String paymentType = transactionObj.optString("paymentType", "Карта");

                    Transaction transaction = new Transaction(
                            description,
                            amount,
                            isIncome,
                            date,
                            category,
                            notes,
                            accountName,
                            paymentType
                    );

                    if (transactionObj.has("id")) {
                        transaction.setId(transactionObj.getString("id"));
                    }

                    transactions.add(transaction);
                }
                dataManager.saveTransactions(transactions);
                Log.d(TAG, "Restored " + transactions.size() + " transactions");
            }

            // 2. Восстанавливаем счета
            if (backupObj.has("accounts")) {
                JSONArray accountsArray = backupObj.getJSONArray("accounts");
                List<Account> accounts = new ArrayList<>();

                for (int i = 0; i < accountsArray.length(); i++) {
                    JSONObject accountObj = accountsArray.getJSONObject(i);

                    String name = accountObj.getString("name");
                    String type = accountObj.optString("type", "Основной");
                    String paymentType = accountObj.optString("paymentType", "Карта");
                    double balance = accountObj.getDouble("balance");
                    String notes = accountObj.optString("notes", "");

                    long timestamp = accountObj.getLong("timestamp");
                    Date createdDate = new Date(timestamp);

                    boolean includedInTotalBalance = accountObj.optBoolean("includedInTotalBalance", true);

                    Account account = new Account(
                            name,
                            type,
                            paymentType,
                            balance,
                            notes,
                            createdDate,
                            includedInTotalBalance
                    );

                    if (accountObj.has("id")) {
                        account.setId(accountObj.getString("id"));
                    }

                    accounts.add(account);
                }
                dataManager.saveAccounts(accounts);
                Log.d(TAG, "Restored " + accounts.size() + " accounts");
            }

            // 3. Восстанавливаем переводы
            if (backupObj.has("transfers")) {
                JSONArray transfersArray = backupObj.getJSONArray("transfers");
                List<Transfer> transfers = new ArrayList<>();

                for (int i = 0; i < transfersArray.length(); i++) {
                    JSONObject transferObj = transfersArray.getJSONObject(i);

                    Transfer transfer = new Transfer();
                    transfer.setFromAccount(transferObj.getString("fromAccount"));
                    transfer.setToAccount(transferObj.getString("toAccount"));
                    transfer.setAmount(transferObj.getDouble("amount"));
                    transfer.setCommission(transferObj.optDouble("commission", 0));
                    transfer.setDescription(transferObj.optString("description", ""));
                    transfer.setNotes(transferObj.optString("notes", ""));

                    if (transferObj.has("id")) {
                        transfer.setId(transferObj.getString("id"));
                    }
                    if (transferObj.has("timestamp")) {
                        transfer.setTimestamp(transferObj.getLong("timestamp"));
                    }

                    transfers.add(transfer);
                }
                dataManager.saveTransfers(transfers);
                Log.d(TAG, "Restored " + transfers.size() + " transfers");
            }

            // 4. Восстанавливаем цели
            if (backupObj.has("goals")) {
                JSONArray goalsArray = backupObj.getJSONArray("goals");
                List<FinancialGoal> goals = new ArrayList<>();

                for (int i = 0; i < goalsArray.length(); i++) {
                    JSONObject goalObj = goalsArray.getJSONObject(i);

                    String name = goalObj.getString("name");
                    double targetAmount = goalObj.getDouble("targetAmount");
                    double currentAmount = goalObj.getDouble("currentAmount");

                    // В бэкапе даты записаны строкой — раньше читали как число, и восстановление падало
                    Date deadline = readDate(goalObj, "deadline");
                    Date createdDate = readDate(goalObj, "createdDate");

                    String priority = goalObj.optString("priority", "Средний");
                    String notes = goalObj.optString("notes", "");
                    boolean completed = goalObj.optBoolean("completed", false);
                    String category = goalObj.optString("category", "");
                    boolean autoSave = goalObj.optBoolean("autoSave", false);

                    FinancialGoal goal = new FinancialGoal(
                            name,
                            targetAmount,
                            currentAmount,
                            deadline,
                            priority,
                            notes
                    );

                    goal.setCreatedDate(createdDate);
                    goal.setCompleted(completed);
                    goal.setCategory(category);
                    goal.setAutoSave(autoSave);

                    if (goalObj.has("id")) {
                        goal.setId(goalObj.getString("id"));
                    }

                    goals.add(goal);
                }
                dataManager.saveGoals(goals);
                Log.d(TAG, "Restored " + goals.size() + " goals");
            }

            // 5. Восстанавливаем бюджеты
            if (backupObj.has("budgets")) {
                JSONArray budgetsArray = backupObj.getJSONArray("budgets");
                List<Budget> budgets = new ArrayList<>();

                for (int i = 0; i < budgetsArray.length(); i++) {
                    JSONObject budgetObj = budgetsArray.getJSONObject(i);

                    String category = budgetObj.getString("category");
                    double amount = budgetObj.getDouble("amount");
                    double spent = budgetObj.getDouble("spent");
                    String period = budgetObj.getString("period");

                    Date startDate = readDate(budgetObj, "startDate");
                    Date endDate = readDate(budgetObj, "endDate");

                    boolean active = budgetObj.optBoolean("active", true);

                    // Сохраняем тип периода (неделя/месяц/год), а не превращаем всё в "произвольный"
                    Budget budget = "произвольный".equals(period)
                            ? new Budget(category, amount, startDate, endDate)
                            : new Budget(category, amount, period);
                    budget.setSpent(spent);
                    budget.setActive(active);

                    if (budgetObj.has("id")) {
                        budget.setId(budgetObj.getString("id"));
                    }

                    budgets.add(budget);
                }
                dataManager.saveBudgets(budgets);
                Log.d(TAG, "Restored " + budgets.size() + " budgets");
            }

            // 6. Восстанавливаем пользовательские курсы валют
            if (backupObj.has("customRates") && context instanceof MainActivity) {
                JSONObject customRatesObj = backupObj.getJSONObject("customRates");
                CbrRateManager rateManager = ((MainActivity) context).getRateManager();

                if (rateManager != null) {
                    rateManager.resetAllCustomRates();
                    JSONArray names = customRatesObj.names();
                    if (names != null) {
                        for (int j = 0; j < names.length(); j++) {
                            String currency = names.getString(j);
                            double rate = customRatesObj.getDouble(currency);
                            rateManager.setCustomRate(currency, rate);
                        }
                    }
                    Log.d(TAG, "Restored custom rates");
                }
            }

            // 7. Восстанавливаем тоталы
            if (backupObj.has("balance") && backupObj.has("totalIncome") && backupObj.has("totalExpenses")) {
                double balance = backupObj.getDouble("balance");
                double totalIncome = backupObj.getDouble("totalIncome");
                double totalExpenses = backupObj.getDouble("totalExpenses");
                dataManager.saveTotals(balance, totalIncome, totalExpenses);
            }

            showToast("✅ Данные успешно восстановлены");
            Log.d(TAG, "Data restored successfully from JSON");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error restoring from JSON: " + e.getMessage());
            e.printStackTrace();
            showToast("❌ Ошибка восстановления: " + e.getMessage());
            return false;
        }
    }

    /** Читает дату из бэкапа: строка "dd.MM.yyyy HH:mm:ss", "dd.MM.yyyy" или число миллисекунд. */
    private Date readDate(JSONObject obj, String key) {
        if (!obj.has(key) || obj.isNull(key)) return new Date();
        Object v = obj.opt(key);
        if (v instanceof Number) return new Date(((Number) v).longValue());
        String s = String.valueOf(v);
        String[] patterns = {"dd.MM.yyyy HH:mm:ss", "dd.MM.yyyy"};
        for (String p : patterns) {
            try {
                return new SimpleDateFormat(p, Locale.getDefault()).parse(s);
            } catch (Exception ignored) { }
        }
        try {
            return new Date(Long.parseLong(s));
        } catch (Exception e) {
            return new Date();
        }
    }

    // ============ АВТОМАТИЧЕСКИЙ БЭКАП ============

    public void createAutoBackup() {
        try {
            String backupData = exportToJson();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(KEY_AUTO_BACKUP, backupData);
            editor.putLong(KEY_LAST_BACKUP, System.currentTimeMillis());

            // Сохраняем историю бэкапов (последние 5)
            String historyJson = sharedPreferences.getString(KEY_BACKUP_HISTORY, "[]");
            JSONArray historyArray = new JSONArray(historyJson);

            JSONObject backupEntry = new JSONObject();
            backupEntry.put("timestamp", System.currentTimeMillis());
            backupEntry.put("date", dateFormat.format(new Date()));
            backupEntry.put("size", backupData.length());

            historyArray.put(backupEntry);

            // Оставляем только последние 5 записей
            while (historyArray.length() > 5) {
                historyArray.remove(0);
            }

            editor.putString(KEY_BACKUP_HISTORY, historyArray.toString());
            editor.apply();

            Log.d(TAG, "Auto backup created");
        } catch (Exception e) {
            Log.e(TAG, "Auto backup failed: " + e.getMessage());
        }
    }

    public boolean restoreAutoBackup() {
        try {
            String backupData = sharedPreferences.getString(KEY_AUTO_BACKUP, "");
            if (!backupData.isEmpty()) {
                return restoreFromJson(backupData);
            }
        } catch (Exception e) {
            Log.e(TAG, "Auto backup restore failed: " + e.getMessage());
        }
        return false;
    }

    public List<BackupEntry> getBackupHistory() {
        List<BackupEntry> history = new ArrayList<>();

        try {
            String historyJson = sharedPreferences.getString(KEY_BACKUP_HISTORY, "[]");
            JSONArray historyArray = new JSONArray(historyJson);

            for (int i = 0; i < historyArray.length(); i++) {
                JSONObject obj = historyArray.getJSONObject(i);
                BackupEntry entry = new BackupEntry(
                        obj.getLong("timestamp"),
                        obj.getString("date"),
                        obj.getInt("size")
                );
                history.add(entry);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading backup history: " + e.getMessage());
        }

        return history;
    }

    // НОВЫЙ МЕТОД: добавление записи в историю
    private void addToBackupHistory(int size) {
        try {
            String historyJson = sharedPreferences.getString(KEY_BACKUP_HISTORY, "[]");
            JSONArray historyArray = new JSONArray(historyJson);

            JSONObject backupEntry = new JSONObject();
            backupEntry.put("timestamp", System.currentTimeMillis());
            backupEntry.put("date", dateFormat.format(new Date()));
            backupEntry.put("size", size);

            historyArray.put(backupEntry);

            while (historyArray.length() > 5) {
                historyArray.remove(0);
            }

            sharedPreferences.edit().putString(KEY_BACKUP_HISTORY, historyArray.toString()).apply();
            sharedPreferences.edit().putLong(KEY_LAST_BACKUP, System.currentTimeMillis()).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error adding to history: " + e.getMessage());
        }
    }

    // ============ ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ============

    public long getLastBackupTime() {
        return sharedPreferences.getLong(KEY_LAST_BACKUP, 0);
    }

    public String getLastBackupFormatted() {
        long lastBackup = getLastBackupTime();
        if (lastBackup == 0) {
            return "Никогда";
        }
        return dateFormat.format(new Date(lastBackup));
    }

    private String generateBackupFileName() {
        return "financial_backup_" + fileDateFormat.format(new Date()) + ".json";
    }

    private void showToast(final String message) {
        if (context instanceof MainActivity) {
            ((MainActivity) context).runOnUiThread(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
        }
    }

    // ============ ВНУТРЕННИЙ КЛАСС ============

    public static class BackupEntry {
        public long timestamp;
        public String date;
        public int size;

        public BackupEntry(long timestamp, String date, int size) {
            this.timestamp = timestamp;
            this.date = date;
            this.size = size;
        }

        public String getFormattedSize() {
            if (size < 1024) {
                return size + " B";
            } else if (size < 1024 * 1024) {
                return String.format(Locale.getDefault(), "%.1f KB", size / 1024.0);
            } else {
                return String.format(Locale.getDefault(), "%.1f MB", size / (1024.0 * 1024.0));
            }
        }
    }
}