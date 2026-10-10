import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

val localProperties = Properties().apply {
    val localPropsFile = rootProject.file("local.properties")
    if (localPropsFile.exists()) {
        load(localPropsFile.inputStream())
    }
}
val geminiApiKey = localProperties.getProperty("GEMINI_API_KEY") ?: ""

android {
    namespace = "com.mit.tushar_kaldate"
    compileSdk {
        version = release(35)
    }

    defaultConfig {
        applicationId = "com.mit.tushar_kaldate"
        minSdk {
            version = release(26)
        }
        targetSdk {
            version = release(35)
        }
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    lint {
        abortOnError = true
        checkDependencies = true
    }

    testOptions {
        animationsDisabled = true
        unitTests.isReturnDefaultValues = true
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
    compilerOptions.freeCompilerArgs.add("-Xskip-metadata-version-check")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

configurations.configureEach {
    exclude(group = "org.jetbrains", module = "annotations-java5")
    resolutionStrategy {
        force("org.jetbrains.kotlin:kotlin-stdlib:2.2.10")
        force("org.jetbrains.kotlin:kotlin-reflect:2.2.10")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.2.10")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.2.10")
        force("org.jetbrains.kotlin:kotlin-stdlib-common:2.2.10")
    }
}

dependencies {
    // AndroidX Core & UI
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Navigation
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore & WorkManager
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)

    // Coroutines & Serialization
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    // OkHttp & Markdown
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.markwon.core)
    implementation(libs.markwon.syntax.highlight)
    implementation(libs.markwon.ext.tables)
    implementation(libs.prism4j)

    // LiteRT-LM On-Device LLM Runtime
    implementation("com.google.ai.edge.litertlm:litertlm-android:0.17.1")

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.androidx.arch.core.testing)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
