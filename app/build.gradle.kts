@file:Suppress("UnstableApiUsage")

import com.android.build.api.variant.FilterConfiguration.FilterType.ABI
import java.util.Properties

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(localPropertiesFile.reader())
    }
}

val abiCodes = mapOf("x86" to 1, "x86_64" to 2, "armeabi-v7a" to 3, "arm64-v8a" to 4)

plugins {
    alias(libs.plugins.about.libraries)
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt.gradle)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.spotless)
    alias(libs.plugins.baselineprofile)
}

fun getAbi() = if (hasProperty("abi")) {
    property("abi").toString()
} else {
    null
}

android {
    namespace = "com.ammar.wallflow"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ammar.wallflow"
        minSdk = 24
        targetSdk = 34
        versionCode = 22
        versionName = "2.6.0-alpha01"

        val abi = getAbi()
        ndk {
            if (abi != null) {
                abiFilters += abi
            }
        }

        testInstrumentationRunner = "com.ammar.wallflow.HiltTestRunner"

        ksp {
            arg("room.generateKotlin", "true")
            arg("compose-destinations.codeGenPackageName", "com.ammar.wallflow")
        }
    }

    signingConfigs {
        if (!hasProperty("github") && !hasProperty("fdroid")) {
            create("release") {
                storeFile = file(localProperties.getProperty("release.jks.file", ""))
                storePassword = localProperties.getProperty("release.jks.password", "")
                keyAlias = localProperties.getProperty("release.jks.key.alias", "")
                keyPassword = localProperties.getProperty("release.jks.key.password", "")
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-dev"
            isDebuggable = true
        }

        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (!hasProperty("github") && !hasProperty("fdroid")) {
                signingConfig = signingConfigs.getByName("release")
            }
        }

        create("alpha") {
            initWith(getByName("release"))
        }
    }

    flavorDimensions += "feature"
    productFlavors {
        create("base") {
            dimension = "feature"
        }

        create("plus") {
            dimension = "feature"
            applicationIdSuffix = ".plus"
        }
    }

    splits {
        // Configures multiple APKs based on ABI.
        abi {
            // Enables building multiple APKs per ABI.
            isEnable = !hasProperty("fdroid")
                && !hasProperty("noSplits")
                && gradle.startParameter.taskNames.isNotEmpty()
                && gradle.startParameter.taskNames.any {
                it.contains("Release") || it.contains("Alpha")
            }

            // Resets the list of ABIs that Gradle should create APKs for to none.
            reset()

            // Specifies a list of ABIs that Gradle should create APKs for.
            include("x86", "x86_64", "arm64-v8a", "armeabi-v7a")

            // Specifies that we want to also generate a universal APK that includes all ABIs.
            isUniversalApk = false
        }
    }

    androidComponents {
        onVariants { variant ->
            variant.outputs.forEach { output ->
                val abi = if (hasProperty("fdroid")) {
                    getAbi()
                } else if (project.android.splits.abi.isEnable) {
                    output.filters.find { it.filterType == ABI }?.identifier
                } else {
                    null
                }
                if (abi != null) {
                    val baseAbiCode = abiCodes[abi]
                    if (baseAbiCode != null) {
                        output.versionCode.set(baseAbiCode + (output.versionCode.get() ?: 0) * 100)
                    }
                }
            }
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        aidl = false
        buildConfig = true
        renderScript = false
        shaders = false
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.androidxComposeCompiler.get()
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            merges += "META-INF/LICENSE.md"
            merges += "META-INF/LICENSE-notice.md"
            merges += "META-INF/versions/9/previous-compilation-data.bin"
        }
    }

    kotlin {
        jvmToolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }

        this.sourceSets {
            debug {
                kotlin.srcDir("build/generated/ksp/debug/kotlin")
            }
            release {
                kotlin.srcDir("build/generated/ksp/release/kotlin")
            }
        }
    }

    lint {
        warning += "AutoboxingStateCreation"
    }

    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }

    androidResources {
        generateLocaleConfig = true
    }
}

room {
    schemaDirectory("$projectDir/schemas/")
}

baselineProfile {
    mergeIntoMain = true
}

val plusImplementation by configurations

dependencies {
    coreLibraryDesugaring(libs.android.tools.desugar)

    // Core Android dependencies
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.service)

    // Hilt Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    ksp(libs.androidx.hilt.compiler)
    // Hilt and instrumented tests.
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.android.compiler)
    // Hilt and Robolectric tests.
    testImplementation(libs.hilt.android.testing)
    kspTest(libs.hilt.android.compiler)

    // Arch Components
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    androidTestImplementation(libs.androidx.room.testing)

    // Compose
    implementation(libs.androidx.compose.adaptive.layout)
    implementation(libs.androidx.compose.adaptive.navigation)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.window.size.cls)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.ui.util)
    // Tooling
    debugImplementation(libs.androidx.compose.ui.tooling)
    // Compose Runtime tracing
    debugImplementation(libs.androidx.compose.runtime.tracing)
    // Instrumented tests
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Compose Destinations
    implementation(libs.compose.destinations.core)
    ksp(libs.compose.destinations.ksp)
    androidTestImplementation(libs.androidx.navigation.testing)
    constraints {
        implementation(libs.androidx.navigation.compose)
    }

    // Retrofit
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization)

    // Paging
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.room.paging)

    // Coil
    implementation(libs.coil.compose)

    // Accompanist
    implementation(libs.accompanist.adaptive)

    // jsoup
    implementation(libs.jsoup)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Work
    implementation(libs.androidx.work.ktx)
    implementation(libs.androidx.hilt.work)
    androidTestImplementation(libs.androidx.work.testing)

    // easycrop
    // implementation(libs.easycrop)
    implementation(libs.easycrop.fork)

    // tf-lite
    plusImplementation(libs.tflite.task.vision)
    plusImplementation(libs.tflite.gpu.delegate.plugin)
    plusImplementation(libs.tflite.gpu)
    plusImplementation(libs.tflite.gpu.api)

    // partial
    implementation(libs.partial)
    ksp(libs.partial.ksp)

    // cloudy
    implementation(libs.cloudy)

    // telephoto
    implementation(libs.telephoto.zoomable.image.coil)

    // kotlinx collections immutable
    implementation(libs.kotlinx.collections.immutable)

    // About libraries
    implementation(libs.about.libraries.core)

    // DocumentFileCompat
    implementation(libs.documentfilecompat)

    // ExifInterface
    implementation(libs.androidx.exifinterface)

    // LeakCanary
    debugImplementation(libs.leakcanary.android)

    // Baseline Profiles
    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":benchmarks"))

    // crash reporting
    implementation(libs.acra.core)
    implementation(libs.auto.service.annotations)
    ksp(libs.auto.service.ksp)

    // Local tests: jUnit, coroutines, Android runner
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlin.test.junit)

    // Instrumented tests: jUnit rules and runners
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.kotlin.test.junit)
    androidTestImplementation(libs.kotlinx.coroutines.guava)
    androidTestUtil(libs.androidx.test.services)

    // mockk
    androidTestImplementation(libs.mockk.android)
}

spotless {
    ratchetFrom = "origin/main"
    kotlin {
        target("src/**/*.kt")
        ktlint(libs.versions.ktlint.get())
    }
}

aboutLibraries {
    registerAndroidTasks = false
    excludeFields = arrayOf(
        "description",
        "scm",
        "funding",
        "website",
        "organization",
        "organisationUrl",
    )
}
