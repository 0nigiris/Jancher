package io.jancher.launcher.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.jancher.launcher.data.ComposedGroup
import io.jancher.launcher.model.App
import kotlinx.coroutines.launch

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
    BackHandler(enabled = searchState.active || state.expandedGroupId != null) {
        if (searchState.active) onCloseSearch() else onExpandGroup(null)
    }

    Box(
        modifier
            .fillMaxSize()
            .swipeUpFromBottom(enabled = !searchState.active, onTriggered = onOpenSearch),
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

                item(key = "group-${composed.group.id}") {
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

/**
 * Рельс отодвинут от края: последние ~20dp экрана принадлежат системному
 * жесту «назад», и попытка их отобрать ломает навигацию всей системы.
 */
private val RAIL_EDGE_INSET = 20.dp
