package com.example.financialtracker;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CurrencyRateEditorFragment extends Fragment implements CbrRateManager.RateUpdateListener {

    private MainActivity mainActivity;
    private CbrRateManager rateManager;

    private RecyclerView currenciesRecyclerView;
    private Button resetToOfficialButton;
    private Button saveAllButton;
    private TextView lastUpdateText;
    private TextView statsText;
    private CardView statsCard;

    // ТОЛЬКО 4 ВАЛЮТЫ (плюс RUB базовая)
    private List<EditableCurrency> currencies = new ArrayList<>();
    private CurrencyEditorAdapter adapter;
    private boolean hasChanges = false;

    // Массивы только для нужных валют
    private static final String[] CURRENCY_CODES = {
            "USD", "EUR", "CNY", "AED"
    };

    private static final String[] CURRENCY_NAMES = {
            "Доллар США", "Евро", "Китайский юань", "Дирхам ОАЭ"
    };

    private static final String[] CURRENCY_SYMBOLS = {
            "$", "€", "¥", "د.إ"
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_currency_rate_editor, container, false);

        mainActivity = (MainActivity) getActivity();
        if (mainActivity == null) {
            return view;
        }

        // ПОЛУЧАЕМ rateManager ИЗ MainActivity
        rateManager = mainActivity.getRateManager();
        rateManager.addListener(this);

        initViews(view);
        setupListeners();
        loadCurrencies();
        updateStats();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        rateManager.removeListener(this);
    }

    private void initViews(View view) {
        currenciesRecyclerView = view.findViewById(R.id.currenciesRecyclerView);
        resetToOfficialButton = view.findViewById(R.id.resetToOfficialButton);
        saveAllButton = view.findViewById(R.id.saveAllButton);
        lastUpdateText = view.findViewById(R.id.lastUpdateText);
        statsText = view.findViewById(R.id.statsText);
        statsCard = view.findViewById(R.id.statsCard);

        lastUpdateText.setText("Последнее обновление ЦБ: " + rateManager.getLastUpdateTime());

        currenciesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Скрываем кнопку добавления (она не нужна)
        View addButton = view.findViewById(R.id.addCurrencyButton);
        if (addButton != null) {
            addButton.setVisibility(View.GONE);
        }
    }

    private void setupListeners() {
        resetToOfficialButton.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("Сброс курсов")
                    .setMessage("Вы уверены, что хотите сбросить все курсы к официальным курсам ЦБ РФ?")
                    .setPositiveButton("Сбросить", (dialog, which) -> resetToOfficialRates())
                    .setNegativeButton("Отмена", null)
                    .show();
        });

        saveAllButton.setOnClickListener(v -> saveAllChanges());
    }

    private void loadCurrencies() {
        currencies.clear();

        // Загружаем только 4 валюты
        for (int i = 0; i < CURRENCY_CODES.length; i++) {
            String code = CURRENCY_CODES[i];
            double rate = rateManager.getRate(code);
            String name = CURRENCY_NAMES[i];
            String symbol = CURRENCY_SYMBOLS[i];

            currencies.add(new EditableCurrency(code, name, symbol, rate, rate));
        }

        adapter = new CurrencyEditorAdapter(currencies, new CurrencyEditorListener() {
            @Override
            public void onRateChanged(String currencyCode, double newRate) {
                hasChanges = true;
                updateStats();
            }

            @Override
            public void onDeleteCurrency(String currencyCode) {
                // Не даём удалять валюты
                Toast.makeText(getContext(), "Эту валюту нельзя удалить", Toast.LENGTH_SHORT).show();
            }
        });

        currenciesRecyclerView.setAdapter(adapter);
    }

    private void resetToOfficialRates() {
        for (EditableCurrency currency : currencies) {
            double officialRate = rateManager.getOfficialRate(currency.code);
            currency.currentRate = officialRate;
            currency.originalRate = officialRate;
        }
        adapter.notifyDataSetChanged();
        hasChanges = false;
        updateStats();
        Toast.makeText(getContext(), "Курсы сброшены к официальным", Toast.LENGTH_SHORT).show();
    }

    private void saveAllChanges() {
        for (EditableCurrency currency : currencies) {
            if (currency.currentRate != currency.originalRate) {
                rateManager.setCustomRate(currency.code, currency.currentRate);
                currency.originalRate = currency.currentRate;
            }
        }

        hasChanges = false;
        updateStats();
        Toast.makeText(getContext(), "Все изменения сохранены", Toast.LENGTH_SHORT).show();
    }

    private void updateStats() {
        int changed = 0;
        double avgDeviation = 0;

        for (EditableCurrency c : currencies) {
            if (c.currentRate != c.originalRate) {
                changed++;
                double deviation = Math.abs((c.currentRate - c.originalRate) / c.originalRate * 100);
                avgDeviation += deviation;
            }
        }

        if (changed > 0) {
            avgDeviation /= changed;
        }

        String stats = String.format(Locale.getDefault(),
                "📊 Всего валют: %d\n" +
                        "✏️ Изменено: %d\n" +
                        "📈 Среднее отклонение: %.2f%%",
                currencies.size(), changed, avgDeviation);

        statsText.setText(stats);

        saveAllButton.setEnabled(hasChanges);
        saveAllButton.setAlpha(hasChanges ? 1.0f : 0.5f);
    }

    @Override
    public void onRatesUpdated(boolean success, String message) {
        if (success) {
            lastUpdateText.setText("Последнее обновление ЦБ: " + rateManager.getLastUpdateTime());
            loadCurrencies();
            updateStats();
        }
    }

    // ============ ВСПОМОГАТЕЛЬНЫЙ КЛАСС ============

    private static class EditableCurrency {
        String code;
        String name;
        String symbol;
        double originalRate;
        double currentRate;

        EditableCurrency(String code, String name, String symbol, double originalRate, double currentRate) {
            this.code = code;
            this.name = name;
            this.symbol = symbol;
            this.originalRate = originalRate;
            this.currentRate = currentRate;
        }

        double getDeviation() {
            if (originalRate == 0) return 0;
            return (currentRate - originalRate) / originalRate * 100;
        }

        String getDeviationText() {
            double dev = getDeviation();
            if (Math.abs(dev) < 0.01) return "=";
            return String.format(Locale.getDefault(), "%+.2f%%", dev);
        }

        int getDeviationColor() {
            double dev = getDeviation();
            if (Math.abs(dev) < 0.01) return 0xFF757575;
            if (dev > 0) return 0xFF4CAF50;
            return 0xFFF44336;
        }
    }

    // ============ ИНТЕРФЕЙС СЛУШАТЕЛЯ ============

    private interface CurrencyEditorListener {
        void onRateChanged(String currencyCode, double newRate);
        void onDeleteCurrency(String currencyCode);
    }

    // ============ АДАПТЕР ============

    private class CurrencyEditorAdapter extends RecyclerView.Adapter<CurrencyEditorAdapter.ViewHolder> {

        private List<EditableCurrency> items;
        private CurrencyEditorListener listener;
        private DecimalFormat df = new DecimalFormat("#,##0.0000");

        CurrencyEditorAdapter(List<EditableCurrency> items, CurrencyEditorListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_currency_editor, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            EditableCurrency item = items.get(position);

            holder.codeText.setText(item.symbol + " " + item.code);
            holder.nameText.setText(item.name);
            holder.officialRateText.setText("ЦБ: " + df.format(item.originalRate) + " ₽");

            holder.rateEdit.setText(df.format(item.currentRate));

            holder.deviationText.setText(item.getDeviationText());
            holder.deviationText.setTextColor(item.getDeviationColor());

            // Удаляем предыдущий TextWatcher
            if (holder.textWatcher != null) {
                holder.rateEdit.removeTextChangedListener(holder.textWatcher);
            }

            holder.textWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    try {
                        String text = s.toString().replace(',', '.');
                        if (!text.isEmpty()) {
                            double newRate = Double.parseDouble(text);
                            item.currentRate = newRate;
                            holder.deviationText.setText(item.getDeviationText());
                            holder.deviationText.setTextColor(item.getDeviationColor());
                            listener.onRateChanged(item.code, newRate);
                        }
                    } catch (NumberFormatException e) {
                        // Игнорируем
                    }
                }
            };

            holder.rateEdit.addTextChangedListener(holder.textWatcher);

            // Скрываем кнопку удаления
            holder.deleteButton.setVisibility(View.GONE);

            holder.resetButton.setOnClickListener(v -> {
                item.currentRate = item.originalRate;
                holder.rateEdit.setText(df.format(item.currentRate));
                listener.onRateChanged(item.code, item.currentRate);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView codeText, nameText, officialRateText, deviationText;
            EditText rateEdit;
            Button resetButton, deleteButton;
            TextWatcher textWatcher;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                codeText = itemView.findViewById(R.id.currencyCodeText);
                nameText = itemView.findViewById(R.id.currencyNameText);
                officialRateText = itemView.findViewById(R.id.officialRateText);
                deviationText = itemView.findViewById(R.id.deviationText);
                rateEdit = itemView.findViewById(R.id.rateEditText);
                resetButton = itemView.findViewById(R.id.resetButton);
                deleteButton = itemView.findViewById(R.id.deleteButton);
            }
        }
    }
}