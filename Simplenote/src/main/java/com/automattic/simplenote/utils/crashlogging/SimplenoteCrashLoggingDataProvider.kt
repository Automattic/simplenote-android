package com.automattic.simplenote.utils.crashlogging

import android.util.Log
import com.automattic.android.tracks.crashlogging.CrashLoggingDataProvider
import com.automattic.android.tracks.crashlogging.CrashLoggingUser
import com.automattic.android.tracks.crashlogging.EventLevel
import com.automattic.android.tracks.crashlogging.ExtraKnownKey
import com.automattic.android.tracks.crashlogging.PerformanceMonitoringConfig
import com.automattic.android.tracks.crashlogging.ReleaseName
import com.automattic.simplenote.BuildConfig
import com.automattic.simplenote.Simplenote
import com.automattic.simplenote.utils.locale.LocaleProvider
import com.simperium.client.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import java.util.Locale
import javax.inject.Inject

class SimplenoteCrashLoggingDataProvider @Inject constructor(
    private val app: Simplenote,
    private val localeProvider: LocaleProvider,
) : CrashLoggingDataProvider {

    override val buildType = BuildConfig.BUILD_TYPE
    override val enableCrashLoggingLogs = false
    override val sentryDSN: String = BuildConfig.SENTRY_DSN

    override val locale: Locale?
        get() = localeProvider.provideLocale()

    override val releaseName: ReleaseName = if (BuildConfig.DEBUG) {
        ReleaseName.SetByApplication(DEBUG_RELEASE_NAME)
    } else {
        ReleaseName.SetByTracksLibrary
    }

    override val user: Flow<CrashLoggingUser?>
        get() = provideUser()

    override val applicationContextProvider: Flow<Map<String, String>> = emptyFlow()

    override val performanceMonitoringConfig: PerformanceMonitoringConfig
        get() = PerformanceMonitoringConfig.Disabled

    override fun crashLoggingEnabled(): Boolean {
        if (BuildConfig.DEBUG) {
            return false
        }

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

    private fun provideUser(): Flow<CrashLoggingUser?> =
        flow {
            emit(app.simperium?.user?.toCrashLoggingUser())
        }.catch { e ->
            Log.e(TAG, "Exception getting the user", e)
            emit(null)
        }

    private fun User.toCrashLoggingUser(): CrashLoggingUser? {
        if (userId.isNullOrEmpty()) return null

        return CrashLoggingUser(
            userID = userId,
            email = email.orEmpty(),
            username = ""
        )
    }

    companion object {
        private const val DEBUG_RELEASE_NAME = "debug"
        private val TAG: String = SimplenoteCrashLoggingDataProvider::class.java.getSimpleName()
    }
}
