package io.jancher.launcher.platform

import android.content.Context
import android.os.UserHandle
import android.os.UserManager
import io.jancher.launcher.model.ComponentKey

/**
 * Превращает серийный номер пользователя обратно в `UserHandle`.
 *
 * Благодаря этому весь UI оперирует только [ComponentKey] и ничего не знает
 * про рабочие профили: разрешение владельца — забота платформенного слоя.
 */
class UserResolver(context: Context) {

    private val userManager: UserManager = context.getSystemService(UserManager::class.java)

    private val cache = HashMap<Int, UserHandle>()

    fun resolve(key: ComponentKey): UserHandle? = resolve(key.userSerial)

    fun resolve(serial: Int): UserHandle? = cache.getOrPut(serial) {
        userManager.getUserForSerialNumber(serial.toLong()) ?: return null
    }
}
