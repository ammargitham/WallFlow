package com.ammar.wallflow.activities.crashreport

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ammar.wallflow.extensions.TAG
import com.ammar.wallflow.ui.common.CrashReportDialog
import com.ammar.wallflow.ui.theme.WallFlowTheme
import com.ammar.wallflow.utils.CrashReportHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CrashReportActivity : ComponentActivity() {
    private lateinit var helper: CrashReportHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            helper = CrashReportHelper(this, intent)
            helper.reportData // try to read the report data to fail fast if report is corrupted
            setContent {
                WallFlowTheme {
                    CrashReportContent()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "onCreate: ", e)
            helper.cancelReports()
            finish()
        }
    }

    @Composable
    private fun CrashReportContent() {
        val viewModel = hiltViewModel<CrashReportViewModel>()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            viewModel.setReportData(helper.reportData)
        }

        CrashReportDialog(
            reportData = uiState.reportData,
            enableAcra = uiState.acraEnabled,
            onEnableAcraChange = viewModel::setEnableAcra,
            onSendClick = {
                helper.sendCrash()
                finishAfterTransition()
            },
            onDismissRequest = {
                helper.cancelReports()
                finishAfterTransition()
            },
        )
    }
}
