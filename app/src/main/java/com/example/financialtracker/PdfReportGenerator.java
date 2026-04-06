package com.example.financialtracker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class PdfReportGenerator {

    public interface PdfGenerationCallback {
        void onSuccess(String filePath);
        void onError(String error);
    }

    public static void generateReport(Context context, AnalyticsData data, PdfGenerationCallback callback) {
        try {
            // Создаем PDF документ
            PdfDocument document = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4
            PdfDocument.Page page = document.startPage(pageInfo);

            Canvas canvas = page.getCanvas();
            Paint paint = new Paint();

            int y = 50;

            // Заголовок
            paint.setTextSize(24);
            paint.setColor(Color.BLUE);
            paint.setFakeBoldText(true);
            canvas.drawText("Финансовый отчет", 50, y, paint);
            y += 40;

            // Дата отчета
            paint.setTextSize(14);
            paint.setColor(Color.BLACK);
            paint.setFakeBoldText(false);
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
            canvas.drawText("Создан: " + sdf.format(new Date()), 50, y, paint);
            y += 40;

            // Период
            paint.setTextSize(18);
            paint.setColor(Color.DKGRAY);
            paint.setFakeBoldText(true);
            canvas.drawText("Период: " + data.getFormattedPeriod(), 50, y, paint);
            y += 40;

            // Сводка
            paint.setTextSize(16);
            paint.setColor(Color.BLACK);
            canvas.drawText("📊 Сводка", 50, y, paint);
            y += 30;

            paint.setTextSize(14);
            paint.setColor(Color.DKGRAY);
            canvas.drawText("Доходы: " + String.format(Locale.getDefault(), "%.2f руб.", data.getTotalIncome()), 70, y, paint);
            y += 25;
            canvas.drawText("Расходы: " + String.format(Locale.getDefault(), "%.2f руб.", data.getTotalExpense()), 70, y, paint);
            y += 25;

            paint.setColor(data.getBalance() >= 0 ? Color.GREEN : Color.RED);
            canvas.drawText("Баланс: " + String.format(Locale.getDefault(), "%.2f руб.", data.getBalance()), 70, y, paint);
            y += 40;

            // Сравнение с прошлым периодом
            paint.setColor(Color.BLACK);
            paint.setTextSize(16);
            canvas.drawText("📈 Сравнение с прошлым периодом", 50, y, paint);
            y += 30;

            paint.setTextSize(14);
            paint.setColor(data.getIncomeChangePercent() >= 0 ? Color.GREEN : Color.RED);
            canvas.drawText(data.getIncomeChangeText(), 70, y, paint);
            y += 25;

            paint.setColor(data.getExpenseChangePercent() <= 0 ? Color.GREEN : Color.RED);
            canvas.drawText(data.getExpenseChangeText(), 70, y, paint);
            y += 40;

            // Категории расходов
            paint.setColor(Color.BLACK);
            paint.setTextSize(16);
            canvas.drawText("💰 Топ категории расходов", 50, y, paint);
            y += 30;

            paint.setTextSize(14);
            int i = 1;
            for (Map.Entry<String, Double> entry : data.getExpenseByCategory().entrySet()) {
                if (i > 5) break; // Топ-5
                String category = entry.getKey();
                if (category.contains(" ")) {
                    category = category.substring(category.indexOf(" ") + 1);
                }
                double percent = (entry.getValue() / data.getTotalExpense()) * 100;
                canvas.drawText(i + ". " + category + ": " +
                        String.format(Locale.getDefault(), "%.2f руб. (%.1f%%)",
                                entry.getValue(), percent), 70, y, paint);
                y += 25;
                i++;
            }
            y += 20;

            // Прогноз
            if (data.getPredictionDays() > 0) {
                paint.setColor(Color.BLACK);
                paint.setTextSize(16);
                canvas.drawText("🔮 Прогноз на " + data.getPredictionDays() + " дней", 50, y, paint);
                y += 30;

                paint.setTextSize(14);
                canvas.drawText(data.getPredictionText(), 70, y, paint);
                y += 25;

                paint.setColor(data.getPredictedBalance() >= data.getBalance() ? Color.GREEN : Color.RED);
                canvas.drawText("Прогнозируемый баланс: " +
                        String.format(Locale.getDefault(), "%.2f руб.", data.getPredictedBalance()), 70, y, paint);
                y += 40;
            }

            // Привычки и инсайты
            if (!data.getHabits().isEmpty()) {
                paint.setColor(Color.BLACK);
                paint.setTextSize(16);
                canvas.drawText("✨ Инсайты и привычки", 50, y, paint);
                y += 30;

                paint.setTextSize(13);
                for (AnalyticsData.HabitInsight habit : data.getHabits()) {
                    if (y > 750) break; // Не выходим за пределы страницы

                    paint.setColor(habit.getColor());
                    paint.setFakeBoldText(true);
                    canvas.drawText("• " + habit.getTitle(), 70, y, paint);
                    y += 20;

                    paint.setColor(Color.DKGRAY);
                    paint.setFakeBoldText(false);

                    // Разбиваем длинное описание
                    String desc = habit.getDescription();
                    if (desc.length() > 60) {
                        String part1 = desc.substring(0, 60);
                        String part2 = desc.substring(60);
                        canvas.drawText(part1, 85, y, paint);
                        y += 20;
                        canvas.drawText(part2, 85, y, paint);
                    } else {
                        canvas.drawText(desc, 85, y, paint);
                    }
                    y += 20;

                    paint.setColor(Color.parseColor("#2196F3"));
                    canvas.drawText("💡 " + habit.getRecommendation(), 85, y, paint);
                    y += 30;
                }
            }

            document.finishPage(page);

            // Сохраняем файл
            String fileName = "Financial_Report_" + sdf.format(new Date()) + ".pdf";
            fileName = fileName.replace(":", "-").replace(" ", "_");

            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(downloadsDir, fileName);

            document.writeTo(new FileOutputStream(file));
            document.close();

            callback.onSuccess(file.getAbsolutePath());

        } catch (IOException e) {
            e.printStackTrace();
            callback.onError(e.getMessage());
        }
    }
}