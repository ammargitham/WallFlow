package com.ammar.wallflow.utils

import androidx.room.RoomDatabase
import androidx.room.Transaction

@Transaction
suspend fun <P, R> safeGetAll(
    params: Collection<P>,
    actualGetAll: suspend (Collection<P>) -> List<R>,
): List<R> {
    if (params.isEmpty()) {
        return emptyList()
    }
    if (params.count() <= RoomDatabase.MAX_BIND_PARAMETER_CNT) {
        return actualGetAll(params)
    }
    val all = mutableListOf<R>()
    params.asSequence().chunked(RoomDatabase.MAX_BIND_PARAMETER_CNT).forEach {
        all += actualGetAll(it)
    }
    return all
}
