package com.ammar.wallflow.ui.screens.redditauth

import android.webkit.CookieManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ammar.wallflow.data.repository.AppPreferencesRepository
import com.ammar.wallflow.data.repository.GlobalErrorsRepository
import com.ammar.wallflow.data.repository.GlobalErrorsRepository.GlobalErrorType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class RedditAuthViewModel @Inject constructor(
    private val appPreferencesRepository: AppPreferencesRepository,
    private val globalErrorsRepository: GlobalErrorsRepository,
) : ViewModel() {
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState = appPreferencesRepository.appPreferencesFlow.mapLatest { appPreferences ->
        RedditAuthUiState(
            redditCookie = appPreferences.redditCookie,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RedditAuthUiState(),
    )

    fun saveCookie(cookie: String) {
        viewModelScope.launch {
            CookieManager.getInstance().removeAllCookies { }
            appPreferencesRepository.updateRedditCookie(cookie)
            globalErrorsRepository.removeErrorByType(GlobalErrorType.REDDIT_UNAUTHORISED)
        }
    }
}

data class RedditAuthUiState(
    val redditCookie: String = "",
)
