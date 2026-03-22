plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.princeraj.campustaxipooling"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.princeraj.campustaxipooling"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true   // Enable ProGuard/R8 for security in release
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true   // Enable ViewBinding to avoid findViewById boilerplate
    }
}

dependencies {
    // Firebase BoM — manages all Firebase versions together
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-messaging")   // FCM push notifications
    implementation("com.google.firebase:firebase-analytics")

    // AndroidX Core
    implementation(libs.appcompat)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Material Design 3
    implementation(libs.material)

    // Image loading (for profile photos and ride proofs)
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // EncryptedSharedPreferences (Jetpack Security)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // HTTP Client for free Free-Tier PaaS Notifications
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}