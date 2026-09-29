package com.example.financialtracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Официальные курсы ЦБ РФ по ВСЕМ валютам, которые публикует Центробанк (~50 валют).
 * Источник: https://www.cbr.ru/scripts/XML_daily.asp
 * Запасной источник (если сайт ЦБ недоступен): https://www.cbr-xml-daily.ru/daily_json.js — зеркало тех же данных.
 * Последние загруженные курсы сохраняются и используются без интернета.
 */
public final class CbrAllRatesProvider {

    public static final String CBR_URL = "https://www.cbr.ru/scripts/XML_daily.asp";
    public static final String MIRROR_URL = "https://www.cbr-xml-daily.ru/daily_json.js";

    private static final String PREFS = "cbr_all_rates";
    private static final String KEY_JSON = "rates_json";
    private static final String KEY_DATE = "rates_date";
    private static final String KEY_LOADED_AT = "loaded_at";
    private static final long MAX_AGE_MS = 6 * 60 * 60 * 1000L; // обновляем не чаще раза в 6 часов

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    /** Курс одной валюты: value рублей за nominal единиц. */
    public static final class Rate {
        public final String code;
        public final String name;
        public final int nominal;
        public final double value;

        public Rate(String code, String name, int nominal, double value) {
            this.code = code;
            this.name = name;
            this.nominal = nominal <= 0 ? 1 : nominal;
            this.value = value;
        }

        /** Сколько рублей стоит 1 единица валюты. */
        public double rubPerUnit() {
            return value / nominal;
        }
    }

    public static final class Result {
        public final List<Rate> rates;
        public final String date;        // дата курсов ЦБ, dd.MM.yyyy
        public final long loadedAt;      // когда загрузили
        public final boolean fromCache;  // true — нет сети, показаны сохранённые курсы
        public final String error;

        Result(List<Rate> rates, String date, long loadedAt, boolean fromCache, String error) {
            this.rates = rates;
            this.date = date;
            this.loadedAt = loadedAt;
            this.fromCache = fromCache;
            this.error = error;
        }
    }

    public interface Callback {
        void onResult(Result result);
    }

    private CbrAllRatesProvider() {}

