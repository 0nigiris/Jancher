plugins {
    id("jancher.android.library")
}

android {
    namespace = "io.jancher.launcher.platform"
}

dependencies {
    implementation(project(":core:model"))
    api(project(":core:model"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
}
