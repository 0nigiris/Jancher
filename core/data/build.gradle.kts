plugins {
    id("jancher.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.jancher.launcher.data"
}

dependencies {
    api(project(":core:model"))
    api(project(":core:platform"))
    implementation(libs.androidx.datastore)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.collections.immutable)

    testImplementation(libs.junit)
}
