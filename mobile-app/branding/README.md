# Lumirra — brend va logo

**Nom:** Lumirra — *lumen* (yorug'lik) + *mirra* (aura/shifo) qo'shimchasidan yasalgan
yangi so'z. Ma'nosi: "diqqatingizga yorug'lik". Play Store'da shu nomdagi ilova
topilmadi, `lumirra.com` doméni bo'sh (2026-11 holatiga ko'ra).

**Shior:** *Yorug'lik diqqatingizda.* / *Light on your focus.*

## Belgi (mark)

- **Qalqon** — himoya va xavfsizlik.
- **To'rt qirrali yulduzcha** — aniqlik, fokus va "yongan" payt.
- **Mint → cyan → periwinkle gradient** — xotirjam, ammo texnologik.
- Hech qanday ko'z/kamera/qulf belgisi yo'q — kuzatuv ilovasi taassurotini bermaslik uchun
  (xavfsizlik skanerlari va foydalanuvchi ishonchi uchun muhim).

## Palitra

| Token | Hex | Qayerda |
|---|---|---|
| Midnight | `#05080A` / `#0B1A1D` | fon |
| Mint | `#A9E8B8` / `#8AF0D6` | asosiy aksent |
| Cyan | `#61C6E8` | gradient o'rtasi |
| Periwinkle | `#93A9FF` | gradient oxiri |
| Danger | `#FF5A76` | faqat 18+ to'siq ekrani |
| Text | `#FFFFFF` / `#B3FFFFFF` / `#66FFFFFF` | tipografiya |

Tipografiya: tizim sans, **Bold**, harf oralig'i −0.45sp (wordmark).

## Fayllar

```
branding/
├── lumirra-icon.svg        # to'liq ilova ikonkasi (1024)
├── lumirra-mark.svg        # shaffof fonli belgi
├── lumirra-mark-tight.svg  # header uchun yaqinroq kesilgan belgi (generated)
├── render.mjs              # Android mipmap + iOS 1024 + store 512
├── render-mark.mjs         # ilova ichidagi mark PNG
└── preview-*.png           # ko'rish uchun
```

## Qayta render qilish

```bash
cd mobile-app/branding
npm i
node render.mjs
node render-mark.mjs
```

`render.mjs` quyidagilarni yangilaydi:

- `composeApp/src/androidMain/res/mipmap-*/ic_launcher(.png|_round.png)`
- `composeApp/src/androidMain/res/drawable/ic_launcher_background.xml`
- `composeApp/src/androidMain/res/drawable/ic_launcher_foreground.xml`
- `composeApp/src/androidMain/res/drawable/ic_launcher_monochrome.xml`
- `composeApp/src/commonMain/composeResources/drawable/lumirra_mark.png`
- `iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/icon1024.png`
- `icon1024.png`

iOS ikonkasi **alpha kanalisiz** (App Store talabi), Android adaptive ikonka
`background + foreground + monochrome` uch qatlamli.
