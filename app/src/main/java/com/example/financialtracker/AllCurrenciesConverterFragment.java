package com.example.financialtracker;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Конвертер по всем валютам ЦБ РФ с актуальными официальными курсами.
 * Использует разметку fragment_currency_converter_simple.xml.
 */
public class AllCurrenciesConverterFragment extends Fragment {

    /** Популярные валюты показываем первыми, остальные — по алфавиту. */
    private static final List<String> POPULAR = Arrays.asList(
            "RUB", "USD", "EUR", "CNY", "AED", "TRY", "KZT", "BYN", "GBP", "JPY", "CHF", "AMD", "UZS", "KGS", "GEL", "THB", "INR");

    private Spinner fromSpinner, toSpinner;
    private EditText amountEditText, searchEditText;
    private TextView resultTextView, rateTextView, lastUpdateTextView;
    private LinearLayout ratesContainer;
    private Button updateButton;

    /** code -> рублей за 1 единицу. RUB = 1. */
    private final Map<String, Double> rubPerUnit = new HashMap<>();
    private final Map<String, String> names = new HashMap<>();
    private final List<String> codes = new ArrayList<>();
    private List<CbrAllRatesProvider.Rate> rates = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_currency_converter_simple, container, false);

        fromSpinner = view.findViewById(R.id.fromCurrencySpinner);
        toSpinner = view.findViewById(R.id.toCurrencySpinner);
        amountEditText = view.findViewById(R.id.amountEditText);
        resultTextView = view.findViewById(R.id.resultTextView);
        rateTextView = view.findViewById(R.id.rateTextView);
        lastUpdateTextView = view.findViewById(R.id.lastUpdateTextView);
        ratesContainer = view.findViewById(R.id.ratesContainer);
        updateButton = view.findViewById(R.id.updateRatesButton);
        ImageButton swapButton = view.findViewById(R.id.swapButton);

        amountEditText.setText("1");
        resultTextView.setText("—");
        rateTextView.setText("Загрузка курсов ЦБ РФ…");

        // Поиск по списку курсов
        searchEditText = new EditText(getContext());
        searchEditText.setHint("🔍 Поиск валюты (код или название)");
        searchEditText.setSingleLine(true);
        searchEditText.setTextColor(getResources().getColor(R.color.text_primary));
        searchEditText.setHintTextColor(getResources().getColor(R.color.text_hint));
        ViewGroup parent = (ViewGroup) ratesContainer.getParent();
        parent.addView(searchEditText, parent.indexOfChild(ratesContainer));

        TextWatcher recalc = new SimpleWatcher(this::convert);
        amountEditText.addTextChangedListener(recalc);
        searchEditText.addTextChangedListener(new SimpleWatcher(this::showRatesList));

        AdapterView.OnItemSelectedListener onSelect = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { convert(); }
            @Override public void onNothingSelected(AdapterView<?> p) { }
        };
        fromSpinner.setOnItemSelectedListener(onSelect);
        toSpinner.setOnItemSelectedListener(onSelect);

        swapButton.setOnClickListener(v -> {
            int a = fromSpinner.getSelectedItemPosition();
            fromSpinner.setSelection(toSpinner.getSelectedItemPosition());
            toSpinner.setSelection(a);
            convert();
        });

        updateButton.setOnClickListener(v -> loadRates(true));

        loadRates(false);
        return view;
    }

    private void loadRates(boolean force) {
        updateButton.setEnabled(false);
        lastUpdateTextView.setText("Обновление курсов…");
        CbrAllRatesProvider.load(requireContext(), force, result -> {
            if (!isAdded() || getView() == null) return;
            updateButton.setEnabled(true);

            if (result.rates.isEmpty()) {
                lastUpdateTextView.setText("Не удалось загрузить курсы. Проверьте интернет и нажмите «Обновить».");
                rateTextView.setText(result.error != null ? result.error : "");
                return;
            }
            applyRates(result.rates);

            String loaded = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(new Date(result.loadedAt));
            String text = "Официальные курсы ЦБ РФ" + (result.date != null ? " на " + result.date : "")
                    + "\nЗагружено: " + loaded + " • валют: " + result.rates.size();
            if (result.fromCache) {
                text += "\n⚠️ Нет связи — показаны сохранённые курсы";
            }
            lastUpdateTextView.setText(text);
            if (force && !result.fromCache) {
                Toast.makeText(getContext(), "Курсы обновлены", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyRates(List<CbrAllRatesProvider.Rate> newRates) {
        String prevFrom = selectedCode(fromSpinner, "USD");
        String prevTo = selectedCode(toSpinner, "RUB");

        rates = new ArrayList<>(newRates);
        rubPerUnit.clear();
        names.clear();
        codes.clear();

        rubPerUnit.put("RUB", 1.0);
        names.put("RUB", "Российский рубль");
        for (CbrAllRatesProvider.Rate r : rates) {
            rubPerUnit.put(r.code, r.rubPerUnit());
            names.put(r.code, r.name);
        }

        codes.addAll(rubPerUnit.keySet());
        Collections.sort(codes, (a, b) -> {
            int ia = POPULAR.indexOf(a), ib = POPULAR.indexOf(b);
            if (ia >= 0 || ib >= 0) {
                if (ia < 0) return 1;
                if (ib < 0) return -1;
                return Integer.compare(ia, ib);
            }
            return names.get(a).compareToIgnoreCase(names.get(b));
        });

        List<String> labels = new ArrayList<>();
        for (String c : codes) labels.add(c + " — " + names.get(c));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fromSpinner.setAdapter(adapter);
        toSpinner.setAdapter(adapter);
        fromSpinner.setSelection(Math.max(0, codes.indexOf(prevFrom)));
        toSpinner.setSelection(Math.max(0, codes.indexOf(prevTo)));

        convert();
        showRatesList();
    }

    private String selectedCode(Spinner spinner, String def) {
        int pos = spinner.getSelectedItemPosition();
        return pos >= 0 && pos < codes.size() ? codes.get(pos) : def;
    }

    /** Пересчёт: сумма × (рублей за 1 ед. исходной) / (рублей за 1 ед. целевой). */
    private void convert() {
        if (codes.isEmpty()) return;
        String from = selectedCode(fromSpinner, "USD");
        String to = selectedCode(toSpinner, "RUB");
        Double fromRub = rubPerUnit.get(from), toRub = rubPerUnit.get(to);
        if (fromRub == null || toRub == null || toRub == 0) return;

        double amount = 0;
        String s = amountEditText.getText().toString().trim().replace(',', '.').replace(" ", "");
        if (!s.isEmpty()) {
            try { amount = Double.parseDouble(s); } catch (NumberFormatException ignored) { }
        }
        double rate = fromRub / toRub;
        resultTextView.setText(String.format(Locale.getDefault(), "%s %s", formatMoney(amount * rate), to));
        rateTextView.setText(String.format(Locale.getDefault(), "1 %s = %s %s", from, formatRate(rate), to));
    }

    private void showRatesList() {
        if (ratesContainer == null) return;
        ratesContainer.removeAllViews();
        String q = searchEditText.getText().toString().trim().toLowerCase(Locale.getDefault());
        for (String code : codes) {
            if (code.equals("RUB")) continue;
            String name = names.get(code);
            if (!q.isEmpty() && !code.toLowerCase(Locale.ROOT).contains(q)
                    && !name.toLowerCase(Locale.getDefault()).contains(q)) continue;

            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 12, 0, 12);
            row.setGravity(Gravity.CENTER_VERTICAL);

            TextView left = new TextView(getContext());
            left.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            left.setText(code + "  " + name);
            left.setTextSize(14);
            left.setTextColor(getResources().getColor(R.color.text_primary));

            TextView right = new TextView(getContext());
            right.setText(formatRate(rubPerUnit.get(code)) + " ₽");
            right.setTextSize(14);
            right.setTypeface(null, Typeface.BOLD);
            right.setTextColor(getResources().getColor(R.color.primary_color));

            row.addView(left);
            row.addView(right);
            row.setOnClickListener(v -> {
                fromSpinner.setSelection(codes.indexOf(code));
                toSpinner.setSelection(codes.indexOf("RUB"));
                convert();
            });
            ratesContainer.addView(row);
        }
        if (ratesContainer.getChildCount() == 0) {
            TextView empty = new TextView(getContext());
            empty.setText("Ничего не найдено");
            empty.setTextColor(getResources().getColor(R.color.text_secondary));
            ratesContainer.addView(empty);
        }
    }

    private static String formatMoney(double v) {
        return String.format(Locale.getDefault(), "%,.2f", v);
    }

    private static String formatRate(double v) {
        if (v >= 100) return String.format(Locale.getDefault(), "%,.2f", v);
        if (v >= 1) return String.format(Locale.getDefault(), "%.4f", v);
        return String.format(Locale.getDefault(), "%.6f", v);
    }

    private static final class SimpleWatcher implements TextWatcher {
        private final Runnable action;
        SimpleWatcher(Runnable action) { this.action = action; }
        @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
        @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
        @Override public void afterTextChanged(Editable s) { action.run(); }
    }
}
