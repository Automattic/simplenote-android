package com.automattic.simplenote.utils.locale

import java.util.Locale

interface LocaleProvider {
    fun provideLocale(): Locale?
}
