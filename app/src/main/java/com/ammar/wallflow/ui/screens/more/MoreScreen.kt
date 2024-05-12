package com.ammar.wallflow.ui.screens.more

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.ammar.wallflow.NavGraphs
import com.ammar.wallflow.navigation.AppNavGraphs.MoreNavGraph
import com.ammar.wallflow.ui.common.LocalSystemController
import com.ammar.wallflow.ui.common.MainDestinationBox
import com.ammar.wallflow.ui.common.bottombar.LocalBottomBarController
import com.ammar.wallflow.ui.screens.main.RootNavControllerWrapper
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.spec.NavGraphSpec

@Destination<MoreNavGraph>(
    start = true,
)
@Composable
fun MoreScreen(
    navController: NavController,
    rootNavControllerWrapper: RootNavControllerWrapper,
) {
    val rootNavController = rootNavControllerWrapper.navController
    val bottomBarController = LocalBottomBarController.current
    val systemController = LocalSystemController.current
    val systemState by systemController.state

    val moreNavigate: (ActiveOption) -> Unit = remember(navController) {
        {
            val route = getRoute(it)
            rootNavController.navigate(route.route)
        }
    }

    LaunchedEffect(Unit) {
        bottomBarController.update { it.copy(visible = true) }
    }

    MainDestinationBox(
        isExpanded = systemState.isExpanded,
    ) {
        MoreScreenContent(
            modifier = Modifier.fillMaxSize(),
            isExpanded = systemState.isExpanded,
            isMedium = systemState.isMedium,
            onSettingsClick = { moreNavigate(ActiveOption.SETTINGS) },
            onBackupRestoreClick = { moreNavigate(ActiveOption.BACKUP_RESTORE) },
            onOpenSourceLicensesClick = { moreNavigate(ActiveOption.OSL) },
        )
    }
}

private fun getRoute(
    option: ActiveOption,
): NavGraphSpec = when (option) {
    ActiveOption.SETTINGS -> NavGraphs.settings
    ActiveOption.BACKUP_RESTORE -> NavGraphs.backupRestore
    ActiveOption.OSL -> NavGraphs.openSourceLicenses
}
