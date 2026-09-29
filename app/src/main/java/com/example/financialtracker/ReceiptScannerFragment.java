package com.example.financialtracker;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
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
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.Fragment;

import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Сканер чеков.
 * Сумма: из QR-кода фискального чека (поле s=), если QR не найден — из текста ("ИТОГ").
 * Магазин: распознаётся Tesseract (русский + английский), т.к. в QR-коде названия нет,
 * а ML Kit text-recognition читает только латиницу.
 */
public class ReceiptScannerFragment extends Fragment {

    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int REQUEST_GALLERY_PERMISSION = 101;
    private static final String TAG = "ReceiptScanner";
    private static final int MAX_IMAGE_SIDE = 2000;

    private static final String[] KNOWN_SHOPS = {
            "Пятёрочка", "Магнит", "Ашан", "Перекрёсток", "Окей", "Дикси",
            "Верный", "Светофор", "Метро", "Лента", "Глобус", "Зельгрос",
            "Мираторг", "ВкусВилл", "KFC", "Макдоналдс", "McDonald's",
            "Бургер Кинг", "Вкусно и точка", "Сбер", "Сбербанк", "Тинькофф", "Альфа-Банк",
            "Ozon", "Wildberries", "Яндекс Маркет", "Додо Пицца",
            "М. Видео", "Эльдорадо", "DNS", "Ситилинк", "Fix Price", "Красное&Белое",
            "Бристоль", "Самокат", "Леруа Мерлен", "Спортмастер", "Магнит Косметик"
    };

    private ImageView receiptImageView;
    private TextView resultTextView;
    private TextView shopNameTextView;
    private TextView totalTextView;
    private ProgressBar progressBar;
    private LinearLayout resultLayout;
    private Button scanButton;
    private Button galleryButton;
    private Button scanQrButton;
    private Button addTransactionButton;
    private Spinner categorySpinner;
    private EditText commentEditText;

    private MainActivity mainActivity;
    private Bitmap currentBitmap;
    private String currentPhotoPath;

    private double detectedAmount = 0.0;
    private String detectedShop = "";
    private String detectedDate = "";
    private List<String> detectedItems = new ArrayList<>();
    private String selectedCategory = "";
    private boolean isShopKnown = false;

    /** Результат QR текущего скана (сумма/дата), дополняется данными OCR. */
    private ReceiptParser.Result qrResult;

    private final BarcodeScanner barcodeScanner = BarcodeScanning.getClient(
            new BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build());
    private final TextRecognizer textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

