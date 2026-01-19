plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.chatbot"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.chatbot"
        minSdk = 24
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.room.runtime.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
// Room
    implementation ("androidx.room:room-runtime:2.6.1")
    implementation ("androidx.room:room-common:2.6.1")
    annotationProcessor ("androidx.room:room-compiler:2.6.1")

    // LiveData và ViewModel
    implementation ("androidx.lifecycle:lifecycle-livedata:2.8.0")
    implementation ("androidx.lifecycle:lifecycle-viewmodel:2.8.0")

    // OkHttp
    implementation ("com.squareup.okhttp3:okhttp:4.12.0")

    // JSON
    implementation ("org.json:json:20231013")

    // Lottie
    implementation ("com.airbnb.android:lottie:6.4.1")
}