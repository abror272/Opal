# Nomaqbul kontent himoyasi

Panoh ekranda nomaqbul (kattalar uchun) kontent paydo bo'lsa, ekranni **darhol to'sadi**
va foydalanuvchini o'zini tiyishga undovchi xabarlar ko'rsatadi.

> Android 11+ da to'liq ishlaydi (ekran rasmi tahlili uchun). iOS'da boshqa ilova
> ekranini o'qish API'si yo'q.

## Qanday ishlaydi

```
AccessibilityService
├── matn: ko'rinadigan tugunlar (300 node / 5000 belgi / 12 ms)
│   ├── manzil satri  → nomaqbul domén (aniq blok)
│   └── sahifa matni  → kuchli/o'rtacha/zaif guruhlar
└── ekran rasmi (API 30+) → open_nsfw TFLite (224×224) → 2 kadr tasdiqlash
        ▼
GuardOverlay — to'liq ekranli to'siq + "nafas olish" hisoblagichi
```

### 1. Matn — "bitta so'z = blok" emas

| Signal | Misol | Qoida |
|---|---|---|
| **Kuchli** | `porn`, `hentai`, `порно`, `xxx` | yolg'iz o'zi bloklaydi |
| **O'rtacha guruh** | `nude`, `erotic`, `fetish`, `bdsm`, `nsfw`, `escort` | kamida **2 xil guruh** |
| **O'rtacha + zaif** | `nude` + `sex` + `adult` | 1 o'rtacha + 2 zaif guruh |
| **Zaif** | `sex`, `adult`, `intim` | faqat 3 xil guruh (HIGH) |
| **Domén** | `pornhub.com`, `*.xxx` | faqat **manzil satridan** |

- `sex` / `18+` / `nude` **yolg'iz bloklanmaydi** — YouTube, musiqa, yangiliklar xavfsiz.
- Ta'lim/tibbiyot/yangilik/musiqa konteksti (`sex education`, `same-sex`, `jinsiy tarbiya`,
  `article`, `music`, `official`, `documentary`...) bo'lsa — faqat kuchli atama bloklaydi.
- Sahifa matnidagi brend/havola (`pornhub.com` izohda) yolg'iz bloklamaydi — faqat
  manzil satri yoki ilova nomi.
- Faqat **ko'rinadigan** tugunlar (RecyclerView'dagi yashirin elementlar emas).

### 2. Vizual — ekran rasmining o'zi

- **Model:** Yahoo **open_nsfw** (BSD-2-Clause), TFLite 5.7 MB, 224×224, BGR, mean `[104,117,123]`.
- Faqat qurilmada ishlaydi; rasm xotirada tahlil qilinadi va darhol bo'shatiladi.
- **2 kadr tasdiqlash:** bitta kadr yetarli emas (statik rasm uchun 2.2s dan keyin
  ikkinchi tekshiruv). Ball ≥ 0.95 bo'lsa — darhol.
- Pol: Past 0.94 · **O'rta 0.87** · Yuqori 0.78.
- Skanerlash: content-change'dan keyin 1s debounce, 1.8s throttle, faqat foreground.
- Launcher, tizim UI, klaviatura, o'rnatuvchi — skanerlanmaydi.

### 3. To'siq ekrani

- Ekran butunlay qoplanadi; **5 soniya** "Nafas oling" hisoblagichi.
- 10 ta tasodifiy, ayblamaydigan, o'zini tiyishga undovchi gap.
- **"Chiqish va orqaga qaytish"** → HOME.
- **"Bu xato — davom etish"** → 30 daqiqa istisno (noto'g'ri aniqlash uchun).
- "Orqaga" tugmasi to'siqni yopmaydi.

## Sozlash

**Ilovalar** tab → **Nomaqbul kontent** kartasi:

- yoqish/o'chirish (default: **yoqilgan**),
- sezgirlik: Past / **O'rta** / Yuqori,
- **Rasm va videoni tekshirish** (vizual model; o'chirilsa faqat matn/URL),
- bugungi bloklar soni,
- **"Sinash"** — haqiqiy overlay'ni ko'rsatadi.

## Maxfiylik

- Matn va rasm **faqat qurilmada** tahlil qilinadi.
- Skrinshot saqlanmaydi, internetga yuborilmaydi.
- Saqlanadigan yagona narsa: bloklar soni, sozlamalar, vaqtinchalik istisnolar.
- `allowBackup=false`.

## Halol cheklovlar

1. Vizual model — **open_nsfw**, ya'ni "ehtimoliy nudity" detektori. Rasm/video har doim
   ham 100% aniqlanmaydi; ba'zi badiiy rasmlar past ball oladi.
2. Android 11+ talab qilinadi (screenshot API). Undan pastda faqat matn/URL.
3. Accessibility o'chirilgan bo'lsa hech narsa ishlamaydi. MIUI xizmatni jimgina
   o'chirib qo'yishi mumkin. **Bir marta** MIUI Sozlamalar → Ilovalar → Panoh →
   "Fonda avtomatik ishga tushirish"ni yoqing (bu telefonda yoqildi). Baribir
   o'chsa — Sozlamalar → Qulaylik → **Panoh himoyasi**ni qayta yoqing.
4. 100% aniqlik kafolatlanmaydi — shuning uchun "Bu xato" va sezgirlik sozlamasi bor.

## Sinash

```bash
cd mobile-app
./gradlew :composeApp:testDebugUnitTest   # 13 test
```

Qurilmada: rasm/video sahifasini oching → logcat'da `PanohVision: score=...`.
`score ≥ 0.87` ikki marta bo'lsa — to'siq chiqadi.

Model litsenziyasi: [THIRD_PARTY.md](THIRD_PARTY.md).
