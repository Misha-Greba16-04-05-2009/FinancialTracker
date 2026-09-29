package com.example.financialtracker;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.util.Log;

import java.io.IOException;

/**
 * Утилита для подготовки изображений чеков перед OCR/QR-сканированием.
 * Использует ColorMatrix и буферы пикселей вместо медленных getPixel/setPixel.
 */
public final class ImageProcessor {

    private static final String TAG = "ImageProcessor";

    private static final int OCR_MAX_SIZE = 1500;
    private static final int QR_MAX_SIZE = 1200;

    private static final float CONTRAST = 1.3f;
    private static final float BRIGHTNESS = 15f;

    private ImageProcessor() {
    }

    /**
     * Подготовка изображения для распознавания текста (OCR).
     */
    public static Bitmap preprocessForOcr(Bitmap original) {
        if (original == null) {
            return null;
        }

        try {
            Bitmap scaled = scaleImage(original, OCR_MAX_SIZE);
            Bitmap contrast = applyContrastAndBrightness(scaled, CONTRAST, BRIGHTNESS);
            recycleIfDifferent(original, scaled);
            Bitmap gray = toGrayscale(contrast);
            recycleIfDifferent(scaled, contrast);
            return gray;
        } catch (Exception e) {
            Log.e(TAG, "Error preprocessing image for OCR", e);
            return original;
        }
    }

    /**
     * Масштабирование для QR-сканирования (без фильтров — QR лучше читается с оригинала).
     */
    public static Bitmap preprocessForQr(Bitmap original) {
        if (original == null) {
            return null;
        }
        return scaleImage(original, QR_MAX_SIZE);
    }

    /**
     * Загружает изображение из файла с уменьшением размера и коррекцией EXIF.
     */
    public static Bitmap loadBitmapFromFile(String path, int maxSize) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, bounds);

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = calculateInSampleSize(bounds, maxSize, maxSize);
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

            Bitmap bitmap = BitmapFactory.decodeFile(path, options);
            if (bitmap == null) {
                return null;
            }

            return rotateBitmapIfNeeded(bitmap, path);
        } catch (Exception e) {
            Log.e(TAG, "Error loading bitmap from file", e);
            return null;
        }
    }

    public static Bitmap rotateBitmapIfNeeded(Bitmap bitmap, String filePath) {
        if (bitmap == null || filePath == null) {
            return bitmap;
        }

        try {
            ExifInterface exif = new ExifInterface(filePath);
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);

            int rotationAngle = exifOrientationToDegrees(orientation);
            if (rotationAngle == 0) {
                return bitmap;
            }

            Matrix matrix = new Matrix();
            matrix.postRotate(rotationAngle);
            Bitmap rotated = Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            if (rotated != bitmap) {
                bitmap.recycle();
            }
            return rotated;
        } catch (IOException e) {
            Log.e(TAG, "Error rotating bitmap", e);
            return bitmap;
        }
    }

    public static Bitmap scaleImage(Bitmap bitmap, int maxSize) {
        if (bitmap == null) {
            return null;
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width <= maxSize && height <= maxSize) {
            return bitmap;
        }

        float ratio = Math.min((float) maxSize / width, (float) maxSize / height);
        int newWidth = Math.max(1, Math.round(width * ratio));
        int newHeight = Math.max(1, Math.round(height * ratio));

        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
        return scaled;
    }

    private static Bitmap applyContrastAndBrightness(Bitmap source, float contrast, float brightness) {
        Bitmap result = Bitmap.createBitmap(source.getWidth(), source.getHeight(), Bitmap.Config.ARGB_8888);

        ColorMatrix contrastMatrix = new ColorMatrix(new float[]{
                contrast, 0, 0, 0, brightness,
                0, contrast, 0, 0, brightness,
                0, 0, contrast, 0, brightness,
                0, 0, 0, 1, 0
        });

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        paint.setColorFilter(new ColorMatrixColorFilter(contrastMatrix));

        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(source, 0, 0, paint);
        return result;
    }

    private static Bitmap toGrayscale(Bitmap source) {
        Bitmap result = Bitmap.createBitmap(source.getWidth(), source.getHeight(), Bitmap.Config.ARGB_8888);

        ColorMatrix grayscaleMatrix = new ColorMatrix();
        grayscaleMatrix.setSaturation(0f);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        paint.setColorFilter(new ColorMatrixColorFilter(grayscaleMatrix));

        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(source, 0, 0, paint);
        return result;
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return Math.max(1, inSampleSize);
    }

    private static int exifOrientationToDegrees(int orientation) {
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return 90;
            case ExifInterface.ORIENTATION_ROTATE_180:
                return 180;
            case ExifInterface.ORIENTATION_ROTATE_270:
                return 270;
            default:
                return 0;
        }
    }

    private static void recycleIfDifferent(Bitmap original, Bitmap processed) {
        if (original != null && processed != null && original != processed && !original.isRecycled()) {
            original.recycle();
        }
    }
}
