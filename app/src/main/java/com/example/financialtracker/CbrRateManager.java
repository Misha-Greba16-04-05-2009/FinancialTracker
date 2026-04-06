package com.example.financialtracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CbrRateManager {
    private static final String TAG = "CbrRateManager";
    private static final String PREFS_NAME = "cbr_rates";
    private static final String KEY_RATES = "rates";
    private static final String KEY_CUSTOM_RATES = "custom_rates";
    private static final String KEY_LAST_UPDATE = "last_update";

    private static CbrRateManager instance;
    private Context context;
    private SharedPreferences prefs;

    private Map<String, Double> officialRates = new HashMap<>();
    private Map<String, Double> customRates = new HashMap<>();

    private List<RateUpdateListener> listeners = new ArrayList<>();
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface RateUpdateListener {
        void onRatesUpdated(boolean success, String message);
    }

    private CbrRateManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadOfficialRates();
        loadCustomRates();
    }

    public static synchronized CbrRateManager getInstance(Context context) {
        if (instance == null) {
            instance = new CbrRateManager(context);
        }
        return instance;
    }

    public void fetchLatestRates() {
        fetchLatestRates(null);
    }

    public void fetchLatestRates(final RateUpdateListener listener) {
        if (listener != null) addListener(listener);

        executor.execute(() -> {
            boolean success = false;
            String message = "";

            try {
                URL url = new URL("https://www.cbr-xml-daily.ru/daily_json.js");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "FinancialTracker App");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), "UTF-8"));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject json = new JSONObject(response.toString());
                    JSONObject valute = json.getJSONObject("Valute");

                    Map<String, Double> newRates = new HashMap<>();
                    newRates.put("RUB", 1.0);

                    // Доллар США
                    if (valute.has("USD")) {
                        JSONObject usd = valute.getJSONObject("USD");
                        newRates.put("USD", usd.getDouble("Value"));
                        Log.d(TAG, "1 USD = " + usd.getDouble("Value") + " RUB");
                    }

                    // Евро
                    if (valute.has("EUR")) {
                        JSONObject eur = valute.getJSONObject("EUR");
                        newRates.put("EUR", eur.getDouble("Value"));
                        Log.d(TAG, "1 EUR = " + eur.getDouble("Value") + " RUB");
                    }

                    // Юань
                    if (valute.has("CNY")) {
                        JSONObject cny = valute.getJSONObject("CNY");
                        newRates.put("CNY", cny.getDouble("Value"));
                        Log.d(TAG, "1 CNY = " + cny.getDouble("Value") + " RUB");
                    }

                    // Дирхам
                    if (valute.has("AED")) {
                        JSONObject aed = valute.getJSONObject("AED");
                        newRates.put("AED", aed.getDouble("Value"));
                        Log.d(TAG, "1 AED = " + aed.getDouble("Value") + " RUB");
                    } else {
                        newRates.put("AED", 21.02);
                        Log.d(TAG, "1 AED = 21.02 RUB (дефолтный)");
                    }

                    officialRates = newRates;
                    saveOfficialRates();
                    saveLastUpdateTime();
                    success = true;
                    message = "Курсы обновлены";
                } else {
                    message = "Ошибка сервера: " + responseCode;
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка: " + e.getMessage());
                success = false;
                message = "Ошибка сети";
            }

            final boolean finalSuccess = success;
            final String finalMessage = message;

            mainHandler.post(() -> {
                notifyListeners(finalSuccess, finalMessage);
                if (listener != null) removeListener(listener);
            });
        });
    }

    public double getOfficialRate(String currencyCode) {
        if (currencyCode.equals("RUB")) return 1.0;
        return officialRates.getOrDefault(currencyCode, 0.0);
    }

    public double getRate(String currencyCode) {
        if (currencyCode.equals("RUB")) return 1.0;

        if (customRates.containsKey(currencyCode)) {
            return customRates.get(currencyCode);
        }

        return officialRates.getOrDefault(currencyCode, 0.0);
    }

    public void setCustomRate(String currencyCode, double rate) {
        if (rate > 0) {
            customRates.put(currencyCode, rate);
            saveCustomRates();
        }
    }

    public void resetToOfficial(String currencyCode) {
        customRates.remove(currencyCode);
        saveCustomRates();
    }

    public boolean isCustomRate(String currencyCode) {
        return customRates.containsKey(currencyCode);
    }

    public double getDeviationPercent(String currencyCode) {
        double official = getOfficialRate(currencyCode);
        double current = getRate(currencyCode);
        if (official == 0) return 0;
        return ((current - official) / official) * 100;
    }

    public void resetAllCustomRates() {
        customRates.clear();
        saveCustomRates();
    }

    public double convert(double amount, String fromCurrency, String toCurrency) {
        if (fromCurrency.equals(toCurrency)) return amount;

        double fromRate = getRate(fromCurrency);
        double toRate = getRate(toCurrency);

        if (fromRate == 0 || toRate == 0) return 0;

        double amountInRub = amount * fromRate;
        return amountInRub / toRate;
    }

    public double getInverseRate(String currencyCode) {
        double rate = getRate(currencyCode);
        return rate == 0 ? 0 : 1.0 / rate;
    }

    public String getBaseCurrency() {
        return "RUB";
    }

    public List<String> getSupportedCurrencies() {
        List<String> list = new ArrayList<>();
        list.add("RUB");
        list.add("USD");
        list.add("EUR");
        list.add("CNY");
        list.add("AED");
        return list;
    }

    public String getCurrencySymbol(String code) {
        switch (code) {
            case "RUB": return "₽";
            case "USD": return "$";
            case "EUR": return "€";
            case "CNY": return "¥";
            case "AED": return "د.إ";
            default: return code;
        }
    }

    public String getCurrencyName(String code) {
        switch (code) {
            case "RUB": return "Российский рубль";
            case "USD": return "Доллар США";
            case "EUR": return "Евро";
            case "CNY": return "Китайский юань";
            case "AED": return "Дирхам ОАЭ";
            default: return code;
        }
    }

    private void saveOfficialRates() {
        try {
            JSONObject json = new JSONObject();
            for (Map.Entry<String, Double> entry : officialRates.entrySet()) {
                json.put(entry.getKey(), entry.getValue());
            }
            prefs.edit().putString(KEY_RATES, json.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadOfficialRates() {
        String ratesJson = prefs.getString(KEY_RATES, "");
        if (!ratesJson.isEmpty()) {
            try {
                JSONObject json = new JSONObject(ratesJson);
                for (String currency : getSupportedCurrencies()) {
                    if (json.has(currency)) {
                        officialRates.put(currency, json.getDouble(currency));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (officialRates.isEmpty()) {
            setDefaultRates();
        }
    }

    private void saveCustomRates() {
        try {
            JSONObject json = new JSONObject();
            for (Map.Entry<String, Double> entry : customRates.entrySet()) {
                json.put(entry.getKey(), entry.getValue());
            }
            prefs.edit().putString(KEY_CUSTOM_RATES, json.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadCustomRates() {
        String ratesJson = prefs.getString(KEY_CUSTOM_RATES, "");
        if (!ratesJson.isEmpty()) {
            try {
                JSONObject json = new JSONObject(ratesJson);
                for (String currency : getSupportedCurrencies()) {
                    if (json.has(currency)) {
                        customRates.put(currency, json.getDouble(currency));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void saveLastUpdateTime() {
        prefs.edit().putLong(KEY_LAST_UPDATE, System.currentTimeMillis()).apply();
    }

    public String getLastUpdateTime() {
        long lastUpdate = prefs.getLong(KEY_LAST_UPDATE, 0);
        if (lastUpdate == 0) return "Никогда";
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        return sdf.format(new Date(lastUpdate));
    }

    private void setDefaultRates() {
        officialRates.put("RUB", 1.0);
        officialRates.put("USD", 77.19);
        officialRates.put("EUR", 91.71);
        officialRates.put("CNY", 11.16);
        officialRates.put("AED", 21.02);
        saveOfficialRates();
    }

    public void addListener(RateUpdateListener listener) {
        if (!listeners.contains(listener)) listeners.add(listener);
    }

    public void removeListener(RateUpdateListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(boolean success, String message) {
        for (RateUpdateListener listener : listeners) {
            listener.onRatesUpdated(success, message);
        }
    }
}