# 萤视Pro ProGuard 混淆规则

# ==================== 网络层 (保持序列化字段名) ====================
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod

# Retrofit
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Kotlinx Serialization
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class com.ezvizpro.core.network.model.** {
    *** Companion;
}
-keep class com.ezvizpro.core.network.model.** { *; }

# OkHttp
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# ==================== ExoPlayer / Media3 ====================
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ==================== Hilt / DI ====================
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# ==================== 通用 ====================
# 保持 BuildConfig
-keep class com.ezvizpro.BuildConfig { *; }

# 保持 Timber 日志（debug）
-assumenosideeffects class timber.log.Timber {
    public static void v(...);
    public static void d(...);
}

# 保持 DataStore
-keep class androidx.datastore.** { *; }
