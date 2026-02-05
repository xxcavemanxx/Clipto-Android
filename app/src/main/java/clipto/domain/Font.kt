package clipto.domain

import android.content.Context
import android.graphics.Typeface
import android.util.SparseArray
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import clipto.AppContext
import clipto.analytics.Analytics
import com.wb.clipboard.R

enum class Font(val id: Int, val fontRes: Int, val titleRes: Int, vararg val languages: FontLanguage) {

    DEFAULT(0, 0, R.string.theme_default, FontLanguage.LATIN) {
        override fun apply(textView: TextView, withLoadingState: Boolean) {
            textView.typeface = null
        }

        override fun isAvailable(): Boolean = true
        override fun initTypeface(context: Context) = Unit
    },

    // A
    AMIRI(8, R.font.amiri, R.string.text_config_font_amiri, FontLanguage.ARABIC, FontLanguage.LATIN),
    CAIRO(9, R.font.cairo, R.string.text_config_font_cairo, FontLanguage.ARABIC, FontLanguage.LATIN),

    // C
    KURALE(25, R.font.kurale, R.string.text_config_font_kurale, FontLanguage.CYRILLIC, FontLanguage.DEVANAGARI, FontLanguage.LATIN),

    ALEGREYA(14, R.font.alegreya, R.string.text_config_font_alegreya, FontLanguage.CYRILLIC, FontLanguage.GREEK, FontLanguage.LATIN),
    MANROPE(29, R.font.manrope, R.string.text_config_font_manrope, FontLanguage.CYRILLIC, FontLanguage.GREEK, FontLanguage.LATIN),
    NOTO_SANS(11, R.font.noto_sans, R.string.text_config_font_noto_sans, FontLanguage.CYRILLIC, FontLanguage.GREEK, FontLanguage.LATIN),
    NOTO_SERIF(12, R.font.noto_serif, R.string.text_config_font_noto_serif, FontLanguage.CYRILLIC, FontLanguage.GREEK, FontLanguage.LATIN),
    OPEN_SANS(3, R.font.open_sans, R.string.text_config_font_open_sans, FontLanguage.CYRILLIC, FontLanguage.GREEK, FontLanguage.LATIN),
    ROBOTO(1, R.font.roboto, R.string.text_config_font_roboto, FontLanguage.CYRILLIC, FontLanguage.GREEK, FontLanguage.LATIN),
    ROBOTO_MONO(6, R.font.roboto_mono, R.string.text_config_font_roboto_mono, FontLanguage.CYRILLIC, FontLanguage.GREEK, FontLanguage.LATIN),
    SOURCE_CODE_PRO(27, R.font.source_code_pro, R.string.text_config_font_source_code_pro, FontLanguage.CYRILLIC, FontLanguage.GREEK, FontLanguage.LATIN),
    UBUNTU(5, R.font.ubuntu, R.string.text_config_font_ubuntu, FontLanguage.CYRILLIC, FontLanguage.GREEK, FontLanguage.LATIN),
    UBUNTU_MONO(7, R.font.ubuntu_mono, R.string.text_config_font_ubuntu_mono, FontLanguage.CYRILLIC, FontLanguage.GREEK, FontLanguage.LATIN),
    JETBRAINS_MONO(28, R.font.jetbrains_mono, R.string.text_config_font_jetbrains_mono, FontLanguage.CYRILLIC, FontLanguage.GREEK, FontLanguage.LATIN),
    RUBIK(30, R.font.rubik, R.string.text_config_font_rubik, FontLanguage.CYRILLIC, FontLanguage.GREEK, FontLanguage.LATIN),

    GABRIELA(24, R.font.gabriela, R.string.text_config_font_gabriela, FontLanguage.CYRILLIC, FontLanguage.LATIN),
    MONTSERRAT(4, R.font.montserrat, R.string.text_config_font_montserrat, FontLanguage.CYRILLIC, FontLanguage.LATIN),
    OSWALD(22, R.font.oswald, R.string.text_config_font_oswald, FontLanguage.CYRILLIC, FontLanguage.LATIN),
    PHILOSOPHER(26, R.font.philosopher, R.string.text_config_font_philosopher, FontLanguage.CYRILLIC, FontLanguage.LATIN),

