plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "dev.thingscompanion.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.thingscompanion.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 28
        versionName = "0.5.3"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Published builds are release builds, so not debuggable: no adb run-as into the app's data.
            // They keep the key every earlier APK was signed with, so an update still installs over them.
            signingConfig = signingConfigs.getByName("debug")
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

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":thingscloud"))

    val composeBom = platform("androidx.compose:compose-bom:2025.09.00")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    // Long-press drag to reorder rows of a LazyColumn.
    implementation("sh.calvin.reorderable:reorderable:2.5.1")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.work:work-runtime-ktx:2.10.3")
    implementation("androidx.glance:glance-material3:1.1.1")
}
