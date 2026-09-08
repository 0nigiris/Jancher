package io.jancher.launcher.data

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * Хранилище конфигурации: один JSON-файл, записываемый атомарно.
 *
 * Тот же формат используется для экспорта настроек, поэтому «сделать бэкап»
 * и «сохранить изменение» — это одна и та же структура данных, а не две
 * расходящиеся реализации.
 */
class ConfigStore(context: Context, scope: CoroutineScope) {

    private val store: DataStore<LauncherConfig> = DataStoreFactory.create(
        serializer = ConfigSerializer,
        scope = scope,
        produceFile = { File(context.filesDir, "config.json") },
    )

    val config: Flow<LauncherConfig> get() = store.data

    suspend fun update(transform: (LauncherConfig) -> LauncherConfig) {
        store.updateData(transform)
    }
}

private object ConfigSerializer : Serializer<LauncherConfig> {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    // Пустая конфигурация, а не дефолтная: какие группы создать при первом
    // запуске, решает DefaultConfig, и это зависит от локали устройства.
    override val defaultValue = LauncherConfig()

    override suspend fun readFrom(input: InputStream): LauncherConfig =
        try {
            json.decodeFromString(
                LauncherConfig.serializer(),
                input.readBytes().decodeToString(),
            )
        } catch (e: SerializationException) {
            throw CorruptionException("Не удалось прочитать конфигурацию", e)
        }

    override suspend fun writeTo(t: LauncherConfig, output: OutputStream) {
        output.write(json.encodeToString(LauncherConfig.serializer(), t).encodeToByteArray())
    }
}
