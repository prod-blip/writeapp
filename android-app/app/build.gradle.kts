import java.util.Properties
import java.util.Base64

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

val releaseAiBackendUrl = providers.gradleProperty("CLEARWRITE_AI_BACKEND_URL").orElse("").get()
val escapedReleaseAiBackendUrl = releaseAiBackendUrl.replace("\\", "\\\\").replace("\"", "\\\"")
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.atulpandey.clearwrite"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.atulpandey.clearwrite"
        minSdk = 26
        targetSdk = 36
        versionCode = 8
        versionName = "1.0.7"
        buildConfigField("String", "PRO_PRODUCT_ID", "\"clearwrite_pro\"")
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = String(Base64.getDecoder().decode(keystoreProperties.getProperty("storePasswordBase64")), Charsets.UTF_8)
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = String(Base64.getDecoder().decode(keystoreProperties.getProperty("keyPasswordBase64")), Charsets.UTF_8)
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "AI_BACKEND_URL", "\"http://10.0.2.2:8787\"")
            buildConfigField("String", "AI_DEV_TOKEN", "\"clearwrite-local-development\"")
            buildConfigField("boolean", "AI_DEVELOPMENT_ACCESS", "true")
        }
        release {
            isMinifyEnabled = false
            buildConfigField("String", "AI_BACKEND_URL", "\"$escapedReleaseAiBackendUrl\"")
            buildConfigField("String", "AI_DEV_TOKEN", "\"\"")
            buildConfigField("boolean", "AI_DEVELOPMENT_ACCESS", "false")
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = true
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.core.splashscreen)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.play.billing)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)
}
