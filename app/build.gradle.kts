plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.cjsdudwls1.colorinversion"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.cjsdudwls1.colorinversion"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        resourceConfigurations += listOf("ko")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            // 배포용 서명 키가 없을 때도 설치 가능한 APK를 만들기 위해 debug 키로 서명합니다.
            // Play 스토어 배포 시에는 별도 업로드 키로 교체하세요.
            signingConfig = signingConfigs.getByName("debug")
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
        buildConfig = false
    }
}
