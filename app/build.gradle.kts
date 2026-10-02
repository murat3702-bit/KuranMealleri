import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val ksFile = rootProject.file("keystore.properties")
val ksProps = Properties().apply {
    if (ksFile.exists()) ksFile.inputStream().use { load(it) }
}

android {
    namespace = "com.mb.kuranmealleri"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.mb.Kuranix"   // orijinal uygulamayla aynı paket adı
        minSdk = 26
        targetSdk = 34
        versionCode = 9             // orijinali 8'di; güncelleme için büyük olmalı
        versionName = "2.0"
    }

    signingConfigs {
        if (ksFile.exists()) {
            create("release") {
                storeFile = rootProject.file(ksProps.getProperty("storeFile"))
                storePassword = ksProps.getProperty("storePassword")
                keyAlias = ksProps.getProperty("keyAlias")
                keyPassword = ksProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // Eski uygulamayla yan yana kurulabilsin diye paket adına ek
            applicationIdSuffix = ".yeni"
        }
        release {
            isMinifyEnabled = false
            if (ksFile.exists()) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.webkit:webkit:1.11.0")
}
