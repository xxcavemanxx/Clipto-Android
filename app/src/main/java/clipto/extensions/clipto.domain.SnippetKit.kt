package clipto.extensions

import clipto.common.extensions.notNull
import clipto.common.extensions.toEmoji
import clipto.common.presentation.text.SimpleSpanBuilder
import clipto.domain.SnippetKit
import java.util.*

fun SnippetKit.getLanguageFlag(): String? {
    if (language == null || country == null) return null
    val locale = Locale(language, country)
    return locale.toEmoji()
}

fun SnippetKit.getLanguageLabel(): String {
    if (language == null || country == null) return ""
    val locale = Locale(language, country)
    return locale.getDisplayLanguage(Locale.getDefault())
}

fun SnippetKit.getUserNameLabel(): CharSequence {
    return SimpleSpanBuilder()
            .append(getLanguageFlag().notNull())
            .append("  ")
            .append(userName.notNull())
            .build()
            .trim()
}