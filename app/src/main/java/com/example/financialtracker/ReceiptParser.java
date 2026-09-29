package com.example.financialtracker;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Разбор кассового чека (РФ).
 *
 * 1) parseQr()   — самый надёжный способ получить сумму: QR-код фискального чека
 *                  содержит строку вида t=20260929T1230&s=1234.56&fn=...&i=...&fp=...&n=1
 * 2) parseText() — разбор распознанного текста (OCR): итоговая сумма и название магазина.
 */
public final class ReceiptParser {

    private ReceiptParser() {}

    /** Результат разбора. Поля могут быть null, если найти не удалось. */
    public static final class Result {
        public String shopName;
        public Double total;
        public String dateTime; // из QR, формат yyyyMMdd'T'HHmm

        public boolean hasTotal() { return total != null && total > 0; }
        public boolean hasShop() { return shopName != null && !shopName.isEmpty(); }

        @Override public String toString() {
            return "shop=" + shopName + ", total=" + total + ", date=" + dateTime;
        }
    }

    // ---------------------------------------------------------------- QR

    private static final Pattern QR_SUM = Pattern.compile("(?:^|&)s=([0-9]+(?:[.,][0-9]{1,2})?)");
    private static final Pattern QR_DATE = Pattern.compile("(?:^|&)t=([0-9]{8}T[0-9]{4,6})");

    /** Разбирает содержимое QR-кода чека. Возвращает null, если это не фискальный QR. */
    public static Result parseQr(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        Matcher m = QR_SUM.matcher(s);
        if (!m.find()) return null;
        Result r = new Result();
        r.total = toDouble(m.group(1));
        Matcher d = QR_DATE.matcher(s);
        if (d.find()) r.dateTime = d.group(1);
        return r;
    }

    // ---------------------------------------------------------------- TEXT

    /** Ключевые слова строки с итогом (в порядке приоритета). */
    private static final String[] TOTAL_KEYS = {
            "ИТОГО К ОПЛАТЕ", "К ОПЛАТЕ", "ИТОГО", "ИТОГ", "ВСЕГО", "СУММА ПО ЧЕКУ",
            "БЕЗНАЛИЧНЫМИ", "НАЛИЧНЫМИ", "TOTAL"
    };

    /** Строки, которые точно не являются названием магазина. */
    private static final String[] NOT_SHOP = {
            "КАССОВЫЙ ЧЕК", "ЧЕК", "ПРИХОД", "ИНН", "ККТ", "ФН", "ФД", "ФП", "РН",
            "СМЕНА", "КАССИР", "ДАТА", "ВРЕМЯ", "САЙТ", "WWW", "HTTP", "НДС", "ТЕЛ",
            "ДОБРО ПОЖАЛОВАТЬ", "СПАСИБО", "АДРЕС", "МЕСТО РАСЧЕТОВ", "МЕСТО РАСЧЁТОВ",
            "СНО", "ОСН", "УСН", "ПОКУПКА", "ПРОДАЖА", "ТОВАРНЫЙ", "№", "ЗН ККТ"
    };

    private static final Pattern ORG_FORM = Pattern.compile(
            "\\b(ООО|ОАО|ЗАО|ПАО|АО|ИП|ТОРГОВЫЙ ДОМ|МАГАЗИН|СУПЕРМАРКЕТ|ГИПЕРМАРКЕТ)\\b",
            Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE);

    /** Число вида 1 234,56 / 1234.56 / =1234.56 / 1234-56 */
    private static final Pattern AMOUNT = Pattern.compile(
            "([0-9]{1,3}(?:[ \\u00A0][0-9]{3})+|[0-9]+)\\s*[.,\\-]\\s*([0-9]{2})(?![0-9])");

    public static Result parseText(String text) {
        Result r = new Result();
        if (text == null || text.trim().isEmpty()) return r;

        List<String> lines = new ArrayList<>();
        for (String l : text.split("\\r?\\n")) {
            String c = normalize(l);
            if (!c.isEmpty()) lines.add(c);
        }
        r.total = findTotal(lines);
        r.shopName = findShopName(lines);
        return r;
    }

    /** Объединяет два результата: приоритет у QR для суммы, у текста для названия. */
    public static Result merge(Result fromQr, Result fromText) {
        Result r = new Result();
        if (fromText != null) { r.shopName = fromText.shopName; r.total = fromText.total; }
        if (fromQr != null) {
            if (fromQr.hasTotal()) r.total = fromQr.total;
            r.dateTime = fromQr.dateTime;
        }
        return r;
    }

    // ---------------------------------------------------------------- internals

    private static Double findTotal(List<String> lines) {
        String[] up = new String[lines.size()];
        for (int i = 0; i < lines.size(); i++) up[i] = lines.get(i).toUpperCase(Locale.ROOT);

        // 1. Ищем по ключевым словам: число в той же строке или в следующей.
        for (String key : TOTAL_KEYS) {
            for (int i = 0; i < up.length; i++) {
                if (!up[i].contains(key)) continue;
                if (up[i].contains("НДС")) continue; // "Сумма НДС 20%" — не итог
                Double v = lastAmount(up[i].substring(up[i].indexOf(key) + key.length()));
                if (v == null && i + 1 < up.length) v = lastAmount(up[i + 1]);
                if (v != null && v > 0) return v;
            }
        }
        // 2. Запасной вариант: максимальная сумма в чеке (итог обычно самый большой).
        Double max = null;
        for (String l : up) {
            if (l.contains("НДС") || l.contains("ИНН") || l.contains("ФН") || l.contains("ФД")) continue;
            Matcher m = AMOUNT.matcher(l);
            while (m.find()) {
                Double v = amountFrom(m);
                if (v != null && v < 1_000_000 && (max == null || v > max)) max = v;
            }
        }
        return max;
    }

