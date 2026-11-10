import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.time.Duration


plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.alhadithi.iqshia"
    minSdk = 24
    targetSdk = 36
    versionCode = 20
    versionName = "1.0.18"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  // Release signing is supplied by the build environment. Debug builds use the
  // Android Gradle Plugin's standard per-user debug keystore.
  val releaseKeystorePath = System.getenv("KEYSTORE_PATH")
  val releaseStorePassword = System.getenv("STORE_PASSWORD")
  val releaseKeyPassword = System.getenv("KEY_PASSWORD")
  val releaseKeyAlias = System.getenv("KEY_ALIAS") ?: "upload"
  if (!releaseKeystorePath.isNullOrBlank() &&
      !releaseStorePassword.isNullOrBlank() &&
      !releaseKeyPassword.isNullOrBlank() &&
      file(releaseKeystorePath).isFile
  ) {
    signingConfigs {
      create("release") {
        storeFile = file(releaseKeystorePath)
        storePassword = releaseStorePassword
        keyAlias = releaseKeyAlias
        keyPassword = releaseKeyPassword
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isDebuggable = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      // Release signing is mandatory: verifyReleaseSigningInputs fails packaging if the
      // owner's existing keystore and credentials are not supplied by the build environment.
      if (signingConfigs.findByName("release") != null) {
        signingConfig = signingConfigs.getByName("release")
      }
    }
    debug {
      // The Android Gradle Plugin applies its standard local debug signing configuration.
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.media3.common)
  implementation(libs.androidx.media3.exoplayer)
  implementation(libs.androidx.media3.session)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  implementation(libs.firebase.firestore)
  implementation(libs.firebase.auth)
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services)
  implementation(libs.googleid)
  implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.firebase.appcheck.debug)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  // implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  // "ksp"(libs.moshi.kotlin.codegen)
}

// Never allow a distribution Release task to silently emit an unsigned APK.
// The existing upload key must be supplied by the owner/build environment; this project does
// not generate, rotate, or embed signing credentials.
val verifyReleaseSigningInputs = tasks.register("verifyReleaseSigningInputs") {
  group = "verification"
  description = "Fails Release packaging unless the existing private signing key is configured."
  doLast {
    val required = mapOf(
      "KEYSTORE_PATH" to System.getenv("KEYSTORE_PATH"),
      "STORE_PASSWORD" to System.getenv("STORE_PASSWORD"),
      "KEY_PASSWORD" to System.getenv("KEY_PASSWORD")
    )
    val missing = required.filter { (_, value) -> value.isNullOrBlank() }.keys
    val keystorePath = required["KEYSTORE_PATH"]
    val keystore = if (keystorePath.isNullOrBlank()) null else project.file(keystorePath)
    val keystoreReadable = keystore?.let { it.isFile && it.canRead() } == true
    if (missing.isNotEmpty() || !keystoreReadable) {
      val details = buildList {
        if (missing.isNotEmpty()) add("المتغيرات غير المضبوطة: ${missing.joinToString()}")
        if (!keystoreReadable) add("ملف KEYSTORE_PATH غير موجود أو غير قابل للقراءة")
      }.joinToString("؛ ")
      throw GradleException(
        "تم إيقاف Release قبل إنتاج APK غير موقّع. وفّر مفتاح التوقيع الأصلي عبر متغيرات البيئة فقط. $details"
      )
    }
  }
}

tasks.configureEach {
  if (name == "packageRelease" || name == "assembleRelease" || name == "bundleRelease") {
    dependsOn(verifyReleaseSigningInputs)
  }
}

tasks.withType<Test> {
  // Run every test even when one fails, so a single run reports the complete picture instead
  // of hiding the remaining problems behind the first failure.
  failFast = false
  // Robolectric boots a full Android runtime, which is slow on small machines.
  timeout.set(Duration.ofMinutes(15))
  testLogging {
    events("passed", "failed", "skipped")
  }
}

