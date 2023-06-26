package com.ammar.havenwalls.extensions

import androidx.work.Constraints
import androidx.work.NetworkType
import com.ammar.havenwalls.model.ConstraintType

fun Constraints.toConstraintTypeMap() = mapOf(
    ConstraintType.WIFI to (this.requiredNetworkType == NetworkType.UNMETERED),
    ConstraintType.ROAMING to (this.requiredNetworkType == NetworkType.CONNECTED),
    ConstraintType.CHARGING to this.requiresCharging(),
    ConstraintType.IDLE to this.requiresDeviceIdle(),
)

private val wifiRoamingCombinations = mapOf(
    // if on wifi is true, network type should be un-metered (regardless of roaming boolean)
    listOf(true, true) to NetworkType.UNMETERED,
    listOf(true, false) to NetworkType.UNMETERED,
    // wifi false, roaming true means device just needs to be connected
    listOf(false, true) to NetworkType.CONNECTED,
    // wifi false, roaming false means device needs to be connected to a non-roaming connection
    listOf(false, false) to NetworkType.NOT_ROAMING,
)

fun Map<ConstraintType, Boolean>.toConstraints() = Constraints.Builder().apply {
    val constraintMap = this@toConstraints
    val networkType = wifiRoamingCombinations[listOf(
        constraintMap[ConstraintType.WIFI] ?: false,
        constraintMap[ConstraintType.ROAMING] ?: false,
    )] ?: NetworkType.CONNECTED
    setRequiredNetworkType(networkType)
    setRequiresCharging(constraintMap[ConstraintType.CHARGING] ?: false)
    setRequiresDeviceIdle(constraintMap[ConstraintType.IDLE] ?: false)
}.build()
