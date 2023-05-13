package com.ammar.havenwalls.ui.common.globalerrors

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ammar.havenwalls.data.repository.GlobalErrorsRepository.GlobalError
import com.ammar.havenwalls.data.repository.GlobalErrorsRepository.WallHavenRateLimitError
import com.ammar.havenwalls.data.repository.GlobalErrorsRepository.WallHavenUnauthorisedError

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
                is WallHavenUnauthorisedError -> "Invalid API key provided."
                is WallHavenRateLimitError -> "Rate limited. Please try again after some time."
                else -> "Error"
            }
            val actionText = when (it) {
                is WallHavenUnauthorisedError -> "Fix"
                else -> null
            }
            val onActionClick = when (it) {
                is WallHavenUnauthorisedError -> onFixWallHavenApiKeyClick
                else -> ({})
            }
            SingleLineError(
                modifier = Modifier.animateItemPlacement(),
                errorMsg = errorMsg,
                actionText = actionText,
                onActionClick = onActionClick,
                onDismissClick = { onDismiss(it) }
            )
        }
    }
}
