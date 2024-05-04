package com.ammar.wallflow.ui.common

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation.Url
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ammar.wallflow.ISSUE_TRACKER_URL
import com.ammar.wallflow.R
import com.ammar.wallflow.extensions.getScreenResolution
import com.ammar.wallflow.extensions.toAnnotatedString
import com.ammar.wallflow.extensions.toDp
import com.ammar.wallflow.ui.theme.WallFlowTheme
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import org.acra.ReportField

@OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalMaterial3Api::class,
)
@Composable
fun CrashReportDialog(
    modifier: Modifier = Modifier,
    reportData: ImmutableMap<ReportField, String> = persistentMapOf(),
    enableAcra: Boolean = true,
    onEnableAcraChange: (Boolean) -> Unit = {},
    onSendClick: () -> Unit = {},
    onDismissRequest: () -> Unit = {},
) {
    val context = LocalContext.current
    var reportOpen by rememberSaveable { mutableStateOf(false) }

    BasicAlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
    ) {
        UnpaddedAlertDialogContent(
            title = { Text(text = stringResource(R.string.oops)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(
                        state = rememberScrollState(),
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp),
                    ) {
                        Text(text = stringResource(R.string.crash_report_line_1))
                        Spacer(modifier = Modifier.requiredHeight(8.dp))
                        Text(
                            text = buildAnnotatedString {
                                append(stringResource(R.string.crash_report_line_2))
                                withLink(link = Url(url = ISSUE_TRACKER_URL)) {
                                    append(ISSUE_TRACKER_URL)
                                }
                            },
                        )
                        Spacer(modifier = Modifier.requiredHeight(16.dp))
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(8.dp),
                                ),
                        ) {
                            val transition = updateTransition(reportOpen, label = "open state")
                            val rotation by transition.animateFloat(label = "rotation") { open ->
                                if (open) 180f else 0f
                            }
                            Row(
                                modifier = Modifier
                                    .clickable { reportOpen = !reportOpen }
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(R.string.report_data),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                                IconButton(onClick = { }) {
                                    Icon(
                                        modifier = Modifier.size(ButtonDefaults.IconSize),
                                        painter = painterResource(
                                            R.drawable.baseline_content_copy_24,
                                        ),
                                        contentDescription = null,
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(
                                    modifier = Modifier
                                        .rotate(rotation)
                                        .size(24.dp),
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = null,
                                )
                            }
                            transition.AnimatedVisibility(
                                visible = { targetSelected -> targetSelected },
                                enter = expandVertically(),
                                exit = shrinkVertically(),
                            ) {
                                val bg = MaterialTheme.colorScheme.surfaceDim
                                SelectionContainer {
                                    val verticalScrollState = rememberScrollState()
                                    val horizontalScrollState = rememberScrollState()
                                    val screenHeight = context.getScreenResolution().height

                                    Text(
                                        modifier = Modifier
                                            .drawHorizontalScrollbar(state = horizontalScrollState)
                                            .drawVerticalScrollbar(state = verticalScrollState)
                                            .fillMaxWidth()
                                            .heightIn(
                                                max = (screenHeight / 3)
                                                    .coerceAtLeast(300)
                                                    .toDp(),
                                            )
                                            .background(bg)
                                            .verticalScroll(state = verticalScrollState)
                                            .horizontalScroll(state = horizontalScrollState)
                                            .padding(8.dp),
                                        text = reportData.toAnnotatedString(
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp,
                                            ).toSpanStyle(),
                                        ),
                                        color = contentColorFor(bg),
                                        softWrap = false,
                                    )
                                }
                            }
                        }
                    }
                    CheckboxWithLabel(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        checked = !enableAcra,
                        label = { Text(text = stringResource(R.string.never_show_again)) },
                        onCheckedChange = { onEnableAcraChange(!it) },
                    )
                }
            },
            buttons = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(text = stringResource(R.string.dismiss))
                    }
                    TextButton(
                        enabled = enableAcra,
                        onClick = onSendClick,
                    ) {
                        Text(text = stringResource(R.string.send_email))
                    }
                }
            },
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewCrashReportDialog() {
    WallFlowTheme {
        Surface {
            CrashReportDialog(
                reportData = persistentMapOf(
                    ReportField.REPORT_ID to "test value 1",
                    ReportField.ANDROID_VERSION to "test value 2",
                ),
            )
        }
    }
}
