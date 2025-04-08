plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.jlsoft.firstviewwatch"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.jlsoft.firstviewwatch"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
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
    // Compose BOM for consistent Compose library versions.
    implementation(platform(libs.compose.bom))
    implementation(libs.appcompat)
    androidTestImplementation(platform(libs.compose.bom))

    // --- Core Compose UI Libraries ---
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.compose.material)
    implementation(libs.compose.foundation)

    // --- Activity & Navigation ---
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material:material:1.4.3")
    implementation("androidx.wear:wear:1.3.0" )
    implementation("androidx.wear:wear-ongoing:1.0.0" )
    implementation("com.google.maps.android:maps-compose:2.11.4")


    // --- Wear OS & Horologist Libraries ---
    implementation(libs.play.services.wearable)
    implementation(libs.wear.tooling.preview)
    implementation(libs.tiles)
    implementation(libs.tiles.material)
    implementation(libs.tiles.tooling.preview)
    implementation(libs.horologist.compose.tools)
    implementation(libs.horologist.tiles)
    implementation(libs.watchface.complications.data.source.ktx)
    implementation(libs.compose.material)
    implementation(libs.compose.foundation)

    // --- Core Android Libraries ---
    implementation(libs.core.splashscreen)

    implementation(libs.lifecycle.runtime.ktx)

    // --- Networking & Maps ---
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.play.services.maps)
    implementation(libs.logging.interceptor)

    // --- Testing ---
    androidTestImplementation(libs.ui.test.junit4)

    // --- Debug Tooling ---
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
    debugImplementation(libs.tiles.tooling)
    implementation("androidx.preference:preference:1.2.0")


}