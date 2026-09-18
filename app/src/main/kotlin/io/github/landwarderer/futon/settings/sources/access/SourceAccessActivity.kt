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
import io.github.landwarderer.futon.settings.sources.catalog.SourcesCatalogActivity

private const val MIN_PASSWORD_LENGTH = 1
const val EXTRA_RETURN_TO_CATALOG = "extra_return_to_catalog"
const val EXTRA_RETURN_TO_SETTINGS_ACTION = "extra_return_to_settings_action"
const val EXTRA_RETURN_TO_SETTINGS_SOURCE = "extra_return_to_settings_source"

@AndroidEntryPoint
class SourceAccessActivity :
    BaseActivity<ActivitySetupProtectBinding>(),
    DefaultTextWatcher,
    View.OnClickListener,
    TextView.OnEditorActionListener {

    private val viewModel by viewModels<SourceAccessViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(
            ActivitySetupProtectBinding.inflate(layoutInflater),
        )

        viewBinding.switchBiometric.isGone = true

        viewBinding.textViewTitle.setText(
            R.string.source_access_unlock_title,
        )

        viewBinding.textViewSubtitle.setText(
            R.string.source_access_unlock_subtitle,
        )

        viewBinding.editPassword.addTextChangedListener(this)

        viewBinding.editPassword.setOnEditorActionListener(this)

        viewBinding.buttonNext.setOnClickListener(this)

        viewBinding.buttonCancel.setOnClickListener(this)

        viewBinding.layoutPassword.helperText = null

        viewModel.onGranted.observeEvent(this) {
            when {
                intent?.getBooleanExtra(EXTRA_RETURN_TO_CATALOG, false) == true -> {
                    startActivity(Intent(this, SourcesCatalogActivity::class.java))
                }

                !intent?.getStringExtra(EXTRA_RETURN_TO_SETTINGS_ACTION).isNullOrEmpty() -> {
                    startActivity(
                        Intent(this, SettingsActivity::class.java).apply {
                            action = intent?.getStringExtra(EXTRA_RETURN_TO_SETTINGS_ACTION)
                            intent?.getStringExtra(EXTRA_RETURN_TO_SETTINGS_SOURCE)?.let {
                                putExtra(AppRouter.KEY_SOURCE, it)
                            }
                        },
                    )
                }
            }

            finish()
        }

        viewModel.onPasswordMismatch.observeEvent(this) {
            viewBinding.editPassword.error =
                getString(R.string.source_access_password_incorrect)

            viewBinding.editPassword.requestFocus()
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.button_cancel -> {
                finish()
            }

            R.id.button_next -> {
                viewModel.submit(
                    viewBinding.editPassword.text
                        ?.toString()
                        .orEmpty(),
                )
            }
        }
    }

    override fun onEditorAction(
        view: TextView?,
        actionId: Int,
        event: KeyEvent?,
    ): Boolean {
        return if (
            actionId == EditorInfo.IME_ACTION_DONE &&
            viewBinding.buttonNext.isEnabled
        ) {
            viewBinding.buttonNext.performClick()
            true
        } else {
            false
        }
    }

    override fun afterTextChanged(s: Editable?) {
        viewBinding.editPassword.error = null

        viewBinding.buttonNext.isEnabled =
            (s?.length ?: 0) >= MIN_PASSWORD_LENGTH
    }

    override fun onApplyWindowInsets(
        v: View,
        insets: WindowInsetsCompat,
    ): WindowInsetsCompat {
        return insets
    }
}
