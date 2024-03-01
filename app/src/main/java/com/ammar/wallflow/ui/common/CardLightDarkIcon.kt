package com.ammar.wallflow.ui.common

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.R
import com.ammar.wallflow.model.LightDarkType
import com.ammar.wallflow.model.isDark
import com.ammar.wallflow.model.isExtraDim
import com.ammar.wallflow.model.isLight
import com.ammar.wallflow.ui.theme.WallFlowTheme

@Composable
fun CardLightDarkIcon(
    modifier: Modifier = Modifier,
    typeFlags: Int = LightDarkType.UNSPECIFIED,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.5f))
            .size(24.dp),
    ) {
        Icon(
            modifier = Modifier
                .size(16.dp)
                .align(Alignment.Center),
            painter = painterResource(
                when {
                    typeFlags.isLight() -> R.drawable.baseline_light_mode_24
                    typeFlags.isExtraDim() -> R.drawable.dark_extra_dim
                    typeFlags.isDark() -> R.drawable.baseline_dark_mode_24
                    else -> R.drawable.baseline_light_dark
                },
            ),
            contentDescription = stringResource(
                when {
                    typeFlags.isLight() -> R.string.light
                    typeFlags.isExtraDim() -> R.string.extra_dim
                    typeFlags.isDark() -> R.string.dark
                    else -> R.string.light_dark
                },
            ),
            tint = Color.White,
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewCardLightDarkIcon() {
    WallFlowTheme {
        Surface {
            CardLightDarkIcon()
        }
    }
}
