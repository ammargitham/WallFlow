package com.ammar.wallflow.ui.common.globalerrors

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ammar.wallflow.R
import com.ammar.wallflow.data.repository.GlobalErrorsRepository.GlobalError
import com.ammar.wallflow.data.repository.GlobalErrorsRepository.RateLimitError
import com.ammar.wallflow.data.repository.GlobalErrorsRepository.WallHavenUnauthorisedError
import com.ammar.wallflow.model.Source

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlobalErrorsColumn(
    modifier: Modifier = Modifier,
    globalErrors: List<GlobalError> = emptyList(),
    onFixWallHavenApiKeyClick: () -> Unit = {},
    onDismiss: (error: GlobalError) -> Unit = {},
) {
    LazyColumn(modifier = modifier) {
        items(globalErrors) {
            val errorMsg = when (it) {
                is WallHavenUnauthorisedError -> stringResource(R.string.invalid_api_key_provided)
                is RateLimitError -> stringResource(
                    R.string.rate_limited_please_try_again_after_some_time,
                    when (it.source) {
                        Source.WALLHAVEN -> stringResource(R.string.wallhaven_cc)
                        Source.REDDIT -> stringResource(R.string.reddit)
                        Source.LOCAL -> "" // will never be local
                    },
                )
            }
            val actionText = when (it) {
                is WallHavenUnauthorisedError -> stringResource(R.string.fix)
                else -> null
            }
            val onActionClick = when (it) {
                is WallHavenUnauthorisedError -> onFixWallHavenApiKeyClick
                else -> ({})
            }
            SingleLineError(
                modifier = Modifier.animateItem(),
                errorMsg = errorMsg,
                actionText = actionText,
                onActionClick = onActionClick,
                onDismissClick = { onDismiss(it) },
            )
        }
    }
}
