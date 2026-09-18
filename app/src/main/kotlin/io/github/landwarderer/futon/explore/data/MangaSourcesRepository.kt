package io.github.landwarderer.futon.explore.data

import android.content.Context
import android.content.Intent
import androidx.room.withTransaction
import io.github.landwarderer.futon.BuildConfig
import io.github.landwarderer.futon.core.LocalizedAppContext
import io.github.landwarderer.futon.core.db.MangaDatabase
import io.github.landwarderer.futon.core.db.dao.MangaSourcesDao
import io.github.landwarderer.futon.core.db.entity.MangaSourceEntity
import io.github.landwarderer.futon.core.model.AnonymousMangaSource
import io.github.landwarderer.futon.core.model.MangaSource
import io.github.landwarderer.futon.core.model.MangaSourceInfo
import io.github.landwarderer.futon.core.model.getTitle
import io.github.landwarderer.futon.core.model.updateMihonTitle
import io.github.landwarderer.futon.core.model.isBroken
import io.github.landwarderer.futon.core.model.isNsfw
import io.github.landwarderer.futon.core.parser.external.ExternalMangaSource
import io.github.landwarderer.futon.core.prefs.AppSettings
import io.github.landwarderer.futon.core.prefs.observeAsFlow
import io.github.landwarderer.futon.core.ui.util.ReversibleHandle
import io.github.landwarderer.futon.core.util.ext.flattenLatest
import io.github.landwarderer.futon.mihon.MihonExtensionManager
import io.github.landwarderer.futon.mihon.model.MihonMangaSource
import io.github.landwarderer.futon.mihon.parsers.model.ContentSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import org.koitharu.kotatsu.parsers.model.ContentType
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.model.MangaSource as ParserMangaSource
import org.koitharu.kotatsu.parsers.network.CloudFlareHelper
import org.koitharu.kotatsu.parsers.util.mapNotNullToSet
import org.koitharu.kotatsu.parsers.util.mapToSet
import java.util.Collections
import java.util.EnumSet
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangaSourcesRepository @Inject constructor(
    @LocalizedAppContext private val context: Context,
    private val db: MangaDatabase,
    private val settings: AppSettings,
    private val mihonExtensionManager: MihonExtensionManager,
) {

	private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
	private val isNewSourcesAssimilated = AtomicBoolean(false)
	private val dao: MangaSourcesDao
		get() = db.getSourcesDao()

	init {
		mihonExtensionManager.installedExtensions
			.onEach {
				isNewSourcesAssimilated.set(false)
				assimilateNewSources()
			}
			.launchIn(repositoryScope)
	}

	val allMangaSources: Set<MangaParserSource> = Collections.unmodifiableSet(
		EnumSet.noneOf<MangaParserSource>(MangaParserSource::class.java).also {
            MangaParserSource.entries.filterNotTo(it, MangaParserSource::isBroken)
        }
	)

	suspend fun getEnabledSources(): List<ParserMangaSource> {
		assimilateNewSources()
		val order = settings.sourcesSortOrder
		return dao.findAll(!settings.isAllSourcesEnabled, order).toSources(settings.isNsfwContentDisabled, order)
			.map { it.mangaSource }
	}

	suspend fun getPinnedSources(): Set<ParserMangaSource> {
		assimilateNewSources()
		val skipNsfw = settings.isNsfwContentDisabled
		return dao.findAllPinned().mapNotNullToSet {
			it.toMangaSource()?.takeUnless { x -> skipNsfw && x.isNsfw() }
		}
	}

	suspend fun getTopSources(limit: Int): List<ParserMangaSource> {
		assimilateNewSources()
		return dao.findLastUsed(limit).toSources(settings.isNsfwContentDisabled, null).map { it.mangaSource }
	}

	suspend fun getDisabledSources(): Set<ParserMangaSource> {
		assimilateNewSources()
		if (settings.isAllSourcesEnabled) {
			return emptySet()
		}
		val result = EnumSet.copyOf(allMangaSources)
		val enabled = dao.findAllEnabledNames()
		for (name in enabled) {
			val source = name.toMangaSourceOrNull() ?: continue
			if (source is MangaParserSource) {
				result.remove(source)
			}
		}
		return result
	}

	suspend fun queryParserSources(
		isDisabledOnly: Boolean,
		isNewOnly: Boolean,
		excludeBroken: Boolean,
		types: Set<ContentType>,
		query: String?,
		locale: String?,
		sortOrder: SourcesSortOrder?,
	): List<ParserMangaSource> {
		assimilateNewSources()
		val entities = dao.findAll().toMutableList()
		if (isDisabledOnly && !settings.isAllSourcesEnabled) {
			entities.removeAll { it.isEnabled }
		}
		if (isNewOnly) {
			entities.retainAll { it.addedIn == BuildConfig.VERSION_CODE }
		}
		val sources = entities.toSources(
			skipNsfwSources = settings.isNsfwContentDisabled,
			sortOrder = sortOrder,
		).run {
			mapTo(ArrayList(size)) { it.mangaSource }
		}

		if (locale != null) {
			sources.retainAll { 
				when (it) {
					is MangaParserSource -> it.locale == locale
					is ContentSource -> it.locale == locale
					else -> true
				}
			}
		}
		if (excludeBroken) {
			sources.removeAll { (it as? MangaParserSource)?.isBroken == true }
		}
		if (types.isNotEmpty()) {
			sources.retainAll { 
				when (it) {
					is MangaParserSource -> it.contentType in types
					is MihonMangaSource -> {
						val mihonType = it.contentType
						types.any { kotatsuType ->
							when (kotatsuType) {
								ContentType.MANGA -> mihonType == io.github.landwarderer.futon.mihon.parsers.model.ContentType.MANGA
								ContentType.HENTAI -> mihonType == io.github.landwarderer.futon.mihon.parsers.model.ContentType.HENTAI_MANGA
								ContentType.COMICS -> mihonType == io.github.landwarderer.futon.mihon.parsers.model.ContentType.COMICS
								ContentType.MANHWA -> mihonType == io.github.landwarderer.futon.mihon.parsers.model.ContentType.MANHWA
								ContentType.MANHUA -> mihonType == io.github.landwarderer.futon.mihon.parsers.model.ContentType.MANHUA
								ContentType.NOVEL -> mihonType == io.github.landwarderer.futon.mihon.parsers.model.ContentType.NOVEL
								ContentType.ONE_SHOT -> mihonType == io.github.landwarderer.futon.mihon.parsers.model.ContentType.ONE_SHOT
								ContentType.DOUJINSHI -> mihonType == io.github.landwarderer.futon.mihon.parsers.model.ContentType.DOUJINSHI
								ContentType.IMAGE_SET -> mihonType == io.github.landwarderer.futon.mihon.parsers.model.ContentType.IMAGE_SET
								ContentType.ARTIST_CG -> mihonType == io.github.landwarderer.futon.mihon.parsers.model.ContentType.ARTIST_CG
								ContentType.GAME_CG -> mihonType == io.github.landwarderer.futon.mihon.parsers.model.ContentType.GAME_CG
								else -> false
							}
						}
					}
					else -> true
				}
			}
		}
		if (!query.isNullOrEmpty()) {
			sources.retainAll {
				it.getTitle(context).contains(query, ignoreCase = true) || it.name.contains(query, ignoreCase = true)
			}
		}
		return sources
	}

	fun observeIsEnabled(source: ParserMangaSource): Flow<Boolean> {
		return dao.observeIsEnabled(source.name).onStart { assimilateNewSources() }
	}

	fun observeEnabledSourcesCount(): Flow<Int> {
		return combine(
			observeIsNsfwDisabled(),
			observeAllEnabled().flatMapLatest { isAllSourcesEnabled ->
				dao.observeAll(!isAllSourcesEnabled, SourcesSortOrder.MANUAL)
			},
		) { skipNsfw, sources ->
			sources.count {
				it.toMangaSource()?.let { s -> !skipNsfw || !s.isNsfw() } == true
			}
		}.distinctUntilChanged().onStart { assimilateNewSources() }
	}

	fun observeAvailableSourcesCount(): Flow<Int> {
		return combine(
			observeIsNsfwDisabled(),
			observeAllEnabled().flatMapLatest { isAllSourcesEnabled ->
				dao.observeAll(!isAllSourcesEnabled, SourcesSortOrder.MANUAL)
			},
		) { skipNsfw, enabledSources ->
			val enabled = enabledSources.mapToSet { it.source }
			allMangaSources.count { x ->
				x.name !in enabled && (!skipNsfw || !x.isNsfw())
			}
		}.distinctUntilChanged().onStart { assimilateNewSources() }
	}

	fun observeEnabledSources(): Flow<List<MangaSourceInfo>> = combine(
		observeIsNsfwDisabled(),
		observeAllEnabled(),
		observeSortOrder(),
	) { skipNsfw, allEnabled, order ->
		dao.observeAll(!allEnabled, order).map {
			it.toSources(skipNsfw, order)
		}
	}.flattenLatest()
		.onStart { assimilateNewSources() }

	fun observeAll(): Flow<List<Pair<ParserMangaSource, Boolean>>> = dao.observeAll().map { entities ->
		val result = ArrayList<Pair<ParserMangaSource, Boolean>>(entities.size)
		for (entity in entities) {
			val source = entity.toMangaSource() ?: continue
			if (source in allMangaSources || source is AnonymousMangaSource || source is MihonMangaSource || source is ExternalMangaSource) {
				result.add(source to entity.isEnabled)
			}
		}
		result
	}.onStart { assimilateNewSources() }

	suspend fun setSourcesEnabled(sources: Collection<ParserMangaSource>, isEnabled: Boolean): ReversibleHandle {
		setSourcesEnabledImpl(sources, isEnabled)
		return ReversibleHandle {
			setSourcesEnabledImpl(sources, !isEnabled)
		}
	}

	suspend fun setSourcesEnabledExclusive(sources: Set<ParserMangaSource>) {
		db.withTransaction {
			assimilateNewSources()
			for (s in allMangaSources) {
				dao.setEnabled(s.name, s in sources)
			}
		}
	}

	suspend fun disableAllSources() {
		db.withTransaction {
			assimilateNewSources()
			dao.disableAllSources()
		}
	}

	suspend fun setPositions(sources: List<ParserMangaSource>) {
		db.withTransaction {
			for ((index, item) in sources.withIndex()) {
				dao.setSortKey(item.name, index)
			}
		}
	}

	fun observeHasNewSources(): Flow<Boolean> = observeIsNsfwDisabled().map { skipNsfw ->
		val sources = dao.findAllFromVersion(BuildConfig.VERSION_CODE).toSources(skipNsfw, null)
		sources.isNotEmpty() && sources.size != allMangaSources.size
	}.onStart { assimilateNewSources() }

	fun observeHasNewSourcesForBadge(): Flow<Boolean> = combine(
		settings.observeAsFlow(AppSettings.KEY_SOURCES_VERSION) { sourcesVersion },
		observeIsNsfwDisabled(),
	) { version, skipNsfw ->
		if (version < BuildConfig.VERSION_CODE) {
			val sources = dao.findAllFromVersion(version).toSources(skipNsfw, null)
			sources.isNotEmpty()
		} else {
			false
		}
	}.onStart { assimilateNewSources() }

	fun clearNewSourcesBadge() {
		settings.sourcesVersion = BuildConfig.VERSION_CODE
	}

	private suspend fun assimilateNewSources(): Boolean {
		if (isNewSourcesAssimilated.getAndSet(true)) {
			updateMihonTitles()
			return false
		}
		
		// Initial cache population from database
		dao.findAll().forEach { entity ->
			if ((entity.source.startsWith("mihon:") || entity.source.startsWith("MIHON_")) && entity.title != null) {
				updateMihonTitle(entity.source, entity.title)
			}
		}

		val new = getNewSources()
		var maxSortKey = dao.getMaxSortKey()
		val isAllEnabled = settings.isAllSourcesEnabled
		val entities = new.map { x ->
			MangaSourceEntity(
				source = x.name,
				isEnabled = isAllEnabled,
				sortKey = ++maxSortKey,
				addedIn = BuildConfig.VERSION_CODE,
				lastUsedAt = 0,
				isPinned = false,
				cfState = CloudFlareHelper.PROTECTION_NOT_DETECTED,
				title = x.getTitle(context),
			)
		}
		dao.insertIfAbsent(entities)
		updateMihonTitles()
		return new.isNotEmpty()
	}

	private suspend fun updateMihonTitles() {
		val mihonSources = mihonExtensionManager.getMihonMangaSources()
		for (source in mihonSources) {
			dao.setTitle(source.name, source.displayName)
		}
	}

	suspend fun isSetupRequired(): Boolean {
		return settings.sourcesVersion == 0 && dao.findAllEnabledNames().isEmpty()
	}

	suspend fun setIsPinned(sources: Collection<ParserMangaSource>, isPinned: Boolean): ReversibleHandle {
		setSourcesPinnedImpl(sources, isPinned)
		return ReversibleHandle {
			setSourcesPinnedImpl(sources, !isPinned)
		}
	}

	suspend fun trackUsage(source: ParserMangaSource) {
		if (!settings.isIncognitoModeEnabled(source.isNsfw())) {
			dao.setLastUsed(source.name, System.currentTimeMillis())
		}
	}

	private suspend fun setSourcesEnabledImpl(sources: Collection<ParserMangaSource>, isEnabled: Boolean) {
		if (sources.size == 1) { // fast path
			dao.setEnabled(sources.first().name, isEnabled)
			return
		}
		db.withTransaction {
			for (source in sources) {
				dao.setEnabled(source.name, isEnabled)
			}
		}
	}

	private suspend fun getNewSources(): MutableSet<out ParserMangaSource> {
		val entities = dao.findAll()
		val result = HashSet<ParserMangaSource>()
        result.addAll(MangaParserSource.entries)
        result.addAll(mihonExtensionManager.getMihonMangaSources())
		result.addAll(getExternalSources())
		for (e in entities) {
			result.remove(e.toMangaSource() ?: continue)
		}
		return result
	}

	private suspend fun setSourcesPinnedImpl(sources: Collection<ParserMangaSource>, isPinned: Boolean) {
		if (sources.size == 1) { // fast path
			dao.setPinned(sources.first().name, isPinned)
			return
		}
		db.withTransaction {
			for (source in sources) {
				dao.setPinned(source.name, isPinned)
			}
		}
	}

	fun getExternalSources(): List<ParserMangaSource> {
		return context.packageManager.queryIntentContentProviders(
			Intent("app.futon.parser.PROVIDE_MANGA"), 0,
		).map { resolveInfo ->
			ExternalMangaSource(
				packageName = resolveInfo.providerInfo.packageName,
				authority = resolveInfo.providerInfo.authority,
			)
		}
	}

	fun getMihonSources(): List<ParserMangaSource> {
		return mihonExtensionManager.getMihonMangaSources()
	}

	private fun List<MangaSourceEntity>.toSources(
		skipNsfwSources: Boolean,
		sortOrder: SourcesSortOrder?,
	): MutableList<MangaSourceInfo> {
		val isAllEnabled = settings.isAllSourcesEnabled
		val result = ArrayList<MangaSourceInfo>(size)
		for (entity in this) {
			val source = entity.toMangaSource() ?: continue
			if (skipNsfwSources && source.isNsfw()) {
				continue
			}
			if (source.isBroken) {
				continue
			}
			if (source is MangaParserSource || source is MihonMangaSource || source is ExternalMangaSource) {
				result.add(
					MangaSourceInfo(
						mangaSource = source,
						isEnabled = entity.isEnabled || isAllEnabled,
						isPinned = entity.isPinned,
					),
				)
			}
		}
		if (sortOrder == SourcesSortOrder.ALPHABETIC) {
			result.sortWith(compareBy<MangaSourceInfo> { !it.isPinned }.thenBy { it.getTitle(context) })
		}
		return result
	}

	private fun observeIsNsfwDisabled() = settings.observeAsFlow(AppSettings.KEY_DISABLE_NSFW) {
		isNsfwContentDisabled
	}

	private fun observeSortOrder() = settings.observeAsFlow(AppSettings.KEY_SOURCES_ORDER) {
		sourcesSortOrder
	}

	private fun observeAllEnabled() = settings.observeAsFlow(AppSettings.KEY_SOURCES_ENABLED_ALL) {
		isAllSourcesEnabled
	}

	private fun MangaSourceEntity.toMangaSource(): ParserMangaSource? {
		if (source.startsWith("mihon:") || source.startsWith("MIHON_")) {
			return mihonExtensionManager.getMihonMangaSourceByName(source)
				?: MangaSource(source, title)
		}
		if (source.startsWith("content:")) {
			return MangaSource(source)
		}
		return MangaParserSource.entries.find { it.name == source }
	}

	private fun String.toMangaSourceOrNull(): ParserMangaSource? {
		if (startsWith("mihon:") || startsWith("MIHON_")) {
			return mihonExtensionManager.getMihonMangaSourceByName(this)
				?: MangaSource(this)
		}
		if (startsWith("content:")) {
			return MangaSource(this)
		}
		return MangaParserSource.entries.find { it.name == this }
	}
}
