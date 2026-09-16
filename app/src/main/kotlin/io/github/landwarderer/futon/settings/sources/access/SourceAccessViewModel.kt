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

	private var firstPassword: String? = null

	val onGranted = MutableEventFlow<Unit>()
	val onPasswordMismatch = MutableEventFlow<Unit>()
	val onClearText = MutableEventFlow<Unit>()

	val needsPasswordSetup: Boolean
		get() = sourceAccessManager.needsPasswordSetup

	val isConfirming: Boolean
		get() = firstPassword != null

	fun submit(password: String) {
		if (needsPasswordSetup) {
			if (firstPassword == null) {
				firstPassword = password
				onClearText.call(Unit)
			} else if (firstPassword == password) {
				sourceAccessManager.setPassword(password.toCharArray())
				firstPassword = null
				onGranted.call(Unit)
			} else {
				firstPassword = null
				onPasswordMismatch.call(Unit)
				onClearText.call(Unit)
			}
		} else if (sourceAccessManager.unlock(password.toCharArray())) {
			onGranted.call(Unit)
		} else {
			onPasswordMismatch.call(Unit)
		}
	}
}
