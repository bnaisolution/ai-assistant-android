plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "org.dpdns.bnai.assistant"
    compileSdk = 34

    defaultConfig {
        applicationId = "org.dpdns.bnai.assistant"
        minSdk = 26
        targetSdk = 34
        versionCode = 310
        versionName = "3.1.0"

        buildConfigField("String", "BASE_API_URL", "\"https://api.bnai.dpdns.org/\"")
        buildConfigField("String", "DEFAULT_USER_ID", "\"11111111-1111-1111-1111-111111111111\"")
        buildConfigField("String", "DEFAULT_WORKSPACE_ID", "\"11111111-1111-1111-1111-111111111111\"")
        buildConfigField("String", "ASSISTANT_PROJECT_ID", "\"11111111-1111-1111-1111-111111111111\"")
        buildConfigField("String", "CINEMATIC_PROJECT_ID", "\"33333333-3333-3333-3333-333333333333\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
}
