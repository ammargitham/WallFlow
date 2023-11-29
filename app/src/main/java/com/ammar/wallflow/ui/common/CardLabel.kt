package com.ammar.wallflow.ui.common

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.ui.theme.WallFlowTheme

@Composable
fun CardLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier
            .clip(CardDefaults.shape)
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(8.dp),
        text = text,
        color = Color.White,
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewCardLabel() {
    WallFlowTheme {
        Surface {
            CardLabel(
                modifier = Modifier.padding(8.dp),
                text = "test",
            )
        }
    }
}