    private static Double lastAmount(String s) {
        Matcher m = AMOUNT.matcher(s);
        Double last = null;
        while (m.find()) last = amountFrom(m);
        return last;
    }

    private static Double amountFrom(Matcher m) {
        String whole = m.group(1).replace(" ", "").replace(" ", "");
        return toDouble(whole + "." + m.group(2));
    }

    private static final Pattern QUOTED = Pattern.compile("[\"«“']\\s*([^\"«»“”']{3,50}?)\\s*[\"»”']");

    private static String findShopName(List<String> lines) {
        int limit = Math.min(lines.size(), 12);
        // 1. Название в кавычках в шапке чека: ООО "ПЯТЕРОЧКА", АО «Тандер».
        for (int i = 0; i < limit; i++) {
            Matcher q = QUOTED.matcher(lines.get(i));
            if (q.find() && letterCount(q.group(1)) >= 3
                    && !isServiceLine(q.group(1).toUpperCase(Locale.ROOT))) {
                return cleanShop(q.group(1));
            }
        }
        // 2. Строка с организационно-правовой формой или "магазин".
        for (int i = 0; i < limit; i++) {
            String l = lines.get(i);
            if (ORG_FORM.matcher(l).find() && letterCount(l) >= 3) return cleanShop(l);
        }
        // 3. Первая "осмысленная" строка в шапке чека.
        for (int i = 0; i < limit; i++) {
            String l = lines.get(i);
            String u = l.toUpperCase(Locale.ROOT);
            if (isServiceLine(u)) continue;
            if (letterCount(l) < 3) continue;
            if (digitCount(l) > letterCount(l)) continue; // адрес, дата, номера
            return cleanShop(l);
        }
        return null;
    }

    private static boolean isServiceLine(String u) {
        for (String k : NOT_SHOP) {
            if (u.equals(k) || u.startsWith(k + " ") || u.startsWith(k + ":") || u.contains(" " + k + " ")) {
                return true;
            }
        }
        return u.matches(".*\\d{2}[./]\\d{2}[./]\\d{2,4}.*"); // строка с датой
    }

    private static String cleanShop(String s) {
        String c = s.replaceAll("[\"«»“”]", "\"").replaceAll("\\s{2,}", " ").trim();
        c = c.replaceAll("^[^\\p{L}\\d\"]+|[^\\p{L}\\d\"]+$", "");
        if (c.length() > 60) c = c.substring(0, 60).trim();
        return c;
    }

    /** Чистит строку OCR: латинские "двойники" кириллицы внутри русских слов, O→0 в числах и т.п. */
    static String normalize(String line) {
        String s = line.replace('\t', ' ').replaceAll("\\s{2,}", " ").trim();
        if (s.isEmpty()) return s;
        StringBuilder out = new StringBuilder();
        for (String word : s.split(" ")) {
            if (out.length() > 0) out.append(' ');
            out.append(fixWord(word));
        }
        return out.toString();
    }

    private static final String LAT = "ABCEHKMOPTXaceopxy";
    private static final String CYR = "АВСЕНКМОРТХасеорху";

    private static String fixWord(String w) {
        int cyr = 0, lat = 0, dig = 0;
        for (char ch : w.toCharArray()) {
            if (Character.UnicodeBlock.of(ch) == Character.UnicodeBlock.CYRILLIC) cyr++;
            else if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z')) lat++;
            else if (Character.isDigit(ch)) dig++;
        }
        // Смешанное слово с кириллицей: заменяем латинские двойники на кириллицу.
        if (cyr > 0 && lat > 0) {
            StringBuilder b = new StringBuilder(w);
            for (int i = 0; i < b.length(); i++) {
                int idx = LAT.indexOf(b.charAt(i));
                if (idx >= 0) b.setCharAt(i, CYR.charAt(idx));
            }
            return b.toString();
        }
        // Число с ошибками OCR: 1O5,5O -> 105,50
        if (dig > 0 && dig >= w.length() / 2 && lat + cyr <= 2) {
            return w.replace('O', '0').replace('О', '0').replace('o', '0').replace('о', '0')
                    .replace('l', '1').replace('I', '1').replace('З', '3').replace('б', '6');
        }
        return w;
    }

    private static int letterCount(String s) {
        int n = 0;
        for (char c : s.toCharArray()) if (Character.isLetter(c)) n++;
        return n;
    }

    private static int digitCount(String s) {
        int n = 0;
        for (char c : s.toCharArray()) if (Character.isDigit(c)) n++;
        return n;
    }

    private static Double toDouble(String s) {
        try {
            return Double.parseDouble(s.replace(',', '.'));
        } catch (Exception e) {
            return null;
        }
    }
}
