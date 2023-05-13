package com.ammar.havenwalls.data.repository.utils

import android.util.Log
import com.ammar.havenwalls.extensions.TAG
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

sealed class Resource<out R> {
    data class Loading<out T>(val data: T?) : Resource<T>()
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val throwable: Throwable) : Resource<Nothing>()
}

fun <T> Resource<T>.successOr(fallback: T): T {
    return (this as? Resource.Success<T>)?.data ?: fallback
}

inline fun <EntityType, ResultType, NetworkResponseType> networkBoundResource(
    crossinline dbQuery: () -> Flow<EntityType>,
    crossinline fetch: suspend (EntityType) -> NetworkResponseType,
    crossinline saveFetchResult: suspend (NetworkResponseType) -> Unit,
    crossinline onFetchFailed: (Throwable) -> Unit = {},
    crossinline shouldFetch: suspend (EntityType) -> Boolean = { true },
    crossinline entityConverter: suspend (EntityType) -> ResultType,
) = flow {
    emit(Resource.Loading(null))
    val data = dbQuery().first()
    val flow = if (shouldFetch(data)) {
        emit(Resource.Loading(entityConverter(data)))
        try {
            saveFetchResult(fetch(data))
            dbQuery().map { Resource.Success(entityConverter(it)) }
        } catch (throwable: Throwable) {
            Log.e(TAG, "networkBoundResource: ", throwable)
            onFetchFailed(throwable)
            dbQuery().map { Resource.Error(throwable) }
        }
    } else {
        dbQuery().map { Resource.Success(entityConverter(it)) }
    }
    emitAll(flow)
}

abstract class NetworkBoundResource<DbEntityType, ResultType, NetworkResourceType>(
    initialValue: ResultType,
    private val ioDispatcher: CoroutineDispatcher,
) {
    private val _data = MutableStateFlow<Resource<ResultType>>(Resource.Loading(initialValue))
    val data = _data.asStateFlow()

    abstract suspend fun loadFromDb(): DbEntityType
    abstract suspend fun shouldFetchData(dbData: DbEntityType): Boolean
    abstract suspend fun fetchFromNetwork(dbData: DbEntityType): NetworkResourceType
    abstract fun entityConverter(dbData: DbEntityType): ResultType
    abstract suspend fun saveFetchResult(fetchResult: NetworkResourceType)
    abstract fun onFetchFailed(throwable: Throwable)

    suspend fun init() = withContext(ioDispatcher) {
        val dbData = loadFromDb()
        if (shouldFetchData(dbData)) {
            loadFromNetwork(dbData)
            return@withContext
        }
        _data.emit(Resource.Success(entityConverter(dbData)))
    }

    suspend fun refresh(force: Boolean = false) = withContext(ioDispatcher) {
        val currentState = data.value
        if (currentState is Resource.Loading) {
            return@withContext
        }
        val dbData = loadFromDb()
        val shouldFetch = force || currentState is Resource.Error || shouldFetchData(dbData)
        if (!shouldFetch) {
            return@withContext
        }
        loadFromNetwork(dbData)
    }

    private suspend fun loadFromNetwork(dbData: DbEntityType) = withContext(ioDispatcher) {
        _data.emit(Resource.Loading(entityConverter(dbData)))
        try {
            saveFetchResult(fetchFromNetwork(dbData))
            _data.emit(Resource.Success(entityConverter(loadFromDb())))
        } catch (throwable: Throwable) {
            Log.e(TAG, "networkBoundResource: ", throwable)
            onFetchFailed(throwable)
            _data.emit(Resource.Error(throwable))
        }
    }


}
