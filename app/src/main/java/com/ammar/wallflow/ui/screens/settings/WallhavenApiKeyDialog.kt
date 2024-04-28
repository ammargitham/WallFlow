package com.ammar.wallflow.ui.screens.settings

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ammar.wallflow.R
import com.ammar.wallflow.extensions.trimAll
import com.ammar.wallflow.navigation.AppNavGraphs.RootNavGraph
import com.ammar.wallflow.ui.theme.WallFlowTheme
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.spec.DestinationStyle

@Destination<RootNavGraph>(
    style = DestinationStyle.Dialog::class,
)
@Composable
fun WallhavenApiKeyDialog(
    viewModel: WallhavenApiKeyViewModel = hiltViewModel(),
    navigator: DestinationsNavigator,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    WallhavenApiKeyDialogContent(
        wallhavenApiKey = uiState.apiKey,
        onConfirm = {
            viewModel.updateWallhavenApiKey(it)
            navigator.navigateUp()
        },
        onDismiss = { navigator.navigateUp() },
    )
}

@Composable
fun WallhavenApiKeyDialogContent(
    modifier: Modifier = Modifier,
    wallhavenApiKey: String,
    onConfirm: (apiKey: String) -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    var apiKey by remember(wallhavenApiKey) { mutableStateOf(wallhavenApiKey) }

    Surface(
        shape = AlertDialogDefaults.shape,
        color = AlertDialogDefaults.containerColor,
        tonalElevation = AlertDialogDefaults.TonalElevation,
    ) {
        Column(
            modifier = modifier.padding(24.dp),
        ) {
            Box(
                Modifier
                    .padding(bottom = 16.dp)
                    .align(Alignment.Start),
            ) {
                Text(
                    text = stringResource(R.string.wallhaven_api_key),
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
            Box(
                Modifier
                    .weight(weight = 1f, fill = false)
                    .padding(bottom = 24.dp)
                    .align(Alignment.Start),
            ) {
                OutlinedTextField(
                    value = apiKey,
                    maxLines = 1,
                    onValueChange = { apiKey = it },
                    textStyle = MaterialTheme.typography.bodyMedium,
                )
            }
            Box(modifier = Modifier.align(Alignment.End)) {
                Row {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(R.string.dismiss))
                    }
                    TextButton(onClick = { onConfirm(apiKey.trimAll()) }) {
                        Text(text = stringResource(R.string.confirm))
                    }
                }
            }
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewWallhavenApiKeyAlertDialog() {
    WallFlowTheme {
        Surface {
            WallhavenApiKeyDialogContent(
                wallhavenApiKey = "",
            )
        }
    }
}
