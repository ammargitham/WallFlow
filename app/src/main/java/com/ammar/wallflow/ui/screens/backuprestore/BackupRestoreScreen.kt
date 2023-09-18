package com.ammar.wallflow.ui.screens.backuprestore

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
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
import com.ammar.wallflow.ui.common.LocalSystemController
import com.ammar.wallflow.ui.common.TopBar
import com.ammar.wallflow.ui.common.bottomWindowInsets
import com.ammar.wallflow.ui.common.bottombar.LocalBottomBarController
import com.ammar.wallflow.ui.common.mainsearch.LocalMainSearchBarController
import com.ammar.wallflow.utils.backupFileName
import com.ramcosta.composedestinations.annotation.Destination

@OptIn(ExperimentalMaterial3Api::class)
@Destination
@Composable
fun BackupRestoreScreen(
    navController: NavController,
    viewModel: BackupRestoreViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchBarController = LocalMainSearchBarController.current
    val systemController = LocalSystemController.current
    val bottomBarController = LocalBottomBarController.current
    val context = LocalContext.current
    val systemState by systemController.state
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

    LaunchedEffect(Unit) {
        searchBarController.update { it.copy(visible = false) }
    }

    LaunchedEffect(systemState.isExpanded) {
        bottomBarController.update { it.copy(visible = systemState.isExpanded) }
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = bottomWindowInsets,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
        ) {
            if (!systemState.isExpanded) {
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
            }
            BackupRestoreScreenContent(
                onBackupClicked = { viewModel.showBackupDialog(true) },
                onRestoreClicked = { viewModel.showRestoreDialog(true) },
            )
        }
        if (uiState.showBackupDialog) {
            BackupDialog(
                options = uiState.backupOptions,
                backupProgress = uiState.backupProgress,
                onOptionsChange = viewModel::updateBackupOptions,
                onFileInputClicked = { createDocumentLauncher.launch(backupFileName) },
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
                onFileInputClicked = { openDocumentLauncher.launch(arrayOf(MIME_TYPE_JSON)) },
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
}
