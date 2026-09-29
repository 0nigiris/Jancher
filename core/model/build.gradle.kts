plugins {
    id("jancher.android.library")
}

android {
    namespace = "io.jancher.launcher.model"
}

dependencies {
    // Модель описывает состояние обновления как поток: иначе интерфейс
    // шлюза пришлось бы держать в слое данных, а на него ссылается UI.
    api(libs.kotlinx.coroutines.android)
}
