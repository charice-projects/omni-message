plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("kotlin-kapt")
    id("kotlin-parcelize")
}

android {
    namespace = "com.omnimsg"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.omnimsg.pro"
        minSdk = 24
        targetSdk = 34
        versionCode = 10000
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // 构建配置
        buildConfigField("String", "API_BASE_URL", "\"https://api.omnimsg.com\"")
        buildConfigField("String", "WAKE_WORD", "\"熙熙\"")
        buildConfigField("boolean", "ENABLE_SERVER_FEATURES", "false")
        buildConfigField("boolean", "ENABLE_ANALYTICS", "false")
        buildConfigField("boolean", "ENABLE_DEBUG_FEATURES", "false")
    }

    signingConfigs {
        create("release") {
            storeFile = file("../keystore/omnimsg.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "password"
            keyAlias = System.getenv("KEY_ALIAS") ?: "key"
            keyPassword = System.getenv("KEY_PASSWORD") ?: "password"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("boolean", "ENABLE_DEBUG_FEATURES", "false")
        }

        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
            buildConfigField("boolean", "ENABLE_DEBUG_FEATURES", "true")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-Xjvm-default=all",
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/*.kotlin_module"
        }
    }
}

dependencies {
    // 核心依赖
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.activity.compose)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.compose.navigation)
    implementation(libs.compose.hilt.navigation)

    // DI
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // 数据库
    implementation(libs.bundles.database)
    kapt(libs.room.compiler)

    // 网络
    implementation(libs.bundles.network)

    // AI
    implementation(libs.bundles.ai)

    // 安全
    implementation(libs.bundles.security)

    // 媒体
    implementation(libs.coil)

    // Excel
    implementation(libs.apache.poi)
    implementation(libs.apache.poi.ooxml)

    // 工具
    implementation(libs.gson)
    implementation(libs.timber)
    implementation(libs.lottie)
    implementation(libs.threetenabp)
    implementation(libs.kotlinx.datetime)

    // QR Code
    implementation(libs.zxing)
    implementation(libs.zxing.android)

    // 模块依赖
    implementation(project(":core"))
    implementation(project(":feature:contact"))
    implementation(project(":feature:excelimport"))
    implementation(project(":feature:voice"))
    implementation(project(":feature:quickactions"))

    // 测试
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    androidTestImplementation(libs.junit.ext)
    androidTestImplementation(libs.espresso)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)

    // Java 8+ 支持
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
}