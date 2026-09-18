package io.github.landwarderer.futon.settings.sources.access

import io.github.landwarderer.futon.core.prefs.AppSettings
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SourceAccessManager @Inject constructor(
    private val settings: AppSettings,
) {

    /**
     * Whether Source Access is currently unlocked.
     *
     * This value is stored in AppSettings, so it survives:
     * - closing the app
     * - reopening the app
     * - phone restart
     */
    val isUnlocked: Boolean
        get() = settings.isSourceAccessUnlocked

    /**
     * Source Access uses a fixed developer-defined password.
     *
     * NOTE:
     * A password embedded in an APK is not a truly secret password.
     * This is intended as an access gate, not strong security.
     */
    private companion object {
        const val SOURCE_ACCESS_PASSWORD = "Arik"
    }

    /**
     * Source Access does not use user-created password setup.
     */
    val needsPasswordSetup: Boolean
        get() = false

    /**
     * Checks the entered password.
     *
     * If correct, Source Access is unlocked and the state
     * is persisted in AppSettings.
     */
    fun unlock(password: CharArray): Boolean {
        val enteredPassword = password.concatToString()
        password.fill('\u0000')

        val result = enteredPassword == SOURCE_ACCESS_PASSWORD

        if (result) {
            settings.isSourceAccessUnlocked = true
        }

        return result
    }

    /**
     * Manually locks Source Access again.
     *
     * After this is called, the password will be required
     * the next time Source Access is opened.
     */
    fun lockSources() {
        settings.isSourceAccessUnlocked = false
    }
}
