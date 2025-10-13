package com.feldman.coretools.storage

import android.graphics.drawable.Drawable

data class UsageItem(
    val packageName: String,
    val appName: String,
    var timeUsedMs: Long,
    val appIcon: Drawable,
    val themedIcon: Drawable? = null
)