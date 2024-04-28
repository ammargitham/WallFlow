package com.ammar.wallflow.navigation

import com.ramcosta.composedestinations.annotation.NavGraph
import com.ramcosta.composedestinations.annotation.NavHostGraph

object AppNavGraphs {
    @NavHostGraph(
        defaultTransitions = MainNavHostAnimatedDestinationStyle::class,
    )
    annotation class RootNavGraph

    @NavHostGraph(
        defaultTransitions = MainNavHostAnimatedDestinationStyle::class,
    )
    annotation class MainNavGraph

    @NavGraph<MainNavGraph>(
        start = true,
    )
    annotation class HomeNavGraph

    @NavGraph<MainNavGraph>
    annotation class CollectionsNavGraph

    @NavGraph<MainNavGraph>
    annotation class LocalNavGraph

    @NavGraph<MainNavGraph>
    annotation class MoreNavGraph

    @NavGraph<RootNavGraph>
    annotation class SettingsNavGraph

    @NavGraph<RootNavGraph>
    annotation class BackupRestoreNavGraph

    @NavGraph<RootNavGraph>
    annotation class OpenSourceLicensesNavGraph
}
