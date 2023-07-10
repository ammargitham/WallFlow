package com.ammar.wallflow.extensions

fun <E> List<E>.randomList(size: Int): List<E> = this.asSequence()
    .shuffled()
    .take(size)
    .toList()

fun Map<String, String>.toQueryString(): String = this.entries.joinToString("&") { (k, v) ->
    "${k.urlEncoded()}=${v.urlEncoded()}"
}
