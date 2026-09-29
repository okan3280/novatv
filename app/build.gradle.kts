plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

import java.util.Base64

android {
    namespace = "com.hp.novatv"

    // Compose 1.12.1 (BOM 2026.09.00) compileSdk 37 + AGP 9.1+ zorunlu kiliyor.
    compileSdk = 37
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.hp.novatv"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        // TCL 65Q6C arm64. Tek ABI = kucuk APK.
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    androidResources {
        // AGP 9: resourceConfigurations kaldirildi -> localeFilters
        localeFilters += listOf("tr", "en")
    }

    signingConfigs {
        // GitHub Secrets: KEYSTORE_B64 / STORE_PASSWORD / KEY_ALIAS / KEY_PASSWORD
        val keystoreB64 = providers.environmentVariable("KEYSTORE_B64").orNull
        if (!keystoreB64.isNullOrBlank()) {
            val tmp = layout.buildDirectory.file("keystore/release.jks").get().asFile
            tmp.parentFile.mkdirs()
            // Kotlin DSL script'inde java.util.* otomatik import edilmez.
            tmp.writeBytes(Base64.getDecoder().decode(keystoreB64))
            create("release") {
                storeFile = tmp
                storePassword = providers.environmentVariable("STORE_PASSWORD").orNull
                keyAlias = providers.environmentVariable("KEY_ALIAS").orNull
                keyPassword = providers.environmentVariable("KEY_PASSWORD").orNull
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            // Secrets yoksa release imzasiz cikar; CI'da imzalı artifact uretilir.
            signingConfigs.findByName("release")?.let { signingConfig = it }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        // AGP 9'da buildConfig varsayilan KAPALI.
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/INDEX.LIST"
        }
        // VLC + Compose native birlikte; sikistirma icin native kodlara dokunma.
        jniLibs {
            useLegacyPackaging = false
        }
    }

    lint {
        abortOnError = false
    }
}

// AGP 9: jvmTarget android.compileOptions.targetCompatibility'den turetilir.
kotlin {
    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.runtime)

    implementation(libs.tv.material)
    implementation(libs.tv.foundation)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.exoplayer.dash)
    implementation(libs.media3.exoplayer.rtsp)
    implementation(libs.media3.exoplayer.smoothstreaming)
    implementation(libs.media3.ui)
    implementation(libs.media3.ui.leanback)
    implementation(libs.media3.datasource.okhttp)
    implementation(libs.media3.container)
    implementation(libs.media3.common.ktx)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.datastore.preferences)

    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.serialization.json)
    implementation(libs.coroutines.android)

    implementation(libs.coil3.compose)
    implementation(libs.coil3.network.okhttp)

    // VLC yedek motor: ham .ts / rtmp / rtsp / acik codec'ler
    implementation(libs.libvlc.all)
}
