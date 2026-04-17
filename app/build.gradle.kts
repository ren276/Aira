import java.util.Properties

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

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { inputStream ->
        localProperties.load(inputStream)
    }
}

fun getLocalProperty(key: String): String {
    return localProperties.getProperty(key)
        ?: (project.findProperty(key) as? String)
        ?: System.getenv(key)
        ?: ""
}

fun getLocalPropertyOrDefault(key: String, defaultValue: String): String {
    val value = getLocalProperty(key)
    return if (value.isBlank()) defaultValue else value
}

android {
    namespace = "com.aira.health"
    compileSdk = 36
    compileSdkExtension = 19

    defaultConfig {
        applicationId = "com.aira.health"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "STRAVA_CLIENT_ID", "\"${getLocalProperty("STRAVA_CLIENT_ID")}\"")
        buildConfigField("String", "STRAVA_CLIENT_SECRET", "\"${getLocalProperty("STRAVA_CLIENT_SECRET")}\"")
        buildConfigField(
            "String",
            "STRAVA_REDIRECT_URI",
            "\"${getLocalPropertyOrDefault("STRAVA_REDIRECT_URI", "aira://strava-auth/callback")}\""
        )
    }

    // Product Flavors: dev / staging / prod
    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            buildConfigField("String", "SUPABASE_URL", "\"${getLocalProperty("SUPABASE_STAGING_URL")}\"")
            buildConfigField("String", "SUPABASE_ANON_KEY", "\"${getLocalProperty("SUPABASE_STAGING_ANON_KEY")}\"")
            buildConfigField("Boolean", "ENABLE_FLAG_SECURE", "false")
            buildConfigField("Boolean", "ENABLE_CRASH_REPORTING", "false")
        }
        create("staging") {
            dimension = "environment"
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            buildConfigField("String", "SUPABASE_URL", "\"${getLocalProperty("SUPABASE_STAGING_URL")}\"")
            buildConfigField("String", "SUPABASE_ANON_KEY", "\"${getLocalProperty("SUPABASE_STAGING_ANON_KEY")}\"")
            buildConfigField("Boolean", "ENABLE_FLAG_SECURE", "true")
            buildConfigField("Boolean", "ENABLE_CRASH_REPORTING", "true")
        }
        create("prod") {
            dimension = "environment"
            buildConfigField("String", "SUPABASE_URL", "\"${getLocalProperty("SUPABASE_PROD_URL")}\"")
            buildConfigField("String", "SUPABASE_ANON_KEY", "\"${getLocalProperty("SUPABASE_PROD_ANON_KEY")}\"")
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
        isCoreLibraryDesugaringEnabled = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/LICENSE.md"
            excludes += "/META-INF/LICENSE-notice.md"
        }
    }

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
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
    implementation(libs.material)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.activity)
    implementation(libs.compose.viewmodel)
    implementation(libs.compose.runtime.livedata)
    implementation(libs.compose.navigation)
    implementation(libs.compose.ui.google.fonts)

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
    implementation(libs.androidx.sqlite)
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
    implementation(libs.firebase.auth)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.perf)

    // Utilities
    implementation(libs.coil)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.datastore.preferences)
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.process)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    // ML & AI (declared now, model loaded at runtime)
    implementation(libs.mediapipe.genai)
    implementation(libs.tensorflow.lite)

    // Camera & Scanner (Phase 04 baseline)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.barcode)

    // Testing
    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation("junit:junit:4.13.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.11.3")
    testImplementation("org.robolectric:robolectric:4.12.1")
    testImplementation("androidx.test:core-ktx:1.5.0")
    testImplementation(libs.mockk)
    androidTestImplementation(libs.mockk.android)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
    androidTestImplementation(composeBom)
    androidTestImplementation(libs.compose.ui.test)
    debugImplementation(libs.compose.ui.test.manifest)
}

kotlin {
    jvmToolchain(17)
}
