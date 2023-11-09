package com.ammar.wallflow.extensions

fun <E> List<E>.randomList(size: Int): List<E> = this.asSequence()
    .shuffled()
    .take(size)
    .toList()

fun Map<String, String>.toQueryString(): String = this.entries.joinToString("&") { (k, v) ->
    "${k.urlEncoded()}=${v.urlEncoded()}"
}

fun <K, V> Map<K, V?>.filterNotNullValues(): Map<K, V> =
    mapNotNull { (key, value) -> value?.let { key to it } }.toMap()

fun <T> Iterable<T>.indexMap(): Map<T, Int> {
    val map = mutableMapOf<T, Int>()
    forEachIndexed { i, v ->
        map[v] = i
    }
    return map
}
