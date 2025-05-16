plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.flam.arcade"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.flam.arcade"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"


        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug{
            isMinifyEnabled = false
            isShrinkResources = false
            buildConfigField("boolean", "IS_DEBUG", "true")
        }
        release{
            isMinifyEnabled = false
            isShrinkResources = false
            buildConfigField("boolean", "IS_DEBUG", "false")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    buildFeatures{
        buildConfig =true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.gorisse.thomas.sceneform:sceneform:1.23.0")
}