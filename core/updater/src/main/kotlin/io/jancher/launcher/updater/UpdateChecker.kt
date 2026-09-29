package io.jancher.launcher.updater

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * Читает манифест обновления.
 *
 * Без OkHttp и Retrofit: один GET-запрос не стоит библиотеки на несколько
 * сотен килобайт, а `HttpsURLConnection` умеет всё, что здесь нужно.
 */
internal class UpdateChecker(private val manifestUrl: String) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetch(): UpdateManifest? = withContext(Dispatchers.IO) {
        val connection = (URL(manifestUrl).openConnection() as HttpsURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            requestMethod = "GET"
            // Кеш GitHub раздаёт raw-файлы с большим TTL; для проверки
            // обновлений нужен свежий ответ, а не то, что закешировал CDN.
            setRequestProperty("Cache-Control", "no-cache")
            setRequestProperty("Accept", "application/json")
        }

        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            json.decodeFromString(UpdateManifest.serializer(), body)
        } catch (e: Exception) {
            // Отсутствие сети — норма, а не ошибка, о которой нужно докладывать.
            null
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val TIMEOUT_MS = 15_000
    }
}
