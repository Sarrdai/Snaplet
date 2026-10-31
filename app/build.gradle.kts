plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.roborazzi)
}

// Play releases: the release workflow sets RELEASE_VERSION from the tag (v1.2.3) and passes the upload key.
// Every other build: version 0.1.<CI run> signed with the throwaway key, so phones can update sideloaded APKs.
val releaseVersion = System.getenv("RELEASE_VERSION")?.removePrefix("v")
val runNumber = System.getenv("GITHUB_RUN_NUMBER")?.toInt() ?: 0
val uploadKeystore = System.getenv("UPLOAD_KEYSTORE_FILE")?.takeIf { it.isNotBlank() }

/** v1.2.3 -> 10203. Play only accepts growing version codes, so minor and patch stay below 100. */
fun versionCodeOf(version: String): Int {
    val parts = Regex("""(\d+)\.(\d{1,2})\.(\d{1,2})""").matchEntire(version)?.destructured?.toList()
        ?: error("RELEASE_VERSION must look like v1.2.3, got '$version'")
    val (major, minor, patch) = parts.map(String::toInt)
    return major * 10_000 + minor * 100 + patch
}

android {
    namespace = "app.tinycam"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.tinycam"
        minSdk = 26
        targetSdk = 36
        versionCode = releaseVersion?.let(::versionCodeOf) ?: maxOf(runNumber, 1)
        versionName = releaseVersion ?: "0.1.$runNumber"
    }

    signingConfigs {
        // Throwaway key committed on purpose so every CI build can update the installed app. Never use it for Play.
        create("throwaway") {
            storeFile = file("signing/throwaway.jks")
            storePassword = "tinycam-throwaway"
            keyAlias = "tinycam"
            keyPassword = "tinycam-throwaway"
        }
        // Play upload key from GitHub secrets (see TODO.md); Google re-signs the app with its own app signing key
        if (uploadKeystore != null) {
            create("upload") {
                storeFile = file(uploadKeystore)
                storePassword = System.getenv("UPLOAD_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("UPLOAD_KEY_ALIAS")
                keyPassword = System.getenv("UPLOAD_KEY_PASSWORD")
            }
        }
    }
    check(releaseVersion == null || uploadKeystore != null) { "A Play release (RELEASE_VERSION) needs the upload key" }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName(if (uploadKeystore != null) "upload" else "throwaway")
        }
        debug {
            signingConfig = signingConfigs.getByName("throwaway")
        }
    }

    buildFeatures {
        compose = true
    }

    // Lists the app's languages for Android 13+ per-app language settings (default language: res/resources.properties)
    androidResources {
        generateLocaleConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                // Robolectric downloads its Android runtime itself; use the Maven Central mirror
                it.systemProperty("robolectric.dependency.repo.url", "https://maven-central.storage-download.googleapis.com/maven2/")
                // Store screenshots: -PstoreCamera=/path/photo.jpg replaces the drawn camera scene (StoreScreenshotTest)
                providers.gradleProperty("storeCamera").orNull?.let { path -> it.systemProperty("store.camera", file(path).absolutePath) }
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.core)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.camera.camera2)
    implementation(libs.camera.extensions)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.video)
    implementation(libs.camera.view)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
}
