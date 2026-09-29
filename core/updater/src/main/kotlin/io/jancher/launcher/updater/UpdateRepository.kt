package io.jancher.launcher.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import io.jancher.launcher.model.UpdateError
import io.jancher.launcher.model.UpdateGateway
import io.jancher.launcher.model.UpdateInfo
import io.jancher.launcher.model.UpdateState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * Самообновление: проверка, скачивание, передача установщику.
 *
 * Проверка происходит при открытии лаунчера, но не чаще одного раза
 * в [CHECK_INTERVAL_MS]. Фоновая задача (WorkManager, периодический будильник)
 * здесь была бы хуже по всем параметрам: лаунчер открывают десятки раз в день,
 * так что обновление всё равно находится в первые же часы, а постоянно
 * работающая фоновая задача тратит батарею на проверку того, что почти
 * всегда не изменилось.
 */
class UpdateRepository(
    private val context: Context,
    private val scope: CoroutineScope,
    private val currentVersionCode: Int,
    manifestUrl: String = DEFAULT_MANIFEST_URL,
) : UpdateGateway {

    private val checker = UpdateChecker(manifestUrl)
    private val downloader = UpdateDownloader(File(context.cacheDir, "updates").apply { mkdirs() })
    private val installer = UpdateInstaller(context)

    private val prefs = context.getSharedPreferences("updates", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    override val state: StateFlow<UpdateState> = _state.asStateFlow()

    override val supported: Boolean = true

    private var manifest: UpdateManifest? = null
    private var downloaded: File? = null

    override suspend fun checkIfDue() {
        val now = System.currentTimeMillis()
        val last = prefs.getLong(KEY_LAST_CHECK, 0L)
        if (now - last < CHECK_INTERVAL_MS) return
        check()
    }

    suspend fun check() {
        if (_state.value is UpdateState.Downloading) return

        _state.value = UpdateState.Checking
        val fetched = checker.fetch()
        prefs.edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply()

        if (fetched == null) {
            _state.value = UpdateState.Idle
            return
        }

        manifest = fetched

        val offer = UpdateDecision.shouldOffer(
            manifest = fetched,
            currentVersionCode = currentVersionCode,
            dismissedVersionCode = prefs.getInt(KEY_DISMISSED, 0),
        )

        _state.value = if (offer) {
            UpdateState.Available(fetched.toInfo())
        } else {
            UpdateState.Idle
        }
    }

    override suspend fun download() {
        val target = manifest ?: return
        _state.value = UpdateState.Downloading(target.toInfo(), 0f)

        val result = downloader.download(target) { progress ->
            _state.value = UpdateState.Downloading(target.toInfo(), progress)
        }

        result.fold(
            onSuccess = { file ->
                downloaded = file
                _state.value = UpdateState.ReadyToInstall(target.toInfo())
            },
            onFailure = { error ->
                _state.value = UpdateState.Failed(
                    if (error is ChecksumMismatch) UpdateError.CHECKSUM else UpdateError.NETWORK,
                )
            },
        )
    }

    override fun install() {
        val file = downloaded ?: return

        // Без разрешения на установку из этого источника установщик молча
        // откажет, поэтому пользователя нужно отвести в настройки один раз.
        if (!context.packageManager.canRequestPackageInstalls()) {
            openInstallPermissionSettings()
            return
        }

        if (!installer.install(file)) {
            _state.value = UpdateState.Failed(UpdateError.INSTALL_FAILED)
        }
    }

    override fun dismiss() {
        manifest?.let { prefs.edit().putInt(KEY_DISMISSED, it.versionCode).apply() }
        _state.value = UpdateState.Idle
    }

    /** Повторить проверку принудительно — для кнопки в настройках. */
    fun checkNow() {
        scope.launch { check() }
    }

    private fun openInstallPermissionSettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    private fun UpdateManifest.toInfo() = UpdateInfo(
        versionCode = versionCode,
        versionName = versionName,
        notes = notes,
        sizeBytes = sizeBytes,
    )

    companion object {
        /**
         * Манифест лежит в ветке репозитория, а не в релизе: так его можно
         * поправить, не пересобирая и не перевыкладывая APK.
         */
        const val DEFAULT_MANIFEST_URL =
            "https://raw.githubusercontent.com/0nigiris/Jancher/master/update.json"

        private const val CHECK_INTERVAL_MS = 6 * 60 * 60 * 1000L
        private const val KEY_LAST_CHECK = "last_check"
        private const val KEY_DISMISSED = "dismissed_version"
    }
}
