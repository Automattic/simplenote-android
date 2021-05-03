package com.automattic.simplenote.utils

import java.security.SecureRandom

val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

fun getRandomStringOfLen(len: Int): String {
    val random = SecureRandom()
    val bytes = ByteArray(len)
    random.nextBytes(bytes)

    return (bytes.indices)
            .map { charPool[random.nextInt(charPool.size)] }
            .joinToString("")
}
