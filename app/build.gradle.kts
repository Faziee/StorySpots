plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-android")
    id("com.google.relay") version "0.3.12"
    id("com.google.gms.google-services") version "4.4.2"
}

android {
    namespace = "com.storyspots"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.storyspots"
        minSdk = 23
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.play.services.location)
    implementation(libs.androidx.material)
    implementation(libs.coil.compose)
    implementation (libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.room.runtime.android)
    implementation(libs.firebase.messaging)
    implementation(libs.androidx.appcompat)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.dotenv.kotlin)

    //-----SERVICES: FIREBASE-------
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.firebase.firestore.ktx)

    //-----SERVICES: CLOUDINARY-----
    implementation(libs.cloudinary.android)

    //-----SERVICES: MAPBOX-------
    implementation(libs.android)
    implementation(libs.maps.compose)

    //-----SERVICES: COIL---------
    implementation("io.coil-kt:coil-compose:2.2.2")

    //-----SERVICES: LOCATION-----
    implementation("com.google.android.gms:play-services-location:21.1.0")

    //Login dependencies
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)

    //-----SERVICES: LOTTIE------
    implementation(libs.dotlottie.android)
}