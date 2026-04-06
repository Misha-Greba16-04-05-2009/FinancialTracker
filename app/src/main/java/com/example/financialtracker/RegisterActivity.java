package com.example.financialtracker;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    private ImageButton backButton;
    private EditText usernameEditText;
    private EditText fullNameEditText;
    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText confirmPasswordEditText;
    private CheckBox termsCheckBox;
    private Button registerButton;
    private TextView loginLink;

    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authManager = new AuthManager(this);

        initViews();
        setupListeners();
    }

    private void initViews() {
        backButton = findViewById(R.id.backButton);
        usernameEditText = findViewById(R.id.usernameEditText);
        fullNameEditText = findViewById(R.id.fullNameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        termsCheckBox = findViewById(R.id.termsCheckBox);
        registerButton = findViewById(R.id.registerButton);
        loginLink = findViewById(R.id.loginLink);
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> finish());

        loginLink.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        registerButton.setOnClickListener(v -> performRegistration());
    }

    private void performRegistration() {
        String username = usernameEditText.getText().toString().trim();
        String fullName = fullNameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(username)) {
            usernameEditText.setError("Введите имя пользователя");
            usernameEditText.requestFocus();
            return;
        }

        if (username.length() < 3) {
            usernameEditText.setError("Имя пользователя должно содержать минимум 3 символа");
            usernameEditText.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Введите email");
            emailEditText.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Введите корректный email");
            emailEditText.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Введите пароль");
            passwordEditText.requestFocus();
            return;
        }

        if (password.length() < 6) {
            passwordEditText.setError("Пароль должен содержать минимум 6 символов");
            passwordEditText.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError("Пароли не совпадают");
            confirmPasswordEditText.requestFocus();
            return;
        }

        if (!termsCheckBox.isChecked()) {
            Toast.makeText(this, "Необходимо принять условия использования", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean success = authManager.register(username, email, password, fullName);

        if (success) {
            new AlertDialog.Builder(this)
                    .setTitle("✅ Регистрация успешна")
                    .setMessage("Добро пожаловать, " + username + "!\n\nТеперь вы можете войти в свой аккаунт.")
                    .setPositiveButton("Войти", (dialog, which) -> {
                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                        intent.putExtra("username", username);
                        intent.putExtra("password", password);
                        startActivity(intent);
                        finish();
                    })
                    .setCancelable(false)
                    .show();
        } else {
            if (authManager.getUserByUsername(username) != null) {
                Toast.makeText(this, "Имя пользователя уже занято", Toast.LENGTH_LONG).show();
            } else if (authManager.getUserByEmail(email) != null) {
                Toast.makeText(this, "Email уже зарегистрирован", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Ошибка регистрации. Попробуйте позже.", Toast.LENGTH_LONG).show();
            }
        }
    }
}