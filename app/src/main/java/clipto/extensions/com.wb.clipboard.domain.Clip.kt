package clipto.extensions

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import clipto.AppContext
import clipto.common.extensions.toLinkifiedSpannable
import clipto.common.misc.ThemeUtils
import clipto.dao.objectbox.model.ClipBox
import clipto.dao.objectbox.model.toBox
import clipto.domain.Clip
import clipto.domain.Font
import clipto.domain.ObjectType
import com.wb.clipboard.R
import java.util.*

private val defaultTypefaceBold = Typeface.defaultFromStyle(Typeface.BOLD)

fun Clip.Companion.from(text: String, tracked: Boolean = false, objectType: ObjectType = ObjectType.INTERNAL): Clip = ClipBox().apply {
    this.objectType = objectType
    this.createDate = Date()
    this.tracked = tracked
    this.text = text
}

fun Clip?.getId(): Long = this?.toBox()?.localId ?: 0L

fun Clip?.isNew() = getId() == 0L

fun Clip.toLinkifiedSpannable(): Spannable = text.toLinkifiedSpannable()

fun Clip?.getActionIconRes(ignoreDeletedState: Boolean = false) =
    when {
        !ignoreDeletedState && this?.isDeleted() == true -> R.drawable.action_restore
        this?.isActive == true -> R.drawable.action_clear
        else -> R.drawable.action_copy
    }

fun Clip.updateIcon(textView: TextView) {
    val icon: Int
    val iconColor: Int
    val ctx = textView.context
    when {
        fav -> {
            iconColor = ThemeUtils.getColor(ctx, R.attr.swipeActionStarred)
            icon = R.drawable.clip_icon_fav
        }
        snippet -> {
            val filters = AppContext.get().getFilters()
            val kitId = snippetSetsIds.firstOrNull()
            iconColor = kitId
                ?.let { filters.findFilterBySnippetKitId(kitId) }
                ?.color
                ?.let { Color.parseColor(it) }
                ?: ctx.getTextColorSecondary()
            icon = R.drawable.clip_icon_snippet
        }
        isDynamic() -> {
            iconColor = ctx.getTextColorSecondary()
            icon = R.drawable.clip_icon_dynamic
        }
        isClipboard() -> {
            iconColor = ctx.getTextColorSecondary()
            icon = R.drawable.clip_icon_clipboard
        }
        else -> {
            iconColor = 0
            icon = 0
        }
    }
    textView.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0)
    if (iconColor != 0) {
        TextViewCompat.setCompoundDrawableTintList(textView, ColorStateList.valueOf(iconColor))
    }
}

fun Clip?.getTextTypeface(): Typeface? {
    val appContext = AppContext.get()
    val settings = appContext.getSettings()
    val font = Font.valueOf(settings)
    val baseTypeface = font.typeface
    return if (this?.isActive == true) {
        font.typefaceBold ?: defaultTypefaceBold
    } else {
        baseTypeface
    }
}

fun Clip?.getText(): String? = this?.newText ?: this?.text