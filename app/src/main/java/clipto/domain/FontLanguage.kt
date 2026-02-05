package clipto.domain

import com.wb.clipboard.R

enum class FontLanguage(val titleRes: Int, val exampleRes: Int) {

    ARABIC(R.string.font_language_arabic, R.string.font_arabic),
    CYRILLIC(R.string.font_language_cyrillic, R.string.font_cyrillic),
    DEVANAGARI(R.string.font_language_devanagari, R.string.font_devanagari),
    GREEK(R.string.font_language_greek, R.string.font_greek),
    LATIN(R.string.font_language_latin, R.string.font_latin),

    ;

}