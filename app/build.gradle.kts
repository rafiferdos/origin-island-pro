import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
}

// ktlint runs in CI on every push. Every rule stays ON; the pre-existing tree's ~176 violations
// are recorded in app/config/ktlint/baseline.xml, which is the mechanism ktlint provides for
// exactly this situation. A baseline blocks only the violations it lists, at the exact file/line/
// rule they were recorded at — so a NEW violation anywhere fails the build, and a violation that
// moves (because a line was edited) also fails, because the recorded line number no longer matches.
//
// To regenerate after a deliberate style change: `./gradlew :app:ktlintGenerateBaseline`.
ktlint {
    android.set(true)
    ignoreFailures.set(false)
    filter {
        // Only exclude generated / vendored sources.
        exclude("**/build/**")
    }
}

// Release signing is optional and driven by a local, gitignored keystore.properties (see README).
// Its absence must not break a fresh clone's `assembleDebug` — only assembleRelease needs it, and
// even that build still succeeds unsigned if the file is missing.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps =
    Properties().apply {
        if (keystorePropsFile.exists()) {
            keystorePropsFile.inputStream().use { load(it) }
        }
    }

android {
    // R class / generated sources live under this package. (Distinct from applicationId below —
    // this was renamed away from the decompiled APK's artifact package name.)
    namespace = "com.originisle.android"

    // compileSdk 36 matches the platform installed on this machine (android-36). The original
    // targeted 37 (Android 17 preview); if you install the 37 platform + a matching AGP, bump.
    compileSdk = 36

    defaultConfig {
        // IMPORTANT: this applicationId is load-bearing. vivo OriginOS only honours
        // setSuperXInfosSceneList for whitelisted packages; the original author spoofed
        // AMap (com.autonavi.minimap) to get onto that whitelist. Change it and the island
        // access will likely stop working on your X200 Pro.
        applicationId = "com.autonavi.minimap"
        minSdk = 34
        targetSdk = 36
        versionCode = 55
        versionName = "2.34.0"
    }

    signingConfigs {
        if (keystorePropsFile.exists()) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Falls back to unsigned when no keystore.properties is present (e.g. a fresh clone),
            // so `assembleRelease` never hard-fails on a missing local secret.
            if (keystorePropsFile.exists()) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
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
    implementation(libs.androidx.appcompat)
    // Provides the Theme.Material3.* XML theme parents used in themes.xml (not directly imported
    // in Kotlin, but required for the manifest/activity theme resource to resolve).
    implementation(libs.google.material)

    testImplementation(libs.junit)
    debugImplementation(libs.androidx.ui.tooling)
}
