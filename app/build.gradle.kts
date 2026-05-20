import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.msu.mfalocker"
    compileSdk = 34

    viewBinding {
        enable = true
    }

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.msu.mfalocker"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // LLM credentials — set LLM_API_KEY and LLM_BASE_URL in local.properties
        // Force Gradle rebuild to pick up local.properties changes
        val localProps = Properties().also { props ->
            val f = rootProject.file("local.properties")
            if (f.exists()) props.load(f.inputStream())
        }
        buildConfigField("String", "LLM_BASE_URL", "\"${localProps.getProperty("LLM_BASE_URL",)}\"")
        buildConfigField("String", "LLM_API_KEY", "\"${localProps.getProperty("LLM_API_KEY", "")}\"")
        buildConfigField("String", "LLM_MODEL", "\"${localProps.getProperty("LLM_MODEL",)}\"")

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
    kotlinOptions {
        jvmTarget = "1.8"
    }
    testOptions {
        unitTests {
            isReturnDefaultValues = true
            all {
                it.useJUnitPlatform()
            }
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("com.github.Dimezis:BlurView:version-2.0.5")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.biometric:biometric:1.1.0")
    testImplementation("io.kotest:kotest-runner-junit5:5.8.1")
    testImplementation("io.kotest:kotest-property:5.8.1")
    testImplementation("org.json:json:20231013")
}
