package com.example.financialtracker;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Account implements Serializable {
    private String id;
    private String name;
    private String type;
    private String paymentType;
    private double balance;
    private String notes;
    private Date createdDate;
    private transient long timestamp;
    private boolean includedInTotalBalance;

    public Account(String name, String type, String paymentType, double balance, String notes) {
        this(name, type, paymentType, balance, notes, new Date(), true);
    }

    public Account(String name, String type, String paymentType, double balance,
                   String notes, Date createdDate, boolean includedInTotalBalance) {
        this.id = generateId();
        this.name = name;
        this.type = type != null ? type : "Основной";
        this.paymentType = paymentType != null ? paymentType : "Карта";
        this.balance = balance;
        this.notes = notes;
        this.createdDate = createdDate != null ? createdDate : new Date();
        this.includedInTotalBalance = includedInTotalBalance;
        this.timestamp = this.createdDate.getTime();
    }

    private String generateId() {
        return "account_" + System.currentTimeMillis() + "_" + Math.random();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getPaymentType() { return paymentType; }
    public void setPaymentType(String paymentType) { this.paymentType = paymentType; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) {
        this.balance = balance;
    }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getDescription() { return notes; }
    public void setDescription(String description) { this.notes = description; }

    public Date getCreatedDate() { return createdDate; }
    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
        if (createdDate != null) this.timestamp = createdDate.getTime();
    }

    public boolean isIncludedInTotalBalance() { return includedInTotalBalance; }
    public void setIncludedInTotalBalance(boolean includedInTotalBalance) {
        this.includedInTotalBalance = includedInTotalBalance;
    }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        this.createdDate = new Date(timestamp);
    }

    public String getFormattedBalance() {
        return String.format(Locale.getDefault(), "%.2f ₽", balance);
    }

    public String getFullName() {
        return name + " (" + paymentType + ")";
    }

    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        return sdf.format(createdDate);
    }

    public String getIcon() {
        switch (type) {
            case "Сберегательный": return "💰";
            case "Инвестиционный": return "📈";
            case "Кредитный": return "💳";
            case "Основной":
            default: return getPaymentIcon();
        }
    }

    public String getPaymentIcon() {
        switch (paymentType) {
            case "Наличные": return "💵";
            case "Электронные деньги": return "📱";
            case "Карта":
            default: return "💳";
        }
    }

    public int getBalanceColor() {
        if (isCreditAccount()) return 0xFFF44336;
        return balance >= 0 ? 0xFF4CAF50 : 0xFFF44336;
    }

    public boolean isCreditAccount() {
        return type.equals("Кредитный");
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Account account = (Account) obj;
        return id.equals(account.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}