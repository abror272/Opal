# Panoh — brend va logo

**Nom:** **Panoh** — o'zbekcha "boshpana, himoya" ma'nosini beradi. Qisqa (5 harf),
oson talaffuz qilinadi va ilovaning maqsadini (ekran uchun xavfsiz burchak) aniq
ifodalaydi. Play qidiruvida aynan shu nomdagi ilova topilmadi.

**Shior:** *Ekraningiz panohi.*


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
├── panoh-icon.svg        # to'liq ikonka (1024, rounded)
├── panoh-mark.svg        # shaffof belgi
├── panoh-mark-tight.svg  # header uchun yaqin kesim (generated)
├── render.mjs              # Android mipmap + adaptive + iOS 1024 + store
├── render-mark.mjs         # ilova ichidagi mark PNG
├── panoh-store-512.png
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
`icon1024.png`, `composeResources/drawable/panoh_mark.png`.

iOS ikonkasi **alpha kanalisiz**; Android adaptive ikonka
`background + foreground + monochrome` uch qatlamli.