    /** Загружает курсы в фоне. force = true — всегда идти в сеть. */
    public static void load(Context context, boolean force, Callback callback) {
        final Context app = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            SharedPreferences prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            Result cached = readCache(prefs, true, null);
            boolean fresh = cached != null && System.currentTimeMillis() - cached.loadedAt < MAX_AGE_MS;

            Result result;
            if (!force && fresh) {
                result = new Result(cached.rates, cached.date, cached.loadedAt, false, null);
            } else {
                String error = null;
                List<Rate> rates = null;
                String[] date = new String[1];
                try {
                    rates = parseCbrXml(download(CBR_URL, "windows-1251"), date);
                } catch (Exception e) {
                    error = "ЦБ: " + e.getMessage();
                }
                if (rates == null || rates.isEmpty()) {
                    try {
                        rates = parseMirrorJson(download(MIRROR_URL, "UTF-8"), date);
                        error = null;
                    } catch (Exception e) {
                        error = (error == null ? "" : error + "; ") + "зеркало: " + e.getMessage();
                    }
                }

                if (rates != null && !rates.isEmpty()) {
                    long now = System.currentTimeMillis();
                    writeCache(prefs, rates, date[0], now);
                    result = new Result(rates, date[0], now, false, null);
                } else if (cached != null) {
                    result = readCache(prefs, true, error);
                } else {
                    result = new Result(new ArrayList<>(), null, 0, true, error);
                }
            }
            final Result r = result;
            MAIN.post(() -> callback.onResult(r));
        });
    }

    // ------------------------------------------------------------------ сеть

    private static String download(String url, String charset) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(10000);
        c.setReadTimeout(15000);
        c.setRequestProperty("User-Agent", "FinancialTracker/1.0 (Android)");
        try {
            int code = c.getResponseCode();
            if (code != 200) throw new IllegalStateException("HTTP " + code);
            try (InputStream in = c.getInputStream()) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
                return new String(bos.toByteArray(), Charset.forName(charset));
            }
        } finally {
            c.disconnect();
        }
    }

    // ------------------------------------------------------------------ разбор

    private static final Pattern VALUTE = Pattern.compile("<Valute\\b[^>]*>(.*?)</Valute>", Pattern.DOTALL);
    private static final Pattern DATE_ATTR = Pattern.compile("<ValCurs[^>]*\\bDate=\"([0-9.]+)\"");

    /** Разбор XML ЦБ РФ: &lt;Valute&gt;&lt;CharCode&gt;USD&lt;/CharCode&gt;&lt;Nominal&gt;1&lt;/Nominal&gt;... */
    static List<Rate> parseCbrXml(String xml, String[] dateOut) {
        List<Rate> list = new ArrayList<>();
        if (xml == null) return list;
        Matcher d = DATE_ATTR.matcher(xml);
        if (d.find() && dateOut != null) dateOut[0] = d.group(1);

        Matcher m = VALUTE.matcher(xml);
        while (m.find()) {
            String block = m.group(1);
            String code = tag(block, "CharCode");
            String name = tag(block, "Name");
            String nominal = tag(block, "Nominal");
            String value = tag(block, "Value");
            if (code == null || value == null) continue;
            try {
                list.add(new Rate(code.trim().toUpperCase(Locale.ROOT),
                        name == null ? code : name.trim(),
                        nominal == null ? 1 : Integer.parseInt(nominal.trim()),
                        Double.parseDouble(value.trim().replace(',', '.').replace(" ", ""))));
            } catch (NumberFormatException ignored) { }
        }
        return list;
    }

    private static String tag(String block, String name) {
        Matcher m = Pattern.compile("<" + name + ">(.*?)</" + name + ">", Pattern.DOTALL).matcher(block);
        return m.find() ? m.group(1) : null;
    }

    /** Разбор зеркала cbr-xml-daily.ru (JSON). */
    static List<Rate> parseMirrorJson(String json, String[] dateOut) throws Exception {
        List<Rate> list = new ArrayList<>();
        JSONObject root = new JSONObject(json);
        String date = root.optString("Date", "");
        if (dateOut != null && date.length() >= 10) {
            // 2026-09-29T11:30:00+03:00 -> 29.09.2026
            dateOut[0] = date.substring(8, 10) + "." + date.substring(5, 7) + "." + date.substring(0, 4);
        }
        JSONObject valute = root.getJSONObject("Valute");
        Iterator<String> keys = valute.keys();
        while (keys.hasNext()) {
            JSONObject v = valute.getJSONObject(keys.next());
            list.add(new Rate(v.getString("CharCode"), v.optString("Name", v.getString("CharCode")),
                    v.optInt("Nominal", 1), v.getDouble("Value")));
        }
        return list;
    }

    // ------------------------------------------------------------------ кэш

    private static void writeCache(SharedPreferences prefs, List<Rate> rates, String date, long now) {
        try {
            JSONArray arr = new JSONArray();
            for (Rate r : rates) {
                JSONObject o = new JSONObject();
                o.put("c", r.code);
                o.put("n", r.name);
                o.put("nom", r.nominal);
                o.put("v", r.value);
                arr.put(o);
            }
            prefs.edit()
                    .putString(KEY_JSON, arr.toString())
                    .putString(KEY_DATE, date)
                    .putLong(KEY_LOADED_AT, now)
                    .apply();
        } catch (Exception ignored) { }
    }

    private static Result readCache(SharedPreferences prefs, boolean fromCache, String error) {
        String json = prefs.getString(KEY_JSON, null);
        if (json == null) return null;
        try {
            JSONArray arr = new JSONArray(json);
            List<Rate> list = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                list.add(new Rate(o.getString("c"), o.getString("n"), o.getInt("nom"), o.getDouble("v")));
            }
            return new Result(list, prefs.getString(KEY_DATE, null),
                    prefs.getLong(KEY_LOADED_AT, 0), fromCache, error);
        } catch (Exception e) {
            return null;
        }
    }
}
