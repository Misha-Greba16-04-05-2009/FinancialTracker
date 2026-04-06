package com.example.financialtracker;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ExpenseHistoryFragment extends Fragment implements AnalysisBaseFragment {

    private LinearLayout expensesContainer;
    private int currentMonth, currentYear;
    private MainActivity mainActivity;
    private TextView emptyTextView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_expense_history, container, false);

        expensesContainer = view.findViewById(R.id.expensesContainer);
        emptyTextView = view.findViewById(R.id.emptyTextView);
        mainActivity = (MainActivity) getActivity();

        // Инициализируем текущую дату
        Calendar calendar = Calendar.getInstance();
        currentMonth = calendar.get(Calendar.MONTH) + 1;
        currentYear = calendar.get(Calendar.YEAR);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadExpenses(currentMonth, currentYear);
    }

    private void loadExpenses(int month, int year) {
        if (mainActivity == null || expensesContainer == null) return;

        expensesContainer.removeAllViews();

        List<Transaction> filteredExpenses = new ArrayList<>();

        // Фильтруем расходы по месяцу и году
        for (Transaction transaction : mainActivity.getTransactions()) {
            if (!transaction.isIncome()) { // Только расходы
                Calendar cal = Calendar.getInstance();
                cal.setTime(transaction.getDate());
                int transactionYear = cal.get(Calendar.YEAR);
                int transactionMonth = cal.get(Calendar.MONTH) + 1;

                if (transactionYear == year && transactionMonth == month) {
                    filteredExpenses.add(transaction);
                }
            }
        }

        if (filteredExpenses.isEmpty()) {
            emptyTextView.setVisibility(View.VISIBLE);
            expensesContainer.setVisibility(View.GONE);
            return;
        }

        emptyTextView.setVisibility(View.GONE);
        expensesContainer.setVisibility(View.VISIBLE);

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

        for (int i = 0; i < filteredExpenses.size(); i++) {
            Transaction expense = filteredExpenses.get(i);
            addExpenseView(expense, i, dateFormat);
        }
    }

    private void addExpenseView(Transaction expense, int index, SimpleDateFormat dateFormat) {
        // Создаем контейнер для транзакции
        LinearLayout expenseLayout = new LinearLayout(getContext());
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        if (index > 0) {
            layoutParams.topMargin = 10;
        }
        layoutParams.bottomMargin = 10;
        expenseLayout.setLayoutParams(layoutParams);
        expenseLayout.setOrientation(LinearLayout.VERTICAL);
        expenseLayout.setPadding(15, 15, 15, 15);
        expenseLayout.setBackgroundResource(R.drawable.transaction_background_expense);

        // Верхняя строка: категория и сумма
        LinearLayout topRow = new LinearLayout(getContext());
        topRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        topRow.setOrientation(LinearLayout.HORIZONTAL);

        // Категория
        TextView categoryView = new TextView(getContext());
        LinearLayout.LayoutParams categoryParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
        );
        categoryView.setLayoutParams(categoryParams);
        categoryView.setText(expense.getCategoryIcon() + " " + expense.getCategoryWithoutIcon());
        categoryView.setTextSize(16);
        categoryView.setTypeface(null, Typeface.BOLD);
        categoryView.setTextColor(mainActivity.getCategoryManager().getCategoryColor(
                expense.getCategory(), expense.isIncome()));

        // Сумма
        TextView amountView = new TextView(getContext());
        amountView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        amountView.setText(expense.getFormattedAmount());
        amountView.setTextSize(16);
        amountView.setTypeface(null, Typeface.BOLD);
        amountView.setTextColor(Color.parseColor("#D32F2F"));

        topRow.addView(categoryView);
        topRow.addView(amountView);

        // Описание
        TextView descriptionView = new TextView(getContext());
        descriptionView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        descriptionView.setText(expense.getDescription());
        descriptionView.setTextSize(14);
        descriptionView.setTextColor(Color.parseColor("#424242"));
        descriptionView.setPadding(0, 5, 0, 5);

        // Нижняя строка: дата
        TextView dateView = new TextView(getContext());
        dateView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        dateView.setText("📅 " + dateFormat.format(expense.getDate()));
        dateView.setTextSize(12);
        dateView.setTextColor(Color.parseColor("#757575"));

        // Собираем все вместе
        expenseLayout.addView(topRow);
        expenseLayout.addView(descriptionView);
        expenseLayout.addView(dateView);

        expensesContainer.addView(expenseLayout);
    }

    @Override
    public void updateData(int month, int year) {
        currentMonth = month;
        currentYear = year;
        loadExpenses(month, year);
    }

    @Override
    public void loadCategories() {
        // Перезагружаем список с текущими категориями
        loadExpenses(currentMonth, currentYear);
    }
}