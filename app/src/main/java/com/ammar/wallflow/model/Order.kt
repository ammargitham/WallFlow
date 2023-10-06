package com.ammar.wallflow.model

enum class Order(
    val value: String,
) {
    DESC("desc"),
    ASC("asc"),
    ;

    companion object {
        fun fromValue(value: String) = if (value == "desc") DESC else ASC
    }
}
