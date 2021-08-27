package com.automattic.simplenote.utils

import java.security.SecureRandom


fun getLocalRandomStringOfLen(len: Int): String {
    val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    val random = SecureRandom()
    val bytes = ByteArray(len)
    random.nextBytes(bytes)

    return (bytes.indices)
            .map { charPool[random.nextInt(charPool.size)] }
            .joinToString("")
}
