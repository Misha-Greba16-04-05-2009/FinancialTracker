package com.example.financialtracker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ExpenseAnalysisFragment extends BaseAnalysisFragment {

    @Override
    protected boolean isIncomeAnalysis() {
        return false;
    }

    @Override
    protected String getTitleText() {
        return "📉 Анализ расходов";
    }

    @Override
    protected String getEmptyMessage() {
        return "Нет расходов за выбранный период";
    }

    @Override
    protected int getPrimaryColor() {
        return getResources().getColor(android.R.color.holo_red_dark);
    }

    @Override
    protected String getCategoryTypeText() {
        return "Расход";
    }
}