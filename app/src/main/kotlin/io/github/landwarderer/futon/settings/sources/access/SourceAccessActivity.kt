package io.github.landwarderer.futon.settings.sources.access

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone
import dagger.hilt.android.AndroidEntryPoint
import io.github.landwarderer.futon.R
import io.github.landwarderer.futon.core.nav.AppRouter
import io.github.landwarderer.futon.core.ui.BaseActivity
import io.github.landwarderer.futon.core.ui.util.DefaultTextWatcher
import io.github.landwarderer.futon.core.util.ext.observeEvent
import io.github.landwarderer.futon.databinding.ActivitySetupProtectBinding
import io.github.landwarderer.futon.settings.SettingsActivity

private const val MIN_PASSWORD_LENGTH = 8

@AndroidEntryPoint
class SourceAccessActivity :
	BaseActivity<ActivitySetupProtectBinding>(),
	DefaultTextWatcher,
	View.OnClickListener,
	TextView.OnEditorActionListener {

	private val viewModel by viewModels<SourceAccessViewModel>()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivitySetupProtectBinding.inflate(layoutInflater))
		viewBinding.switchBiometric.isGone = true
		viewBinding.textViewTitle.setText(
			if (viewModel.needsPasswordSetup) R.string.source_access_set_title else R.string.source_access_unlock_title,
		)
		viewBinding.textViewSubtitle.setText(
			if (viewModel.needsPasswordSetup) R.string.source_access_set_subtitle else R.string.source_access_unlock_subtitle,
		)
		viewBinding.editPassword.addTextChangedListener(this)
		viewBinding.editPassword.setOnEditorActionListener(this)
		viewBinding.buttonNext.setOnClickListener(this)
		viewBinding.buttonCancel.setOnClickListener(this)
		viewBinding.layoutPassword.helperText = getString(R.string.source_access_password_minimum)

		viewModel.onGranted.observeEvent(this) {
			startActivity(Intent(this, SettingsActivity::class.java).setAction(AppRouter.ACTION_SOURCES))
			finish()
		}
		viewModel.onPasswordMismatch.observeEvent(this) {
			viewBinding.editPassword.error = getString(
				if (viewModel.needsPasswordSetup) R.string.passwords_mismatch else R.string.source_access_password_incorrect,
			)
		}
		viewModel.onClearText.observeEvent(this) { viewBinding.editPassword.text?.clear() }
	}

	override fun onClick(view: View) {
		when (view.id) {
			R.id.button_cancel -> finish()
			R.id.button_next -> viewModel.submit(viewBinding.editPassword.text?.toString().orEmpty())
		}
		if (viewModel.isConfirming) {
			viewBinding.buttonNext.setText(R.string.confirm)
			viewBinding.layoutPassword.helperText = getString(R.string.repeat_password)
		}
	}

	override fun onEditorAction(view: TextView?, actionId: Int, event: KeyEvent?): Boolean =
		if ((actionId == EditorInfo.IME_ACTION_DONE) && viewBinding.buttonNext.isEnabled) {
			viewBinding.buttonNext.performClick()
			true
		} else {
			false
		}

	override fun afterTextChanged(s: Editable?) {
		viewBinding.editPassword.error = null
		viewBinding.buttonNext.isEnabled = (s?.length ?: 0) >= MIN_PASSWORD_LENGTH
	}

	override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
		return insets
	}
}
