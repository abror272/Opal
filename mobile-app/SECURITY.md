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

## 1.3.4 — bo'lakli (tiled) vizual skaner

- Muammo: butun ekran 224×224'ga siqilganda feed/grid/thumbnail'lardagi kichik
  18+ rasmlar yo'qoladi; model butun kadrga past ball beradi (masalan 0.069).
- Yechim: ekran **3×4 bo'lak**ka bo'linadi (10% qoplama), har biri alohida
  baholanadi, **eng yuqori ball** olinadi; 0.98'dan yuqori bo'lsa darhol to'xtaydi.
- Pol: Past 0.90 / **O'rta 0.78** / Yuqori 0.70 (bo'lakdagi maksimal ball uchun).
- Wikipedia/Wikimedia sahifalarida vizual blok o'tkaziladi (matn/URL yo'li qoladi) —
  badiiy/ta'limiy rasmlar bo'yicha yolg'on bloklarni kamaytirish uchun.
- 15 unit test PASS. 1.3.4 APK tayyor; telefon qulflangan/uzilgani uchun o'rnatilmadi.

## 1.3.3 — sezgirlik oshirildi

- Vizual pol: Past 0.82 / **O'rta 0.65** / Yuqori 0.55 (avval 0.96/0.91/0.85).
  Maqsad — real rasm/videoni qamrash; bikini kabi chegara rasmlar baribir past ball oladi.
- **URL yo'l/query kalitlari**: `/porn/`, `/sex/`, `/nude/`, `/adult/`, `/hentai/`,
  `/onlyfans/` va h.k. — sahifa matni ko'rinmasa ham manzil satridan darhol blok.
  Ta'lim hostlari (wikipedia/wikimedia/.edu/.gov) va `sex education` kabi iboralar istisno.
- Matn skaneri: 800 node / 8000 belgi / 24 ms / 28 depth — Telegram va uzun sahifalar uchun.
- `PanohVision` logiga `lum=...` qo'shildi: qora/himoyalangan skrin (masalan, Telegram
  secret chat) bo'lsa ko'rinadi; bunday oynani hech qanday skrinshot-asosli ilova o'qiy olmaydi.
- 1.3.3 qurilmaga o'rnatildi (16:49). Bo'sh sahifadagi `/porn/video` manzili ham ~1s da bloklandi (SITE).

## 1.3.2 — kech bloklar tuzatildi

- Vizual "strike" tizimi noto'g'ri ishlardi: birinchi kadr polldan o'tsa strike
  qo'yilardi, tasdiqlovchi ikkinchi skan `VISUAL_MIN_INTERVAL_MS` tufayli
  o'tkazib yuborilardi — strike esa saqlanib qolardi. Keyingi sahifadagi
  birinchi kadr shu eski strike bilan darhol blok ochardi (1-2 daqiqadan keyin
  begona kontentda). Strike/2-kadr tizimi butunlay olib tashlandi.
- Endi **bir kadr — darhol qaror**: ball ≥ pol bo'lsa shu zahoti to'siq.
  `pageGeneration` yangi oyna/sahifada eski natijani bekor qiladi; trigger
  oldidan `foregroundPkg == pkg && gen == pageGeneration` tekshiriladi.
- Pol: Past 0.96 / **O'rta 0.91** / Yuqori 0.85; debounce 450ms, throttle 700ms.
- Tekshirildi (Redmi Note 9 Pro): Wikimedia bikini rasmi `score=0.106` —
  blok yo'q (u 18+ emas); matnli test sahifasi ~1s da blok; Google'da 10s
  kuzatuvda kech blok yo'q. Debug (pol 0.05) bilan bir kadrli trigger darhol
  ishlashi tasdiqlandi.

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
