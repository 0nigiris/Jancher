package io.jancher.launcher.model

/**
 * Состояние самообновления.
 *
 * Скачивание и установку приложение делает само только в сборках, которые
 * распространяются напрямую. Правила Google Play прямо запрещают приложению
 * обновлять себя в обход Play, поэтому в Play-сборке весь этот механизм
 * физически отсутствует, а не просто выключен флагом.
 */
sealed interface UpdateState {

    /** Обновлений нет либо проверка ещё не проводилась. */
    data object Idle : UpdateState

    data object Checking : UpdateState

    data class Available(val info: UpdateInfo) : UpdateState

    data class Downloading(val info: UpdateInfo, val progress: Float) : UpdateState

    /** Файл скачан и проверен, осталось отдать его системному установщику. */
    data class ReadyToInstall(val info: UpdateInfo) : UpdateState

    data class Failed(val reason: UpdateError) : UpdateState
}

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val notes: String,
    val sizeBytes: Long,
)

enum class UpdateError {
    /** Нет сети или сервер недоступен. Не повод показывать ошибку пользователю. */
    NETWORK,

    /** Ответ сервера не разобрать: скорее всего сломан формат манифеста. */
    MALFORMED,

    /**
     * Контрольная сумма скачанного файла не совпала с объявленной.
     * Это либо обрыв загрузки, либо подмена файла — устанавливать нельзя.
     */
    CHECKSUM,

    INSTALL_FAILED,
}

/** Абстракция, чтобы главный экран не знал, есть ли в этой сборке обновления вообще. */
interface UpdateGateway {

    val state: kotlinx.coroutines.flow.StateFlow<UpdateState>

    /** Присутствует ли механизм в этой сборке. В Play-сборке — false. */
    val supported: Boolean

    /** Проверка с учётом интервала; вызывается при каждом открытии лаунчера. */
    suspend fun checkIfDue()

    /** Проверка без учёта интервала — для кнопки в настройках. */
    suspend fun checkNow()

    suspend fun download()

    fun install()

    fun dismiss()
}
