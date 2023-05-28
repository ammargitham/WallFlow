package com.ammar.havenwalls.ui.common

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ammar.havenwalls.model.Purity
import com.ammar.havenwalls.model.Tag
import com.ammar.havenwalls.ui.theme.HavenWallsTheme
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material.fade
import com.google.accompanist.placeholder.material.placeholder
import kotlinx.datetime.Clock

@Composable
fun TagChip(
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    tag: Tag,
    onClick: () -> Unit = {},
) {
    Box(
        modifier = modifier.width(IntrinsicSize.Max),
    ) {
        AssistChip(
            onClick = onClick,
            label = { Text(text = "#${tag.name}") },
        )
        if (loading) {
            Box(
                modifier = Modifier
                    .height(AssistChipDefaults.Height)
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .placeholder(
                        color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
                        visible = true,
                        highlight = PlaceholderHighlight.fade(),
                        shape = AssistChipDefaults.shape,
                    )
            )
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewTagChip() {
    HavenWallsTheme {
        Surface {
            Row(
                modifier = Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TagChip(
                    loading = true,
                    tag = Tag(
                        id = 1,
                        name = "Test-1",
                        alias = emptyList(),
                        categoryId = 1,
                        category = "",
                        purity = Purity.SFW,
                        createdAt = Clock.System.now(),
                    )
                )
                TagChip(
                    tag = Tag(
                        id = 1,
                        name = "Test-1",
                        alias = emptyList(),
                        categoryId = 1,
                        category = "",
                        purity = Purity.SFW,
                        createdAt = Clock.System.now(),
                    )
                )
            }
        }
    }
}
