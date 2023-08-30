package com.ammar.wallflow.ui.screens.osl

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ammar.wallflow.R
import com.ammar.wallflow.extensions.filterNotNullValues
import com.ammar.wallflow.ui.common.LocalSystemController
import com.ammar.wallflow.ui.common.TopBar
import com.ammar.wallflow.ui.common.bottomWindowInsets
import com.ammar.wallflow.ui.common.bottombar.LocalBottomBarController
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.util.withContext
import com.ramcosta.composedestinations.annotation.Destination

@OptIn(ExperimentalMaterial3Api::class)
@Destination
@Composable
fun OSLibrariesScreen(
    navController: NavController,
) {
    val bottomBarController = LocalBottomBarController.current
    val systemController = LocalSystemController.current
    val context = LocalContext.current
    val systemState by systemController.state
    var showLibraryDialog: Library? by rememberSaveable {
        mutableStateOf(null)
    }

    LaunchedEffect(systemState.isExpanded) {
        bottomBarController.update { it.copy(visible = systemState.isExpanded) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(bottomWindowInsets),
    ) {
        if (!systemState.isExpanded) {
            TopBar(
                navController = navController,
                title = {
                    Text(
                        text = stringResource(R.string.open_source_licenses),
                        maxLines = 1,
                    )
                },
                showBackButton = true,
            )
        }
        OSLibrariesScreenContent(
            libraries = Libs.Builder().withContext(context).build().libraries,
            onLibraryClick = { showLibraryDialog = it },
        )
    }

    showLibraryDialog?.run {
        AlertDialog(
            title = { Text(text = this.name) },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    this@run.licenses
                        .associateBy(
                            keySelector = { it.name },
                            valueTransform = { it.licenseContent },
                        )
                        .filterNotNullValues()
                        .forEach {
                            item {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(text = it.key)
                                    Text(text = it.value)
                                }
                            }
                        }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showLibraryDialog = null },
                ) {
                    Text(text = stringResource(R.string.ok))
                }
            },
            onDismissRequest = { showLibraryDialog = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OSLibrariesScreenContent(
    libraries: List<Library>,
    modifier: Modifier = Modifier,
    onLibraryClick: (Library) -> Unit = {},
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
    ) {
        items(
            items = libraries,
            key = { it.uniqueId },
        ) {
            ListItem(
                modifier = Modifier.clickable { onLibraryClick(it) },
                headlineContent = { Text(text = it.name) },
                supportingContent = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = it.developers
                                .mapNotNull { it.name }
                                .joinToString(", "),
                        )
                        Badge {
                            Text(text = it.licenses.joinToString(", ") { it.name })
                        }
                    }
                },
                trailingContent = { Text(text = it.artifactVersion ?: "") },
            )
        }
    }
}
