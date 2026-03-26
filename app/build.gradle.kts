plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
    alias(libs.plugins.hilt)
    alias(libs.plugins.crashlytics)
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
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Enterprise Force: Use JDK 22 toolchain to match user's local environment
    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(22))
        }
    }


    buildFeatures {
        viewBinding = true   // Enable ViewBinding to avoid findViewById boilerplate
    }

    testOptions {
        unitTests.all {
            // Required for Mockito inline mocking under JDK 17+ / JDK 22 toolchain
            it.jvmArgs(
                "-Dnet.bytebuddy.experimental=true",
                "-XX:+EnableDynamicAgentLoading"
            )
        }
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    // Firebase BoM — manages all Firebase versions together
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-messaging")   // FCM push notifications
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics") // Enterprise crash reporting
    implementation("com.google.firebase:firebase-perf")        // Performance monitoring

    // Hilt Dependency Injection (Enterprise Standard)
    implementation("com.google.dagger:hilt-android:2.48")
    annotationProcessor("com.google.dagger:hilt-compiler:2.48")

    // AndroidX Core
    implementation(libs.appcompat)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // ViewModel & LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime:2.7.0")
    
    // Room Database (for offline caching)
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")
    
    // Retrofit + OkHttp (for typed HTTP operations)
    implementation("com.squareup.retrofit2:retrofit:2.10.0")
    implementation("com.squareup.retrofit2:converter-gson:2.10.0")

    // Material Design 3
    implementation(libs.material)

    // Image loading (for profile photos and ride proofs)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.vanniktech:android-image-cropper:4.5.0")

    // Shimmer effect
    implementation("com.facebook.shimmer:shimmer:0.5.0")

    // EncryptedSharedPreferences (Jetpack Security)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // HTTP Client for free Free-Tier PaaS Notifications
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    
    // Unit Testing
    testImplementation("org.mockito:mockito-core:5.5.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("androidx.lifecycle:lifecycle-runtime-testing:2.7.0")
    
    // 100% Free Open-Source Mapping (No API Key Required)
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    // Background Workers (Reliable Sync WITHOUT Google Play Services)
    implementation("androidx.work:work-runtime:2.9.0")
    implementation("com.google.android.gms:play-services-location:21.1.0")

    // Instrumentation Testing
    androidTestImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.5.1")
}
