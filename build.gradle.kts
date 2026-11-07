// AGP 9는 Kotlin 컴파일을 내장하므로 org.jetbrains.kotlin.android 플러그인을 따로 적용하지 않습니다.
// 내장 Kotlin이 쓸 Kotlin Gradle 플러그인 버전만 classpath로 고정합니다.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.21")
    }
}

plugins {
    id("com.android.application") version "9.4.0" apply false
}
