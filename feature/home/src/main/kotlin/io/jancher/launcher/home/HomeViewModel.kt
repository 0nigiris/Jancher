package io.jancher.launcher.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.jancher.launcher.data.AppMatcher
import io.jancher.launcher.data.ComposedGroup
import io.jancher.launcher.data.Settings
import io.jancher.launcher.data.LauncherRepository
import io.jancher.launcher.model.App
import io.jancher.launcher.model.ComponentKey
import io.jancher.launcher.model.GroupRole
import io.jancher.launcher.model.UpdateGateway
import io.jancher.launcher.model.UpdateState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val favorites: List<App> = emptyList(),
    val groups: List<ComposedGroup> = emptyList(),
    val expandedGroupId: Long? = null,
    val loaded: Boolean = false,
    val settings: Settings = Settings(),
    val settingsOpen: Boolean = false,
)

data class MenuUiState(
    val app: App? = null,
    val isFavorite: Boolean = false,
)

data class SearchUiState(
    val active: Boolean = false,
    val query: String = "",
    val results: List<App> = emptyList(),
)

class HomeViewModel(
    private val repository: LauncherRepository,
    private val updates: UpdateGateway,
) : ViewModel() {

    val updateState: StateFlow<UpdateState> = updates.state

    init {
        // Проверка при каждом открытии лаунчера, но сам шлюз соблюдает
        // интервал: лишних запросов не будет.
        viewModelScope.launch { updates.checkIfDue() }
    }

    fun downloadUpdate() {
        viewModelScope.launch { updates.download() }
    }

    fun installUpdate() = updates.install()

    fun dismissUpdate() = updates.dismiss()

    private val expandedGroupId = MutableStateFlow<Long?>(null)
    private val searchQuery = MutableStateFlow<String?>(null)
    private val menuFor = MutableStateFlow<ComponentKey?>(null)
    private val settingsOpen = MutableStateFlow(false)

    val uiState: StateFlow<HomeUiState> =
        combine(repository.state, expandedGroupId, settingsOpen) { state, expanded, settings ->
            val favorites = state.groups
                .firstOrNull { it.group.role == GroupRole.FAVORITES }
                ?.apps
                .orEmpty()

            HomeUiState(
                favorites = favorites,
                // Избранное показывается отдельной зоной сверху, поэтому
                // в общем списке групп его дублировать не нужно.
                groups = state.groups.filter {
                    it.group.role != GroupRole.FAVORITES && !it.group.hidden
                },
                expandedGroupId = expanded,
                loaded = true,
                settings = state.settings,
                settingsOpen = settings,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = HomeUiState(),
        )

    /**
     * Поиск живёт отдельным потоком состояния, а не полем в [HomeUiState]:
     * набор текста не должен перерисовывать список групп под оверлеем.
     */
    val searchState: StateFlow<SearchUiState> =
        combine(repository.state, searchQuery) { state, query ->
            SearchUiState(
                active = query != null,
                query = query.orEmpty(),
                results = if (query.isNullOrBlank()) {
                    emptyList()
                } else {
                    AppMatcher.search(state.allApps, query)
                },
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = SearchUiState(),
        )

    val menuState: StateFlow<MenuUiState> =
        combine(repository.state, menuFor) { state, key ->
            if (key == null) {
                MenuUiState()
            } else {
                MenuUiState(
                    app = state.allApps.firstOrNull { it.key == key },
                    isFavorite = state.groups
                        .firstOrNull { it.group.role == GroupRole.FAVORITES }
                        ?.apps
                        ?.any { it.key == key } == true,
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = MenuUiState(),
        )

    fun openSettings() {
        settingsOpen.value = true
    }

    fun closeSettings() {
        settingsOpen.value = false
    }

    fun setTrueBlack(enabled: Boolean) {
        viewModelScope.launch { repository.setTrueBlack(enabled) }
    }

    fun openMenu(key: ComponentKey) {
        menuFor.value = key
    }

    fun closeMenu() {
        menuFor.value = null
    }

    fun openSearch() {
        searchQuery.value = ""
    }

    fun closeSearch() {
        searchQuery.value = null
    }

    fun onQueryChange(query: String) {
        searchQuery.value = query
    }

    /**
     * Аккордеон эксклюзивный: раскрытая группа ровно одна. Иначе список
     * растёт бесконтрольно, и попасть в нужную группу с рельса становится
     * невозможно — позиции уезжают под пальцем.
     */
    fun toggleGroup(groupId: Long) {
        expandedGroupId.value = if (expandedGroupId.value == groupId) null else groupId
    }

    fun expandGroup(groupId: Long?) {
        expandedGroupId.value = groupId
    }

    /** Возврат в исходное состояние: поиск закрыт, все группы схлопнуты. */
    fun resetToHome() {
        searchQuery.value = null
        expandedGroupId.value = null
        menuFor.value = null
        settingsOpen.value = false
    }

    fun toggleFavorite(key: ComponentKey) {
        viewModelScope.launch { repository.toggleFavorite(key) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
