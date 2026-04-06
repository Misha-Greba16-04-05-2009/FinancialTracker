package com.example.financialtracker;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Transfer implements Serializable {
    private String id;
    private String fromAccount;
    private String toAccount;
    private double amount;
    private double commission;
    private String description;
    private Date date;
    private String notes;
    private transient long timestamp;

    // Конструктор по умолчанию
    public Transfer() {
        this.id = String.valueOf(System.currentTimeMillis());
        this.date = new Date();
        this.timestamp = this.date.getTime();
        this.commission = 0;
    }

    // Конструктор с основными параметрами
    public Transfer(String fromAccount, String toAccount, double amount, Date date) {
        this();
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.date = date;
    }

    // Полный конструктор
    public Transfer(String fromAccount, String toAccount, double amount,
                    String description, Date date, String notes, double commission) {
        this();
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.description = description;
        this.date = date;
        this.notes = notes;
        this.commission = commission;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFromAccount() { return fromAccount; }
    public void setFromAccount(String fromAccount) { this.fromAccount = fromAccount; }

    public String getToAccount() { return toAccount; }
    public void setToAccount(String toAccount) { this.toAccount = toAccount; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public double getCommission() { return commission; }
    public void setCommission(double commission) { this.commission = commission; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Date getDate() { return date; }
    public void setDate(Date date) {
        this.date = date;
        this.timestamp = date.getTime();
    }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        this.date = new Date(timestamp);
    }

    // Метод для восстановления даты
    public void restoreDate() {
        if (timestamp > 0) {
            this.date = new Date(timestamp);
        }
    }

    // Метод для получения общей суммы (сумма + комиссия)
    public double getTotalAmount() {
        return amount + commission;
    }

    // ============ ИСПРАВЛЕННЫЕ МЕТОДЫ ФОРМАТИРОВАНИЯ ============
    public String getFormattedAmount() {
        return String.format(Locale.getDefault(), "%.2f ₽", amount);
    }

    public String getFormattedCommission() {
        return String.format(Locale.getDefault(), "%.2f ₽", commission);
    }

    public String getFormattedTotalAmount() {
        return String.format(Locale.getDefault(), "%.2f ₽", getTotalAmount());
    }

    public String getFormattedAmountWithCommission() {
        if (commission > 0) {
            return String.format(Locale.getDefault(),
                    "%.2f ₽ (+%.2f ₽ комиссия)",
                    amount, commission);
        } else {
            return getFormattedAmount();
        }
    }

    // Метод для форматированной даты
    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        return sdf.format(date);
    }

    // Метод для краткой даты
    public String getFormattedDateShort() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        return sdf.format(date);
    }

    // Метод для получения даты в формате "сегодня/вчера/дата"
    public String getRelativeDate() {
        Calendar now = Calendar.getInstance();
        Calendar transferDate = Calendar.getInstance();
        transferDate.setTime(date);

        SimpleDateFormat todayFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        SimpleDateFormat yesterdayFormat = new SimpleDateFormat("вчера, HH:mm", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.getDefault());

        if (now.get(Calendar.YEAR) == transferDate.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == transferDate.get(Calendar.DAY_OF_YEAR)) {
            return "сегодня, " + todayFormat.format(date);
        } else if (now.get(Calendar.YEAR) == transferDate.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == transferDate.get(Calendar.DAY_OF_YEAR) + 1) {
            return yesterdayFormat.format(date);
        } else {
            return dateFormat.format(date);
        }
    }

    // Метод для проверки валидности перевода
    public boolean isValid() {
        return fromAccount != null && !fromAccount.isEmpty() &&
                toAccount != null && !toAccount.isEmpty() &&
                amount > 0 && date != null;
    }

    // Метод для получения иконки перевода
    public String getIcon() {
        if (commission > 0) {
            return "💸"; // Иконка с деньгами и комиссией
        } else {
            return "🔄"; // Иконка обычного перевода
        }
    }

    // Метод для получения цвета перевода
    public int getColor() {
        if (commission > 0) {
            return 0xFFFF9800; // Оранжевый для переводов с комиссией
        } else {
            return 0xFF2196F3; // Синий для бесплатных переводов
        }
    }

    // Метод для получения цвета суммы
    public int getAmountColor() {
        if (commission > 0) {
            return 0xFFFF5722; // Темно-оранжевый
        } else {
            return 0xFF4CAF50; // Зеленый
        }
    }

    // Метод для получения краткого описания
    public String getShortDescription() {
        if (description != null && !description.isEmpty()) {
            if (description.length() > 30) {
                return description.substring(0, 27) + "...";
            }
            return description;
        }
        return "Перевод между счетами";
    }

    // Метод для получения полного описания
    public String getFullDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("🔄 Перевод: ").append(fromAccount).append(" → ").append(toAccount).append("\n");
        sb.append("💰 Сумма: ").append(getFormattedAmount());

        if (commission > 0) {
            sb.append("\n💸 Комиссия: ").append(getFormattedCommission());
            sb.append("\n💵 Итого: ").append(getFormattedTotalAmount());
        }

        if (description != null && !description.isEmpty()) {
            sb.append("\n📝 Описание: ").append(description);
        }

        if (notes != null && !notes.isEmpty()) {
            sb.append("\n💬 Примечания: ").append(notes);
        }

        sb.append("\n📅 Дата: ").append(getFormattedDate());

        return sb.toString();
    }

    // Метод для получения статуса перевода
    public String getStatus() {
        if (commission > 0) {
            return "💸 С комиссией";
        } else {
            return "✅ Без комиссии";
        }
    }

    @Override
    public String toString() {
        return String.format(Locale.getDefault(),
                "Transfer{id='%s', from='%s', to='%s', amount=%.2f, commission=%.2f, date=%s}",
                id, fromAccount, toAccount, amount, commission, getFormattedDate());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Transfer transfer = (Transfer) obj;
        return id.equals(transfer.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    // Метод для копирования перевода
    public Transfer copy() {
        Transfer copy = new Transfer();
        copy.id = this.id;
        copy.fromAccount = this.fromAccount;
        copy.toAccount = this.toAccount;
        copy.amount = this.amount;
        copy.commission = this.commission;
        copy.description = this.description;
        copy.date = this.date != null ? (Date) this.date.clone() : null;
        copy.notes = this.notes;
        copy.timestamp = this.timestamp;
        return copy;
    }

    // Метод для проверки, является ли перевод сегодняшним
    public boolean isToday() {
        Calendar now = Calendar.getInstance();
        Calendar transferDate = Calendar.getInstance();
        transferDate.setTime(date);

        return now.get(Calendar.YEAR) == transferDate.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == transferDate.get(Calendar.DAY_OF_YEAR);
    }

    // Метод для проверки, является ли перевод вчерашним
    public boolean isYesterday() {
        Calendar now = Calendar.getInstance();
        Calendar transferDate = Calendar.getInstance();
        transferDate.setTime(date);

        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);

        return transferDate.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                transferDate.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR);
    }

    // Метод для проверки, является ли перевод старым (более недели)
    public boolean isOld() {
        Calendar now = Calendar.getInstance();
        Calendar transferDate = Calendar.getInstance();
        transferDate.setTime(date);

        long diff = now.getTimeInMillis() - transferDate.getTimeInMillis();
        long daysDiff = diff / (1000 * 60 * 60 * 24);

        return daysDiff > 7;
    }
}