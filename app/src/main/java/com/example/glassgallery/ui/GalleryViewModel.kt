package com.example.glassgallery.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.glassgallery.data.Album
import com.example.glassgallery.data.DaySection
import com.example.glassgallery.data.FavoritesStore
import com.example.glassgallery.data.MediaItem
import com.example.glassgallery.data.MediaRepository
import com.example.glassgallery.data.SettingsStore
import com.example.glassgallery.data.ThemeMode
import com.example.glassgallery.data.buildDaySections
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MediaFilter(val label: String) {
    ALL("All"),
    FAVORITES("Favorites"),
    VIDEOS("Videos"),
}

enum class SortOrder(val label: String) {
    NEWEST("Newest first"),
    OLDEST("Oldest first"),
    NAME("Name (A–Z)"),
    LARGEST("Largest first"),
}

private val gridSizes = listOf(80, 112, 150)

class GalleryViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = MediaRepository(app)
    private val favoritesStore = FavoritesStore(app)
    private val settings = SettingsStore(app)
    private var loadStarted = false

    private val _media = MutableStateFlow<List<MediaItem>>(emptyList())
    val media: StateFlow<List<MediaItem>> = _media.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _favorites = MutableStateFlow(favoritesStore.load())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    private val _selection = MutableStateFlow<Set<String>>(emptySet())
    val selection: StateFlow<Set<String>> = _selection.asStateFlow()

    private val _filter = MutableStateFlow(MediaFilter.ALL)
    val filter: StateFlow<MediaFilter> = _filter.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _sort = MutableStateFlow(SortOrder.NEWEST)
    val sort: StateFlow<SortOrder> = _sort.asStateFlow()

    private val _themeMode = MutableStateFlow(settings.themeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _cellDp = MutableStateFlow(settings.gridCellDp())
    val cellDp: StateFlow<Int> = _cellDp.asStateFlow()

    /** The main grid: filtered, searched, sorted, and grouped. */
    val sections: StateFlow<List<DaySection>> = combine(_media, _favorites, _filter, _query, _sort) { items, favs, mode, q, order ->
        val filtered = items.filter { item ->
            val modeOk = when (mode) {
                MediaFilter.ALL -> true
                MediaFilter.FAVORITES -> item.key in favs
                MediaFilter.VIDEOS -> item.isVideo
            }
            modeOk && (q.isBlank() || item.name.contains(q, ignoreCase = true))
        }
        when (order) {
            SortOrder.NEWEST -> buildDaySections(filtered.sortedByDescending { it.dateTaken })
            SortOrder.OLDEST -> buildDaySections(filtered.sortedBy { it.dateTaken }).asReversed()
            SortOrder.NAME -> listOf(DaySection("all", "All items", filtered.sortedBy { it.name.lowercase() }))
            SortOrder.LARGEST -> listOf(DaySection("all", "All items", filtered.sortedByDescending { it.sizeBytes }))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val albums: StateFlow<List<Album>> = _media.map { items ->
        items.groupBy { it.bucketId }
            .map { (id, list) ->
                Album(bucketId = id, name = list.first().bucketName, cover = list.first(), count = list.size)
            }
            .sortedByDescending { it.count }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Items handed to the full-screen viewer, in display order. */
    var viewerItems: List<MediaItem> = emptyList()
        private set

    fun load(force: Boolean = false) {
        if (loadStarted && !force) return
        loadStarted = true
        viewModelScope.launch {
            _loading.value = true
            _media.value = repository.loadMedia()
            _loading.value = false
        }
    }

    fun cycleTheme() {
        val next = ThemeMode.entries[(_themeMode.value.ordinal + 1) % ThemeMode.entries.size]
        _themeMode.value = next
        settings.saveThemeMode(next)
    }

    fun cycleCell() {
        val next = gridSizes[(gridSizes.indexOf(_cellDp.value) + 1) % gridSizes.size]
        _cellDp.value = next
        settings.saveGridCellDp(next)
    }

    fun openViewer(items: List<MediaItem>) {
        viewerItems = items
    }

    fun setFilter(mode: MediaFilter) {
        _filter.value = mode
    }

    fun setSort(order: SortOrder) {
        _sort.value = order
    }

    fun setQuery(text: String) {
        _query.value = text
    }

    fun toggleSelection(key: String) {
        _selection.update { if (key in it) it - key else it + key }
    }

    fun setSelection(keys: Set<String>) {
        _selection.value = keys
    }

    fun clearSelection() {
        _selection.value = emptySet()
    }

    fun toggleFavorite(key: String) {
        updateFavorites(setOf(key))
    }

    fun favoriteSelection() {
        updateFavorites(_selection.value)
        clearSelection()
    }

    /** Removes items the system has deleted, so the grid updates without a reload. */
    fun markDeleted(keys: Set<String>) {
        _media.update { list -> list.filterNot { it.key in keys } }
        _selection.update { it - keys }
        _favorites.update { favs ->
            val next = favs - keys
            favoritesStore.save(next)
            next
        }
    }

    private fun updateFavorites(keys: Set<String>) {
        _favorites.update { current ->
            val next = if (keys.all { it in current }) current - keys else current + keys
            favoritesStore.save(next)
            next
        }
    }
}
