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

        buildConfigField(
            "String",
            "TAPSELL_KEY",
            "\"hbnepqhlndllgpkhnsegfhinnarknqkcktcetkcetjtesdoigpagnbbhegffsahpqtkgaqn\""
        )

        buildConfigField(
            "String",
            "TAPSELL_INTERSTITIAL",
            "\"6aa818c2cd33cd4ed6e4327e\""
        )

        buildConfigField(
            "String",
            "TAPSELL_REWARDED",
            "\"6aa81945cd33cd4ed6e4327f\""
        )

        buildConfigField(
            "String",
            "ADIVERY_APP_ID",
            "\"6a191640-20fa-4e99-b75b-454972c897e3\""
        )

        buildConfigField(
            "String",
            "ADIVERY_INTERSTITIAL",
            "\"af93d446-1ef9-4e4c-b175-9938060ea971\""
        )

        buildConfigField(
            "String",
            "ADIVERY_REWARDED",
            "\"5fe5acbc-01b6-4988-a841-c626a0bdb08b\""
        )
    }

    signingConfigs {
        create("release") {
            val keystorePath =
                System.getenv("VIDORA_KEYSTORE_PATH")

            val keystorePassword =
                System.getenv("VIDORA_KEYSTORE_PASSWORD")

            val keyAlias =
                System.getenv("VIDORA_KEY_ALIAS")

            val keyPassword =
                System.getenv("VIDORA_KEY_PASSWORD")

            if (
                !keystorePath.isNullOrBlank() &&
                !keystorePassword.isNullOrBlank() &&
                !keyAlias.isNullOrBlank() &&
                !keyPassword.isNullOrBlank()
            ) {
                storeFile = file(keystorePath)
                storeType = "PKCS12"
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true

            signingConfig =
                signingConfigs.getByName("release")

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

    buildFeatures {
        buildConfig = true
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
        "androidx.fragment:fragment-ktx:1.8.9"
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

    implementation(
        "androidx.media3:media3-cast:1.11.0"
    )

    implementation(
        "ir.tapsell.plus:tapsell-plus-sdk-android:2.3.3"
    )

    implementation(
        "com.adivery:sdk:4.9.0"
    )

    implementation(
        "com.google.android.gms:play-services-base:18.5.0"
    )

    implementation(
        "com.google.android.gms:play-services-ads-identifier:18.1.0"
    )
}
