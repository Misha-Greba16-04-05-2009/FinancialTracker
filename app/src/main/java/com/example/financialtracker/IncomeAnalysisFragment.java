package com.example.financialtracker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

public class IncomeAnalysisFragment extends BaseAnalysisFragment {

    @Override
    protected boolean isIncomeAnalysis() {
        return true;
    }

    @Override
    protected String getTitleText() {
        return "📈 Анализ доходов";
    }

    @Override
    protected String getEmptyMessage() {
        return "Нет доходов за выбранный период";
    }

    @Override
    protected int getPrimaryColor() {
        return ContextCompat.getColor(getContext(), android.R.color.holo_green_dark);
    }

    @Override
    protected String getCategoryTypeText() {
        return "Доход";
    }
}