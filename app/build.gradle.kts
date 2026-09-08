plugins {
    id("com.android.application")
    id("androidx.baselineprofile")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("com.android.compose.screenshot")
}

android {
    namespace = "com.worktime.app"
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "com.worktime.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 2
        versionName = "0.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    experimentalProperties["android.experimental.enableScreenshotTest"] = true

    val releaseStoreFile = providers.gradleProperty("releaseStoreFile").orNull
        ?: System.getenv("RELEASE_STORE_FILE")
    val releaseStorePassword = providers.gradleProperty("releaseStorePassword").orNull
        ?: System.getenv("RELEASE_STORE_PASSWORD")
    val releaseKeyAlias = providers.gradleProperty("releaseKeyAlias").orNull
        ?: System.getenv("RELEASE_KEY_ALIAS")
    val releaseKeyPassword = providers.gradleProperty("releaseKeyPassword").orNull
        ?: System.getenv("RELEASE_KEY_PASSWORD")

    if (listOf(releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword).all { it != null }) {
        signingConfigs.create("production") {
            storeFile = file(releaseStoreFile!!)
            storePassword = releaseStorePassword!!
            keyAlias = releaseKeyAlias!!
            keyPassword = releaseKeyPassword!!
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        getByName("debug") {
            // Keep development/instrumentation installs isolated from the real app package.
            // connectedDebugAndroidTest may uninstall its tested APK during cleanup, so a
            // dedicated application id prevents a physical QA run from deleting user data.
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }

        release {
            // Production signing is opt-in through RELEASE_* properties/env vars.
            // Without them Gradle produces an unsigned release artifact, never a
            // misleading debug-signed production build.
            signingConfig = signingConfigs.findByName("production")

            // AGP 9.3 optimization DSL enables R8 code optimization and optimized
            // resource shrinking together for the release variant.
            optimization {
                enable = true
            }
            proguardFiles("proguard-rules.pro")
        }

        create("nonMinifiedRelease") {
            // Baseline Profile capture must preserve source-level class and method names.
            // With AGP 9.x, disable the new optimization DSL explicitly in addition to
            // the Baseline Profile plugin's non-minified variant overrides.
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            optimization {
                enable = false
            }
            matchingFallbacks += listOf("release")
        }

        create("benchmark") {
            // Macrobenchmark must measure release-like code without touching the installed
            // production package on a physical QA device. The benchmark application id is
            // disposable and independently debug-signed.
            initWith(getByName("release"))
            applicationIdSuffix = ".benchmark"
            versionNameSuffix = "-benchmark"
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
        managedDevices {
            localDevices {
                create("pixel2Api30") {
                    device = "Pixel 2"
                    apiLevel = 30
                    systemImageSource = "aosp-atd"
                    testedAbi = "x86"
                }
                create("pixel6Api37") {
                    device = "Pixel 6"
                    apiLevel = 37
                    systemImageSource = "google"
                    testedAbi = "x86_64"
                }
            }
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    val composeBom = platform(libs.compose.bom)

    baselineProfile(project(":baselineprofile"))

    implementation(composeBom)
    androidTestImplementation(composeBom)
    screenshotTestImplementation(composeBom)

    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.navigation3)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material3.adaptive)
    implementation(libs.compose.material3.adaptive.layout)
    implementation(libs.compose.material.icons.core)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.profileinstaller)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.datastore.preferences)
    implementation(libs.coroutines.android)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    screenshotTestImplementation(libs.compose.screenshot.validation.api)
    screenshotTestImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.json)

    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.coroutines.test)
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.room.testing)
}
