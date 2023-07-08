package com.ammar.wallflow.ui.common

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ammar.wallflow.R
import com.ammar.wallflow.model.Avatar
import com.ammar.wallflow.model.Uploader
import com.ammar.wallflow.ui.theme.WallFlowTheme

@Composable
fun UploaderChip(
    modifier: Modifier = Modifier,
    uploader: Uploader,
    onClick: () -> Unit = {},
) {
    AssistChip(
        modifier = modifier,
        onClick = onClick,
        leadingIcon = {
            AsyncImage(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape),
                placeholder = forwardingPainter(
                    painter = painterResource(R.drawable.outline_account_circle_24),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                ),
                model = uploader.avatar.medium,
                contentScale = ContentScale.Crop,
                contentDescription = "${uploader.username}'s avatar",
            )
        },
        label = { Text(text = uploader.username) },
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewUploaderChip() {
    WallFlowTheme {
        Surface {
            Row(
                modifier = Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                UploaderChip(
                    uploader = Uploader(
                        username = "test",
                        group = "",
                        avatar = Avatar(
                            large = "",
                            medium = "",
                            small = "",
                            tiny = "",
                        ),
                    )
                )
            }
        }
    }
}
