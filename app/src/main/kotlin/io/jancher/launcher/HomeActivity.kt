package io.jancher.launcher

import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.content.Intent as AndroidIntent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.jancher.launcher.designsystem.JancherTheme
import io.jancher.launcher.home.HomeScreen
import io.jancher.launcher.home.HomeViewModel
import io.jancher.launcher.home.LocalAppLauncher
import io.jancher.launcher.home.LocalIconCache
import io.jancher.launcher.home.LocalShortcutSource
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

class HomeActivity : ComponentActivity() {

    private val container: AppContainer by lazy { (application as JancherApp).container }

    private var viewModelRef: HomeViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Первый запуск создаёт группы по алфавиту локали. Делается вне
        // критического пути отрисовки: экран умеет показывать пустое состояние.
        lifecycleScope.launch { container.repository.initialiseIfEmpty() }

        setContent {
            val viewModel: HomeViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { HomeViewModel(container.repository, container.updates) }
                },
            )
            viewModelRef = viewModel
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val searchState by viewModel.searchState.collectAsStateWithLifecycle()
            val updateState by viewModel.updateState.collectAsStateWithLifecycle()
            val menuState by viewModel.menuState.collectAsStateWithLifecycle()

            JancherTheme(trueBlack = state.settings.trueBlack) {
                CompositionLocalProvider(
                    LocalIconCache provides container.iconCache,
                    LocalAppLauncher provides container.appLauncher,
                    LocalShortcutSource provides container.shortcutSource,
                ) {
                    HomeScreen(
                        state = state,
                        searchState = searchState,
                        onToggleGroup = viewModel::toggleGroup,
                        onExpandGroup = viewModel::expandGroup,
                        onLongClickApp = { viewModel.openMenu(it.key) },
                        onOpenSearch = viewModel::openSearch,
                        onQueryChange = viewModel::onQueryChange,
                        onCloseSearch = viewModel::closeSearch,
                        menuState = menuState,
                        onCloseMenu = viewModel::closeMenu,
                        onToggleFavorite = {
                            menuState.app?.let { viewModel.toggleFavorite(it.key) }
                        },
                        onOpenAppInfo = {
                            menuState.app?.let { container.appLauncher.openAppInfo(it.key, null) }
                        },
                        updateState = updateState,
                        onDownloadUpdate = viewModel::downloadUpdate,
                        onInstallUpdate = viewModel::installUpdate,
                        onDismissUpdate = viewModel::dismissUpdate,
                        onOpenSettings = viewModel::openSettings,
                        onCloseSettings = viewModel::closeSettings,
                        onTrueBlackChange = viewModel::setTrueBlack,
                        onRequestDefaultHome = ::requestDefaultHome,
                        onCheckUpdates = { container.checkUpdatesNow() },
                        isDefaultHome = isDefaultHome(),
                        versionName = BuildConfig.VERSION_NAME,
                        updatesSupported = container.updates.supported,
                    )
                }
            }
        }
    }

    /**
     * Активность помечена singleTask, поэтому повторное нажатие Home приходит
     * сюда, а не создаёт новый экземпляр. Пользователь ждёт, что экран
     * вернётся в исходное состояние — это главное назначение кнопки Home.
     */
    override fun onNewIntent(intent: AndroidIntent) {
        super.onNewIntent(intent)
        viewModelRef?.resetToHome()
    }

    /**
     * ROLE_HOME даёт системный диалог вместо блуждания по настройкам,
     * но роль доступна не на всех прошивках — отсюда запасной путь.
     */
    fun requestDefaultHome() {
        val roleManager = getSystemService(RoleManager::class.java)
        if (roleManager != null &&
            roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
            !roleManager.isRoleHeld(RoleManager.ROLE_HOME)
        ) {
            startActivity(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME))
        } else {
            startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
        }
    }

    fun isDefaultHome(): Boolean {
        val resolved = packageManager.resolveActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            PackageManager.MATCH_DEFAULT_ONLY,
        )
        return resolved?.activityInfo?.packageName == packageName
    }
}
