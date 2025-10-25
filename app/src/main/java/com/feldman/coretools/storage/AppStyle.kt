package com.feldman.coretools.storage

enum class AppStyle(val key: String) {
    Material("Material"),
    Glass("glass");

    companion object {
        fun fromKey(key: String?): AppStyle =
            entries.find { it.key == key } ?: Material
    }
}