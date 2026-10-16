plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Versi bisa diatur dari CI: tag "v1.2.3" -> ZL_VERSION=1.2.3, nomor run -> versionCode.
val zlVersionName: String = System.getenv("ZL_VERSION") ?: "0.1.0"
val zlVersionCode: Int = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1

android {
    namespace = "com.sally.zlauncher"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sally.zlauncher"
        minSdk = 28
        targetSdk = 35
        versionCode = zlVersionCode
        versionName = zlVersionName

        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
