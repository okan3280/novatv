// NovaTV - root build
//
// AGP 9.4.1 -> Gradle 9.6.0+ ve JDK 17 zorunlu.
// AGP 9 "built-in Kotlin" varsayilan: org.jetbrains.kotlin.android UYGULANMAZ.
// AGP 9.0 KGP 2.2.10'u runtime bagimlilik olarak getirir; 2.4.20 kullanmak icin
// buildscript uzerinden strictly ile sabitliyoruz.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin") {
            version { strictly("2.4.20") }
        }
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
