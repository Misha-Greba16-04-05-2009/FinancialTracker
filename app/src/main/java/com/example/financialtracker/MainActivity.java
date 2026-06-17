package com.example.financialtracker;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.view.Gravity;
import android.view.ViewGroup;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MainActivity";

    private AuthManager authManager;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private ImageButton menuButton;

    private double balance = 0.0;
    private double totalIncome = 0.0;
    private double totalExpenses = 0.0;
    private ArrayList<Transaction> transactions = new ArrayList<>();
    private DataManager dataManager;
    private BackupManager backupManager;
    private CategoryManager categoryManager;
    private CbrRateManager rateManager;

    private HomeFragment homeFragment;
    private DataFragment dataFragment;
    private StatisticsFragment statisticsFragment;

    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Инициализируем AuthManager
        authManager = new AuthManager(this);

        // Проверяем, авторизован ли пользователь
        if (!authManager.isLoggedIn()) {
            // Если не авторизован, переходим на экран входа
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        setContentView(R.layout.activity_main);

        Log.d(TAG, "=== ЗАПУСК ПРИЛОЖЕНИЯ ===");
        Log.d(TAG, "Пользователь: " + authManager.getCurrentUser().getUsername());

        dataManager = new DataManager(this);
        backupManager = new BackupManager(this);
        categoryManager = new CategoryManager(this);
        rateManager = CbrRateManager.getInstance(this);

        loadSavedData();

        initViews();
        setupNavigation();

        loadHomeFragment();

        checkExpenseWarning();

        Log.d(TAG, "Приложение загружено");
    }

    // ============ ПЕРЕСЧЕТ ОБЩЕГО БАЛАНСА (ИСПРАВЛЕНО) ============
    public void recalculateTotalBalance() {
        // Баланс = Общий доход - Общий расход
        balance = totalIncome - totalExpenses;
        Log.d(TAG, String.format(Locale.getDefault(),
                "recalculateTotalBalance: Доходы=%.2f, Расходы=%.2f, Баланс=%.2f",
                totalIncome, totalExpenses, balance));
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        menuButton = null;

        int menuButtonId = getResources().getIdentifier("menuButton", "id", getPackageName());
        if (menuButtonId != 0) {
            menuButton = findViewById(menuButtonId);
        }

        if (menuButton == null) {
            for (int i = 0; i < toolbar.getChildCount(); i++) {
                View child = toolbar.getChildAt(i);
                if (child instanceof ImageButton) {
                    menuButton = (ImageButton) child;
                    break;
                }
            }
        }

        if (menuButton == null) {
            menuButton = new ImageButton(this);
            menuButton.setImageResource(android.R.drawable.ic_menu_sort_by_size);
            menuButton.setBackgroundColor(Color.TRANSPARENT);
            menuButton.setContentDescription("Меню");

            Toolbar.LayoutParams params = new Toolbar.LayoutParams(
                    Toolbar.LayoutParams.WRAP_CONTENT,
                    Toolbar.LayoutParams.WRAP_CONTENT
            );
            menuButton.setLayoutParams(params);
            toolbar.addView(menuButton);
        }
    }

    private void setupNavigation() {
        menuButton.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        navigationView.setNavigationItemSelectedListener(this);
        updateNavHeader();
    }
    private void loadHomeFragment() {
        homeFragment = new HomeFragment();
        loadFragment(homeFragment, "Главная");
    }

    public void loadFragment(Fragment fragment, String title) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
    }

    // ============ ИСПРАВЛЕННЫЙ МЕТОД ОБНОВЛЕНИЯ ЗАГОЛОВКА ============
    public void updateNavHeader() {
        Log.d(TAG, "=== ОБНОВЛЕНИЕ ЗАГОЛОВКА ===");

        View headerView = navigationView.getHeaderView(0);
        if (headerView != null) {
            TextView iconView = headerView.findViewById(R.id.nav_header_icon);
            TextView titleView = headerView.findViewById(R.id.nav_header_title);
            TextView subtitleView = headerView.findViewById(R.id.nav_header_subtitle);
            TextView emailView = headerView.findViewById(R.id.nav_header_email);

            User currentUser = getCurrentUser();
            if (currentUser != null) {
                if (iconView != null) iconView.setText(currentUser.getProfileIcon());
                if (titleView != null) titleView.setText(currentUser.getFullName().isEmpty() ?
                        currentUser.getUsername() : currentUser.getFullName());
                if (subtitleView != null) subtitleView.setText("@" + currentUser.getUsername());
                if (emailView != null) emailView.setText(currentUser.getEmail());
            }
        }

        Log.d(TAG, "=== ОБНОВЛЕНИЕ ЗАГОЛОВКА ЗАВЕРШЕНО ===");
    }

    private void findTextViews(View view, List<TextView> result) {
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                if (child instanceof TextView) {
                    result.add((TextView) child);
                } else if (child instanceof ViewGroup) {
                    findTextViews(child, result);
                }
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        try {
            if (id == R.id.nav_home) {
                if (homeFragment == null) {
                    homeFragment = new HomeFragment();
                }
                loadFragment(homeFragment, "Главная");
            } else if (id == R.id.nav_data) {
                if (dataFragment == null) {
                    dataFragment = new DataFragment();
                }
                loadFragment(dataFragment, "Данные и записи");
            } else if (id == R.id.nav_statistics) {
                if (statisticsFragment == null) {
                    statisticsFragment = new StatisticsFragment();
                }
                loadFragment(statisticsFragment, "Аналитика");
            } else if (id == R.id.nav_reports_analytics) {
                ReportsAndAnalyticsFragment reportsFragment = new ReportsAndAnalyticsFragment();
                loadFragment(reportsFragment, "Аналитика и отчёты");
            } else if (id == R.id.nav_budgets) {
                BudgetsFragment budgetsFragment = new BudgetsFragment();
                loadFragment(budgetsFragment, "Бюджеты и цели");
            } else if (id == R.id.nav_goals) {
                GoalsFragment goalsFragment = new GoalsFragment();
                loadFragment(goalsFragment, "Финансовые цели");
            } else if (id == R.id.nav_expense_analysis) {
                FinancialAnalysisFragment financialAnalysisFragment = new FinancialAnalysisFragment();
                loadFragment(financialAnalysisFragment, "Анализ финансов");
            } else if (id == R.id.nav_accounts) {
                try {
                    AccountsFragment accountsFragment = new AccountsFragment();
                    loadFragment(accountsFragment, "Управление счетами");
                } catch (Exception e) {
                    Toast.makeText(this, "Ошибка открытия счетов: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            } else if (id == R.id.nav_receipt_scanner) {
                ReceiptScannerFragment scannerFragment = new ReceiptScannerFragment();
                loadFragment(scannerFragment, "📷 Сканер чеков");
            } else if (id == R.id.nav_transfers) {
                TransfersFragment transfersFragment = new TransfersFragment();
                loadFragment(transfersFragment, "Переводы между счетами");
            } else if (id == R.id.nav_currencies) {
                CbrCurrencyConverterFragment converterFragment = new CbrCurrencyConverterFragment();
                loadFragment(converterFragment, "💱 Конвертер валют");
            } else if (id == R.id.nav_rate_editor) {
                CurrencyRateEditorFragment editorFragment = new CurrencyRateEditorFragment();
                loadFragment(editorFragment, "✏️ Редактор курсов");
            } else if (id == R.id.nav_clear_history) {
                showClearOptionsDialog();
            } else if (id == R.id.nav_export) {
                boolean success = backupManager.exportToFile();
                if (success) {
                    Toast.makeText(this, "Экспорт успешно завершен", Toast.LENGTH_SHORT).show();
                }
            } else if (id == R.id.nav_import) {
                showImportDialog();
            } else if (id == R.id.nav_info) {
                showAppInfo();
            } else if (id == R.id.nav_help) {
                showHelpDialog();
            } else if (id == R.id.nav_logout) {
            new AlertDialog.Builder(this)
                    .setTitle("Выход из аккаунта")
                    .setMessage("Вы уверены, что хотите выйти?")
                    .setPositiveButton("Выйти", (dialog, which) -> logout())
                    .setNegativeButton("Отмена", null)
                    .show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void checkExpenseWarning() {
        if (totalExpenses > totalIncome) {
            showExpenseWarning();
        }
    }

    private void showExpenseWarning() {
        double difference = totalExpenses - totalIncome;
        double percentage = totalIncome > 0 ? (totalExpenses / totalIncome) * 100 : 0;

        new AlertDialog.Builder(this)
                .setTitle("⚠️ ВНИМАНИЕ! Превышение расходов!")
                .setMessage(String.format(Locale.getDefault(),
                        "Ваши расходы превышают доходы!\n\n" +
                                "📊 Статистика:\n" +
                                "📈 Доходы: %.2f руб.\n" +
                                "📉 Расходы: %.2f руб.\n" +
                                "❗️ Превышение: %.2f руб.\n" +
                                "Рекомендуется срочно пересмотреть свои финансовые привычки!",
                        totalIncome, totalExpenses, difference, percentage))
                .setPositiveButton("Понятно", (dialog, which) -> dialog.dismiss())
                .setNeutralButton("Начать с нуля", (dialog, which) -> showStartFromZeroDialog())
                .setCancelable(true)
                .show();
    }

    public void showStartFromZeroDialog() {
        new AlertDialog.Builder(this)
                .setTitle("🔄 Начать с нуля")
                .setMessage("Вы уверены, что хотите удалить ВСЕ данные и начать заново?\n\n" +
                        "❌ БУДЕТ УДАЛЕНО ВСЁ:\n" +
                        "• Транзакции (" + transactions.size() + " записей)\n" +
                        "• Счета (" + dataManager.loadAccounts().size() + " штук)\n" +
                        "• Переводы между счетами\n" +
                        "• Все финансовые данные\n\n" +
                        "⚠️ Это действие нельзя отменить!")
                .setPositiveButton("НАЧАТЬ С НУЛЯ", (dialog, which) -> startFromZero())
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void startFromZero() {
        clearAllData();
        Toast.makeText(this, "✅ Все данные удалены! Начинаем с чистого листа.",
                Toast.LENGTH_LONG).show();
    }

    private void showClearOptionsDialog() {
        if (transactions.isEmpty() && balance == 0) {
            Toast.makeText(this, "Нет данных для очистки", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Очистка данных")
                .setMessage("Выберите тип очистки:\n\n" +
                        "📊 Текущая статистика:\n" +
                        "• Баланс: " + String.format(Locale.getDefault(), "%.2f руб.", balance) + "\n" +
                        "• Записей: " + transactions.size())
                .setPositiveButton("Только историю", (dialog, which) -> showClearHistoryDialog())
                .setNegativeButton("Всё полностью", (dialog, which) -> showClearAllDialog())
                .setNeutralButton("Отмена", null)
                .show();
    }

    private void showClearHistoryDialog() {
        if (transactions.isEmpty()) {
            Toast.makeText(this, "История уже пуста", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Очистить только историю")
                .setMessage("Вы уверены, что хотите удалить все транзакции?\n\n" +
                        "✅ БУДЕТ СОХРАНЕНО:\n" +
                        "• Баланс: " + String.format(Locale.getDefault(), "%.2f руб.", balance) + "\n" +
                        "• Доходы: " + String.format(Locale.getDefault(), "%.2f руб.", totalIncome) + "\n" +
                        "• Расходы: " + String.format(Locale.getDefault(), "%.2f руб.", totalExpenses) + "\n\n" +
                        "❌ БУДЕТ УДАЛЕНО:\n" +
                        "• Все транзакции (" + transactions.size() + " записей)\n" +
                        "• Детальная история операций")
                .setPositiveButton("Очистить историю", (dialog, which) -> clearHistoryOnly())
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showClearAllDialog() {
        if (transactions.isEmpty() && balance == 0) {
            Toast.makeText(this, "Нет данных для удаления", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Очистить ВСЁ полностью")
                .setMessage("⚠️ ВНИМАНИЕ! Это действие нельзя отменить!\n\n" +
                        "❌ БУДЕТ УДАЛЕНО ВСЁ:\n" +
                        "• Баланс: " + String.format(Locale.getDefault(), "%.2f руб.", balance) + "\n" +
                        "• Доходы: " + String.format(Locale.getDefault(), "%.2f руб.", totalIncome) + "\n" +
                        "• Расходы: " + String.format(Locale.getDefault(), "%.2f руб.", totalExpenses) + "\n" +
                        "• Все транзакции (" + transactions.size() + " записей)\n\n" +
                        "Приложение вернется в исходное состояние.")
                .setPositiveButton("УДАЛИТЬ ВСЁ", (dialog, which) -> clearAllData())
                .setNegativeButton("Отмена", null)
                .show();
    }

    public void clearHistoryOnly() {
        double savedBalance = balance;
        double savedIncome = totalIncome;
        double savedExpenses = totalExpenses;

        transactions.clear();

        balance = savedBalance;
        totalIncome = savedIncome;
        totalExpenses = savedExpenses;

        saveAllData();
        updateFragments();
        updateNavHeader();

        Toast.makeText(this, "✅ История очищена!\nБаланс сохранен: " +
                        String.format(Locale.getDefault(), "%.2f руб.", balance),
                Toast.LENGTH_LONG).show();
    }

    private void clearAllData() {
        transactions.clear();
        balance = 0.0;
        totalIncome = 0.0;
        totalExpenses = 0.0;

        List<Account> accounts = new ArrayList<>();
        dataManager.saveAccounts(accounts);

        List<Transfer> transfers = new ArrayList<>();
        dataManager.saveTransfers(transfers);

        saveAllData();

        updateFragments();
        updateNavHeader();

        Toast.makeText(this, "✅ Все данные удалены!\nПриложение сброшено.",
                Toast.LENGTH_LONG).show();
    }

    public void updateFragments() {
        try {
            if (homeFragment != null && homeFragment.isAdded()) {
                homeFragment.updateDashboard();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (dataFragment != null && dataFragment.isAdded()) {
                dataFragment.updateTransactionsList();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (statisticsFragment != null && statisticsFragment.isAdded()) {
                statisticsFragment.updateStatistics();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateAccountsFragment() {
        try {
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (currentFragment instanceof AccountsFragment) {
                ((AccountsFragment) currentFragment).refreshAccounts();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadSavedData() {
        List<Transaction> loadedTransactions = dataManager.loadTransactions();
        if (loadedTransactions != null) {
            transactions.clear();
            transactions.addAll(loadedTransactions);

            for (Transaction transaction : transactions) {
                transaction.restoreDate();
            }
        }

        double[] totals = dataManager.loadTotals();
        balance = totals[0];
        totalIncome = totals[1];
        totalExpenses = totals[2];

        Log.d(TAG, "loadSavedData: баланс=" + balance + ", доходы=" + totalIncome + ", расходы=" + totalExpenses);
    }

    public void saveAllData() {
        dataManager.saveTransactions(transactions);
        dataManager.saveTotals(balance, totalIncome, totalExpenses);
        recalculateTotalBalance();
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveAllData();
    }

    @Override
    protected void onStop() {
        super.onStop();
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        long lastBackup = prefs.getLong("last_backup", 0);
        long currentTime = System.currentTimeMillis();
        long weekInMillis = 7 * 24 * 60 * 60 * 1000L;

        if (currentTime - lastBackup > weekInMillis) {
            backupManager.createAutoBackup();
            prefs.edit().putLong("last_backup", currentTime).apply();
        }
    }

    // Геттеры
    public double getBalance() {
        recalculateTotalBalance();
        return balance;
    }

    public double getTotalIncome() { return totalIncome; }
    public double getTotalExpenses() { return totalExpenses; }
    public ArrayList<Transaction> getTransactions() { return transactions; }
    public DataManager getDataManager() { return dataManager; }
    public CategoryManager getCategoryManager() { return categoryManager; }
    public CbrRateManager getRateManager() { return rateManager; }

    public double getTotalAccountsBalance() {
        return dataManager.getTotalAccountsBalance();
    }

    public void updateBalance(double newBalance) {
        this.balance = newBalance;
        saveAllData();
    }

    public void updateTotalIncome(double newIncome) {
        this.totalIncome = newIncome;
        recalculateTotalBalance();
        saveAllData();
    }

    public void updateTotalExpenses(double newExpenses) {
        this.totalExpenses = newExpenses;
        recalculateTotalBalance();
        saveAllData();
    }

    public void openReceiptScannerFromDialog(AlertDialog dialog) {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
        ReceiptScannerFragment scannerFragment = new ReceiptScannerFragment();
        loadFragment(scannerFragment, "📷 Сканер чеков");
    }

    public void showEnhancedAddTransactionDialogWithData(double amount, String shop, String comment) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_transaction_enhanced, null);

        final TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        final Button incomeTypeButton = dialogView.findViewById(R.id.incomeTypeButton);
        final Button expenseTypeButton = dialogView.findViewById(R.id.expenseTypeButton);
        final Spinner accountSpinner = dialogView.findViewById(R.id.accountSpinner);
        final Spinner categorySpinner = dialogView.findViewById(R.id.categorySpinner);
        final EditText newCategoryEditText = dialogView.findViewById(R.id.newCategoryEditText);
        final Button cashButton = dialogView.findViewById(R.id.cashButton);
        final Button cardButton = dialogView.findViewById(R.id.cardButton);
        final Button electronicButton = dialogView.findViewById(R.id.electronicButton);
        final EditText amountEditText = dialogView.findViewById(R.id.amountEditText);
        final EditText notesEditText = dialogView.findViewById(R.id.notesEditText);
        final LinearLayout dateLayout = dialogView.findViewById(R.id.dateLayout);
        final TextView dateTextView = dialogView.findViewById(R.id.dateTextView);
        final Button scanReceiptButton = dialogView.findViewById(R.id.scanReceiptButton);

        // Скрываем выбор валюты
        final Spinner currencySpinner = dialogView.findViewById(R.id.currencySpinner);
        if (currencySpinner != null) {
            currencySpinner.setVisibility(View.GONE);
        }

        final boolean[] currentIsIncome = {false}; // Расход по умолчанию
        final Date[] selectedDate = {new Date()};
        final String[] selectedPaymentType = {"Карта"};

        // Заполняем данными из чека
        if (amount > 0) {
            amountEditText.setText(String.valueOf(amount));
        }
        if (comment != null && !comment.isEmpty()) {
            notesEditText.setText(comment);
        }

        updateTypeButtons(incomeTypeButton, expenseTypeButton, currentIsIncome[0]);
        updatePaymentTypeButtons(cashButton, cardButton, electronicButton, selectedPaymentType[0]);
        dialogTitle.setText(currentIsIncome[0] ? "📈 Добавить доход" : "📉 Добавить расход");

        loadCategoriesToSpinner(categorySpinner, currentIsIncome[0]);
        loadAccountsToSpinner(accountSpinner);
        updateDateTextView(dateTextView, selectedDate[0]);

        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCategory = (String) parent.getItemAtPosition(position);
                boolean isOtherCategory = (selectedCategory != null &&
                        (selectedCategory.equals("Прочие доходы") || selectedCategory.equals("Прочие расходы")));
                newCategoryEditText.setVisibility(isOtherCategory ? View.VISIBLE : View.GONE);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        incomeTypeButton.setOnClickListener(v -> {
            currentIsIncome[0] = true;
            updateTypeButtons(incomeTypeButton, expenseTypeButton, currentIsIncome[0]);
            dialogTitle.setText("📈 Добавить доход");
            loadCategoriesToSpinner(categorySpinner, currentIsIncome[0]);
        });

        expenseTypeButton.setOnClickListener(v -> {
            currentIsIncome[0] = false;
            updateTypeButtons(incomeTypeButton, expenseTypeButton, currentIsIncome[0]);
            dialogTitle.setText("📉 Добавить расход");
            loadCategoriesToSpinner(categorySpinner, currentIsIncome[0]);
        });

        cashButton.setOnClickListener(v -> {
            selectedPaymentType[0] = "Наличные";
            updatePaymentTypeButtons(cashButton, cardButton, electronicButton, selectedPaymentType[0]);
        });

        cardButton.setOnClickListener(v -> {
            selectedPaymentType[0] = "Карта";
            updatePaymentTypeButtons(cashButton, cardButton, electronicButton, selectedPaymentType[0]);
        });

        electronicButton.setOnClickListener(v -> {
            selectedPaymentType[0] = "Электронные деньги";
            updatePaymentTypeButtons(cashButton, cardButton, electronicButton, selectedPaymentType[0]);
        });

        dateLayout.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(selectedDate[0]);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    MainActivity.this,
                    (view, year, month, dayOfMonth) -> {
                        Calendar cal = Calendar.getInstance();
                        cal.set(year, month, dayOfMonth);
                        selectedDate[0] = cal.getTime();
                        updateDateTextView(dateTextView, selectedDate[0]);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        // Кнопка сканирования чека
        scanReceiptButton.setOnClickListener(v -> {
            if (builder != null) {
                try {
                    AlertDialog dialog = builder.create();
                    if (dialog.isShowing()) {
                        dialog.dismiss();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            openReceiptScannerFromDialog(null);
        });

        builder.setView(dialogView)
                .setPositiveButton("Добавить", (dialog, which) -> {
                    try {
                        String amountStr = amountEditText.getText().toString().trim();
                        String notes = notesEditText.getText().toString().trim();
                        String category = (String) categorySpinner.getSelectedItem();

                        String accountDisplay = (String) accountSpinner.getSelectedItem();
                        String accountName = accountDisplay;
                        if (accountDisplay != null && accountDisplay.contains(" - ")) {
                            accountName = accountDisplay.substring(0, accountDisplay.indexOf(" - "));
                        }

                        if (amountStr.isEmpty()) {
                            Toast.makeText(MainActivity.this, "Введите сумму", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        double amountValue = Double.parseDouble(amountStr);
                        if (amountValue <= 0) {
                            Toast.makeText(MainActivity.this, "Сумма должна быть больше 0", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (category == null) {
                            Toast.makeText(MainActivity.this, "Выберите категорию", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if ((category.equals("Прочие доходы") || category.equals("Прочие расходы"))
                                && !newCategoryEditText.getText().toString().trim().isEmpty()) {
                            String newCategory = newCategoryEditText.getText().toString().trim();
                            if (currentIsIncome[0]) {
                                categoryManager.addIncomeCategory(newCategory);
                                category = newCategory;
                            } else {
                                categoryManager.addExpenseCategory(newCategory);
                                category = newCategory;
                            }
                        }

                        String description = notes.isEmpty() ?
                                (currentIsIncome[0] ? "Доход" : "Расход") + ": " + category :
                                notes;

                        Account selectedAccount = null;
                        List<Account> accounts = dataManager.loadAccounts();

                        for (Account acc : accounts) {
                            if (acc.getName().equals(accountName)) {
                                selectedAccount = acc;
                                break;
                            }
                        }

                        if (selectedAccount != null) {
                            double oldBalance = selectedAccount.getBalance();
                            if (currentIsIncome[0]) {
                                selectedAccount.setBalance(oldBalance + amountValue);
                            } else {
                                selectedAccount.setBalance(oldBalance - amountValue);
                            }
                            dataManager.saveAccounts(accounts);
                        }

                        Transaction transaction = new Transaction(
                                description,
                                amountValue,
                                currentIsIncome[0],
                                selectedDate[0],
                                category,
                                notes,
                                accountName,
                                selectedPaymentType[0]
                        );

                        transactions.add(0, transaction);

                        if (currentIsIncome[0]) {
                            totalIncome += amountValue;
                        } else {
                            totalExpenses += amountValue;
                        }

                        recalculateTotalBalance();
                        saveAllData();

                        runOnUiThread(() -> {
                            updateNavHeader();
                            updateFragments();
                            updateAccountsFragment();

                            String message = String.format(Locale.getDefault(),
                                    "✅ %s добавлен: %.2f руб.",
                                    currentIsIncome[0] ? "Доход" : "Расход",
                                    amountValue);

                            Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                        });

                    } catch (NumberFormatException e) {
                        Toast.makeText(MainActivity.this, "Введите корректную сумму", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Отмена", null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // ============ ИСПРАВЛЕННЫЙ МЕТОД ДОБАВЛЕНИЯ ТРАНЗАКЦИИ ============
    public void showEnhancedAddTransactionDialog(final boolean isIncome) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_transaction_enhanced, null);

        final TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        final Button incomeTypeButton = dialogView.findViewById(R.id.incomeTypeButton);
        final Button expenseTypeButton = dialogView.findViewById(R.id.expenseTypeButton);
        final Spinner accountSpinner = dialogView.findViewById(R.id.accountSpinner);
        final Spinner categorySpinner = dialogView.findViewById(R.id.categorySpinner);
        final EditText newCategoryEditText = dialogView.findViewById(R.id.newCategoryEditText);
        final Button cashButton = dialogView.findViewById(R.id.cashButton);
        final Button cardButton = dialogView.findViewById(R.id.cardButton);
        final Button electronicButton = dialogView.findViewById(R.id.electronicButton);
        final EditText amountEditText = dialogView.findViewById(R.id.amountEditText);
        final EditText notesEditText = dialogView.findViewById(R.id.notesEditText);
        final LinearLayout dateLayout = dialogView.findViewById(R.id.dateLayout);
        final TextView dateTextView = dialogView.findViewById(R.id.dateTextView);
        final Button scanReceiptButton = dialogView.findViewById(R.id.scanReceiptButton);

        // Скрываем выбор валюты
        final Spinner currencySpinner = dialogView.findViewById(R.id.currencySpinner);
        if (currencySpinner != null) {
            currencySpinner.setVisibility(View.GONE);
        }

        final boolean[] currentIsIncome = {isIncome};
        final Date[] selectedDate = {new Date()};
        final String[] selectedPaymentType = {"Карта"};

        updateTypeButtons(incomeTypeButton, expenseTypeButton, currentIsIncome[0]);
        updatePaymentTypeButtons(cashButton, cardButton, electronicButton, selectedPaymentType[0]);
        dialogTitle.setText(currentIsIncome[0] ? "📈 Добавить доход" : "📉 Добавить расход");

        loadCategoriesToSpinner(categorySpinner, currentIsIncome[0]);
        loadAccountsToSpinner(accountSpinner);
        updateDateTextView(dateTextView, selectedDate[0]);

        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCategory = (String) parent.getItemAtPosition(position);
                boolean isOtherCategory = (selectedCategory != null &&
                        (selectedCategory.equals("Прочие доходы") || selectedCategory.equals("Прочие расходы")));
                newCategoryEditText.setVisibility(isOtherCategory ? View.VISIBLE : View.GONE);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        incomeTypeButton.setOnClickListener(v -> {
            currentIsIncome[0] = true;
            updateTypeButtons(incomeTypeButton, expenseTypeButton, currentIsIncome[0]);
            dialogTitle.setText("📈 Добавить доход");
            loadCategoriesToSpinner(categorySpinner, currentIsIncome[0]);
        });

        expenseTypeButton.setOnClickListener(v -> {
            currentIsIncome[0] = false;
            updateTypeButtons(incomeTypeButton, expenseTypeButton, currentIsIncome[0]);
            dialogTitle.setText("📉 Добавить расход");
            loadCategoriesToSpinner(categorySpinner, currentIsIncome[0]);
        });

        cashButton.setOnClickListener(v -> {
            selectedPaymentType[0] = "Наличные";
            updatePaymentTypeButtons(cashButton, cardButton, electronicButton, selectedPaymentType[0]);
        });

        cardButton.setOnClickListener(v -> {
            selectedPaymentType[0] = "Карта";
            updatePaymentTypeButtons(cashButton, cardButton, electronicButton, selectedPaymentType[0]);
        });

        electronicButton.setOnClickListener(v -> {
            selectedPaymentType[0] = "Электронные деньги";
            updatePaymentTypeButtons(cashButton, cardButton, electronicButton, selectedPaymentType[0]);
        });

        dateLayout.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(selectedDate[0]);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    MainActivity.this,
                    (view, year, month, dayOfMonth) -> {
                        Calendar cal = Calendar.getInstance();
                        cal.set(year, month, dayOfMonth);
                        selectedDate[0] = cal.getTime();
                        updateDateTextView(dateTextView, selectedDate[0]);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        // ============ КНОПКА СКАНИРОВАНИЯ ЧЕКА ============
        scanReceiptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Закрываем диалог
                try {
                    // Получаем диалог через builder
                    AlertDialog dialog = builder.create();
                    if (dialog.isShowing()) {
                        dialog.dismiss();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Открываем сканер чеков
                ReceiptScannerFragment scannerFragment = new ReceiptScannerFragment();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, scannerFragment)
                        .addToBackStack(null)
                        .commit();
            }
        });

        builder.setView(dialogView)
                .setPositiveButton("Добавить", (dialog, which) -> {
                    try {
                        String amountStr = amountEditText.getText().toString().trim();
                        String notes = notesEditText.getText().toString().trim();
                        String category = (String) categorySpinner.getSelectedItem();

                        String accountDisplay = (String) accountSpinner.getSelectedItem();
                        String accountName = accountDisplay;
                        if (accountDisplay != null && accountDisplay.contains(" - ")) {
                            accountName = accountDisplay.substring(0, accountDisplay.indexOf(" - "));
                        }

                        if (amountStr.isEmpty()) {
                            Toast.makeText(MainActivity.this, "Введите сумму", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        double amount = Double.parseDouble(amountStr);
                        if (amount <= 0) {
                            Toast.makeText(MainActivity.this, "Сумма должна быть больше 0", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (category == null) {
                            Toast.makeText(MainActivity.this, "Выберите категорию", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if ((category.equals("Прочие доходы") || category.equals("Прочие расходы"))
                                && !newCategoryEditText.getText().toString().trim().isEmpty()) {
                            String newCategory = newCategoryEditText.getText().toString().trim();
                            if (currentIsIncome[0]) {
                                categoryManager.addIncomeCategory(newCategory);
                                category = newCategory;
                            } else {
                                categoryManager.addExpenseCategory(newCategory);
                                category = newCategory;
                            }
                        }

                        String description = notes.isEmpty() ?
                                (currentIsIncome[0] ? "Доход" : "Расход") + ": " + category :
                                notes;

                        Account selectedAccount = null;
                        List<Account> accounts = dataManager.loadAccounts();

                        for (Account acc : accounts) {
                            if (acc.getName().equals(accountName)) {
                                selectedAccount = acc;
                                break;
                            }
                        }

                        if (selectedAccount != null) {
                            double oldBalance = selectedAccount.getBalance();
                            if (currentIsIncome[0]) {
                                selectedAccount.setBalance(oldBalance + amount);
                            } else {
                                selectedAccount.setBalance(oldBalance - amount);
                            }
                            dataManager.saveAccounts(accounts);
                        }

                        Transaction transaction = new Transaction(
                                description,
                                amount,
                                currentIsIncome[0],
                                selectedDate[0],
                                category,
                                notes,
                                accountName,
                                selectedPaymentType[0]
                        );

                        transactions.add(0, transaction);

                        if (currentIsIncome[0]) {
                            totalIncome += amount;
                        } else {
                            totalExpenses += amount;
                        }

                        recalculateTotalBalance();
                        saveAllData();

                        runOnUiThread(() -> {
                            updateNavHeader();
                            updateFragments();
                            updateAccountsFragment();

                            String message = String.format(Locale.getDefault(),
                                    "✅ %s добавлен: %.2f руб.",
                                    currentIsIncome[0] ? "Доход" : "Расход",
                                    amount);

                            Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                        });

                    } catch (NumberFormatException e) {
                        Toast.makeText(MainActivity.this, "Введите корректную сумму", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Отмена", null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updatePaymentTypeButtons(Button cashBtn, Button cardBtn, Button electronicBtn, String selectedType) {
        cashBtn.setBackgroundResource(R.drawable.button_payment_unselected);
        cashBtn.setTextColor(Color.BLACK);
        cardBtn.setBackgroundResource(R.drawable.button_payment_unselected);
        cardBtn.setTextColor(Color.BLACK);
        electronicBtn.setBackgroundResource(R.drawable.button_payment_unselected);
        electronicBtn.setTextColor(Color.BLACK);

        switch (selectedType) {
            case "Наличные":
                cashBtn.setBackgroundResource(R.drawable.button_payment_cash_selected);
                cashBtn.setTextColor(Color.WHITE);
                break;
            case "Карта":
                cardBtn.setBackgroundResource(R.drawable.button_payment_card_selected);
                cardBtn.setTextColor(Color.WHITE);
                break;
            case "Электронные деньги":
                electronicBtn.setBackgroundResource(R.drawable.button_payment_electronic_selected);
                electronicBtn.setTextColor(Color.WHITE);
                break;
        }
    }

    public void showQuickAddTransactionDialog(final boolean isIncome) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isIncome ? "📈 Быстрый доход" : "📉 Быстрый расход");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_quick_transaction, null);
        final EditText amountInput = dialogView.findViewById(R.id.amountInput);
        final Spinner categorySpinner = dialogView.findViewById(R.id.categorySpinner);

        loadCategoriesToSpinner(categorySpinner, isIncome);

        builder.setView(dialogView);

        builder.setPositiveButton("Добавить", (dialog, which) -> {
            try {
                String amountStr = amountInput.getText().toString().trim();
                String category = (String) categorySpinner.getSelectedItem();

                if (amountStr.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Введите сумму", Toast.LENGTH_SHORT).show();
                    return;
                }

                double amount = Double.parseDouble(amountStr);
                if (amount <= 0) {
                    Toast.makeText(MainActivity.this, "Сумма должна быть больше 0", Toast.LENGTH_SHORT).show();
                    return;
                }

                addQuickTransaction(category, amount, isIncome);

            } catch (NumberFormatException e) {
                Toast.makeText(MainActivity.this, "Введите корректную сумму", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Подробнее", (dialog, which) -> showEnhancedAddTransactionDialog(isIncome));

        builder.setNeutralButton("Отмена", null);
        builder.show();
    }

    public void addQuickTransaction(String category, double amount, boolean isIncome) {
        Date currentDate = new Date();

        Transaction transaction = new Transaction(
                (isIncome ? "Доход: " : "Расход: ") + category,
                amount,
                isIncome,
                currentDate,
                category,
                "",
                "",
                "Карта"
        );

        transactions.add(0, transaction);

        // Обновляем доходы/расходы
        if (isIncome) {
            totalIncome += amount;
        } else {
            totalExpenses += amount;
        }

        // Пересчитываем баланс
        recalculateTotalBalance();

        saveAllData();
        updateFragments();
        updateNavHeader();

        Toast.makeText(this,
                isIncome ? "✅ Быстрый доход добавлен" : "✅ Быстрый расход добавлен",
                Toast.LENGTH_SHORT).show();
    }

    private void loadAccountsToSpinner(Spinner accountSpinner) {
        List<Account> accounts = dataManager.loadAccounts();
        List<String> accountNames = new ArrayList<>();

        accountNames.add("Без счета");

        for (Account account : accounts) {
            accountNames.add(account.getName() + " - " + account.getFormattedBalance());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, accountNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        accountSpinner.setAdapter(adapter);
    }

    private void updateDateTextView(TextView dateTextView, Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        String dateText = "📅 " + sdf.format(date);
        dateTextView.setText(dateText);
        dateTextView.setTextColor(Color.BLACK);
    }

    public void loadCategoriesToSpinner(Spinner categorySpinner, boolean isIncome) {
        List<String> categories = isIncome ?
                categoryManager.getIncomeCategories() :
                categoryManager.getExpenseCategories();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);
    }

    private void updateTypeButtons(Button incomeBtn, Button expenseBtn, boolean isIncomeSelected) {
        if (isIncomeSelected) {
            incomeBtn.setBackgroundResource(R.drawable.button_type_selected);
            incomeBtn.setTextColor(Color.WHITE);
            expenseBtn.setBackgroundResource(R.drawable.button_type_unselected);
            expenseBtn.setTextColor(Color.BLACK);
        } else {
            incomeBtn.setBackgroundResource(R.drawable.button_type_unselected);
            incomeBtn.setTextColor(Color.BLACK);
            expenseBtn.setBackgroundResource(R.drawable.button_type_expense_selected);
            expenseBtn.setTextColor(Color.WHITE);
        }
    }

    public void addTransaction(Transaction transaction) {
        transactions.add(0, transaction);

        if (transaction.isIncome()) {
            totalIncome += transaction.getAmount();
        } else {
            totalExpenses += transaction.getAmount();
        }

        // Пересчитываем баланс
        recalculateTotalBalance();

        saveAllData();
        updateFragments();
        updateNavHeader();
        showTransactionAddedNotification(transaction);
    }

    private void showTransactionAddedNotification(Transaction transaction) {
        String type = transaction.isIncome() ? "📈 Доход" : "📉 Расход";
        String message = String.format(Locale.getDefault(),
                "%s добавлен\nКатегория: %s\nСумма: %s",
                type,
                transaction.getCategoryWithoutIcon(),
                transaction.getFormattedAmount());

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showImportDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Импорт данных")
                .setMessage("Выберите файл .json с бэкапом.\n\n" +
                        "⚠️ Внимание: текущие данные будут перезаписаны!")
                .setPositiveButton("Выбрать файл", (dialog, which) -> openFilePicker())
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        startActivityForResult(intent, 1001);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1001) {
            if (resultCode == RESULT_OK && data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    boolean success = backupManager.saveExportData(uri);
                    if (success) {
                        Toast.makeText(this, "✅ Данные экспортированы", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    private void showImportConfirmDialog(final Uri fileUri) {
        new AlertDialog.Builder(this)
                .setTitle("Подтверждение импорта")
                .setMessage("Вы уверены, что хотите импортировать данные из выбранного файла?\n\n" +
                        "⚠️ ВНИМАНИЕ: Все текущие данные будут ЗАМЕНЕНЫ данными из бэкапа!\n\n" +
                        "Это действие нельзя отменить!")
                .setPositiveButton("ИМПОРТИРОВАТЬ", (dialog, which) -> {
                    boolean success = backupManager.importFromFile(fileUri);
                    if (success) {
                        loadSavedData();
                        updateFragments();
                        updateNavHeader();
                        updateAccountsFragment();
                        Toast.makeText(MainActivity.this,
                                "✅ Данные успешно импортированы",
                                Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }


    private void showAppInfo() {
        long sizeInBytes = dataManager.getDataSize();
        String sizeText = String.format(Locale.getDefault(),
                "📱 Финансовый трекер v2.0\n\n" +
                        "📊 Статистика:\n" +
                        "• Записей: %d\n" +
                        "• Размер данных: %.2f КБ\n" +
                        "• Баланс: %.2f руб.\n" +
                        "• Доходы: %.2f руб.\n" +
                        "• Расходы: %.2f руб.\n\n" +
                        "✅ Функции:\n" +
                        "• Мультивалютность\n" +
                        "• Автоматическая конвертация валют\n" +
                        "• Курсы ЦБ РФ\n" +
                        "• Категории доходов/расходов\n" +
                        "• Детальная аналитика\n" +
                        "• Выбор даты операций\n" +
                        "• Комментарии к транзакциям\n" +
                        "• Автосохранение и бэкапы",
                transactions.size(),
                sizeInBytes / 1024.0,
                balance,
                totalIncome,
                totalExpenses);

        new AlertDialog.Builder(this)
                .setTitle("О приложении")
                .setMessage(sizeText)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showHelpDialog() {
        String helpText = "📱 ФИНАНСОВЫЙ ТРЕКЕР - ПОЛНОЕ РУКОВОДСТВО\n\n" +

                "🏠 ГЛАВНАЯ СТРАНИЦА\n" +
                "• Просматривайте текущий баланс\n" +
                "• Быстро добавляйте доходы и расходы\n" +
                "• Следите за превышением расходов\n" +
                "• Очищайте данные при необходимости\n\n" +

                "📊 РАЗДЕЛ «ДАННЫЕ И ЗАПИСИ»\n" +
                "• Вся история операций с категориями\n" +
                "• Детальная статистика по категориям\n" +
                "• Фильтрация по датам и типам\n" +
                "• Экспорт данных для анализа\n\n" +

                "📈 РАЗДЕЛ «АНАЛИТИКА»\n" +
                "• Визуализация распределения доходов/расходов\n" +
                "• Процентное соотношение по категориям\n" +
                "• Графики для наглядного представления\n" +
                "• Анализ финансовых привычек\n\n" +

                "💱 МУЛЬТИВАЛЮТНОСТЬ\n" +
                "• Поддерживаемые валюты: RUB, USD, EUR, CNY, AED\n" +
                "• Автоматическая конвертация при добавлении транзакций\n" +
                "• Курсы валют обновляются с сайта ЦБ РФ\n" +
                "• Возможность ручной корректировки курсов\n\n" +

                "➕ КАК ДОБАВИТЬ ТРАНЗАКЦИЮ\n" +
                "1. Нажмите «➕ Добавить доход» или «➖ Добавить расход»\n" +
                "2. Выберите счет (если нужно)\n" +
                "3. Выберите валюту транзакции\n" +
                "4. Введите сумму\n" +
                "5. Выберите категорию\n" +
                "6. Выберите дату\n" +
                "7. Нажмите «Добавить»\n\n" +
                "Примечание: если валюта транзакции отличается от валюты счета,\n" +
                "сумма будет автоматически конвертирована по текущему курсу.\n\n" +

                "💰 УПРАВЛЕНИЕ СЧЕТАМИ\n" +
                "• Создавайте счета в разных валютах\n" +
                "• Пополняйте и снимайте средства\n" +
                "• Отслеживайте баланс в рублях\n\n" +

                "💸 ПЕРЕВОДЫ МЕЖДУ СЧЕТАМИ\n" +
                "• Переводите средства между счетами\n" +
                "• Автоматическая конвертация валют\n" +
                "• Поддержка комиссий\n\n" +

                "📊 БЮДЖЕТЫ И ЦЕЛИ\n" +
                "• Устанавливайте бюджеты по категориям\n" +
                "• Создавайте финансовые цели\n" +
                "• Отслеживайте прогресс\n\n" +

                "💾 ЭКСПОРТ И ИМПОРТ\n" +
                "• Сохраняйте данные в JSON файл\n" +
                "• Восстанавливайте из резервной копии\n\n" +

                "⚠️ ПРЕДУПРЕЖДЕНИЯ\n" +
                "• При превышении расходов над доходами\n" +
                "• При приближении к лимиту бюджета\n" +
                "• При значительном изменении курсов валют\n\n" +

                "📞 ПОДДЕРЖКА\n" +
                "При возникновении проблем:\n" +
                "• Проверьте подключение к интернету для обновления курсов\n" +
                "• Сделайте резервную копию перед важными изменениями\n" +
                "• Перезапустите приложение";

        new AlertDialog.Builder(this)
                .setTitle("❓ ПОМОЩЬ И РУКОВОДСТВО")
                .setMessage(helpText)
                .setPositiveButton("ПОНЯТНО", null)
                .show();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    public void deleteTransaction(int position) {
        if (position >= 0 && position < transactions.size()) {
            Transaction transaction = transactions.get(position);

            if (transaction.isIncome()) {
                totalIncome -= transaction.getAmount();
            } else {
                totalExpenses -= transaction.getAmount();
            }

            // Пересчитываем баланс
            recalculateTotalBalance();

            transactions.remove(position);
            saveAllData();
            updateFragments();
            updateNavHeader();

            checkExpenseWarning();

            Toast.makeText(this, "Транзакция удалена", Toast.LENGTH_SHORT).show();
        }
    }

    public NavigationView getNavigationView() {
        return navigationView;
    }

    public void updateWarnings() {
        if (homeFragment != null && homeFragment.isAdded()) {
            homeFragment.updateDashboard();
        }
    }

    public String getTotalBalanceStatus() {
        if (balance < 0) {
            return "⚠️ Отрицательный баланс: " + String.format(Locale.getDefault(), "%.2f руб.", balance);
        } else if (balance < 1000) {
            return "⚠️ Низкий баланс: " + String.format(Locale.getDefault(), "%.2f руб.", balance);
        } else {
            return "✅ Нормальный баланс: " + String.format(Locale.getDefault(), "%.2f руб.", balance);
        }
    }

    public String getExpenseStatus() {
        if (totalExpenses > totalIncome) {
            return "⚠️ Расходы превышают доходы на " +
                    String.format(Locale.getDefault(), "%.2f руб.", totalExpenses - totalIncome);
        } else if (totalIncome > 0 && totalExpenses > totalIncome * 0.8) {
            return "⚠️ Высокие расходы: " +
                    String.format(Locale.getDefault(), "%.1f%% от доходов", (totalExpenses / totalIncome) * 100);
        } else {
            return "✅ Здоровое соотношение расходов/доходов";
        }
    }

    public User getCurrentUser() {
        return authManager != null ? authManager.getCurrentUser() : null;
    }

    public void logout() {
        authManager.logout();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}