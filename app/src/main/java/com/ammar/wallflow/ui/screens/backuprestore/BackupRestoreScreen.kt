package com.ammar.wallflow.ui.screens.backuprestore

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.ammar.wallflow.MIME_TYPE_JSON
import com.ammar.wallflow.R
import com.ammar.wallflow.extensions.safeLaunch
import com.ammar.wallflow.navigation.AppNavGraphs.BackupRestoreNavGraph
import com.ammar.wallflow.ui.common.TopBar
import com.ammar.wallflow.utils.backupFileName
import com.ramcosta.composedestinations.annotation.Destination

@OptIn(ExperimentalMaterial3Api::class)
@Destination<BackupRestoreNavGraph>(
    start = true,
)
@Composable
fun BackupRestoreScreen(
    navController: NavController,
    viewModel: BackupRestoreViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(MIME_TYPE_JSON),
    ) {
        viewModel.updateBackupOptions(
            uiState.backupOptions.copy(
                file = it ?: return@rememberLauncherForActivityResult,
            ),
        )
    }
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) {
        viewModel.setRestoreFile(
            it ?: return@rememberLauncherForActivityResult,
        )
    }

    LaunchedEffect(context, uiState.showSnackbar) {
        val snackbarType = uiState.showSnackbar ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(
            message = when (snackbarType) {
                SnackbarType.BACKUP_SUCCESS -> context.getString(R.string.backup_done)
                SnackbarType.BACKUP_FAIL -> context.getString(R.string.backup_failed)
                SnackbarType.RESTORE_SUCCESS -> context.getString(R.string.restore_done)
                SnackbarType.RESTORE_FAIL -> context.getString(R.string.restore_failed)
            },
        )
        viewModel.showSnackbar(null)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopBar(
                navController = navController,
                title = {
                    Text(
                        text = stringResource(R.string.backup_and_restore),
                        maxLines = 1,
                    )
                },
                showBackButton = true,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) {
        BackupRestoreScreenContent(
            modifier = Modifier.padding(it),
            onBackupClicked = { viewModel.showBackupDialog(true) },
            onRestoreClicked = { viewModel.showRestoreDialog(true) },
        )
    }
    if (uiState.showBackupDialog) {
        BackupDialog(
            options = uiState.backupOptions,
            backupProgress = uiState.backupProgress,
            onOptionsChange = viewModel::updateBackupOptions,
            onFileInputClicked = { createDocumentLauncher.safeLaunch(context, backupFileName) },
            onBackupClick = viewModel::performBackup,
            onDismissRequest = {
                if (uiState.backupProgress != null) {
                    // backup in progress
                    return@BackupDialog
                }
                viewModel.showBackupDialog(false)
            },
        )
    }
    if (uiState.showRestoreDialog) {
        RestoreDialog(
            summary = uiState.restoreSummary,
            options = uiState.restoreOptions,
            parsingJson = uiState.parsingRestoreJson,
            restoreProgress = uiState.restoreProgress,
            exception = uiState.restoreException,
            onOptionsChange = viewModel::updateRestoreOptions,
            onFileInputClicked = { openDocumentLauncher.safeLaunch(context, arrayOf("*/*")) },
            onRestoreClick = viewModel::performRestore,
            onDismissRequest = {
                if (uiState.restoreProgress != null) {
                    // restore in progress
                    return@RestoreDialog
                }
                viewModel.showRestoreDialog(false)
            },
        )
    }
}
