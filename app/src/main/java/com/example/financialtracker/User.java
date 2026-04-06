package com.example.financialtracker;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class User implements Serializable {
    private String id;
    private String username;
    private String email;
    private String passwordHash; // Храним хеш пароля, не сам пароль
    private String fullName;
    private Date registrationDate;
    private Date lastLoginDate;
    private boolean isActive;
    private String profileIcon;
    private String currencyPreference; // RUB, USD, EUR и т.д.
    private String languagePreference; // ru, en и т.д.

    public User() {
        this.id = UUID.randomUUID().toString();
        this.registrationDate = new Date();
        this.isActive = true;
        this.profileIcon = "👤";
        this.currencyPreference = "RUB";
        this.languagePreference = "ru";
    }

    public User(String username, String email, String password, String fullName) {
        this();
        this.username = username;
        this.email = email;
        this.passwordHash = hashPassword(password);
        this.fullName = fullName;
    }

    // Хеширование пароля (простой способ, для продакшена используйте BCrypt)
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return password; // fallback, небезопасно
        }
    }

    // Проверка пароля
    public boolean checkPassword(String password) {
        return passwordHash.equals(hashPassword(password));
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setPassword(String password) { this.passwordHash = hashPassword(password); }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public Date getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(Date registrationDate) { this.registrationDate = registrationDate; }

    public Date getLastLoginDate() { return lastLoginDate; }
    public void setLastLoginDate(Date lastLoginDate) { this.lastLoginDate = lastLoginDate; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public String getProfileIcon() { return profileIcon; }
    public void setProfileIcon(String profileIcon) { this.profileIcon = profileIcon; }

    public String getCurrencyPreference() { return currencyPreference; }
    public void setCurrencyPreference(String currencyPreference) { this.currencyPreference = currencyPreference; }

    public String getLanguagePreference() { return languagePreference; }
    public void setLanguagePreference(String languagePreference) { this.languagePreference = languagePreference; }

    public String getFormattedRegistrationDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        return sdf.format(registrationDate);
    }

    public String getFormattedLastLoginDate() {
        if (lastLoginDate == null) return "Никогда";
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        return sdf.format(lastLoginDate);
    }

    @Override
    public String toString() {
        return String.format(Locale.getDefault(),
                "User{id='%s', username='%s', email='%s', fullName='%s'}",
                id, username, email, fullName);
    }
}