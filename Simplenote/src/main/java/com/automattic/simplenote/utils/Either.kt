package com.automattic.simplenote.utils

sealed class Either<out L, out R> {

    data class Left<out L>(val l: L) : Either<L, Nothing>()

    data class Right<out R>(val r: R) : Either<Nothing, R>()

    fun <T> fold(fnL: (L) -> T, fnR: (R) -> T): T {
        return when (this) {
            is Left -> fnL(l)
            is Right -> fnR(r)
        }
    }

    fun <R2> map(fn: (R) -> R2): Either<L, R2> {
        return when (this) {
            is Left -> this
            is Right -> Right(fn(r))
        }
    }
}
