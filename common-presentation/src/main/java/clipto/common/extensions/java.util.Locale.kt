package clipto.common.extensions

import java.util.*

private val localesByCountry by lazy { Locale.getAvailableLocales().associateBy { it.country } }
private val emojies by lazy { mutableMapOf<Locale, String>() }

fun Locale.toEmoji(): String? = emojies[this] ?: run {
    val countryCode = this.country
    if (countryCode.isNotBlank()) {
        val firstLetter = Character.codePointAt(countryCode, 0) - 0x41 + 0x1F1E6
        val secondLetter = Character.codePointAt(countryCode, 1) - 0x41 + 0x1F1E6
        (String(Character.toChars(firstLetter)) + String(Character.toChars(secondLetter))).also {
            emojies[this] = it
        }
    } else {
        null
    }
}

fun toEmoji(country: String): String = localesByCountry[country]?.toEmoji() ?: country
fun toLocale(country: String): Locale? = localesByCountry[country]