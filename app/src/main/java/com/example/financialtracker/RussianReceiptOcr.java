package com.example.financialtracker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Распознавание русского текста чека офлайн (Tesseract, языки rus+eng).
 * ML Kit text-recognition:16.0.0 знает только латиницу, поэтому кириллица
 * (название магазина, "ИТОГ") им не читается.
 *
 * Требует файл app/src/main/assets/tessdata/rus.traineddata
 * и зависимость cz.adaptech.tesseract4android:tesseract4android в build.gradle.
 */
public final class RussianReceiptOcr {

    public interface Callback {
        void onResult(String text, ReceiptParser.Result parsed);
        void onError(Exception e);
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private RussianReceiptOcr() {}

    /** Распознаёт чек в фоне, результат приходит в главном потоке. */
    public static void recognize(Context context, Bitmap bitmap, Callback cb) {
        final Context app = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            TessBaseAPI tess = null;
            try {
                String dataPath = ensureTrainedData(app);
                tess = new TessBaseAPI();
                if (!tess.init(dataPath, "rus+eng")) {
                    throw new IllegalStateException("Tesseract init failed");
                }
                tess.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO);
                tess.setImage(prepare(bitmap));
                String text = tess.getUTF8Text();
                ReceiptParser.Result parsed = ReceiptParser.parseText(text);
                MAIN.post(() -> cb.onResult(text, parsed));
            } catch (Exception e) {
                MAIN.post(() -> cb.onError(e));
            } finally {
                if (tess != null) tess.recycle();
            }
        });
    }

    /** Копирует rus.traineddata/eng.traineddata из assets во внутреннюю память (один раз). */
    private static String ensureTrainedData(Context ctx) throws Exception {
        File base = new File(ctx.getFilesDir(), "ocr");
        File dir = new File(base, "tessdata");
        if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("mkdir " + dir);
        for (String lang : new String[]{"rus", "eng"}) {
            File out = new File(dir, lang + ".traineddata");
            if (out.exists() && out.length() > 0) continue;
            try (InputStream in = ctx.getAssets().open("tessdata/" + lang + ".traineddata");
                 OutputStream os = new FileOutputStream(out)) {
                byte[] buf = new byte[64 * 1024];
                int n;
                while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
            }
        }
        return base.getAbsolutePath();
    }

    /** Масштаб до ~1600px по ширине + ч/б с повышенным контрастом — сильно помогает OCR чеков. */
    private static Bitmap prepare(Bitmap src) {
        int w = src.getWidth(), h = src.getHeight();
        float scale = 1f;
        if (w < 1200) scale = 1600f / w;          // слишком маленькое фото (например, миниатюра камеры)
        else if (w > 2400) scale = 2000f / w;     // слишком большое — медленно
        int nw = Math.round(w * scale), nh = Math.round(h * scale);

        Bitmap out = Bitmap.createBitmap(nw, nh, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        ColorMatrix gray = new ColorMatrix();
        gray.setSaturation(0f);
        float k = 1.6f, t = -0.3f * 255f;         // контраст
        ColorMatrix contrast = new ColorMatrix(new float[]{
                k, 0, 0, 0, t,
                0, k, 0, 0, t,
                0, 0, k, 0, t,
                0, 0, 0, 1, 0});
        gray.postConcat(contrast);
        Paint p = new Paint(Paint.FILTER_BITMAP_FLAG);
        p.setColorFilter(new ColorMatrixColorFilter(gray));
        c.scale(scale, scale);
        c.drawBitmap(src, 0, 0, p);
        return out;
    }
}
