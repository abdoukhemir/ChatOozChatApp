plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.kapt")// Google Services plugin
}

android {
    namespace = "com.codewithfk.chatooz_android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.codewithfk.chatooz_android"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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

    buildFeatures {
        viewBinding = true
        compose = true
        dataBinding = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/*"
        }
    }
}

dependencies {

    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1") // Use a recent stable version
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")
    implementation ("com.cloudinary:cloudinary-android:2.+") // Use the latest version if needed
    implementation ("com.cloudinary:cloudinary-android-core:2.+")
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.activity:activity-ktx:1.8.2")

    // Firebase BoM (Bill of Materials)
    implementation(platform("com.google.firebase:firebase-bom:32.8.0"))

    // Firebase dependencies (without versions - uses BoM version)
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation ("com.google.firebase:firebase-database:21.0.0")

    // Compose
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation(libs.firebase.database)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.androidx.activity)

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Zegocloud Chat
    implementation("com.github.ZEGOCLOUD:zego_inapp_chat_uikit_android:+")
    implementation("com.google.code.gson:gson:2.10.1")

    // Data Binding
    implementation("com.android.databinding:compiler:3.5.1")
    implementation ("androidx.cardview:cardview:1.0.0")
    implementation ("com.github.bumptech.glide:glide:4.16.0") // Use the latest version if needed
    annotationProcessor ("com.github.bumptech.glide:compiler:4.16.0")
    implementation ("androidx.credentials:credentials:1.5.0")
    implementation ("androidx.credentials:credentials-play-services-auth:1.5.0")
    implementation ("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation ("id.zelory:compressor:3.0.1")
}
