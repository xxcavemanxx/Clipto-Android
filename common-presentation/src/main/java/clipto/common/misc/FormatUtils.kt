package clipto.common.misc

import android.content.Context
import android.content.res.Resources
import android.text.format.DateUtils
import android.text.format.Formatter
import clipto.common.extensions.threadLocal
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object FormatUtils {

    private val ratingFormatter = threadLocal { DecimalFormat("#.#") }
    private val dateFormatter = threadLocal { DateFormat.getDateInstance() }
    private val timeFormatter = threadLocal { DateFormat.getTimeInstance() }
    private val uniqueNameSuffixFormatter = threadLocal { SimpleDateFormat("yyMMdd_hhmmss", Locale.ROOT) }
    private val uniqueScreenshotSuffixFormatter = threadLocal { SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.ROOT) }
    private val dateTimeMediumFormatter = threadLocal {
        val locale = runCatching { Resources.getSystem().configuration.locale }.getOrDefault(Locale.ROOT)
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, locale)
    }
    private val dateTimeShortFormatter = threadLocal {
        val locale = runCatching { Resources.getSystem().configuration.locale }.getOrDefault(Locale.ROOT)
        SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale) as SimpleDateFormat
    }

    private const val SCREENSHOT_FILE_NAME_TEMPLATE = "Screenshot_%s_%s.png"
    private const val IMG_FILE_NAME_TEMPLATE = "IMG_%s_%s.jpg"

    private const val UNKNOWN_SIZE = "? B"
    private const val EMPTY = ""
    const val UNKNOWN = "???"
    const val DASH = "-"

    fun clearCache() {
        dateTimeMediumFormatter.remove()
        dateTimeShortFormatter.remove()
    }

    fun getDateTimeShortPattern() = dateTimeShortFormatter.get()!!.toLocalizedPattern()

    fun formatDateTime(date: Long): String {
        return formatDateTime(Date(date))
    }

    fun formatDateTime(date: Date?): String {
        return if (date == null) {
            EMPTY
        } else dateTimeMediumFormatter.get()!!.format(date)
    }

    fun parseDateTime(string: String?): Date? {
        return if (string == null) {
            null
        } else {
            runCatching { dateTimeMediumFormatter.get()!!.parse(string) }.getOrNull()
        }
    }

    fun formatDateTimeShort(date: Date?): String {
        return if (date == null) {
            EMPTY
        } else dateTimeShortFormatter.get()!!.format(date)
    }

    fun parseDateTimeShort(string: String?): Date? {
        return if (string == null) {
            null
        } else {
            runCatching { dateTimeShortFormatter.get()!!.parse(string) }.getOrNull()
        }
    }

    fun formatDateLocal(context: Context, date: Date?): CharSequence {
        return if (date == null) {
            ""
        } else DateUtils.getRelativeDateTimeString(
            context,
            date.time,
            DateUtils.DAY_IN_MILLIS,
            DateUtils.WEEK_IN_MILLIS,
            DateUtils.FORMAT_SHOW_YEAR
        )
    }

    fun formatDate(date: Date?, format: String? = null): String {
        return when {
            date == null -> EMPTY
            format == null -> dateFormatter.get()!!.format(date)
            else -> {
                val locale = runCatching { Resources.getSystem().configuration.locale }.getOrDefault(Locale.ROOT)
                runCatching { SimpleDateFormat(format, locale).format(date) }.getOrDefault(format)
            }
        }
    }

    fun formatTime(date: Date?): String {
        return if (date == null) {
            EMPTY
        } else timeFormatter.get()!!.format(date)
    }

    fun formatSeconds(seconds: Number?): String {
        var secondsRef = seconds
        if (secondsRef == null) {
            secondsRef = 0
        }
        val value = secondsRef.toLong()
        val hours = value / 3600
        val minutes = value % 3600 / 60
        secondsRef = value % 60
        return String.format("%02d:%02d:%02d", hours, minutes, secondsRef)
    }

    fun formatSize(context: Context, size: Number?): String {
        if (size == null) {
            return UNKNOWN_SIZE
        }
        return Formatter.formatShortFileSize(context, size.toLong())
    }

    fun formatNumber(number: Double?, withScale: Int, withPlusIfPositive: Boolean): String {
        var decimal = BigDecimal.valueOf(number!!)
        decimal = decimal.setScale(withScale, BigDecimal.ROUND_HALF_UP)
        if (withPlusIfPositive) {
            if (decimal > BigDecimal.ZERO) {
                return "+$decimal"
            }
        }
        return decimal.toString()

    }

    fun formatNumber(number: Float?, withScale: Int, withPlusIfPositive: Boolean, withStripTrailingZeros: Boolean = true): String {
        if (number == null || java.lang.Float.isInfinite(number) || java.lang.Float.isNaN(number)) {
            return EMPTY
        }
        var decimal = BigDecimal.valueOf(number.toDouble())
        decimal = decimal.setScale(withScale, BigDecimal.ROUND_HALF_UP)
        if (withStripTrailingZeros) {
            val fractional = decimal.remainder(BigDecimal.ONE)
            if (fractional.toFloat() == 0.0f) {
                decimal = decimal.setScale(2, BigDecimal.ROUND_HALF_UP)
            } else {
                decimal = decimal.stripTrailingZeros()
            }
        }
        if (withPlusIfPositive) {
            if (decimal > BigDecimal.ZERO) {
                return "+" + decimal.toPlainString()
            }
        }
        return decimal.toPlainString()

    }

    fun formatNumber(number: Int?, withPlusIfPositive: Boolean): String {
        if (withPlusIfPositive) {
            if (number != null && number > 0) {
                return "+$number"
            }
        }
        return "$number"

    }

    fun formatCurrency(number: Number?, currency: Currency?, withScale: Int, withPlusIfPositive: Boolean): String {
        var currencyRef = currency
        if (number == null) {
            return EMPTY
        }
        val nf = NumberFormat.getCurrencyInstance()
        nf.roundingMode = RoundingMode.HALF_UP
        nf.minimumFractionDigits = withScale
        nf.maximumFractionDigits = withScale
        if (currencyRef != null) {
            nf.currency = currencyRef
        }
        var text = nf.format(number)
        if (currencyRef == null) {
            currencyRef = Currency.getInstance(Locale.getDefault())
            text = text.replace(currencyRef!!.symbol, "")
        }
        if (withPlusIfPositive) {
            if (number.toFloat() > 0f) {
                text = "+$text"
            }
        }
        return text.trim { it <= ' ' }
    }

    fun formatRating(rating: Number?): String {
        var rating = rating
        if (rating == null) {
            rating = 0f
        }
        return ratingFormatter.get()!!.format(rating)
    }

    fun formatCountryCode(code: String): String {
        val locale = Locale("", code)
        return locale.displayCountry
    }

    fun buildString(vararg strings: String): String {
        var size = 0
        for (string in strings) {
            size += string.length
        }
        val builder = StringBuilder(size)
        for (string in strings) {
            builder.append(string)
        }
        return builder.toString()
    }

    fun buildPath(vararg strings: String): String {
        var size = 0
        for (string in strings) {
            size += string.length + 1
        }
        val builder = StringBuilder(size)
        for (string in strings) {
            if (!string.startsWith(File.separator)) {
                builder.append(File.separator)
            }
            builder.append(string)
        }
        return builder.toString()
    }

    fun buildUniqueName(prefix: String): String {
        var prefixRef = prefix
        prefixRef = prefixRef.trim { it <= ' ' }
        prefixRef = prefixRef.replace('.', '_')
        prefixRef = prefixRef.replace(':', '_')
        val suffix = uniqueNameSuffixFormatter.get()!!.format(Date())
        return buildString(prefixRef, "_", suffix)
    }

    fun newScreenshotName(prefix: String): String {
        val screenshotSuffix = uniqueScreenshotSuffixFormatter.get()!!.format(Date())
        return String.format(SCREENSHOT_FILE_NAME_TEMPLATE, screenshotSuffix, prefix)
    }

    fun newImgName(prefix: String): String {
        val screenshotSuffix = uniqueNameSuffixFormatter.get()!!.format(Date())
        return String.format(IMG_FILE_NAME_TEMPLATE, screenshotSuffix, prefix)
    }

}
