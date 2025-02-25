plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    id("kotlin-parcelize")
    id("kotlinx-serialization")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.example.animeapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.animeapp"
        minSdk = 27
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
        viewBinding = true
    }
    hilt {
        enableAggregatingTask = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.ktx.v286)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Room
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

    // Navigation
    implementation(libs.androidx.navigation.fragment.ktx.v282)
    implementation(libs.androidx.navigation.ui.ktx.v282)

    // Glide
    implementation(libs.glide)
    ksp(libs.compiler)

    //PrettyTime
    implementation(libs.prettytime)

    //Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)

    //ViewModel injection
    kapt(libs.androidx.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.fragment)

    //Kotlinx Serialization
    implementation(libs.kotlinx.serialization.json)

    //Loading Skeleton
    implementation(libs.shimmer)

    //Splash screen
    implementation(libs.androidx.core.splashscreen)

    //Commons text
    implementation(libs.commons.text)

    //Chucker
    debugImplementation(libs.library)
    releaseImplementation(libs.library.no.op)

    // --- Testing Dependencies ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Mocking Libraries
    testImplementation(libs.mockito.core)
    implementation(libs.mockito.core)
    androidTestImplementation(libs.mockito.android)

    // --- Architecture Components Testing ---
    androidTestImplementation(libs.androidx.core.testing)

    // --- Coroutines Testing ---
    androidTestImplementation(libs.kotlinx.coroutines.test)

    testImplementation(libs.byte.buddy)
    testImplementation(libs.byte.buddy.agent)
    implementation(libs.androidx.multidex)
    androidTestImplementation(libs.mockwebserver)
}

kapt {
    correctErrorTypes = true
}
