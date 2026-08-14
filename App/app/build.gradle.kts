import java.io.File
import java.util.Properties

// 1. Cargar el archivo local.properties
val localProperties = Properties()
val localPropertiesFile = File(rootProject.projectDir, "local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}
// 2. Extraer el token PÚBLICO de Mapbox
val mapboxPublicToken = localProperties.getProperty("MAPBOX_PUBLIC_TOKEN") ?: ""

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.raul.minimapagta"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.raul.minimapagta"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 3. Generar dinámicamente el string "mapbox_access_token" sin exponerlo en el XML
        resValue("string", "mapbox_access_token", mapboxPublicToken)
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        resValues = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // API de Rutas de Mapbox y calculadora matemática Turf
    implementation("com.mapbox.mapboxsdk:mapbox-sdk-services:5.8.0")
    implementation("com.mapbox.mapboxsdk:mapbox-sdk-turf:5.8.0")

// Motor de conexión a internet (Retrofit)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    testImplementation(libs.junit)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // MAPBOX - COMPATIBILIDAD 16 KB
    implementation("com.mapbox.maps:android-ndk27:11.8.0")
    implementation("com.mapbox.extension:maps-compose-ndk27:11.8.0")
}