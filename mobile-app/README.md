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

Talablar: **Android Studio Ladybug+**, Android SDK 35. JDK alohida o'rnatish shart emas —
`mobile-app/gradle/gradle-daemon-jvm.properties` Gradle daemon'ini **JDK 21** ga majburlaydi
(AGP 8.7.3 JDK 25 bilan ishlamaydi: sync `java.lang.IllegalArgumentException: 25.0.4` beradi).

> ⚠️ Bu repo'da Gradle project **`mobile-app/`** ichida. Android Studio'da aynan
> shu papkani `Open` qiling (repo ildizini emas — ildizda Gradle build yo'q).

1. Android Studio → `Open` → **`mobile-app/`** papkasini tanlang
2. **Gradle JDK'ni tekshiring:** `Settings` → `Build, Execution, Deployment` →
   `Build Tools` → `Gradle` → **Gradle JDK = Embedded JDK (jbr-21) / 17 / 21**.
   ❌ JDK 25 yoki "JAVA_HOME" ni tanlamang — sync yiqiladi va Edit Configurations'da
   module `<no module>` bo'lib qoladi.
3. Sync tugagach `composeApp` konfiguratsiyasini tanlab ▶ Run
4. Yoki terminalda:
   ```bash
   cd mobile-app
   chmod +x gradlew        # macOS/Linux'da bir marta
   ./gradlew :composeApp:assembleDebug
   # APK: composeApp/build/outputs/apk/debug/
   ```

> `local.properties` (Android SDK yo'li) `.gitignore`da — Android Studio uni o'zi yaratadi.

### ❗️ "no module" xatosi bo'lsa
1. `mobile-app/` papkasi ochilganiga ishonch hosil qiling (ildiz emas).
2. `File` → `Invalidate Caches…` → `Invalidate and Restart`.
3. Sync'dan keyin: `Edit Configurations` → `+` → `Android App` → **Module** ro'yxatida
   `OpalApp.composeApp` paydo bo'ladi.

## 🍎 iOS qurish (macOS)

Talablar:
- **macOS** + **Xcode 15+**
- **JDK 17+** (`java -version` bilan tekshiring). Agar `JAVA_HOME` bo'sh bo'lsa:
  `export JAVA_HOME=$(/usr/libexec/java_home -v 17)`
- [XcodeGen](https://github.com/yonaskolb/XcodeGen): `brew install xcodegen`

1. Repo'ni klon qilgach, wrapper'ni executable qiling (bir marta):
   ```bash
   cd mobile-app
   chmod +x gradlew
   ```
2. Xcode project'ini generatsiya qiling:
   ```bash
   cd iosApp
   xcodegen generate      # iosApp.xcodeproj yaratadi
   open iosApp.xcodeproj
   ```
3. Scheme: **iosApp** → Run ▶ (iPhone simulyator)
   - Pre-build script avtomatik `ComposeApp.framework`ni Kotlin'dan quradi
     (`composeApp/build/xcode-frameworks/<CONFIGURATION>/<SDK_NAME>`)
   - **Real qurilma** uchun: Signing & Capabilities → Team tanlang.
     `project.yml`da hech narsani o'zgartirish shart emas — skript barcha
     arxitekturalar (arm64, simulator arm64/x64) uchun ishlaydi.
4. Agar Xcode "Sandbox: bash deny file-read-data" bersa — bu allaqachon
   `ENABLE_USER_SCRIPT_SANDBOXING: NO` bilan tuzatilgan (`project.yml`).

## ⚠️ Cheklovlar va keyingi qadamlar

- Haqiqiy Screen Time bloklash uchun platform API'lari kerak:
  iOS `DeviceActivity`/`ScreenTime` (Family Controls entitlement), Android `UsageStats`+`Accessibility`.
  Hozirgi versiya **UI/UX clone + server-sync demo** (portfolio uchun to'liq yetarli).
- Push notification, PIN qulfi va Battle Math keyingi bosqich rejasida.
- HTTP (cleartext) faqat local dev uchun yoqilgan — release'da HTTPS kerak.
