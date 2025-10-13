package com.feldman.coretools.storage

enum class AppStyle(val key: String) {
    Playful("playful"),
    Glass("glass");

    companion object {
        fun fromKey(key: String?): AppStyle =
            entries.find { it.key == key } ?: Playful
    }
}