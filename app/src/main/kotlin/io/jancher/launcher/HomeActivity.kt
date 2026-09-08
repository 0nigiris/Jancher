package io.jancher.launcher

import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

class HomeActivity : ComponentActivity() {

    private val container: AppContainer by lazy { (application as JancherApp).container }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Первый запуск создаёт группы по алфавиту локали. Делается вне
        // критического пути отрисовки: экран умеет показывать пустое состояние.
        lifecycleScope.launch { container.repository.initialiseIfEmpty() }

        setContent {
            val viewModel: HomeViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { HomeViewModel(container.repository) }
                },
            )
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val searchState by viewModel.searchState.collectAsStateWithLifecycle()

            JancherTheme {
                CompositionLocalProvider(
                    LocalIconCache provides container.iconCache,
                    LocalAppLauncher provides container.appLauncher,
                ) {
                    HomeScreen(
                        state = state,
                        searchState = searchState,
                        onToggleGroup = viewModel::toggleGroup,
                        onExpandGroup = viewModel::expandGroup,
                        onLongClickApp = { viewModel.toggleFavorite(it.key) },
                        onOpenSearch = viewModel::openSearch,
                        onQueryChange = viewModel::onQueryChange,
                        onCloseSearch = viewModel::closeSearch,
                    )
                }
            }
        }
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
