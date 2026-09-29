package io.jancher.launcher

import android.content.Context
import io.jancher.launcher.model.UpdateGateway
import io.jancher.launcher.model.UpdateState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Play-сборка: обновления приходят через Play, поэтому шлюз — заглушка,
 * а модуль `:core:updater` в этот вариант вообще не подключён.
 */
fun createUpdateGateway(
    context: Context,
    scope: CoroutineScope,
    versionCode: Int,
): UpdateGateway = NoUpdates

private object NoUpdates : UpdateGateway {
    override val state: StateFlow<UpdateState> = MutableStateFlow(UpdateState.Idle)
    override val supported: Boolean = false
    override suspend fun checkIfDue() = Unit
    override suspend fun checkNow() = Unit
    override suspend fun download() = Unit
    override fun install() = Unit
    override fun dismiss() = Unit
}
