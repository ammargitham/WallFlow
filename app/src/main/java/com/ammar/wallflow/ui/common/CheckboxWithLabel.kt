package com.ammar.wallflow.ui.common

import android.content.res.Configuration
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.ui.theme.WallFlowTheme

@Composable
fun CheckboxWithLabel(
    modifier: Modifier = Modifier,
    checked: Boolean = false,
    label: @Composable () -> Unit = {},
    onCheckedChange: (Boolean) -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .toggleable(
                indication = LocalIndication.current,
                interactionSource = null,
                value = checked,
                onValueChange = onCheckedChange,
                role = Role.Checkbox,
            )
            .padding(horizontal = 16.dp)
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
        )
        Spacer(modifier = Modifier.requiredWidth(16.dp))
        CompositionLocalProvider(
            LocalTextStyle provides LocalTextStyle.current.merge(
                MaterialTheme.typography.bodyLarge,
            ),
            content = label,
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewCheckboxWithLabel() {
    WallFlowTheme {
        Surface {
            CheckboxWithLabel(
                label = {
                    Text(text = "Label")
                },
            )
        }
    }
}
