package io.jancher.launcher.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import io.jancher.launcher.data.ComposedGroup
import io.jancher.launcher.model.App
import io.jancher.launcher.model.UpdateState
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    searchState: SearchUiState,
    onToggleGroup: (Long) -> Unit,
    onExpandGroup: (Long?) -> Unit,
    onLongClickApp: (App) -> Unit,
    onOpenSearch: () -> Unit,
    onQueryChange: (String) -> Unit,
    onCloseSearch: () -> Unit,
    menuState: MenuUiState,
    onCloseMenu: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenAppInfo: () -> Unit,
    updateState: UpdateState,
    onDownloadUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    onDismissUpdate: () -> Unit,
    onOpenSettings: () -> Unit,
    onCloseSettings: () -> Unit,
    onTrueBlackChange: (Boolean) -> Unit,
    onRequestDefaultHome: () -> Unit,
    onCheckUpdates: () -> Unit,
    isDefaultHome: Boolean,
    versionName: String,
    updatesSupported: Boolean,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var railPreview by remember { mutableStateOf<Int?>(null) }

    // Позиции заголовков групп в списке: рельсу нужно знать, куда прокручивать
    // после раскрытия. Пересчитывается только при изменении состава.
    val headerIndices = remember(state.groups, state.expandedGroupId, state.favorites.isEmpty()) {
        buildMap {
            // Часы есть всегда, блок избранного — только когда он не пуст.
            var index = if (state.favorites.isEmpty()) 1 else 2
            state.groups.forEach { composed ->
                put(composed.group.id, index)
                index++
                if (composed.group.id == state.expandedGroupId) index += composed.apps.size
            }
        }
    }

    // Для лаунчера «назад» — это не выход, а возврат к исходному состоянию
    // главного экрана. Выходить некуда: это и есть корень системы.
    BackHandler(
        enabled = searchState.active || menuState.app != null ||
            state.settingsOpen || state.expandedGroupId != null,
    ) {
        when {
            state.settingsOpen -> onCloseSettings()
            menuState.app != null -> onCloseMenu()
            searchState.active -> onCloseSearch()
            else -> onExpandGroup(null)
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .swipeUpFromBottom(enabled = !searchState.active, onTriggered = onOpenSearch)
            // Долгое нажатие по пустому месту — вход в настройки. Модификатор
            // стоит на контейнере под списком, поэтому строки приложений
            // забирают своё долгое нажатие раньше и меню не конфликтует.
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { onOpenSettings() })
            },
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            contentPadding = PaddingValues(start = 24.dp, end = 48.dp, bottom = 48.dp),
        ) {
            item(key = "clock") {
                Clock(Modifier.padding(top = 32.dp, bottom = 24.dp))
            }

            if (state.favorites.isNotEmpty()) {
                item(key = "favorites") {
                    Column(Modifier.padding(bottom = 24.dp)) {
                        state.favorites.forEach { app ->
                            AppRow(app = app, onLongClick = onLongClickApp)
                        }
                    }
                }
            }

            state.groups.forEach { composed ->
                val expanded = composed.group.id == state.expandedGroupId

                // Липкий заголовок: при прокрутке раскрытой группы видно,
                // где ты находишься, — без этого длинный список теряет контекст.
                stickyHeader(key = "group-${composed.group.id}") {
                    GroupHeader(
                        title = composed.group.title,
                        count = composed.apps.size,
                        expanded = expanded,
                        onClick = { onToggleGroup(composed.group.id) },
                    )
                }

                if (expanded) {
                    items(
                        items = composed.apps,
                        key = { "app-${composed.group.id}-${it.key.asString()}" },
                    ) { app ->
                        AppRow(app = app, onLongClick = onLongClickApp)
                    }
                }
            }
        }

        GroupRail(
            labels = state.groups.map { it.group.title },
            onPreview = { railPreview = it },
            onCommit = { index ->
                val group = state.groups.getOrNull(index) ?: return@GroupRail
                onExpandGroup(group.group.id)
                scope.launch {
                    // Прокрутка происходит после раскрытия, а не во время
                    // ведения: список должен переставиться ровно один раз.
                    listState.animateScrollToItem(headerIndices[group.group.id] ?: 0)
                }
            },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .systemBarsPadding()
                .padding(end = RAIL_EDGE_INSET),
        )

        if (state.settingsOpen) {
            SettingsOverlay(
                trueBlack = state.settings.trueBlack,
                isDefaultHome = isDefaultHome,
                versionName = versionName,
                updatesSupported = updatesSupported,
                onTrueBlackChange = onTrueBlackChange,
                onRequestDefaultHome = onRequestDefaultHome,
                onCheckUpdates = onCheckUpdates,
                onDismiss = onCloseSettings,
            )
        }

        menuState.app?.let { app ->
            AppMenu(
                app = app,
                isFavorite = menuState.isFavorite,
                onToggleFavorite = onToggleFavorite,
                onOpenAppInfo = onOpenAppInfo,
                onDismiss = onCloseMenu,
            )
        }

        UpdateBanner(
            state = updateState,
            onDownload = onDownloadUpdate,
            onInstall = onInstallUpdate,
            onDismiss = onDismissUpdate,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .systemBarsPadding(),
        )

        if (searchState.active) {
            SearchOverlay(
                state = searchState,
                onQueryChange = onQueryChange,
                onDismiss = onCloseSearch,
                onLongClickApp = onLongClickApp,
            )
        }

        railPreview?.let { index ->
            state.groups.getOrNull(index)?.let { composed ->
                RailPreviewPill(
                    label = composed.group.title,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(end = 48.dp),
                )
            }
        }
    }
}

@Composable
private fun GroupHeader(
    title: String,
    count: Int,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            // Фон нужен только раскрытому заголовку: под ним прокручиваются
            // строки приложений, и без подложки текст ложится на текст.
            // Схлопнутым он не нужен — они просто вытесняют друг друга.
            .then(
                if (expanded) {
                    Modifier.background(
                        MaterialTheme.colorScheme.background.copy(alpha = STICKY_HEADER_ALPHA),
                    )
                } else {
                    Modifier
                },
            )
            .padding(vertical = 10.dp),
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.titleMedium,
            color = if (expanded) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

/** Раскрытому заголовку нужна подложка, но не глухая: обои должны просвечивать. */
private const val STICKY_HEADER_ALPHA = 0.92f

/**
 * Рельс отодвинут от края: последние ~20dp экрана принадлежат системному
 * жесту «назад», и попытка их отобрать ломает навигацию всей системы.
 */
private val RAIL_EDGE_INSET = 20.dp
