package com.ammar.wallflow.extensions

import com.lazygeniouz.dfc.file.DocumentFileCompat

fun DocumentFileCompat.deepListFiles(): List<DocumentFileCompat> =
    listFiles().fold(mutableListOf()) { acc, file ->
        when {
            file.isDirectory() -> acc.addAll(file.deepListFiles())
            else -> acc.add(file)
        }
        acc
    }
