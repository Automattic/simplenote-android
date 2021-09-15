package com.automattic.simplenote.utils.crashlogging

import com.automattic.android.tracks.crashlogging.CrashLoggingDataProvider
import com.automattic.android.tracks.crashlogging.CrashLoggingUser
import com.automattic.android.tracks.crashlogging.EventLevel
import com.automattic.android.tracks.crashlogging.ExtraKnownKey
import com.automattic.simplenote.BuildConfig
import com.automattic.simplenote.Simplenote
import com.automattic.simplenote.utils.locale.LocaleProvider
import java.util.Locale
import javax.inject.Inject

class SimplenoteCrashLoggingDataProvider @Inject constructor(
    private val app: Simplenote,
    private val localeProvider: LocaleProvider,
) : CrashLoggingDataProvider {

    override val buildType = BuildConfig.BUILD_TYPE
    override val enableCrashLoggingLogs = BuildConfig.DEBUG
    override val sentryDSN: String = BuildConfig.SENTRY_DSN

    override val locale: Locale?
        get() = localeProvider.provideLocale()

    override val releaseName = if (BuildConfig.DEBUG) {
        DEBUG_RELEASE_NAME
    } else {
        BuildConfig.VERSION_NAME
    }

    override fun applicationContextProvider(): Map<String, String> {
        return emptyMap()
    }

    override fun crashLoggingEnabled(): Boolean {
        return Simplenote.analyticsIsEnabled()
    }

    override fun extraKnownKeys(): List<ExtraKnownKey> {
        return emptyList()
    }

    override fun provideExtrasForEvent(
        currentExtras: Map<ExtraKnownKey, String>,
        eventLevel: EventLevel
    ): Map<ExtraKnownKey, String> {
        return emptyMap()
    }

    override fun shouldDropWrappingException(module: String, type: String, value: String): Boolean {
        return false
    }

    override fun userProvider(): CrashLoggingUser {
        return CrashLoggingUser(
            userID = app.simperium.user.userId.orEmpty(),
            email = app.simperium.user.email.orEmpty(),
            username = ""
        )
    }

    companion object {
        const val DEBUG_RELEASE_NAME = "debug"
    }
}
