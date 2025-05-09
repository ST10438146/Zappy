plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)


}

android {
    namespace = "vcmsa.projects.zappy"
    compileSdk = 35

    defaultConfig {
        applicationId = "vcmsa.projects.zappy"
        minSdk = 32
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.androidx.camera.view)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // Firebase Authentication
    implementation("com.google.firebase:firebase-auth-ktx:23.2.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")


    // Firestore
    implementation("com.google.firebase:firebase-firestore-ktx:25.1.3")

    // CameraX
    implementation("androidx.camera:camera-core:1.4.0-alpha01")
    implementation("androidx.camera:camera-camera2:1.4.0-alpha01")
    implementation("androidx.camera:camera-lifecycle:1.4.0-alpha01")
    implementation("androidx.camera:camera-viewfinder:1.4.0-alpha01")
    implementation("androidx.camera:camera-extensions:1.4.0-alpha01")

    // Coil for image loading (or Picasso/Glide)
    implementation("io.coil-kt:coil:2.6.0")

    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Coroutines for asynchronous tasks
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

    // Optional: Navigation Component
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // CameraX core library
    implementation("androidx.camera:camera-core:1.3.1")
    // CameraX camera2 extensions
    implementation("androidx.camera:camera-camera2:1.3.1")
    // CameraX lifecycle extensions
    implementation("androidx.camera:camera-lifecycle:1.3.1")

    // Kotlin coroutines for CameraX
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Material Design for Bottom Navigation
    implementation("com.google.android.material:material:1.11.0")

    // Glide for image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Firebase Storage for image uploads
    implementation("com.google.firebase:firebase-storage-ktx")


}