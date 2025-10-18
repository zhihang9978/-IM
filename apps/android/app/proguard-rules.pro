# LanXin IM ProGuard Rules
# 蓝信即时通讯混淆配置

# ==================== 基础配置 ====================

# Keep line numbers for debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep generic signatures for reflection
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# ==================== Kotlin ====================

# Kotlin Metadata
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keep class kotlin.coroutines.** { *; }

# Kotlin Parcelize
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

# ==================== Android ====================

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep View constructors
-keepclassmembers class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Keep Activity lifecycle methods
-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ==================== 数据模型 ====================

# Keep all data classes (used by Gson/Retrofit)
-keep class com.lanxin.im.data.model.** { *; }
-keep class com.lanxin.im.data.remote.** { *; }

# Room Database
-keep class com.lanxin.im.data.local.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Database class * { *; }

# ==================== Retrofit & OkHttp ====================

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# OkHttp Platform
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# OkHttp WebSocket
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# ==================== Gson ====================

# Gson uses generic type information stored in a class file when working with fields
-keepattributes Signature

# Gson specific classes
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Prevent proguard from stripping interface information from TypeAdapter, TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Prevent R8 from leaving Data object members always null
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# ==================== Glide ====================

-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
 <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
  *** rewind();
}

# ==================== Hilt ====================

-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Hilt AndroidX
-keep class androidx.hilt.** { *; }
-dontwarn androidx.hilt.**

# ==================== TRTC SDK ====================

# 腾讯云 TRTC SDK
-keep class com.tencent.** { *; }
-dontwarn com.tencent.**
-keep class org.webrtc.** { *; }
-dontwarn org.webrtc.**

# ==================== MinIO ====================

# MinIO S3 Client
-keep class io.minio.** { *; }
-dontwarn io.minio.**
-keep class com.amazonaws.** { *; }
-dontwarn com.amazonaws.**
-dontwarn org.apache.commons.**

# ==================== Custom Classes ====================

# Keep WebSocket related classes
-keep class com.lanxin.im.data.remote.WebSocketClient { *; }
-keep class com.lanxin.im.data.remote.WebSocketClient$** { *; }

# Keep ViewModel classes
-keep class com.lanxin.im.viewmodel.** { *; }

# Keep Repository classes
-keep class com.lanxin.im.data.repository.** { *; }

# Keep custom views
-keep class com.lanxin.im.widget.** { *; }

# Keep Managers
-keep class com.lanxin.im.ui.chat.manager.** { *; }

# ==================== Security ====================

# EncryptedSharedPreferences
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

# ==================== 其他 ====================

# Remove logging in release build
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Suppress warnings
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.*
