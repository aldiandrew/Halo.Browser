plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.aldiandrew.halobrowser"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.aldiandrew.halobrowser"
        minSdk = 29
        targetSdk = 36
        versionCode = 2
        versionName = "1.1"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.5.0-alpha29")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.dynamicanimation:dynamicanimation:1.0.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
