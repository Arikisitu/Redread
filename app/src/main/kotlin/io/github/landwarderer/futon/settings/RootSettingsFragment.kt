package io.github.landwarderer.futon.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.fragment.app.activityViewModels
import androidx.preference.Preference
import dagger.hilt.android.AndroidEntryPoint
import io.github.landwarderer.futon.BuildConfig
import io.github.landwarderer.futon.R
import io.github.landwarderer.futon.core.prefs.AppSettings
import io.github.landwarderer.futon.core.ui.BasePreferenceFragment
import io.github.landwarderer.futon.core.util.ext.addMenuProvider
import io.github.landwarderer.futon.settings.search.SettingsSearchMenuProvider
import io.github.landwarderer.futon.settings.search.SettingsSearchViewModel
import io.github.landwarderer.futon.settings.sources.access.SourceAccessActivity

@AndroidEntryPoint
class RootSettingsFragment : BasePreferenceFragment(0) {

	private val activityViewModel: SettingsSearchViewModel by activityViewModels()

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_root)
		addPreferencesFromResource(R.xml.pref_root_debug)
		bindPreferenceSummary("appearance", R.string.theme, R.string.list_mode, R.string.language)
		bindPreferenceSummary("reader", R.string.read_mode, R.string.scale_mode, R.string.switch_pages)
		bindPreferenceSummary("network", R.string.storage_usage, R.string.proxy, R.string.prefetch_content)
		bindPreferenceSummary("userdata", R.string.create_or_restore_backup, R.string.periodic_backups)
		bindPreferenceSummary("downloads", R.string.manga_save_location, R.string.downloads_wifi_only)
		bindPreferenceSummary("tracker", R.string.track_sources, R.string.notifications_settings)
		bindPreferenceSummary("services", R.string.suggestions, R.string.sync, R.string.tracking)
		findPreference<Preference>("about")?.summary = getString(R.string.app_version, BuildConfig.VERSION_NAME)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		addMenuProvider(SettingsSearchMenuProvider(activityViewModel))
	}

	override fun onPreferenceTreeClick(preference: Preference): Boolean = when (preference.key) {
		AppSettings.KEY_SOURCE_ACCESS -> {
			startActivity(Intent(preference.context, SourceAccessActivity::class.java))
			true
		}

		else -> super.onPreferenceTreeClick(preference)
	}

	override fun setTitle(title: CharSequence?) {
		if (!resources.getBoolean(R.bool.is_tablet)) {
			super.setTitle(title)
		}
	}

	private fun bindPreferenceSummary(key: String, @StringRes vararg items: Int) {
		findPreference<Preference>(key)?.summary = items.joinToString { getString(it) }
	}
}
