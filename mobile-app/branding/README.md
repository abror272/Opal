# Lumirra — brend va logo

**Nom:** Lumirra — *lumen* (yorug'lik) + *mirra* (aura) qo'shimchasidan yasalgan yangi so'z.
Ma'nosi: "diqqatingizga yorug'lik". Play'da shu nomdagi ilova topilmadi, `lumirra.com`
bo'sh (2026-11 holatiga ko'ra).

**Shior:** *Yorug'lik diqqatingizda.*

## Belgi

- **Arka / darvoza** — diqqatni almashtirish, "chiqish eshigi"; himoya qalqoni
  kabi agressiv emas.
- **Orbita** — vaqt va kunning aylanishi; ilovaning fokus/screen-time mavzusi.
- **To'rt qirrali yulduzcha** — aniqlik va yongan diqqat.
- **Gradient** — mint → cyan → periwinkle; xotirjam, ammo texnologik.
- Ko'z/kamera/qulf yo'q — kuzatuv ilovasi taassurotini bermaslik uchun.

## Palitra

| Token | Hex | Qayerda |
|---|---|---|
| Midnight | `#030407` → `#12141F` | fon |
| Mint | `#A9E8B8` / `#8AF0D6` | asosiy aksent |
| Cyan | `#61C6E8` / `#6FD9E8` | gradient o'rtasi |
| Periwinkle | `#B08CFF` / `#93A9FF` | gradient oxiri |
| Ivory | `#FFF7E8` | yorug' qirra |
| Danger | `#FF5A76` | faqat to'siq ekrani |
| Text | `#FFFFFF` / `#B3FFFFFF` / `#66FFFFFF` | tipografiya |

Tipografiya: tizim sans, **Bold**, harf oralig'i −0.45sp (wordmark).

## Fayllar

```
branding/
├── lumirra-icon.svg        # to'liq ikonka (1024, rounded)
├── lumirra-mark.svg        # shaffof belgi
├── lumirra-mark-tight.svg  # header uchun yaqin kesim (generated)
├── render.mjs              # Android mipmap + adaptive + iOS 1024 + store
├── render-mark.mjs         # ilova ichidagi mark PNG
├── lumirra-store-512.png
└── preview-*.png
```

## Qayta render

```bash
cd mobile-app/branding
npm i
node render.mjs
node render-mark.mjs   # oxirida — tight mark'ni yozadi
```

Yangilanadi: `res/mipmap-*`, `drawable/ic_launcher_*` (vector), iOS AppIcon,
`icon1024.png`, `composeResources/drawable/lumirra_mark.png`.

iOS ikonkasi **alpha kanalisiz**; Android adaptive ikonka
`background + foreground + monochrome` uch qatlamli.
