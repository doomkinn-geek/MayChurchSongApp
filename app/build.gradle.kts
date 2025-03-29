plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    id("com.google.devtools.ksp") version "1.9.22-1.0.16"
    id("kotlin-parcelize")
}

android {
    namespace = "ru.maychurch.maychurchsong"
    compileSdk = 35

    defaultConfig {
        applicationId = "ru.maychurch.maychurchsong"
        minSdk = 24
        targetSdk = 35
        versionCode = 5
        versionName = "1.3.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            // Эти значения будут задаваться через переменные окружения или local.properties
            // для безопасности ключей
            val keystoreFile = project.findProperty("RELEASE_STORE_FILE") as String? ?: "keystore/release-key.jks"
            storeFile = file("${rootProject.projectDir}/${keystoreFile}")
            storePassword = project.findProperty("RELEASE_STORE_PASSWORD") as String? ?: "changeit"
            keyAlias = project.findProperty("RELEASE_KEY_ALIAS") as String? ?: "key0"
            keyPassword = project.findProperty("RELEASE_KEY_PASSWORD") as String? ?: "changeit"
        }
    }
    
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE*"
            excludes += "/META-INF/NOTICE*"
            excludes += "/*.txt"
            excludes += "/META-INF/*.kotlin_module"
            excludes += "/*.properties"
            excludes += "/org/jspecify/annotations/**"
        }
    }
}

dependencies {
    // Core и основные библиотеки Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    
    // WorkManager для периодических задач
    implementation("androidx.work:work-runtime-ktx:2.8.1")
    
    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    
    // Навигация
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    
    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    
    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("androidx.room:room-common:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    
    // Kotlin Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    
    // Сетевые запросы
    implementation("io.ktor:ktor-client-core:2.3.7") {
        exclude("org.jetbrains.kotlinx", "kotlinx-coroutines-debug")
    }
    implementation("io.ktor:ktor-client-android:2.3.7")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
    implementation("org.jsoup:jsoup:1.17.2") // Для парсинга HTML
    
    // Preferences DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Coil для загрузки изображений
    implementation("io.coil-kt:coil-compose:2.5.0")
    
    // JSpecify аннотации (для JSoup)
    compileOnly("org.jspecify:jspecify:0.3.0")
    
    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
}