package com.ammar.wallflow.activities.crashreport

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ammar.wallflow.data.repository.AppPreferencesRepository
import com.ammar.wallflow.extensions.toReportFieldMap
import com.github.materiiapps.partial.Partialize
import com.github.materiiapps.partial.partial
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.acra.ReportField
import org.acra.data.CrashReportData

@HiltViewModel
class CrashReportViewModel @Inject constructor(
    private val appPreferencesRepository: AppPreferencesRepository,
) : ViewModel() {
    private val localUiState = MutableStateFlow(CrashReportUiStatePartial())

    val uiState = combine(
        localUiState,
        appPreferencesRepository.appPreferencesFlow,
    ) { local, appPreferences ->
        local.merge(
            CrashReportUiState(
                acraEnabled = appPreferences.acraEnabled,
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CrashReportUiState(),
    )

    fun setReportData(reportData: CrashReportData) = localUiState.update {
        it.copy(
            reportData = partial(
                reportData
                    .toReportFieldMap()
                    .toImmutableMap(),
            ),
        )
    }

    fun setEnableAcra(enable: Boolean) = viewModelScope.launch {
        appPreferencesRepository.updateAcraEnabled(enable)
    }
}

@Partialize
@Stable
data class CrashReportUiState(
    val reportData: ImmutableMap<ReportField, String> = persistentMapOf(),
    val acraEnabled: Boolean = true,
)
