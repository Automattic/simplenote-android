package com.automattic.simplenote.utils

import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

fun AppCompatActivity.toast(@StringRes resId: Int, length: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, getString(resId), length).show()
}

fun AppCompatActivity.getColorStr(@ColorRes color: Int): String {
    val hintColor = ContextCompat.getColor(this, color)
    val hintHexColor = hintColor.toHexString().replace("ff", "")
    return "#$hintHexColor"
}

fun Int.toHexString(): String {
    return Integer.toHexString(this)
}
