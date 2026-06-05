plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.campusmart"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.campusmart"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.mediation.test.suite)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    // Gson
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("com.alipay.sdk:alipaysdk-android:+@aar")
    implementation("com.squareup.picasso:picasso:2.71828")
    implementation("com.github.bumptech.glide:glide:4.16.0")
}