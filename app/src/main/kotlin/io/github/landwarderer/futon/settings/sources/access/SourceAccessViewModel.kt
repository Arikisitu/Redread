package io.github.landwarderer.futon.settings.sources.access

import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.landwarderer.futon.core.ui.BaseViewModel
import io.github.landwarderer.futon.core.util.ext.MutableEventFlow
import io.github.landwarderer.futon.core.util.ext.call
import javax.inject.Inject

@HiltViewModel
class SourceAccessViewModel @Inject constructor(
    private val sourceAccessManager: SourceAccessManager,
) : BaseViewModel() {

    val onGranted = MutableEventFlow<Unit>()
    val onPasswordMismatch = MutableEventFlow<Unit>()

    /**
     * Source Access uses a fixed developer-defined password.
     * There is no user password setup or confirmation.
     */
    val needsPasswordSetup: Boolean
        get() = false

    /**
     * Whether Source Access is already unlocked.
     */
    val isUnlocked: Boolean
        get() = sourceAccessManager.isUnlocked

    /**
     * Check the entered password.
     *
     * Correct password:
     * - unlocks Source Access
     * - persists the unlocked state
     *
     * Wrong password:
     * - keeps Source Access locked
     */
    fun submit(password: String) {
        if (sourceAccessManager.unlock(password.toCharArray())) {
            onGranted.call(Unit)
        } else {
            onPasswordMismatch.call(Unit)
        }
    }
}
