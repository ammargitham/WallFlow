package com.ammar.wallflow.ui.screens.osl

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ammar.wallflow.ui.theme.WallFlowTheme
import com.mikepenz.aboutlibraries.ui.compose.LibrariesContainer
import com.ramcosta.composedestinations.annotation.Destination

@Destination
@Composable
fun OSLibrariesScreen() {
    OSLibrariesScreenContent()
}

@Composable
fun OSLibrariesScreenContent(
    modifier: Modifier = Modifier,
) {
    LibrariesContainer(
        modifier = modifier.fillMaxSize(),
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewOSLibrariesScreenContent() {
    WallFlowTheme {
        Surface {
            OSLibrariesScreenContent()
        }
    }
}
