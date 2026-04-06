package com.example.financialtracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AuthManager {
    private static final String TAG = "AuthManager";
    private static final String AUTH_FILE_NAME = "users.json";
    private static final String PREFS_NAME = "auth_prefs";
    private static final String KEY_CURRENT_USER_ID = "current_user_id";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_REMEMBER_ME = "remember_me";

    private Context context;
    private SharedPreferences sharedPreferences;
    private List<User> users;
    private User currentUser;
    private SimpleDateFormat dateFormat;

    public AuthManager(Context context) {
        this.context = context;
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault());
        this.users = new ArrayList<>();
        loadUsers();
        loadCurrentUser();
    }

    // ============ РАБОТА С JSON ФАЙЛОМ ============

    private File getUsersFile() {
        return new File(context.getFilesDir(), AUTH_FILE_NAME);
    }

    private void loadUsers() {
        File file = getUsersFile();
        if (!file.exists()) {
            users = new ArrayList<>();
            return;
        }

        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }
            reader.close();

            JSONArray usersArray = new JSONArray(jsonBuilder.toString());
            users = new ArrayList<>();

            for (int i = 0; i < usersArray.length(); i++) {
                JSONObject userObj = usersArray.getJSONObject(i);
                User user = new User();

                user.setId(userObj.getString("id"));
                user.setUsername(userObj.getString("username"));
                user.setEmail(userObj.getString("email"));
                user.setPasswordHash(userObj.getString("passwordHash"));
                user.setFullName(userObj.optString("fullName", ""));

                if (userObj.has("registrationDate")) {
                    String dateStr = userObj.getString("registrationDate");
                    try {
                        user.setRegistrationDate(dateFormat.parse(dateStr));
                    } catch (Exception e) {
                        user.setRegistrationDate(new Date());
                    }
                }

                if (userObj.has("lastLoginDate") && !userObj.isNull("lastLoginDate")) {
                    String dateStr = userObj.getString("lastLoginDate");
                    try {
                        user.setLastLoginDate(dateFormat.parse(dateStr));
                    } catch (Exception e) {
                        user.setLastLoginDate(null);
                    }
                }

                user.setActive(userObj.optBoolean("isActive", true));
                user.setProfileIcon(userObj.optString("profileIcon", "👤"));
                user.setCurrencyPreference(userObj.optString("currencyPreference", "RUB"));
                user.setLanguagePreference(userObj.optString("languagePreference", "ru"));

                users.add(user);
            }

            Log.d(TAG, "Загружено пользователей: " + users.size());

        } catch (Exception e) {
            Log.e(TAG, "Ошибка загрузки пользователей: " + e.getMessage());
            e.printStackTrace();
            users = new ArrayList<>();
        }
    }

    private void saveUsers() {
        try {
            JSONArray usersArray = new JSONArray();

            for (User user : users) {
                JSONObject userObj = new JSONObject();
                userObj.put("id", user.getId());
                userObj.put("username", user.getUsername());
                userObj.put("email", user.getEmail());
                userObj.put("passwordHash", user.getPasswordHash());
                userObj.put("fullName", user.getFullName());
                userObj.put("registrationDate", dateFormat.format(user.getRegistrationDate()));
                userObj.put("lastLoginDate", user.getLastLoginDate() != null ? dateFormat.format(user.getLastLoginDate()) : JSONObject.NULL);
                userObj.put("isActive", user.isActive());
                userObj.put("profileIcon", user.getProfileIcon());
                userObj.put("currencyPreference", user.getCurrencyPreference());
                userObj.put("languagePreference", user.getLanguagePreference());

                usersArray.put(userObj);
            }

            FileWriter writer = new FileWriter(getUsersFile());
            writer.write(usersArray.toString(2)); // Красивое форматирование с отступами
            writer.close();

            Log.d(TAG, "Сохранено пользователей: " + users.size());

        } catch (Exception e) {
            Log.e(TAG, "Ошибка сохранения пользователей: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ============ РЕГИСТРАЦИЯ И ВХОД ============

    public boolean register(String username, String email, String password, String fullName) {
        // Проверка на существующего пользователя
        for (User user : users) {
            if (user.getUsername().equalsIgnoreCase(username)) {
                Log.e(TAG, "Имя пользователя уже занято: " + username);
                return false;
            }
            if (user.getEmail().equalsIgnoreCase(email)) {
                Log.e(TAG, "Email уже зарегистрирован: " + email);
                return false;
            }
        }

        // Создание нового пользователя
        User newUser = new User(username, email, password, fullName);
        users.add(newUser);
        saveUsers();

        Log.d(TAG, "Зарегистрирован новый пользователь: " + username);
        return true;
    }

    public boolean login(String username, String password, boolean rememberMe) {
        for (User user : users) {
            if (user.getUsername().equalsIgnoreCase(username) && user.checkPassword(password)) {
                if (!user.isActive()) {
                    Log.e(TAG, "Пользователь заблокирован: " + username);
                    return false;
                }

                // Обновляем дату последнего входа
                user.setLastLoginDate(new Date());
                saveUsers();

                // Сохраняем текущего пользователя
                currentUser = user;
                sharedPreferences.edit()
                        .putString(KEY_CURRENT_USER_ID, user.getId())
                        .putBoolean(KEY_IS_LOGGED_IN, true)
                        .putBoolean(KEY_REMEMBER_ME, rememberMe)
                        .apply();

                Log.d(TAG, "Пользователь вошел: " + username);
                return true;
            }
        }

        Log.e(TAG, "Неверное имя пользователя или пароль");
        return false;
    }

    public boolean loginWithEmail(String email, String password, boolean rememberMe) {
        for (User user : users) {
            if (user.getEmail().equalsIgnoreCase(email) && user.checkPassword(password)) {
                if (!user.isActive()) {
                    Log.e(TAG, "Пользователь заблокирован: " + email);
                    return false;
                }

                user.setLastLoginDate(new Date());
                saveUsers();

                currentUser = user;
                sharedPreferences.edit()
                        .putString(KEY_CURRENT_USER_ID, user.getId())
                        .putBoolean(KEY_IS_LOGGED_IN, true)
                        .putBoolean(KEY_REMEMBER_ME, rememberMe)
                        .apply();

                Log.d(TAG, "Пользователь вошел по email: " + email);
                return true;
            }
        }

        Log.e(TAG, "Неверный email или пароль");
        return false;
    }

    public void logout() {
        if (currentUser != null) {
            Log.d(TAG, "Пользователь вышел: " + currentUser.getUsername());
        }

        currentUser = null;
        sharedPreferences.edit()
                .putBoolean(KEY_IS_LOGGED_IN, false)
                .apply();

        if (!sharedPreferences.getBoolean(KEY_REMEMBER_ME, false)) {
            sharedPreferences.edit().remove(KEY_CURRENT_USER_ID).apply();
        }
    }

    private void loadCurrentUser() {
        boolean isLoggedIn = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
        if (!isLoggedIn) {
            currentUser = null;
            return;
        }

        String userId = sharedPreferences.getString(KEY_CURRENT_USER_ID, null);
        if (userId == null) {
            currentUser = null;
            return;
        }

        for (User user : users) {
            if (user.getId().equals(userId)) {
                currentUser = user;
                Log.d(TAG, "Загружен текущий пользователь: " + user.getUsername());
                break;
            }
        }
    }

    // ============ GETTERS ============

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(users);
    }

    public User getUserById(String id) {
        for (User user : users) {
            if (user.getId().equals(id)) {
                return user;
            }
        }
        return null;
    }

    public User getUserByUsername(String username) {
        for (User user : users) {
            if (user.getUsername().equalsIgnoreCase(username)) {
                return user;
            }
        }
        return null;
    }

    public User getUserByEmail(String email) {
        for (User user : users) {
            if (user.getEmail().equalsIgnoreCase(email)) {
                return user;
            }
        }
        return null;
    }

    // ============ УПРАВЛЕНИЕ ПОЛЬЗОВАТЕЛЯМИ ============

    public boolean updateUser(User updatedUser) {
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getId().equals(updatedUser.getId())) {
                users.set(i, updatedUser);
                saveUsers();

                if (currentUser != null && currentUser.getId().equals(updatedUser.getId())) {
                    currentUser = updatedUser;
                }

                Log.d(TAG, "Пользователь обновлен: " + updatedUser.getUsername());
                return true;
            }
        }
        return false;
    }

    public boolean changePassword(String userId, String oldPassword, String newPassword) {
        User user = getUserById(userId);
        if (user != null && user.checkPassword(oldPassword)) {
            user.setPassword(newPassword);
            saveUsers();
            Log.d(TAG, "Пароль изменен для пользователя: " + user.getUsername());
            return true;
        }
        return false;
    }

    public boolean deleteUser(String userId) {
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getId().equals(userId)) {
                users.remove(i);
                saveUsers();

                if (currentUser != null && currentUser.getId().equals(userId)) {
                    currentUser = null;
                    sharedPreferences.edit().clear().apply();
                }

                Log.d(TAG, "Пользователь удален: " + userId);
                return true;
            }
        }
        return false;
    }

    public boolean deactivateUser(String userId) {
        User user = getUserById(userId);
        if (user != null) {
            user.setActive(false);
            saveUsers();

            if (currentUser != null && currentUser.getId().equals(userId)) {
                logout();
            }

            Log.d(TAG, "Пользователь деактивирован: " + userId);
            return true;
        }
        return false;
    }

    public boolean activateUser(String userId) {
        User user = getUserById(userId);
        if (user != null) {
            user.setActive(true);
            saveUsers();
            Log.d(TAG, "Пользователь активирован: " + userId);
            return true;
        }
        return false;
    }

    // ============ СТАТИСТИКА ============

    public int getTotalUsersCount() {
        return users.size();
    }

    public int getActiveUsersCount() {
        int count = 0;
        for (User user : users) {
            if (user.isActive()) {
                count++;
            }
        }
        return count;
    }

    public String getStatistics() {
        int total = users.size();
        int active = getActiveUsersCount();
        int inactive = total - active;

        return String.format(Locale.getDefault(),
                "📊 Статистика пользователей:\n\n" +
                        "👥 Всего пользователей: %d\n" +
                        "✅ Активных: %d\n" +
                        "❌ Неактивных: %d",
                total, active, inactive);
    }
}