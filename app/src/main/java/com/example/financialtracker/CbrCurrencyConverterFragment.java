package com.example.financialtracker;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CbrCurrencyConverterFragment extends Fragment implements CbrRateManager.RateUpdateListener {

    private MainActivity mainActivity;
    private CbrRateManager rateManager;

    private Spinner fromSpinner;
    private Spinner toSpinner;
    private EditText amountEdit;
    private TextView resultText;
    private TextView rateText;
    private TextView lastUpdateText;
    private ImageButton swapButton;
    private Button updateButton;
    private LinearLayout ratesContainer;

    private String fromCurrency = "USD";
    private String toCurrency = "RUB";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_currency_converter_simple, container, false);

        mainActivity = (MainActivity) getActivity();
        if (mainActivity == null) {
            return view;
        }

        rateManager = mainActivity.getRateManager();
        rateManager.addListener(this);

        initViews(view);
        setupSpinners();
        setupListeners();
        updateRates();

        amountEdit.setText("1");
        updateRate();
        convert();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        rateManager.removeListener(this);
    }

    private void initViews(View view) {
        fromSpinner = view.findViewById(R.id.fromCurrencySpinner);
        toSpinner = view.findViewById(R.id.toCurrencySpinner);
        amountEdit = view.findViewById(R.id.amountEditText);
        resultText = view.findViewById(R.id.resultTextView);
        rateText = view.findViewById(R.id.rateTextView);
        lastUpdateText = view.findViewById(R.id.lastUpdateTextView);
        swapButton = view.findViewById(R.id.swapButton);
        updateButton = view.findViewById(R.id.updateRatesButton);
        ratesContainer = view.findViewById(R.id.ratesContainer);

        lastUpdateText.setText("Последнее обновление: " + rateManager.getLastUpdateTime());
    }

    private void setupSpinners() {
        List<String> currencies = rateManager.getSupportedCurrencies();
        List<String> displayNames = new ArrayList<>();

        for (String code : currencies) {
            displayNames.add(rateManager.getCurrencySymbol(code) + " " + code);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, displayNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        fromSpinner.setAdapter(adapter);
        toSpinner.setAdapter(adapter);

        for (int i = 0; i < currencies.size(); i++) {
            if (currencies.get(i).equals("USD")) {
                fromSpinner.setSelection(i);
                fromCurrency = "USD";
            }
            if (currencies.get(i).equals("RUB")) {
                toSpinner.setSelection(i);
                toCurrency = "RUB";
            }
        }
    }

    private void setupListeners() {
        fromSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                List<String> currencies = rateManager.getSupportedCurrencies();
                fromCurrency = currencies.get(position);
                updateRate();
                convert();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        toSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                List<String> currencies = rateManager.getSupportedCurrencies();
                toCurrency = currencies.get(position);
                updateRate();
                convert();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        amountEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) { convert(); }
        });

        swapButton.setOnClickListener(v -> {
            int fromPos = fromSpinner.getSelectedItemPosition();
            int toPos = toSpinner.getSelectedItemPosition();
            fromSpinner.setSelection(toPos);
            toSpinner.setSelection(fromPos);
        });

        updateButton.setOnClickListener(v -> {
            updateButton.setEnabled(false);
            updateButton.setText("Обновление...");
            rateManager.fetchLatestRates();
        });
    }

    private void updateRate() {
        double rate = rateManager.convert(1.0, fromCurrency, toCurrency);
        rateText.setText(String.format(Locale.getDefault(),
                "1 %s = %.4f %s", fromCurrency, rate, toCurrency));
    }

    private void convert() {
        try {
            String amountStr = amountEdit.getText().toString();
            if (amountStr.isEmpty()) {
                resultText.setText("0.00");
                return;
            }

            double amount = Double.parseDouble(amountStr);
            double result = rateManager.convert(amount, fromCurrency, toCurrency);

            String symbol = rateManager.getCurrencySymbol(toCurrency);
            resultText.setText(String.format(Locale.getDefault(), "%s %.2f", symbol, result));

        } catch (NumberFormatException e) {
            resultText.setText("0.00");
        }
    }

    private void updateRates() {
        ratesContainer.removeAllViews();

        List<String> currencies = rateManager.getSupportedCurrencies();
        String base = "RUB";

        for (String code : currencies) {
            if (code.equals("RUB")) continue;

            double rate = rateManager.getRate(code);
            double inverse = rateManager.getInverseRate(code);
            String symbol = rateManager.getCurrencySymbol(code);

            View rateView = getLayoutInflater().inflate(R.layout.item_rate_simple, null);

            TextView currencyText = rateView.findViewById(R.id.currencyText);
            TextView rateToRubText = rateView.findViewById(R.id.rateToRubText);
            TextView rateFromRubText = rateView.findViewById(R.id.rateFromRubText);

            currencyText.setText(symbol + " " + code);
            rateToRubText.setText(String.format(Locale.getDefault(),
                    "1 %s = %.4f RUB", code, rate));
            rateFromRubText.setText(String.format(Locale.getDefault(),
                    "1 RUB = %.4f %s", inverse, code));

            ratesContainer.addView(rateView);
        }
    }

    @Override
    public void onRatesUpdated(boolean success, String message) {
        updateButton.setEnabled(true);
        updateButton.setText("🔄 Обновить курсы");

        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();

        if (success) {
            lastUpdateText.setText("Последнее обновление: " + rateManager.getLastUpdateTime());
            updateRates();
            updateRate();
            convert();
        }
    }
}