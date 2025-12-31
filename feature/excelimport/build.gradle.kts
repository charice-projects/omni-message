plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("kotlin-kapt")
}

android {
    namespace = "com.omnimsg.feature.excelimport"
    compileSdk = 35

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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        // 如果这个模块用Compose就保留，否则可以删掉
        // compose = true
    }
    // 如果上面启用了compose，这里需要配置
    // composeOptions {
    //     kotlinCompilerExtensionVersion = "1.5.11"
    // }
}

dependencies {
    implementation(project(":core"))
    // 添加此模块所需的额外依赖，例如处理Excel的库
    // implementation("org.apache.poi:poi-ooxml:5.2.3")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")
}