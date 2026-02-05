package clipto.domain

import android.content.Context
import clipto.common.misc.Units
import clipto.extensions.getDisplayWidth

data class ListConfig(
    val context: Context,
    val textSize: Int,
    val textFont: Int,
    val textLines: Int,
    val hideLinkPreviews: Boolean,
    val swipeActionRight: SwipeAction,
    val swipeActionLeft: SwipeAction,
    val listStyle: ListStyle = ListStyle.DEFAULT,
    val sortBy: SortBy = SortBy.CREATE_DATE_DESC,
    val excludeWithCustomAttributes: Boolean = false,
    val previewSize: Int = Units.displayMetrics.widthPixels - Units.DP.toPx(76f).toInt(),
    val maxTextLength: Int = (context.getDisplayWidth() / textSize) * (textLines + 1)
) {

    val hasCopyAction: Boolean = swipeActionLeft != SwipeAction.COPY && swipeActionRight != SwipeAction.COPY

    fun copy(from: Filter): ListConfig = copy(
        excludeWithCustomAttributes = from.excludeWithCustomAttributes,
        listStyle = from.listStyle,
        sortBy = from.sortBy
    )

    fun copy(settings: Settings): ListConfig = copy(
        swipeActionRight = settings.swipeActionRight,
        swipeActionLeft = settings.swipeActionLeft
    )

    companion object {
        fun create(settings: Settings, context: Context) = ListConfig(
            context = context,
            textSize = settings.textSize,
            textFont = settings.textFont,
            textLines = settings.textLines,
            hideLinkPreviews = settings.hideLinkPreviews,
            swipeActionRight = settings.swipeActionRight,
            swipeActionLeft = settings.swipeActionLeft
        )
    }
}