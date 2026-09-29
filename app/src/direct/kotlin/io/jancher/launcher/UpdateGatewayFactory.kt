package io.jancher.launcher

import android.content.Context
import io.jancher.launcher.model.UpdateGateway
import io.jancher.launcher.updater.UpdateRepository
import kotlinx.coroutines.CoroutineScope

/**
 * Сборка прямой раздачи: самообновление есть.
 */
fun createUpdateGateway(
    context: Context,
    scope: CoroutineScope,
    versionCode: Int,
): UpdateGateway = UpdateRepository(
    context = context,
    scope = scope,
    currentVersionCode = versionCode,
)
