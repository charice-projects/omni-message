plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")  // 修正为正确的插件ID
    id("kotlin-kapt")
}

android {
    namespace = "com.omni.message.feature.contact"
    compileSdk = 35  // 降级到34

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    buildFeatures {
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"  // 更新版本
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17  // 改为17
        targetCompatibility = JavaVersion.VERSION_17  // 改为17
    }
    
    kotlinOptions {
        jvmTarget = "17"  // 改为17
    }
}

dependencies {
    // 模块依赖
    implementation(project(":core"))
    
    // Hilt - 统一版本
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")
}