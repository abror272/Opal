# Panoh — xavfsizlik va "yuqori risk" bayrog'i

Bu hujjat APK skanerlaydigan vositalar (masalan "APK qo'riqchi", Play Protect,
VirusTotal) nega ba'zi ilovalarni "yuqori risk" deb belgilashini va biz bunga qarshi
nima qilganimizni tushuntiradi.

## Nega bloklovchi ilova "risk" ko'rinadi?

Ekran vaqtini nazorat qiluvchi ilova **tabiatan** quyidagi imkoniyatlarga muhtoj:

| Imkoniyat | Nima uchun | Skanerda ko'rinishi |
|---|---|---|
| AccessibilityService | bloklangan ilovani aniqlash + nomaqbul matnni o'qish | "input monitoring" — eng yuqori signal |
| SYSTEM_ALERT_WINDOW | accessibility o'chsa ham blok ekranini chizish | "overlay" — o'rtacha-yuqori |
| PACKAGE_USAGE_STATS | ekran vaqti statistikasi | "usage access" — o'rtacha |
| RECEIVE_BOOT_COMPLETED | qayta ishga tushgandan keyin himoya | past |

Bu kombinatsiya zararli dasturlarda ham uchraydi, shuning uchun **heuristik skanerlar
uni "riskware" deb belgilashi mumkin**. Bu ilovada ma'lumot yig'ish, yashirin
o'rnatish yoki internetga ma'lumot yuborish YO'Q.

## Nima o'zgartirildi (riskni pasaytirish)

| Avval | Endi | Nega |
|---|---|---|
| `QUERY_ALL_PACKAGES` | **olib tashlandi** — faqat `<queries>` launcher intenti | barcha ilovalarni ko'rish ruxsati eng katta bayroqlardan biri |
| `WRITE_SECURE_SETTINGS` | **olib tashlandi** (o'z-o'zini tiklash o'chdi) | adb orqali beriladigan maxsus ruxsat; juda shubhali |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | **olib tashlandi** — oddiy sozlamalar sahifasi ochiladi | "batareya cheklovini chetlab o'tish" bayrog'i |
| `usesCleartextTraffic="true"` | `false` | shifrlanmagan HTTP — skanerda salbiy |
| `android:debuggable=true` (debug APK) | **release**: `false` | debug APK — eng katta "test/xavfli" belgisi |
| Debug imzo | **release keystore** (`CN=Panoh`) | barqaror, maxsus imzo |
| `allowBackup` default | `false` | zaxira orqali ma'lumot chiqishi yo'q |
| BootReceiver `exported=true` | `false` | tashqi ilova soxta broadcast yubora olmaydi |
| R8 yo'q | `minify + shrinkResources` (14 MB, TFLite model bilan) | ishlatilmagan kod/ruxsat izlari qolmaydi |

Qolgan ruxsatlar (7 ta): INTERNET, POST_NOTIFICATIONS, FOREGROUND_SERVICE,
FOREGROUND_SERVICE_SPECIAL_USE, RECEIVE_BOOT_COMPLETED, SYSTEM_ALERT_WINDOW,
PACKAGE_USAGE_STATS.

## Tekshirish

```bash
# Imzo
$ANDROID_HOME/build-tools/35.0.0/apksigner verify --print-certs \
  composeApp/build/outputs/apk/release/composeApp-release.apk

# Ruxsatlar
$ANDROID_HOME/build-tools/35.0.0/aapt2 dump permissions \
  composeApp/build/outputs/apk/release/composeApp-release.apk

# debuggable yo'qligi
$ANDROID_HOME/build-tools/35.0.0/aapt2 dump xmltree \
  composeApp/build/outputs/apk/release/composeApp-release.apk --file AndroidManifest.xml | grep -i debuggable
```

## Halol cheklov

- Accessibility + overlay ishlatadigan **har qanday** ilova ayrim skanerlarda
  "potensial xavfli" bo'lib qoladi. Bu kod bilan emas, faqat funksiyadan voz kechish
  bilan hal qilinadi.
- Play Protect **sideload** qilingan APK'lar uchun ogohlantirish ko'rsatishi mumkin
  (Play'dan tashqari imzo). Bu normal holat.
- Agar ilova Play Store'ga chiqarilsa, AccessibilityService uchun alohida
  deklaratsiya va Play'ning ruxsat formasi talab qilinadi.

## Maxfiylik

- Ekran matni faqat qurilma xotirasida tahlil qilinadi.
- Skrinshot olinmaydi, matn saqlanmaydi, internetga yuborilmaydi.
- Saqlanadigan yagona ma'lumot: bloklar soni, qoidalar, vaqtinchalik istisnolar.
- `allowBackup=false` — zaxira nusxasida ham chiqmaydi.

## 1.3.1 — sezgirlik saqlanishi tuzatildi

- **Xato:** `setAdultGuardSensitivity` o'zini-o'zi chaqirib (recursion), pref
  saqlanmasdi; har 1.5 s'da ishlaydigan `refreshBlockingService` esa UI'ni
  yana "O'rta"ga qaytarardi. Endi top-level funksiya alias bilan chaqiriladi —
  tanlov darhol saqlanadi va qayta ochilganda ham qoladi.
- Tekshirildi: "Past" tanlandi → 3 s kutildi → ilova to'liq yopildi → qayta
  ochildi: **Past saqlangan**. "Bugun: 1 ta blok" ham yozildi.
- MIUI'da **Fonda avtomatik ishga tushirish** Panoh uchun yoqildi; accessibility
  xizmati barqarorroq turadi. Xizmat baribir o'chsa — Sozlamalar → Qulaylik →
  Panoh himoyasi'ni qayta yoqing.

## 1.3.0 — Panoh nomi

- Brend **Panoh**'ga o'tkazildi: ilova ID `com.panoh.app`, imzo CN=Panoh,
  versiya 1.3.0 (code 4). Ruxsatlar va funksiyalar o'zgarmadi.
- Portret rejimda tekshirildi (1080×2400): YouTube'da blok yo'q, himoya oynasi
  portretga to'liq mos tushadi, "Sinovni yopish" ishlaydi.

## 1.2.0 — vizual tekshirish qo'shildi

- Accessibility konfiguratsiyasiga `android:canTakeScreenshot="true"` qo'shildi
  (faqat Android 11+; ekran rasmi qurilmada tahlil qilinadi).
- `assets/nsfw.tflite` — Yahoo **open_nsfw** (BSD-2-Clause, 5.7 MB) + TensorFlow Lite
  (Apache-2.0). Model faqat lokal, internetga chiqmaydi. Batafsil: `THIRD_PARTY.md`.
- APK hajmi 14 MB (arm64-v8a + armeabi-v7a). Ruxsatlar soni o'zgarmadi (7 ta).
- `canTakeScreenshot` skanerlarda qo'shimcha "screen capture" signali bo'lishi mumkin —
  bu funksiyaning ajralmas qismi (vizual aniqlash uchun).
