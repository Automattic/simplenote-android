package com.automattic.simplenote.utils

import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

fun AppCompatActivity.getColorStr(@ColorRes color: Int): String {
    val hintColor = ContextCompat.getColor(this, color)
    val hintHexColor = hintColor.toHexString().replace("ff", "")
    return "#$hintHexColor"
}

fun Int.toHexString(): String {
    return Integer.toHexString(this)
}