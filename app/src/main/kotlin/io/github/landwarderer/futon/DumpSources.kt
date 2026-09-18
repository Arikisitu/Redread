package io.github.landwarderer.futon
import org.koitharu.kotatsu.parsers.model.MangaParserSource
fun dump() {
    MangaParserSource.entries.forEach { println(it.name + ": " + it.locale) }
}
