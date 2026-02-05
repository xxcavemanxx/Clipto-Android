package clipto.cache

import android.text.Spanned
import clipto.AppContext
import clipto.common.misc.ThemeUtils
import clipto.extensions.getTagBackgroundColor
import clipto.presentation.common.widget.ColorfulTagSpan

object AppTextCache {

    const val TYPE_FONT = 0
    const val TYPE_TAG = 1
    const val TYPE_PLACEHOLDER = 2

    private val stringCache = mutableMapOf<String, CacheItem>()
    private val spanCache = mutableMapOf<Spanned, CacheItem>()

    private val colorProviders: Array<ColorProvider> = arrayOf(
            object : ColorProvider {
                override fun getTagColor(text: String): Int {
                    return 0
                }

                override fun getTextColor(tagColor: Int): Int {
                    return ThemeUtils.getColor(AppContext.get().app, android.R.attr.textColorPrimary)
                }
            },
            object : ColorProvider {
                override fun getTagColor(text: String): Int {
                    return text.getTagBackgroundColor(AppContext.get().app)
                }

                override fun getTextColor(tagColor: Int): Int {
                    return AppColorCache.getColorOnSurface(tagColor)
                }
            },
            object : ColorProvider {
                override fun getTagColor(text: String): Int {
                    return 0
                }

                override fun getTextColor(tagColor: Int): Int {
                    val viewModel = AppContext.get()
                    return ThemeUtils.getColor(viewModel.app, android.R.attr.textColorPrimary)
                }
            }
    )

    fun clearCache() {
        stringCache.clear()
        spanCache.clear()
    }

    fun getItem(spanned: Spanned): CacheItem? = spanCache[spanned]

    fun getOrPut(key: String, type: Int, defaultValue: () -> Spanned?): Spanned? {
        return stringCache
                .getOrPut(key) {
                    val spanned = defaultValue.invoke()
                    val spans = spanned?.getSpans(0, spanned.length, ColorfulTagSpan::class.java)
                    val item = CacheItem(
                            spanned = spanned,
                            spans = spans
                    )
                    spanned?.let { spanCache[it] = item }
                    item
                }
                .also {
                    it.spans?.forEach { span ->
                        val provider = colorProviders[type]
                        val tagColor = provider.getTagColor(span.tagId)
                        span.textColor = provider.getTextColor(tagColor)
                        span.tagColor = tagColor
                    }
                }
                .spanned
    }

    data class CacheItem(
            val spanned: Spanned?,
            val spans: Array<ColorfulTagSpan>?
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as CacheItem

            if (spanned != other.spanned) return false
            if (spans != null) {
                if (other.spans == null) return false
                if (!spans.contentEquals(other.spans)) return false
            } else if (other.spans != null) return false

            return true
        }

        override fun hashCode(): Int {
            var result = spanned?.hashCode() ?: 0
            result = 31 * result + (spans?.contentHashCode() ?: 0)
            return result
        }
    }

    interface ColorProvider {
        fun getTagColor(text: String): Int
        fun getTextColor(tagColor: Int): Int
    }

}