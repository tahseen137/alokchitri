plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.alokchitri.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.alokchitri.app" // change before publishing
        minSdk = 26
        targetSdk = 36 // Play requires a target within a year of the newest Android
        versionCode = 1
        versionName = "0.1.0"
        // AdMob app ID, read by the manifest. Google's sample ID: replace with the real one (with the ad unit in AppConfig).
        manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

dependencies {
    implementation(project(":engine"))

    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended") // shrunk by R8 in release
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0") // per-app language (Bangla / English toggle)
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-service:2.8.7")   // for the overlay's lifecycle owner
    implementation("androidx.savedstate:savedstate-ktx:1.2.1")
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")            // How-to screenshots
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.3.0")
    implementation("androidx.exifinterface:exifinterface:1.3.7")    // settings saved in the photo (Feedback)
    implementation("com.android.billingclient:billing-ktx:8.0.0")   // lifetime unlock + feedback top-up
    implementation("com.google.android.gms:play-services-ads:24.4.0") // rewarded ad for one extra feedback
    // The ads SDK pulls WorkManager 2.7 (Room 2.2), whose database R8 strips in release builds: crash at start.
    implementation("androidx.work:work-runtime:2.11.2")
    testImplementation(kotlin("test-junit")) // JUnit 4 runner for Android unit tests
    testImplementation("org.json:json:20240303") // the real org.json; android.jar only has stubs on the JVM

}
