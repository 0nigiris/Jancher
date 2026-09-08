package io.jancher.launcher

import android.app.Application

class JancherApp : Application() {

    /** Создаётся лениво: до первого кадра контейнер не нужен. */
    val container: AppContainer by lazy { AppContainer(this) }
}
