import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.feldman.coretools"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.feldman.coretools"
        minSdk = 34
        targetSdk = 36
        versionCode = 12
        versionName = "2.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file("F:/Users/feldm/Projects/feldman.jks")
            storePassword = "Neches90210"
            keyAlias = "key0"
            keyPassword = "Neches90210"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
        getByName("debug") {
            signingConfig = signingConfigs.getByName("release")
        }
    }
    buildFeatures {
        compose = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_24
        targetCompatibility = JavaVersion.VERSION_24
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_24)
        }
    }

}

dependencies {

    //Core Android
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material.v1130)
    implementation(platform(libs.androidx.compose.bom.v20250801))

    //Jetpack Compose
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.animation.core)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.navigation.compose)

    //Icons
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)

    //AndroidX Libraries
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.compose.material3)
    ksp(libs.androidx.room.compiler)
    implementation(libs.room.ktx)

    //Google Play & Network
    implementation(libs.play.services.location)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

    //Accompanist
    implementation(libs.accompanist.navigation.animation)
    implementation(libs.accompanist.permissions)
    implementation(libs.accompanist.swiperefresh)

    //Coroutines
    implementation(libs.kotlinx.coroutines.android)

    //UI Enhancements / Visual
    implementation(libs.androidliquidglass)
    implementation(libs.capsule)

    //Testing
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom.v20250801))

    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)

}
