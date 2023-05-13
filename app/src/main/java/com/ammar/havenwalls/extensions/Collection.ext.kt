package com.ammar.havenwalls.extensions

fun <E> List<E>.randomList(size: Int): List<E> = this.asSequence()
    .shuffled()
    .take(size)
    .toList()

fun Map<String, String>.toQueryString(): String = this.entries.joinToString("&") { (k, v) ->
    "${k.urlEncoded()}=${v.urlEncoded()}"
}

fun <E> Collection<E>?.isNotNullOrEmpty() = this?.isNotEmpty() ?: false
