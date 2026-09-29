# NovaTV ProGuard kurallari

# --- Room ---
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# --- kotlinx.serialization ---
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.hp.novatv.** {
    *** Companion;
}
-keepclasseswithmembers class com.hp.novatv.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# @Serializable siniflar
-keep,includedescriptorclasses class com.hp.novatv.**$$serializer { *; }
-keepclassmembers class com.hp.novatv.** {
    *** Companion;
}
-keepclasseswithmembers class com.hp.novatv.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- OkHttp ---
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# --- VLC ---
# VLC JNI uzerinden cagriyor; siniflari isimlendirilmemeli.
-keep class org.videolan.libvlc.** { *; }
-keep class org.videolan.libvlc.interfaces.** { *; }
-dontwarn org.videolan.libvlc.**

# --- Media3 ---
-dontwarn androidx.media3.**
# @UnstableApi kullanimi supress edilir, silme degil.
-keepclassmembers class androidx.media3.** { *; }

# --- Compose ---
-dontwarn androidx.compose.**

# --- XMLTV / M3U (reflection yok ama guvence) ---
-keep class com.hp.novatv.source.** { *; }

# R8 kaynak dosya adini gizler; stack trace icin sakla.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
