plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.vidora.player"

    compileSdk = 36

    defaultConfig {
        applicationId = "com.vidora.player"

        minSdk = 23
        targetSdk = 34

        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }
}

dependencies {

    implementation(
        "androidx.core:core-ktx:1.18.0"
    )

    implementation(
        "androidx.activity:activity-ktx:1.13.0"
    )

    implementation(
        "androidx.media3:media3-exoplayer:1.11.0"
    )

    implementation(
        "androidx.media3:media3-ui:1.11.0"
    )

    implementation(
        "androidx.media3:media3-session:1.11.0"
    )

    implementation(
        "androidx.media3:media3-exoplayer-hls:1.11.0"
    )

    implementation(
        "androidx.media3:media3-exoplayer-dash:1.11.0"
    )
}
