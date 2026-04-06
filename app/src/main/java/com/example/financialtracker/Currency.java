package com.example.financialtracker;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class Currency implements Serializable, Comparable<Currency> {
    private String code;
    private String symbol;
    private String name;
    private double rateToBase;
    private boolean isBase;
    private int decimalPlaces;

    // ТОЛЬКО 5 ВАЛЮТ
    public static final Currency RUB = new Currency("RUB", "₽", "Российский рубль", 1.0, true, 2);
    public static final Currency USD = new Currency("USD", "$", "Доллар США", 0, false, 2);
    public static final Currency EUR = new Currency("EUR", "€", "Евро", 0, false, 2);
    public static final Currency CNY = new Currency("CNY", "¥", "Китайский юань", 0, false, 2);
    public static final Currency AED = new Currency("AED", "د.إ", "Дирхам ОАЭ", 0, false, 2);

    public Currency(String code, String symbol, String name, double rateToBase, boolean isBase, int decimalPlaces) {
        this.code = code;
        this.symbol = symbol;
        this.name = name;
        this.rateToBase = rateToBase;
        this.isBase = isBase;
        this.decimalPlaces = decimalPlaces;
    }

    public String getCode() { return code; }
    public String getSymbol() { return symbol; }
    public String getName() { return name; }
    public double getRateToBase() { return rateToBase; }
    public void setRateToBase(double rateToBase) { this.rateToBase = rateToBase; }
    public boolean isBase() { return isBase; }
    public void setBase(boolean base) { isBase = base; }
    public int getDecimalPlaces() { return decimalPlaces; }

    public String format(double amount) {
        DecimalFormat df = new DecimalFormat("#,###.##");
        df.setMinimumFractionDigits(decimalPlaces);
        df.setMaximumFractionDigits(decimalPlaces);
        return symbol + " " + df.format(amount);
    }

    public double convertToBase(double amount) {
        return amount * rateToBase;
    }

    public double convertFromBase(double amount) {
        if (rateToBase == 0) return 0;
        return amount / rateToBase;
    }

    public static double convert(double amount, Currency from, Currency to) {
        if (from.getCode().equals(to.getCode())) return amount;
        double inBase = from.convertToBase(amount);
        return to.convertFromBase(inBase);
    }

    @Override
    public String toString() {
        return symbol + " " + code + " - " + name;
    }

    @Override
    public int compareTo(Currency other) {
        if (this.isBase && !other.isBase) return -1;
        if (!this.isBase && other.isBase) return 1;
        return this.code.compareTo(other.code);
    }

    public static List<Currency> getDefaultCurrencies() {
        List<Currency> list = new ArrayList<>();
        list.add(RUB);
        list.add(USD);
        list.add(EUR);
        list.add(CNY);
        list.add(AED);
        return list;
    }
}