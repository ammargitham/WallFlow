@file:UseSerializers(
    UriSerializer::class,
)

package com.ammar.wallflow.ui.screens.settings.autowallpapersources

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ammar.wallflow.data.db.entity.search.toSavedSearch
import com.ammar.wallflow.data.preferences.AppPreferences
import com.ammar.wallflow.data.preferences.AutoWallpaperPreferences
import com.ammar.wallflow.data.repository.AppPreferencesRepository
import com.ammar.wallflow.data.repository.FavoritesRepository
import com.ammar.wallflow.data.repository.LightDarkRepository
import com.ammar.wallflow.data.repository.SavedSearchRepository
import com.ammar.wallflow.extensions.accessibleFolders
import com.ammar.wallflow.model.WallpaperTarget
import com.ammar.wallflow.model.local.LocalDirectory
import com.ammar.wallflow.model.search.SavedSearch
import com.ammar.wallflow.model.serializers.UriSerializer
import com.ammar.wallflow.ui.screens.settings.updateAutoWallpaperPrefs
import com.ammar.wallflow.utils.combine
import com.ammar.wallflow.utils.getRealPath
import com.github.materiiapps.partial.Partialize
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.UseSerializers

@HiltViewModel
class ManageAutoWallpaperSourcesViewModel @Inject constructor(
    private val application: Application,
    private val appPreferencesRepository: AppPreferencesRepository,
    lightDarkRepository: LightDarkRepository,
    savedSearchRepository: SavedSearchRepository,
    favoritesRepository: FavoritesRepository,
) : AndroidViewModel(application) {
    private val localUiStateFlow = MutableStateFlow(ManageAutoWallpaperSourcesUiStatePartial())
    private val localDirectories = flowOf(
        application.accessibleFolders
            .map { p ->
                LocalDirectory(
                    uri = p.uri,
                    path = getRealPath(
                        context = application,
                        uri = p.uri,
                    ) ?: p.uri.toString(),
                )
            },
    )

    val uiState = combine(
        appPreferencesRepository.appPreferencesFlow,
        localUiStateFlow,
        lightDarkRepository.observeCount(),
        savedSearchRepository.observeAll(),
        favoritesRepository.observeCount(),
        localDirectories,
    ) {
            appPreferences,
            localUiState,
            lightDarkCount,
            savedSearches,
            favoritesCount,
            dirs,
        ->
        val autoWallpaperPreferences = appPreferences.autoWallpaperPreferences
        val allSavedSearches = savedSearches.map { entity -> entity.toSavedSearch() }
        localUiState.merge(
            ManageAutoWallpaperSourcesUiState(
                appPreferences = appPreferences,
                autoWallpaperEnabled = autoWallpaperPreferences.enabled,
                useSameSources = !autoWallpaperPreferences.setDifferentWallpapers,
                savedSearches = allSavedSearches.toPersistentList(),
                hasLightDarkWallpapers = lightDarkCount > 0,
                hasFavorites = favoritesCount > 0,
                localDirectories = dirs.toPersistentList(),
                homeScreenSources = AutoWallpaperSources(
                    lightDarkEnabled = autoWallpaperPreferences.lightDarkEnabled,
                    useDarkWithExtraDim = autoWallpaperPreferences.useDarkWithExtraDim,
                    savedSearchEnabled = autoWallpaperPreferences.savedSearchEnabled,
                    savedSearchIds = autoWallpaperPreferences.savedSearchIds,
                    favoritesEnabled = autoWallpaperPreferences.favoritesEnabled,
                    localEnabled = autoWallpaperPreferences.localEnabled,
                    localDirs = autoWallpaperPreferences.localDirs,
                ),
                lockScreenSources = AutoWallpaperSources(
                    lightDarkEnabled = autoWallpaperPreferences.lsLightDarkEnabled,
                    useDarkWithExtraDim = autoWallpaperPreferences.lsUseDarkWithExtraDim,
                    savedSearchEnabled = autoWallpaperPreferences.lsSavedSearchEnabled,
                    savedSearchIds = autoWallpaperPreferences.lsSavedSearchIds,
                    favoritesEnabled = autoWallpaperPreferences.lsFavoritesEnabled,
                    localEnabled = autoWallpaperPreferences.lsLocalEnabled,
                    localDirs = autoWallpaperPreferences.lsLocalDirs,
                ),
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ManageAutoWallpaperSourcesUiState(),
    )

    fun updateUseSameSources(useSameSources: Boolean) = updateAutoWallpaperPrefs(
        uiState.value.appPreferences.autoWallpaperPreferences.copy(
            setDifferentWallpapers = !useSameSources,
        ),
    )

    fun updateLightDarkEnabled(
        lightDarkEnabled: Boolean,
        target: WallpaperTarget,
    ) {
        val autoWallpaperPreferences = uiState.value.appPreferences.autoWallpaperPreferences
        updateAutoWallpaperPrefs(
            when (target) {
                WallpaperTarget.HOME -> autoWallpaperPreferences.copy(
                    lightDarkEnabled = lightDarkEnabled,
                )
                WallpaperTarget.LOCKSCREEN -> autoWallpaperPreferences.copy(
                    lsLightDarkEnabled = lightDarkEnabled,
                )
            },
        )
    }

    fun updateUseDarkWithExtraDim(
        useDarkWithExtraDim: Boolean,
        target: WallpaperTarget,
    ) {
        val autoWallpaperPreferences = uiState.value.appPreferences.autoWallpaperPreferences
        updateAutoWallpaperPrefs(
            when (target) {
                WallpaperTarget.HOME -> autoWallpaperPreferences.copy(
                    useDarkWithExtraDim = useDarkWithExtraDim,
                )
                WallpaperTarget.LOCKSCREEN -> autoWallpaperPreferences.copy(
                    lsUseDarkWithExtraDim = useDarkWithExtraDim,
                )
            },
        )
    }

    fun updateSavedSearchEnabled(
        savedSearchEnabled: Boolean,
        target: WallpaperTarget,
    ) {
        val state = uiState.value
        val autoWallpaperPreferences = state.appPreferences.autoWallpaperPreferences
        val savedSearches = state.savedSearches
        updateAutoWallpaperPrefs(
            when (target) {
                WallpaperTarget.HOME -> {
                    val newSavedSearchIds = autoWallpaperPreferences.savedSearchIds.ifEmpty {
                        setOf(savedSearches.first().id)
                    }
                    autoWallpaperPreferences.copy(
                        savedSearchEnabled = savedSearchEnabled,
                        savedSearchIds = newSavedSearchIds,
                    )
                }
                WallpaperTarget.LOCKSCREEN -> {
                    val newSavedSearchIds = autoWallpaperPreferences.lsSavedSearchIds.ifEmpty {
                        setOf(savedSearches.first().id)
                    }
                    autoWallpaperPreferences.copy(
                        lsSavedSearchEnabled = savedSearchEnabled,
                        lsSavedSearchIds = newSavedSearchIds,
                    )
                }
            },
        )
    }

    fun updateSavedSearchIds(
        savedSearchIds: Set<Long>,
        target: WallpaperTarget,
    ) {
        val autoWallpaperPreferences = uiState.value.appPreferences.autoWallpaperPreferences
        updateAutoWallpaperPrefs(
            when (target) {
                WallpaperTarget.HOME -> autoWallpaperPreferences.copy(
                    savedSearchIds = savedSearchIds,
                )
                WallpaperTarget.LOCKSCREEN -> autoWallpaperPreferences.copy(
                    lsSavedSearchIds = savedSearchIds,
                )
            },
        )
    }

    fun updateFavoritesEnabled(
        favoritesEnabled: Boolean,
        target: WallpaperTarget,
    ) {
        val autoWallpaperPreferences = uiState.value.appPreferences.autoWallpaperPreferences
        updateAutoWallpaperPrefs(
            when (target) {
                WallpaperTarget.HOME -> autoWallpaperPreferences.copy(
                    favoritesEnabled = favoritesEnabled,
                )
                WallpaperTarget.LOCKSCREEN -> autoWallpaperPreferences.copy(
                    lsFavoritesEnabled = favoritesEnabled,
                )
            },
        )
    }

    fun updateLocalEnabled(
        localEnabled: Boolean,
        target: WallpaperTarget,
    ) {
        val state = uiState.value
        val autoWallpaperPreferences = state.appPreferences.autoWallpaperPreferences
        val localDirs = state.localDirectories
        updateAutoWallpaperPrefs(
            when (target) {
                WallpaperTarget.HOME -> {
                    val newUris = autoWallpaperPreferences.localDirs.ifEmpty {
                        setOf(localDirs.first().uri)
                    }
                    autoWallpaperPreferences.copy(
                        localEnabled = localEnabled,
                        localDirs = newUris,
                    )
                }
                WallpaperTarget.LOCKSCREEN -> {
                    val newUris = autoWallpaperPreferences.lsLocalDirs.ifEmpty {
                        setOf(localDirs.first().uri)
                    }
                    autoWallpaperPreferences.copy(
                        lsLocalEnabled = localEnabled,
                        lsLocalDirs = newUris,
                    )
                }
            },
        )
    }

    fun updateSelectedLocalDirs(
        localDirs: Set<Uri>,
        target: WallpaperTarget,
    ) {
        val autoWallpaperPreferences = uiState.value.appPreferences.autoWallpaperPreferences
        updateAutoWallpaperPrefs(
            when (target) {
                WallpaperTarget.HOME -> autoWallpaperPreferences.copy(
                    localDirs = localDirs,
                )
                WallpaperTarget.LOCKSCREEN -> autoWallpaperPreferences.copy(
                    lsLocalDirs = localDirs,
                )
            },
        )
    }

    private fun updateAutoWallpaperPrefs(
        autoWallpaperPreferences: AutoWallpaperPreferences,
    ) = viewModelScope.launch {
        updateAutoWallpaperPrefs(
            context = application,
            appPreferencesRepository = appPreferencesRepository,
            prevAppPreferences = uiState.value.appPreferences,
            newAutoWallpaperPreferences = autoWallpaperPreferences.copy(
                enabled = autoWallpaperPreferences.anySourceEnabled,
            ),
        )
    }
}

@Partialize
data class ManageAutoWallpaperSourcesUiState(
    val appPreferences: AppPreferences = AppPreferences(),
    val autoWallpaperEnabled: Boolean = false,
    val useSameSources: Boolean = true,
    val hasLightDarkWallpapers: Boolean = false,
    val savedSearches: ImmutableList<SavedSearch> = persistentListOf(),
    val hasFavorites: Boolean = false,
    val localDirectories: ImmutableList<LocalDirectory> = persistentListOf(),
    val homeScreenSources: AutoWallpaperSources = AutoWallpaperSources(),
    val lockScreenSources: AutoWallpaperSources = AutoWallpaperSources(),
)

data class AutoWallpaperSources(
    val savedSearchEnabled: Boolean = false,
    val savedSearchIds: Set<Long> = emptySet(),
    val favoritesEnabled: Boolean = false,
    val localEnabled: Boolean = false,
    val localDirs: Set<Uri> = emptySet(),
    val lightDarkEnabled: Boolean = false,
    val useDarkWithExtraDim: Boolean = false,
)
