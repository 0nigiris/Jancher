package io.jancher.launcher

import android.app.role.RoleManager
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.jancher.launcher.ui.HomeScreen
import io.jancher.launcher.ui.JancherTheme

class HomeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            JancherTheme {
                HomeScreen(
                    isDefaultHome = isDefaultHome(),
                    onRequestDefaultHome = ::requestDefaultHome,
                )
            }
        }
    }

    private fun isDefaultHome(): Boolean {
        val resolved = packageManager.resolveActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            android.content.pm.PackageManager.MATCH_DEFAULT_ONLY,
        )
        return resolved?.activityInfo?.packageName == packageName
    }

    /**
     * ROLE_HOME даёт системный диалог вместо блуждания по настройкам,
     * но доступен не на всех прошивках — отсюда fallback на экран настроек.
     */
    private fun requestDefaultHome() {
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
}
