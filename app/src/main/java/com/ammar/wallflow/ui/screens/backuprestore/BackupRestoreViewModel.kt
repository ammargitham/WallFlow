package com.ammar.wallflow.ui.screens.backuprestore

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ammar.wallflow.data.db.dao.FavoriteDao
import com.ammar.wallflow.data.db.dao.LightDarkDao
import com.ammar.wallflow.data.db.dao.ViewedDao
import com.ammar.wallflow.data.db.dao.search.SavedSearchDao
import com.ammar.wallflow.data.db.dao.wallpaper.RedditWallpapersDao
import com.ammar.wallflow.data.db.dao.wallpaper.WallhavenWallpapersDao
import com.ammar.wallflow.data.repository.AppPreferencesRepository
import com.ammar.wallflow.data.repository.FavoritesRepository
import com.ammar.wallflow.data.repository.LightDarkRepository
import com.ammar.wallflow.data.repository.SavedSearchRepository
import com.ammar.wallflow.data.repository.ViewedRepository
import com.ammar.wallflow.data.repository.reddit.RedditRepository
import com.ammar.wallflow.data.repository.wallhaven.WallhavenRepository
import com.ammar.wallflow.extensions.TAG
import com.ammar.wallflow.extensions.readFromUri
import com.ammar.wallflow.extensions.writeToUri
import com.ammar.wallflow.model.backup.BackupOptions
import com.ammar.wallflow.model.backup.FileNotFoundException as BackupFileNotFoundException
import com.ammar.wallflow.model.backup.InvalidJsonException
import com.ammar.wallflow.model.backup.RestoreException
import com.ammar.wallflow.model.backup.RestoreSummary
import com.ammar.wallflow.utils.getBackupV1Json
import com.ammar.wallflow.utils.readBackupJson
import com.ammar.wallflow.utils.restoreBackup
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.FileNotFoundException
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class BackupRestoreViewModel @Inject constructor(
    private val application: Application,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val favoriteDao: FavoriteDao,
    private val wallhavenWallpapersDao: WallhavenWallpapersDao,
    private val redditWallpapersDao: RedditWallpapersDao,
    private val savedSearchDao: SavedSearchDao,
    private val savedSearchRepository: SavedSearchRepository,
    private val wallhavenRepository: WallhavenRepository,
    private val redditRepository: RedditRepository,
    private val favoritesRepository: FavoritesRepository,
    private val viewedDao: ViewedDao,
    private val viewedRepository: ViewedRepository,
    private val lightDarkDao: LightDarkDao,
    private val lightDarkRepository: LightDarkRepository,
) : AndroidViewModel(
    application = application,
) {
    private val localUiState = MutableStateFlow(BackupRestoreUiState())

    val uiState = localUiState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BackupRestoreUiState(),
    )

    fun showBackupDialog(show: Boolean) = localUiState.update {
        it.copy(showBackupDialog = show)
    }

    fun showRestoreDialog(show: Boolean) = localUiState.update {
        it.copy(showRestoreDialog = show)
    }

    fun updateBackupOptions(options: BackupOptions) = localUiState.update {
        it.copy(backupOptions = options)
    }

    fun setRestoreFile(uri: Uri) {
        viewModelScope.launch {
            localUiState.update {
                it.copy(
                    parsingRestoreJson = true,
                    restoreSummary = RestoreSummary(file = uri),
                    restoreOptions = BackupOptions(file = uri),
                    restoreException = null,
                )
            }
            // delay to show reading progress
            delay(200)
            try {
                val json = application.readFromUri(uri)
                if (json == null) {
                    localUiState.update {
                        it.copy(
                            parsingRestoreJson = false,
                            restoreException = InvalidJsonException(),
                        )
                    }
                    return@launch
                }
                // delay to show reading progress
                delay(200)
                val backup = readBackupJson(json)
                val restoreSummary = backup.getRestoreSummary(uri)
                val restoreOptions = restoreSummary.getInitialRestoreOptions()
                localUiState.update {
                    it.copy(
                        parsingRestoreJson = false,
                        restoreSummary = restoreSummary,
                        restoreOptions = restoreOptions,
                        restoreException = null,
                    )
                }
            } catch (e: FileNotFoundException) {
                Log.e(TAG, "setRestoreFile: ", e)
                localUiState.update {
                    it.copy(
                        parsingRestoreJson = false,
                        restoreException = BackupFileNotFoundException(e),
                    )
                }
            } catch (e: IOException) {
                Log.e(TAG, "setRestoreFile: ", e)
                localUiState.update {
                    it.copy(
                        parsingRestoreJson = false,
                        restoreException = InvalidJsonException(e),
                    )
                }
            } catch (e: RestoreException) {
                Log.e(TAG, "setRestoreFile: ", e)
                localUiState.update {
                    it.copy(
                        parsingRestoreJson = false,
                        restoreException = e,
                    )
                }
            }
        }
    }

    fun performBackup() {
        val options = uiState.value.backupOptions
        if (options.file == null || !options.atleastOneChosen) {
            return
        }
        viewModelScope.launch {
            localUiState.update {
                it.copy(backupProgress = -1F)
            }
            val json = getBackupV1Json(
                options = options,
                appPreferencesRepository = appPreferencesRepository,
                favoriteDao = favoriteDao,
                wallhavenWallpapersDao = wallhavenWallpapersDao,
                redditWallpapersDao = redditWallpapersDao,
                savedSearchDao = savedSearchDao,
                viewedDao = viewedDao,
                lightDarkDao = lightDarkDao,
            ) ?: return@launch
            try {
                application.writeToUri(
                    uri = options.file,
                    content = json,
                )
                localUiState.update {
                    it.copy(
                        backupProgress = null,
                        showSnackbar = SnackbarType.BACKUP_SUCCESS,
                        // reset options
                        backupOptions = BackupOptions(),
                        // close the dialog
                        showBackupDialog = false,
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "performBackup: ", e)
                localUiState.update {
                    it.copy(
                        backupProgress = null,
                        showSnackbar = SnackbarType.BACKUP_FAIL,
                        // reset options
                        backupOptions = BackupOptions(),
                        // close the dialog
                        showBackupDialog = false,
                    )
                }
            }
        }
    }

    fun showSnackbar(type: SnackbarType?) = localUiState.update {
        it.copy(showSnackbar = type)
    }

    fun updateRestoreOptions(options: BackupOptions) = localUiState.update {
        it.copy(restoreOptions = options)
    }

    fun performRestore() {
        val currentUiState = uiState.value
        val options = currentUiState.restoreOptions ?: return
        val backup = currentUiState.restoreSummary?.backup ?: return
        if (options.file == null || !options.atleastOneChosen) {
            return
        }
        viewModelScope.launch {
            localUiState.update {
                it.copy(restoreProgress = -1F)
            }
            try {
                restoreBackup(
                    context = application,
                    backup = backup,
                    options = options,
                    appPreferencesRepository = appPreferencesRepository,
                    savedSearchRepository = savedSearchRepository,
                    wallhavenRepository = wallhavenRepository,
                    redditRepository = redditRepository,
                    favoritesRepository = favoritesRepository,
                    wallhavenWallpapersDao = wallhavenWallpapersDao,
                    redditWallpapersDao = redditWallpapersDao,
                    viewedRepository = viewedRepository,
                    lightDarkRepository = lightDarkRepository,
                )
                localUiState.update {
                    it.copy(
                        restoreProgress = null,
                        showSnackbar = SnackbarType.RESTORE_SUCCESS,
                        // reset options
                        restoreOptions = null,
                        // reset summary
                        restoreSummary = null,
                        // close the dialog
                        showRestoreDialog = false,
                    )
                }
            } catch (e: RestoreException) {
                Log.e(TAG, "performRestore: ", e)
                localUiState.update {
                    it.copy(
                        restoreProgress = null,
                        showSnackbar = SnackbarType.RESTORE_FAIL,
                        restoreException = e,
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "performRestore: ", e)
                localUiState.update {
                    it.copy(
                        restoreProgress = null,
                        showSnackbar = SnackbarType.RESTORE_FAIL,
                        restoreException = InvalidJsonException(e),
                    )
                }
            }
        }
    }
}

data class BackupRestoreUiState(
    val backupOptions: BackupOptions = BackupOptions(),
    val showBackupDialog: Boolean = false,
    val backupProgress: Float? = null,
    val showSnackbar: SnackbarType? = null,
    val showRestoreDialog: Boolean = false,
    val restoreOptions: BackupOptions? = null,
    val restoreSummary: RestoreSummary? = null,
    val parsingRestoreJson: Boolean = false,
    val restoreProgress: Float? = null,
    val restoreException: RestoreException? = null,
)

enum class SnackbarType {
    BACKUP_SUCCESS,
    BACKUP_FAIL,
    RESTORE_SUCCESS,
    RESTORE_FAIL,
}
