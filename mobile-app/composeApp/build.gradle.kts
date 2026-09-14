import com.android.build.api.dsl.ApplicationExtension
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    // ❗️ com.android.application ATAYLAB bu yerda yo'q — pastga qarang:
    // u faqat Android SDK topilganda `apply(plugin = ...)` bilan qo'llanadi.
}

/**
 * iOS-only Mac'larda Android SDK ko'pincha umuman o'rnatilmagan bo'ladi.
 * AGP SDK'siz Gradle KONFIGURATSIYASINI "SDK location not found" bilan yiqitadi —
 * natijada iOS targetlarni ham qurib bo'lmaydi (Xcode pre-build script shu xatoni ko'rsatadi).
 *
 * Shuning uchun: SDK topilmasa Android target umuman qo'shilmaydi, iOS buildlar ishlayveradi.
 * Majburan o'chirish:  ./gradlew ... -Popal.skipAndroid=true
 */
val androidSdkDir: File? = run {
    if (project.hasProperty("opal.skipAndroid")) return@run null

    val fromEnv = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")

    val fromLocalProperties = rootProject.file("local.properties")
        .takeIf { it.isFile }
        ?.let { f ->
            Properties().apply { f.inputStream().use { load(it) } }
                .getProperty("sdk.dir")
        }

    // Android Studio'ning standart SDK joylari (macOS, Windows, Linux)
    val home = System.getProperty("user.home")
    val defaults = listOfNotNull(
        System.getenv("LOCALAPPDATA")?.let { "$it/Android/Sdk" },
        "$home/Library/Android/sdk",
        "$home/Android/Sdk",
        "$home/Android/sdk",
    )

    (listOfNotNull(fromEnv, fromLocalProperties) + defaults)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { File(it) }
        .firstOrNull { it.isDirectory }
}

val androidEnabled = androidSdkDir != null

if (androidEnabled) {
    apply(plugin = libs.plugins.androidApplication.get().pluginId)
} else {
    logger.lifecycle(
        "Opal: Android SDK topilmadi — faqat iOS targetlar quriladi " +
            "(Android uchun ANDROID_HOME yoki local.properties -> sdk.dir kerak)."
    )
}

kotlin {
    if (androidEnabled) {
        androidTarget {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
            }
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }

        if (androidEnabled) {
            // `androidMain` accessor faqat androidTarget mavjud bo'lganda yaratiladi
            getByName("androidMain").dependencies {
                implementation(libs.androidx.activity.compose)
                implementation(libs.ktor.client.okhttp)
            }
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

if (androidEnabled) {
    // `android { }` type-safe accessor plugin `plugins {}` blokida bo'lmagani uchun
    // mavjud emas — shu sababli ochiq tur (public DSL interface) orqali sozlaymiz.
    (extensions.getByName("android") as ApplicationExtension).apply {
        namespace = "com.opal.app"
        compileSdk = libs.versions.compileSdk.get().toInt()

        defaultConfig {
            applicationId = "com.opal.app"
            minSdk = libs.versions.minSdk.get().toInt()
            targetSdk = libs.versions.targetSdk.get().toInt()
            versionCode = 1
            versionName = "1.0.0"
        }

        packaging {
            resources {
                excludes += "/META-INF/{AL2.0,LGPL2.1}"
            }
        }

        buildTypes {
            getByName("release") {
                isMinifyEnabled = false
            }
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }
}
