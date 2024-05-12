package com.ammar.wallflow.ui.screens.osl

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ammar.wallflow.R
import com.ammar.wallflow.extensions.filterNotNullValues
import com.ammar.wallflow.model.SerializableLibrary
import com.ammar.wallflow.model.toSerializableLibrary
import com.ammar.wallflow.navigation.AppNavGraphs.OpenSourceLicensesNavGraph
import com.ammar.wallflow.safeJson
import com.ammar.wallflow.ui.common.TopBar
import com.ammar.wallflow.ui.theme.WallFlowTheme
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Developer
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.entity.License
import com.mikepenz.aboutlibraries.util.withContext
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.serialization.encodeToString

@OptIn(ExperimentalMaterial3Api::class)
@Destination<OpenSourceLicensesNavGraph>(
    start = true,
)
@Composable
fun OSLibrariesScreen(
    navController: NavController,
) {
    val context = LocalContext.current
    var showLibraryDialog: Library? by rememberSaveable(
        saver = MutableLibrarySaver,
    ) { mutableStateOf(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
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
        },
    ) {
        OSLibrariesScreenContent(
            modifier = Modifier.padding(it),
            libraries = Libs.Builder().withContext(context).build().libraries,
            onLibraryClick = { lib -> showLibraryDialog = lib },
        )
    }

    showLibraryDialog?.run {
        LicenseDialog(
            library = this,
            onDismissRequest = { showLibraryDialog = null },
        )
    }
}

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

@Composable
private fun LicenseDialog(
    library: Library,
    onDismissRequest: () -> Unit = {},
) {
    AlertDialog(
        title = {
            Column {
                Text(text = library.name)
                library.website?.let {
                    Text(
                        text = buildAnnotatedString {
                            withLink(link = LinkAnnotation.Url(url = it)) {
                                append(it)
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                library.licenses
                    .associateBy(
                        keySelector = { it.name },
                        valueTransform = { it.licenseContent },
                    )
                    .filterNotNullValues()
                    .forEach {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = it.key,
                                    textAlign = TextAlign.Center,
                                )
                                Text(text = it.value)
                            }
                        }
                    }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(R.string.ok))
            }
        },
        onDismissRequest = onDismissRequest,
    )
}

private val MutableLibrarySaver = Saver<MutableState<Library?>, String>(
    save = { safeJson.encodeToString(it.value?.toSerializableLibrary()) },
    restore = {
        val value = safeJson.decodeFromString<SerializableLibrary?>(it)
        mutableStateOf(value?.toLibrary())
    },
)

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewLicenseDialog() {
    WallFlowTheme {
        Surface {
            LicenseDialog(
                library = Library(
                    uniqueId = "1",
                    artifactVersion = "1.0.0",
                    name = "Test Library",
                    description = "Desc",
                    website = "http://example.com",
                    licenses = persistentSetOf(
                        License(
                            name = "Apache",
                            url = "http://licence.com",
                            year = "2024",
                            spdxId = null,
                            licenseContent = "This is the content",
                            hash = "",
                        ),
                    ),
                    developers = persistentListOf(
                        Developer(
                            name = "dev1",
                            organisationUrl = null,
                        ),
                    ),
                    organization = null,
                    scm = null,
                ),
            )
        }
    }
}
