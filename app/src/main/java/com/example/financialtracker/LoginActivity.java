package com.example.financialtracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameEditText;
    private EditText passwordEditText;
    private CheckBox rememberMeCheckBox;
    private Button loginButton;
    private TextView registerLink;
    private TextView forgotPasswordText;

    private AuthManager authManager;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authManager = new AuthManager(this);
        prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);

        initViews();
        checkSavedLogin();
        setupListeners();
    }

    private void initViews() {
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        rememberMeCheckBox = findViewById(R.id.rememberMeCheckBox);
        loginButton = findViewById(R.id.loginButton);
        registerLink = findViewById(R.id.registerLink);
        forgotPasswordText = findViewById(R.id.forgotPasswordText);
    }

    private void checkSavedLogin() {
        boolean rememberMe = prefs.getBoolean("remember_me", false);
        if (rememberMe) {
            String savedUsername = prefs.getString("username", "");
            // Пароль больше не храним в открытом виде — запоминаем только логин
            prefs.edit().remove("password").apply();
            if (!savedUsername.isEmpty()) {
                usernameEditText.setText(savedUsername);
                rememberMeCheckBox.setChecked(true);
            }
        }
    }

    private void setupListeners() {
        loginButton.setOnClickListener(v -> performLogin());

        registerLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        forgotPasswordText.setOnClickListener(v ->
                Toast.makeText(this, "Функция восстановления пароля в разработке", Toast.LENGTH_SHORT).show()
        );
    }

    private void performLogin() {
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(username)) {
            usernameEditText.setError("Введите имя пользователя или email");
            usernameEditText.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Введите пароль");
            passwordEditText.requestFocus();
            return;
        }

        boolean success;
        if (username.contains("@")) {
            success = authManager.loginWithEmail(username, password, rememberMeCheckBox.isChecked());
        } else {
            success = authManager.login(username, password, rememberMeCheckBox.isChecked());
        }

        if (success) {
            if (rememberMeCheckBox.isChecked()) {
                prefs.edit()
                        .putBoolean("remember_me", true)
                        .putString("username", username)
                        .remove("password")
                        .apply();
            } else {
                prefs.edit().clear().apply();
            }

            Toast.makeText(this, "Добро пожаловать, " + username + "!", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Неверное имя пользователя или пароль", Toast.LENGTH_LONG).show();
        }
    }
}