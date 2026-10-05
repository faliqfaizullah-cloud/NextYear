plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.nextyear.app"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.nextyear.app"
        minSdk = 24
        targetSdk = 34
        versionCode = (project.findProperty("vc") as String?)?.toInt() ?: 1
        versionName = "1.0.${project.findProperty("vc") ?: "1"}"
    }
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }
    buildTypes { release { isMinifyEnabled = false } }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
