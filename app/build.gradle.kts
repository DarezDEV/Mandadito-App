plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// Función para leer propiedades desde local.properties
fun getLocalProperty(key: String): String {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.readText().lines().forEach { line ->
            if (line.startsWith("$key=")) {
                return line.substring("$key=".length)
            }
        }
    }
    return ""
}

android {
    namespace = "com.dev.mandadito"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.dev.mandadito"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // lee los valores del proyecto (local.properties)
        val supabaseUrl: String = getLocalProperty("SUPABASE_URL")
        val supabaseAnonKey: String = getLocalProperty("SUPABASE_ANON_KEY")

        // expone en BuildConfig
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    buildTypes {

        debug {
            // Permitir HTTP solo en debug
            manifestPlaceholders["cleartextTrafficPermitted"] = "true"
        }

        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            manifestPlaceholders["cleartextTrafficPermitted"] = "false"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
        )
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.navigation.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Supabase
    implementation(libs.supabase.kt)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.auth)
    implementation(libs.supabase.storage)
    implementation("io.github.jan-tennert.supabase:functions-kt:3.0.0")
    implementation("io.github.jan-tennert.supabase:realtime-kt:3.0.0") // Para actualizaciones en tiempo real

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // Ktor Client (requerido por Supabase y para llamadas HTTP)
    // IMPORTANTE: Usar okhttp en lugar de android para soporte de WebSockets (Realtime)
    implementation("io.ktor:ktor-client-okhttp:3.0.0")
    implementation("io.ktor:ktor-client-content-negotiation:3.0.0")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.0")

    // Iconos de Compose
    implementation("androidx.compose.material:material-icons-extended")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // Accompanist Pager para el slider de imágenes
    implementation("com.google.accompanist:accompanist-pager:0.30.1")
    implementation("com.google.accompanist:accompanist-pager-indicators:0.30.1")

    // Coil para cargar imágenes (si aún no lo tienes)
    implementation("io.coil-kt:coil-compose:2.4.0")

    // Room Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Stripe Android SDK (actualizado a la última versión compatible con Compose 2024.10.00)
    implementation("com.stripe:stripe-android:20.52.3")

    // Coroutines (si no las tienes)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Retrofit (para llamadas HTTP al backend)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
}