    // D
    HIND(10, R.font.hind, R.string.text_config_font_hind, FontLanguage.DEVANAGARI),
    POPPINS(13, R.font.poppins, R.string.text_config_font_poppins, FontLanguage.DEVANAGARI),

    // G
    ADVENT_PRO(19, R.font.advent_pro, R.string.text_config_font_advent_pro, FontLanguage.GREEK),
    GFS_DIDOT(20, R.font.gfs_didot, R.string.text_config_font_gfs_didot, FontLanguage.GREEK),

    // L
    AMATIC(17, R.font.amatic_sc, R.string.text_config_font_amatic, FontLanguage.LATIN),
    CAVEAT(18, R.font.caveat, R.string.text_config_font_caveat, FontLanguage.LATIN),
    CAVEAT_BRUSH(16, R.font.caveat_brush, R.string.text_config_font_caveat_brush, FontLanguage.LATIN),
    CHRISTMAS(15, R.font.mountains_of_christmas, R.string.text_config_font_mountains_of_christmas, FontLanguage.LATIN),
    NEW_ROCKER(23, R.font.new_rocker, R.string.text_config_font_new_rocker, FontLanguage.LATIN),
    RALEWAY(21, R.font.raleway, R.string.text_config_font_raleway, FontLanguage.LATIN),
    WORK_SANS(2, R.font.work_sans, R.string.text_config_font_work_sans, FontLanguage.LATIN),

    MORE(-1, 0, R.string.font_more, FontLanguage.LATIN, FontLanguage.CYRILLIC) {
        override fun isAvailable(): Boolean = false
    },

    ;

    var typeface: Typeface? = null
        set(value) {
            field = value
            if (value != null) {
                typefaceBold = Typeface.create(value, Typeface.BOLD)
            }
        }
    var typefaceBold: Typeface? = null

    open fun initTypeface(context: Context) {
        if (typeface == null) {
            typeface = ResourcesCompat.getFont(context, fontRes)
        }
    }

    open fun apply(textView: TextView, withLoadingState: Boolean = true) {
        try {
            val fontRef = this
            if (typeface != null) {
                textView.typeface = typeface
            } else {
                val appContext = AppContext.get()
                val appState = appContext.appState
                if (withLoadingState) appState.setLoadingState()
                ResourcesCompat.getFont(textView.context, fontRes, object : ResourcesCompat.FontCallback() {
                    override fun onFontRetrieved(typeface: Typeface) {
                        try {
                            fontRef.typeface = typeface
                            textView.typeface = typeface
                        } catch (e: Exception) {
                            Analytics.onError("error_font_retrieved", e)
                        } finally {
                            if (withLoadingState) appState.setLoadedState()
                        }
                    }

                    override fun onFontRetrievalFailed(reason: Int) {
                        if (withLoadingState) appState.setLoadedState()
                    }
                }, null)
            }
        } catch (e: Exception) {
            Analytics.onError("error_apply_font", e)
        }
    }

    open fun isAvailable() = true

    fun toMeta(): FontMeta = FontMeta(id, order, visible)

    val uid by lazy { languages.map { it.ordinal }.joinToString("_") }
    var visible: Boolean = false
    var order: Int = id

    companion object {

        init {
            ALEGREYA.visible = true
            NOTO_SANS.visible = true
            OPEN_SANS.visible = true
            UBUNTU.visible = true
            RUBIK.visible = true
        }

        private val itemsArray = SparseArray<Font>().also { array ->
            values().forEachIndexed { index, font ->
                font.order = index
                array.put(font.id, font)
            }
            DEFAULT.order = -1
        }

        fun getMoreFonts() = values().filter { it.isAvailable() && it != DEFAULT }.sortedBy { it.order }

        fun valueOf(id: Int): Font? = itemsArray.get(id)

        fun valueOf(settings: Settings?): Font = itemsArray[settings?.textFont ?: 0]
            .let { return if (it.isAvailable()) it else DEFAULT }
    }

}