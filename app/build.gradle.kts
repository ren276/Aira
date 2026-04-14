plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics.plugin)
    alias(libs.plugins.firebase.perf.plugin)
}

android {
    namespace = "com.aira.health"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aira.health"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Product Flavors: dev / staging / prod
    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            buildConfigField("String", "SUPABASE_URL", "\"${project.findProperty("SUPABASE_STAGING_URL") ?: ""}\"")
            buildConfigField("String", "SUPABASE_ANON_KEY", "\"${project.findProperty("SUPABASE_STAGING_ANON_KEY") ?: ""}\"")
            buildConfigField("Boolean", "ENABLE_FLAG_SECURE", "false")
            buildConfigField("Boolean", "ENABLE_CRASH_REPORTING", "false")
        }
        create("staging") {
            dimension = "environment"
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            buildConfigField("String", "SUPABASE_URL", "\"${project.findProperty("SUPABASE_STAGING_URL") ?: ""}\"")
            buildConfigField("String", "SUPABASE_ANON_KEY", "\"${project.findProperty("SUPABASE_STAGING_ANON_KEY") ?: ""}\"")
            buildConfigField("Boolean", "ENABLE_FLAG_SECURE", "true")
            buildConfigField("Boolean", "ENABLE_CRASH_REPORTING", "true")
        }
        create("prod") {
            dimension = "environment"
            buildConfigField("String", "SUPABASE_URL", "\"${project.findProperty("SUPABASE_PROD_URL") ?: ""}\"")
            buildConfigField("String", "SUPABASE_ANON_KEY", "\"${project.findProperty("SUPABASE_PROD_ANON_KEY") ?: ""}\"")
            buildConfigField("Boolean", "ENABLE_FLAG_SECURE", "true")
            buildConfigField("Boolean", "ENABLE_CRASH_REPORTING", "true")
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Compose
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.activity)
    implementation(libs.compose.viewmodel)
    implementation(libs.compose.runtime.livedata)
    implementation(libs.compose.navigation)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // Room + SQLCipher
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.sqlcipher)
    implementation(libs.security.crypto)

    // WorkManager
    implementation(libs.work.runtime)

    // Supabase
    val supabaseBom = platform(libs.supabase.bom)
    implementation(supabaseBom)
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.realtime)
    implementation(libs.supabase.storage)
    implementation(libs.ktor.client.android)

    // Health & Fitness
    implementation(libs.health.connect)
    implementation(libs.play.services.fitness)
    implementation(libs.play.services.auth)

    // Coroutines
    implementation(libs.coroutines.android)
    implementation(libs.coroutines.play.services)

    // Security
    implementation(libs.biometric)

    // Monetisation
    implementation(libs.revenuecat)

    // Firebase
    val firebaseBom = platform(libs.firebase.bom)
    implementation(firebaseBom)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.perf)

    // Utilities
    implementation(libs.coil)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.datastore.preferences)
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.process)

    // ML & AI (declared now, model loaded at runtime)
    implementation(libs.mediapipe.genai)
    implementation(libs.tensorflow.lite)
    implementation(libs.tensorflow.lite.support)

    // Testing
    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
    androidTestImplementation(composeBom)
    androidTestImplementation(libs.compose.ui.test)
    debugImplementation(libs.compose.ui.test.manifest)
}
