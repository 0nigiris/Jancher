plugins {
    id("jancher.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.jancher.launcher.updater"
}

dependencies {
    api(project(":core:model"))
    implementation(libs.androidx.datastore)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
}
