# ===== Lumirra release (R8) =====

-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*, RuntimeVisibleAnnotations

# kotlinx.serialization
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.opal.app.**$$serializer { *; }
-keepclassmembers class com.opal.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.opal.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-dontnote kotlinx.serialization.**

# Ktor / OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.slf4j.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-keepclassmembers class io.ktor.** { volatile <fields>; }

# Ilova modellari (serializatsiya uchun nomlar saqlanadi)
-keep class com.opal.app.data.ModelsKt { *; }
-keep class com.opal.app.network.** { *; }

# Accessibility/overlay klasslari reflection orqali chaqirilmaydi, lekin
# manifestda nomlari bor — R8 ularni saqlaydi. Qo'shimcha himoya:
-keep class com.opal.app.blocking.** extends android.app.Service { *; }
-keep class com.opal.app.blocking.** extends android.app.Activity { *; }
-keep class com.opal.app.blocking.** extends android.content.BroadcastReceiver { *; }
