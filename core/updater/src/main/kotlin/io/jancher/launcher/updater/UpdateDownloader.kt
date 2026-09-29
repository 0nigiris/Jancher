package io.jancher.launcher.updater

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

internal class UpdateDownloader(private val cacheDir: File) {

    /**
     * Качает APK и сразу считает SHA-256 по мере чтения.
     *
     * Второй проход по файлу ради хеша не нужен, а проверка обязательна:
     * дальше этот файл уходит системному установщику как исполняемый код.
     * Несовпадение хеша означает обрыв загрузки или подмену — в обоих
     * случаях файл удаляется, а не устанавливается.
     */
    suspend fun download(
        manifest: UpdateManifest,
        onProgress: (Float) -> Unit,
    ): Result<File> = withContext(Dispatchers.IO) {
        val target = File(cacheDir, "update-${manifest.versionCode}.apk")
        target.delete()

        val connection = (URL(manifest.apkUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            instanceFollowRedirects = true
        }

        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(IllegalStateException("HTTP ${connection.responseCode}"))
            }

            val total = connection.contentLengthLong.takeIf { it > 0 } ?: manifest.sizeBytes
            val digest = MessageDigest.getInstance("SHA-256")
            var read = 0L

            connection.inputStream.use { input ->
                target.outputStream().buffered().use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count <= 0) break
                        output.write(buffer, 0, count)
                        digest.update(buffer, 0, count)
                        read += count
                        if (total > 0) onProgress((read.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
            }

            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            if (!actual.equals(manifest.sha256, ignoreCase = true)) {
                target.delete()
                return@withContext Result.failure(ChecksumMismatch(manifest.sha256, actual))
            }

            Result.success(target)
        } catch (e: Exception) {
            target.delete()
            Result.failure(e)
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val TIMEOUT_MS = 30_000
        const val BUFFER_SIZE = 64 * 1024
    }
}

internal class ChecksumMismatch(expected: String, actual: String) :
    IllegalStateException("Контрольная сумма не совпала: ожидалась $expected, получена $actual")
