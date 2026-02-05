package clipto.presentation.common

import android.content.Context
import android.text.Spannable
import android.text.format.Formatter
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import clipto.AppContext
import clipto.common.extensions.inBrackets
import clipto.common.extensions.notNull
import clipto.common.extensions.toNullIfEmpty
import clipto.common.misc.FormatUtils
import clipto.common.misc.ThemeUtils
import clipto.common.presentation.text.SimpleSpanBuilder
import clipto.domain.*
import clipto.extensions.getTextColorSecondarySpan
import clipto.extensions.getTitle
import com.google.android.material.chip.Chip
import com.wb.clipboard.BuildConfig
import com.wb.clipboard.R
import java.util.*
import kotlin.math.max

object StyleHelper {

    fun getExpandIcon(expanded:Boolean):Int = if (expanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more

    fun bind(chip: Chip, fileRef: FileRef) {
        val state = fileRef.getState()
        chip.text = getLabel(chip.context, fileRef)
        chip.tag = fileRef
        state.apply(chip, fileRef)
    }

    fun getAppTitle(context: Context): CharSequence {
        val appName = context.getString(R.string.app_name)
        val versionColor = ThemeUtils.getColor(context, android.R.attr.textColorSecondary)
        return SimpleSpanBuilder()
            .append(appName)
            .append(" ")
            .append("(${BuildConfig.VERSION_NAME})", RelativeSizeSpan(0.6f), ForegroundColorSpan(versionColor))
            .build()
    }

    fun getLimit(context: Context, limit: Int): CharSequence {
        if (limit == ClientSession.UNLIMITED) {
            return context.getString(R.string.filter_limit_unlimited)
        }
        return limit.toString()
    }

    fun getFilterCounter(context: Context, count: Long, limit: Int?): CharSequence {
        return limit
            ?.takeIf { it > 0 }
            ?.let {
                if (count >= it) {
                    val highlightColor = ThemeUtils.getColor(context, R.attr.swipeActionDelete)
                    SimpleSpanBuilder()
                        .append(count.toString(), ForegroundColorSpan(highlightColor))
                        .append(" / ")
                        .append(it.toString())
                        .build()
                } else {
                    "$count / $it"
                }
            }
            ?: count.toString()
    }

    fun getFilterLabel(context: Context, filter: Filter): CharSequence? =
        filter.getTitle(context).toNullIfEmpty()?.let {
            getFilterLabel(context, it, filter.notesCount)
        }

    fun getFilterLabel(context: Context, name: CharSequence, notesCount: Number): CharSequence {
        return SimpleSpanBuilder()
            .append(name)
            .append(" (${max(0, notesCount.toInt())})")
            .build()
            .also {
                it.setSpan(
                    relativeSpan,
                    name.length,
                    it.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
    }

    fun getFilterLabel(context: Context, name: CharSequence, count: Long, limit: Int?): CharSequence {
        return SimpleSpanBuilder()
            .append(name)
            .append(" (", context.getTextColorSecondarySpan())
            .let { builder ->
                if (limit != null && limit > 0) {
                    if (count >= limit) {
                        val highlightColor = ThemeUtils.getColor(context, R.attr.swipeActionDelete)
                        builder
                            .append(count.toString(), ForegroundColorSpan(highlightColor))
                            .append(" / $limit", context.getTextColorSecondarySpan())
                    } else {
                        builder.append("$count / $limit", context.getTextColorSecondarySpan())
                    }
                } else {
                    builder.append(count.toString(), context.getTextColorSecondarySpan())
                }
            }
            .append(")", context.getTextColorSecondarySpan())
            .build()
            .also {
                it.setSpan(
                    relativeSpan,
                    name.length,
                    it.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
    }

    fun getUsageCountValue(clip: Clip?) =
        clip?.usageCount?.toString() ?: "-"

    fun getSizeValue(clip: Clip?, context: Context) =
        clip?.let { Formatter.formatShortFileSize(context, it.size) } ?: "-"

    fun getCharactersValue(clip: Clip?) =
        clip?.characters?.toString() ?: "-"

    fun getCreateDateValue(clip: Clip?) =
        clip?.createDate?.let { FormatUtils.formatDateTime(it) } ?: "-"

    fun getUpdateDateValue(clip: Clip?) =
        clip?.updateDate?.let { FormatUtils.formatDateTime(it) } ?: "-"

    fun getDeleteDateValue(clip: Clip?) =
        clip?.deleteDate?.let { FormatUtils.formatDateTime(it) } ?: "-"

    fun getModifyDateValue(clip: Clip?) =
        clip?.modifyDate?.let { FormatUtils.formatDateTime(it) } ?: "-"

    fun getDateValue(date: Date?) =
        date?.let { FormatUtils.formatDateTime(it) } ?: "-"

    fun getLabel(context: Context, fileRef: FileRef): CharSequence {
        val size = Formatter.formatShortFileSize(context, fileRef.size)
        val label = fileRef.title.notNull()
        return SimpleSpanBuilder()
            .append(label)
            .append(" ")
            .append(size.inBrackets())
            .build()
            .also {
                if (label.isNotBlank()) {
                    it.setSpan(
                        relativeSpan,
                        label.length,
                        it.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
    }

    fun getNewText(clip: Clip, text: String): String {
        val settings = AppContext.get().getSettings()
        val textPositionBeginning = settings.textPositionBeginning
        val separator = settings.textSeparator
        return if (!textPositionBeginning) {
            "${clip.text}${separator}${text}"
        } else {
            "${text}${separator}${clip.text}"
        }
    }

    private val relativeSpan = RelativeSizeSpan(0.83f)
}