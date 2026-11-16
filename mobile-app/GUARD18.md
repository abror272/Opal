# 18+ himoyasi (Adult Content Guard)

Lumirra'ning yangi yuqori o'rve funksiyasi: **ekranda 18+ kontent paydo bo'lsa, ekran darhol
to'siladi** va foydalanuvchini o'zini tiyishga undovchi xabarlar ko'rsatiladi.

> Faqat Android. iOS'da boshqa ilova ekranini o'qish uchun API yo'q
> (Apple Screen Time / Family Controls faqat Apple tizimi orqali ishlaydi).

## Qanday ishlaydi

```
AccessibilityService (TYPE_WINDOW_CONTENT_CHANGED)
        │  debounce 320 ms + throttle 650 ms + node/vaqt byudjeti
        ▼
ScreenTextScanner — ekran daraxtidan matn (420 node / 6000 belgi / 14 ms)
        ▼
AdultContentGuard — matn + havola + ilova nomi tahlili (qurilmada, offline)
        ▼
GuardOverlay — to'liq ekranli to'siq + "nafas olish" hisoblagichi
```

### 1. Aniqlash manbalari

| Manba | Misol | Ball |
|---|---|---|
| **Havola/domén** | `pornhub.com`, `*.xxx` | 200 (darhol blok) |
| **Kuchli so'z** | `porn`, `порно`, `hentai`, `onlyfans` | 100 |
| **O'rtacha so'z** | `nude`, `эротика`, `seks`, `18+ video` | 65 |
| **Zaif so'z** | `adult`, `18+`, `kattalar` | 35–60 |
| **Ilova nomi** | "Porn Hub" | 65–100 |

- **3 til**: o'zbek (lotin), rus (kirill), ingliz.
- **Obfuskatsiyaga chidamli**: `p0rn`, `p.o.r.n`, `p o r n`, `poooorn`,
  kirill harflari bilan yozilgan lotin so'zlar (`pоrn`).
- **Xavfsiz kontekst**: `sex education`, `same-sex marriage`, `jinsiy tarbiya`, `tibbiyot`,
  `maqola`, `гинекология`, `research` kabi iboralar tahlildan oldin neytrallanadi —
  ta'limiy va yangilik sahifalari bloklanmaydi.
- **Takrorlanish**: bitta so'z bir necha marta uchrasa ball oshadi (mavzu aniq).

### 2. Sezgirlik

| Daraja | Chegara | Amalda |
|---|---|---|
| Past | 100 | faqat domén yoki kuchli so'z (`porn`, `hentai`) |
| **O'rta** (default) | 60 | bitta o'rtacha signal: `nude`, `seks`, `18+` |
| Yuqori | 35 | zaif signal ham: `adult`, `kattalar` |

### 3. To'siq ekrani

- Ekran **butunlay** qoplanadi (opaque) — kontent ko'rinmaydi.
- **5 soniya** "Nafas oling" hisoblagichi — impuls bilan bir bosishda chiqib ketish yo'q.
- Matnlar **o'zini tiyishga** undaydi, ayblamaydi:
  "Bir nafas oling. Impuls 30 soniyada o'tadi — qaror qoladi." va h.k. (10 ta, tasodifiy).
- Faqat 2 yo'l:
  - **"Chiqish va orqaga qaytish"** → bosh ekranga qaytadi.
  - **"Bu xato — davom etish"** → 15 daqiqa istisno (noto'g'ri aniqlashlar uchun).
- "Orqaga" tugmasi to'siqni yopmaydi.
- Ilovaga qaytsangiz va kontent hali ham bo'lsa — yana bloklanadi.

### 4. Qayerda sozlanadi

**Ilovalar** tab → **18+ himoyasi** kartasi:

- yoqish/o'chirish (default: **yoqilgan**),
- sezgirlik (Past / O'rta / Yuqori),
- bugungi bloklar soni,
- **"Sinash"** — haqiqiy overlay'ni ko'rsatadi (soxta signal bilan),
- "Matn qurilmada tekshiriladi" izohi.

## Maxfiylik

- Ekran matni **faqat qurilma xotirasida** tahlil qilinadi.
- Hech qaerda saqlanmaydi, internetga yuborilmaydi, skrinshot olinmaydi.
- Saqlanadigan yagona narsa: bloklar soni va vaqtinchalik istisnolar.

## Cheklovlar (halol)

1. **Rasm/video ichidagi yalang'ochlik aniqlanmaydi** — bu uchun qurilmada ishlaydigan
   NSFW modeli (TFLite) kerak. Hozir matn, havola va ilova nomi tekshiriladi.
   Shu sababli brauzer manzil satri va sahifa sarlavhasi asosiy signal bo'lib xizmat qiladi.
2. Ba'zi ilovalar matnni accessibility'ga bermaydi (masalan, ba'zi o'yinlar yoki
   "secure" oynalar) — ularda aniqlash ishlamaydi.
3. 100% emas: noto'g'ri aniqlash bo'lishi mumkin. Shuning uchun "Bu xato" tugmasi va
   sezgirlik sozlamasi bor.
4. **Xizmat yoqilgan bo'lishi shart**: Sozlamalar → Maxsus imkoniyatlar → Lumirra himoyasi.
   MIUI/HyperOS'da qo'shimcha: avtostart + batareya cheklovi o'chirilgan bo'lsin.

## Sinash

1. `Ilovalar` → `18+ himoyasi` → **yoqilgan** bo'lsin.
2. `Sozlamalar → Maxsus imkoniyatlar → Lumirra himoyasi` yoqilgan bo'lsin.
3. **"Sinash"** tugmasi — overlay, hisoblagich va tugmalarni tekshiradi.
4. Haqiqiy aniqlash: brauzerda matnida `porn` / `18+` / domenda `*.xxx` bo'lgan sahifani
   oching (masalan, o'zingiz yaratgan lokal test HTML). ~1 soniya ichida to'siq chiqadi.

Unit testlar:

```bash
cd mobile-app
./gradlew :composeApp:testDebugUnitTest
```

`AdultContentGuardTest` — 9 ta test: doménlar, obfuskatsiya, 3 til, xavfsiz kontekst,
sezgirlik, ilova nomi, takrorlanish, bo'sh kirish.

## Yangilashdan keyin

Accessibility event turlari o'zgargan (`typeWindowContentChanged` qo'shildi). Ilovani
yangilagandan so'ng, agar aniqlash ishlamasa:

1. `Sozlamalar → Maxsus imkoniyatlar → Lumirra himoyasi` ni **o'chirib, qayta yoqing**
   (yoki telefonni qayta ishga tushiring) — Android xizmat konfiguratsiyasini qayta o'qiydi.
2. MIUI/HyperOS: `Avtostart` yoqilgan, batareya tejash cheklovi o'chirilgan bo'lsin.
3. `Ilovalar → 18+ himoyasi → Sinash` bilan overlay ishlashini tekshiring.
