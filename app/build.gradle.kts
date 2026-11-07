import java.util.Properties

plugins {
    id("com.android.application")
}

// 배포 서명 정보는 저장소에 올리지 않는 keystore.properties(루트)에서 읽습니다.
//   storeFile=C:/Users/<you>/.android-keys/colorinversion-upload.jks
//   storePassword=...
//   keyAlias=upload
//   keyPassword=...
// 파일이 없으면 release 빌드도 debug 키로 서명해, 누구나 설치 가능한 APK를 만들 수 있습니다.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) keystorePropertiesFile.inputStream().use { load(it) }
}

android {
    namespace = "com.cjsdudwls1.colorinversion"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.cjsdudwls1.colorinversion"
        minSdk = 26
        targetSdk = 36
        // CI(GitHub Actions)는 VERSION_CODE 를 실행 번호로 넘겨 매 업로드마다 자동 증가시킵니다.
        versionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 3
        versionName = "1.1.0"
    }

    androidResources {
        localeFilters += listOf("ko")
    }

    signingConfigs {
        if (keystoreProperties.isNotEmpty()) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = if (keystoreProperties.isNotEmpty()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        buildConfig = false
    }
}
