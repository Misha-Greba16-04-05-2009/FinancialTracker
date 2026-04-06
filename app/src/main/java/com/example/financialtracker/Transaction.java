package com.example.financialtracker;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Transaction implements Serializable {
    private String id;
    private String description;
    private double amount;
    private boolean isIncome;
    private Date date;
    private long timestamp;
    private String category;
    private String notes;
    private String accountName;
    private String accountId;
    private String paymentType;
    private boolean isSelected = false;

    public Transaction(String description, double amount,
                       boolean isIncome, Date date, String category,
                       String notes, String accountName, String paymentType) {
        this.id = generateId();
        this.description = description;
        this.amount = amount;
        this.isIncome = isIncome;
        this.date = date;
        this.timestamp = date != null ? date.getTime() : System.currentTimeMillis();
        this.category = category;
        this.notes = notes;
        this.accountName = accountName;
        this.paymentType = paymentType;
        this.isSelected = false;
    }

    private String generateId() {
        return "transaction_" + System.currentTimeMillis() + "_" + Math.random();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public boolean isIncome() { return isIncome; }
    public void setIncome(boolean income) { isIncome = income; }

    public Date getDate() { return date; }
    public void setDate(Date date) {
        this.date = date;
        if (date != null) this.timestamp = date.getTime();
    }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        this.date = new Date(timestamp);
    }

    public void restoreDate() {
        if (timestamp > 0 && date == null) {
            this.date = new Date(timestamp);
        }
    }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public String getPaymentType() { return paymentType; }
    public void setPaymentType(String paymentType) { this.paymentType = paymentType; }

    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }

    public String getFormattedAmount() {
        return String.format(Locale.getDefault(), "%.2f ₽", amount);
    }

    public String getFormattedDate() {
        if (date == null) return "Дата не указана";
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        return sdf.format(date);
    }

    public String getFormattedDateShort() {
        if (date == null) return "???";
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        return sdf.format(date);
    }

    public String getCategoryWithoutIcon() {
        if (category != null && category.contains(" ")) {
            return category.substring(category.indexOf(" ") + 1);
        }
        return category != null ? category : "Без категории";
    }

    public String getCategoryIcon() {
        if (category != null && category.contains(" ")) {
            return category.split(" ")[0];
        }
        return isIncome ? "📈" : "📉";
    }

    public String getPaymentIcon() {
        if (paymentType == null) return "💳";
        switch (paymentType) {
            case "Наличные": return "💵";
            case "Электронные деньги": return "📱";
            case "Карта":
            default: return "💳";
        }
    }

    public int getPaymentTypeColor() {
        if (paymentType == null) return 0xFF3F51B5;
        switch (paymentType) {
            case "Наличные": return 0xFFFF9800;
            case "Электронные деньги": return 0xFF00BCD4;
            case "Карта":
            default: return 0xFF3F51B5;
        }
    }

    @Override
    public String toString() {
        return String.format(Locale.getDefault(),
                "%s: %s %.2f ₽ (%s)",
                isIncome ? "Доход" : "Расход",
                description, amount,
                getFormattedDateShort());
    }
}