    // Launcher для камеры
    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != Activity.RESULT_OK) return;
                if (currentPhotoPath == null) {
                    Toast.makeText(getContext(), "Фото не сохранено", Toast.LENGTH_SHORT).show();
                    return;
                }
                Bitmap bitmap = loadBitmapFromFileWithExif(currentPhotoPath, MAX_IMAGE_SIDE);
                if (bitmap != null) {
                    onImageLoaded(bitmap);
                } else {
                    Toast.makeText(getContext(), "Не удалось загрузить фото", Toast.LENGTH_SHORT).show();
                }
            });

    // Launcher для галереи
    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        try {
                            Bitmap bitmap = loadBitmapFromUri(imageUri);
                            if (bitmap != null) {
                                onImageLoaded(bitmap);
                            }
                        } catch (IOException e) {
                            Log.e(TAG, "Gallery load error", e);
                            Toast.makeText(getContext(), "Ошибка загрузки изображения", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    private void onImageLoaded(Bitmap bitmap) {
        // Старую картинку не recycle(): её ещё может читать фоновый OCR, память освободит GC
        currentBitmap = bitmap;
        receiptImageView.setImageBitmap(bitmap);
        scanQrCode(bitmap);
    }

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
        scanQrButton = view.findViewById(R.id.scanQrButton);
        addTransactionButton = view.findViewById(R.id.addTransactionButton);
        categorySpinner = view.findViewById(R.id.categorySpinner);
        commentEditText = view.findViewById(R.id.commentEditText);

        addTransactionButton.setVisibility(View.GONE);
    }

    private void setupListeners() {
        scanButton.setOnClickListener(v -> checkCameraPermissionAndOpenCamera());
        galleryButton.setOnClickListener(v -> openGallery());
        scanQrButton.setOnClickListener(v -> checkCameraPermissionAndOpenQrScanner());
        addTransactionButton.setOnClickListener(v -> returnToTransactionDialog());
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

    private void checkCameraPermissionAndOpenQrScanner() {
        if (currentBitmap != null) {
            scanQrCode(currentBitmap);
            return;
        }
        checkCameraPermissionAndOpenCamera();
    }

    @Override
    public void onDestroyView() {
        // Сканеры закрываем в onDestroy: при возврате из back stack view создаётся заново,
        // а закрытый ML Kit-клиент больше не работает.
        currentBitmap = null;
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        barcodeScanner.close();
        textRecognizer.close();
        super.onDestroy();
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
        } else if (requestCode == REQUEST_GALLERY_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(getContext(), "Разрешение на галерею необходимо для выбора чека", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            File photoFile = createImageFile();
            currentPhotoPath = photoFile.getAbsolutePath();
            Uri photoURI = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".fileprovider", photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            // Не используем resolveActivity(): на Android 11+ без <queries> он всегда возвращает null
            cameraLauncher.launch(takePictureIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getContext(), "Приложение камеры не найдено", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Camera error", e);
            Toast.makeText(getContext(), "Ошибка открытия камеры: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir == null) storageDir = requireContext().getCacheDir();
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    /** Загружает фото с камеры с уменьшением и поворотом по EXIF (иначе текст лежит боком и не читается). */
    private Bitmap loadBitmapFromFileWithExif(String path, int maxSide) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, bounds);
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;

            int sample = 1;
            while (Math.max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2;
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sample;
            Bitmap bitmap = BitmapFactory.decodeFile(path, opts);
            if (bitmap == null) return null;

            int orientation = new ExifInterface(path).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            return ImageProcessor.scaleImage(rotateByExif(bitmap, orientation), maxSide);
        } catch (Exception e) {
            Log.e(TAG, "Load photo error", e);
            return null;
        }
    }

    private static Bitmap rotateByExif(Bitmap src, int orientation) {
        int degrees;
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90: degrees = 90; break;
            case ExifInterface.ORIENTATION_ROTATE_180: degrees = 180; break;
            case ExifInterface.ORIENTATION_ROTATE_270: degrees = 270; break;
            default: return src;
        }
        Matrix m = new Matrix();
        m.postRotate(degrees);
        Bitmap rotated = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), m, true);
        if (rotated != src) src.recycle();
        return rotated;
    }

    private Bitmap loadBitmapFromUri(Uri imageUri) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // ImageDecoder сам учитывает EXIF-поворот
            ImageDecoder.Source source = ImageDecoder.createSource(
                    requireContext().getContentResolver(), imageUri);
            Bitmap bitmap = ImageDecoder.decodeBitmap(source, (decoder, info, src) ->
                    decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE));
            return ImageProcessor.scaleImage(bitmap, MAX_IMAGE_SIDE);
        }

        Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                requireActivity().getContentResolver(), imageUri);
        int orientation = ExifInterface.ORIENTATION_NORMAL;
        try (InputStream in = requireContext().getContentResolver().openInputStream(imageUri)) {
            if (in != null) {
                orientation = new ExifInterface(in).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            }
        } catch (Exception ignored) { }
        return ImageProcessor.scaleImage(rotateByExif(bitmap, orientation), MAX_IMAGE_SIDE);
    }

    private void openGallery() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_GALLERY_PERMISSION);
                return;
            }
        }

        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }



    // ============ QR-КОД ============

    private void scanQrCode(Bitmap bitmap) {
        if (bitmap == null) return;

        resetDetectedData();
        qrResult = null;
        progressBar.setVisibility(View.VISIBLE);
        resultLayout.setVisibility(View.GONE);
        addTransactionButton.setVisibility(View.GONE);
        resultTextView.setText("Поиск QR-кода...");

        Bitmap qrBitmap = ImageProcessor.preprocessForQr(bitmap);
        if (qrBitmap == null) qrBitmap = bitmap;

        barcodeScanner.process(InputImage.fromBitmap(qrBitmap, 0))
                .addOnSuccessListener(barcodes -> {
                    if (!isViewAlive()) return;
                    for (Barcode b : barcodes) {
                        ReceiptParser.Result r = ReceiptParser.parseQr(b.getRawValue());
                        Log.d(TAG, "QR: " + b.getRawValue() + " -> " + r);
                        if (r != null && r.hasTotal()) {
                            qrResult = r;
                            break;
                        }
                    }
                    resultTextView.setText(qrResult != null
                            ? "QR-код найден. Распознаём название магазина..."
                            : "QR-код не найден. Распознаём текст чека...");
                    recognizeReceipt(bitmap);
                })
                .addOnFailureListener(e -> {
                    if (!isViewAlive()) return;
                    Log.e(TAG, "QR scan error", e);
                    recognizeReceipt(bitmap);
                });
    }

    /** t=20260929T1230 или t=20260929T123045 -> 29.09.2026 12:30 */
    private String formatQrDate(String qrDate) {
        String[] formats = {"yyyyMMdd'T'HHmmss", "yyyyMMdd'T'HHmm", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd'T'HH:mm"};
        for (String f : formats) {
            try {
                SimpleDateFormat in = new SimpleDateFormat(f, Locale.ROOT);
                in.setLenient(false);
                Date d = in.parse(qrDate);
                if (d != null) return new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(d);
            } catch (Exception ignored) { }
        }
        return qrDate;
    }

    // ============ РАСПОЗНАВАНИЕ ТЕКСТА ============

    private void recognizeReceipt(Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) {
            progressBar.setVisibility(View.GONE);
            resultTextView.setText("Ошибка: изображение не загружено");
            return;
        }
        progressBar.setVisibility(View.VISIBLE);

        // Tesseract (русский + английский) — основной способ
        RussianReceiptOcr.recognize(requireContext(), bitmap, new RussianReceiptOcr.Callback() {
            @Override
            public void onResult(String text, ReceiptParser.Result parsed) {
                if (!isViewAlive()) return;
                Log.d(TAG, "=== ТЕКСТ ЧЕКА (Tesseract) ===\n" + text);
                if (qrResult == null && !parsed.hasTotal() && !parsed.hasShop()) {
                    // Tesseract ничего не нашёл — пробуем ML Kit
                    recognizeWithMlKit(bitmap);
                } else {
                    applyResult(text, parsed);
                }
            }

            @Override
            public void onError(Exception e) {
                if (!isViewAlive()) return;
                Log.e(TAG, "Tesseract error, fallback to ML Kit", e);
                recognizeWithMlKit(bitmap);
            }
        });
    }

    /** Запасной вариант: ML Kit (латиница + цифры) — сумма обычно читается. */
    private void recognizeWithMlKit(Bitmap bitmap) {
        Bitmap ocrBitmap = ImageProcessor.preprocessForOcr(bitmap);
        if (ocrBitmap == null) ocrBitmap = bitmap;
        textRecognizer.process(InputImage.fromBitmap(ocrBitmap, 0))
                .addOnSuccessListener(visionText -> {
                    if (!isViewAlive()) return;
                    String text = visionText.getText();
                    Log.d(TAG, "=== ТЕКСТ ЧЕКА (ML Kit) ===\n" + text);
                    applyResult(text, ReceiptParser.parseText(text));
                })
                .addOnFailureListener(e -> {
                    if (!isViewAlive()) return;
                    Log.e(TAG, "ML Kit error", e);
                    applyResult("", new ReceiptParser.Result());
                });
    }

    private void applyResult(String fullText, ReceiptParser.Result fromText) {
        progressBar.setVisibility(View.GONE);
        resetDetectedData();

        ReceiptParser.Result r = ReceiptParser.merge(qrResult, fromText);
        detectedAmount = r.hasTotal() ? r.total : 0.0;
        if (r.dateTime != null) detectedDate = formatQrDate(r.dateTime);

        // Известная сеть, найденная в любом месте текста, надёжнее "первой строки"
        String known = findKnownShop(fullText);
        if (known != null) {
            detectedShop = known;
        } else if (r.hasShop()) {
            detectedShop = autoCorrectShopName(r.shopName);
        } else {
            detectedShop = "Не определен";
        }

        Log.d(TAG, "Найдена сумма: " + detectedAmount + ", магазин: " + detectedShop);
        showResult();
    }

    private String findKnownShop(String text) {
        if (text == null || text.isEmpty()) return null;
        String t = normalizeForSearch(text);
        for (String shop : KNOWN_SHOPS) {
            String s = normalizeForSearch(shop);
            if (s.length() >= 3 && t.contains(s)) return shop;
        }
        return null;
    }

    private static String normalizeForSearch(String s) {
        return s.toLowerCase(Locale.ROOT).replace('ё', 'е').replaceAll("[^\\p{L}\\d&]", "");
    }

    private boolean isViewAlive() {
        return isAdded() && getView() != null;
    }

    // ============ ОБРАБОТКА ============

    private void resetDetectedData() {
        detectedAmount = 0.0;
        detectedShop = "";
        detectedDate = "";
        detectedItems.clear();
        isShopKnown = false;
    }

    private boolean isShopInDatabase(String shopName) {
        if (shopName == null || shopName.isEmpty() || shopName.equals("Не определен")) {
            return false;
        }

        String lowerShop = shopName.toLowerCase().trim();

        for (String known : KNOWN_SHOPS) {
            String lowerKnown = known.toLowerCase().trim();
            if (lowerShop.contains(lowerKnown) || lowerKnown.contains(lowerShop)) {
                return true;
            }
            String[] shopWords = lowerShop.split(" ");
            for (String word : shopWords) {
                if (word.length() > 2 && lowerKnown.contains(word)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String autoCorrectShopName(String shop) {
        // Словарь исправлений для популярных магазинов
        String[][] corrections = {
                {"ВААЕо", "Видео"},
                {"BAAEo", "Видео"},
                {"BAАЕО", "Видео"},
                {"M. ВААЕо", "М. Видео"},
                {"M.BAAEo", "М. Видео"},
                {"М. BAAEo", "М. Видео"},
                {"M. ВААЕО", "М. Видео"},
                {"М.ВААЕо", "М. Видео"},
                {"М.ВААЕО", "М. Видео"},
                {"M.BAАЕО", "М. Видео"},
                {"M VIDEO", "М. Видео"},
                {"MVIDEO", "М. Видео"},
                {"Mvideo", "М. Видео"},
                {"Мвидео", "М. Видео"},
                {"М.ВИДЕО", "М. Видео"},
                {"M.VIDEO", "М. Видео"},
                {"Пятерочка", "Пятёрочка"},
                {"Пятерочк", "Пятёрочка"},
                {"Пятероч", "Пятёрочка"},
                {"Магнит", "Магнит"},
                {"Ашан", "Ашан"},
                {"Перекресток", "Перекрёсток"},
                {"Перекрест", "Перекрёсток"},
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
                {"Яндекс Еда", "Яндекс Еда"},
                {"Додо Пицца", "Додо Пицца"},
                {"Теремок", "Теремок"},
                {"Шоколадница", "Шоколадница"},
                {"Кофе Хауз", "Кофе Хауз"},
                {"Старбакс", "Старбакс"},
                {"DNS", "DNS"},
                {"Ситилинк", "Ситилинк"},
                {"Эльдорадо", "Эльдорадо"}
        };

        String result = shop;

        // Сначала пробуем найти полное совпадение
        for (String[] correction : corrections) {
            if (shop.equalsIgnoreCase(correction[0]) ||
                    shop.toLowerCase().contains(correction[0].toLowerCase())) {
                if (shop.toLowerCase().contains(correction[0].toLowerCase())) {
                    result = shop.replaceAll("(?i)" + Pattern.quote(correction[0]), correction[1]);
                } else {
                    result = correction[1];
                }
                break;
            }
        }

        // Специальная проверка для М. Видео
        String lowerResult = result.toLowerCase();
        if (lowerResult.contains("виде") ||
                lowerResult.contains("baae") ||
                lowerResult.contains("ваае") ||
                lowerResult.contains("vid") ||
                lowerResult.contains("video") ||
                lowerResult.contains("м.в") ||
                lowerResult.contains("m.v")) {
            // Проверяем, есть ли буква "М" или "M" в начале
            if (result.matches("(?i)^[МM].*")) {
                return "М. Видео";
            } else {
                return "Видео";
            }
        }

        // Удаляем лишние точки в конце
        result = result.replaceAll("\\.$", "");

        return result;
    }

    // ============ ОТОБРАЖЕНИЕ РЕЗУЛЬТАТА ============

    private void showResult() {
        String shopInfo = detectedShop.isEmpty() || detectedShop.equals("Не определен") ? "Не определен" : detectedShop;
        String totalInfo = String.format(Locale.getDefault(), "%.2f ₽", detectedAmount);

        StringBuilder shortResult = new StringBuilder();
        shortResult.append("✅ Чек распознан!\n");
        shortResult.append("🏪 Магазин: ").append(shopInfo);

        // Проверяем, известен ли магазин
        isShopKnown = isShopInDatabase(shopInfo);

        if (shopInfo.equals("Не определен") || !isShopKnown) {
            shortResult.append("\n⚠️ Магазин не определён в базе данных, просим прощение");
        }

        shortResult.append("\n💰 Сумма: ").append(totalInfo);

        if (detectedItems != null && !detectedItems.isEmpty()) {
            shortResult.append("\n📦 Товаров: ").append(detectedItems.size());
        }

        resultTextView.setText(shortResult.toString());
        resultTextView.setTextColor(getResources().getColor(R.color.text_primary));

        shopNameTextView.setText("🏪 " + shopInfo);
        totalTextView.setText("💰 " + totalInfo);

        // Формируем комментарий
        StringBuilder comment = new StringBuilder();
        comment.append("🧾 Чек из ").append(shopInfo).append("\n");
        if (!isShopKnown && !shopInfo.equals("Не определен")) {
            comment.append("⚠️ Магазин не определён в базе данных\n");
        }
        comment.append("💰 ").append(totalInfo).append("\n");
        if (!detectedDate.isEmpty()) {
            comment.append("📅 ").append(detectedDate).append("\n");
        }
        if (detectedItems != null && !detectedItems.isEmpty()) {
            comment.append("\n📦 Товары:\n");
            for (String item : detectedItems) {
                comment.append("  • ").append(item).append("\n");
            }
        }
        commentEditText.setText(comment.toString());

        resultLayout.setVisibility(View.VISIBLE);

        if (detectedAmount > 0) {
            addTransactionButton.setVisibility(View.VISIBLE);
            addTransactionButton.setText("✅ Использовать в транзакции");
        } else {
            addTransactionButton.setVisibility(View.GONE);
            resultTextView.setText("⚠️ Не удалось найти сумму на чеке\n" +
                    "🏪 Магазин: " + shopInfo + "\n" +
                    "💡 Попробуйте сфотографировать чек чётче");
        }
    }

    // ============ ВОЗВРАТ В ДИАЛОГ ============

    private void returnToTransactionDialog() {
        if (mainActivity != null && detectedAmount > 0) {
            getParentFragmentManager().popBackStack();

            String shopInfo = detectedShop.isEmpty() || detectedShop.equals("Не определен") ? "Не определен" : detectedShop;
            String comment = commentEditText.getText().toString();

            mainActivity.showEnhancedAddTransactionDialogWithData(
                    detectedAmount,
                    shopInfo,
                    comment
            );
        } else {
            getParentFragmentManager().popBackStack();
            Toast.makeText(getContext(), "Не удалось распознать сумму на чеке", Toast.LENGTH_SHORT).show();
        }
    }
}
