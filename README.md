# FinancialTracker

Android-приложение для учёта личных финансов (Java).

## Возможности

- Доходы, расходы, переводы между счетами
- Бюджеты и финансовые цели
- Аналитика и графики (MPAndroidChart), PDF-отчёты
- Конвертер валют по курсам ЦБ РФ
- Резервное копирование данных
- **Сканер чеков**: сумма берётся из QR-кода фискального чека (поле `s=`), название магазина и итог распознаются офлайн через Tesseract (русский + английский)

## Сборка

1. Android Studio (JDK 11+), `minSdk 24`, `targetSdk 34`
2. Открыть папку проекта → Sync Gradle → Run

Языковые модели OCR лежат в `app/src/main/assets/tessdata/` (`rus.traineddata`, `eng.traineddata` из [tessdata_fast](https://github.com/tesseract-ocr/tessdata_fast)).

## Основные зависимости

- AndroidX, Material Components
- MPAndroidChart, Gson
- ML Kit Barcode Scanning (QR-коды чеков)
- Tesseract4Android (распознавание кириллицы)
- CameraX
