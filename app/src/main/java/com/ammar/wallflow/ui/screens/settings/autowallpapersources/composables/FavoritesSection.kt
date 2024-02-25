package com.ammar.wallflow.ui.screens.settings.autowallpapersources.composables

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItem
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
import com.ammar.wallflow.DISABLED_ALPHA
import com.ammar.wallflow.R
import com.ammar.wallflow.ui.common.SectionHeader
import com.ammar.wallflow.ui.theme.WallFlowTheme

@Composable
internal fun FavoritesSection(
    favoritesEnabled: Boolean = false,
    hasFavorites: Boolean = false,
    onChangeFavoritesEnabled: (Boolean) -> Unit = {},
) {
    val alpha = if (hasFavorites) 1f else DISABLED_ALPHA

    SectionHeader(text = stringResource(R.string.favorites))
    ListItem(
        modifier = Modifier.clickable(
            enabled = hasFavorites,
        ) { onChangeFavoritesEnabled(!favoritesEnabled) },
        headlineContent = {
            Text(
                text = stringResource(R.string.use_favorites),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            )
        },
        supportingContent = if (!hasFavorites) {
            {
                Text(
                    text = stringResource(R.string.no_favorites),
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
                enabled = hasFavorites,
                checked = favoritesEnabled && hasFavorites,
                onCheckedChange = onChangeFavoritesEnabled,
            )
        },
    )
}

private data class FavoritesSectionParameters(
    val favoritesEnabled: Boolean = false,
    val hasFavorites: Boolean = false,
)

private class FavoritesSectionPPP : CollectionPreviewParameterProvider<FavoritesSectionParameters>(
    listOf(
        FavoritesSectionParameters(),
        FavoritesSectionParameters(
            favoritesEnabled = true,
        ),
        FavoritesSectionParameters(
            favoritesEnabled = true,
            hasFavorites = true,
        ),
    ),
)

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewFavoritesSection(
    @PreviewParameter(provider = FavoritesSectionPPP::class) parameters: FavoritesSectionParameters,
) {
    WallFlowTheme {
        Surface {
            FavoritesSection(
                favoritesEnabled = parameters.favoritesEnabled,
                hasFavorites = parameters.hasFavorites,
            )
        }
    }
}
