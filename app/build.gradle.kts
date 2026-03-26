import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.tony.pcremote"

    compileSdk = 35

    defaultConfig {
        applicationId = "com.tony.pcremote"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "1.6"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // Priority: Environment Variables (CI) > keystore.properties (Local)
            val sFile = System.getenv("RELEASE_STORE_FILE") ?: keystoreProperties["storeFile"]?.toString() ?: "release.keystore"
            val sPass = System.getenv("RELEASE_STORE_PASSWORD") ?: keystoreProperties["storePassword"]?.toString()
            val kAlias = System.getenv("RELEASE_KEY_ALIAS") ?: keystoreProperties["keyAlias"]?.toString()
            val kPass = System.getenv("RELEASE_KEY_PASSWORD") ?: keystoreProperties["keyPassword"]?.toString()

            if (sPass != null && kAlias != null && kPass != null) {
                storeFile = file(sFile)
                storePassword = sPass
                keyAlias = kAlias
                keyPassword = kPass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            
            val sFile = System.getenv("RELEASE_STORE_FILE") ?: keystoreProperties["storeFile"]?.toString() ?: "release.keystore"
            if (file(sFile).exists()) {
                signingConfig = signingConfigs.getByName("release")
            }

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
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
