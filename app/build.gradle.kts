import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Ключ подписи живёт вне репозитория (~/.jancher/keystore.properties).
// Нет файла — релиз собирается неподписанным, сборка не падает.
val keystoreProps = Properties().apply {
    val f = File(System.getProperty("user.home"), ".jancher/keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "io.jancher.launcher"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.jancher.launcher"
        minSdk = 31
        targetSdk = 36
        versionCode = 4
        versionName = "0.3.1"
    }

    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    /**
     * direct — сборка для прямой раздачи: умеет обновлять себя сама.
     * play — сборка для Google Play: правила Play прямо запрещают приложению
     * обновляться в обход Play, поэтому весь механизм в неё не попадает —
     * ни кода, ни разрешений, а не «выключен настройкой».
     */
    flavorDimensions += "distribution"

    productFlavors {
        create("direct") {
            dimension = "distribution"
            isDefault = true
        }
        create("play") {
            dimension = "distribution"
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    androidResources {
        localeFilters += listOf("en", "ru")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    lint {
        // lintVital в релизной сборке — это ещё один полный анализ поверх R8,
        // и на этой машине он упирается в Metaspace. Проверки не отменяются:
        // lint запускается отдельной задачей (./gradlew :app:lintDirectDebug),
        // и её результат так же обязателен перед выпуском релиза.
        checkReleaseBuilds = false
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(project(":core:model"))
    "directImplementation"(project(":core:updater"))
    implementation(project(":core:data"))
    implementation(project(":core:platform"))
    implementation(project(":core:designsystem"))
    implementation(project(":feature:home"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

