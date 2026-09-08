package io.jancher.launcher.designsystem

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Лаунчер рисуется поверх обоев, поэтому у темы две задачи: взять цвета
 * системы и обеспечить читаемость текста на произвольном фоне. Второе решается
 * не подложкой под весь экран, а тенью под текстом — подложка убивает
 * ощущение, что список «лежит на обоях».
 */
@Composable
fun JancherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    trueBlack: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val base = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (darkTheme) darkColorScheme() else lightColorScheme()
    }

    // На AMOLED-экране S23 чёрный фон буквально не светится: это и контраст,
    // и экономия батареи, но навязывать его нельзя — на LCD выглядит грязно.
    val colorScheme = if (darkTheme && trueBlack) {
        base.copy(background = Color.Black, surface = Color.Black)
    } else {
        base
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = JancherTypography,
        content = content,
    )
}
