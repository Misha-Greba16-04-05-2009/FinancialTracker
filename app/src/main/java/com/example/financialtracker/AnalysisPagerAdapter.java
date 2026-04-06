package com.example.financialtracker;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class AnalysisPagerAdapter extends FragmentStateAdapter {

    public AnalysisPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    public AnalysisPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new ExpenseAnalysisFragment(); // Используем новую версию
            case 1:
                return new IncomeAnalysisFragment();   // Используем новую версию
            case 2:
                return new StatisticsFragment();       // Новая версия статистики
            default:
                return new ExpenseAnalysisFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}