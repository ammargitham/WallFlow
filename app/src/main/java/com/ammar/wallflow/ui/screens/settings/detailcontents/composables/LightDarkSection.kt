package com.ammar.wallflow.ui.screens.settings.detailcontents.composables

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.DISABLED_ALPHA
import com.ammar.wallflow.R
import com.ammar.wallflow.ui.common.SectionHeader
import com.ammar.wallflow.ui.screens.settings.composables.SettingsExtraListItem
import com.ammar.wallflow.ui.theme.WallFlowTheme

@Composable
internal fun ColumnScope.LightDarkSection(
    hasLightDarkWallpapers: Boolean = false,
    lightDarkEnabled: Boolean = false,
    useDarkWithExtraDim: Boolean = false,
    isExpanded: Boolean = false,
    onChangeLightDarkEnabled: (Boolean) -> Unit = {},
    onChangeUseDarkWithExtraDim: (Boolean) -> Unit = {},
) {
    val alpha = if (hasLightDarkWallpapers) 1f else DISABLED_ALPHA

    SectionHeader(
        modifier = Modifier.padding(top = 8.dp),
        text = stringResource(R.string.light_dark),
    )
    SettingsExtraListItem(
        modifier = Modifier.clickable(
            enabled = hasLightDarkWallpapers,
        ) { onChangeLightDarkEnabled(!lightDarkEnabled) },
        isExpanded = isExpanded,
        headlineContent = {
            Text(
                text = stringResource(R.string.use_light_dark_wallpapers),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            )
        },
        supportingContent = if (!hasLightDarkWallpapers) {
            {
                Text(
                    text = stringResource(R.string.no_light_dark),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = alpha,
                    ),
                )
            }
        } else {
            null
        },
        trailingContent = {
            Switch(
                enabled = hasLightDarkWallpapers,
                checked = lightDarkEnabled && hasLightDarkWallpapers,
                onCheckedChange = onChangeLightDarkEnabled,
            )
        },
    )
    AnimatedVisibility(visible = lightDarkEnabled && hasLightDarkWallpapers) {
        SettingsExtraListItem(
            modifier = Modifier.clickable(
                enabled = lightDarkEnabled && hasLightDarkWallpapers,
            ) { onChangeUseDarkWithExtraDim(!useDarkWithExtraDim) },
            isExpanded = isExpanded,
            headlineContent = {
                Text(text = stringResource(R.string.use_dark_with_extra_dim))
            },
            supportingContent = {
                Text(text = stringResource(R.string.use_dark_with_extra_dim_desc))
            },
            trailingContent = {
                Switch(
                    checked = useDarkWithExtraDim,
                    onCheckedChange = onChangeUseDarkWithExtraDim,
                )
            },
        )
    }
}

private data class LightDarkSectionParameters(
    val lightDarkEnabled: Boolean = false,
    val hasLightDarkWallpapers: Boolean = false,
)

private class LightDarkSectionPPP : CollectionPreviewParameterProvider<LightDarkSectionParameters>(
    listOf(
        LightDarkSectionParameters(),
        LightDarkSectionParameters(
            lightDarkEnabled = true,
            hasLightDarkWallpapers = true,
        ),
        LightDarkSectionParameters(
            lightDarkEnabled = false,
            hasLightDarkWallpapers = true,
        ),
        LightDarkSectionParameters(
            lightDarkEnabled = true,
            hasLightDarkWallpapers = false,
        ),
    ),
)

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewLightDarkSection(
    @PreviewParameter(provider = LightDarkSectionPPP::class) parameters: LightDarkSectionParameters,
) {
    WallFlowTheme {
        Surface {
            Column {
                LightDarkSection(
                    hasLightDarkWallpapers = parameters.hasLightDarkWallpapers,
                    lightDarkEnabled = parameters.lightDarkEnabled,
                )
            }
        }
    }
}
