# Opal Mobile — Kotlin Multiplatform (Android + iOS)

Opal screen-time clone'ning **native mobil versiyasi** — bir Kotlin codebase, ikkala platforma.
Backend sifatida web loyihaning Next.js API'si ishlatiladi (`/api/apps`, `/api/sessions`, `/api/stats`, `/api/profile`).
API band bo'lmasa — ilova offline demo ma'lumotlari bilan to'liq ishlaydi.

## ✨ Imkoniyatlar

| Funksiya | Tavsif |
|---|---|
| 🏠 Home | Himoya ringi, streak 🔥, himoya switch, haftalik mini-chart |
| 🎯 Fokus | 5 preset (Deep Focus / Ish / O'qish / Uyqu / Maxsus), davomiylik chiplari, jonli taymer |
| 📊 Statistika | 7 kunlik bar chart (stagger animatsiya) + bezier trend line chart |
| 📱 Ilovalar | Bloklash switch (optimistic update), limit progress |
| 👤 Profil | Yutuqlar, sozlamalar, kunlik maqsad, Opal Plus |
| ⏱️ Sessiya | Sekundlik ticker, gradient countdown ring, erta chiqish = streak -1 (web bilan bir xil mantiq) |

## 🪟 Glass Effect (Telegram uslubi)

- **iOS — haqiqiy glass**: pastki tab bar `UIVisualEffectView` (`SystemUltraThinMaterialDark`) bilan qurilgan —
  Compose kontenti **jonli xiralashib** ko'rinadi (aynan Telegram'dagidek). Tanlangan tab frosted
  light-material pill bilan **spring animatsiyasida sirg'aladi**, ikonka ko'tarilib label chiqadi.
- **Tab o'tishida glass "veil"**: boshqa bo'limga o'tganda butun ekranni ultra-thin blur qoplaydi va
  smooth easing bilan erib ketadi — content glass orqali o'tgandek his beradi.
- **Android**: real backdrop blur cheklovi tufayli glass gradient + specular nur + hairline border
  bilan imitatsiya qilinadi (Material-3 uslubida, baribir silliq).

## 📁 Struktura

```
mobile-app/
├── composeApp/
│   └── src/
│       ├── commonMain/kotlin/com/opal/app/   # 100% umumiy kod
│       │   ├── OpalApp.kt                    # root + tab navigatsiya + veil
│       │   ├── theme/OpalTheme.kt            # ranglar, gradientlar
│       │   ├── data/                         # Models, DemoData, Repository, AppGraph
│       │   ├── network/ApiClient.kt          # Ktor client (expect engine)
│       │   ├── session/SessionController.kt  # taymer + tugatish mantiqi
│       │   ├── glass/OpalGlass.kt            # expect GlassTabBar/GlassVeil + GlassPane
│       │   ├── platform/Platform.kt          # expect rememberSafePadding
│       │   └── ui/                           # komponentlar, chartlar, 5 ekran
│       ├── androidMain/                      # MainActivity, Manifest, OkHttp engine
│       └── iosMain/                          # UIKit glass bar, veil, Darwin engine
├── iosApp/
│   ├── project.yml                           # XcodeGen konfiguratsiyasi
│   └── iosApp/                               # App.swift, Info.plist, Assets
└── icon1024.png                              # AI-generatsiya qilingan icon
```

## 🔌 Backend ulash

Ilova dev server API'siga ulanadi. Base URL platformaga qarab:

| Platforma | Default | Izoh |
|---|---|---|
| Android emulyator | `http://10.0.2.2:3000` | host kompyuterning localhost'i |
| iOS simulyator | `http://localhost:3000` | Mac'dagi server |
| **Real qurilma** | LAN IP kerak | `composeApp/src/*/network/Api*.kt` da `defaultApiBase` ni o'zgartiring: `http://192.168.1.5:3000` |

Serverni ishga tushirish: web loyiha ildizida `bun run dev`.

## 🤖 Android qurish

Talablar: **Android Studio Ladybug+**, JDK 17, Android SDK 35.

1. Android Studio → `Open` → `mobile-app/` papkasini tanlang
2. Gradle sync tugagach (birinchi marta internet kerak) → `composeApp` konfiguratsiyasini tanlab ▶ Run
3. Yoki terminalda:
   ```bash
   cd mobile-app
   gradle wrapper --gradle-version 8.10.2   # bir marta (wrapper jar'ini yaratadi)
   ./gradlew :composeApp:assembleDebug
   # APK: composeApp/build/outputs/apk/debug/
   ```

## 🍎 iOS qurish

Talablar: **macOS, Xcode 15+**, [XcodeGen](https://github.com/yonaskolb/XcodeGen) (`brew install xcodegen`), Gradle.

1. Xcode project'ini generatsiya qiling:
   ```bash
   cd mobile-app/iosApp
   xcodegen            # iosApp.xcodeproj yaratadi
   open iosApp.xcodeproj
   ```
2. Scheme: **iosApp** → Run ▶ (iPhone 15+ simulyator)
   - Pre-build script avtomatik `composeApp.framework` ni Kotlin'dan quradi
   - Real qurilma uchun `project.yml` dagi framework path'ni `iosArm64` ga o'zgartiring va Signing'da Team tanlang

## ⚠️ Cheklovlar va keyingi qadamlar

- Haqiqiy Screen Time bloklash uchun platform API'lari kerak:
  iOS `DeviceActivity`/`ScreenTime` (Family Controls entitlement), Android `UsageStats`+`Accessibility`.
  Hozirgi versiya **UI/UX clone + server-sync demo** (portfolio uchun to'liq yetarli).
- Push notification, PIN qulfi va Battle Math keyingi bosqich rejasida.
- HTTP (cleartext) faqat local dev uchun yoqilgan — release'da HTTPS kerak.
