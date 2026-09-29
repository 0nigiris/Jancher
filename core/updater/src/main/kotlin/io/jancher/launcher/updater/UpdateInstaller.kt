package io.jancher.launcher.updater

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import java.io.File

/**
 * Передаёт скачанный APK системному установщику.
 *
 * `PackageInstaller` вместо `ACTION_VIEW` с FileProvider: не нужен ни
 * временный доступ к файлу, ни внешняя активность-посредник, а результат
 * приходит обратно в приложение.
 *
 * Подпись проверять руками не нужно и нельзя подделать: Android сам откажет
 * в установке, если подпись APK не совпадает с подписью уже установленного
 * приложения. Это и есть настоящая защита от подмены файла — наш SHA-256
 * лишь ловит обрыв загрузки раньше, чем система покажет ошибку.
 */
class UpdateInstaller(private val context: Context) {

    fun install(apk: File): Boolean = runCatching {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(
            PackageInstaller.SessionParams.MODE_FULL_INSTALL,
        ).apply {
            setAppPackageName(context.packageName)
        }

        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            session.openWrite("jancher", 0, apk.length()).use { output ->
                apk.inputStream().use { it.copyTo(output) }
                session.fsync(output)
            }

            val intent = Intent(ACTION_INSTALL_RESULT).setPackage(context.packageName)
            val pending = PendingIntent.getBroadcast(
                context,
                sessionId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            session.commit(pending.intentSender)
        }
        true
    }.getOrDefault(false)

    companion object {
        const val ACTION_INSTALL_RESULT = "io.jancher.launcher.INSTALL_RESULT"
    }
}
