package com.example.financialtracker;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReceiptScannerFragment extends Fragment {

    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int REQUEST_STORAGE_PERMISSION = 101;

    private ImageView receiptImageView;
    private TextView resultTextView;
    private TextView shopNameTextView;
    private TextView totalTextView;
    private ProgressBar progressBar;
    private LinearLayout resultLayout;
    private Button scanButton;
    private Button galleryButton;
    private Button addTransactionButton;
    private Spinner categorySpinner;
    private EditText commentEditText;

    private MainActivity mainActivity;
    private Bitmap currentBitmap;

    private double detectedAmount = 0.0;
    private String detectedShop = "";
    private String selectedCategory = "";

    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                                    getActivity().getContentResolver(), imageUri);
                            if (bitmap != null) {
                                currentBitmap = bitmap;
                                receiptImageView.setImageBitmap(bitmap);
                                recognizeReceipt(bitmap);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(getContext(), "Ошибка загрузки изображения", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_receipt_scanner, container, false);

        mainActivity = (MainActivity) getActivity();

        initViews(view);
        setupListeners();
        setupCategorySpinner();

        return view;
    }

    private void initViews(View view) {
        receiptImageView = view.findViewById(R.id.receiptImageView);
        resultTextView = view.findViewById(R.id.resultTextView);
        shopNameTextView = view.findViewById(R.id.shopNameTextView);
        totalTextView = view.findViewById(R.id.totalTextView);
        progressBar = view.findViewById(R.id.progressBar);
        resultLayout = view.findViewById(R.id.resultLayout);
        scanButton = view.findViewById(R.id.scanButton);
        galleryButton = view.findViewById(R.id.galleryButton);
        addTransactionButton = view.findViewById(R.id.addTransactionButton);
        categorySpinner = view.findViewById(R.id.categorySpinner);
        commentEditText = view.findViewById(R.id.commentEditText);
    }

    private void setupListeners() {
        scanButton.setOnClickListener(v -> checkCameraPermissionAndOpenCamera());
        galleryButton.setOnClickListener(v -> openGallery());
        addTransactionButton.setOnClickListener(v -> addTransactionFromReceipt());
    }

    private void setupCategorySpinner() {
        if (mainActivity == null) return;

        List<String> categories = mainActivity.getCategoryManager().getExpenseCategories();
        if (categories.isEmpty()) {
            categories.add("💸 Прочие расходы");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);

        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCategory = (String) parent.getItemAtPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedCategory = categories.get(0);
            }
        });
    }

    private void checkCameraPermissionAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            openCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(getContext(), "Разрешение на камеру необходимо для сканирования", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(getContext(), "Разрешение на чтение хранилища необходимо", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, 200);
        } else {
            Toast.makeText(getContext(), "Камера не доступна", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 200 && resultCode == getActivity().RESULT_OK && data != null) {
            Bundle extras = data.getExtras();
            if (extras != null) {
                Bitmap bitmap = (Bitmap) extras.get("data");
                if (bitmap != null) {
                    currentBitmap = bitmap;
                    receiptImageView.setImageBitmap(bitmap);
                    recognizeReceipt(bitmap);
                }
            }
        }
    }

    private void openGallery() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
                return;
            }
        }

        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void recognizeReceipt(Bitmap bitmap) {
        progressBar.setVisibility(View.VISIBLE);
        resultLayout.setVisibility(View.GONE);
        resultTextView.setText("Распознавание чека...");

        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    progressBar.setVisibility(View.GONE);
                    processRecognizedText(visionText);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    resultTextView.setText("Ошибка распознавания: " + e.getMessage());
                    Toast.makeText(getContext(), "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void processRecognizedText(Text visionText) {
        detectedAmount = 0.0;
        detectedShop = "";

        String fullText = visionText.getText();

        detectedAmount = extractTotalAmount(fullText);
        detectedShop = extractShopName(fullText);

        showResult();
    }

    private String extractShopName(String text) {
        String[] lines = text.split("\n");

        // Проходим по первым 10 строкам чека
        for (int i = 0; i < Math.min(lines.length, 10); i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            // Пропускаем строки с датой
            if (line.matches(".*\\d{2}[./]\\d{2}[./]\\d{4}.*")) continue;
            if (line.matches(".*\\d{2}[./]\\d{2}[./]\\d{2}.*")) continue;

            // Пропускаем строки с большим количеством цифр
            if (line.matches(".*\\d{4,}.*") && line.length() < 20) continue;

            // Пропускаем служебные строки
            String lower = line.toLowerCase();
            if (lower.contains("кассовый") || lower.contains("чек") ||
                    lower.contains("ндс") || lower.contains("итог") ||
                    lower.contains("всего") || lower.contains("сумма") ||
                    lower.contains("кассир") || lower.contains("смена") ||
                    lower.contains("фн") || lower.contains("фд") ||
                    lower.contains("адрес") || lower.contains("телефон") ||
                    lower.contains("инн") || lower.contains("кпп") ||
                    lower.contains("скидка") || lower.contains("бонус") ||
                    lower.contains("спасибо") || lower.contains("карта") ||
                    lower.contains("клиент") || lower.contains("терминал") ||
                    lower.contains("оплата") || lower.contains("налог") ||
                    lower.contains("печать") || lower.contains("электронный")) {
                continue;
            }

            // Если строка содержит буквы и длиннее 2 символов
            if (line.matches(".*[a-zA-Zа-яА-Я].*") && line.length() > 2) {
                String shop = line.replaceAll("[^a-zA-Zа-яА-Я0-9\\s.,&]", "").trim();
                shop = shop.replaceAll("\\s{2,}", " ").trim();

                if (shop.length() > 50) {
                    shop = shop.substring(0, 47) + "...";
                }

                if (shop.length() > 1) {
                    // АВТОИСПРАВЛЕНИЕ популярных названий магазинов
                    shop = autoCorrectShopName(shop);
                    return shop;
                }
            }
        }

        // Если не нашли — ищем по ключевым словам
        String[] shopKeywords = {"магазин", "супермаркет", "гипермаркет", "market", "store", "shop", "ооо", "ип"};
        for (String keyword : shopKeywords) {
            Pattern p = Pattern.compile("(.{0,40}" + keyword + ".{0,40})", Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(text);
            if (m.find()) {
                String shop = m.group(1).trim();
                shop = shop.replaceAll("[^a-zA-Zа-яА-Я0-9\\s.,&]", "").trim();
                shop = shop.replaceAll("\\s{2,}", " ").trim();
                if (shop.length() > 2 && shop.length() < 60) {
                    shop = autoCorrectShopName(shop);
                    return shop;
                }
            }
        }

        return "Не определен";
    }

    /**
     * Автоисправление популярных названий магазинов
     * Если распознавание перепутало буквы, исправляем по словарю
     */
    private String autoCorrectShopName(String shop) {
        // Словарь исправлений
        String[][] corrections = {
                // { "что распозналось", "что должно быть" }
                {"ВААЕо", "Видео"},
                {"BAAEo", "Видео"},
                {"BAАЕО", "Видео"},
                {"M. ВААЕо", "М. Видео"},
                {"M.BAAEo", "М. Видео"},
                {"М. BAAEo", "М. Видео"},
                {"M. ВААЕО", "М. Видео"},
                {"Пятерочка", "Пятёрочка"},
                {"Пятерочк", "Пятёрочка"},
                {"Магнит", "Магнит"},
                {"Ашан", "Ашан"},
                {"Перекресток", "Перекрёсток"},
                {"Окей", "Окей"},
                {"Дикси", "Дикси"},
                {"Верный", "Верный"},
                {"Светофор", "Светофор"},
                {"Метро", "Метро"},
                {"Лента", "Лента"},
                {"Глобус", "Глобус"},
                {"Зельгрос", "Зельгрос"},
                {"Мираторг", "Мираторг"},
                {"ВкусВилл", "ВкусВилл"},
                {"Вкусвилл", "ВкусВилл"},
                {"KFC", "KFC"},
                {"Макдоналдс", "Макдоналдс"},
                {"McDonald's", "McDonald's"},
                {"Бургер Кинг", "Бургер Кинг"},
                {"Сбер", "Сбер"},
                {"Сбербанк", "Сбербанк"},
                {"Тинькофф", "Тинькофф"},
                {"Альфа-Банк", "Альфа-Банк"},
                {"Ozon", "Ozon"},
                {"Wildberries", "Wildberries"},
                {"WB", "Wildberries"},
                {"Яндекс Маркет", "Яндекс Маркет"},
                {"Delivery Club", "Delivery Club"},
                {"Яндекс Еда", "Яндекс Еда"}
        };

        String result = shop;
        for (String[] correction : corrections) {
            if (shop.equalsIgnoreCase(correction[0]) ||
                    shop.toLowerCase().contains(correction[0].toLowerCase())) {
                // Заменяем только часть строки, если нужно
                if (shop.toLowerCase().contains(correction[0].toLowerCase())) {
                    result = shop.replaceAll("(?i)" + Pattern.quote(correction[0]), correction[1]);
                } else {
                    result = correction[1];
                }
                break;
            }
        }

        // Если строка начинается с "М. " - исправляем
        if (result.matches("(?i)M\\.\\s*[А-ЯA-Z].*")) {
            // Оставляем как есть, это похоже на "М. Видео" или "М. Тинькофф"
        }

        // Удаляем лишние точки в конце
        result = result.replaceAll("\\.$", "");

        return result;
    }

    private void returnToTransactionDialog() {
        if (mainActivity != null && detectedAmount > 0) {
            // Возвращаемся назад с данными
            getParentFragmentManager().popBackStack();

            // Показываем диалог с предзаполненными данными
            mainActivity.showEnhancedAddTransactionDialogWithData(
                    detectedAmount,
                    detectedShop,
                    "🧾 Чек из " + detectedShop + "\n💰 " + String.format(Locale.getDefault(), "%.2f ₽", detectedAmount)
            );
        } else {
            // Если сумма не найдена, просто возвращаемся
            getParentFragmentManager().popBackStack();
            Toast.makeText(getContext(), "Не удалось распознать сумму на чеке", Toast.LENGTH_SHORT).show();
        }
    }

    private double extractTotalAmount(String text) {
        // 1. Ищем по ключевым словам
        String[] keywords = {"ИТОГ", "ИТОГО", "ВСЕГО", "TOTAL", "СУММА", "К ОПЛАТЕ", "Сумма", "Итого"};

        for (String keyword : keywords) {
            Pattern p = Pattern.compile(keyword + "[^\\d]*([\\d,]+[.,]?\\d*)", Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(text);
            if (m.find()) {
                try {
                    String amountStr = m.group(1).replace(",", ".");
                    double amount = Double.parseDouble(amountStr);
                    if (amount > 0 && amount < 1000000) {
                        return amount;
                    }
                } catch (NumberFormatException e) {
                    // Игнорируем
                }
            }

            Pattern p2 = Pattern.compile("([\\d,]+[.,]?\\d*)\\s*" + keyword, Pattern.CASE_INSENSITIVE);
            Matcher m2 = p2.matcher(text);
            if (m2.find()) {
                try {
                    String amountStr = m2.group(1).replace(",", ".");
                    double amount = Double.parseDouble(amountStr);
                    if (amount > 0 && amount < 1000000) {
                        return amount;
                    }
                } catch (NumberFormatException e) {
                    // Игнорируем
                }
            }
        }

        // 2. Ищем числа с ₽ или руб
        Pattern rubPattern = Pattern.compile("(\\d+[.,]?\\d*)\\s*[₽руб]");
        Matcher rubMatcher = rubPattern.matcher(text);
        List<Double> rubAmounts = new ArrayList<>();

        while (rubMatcher.find()) {
            try {
                String amountStr = rubMatcher.group(1).replace(",", ".");
                double amount = Double.parseDouble(amountStr);
                if (amount > 0 && amount < 1000000) {
                    rubAmounts.add(amount);
                }
            } catch (NumberFormatException e) {
                // Игнорируем
            }
        }

        if (!rubAmounts.isEmpty()) {
            double max = 0;
            for (double d : rubAmounts) {
                if (d > max) max = d;
            }
            return max;
        }

        // 3. Ищем все числа с двумя знаками после запятой
        Pattern pricePattern = Pattern.compile("(\\d+[.,]\\d{2})");
        Matcher priceMatcher = pricePattern.matcher(text);
        List<Double> prices = new ArrayList<>();

        while (priceMatcher.find()) {
            try {
                String amountStr = priceMatcher.group(1).replace(",", ".");
                double amount = Double.parseDouble(amountStr);
                if (amount > 0 && amount < 1000000) {
                    prices.add(amount);
                }
            } catch (NumberFormatException e) {
                // Игнорируем
            }
        }

        if (!prices.isEmpty()) {
            double max = 0;
            for (double d : prices) {
                if (d > max) max = d;
            }
            return max;
        }

        return 0.0;
    }

    private void showResult() {
        String shopInfo = detectedShop.isEmpty() ? "Не определен" : detectedShop;
        String totalInfo = String.format(Locale.getDefault(), "%.2f ₽", detectedAmount);

        String shortResult = "✅ Чек распознан!\n" +
                "🏪 Магазин: " + shopInfo + "\n" +
                "💰 Сумма: " + totalInfo;

        resultTextView.setText(shortResult);
        resultTextView.setTextColor(getResources().getColor(R.color.text_primary));

        shopNameTextView.setText("🏪 " + shopInfo);
        totalTextView.setText("💰 " + totalInfo);

        String comment = "🧾 Чек из " + shopInfo + "\n💰 " + totalInfo;
        commentEditText.setText(comment);

        resultLayout.setVisibility(View.VISIBLE);

        if (detectedAmount > 0) {
            addTransactionButton.setVisibility(View.VISIBLE);
            // Меняем текст кнопки
            addTransactionButton.setText("✅ Использовать в транзакции");
            // Меняем действие
            addTransactionButton.setOnClickListener(v -> returnToTransactionDialog());
        } else {
            addTransactionButton.setVisibility(View.GONE);
            resultTextView.setText("⚠️ Не удалось найти сумму на чеке\n" +
                    "🏪 Магазин: " + shopInfo + "\n" +
                    "💡 Попробуйте сфотографировать чек чётче");
        }
    }

    private void addTransactionFromReceipt() {
        if (detectedAmount <= 0) {
            Toast.makeText(getContext(), "Не удалось определить сумму. Попробуйте ещё раз.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedCategory == null || selectedCategory.isEmpty()) {
            Toast.makeText(getContext(), "Выберите категорию", Toast.LENGTH_SHORT).show();
            return;
        }

        String comment = commentEditText.getText().toString().trim();
        if (comment.isEmpty()) {
            comment = "🧾 Чек из " + (detectedShop.isEmpty() ? "магазина" : detectedShop) +
                    "\n💰 " + String.format(Locale.getDefault(), "%.2f ₽", detectedAmount);
        }

        String description = "Покупка в " + (detectedShop.isEmpty() ? "магазине" : detectedShop);

        Transaction transaction = new Transaction(
                description,
                detectedAmount,
                false,
                new Date(),
                selectedCategory,
                comment,
                "Без счета",
                "Карта"
        );

        if (mainActivity != null) {
            mainActivity.addTransaction(transaction);

            Toast.makeText(getContext(),
                    "✅ Расход добавлен: " + String.format(Locale.getDefault(), "%.2f ₽", detectedAmount) +
                            "\n📂 Категория: " + selectedCategory,
                    Toast.LENGTH_LONG).show();

            getParentFragmentManager().popBackStack();
        }
    }
}