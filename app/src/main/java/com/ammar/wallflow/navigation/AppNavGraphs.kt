package com.ammar.wallflow.navigation

import com.ramcosta.composedestinations.annotation.NavGraph
import com.ramcosta.composedestinations.annotation.NavHostGraph

object AppNavGraphs {
    @NavHostGraph(
        defaultTransitions = MainNavHostAnimatedDestinationStyle::class,
    )
    annotation class MainGraph

    @NavGraph<MainGraph>(
        start = true,
    )
    annotation class HomeNavGraph

    @NavGraph<MainGraph>
    annotation class CollectionsNavGraph

    @NavGraph<MainGraph>
    annotation class LocalNavGraph

    @NavGraph<MainGraph>
    annotation class MoreNavGraph

    @NavGraph<MoreNavGraph>
    annotation class SettingsNavGraph

    @NavGraph<MoreNavGraph>
    annotation class BackupRestoreNavGraph

    @NavGraph<MoreNavGraph>
    annotation class OpenSourceLicensesNavGraph

    @NavHostGraph(
        defaultTransitions = MoreDetailNavHostAnimatedDestinationStyle::class,
    )
    annotation class MoreDetailNavGraph

    @NavGraph<MoreDetailNavGraph>(
        start = true,
    )
    annotation class SettingsForMoreDetailNavGraph

    @NavGraph<MoreDetailNavGraph>
    annotation class BackupRestoreForMoreDetailNavGraph

    @NavGraph<MoreDetailNavGraph>
    annotation class OpenSourceLicensesForMoreDetailNavGraph
}
